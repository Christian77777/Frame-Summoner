
package com.Christian77777.Frame_Summoner;

import java.awt.Color;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database.DBChannel;
import com.Christian77777.Frame_Summoner.Database.DBGuild;
import com.Christian77777.Frame_Summoner.Database.DBVideo;
import com.Christian77777.Frame_Summoner.Limiters.AdminLimiter;
import com.Christian77777.Frame_Summoner.Limiters.ListedLimiter;
import com.Christian77777.Frame_Summoner.Limiters.TierLimiter;
import com.Christian77777.Frame_Summoner.Limiters.VIPLimiter;
import com.darichey.discord.Command;
import com.darichey.discord.Command.Builder;
import com.darichey.discord.CommandContext;
import com.darichey.discord.CommandRegistry;
import com.darichey.discord.limiter.Limiter;
import com.darichey.discord.limiter.UserLimiter;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

/**
 * 
 * @author Christian77777
 * Channel Tiers
 * 0 = No Response
 * 1 = info commands maybe
 * 2 = extraction commands
 * 3 =
 * a = announcements
 */
public class UserActivity
{

	private static Logger logger = LogManager.getLogger();
	private DRI dri;
	private Database db;
	private Properties prop;
	private Extractor extractor;
	public static DateTimeFormatter milliDateFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	public static DateTimeFormatter secondDateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
	private CommandRegistry registry;
	private ArrayList<CommandWrapper> commands = new ArrayList<CommandWrapper>();
	private final String prefix;
	public static ReactionEmoji confirm = ReactionEmoji.of(new String(Character.toChars(9989)));
	public static ReactionEmoji deny = ReactionEmoji.of(new String(Character.toChars(10062)));

