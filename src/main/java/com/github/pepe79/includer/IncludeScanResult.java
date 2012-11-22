package com.github.pepe79.includer;

import java.util.ArrayList;
import java.util.List;


public class IncludeScanResult
{
	private List<IncludeDescriptor> descriptors = new ArrayList<IncludeDescriptor>();

	public List<IncludeDescriptor> getDescriptors()
	{
		return descriptors;
	}

	public void setDescriptors(List<IncludeDescriptor> descriptors)
	{
		this.descriptors = descriptors;
	}

}
