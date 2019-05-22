/**
 * 
 */

package com.Christian77777.Frame_Summoner.Limiters;

import java.util.ArrayList;
import com.Christian77777.Frame_Summoner.Database.DBRolePerm;
import com.Christian77777.Frame_Summoner.Database.Database;
import com.darichey.discord.CommandContext;
import com.darichey.discord.limiter.Limiter;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.RequestBuffer;

/**
 * @author Christian
 *
 */
public class ListedLimiter implements Limiter
{
	private Database db;
	private boolean failPrivately;

	/**
	 * 
	 */
	public ListedLimiter(Database d, boolean failPrivately)
	{
		db = d;
		this.failPrivately = failPrivately;
	}

	@Override
	public boolean check(CommandContext ctx)
	{
		Standing standing = null;
		ArrayList<Long> actualRoles = new ArrayList<Long>();
		//Insertion Sort, for sorting the user roles in ascending order. Using Insertion Sort because iteration is already required to extract the actual Longs
		for (IRole r : ctx.getAuthor().getRolesForGuild(ctx.getGuild()))
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
		ArrayList<DBRolePerm> rolePerms = db.getReleventRoles(ctx.getGuild().getLongID());
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
					standing = new Standing(isBlacklist, actualRoles.get(lIndex), !isBlacklist);
					break;
				}
				else if (comparision > 0)
					rIndex++;
				else
					lIndex++;
			}
			//No Match found, use Hint as database mode
			if (standing == null)
			{
				standing = new Standing(isBlacklist, null, isBlacklist);
			}
		}
		//No comparison found, must check if white or blacklist (Complain to Admin about requiring a second Database call)
		else
		{
			boolean isBMode = db.checkGuildBlacklistMode(ctx.getGuild().getLongID());
			standing = new Standing(isBMode, null, isBMode);
		}
		if (standing.pass)
			return true;
		else
		{
			IChannel c;
			String message;
			if (standing.isBlacklist)
				message = ":no_entry_sign: Server Admins of `" + ctx.getGuild().getName()
						+ "` have :black_large_square: Blacklisted you from making requests to Frame-Summoner in their server, because you have the `"
						+ ctx.getGuild().getRoleByID(standing.roleName).getName() + "` Role";
			else
				message = ":no_entry_sign: Server Admins of `" + ctx.getGuild().getName()
						+ "` have not :white_large_square: Whitelisted you to make requests to Frame-Summoner, in their server";
			if (failPrivately)
				c = ctx.getAuthor().getOrCreatePMChannel();
			else
				c = ctx.getChannel();
			RequestBuffer.request(() -> {
				c.sendMessage(message);
			});
			return false;
		}
	}

	public class Standing
	{
		private boolean isBlacklist;
		private Long roleName;
		private boolean pass;

		public Standing(boolean isBlacklist, Long roleName, boolean pass)
		{
			this.isBlacklist = isBlacklist;
			this.roleName = roleName;
			this.pass = pass;
		}

		public boolean isBlacklist()
		{
			return isBlacklist;
		}

		public Long getRoleName()
		{
			return roleName;
		}

		public boolean isPass()
		{
			return pass;
		}

	}
}
