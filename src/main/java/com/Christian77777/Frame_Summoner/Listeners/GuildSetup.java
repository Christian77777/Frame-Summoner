
package com.Christian77777.Frame_Summoner.Listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.DRI;
import com.Christian77777.Frame_Summoner.Database;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

public class GuildSetup implements IListener<GuildCreateEvent>
{

	private static Logger logger = LogManager.getLogger();
	private Database db;
	private Properties prop;
	private String prefix;

	public GuildSetup(Database db, Properties prop, String prefix)
	{
		this.db = db;
		this.prop = prop;
		this.prefix = prefix;
	}

	@Override
	public void handle(GuildCreateEvent event)
	{
		long id = event.getGuild().getLongID();
		if (checkWhitelist(String.valueOf(id)))
		{
			if (db.checkIfGuildExists(id))//Already Initialized
			{
				//Synchronize Channels
				ArrayList<Long> storedIDs = db.getListOfConfiguredChannels(id);
				ArrayList<IChannel> actualChannels = new ArrayList<IChannel>(event.getGuild().getChannels());
				ArrayList<Long> actualIDs = new ArrayList<Long>();
				for (int x = 0; x < actualChannels.size(); x++)
				{
					actualIDs.add(actualChannels.get(x).getLongID());
				}
				for (int x = 0; x < storedIDs.size(); x++)
				{
					if (!actualIDs.contains(storedIDs.get(x)))//Channel Deleted
					{
						db.removeChannel(storedIDs.get(x));
						logger.debug("Channel `{}` in Guild `{}` was Deleted", storedIDs.get(x), event.getGuild().getName());
					}
					/*
					if (!storedIDs.contains(actualIDs.get(x)))//Channel Added
					{
						db.addNewChannel(actualIDs.get(x), id, 0, false);
						logger.debug("Channel `{}` in Guild `{}` was Added", event.getClient().getChannelByID(storedIDs.get(x)).getName(),
								event.getGuild().getName());
					}*/
				}
				//Synchronize Admin Roles
				storedIDs = db.getListOfAdminRoles(id);
				ArrayList<IRole> actualRoles = new ArrayList<IRole>(event.getGuild().getRoles());
				actualIDs = new ArrayList<Long>();
				for (int x = 0; x < actualRoles.size(); x++)
				{
					actualIDs.add(actualRoles.get(x).getLongID());
				}
				for (int x = 0; x < storedIDs.size(); x++)
				{
					if (!actualIDs.contains(storedIDs.get(x)))//Role Deleted
					{
						db.removeAdminRole(storedIDs.get(x));
					}
					//Ignore new Admin Roles
				}
				//Synchronize UserRoles
				storedIDs = db.getListOfUserRoles(id);
				//Full Role List already declared
				for (int x = 0; x < storedIDs.size(); x++)
				{
					if (!actualIDs.contains(storedIDs.get(x)))//Role Deleted
					{
						db.removeUserRole(storedIDs.get(x));
					}
					//Ignore Roles Added
				}
			}
			else//New Server
			{
				//Initialize Server
				db.addNewDefaultGuild(id);
				logger.info("Added new Guild: " + event.getGuild().getName());
				//Initialize Channels
				ArrayList<IChannel> channels = new ArrayList<IChannel>(event.getGuild().getChannels());
				IChannel firstChannel = null;
				for (IChannel c : channels)
				{
					//db.addNewChannel(c.getLongID(), id, 0, false);
					if (c.getModifiedPermissions(event.getClient().getOurUser()).contains(Permissions.SEND_MESSAGES))
						firstChannel = c;
				}
				String rolecall = new String("");
				//Initialize Admin Roles
				String declaration;
				if (firstChannel != null)
				{
					for (IRole r : event.getGuild().getRoles())
					{
						if (r.getPermissions().contains(Permissions.MANAGE_SERVER))
						{
							db.addNewAdminRole(r.getLongID(), id);
							if (r.isMentionable())
							{
								rolecall += r.mention() + ", ";
							}
						}
					}
					rolecall = rolecall.substring(0, rolecall.length() - 2);
					declaration = "Hello " + rolecall
							+ "\nI am __Frame-Summoner__, a Discord bot that provides access to frames of video content.\nCurrently I acknowledge the `"
							+ prefix + "` command prefix and have a syntax like `fs!command <arguments>`\nI focus on video content for \""
							+ prop.getProperty("Content_Name") + "\" and am running on \"" + prop.getProperty("Server_Name")
							+ "\".\nOn First Join, I am to ignore all commands from users, and all commands in any channel. \nThe first step is for the Server Admins, AKA the users I pinged to do `fs!guide init` in PMs for instructions on unlocking functionality.";
					firstChannel.sendMessage(declaration);
				}
				else
				{
					HashSet<Long> admins = new HashSet<Long>();
					admins.add(event.getGuild().getOwner().getLongID());
					for (IRole r : event.getGuild().getRoles())
					{
						if (r.getPermissions().contains(Permissions.MANAGE_SERVER))
						{
							db.addNewAdminRole(r.getLongID(), id);
							for (IUser u : event.getGuild().getUsersByRole(r))
							{
								admins.add(u.getLongID());
							}
							rolecall += r.getName() + ", ";
						}
					}
					rolecall = rolecall.substring(0, rolecall.length() - 2);
					declaration = "Hello " + rolecall
							+ "\nI am __Frame-Summoner__, a Discord bot that provide access to frames of video content.\nCurrently I acknowledge the `"
							+ prefix + "` command prefix and have a syntax like `fs!command <arguments>`\nI focus on video content for \""
							+ prop.getProperty("Content_Name") + "\" and am running on \"" + prop.getProperty("Server_Name")
							+ "\".\nOn First Join, I am to ignore all commands from users, and all commands in any channel. \nThe first step is for the Server Admins, AKA the users I pinged to do `fs!guide init` for instructions on unlocking functionality.";
					for (Long l : admins)
					{
						event.getClient().getUserByID(l).getOrCreatePMChannel().sendMessage(declaration
								+ "\n**NOTE**: I could not send ANY messages in the " + event.getGuild().getName()
								+ " Server, make sure to give me permission to read and send messages + embeds in at least 1 channel before crying about bugs.");
					}
				}
			}
		}
		else
		{
			logger.warn("{} Server not whitelisted, disconnecting but not deleting.", event.getGuild().getName());
			event.getGuild().leave();
		}
	}

	private boolean checkWhitelist(String guildID)
	{
		File file = new File(DRI.dir + File.separator + "whitelist.txt");
		boolean result = false;
		try (FileReader stream = new FileReader(file); BufferedReader reader = new BufferedReader(stream);)
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (guildID.equals(line))
					result = true;
			}
		}
		catch (FileNotFoundException e)
		{
			result = true;
		}
		catch (IOException e)
		{
			logger.fatal("Could not read Guild Whitelist", e);
			System.exit(20);
		}
		return result;
	}
}
