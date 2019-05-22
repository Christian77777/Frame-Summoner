package com.Christian77777.Frame_Summoner.Database;

public class DBVideo
{
	private String filename;
	private String nickname;
	private long length;
	private long offset;
	private String fps;
	private boolean restricted;
	private boolean usable;
	private boolean linked;

	protected DBVideo(String f, String n, long l, long o, String s, boolean r, boolean u, boolean li)
	{
		filename = f;
		nickname = n;
		length = l;
		offset = o;
		fps = s;
		restricted = r;
		usable = u;
		linked = li;
	}

	public String getFilename()
	{
		return filename;
	}

	public String getNickname()
	{
		return nickname;
	}

	public long getLength()
	{
		return length;
	}

	public long getOffset()
	{
		return offset;
	}

	public String getFps()
	{
		return fps;
	}

	public boolean isRestricted()
	{
		return restricted;
	}

	public boolean isUsable()
	{
		return usable;
	}

	public boolean isLinked()
	{
		return linked;
	}
	
	public String toString()
	{
		return filename;
	}
}
