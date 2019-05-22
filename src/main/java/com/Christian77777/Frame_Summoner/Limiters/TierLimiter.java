/**
 * 
 */

package com.Christian77777.Frame_Summoner.Limiters;

import com.Christian77777.Frame_Summoner.Database.Database;
import com.darichey.discord.CommandContext;
import com.darichey.discord.limiter.Limiter;
import sx.blah.discord.util.RequestBuffer;

/**
 * @author Christian
 *
 */
public class TierLimiter implements Limiter
{
	private Database db;
	private int tier;
	private boolean allowPrivate;
	private int prefixSize;

	public TierLimiter(Database db, int tier, boolean allowPrivate, String prefix)
	{
		this.db = db;
		this.tier = tier;
		this.allowPrivate = allowPrivate;
		prefixSize = prefix.length();
	}

	@Override
	public boolean check(CommandContext ctx)
	{
		if (ctx.getChannel().isPrivate())
			return allowPrivate;
		return db.checkChannelPermission(ctx.getChannel().getLongID(), tier);
	}

	@Override
	public void onFail(CommandContext ctx)
	{
		String message = ctx.getMessage().getFormattedContent();
		int index = message.indexOf(' ');
		if (index == -1)
			index = message.length();
		String command = message.substring(prefixSize, index);
		if (ctx.getChannel().isPrivate())
			RequestBuffer.request(() -> {
				ctx.getAuthor().getOrCreatePMChannel().sendMessage(":no_entry: The `" + command + "` command can not be used in PMs");
			});

		else
			RequestBuffer.request(() -> {
				ctx.getAuthor().getOrCreatePMChannel().sendMessage(":no_entry: The `" + command + "` command can not be used in the `"
						+ ctx.getGuild().getName() + "` Server for this Channel: `" + ctx.getChannel().getName() + "`");
			});
	}

}
