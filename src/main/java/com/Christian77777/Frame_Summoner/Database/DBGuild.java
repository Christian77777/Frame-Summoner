package com.Christian77777.Frame_Summoner.Database;

public class DBGuild
{
	private long guildID;
	private int requestLimit;
	private int usedToday;
	private boolean enabled;
	private boolean isBlacklistMode;

	protected DBGuild(long g, int r, int u, boolean e, boolean b)
	{
		guildID = g;
		requestLimit = r;
		usedToday = u;
		enabled = e;
		isBlacklistMode = b;
	}

	public long getGuildID()
	{
		return guildID;
	}

	public int getRequestLimit()
	{
		return requestLimit;
	}

	public int getUsedToday()
	{
		return usedToday;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public boolean isBlacklistMode()
	{
		return isBlacklistMode;
	}
}
