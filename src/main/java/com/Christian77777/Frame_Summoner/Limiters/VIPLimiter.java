/**
 * 
 */
package com.Christian77777.Frame_Summoner.Limiters;

import java.util.Properties;
import com.Christian77777.Frame_Summoner.Database;
import com.darichey.discord.CommandContext;
import com.darichey.discord.limiter.Limiter;
import sx.blah.discord.util.RequestBuffer;

/**
 * @author Christian
 *
 */
public class VIPLimiter implements Limiter
{
	private Database db;
	private Properties prop;
	
	public VIPLimiter(Database db, Properties p)
	{
		this.db = db;
		prop = p;
	}

	@Override
	public boolean check(CommandContext ctx)
	{
		return checkOperator(prop, ctx.getAuthor().getLongID()) || db.isUserVIP(ctx.getAuthor().getLongID());
	}

	@Override
	public void onFail(CommandContext ctx)
	{
		RequestBuffer.request(() -> {
			ctx.getChannel().sendMessage(":no_entry: You must be a :large_orange_diamond: VIP to use this Command");
		});
	}
	
	public static boolean checkOperator(Properties prop, long userID)
	{
		try
		{
			return Long.parseLong(prop.getProperty("Bot_Manager")) == userID ? true : false;
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Property Value not a parsable Long", e);
		}
	}
}
