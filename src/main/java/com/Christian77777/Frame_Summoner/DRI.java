/**
 * Copyright 2018 Christian Devile
 * 
 * This file is part of Frame-Summoner.
 * 
 * Frame-Summoner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Frame-Summoner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Frame-Summoner. If not, see <http://www.gnu.org/licenses/>.
 */

package com.Christian77777.Frame_Summoner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.Christian77777.Frame_Summoner.Database.Database;
import com.Christian77777.Frame_Summoner.Database.DBChannel;
import com.Christian77777.Frame_Summoner.Database.DBVideo;
import com.Christian77777.Frame_Summoner.Listeners.ChannelDeleted;
import com.Christian77777.Frame_Summoner.Listeners.GuildSetup;
import com.darichey.discord.CommandContext;
import com.darichey.discord.CommandListener;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;

/**
 * @author Christian
 *
 */
public class DRI implements IListener<ReadyEvent>
{

	private static Logger logger;
	public static String dir;
	private static FileLock lock;
	public static final String version = new String("2.2.0");
	public static final String prefix = new String("fs!");
	public static LocalInterface menu;
	private UserActivity instructions;
	private IDiscordClient api;
	private CommandListener actions;
	private Properties prop;
	private Database db;

	/**
	 * @param args
	 * @throws URISyntaxException
	 */
	public static void main(String[] args) throws URISyntaxException
	{
		File temp = new File(DRI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		if (temp.getAbsolutePath().endsWith("jar"))
			dir = temp.getParent();
		else
			dir = temp.getAbsolutePath();
		System.setProperty("directory", dir);
		logger = LogManager.getLogger();
		logger.info("Directory resolved to: {}", dir);
		logger.info("Running on OS: {}", System.getProperty("os.name"));
		checkIfSingleInstance();
		//Read Config and refresh database local data before interfacing with Discord
		DRI controller = new DRI();
		//Connect to Discord, refresh database remote data, establish bot commands, and Start UI
		controller.connectToDiscord();
	}

	/**
	 * Read in the Config,
	 * Create the Frame-cache folder if necessary
	 * Establish the Database connection
	 * Refresh Video Files
	 * End with Terminating Connection Call
	 */
	public DRI()
	{
		readConfig();
		File f = new File(dir + File.separator + "frame-cache");
		if (!f.exists())
		{
			if (!f.mkdir())
			{
				logger.fatal("Could not create Frame-cache folder");
				System.exit(4);
			}
		}
		db = new Database(dir);
		refreshVideos();
		menu = new LocalInterface(this);
	}

	public void connectDatabase()
	{
		db = new Database(dir);
	}

	/**
	 * Establish Connection to Discord and terminate thread.
	 */
	public void connectToDiscord()
	{
		if (api != null)
		{
			api.logout();
			try
			{
				Thread.sleep(2000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		ClientBuilder clientBuilder = new ClientBuilder();
		clientBuilder.withToken(prop.getProperty("Discord_Token"));
		clientBuilder.registerListener(new GuildSetup(db, prop, prefix));
		clientBuilder.registerListener(new ChannelDeleted(db));
		clientBuilder.registerListener(this);
		clientBuilder.registerListener(menu);
		try
		{
			api = clientBuilder.login();//Thread Complete, does not block
		}
		catch (DiscordException e)
		{
			logger.fatal("Failed To Connect: Probably a bad Discord Token", e);
			System.exit(21);
		}
	}

	@Override
	public void handle(ReadyEvent event)
	{
		Thread.currentThread().setName("DiscordConnectionThread");
		this.api = event.getClient();
		logger.info("Connected to Discord");
		instructions = new UserActivity(this, api, db, prop, prefix);
		actions = new CommandListener(instructions.getRegistry());
		api.getDispatcher().registerListener(actions);
	}

	public void disconnect(String message)
	{
		for (IChannel c : getAnnoucementChannels())
		{
			RequestBuffer.request(() -> {
				c.sendMessage(message);
			});
		}
		api.getDispatcher().unregisterListener(actions);
		api.logout();
		db.closeDatabase();
		api = null;
		actions = null;
		db = null;
	}

	public void editVideoDirectory(String path, boolean alertDiscord, CommandContext ctx)
	{
		prop.setProperty("Video_Directory", path);
		try
		{
			FileOutputStream prop_file = new FileOutputStream(DRI.dir + File.separator + "config.properties");
			prop.store(prop_file, "Frame-Summoner Properties");
			instructions.updateProperties(prop);
		}
		catch (IOException e)
		{
			logger.fatal("Could Not Save to config.properties", e);
			System.exit(20);
		}
	}

	/**
	 * If video removed, mark as unusable
	 * If new video found, ignore, require manual addition.
	 * Just remark its existence in the console.
	 * Does not read video, 
	 */
	public void refreshVideos()
	{
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
				if (comparision > 0)//cache missing item, from realFiles
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
					rIndex++;
					lIndex++;
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
			logger.info("Database is fresh and has no videos, make sure verify them before usage");
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
			logger.info("Lost Files: {}", Arrays.toString(unusable.toArray()));
			logger.info("New Files Found: {}", Arrays.toString(newFiles.toArray()));
			logger.info("Currently Disabled Videos: {}", Arrays.toString(db.getDisabledList().toArray()));
		}
	}

	public ArrayList<IChannel> getAnnoucementChannels()
	{
		ArrayList<IChannel> channels = new ArrayList<IChannel>();
		for (DBChannel c : db.getServerChannels(null))
		{
			if (c.isAnnoucement())
			{
				channels.add(api.getChannelByID(c.getId()));
			}
		}
		return channels;
	}

	public void readConfig()
	{
		try (InputStream defaultInput = getClass().getClassLoader().getResourceAsStream("default.properties");)
		{
			Properties defaults = new Properties();
			if (defaultInput != null)
			{
				defaults.load(defaultInput);
			}
			else
			{
				logger.fatal("Main Property file not found in the classpath");
				System.exit(40);
			}
			try (FileInputStream actualInput = new FileInputStream(new File(dir + File.separator + "config.properties"));)
			{
				prop = new Properties(defaults);
				prop.load(actualInput);
				boolean readFailed = false;
				for (Entry<Object, Object> entry : defaults.entrySet())
				{
					try
					{
						//Default Value matched
						if (prop.getProperty((String) entry.getKey()).equals((String) entry.getValue()))
						{
							logger.error("Config not properly configured, still has default values\nKey: `{}` has Value `{}`",
									(String) entry.getKey(), (String) entry.getValue());
							prop.remove(entry.getKey());//Remove Key to not double dip on the validation error
							readFailed = true;
						}
					}
					catch (NullPointerException a)
					{
						logger.error(
								"Config not properly configured, Key: `{}` does not exist\nDelete \"config.properties\" to regenerate the properties on the next program start",
								(String) entry.getKey());
						readFailed = true;
					}
				}
				//Properties Specific Validation
				String value = prop.getProperty("Bot_Manager");
				if (value != null)
				{
					try
					{
						Long.parseLong(value);
					}
					catch (NumberFormatException e)
					{
						logger.error("Config not properly configured, \nKey: `Bot_Manager` is not a parsable SnowflakeID");
						readFailed = true;
					}
				}
				value = prop.getProperty("MaxUserExtracts");
				if (value != null)
				{
					try
					{
						Byte.parseByte(value);
					}
					catch (NullPointerException | NumberFormatException e)
					{
						logger.error("Config not properly configured, \nKey: `MaxUserExtracts` is not a number less then 128");
						readFailed = true;
					}
				}
				value = prop.getProperty("MaxServerExtracts");
				if (value != null)
				{
					try
					{
						Integer.parseInt(value);
					}
					catch (NullPointerException | NumberFormatException e)
					{
						logger.error("Config not properly configured, \nKey: `MaxServerExtracts` is not a number less then 2 million");
						readFailed = true;
					}
				}
				value = prop.getProperty("FFmpeg_Path");
				if (value != null)
				{
					try
					{
						Process x = Runtime.getRuntime().exec(Extractor.escapeFilepath(value));
						StreamGobbler reader = new StreamGobbler(x.getInputStream(), false);
						StreamGobbler eater = new StreamGobbler(x.getErrorStream(), false);
						reader.start();
						eater.start();
						if (x.waitFor(100, TimeUnit.MILLISECONDS))
							logger.info("{} recognized", value);
						else
							logger.info("{} accepted", value);

					}
					catch (InterruptedException | IOException e)
					{
						logger.error("{} rejected", value);
						logger.fatal("File Path for FFmpeg is not an executable, please put the correct path in the values.txt file and restart.");
						readFailed = true;
					}
				}
				value = prop.getProperty("FFprobe_Path");
				if (value != null)
				{
					try
					{
						Process x = Runtime.getRuntime().exec(Extractor.escapeFilepath(value));
						StreamGobbler reader = new StreamGobbler(x.getInputStream(), false);
						StreamGobbler eater = new StreamGobbler(x.getErrorStream(), false);
						reader.start();
						eater.start();
						if (x.waitFor(100, TimeUnit.MILLISECONDS))
							logger.info("{} recognized", value);
						else
							logger.info("{} accepted", value);

					}
					catch (InterruptedException | IOException e)
					{
						logger.error("{} rejected", value);
						logger.fatal("File Path for FFprobe is not an executable, please put the correct path in the values.txt file and restart.");
						readFailed = true;
					}
				}
				value = prop.getProperty("Video_Directory");
				if (value != null && !new File(value).exists() && !new File(value).isDirectory())
				{
					logger.error("Config not properly configured, \nKey: `Video_Directory` is not a valid file path");
					readFailed = true;
				}
				value = prop.getProperty("AllowDirectoryChange");
				if (value != null && !value.equals("Y") && !value.equals("N"))
				{
					logger.error("Config not properly configured, \nKey: `AllowDirectoryChange` is not either a 'Y' or 'N'");
					readFailed = true;
				}
				if (readFailed)
					System.exit(21);
				else
				{
					String response = new String("");
					for (Entry<Object, Object> entry : prop.entrySet())
					{
						String key = (String) entry.getKey();
						String val = (String) entry.getValue();
						if (key.equals("Discord_Token"))
						{
							val = "..." + val.substring(val.length() - 6, val.length() - 1);
						}
						response += "\n\t" + key + ": " + val;

					}
					logger.info("Frame-Summoner Properties{}", response);
				}
				if (instructions != null)
				{
					instructions.updateProperties(prop);
				}
			}
			catch (IOException b)
			{
				//Create Properties File
				try (FileOutputStream newProperties = new FileOutputStream(new File(dir + File.separator + "config.properties")))
				{
					defaults.store(newProperties, "Frame-Summoner Properties File");
					logger.warn("Created values.txt\nPlease Edit before Using Program\n Omit Quotation Marks in File Paths");
				}
				catch (IOException c)
				{
					logger.fatal("Failed to Generate values.txt: ", c);
				}
				System.exit(0);
			}
		}
		catch (IOException e1)
		{
			logger.fatal("IOException reading default config.properties", e1);
			System.exit(40);
		}
	}

	@SuppressWarnings("resource")
	public static void checkIfSingleInstance()
	{
		FileChannel channel;
		try
		{
			File lockedFile = new File(dir + File.separator + "running");
			channel = new RandomAccessFile(lockedFile, "rw").getChannel();

			try
			{

				try
				{
					lock = channel.tryLock();
				}
				catch (OverlappingFileLockException e)
				{
					// already locked
					logger.fatal("Instance Already Running!");
					System.exit(5);
				}

				if (lock == null)
				{
					// already locked
					logger.fatal("Instance Already Running!");
					System.exit(5);
				}

				Runtime.getRuntime().addShutdownHook(new Thread()
				{

					// destroy the lock when the JVM is closing
					public void run()
					{
						try
						{
							lock.release();
						}
						catch (Exception e5)
						{
						}
						try
						{
							channel.close();
						}
						catch (Exception e6)
						{
						}
						try
						{
							lockedFile.delete();
						}
						catch (Exception e7)
						{
						}
					}
				});
			}
			catch (IOException e8)
			{
				// already locked
				logger.fatal("Instance Already Running!");
				System.exit(5);
			}
		}
		catch (FileNotFoundException e1)
		{
			e1.printStackTrace();
		}
	}

	public static URL getLocation(final Class<?> c)
	{
		if (c == null)
			return null; // could not load the class

		// try the easy way first
		try
		{
			final URL codeSourceLocation = c.getProtectionDomain().getCodeSource().getLocation();
			if (codeSourceLocation != null)
				return codeSourceLocation;
		}
		catch (final SecurityException e)
		{
			// NB: Cannot access protection domain.
		}
		catch (final NullPointerException e)
		{
			// NB: Protection domain or code source is null.
		}

		// NB: The easy way failed, so we try the hard way. We ask for the class
		// itself as a resource, then strip the class's path from the URL
		// string,
		// leaving the base path.

		// get the class's raw resource path
		final URL classResource = c.getResource(c.getSimpleName() + ".class");
		if (classResource == null)
			return null; // cannot find class resource

		final String url = classResource.toString();
		final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
		if (!url.endsWith(suffix))
			return null; // weird URL

		// strip the class's path from the URL string
		final String base = url.substring(0, url.length() - suffix.length());

		String path = base;

		// remove the "jar:" prefix and "!/" suffix, if present
		if (path.startsWith("jar:"))
			path = path.substring(4, path.length() - 2);

		try
		{
			return new URL(path);
		}
		catch (final MalformedURLException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Converts the given {@link URL} to its corresponding {@link File}.
	 * <p>
	 * This method is similar to calling {@code new File(url.toURI())} except
	 * that it also handles "jar:file:" URLs, returning the path to the JAR
	 * file.
	 * </p>
	 * 
	 * @param url
	 * The URL to convert.
	 * @return A file path suitable for use with e.g. {@link FileInputStream}
	 * @throws IllegalArgumentException
	 * if the URL does not correspond to a file.
	 */
	public static File urlToFile(final URL url)
	{
		return url == null ? null : urlToFile(url.toString());
	}

	/**
	 * Converts the given URL string to its corresponding {@link File}.
	 * 
	 * @param url
	 * The URL to convert.
	 * @return A file path suitable for use with e.g. {@link FileInputStream}
	 * @throws IllegalArgumentException
	 * if the URL does not correspond to a file.
	 */
	public static File urlToFile(final String url)
	{
		String path = url;
		if (path.startsWith("jar:"))
		{
			// remove "jar:" prefix and "!/" suffix
			final int index = path.indexOf("!/");
			path = path.substring(4, index);
		}
		try
		{
			String os = System.getProperty("os.name");
			if (os.equals("Window 10") && path.matches("file:[A-Za-z]:.*"))
			{
				path = "file:/" + path.substring(5);
			}
			return new File(new URL(path).toURI());
		}
		catch (final MalformedURLException e)
		{
			// NB: URL is not completely well-formed.
		}
		catch (final URISyntaxException e)
		{
			// NB: URL is not completely well-formed.
		}
		if (path.startsWith("file:"))
		{
			// pass through the URL as-is, minus "file:" prefix
			path = path.substring(5);
			return new File(path);
		}
		throw new IllegalArgumentException("Invalid URL: " + url);
	}
}
