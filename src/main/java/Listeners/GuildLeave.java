/**
 * 
 */
package Listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;

/**
 * @author Christian
 *
 */
public class GuildLeave implements IListener<GuildLeaveEvent>
{
	private static Logger logger = LogManager.getLogger();
	private Database db;
	
	public GuildLeave(Database d)
	{
		db = d;
	}
	
	@Override
	public void handle(GuildLeaveEvent event)
	{
		logger.info("This Bot has been ejected from {} Server",event.getGuild());
		db.wipeGuild(event.getGuild().getLongID());
	}

}
