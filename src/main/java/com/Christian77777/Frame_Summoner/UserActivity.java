
package com.Christian77777.Frame_Summoner;

import java.awt.Color;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import javax.script.SimpleBindings;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database.DBChannel;
import com.Christian77777.Frame_Summoner.Database.DBGuild;
import com.Christian77777.Frame_Summoner.Database.DBLink;
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
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IMessage.Attachment;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageHistory;
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
	public static ReactionEmoji ok = ReactionEmoji.of(new String(Character.toChars(127383)));
	public static ReactionEmoji begin = ReactionEmoji.of(new String(Character.toChars(10035)));
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
		//BOOKMARK Command Directory
		//Debug Commands
		addDebugCommand();
		addUpdateDBCommand();
		addTestTimecodeCommand();
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
		addVerifyNewVideosCommand(2, new Limiter[]
		{ tier3L, vipL });
		addRemoveVideosCommand(2, new Limiter[]
		{ tier3L, vipL });
		addRestrictCommand(2, new Limiter[]
		{ tier3L, vipL });
		addSetUserUsage(2, new Limiter[]
		{ tier3L, vipL });
		addSetServerUsage(2, new Limiter[]
		{ tier3L, vipL });
		addDisableServerCommand(2, new Limiter[]
		{ tier3L, vipL });
		addCreateLinkCommand(2, new Limiter[]
		{ tier3L, vipL });
		addDeleteLinkCommand(2, new Limiter[]
		{ tier3L, vipL });
		addSetOffsetCommand(2, new Limiter[]
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
		{ privateAdminL });//Allowed in PMs, Admin Check must be done in program if Private Channel
		addChangeChannelAnnoucementCommand(1, new Limiter[]
		{ tier3L, adminL });
		addSetServerLimit(1, new Limiter[]
		{ tier3L, adminL });
		//Extraction Commands
		addFrameCommand(0, new Limiter[]
		{ tier2L, listedL });
		//Query Commands
		addListCommand(0, new Limiter[]
		{ tier2L, listedL });
		addFetchLinkCommand(0, new Limiter[]
		{ tier1L, listedL });
		//Generic Commands
		addInfoCommand(0, new Limiter[]
		{ tier1L, listedL });
		addGuideCommand(0, new Limiter[]//Fallback to PM if Channel is Tier 0
		{});
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

	private void addTestTimecodeCommand()
	{
		Builder b = Command.builder();
		Command timecode = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			if (result.length == 1)
			{
				try
				{
					long time = Extractor.convertTimeToMilli(result[0], null, null);
					RequestBuffer.request(() -> {
						ctx.getChannel()
								.sendMessage("Timecode parsed to : `" + time + "` back into String: `" + Extractor.convertMilliToTime(time) + "`");
					}).get();
				}
				catch (IllegalArgumentException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(e.getMessage());
					}).get();
				}
			}
		}).build();
		registry.register(timecode, "tc");
	}

	private void addUpdateDBCommand()
	{
		Builder b = Command.builder();
		b.limiter(new UserLimiter(163810952905490432L));
		Command updateDB = b.onCalled(ctx -> {
			String command = ctx.getMessage().getFormattedContent().substring(prefix.length() + 9, ctx.getMessage().getFormattedContent().length());
			logger.debug("SQL Command: >{}", command);
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
	 * Not for Production use
	 */
	private void addBackupChannelCommand()
	{
		Builder b = Command.builder();
		b.limiter(new UserLimiter(163810952905490432L));
		Command backup = b.onCalled(ctx -> {
			String channel = ctx.getChannel().getName();
			try (Connection dbConnection = DriverManager.getConnection("jdbc:sqlite:" + DRI.dir + File.separator + "ChannelBackup.db");)
			{
				Statement stmt = dbConnection.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%';");
				while (rs.next())
				{
					if (rs.getString(1).equals(channel))
					{
						if (confirmAction(ctx, "Duplicate Table found, do you want to overwrite it?"))
							stmt.executeUpdate("DROP TABLE \"" + channel + "\";");
						else
							return;
					}
				}
				Instant start = Instant.now();
				if (ctx.getClient().getOurUser().getPermissionsForGuild(ctx.getGuild()).contains(Permissions.MANAGE_MESSAGES))
				{
					RequestBuffer.request(() -> {
						ctx.getMessage().delete();
					}).get();
				}
				RequestBuffer.request(() -> {
					ctx.getChannel()
							.sendMessage("Backing up Channel: `" + ctx.getChannel().getName() + "` in Server: `" + ctx.getGuild().getName() + "`");
				}).get();
				MessageHistory history = ctx.getChannel().getFullMessageHistory();
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Total of `" + history.size() + "` Messages");
				}).get();
				stmt.executeUpdate("CREATE TABLE \"" + channel
						+ "\" (\"Rank\" INTEGER NOT NULL, \"CreationDate\" TEXT NOT NULL, \"AuthorName\" TEXT NOT NULL, \"AuthorID\" INTEGER NOT NULL, \"Content\"	TEXT, \"Attachments\" TEXT, PRIMARY KEY(\"Rank\"))");
				PreparedStatement ps = dbConnection.prepareStatement("INSERT INTO \"" + channel + "\" VALUES (?,?,?,?,?,?);");
				DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.US)
						.withZone(ZoneId.systemDefault());
				dbConnection.setAutoCommit(false);
				int batchVarCount = 0;
				for (int x = history.size() - 1; x >= 0; x--)
				{
					//Avoid Batch Var Limit of 999 in SQLite Driver
					if (batchVarCount >= 994)
					{
						ps.executeBatch();
						batchVarCount = 0;
					}
					IMessage m = history.get(x);
					ps.setInt(1, history.size() - x);
					ps.setString(2, formatter.format(m.getCreationDate()));
					ps.setString(3, m.getAuthor().getName());
					ps.setLong(4, m.getAuthor().getLongID());
					String content = m.getContent();
					System.out.println(content);
					if (content == null || content.isEmpty())
						ps.setNull(5, Types.VARCHAR);
					else
					{
						try
						{
							ps.setString(5, m.getFormattedContent());
						}
						catch (NullPointerException e1)
						{
							ps.setString(5, m.getContent());
						}
					}
					List<Attachment> attachmentList = m.getAttachments();
					if (attachmentList.size() > 0)
					{
						String attachmentString = "";
						for (Attachment a : attachmentList)
						{
							attachmentString += a.getUrl() + " ";
						}
						attachmentString = attachmentString.substring(0, attachmentString.length() - 1);
						ps.setString(6, attachmentString);
					}
					else
						ps.setNull(6, Types.VARCHAR);
					ps.addBatch();
					batchVarCount += 5;
				}
				if (batchVarCount > 0)
					ps.executeBatch();
				dbConnection.commit();
				dbConnection.close();
				Duration time = Duration.between(start, Instant.now());
				String timeLength = Extractor.convertMilliToTime((time.getSeconds() * 1000) + ((long) ((time.getNano() * .000001) % 1000)));
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Backup **COMPLETE**\nTook `" + timeLength + "` long to backup");
				}).get();
			}
			catch (SQLException e)
			{
				logger.catching(e);
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Backup **FAILED**: SQLException, check Console");
				}).get();
			}
			catch (Exception e)
			{
				logger.catching(e);
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Backup **FAILED**: Generic Exception, check Console");
				}).get();
			}
		}).build();
		registry.register(backup, "backupChannelTangoContingency");
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
		{ "extract", "f" };
		String usage = prefix + name + " <-f Frame Number> <-o> [Filename] [Timecode]";
		String description = "Extracts frame from video and uploads to Discord";
		String detailedDescription = "Extracts a Frame from specified video at the timecode and uploads it to Discord if authorized.\nUsage: `"
				+ usage
				+ "`\n*[Filename]* = Name of Video file validated in Database, use fs!list to see avaliable files\n*[Timecode]* = Timecode specifying which frame to extract, in this format `##:##:##.###`, hours, minutes, milliseconds optional\n*<-f Frame Number>* = Increment the frame selected by the timecode, easier way to be precise.\n*<-o>* = Ignore video link offset. __Advanced__\n - Several Restrictions on command to prevent abuse";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(Option.builder("o").desc("Ignores Preset Offset when used").longOpt("offset").build());
		options.addOption(Option.builder("f").argName("Number").desc("Skip Frames to find actual value").hasArg().longOpt("frames").build());
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, true);
				try
				{
					String[] required = line.getArgs();
					if (required.length == 2)
					{
						boolean ignoreOffset = line.hasOption('o');
						Integer frameCount = null;
						if (line.hasOption('f'))
						{
							frameCount = Integer.valueOf(line.getOptionValue('f'));
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
						try
						{
							Extractor.convertTimeToMilli(required[1], null, null);
						}
						catch (IllegalArgumentException e)
						{
							logger.error(e);
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":octagonal_sign: " + e.getMessage());
							});
							return;
						}
						long timecheck = Extractor.convertTimeToMilli(required[1], null, null);
						extractor.requestFrame(ctx, required[0], required[1], frameCount, !ignoreOffset);
					}
					else if (required.length > 2)
					{
						final String response = ":octagonal_sign: Too many unmarked arguments! Arguments past: `" + required[1] + "` causing error. Flags may only come before unmarked arguments\nDo `" + prefix + "help` frame for proper usage";
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(response);
						});
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(":octagonal_sign: Not Enough Arguments! Do " + prefix + "help frame for proper usage");
						});
					}
				}
				catch (NumberFormatException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: The `-f` flag argument is not a valid integer!");
					});
				}
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addVerifyNewVideosCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "verifyNew";
		String[] aliases = new String[]
		{ "addVideos" };
		String usage = prefix + name;
		String description = ":large_orange_diamond: __VIP__: Scan for new video files to verify.";
		String detailedDescription = "Scans the current Video Directory, for videos not already in the Database, to attempt verification and inclusion.\nUsage: `"
				+ usage + "`\n:large_orange_diamond: __VIP only__\n:warning: Requires Confirmation since data will be potentially accessible.";
		detailedDescription += appendAliases(aliases);
		Command command = b.onCalled(ctx -> {
			if (confirmAction(ctx, "Would you like to scan for new Videos and mark now inaccessible videos?"))
			{
				//Find all files not already in database, and attempt verification
				String[] realFiles = new File(prop.getProperty("Video_Directory")).list();
				Arrays.sort(realFiles, null);
				ArrayList<DBVideo> cache = db.getVideoList();
				ArrayList<DBVideo> unusable = new ArrayList<DBVideo>();
				ArrayList<String> newFiles = new ArrayList<String>();
				if (realFiles.length != 0 && !cache.isEmpty())
				{
					int lIndex = 0;
					int rIndex = 0;
					//Iterate through both sorted arrays for any match
					while (cache.size() > lIndex && realFiles.length > rIndex)
					{
						int comparision = cache.get(lIndex).getFilename().compareTo(realFiles[rIndex]);
						if (comparision > 0)//cache missing item from realFiles
						{
							newFiles.add(realFiles[rIndex]);
							rIndex++;
						}
						else if (comparision < 0)//File indexed in Database now missing!
						{
							if (cache.get(lIndex).isUsable())
							{
								unusable.add(cache.get(lIndex));
							}
							lIndex++;
						}
						else
						{
							if (!cache.get(lIndex).isUsable())
							{
								newFiles.add(realFiles[rIndex]);
							}
							lIndex++;
							rIndex++;
						}
						//else, good
					}
				}
				else if (realFiles.length == 0)//Likely file path accidently modified
					unusable = cache;
				else//Likely Fresh Database, Database has no videos
					unusable = null;
				if (unusable == null)
				{
					logger.warn("Database is fresh and has no videos, will attempt verification on all of them");
				}
				else if (cache.size() == unusable.size())
				{
					logger.info("Database record is completely different from actual Video Directory, maybe double check the file path? \"{}\"",
							prop.getProperty("Video_Directory"));
					for (DBVideo s : unusable)
					{
						db.setVideoUnusable(s.getNickname(), false);
					}
				}
				else
				{
					for (DBVideo s : unusable)
					{
						db.setVideoUnusable(s.getNickname(), false);
					}
				}
				for (String filename : newFiles)
				{
					extractor.requestSingleVerification(ctx, filename);
				}
				extractor.beginVerifications(ctx.getChannel());
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
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
			if (confirmAction(ctx, "Would you like to temporarly Disable the Extractor, and Authorize extraction of New Videos?"))
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
		String usage = prefix + name + " [-u number] [<-n Username> or <-s SnowflakeID>]";
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
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
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
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: '" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addDisableServerCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "disableServer";
		String[] aliases = new String[]
		{ "pauseServer" };
		String usage = prefix + name + " <-u> <<-n GuildName> or <-s SnowflakeID>>";
		String description = ":large_orange_diamond: __VIP__: Disable a Server's access to Frame Extractions";
		String detailedDescription = "Changes the number of extractions executed by a Server today. Useful for giving some Servers more extractions. \nUsage: `"
				+ usage
				+ "`\n*[-u number]* = The new number of times the User used the bot today\n*<<-n GuildName> or <-s SnowflakeID>>* = Identify the User account to be added by Name or Snowflake ID as declared by flag. Otherwise the current server is affected. Example `-n Wumpus` or `-s 389021830`\n:large_orange_diamond: __VIP only__\n - VIPs are always immune to this restriction";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(Option.builder("u").desc("New Daily Extraction Limit.").hasArg(false).longOpt("undo").build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("GuildName").desc("Name of the Guild").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Discord Object").hasArg().longOpt("snowflake").build());
		g1.setRequired(false);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				boolean enable = line.hasOption('u');
				IGuild guild = parseGuild(ctx, line);
				String newMode = enable ? ":large_blue_circle: Online" : ":red_circle: Offline";
				String action = enable ? ":large_blue_circle: Enable" : ":red_circle: Disable";
				//Global Change
				if (guild != null)
				{
					if (confirmAction(ctx,
							"Are you sure you want to " + action + " access to Frame Extractions for `" + ctx.getGuild().getName() + "` Server?"))
					{
						if (db.updateServerStanding(enable, guild.getLongID()))
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(guild.getName() + " Server is now " + newMode);
							});
						}
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":radioactive: No Guild was found?");
							});
						}
					}
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addSetServerLimit(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "setServerLimit";
		String[] aliases = new String[]
		{ "modifyServerLimit", "changeServerLimit", "ssl", "msl", "csl" };
		String usage = prefix + name + " [-l limit] <<-n GuildName> or <-s SnowflakeID>>";
		String description = ":a: __Admin__: Changes the limit of how many times a Server can extract Frames in 24 hours.";
		String detailedDescription = "Resets the number of extractions executed by a Server today. Useful for awarding more extractions. \nUsage: `"
				+ usage
				+ "`\n*[-l limit]* = The new limit of times the Server can use the bot in 24 hours\n*<<-n GuildName> or <-s SnowflakeID>>* = Identify the Server to be affected by Name or Snowflake ID as declared by flag. Otherwise affects the current Server. Example `-n Server` or `-s 123456789`\n:large_orange_diamond: __VIP only__\n - VIP extractions will not affect this usage";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(Option.builder("l").argName("Number").desc("New Daily Extraction Limit.").hasArg().longOpt("limit").required().build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("GuildName").desc("Name of the Guild").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Discord Object").hasArg().longOpt("snowflake").build());
		g1.setRequired(false);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				try
				{
					int limit = Integer.parseInt(line.getOptionValue('l'));
					IGuild guild = parseGuild(ctx, line);
					//Global Change
					if (guild != null)
					{
						int max = Integer.valueOf(prop.getProperty("MaxServerExtracts"));
						if (limit < max)
						{
							if (db.updateServerDailyLimit(limit, guild.getLongID()))
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage("Daily Limit now at `" + limit + "` for " + guild.getName());
								});
							}
							else
							{
								RequestBuffer.request(() -> {
									ctx.getChannel().sendMessage(":radioactive: No Guild was found?");
								});
							}
						}
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":no_entry_sign: Value selected is higher then the enforced max of  `" + max
										+ "` Frames per day. Daily Limit was unchanged.");
							});
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
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
		String usage = prefix + name + " [-u number] <<-n GuildName> or <-s SnowflakeID>>";
		String description = ":large_orange_diamond: __VIP__: Changes how many times a Server has extracted Frames today.";
		String detailedDescription = "Changes the number of extractions executed by a Server today. Useful for giving some Servers more extractions. \nUsage: `"
				+ usage
				+ "`\n*[-u number]* = The new number of times the User used the bot today\n*<<-n GuildName> or <-s SnowflakeID>>* = Identify the User account to be added by Name or Snowflake ID as declared by flag. Otherwise the current server is affected. Example `-n Wumpus` or `-s 389021830`\n:large_orange_diamond: __VIP only__\n - VIPs are always immune to this restriction";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(
				Option.builder("u").argName("Number").desc("New number of extractions made today.").hasArg().longOpt("usage").required().build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("GuildName").desc("Name of the Guild").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Discord Object").hasArg().longOpt("snowflake").build());
		g1.setRequired(false);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
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
									ctx.getChannel().sendMessage(":radioactive: Server was not found?");
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
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
		String usage = prefix + name + " <-u> [<-a> or <-s Video Name>]";
		String description = ":large_orange_diamond: __VIP__: Restricts or unrestricts a video to VIP only access.";
		String detailedDescription = "Restrict or Unrestrict a Video. Restricted videos do not exist to normal Users. VIP Users can see them, and interact with them.\nUsage: `"
				+ usage
				+ "`\n*<-u>* = Inverts the command, making the listed videos normally accessible\n*[-a or -s]* = Specifies to either affect ALL videos, or just the ones specified. Can specify more then one with -s\n:large_orange_diamond: __VIP only__\n - All Extractions temporarily halted while verifications take place";
		detailedDescription += appendAliases(aliases);
		Options o = new Options();
		o.addOption("u", "undo", false, "If the Video(s) should be unrestricted instead");
		OptionGroup g1 = new OptionGroup();
		g1.addOption(new Option("a", "all", false, "Modifies ALL Videos in Record"));
		g1.addOption(Option.builder("s").argName("Video name").desc("Modifies only the listed videos").hasArgs().longOpt("single").build());
		g1.setRequired(true);
		o.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
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
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addRemoveVideosCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "removeVideo";
		String[] aliases = new String[]
		{ "deleteVideo" };
		String usage = prefix + name + " [-n Video Name]";
		String description = ":large_orange_diamond: __VIP__: Completely removes a video from the Database";
		String detailedDescription = "Removes a video from the database, and disassociates dependent data. Only way to remove unusable entries. Extraction Record is untouched.\nUsage: `"
				+ usage
				+ "`\n*[-n Video Name]* = Identify the Video to remove, use `fs!list` to see what can be removed\n:large_orange_diamond: __VIP only__\n:warning: Requires Confirmation since data will be potentially removed.";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(
				Option.builder("n").argName("VideoName").desc("Name of Video to Remove from Database").hasArg().longOpt("name").required().build());
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
				{
					if (confirmAction(ctx, "Would you like to remove the video \"" + line.getOptionValue('n') + "\" if it exists?"))
					{
						if (db.removeVideo(line.getOptionValue("n")))
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":eject: Video was Removed");
							});
						}
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":warning: Video was not found");
							});
						}
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
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
		g1.setRequired(false);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
							s.append(String.format("%4d", videos.size()) + " " + (line.hasOption('r') ? "Restricted" : "Accessible")
									+ " Videos  | Duration     | Linked | FPS\n");
							s.append("------------------------------------------------------------\n");
							for (int x = 0; x < videos.size(); x++)
							{
								DBVideo v = videos.get(x);
								StringBuilder entry = new StringBuilder();
								if (!v.isUsable())
									entry.append(" * ");
								else if (v.isRestricted())
									entry.append(" < ");
								else
									entry.append(" | ");
								entry.append(String.format("%-20s", v.getNickname().trim().substring(0, Math.min(20, v.getNickname().length()))));
								entry.append(" | ");
								entry.append(Extractor.convertMilliToTime(v.getLength()));
								if (!v.isUsable())
									entry.append(" * ");
								else if (v.isRestricted())
									entry.append(" > ");
								else
									entry.append(" | ");
								if (v.isLinked())
									entry.append("<Link>");
								else
									entry.append("------");
								if (!v.isUsable())
									entry.append(" * ");
								else if (v.isRestricted())
									entry.append(" < ");
								else
									entry.append(" | ");
								entry.append(String.format("%-4s", v.getFps()));
								if (!v.isUsable())
									entry.append(" * ");
								else if (v.isRestricted())
									entry.append(" > ");
								else
									entry.append(" | ");
								String nextLine = entry.toString();
								if ((s.length() + nextLine.length()) >= 1996)
								{
									//Send in Chunks to avoid 2000 char limit
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
					DRI.menu.restartUI();
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
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
				{
					if (line.hasOption('s'))
					{
						ArrayList<String> textTitle = new ArrayList<String>();
						ArrayList<String> textValue = new ArrayList<String>();
						ArrayList<Boolean> inline = new ArrayList<Boolean>();
						//Field Titles: 50
						//Field Values: 175
						DBGuild g = db.getServerData(ctx.getGuild().getLongID());
						String mode = g.isBlacklistMode() ? ":black_large_square: Blacklist" : ":white_large_square: Whitelist";
						textTitle.add("Operating Mode");
						textValue.add(g.isEnabled() ? ":large_blue_circle: Online" : ":red_circle: Offline");
						inline.add(true);
						textTitle.add("Role Listing Mode");
						textValue.add(mode);
						inline.add(true);
						textTitle.add("Daily Server Request Limit");
						String max = g.getRequestLimit() == Integer.valueOf(prop.getProperty("MaxServerExtracts")) ? ":arrow_up: " : "";
						textValue.add(max + g.getRequestLimit() + " requests");
						inline.add(true);
						textTitle.add("Today's Extraction Total");
						max = g.getRequestLimit() == g.getUsedToday() ? ":arrow_up: " : "";
						textValue.add(max + g.getUsedToday() + " requests");
						inline.add(true);
						textTitle.add(String.valueOf('\u200B'));
						textValue.add(String.valueOf('\u200B'));
						inline.add(false);
						String word = new String("");
						for (Long l : db.getListOfAdminRoles(ctx.getGuild().getLongID()))
						{
							word += ctx.getGuild().getRoleByID(l).getName() + "\n";
						}
						word = word.length() == 0 ? "<" + ctx.getGuild().getOwner().mention() + ">" : word.substring(0, word.length() - 1);
						textTitle.add(":a: Administrator Roles");
						textValue.add(word);
						inline.add(true);
						word = new String("");
						for (Long l : db.getListOfUserRoles(ctx.getGuild().getLongID()))
						{
							word += ctx.getGuild().getRoleByID(l).getName() + "\n";
						}
						word = word.length() == 0 ? (":small_red_triangle_down:") : word.substring(0, word.length() - 1);
						textTitle.add(mode + "ed Roles");
						textValue.add(word);
						inline.add(true);
						textTitle.add(String.valueOf('\u200B'));
						textValue.add(String.valueOf('\u200B'));
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
						tier3 = tier3.length() == 0 ? ":small_red_triangle_down:" : tier3.substring(0, tier3.length() - 1);
						tier2 = tier2.length() == 0 ? ":small_red_triangle_down:" : tier2.substring(0, tier2.length() - 1);
						tier1 = tier1.length() == 0 ? ":small_red_triangle_down:" : tier1.substring(0, tier1.length() - 1);
						tier3 = tier3.length() >= 300 ? tier3.substring(0, 297) + "..." : tier3;
						tier2 = tier2.length() >= 300 ? tier2.substring(0, 297) + "..." : tier2;
						tier1 = tier1.length() >= 300 ? tier1.substring(0, 297) + "..." : tier1;
						textTitle.add(":a: Admin Channels");
						textValue.add(tier3);
						inline.add(true);
						textTitle.add(":loud_sound: Full Usage Channels");
						textValue.add(tier2);
						inline.add(true);
						textTitle.add(":information_source: Info Only Channels");
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
		String usage = prefix + name + " [<-n RoleName> or <-s SnowflakeID>]";
		String description = ":a: __Admin__: Adds a Role as Admin for this Server.";
		String detailedDescription = "Gives access to Admin commands for a Role in the current server\nUsage: `" + usage
				+ "`\n*[<-n RoleName> or <-s SnowflakeID>]* = Identify the Server Role to be affected by RoleName or Snowflake ID as declared by flag. Example `-n ADMIN` or `-s 123456789`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Role Name").desc("Discord Role Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
		String usage = prefix + name + " [<-n RoleName> or <-s SnowflakeID>]";
		String description = ":a: __Admin__: Removes a Role as Admin for this Server.";
		String detailedDescription = "Removes access to Admin commands for a Role in the current server\nUsage: `" + usage
				+ "`\n*[<-n RoleName> or <-s SnowflakeID>]* = Identify the Server Role to be affected by RoleName or SnowflakeID as declared by flag. Example `-n ADMIN` or `-s 123456789`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Role Name").desc("Discord Role Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
				+ "`\n*[<-n RoleName> or <-s SnowflakeID>]* = Identify the Server Role to be affected by Name or Snowflake ID as declared by flag. Example `-n ADMIN` or `-s 123456789`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("RoleName").desc("Discord Role Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
				+ "`\n*[<-n RoleName> or <-s SnowflakeID>]* = Identify the Server Role to be affected by Name or Snowflake ID as declared by flag. Example `-n ADMIN` or `-s 123456789`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("RoleName").desc("Discord Role Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
				+ "`\n*[-t Tier (0-3)]* = The new Permission Tier, higher tiers allow the commands of the previous tier. Example: `-t 3`\n*<<-n ChannelName> or <-s SnowflakeID>>* = Identify a Channel in the Server OTHER then the current channel to be affected by Name or Snowflake ID as declared by flag. Example `-n bot-spam` or `-s 123456789`\n:a: __Admin only__\n - Allowed in PM's, but channel then must be specified\n - Tier 0 = Complete Silence\n - Tier 1 = Basic Information Commands\n - Tier 2 = Normal usage\n - Tier 3 = Admin commands";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(Option.builder("t").argName("Number").desc("Channel Permission Tier [0-3]").hasArg().longOpt("tier").required().build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Channel Name").desc("Discord Channel Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
		String detailedDescription = "Toggle channel as annoucement channel. These channels occasionally will get status messages like `Bot online` and such\nUsage: `"
				+ usage
				+ "`\n*<-u>* = Inverts Command to clear Annoucement status\n*<<-n ChannelName> or <-s SnowflakeID>>* = Identify a Channel in the Server OTHER then the current channel to be affected by Name or Snowflake ID as declared by flag. Example `-n bot-spam` or `-s 123456789`\n:a: __Admin only__";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(Option.builder("u").desc("Channel Permission Tier [0-3]").hasArg(false).longOpt("undo").build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("Channel Name").desc("Discord Channel Name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("s").argName("SnowflakeID").desc("Snowflake ID of the Channel").hasArg().longOpt("snowflake").build());
		g1.setRequired(false);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addCreateLinkCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "createLink";
		String[] aliases = new String[]
		{};
		String usage = prefix + name + " [-l Link] [-e Episode Name] <-n Video Name> <-d Description>";
		String description = ":large_orange_diamond: __VIP__: Create's a fetchable link to a YouTube video.";
		String detailedDescription = "Stores a link to a YouTube video, likely mirrored as a local file avaliable for Frame Extraction.\nUsage: `"
				+ usage
				+ "`\n*[-l Link]* = Youtube Link to serve on request.\n*[-e Episode Name]* = Episode name to associate with the link. Example `-e Teddygozilla`\n*<-e Episode Name>* = Local Video in Database to associate with the Link. Example `-n EP01`\n*<-d Description>* = 480 character Description to associate with the Link and Episode name\n - Recreating already stored links, will just overwrite the associated data";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(
				Option.builder("l").argName("Youtube Link").desc("Link of the Corrisponding video").hasArg().required().longOpt("link").build());
		options.addOption(Option.builder("n").argName("VideoName").desc("Video in Database to associate link with").hasArg().longOpt("name").build());
		options.addOption(Option.builder("e").argName("EpisodeName").desc("Name of the Episode from " + prop.getProperty("Content_Name")).hasArg()
				.required().longOpt("episode").build());
		options.addOption(Option.builder("d").argName("480char-Description").desc("Description of Episode").hasArg().longOpt("description").build());
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
				{
					String link = line.getOptionValue('l');
					boolean validLink = false;
					try
					{
						new URL(link).toURI();
						//https://youtu.be/<code><?t=##>
						validLink = link.substring(0, 17).equals("https://youtu.be/") ? true : false;
					}
					catch (MalformedURLException | URISyntaxException | IndexOutOfBoundsException e)
					{
						//Boolean already false
					}
					if (validLink)
					{
						if (db.createLink(link, line.getOptionValue('n'), line.getOptionValue('e'), line.getOptionValue('d')))
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Link Stored");
							});
						}
						else
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":radioactive: SQLExecption, could not store link");
							});
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(":octagonal_sign: Invalid youtu.be link provided!: `" + link + "`");
						});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addFetchLinkCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "link";
		String[] aliases = new String[]
		{ "l" };
		String usage = prefix + name + " [<-n Video Name> or <-e Episode Name>] <-t start time>";
		String description = "Links to a YouTube video.";
		String detailedDescription = "Posts a link to a YouTube video, likely mirrored as a local file ripe for Frame Extraction.\nUsage: `" + usage
				+ "`\n*[<-n Video Name> or <-e Episode Name>]* = Identify the Link to be retrieved by Video Name or Episode Name as declared by flag. Example `-n EP01` or `-e Teddygozilla`\n*<-t timecode>* = Seek to a certain time code in the Video automatically";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(
				Option.builder("t").argName("<##:><##:><#>#<.###>").desc("Timecode to seek to in the video.").hasArg().longOpt("timecode").build());
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("n").argName("VideoName").desc("Searches for Link in Database by Video name").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("e").argName("EpisodeName").desc("Name of the Episode from " + prop.getProperty("Content_Name")).hasArg()
				.longOpt("episode").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				long time = 0;
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
				{
					if (line.hasOption('t'))
					{
						try
						{
							time = Extractor.convertTimeToMilli(line.getOptionValue('t'), null, null);
						}
						catch (IllegalArgumentException e)
						{
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(":warning: " + e.getMessage());
							});
						}
					}
					ArrayList<DBLink> links = null;
					boolean isVIP = VIPLimiter.checkOperator(prop, ctx.getAuthor().getLongID()) || db.isUserVIP(ctx.getAuthor().getLongID());
					if (line.hasOption('n'))
						links = db.getLink(line.getOptionValue('n'), false, isVIP);
					else
						links = db.getLink(line.getOptionValue('e'), true, isVIP);
					if (links.size() > 0)
					{
						if (links.size() == 1)
						{
							DBLink data = links.get(0);
							String linkText = data.getLink() + (line.hasOption('t') ? "?t=" + (time / 1000) : "");
							EmbedBuilder embed = new EmbedBuilder();
							embed.withAuthorName("FoxTrot Fanatics");
							embed.withAuthorUrl("https://foxtrotfanatics.info");
							embed.withAuthorIcon("https://media.foxtrotfanatics.info/i/ftf_logo.png");
							embed.withTitle(data.getTitle());
							if (data.getDescription() != null)
								embed.withDesc(data.getDescription());
							embed.withUrl(linkText);
							embed.withColor(new Color(255, 0, 0));
							if (data.getNickname() != null)
								embed.appendField("Video File", data.getNickname(), true);
							if (line.hasOption('t'))
								embed.appendField("Start Time", Extractor.convertMilliToTime(time), true);
							embed.withFooterIcon(ctx.getAuthor().getAvatarURL());
							embed.withFooterText("Requested By: \"" + ctx.getAuthor().getDisplayName(ctx.getGuild()) + "\"");
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(embed.build());
							}).get();
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(linkText);
							}).get();
						}
						else
						{
							String tmp = "Found Multiple Videos\n---------------------------------------------------------------------\n";
							for (DBLink s : links)
							{
								tmp += "<" + s.getLink() + (line.hasOption('t') ? "?t=" + (time / 1000) : "") + "> - " + s.getTitle() + "\n";
							}
							final String linkText = tmp;
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage(linkText);
							});
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(":warning: No Link Found!");
						});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addDeleteLinkCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "deleteLink";
		String[] aliases = new String[]
		{};
		String usage = prefix + name + " [<-l Link> or <-n Video Name> or <-e Episode Name>]";
		String description = ":large_orange_diamond: __VIP__: Delete link to a YouTube video.";
		String detailedDescription = "Deletes a link from the Database.\nUsage: `" + usage
				+ "`\n*<-l Link>* = Youtube Link in Database.\n*<-n Video Name>* = Video Name to remove from Database. Example `-n EP01`\n*<-e Episode Name>* = Episode name to remove from Database\n";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		OptionGroup g1 = new OptionGroup();
		g1.addOption(Option.builder("l").argName("Youtube Link").desc("Link of the Corrisponding video").hasArg().longOpt("link").build());
		g1.addOption(Option.builder("n").argName("VideoName").desc("Video in Database to associate link with").hasArg().longOpt("name").build());
		g1.addOption(Option.builder("e").argName("EpisodeName").desc("Name of the Episode from " + prop.getProperty("Content_Name")).hasArg()
				.longOpt("episode").build());
		g1.setRequired(true);
		options.addOptionGroup(g1);
		Command command = b.onCalled(ctx -> {
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
				{
					if (db.deleteLink(line.getOptionValue('l'), line.getOptionValue('n'), line.getOptionValue('e')))
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Link Removed");
						});
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Could not find reference to Link");
						});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addSetOffsetCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "setOffset";
		String[] aliases = new String[]
		{ "updateOffset", "so" };
		String usage = prefix + name + " [-n Video Name] [-o Offset]";
		String description = ":large_orange_diamond: __VIP__: Add default offset to video";
		String detailedDescription = "Adds a default offset to a Video file for every normal extraction. Used to sync local video with an external source. Can be bypassed with a -o flag in the Frame command.\nUsage: `"
				+ usage + "`\n*[-n Video Name]* = Video file in Database to modify.\n*[-o Offset]* = Timecode offset to add to video";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		options.addOption(Option.builder("o").argName("##:##:##.###").desc("Timecode to stand for length of time").hasArg().longOpt("offset")
				.required().build());
		options.addOption(Option.builder("n").argName("VideoName").desc("Video in Database to modify").hasArg().longOpt("name").required().build());
		Command command = b.onCalled(ctx -> {
			try
			{
				String[] result = assembleArguments(ctx.getArgs());
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
				{
					if (db.updateVideoOffset(line.getOptionValue('n'),
							Long.valueOf(Extractor.convertTimeToMilli(line.getOptionValue('o'), null, null))))
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Video Updated");
						});
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(":warning: No Video found");
						});
					}
				}
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addExamineExtractionCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "examineRecord";
		String[] aliases = new String[]
		{ "er" };
		String usage = prefix + name;
		String description = ":large_orange_diamond: __VIP__: Exports an Excel spreadsheet of the Extraction Record.";
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
				if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
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
						ctx.getChannel().sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
			}
		}).build();
		commands.add(new CommandWrapper(command, commandTier, usage, description, detailedDescription, name, aliases));
		registry.register(command, name, aliases);
	}

	private void addGuideCommand(int commandTier, Limiter[] limits)
	{
		Builder b = Command.builder();
		b.limiters(new LinkedHashSet<Limiter>(Arrays.asList(limits)));
		String name = "guide";
		String[] aliases = new String[]
		{ "instructions" };
		String usage = prefix + name + " <name>";
		String description = "Display a guide on how to do something";
		String detailedDescription = "Displays a guide on how to do something regarding Frame-Summoner?\nUsage: `" + usage
				+ "`\n*<name>* = Name of the guide to read.";
		detailedDescription += appendAliases(aliases);
		Options options = new Options();
		SimpleBindings sb = new SimpleBindings();
		EmbedBuilder eb = new EmbedBuilder();
		eb.withColor(new Color(0, 255, 255));
		eb.withAuthorName("FoxTrot Fanatics");
		eb.withAuthorUrl("https://foxtrotfanatics.info");
		eb.withAuthorIcon("https://storage.googleapis.com/ftf-public/CYTUBE/imgs/ftf_logo.png");
		//eb.withUrl("");
		eb.withTitle("Initalization Guide");
		eb.withDesc("How to setup the bot to your liking in your Server");
		//Thumbnails content changed at runtime
		//eb.withImage("");
		eb.appendField("1. Designate at least one Admin Channel",
				"Use `" + prefix + "cct -t 3` in a Server channel where you will continue initalization **REQUIRED**", false);
		eb.appendField("2. Check Command Help", "Use `fs!help` in the channel above. You will be referencing it often for the rest of the guide.",
				false);
		eb.appendField("3. Check Initial Server Settings", "Use `fs!info -s` to see how the server is currently configured. ", false);
		eb.appendField("4. Set Server Daily Extraction Limit", "Use `fs!ssl..` if you need to reduce usage server wide for some reason", false);
		eb.appendField("5. Set Server Listing Mode", "Use `fs!clm` if you would rather use a Whitelist instead of the default Blacklist", false);
		eb.appendField("6. Designate Administrator Roles", "Use `fs!aar...` to give other Discord roles, Frame-Summoner Administrator authority",
				false);
		eb.appendField("7. Designate Listed Roles", "Use `fs!alr...` to Blacklist or Whitelist roles for normal Frame-Summoner usage", false);
		eb.appendField("8. Designate Channel Permissions",
				"Use `fs!cct...` in other channels to change which commands are allowed in which channels\n default = 0", false);
		eb.appendField("9. Double Check Server Settings", "Use `fs!info -s` to see if there is anything else you want to change. ", false);
		eb.appendField("Profit", ":moneybag:", false);
		//Footer content changed at runtime
		sb.put("init", eb);
		eb = new EmbedBuilder();
		eb.withColor(new Color(0, 255, 255));
		eb.withAuthorName("FoxTrot Fanatics");
		eb.withAuthorUrl("https://foxtrotfanatics.info");
		eb.withAuthorIcon("https://storage.googleapis.com/ftf-public/CYTUBE/imgs/ftf_logo.png");
		//eb.withUrl("");
		eb.withTitle("Frame Extraction Guide");
		eb.withDesc("How to extract a frame from the videos correctly");
		//eb.withThumbnail("");
		//eb.withImage("");
		eb.appendField("1. Learn more abou the Frame command", "Use `" + prefix + "help frame` in an appropriate channel.", false);
		eb.appendField("2. Use the List command to see what is avaliable", "Use `fs!list` to get a list of videos prepared for extraction", false);
		eb.appendField("3. Figure out the timecode of the Frame", "Best thing to do is check out youtube, skip to the frame and get the timecode",
				false);
		eb.appendField("4. Run an extraction", "Fill in the parameters of the command with the info you have", false);
		eb.appendField("6. Adjust if necessary", "Extract again with the -c flag if you need to be more precise (read command details)", false);
		eb.appendField("$. Check out Recommendations", "Use `<COMING SOON>` to see a list of approved memorable moments", false);
		eb.appendField("!. Watch your Limit", "To prevent abuse, Extractions are limited to " + prop.getProperty("MaxUserExtracts") + "/day per User",
				false);
		eb.appendField("Profit", ":moneybag:", false);
		//Footer Content changed at runtime
		sb.put("frame", eb);
		Command command = b.onCalled(ctx -> {
			final IChannel location;
			if (db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				location = ctx.getChannel();
			else
				location = ctx.getAuthor().getOrCreatePMChannel();
			String[] result = assembleArguments(ctx.getArgs());
			try
			{
				CommandLine line = new DefaultParser().parse(options, result, false);
				String[] extraArgs = line.getArgs();
				if (extraArgs.length == 1 && (!extraArgs[0].equals("") && extraArgs.length == 1))
				{
					Object guide = sb.get(extraArgs[0]);
					if (guide != null)
					{
						EmbedBuilder builder = (EmbedBuilder) guide;
						builder.withThumbnail(ctx.getGuild().getIconURL());
						builder.withFooterIcon(ctx.getAuthor().getAvatarURL());
						builder.withFooterText("Requested By: \"" + ctx.getAuthor().getDisplayName(ctx.getGuild()) + "\"");
						RequestBuffer.request(() -> {
							location.sendMessage(builder.build());
						});
					}
					else
					{
						RequestBuffer.request(() -> {
							location.sendMessage(
									":octagonal_sign: Guide was not found: `" + extraArgs[0] + "`\nDo `" + prefix + "guide` for a list of guides.");
						});
					}
				}
				else if (extraArgs.length == 0 || (extraArgs[0].equals("") && extraArgs.length == 1))
				{
					StringBuilder guides = new StringBuilder("```md\n");
					guides.append(sb.size());
					guides.append(" Avaliable Guides\n");
					for (Entry<String, Object> e : sb.entrySet())
					{
						guides.append(prefix);
						guides.append("guide ");
						guides.append(e.getKey());
						guides.append(" = ");
						guides.append(((EmbedObject) e.getValue()).title);
						guides.append('\n');
					}
					guides.append("```");
					RequestBuffer.request(() -> {
						location.sendMessage(guides.toString());
					});
				}
				else//Too Many Arguments
				{
					RequestBuffer.request(() -> {
						location.sendMessage(":octagonal_sign: Unrecognized arguments: `" + line.getArgList()
								+ "`\nMaybe you provided an argument with spaces? Those require \"Double Quotation Marks\" to interpret correctly\nDo `"
								+ prefix + "help " + name + "` for proper Usage");
					});
				}
			}
			catch (IllegalArgumentException e)
			{
				logger.warn("Incomplete Double Quotes in message: {}", ctx.getMessage().getContent());
				final String response = ":octagonal_sign: Double Quotes unclosed, could not parse command around: `" + e.getMessage() + "`";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(response);
				});
			}
			catch (ParseException e)
			{
				handleParseException(ctx.getChannel(), e, name);
			}
			catch (Exception e)
			{
				String errorMessage = prefix + name + " command experienced error for >" + ctx.getMessage().getContent();
				logger.error(errorMessage, e);
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
						null, textTitle, textValue, inline, null);
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
						ctx.getChannel().sendMessage(
								"Unknown Command!\nUse " + prefix + "help for Command List\nUsage: **" + prefix + "help** <Command Name>");
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
			if (ctx.getGuild() == null)
			{
				throw new IllegalStateException("Sent in Private Message, parseGuild() Not applicable");
			}
			guild = ctx.getGuild();
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

	private void handleParseException(IChannel ch, ParseException e, String commandName)
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
			ch.sendMessage(response);
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

	public String[] assembleArguments(List<String> args) throws IllegalArgumentException
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
				try
				{
					if (temp.startsWith("\""))
					{
						temp = temp.substring(1);
						while (!temp.endsWith("\""))
						{
							temp += " " + args.get(++x);
						}
						fullArgs.add(temp.substring(0, temp.length() - 1));
					}
					else
						fullArgs.add(temp);
				}
				catch (IndexOutOfBoundsException e)
				{
					throw new IllegalArgumentException(temp);
				}
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
			message.withAuthorIcon("https://media.foxtrotfanatics.info/i/ftf_logo.png");
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
				message.withFooterText("Requested By: \"" + ctx.getAuthor().getDisplayName(ctx.getGuild()) + "\" - Showing " + fieldCount
						+ " Entries - Page " + (z + 1) + "/" + pageCount);
			}
			else
			{
				message.withFooterText(footer);
			}
			message.withFooterIcon(ctx.getAuthor().getAvatarURL());
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

	public void updateProperties(Properties p)
	{
		prop = p;
	}

	public CommandRegistry getRegistry()
	{
		return registry;
	}

	private class CommandWrapper
	{

		private int rank;
		private String usage;
		private String description;
		private String detailed;
		private String[] names;

		public CommandWrapper(Command command, int rank, String usage, String description, String detailed, String name, String[] aliases)
		{
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
