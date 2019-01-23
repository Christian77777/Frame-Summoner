
package com.Christian77777.Frame_Summoner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import com.Christian77777.Frame_Summoner.Database.NormalUser;
import com.Christian77777.Frame_Summoner.Database.Video;
import com.darichey.discord.CommandContext;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuffer.RequestFuture;

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
			db.resetDailyUsage(0, null);
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
							job.channel.sendMessage(":no_entry: Cancelling Request, Frame Extraction was manually disabled");
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
				c.sendMessage("Frame-Summoner is disabled, please ensable it first!");
			});
			return false;
		}
	}

	/**
	 * Submit Request to Extract Frame. If successful (video exists, timecode within length), request added to Queue until
	 * worker thread can take the request
	 * Allows Discord Communication thread to return quickly.
	 * @param ctx
	 * @param filename
	 * @param timecode
	 * @param frameCount
	 * @return
	 */
	public boolean requestFrame(CommandContext ctx, String filename, String timecode, Integer frameCount, boolean useOffset)
	{
		if (running)
		{
			File video = new File(prop.getProperty("Video_Directory") + File.separator + filename);
			NormalUser person = db.getUserUsage(ctx.getAuthor().getLongID());
			int maxUserExtracts = Integer.valueOf(prop.getProperty("MaxUserExtracts"));
			//Permission Denied if Globally banned.
			if (person != null)
			{
				//Permission Denied if Globally banned.
				if (person.isBanned())
				{
					RequestBuffer.request(() -> {
						ctx.getAuthor().getOrCreatePMChannel().sendMessage(":no_entry_sign: You are banned interacting with Frame-Summoner");
					});
					return false;
				}
				//Permission Denied if not VIP and over the maximum number of extractions
				if (person.getUsedToday() >= maxUserExtracts && !(person.isVip() || person.getId() == Long.parseLong(prop.getProperty("Bot_Manager"))))
				{
					RequestBuffer.request(() -> {
						ctx.getAuthor().getOrCreatePMChannel().sendMessage(":clock3: You have reached the max of `" + maxUserExtracts
								+ "` Frames per day. Reset in `" + convertMilliToTime(localSecondsUntilMidnight() * 1000) + "`");
					});
					return false;
				}
			}
			Video data = db.getVideoData(filename);
			//Extraction cancelled if video is not even recorded at all
			if (data == null || !data.isUsable())
			{
				logger.warn("Video Not Found: {}", video.getAbsolutePath());
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":warning: Video Not Found");
				});
				return false;
			}
			//Permission denied if Video is not visible with the users current permission level
			if (data.isRestricted() && !person.isVip())
			{
				logger.warn("Video Not Accessible: {}", video.getAbsolutePath());
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage(":warning: Video Not Found");
				});
				return false;
			}
			long timestamp = convertTimeToMilli(timecode, frameCount, data.getFps());
			if (useOffset && data.getOffset() != 0)//If Offset, and requested, add offset to timecode
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
			if (queue.offer(new ExtractionJob(ctx.getChannel(), ctx.getAuthor(), filename, timecode, frameCount, person)))//Add to Queue if not too full
			{
				RequestBuffer.request(() -> {
					ctx.getMessage().addReaction(UserActivity.confirm);
					return 0;
				});
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
		File video = new File(prop.getProperty("Video_Directory") + File.separator + job.filename);
		if (!video.exists())//If Video deleted
		{
			logger.error("Video Not Found: {}", video.getAbsolutePath());
			RequestBuffer.request(() -> {
				job.channel.sendMessage("Source Video Removed!");
			});
			return false;
		}
		job.channel.getClient().changePresence(StatusType.IDLE, ActivityType.PLAYING, job.filename);
		RequestBuffer.request(() -> {
			job.channel.sendMessage("Summoning Frame...");
		}).get();
		String framecut = "";
		if (job.frameCount != null)
		{
			framecut = "-filter:v \"select=gte(n\\," + job.frameCount + ")\" ";
		}
		//Extraction by exact time code: ffmpeg -ss ##:##:##.### -i "C:\Path\to\Video" -t 1 -f image2 -frames:v 1 "C:\Path\to\frame-videofilename.png"
		//Extraction by Timestamp and frame number in range [0,fps):ffmpeg -ss ##:##:## -i "C:\Path\to\Video" -filter:v "select=gte(n/,%%FRAMENUMBER)" -f image2 -frames:v 1 "C:\Path\to\frame-videofilename.png"
		String command = prop.getProperty("FFmpeg_Path") + " -loglevel quiet -y -ss " + job.timecode + " -i \"" + video.getAbsolutePath() + "\" "
				+ framecut + "-f image2 -frames:v 1 \"" + DRI.dir + File.separator + "frame-cache" + File.separator + "frame-" + job.filename
				+ ".png\"";
		try
		{
			logger.info("Command: ({})", command);
			Process r = Runtime.getRuntime().exec(command);
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
							job.channel.sendMessage(":no_entry: Terminating Request, Frame Extraction was manually disabled");
						});
						File frame = new File(DRI.dir + File.separator + "frame-" + job.filename + ".png");
						if (frame.exists())
							frame.delete();
						return false;
					}
					waiting = true;
				}
			}
			while (waiting);
			if (exitValue == 0)
			{
				logger.info("Process Completed");
				File frame = new File(DRI.dir + File.separator + "frame-cache" + File.separator + "frame-" + job.filename + ".png");
				IMessage message = RequestBuffer.request(() -> {
					try
					{
						return job.channel.sendFile(job.author.mention() + " Frame from video " + job.filename + " at " + job.timecode, frame);
					}
					catch (FileNotFoundException e)
					{
						logger.error("File Not Found: {}", frame.getAbsolutePath());
						logger.catching(e);
					}
					return null;
				}).get();
				if (message == null)
				{
					RequestBuffer.request(() -> {
						job.channel.sendMessage("Frame was not Extracted!");
					});
					return false;
				}
				else
				{
					db.declareExtraction(job.author.getLongID(), job.channel.getGuild().getLongID(), job.filename, job.timecode, job.frameCount,
							message.getAttachments().get(0).getUrl());

				}
				logger.info("{} Uploaded", job.filename);
				return true;
			}
			else
			{
				logger.error("Process Failed, Error: {}", exitValue);
				RequestBuffer.request(() -> {
					job.channel.sendMessage(":no_entry_sign: Extraction Failed, check Console for errors");
				});
				return false;
			}
		}
		catch (IOException e1)
		{
			logger.catching(e1);
			RequestBuffer.request(() -> {
				job.channel.sendMessage(":radioactive: Could not start Process.");
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
					ctx.getMessage().addReaction(UserActivity.confirm);
					return 0;
				});
				return true;
			}
			else
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Overloaded with requests, please try again later");
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
		//Don't Change Presence per extraction, too likely to be rate limited.
		/*
		 * <frames>/<second>
		 * [video duration] <-Priority
		 * [format duration]
		 */
		String command = prop.getProperty("FFprobe_Path")
				+ " -v error -show_entries format=duration:stream=r_frame_rate:stream=duration -select_streams v:0 -print_format default=noprint_wrappers=1:nokey=1 -sexagesimal \""
				+ video.getAbsolutePath() + "\"";
		try
		{
			logger.info("Command: >{}", command);
			Process r = Runtime.getRuntime().exec(command);
			StreamGobbler eater = new StreamGobbler(r.getErrorStream(), false);
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
					db.setVideoUnusable(job.filename, false);
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
				db.addOrUpdateVideo(job.filename, milliDuration, 0, fps, null);
				return true;
			}
			else
			{
				logger.error("Process Failed, Error: {}", exitValue);
				RequestBuffer.request(() -> {
					job.channel.sendMessage("Probing Failed, check Console for errors");
				});
				db.setVideoUnusable(job.filename, false);
			}
		}
		catch (IOException e1)
		{
			logger.catching(e1);
			RequestBuffer.request(() -> {
				job.channel.sendMessage("Could not start Probing Process for File: " + job.filename);
			});
			db.setVideoUnusable(job.filename, false);
		}
		return false;
	}

	public static String convertMilliToTime(long time)
	{
		String hours = String.format("%02d", (time / (1000 * 60 * 60)) % 24);
		String minutes = String.format("%02d", (time / (1000 * 60)) % 60);
		String seconds = String.format("%02d", (time / 1000) % 60);
		String milli = String.format("%03d", time % 1000);
		return hours + ":" + minutes + ":" + seconds + "." + milli;
	}

	public static long convertTimeToMilli(String time, Integer frameCount, String framerate)
	{
		try
		{
			int index = time.indexOf(":");
			long milli = Integer.valueOf(time.substring(0, index)) * 3600000;
			milli += Integer.valueOf(time.substring(index + 1, index + 3)) * 60000;
			milli += Integer.valueOf(time.substring(index + 4, index + 6)) * 1000;
			if ((index = time.indexOf(".")) > 6)
				milli += Integer.valueOf(time.substring(index + 1, Math.min(time.length(), index + 4)));
			if (framerate != null && frameCount != null)
			{
				double numerator = Double.valueOf(framerate.substring(0, framerate.indexOf("/")));
				double denominator = Double.valueOf(framerate.substring(framerate.indexOf("/") + 1));
				milli += Math.round(((numerator / denominator) * frameCount));
			}
			return milli;
		}
		catch (IndexOutOfBoundsException | NumberFormatException e)
		{
			throw new IllegalArgumentException();
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
		private IChannel channel;
		private IUser author;
		private String filename;
		private String timecode;
		private Integer frameCount;
		private NormalUser person;

		public ExtractionJob(IChannel c, IUser u, String f, String t, Integer n, NormalUser p)
		{
			channel = c;
			author = u;
			filename = f;
			timecode = t;
			frameCount = n;
			person = p;
		}
	}
}
