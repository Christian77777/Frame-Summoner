
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
import com.Christian77777.Frame_Summoner.Database.Video;
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
	private IDiscordClient c;
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
		this.c = c;
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
		TierLimiter privateTier3L = new TierLimiter(db, 3, true, prefix);
		VIPLimiter vipL = new VIPLimiter(d, p);
		AdminLimiter adminL = new AdminLimiter(db, true);
		ListedLimiter listedL = new ListedLimiter(db);
		//TODO Bookmark
		//Debug Commands
		addDebugCommand();
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
		addSetUsage(2, new Limiter[] 
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
		{ privateTier3L, adminL });//Allowed in PMs, Admin Check must be done in program if Private
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

	private void addSetUsage(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "setUsage";
		String[] aliases = new String[]
		{ "modifyUsage", "changeUsage" };
		String usage = prefix + name;
		String description = ":large_orange_diamond: __VIP__: Changes how many times a User has extracted Frames today.";
		String detailedDescription = "Changes the number of extractions executed by a User today. Useful for giving some users more extractions. \nUsage: `"
				+ usage + "`\n:large_orange_diamond: __VIP only__\n - VIPs are always immune to this restriction";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(
				Option.builder("u").argName("Number").desc("New number of extractions made today.").hasArg().longOpt("usage").required().build());
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
				int used;
				try
				{
					used = Integer.parseInt(line.getOptionValue('u'));
				}
				catch (NumberFormatException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("The `-u` flag argument is not a valid integer!");
					});
					return;
				}
				IUser user = null;
				if (line.hasOption('n'))
				{
					List<IUser> users = ctx.getClient().getUsersByName(line.getOptionValue('n'), true);
					if (users.size() == 1)
						user = users.get(0);
					else if (users.size() == 0)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("No User with that name!");
						});
						return;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Too many Users with this Name!");
						});
						return;
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
							ctx.getChannel().sendMessage("Not a valid Snowflake ID!");
						});
						return;
					}
					if (user == null)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("No User with this Snowflake ID!");
						});
						return;
					}
				}
				if(db.resetDailyUsage(used, user.getLongID()))
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
			catch (ParseException e)
			{
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
							for(Option oz : group.getOptions())
							{
								temp += "-" + oz.getOpt() + " or ";
							}
							temp = temp.substring(0,temp.length()-4) + ")";
							list.add(temp);
						}
						else if(o instanceof Option)
						{
							Option ox = ((Option) o);
							list.add(ox.getOpt());
						}
						else //String
						{
							list.add("-" + (String)o);
						}
					}
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Missing Required Options: `" + list + "`");
					});
				}
				else if (e instanceof AlreadySelectedException)
				{
					AlreadySelectedException e2 = (AlreadySelectedException) e;
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Conflicting Options were used: `" + e2.getOption().getOpt()
								+ "` conflicts with `" + e2.getOptionGroup().getSelected() + "`");
					});
				}
				else if (e instanceof MissingArgumentException)
				{
					MissingArgumentException e2 = (MissingArgumentException) e;
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(
								"Option `" + e2.getOption().getOpt() + "` was not provided with an argument: `" + e2.getOption().getArgName() + "`");
					});
				}
				else if (e instanceof UnrecognizedOptionException)
				{
					UnrecognizedOptionException e2 = (UnrecognizedOptionException) e;
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(
								"Unrecognized Option `" + e2.getOption() + "`");
					});
				}
				else
				{
					logger.debug("Unable to Parse Command Arguments supplied", e);
				}
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
				if (e instanceof MissingOptionException)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Missing Required Option that specifies which Videos to modify `[-a | -s | -r]`");
					});
				}
				else if (e instanceof AlreadySelectedException)
				{
					AlreadySelectedException e2 = (AlreadySelectedException) e;
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Too Many Options used that specifies which Videos to modify `" + e2.getOption().getOpt()
								+ "` conflicts with `" + e2.getOptionGroup().getSelected() + "`");
					});
				}
				else if (e instanceof MissingArgumentException)
				{
					MissingArgumentException e2 = (MissingArgumentException) e;
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(
								"Option `" + e2.getOption().getOpt() + "` was not provided with an Argument: `" + e2.getOption().getArgName() + "`");
					});
				}
				else
				{
					logger.debug("Unable to Parse Command Arguments supplied", e);
				}
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
		String usage = prefix + name + " [Filename] <-s>";
		String description = ":large_orange_diamond: __VIP__: Adds video to verification Queue.";
		String detailedDescription = "Places video in verification queue, where on execution, the length of time, framerate, and frame extraction eligibility are determined. Optionally begin queued verifications right away.\nUsage: `"
				+ usage
				+ "`\n*[Filename]* = Name of Video file in Video Directory to add to Verification Queue\n*<-s>* = Option to execute current queue of verifications if video specified was properly added.\n:large_orange_diamond: __VIP only__\n:warning: Sometimes requires Confirmation since some functions are disabled while process runs.\n:anger: Cancellable: "
				+ prefix + "pause | " + prefix + "kill\n - All Extractions are halted while verifications take place";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean execute = false;
			String filename = null;
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
							case "-r":
								execute = true;
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
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Duplicate Filename! Do " + prefix + "help frame for proper usage");
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
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	public void addWipeDeletedVideosCommand(int commandTier, Limiter[] limits)
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
		String usage = prefix + name + " <-l>";
		String description = "Lists all Videos accessible to User that can be extracted from";
		String detailedDescription = "Lists all videos accessible to User with current privleges by Filename, Duration, and Framerate to assist with formulating extractions.\nUsage: `"
				+ usage + "`\n*<-l>* = :large_orange_diamond: __VIP argument only__ Lists literal files in folder rather then accessible list";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean literal = false;
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
							case "-l":
								literal = true;
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help frame for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Expecting a Flag! Do " + prefix + "help frame for proper usage");
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
			boolean isVIP = VIPLimiter.checkOperator(prop, ctx.getAuthor().getLongID()) || db.isUserVIP(ctx.getAuthor().getLongID());
			if (literal && isVIP)
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
					s.append("\n```\n");
					for (int x = 0; x < vlist.length; x++)
					{
						String nextLine = String.format("%3d", x + 1) + ". " + vlist[x].getName() + "\n";
						if ((s.length() + nextLine.length()) >= 1996)
						{
							//Send in Chunks
							s.append("```");
							final String stringPart = s.toString();
							RequestBuffer.request(() -> {
								return ctx.getChannel().sendMessage(stringPart);
							});
							s = new StringBuilder("```\n");
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
			else if (literal)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry: You must be a :large_orange_diamond: VIP to use the `-l` Flag");
				});
			}
			else
			{
				ArrayList<Video> videos = new ArrayList<Video>();
				if (!isVIP)
				{
					for (Video v : db.getVideoList())
					{
						if (!v.isRestricted() && v.isUsable())
							videos.add(v);
					}
				}
				else
					videos = db.getVideoList();
				StringBuilder s = new StringBuilder("```md\n");
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
				String path = "";
				for (String s : args)
				{
					path += s;
				}
				path = path.trim();
				if (path.startsWith("`") && path.endsWith("`"))
				{
					path = path.substring(1, path.length() - 2);
				}
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
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			String message;
			if (args.size() > 0)
				message = "Too many arguments!";
			else
				message = "- **Version:** " + DRI.version + "\n- **Author:** Christian77777\n" + "- **Programming Language:** Java\n"
						+ "- **Discord Connection Library:** Discord4J\n" + "- **Discord (Command) Library:** Commands4J";
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
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
		String usage = prefix + name + " [<-n SnowflakeID> or <-s Name>]";
		String description = ":customs: __Operator__: Marks a User as VIP.";
		String detailedDescription = "Gives a Discord User access to VIP commands and status across all Discord Servers\nUsage: `" + usage
				+ "`\n*[<-n SnowflakeID> or <-s Name>]* = Identify the User account to be added by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s Wumpus`\n:customs: __Operator only__";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isSnowflake = false;
			String id = null;
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
							case "-n":
								isSnowflake = true;
								id = assembleString(args);
								break;
							case "-s":
								isSnowflake = false;
								id = assembleString(args);
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help addVIPUser for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do " + prefix + "help addVIPUser for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help addVIPUser for proper usage");
				});
				return;
			}
			final String message;
			if (id != null)
			{
				IUser u;
				if (isSnowflake)
				{
					try
					{
						u = ctx.getGuild().getUserByID(Long.parseLong(id));
					}
					catch (NumberFormatException e)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Not a valid Snowflake ID!");
						});
						return;
					}
				}
				else
				{
					List<IUser> users = ctx.getGuild().getUsersByName(id);
					if (users.size() == 1)
						u = users.get(0);
					else if (users.size() == 0)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("No User with that name!");
						});
						return;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Too many Users with this Name!");
						});
						return;
					}
				}
				try
				{
					if (db.addNewVIP(u.getLongID()))
						message = u.mention() + " now VIP";
					else
						message = "Role Already Added!";
				}
				catch (NullPointerException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("No User with this snowflake ID!");
					});
					return;
				}
			}
			else
			{
				message = "No User Provided! Do " + prefix + "help addVIPUser for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
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
		String usage = prefix + name + " [<-n SnowflakeID> or <-s Name>]";
		String description = ":customs: __Operator__: Removes a User as VIP";
		String detailedDescription = "Removes a Discord User's access to VIP commands and status across all Discord Servers\nUsage: `" + usage
				+ "`\n*[<-n SnowflakeID> or <-s Name>]* = Identify the User account to be removed by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s Wumpus`\n:customs: __Operator only__";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isSnowflake = false;
			String id = null;
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
							case "-n":
								isSnowflake = true;
								id = assembleString(args);
								break;
							case "-s":
								isSnowflake = false;
								id = assembleString(args);
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help removeVIPUser for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do " + prefix + "help removeVIPUser for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help removeVIPUser for proper usage");
				});
				return;
			}
			final String message;
			if (id != null)
			{
				IUser u;
				if (isSnowflake)
				{
					try
					{
						u = ctx.getGuild().getUserByID(Long.parseLong(id));
					}
					catch (NumberFormatException e)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Not a valid Snowflake ID!");
						});
						return;
					}
				}
				else
				{
					List<IUser> users = ctx.getGuild().getUsersByName(id);
					if (users.size() == 1)
						u = users.get(0);
					else if (users.size() == 0)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("No User with that name!");
						});
						return;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Too many Users with this Name!");
						});
						return;
					}
				}
				try
				{
					if (db.removeVIP(u.getLongID()))
						message = u.mention() + " is no longer VIP";
					else
						message = "User was not already VIP!";
				}
				catch (NullPointerException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("No User with this snowflake ID!");
					});
					return;
				}
			}
			else
			{
				message = "No User Provided! Do " + prefix + "help removeVIPUser for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
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
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isSnowflake = false;
			String id = null;
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
							case "-n":
								isSnowflake = true;
								id = assembleString(args);
								break;
							case "-s":
								isSnowflake = false;
								id = assembleString(args);
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help addAdminRole for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do " + prefix + "help addAdminRole for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help addAdminRole for proper usage");
				});
				return;
			}
			final String message;
			if (id != null)
			{
				IRole r;
				if (isSnowflake)
				{
					try
					{
						r = ctx.getGuild().getRoleByID(Long.parseLong(id));
					}
					catch (NumberFormatException e)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Not a valid Snowflake ID!");
						});
						return;
					}
				}
				else
				{
					List<IRole> roles = ctx.getGuild().getRolesByName(id);
					if (roles.size() == 1)
						r = roles.get(0);
					else if (roles.size() == 0)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("No role with that name!");
						});
						return;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Too many roles with this Name!");
						});
						return;
					}
				}
				try
				{
					if (db.addNewAdminRole(r.getLongID(), ctx.getGuild().getLongID()))
						message = "Admin Role Added";
					else
						message = "Role Already Added!";
				}
				catch (NullPointerException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("No role with this snowflake ID!");
					});
					return;
				}
			}
			else
			{
				message = "No ID Provided! Do " + prefix + "help addAdminRole for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
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
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isSnowflake = false;
			String id = null;
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
							case "-n":
								isSnowflake = true;
								id = assembleString(args);
								break;
							case "-s":
								isSnowflake = false;
								id = assembleString(args);
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help removeAdminRole for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do " + prefix + "help removeAdminRole for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help removeAdminRole for proper usage");
				});
				return;
			}
			final String message;
			if (id != null)
			{
				IRole r;
				if (isSnowflake)
				{
					try//Shouldn't care if the role exists, because outdated roles will be removed on reboot
					{
						r = ctx.getGuild().getRoleByID(Long.parseLong(id));
					}
					catch (NumberFormatException e)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Not a valid Snowflake ID!");
						});
						return;
					}
				}
				else
				{
					List<IRole> roles = ctx.getGuild().getRolesByName(id);
					if (roles.size() == 1)
						r = roles.get(0);
					else if (roles.size() == 0)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("No role with that name!");
						});
						return;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Too many roles with this Name!");
						});
						return;
					}
				}
				try
				{
					if (db.removeAdminRole(r.getLongID()))
						message = "Admin Role Removed";
					else
						message = "Role not an Admin";
				}
				catch (NullPointerException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("No role with this snowflake ID!");
					});
					return;
				}
			}
			else
			{
				message = "No ID Provided! Do " + prefix + "help removeAdminRole for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
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
		String usage = prefix + name + " [<-n RoleID> or <-s RoleName>]";
		String description = ":a: __Admin__: White/Blacklists a Role to regulate Bot Usage.";
		String detailedDescription = "Change access to normal commands for a Role in the current server depending on Black/White listing mode by listing it.\nUsage: `"
				+ usage
				+ "`\n*[<-n RoleID> or <-s RoleName>]* = Identify the Server Role to be affected by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s ADMIN`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isSnowflake = false;
			String id = null;
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
							case "-n":
								isSnowflake = true;
								id = assembleString(args);
								break;
							case "-s":
								isSnowflake = false;
								id = assembleString(args);
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help addListedRole for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do " + prefix + "help addListedRole for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help addListedRole for proper usage");
				});
				return;
			}
			final String message;
			if (id != null)
			{
				IRole r;
				if (isSnowflake)
				{
					try
					{
						r = ctx.getGuild().getRoleByID(Long.parseLong(id));
					}
					catch (NumberFormatException e)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Not a valid Snowflake ID!");
						});
						return;
					}
				}
				else
				{
					List<IRole> roles = ctx.getGuild().getRolesByName(id);
					if (roles.size() == 1)
						r = roles.get(0);
					else if (roles.size() == 0)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("No role with that name!");
						});
						return;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Too many roles with this Name!");
						});
						return;
					}
				}
				boolean isBlacklist = db.checkGuildBlacklistMode(ctx.getGuild().getLongID());
				try
				{
					if (db.addNewUserRole(r.getLongID(), ctx.getGuild().getLongID(), isBlacklist))
					{
						if (isBlacklist)
							message = "Blacklist Role Added";
						else
							message = "Whitelist Role Added";
					}
					else
						message = "Role Already Added!";
				}
				catch (NullPointerException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("No role with this snowflake ID!");
					});
					return;
				}
			}
			else
			{
				message = "No ID Provided! Do " + prefix + "help addListedRole for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
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
		String usage = prefix + name + " [<-n RoleID> or <-s RoleName>]";
		String description = ":a: __Admin__: Unlists a Role that was previously White/Blacklisted.";
		String detailedDescription = "Change access to normal commands for a Role in the current server depending on Black/White listing mode by unlisting it.\nUsage: `"
				+ usage
				+ "`\n*[<-n RoleID> or <-s RoleName>]* = Identify the Server Role to be affected by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s ADMIN`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isSnowflake = false;
			String id = null;
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
							case "-n":
								isSnowflake = true;
								id = assembleString(args);
								break;
							case "-s":
								isSnowflake = false;
								id = assembleString(args);
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help removeListedRole for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do " + prefix + "help removeListedRole for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help removeListedRole for proper usage");
				});
				return;
			}
			final String message;
			if (id != null)
			{
				IRole r;
				if (isSnowflake)
				{
					try
					{
						r = ctx.getGuild().getRoleByID(Long.parseLong(id));
					}
					catch (NumberFormatException e)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Not a valid Snowflake ID!");
						});
						return;
					}
				}
				else
				{
					List<IRole> roles = ctx.getGuild().getRolesByName(id);
					if (roles.size() == 1)
						r = roles.get(0);
					else if (roles.size() == 0)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("No role with that name!");
						});
						return;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Too many roles with this Name!");
						});
						return;
					}
				}
				try
				{
					if (db.removeUserRole(r.getLongID()))
						message = "Listed Role Removed";
					else
						message = "Role not Listed!";
				}
				catch (NullPointerException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("No role with this snowflake ID!");
					});
					return;
				}
			}
			else
			{
				message = "No ID Provided! Do " + prefix + "help removeListedRole for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
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
					ctx.getChannel().sendMessage("Too Many Arguments! Do " + prefix + "help changeListingMode for proper usage");
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
		String usage = prefix + name + " [Tier (0-3)] <<-n ChannelID> or <-s Channel Name>>";
		String description = ":a: __Admin__: Changes a Channel Permission Tier.";
		String detailedDescription = "Changes the types of commands allowed in a Channel of a Server.\nUsage: `" + usage
				+ "`\n*[Tier (0-3)]* = The new Permission Tier, higher tiers allow the commands of the previous tier\n*<<-n ChannelID> or <-s Channel Name>>* = Identify a Channel in the Server OTHER then the current channel to be affected by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s bot-spam`\n:a: __Admin only__\n - Allowed in PM's to prevent lockout, but channel must be specified\n - Tier 0 = Complete Silence\n - Tier 1 = Basic Information Commands\n - Tier 2 = Normal usage\n - Tier 3 = Admin commands";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isSnowflake = false;
			String channel = null;
			int tier = -1;
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
							case "-n":
								isSnowflake = true;
								channel = assembleString(args);
								break;
							case "-s":
								isSnowflake = false;
								channel = assembleString(args);
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help changeChannelTier for proper usage");
								});
								return;
						}
					}
					else if (tier == -1)
					{
						int result = Integer.parseInt(arg);
						if (result >= 0 && result <= 3)
							tier = result;
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(
										"Invalid Tier Chosen! Only 0 <= tier <= 3 valid. Do " + prefix + "help changeChannelTier for proper usage");
							});
							return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Duplicate Tier specified! Do " + prefix + "help changeChannelTier for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help changeChannelTier for proper usage");
				});
				return;
			}
			if (tier == -1)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("No Tier Specified! Do " + prefix + "help changeChannelTier for proper usage");
				});
				return;
			}
			Long id;
			IGuild guild = ctx.getGuild();
			if (channel == null)
			{
				if (ctx.getChannel().isPrivate())
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("This is not a Discord Server Channel!");
					});
					return;
				}
				id = ctx.getChannel().getLongID();
			}
			else if (isSnowflake)
			{
				try
				{
					id = Long.parseLong(channel);
					IChannel ch;
					if (ctx.getChannel().isPrivate())
						ch = ctx.getClient().getChannelByID(id);
					else
						ch = ctx.getGuild().getChannelByID(id);
					if (ch == null)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Channel does not exist!");
						});
						return;
					}
					else if (ch.isPrivate())
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Channel is not part of a Guild!");
						});
						return;
					}
					if (ctx.getChannel().isPrivate())//Must do Admin Check now
					{
						IGuild channelGuild = ch.getGuild();
						if (AdminLimiter.checkServerOwner(channelGuild, ctx.getAuthor())
								|| AdminLimiter.checkAdmin(db, channelGuild, ctx.getAuthor()))
						{
							guild = channelGuild;
							id = ch.getLongID();
						}
						else//Not Admin of appropriate server
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(
										":no_entry: You must be an :a: Admin of the " + channelGuild.getName() + " Server to affect this channel");
							});
							return;
						}
					}
					else
						id = ch.getLongID();
				}
				catch (NumberFormatException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Channel ID provided not a valid SnowflakeID");
					});
					return;
				}
			}
			else
			{
				ArrayList<IChannel> channels = new ArrayList<IChannel>();
				for (IChannel e : ctx.getClient().getChannels())
				{
					if (e.getName().equals(channel))
						channels.add(e);
				}
				if (channels.size() == 1)
				{
					if (ctx.getChannel().isPrivate())//Must do Admin Check now
					{
						IGuild channelGuild = channels.get(0).getGuild();
						if (AdminLimiter.checkServerOwner(channelGuild, ctx.getAuthor())
								|| AdminLimiter.checkAdmin(db, channelGuild, ctx.getAuthor()))
						{
							guild = channelGuild;
							id = channels.get(0).getLongID();
						}
						else//Not Admin of appropriate server
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(
										":no_entry: You must be an :a: Admin of the " + channelGuild.getName() + " Server to affect this channel");
							});
							return;
						}
					}
					else
						id = channels.get(0).getLongID();
				}
				else if (channels.size() == 0)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("No Channel with that name!");
					});
					return;
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Too many Channels with this Name!");
					});
					return;
				}
			}
			if (db.updateChannelTier(id, guild.getLongID(), tier))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Tier Updated");
				});
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Channel Not Found in this Server!");
				});
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
		String usage = prefix + name + " [true or false] <<-n ChannelID> or <-s Channel Name>>";
		String description = ":a: __Admin__: Designate channel as Annoucement Channel.";
		String detailedDescription = "Toggle channel as annoucement channel. These channels might get impromptu messages regarding the bot specific to the Server or Global\nUsage: `"
				+ usage
				+ "`\n*[true or false]* = boolean to decide annoucement status\n*<<-n ChannelID> or <-s Channel Name>>* = Identify a Channel in the Server OTHER then the current channel to be affected by Name or Snowflake ID as declared by flag. Example `-n 389021830` or `-s bot-spam`\n:a: __Admin only__\n - true or false can be substituted by `y` and `n` or `1` and `0`";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			boolean isSnowflake = false;
			String channel = null;
			boolean annoucement = false;
			boolean annoucementMarked = false;
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
							case "-n":
								isSnowflake = true;
								channel = assembleString(args);
								break;
							case "-s":
								isSnowflake = false;
								channel = assembleString(args);
								break;
							default:
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Unknown Flag! Do " + prefix + "help changeChannelAnnoucement for proper usage");
								});
								return;
						}
					}
					else if (!annoucementMarked)
					{
						if (arg.equals("y") || arg.equals("true") || arg.equals("1"))
							annoucement = true;
						else if (arg.equals("n") || arg.equals("false") || arg.equals("0"))
							annoucement = false;
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Invalid Boolean Chosen! Expected true or false. Do " + prefix
										+ "help changeChannelAnnoucement for proper usage");
							});
							return;
						}
						annoucementMarked = true;
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel()
									.sendMessage("Duplicate Boolean specified! Do " + prefix + "help changeChannelAnnoucement for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do " + prefix + "help changeChannelAnnoucement for proper usage");
				});
				return;
			}
			if (!annoucementMarked)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("No Boolean Specified! Do " + prefix + "help changeChannelAnnoucement for proper usage");
				});
				return;
			}
			Long id;
			IGuild guild = ctx.getGuild();
			if (channel == null)
			{
				if (ctx.getChannel().isPrivate())
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("This is not a Discord Server Channel!");
					});
					return;
				}
				id = ctx.getChannel().getLongID();
			}
			else if (isSnowflake)
			{
				try
				{
					id = Long.parseLong(channel);
					IChannel ch;
					if (ctx.getChannel().isPrivate())
						ch = ctx.getClient().getChannelByID(id);
					else
						ch = ctx.getGuild().getChannelByID(id);
					if (ch == null)
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Channel does not exist!");
						});
						return;
					}
					else if (ch.isPrivate())
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Channel is not part of a Guild!");
						});
						return;
					}
					if (ctx.getChannel().isPrivate())//Must do Admin Check now
					{
						IGuild channelGuild = ch.getGuild();
						if (AdminLimiter.checkServerOwner(channelGuild, ctx.getAuthor())
								|| AdminLimiter.checkAdmin(db, channelGuild, ctx.getAuthor()))
						{
							guild = channelGuild;
							id = ch.getLongID();
						}
						else//Not Admin of appropriate server
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(
										":no_entry: You must be an :a: Admin of the " + channelGuild.getName() + " Server to affect this channel");
							});
							return;
						}
					}
					else
						id = ch.getLongID();
				}
				catch (NumberFormatException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Channel ID provided not a valid SnowflakeID");
					});
					return;
				}
			}
			else
			{
				ArrayList<IChannel> channels = new ArrayList<IChannel>();
				for (IChannel e : ctx.getClient().getChannels())
				{
					if (e.getName().equals(channel))
						channels.add(e);
				}
				if (channels.size() == 1)
				{
					if (ctx.getChannel().isPrivate())//Must do Admin Check now
					{
						IGuild channelGuild = channels.get(0).getGuild();
						if (AdminLimiter.checkServerOwner(channelGuild, ctx.getAuthor())
								|| AdminLimiter.checkAdmin(db, channelGuild, ctx.getAuthor()))
						{
							guild = channelGuild;
							id = channels.get(0).getLongID();
						}
						else//Not Admin of appropriate server
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(
										":no_entry: You must be an :a: Admin of the " + channelGuild.getName() + " Server to affect this channel");
							});
							return;
						}
					}
					id = channels.get(0).getLongID();
				}
				else if (channels.size() == 0)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("No Channel with that name!");
					});
					return;
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Too many Channels with this Name!");
					});
					return;
				}
			}
			if (db.updateChannelAnnoucement(id, guild.getLongID(), annoucement))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Annoucement Mode Updated");
				});
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Channel Not Found in this Server!");
				});
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
						ctx.getAuthor().getAvatarURL(), textTitle, textValue, inline);
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
			ArrayList<String> textValue, ArrayList<Boolean> inline)
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
			message.withFooterText("Showing " + fieldCount + " Entries - Page " + (z + 1) + "/" + pageCount);
			RequestBuffer.request(() -> {
				return ctx.getChannel().sendMessage(message.build());
			});
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
