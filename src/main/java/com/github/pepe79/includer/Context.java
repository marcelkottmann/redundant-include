package com.github.pepe79.includer;
import java.util.HashMap;
import java.util.Map;

public class Context
{

	private Map<String, IncludeScanResult> fileToResult = new HashMap<String, IncludeScanResult>();

	public Map<String, IncludeScanResult> getFileToResult()
	{
		return fileToResult;
	}

	public void setFileToResult(Map<String, IncludeScanResult> fileToResult)
	{
		this.fileToResult = fileToResult;
	}

}
