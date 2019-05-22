
package com.Christian77777.Frame_Summoner;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database.Database;
import com.Christian77777.Frame_Summoner.Database.DBGuild;
import com.Christian77777.Frame_Summoner.Database.DBLink;
import com.Christian77777.Frame_Summoner.Database.DBNormalUser;
import com.Christian77777.Frame_Summoner.Database.DBVideo;
import com.darichey.discord.CommandContext;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MessageBuilder;
import sx.blah.discord.util.RequestBuffer;

public class Extractor
{

	private static Logger logger = LogManager.getLogger();
	private static final int QUEUE_LIMIT = 10;
	private Properties prop;
	private Database db;
	private Thread extractionThread;
	private Thread verificationThread;
	private volatile boolean extracting = true;
	private volatile boolean running = true;
	private volatile boolean forcefulShutdown = false;
	private IDiscordClient client;
	private final LinkedBlockingQueue<ExtractionJob> queue;
	private final Queue<TimestampJob> verifications;
	private ScheduledExecutorService midnightReset = Executors.newScheduledThreadPool(1);

	//Offsets are positive if the first frame of the Youtube video starts later in the life of the source video
	public Extractor(IDiscordClient c, Database d, Properties p)
	{
		client = c;
		db = d;
		prop = p;
		verifications = new LinkedList<TimestampJob>();
		queue = new LinkedBlockingQueue<ExtractionJob>(QUEUE_LIMIT);
		long seconds = localSecondsUntilMidnight();
		midnightReset.scheduleWithFixedDelay(() -> {
			Thread.currentThread().setName("MidnightReset");
			db.updateUserDailyUsage(0, null);
			db.updateServerDailyUsage(0, null);
			logger.info("Daily Usage Reset");
		}, seconds, 24 * 60 * 60, TimeUnit.SECONDS);
		logger.info("Next Reset takes place in {}", convertMilliToTime(seconds * 1000));
		startExtractor();
	}

