package Listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;

public class ChannelDeleted implements IListener<ChannelDeleteEvent>
{
	private static Logger logger = LogManager.getLogger();
	private Database db;
	
	public ChannelDeleted(Database d)
	{
		db = d;
	}


	@Override
	public void handle(ChannelDeleteEvent event)
	{
		logger.info("New Channel: `{}` Created in Guild `{}`",event.getChannel().getName(), event.getGuild().getName());
		db.removeChannel(event.getChannel().getLongID());
	}

}
