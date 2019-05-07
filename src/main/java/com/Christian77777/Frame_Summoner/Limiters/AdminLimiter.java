/**
 * 
 */

package com.Christian77777.Frame_Summoner.Limiters;

import java.util.ArrayList;
import java.util.List;
import com.Christian77777.Frame_Summoner.Database;
import com.darichey.discord.CommandContext;
import com.darichey.discord.limiter.Limiter;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

/**
 * @author Christian
 *
 */
public class AdminLimiter implements Limiter
{

	private Database db;
	private boolean allowPrivate;
	private boolean failPrivately;

	/**
	 * Create Limiter that checks if User has admin privileges in the specified server
	 * @param d The Database that stores the information
	 * @param allowPrivate If Admin commands sent by private messages should pass
	 * @param failPrivately If users should be informed that they are not authorized in Private messages instead
	 */
	public AdminLimiter(Database d, boolean allowPrivate, boolean failPrivately)
	{
		db = d;
		this.allowPrivate = allowPrivate;
		this.failPrivately = failPrivately;
	}

	@Override
	public boolean check(CommandContext ctx)
	{
		if (ctx.getChannel().isPrivate())
			return allowPrivate;
		return checkServerOwner(ctx.getGuild(), ctx.getAuthor()) || checkAdmin(db, ctx.getGuild(), ctx.getAuthor());
	}

	@Override
	public void onFail(CommandContext ctx)
	{
		if (failPrivately)
		{
			RequestBuffer.request(() -> {
				ctx.getAuthor().getOrCreatePMChannel()
						.sendMessage(":no_entry: You must be an :a: Admin of the " + ctx.getGuild().getName() + " Server to use this Command here");
			});
		}
		else
		{
			RequestBuffer.request(() -> {
				ctx.getChannel()
						.sendMessage(":no_entry: You must be an :a: Admin of the " + ctx.getGuild().getName() + " Server to use this Command here");
			});
		}
	}

	/**
	 * Check if User has the required admin role
	 * @param guild Guild the message was required
	 * @param user User that sent the message
	 * @return if user had the required roles
	 */
	public static boolean checkAdmin(Database db, IGuild guild, IUser user)
	{
		ArrayList<Long> actualRoles = new ArrayList<Long>();
		//Insertion Sort, for sorting the admin roles in ascending order. Using Insertion Sort because iteration is already required to extract the actual Longs
		for (IRole r : user.getRolesForGuild(guild))
		{
			if (actualRoles.isEmpty())
				actualRoles.add(r.getLongID());
			else
			{
				boolean wasInserted = false;
				for (int x = 0; x < actualRoles.size(); x++)
				{
					if (actualRoles.get(x).longValue() >= r.getLongID())
					{
						actualRoles.add(x, r.getLongID());
						wasInserted = true;
						break;
					}
				}
				if (!wasInserted)
					actualRoles.add(r.getLongID());
			}
		}
		//Get already sorted list of relevant roles for Guild
		ArrayList<Long> requiredRoles = db.getListOfAdminRoles(guild.getLongID());
		if (!requiredRoles.isEmpty())
		{
			int lIndex = 0;
			int rIndex = 0;
			//Iterate through both sorted arrays for any match
			while (actualRoles.size() > lIndex && requiredRoles.size() > rIndex)
			{
				int comparision = actualRoles.get(lIndex).compareTo(requiredRoles.get(rIndex));
				if (comparision == 0)//Match found
					return true;
				else if (comparision > 0)
					rIndex++;
				else
					lIndex++;
			}
			//No Matches found
		}
		return false;
	}

	public static boolean checkServerOwner(IGuild guild, IUser user)
	{
		return guild.getOwnerLongID() == user.getLongID() ? true : false;
	}
}
