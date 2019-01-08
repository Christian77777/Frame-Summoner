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
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

/**
 * @author Christian
 * Class that interfaces with the database, and the rest of the Classes
 * No other class should import the java.sql libraries except this one
 * Also, all methods should open and close Connection objects as quickly as possible, to enforce concurrency
 * 
 */
public class Database
{
	private static String directory;
	private static Logger logger = LogManager.getLogger();
	private String connectionPath;
	private Connection c;
	private ReentrantLock lock = new ReentrantLock();

	private PreparedStatement psCheckIfGuildExists, psAddNewGuild, psRemoveGuild, psAddNewChannel, psListOfConfiguredChannels, psRemoveChannel, psAddNewAdminRole,
			psListOfAdminRoles, psRemoveAdminRole, psAddNewUserRole, psListOfUserRoles, psRemoveUserRole, psRemoveVideo, psAddOrUpdateVideo,
			psGetVideoByFilename, psCheckIFVIP, psWipeAllVideos, psCountVideos, psCheckChannelTier, psGetRolePerms, psCheckGuildBlacklistMode, psChangeGuildBlacklistMode, psRemoveAllUserRoles;

	public static void main(String[] args) throws SQLException
	{
		File temp;
		try
		{
			temp = new File(Database.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			if (temp.getAbsolutePath().endsWith("jar"))
				directory = temp.getParent();
			else
				directory = temp.getAbsolutePath();
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
		System.setProperty("directory", directory);
		logger = LogManager.getLogger();
		logger.info("Directory name found: {}", directory);
		String path = "jdbc:sqlite:" + directory + File.separator + "Frame-Summoner_Data.db";
		Connection conn = DriverManager.getConnection(path);
		DatabaseMetaData dbMetaData = conn.getMetaData();
		System.out.println("Support RESULT_SET_TYPE: TYPE_FORWARD_ONLY? " + dbMetaData.supportsResultSetType(ResultSet.TYPE_FORWARD_ONLY));
		System.out
				.println("Support RESULT_SET_TYPE: TYPE_SCROLL_INSENSITIVE? " + dbMetaData.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE));
		System.out.println("Support RESULT_SET_TYPE: TYPE_SCROLL_SENSITIVE? " + dbMetaData.supportsResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE));
		System.out.println("Support RESULT_SET_CONCURRENCY: TYPE_FORWARD_ONLY: CONCUR_READ_ONLY? "
				+ dbMetaData.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
		System.out.println("Support RESULT_SET_CONCURRENCY: TYPE_FORWARD_ONLY: CONCUR_UPDATABLE? "
				+ dbMetaData.supportsResultSetConcurrency(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE));
		System.out.println("Support RESULT_SET_CONCURRENCY: TYPE_SCROLL_INSENSITIVE: CONCUR_READ_ONLY? "
				+ dbMetaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
		System.out.println("Support RESULT_SET_CONCURRENCY: TYPE_SCROLL_INSENSITIVE: CONCUR_UPDATABLE? "
				+ dbMetaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE));
		System.out.println("Support RESULT_SET_CONCURRENCY: TYPE_SCROLL_SENSITIVE: CONCUR_READ_ONLY? "
				+ dbMetaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY));
		System.out.println("Support RESULT_SET_CONCURRENCY: TYPE_SCROLL_SENSITIVE: CONCUR_UPDATABLE? "
				+ dbMetaData.supportsResultSetConcurrency(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE));
		System.out.println("ResultSet.HOLD_CURSORS_OVER_COMMIT = " + ResultSet.HOLD_CURSORS_OVER_COMMIT);

		System.out.println("ResultSet.CLOSE_CURSORS_AT_COMMIT = " + ResultSet.CLOSE_CURSORS_AT_COMMIT);

		System.out.println("Default cursor holdability: " + dbMetaData.getResultSetHoldability());

		System.out.println("Supports HOLD_CURSORS_OVER_COMMIT? " + dbMetaData.supportsResultSetHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT));

