package com.Christian77777.Frame_Summoner.Database;

public class DBRecord
{
	private long date;
	private boolean elevated;
	private long userID;
	private long guildID;
	private long channelID;
	private long messageID;
	private String filename;
	private String timestamp;
	private Integer frameCount;
	private String url;
	
	protected DBRecord(long date, boolean vip, long uID, long gID, long cID, long mID, String filename, String time, Integer frame, String url)
	{
		this.date = date;
		elevated = vip;
		userID = uID;
		guildID = gID;
		channelID = cID;
		messageID = mID;
		this.filename = filename;
		timestamp = time;
		frameCount = frame;
		this.url = url;
	}

	public long getDate()
	{
		return date;
	}

	public boolean isElevated()
	{
		return elevated;
	}

	public long getUserID()
	{
		return userID;
	}

	public long getGuildID()
	{
		return guildID;
	}

	public long getChannelID()
	{
		return channelID;
	}

	public long getMessageID()
	{
		return messageID;
	}

	public String getFilename()
	{
		return filename;
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public Integer getFrameCount()
	{
		return frameCount;
	}

	public String getUrl()
	{
		return url;
	}
}
