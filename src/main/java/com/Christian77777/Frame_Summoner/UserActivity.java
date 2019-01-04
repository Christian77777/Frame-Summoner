
package com.Christian77777.Frame_Summoner;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.darichey.discord.Command;
import com.darichey.discord.Command.Builder;
import com.darichey.discord.CommandRegistry;
import com.darichey.discord.limiter.UserLimiter;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

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
	private CommandRegistry registry = new CommandRegistry("fs!");
	public static ReactionEmoji confirm = ReactionEmoji.of(new String(Character.toChars(9989)));
	public static ReactionEmoji deny = ReactionEmoji.of(new String(Character.toChars(10062)));

	public UserActivity(DRI dri, IDiscordClient c, Database d, Properties p)
	{
		this.dri = dri;
		this.c = c;
		db = d;
		prop = p;
		extractor = new Extractor(c, d, p);
		//Debug Commands
		addDebugCommand();
		//Admin Commands
		addFullVerificationCommand();
		addExecuteVerification();
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
	 * Extracts the specified Frame and uploads to Discord
	 * ffmpeg -ss ##:##:##.### -i filename.mkv -t 1 -f image2 frame-[].jpg
	 */
	private void addFrameCommand()
	{
		Builder b = Command.builder();
		//b.limiter(channelLimiter);
		//b.limiter(userRoleLimiter);
		Command frame = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			//Permission to Respond
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
		}).build();
		registry.register(frame, "frame");
	}

	private void addRefreshVideosCommand()
	{
		
	}

	private void addFullVerificationCommand()
	{
		Builder b = Command.builder();
		//b.limiter(channelLimiter);
		//b.limiter(userRoleLimiter);
		Command fullVerify = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			//Permission to Respond
			//Permission to Verify (VIP)
			if(args.size() > 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do s!help frame for proper usage");
				});
				return;
			}
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
			//Permission to Respond
			//Permission to Verify (VIP)
			if(args.size() > 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do s!help frame for proper usage");
				});
				return;
			}
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
		//b.limiter(channelLimiter);
		//b.limiter(userRoleLimiter);
		Command verify = b.onCalled(ctx -> {
			//Permission to Respond
			//VIP Permission
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
		//b.limiter(channelLimiter);
		//b.limiter(userRoleLimiter);
		Command list = b.onCalled(ctx -> {
			String videoDir = prop.getProperty("Video_Directory");
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			File videos = new File(videoDir);
			if (!videos.exists())
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("No Videos found");
				});
				videos.mkdirs();
			}
			else if (videos.listFiles().length == 0)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("No Videos found");
				});
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
		}).build();
		registry.register(list, "list");
	}

	/**
	 * Changes the Video Directory of bot after a quick Reboot
	 */
	private void addDirCommand()
	{
		Builder b = Command.builder();
		//b.limiter(channelLimiter);
		//b.limiter(adminRoleLimiter);
		Command dir = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
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
				logger.info("Updating Video Directory to: {}", path);
				dri.editVideoDirectory(path, true, ctx);
				final String path2 = new String(path);
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("File Path: `" + path2 + "`\nSaved to values.txt");
				});
				logger.info("Edited values.txt, now rebooting");
				DRI.menu.manualRestart(500);
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
		//b.limiter(channelLimiter);
		//b.limiter(adminRoleLimiter);
		Command dir = b.onCalled(ctx -> {
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
		//b.limiter(channelLimiter);
		Command info = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			String message;
			if (args.size() > 0)
			{ // more than 1 argument
				message = "Too many arguments!";
			}
			else if (args.size() == 0)
			{ // !info
				message = "- **Version:** " + DRI.version + "\n- **Author:** Christian77777\n" + "- **Programming Language:** Java\n"
						+ "- **Discord Connection Library:** Discord4J\n" + "- **Discord (Command) Library:** Commands4J";
			}
			else
				throw new IllegalArgumentException("Negative number of Arguments!");
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(message);
			});
		}).build();
		registry.register(info, "info");
	}

	private void addHelpCommand()
	{
		Builder b = Command.builder();
		//b.limiter(channelLimiter);
		Command help = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
			{
				args.remove(0);
			}
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
