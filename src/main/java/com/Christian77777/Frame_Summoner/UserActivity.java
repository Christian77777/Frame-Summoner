
package com.Christian77777.Frame_Summoner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.darichey.discord.Command;
import com.darichey.discord.Command.Builder;
import com.darichey.discord.CommandRegistry;
import com.darichey.discord.limiter.ChannelLimiter;
import com.darichey.discord.limiter.RoleLimiter;
import com.darichey.discord.limiter.UserLimiter;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

public class UserActivity
{

	private static Logger logger = LogManager.getLogger();
	private DRI dri;
	private ChannelLimiter channelLimiter;
	private RoleLimiter adminRoleLimiter;
	private RoleLimiter userRoleLimiter;
	private String serverName;
	private String ffmpegDir;
	private String ffprobeDir;
	private String videoDir;
	private boolean changeDir;
	public static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss");
	private CommandRegistry registry = new CommandRegistry("s!");

	public UserActivity(DRI dri, IChannel c, IRole a, IRole u, String s, String f, String f2, String v, boolean d)
	{
		this.dri = dri;
		this.serverName = s;
		this.ffmpegDir = f;
		this.ffprobeDir = f2;
		this.videoDir = v;
		this.changeDir = d;
		channelLimiter = new ChannelLimiter(c);
		adminRoleLimiter = new RoleLimiter(a);
		userRoleLimiter = new RoleLimiter(a, u);
		//Debug Commands
		addDebugCommand();
		//Admin Commands
		if(changeDir)
			addDirCommand();
		else
			addCantChangeCommand();
		if (videoDir != null)
		{
			//Extraction Commands
			addFrameCommand();
			//Query Commands
			addTimeCommand();
			addListCommand();
		}
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
		b.limiter(channelLimiter);
		b.limiter(userRoleLimiter);
		Command frame = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (args.size() == 2)
			{
				File video = new File(videoDir + File.separator + args.get(0));
				if (!video.exists())
				{
					logger.error("Video Not Found: {}", video.getAbsolutePath());
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Video Not Found");
					});
					return;
				}
				else if (!checkOffset(args.get(1)))
				{
					logger.error("Offset Format Invalid: {}", args.get(1));
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Offset Format invalid, must be in ##:##:##.###");
					});
					return;
				}
				else
				{
					ctx.getClient().changePresence(StatusType.IDLE, ActivityType.PLAYING, args.get(0));
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Summoning Frame...");
					});
					String command = ffmpegDir + " -ss " + args.get(1) + " -i \"" + video.getAbsolutePath() + "\" -t 1 -f image2 -frames:v 1 \""
							+ DRI.dir + File.separator + "frame-" + args.get(0) + ".png\"";
					try
					{

						logger.info("Command: ({})", command);
						Process r = Runtime.getRuntime().exec(command);
						StreamGobbler reader = new StreamGobbler(r.getInputStream(), true);
						StreamGobbler eater = new StreamGobbler(r.getErrorStream(), true);
						reader.start();
						eater.start();
						int exitValue = r.waitFor();
						if (exitValue == 0)
						{
							logger.info("Process Completed");
							File result = new File(DRI.dir + File.separator + "frame-" + args.get(0) + ".png");
							RequestBuffer.request(() -> {
								try
								{
									ctx.getChannel().sendFile(ctx.getAuthor().mention() + " Frame from video " + args.get(0) + " at " + args.get(1),
											result);
									logger.info("File Uploaded");
									if (result.delete())
										logger.info("Temp Frame Deleted");
									else
										logger.error("Unable to Delete Frame");
								}
								catch (FileNotFoundException e)
								{
									logger.error("File Not Found: {}", result.getAbsolutePath());
									logger.catching(e);
									RequestBuffer.request(() -> {
										ctx.getChannel().sendMessage("Extracted Frame not found!");
									});
								}
							});
						}
						else
						{
							logger.error("Process Failed, Error: {}", exitValue);
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Extraction Failed, check Console for errors");
							});
						}
					}
					catch (IOException e1)
					{
						logger.catching(e1);
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Could not start Process.");
						});
					}
					catch (InterruptedException e2)
					{
						logger.catching(e2);
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Interruption Exception, process not completed yet.");
						});
					}
				}
			}
			else if (args.size() < 2)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help frame for more information");
				});
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do s!help frame for more information");
				});
			}
			dri.updateStatus();
		}).build();
		registry.register(frame, "frame");
	}

	/**
	 * Return the specified Video's length of time to Discord
	 */
	private void addTimeCommand()
	{
		Builder b = Command.builder();
		b.limiter(channelLimiter);
		b.limiter(userRoleLimiter);
		Command time = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (args.size() == 1)
			{
				File video = new File(videoDir + File.separator + args.get(0));
				if (!video.exists())
				{
					logger.error("Video Not Found: {}", video.getAbsolutePath());
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("Video Not Found");
					});
					dri.updateStatus();
					return;
				}
				else
				{
					ctx.getClient().changePresence(StatusType.IDLE, ActivityType.LISTENING, args.get(0));
					String command = ffprobeDir + " -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 -sexagesimal \""
							+ video.getAbsolutePath() + "\"";
					try
					{
						logger.info("Command: ({})", command);
						Process r = Runtime.getRuntime().exec(command);
						StreamGobbler eater = new StreamGobbler(r.getErrorStream(), true);
						eater.start();
						int exitValue = r.waitFor();
						if (exitValue == 0)
						{
							logger.info("Process Completed");
							BufferedReader reader = new BufferedReader(new InputStreamReader(r.getInputStream()));
							StringBuilder builder = new StringBuilder();
							String line = null;
							while ((line = reader.readLine()) != null)
							{
								builder.append(line);
								builder.append(System.getProperty("line.separator"));
							}
							String result = new String(builder.toString().trim());
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Video Length: " + result);
							});
						}
						else
						{
							logger.error("Process Failed, Error: {}", exitValue);
							RequestBuffer.request(() -> {
								ctx.getChannel().sendMessage("Probing Failed, check Console for errors");
							});
						}
					}
					catch (IOException e1)
					{
						logger.catching(e1);
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Could not start Process.");
						});
					}
					catch (InterruptedException e2)
					{
						logger.catching(e2);
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Interruption Exception, process not completed yet.");
						});
						e2.printStackTrace();
					}
				}
			}
			else if (args.size() < 1)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Not Enough Arguments! Do s!help time for more information");
				});
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Too Many Arguments! Do s!help time for more information");
				});
			}
			dri.updateStatus();
		}).build();
		registry.register(time, "time");
	}

	/**
	 * Return a list of videos to Discord
	 */
	private void addListCommand()
	{
		Builder b = Command.builder();
		b.limiter(channelLimiter);
		b.limiter(userRoleLimiter);
		Command list = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			dri.updateStatus();
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
							ctx.getChannel().sendMessage(stringPart);
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
		b.limiter(channelLimiter);
		b.limiter(adminRoleLimiter);
		Command dir = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			if (args.size() > 0)
			{ // more than 1 argument
				String path = "";
				for(String s:args)
				{
					path += s;
				}
				path = path.trim();
				if(path.startsWith("`") && path.endsWith("`"))
				{
					path = path.substring(1, path.length()-2);
				}
				if (!new File(path).exists() || !new File(path).isDirectory())
				{
					final String path2 = new String(path);
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage("File Path: `" + path2 + "`\nIs not valid, or does not exist.");
					});
					dri.updateStatus();
					return;
				}
				logger.info("Updating Video Directory to: {}",path);
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
		b.limiter(channelLimiter);
		b.limiter(adminRoleLimiter);
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
		b.limiter(channelLimiter);
		Command info = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			if (args.size() == 1 && args.get(0).equals(""))
				args.remove(0);
			dri.updateStatus();
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
		b.limiter(channelLimiter);
		Command help = b.onCalled(ctx -> {
			ArrayList<String> args = new ArrayList<String>(ctx.getArgs());
			dri.updateStatus();
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
				textTitle.add("s!debugDB");
				textValue.add("Freeze code in IDE to see fields");
				inline.add(false);
				textTitle.add("s!dir");
				textValue.add("Change the Video Directory of Bot, Admin only, enabled only in values.txt");
				inline.add(false);
				//ffmpeg -ss ##:##:##.### -i filename.mkv -t 1 -f image2 frame-[].jpg
				textTitle.add("s!frame [Filename] [Timecode]");
				textValue.add("Extract Frame from video at timecode and upload to Discord");
				inline.add(false);
				textTitle.add("s!time [Filename]");
				textValue.add("Returns the length of the video as a time code to know how long the video is");
				inline.add(false);
				textTitle.add("s!list");
				textValue.add("Lists all Videos in accessible folder that can be accessed");
				inline.add(false);
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
					message.withAuthorName(serverName);
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
						ctx.getChannel().sendMessage(message.build());
					});
				}
			}
			else if (args.size() == 1)
			{
				switch (args.get(0))
				{
					case "debugDB":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Debug command, please ignore");
						}).get();
						break;
					case "dir":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage("Changes the Video Directory the bot monitors, requires Reboot.\nCommand can be completely disabled in values.txt and even then, is Admin Role only");
						}).get();
						break;
					case "frame":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(
									"Extracts frame from video and uploads to Discord\nUsage: s!frame [filename] [Timecode]\n[Filename]: Name of File, use s!list to see avaliable files\n[Timecode]: closest time to extract frame from, in this format ##:##:##.###");
						}).get();
						break;
					case "time":
						RequestBuffer.request(() -> {
							ctx.getChannel().sendMessage(
									"Finds the length of time a video has\nUsage: s!time [filename]\n[Filename]: Name of File, use s!list to see avaliable files");
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
	private boolean checkOffset(String text)
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
			return !wrong;
		}
		else
		{
			return true;
		}
	}

	public CommandRegistry getRegistry()
	{
		return registry;
	}
}
