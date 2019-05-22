/**
 * 
 */
package com.Christian77777.Frame_Summoner.Database;

/**
 * @author Christian
 *
 */
public class DBNormalUser
{
	private long id;
	private int totalUsed;
	private int usedToday;
	private boolean banned;
	private boolean vip;

	public DBNormalUser(long id)
	{
		this.id = id;
		totalUsed = 0;
		usedToday = 0;
		banned = false;
		vip = false;
	}

	protected DBNormalUser(long id, int totalUsed, int usedToday, boolean banned, boolean vip)
	{
		this.id = id;
		this.totalUsed = totalUsed;
		this.usedToday = usedToday;
		this.banned = banned;
		this.vip = vip;
	}

	public long getId()
	{
		return id;
	}

	public int getTotalUsed()
	{
		return totalUsed;
	}

	public int getUsedToday()
	{
		return usedToday;
	}

	public boolean isBanned()
	{
		return banned;
	}

	public boolean isVip()
	{
		return vip;
	}
}