	public UserActivity(DRI dri, IDiscordClient c, Database d, Properties p, String prefix)
	{
		this.dri = dri;
		db = d;
		prop = p;
		this.prefix = prefix;
		extractor = new Extractor(c, d, p);
		registry = new CommandRegistry(prefix);
		//Limiters
		UserLimiter operatorL = new UserLimiter(Long.valueOf(prop.getProperty("Bot_Manager")))
		{

			@Override
			public void onFail(CommandContext ctx)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be a the Frame-Summoner Operator to use this Command");
				});
			}
		};
		TierLimiter tier1L = new TierLimiter(db, 1, false, prefix);
		TierLimiter tier2L = new TierLimiter(db, 2, false, prefix);
		TierLimiter tier3L = new TierLimiter(db, 3, false, prefix);
		VIPLimiter vipL = new VIPLimiter(d, p, false);
		AdminLimiter adminL = new AdminLimiter(db, true, false);
		AdminLimiter privateAdminL = new AdminLimiter(db, true, true);
		ListedLimiter listedL = new ListedLimiter(db, true);
		//TODO Bookmark
		//Debug Commands
		addDebugCommand();
		addUpdateDBCommand();
		//Operator Commands
		addDirCommand(3, new Limiter[]
		{ operatorL });
		addNewVIPCommand(3, new Limiter[]
		{ operatorL });
		addRemoveVIPCommand(3, new Limiter[]
		{ operatorL });
		//VIP Commands
		addFullVerificationCommand(2, new Limiter[]
		{ tier3L, vipL });
		addExecuteVerification(2, new Limiter[]
		{ tier3L, vipL });
		addVerifyCommand(2, new Limiter[]
		{ tier3L, vipL });
		addRestrictCommand(2, new Limiter[]
		{ tier3L, vipL });
		addSetUserUsage(2, new Limiter[]
		{ tier3L, vipL });
		addSetServerUsage(2, new Limiter[]
		{ tier3L, vipL });
		//Admin Commands
		addNewAdminRoleCommand(1, new Limiter[]
		{ tier3L, adminL });
		addRemoveAdminRoleCommand(1, new Limiter[]
		{ tier3L, adminL });
		addNewListedRoleCommand(1, new Limiter[]
		{ tier3L, adminL });
		addRemoveListedRoleCommand(1, new Limiter[]
		{ tier3L, adminL });
		addChangeListingModeCommand(1, new Limiter[]
		{ tier3L, adminL });
		addChangeChannelTierCommand(1, new Limiter[]
		{ privateAdminL });//Allowed in PMs, Admin Check must be done in program if Private
		addChangeChannelAnnoucementCommand(1, new Limiter[]
		{ tier3L, adminL });
		//Extraction Commands
		addFrameCommand(0, new Limiter[]
		{ tier2L, listedL });
		//Query Commands
		addListCommand(0, new Limiter[]
		{ tier2L, listedL });
		//Generic Commands
		addInfoCommand(0, new Limiter[]
		{ tier1L, listedL });
		addHelpCommand(0, new Limiter[]
		{ tier1L, listedL });
	}

	private void addDebugCommand()
	{
		Builder b = Command.builder();
		b.limiter(new UserLimiter(163810952905490432L));
		Command debug = b.onCalled(ctx -> {
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage("No");
			});
		}).build();
		registry.register(debug, "debug");
	}

	private void addUpdateDBCommand()
	{
		Builder b = Command.builder();
		b.limiter(new UserLimiter(163810952905490432L));
		Command updateDB = b.onCalled(ctx -> {
			String command = ctx.getMessage().getFormattedContent().substring(prefix.length() + 8, ctx.getMessage().getFormattedContent().length());
			logger.debug("SQL Command: {}", command);
			boolean result = db.executeDebugUpdate(command);
			String content;
			if (result)
				content = "Command Executed Successfully";
			else
				content = "SQL Failed, check Console for Errors";
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(content);
			});
		}).build();
		registry.register(updateDB, "updateDB");
	}

	/**
	 * Requests the Extractor to extract a frame
	 */
	private void addFrameCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "frame";
		String[] aliases = new String[]
		{ "extract" };
		String usage = prefix + name + " [Filename] [Timecode] <-f Frame Number> <-o>";
		String description = "Extracts frame from video and uploads to Discord";
		String detailedDescription = "Extracts a Frame from specified video at the timecode and uploads it to Discord if authorized.\nUsage: `"
				+ usage
				+ "`\n*[Filename]* = Name of Video file validated in Database, use fs!list to see avaliable files\n*[Timecode]* = Timecode to extract frame from in video, in this format `##:##:##<.###>`, milliseconds optional\n*<-f Frame Number>* = Increment the frame selected by the timecode, easier way to be precise.\n*<-o>* = Ignore video link offset. __Advanced__\n - Several Restrictions on command to prevent abuse";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean useOffset = true;
			String filename = null;
			String timecode = null;
			Integer frameCount = null;
			//TODO Deal with repeated flags
			try
			{
				while (!args.isEmpty())
				{
					String arg = assembleString(args);
					if (arg.startsWith("-"))
					{
						switch (arg)
						{
							case "-o":
								useOffset = false;
								break;
							case "-f":
								try
								{
									frameCount = Integer.valueOf(assembleString(args));
									if (frameCount <= 0 || frameCount > 1000)
									{
										final int frameCount2 = frameCount;
										RequestBuffer.request(() -> {
											return ctx.getChannel()
													.sendMessage("Invalid Frame Count value: `" + frameCount2 + "`! Valid range between 1 and 1000");
										});
										return;
									}
								}
								catch (NumberFormatException e)
								{
									final int frameCount2 = frameCount;
									RequestBuffer.request(() -> {
										return ctx.getChannel().sendMessage("Frame Count value: " + frameCount2 + "\nnot a number!");
									});
									return;
								}
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help frame for proper usage");
								});
								return;
						}
					}
					else if (filename == null)
					{
						filename = arg;
					}
					else if (timecode == null)
					{
						if (!checkOffset(arg))
						{
							logger.error("Offset Format Invalid: {}", timecode);
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Offset Format invalid, must be in ##:##:##.### or ##:##:##");
							});
							return;
						}
						timecode = arg;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Duplicate Filename or Timecode! Do " + prefix + "help frame for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help frame for proper usage");
				});
				return;
			}
			if (filename != null && timecode != null)//Required Parameters
			{
				extractor.requestFrame(ctx, filename, timecode, frameCount, useOffset);
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help frame for proper usage");
				});
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addRefreshVideosCommand(int commandTier, Limiter[] limits)
	{

	}

	private void addFullVerificationCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "fullVerify";
		String[] aliases = new String[]
		{ "fullVerification", "resetVideos" };
		String usage = prefix + name;
		String description = ":large_orange_diamond: __VIP__: Reset Database to refind all videos in Video Directory.";
		String detailedDescription = "Clears all saved records of video, and one by one, finds all eligible videos that can be extracted from in the directory, and adds them to the database.\nUsage: `"
				+ usage + "`\n:large_orange_diamond: __VIP only__\n:warning: Requires Confirmation since data will be deleted.";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (args.size() > 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do " + prefix + "help frame for proper usage");
				});
				return;
			}
			if (confirmAction(ctx, "Would you like to temporarly Disable the Extractor and begin Verification?"))
				extractor.fullVerification(ctx.getChannel());
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addExecuteVerification(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "startVerify";
		String[] aliases = new String[]
		{ "executeVerify", "startVerification", "executeVerification" };
		String usage = prefix + name;
		String description = ":large_orange_diamond: __VIP__: Executes the current queue of videos to verify.";
		String detailedDescription = "One by one, verifies all video files in queue if frames can be extracted, and adds successfully tested video to the list.\nUsage: `"
				+ usage
				+ "`\n:large_orange_diamond: __VIP only__\n:warning: Requires Confirmation since some functions are disabled while process runs.\n - All Extractions are halted while verifications take place";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (args.size() > 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do " + prefix + "help frame for proper usage");
				});
				return;
			}
			if (confirmAction(ctx, "Would you like to temporarly Disable the Extractor and begin Verification?"))
				extractor.beginVerifications(ctx.getChannel());
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addSetUserUsage(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "setUserUsage";
		String[] aliases = new String[]
		{ "modifyUserUsage", "changeUserUsage", "suu", "muu", "cuu" };
		String usage = prefix + name + " [-u number] [<-n name> or <-s snowflake>]";
		String description = ":large_orange_diamond: __VIP__: Changes how many times a User has extracted Frames today.";
		String detailedDescription = "Changes the number of extractions executed by a User today. Useful for giving some users more extractions. \nUsage: `"
				+ usage
				+ "`\n*[-u number]* = The new number of times the User used the bot today\n*[<-n name> or <-s snowflake>]* = Identify the User account to be added by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s Wumpus`\n:large_orange_diamond: __VIP only__\n - VIPs are always immune to this restriction";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(
				Option.builder("u").argName("Number").desc("New number of extractions made today.").hasArg().longOpt("usage").required().build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Username").desc("Name of Discord User without Discriminator").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Discord User").hasArg().longOpt("snowflake").build());
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				try
				{
					int used = Integer.parseInt(line.getOptionValue('u'));
					if (line.hasOption('n') || line.hasOption('s'))
					{
						IUser user = parseUser(ctx, line);
						if (user != null)
						{
							if (db.updateUserDailyUsage(used, user.getLongID()))
							{
								String uname = user.mention(false);
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Today's Usage now at `" + used + "` for " + uname);
								});
							}
							else
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("User has never interacted with Frame-Summoner!");
								});
							}
						}
					}
					else
					{
						if (confirmAction(ctx, "Are you sure you want to update **EVERYONE'S** extraction count for today to `" + used + "`?"))
						{
							if (db.updateUserDailyUsage(used, null))
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Today's Usage now at `" + used + "` for everyone");
								});
							}
							else
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage(":radioactive: SQLException occured");
								});
							}
						}
					}
				}
				catch (NumberFormatException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("The `-u` flag argument is not a valid integer!");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addSetServerUsage(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "setServerUsage";
		String[] aliases = new String[]
		{ "modifyServerUsage", "changeServerUsage", "ssu", "msu", "csu" };
		String usage = prefix + name + " [-u number] [<-n GuildName> or <-s SnowflakeID>]";
		String description = ":large_orange_diamond: __VIP__: Changes how many times a Server has extracted Frames today.";
		String detailedDescription = "Changes the number of extractions executed by a Server today. Useful for giving some Servers more extractions. \nUsage: `"
				+ usage
				+ "`\n*[-u number]* = The new number of times the User used the bot today\n*[<-n GuildName> or <-s SnowflakeID>]* = Identify the User account to be added by Name or Snowflake ID as declared by flag. Example `-n Wumpus` or `-s 389021830`\n:large_orange_diamond: __VIP only__\n - VIPs are always immune to this restriction";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(
				Option.builder("u").argName("Number").desc("New number of extractions made today.").hasArg().longOpt("usage").required().build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("GuildName").desc("Name of the Guild").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Discord Object").hasArg().longOpt("snowflake").build());
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				try
				{
					int used = Integer.parseInt(line.getOptionValue('u'));
					if (line.hasOption('n') || line.hasOption('s'))
					{
						IGuild guild = parseGuild(ctx, line);
						//Global Change
						if (guild != null)
						{
							if (db.updateServerDailyUsage(used, guild.getLongID()))
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Today's Usage now at `" + used + "` for " + guild.getName());
								});
							}
							else
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("User has never interacted with Frame-Summoner!");
								});
							}
						}
					}
					else
					{
						if (confirmAction(ctx, "Are you sure you want to update **EVERY GUILD'S** extraction count for today to `" + used + "`?"))
						{
							if (db.updateServerDailyUsage(used, null))
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Today's Usage now at `" + used + "` for every server");
								});
							}
							else
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage(":radioactive: SQLException occured");
								});
							}
						}
					}
				}
				catch (NumberFormatException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("The `-u` flag argument is not a valid integer!");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addRestrictCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "restrict";
		String[] aliases = new String[]
		{};
		String usage = prefix + name;
		String description = ":large_orange_diamond: __VIP__: Restricts or unrestricts a video to VIP only access.";
		String detailedDescription = "Restrict or Unrestrict a Video. Restricted videos do not exist to normal Users. VIP Users can see them, and interact with them.\nUsage: `"
				+ usage + "`\n:large_orange_diamond: __VIP only__\n - All Extractions are halted while verifications take place";
		detailedDescription += appendAliases(aliases);
		Options o = new Options();
		o.addOption("u", "undo", false, "If the Video(s) should be unrestricted instead");
		OptionGroup g1 = new OptionGroup();
		g1.addOption(new Option("a", "all", false, "Modifies ALL Videos in Record"));
		g1.addOption(Option.builder("s").argName("Video name").desc("Modifies only the listed videos").hasArgs().longOpt("single").build());
		g1.setRequired(true);
		o.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(o, result, false);
				boolean restrict = !line.hasOption('u');
				if (line.hasOption('a'))
				{
					int updateCount = db.setAllVideosRestricted(restrict);
					int fullCount = db.getVisibleVideoCount(true);
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(updateCount + "/" + fullCount + " Videos Updated");
					});
				}
				else if (line.hasOption('s'))
				{
					String[] names = line.getOptionValues('s');
					ArrayList<String> unchanged = db.setVideoRestricted(names, restrict);
					String message;
					if (unchanged.isEmpty())
					{
						message = "All Specified Videos Successfully Updated";
					}
					else
					{
						StringBuilder s = new StringBuilder("```md\n");
						s.append(unchanged.size());
						s.append('/');
						s.append(names.length);
						s.append(" Files Were Not Found\n--------------------------\n");
						for (int x = 0; x < unchanged.size(); x++)
						{
							s.append(String.format("%-3s", (x + 1) + "."));
							s.append(' ');
							s.append(unchanged.get(x));
							s.append("\n");
						}
						s.append("```");
						message = s.toString();
					}
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(message);
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	/**
	 * Return the specified Video's length of time to Discord
	 */
	private void addVerifyCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "verify";
		String[] aliases = new String[]
		{};
		String usage = prefix + name + " [-f Filename] <-e>";
		String description = ":large_orange_diamond: __VIP__: Adds video to verification Queue.";
		String detailedDescription = "Places video in verification queue, where on execution, the length of time, framerate, and frame extraction eligibility are determined. Optionally begin queued verifications right away.\nUsage: `"
				+ usage
				+ "`\n*[-f Filename]* = Name of Video file in Video Directory to add to Verification Queue\n*<-e>* = Option to execute current queue of verifications if video specified was properly added.\n:large_orange_diamond: __VIP only__\n:warning: Sometimes requires Confirmation since some functions are disabled while process runs.\n:anger: Cancellable: "
				+ prefix + "pause | " + prefix + "kill\n - All Extractions are halted while verifications take place";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(
				Option.builder("e").desc("Immediately executes current verifications in the queue.").hasArg(false).longOpt("execute").build());
		options.addOption(Option.builder("f").argName("Filename").desc("The Filename of the video.").hasArg().longOpt("file").required().build());
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					boolean execute = line.hasOption('e');
					String filename = line.getOptionValue('f');
					if (extractor.requestSingleVerification(ctx, filename))
					{
						if (execute)
							if (confirmAction(ctx, "Would you like to temporarly Disable the Extractor and begin Verification?"))
								extractor.beginVerifications(ctx.getChannel());
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Skipping Verification Process");
						});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addWipeDeletedVideosCommand(int commandTier, Limiter[] limits)
	{

	}

	/**
	 * Return a list of videos to Discord
	 */
	private void addListCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "list";
		String[] aliases = new String[]
		{ "videos", "record" };
		String usage = prefix + name + " <-l> <-r>";
		String description = "Lists all Videos accessible to User that can be extracted from";
		String detailedDescription = "Lists all videos accessible to User with current privleges by Filename, Duration, and Framerate to assist with formulating extractions.\nUsage: `"
				+ usage
				+ "`\n*<-l>* = :large_orange_diamond: __VIP argument only__ Lists literal files in folder rather then the verified list\n*<-r* = :large_orange_diamond: __VIP argument only__ Shows verified List of Videos of only Restricted or Unusable files";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("l").desc("Shows list of literal files in the Video Directory").hasArg(false).longOpt("literal").build());
		g1.addOption(Option.builder("r").desc("Shows list of Restricted Videos Only").hasArg(false).longOpt("restricted").build());
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					boolean literal = line.hasOption('l');
					boolean restrictedOnly = line.hasOption('r');
					boolean isVIP = VIPLimiter.checkOperator(prop, ctx.getAuthor().getLongID()) || db.isUserVIP(ctx.getAuthor().getLongID());
					if (literal)
					{
						if (isVIP)
						{
							String videoDir = prop.getProperty("Video_Directory");
							File videos = new File(videoDir);
							if (!videos.exists() || videos.listFiles().length == 0)
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("No Videos found");
								});
								videos.mkdirs();
							}
							else
							{
								StringBuilder s = new StringBuilder("Avaliable Video Files in: *" + videoDir + "*");
								File[] vlist = videos.listFiles();
								s.append("\n```md\n");
								for (int x = 0; x < vlist.length; x++)
								{
									String nextLine = String.format("%-3s", (x + 1) + ".") + vlist[x].getName() + "\n";
									if ((s.length() + nextLine.length()) >= 1996)
									{
										//Send in Chunks
										s.append("```");
										final String stringPart = s.toString();
										RequestBuffer.request(() -> {
											return ctx.getChannel().sendMessage(stringPart);
										});
										s = new StringBuilder("```md\n");
									}
									s.append(String.format("%3d", x + 1) + ". " + vlist[x].getName() + "\n");
								}
								s.append("```");
								final String finalStringPart = s.toString();
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage(finalStringPart);
								});
							}
						}
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":no_entry: You must be a :large_orange_diamond: VIP to use the `-l` Flag");
							});
						}
					}
					else
					{
						if (isVIP || !restrictedOnly)
						{
							ArrayList<DBVideo> videos = db.getVideoList();
							if (!isVIP)
							{
								for (int x = videos.size() - 1; x >= 0; x--)
								{
									if (videos.get(x).isRestricted() || !videos.get(x).isUsable())
										videos.remove(x);
								}
							}
							else if (restrictedOnly)
							{
								for (int x = videos.size() - 1; x >= 0; x--)
								{
									if (!videos.get(x).isRestricted() && videos.get(x).isUsable())
										videos.remove(x);
								}
							}
							StringBuilder s = new StringBuilder("```md\n");
							if (line.hasOption('r'))
								s.append(String.format("%3d", videos.size()) + " Restricted Videos  | Duration     | FPS\n");
							else
								s.append(String.format("%3d", videos.size()) + " Accessible Videos  | Duration     | FPS\n");
							s.append("------------------------------------------------\n");
							for (int x = 0; x < videos.size(); x++)
							{
								String vname = videos.get(x).getName();
								//Even Restricted videos will have all necessary values
								String nextLine = "  " + String.format("%-20s", vname.trim().substring(0, Math.min(20, vname.length()))) + " | "
										+ Extractor.convertMilliToTime(videos.get(x).getLength()).trim() + " | " + videos.get(x).getFps().trim();
								if (videos.get(x).isRestricted())
									nextLine = "< " + nextLine.substring(2) + " >";
								else if (!videos.get(x).isUsable())
									nextLine = "> " + nextLine.substring(2);
								if ((s.length() + nextLine.length()) >= 1996)
								{
									//Send in Chunks
									s.append("```");
									final String stringPart = s.toString();
									RequestBuffer.request(() -> {
										ctx.getChannel().sendMessage(stringPart);
									}).get();
									s = new StringBuilder("```md\n");
								}
								s.append(nextLine + "\n");
							}
							s.append("```");
							final String finalStringPart = s.toString();
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(finalStringPart);
							});
						}
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":no_entry: You must be a :large_orange_diamond: VIP to use the `-r` Flag");
							});
						}
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	/**
	 * Changes the Video Directory of bot after a quick Reboot
	 */
	private void addDirCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "dir";
		String[] aliases = new String[]
		{ "directory" };
		String usage = prefix + name + " [\"Directory Path\"]";
		String description = ":customs: __Operator__: Change the Directory of Videos to extract from";
		String detailedDescription = "Changes the Video Directory the bot monitors.\nUsage: `" + prefix
				+ "dir [\"Directory Path\"]`\n*[\"Directory Path\"]* = Path of Directory with videos to extract from, use quotation marks around path names with spaces if necessary.\n:customs: __Operator only__\n - Command can be completely disabled in config\n - Forces Bot Reboot directly after";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (args.size() > 0)
			{ // more than 1 argument
				if (!prop.getProperty("AllowDirectoryChange").equals("Y"))
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("**Command Disabled**\nEdit property in values.txt and reboot to allow usage");
					});
				}
				String path = assembleString(args);
				if (!new File(path).exists() || !new File(path).isDirectory())
				{
					final String path2 = new String(path);
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("File Path: `" + path2 + "`\nIs not valid, or does not exist.");
					});
					return;
				}
				if (confirmAction(ctx, "Are you sure you want to change the Directory to\n" + path))
				{
					logger.info("Updating Video Directory to: {}", path);
					dri.editVideoDirectory(path, true, ctx);
					final String path2 = new String(path);
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("File Path: `" + path2 + "`\nSaved to values.txt");
					});
					logger.info("Edited values.txt, now rebooting");
					DRI.menu.manualRestart(500);
				}
				else
				{

				}
			}
			else if (args.size() == 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("No File Path Provided, please make sure to reboot path ");
				});
			}
			else
				throw new IllegalArgumentException("Negative number of Arguments!");
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	/**
	 * Shows some information about the bot.
	 */
	private void addInfoCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "info";
		String[] aliases = new String[]
		{ "information" };
		String usage = prefix + name;
		String description = "Display Info about the bot";
		String detailedDescription = "Provides Generic information about this Discord bot\nUsage: `" + usage + "'";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("s").desc("Shows Info about this Server").hasArg(false).longOpt("server").build());
		g1.addOption(
				Option.builder("u").desc("Shows Info about the current user's standing with Frame-Summoner").hasArg(false).longOpt("user").build());
		g1.addOption(Option.builder("b").desc("Shows Info about Frame-Summoner").hasArg(false).longOpt("bot").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					if (line.hasOption('s'))
					{
						ArrayList<String> textTitle = new ArrayList<String>();
						ArrayList<String> textValue = new ArrayList<String>();
						ArrayList<Boolean> inline = new ArrayList<Boolean>();
						//Field Titles: 50
						//Field Values: 175
						DBGuild g = db.getServerData(ctx.getGuild().getLongID());
						String mode = g.isBlacklistMode() ? "Blacklist" : "Whitelist";
						textTitle.add("Operating Mode");
						textValue.add(g.isEnabled() ? "Online" : "Paused");
						inline.add(true);
						textTitle.add("Role Listing Mode");
						textValue.add(mode);
						inline.add(true);
						textTitle.add("Daily Server Request Limit");
						textValue.add(g.getRequestLimit() + " requests");
						inline.add(true);
						textTitle.add("Today's Extraction Total");
						textValue.add(g.getUsedToday() + " requests");
						inline.add(true);
						String word = new String("");
						for (Long l : db.getListOfAdminRoles(ctx.getGuild().getLongID()))
						{
							word += ctx.getGuild().getRoleByID(l).getName() + ", ";
						}
						word = word.length() == 0 ? "<>" : word.substring(0, word.length() - 2);
						textTitle.add("Administrator Roles");
						textValue.add(word);
						inline.add(false);
						word = new String("");
						for (Long l : db.getListOfUserRoles(ctx.getGuild().getLongID()))
						{
							word += ctx.getGuild().getRoleByID(l).getName() + ", ";
						}
						word = word.length() == 0 ? "<>" : word.substring(0, word.length() - 2);
						textTitle.add(mode + "ed Roles");
						textValue.add(word);
						inline.add(false);
						String tier3 = "";
						String tier2 = "";
						String tier1 = "";
						for (DBChannel c : db.getServerChannels(ctx.getGuild().getLongID()))
						{
							switch (c.getTier())
							{
								case 3:
									tier3 += ctx.getClient().getChannelByID(c.getId()).getName() + "\n";
									break;
								case 2:
									tier2 += ctx.getClient().getChannelByID(c.getId()).getName() + "\n";
									break;
								case 1:
									tier1 += ctx.getClient().getChannelByID(c.getId()).getName() + "\n";
									break;
								default:
									break;
							}
						}
						tier3 = tier3.length() == 0 ? "<>" : tier3.substring(0, tier3.length() - 1);
						tier2 = tier2.length() == 0 ? "<>" : tier2.substring(0, tier2.length() - 1);
						tier1 = tier1.length() == 0 ? "<>" : tier1.substring(0, tier1.length() - 1);
						textTitle.add("Admin Channels");
						textValue.add(tier3);
						inline.add(true);
						textTitle.add("Full Usage Channels");
						textValue.add(tier2);
						inline.add(true);
						textTitle.add("Info Only Channels");
						textValue.add(tier1);
						inline.add(true);
						sendEmbedList(ctx, new Color(0, 255, 0), ctx.getGuild().getName() + " Frame-Summoner Configuration",
								"List all Information regarding this server", ctx.getGuild().getIconURL(), textTitle, textValue, inline, null);
					}
					else if (line.hasOption('u'))
					{

					}
					else if (line.hasOption('b'))
					{
						RequestBuffer.request(() -> {
							ctx.getChannel()
									.sendMessage("- **Version:** " + DRI.version + "\n- **Author:** Christian77777\n"
											+ "- **Programming Language:** Java\n" + "- **Discord Connection Library:** Discord4J\n"
											+ "- **Discord (Command) Library:** Commands4J");
						});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addNewVIPCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "addVIPUser";
		String[] aliases = new String[]
		{ "avu", "newVIPUser", "nvu" };
		String usage = prefix + name + " [<-n Username> or <-s SnowflakeID>]";
		String description = ":customs: __Operator__: Marks a User as VIP.";
		String detailedDescription = "Gives a Discord User access to VIP commands and status across all Discord Servers\nUsage: `" + usage
				+ "`\n*[<-n Username> or <-s SnowflakeID>]* = Identify the User account to be added by Name or Snowflake ID as declared by flag. Example `-n Wumpus` or `-s 389021830`\n:customs: __Operator only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Username").desc("Name of Discord User without Discriminator").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Discord User").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					IUser user = parseUser(ctx, line);
					if (user != null)
					{
						if (db.addNewVIP(user.getLongID()))
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(user.mention() + " is now VIP");
							});
						else
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("User was already VIP!");
							});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addRemoveVIPCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "removeVIPUser";
		String[] aliases = new String[]
		{ "rvu", "deleteVIPUser", "dvu" };
		String usage = prefix + name + " [<-n Username> or <-s SnowflakeID>]";
		String description = ":customs: __Operator__: Removes a User as VIP";
		String detailedDescription = "Removes a Discord User's access to VIP commands and status across all Discord Servers\nUsage: `" + usage
				+ "`\n*[<-n Username> or <-s SnowflakeID>]* = Identify the User account to be removed by Name or Snowflake ID as declared by flag. Example `-n Wumpus` or `-s 389021830`\n:customs: __Operator only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Username").desc("Name of Discord User without Discriminator").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Discord User").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					IUser user = parseUser(ctx, line);
					if (user != null)
					{
						if (db.removeVIP(user.getLongID()))
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(user.getName() + " is no longer VIP");
							});
						else
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("User was not already VIP!");
							});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addNewAdminRoleCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "addAdminRole";
		String[] aliases = new String[]
		{ "aar", "newAdminRole", "nar" };
		String usage = prefix + name + " [<-n RoleID> or <-s RoleName>]";
		String description = ":a: __Admin__: Adds a Role as Admin for this Server.";
		String detailedDescription = "Gives access to Admin commands for a Role in the current server\nUsage: `" + usage
				+ "`\n*[<-n RoleID> or <-s RoleName>]* = Identify the Server Role to be affected by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s ADMIN`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Role Name").desc("Discord Role Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					IRole role = parseRole(ctx, line, false);
					if (role != null)
					{
						if (db.addNewAdminRole(role.getLongID(), ctx.getGuild().getLongID()))
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Admin Role Added");
							});
						else
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Role Already Added!");
							});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addRemoveAdminRoleCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "removeAdminRole";
		String[] aliases = new String[]
		{ "rar", "deleteAdminRole", "dar" };
		String usage = prefix + name + " [<-n RoleID> or <-s RoleName>]";
		String description = ":a: __Admin__: Removes a Role as Admin for this Server.";
		String detailedDescription = "Removes access to Admin commands for a Role in the current server\nUsage: `" + usage
				+ "`\n*[<-n RoleID> or <-s RoleName>]* = Identify the Server Role to be affected by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s ADMIN`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Role Name").desc("Discord Role Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					IRole role = parseRole(ctx, line, false);
					if (role != null)
					{
						if (db.removeAdminRole(role.getLongID()))
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Admin Role Removed");
							});
						else
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Role not an Admin");
							});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addNewListedRoleCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "addListedRole";
		String[] aliases = new String[]
		{ "alr", "newListedRole", "nlr" };
		String usage = prefix + name + " [<-n RoleName> or <-s SnowflakeID>]";
		String description = ":a: __Admin__: White/Blacklists a Role to regulate Bot Usage.";
		String detailedDescription = "Change access to normal commands for a Role in the current server depending on Black/White listing mode by listing it.\nUsage: `"
				+ usage
				+ "`\n*[<-n RoleName> or <-s SnowflakeID>]* = Identify the Server Role to be affected by Name or Snowflake ID as declared by flag. Example `-n ADMIN` or `-s 389021830`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("RoleName").desc("Discord Role Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					IRole role = parseRole(ctx, line, false);
					if (role != null)
					{
						boolean isBlacklist = db.checkGuildBlacklistMode(ctx.getGuild().getLongID());
						String message;
						if (db.addNewUserRole(role.getLongID(), ctx.getGuild().getLongID(), isBlacklist))
						{
							if (isBlacklist)
								message = "Blacklist Role Added";
							else
								message = "Whitelist Role Added";
						}
						else
							message = ":no_entry_sign: Role was already added!";
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(message);
						});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addRemoveListedRoleCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "removeListedRole";
		String[] aliases = new String[]
		{ "rlr", "deleteListedRole", "dlr" };
		String usage = prefix + name + " [<-n RoleName> or <-s SnowflakeID>]";
		String description = ":a: __Admin__: Unlists a Role that was previously White/Blacklisted.";
		String detailedDescription = "Change access to normal commands for a Role in the current server depending on Black/White listing mode by unlisting it.\nUsage: `"
				+ usage
				+ "`\n*[<-n RoleName> or <-s SnowflakeID>]* = Identify the Server Role to be affected by Name or Snowflake ID as declared by flag. Example `-n ADMIN` or `-s 389021830`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("RoleName").desc("Discord Role Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					IRole role = parseRole(ctx, line, false);
					if (role != null)
					{
						if (db.removeUserRole(role.getLongID()))
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Listed Role Removed");
							});
						}
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":no_entry_sign: Role was already not Listed!");
							});
						}
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addChangeListingModeCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "changeListingMode";
		String[] aliases = new String[]
		{ "clm", "updateListingMode", "ulm" };
		String usage = prefix + name;
		String description = ":a: __Admin__: Changes a Server to toggle between a White or Blacklist for allowing Bot usage";
		String detailedDescription = "Toggles the current server listing mode from Black -> White or White -> Black.\nUsage: `" + usage
				+ "`\n:a: __Admin only__\n:warning: Requires Confirmation since data will be deleted.\n - All previously listed roles are delisted on toggle";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (args.size() > 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do `" + prefix + "help changeListingMode` for proper usage");
				});
				return;
			}
			boolean isBlacklist = db.checkGuildBlacklistMode(ctx.getGuild().getLongID());
			String message;
			if (isBlacklist)
				message = "Would you like to change this server from a Blacklist to Whitelist?\nAll Current Listing Permissions will be deleted.";
			else
				message = "Would you like to change this server from a Whitelist to Blacklist?\nAll Current Listing Permissions will be deleted.";
			if (confirmAction(ctx, message))
			{
				db.changeGuildBlacklistMode(ctx.getGuild().getLongID(), !isBlacklist);
				String alternateList;
				if (isBlacklist)
					alternateList = "Whitelist";
				else
					alternateList = "Blacklist";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("This Guild has switched to a " + alternateList);
				});
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addChangeChannelTierCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "changeChannelTier";
		String[] aliases = new String[]
		{ "cct", "changeChannelPermission", "ccp", "updateChannelTier", "updateChannelPermission", "changeChannelPerm", "updateChannelPerm", "uct",
				"ucp" };
		String usage = prefix + name + " [-t Tier (0-3)] <<-n ChannelName> or <-s SnowflakeID>>";
		String description = ":a: __Admin__: Changes a Channel Permission Tier.";
		String detailedDescription = "Changes the types of commands allowed in a Channel of a Server.\nUsage: `" + usage
				+ "`\n*[-t Tier (0-3)]* = The new Permission Tier, higher tiers allow the commands of the previous tier\n*<<-n ChannelName> or <-s SnowflakeID>>* = Identify a Channel in the Server OTHER then the current channel to be affected by Name or Snowflake ID as declared by flag. Example `-n bot-spam` or `-s 389021830`\n:a: __Admin only__\n - Allowed in PM's to prevent lockout, but channel must be specified\n - Tier 0 = Complete Silence\n - Tier 1 = Basic Information Commands\n - Tier 2 = Normal usage\n - Tier 3 = Admin commands";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(Option.builder("t").argName("Number").desc("Channel Permission Tier [0-3]").hasArg().longOpt("tier").required().build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Channel Name").desc("Discord Channel Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					try
					{
						int tier = Integer.parseInt(line.getOptionValue('t'));
						if (tier >= 0 && tier <= 3)
						{
							IChannel channel = parseChannel(ctx, line, true);
							if (channel != null)
							{
								if (db.updateChannelTier(channel.getLongID(), channel.getGuild().getLongID(), tier))
								{
									RequestBuffer.request(() -> {
										ctx.getChannel().sendMessage("Tier Updated");
									});
								}
								else//Should already be guaranteed not to happen in parseChannel()
								{
									RequestBuffer.request(() -> {
										ctx.getChannel().sendMessage(":radioactive: no channel was updated?");
									});
								}
							}
						}
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":no_entry_sign: The `-t` flag argument is not valid! `0 <= tier <= 3`");
							});
						}
					}
					catch (NumberFormatException e)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(":no_entry_sign: The `-t` flag argument is not a valid integer!");
						});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));

		registry.register(command, name, aliases);
	}

	private void addChangeChannelAnnoucementCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "changeChannelAnnoucement";
		String[] aliases = new String[]
		{ "cca", "updateChannelAnnoucement", "uca" };
		String usage = prefix + name + " <-u> <<-n ChannelName> or <-s SnowflakeID>>";
		String description = ":a: __Admin__: Designate channel as Annoucement Channel.";
		String detailedDescription = "Toggle channel as annoucement channel. These channels might get impromptu messages regarding the bot specific to the Server or Global\nUsage: `"
				+ usage
				+ "`\n*<-u>* = Changes command to unset annoucement status\n*<<-n ChannelName> or <-s SnowflakeID>>* = Identify a Channel in the Server OTHER then the current channel to be affected by Name or Snowflake ID as declared by flag. Example `-n bot-spam` or `-s 389021830`\n:a: __Admin only__\n - true or false can be substituted by `y` and `n` or `1` and `0`";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(Option.builder("u").desc("Channel Permission Tier [0-3]").hasArg(false).longOpt("undo").build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Channel Name").desc("Discord Channel Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 0))
				{
					boolean setAnnoucement = !line.hasOption('u');
					IChannel channel = parseChannel(ctx, line, true);
					if (channel != null)
					{
						if (db.updateChannelAnnoucement(channel.getLongID(), channel.getGuild().getLongID(), setAnnoucement))
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Channel Updated");
							});
						}
						else//Should already be guaranteed not to happen in parseChannel()
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":radioactive: Channel not already in Database?");
							});
						}
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList() + "`\nDo `" + prefix + "help "
								+ name + "` for proper Usage");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx, e, name);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addHelpCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "help";
		String[] aliases = new String[]
		{ "halp" };
		String usage = prefix + name + " <Command>";
		String description = "Display avaliable Command list with descriptions, or details just for one";
		String detailedDescription = "You want help about using the Help command? Sheesh, seems like you figured it out already.\nUsage: `" + usage
				+ "`\n*<Command>* = Name of Command to get detailed info about, all commands case sensitive, but some command have aliases for convenience.";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isAdmin = AdminLimiter.checkAdmin(db, ctx.getGuild(), ctx.getAuthor());
			boolean isVIP = db.isUserVIP(ctx.getAuthor().getLongID());
			boolean isOperator = VIPLimiter.checkOperator(prop, ctx.getAuthor().getLongID());
			if (args.size() == 0)
			{
				ArrayList<String> textTitle = new ArrayList<String>();
				ArrayList<String> textValue = new ArrayList<String>();
				ArrayList<Boolean> inline = new ArrayList<Boolean>();
				//Field Titles: 50
				//Field Values: 175
				for (CommandWrapper w : commands)
				{
					if (w.rank == 0 || ((w.rank != 1 || isAdmin) && (w.rank != 2 || (isVIP || isOperator)) && (w.rank != 3 || isOperator)))
					{
						textTitle.add(w.usage);
						textValue.add(w.description);
						inline.add(false);
					}
				}
				sendEmbedList(ctx, new Color(255, 255, 0), "Command List",
						"All Frame-Summoner commands, avaliable to " + ctx.getAuthor().getDisplayName(ctx.getGuild()) + " [Required] <Optional>",
						ctx.getAuthor().getAvatarURL(), textTitle, textValue, inline, null);
			}
			else if (args.size() == 1)
			{
				String longDescription = null;
				int rank = -1;
				for (CommandWrapper w : commands)
				{
					for (String s : w.names)
					{
						if (s.equals(args.get(0)))
						{
							longDescription = w.detailed;
							rank = w.rank;
							break;
						}
					}
					if (longDescription != null)
						break;
				}
				if (longDescription == null)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Unknown Command!\nUse " + prefix + "help for Command List\nUsage: **s!help** <Command Name>");
					});
				}
				else
				{
					String permission = "";
					if (((rank == 1 && !isAdmin) || (rank == 2 && !(isVIP || isOperator)) || (rank == 3 && !isOperator)))
					{
						permission = "\n\n:no_entry: This command is currently not avaliable to you";
					}
					final String message = longDescription + permission;
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(message);
					});
				}
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments");
				}).get();
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private IRole parseRole(CommandContext ctx, CommandLine line, boolean checkAdminAuthority)
	{
		IRole role = null;
		if (line.hasOption('s'))
		{
			try
			{
				if (ctx.getChannel().isPrivate())
					role = ctx.getClient().getRoleByID(Long.parseLong(line.getOptionValue('s')));
				else
					role = ctx.getGuild().getRoleByID(Long.parseLong(line.getOptionValue('s')));
				if (role == null)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":no_entry_sign: Role does not exist!");
					});
				}
			}
			catch (NumberFormatException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not a valid Snowflake ID!");
				});
			}
		}
		else if (line.hasOption('n'))
		{
			List<IRole> roles;
			if (ctx.getChannel().isPrivate())
			{
				roles = new ArrayList<IRole>();
				for (IRole r : ctx.getClient().getRoles())
				{
					if (r.getName().equals(line.getOptionValue('n')))
						roles.add(r);
				}
			}
			else
			{
				roles = ctx.getGuild().getRolesByName(line.getOptionValue('n'));
			}
			if (roles.size() == 1)
				role = roles.get(0);
			else if (roles.size() > 1)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too many roles with this Name!");
				});
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: Role does not exist!");
				});
			}
		}
		else
		{
			throw new IllegalArgumentException("Command Argument Expectations failed");
		}
		if (role != null && ctx.getChannel().isPrivate() && checkAdminAuthority)//Must do Admin Check now
		{
			IGuild channelGuild = role.getGuild();
			//Check if Not Authorized
			if (!AdminLimiter.checkServerOwner(channelGuild, ctx.getAuthor()) && !AdminLimiter.checkAdmin(db, channelGuild, ctx.getAuthor()))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel()
							.sendMessage(":no_entry: You must be an :a: Admin of the " + channelGuild.getName() + " Server to affect this channel");
				});
				role = null;
			}
		}
		return role;
	}

	private IChannel parseChannel(CommandContext ctx, CommandLine line, boolean checkAdminAuthority)
	{
		IChannel channel = null;
		if (!line.hasOption('n') && !line.hasOption('s'))
		{
			if (ctx.getChannel().isPrivate())
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: This is not a Discord Server Channel!");
				});
			}
			else
				channel = ctx.getChannel();
		}
		else if (line.hasOption('s'))
		{
			try
			{
				Long id = Long.parseLong(line.getOptionValue('s'));
				if (ctx.getChannel().isPrivate())//Speed Optimization
					channel = ctx.getClient().getChannelByID(id);
				else
					channel = ctx.getGuild().getChannelByID(id);
				if (channel != null)
				{
					if (channel.isPrivate())
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(":no_entry_sign: This is a Private Channel Snowflake ID, not allowed!");
						});
						channel = null;
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":no_entry_sign: No Channel found with that Snowflake ID!");
					});
				}
			}
			catch (NumberFormatException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: Channel ID provided not a valid Snowflake ID");
				});
			}
		}
		else if (line.hasOption('n'))
		{
			String channelName = line.getOptionValue('n');
			List<IChannel> channels;
			if (ctx.getChannel().isPrivate())//Speed Optimization
			{
				channels = new ArrayList<IChannel>();
				for (IChannel e : ctx.getClient().getChannels())
				{
					if (e.getName().equals(channelName))
						channels.add(e);
				}
			}
			else
			{
				channels = ctx.getGuild().getChannelsByName(channelName);
			}
			if (channels.size() == 1)
				channel = channels.get(0);
			else if (channels.size() == 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: No Channel found with that name!");
				});
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: Too many Channels found with this Name!");
				});
			}
		}
		if (channel != null && ctx.getChannel().isPrivate() && checkAdminAuthority)//Must do Admin Check now
		{
			IGuild channelGuild = channel.getGuild();
			//Check if Not Authorized
			if (!AdminLimiter.checkServerOwner(channelGuild, ctx.getAuthor()) && !AdminLimiter.checkAdmin(db, channelGuild, ctx.getAuthor()))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel()
							.sendMessage(":no_entry: You must be an :a: Admin of the " + channelGuild.getName() + " Server to affect this channel");
				});
				channel = null;
			}
		}
		return channel;
	}

	private IGuild parseGuild(CommandContext ctx, CommandLine line)
	{
		IGuild guild = null;
		if (line.hasOption('n'))
		{
			ArrayList<IGuild> guilds = new ArrayList<IGuild>();
			for (IGuild g : ctx.getClient().getGuilds())
			{
				if (g.getName().equals(line.getOptionValue('n')))
					guilds.add(g);
			}
			if (guilds.size() == 1)
				guild = guilds.get(0);
			else if (guilds.size() == 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: No Guild with that name!");
				});
				return null;
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: Too many Guilds with this name!");
				});
				return null;
			}
		}
		else if (line.hasOption('s'))
		{
			try
			{
				guild = ctx.getClient().getGuildByID(Long.parseLong(line.getOptionValue('s')));
			}
			catch (NumberFormatException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: Not a valid Snowflake ID!");
				});
				return null;
			}
			if (guild == null)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: No Guild with this Snowflake ID!");
				});
				return null;
			}
		}
		else
		{
			throw new IllegalArgumentException(
					"Required Flags -n or -s never set! ParseException should have occured or Option Configuration incorrect.");
		}
		return guild;
	}

	private IUser parseUser(CommandContext ctx, CommandLine line)
	{
		IUser user = null;
		if (line.hasOption('n'))
		{
			List<IUser> users = ctx.getClient().getUsersByName(line.getOptionValue('n'), true);
			if (users.size() == 1)
				user = users.get(0);
			else if (users.size() == 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: No User with that name!");
				});
				return null;
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: Too many Users with this Name!");
				});
				return null;
			}
		}
		else if (line.hasOption('s'))
		{
			try
			{
				user = ctx.getClient().getUserByID(Long.parseLong(line.getOptionValue('s')));
			}
			catch (NumberFormatException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: Not a valid Snowflake ID!");
				});
				return null;
			}
			if (user == null)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: No User with this Snowflake ID!");
				});
				return null;
			}
		}
		else
		{
			throw new IllegalArgumentException(
					"Required Flags -n or -s never set! ParseException should have occured or Option Configuration incorrect.");
		}
		return user;
	}

	private void handleParseException(CommandContext ctx, ParseException e, String commandName)
	{
		String specificError = new String("");
		if (e instanceof MissingOptionException)
		{
			MissingOptionException e2 = (MissingOptionException) e;
			ArrayList<String> list = new ArrayList<String>();
			for (Object o : e2.getMissingOptions())
			{
				if (o instanceof OptionGroup)
				{
					OptionGroup group = (OptionGroup) o;
					String temp = "(";
					for (Option oz : group.getOptions())
					{
						temp += "-" + oz.getOpt() + " or ";
					}
					temp = temp.substring(0, temp.length() - 4) + ")";
					list.add(temp);
				}
				else if (o instanceof Option)
				{
					Option ox = ((Option) o);
					list.add(ox.getOpt());
				}
				else //String
				{
					list.add("-" + (String) o);
				}
			}
			specificError = "Missing Required Options: `" + list + "`";
		}
		else if (e instanceof AlreadySelectedException)
		{
			AlreadySelectedException e2 = (AlreadySelectedException) e;
			specificError = "Conflicting Options were used: `" + e2.getOption().getOpt() + "` conflicts with `" + e2.getOptionGroup().getSelected()
					+ "`";
		}
		else if (e instanceof MissingArgumentException)
		{
			MissingArgumentException e2 = (MissingArgumentException) e;
			specificError = "Option `" + e2.getOption().getOpt() + "` was not provided with an argument: `" + e2.getOption().getArgName() + "`";
		}
		else if (e instanceof UnrecognizedOptionException)
		{
			UnrecognizedOptionException e2 = (UnrecognizedOptionException) e;
			specificError = "Unrecognized Option `" + e2.getOption() + "`";
		}
		else
		{
			logger.error("Unable to Determine ParseException Instance", e);
		}
		final String response = ":octagonal_sign: " + specificError + "\nDo `" + prefix + "help " + commandName + "` for proper Usage";
		RequestBuffer.request(() -> {
			ctx.getChannel().sendMessage(response);
		});
	}

	private boolean confirmAction(CommandContext ctx, String message)
	{
		IMessage m = RequestBuffer.request(() -> {
			return ctx.getMessage().reply(message.substring(0, Math.min(message.length(), 1900))
					+ "\n:white_check_mark: to Confirm\n:negative_squared_cross_mark: to Deny\n5 seconds to Accept");
		}).get();
		RequestBuffer.request(() -> {
			m.addReaction(confirm);
		}).get();
		RequestBuffer.request(() -> {
			m.addReaction(deny);
		}).get();
		try
		{
			Thread.sleep(5000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		IMessage m3 = ctx.getClient().getMessageByID(m.getLongID());
		if (m3.getReactionByEmoji(deny).getUserReacted(ctx.getMessage().getAuthor()))
		{
			RequestBuffer.request(() -> {
				m3.removeReaction(m3.getClient().getOurUser(), confirm);
			}).get();
			RequestBuffer.request(() -> {
				m3.addReaction(ReactionEmoji.of(new String(Character.toChars(127383))));
			}).get();
			return false;
		}
		else if (m3.getReactionByEmoji(confirm).getUserReacted(ctx.getMessage().getAuthor()))
		{
			RequestBuffer.request(() -> {
				m3.removeReaction(m3.getClient().getOurUser(), deny);
			}).get();
			RequestBuffer.request(() -> {
				m3.addReaction(ReactionEmoji.of(new String(Character.toChars(127383))));
			}).get();
			return true;
		}
		else
		{
			RequestBuffer.request(() -> {
				m3.removeAllReactions();
			}).get();
			RequestBuffer.request(() -> {
				m3.addReaction(ReactionEmoji.of(new String(Character.toChars(128162))));
			}).get();
			return false;
		}
	}

	/**
	 * Analyze String to Update GUI border if needed
	 * 
	 * @param text
	 * The text to Verify
	 * @return if Offset Text is Acceptable
	 */
	public static boolean checkOffset(String text)
	{
		if (!text.equals("00:00:00.000"))
		{
			boolean wrong = true;
			if (text.length() == 12 && StringUtils.isNumeric(text.substring(0, 2)) && text.substring(2, 3).equals(":")
					&& StringUtils.isNumeric(text.substring(3, 5)) && text.substring(5, 6).equals(":") && StringUtils.isNumeric(text.substring(6, 8))
					&& text.substring(8, 9).equals(".") && StringUtils.isNumeric(text.substring(9, 12)))
			{
				wrong = false;
			}
			else if (text.length() == 8 && StringUtils.isNumeric(text.substring(0, 2)) && text.substring(2, 3).equals(":")
					&& StringUtils.isNumeric(text.substring(3, 5)) && text.substring(5, 6).equals(":") && StringUtils.isNumeric(text.substring(6, 8)))
			{
				wrong = false;
			}
			return !wrong;
		}
		else
		{
			return true;
		}
	}

	public String[] assembleArguments(List<String> args) throws IndexOutOfBoundsException
	{
		ArrayList<String> fullArgs = new ArrayList<String>();
		String[] result;
		if (fullArgs.size() == 1 && fullArgs.get(0).equals(""))
			result = new String[0];
		else
		{
			for (int x = 0; x < args.size(); x++)
			{
				String temp = args.get(x);
				if (temp.startsWith("\""))
				{

					do
					{
						temp += " " + args.get(++x);
					}
					while (!temp.endsWith("\""));
					fullArgs.add(temp);

				}
				else
					fullArgs.add(temp);
			}
			result = fullArgs.toArray(new String[0]);
		}
		return result;
	}

	public void sendEmbedList(CommandContext ctx, Color c, String title, String desc, String thumbnail, ArrayList<String> textTitle,
			ArrayList<String> textValue, ArrayList<Boolean> inline, String footer)
	{
		int pageCount = ((int) Math.ceil(textTitle.size() / 25.0));
		for (int z = 0; z < pageCount; z++)
		{
			EmbedBuilder message = new EmbedBuilder();
			message.withAuthorName("FoxTrot Fanatics");
			message.withAuthorUrl("https://foxtrotfanatics.info");
			message.withAuthorIcon("https://storage.googleapis.com/ftf-public/CYTUBE/imgs/ftf_logo.png");
			message.withTitle(title);
			message.withDesc(desc);
			if (thumbnail != null)
			{
				message.withThumbnail(thumbnail);
			}
			message.withColor(c);
			int fieldCount = 0;
			for (int x = 0; x < 25; x++)
			{
				if ((x + (z * 25)) < textTitle.size())
				{
					message.appendField(textTitle.get(x + (z * 25)), textValue.get(x + (z * 25)), inline.get(x + (z * 25)));
					fieldCount++;
				}
			}
			if (footer == null)
			{
				message.withFooterText("Showing " + fieldCount + " Entries - Page " + (z + 1) + "/" + pageCount);
			}
			else
			{
				message.withFooterText(footer);
			}
			RequestBuffer.request(() -> {
				return ctx.getChannel().sendMessage(message.build());
			}).get();
		}
	}

	public String assembleString(ArrayList<String> args) throws IndexOutOfBoundsException
	{
		String result = args.remove(0);
		if (result.startsWith("\""))
		{
			do
			{
				result += " " + args.remove(0);
			}
			while (!result.endsWith("\""));
			return result.substring(1, result.length() - 1);
		}
		else
			return result;
	}

	public String appendAliases(String[] aliases)
	{
		String tail = "";
		if (aliases.length > 0)
		{
			tail += "\nAliases:";
			for (String a : aliases)
			{
				tail += " `" + a + "`,";
			}
			tail = tail.substring(0, tail.length() - 1);
		}
		return tail;
	}

	public CommandRegistry getRegistry()
	{
		return registry;
	}

	private class CommandWrapper
	{

		private Command command;
		private int rank;
		private String usage;
		private String description;
		private String detailed;
		private String[] names;

		public CommandWrapper(Command command, int rank, String usage, String description, String detailed, String name, String[] aliases)
		{
			this.command = command;
			this.rank = rank;
			this.usage = usage;
			this.description = description;
			this.detailed = detailed;
			this.names = new String[aliases.length + 1];
			this.names[0] = name;
			for (int x = 1; x < names.length; x++)
			{
				this.names[x] = aliases[x - 1];
			}
		}
	}
}
