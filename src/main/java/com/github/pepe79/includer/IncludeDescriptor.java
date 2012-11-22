package com.github.pepe79.includer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IncludeDescriptor
{
	private int start;

	private int end;

	private String file;

	private IncludeDescriptor parent;

	private StringBuffer content;

	private List<IncludeDescriptor> children = new ArrayList<IncludeDescriptor>();

	private File origin;

	public IncludeDescriptor getParent()
	{
		return parent;
	}

	public void addChild(IncludeDescriptor descriptor)
	{
		getChildren().add(descriptor);
	}

	public void setParent(IncludeDescriptor parent)
	{
		this.parent = parent;
	}

	public List<IncludeDescriptor> getChildren()
	{
		return children;
	}

	public void setChildren(List<IncludeDescriptor> children)
	{
		this.children = children;
	}

	public int getStart()
	{
		return start;
	}

	public void setStart(int start)
	{
		this.start = start;
	}

	public StringBuffer getContent()
	{
		return content;
	}

	public void setContent(StringBuffer content)
	{
		this.content = content;
	}

	public int getEnd()
	{
		return end;
	}

	public void setEnd(int end)
	{
		this.end = end;
	}

	public String getFile()
	{
		return file;
	}

	public void setFile(String file)
	{
		this.file = file;
	}

	public File getOrigin()
	{
		return origin;
	}

	public void setOrigin(File origin)
	{
		this.origin = origin;
	}

}
