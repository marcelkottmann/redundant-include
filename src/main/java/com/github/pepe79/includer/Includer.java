package com.github.pepe79.includer;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class Includer
{
	private static final Pattern TAG = Pattern
			.compile("(<!--\\s*<(INCLUDE)\\s+file=\"([^\"]*)\">\\s*-->|<!--\\s*<(/INCLUDE)>\\s*-->)");

	public static final String CONTRACTED_FLAG = "<!-- CONTRACTED --->";

	private static final Pattern EMPTY_TAGS = Pattern.compile("(<!--\\s*<INCLUDE\\s+file=\"([^\"]*)\">\\s*-->)"
			+ CONTRACTED_FLAG + "(<!--\\s*</INCLUDE>\\s*-->)");

	private static final String REDUNDANT_INCLUDE_DIR = "/redundant-includes";

	private static boolean allInOkState(Context ctx) throws NoSuchAlgorithmException, IOException
	{
		// create status list
		Map<String, Status> statusMap = new HashMap<String, Status>();
		for (String file : ctx.getFileToResult().keySet())
		{
			Status status = createStatus(file, ctx.getFileToResult().get(file));
			statusMap.put(file, status);
			if (!StatusCode.OK.equals(status.getStatusCode()))
			{
				return false;
			}
		}
		return true;
	}

	private static void contract(PrintStream out, Context ctx, File outputDir) throws NoSuchAlgorithmException,
			IOException
	{
		if (!allInOkState(ctx))
		{
			out.println("Not in ok state. Please merge or resolve conflict.");
			return;
		}

		int numExtracted = 0;
		for (String file : ctx.getFileToResult().keySet())
		{
			File output = new File(outputDir, file);
			StringBuffer content = ctx.getFileToResult().get(file).getDescriptors().get(0).getContent();

			if (!CONTRACTED_FLAG.equals(content.toString()))
			{
				output.getParentFile().mkdirs();
				FileWriter fw = new FileWriter(output);
				fw.write(content.toString());
				fw.close();
				numExtracted++;
			}
			else
			{
				out.println("Skipping " + file + " because it's already contracted.");
			}
		}
		out.println(numExtracted + " includes extracted.");

		Set<File> origins = getOrigins(ctx);
		for (File origin : origins)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(new FileReader(origin), bos);
			String oldFileContent = new String(bos.toByteArray());
			StringBuffer contracted = contract(oldFileContent);
			FileWriter fw = new FileWriter(origin);
			fw.write(contracted.toString());
			fw.close();
		}
		out.println(origins.size() + " files contracted.");
	}

	protected static StringBuffer contract(String oldFileContent)
	{
		StringBuffer result = new StringBuffer();
		Matcher matcher = TAG.matcher(oldFileContent);

		int statckCounter = 0;
		int start = 0;
		int end = -1;
		while (matcher.find())
		{
			if ("INCLUDE".equals(matcher.group(2)))
			{
				if (statckCounter == 0)
				{
					end = matcher.end();
					result.append(oldFileContent.substring(start, end));
					result.append(CONTRACTED_FLAG);
				}
				statckCounter++;
			}
			else
			{
				statckCounter--;
				if (statckCounter == 0)
				{
					start = matcher.start();
				}
			}
		}
		result.append(oldFileContent.substring(start));
		return result;
	}

	public static Status createStatus(String file, IncludeScanResult result) throws IOException,
			NoSuchAlgorithmException
	{
		Status status = new Status();

		status.setFile(file);

		Map<String, List<IncludeDescriptor>> hashes = new HashMap<String, List<IncludeDescriptor>>();

		for (IncludeDescriptor d : result.getDescriptors())
		{
			String hash = new String(Hex.encodeHex(MessageDigest.getInstance("MD5").digest(
					d.getContent().toString().getBytes())));

			List<IncludeDescriptor> descriptors = hashes.get(hash);
			if (descriptors == null)
			{
				descriptors = new ArrayList<IncludeDescriptor>();
				hashes.put(hash, descriptors);
			}
			descriptors.add(d);
		}

		status.setHashes(hashes);

		switch (hashes.size())
		{
		case 1:
			status.setStatusCode(StatusCode.OK);
			status.setNewHash(hashes.keySet().iterator().next());
			break;
		case 2:
			Iterator<Map.Entry<String, List<IncludeDescriptor>>> it = hashes.entrySet().iterator();

			Map.Entry<String, List<IncludeDescriptor>> descriptors1 = it.next();
			Map.Entry<String, List<IncludeDescriptor>> descriptors2 = it.next();

			String newHash = null;
			String oldHash = null;

			int diff = descriptors1.getValue().size() - descriptors2.getValue().size();
			if (diff == 0)
			{
				long lastModified1 = maxLastModified(descriptors1);
				long lastModified2 = maxLastModified(descriptors2);

				if (lastModified1 > lastModified2)
				{
					newHash = descriptors1.getKey();
					oldHash = descriptors2.getKey();
				}
				else if (lastModified1 < lastModified2)
				{
					newHash = descriptors2.getKey();
					oldHash = descriptors1.getKey();
				}
				else
				{
					// equal
					throw new RuntimeException("Unresolved conflict.");
				}
			}
			else
			{
				if (diff > 0)
				{
					newHash = descriptors2.getKey();
					oldHash = descriptors1.getKey();
				}
				else
				{
					newHash = descriptors1.getKey();
					oldHash = descriptors2.getKey();
				}
			}

			status.setStatusCode(StatusCode.DIFFER);
			status.setOldHash(oldHash);
			status.setNewHash(newHash);

			break;
		default:
			status.setStatusCode(StatusCode.CONFLICT);
			break;
		}

		return status;
	}

	private static void expand(PrintStream out, Context ctx, File inputDir) throws NoSuchAlgorithmException,
			IOException
	{

		if (!inputDir.exists())
		{
			out.println("Input directory does not exists. Maybe nothing to expand.");
			return;
		}

		File[] files = inputDir.listFiles();

		Map<String, StringBuffer> fileToContent = new HashMap<String, StringBuffer>();
		for (File file : files)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			FileReader fr = new FileReader(file);
			IOUtils.copy(fr, bos);
			fr.close();
			StringBuffer fileContent = new StringBuffer(new String(bos.toByteArray()));
			fileToContent.put(file.getName(), fileContent);

			// TODO: check for rename success
			file.renameTo(new File(file.getParentFile(), file.getName() + ".bkp"));
		}

		IncludeProvider includeProvider = new SimpleMapIncludeProvider(fileToContent);

		Set<File> origins = getOrigins(ctx);

		for (File origin : origins)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(new FileReader(origin), bos);
			StringBuffer fileContent = new StringBuffer(new String(bos.toByteArray()));
			StringBuffer contracted = expand(fileContent, includeProvider);
			FileWriter fw = new FileWriter(origin);
			fw.write(contracted.toString());
			fw.close();
		}

		for (File file : files)
		{
			new File(file.getParent(), file.getName() + ".bkp").delete();
		}

		inputDir.delete();

		out.println(origins.size() + " files expanded.");
		out.println(files.length + " includes used.");

	}

	protected static StringBuffer expand(StringBuffer newContent, IncludeProvider includeProvider)
	{
		StringBuffer result = new StringBuffer();
		Matcher m = EMPTY_TAGS.matcher(newContent);

		int start = 0;
		int end = -1;
		while (m.find())
		{
			end = m.start();
			result.append(newContent.substring(start, end));
			result.append(m.group(1));
			result.append(expand(includeProvider.getContent(m.group(2)), includeProvider));
			result.append(m.group(3));
			start = m.end();
		}
		result.append(newContent.substring(start));

		return result;
	}

	private static Set<File> getOrigins(Context ctx)
	{
		Set<File> files = new HashSet<File>();
		for (IncludeScanResult r : ctx.getFileToResult().values())
		{
			for (IncludeDescriptor d : r.getDescriptors())
			{
				files.add(d.getOrigin());
			}
		}

		return files;
	}

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException
	{
		String workingDir = System.getProperty("user.dir");

		System.out.println("Scanning folder " + workingDir);

		File rootFolderFile = new File(workingDir);
		List<File> files = Arrays.asList(rootFolderFile.listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".html");
			}
		}));

		// scan files
		Context ctx = scan(files);

		if (args.length > 0 && "status".equals(args[0]))
		{
			Set<String> includeFiles = ctx.getFileToResult().keySet();
			if (includeFiles.isEmpty())
			{
				System.out.println("No includes found.");
			}
			else if (new File(workingDir + REDUNDANT_INCLUDE_DIR).exists())
			{
				System.out.println("Seems like directory is in contracted state. Please expand to see the status.");
			}
			else
			{
				for (String file : includeFiles)
				{
					System.out.print(file + " status ");
					printStatus(System.out, file, ctx.getFileToResult().get(file));
				}
			}
		}
		else if (args.length > 0 && ("merge".equals(args[0]) || "reversemerge".equals(args[0])))
		{
			merge(System.out, ctx, "reversemerge".equals(args[0]));
		}
		else if (args.length > 0 && "resolve".equals(args[0]))
		{
			List<String> overrideHashes = new ArrayList<String>(Arrays.asList(args));
			// remove first argument, i.e. "resolve"
			overrideHashes.remove(0);

			resolve(System.out, ctx, overrideHashes);
		}
		else if (args.length > 0 && "contract".equals(args[0]))
		{
			contract(System.out, ctx, new File(workingDir + REDUNDANT_INCLUDE_DIR));
		}
		else if (args.length > 0 && "expand".equals(args[0]))
		{
			expand(System.out, ctx, new File(workingDir + REDUNDANT_INCLUDE_DIR));
		}
		else if (args.length > 0 && "show".equals(args[0]))
		{
			if (args.length > 1)
			{
				show(System.out, ctx, args[1]);
			}
			else
			{
				System.out.println("Please provide template filename or hash.");
			}
		}
		else
		{
			System.out.println("No command provided.");
		}
	}

	private static void show(PrintStream out, Context ctx, String fileOrHash) throws NoSuchAlgorithmException,
			IOException
	{
		IncludeScanResult result = ctx.getFileToResult().get(fileOrHash);
		if (result == null)
		{
			// perhaps hash?
			for (Map.Entry<String, IncludeScanResult> e : ctx.getFileToResult().entrySet())
			{
				Status status = createStatus(e.getKey(), e.getValue());
				List<IncludeDescriptor> includeDescriptors = status.getHashes().get(fileOrHash);
				if (includeDescriptors != null)
				{
					showFilename(out, e.getKey());
					show(out, fileOrHash, includeDescriptors.get(0));
				}
			}
		}
		else
		{
			showFilename(out, fileOrHash);
			Status status = createStatus(fileOrHash, result);
			for (Map.Entry<String, List<IncludeDescriptor>> e : status.getHashes().entrySet())
			{
				show(out, e.getKey(), e.getValue().get(0));
			}
		}

	}

	private static void showFilename(PrintStream out, String file)
	{
		out.println(file);
	}

	private static void show(PrintStream out, String hash, IncludeDescriptor includeDescriptor)
	{
		out.println(hash);
		out.println("--");
		out.println(includeDescriptor.getContent());
		out.println("----");
	}

	private static long maxLastModified(Entry<String, List<IncludeDescriptor>> descriptors)
	{
		long maxLastModified = 0;
		for (IncludeDescriptor d : descriptors.getValue())
		{
			if (d.getOrigin().lastModified() > maxLastModified)
			{
				maxLastModified = d.getOrigin().lastModified();
			}
		}
		return maxLastModified;
	}

	private static void merge(PrintStream out, Context ctx, boolean reverseMerge) throws IOException,
			NoSuchAlgorithmException
	{
		// create status list
		boolean conflictState = false;
		Map<String, Status> statusMap = new HashMap<String, Status>();
		for (String file : ctx.getFileToResult().keySet())
		{
			Status status = createStatus(file, ctx.getFileToResult().get(file));
			statusMap.put(file, status);
			if (StatusCode.CONFLICT.equals(status.getStatusCode()))
			{
				conflictState = true;
			}
		}

		if (conflictState)
		{
			out.println("Some includes are in state " + StatusCode.CONFLICT
					+ " -> Please resolve the conflict first. Aborting merge:");
			for (Map.Entry<String, Status> e : statusMap.entrySet())
			{
				if (StatusCode.CONFLICT.equals(e.getValue().getStatusCode()))
				{
					out.println(e.getKey());
				}
			}
		}
		else
		{
			merge(out, statusMap, reverseMerge);
		}
	}

	private static void merge(PrintStream out, Map<String, Status> statusMap, boolean reverseMerge)
			throws FileNotFoundException, IOException
	{
		for (String file : statusMap.keySet())
		{
			Status status = statusMap.get(file);
			if (StatusCode.DIFFER.equals(status.getStatusCode()))
			{
				List<String> hashes = new ArrayList<String>();
				List<IncludeDescriptor> descriptorsToOverride = null;
				if (reverseMerge)
				{
					hashes.add(status.getOldHash());
					descriptorsToOverride = status.getNewDescriptors();
				}
				else
				{
					hashes.add(status.getNewHash());
					descriptorsToOverride = status.getOldDescriptors();
				}

				System.out.println("Hash to use: " + hashes);

				updateOldContent(descriptorsToOverride, statusMap, hashes);
				out.println(file + " [M]");
			}
		}
	}

	private static void printConflict(PrintStream out, Status status)
	{
		if (!StatusCode.CONFLICT.equals(status.getStatusCode()))
		{
			throw new IllegalStateException("Must be in " + StatusCode.CONFLICT + " state.");
		}

		String file = status.getFile();

		out.println("Include " + file + " has " + status.getHashes().size() + " different states:");

		for (Map.Entry<String, List<IncludeDescriptor>> e : status.getHashes().entrySet())
		{
			out.println(e.getKey() + ":");
			for (IncludeDescriptor d : e.getValue())
			{
				out.println("\t" + d.getOrigin());
			}
		}
	}

	private static void printDiff(PrintStream out, StringBuffer content1, StringBuffer content2) throws IOException
	{
		StringBuffer buffer = new StringBuffer();
		List<String> lines1 = toLines(content1.toString());
		List<String> lines2 = toLines(content2.toString());
		Patch patch = DiffUtils.diff(lines1, lines2);
		for (Delta delta : patch.getDeltas())
		{
			buffer.append("---- ");
			buffer.append(delta.getType());
			buffer.append("\n");
			buffer.append(delta.getRevised());
			buffer.append("\n----\n");
		}
		out.println(buffer.toString());
	}

	public static void printStatus(PrintStream out, String file, IncludeScanResult result) throws IOException,
			NoSuchAlgorithmException
	{
		Status status = createStatus(file, result);
		out.println(status.getStatusCode());
		switch (status.getStatusCode())
		{
		case DIFFER:
			out.println("New hash " + status.getNewHash());
			out.println("Old hash " + status.getOldHash());

			out.println("Newest version in ");
			for (IncludeDescriptor d : status.getNewDescriptors())
			{
				out.println(d.getOrigin());
			}

			printDiff(out, status.getOldDescriptors().get(0).getContent(), status.getNewDescriptors().get(0)
					.getContent());
			break;

		case CONFLICT:
			printConflict(out, status);
			break;
		case OK:
			// do nothing
			break;
		}
	}

	private static void resolve(PrintStream out, Context ctx, List<String> overrideHashes)
			throws NoSuchAlgorithmException, IOException
	{
		Map<String, Status> statusMap = new HashMap<String, Status>();
		for (String file : ctx.getFileToResult().keySet())
		{
			Status status = createStatus(file, ctx.getFileToResult().get(file));
			statusMap.put(file, status);
		}

		for (Status status : statusMap.values())
		{
			if (!StatusCode.OK.equals(status.getStatusCode()))
			{
				for (String hash : overrideHashes)
				{
					if (status.getHashes().containsKey(hash))
					{
						List<IncludeDescriptor> allDescriptors = ctx.getFileToResult().get(status.getFile())
								.getDescriptors();
						updateOldContent(allDescriptors, statusMap, overrideHashes);
						out.println(status.getFile() + " [R]");
					}
				}
			}
		}

	}

	private static Context scan(List<File> files) throws IOException
	{
		Context ctx = new Context();

		for (File f : files)
		{
			List<IncludeDescriptor> descriptors = new ArrayList<IncludeDescriptor>();
			Stack<IncludeDescriptor> stack = new Stack<IncludeDescriptor>();

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(new FileReader(f), bos);
			String fileContent = new String(bos.toByteArray());

			Matcher matcher = TAG.matcher(fileContent);

			while (matcher.find())
			{
				if ("INCLUDE".equals(matcher.group(2)))
				{
					IncludeDescriptor td = new IncludeDescriptor();
					td.setStart(matcher.end());
					td.setFile(matcher.group(3));
					td.setOrigin(f);

					if (!stack.isEmpty())
					{
						td.setParent(stack.peek());
						stack.peek().addChild(td);
					}
					stack.push(td);
				}
				else
				{
					IncludeDescriptor td = stack.pop();
					td.setEnd(matcher.start());
					descriptors.add(td);

					StringBuffer relevantContent = new StringBuffer();
					int start = td.getStart();
					int end = -1;
					for (IncludeDescriptor child : td.getChildren())
					{
						end = child.getStart();
						relevantContent.append(fileContent.substring(start, end));
						relevantContent.append(Includer.CONTRACTED_FLAG);
						start = child.getEnd();
					}
					relevantContent.append(fileContent.substring(start, td.getEnd()));

					td.setContent(relevantContent);
				}
			}

			for (IncludeDescriptor td : descriptors)
			{
				IncludeScanResult includeScanResult = ctx.getFileToResult().get(td.getFile());
				if (includeScanResult == null)
				{
					includeScanResult = new IncludeScanResult();
					ctx.getFileToResult().put(td.getFile(), includeScanResult);
				}
				includeScanResult.getDescriptors().add(td);
			}
		}

		return ctx;
	}

	private static List<String> toLines(String input) throws IOException
	{
		final List<String> lines = new ArrayList<String>();
		String line;
		final BufferedReader in = new BufferedReader(new StringReader(input));
		while ((line = in.readLine()) != null)
		{
			lines.add(line);
		}
		return lines;
	}

	private static void updateOldContent(List<IncludeDescriptor> oldDescriptors, Map<String, Status> statusMap,
			List<String> overrideHashes) throws FileNotFoundException, IOException
	{
		for (IncludeDescriptor d : oldDescriptors)
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(new FileReader(d.getOrigin()), bos);
			String oldFileContent = new String(bos.toByteArray());
			StringBuffer unexpanded = contract(oldFileContent);
			StringBuffer expanded = expand(unexpanded, new StatusMapIncludeProvider(statusMap, overrideHashes));
			FileWriter fw = new FileWriter(d.getOrigin());
			fw.write(expanded.toString());
			fw.close();
		}
	}

}
