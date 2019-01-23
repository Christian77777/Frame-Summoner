/**
 * 
 */
package com.Christian77777.Frame_Summoner.Limiters;

import java.util.ArrayList;
import com.Christian77777.Frame_Summoner.Database;
import com.Christian77777.Frame_Summoner.Database.RolePerm;
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
public class ListedLimiter implements Limiter
{
	private Database db;
	/**
	 * 
	 */
	public ListedLimiter(Database d)
	{
		db = d;
	}

	@Override
	public boolean check(CommandContext ctx)
	{
		return checkWhitelisted(db, ctx.getGuild(), ctx.getAuthor()) || AdminLimiter.checkServerOwner(ctx.getGuild(), ctx.getAuthor()) || AdminLimiter.checkAdmin(db, ctx.getGuild(), ctx.getAuthor());
	}
	
	@Override
	public void onFail(CommandContext ctx)
	{
		RequestBuffer.request(() -> {
			ctx.getChannel().sendMessage(":no_entry_sign: You are not authorized to use this command");
		});
	}
	
	/**
	 * Check if the User is allowed to use the bot in the server
	 * REQUIRED ASSUMPTION: Server must have a single mode, Blacklist or Whitelist. All entries for a single guild assumed
	 * to be of one type
	 * @param guild Guild where the rules are relevant
	 * @param user who needs to be checked
	 * @return if user has the proper roles required
	 */
	public static boolean checkWhitelisted(Database db, IGuild guild, IUser user)
	{
		ArrayList<Long> actualRoles = new ArrayList<Long>();
		//Insertion Sort, for sorting the user roles in ascending order. Using Insertion Sort because iteration is already required to extract the actual Longs
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
				if(!wasInserted)
					actualRoles.add(r.getLongID());
			}
		}
		//Get already sorted list of relevant roles for Guild
		ArrayList<RolePerm> rolePerms = db.getReleventRoles(guild.getLongID());
		if (!rolePerms.isEmpty())
		{
			boolean isBlacklist = rolePerms.get(0).getBlackVSWhite();//Hints Guild Listing Mode
			int lIndex = 0;
			int rIndex = 0;
			//Iterate through both sorted arrays for any match
			while (actualRoles.size() > lIndex && rolePerms.size() > rIndex)
			{
				int comparision = actualRoles.get(lIndex).compareTo(rolePerms.get(rIndex).getRoleID());
				if (comparision == 0)//Match found
				{
					if (isBlacklist)//Blacklist matched
						return false;
					else//Whitelist matched
						return true;
				}
				else if (comparision > 0)
					rIndex++;
				else
					lIndex++;
			}
			//No Match found, use Hint as database mode
			if (isBlacklist)//blacklist
				return true;
			else
				return false;
		}
		//No comparison found, must check if white or blacklist (BUG Admin about requiring two Database calls)
		if (db.checkGuildBlacklistMode(guild.getLongID()))
			return true;
		else
			return false;
	}
}