		System.out.println("Supports CLOSE_CURSORS_AT_COMMIT? " + dbMetaData.supportsResultSetHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT));
		conn.close();
		@SuppressWarnings("unused")
		Database db = new Database(directory);
	}

	/**
	 * Creates Database, and prepares tables if not prepared already
	 */
	public Database(String directory)
	{
		connectionPath = "jdbc:sqlite:" + directory + File.separator + "Frame-Summoner_Data.db";
		SQLiteConfig config = new SQLiteConfig();
		config.enforceForeignKeys(true);
		config.setDateStringFormat("MM/dd HH:mm:ss");
		config.setOpenMode(SQLiteOpenMode.NOMUTEX);
		try
		{
			c = DriverManager.getConnection(connectionPath, config.toProperties());
			logger.info("Opened Local Database Successfully");
			c.setAutoCommit(false);
		}
		catch (Exception e)
		{
			logger.catching(e);
			System.exit(20);
		}
		lock.lock();
		try (Statement stmt = c.createStatement();)
		{
			//Date Field: (MM/dd HH:mm:ss)
			generateTablesAndTriggers(stmt);
			generateStatements();
			c.commit();
		}
		catch (SQLException e)
		{
			logger.catching(e);
			try
			{
				c.rollback();
			}
			catch (SQLException e1)
			{
				logger.catching(e1);
			}
		}
		finally
		{
			try
			{
				c.setAutoCommit(true);
			}
			catch (SQLException e)
			{
				logger.catching(e);
			}
		}
		lock.unlock();
	}
	
	/**
	 * Adds a Guild to the Database
	 * @param id The GuildID
	 * @return if the SQL executed successfully
	 */
	public boolean addNewDefaultGuild(long id)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psAddNewGuild.setLong(1, id);
			psAddNewGuild.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Sees if a ClientID name exists already
	 * @param name The ClientID to verify if exists
	 * @return if the ClientID was already entered
	 */
	public boolean checkIfGuildExists(long id)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psCheckIfGuildExists.setLong(1, id);
			ResultSet rs = psCheckIfGuildExists.executeQuery();
			if (rs.next())
			{
				result = (rs.getInt(1) == 1) ? true : false;
			}
			else
			{
				logger.error("Guild Existence Query Statement somehow failed to return a value");
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}
	
	/**
	 * Change Guild Blacklist Mode
	 * @param id Guild to change mode
	 * @param isBlacklist the Listing mode to change to
	 * @return if the Guild was Found
	 */
	public boolean changeGuildBlacklistMode(long id, boolean isBlacklist)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psRemoveAllUserRoles.setLong(1, id);
			psRemoveAllUserRoles.executeUpdate();
			psChangeGuildBlacklistMode.setBoolean(1, isBlacklist);
			psChangeGuildBlacklistMode.setLong(2, id);
			psChangeGuildBlacklistMode.executeUpdate();
			if(psChangeGuildBlacklistMode.getUpdateCount() > 0)
				result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}
	
	/**
	 * Removes a Guild to the Database
	 * @param id The GuildID
	 * @return if a Guild was deleted
	 */
	public boolean wipeGuild(long id)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psRemoveGuild.setLong(1, id);
			psRemoveGuild.executeUpdate();
			if(psRemoveGuild.getUpdateCount() > 0)
				result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Adds a Channel to the Database
	 * @param id The Channel ID
	 * @param guild The Guild ID, must be already in Guilds Table
	 * @param tier The Permission Tier, [1-4]
	 * @param annoucementChannel Occasionally use this channel for announcements
	 * @return if the SQL executed successfully
	 */
	public boolean addNewChannel(long id, long guild, int tier, boolean annoucementChannel)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psAddNewChannel.setLong(1, id);
			psAddNewChannel.setLong(2, guild);
			psAddNewChannel.setInt(3, tier);
			psAddNewChannel.setBoolean(4, annoucementChannel);
			psAddNewChannel.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Returns static ArrayList of channels stored on the server
	 * @param guildID Restrict Channels to Guild with this ID
	 * @return the ArrayList of channels
	 */
	public ArrayList<Long> getListOfConfiguredChannels(long guildID)
	{
		ArrayList<Long> list = new ArrayList<Long>();
		lock.lock();
		try
		{
			psListOfConfiguredChannels.setLong(1, guildID);
			ResultSet rs = psListOfConfiguredChannels.executeQuery();
			while (rs.next())
			{
				list.add(rs.getLong(1));
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return list;
	}

	/**
	 * Remove Channel in Database
	 * @param id The Channel to Remove
	 * @return if the SQL executed successfully
	 */
	public boolean removeChannel(long id)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psRemoveChannel.setLong(1, id);
			psRemoveChannel.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Recognizes a new Role as Admin
	 * @param id The Role ID
	 * @param guild The Guild ID, must be already in Guilds Table
	 * @return if the Role is actually new
	 */
	public boolean addNewAdminRole(long id, long guild)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psAddNewAdminRole.setLong(1, id);
			psAddNewAdminRole.setLong(2, guild);
			psAddNewAdminRole.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Returns static ArrayList of Roles recognized as Admin stored on the server
	 * @param guildID Restrict Roles to Guild with this ID
	 * @return the ArrayList of RoleIDs
	 */
	public ArrayList<Long> getListOfAdminRoles(long guildID)
	{
		ArrayList<Long> list = new ArrayList<Long>();
		lock.lock();
		try
		{
			psListOfAdminRoles.setLong(1, guildID);
			ResultSet rs = psListOfAdminRoles.executeQuery();
			while (rs.next())
			{
				list.add(rs.getLong(1));
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return list;
	}

	/**
	 * Remove Admin recognition of Role in Database
	 * @param id The Role to forget
	 * @return if the SQL executed successfully
	 */
	public boolean removeAdminRole(long id)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psRemoveAdminRole.setLong(1, id);
			psRemoveAdminRole.executeUpdate();
			if(psRemoveAdminRole.getUpdateCount() > 0)
				result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Recognizes a new User Role as White/Black Listed
	 * @param id The Role ID
	 * @param guild The Guild ID, must be already in Guilds Table
	 * @param blackVSWhite If entry is supposed to be treated as blacklist entry, otherwise whitelist entry
	 * @return if the SQL executed successfully
	 */
	public boolean addNewUserRole(long id, long guild, boolean blackVSWhite)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psAddNewUserRole.setLong(1, id);
			psAddNewUserRole.setLong(2, guild);
			psAddNewUserRole.setBoolean(3, blackVSWhite);
			psAddNewUserRole.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Returns static ArrayList of User Roles recognized as White/Blacklisted, that are stored on the server
	 * @param guildID Restrict Roles to Guild with this ID
	 * @return the ArrayList of RoleIDs
	 */
	public ArrayList<Long> getListOfUserRoles(long guildID)
	{
		ArrayList<Long> list = new ArrayList<Long>();
		lock.lock();
		try
		{
			psListOfUserRoles.setLong(1, guildID);
			ResultSet rs = psListOfUserRoles.executeQuery();
			while (rs.next())
			{
				list.add(rs.getLong(1));
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return list;
	}

	/**
	 * Remove User Role recognition in Database
	 * @param id The Role to forget
	 * @return if the SQL executed successfully
	 */
	public boolean removeUserRole(long id)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psRemoveUserRole.setLong(1, id);
			psRemoveUserRole.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Adds or updates a Database entry into videos
	 * @param filename Primary Key, name of file to add
	 * @param length Length of video time
	 * @param offset Offset to sync with external video source avaliable
	 * @param fps frame rate of video
	 * @param enabled if Video should be accessible to normal audience
	 * @return If the update was successful
	 */
	public boolean addOrUpdateVideo(String filename, long length, long offset, String fps, Boolean enabled)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psAddOrUpdateVideo.setString(1, filename);
			psAddOrUpdateVideo.setLong(2, length);
			//Offset can be null, but JDBC allows direct passing of null
			psAddOrUpdateVideo.setLong(3, offset);
			psAddOrUpdateVideo.setString(4, fps);
			if (enabled != null)
				psAddOrUpdateVideo.setBoolean(5, enabled);
			else
				psAddOrUpdateVideo.setNull(5, Types.BOOLEAN);
			psAddOrUpdateVideo.setString(6, filename);
			psAddOrUpdateVideo.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	public boolean isUserVIP(long id)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psCheckIFVIP.setLong(1, id);
			ResultSet rs = psCheckIFVIP.executeQuery();
			while (rs.next())//Exists at all
			{
				if (rs.getBoolean(1))//Is VIP
					result = true;
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/*
	 * public boolean isVideoAccessible(String filename, long id)
	 * {
	 * boolean result = false;
	 * lock.lock();
	 * try
	 * {
	 * psGetVideoByFilename.setString(1, filename);
	 * ResultSet rs = psGetVideoByFilename.executeQuery();
	 * while (rs.next())//Exists at all
	 * {
	 * if(rs.getBoolean(4))//Exists
	 * result = true;
	 * else if(isUserVIP(id))//Exists but VIP only
	 * result = true;
	 * }
	 * rs.close();
	 * }
	 * catch (SQLException e)
	 * {
	 * logger.catching(e);
	 * }
	 * lock.unlock();
	 * return result;
	 * }
	 */

	/**
	 * Remove Video from Database if deleted, or corrupted (can't probe time length)
	 * @param filename the file name
	 * @return if the video existed
	 */
	public boolean removeVideo(String filename)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psRemoveVideo.setString(1, filename);
			psRemoveVideo.executeUpdate();
			if (psRemoveVideo.getUpdateCount() > 0)
				result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}
	
	/**
	 * Remove all Videos
	 * @return if the SQL executed successfully
	 */
	public boolean removeAllVideos()
	{
		boolean result = false;
		lock.lock();
		try
		{
			psWipeAllVideos.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Returns all Video Data in one SQL Call of a file
	 * @param filename Name of File
	 * @return Object array with the first 3 slots being String, and the last slot being a boolean
	 */
	public Video getVideoData(String filename)
	{
		Video result = null;
		lock.lock();
		try
		{
			psGetVideoByFilename.setString(1, filename);
			ResultSet rs = psGetVideoByFilename.executeQuery();
			if (rs.next())//Exists at all
			{
				result = new Video(rs.getLong(1), rs.getLong(2),rs.getString(3),rs.getBoolean(4));
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}
	
	/**
	 * Count the number of visible videos in the Database
	 * @param filename Name of File
	 * @return Object array with the first 3 slots being String, and the last slot being a boolean
	 */
	public int getVisibleVideoCount()
	{
		int result = -1;
		lock.lock();
		try
		{
			ResultSet rs = psCountVideos.executeQuery();
			if (rs.next())//Exists at all
			{
				result = rs.getInt(1);
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}
	
	/**
	 * Check if command is available in channel
	 * @param channel id of channel to check
	 * @param tier permission tier required
	 * @return If the permission was accepted
	 */
	public boolean checkChannelPermission(long channel, int tier)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psCheckChannelTier.setLong(1, channel);
			ResultSet rs = psCheckChannelTier.executeQuery();
			if (rs.next())
			{
				int perm = rs.getInt(1);
				if(perm >= tier)
					result = true;
			}
			else
			{
				logger.warn("Channel `{}` does not Exist!", channel);
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}
	
	/**
	 * Count the number of visible videos in the Database
	 * @param filename Name of File
	 * @return Object array with the first 3 slots being String, and the last slot being a boolean
	 */
	public ArrayList<RolePerm> getReleventRoles(long id)
	{
		ArrayList<RolePerm> result = new ArrayList<RolePerm>();
		lock.lock();
		try
		{
			psGetRolePerms.setLong(1, id);
			ResultSet rs = psGetRolePerms.executeQuery();
			while (rs.next())//Exists at all
			{
				result.add(new RolePerm(rs.getLong(1), rs.getBoolean(2)));
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}
	
	/**
	 * Check what Listing Mode the Guild has specified
	 * @param guildID id of Guild to check
	 * @return If the guild is running in Blacklist Mode
	 */
	public boolean checkGuildBlacklistMode(long guildID)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psCheckGuildBlacklistMode.setLong(1, guildID);
			ResultSet rs = psCheckGuildBlacklistMode.executeQuery();
			if (rs.next())
			{
				result = rs.getBoolean(1);
			}
			else
			{
				logger.warn("GuildID `{}` does not Exist!", guildID);
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	private void generateTablesAndTriggers(Statement stmt) throws SQLException
	{
		//Create Tables
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `AdminRoles` ( `RoleID` INTEGER NOT NULL UNIQUE, `GuildID` INTEGER NOT NULL, FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE CASCADE, PRIMARY KEY(`RoleID`) )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `ChannelPerms` ( `ChannelID` INTEGER NOT NULL UNIQUE, `GuildID` INTEGER NOT NULL, `Permission` INTEGER NOT NULL DEFAULT 0, `Annoucements` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`ChannelID`), FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE CASCADE )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `ExtractionRecord` ( `Date` TEXT NOT NULL UNIQUE, `UserID` INTEGER NOT NULL, `GuildID` INTEGER NOT NULL, `Filename` TEXT NOT NULL, `Timestamp` TEXT NOT NULL, `Frame Count` INTEGER, `Processed` INTEGER NOT NULL, FOREIGN KEY(`Filename`) REFERENCES `Videos`(`Filename`) ON UPDATE CASCADE ON DELETE NO ACTION, PRIMARY KEY(`Date`), FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE NO ACTION, FOREIGN KEY(`UserID`) REFERENCES `UserRecord`(`UserID`) ON UPDATE CASCADE ON DELETE NO ACTION )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `Guilds` ( `GuildID` INTEGER NOT NULL UNIQUE, `RequestLimit` INTEGER NOT NULL DEFAULT 1000, `UsedToday` INTEGER NOT NULL DEFAULT 0, `Enabled` INTEGER NOT NULL DEFAULT 1, `isBlacklist` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`GuildID`) )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `Links` ( `Title` TEXT NOT NULL UNIQUE, `Link` TEXT NOT NULL, `Usable` INTEGER NOT NULL, `Filename` TEXT, PRIMARY KEY(`Title`), FOREIGN KEY(`Filename`) REFERENCES `Videos`(`Filename`) ON UPDATE CASCADE ON DELETE SET NULL )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `Moments` ( `GuildID` INTEGER NOT NULL, `Name` TEXT NOT NULL, `Filename` TEXT NOT NULL, `Timestamp` TEXT NOT NULL, `TimesUsed` INTEGER NOT NULL DEFAULT 1, `Disabled` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(`Filename`) REFERENCES `Videos`(`Filename`) ON UPDATE CASCADE ON DELETE RESTRICT )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `RolePerms` ( `RoleID` INTEGER NOT NULL, `GuildID` INTEGER NOT NULL, `BlackVSWhite` INTEGER NOT NULL, FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE CASCADE )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `ServerUserBans` ( `UserID` INTEGER NOT NULL, `GuildID` INTEGER NOT NULL, PRIMARY KEY(`UserID`,`GuildID`), FOREIGN KEY(`UserID`) REFERENCES `UserRecord`(`UserID`) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE NO ACTION )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `UserRecord` ( `UserID` INTEGER NOT NULL UNIQUE, `TimesUsed` INTEGER NOT NULL, `UsedToday` INTEGER NOT NULL, `GlobalBan` INTEGER NOT NULL DEFAULT 0, `VIP` INTEGER NOT NULL, PRIMARY KEY(`UserID`) )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `Videos` ( `Filename` TEXT NOT NULL UNIQUE, `Length` INTEGER NOT NULL, `Offset` INTEGER NOT NULL, `Fps` STRING NOT NULL, `Usable` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`Filename`) )");
		//CreateTriggers
		stmt.executeUpdate(
				"CREATE TRIGGER IF NOT EXISTS AutoAddExtractionRecordUser BEFORE INSERT ON ExtractionRecord WHEN NOT EXISTS(SELECT 1 FROM UserRecord WHERE UserID = NEW.UserID LIMIT 1) BEGIN INSERT INTO UserRecord VALUES(NEW.UserID,1,1,0,0); END");
		stmt.executeUpdate(
				"CREATE TRIGGER IF NOT EXISTS AutoAddServerBanUser BEFORE INSERT ON ServerUserBans WHEN NOT EXISTS(SELECT 1 FROM UserRecord WHERE UserID = NEW.UserID LIMIT 1) BEGIN INSERT INTO UserRecord VALUES(NEW.UserID,0,0,0,0); END");
	}

	private void generateStatements() throws SQLException
	{
		psCheckIfGuildExists = c.prepareStatement("SELECT EXISTS(SELECT 1 FROM `Guilds` WHERE `GuildID` = ? LIMIT 1);");
		psAddNewGuild = c.prepareStatement("INSERT INTO `Guilds` VALUES(?,1000,0,1,1);");
		psRemoveGuild = c.prepareStatement("DELETE FROM `Guilds` WHERE `GuildID` = ?;");
		psAddNewChannel = c.prepareStatement("INSERT INTO `ChannelPerms` VALUES(?,?,?,?);");
		psListOfConfiguredChannels = c.prepareStatement("SELECT `ChannelID` FROM `ChannelPerms` WHERE `GuildID` = ?;");
		psRemoveChannel = c.prepareStatement("DELETE FROM `ChannelPerms` WHERE `ChannelID` = ?;");
		psAddNewAdminRole = c.prepareStatement("INSERT INTO `AdminRoles` VALUES(?,?);");
		psListOfAdminRoles = c.prepareStatement("SELECT `RoleID` FROM `AdminRoles` WHERE `GuildID` = ?;");
		psRemoveAdminRole = c.prepareStatement("DELETE FROM `AdminRoles` WHERE `RoleID` = ?;");
		psAddNewUserRole = c.prepareStatement("INSERT INTO `RolePerms` VALUES(?,?,?);");
		psListOfUserRoles = c.prepareStatement("SELECT `RoleID` FROM `RolePerms` WHERE `GuildID` = ?;");
		psRemoveUserRole = c.prepareStatement("DELETE FROM `RolePerms` WHERE `RoleID` = ?;");
		psAddOrUpdateVideo = c.prepareStatement(
				"INSERT OR REPLACE INTO `Videos` (`Filename`,`Length`,`Offset`,`Fps`,`Usable`) VALUES(?,?,?,?,COALESCE(?,(SELECT `Usable` FROM `Videos` WHERE `Filename` = ?),1));");
		psGetVideoByFilename = c.prepareStatement("SELECT `Length`, `Offset`, `Fps`, `Usable` FROM `Videos` WHERE `Filename` = ? LIMIT 1");
		psRemoveVideo = c.prepareStatement("DELETE FROM `Videos` WHERE `Filename` = ?;");
		psCheckIFVIP = c.prepareStatement("SELECT EXISTS(SELECT 1 FROM `UserRecord` WHERE `UserID` = ? AND `VIP` = 1 LIMIT 1);");
		psWipeAllVideos = c.prepareStatement("DELETE FROM `Videos`;");
		psCountVideos = c.prepareStatement("SELECT COUNT(rowid) FROM `Videos` WHERE `Usable` = 1;");
		psCheckChannelTier = c.prepareStatement("SELECT `Permission` FROM `ChannelPerms` WHERE `ChannelID` = ? LIMIT 1;");
		psGetRolePerms = c.prepareStatement("SELECT `RoleID`, `BlackVSWhite` FROM `RolePerms` ORDER BY `RoleID` ASC WHERE `GuildID`= ?;");
		psCheckGuildBlacklistMode = c.prepareStatement("SELECT `isBlacklist` FROM `Guilds` WHERE `GuildID` = ?;");
		psChangeGuildBlacklistMode = c.prepareStatement("UPDATE `Guilds` SET `isBlacklist` = ? WHERE `GuildID` = ?;");
		psRemoveAllUserRoles = c.prepareStatement("DELETE FROM `RolePerms` WHERE `GuildID` = ?;");
	}
	
	public class RolePerm
	{
		private long roleID;
		private boolean blackVSwhite;
		
		private RolePerm(long roleID, boolean blackVSwhite)
		{
			this.roleID = roleID;
			this.blackVSwhite = blackVSwhite;
		}
		
		public long getRoleID()
		{
			return roleID;
		}
		
		public boolean getBlackVSWhite()
		{
			return blackVSwhite;
		}
	}
	
	public class Video
	{
		private long length;
		private long offset;
		private String fps;
		private boolean usable;
		
		private Video(long l, long o, String s, boolean u)
		{
			length = l;
			offset = o;
			fps = s;
			usable = u;
		}

		public long getLength()
		{
			return length;
		}

		public long getOffset()
		{
			return offset;
		}

		public String getFps()
		{
			return fps;
		}

		public boolean isUsable()
		{
			return usable;
		}
	}
}
