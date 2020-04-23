package com.Christian77777.Frame_Summoner.Database;

public class DBRolePerm
{
	private long roleID;
	private boolean blackVSwhite;

	protected DBRolePerm(long roleID, boolean blackVSwhite)
	{
		this.roleID = roleID;
		this.blackVSwhite = blackVSwhite;
	}

	public long getRoleID()
	{
		return roleID;
	}

	public boolean getBlackVSWhite()
	{
		return blackVSwhite;
	}
}
