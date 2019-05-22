package com.Christian77777.Frame_Summoner.Database;

public class DBLink
{
	private String link;
	private String title;
	private String description;
	private String nickname;

	protected DBLink(String l, String t, String d, String n)
	{
		link = l;
		title = t;
		description = d;
		nickname = n;
	}

	public String getLink()
	{
		return link;
	}

	public String getTitle()
	{
		return title;
	}

	public String getDescription()
	{
		return description;
	}

	public String getNickname()
	{
		return nickname;
	}
}
