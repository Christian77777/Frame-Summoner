package com.Christian77777.Frame_Summoner;

import java.io.Serializable;
import java.util.ArrayList;
import javax.script.SimpleBindings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sx.blah.discord.handle.obj.IRole;

public class GuildData implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 4909223641475712564L;
	private static Logger logger = LogManager.getLogger();
	private long id;
	private String name;
	private int requestLimit;
	private ArrayList<IRole> admins = new ArrayList<IRole>();
	private ArrayList<IRole> blacklist = new ArrayList<IRole>();
	private SimpleBindings channelPerms = new SimpleBindings();
	private boolean disabled = false;
	private boolean serverOnline = true;

	public GuildData(long id, String name, int requestLimit, boolean disabled)
	{
		this.id = id;
		this.name = name;
		this.requestLimit = requestLimit;
		this.disabled = disabled;
	}

	public long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}
	
	public int getRequestLimit()
	{
		return requestLimit;
	}
	
	public boolean isDisabled()
	{
		return disabled;
	}
	
	public int getChannelTier(long id)
	{
		return (int)channelPerms.get(Long.toString(id));
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	public void setRequestLimit(int requestLimit)
	{
		if(requestLimit <= 1000)
		{
			this.requestLimit = requestLimit;
		}
		else if(requestLimit > 1)
		{
			this.requestLimit = 1000;
		}
		else
			logger.warn("Request Limit for Guild: {} set to negative number: {}",name,requestLimit	);
	}
	
	public void setDisabled(boolean disabled)
	{
		this.disabled = disabled;
	}
	
	public void editChannel(long id, Integer tier)
	{
		channelPerms.put(String.valueOf(id), tier);
	}
	
	public void deleteChannel(long id)
	{
		channelPerms.remove(Long.toString(id));
	}
	
	public int getConfiguredChannelsNumber()
	{
		return channelPerms.size();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof GuildData)
			return ((GuildData)o).id == id ? true : false;
		else
			throw new IllegalArgumentException();
	}
}
