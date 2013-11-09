package com.github.pepe79.includer;

import java.util.List;
import java.util.Map;


public class StatusMapIncludeProvider implements IncludeProvider
{

	private IncludeDescriptor selectDescriptor(Map<String, Status> statusMap, String file,
		List<String> overrideHashes)
	{
		if (overrideHashes != null)
		{
			for (String hash : overrideHashes)
			{
				List<IncludeDescriptor> descriptors = statusMap.get(file).getHashes().get(hash);
				if (descriptors != null)
				{
					return descriptors.get(0);
				}
			}
		}

		return statusMap.get(file).getNewDescriptors().get(0);
	}

	private Map<String, Status> statusMap;

	private List<String> overrideHashes;

	public StatusMapIncludeProvider(Map<String, Status> statusMap, List<String> overrideHashes)
	{
		this.statusMap = statusMap;
		this.overrideHashes = overrideHashes;
	}

	@Override
	public StringBuffer getContent(String includeFilename)
	{
		return selectDescriptor(statusMap, includeFilename, overrideHashes).getContent();
	}
}
