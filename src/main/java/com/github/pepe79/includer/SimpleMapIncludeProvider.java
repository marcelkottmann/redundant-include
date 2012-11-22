package com.github.pepe79.includer;

import java.util.Map;


public class SimpleMapIncludeProvider implements IncludeProvider
{

	private Map<String, StringBuffer> nameToContent;

	public SimpleMapIncludeProvider(Map<String, StringBuffer> nameToContent)
	{
		this.nameToContent = nameToContent;
	}

	@Override
	public StringBuffer getContent(String includeFilename)
	{
		return nameToContent.get(includeFilename);
	}

}
