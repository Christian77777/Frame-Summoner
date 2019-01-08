
package com.Christian77777.Frame_Summoner;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database.RolePerm;
import com.darichey.discord.Command;
import com.darichey.discord.CommandContext;
import com.darichey.discord.Command.Builder;
import com.darichey.discord.CommandRegistry;
import com.darichey.discord.limiter.UserLimiter;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
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
	private UserLimiter operator;
	public static DateTimeFormatter milliDateFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
	public static DateTimeFormatter secondDateFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
	private CommandRegistry registry = new CommandRegistry("fs!");
	public static ReactionEmoji confirm = ReactionEmoji.of(new String(Character.toChars(9989)));
	public static ReactionEmoji deny = ReactionEmoji.of(new String(Character.toChars(10062)));

	public UserActivity(DRI dri, IDiscordClient c, Database d, Properties p)
	{
		this.dri = dri;
		this.c = c;
		db = d;
		prop = p;
		operator = new UserLimiter(Long.valueOf(prop.getProperty("Bot_Manager")))
		{

			@Override
			public void onFail(CommandContext ctx)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be a the Frame-Summoner Operator to use this Command");
				});
			}
		};
		extractor = new Extractor(c, d, p);
		//Debug Commands
		addDebugCommand();
		//Admin Commands
		addFullVerificationCommand();
		addExecuteVerification();
		addNewAdminRoleCommand();
		addRemoveAdminRoleCommand();
		addNewListedRoleCommand();
		addRemoveListedRoleCommand();
		addChangeListingModeCommand();
		if (prop.getProperty("AllowDirectoryChange").equals("Y"))
			addDirCommand();
		else
			addCantChangeCommand();
		//Extraction Commands
		addFrameCommand();
		//Query Commands
		addVerifyCommand();
		addListCommand();
		//Generic Commands
		addInfoCommand();
		addHelpCommand();
	}

	private void addDebugCommand()
	{
		Builder b = Command.builder();
		b.limiter(new UserLimiter(163810952905490432L));
		Command debug = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage("No");
			});
		}).build();
		registry.register(debug, "debug");
	}

	/**
	 * Requests the Extractor to extract a frame
	 */
	private void addFrameCommand()
	{
		Builder b = Command.builder();
		//b.limiter(userRoleLimiter);
		Command frame = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 2))
				return;
			if (checkWhitelisted(ctx.getGuild(), ctx.getAuthor()))
			{
				//Permission to Extract
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
												return ctx.getChannel().sendMessage(
														"Invalid Frame Count value: `" + frameCount2 + "`! Valid range between 1 and 1000");
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
										ctx.getChannel().sendMessage("Unknown Flag! Do s!help frame for proper usage");
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
								ctx.getChannel().sendMessage("Duplicate Filename or Timecode! Do s!help frame for proper usage");
							});
							return;
						}
					}
				}
				catch (IndexOutOfBoundsException e)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help frame for proper usage");
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
						ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help frame for proper usage");
					});
				}
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You are not authorized to use this command");
				});
			}
		}).build();
		registry.register(frame, "frame", "extract");
	}

	private void addRefreshVideosCommand()
	{

	}

	private void addFullVerificationCommand()
	{
		Builder b = Command.builder();
		//b.limiter(userRoleLimiter);
		Command fullVerify = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (!db.isUserVIP(ctx.getAuthor().getLongID()))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be a VIP to use this Command");
				});
				return;
			}
			if (args.size() > 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do s!help frame for proper usage");
				});
				return;
			}
			if (confirmAction(ctx, "Would you like to temporarly Disable the Extractor and begin Verification?"))
				extractor.fullVerification(ctx.getChannel());
		}).build();
		registry.register(fullVerify, "fullVerify");
	}

	private void addExecuteVerification()
	{
		Builder b = Command.builder();
		Command startVerify = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (!db.isUserVIP(ctx.getAuthor().getLongID()))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be a VIP to use this Command");
				});
				return;
			}
			//Permission to Verify (VIP)
			if (args.size() > 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do s!help frame for proper usage");
				});
				return;
			}
			if (confirmAction(ctx, "Would you like to temporarly Disable the Extractor and begin Verification?"))
				extractor.beginVerifications(ctx.getChannel());
		}).build();
		registry.register(startVerify, "startVerify");
	}

	/**
	 * Return the specified Video's length of time to Discord
	 */
	private void addVerifyCommand()
	{
		Builder b = Command.builder();
		//b.limiter(userRoleLimiter);
		Command verify = b.onCalled(ctx -> {
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (!db.isUserVIP(ctx.getAuthor().getLongID()))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be a VIP to use this Command");
				});
				return;
			}
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
									ctx.getChannel().sendMessage("Unknown Flag! Do s!help frame for proper usage");
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
							ctx.getChannel().sendMessage("Duplicate Filename! Do s!help frame for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help frame for proper usage");
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
		registry.register(verify, "verify");
	}

	public void addWipeDeletedVideosCommand()
	{

	}

	/**
	 * Return a list of videos to Discord
	 */
	private void addListCommand()
	{
		Builder b = Command.builder();
		Command list = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			//Respond at all
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 2))
			{
				if (!checkWhitelisted(ctx.getGuild(), ctx.getAuthor()))
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
				else
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":no_entry_sign: You are not authorized to use this command");
					});
				}
			}
		}).build();
		registry.register(list, "list");
	}

	/**
	 * Changes the Video Directory of bot after a quick Reboot
	 */
	private void addDirCommand()
	{
		Builder b = Command.builder();
		//Restrict to Operator
		b.limiter(operator);
		Command dir = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			//Respond at all
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (args.size() > 0)
			{ // more than 1 argument
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
		registry.register(dir, "dir");
	}

	/**
	 * Maintains command, but alerts user that command disabled
	 */
	private void addCantChangeCommand()
	{
		Builder b = Command.builder();
		//Restrict to Operator
		b.limiter(operator);
		Command dir = b.onCalled(ctx -> {
			//Respond at all
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (ctx.getAuthor().getLongID() != Long.valueOf(prop.getProperty("Bot_Manager")))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be a the Frame-Summoner Operator to use this Command");
				});
				return;
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage("**Command Disabled**\nEdit property in values.txt and reboot to allow usage");
			});
		}).build();
		registry.register(dir, "dir");
	}

	/**
	 * Shows some information about the bot.
	 */
	private void addInfoCommand()
	{
		Builder b = Command.builder();
		Command info = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
			{
				if (checkWhitelisted(ctx.getGuild(), ctx.getAuthor()))
				{
					String message;
					if (args.size() > 0)
						message = "Too many arguments!";
					else
						message = "- **Version:** " + DRI.version + "\n- **Author:** Christian77777\n" + "- **Programming Language:** Java\n"
								+ "- **Discord Connection Library:** Discord4J\n" + "- **Discord (Command) Library:** Commands4J";
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(message);
					});
				}
				else
				{
					//Tell User they are not authorized
				}
			}
		}).build();
		registry.register(info, "info");
	}

	private void addHelpCommand()
	{
		Builder b = Command.builder();
		Command help = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (args.size() == 0)
			{
				ArrayList<String> textTitle = new ArrayList<String>();
				ArrayList<String> textValue = new ArrayList<String>();
				ArrayList<Boolean> inline = new ArrayList<Boolean>();
				//Field Titles: 50
				//Field Values: 175
				textTitle.add("s!dir");
				textValue.add("Change the Video Directory to extract from, **Admin only**");
				inline.add(false);
				textTitle.add("<- Prerequisite");
				textValue.add("Command will be disabled if values.txt disables it");
				inline.add(true);
				//ffmpeg -ss ##:##:##.### -i filename.mkv -t 1 -f image2 frame-[].jpg
				textTitle.add("s!frame [Filename] [Timecode] (Frame Number)");
				textValue.add("Extracts Frame from video at the timecode and uploads it to Discord");
				inline.add(false);
				textTitle.add("<- Prerequisite");
				textValue.add("Command will be disabled if Video Directory is invalid");
				inline.add(true);
				textTitle.add("s!verify [Filename] <-s>");
				textValue.add("Manually Analyzes the video to determine the length of time, framerate.");
				inline.add(false);
				textTitle.add("<- Prerequisite");
				textValue.add("Command will be disabled if Video Directory is invalid");
				inline.add(true);
				textTitle.add("s!list");
				textValue.add("Lists all Videos in accessible folder that can be accessed");
				inline.add(false);
				textTitle.add("<- Prerequisite");
				textValue.add("Command will be disabled if Video Directory is invalid");
				inline.add(true);
				textTitle.add("s!info");
				textValue.add("Display Info about the bot");
				inline.add(false);
				textTitle.add("s!help <Command>");
				textValue.add("Display Command list with descriptions, or command details");
				inline.add(false);
				int pageCount = ((int) Math.ceil(textTitle.size() / 25.0));
				for (int z = 0; z < pageCount; z++)
				{
					EmbedBuilder message = new EmbedBuilder();
					message.withAuthorName(prop.getProperty("Server_Name"));
					message.appendDesc("All Commands FTU-Bot respects, might require special permission set in values.txt");
					message.withColor(255, 165, 0);
					message.withTitle("Command List");
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
			else if (args.size() == 1)
			{
				switch (args.get(0))
				{
					case "debugDB":
						//Debug Only
						break;
					case "dir":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(
									"Changes the Video Directory the bot monitors, requires Reboot.\nCommand can be completely disabled in values.txt and even then, is Admin Role only");
						}).get();
						break;
					case "frame":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(
									"Extracts frame from video and uploads to Discord\nUsage: s!frame [filename] [Timecode]\n[Filename]: Name of File, use s!list to see avaliable files\n[Timecode]: closest time to extract frame from, in this format ##:##:##.###");
						}).get();
						break;
					case "verify":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(
									"Verifies if Video can be extracted from, and if so, records the length and framerate into database\nUsage: s!verify [filename] <-r>\n[Filename]: Name of File, use s!list to see avaliable files\n<-r> include this argument to execute the verification queue\n__Warning__: pauses all extractions until completion\n__Cancellable__: fs!pause | fs!kill");
						}).get();
						break;
					case "list":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Command to List avaliable videos accessible");
						}).get();
						break;
					case "info":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Generic information about this Discord bot");
						}).get();
						break;
					case "help":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("You want help about using the Help command? sheesh");
						}).get();
						break;
					default:
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Unknown Command!\nUse s!help for Command List\nUsage: **s!help** <Command Name>");
						}).get();
						break;
				}
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments");
				}).get();
			}
		}).build();
		registry.register(help, "help");
	}

	private void addNewAdminRoleCommand()
	{
		Builder b = Command.builder();
		//b.limiter(userRoleLimiter);
		Command newAdmin = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (!checkAdmin(ctx.getGuild(), ctx.getAuthor()))
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be an Admin to use this Command");
				});
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
									ctx.getChannel().sendMessage("Unknown Flag! Do s!help addAdminRole for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do s!help addAdminRole for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help addAdminRole for proper usage");
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
				message = "No ID Provided! Do s!help addAdminRole for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
		}).build();
		registry.register(newAdmin, "newAdminRole");
	}

	private void addRemoveAdminRoleCommand()
	{
		Builder b = Command.builder();
		//b.limiter(userRoleLimiter);
		Command removeAdmin = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (!checkAdmin(ctx.getGuild(), ctx.getAuthor()))
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be an Admin to use this Command");
				});
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
									ctx.getChannel().sendMessage("Unknown Flag! Do s!help removeAdminRole for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do s!help removeAdminRole for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help removeAdminRole for proper usage");
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
				message = "No ID Provided! Do s!help removeAdminRole for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
		}).build();
		registry.register(removeAdmin, "removeAdminRole");
	}
	
	private void addNewListedRoleCommand()
	{
		Builder b = Command.builder();
		//b.limiter(userRoleLimiter);
		Command newRole = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (!checkAdmin(ctx.getGuild(), ctx.getAuthor()))
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be an Admin to use this Command");
				});
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
									ctx.getChannel().sendMessage("Unknown Flag! Do s!help addListedRole for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do s!help addListedRole for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help addListedRole for proper usage");
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
						if(isBlacklist)
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
				message = "No ID Provided! Do s!help addListedRole for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
		}).build();
		registry.register(newRole, "addListedRole");
	}
	
	private void addRemoveListedRoleCommand()
	{
		Builder b = Command.builder();
		//b.limiter(userRoleLimiter);
		Command removeRole = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (!checkAdmin(ctx.getGuild(), ctx.getAuthor()))
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be an Admin to use this Command");
				});
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
									ctx.getChannel().sendMessage("Unknown Flag! Do s!help removeListedRole for proper usage");
								});
								return;
						}
					}
					else
					{
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Undefined Argument! Do s!help removeListedRole for proper usage");
						});
						return;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help removeListedRole for proper usage");
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
				message = "No ID Provided! Do s!help removeListedRole for proper usage";
			}
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
		}).build();
		registry.register(removeRole, "removeListedRole");
	}
	
	private void addChangeListingModeCommand()
	{
		Builder b = Command.builder();
		Command changeListing = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (!db.checkChannelPermission(ctx.getChannel().getLongID(), 1))
				return;
			if (!checkAdmin(ctx.getGuild(), ctx.getAuthor()))
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":no_entry_sign: You must be an Admin to use this Command");
				});
				return;
			}
			if (args.size() > 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do s!help changeListingMode for proper usage");
				});
				return;
			}
			boolean isBlacklist = db.checkGuildBlacklistMode(ctx.getGuild().getLongID());
			String message;
			if(isBlacklist)
				message = "Would you like to change this server from a Blacklist to Whitelist?\nAll Current Listing Permissions will be deleted.";
			else
				message = "Would you like to change this server from a Whitelist to Blacklist?\nAll Current Listing Permissions will be deleted.";
			if (confirmAction(ctx, message))
			{
				db.changeGuildBlacklistMode(ctx.getGuild().getLongID(),!isBlacklist);
				String alternateList;
				if(isBlacklist)
					alternateList = "Whitelist";
				else
					alternateList = "Blacklist";
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("This Guild has switched to a " + alternateList);
				});
			}
		}).build();
		registry.register(changeListing, "changeListingMode");
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
	 * Check if the User is allowed to use the bot in the server
	 * REQUIRED ASSUMPTION: Server must have a single mode, Blacklist or Whitelist. All entries for a single guild assumed
	 * to be of one type
	 * @param guild Guild where the rules are relevant
	 * @param user who needs to be checked
	 * @return if user has the proper roles required
	 */
	public boolean checkWhitelisted(IGuild guild, IUser user)
	{
		ArrayList<Long> roles = new ArrayList<Long>();
		//Insertion Sort, for sorting the user roles in ascending order. Using Insertion Sort because iteration is already required to extract the actual Longs
		for (IRole r : user.getRolesForGuild(guild))
		{
			if (roles.isEmpty())
				roles.add(r.getLongID());
			else
				for (int x = 0; x < roles.size(); x++)
				{
					if (roles.get(x) >= r.getLongID())
					{
						roles.add(x, r.getLongID());
						break;
					}
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
			while (roles.size() > lIndex && rolePerms.size() > rIndex)
			{
				int comparision = roles.get(lIndex).compareTo(rolePerms.get(rIndex).getRoleID());
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

	/**
	 * Check if User has the required admin role
	 * @param guild Guild the message was required
	 * @param user User that sent the message
	 * @return if user had the required roles
	 */
	public boolean checkAdmin(IGuild guild, IUser user)
	{
		ArrayList<IRole> roles = new ArrayList<IRole>(user.getRolesForGuild(guild));
		ArrayList<Long> required = db.getListOfAdminRoles(guild.getLongID());
		for (IRole r : roles)
		{
			if (required.contains(r.getLongID()))
				return true;
		}
		return false;
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

	public CommandRegistry getRegistry()
	{
		return registry;
	}
}
