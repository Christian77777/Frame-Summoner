package Listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelCreateEvent;

public class NewChannel implements IListener<ChannelCreateEvent>
{
	private static Logger logger = LogManager.getLogger();
	private Database db;
	
	public NewChannel(Database d)
	{
		db = d;
	}


	@Override
	public void handle(ChannelCreateEvent event)
	{
		logger.info("New Channel: `{}` Created in Guild `{}`",event.getChannel().getName(), event.getGuild().getName());
		db.addNewChannel(event.getChannel().getLongID(), event.getGuild().getLongID(), 0, false);
	}

}
