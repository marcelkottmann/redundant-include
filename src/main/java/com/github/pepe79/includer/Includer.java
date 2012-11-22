package com.github.pepe79.includer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class Includer
{
	public static void main(String[] args) throws IOException
	{
		URL rootFolder = Includer.class.getResource("/.");

		File rootFolderFile = new File(rootFolder.getFile());
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

		for (String file : ctx.getFileToResult().keySet())
		{
			System.out.println(file + " status " + ctx.getFileToResult().get(file).getStatus());
		}
	}

	private static Context scan(List<File> files) throws IOException
	{
		Context ctx = new Context();

		Pattern tag = Pattern.compile("(<!-- <(INCLUDE) file=\"([^\"]*)\"> -->|<!-- <(/INCLUDE)> -->)");

		for (File f : files)
		{
			List<IncludeDescriptor> descriptors = new ArrayList<IncludeDescriptor>();
			Stack<IncludeDescriptor> stack = new Stack<IncludeDescriptor>();

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(new FileReader(f), bos);
			String fileContent = new String(bos.toByteArray());

			Matcher matcher = tag.matcher(fileContent);

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
						start = child.getEnd();
					}
					relevantContent.append(fileContent.substring(start, td.getEnd()));

					td.setContent(relevantContent);
				}
			}

			for (IncludeDescriptor td : descriptors)
			{
				// System.out.println("---- INCLUDE: " + td.getFile() +
				// " ----");
				// System.out.println(td.getContent());
				// System.out.println("---------------------------------");

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
}
