package com.github.pepe79.includer;

import java.util.List;
import java.util.Map;


public class Status
{
	private StatusCode statusCode;

	private String newHash;

	private String oldHash;

	private Map<String, List<IncludeDescriptor>> hashes;

	private String file;

	public String getFile()
	{
		return file;
	}

	public Map<String, List<IncludeDescriptor>> getHashes()
	{
		return hashes;
	}

	public List<IncludeDescriptor> getNewDescriptors()
	{
		return newHash == null ? null : getHashes().get(newHash);
	}

	public String getNewHash()
	{
		return newHash;
	}

	public List<IncludeDescriptor> getOldDescriptors()
	{
		return oldHash == null ? null : getHashes().get(oldHash);
	}

	public String getOldHash()
	{
		return oldHash;
	}

	public StatusCode getStatusCode()
	{
		return statusCode;
	}

	public void setFile(String file)
	{
		this.file = file;
	}

	public void setHashes(Map<String, List<IncludeDescriptor>> hashes)
	{
		this.hashes = hashes;
	}

	public void setNewHash(String newHash)
	{
		this.newHash = newHash;
	}

	public void setOldHash(String oldHash)
	{
		this.oldHash = oldHash;
	}

	public void setStatusCode(StatusCode statusCode)
	{
		this.statusCode = statusCode;
	}

}
