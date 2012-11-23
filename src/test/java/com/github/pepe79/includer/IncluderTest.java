package com.github.pepe79.includer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

public class IncluderTest
{
	@Test
	public void testNestedExpand()
	{
		String includeFilename = "test";
		String includeFilename2 = "test2";

		Map<String, Status> statusMap = new HashMap<String, Status>();

		List<IncludeDescriptor> newDescriptors = new ArrayList<IncludeDescriptor>();
		IncludeDescriptor includeDescriptor = new IncludeDescriptor();
		includeDescriptor.setFile(includeFilename);
		includeDescriptor.setContent(new StringBuffer("Before <!-- <INCLUDE file=\"test2\"> -->"
				+ Includer.CONTRACTED_FLAG + "<!-- </INCLUDE> --> After"));
		newDescriptors.add(includeDescriptor);

		List<IncludeDescriptor> newDescriptors2 = new ArrayList<IncludeDescriptor>();
		IncludeDescriptor includeDescriptor2 = new IncludeDescriptor();
		includeDescriptor2.setFile(includeFilename2);
		includeDescriptor2.setContent(new StringBuffer("Inner content"));
		newDescriptors2.add(includeDescriptor2);

		Status status = new Status();
		status.setHashes(new HashMap<String, List<IncludeDescriptor>>());
		status.getHashes().put("hash1", newDescriptors);
		status.setNewHash("hash1");
		statusMap.put(includeFilename, status);

		Status status2 = new Status();
		status2.setHashes(new HashMap<String, List<IncludeDescriptor>>());
		status2.getHashes().put("hash2", newDescriptors2);
		status2.setNewHash("hash2");
		statusMap.put(includeFilename2, status2);

		List<String> hashes = new ArrayList<String>();
		hashes.add("hash1");
		hashes.add("hash2");

		StringBuffer content = new StringBuffer("Test content before include <!-- <INCLUDE file=\"test\"> -->"
				+ Includer.CONTRACTED_FLAG + "<!-- </INCLUDE> --> Test content after include.");

		StringBuffer result = Includer.expand(content, new StatusMapIncludeProvider(statusMap, hashes));

		Assert.assertEquals(
				"Test content before include <!-- <INCLUDE file=\"test\"> -->Before <!-- <INCLUDE file=\"test2\"> -->Inner content<!-- </INCLUDE> --> After<!-- </INCLUDE> --> Test content after include.",
				result.toString());
	}

	@Test
	public void testNestedUnexpand()
	{
		String old = "Test content before include <!-- <INCLUDE file=\"test\"> --> Test content before inner include <!-- <INCLUDE file=\"test2\"> --> Include content <!-- </INCLUDE> --> Test content after inner include. <!-- </INCLUDE> --> Test content after include.";
		StringBuffer result = Includer.contract(old);
		Assert.assertEquals("Test content before include <!-- <INCLUDE file=\"test\"> -->" + Includer.CONTRACTED_FLAG
				+ "<!-- </INCLUDE> --> Test content after include.", result.toString());
	}

	@Test
	public void testSimpleExpand()
	{
		String includeFilename = "test";

		Map<String, Status> statusMap = new HashMap<String, Status>();

		List<IncludeDescriptor> newDescriptors = new ArrayList<IncludeDescriptor>();
		IncludeDescriptor includeDescriptor = new IncludeDescriptor();
		includeDescriptor.setFile(includeFilename);
		includeDescriptor.setContent(new StringBuffer("Inside include"));
		newDescriptors.add(includeDescriptor);

		Status status = new Status();
		status.setHashes(new HashMap<String, List<IncludeDescriptor>>());
		status.getHashes().put("hash1", newDescriptors);
		status.setNewHash("hash1");
		statusMap.put(includeFilename, status);

		List<String> hashes = new ArrayList<String>();
		hashes.add("hash1");

		StringBuffer content = new StringBuffer("Test content before include <!-- <INCLUDE file=\"test\"> -->"
				+ Includer.CONTRACTED_FLAG + "<!-- </INCLUDE> --> Test content after include.");

		StringBuffer result = Includer.expand(content, new StatusMapIncludeProvider(statusMap, hashes));

		Assert.assertEquals(
				"Test content before include <!-- <INCLUDE file=\"test\"> -->Inside include<!-- </INCLUDE> --> Test content after include.",
				result.toString());

	}

	@Test
	public void testSimpleUnexpand()
	{
		String old = "Test content before include <!-- <INCLUDE file=\"test\"> --> Include content <!-- </INCLUDE> --> Test content after include.";
		StringBuffer result = Includer.contract(old);
		Assert.assertEquals("Test content before include <!-- <INCLUDE file=\"test\"> -->" + Includer.CONTRACTED_FLAG
				+ "<!-- </INCLUDE> --> Test content after include.", result.toString());
	}

}
