package com.Christian77777.Frame_Summoner.Database;

public class DBChannel
{
	private long id;
	private int tier;
	private boolean annoucement;

	protected DBChannel(long id, int tier, boolean annoucement)
	{
		this.id = id;
		this.tier = tier;
		this.annoucement = annoucement;
	}

	public long getId()
	{
		return id;
	}

	public int getTier()
	{
		return tier;
	}

	public boolean isAnnoucement()
	{
		return annoucement;
	}
}
