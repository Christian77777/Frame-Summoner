
package Listeners;

import java.util.ArrayList;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.Permissions;

public class GuildSetup implements IListener<GuildCreateEvent>
{
	private static Logger logger = LogManager.getLogger();
	private Database db;
	private Properties prop;

	public GuildSetup(Database db, Properties prop)
	{
		this.db = db;
		this.prop = prop;
	}

	@Override
	public void handle(GuildCreateEvent event)
	{
		long id = event.getGuild().getLongID();
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
				if (!storedIDs.contains(actualIDs.get(x)))//Channel Added
				{
					db.addNewChannel(actualIDs.get(x), id, 0, false);
					logger.debug("Channel `{}` in Guild `{}` was Added", event.getClient().getChannelByID(storedIDs.get(x)).getName(),
							event.getGuild().getName());
				}
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
			db.addNewGuild(id);
			logger.info("Added new Guild: " + event.getGuild().getName());
			//Initialize Channels
			ArrayList<IChannel> channels = new ArrayList<IChannel>(event.getGuild().getChannels());
			IChannel firstChannel = null;
			for (IChannel c : channels)
			{
				db.addNewChannel(c.getLongID(), id, 0, false);
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
				declaration = "Sup " + rolecall
						+ "\nI am __Frame-Summoner__, a Discord bot that provides access to frames of video content.\nCurrently, I focus on video content for \""
						+ prop.getProperty("Content_Name") + "\" and am running on \"" + prop.getProperty("Server_Name")
						+ "\".\nOn First Join, I am to ignore all commands from users, and only respond to Server Managers, AKA the peeps I pinged.\nSo, for pete's sake, make sure to do `fs!help init` for instructions on unlocking functionality for the plebs";
				firstChannel.sendMessage(declaration);
			}
			else
			{
				for (IRole r : event.getGuild().getRoles())
				{
					if (r.getPermissions().contains(Permissions.MANAGE_SERVER))
					{
						db.addNewAdminRole(r.getLongID(), id);
						rolecall += r.getName() + ", ";
					}
				}
				rolecall = rolecall.substring(0, rolecall.length() - 2);
				declaration = "Sup " + rolecall
						+ "\nI am __Frame-Summoner__, a Discord bot that provide access to frames of video content.\nCurrently, I focus on video content for \""
						+ prop.getProperty("Content_Name") + "\" and am running on \"" + prop.getProperty("Server_Name")
						+ "\".\nOn First Join, I am to ignore all commands from users, and only respond to Server Managers, AKA the peeps I pinged.\nSo, for pete's sake, make sure to do `fs!help init` for instructions on unlocking functionality for the plebs";
				event.getGuild().getOwner().getOrCreatePMChannel().sendMessage(declaration + "\n**NOTE**: I could not send ANY messages in the "
						+ event.getGuild().getName() + " Server, make sure that every channel that I can read, I can also respond in.");
			}
		}
	}
}
