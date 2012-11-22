package com.github.pepe79.includer;
import java.util.ArrayList;
import java.util.List;

public class IncludeScanResult
{
	private String file;

	private List<IncludeDescriptor> descriptors = new ArrayList<IncludeDescriptor>();

	public String getFile()
	{
		return file;
	}

	public void setFile(String file)
	{
		this.file = file;
	}

	public List<IncludeDescriptor> getDescriptors()
	{
		return descriptors;
	}

	public void setDescriptors(List<IncludeDescriptor> descriptors)
	{
		this.descriptors = descriptors;
	}

	public Status getStatus()
	{
		Integer hash = null;
		for (IncludeDescriptor d : descriptors)
		{
			if (hash == null)
			{
				hash = d.getContent().toString().hashCode();
			}
			else if (!hash.equals(d.getContent().toString().hashCode()))
			{
				return Status.DIFFER;
			}
		}
		return Status.OK;
	}

}