	public static void main(String[] args)
	{
		String video = "E:\\ENG\\002_ENG_Code_Lyoko.mkv";
		String command = "ffprobe -v error -show_entries format=duration:stream=r_frame_rate:stream=duration -select_streams v:0 -print_format default=noprint_wrappers=1:nokey=1 -sexagesimal \""
				+ video + "\"";
		try
		{
			System.out.println("Command>" + command);
			Process r = Runtime.getRuntime().exec(command);
			StreamGobbler eater = new StreamGobbler(r.getErrorStream(), false);
			BufferedReader reader = new BufferedReader(new InputStreamReader(r.getInputStream()));
			eater.start();
			int exitValue = r.waitFor();
			if (exitValue == 0)
			{
				System.out.println("Process Completed");
				String fps = reader.readLine();
				String duration = reader.readLine();
				String vduration = reader.readLine();
				String line;
				while ((line = reader.readLine()) != null)
				{
					System.out.println("EXTRA: " + line);
				}
				if (duration == null || vduration == null || fps == null)//No Video Stream
				{
					System.out.println("Not a Video File");
					//return false
				}
				else if (duration.equals("N/A"))//Container does not allow stream specific duration
				{
					System.out.println("Container does not allow stream specific duration");
					duration = vduration;
				}
				else
					reader.readLine();
				System.out.println("FPS: " + fps);
				duration = "0" + duration.substring(0, duration.length() - 3);
				System.out.println("Duration: " + duration);
				System.out.println("Video Duration: " + vduration);
				//Could be corrupted

			}
			else
			{
				System.out.println("Process Failed, Error Value: " + exitValue);
				String line;
				while ((line = reader.readLine()) != null)
				{
					System.out.println(line);
				}
			}
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
		catch (InterruptedException e2)
		{
			e2.printStackTrace();
		}
	}

	public void pauseExtractor()
	{
		if (extracting)
		{
			logger.info("Stopping Extractor");
			extracting = false;
			extractionThread.interrupt();
		}
		else
			logger.warn("Attempting to pause Extractor thats already stopped");
	}

	public void startExtractor()
	{
		extractionThread = new Thread()
		{

			public void run()
			{
				logger.info("Extraction Thread Started");
				try
				{
					normalStatus();
					while (extracting)
					{
						extractFrame(queue.take());
						if (!forcefulShutdown && queue.peek() == null)
						{
							try
							{
								FileUtils.cleanDirectory(new File(DRI.dir + File.separator + "frame-cache"));
							}
							catch (IOException e)
							{
								logger.error("Unable to clear frame-cache", e);
							}
							normalStatus();
						}
					}
				}
				catch (InterruptedException e)
				{
					logger.info("Extraction Thread terminated");
				}
				if (forcefulShutdown)
				{
					while (queue.peek() != null)
					{
						ExtractionJob job = queue.remove();
						RequestBuffer.request(() -> {
							job.message.getChannel().sendMessage(":no_entry: Cancelling Request, Frame Extraction was manually disabled");
						});
					}
				}
			}
		};
		extractionThread.setName("Extractor Thread");
		extracting = true;
		extractionThread.start();
	}

	public void beginVerifications(IChannel channel)
	{
		verificationThread = new Thread()
		{

			public void run()
			{
				logger.info("Video Verification Thread Started");
				pauseExtractor();
				try
				{
					extractionThread.join();
					RequestBuffer.request(() -> {
						channel.sendMessage("Beginning Verification for " + verifications.size() + " Videos");
					}).get();
					client.changePresence(StatusType.DND, ActivityType.LISTENING, "to " + verifications.size() + " Videos");
					int initial = verifications.size();
					int count = 0;
					for (int x = 0; x < initial; x++)
					{
						if (extractMetadata(verifications.poll()))
							count++;
						if (!running || forcefulShutdown)
							break;
					}
					String response;
					if (forcefulShutdown || !running)
					{
						if (forcefulShutdown)
							response = ":no_entry: Terminating verification, Frame Extraction was manually disabled";
						else
							response = ":warning: Video verification completed only for " + count + "/" + initial + " files";
						while (queue.peek() != null)
							queue.remove();
					}
					else
					{
						normalStatus();
						if (count == initial)
							response = ":white_check_mark: Video verification completed for " + count + " files";
						else
							response = ":warning: Video verification completed only for " + count + "/" + initial + " files";
					}
					RequestBuffer.request(() -> {
						channel.sendMessage(response);
					});
					logger.info(response);
					startExtractor();
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		};
		verificationThread.setName("Verification Thread");
		verificationThread.start();
	}

	/**
	 * Rebuild the Video Database with the most current list of videos
	 * @param c Channel to respond to
	 * @return if verification took place
	 */
	public boolean fullVerification(IChannel c)
	{
		if (extracting && running)
		{
			db.removeAllVideos();
			File folder = new File(prop.getProperty("Video_Directory"));
			String[] videos = folder.list();
			if (videos == null)//folder File not a Directory
			{
				logger.error("Video_Directory property is not a valid Directory: {}", prop.getProperty("Video_Directory"));
				RequestBuffer.request(() -> {
					c.sendMessage(":warning: Video Directory is not a Directory!");
				});
				return false;
			}
			for (String v : videos)
			{
				verifications.offer(new TimestampJob(c, v));
			}
			beginVerifications(c);
			return true;
		}
		else
		{
			RequestBuffer.request(() -> {
				c.sendMessage("Frame-Summoner is disabled, please enable it first!");
			});
			return false;
		}
	}

	/**
	 * Submit Request to Extract Frame. If successful (video exists, timecode within length), request added to Queue until
	 * worker thread can take the request
	 * Allows Discord Communication thread to return quickly.
	 * @param ctx
	 * @param nickname
	 * @param timecode
	 * @param frameCount
	 * @return
	 */
	public boolean requestFrame(CommandContext ctx, String nickname, String timecode, Integer frameCount, boolean useOffset)
	{
		if (running)
		{
			String directory = prop.getProperty("Video_Directory");
			DBVideo data = db.getVideoData(nickname);
			//Extraction cancelled if video is not even recorded at all
			if (data == null)
			{
				logger.warn("Video Not Found: {}{}{}.<extension>", directory, File.separator, nickname);
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":warning: Video Not Found");
				});
				return false;
			}
			else if (!data.isUsable())
			{
				logger.warn("Video Not Usable: {}{}{}.<extension>", directory, File.separator, nickname);
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":warning: Video Unusable");
				});
				return false;
			}
			DBNormalUser person = db.getUserUsage(ctx.getAuthor().getLongID());
			int maxUserExtracts = Integer.valueOf(prop.getProperty("MaxUserExtracts"));
			boolean isVIP = (person != null && person.isVip()) || (ctx.getAuthor().getLongID() == Long.parseLong(prop.getProperty("Bot_Manager")));
			//Permission Denied if not VIP and over the maximum number of extractions per Server
			DBGuild g = db.getServerData(ctx.getGuild().getLongID());
			if (!g.isEnabled() && !isVIP)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":red_circle: This Server is unauthorized to extract Frames until it is reinstated.");
				});
				return false;
			}
			//Permission Denied if Globally banned.
			if (person != null)
			{
				//Permission Denied if Globally banned.
				if (person.isBanned())
				{
					RequestBuffer.request(() -> {
						ctx.getAuthor().getOrCreatePMChannel()
								.sendMessage(":no_entry_sign: You are banned from Requesting Frames from Frame-Summoner");
					});
					return false;
				}
				//Permission Denied if not VIP and over the maximum number of extractions per User
				if (!isVIP && person.getUsedToday() >= maxUserExtracts)
				{
					RequestBuffer.request(() -> {
						ctx.getChannel().sendMessage(":clock3: You have reached the daily limit of `" + maxUserExtracts
								+ "` Frames per day. Reset in `" + convertMilliToTime(localSecondsUntilMidnight() * 1000) + "`");
					});
					return false;
				}
			}
			else
				person = new DBNormalUser(ctx.getAuthor().getLongID());
			//Permission Denied if not VIP and over the maximum number of extractions per Server
			if (!isVIP && g.getUsedToday() >= g.getRequestLimit())
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":red_circle: This Server has reached the daily limit of `" + g.getRequestLimit()
							+ "` Frames per day. Reset in `" + convertMilliToTime(localSecondsUntilMidnight() * 1000) + "`");
				});
				return false;
			}
			//Permission denied if Video is not visible with the users current permission level
			if (data.isRestricted() && !isVIP)
			{
				logger.warn("Video Not Accessible: {}{}{}", directory, File.separator, data.getFilename());
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":warning: Video Not Found");
				});
				return false;
			}
			long timestamp = convertTimeToMilli(timecode, frameCount, data.getFps());
			//If Offset, and requested, add offset to timecode
			if (useOffset && data.getOffset() != 0)
				timestamp += data.getOffset();
			//Compare timecode to length
			if (data.getLength() < timestamp)
			{
				logger.error("Timestamp extends past video Length: {}", timecode);
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":warning: Timestamp extends past video Length: " + convertMilliToTime(data.getLength()));
				});
				return false;
			}
			//Recalculate timecode for -s FFMPEG flag
			long properTimestamp = convertTimeToMilli(timecode, null, null);
			ArrayList<DBLink> links = db.getLink(nickname, false, isVIP);
			DBLink link = null;
			if (!links.isEmpty())
				link = links.get(0);
			//Keep timecode and offset separate to obfuscate offset to users
			if (queue.offer(new ExtractionJob(ctx.getMessage(), ctx.getAuthor(), g, person, link, data, properTimestamp, useOffset, frameCount,
					maxUserExtracts, isVIP)))//Add to Queue if not too full
			{
				RequestBuffer.request(() -> {
					ctx.getMessage().addReaction(UserActivity.ok);
				}).get();
				return true;
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":clock3: Overloaded with requests, please try again later");
				});
				return false;
			}
		}
		else
		{
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(":no_entry: All Extraction Extraction Activity is currently Disabled");
			});
			return false;
		}
	}

	private boolean extractFrame(ExtractionJob job)
	{
		File videoFile = new File(prop.getProperty("Video_Directory") + File.separator + job.video.getFilename());
		if (!videoFile.exists())//If Video deleted
		{
			logger.error("Video Not Found: {}", videoFile.getAbsolutePath());
			RequestBuffer.request(() -> {
				job.message.getChannel().sendMessage("Source Video Removed!");
			});
			return false;
		}
		job.message.getClient().changePresence(StatusType.IDLE, ActivityType.PLAYING, job.video.getNickname());
		RequestBuffer.request(() -> {
			job.message.addReaction(UserActivity.begin);
		}).get();
		String frameSkip = "";
		if (job.frameCount != null)
		{
			frameSkip = "-filter:v select=gte(n\\," + job.frameCount + ") ";
		}
		String startTime = convertMilliToTime(job.timecode + (job.useOffset ? job.video.getOffset() : 0));
		//Extraction by exact time code: ffmpeg -ss ##:##:##.### -i "C:\Path\to\Video" -t 1 -f image2 -frames:v 1 "C:\Path\to\frame-videofilename.png"
		//Extraction by Timestamp and frame number in range [0,fps):ffmpeg -ss ##:##:## -i "C:\Path\to\Video" -filter:v "select=gte(n/,%%FRAMENUMBER)" -f image2 -frames:v 1 "C:\Path\to\frame-videofilename.png"
		String command = "" + escapeFilepath(prop.getProperty("FFmpeg_Path")) + " -loglevel error -y -ss " + startTime + " -i "
				+ escapeFilepath(videoFile.getAbsolutePath()) + " " + frameSkip + "-f image2 -frames:v 1 "
				+ escapeFilepath(DRI.dir + File.separator + "frame-cache" + File.separator + "frame-" + job.video.getNickname() + ".png");
		try
		{
			logger.info("Command> {}", command);
			Process r = Runtime.getRuntime().exec(command);
			StreamGobbler reader = new StreamGobbler(r.getInputStream(), true);
			StreamGobbler eater = new StreamGobbler(r.getErrorStream(), true);
			reader.start();
			eater.start();
			int exitValue = -1;
			boolean waiting = false;
			do
			{
				waiting = false;
				try
				{
					exitValue = r.waitFor();//Should not be interrupted unless forcibly
				}
				catch (InterruptedException e2)
				{
					if (forcefulShutdown)
					{
						logger.warn("Terminating Extraction Forcibly");
						RequestBuffer.request(() -> {
							job.message.getChannel().sendMessage(":no_entry: Terminating Request, Frame Extraction was manually disabled");
						});
						return false;
					}
					waiting = true;
				}
			}
			while (waiting);
			if (exitValue == 0)
			{
				long completionTimestamp = Instant.now().toEpochMilli();
				logger.info("Process Completed");
				File frameFile = new File(DRI.dir + File.separator + "frame-cache" + File.separator + "frame-" + job.video.getNickname() + ".png");
				if (frameFile.exists())
				{
					EmbedBuilder frameEmbed = new EmbedBuilder();
					frameEmbed.withColor(new Color(152, 146, 251));
					frameEmbed.withAuthorIcon("https://media.foxtrotfanatics.info/i/ftf_logo.png");
					frameEmbed.withAuthorName("FoxTrot Fanatics");
					frameEmbed.withAuthorUrl("https://foxtrotfanatics.info");
					if (job.link != null)
					{
						frameEmbed.withTitle(job.link.getTitle());
						if (job.link.getDescription() != null)
							frameEmbed.withDesc(job.link.getDescription());
						frameEmbed.withUrl(job.link.getLink());
					}
					frameEmbed.withTimestamp(job.message.getCreationDate());
					frameEmbed.withFooterIcon(job.author.getAvatarURL());
					frameEmbed.withFooterText("Requested By: \"" + job.author.getDisplayName(job.message.getGuild()) + "\"");
					frameEmbed.appendField("Video name", job.video.getNickname(), true);
					frameEmbed.appendField("Timecode", convertMilliToTime(job.timecode), true);
					if (job.frameCount != null)
						frameEmbed.appendField("Frame Offset", String.valueOf(job.frameCount), true);
					//TODO Moment Field Inline
					frameEmbed.appendField("Accessibility", job.video.isRestricted() ? ":large_orange_diamond:" : ":arrow_forward:", false);
					frameEmbed.withImage("attachment://" + frameFile.getName());
					if (job.elevated)
					{
						frameEmbed.appendField("User Extraction Count", (job.user.getUsedToday() + 1) + " :large_orange_diamond:", true);
						frameEmbed.appendField("Server Extraction Count", "unaffected (" + (job.guild.getUsedToday()) + ")", true);
					}
					else
					{
						frameEmbed.appendField("User Extraction Count", (job.user.getUsedToday() + 1 >= job.maxUserCount ? ":arrow_up: " : "")
								+ (job.user.getUsedToday() + 1) + "/" + job.maxUserCount, true);
						frameEmbed.appendField("Server Extraction Count",
								(job.guild.getUsedToday() + 1 >= job.guild.getRequestLimit() ? ":arrow_up: " : "") + (job.guild.getUsedToday() + 1)
										+ "/" + job.guild.getRequestLimit(),
								true);
					}
					IMessage message = RequestBuffer.request(() -> {
						try
						{
							MessageBuilder b = new MessageBuilder(client);
							b.withChannel(job.message.getChannel());
							b.withContent(job.author.mention());
							b.withFile(frameFile);
							b.withEmbed(frameEmbed.build());
							return b.build();
						}
						catch (FileNotFoundException e)
						{
							logger.error("File Not Found: {}", frameFile.getAbsolutePath());
							logger.catching(e);
						}
						return null;
					}).get();
					if (message != null)
					{
						logger.info("{} Uploaded", job.video.getNickname());
					}
					else
					{
						job.message.getChannel().sendMessage("Frame was not Extracted!");
					}
					db.declareExtraction(completionTimestamp, job.elevated, job.author.getLongID(), job.message.getGuild().getLongID(),
							job.message.getChannel().getLongID(), job.message.getLongID(), job.video.getFilename(), startTime, job.frameCount,
							message.getEmbeds().get(0).getImage().getUrl());
				}
				else
				{
					RequestBuffer.request(() -> {
						job.message.getChannel().sendMessage("Frame was not Extracted!");
					});
					return false;
				}
				return true;
			}
			else
			{
				logger.error("Process Failed, Error: {}", exitValue);
				RequestBuffer.request(() -> {
					job.message.getChannel().sendMessage(":no_entry_sign: Extraction Failed, check Console for errors");
				});
				return false;
			}
		}
		catch (IOException e1)
		{
			logger.catching(e1);
			RequestBuffer.request(() -> {
				job.message.getChannel().sendMessage(":radioactive: Could not start Process.");
			});
			return false;
		}

	}

	public boolean requestSingleVerification(CommandContext ctx, String filename)
	{
		if (running)
		{
			File video = new File(prop.getProperty("Video_Directory") + File.separator + filename);
			if (!video.exists())
			{
				logger.warn("Video Not Found: {}", video.getAbsolutePath());
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Video Not Found");
				});
				return false;
			}
			if (verifications.offer(new TimestampJob(ctx.getChannel(), filename)))
			{
				RequestBuffer.request(() -> {
					ctx.getMessage().addReaction(UserActivity.ok);
					return 0;
				});
				return true;
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":radioactive: Verification Request Buffer maxed");
				});
				return false;
			}
		}
		else
		{
			RequestBuffer.request(() -> {
				ctx.getChannel().sendMessage(":no_entry: All Extraction Extraction Activity is currently Disabled");
			});
			return false;
		}
	}

	/**
	 * Updates the Video Duration and Frame rate of a file in the Database
	 * @param job Data containing the Channel to respond to, and the filename
	 * @return if the extraction was successful or not
	 */
	private boolean extractMetadata(TimestampJob job)
	{
		File video = new File(prop.getProperty("Video_Directory") + File.separator + job.filename);
		String nickname = job.filename.substring(0, job.filename.lastIndexOf("."));
		//Don't Change Presence per extraction, too likely to be rate limited.
		/*
		 * <frames>/<second>
		 * [video duration] <-Priority
		 * [format duration]
		 */
		String command = "" + escapeFilepath(prop.getProperty("FFprobe_Path"))
				+ " -v error -show_entries format=duration:stream=r_frame_rate:stream=duration -select_streams v:0 -print_format default=noprint_wrappers=1:nokey=1 -sexagesimal "
				+ escapeFilepath(video.getAbsolutePath());
		try
		{
			logger.info("Command: >{}", command);
			Process r = Runtime.getRuntime().exec(command);
			StreamGobbler eater = new StreamGobbler(r.getErrorStream(), true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(r.getInputStream()));
			eater.start();
			int exitValue = -1;
			boolean waiting = false;
			do
			{
				waiting = false;
				try
				{
					exitValue = r.waitFor();//Should not be interrupted unless forcibly
				}
				catch (InterruptedException e2)
				{
					if (forcefulShutdown)
					{
						logger.warn("Terminating Verification Forcibly");
						return false;
					}
					waiting = true;
				}
			}
			while (waiting);
			if (exitValue == 0)
			{
				logger.debug("Process Completed");
				String fps = reader.readLine();
				String duration = reader.readLine();
				if (duration == null || fps == null)//No Video Stream Found
				{
					logger.warn("File `{}` has no Video Streams, hiding in Database");
					db.setVideoUnusable(nickname, false);
					return false;
				}
				else if (duration.equals("N/A"))//Container does not support per stream durations
				{
					duration = reader.readLine();
				}
				else//OK
				{
					reader.readLine();
				}
				while (reader.readLine() != null)//Should be none
				{
					logger.warn("Extra Input Found while probing {}{}{}", prop.getProperty("Video_Directory"), File.separator, job.filename);
				}
				long milliDuration = convertTimeToMilli(duration, null, null);
				if (db.addOrUpdateVideo(job.filename, nickname, milliDuration, 0, fps, null))
				{
					return true;
				}
				else
				{
					logger.error("Could not add File {}, conflicts with prexisting nickname {}", job.filename, nickname);
					return false;
				}
			}
			else
			{
				logger.error("Process Failed, Error: {}", exitValue);
				RequestBuffer.request(() -> {
					job.channel.sendMessage("Probing Failed, check Console for errors");
				});
				db.setVideoUnusable(nickname, false);
			}
		}
		catch (IOException e1)
		{
			logger.catching(e1);
			RequestBuffer.request(() -> {
				job.channel.sendMessage("Could not start Probing Process for File: " + job.filename);
			});
			db.setVideoUnusable(nickname, false);
		}
		return false;
	}

	public static String convertMilliToTime(long time)
	{
		boolean wasNegative = false;
		if (time < 0)
		{
			wasNegative = true;
			time = -time;
		}
		String hours = String.format("%02d", (time / (1000 * 60 * 60)));
		String minutes = String.format("%02d", (time / (1000 * 60)) % 60);
		String seconds = String.format("%02d", (time / 1000) % 60);
		String milli = String.format("%03d", time % 1000);
		String negative = "";
		if (wasNegative)
		{
			negative = "-";
		}
		return negative + hours + ":" + minutes + ":" + seconds + "." + milli;
	}

	/**
	 * Analyze String to convert to milliseconds if in this format <...###:><##:><#>#<.###>
	 * @param text The text to Verify
	 * @param frameCount Extra Time to add based on Framerate
	 * @param framerate Rate of Frames to use for framecount
	 * @return A signed long in milliseconds
	 * @throws IllegalArgumentException If Text is improperly formatted
	 */
	public static long convertTimeToMilli(String text, Integer frameCount, String framerate) throws IllegalArgumentException
	{
		try
		{
			long timecode = 0;
			String hoursT = "0";
			String minutesT = "0";
			String secondsT = "0";
			String millisecondsT = "0";
			int decimalIndex = text.lastIndexOf('.');
			if (decimalIndex == -1)
				decimalIndex = text.length();
			int firstIndex = 0;
			if (text.startsWith("-"))
			{
				firstIndex = 1;
			}
			int secondsColonIndex = text.lastIndexOf(':', decimalIndex);
			secondsT = text.substring(Math.max(secondsColonIndex + 1, firstIndex), decimalIndex);
			if (secondsColonIndex >= firstIndex)//Minutes should exist
			{
				int minutesColonIndex = text.lastIndexOf(':', secondsColonIndex - 1);
				minutesT = text.substring(Math.max(minutesColonIndex + 1, firstIndex), secondsColonIndex);
				if (minutesColonIndex >= firstIndex)
				{
					hoursT = text.substring(firstIndex, minutesColonIndex);
				}
			}
			if (decimalIndex < text.length() - 1)
			{
				millisecondsT = text.substring(decimalIndex + 1, Math.min(decimalIndex + 4, text.length()));
			}
			try
			{
				timecode += Long.parseLong(hoursT) * 3600000;
			}
			catch (NumberFormatException e)
			{
				throw new IllegalArgumentException("Hours `" + hoursT + "` is not a valid number in Timecode: <" + text + ">");
			}
			try
			{
				timecode += Long.parseLong(minutesT) * 60000;
			}
			catch (NumberFormatException e)
			{
				throw new IllegalArgumentException("Minutes `" + minutesT + "` is not a valid number in Timecode: <" + text + ">");
			}
			try
			{
				timecode += Long.parseLong(secondsT) * 1000;
			}
			catch (NumberFormatException e)
			{
				throw new IllegalArgumentException("Seconds `" + secondsT + "` is not a valid number in Timecode: <" + text + ">");
			}
			try
			{
				timecode += Long.parseLong(millisecondsT);
			}
			catch (NumberFormatException e)
			{
				throw new IllegalArgumentException("Milliseconds `" + millisecondsT + "` is not a valid number in Timecode: <" + text + ">");
			}
			if (framerate != null && frameCount != null)
			{
				double numerator = Double.valueOf(framerate.substring(0, framerate.indexOf("/")));
				double denominator = Double.valueOf(framerate.substring(framerate.indexOf("/") + 1));
				timecode += Math.round(((denominator / numerator) * frameCount * 1000));
				//FPS = 25/1 & 25 Frames Extra -> (1/25) * 25 * 1000 = 1000 milliseconds
			}
			if (firstIndex == 1)
			{
				timecode = -timecode;
			}
			return timecode;
		}
		catch (IndexOutOfBoundsException e)
		{
			throw new IllegalArgumentException("Error Parsing Timecode, Check Console :radioactive:");
		}
	}

	public void normalStatus()
	{
		try
		{
			int videoCount = db.getVisibleVideoCount(false);
			if (videoCount > 0)
				client.changePresence(StatusType.ONLINE, ActivityType.WATCHING, videoCount + " videos");
			else
				client.changePresence(StatusType.DND, ActivityType.WATCHING, "nothing");
		}
		catch (NullPointerException f)
		{
			logger.error("Database not initalized yet", f);
			client.changePresence(StatusType.INVISIBLE);
		}
	}
	
	public static String escapeFilepath(String path)
	{
		String os = System.getProperty("os.name");
		if (os.equalsIgnoreCase("Linux") || os.equalsIgnoreCase("Mac OS X"))
		{
			return path.trim().replaceAll(" ", "\\ ");
		}
		else if (os.startsWith("Win"))
		{
			return '\"' + path.trim() + '\"';
		}
		else
		{
			return path.trim();
		}
	}

	public static long localSecondsUntilMidnight()
	{
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime midnight = LocalDate.now().plusDays(1).atStartOfDay();
		return Duration.between(now, midnight).getSeconds();
	}

	private class TimestampJob
	{

		private IChannel channel;
		private String filename;

		public TimestampJob(IChannel c, String f)
		{
			channel = c;
			filename = f;
		}
	}

	private class ExtractionJob
	{

		private IMessage message;
		private IUser author;
		private DBGuild guild;
		private DBNormalUser user;
		private DBLink link;
		private DBVideo video;
		private long timecode;
		//Offset in DBVideo
		private boolean useOffset;
		private Integer frameCount;
		private int maxUserCount;
		private boolean elevated;

		public ExtractionJob(IMessage m, IUser a, DBGuild g, DBNormalUser u, DBLink l, DBVideo v, long t, boolean o, Integer c, int mu, boolean e)
		{
			message = m;
			author = a;
			guild = g;
			user = u;
			link = l;
			video = v;
			timecode = t;
			useOffset = o;
			frameCount = c;
			maxUserCount = mu;
			elevated = e;
		}
	}
}
