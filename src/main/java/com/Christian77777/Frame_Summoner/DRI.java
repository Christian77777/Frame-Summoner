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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.darichey.discord.CommandContext;
import com.darichey.discord.CommandListener;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.ActivityType;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.StatusType;
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
	public static String version = new String("1.2.3");
	public static TrayMenu menu;
	private String token = new String("fake");
	private String serverName = new String("JoeBlow's Server");
	private String adminChannelName = new String("bot-spam");
	private String adminRoleName = new String("ADMIN");
	private String userRoleName = new String("user");
	private String ffmpegDir = null;
	private String ffprobeDir = null;
	private String videoDir = null;
	private boolean changeDir = false;
	private IDiscordClient api;
	private CommandListener actions;
	private IChannel adminChannel = null;
	private IRole adminRole = null;
	private IRole userRole = null;

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
		TrayMenu menu;
		menu = new TrayMenu();
		menu.showMenu();
	}

	@Override
	public void handle(ReadyEvent event)
	{
		Thread.currentThread().setName("DiscordConnectionThread");
		this.api = event.getClient();
		logger.info("Connected to Discord");
		for (IChannel ch : api.getChannels())
		{
			if (ch.getName().equals(adminChannelName))
			{
				adminChannel = ch;
				logger.info("Found Admin Channel: {}", adminChannel.getLongID());
				break;
			}
		}
		for (IRole r : api.getRoles())
		{
			if (r.getName().equals(adminRoleName))
			{
				adminRole = r;
				logger.info("Found Admin Role: {}", adminRole.getLongID());
			}
		}
		for (IRole r : api.getRoles())
		{
			if (r.getName().equals(userRoleName))
			{
				userRole = r;
				logger.info("Found User Role: {}", userRole.getLongID());
			}
		}
		try
		{
			Process x = Runtime.getRuntime().exec(ffmpegDir);
			StreamGobbler reader = new StreamGobbler(x.getInputStream(), false);
			StreamGobbler eater = new StreamGobbler(x.getErrorStream(), false);
			reader.start();
			eater.start();
			if (x.waitFor(100, TimeUnit.MILLISECONDS))

				logger.info("{} recognized", ffmpegDir);

			else
				logger.info("{} accepted", ffmpegDir);

		}
		catch (InterruptedException | IOException e)
		{
			logger.error("{} rejected", ffmpegDir);
			logger.fatal("File Path for FFMPEG Rejected, please put the correct path in the values.txt file and restart.");
			System.exit(1);
		}
		try
		{
			Process x = Runtime.getRuntime().exec(ffprobeDir);
			StreamGobbler reader = new StreamGobbler(x.getInputStream(), false);
			StreamGobbler eater = new StreamGobbler(x.getErrorStream(), false);
			reader.start();
			eater.start();
			if (x.waitFor(100, TimeUnit.MILLISECONDS))
				logger.info("{} recognized", ffprobeDir);
			else
				logger.info("{} accepted", ffprobeDir);

		}
		catch (InterruptedException | IOException e)
		{
			logger.error("{} rejected", ffmpegDir);
			logger.fatal("File Path for FFprobe Rejected, please put the correct path in the values.txt file and restart.");
			System.exit(1);
		}
		if (!new File(videoDir).exists() || !new File(videoDir).isDirectory())
		{
			logger.error("Video Directory invalid: {}", videoDir);
			//New Thread spawned, needs own reference
			final String vDir = videoDir;
			String message;
			if(changeDir)
				message = "Video Directory Invalid!\n`" + vDir + "`\nUse s!dir [File Path (without quotes)] to change path and reboot";
			else
				message = "Video Directory Invalid!\n`" + vDir + "`\nEdit values.txt and reboot";
			RequestBuffer.request(() -> {
				adminChannel.sendMessage(message);
			});
			videoDir = null;
		}
		if (adminChannel != null && adminRole != null && userRole != null)
		{
			//api.changePresence(status, activity, text);
			actions = new CommandListener(
					new UserActivity(this, adminChannel, adminRole, userRole, serverName, ffmpegDir, ffprobeDir, videoDir, changeDir).getRegistry());
			api.getDispatcher().registerListener(actions);
			updateStatus();
			RequestBuffer.request(() -> {
				adminChannel.sendMessage("Frame Extractor Started");
			});
			menu.setupComplete();
		}
		else
		{
			if (adminChannel == null)
				logger.fatal("Admin Channel Not Found!");
			if (adminRole == null)
				logger.fatal("Admin Role Not Found!");
			if (userRole == null)
				logger.fatal("User Role Not Found!");
			//videoDir already handled
		}
	}

	public void editVideoDirectory(String path, boolean alertDiscord, CommandContext ctx)
	{
		String[] storage = new String[9];
		try (FileReader file = new FileReader(DRI.dir + File.separator + "values.txt"); BufferedReader in = new BufferedReader(file);)
		{
			for (int x = 0; x < 9; x++)
			{
				storage[x] = in.readLine();
			}
			storage[7] = storage[7].substring(0, 16) + path;
		}
		catch (StringIndexOutOfBoundsException | NullPointerException | IOException e)
		{
			logger.fatal("Could not read values.txt\n Please edit before rerunning Program\nOr Delete to generate a clean values.txt");
			if (alertDiscord)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Failed to read prexisting values.txt, shutting down...");
				});
			}
			System.exit(20);
		}
		try (PrintWriter printer = new PrintWriter(DRI.dir + File.separator + "values.txt");)
		{
			for (int x = 0; x < 9; x++)
			{
				printer.println(storage[x]);
			}
			printer.flush();
			printer.close();
		}
		catch (IOException e)
		{
			logger.fatal("Failed to Generate values.txt: ", e);
			if (alertDiscord)
			{
				RequestBuffer.request(() -> {
					ctx.getChannel().sendMessage("Major Error rewriting values.txt, shutting down...");
				});
			}
			System.exit(20);
		}
	}

	public void disconnect(String message)
	{
		RequestBuffer.request(() -> {
			adminChannel.sendMessage(message);
		});
		api.getDispatcher().unregisterListener(actions);
		api.logout();
		api = null;
		actions = null;
	}

	public void sendCommand()
	{
		String s = (String) JOptionPane.showInputDialog(null, "Send Message or Command from Frame-Summoner Discord Bot", "Frame-Summoner Message",
				JOptionPane.INFORMATION_MESSAGE, new ImageIcon(TrayMenu.image), null, null);
		adminChannel.sendMessage(s.substring(0, Math.min(s.length(), 2000)));
	}

	public void updateStatus()
	{
		try
		{
			int fileCount = new File(videoDir).listFiles().length;
			if (fileCount > 0)
			{
				api.changePresence(StatusType.ONLINE, ActivityType.WATCHING, fileCount + " videos");
			}
			else
			{
				api.changePresence(StatusType.IDLE, ActivityType.WATCHING, "nothing");
			}
		}
		catch (NullPointerException f)
		{
			//logger.error("File Path Provided in values.txt invalid", f);
			//Already reported
			api.changePresence(StatusType.DND);
		}
	}

	private void readConfig()
	{
		try (FileReader file = new FileReader(dir + File.separator + "values.txt"); BufferedReader in = new BufferedReader(file);)
		{
			token = in.readLine().substring(6);
			serverName = in.readLine();
			serverName = serverName.substring(5, Math.min(40, serverName.length()));
			adminChannelName = in.readLine().substring(17);
			adminRoleName = in.readLine().substring(14);
			userRoleName = in.readLine().substring(13);
			ffmpegDir = in.readLine().substring(21);
			ffprobeDir = in.readLine().substring(22);
			videoDir = in.readLine().substring(16);
			changeDir = in.readLine().substring(35).equals("Y");
			logger.info(
					"\n\tToken (masked Ending): {}\n\tServer Name: {}\n\tAdmin Channel Name: {}\n\tAdmin Role Name: {}\n\tUser Role Name: {}\n\tFFMPEG Path: {}\n\tFFprobe Path: {}\n\tVideo Path: {}\n\tAllow Changing the Video Path: {}",
					token.substring(token.length() - 6, token.length() - 1), serverName, adminChannelName, adminRoleName, userRoleName, ffmpegDir,
					ffprobeDir, videoDir, changeDir);
		}
		catch (StringIndexOutOfBoundsException | NullPointerException e)
		{
			logger.fatal("Could not read values.txt\n Please edit before rerunning Program\nOr Delete to generate a clean values.txt");
			System.exit(20);
		}
		catch (FileNotFoundException o)
		{
			try
			{
				PrintWriter printer = new PrintWriter(dir + File.separator + "values.txt");
				printer.println("Token=reallyLongWord");
				printer.println("Name=JoeBlow's Server");
				printer.println("AdminChannelName=bot-spam");
				printer.println("AdminRoleName=ADMIN");
				printer.println("UserRoleName=user");
				printer.println("ffmpeg.exe File Path=*OS path to ffmpeg.exe*");
				printer.println("ffprobe.exe File Path=*OS path to ffprobe.exe*");
				printer.println("Video Directory=Choose the directory of the videos to access here");
				printer.println("AllowDirectoryChangeThroughDiscord=N");
				printer.flush();
				printer.close();
				logger.warn(
						"Created values.txt\nPlease Edit before Using Program\n Remember to not use Quotation Marks when specifying file paths with spaces");
			}
			catch (IOException e)
			{
				logger.fatal("Failed to Generate values.txt: ", e);
			}
			System.exit(20);
		}
		catch (IOException o)
		{
			logger.fatal("IO Exception While Reading Parameters", o);
			System.exit(20);
		}
		File videos = new File(videoDir);
		if (!videos.exists())
		{
			videos.mkdirs();
		}
	}

	public DRI()
	{
		readConfig();
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
		ClientBuilder clientBuilder = new ClientBuilder(); // Creates the ClientBuilder instance
		clientBuilder.withToken(token); // Adds the login info to the builder
		try
		{
			api = clientBuilder.login(); // Creates and logs in the client instance
			api.getDispatcher().registerListener(this);
		}
		catch (DiscordException e)
		{ // This is thrown if there was a problem building the client
			logger.fatal("Failed To Connect: Probably a bad Discord Token");
			logger.catching(e);
			System.exit(21);
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
