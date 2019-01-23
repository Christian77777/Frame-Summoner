/**
 * Copyright 2018 Christian Devile
 * 
 * This file is part of FTUServerBot.
 * 
 * FTUServerBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FTUServerBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with FTUServerBot. If not, see <http://www.gnu.org/licenses/>.
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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.darichey.discord.CommandContext;
import com.darichey.discord.CommandListener;
import Listeners.ChannelDeleted;
import Listeners.GuildSetup;
import Listeners.NewChannel;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageSendEvent;
import sx.blah.discord.util.DiscordException;

/**
 * @author Christian
 *
 */
public class DRI implements IListener<ReadyEvent>
{

	private static Logger logger;
	public static String dir;
	private static FileLock lock;
	public static final String version = new String("2.0.0a");
	public static final String prefix = new String("fs!");
	public static TrayMenu menu;
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
		logger.info("Directory name found: {}", dir);
		checkIfSingleInstance();
		//Read Config and evaluate if it can continue
		DRI controller = new DRI();
		//Create and setup GUI menu if possible
		TrayMenu menu = new TrayMenu(controller);
		//Connect to Discord, Update database on changes, and establish bot commands
		controller.connectToDiscord();
		//Display Menu
		menu.showMenu();
	}

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
		clientBuilder.registerListener(new NewChannel(db));
		clientBuilder.registerListener(new ChannelDeleted(db));
		try
		{
			api = clientBuilder.login();
			api.getDispatcher().registerListener(this);
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
		actions = new CommandListener(new UserActivity(this, api, db, prop, prefix).getRegistry());
		api.getDispatcher().registerListener(actions);
		//TODO Announce arrival
		menu.setupComplete();
	}

	public void editVideoDirectory(String path, boolean alertDiscord, CommandContext ctx)
	{
		prop.setProperty("Video_Directory", path);
		try
		{
			FileOutputStream prop_file = new FileOutputStream(DRI.dir + File.separator + "config.properties");
			prop.store(prop_file, "Frame-Summoner Properties");
		}
		catch (IOException e)
		{
			logger.fatal("Could Not Save to config.properties", e);
			System.exit(20);
		}
	}

	public void refreshVideos()
	{
		
	}

	public void disconnect(String message)
	{
		//TODO Announce Disconnection, wait for extractions to complete
		api.getDispatcher().unregisterListener(actions);
		api.logout();
		api = null;
		actions = null;
	}

	public void sendMessage()
	{
		String s = (String) JOptionPane.showInputDialog(null, "Send Message or Command from Frame-Summoner Discord Bot", "Frame-Summoner Message",
				JOptionPane.INFORMATION_MESSAGE, new ImageIcon(TrayMenu.image), null, null);
		//adminChannel.sendMessage(s.substring(0, Math.min(s.length(), 2000)));
		//TODO Announce Message
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
				try
				{
					Byte.parseByte(value);
				}
				catch (NullPointerException | NumberFormatException e)
				{
					logger.error("Config not properly configured, \nKey: `MaxUserExtracts` is not a number less then 128");
					readFailed = true;
				}
				value = prop.getProperty("MaxServerExtracts");
				try
				{
					Integer.parseInt(value);
				}
				catch (NullPointerException | NumberFormatException e)
				{
					logger.error("Config not properly configured, \nKey: `MaxServerExtracts` is not a number less then 2 million");
					readFailed = true;
				}
				value = prop.getProperty("FFmpeg_Path");
				if (value != null && !new File(value).exists())
				{
					logger.error("Config not properly configured, \nKey: `FFmpeg_Path` is not a valid file path");
					readFailed = true;
				}
				try
				{
					Process x = Runtime.getRuntime().exec(value);
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
				value = prop.getProperty("FFprobe_Path");
				if (value != null && !new File(value).exists())
				{
					logger.error("Config not properly configured, \nKey: `FFprobe_Path` is not a valid file path");
					readFailed = true;
				}
				try
				{
					Process x = Runtime.getRuntime().exec(value);
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
				value = prop.getProperty("Video_Directory");
				if (value != null && !new File(value).exists() && !new File(value).isDirectory())
				{
					logger.error("Config not properly configured, \nKey: `FFprobe_Path` is not a valid file path");
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
						if (key.equals("DiscordToken"))
						{
							val = "..." + val.substring(val.length() - 6, val.length() - 1);
						}
						response += "\n\t" + key + ": " + val;

					}
					logger.info("Frame-Summoner Properties{}", response);
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

	public DRI()
	{
		readConfig();
		File f = new File(dir + File.separator + "frame-cache");
		if (!f.exists())
		{
			if (f.mkdir())
				db = new Database(dir);
			else
			{
				logger.fatal("Could not create Frame-cache folder");
				System.exit(4);
			}
		}
		else
			db = new Database(dir);
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
