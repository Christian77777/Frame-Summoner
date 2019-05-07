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
import org.sqlite.util.OSInfo;

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

	private PreparedStatement psCheckIfGuildExists, psAddNewGuild, psRemoveGuild, psListOfConfiguredChannels, psRemoveChannel, psAddNewVIP,
			psRemoveVIP, psAddNewAdminRole, psListOfAdminRoles, psRemoveAdminRole, psAddNewUserRole, psListOfUserRoles, psRemoveUserRole,
			psRemoveVideo, psAddOrUpdateVideo, psGetVideoByFilename, psCheckIFVIP, psWipeAllVideos, psCountVisibleVideos, psCheckChannelTier,
			psGetRolePerms, psCheckGuildBlacklistMode, psChangeGuildBlacklistMode, psRemoveAllUserRoles, psUpdateChannelTier,
			psSetChannelAnnoucementsTrue, psGetVideoList, psSetVideoUsability, psSetVideoRestricted, psSetAllVideosRestricted, psGetUserData,
			psRecordExtraction, psUpdateUserDailyUsage, psUpdateAllUserDailyUsage, psGetServerUsage, psUpdateServerDailyUsage,
			psUpdateAllServerDailyUsage, psUpdateServerDailyLimit, psUpdateServerStanding, psSetChannelAnnoucementsFalse, psGetLinkByName,
			psGetLinkByTitle, psCreateLink, psDeleteLink1, psDeleteLink2, psDeleteLink3, psUpdateOffset, psGetAllChannels;

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
			logger.info("Opened Local Database");
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
			ResultSet rs = stmt.executeQuery("SELECT sqlite_version();");
			rs.next();
			logger.info("Current Architecture: {}", OSInfo.getNativeLibFolderPathForCurrentOS());
			logger.info("Library Version: {}",rs.getString(1));
			rs.close();
			generateTablesAndTriggers(stmt);
			generateStatements();
			c.commit();
			logger.info("Database Verified");
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

	public void closeDatabase()
	{
		lock.lock();
		try
		{
			psCheckIfGuildExists.close();
			psAddNewGuild.close();
			psRemoveGuild.close();
			psListOfConfiguredChannels.close();
			psRemoveChannel.close();
			psAddNewVIP.close();
			psRemoveVIP.close();
			psAddNewAdminRole.close();
			psListOfAdminRoles.close();
			psRemoveAdminRole.close();
			psAddNewUserRole.close();
			psListOfUserRoles.close();
			psRemoveUserRole.close();
			psRemoveVideo.close();
			psAddOrUpdateVideo.close();
			psGetVideoByFilename.close();
			psCheckIFVIP.close();
			psWipeAllVideos.close();
			psCountVisibleVideos.close();
			psCheckChannelTier.close();
			psGetRolePerms.close();
			psCheckGuildBlacklistMode.close();
			psChangeGuildBlacklistMode.close();
			psRemoveAllUserRoles.close();
			psUpdateChannelTier.close();
			psSetChannelAnnoucementsTrue.close();
			psGetVideoList.close();
			psSetVideoUsability.close();
			psSetVideoRestricted.close();
			psSetAllVideosRestricted.close();
			psGetUserData.close();
			psRecordExtraction.close();
			psUpdateUserDailyUsage.close();
			psUpdateAllUserDailyUsage.close();
			psGetServerUsage.close();
			psUpdateServerDailyUsage.close();
			psUpdateAllServerDailyUsage.close();
			psUpdateServerDailyLimit.close();
			psUpdateServerStanding.close();
			psSetChannelAnnoucementsFalse.close();
			psGetLinkByName.close();
			psGetLinkByTitle.close();
			psCreateLink.close();
			psDeleteLink1.close();
			psDeleteLink2.close();
			psDeleteLink3.close();
			psUpdateOffset.close();
			psGetAllChannels.close();
			c.close();
			logger.info("Database closed");
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
	}

	/**
	 * Runs any command given by @author on SQLite Database
	 * Nothing is verified, or sanitized
	 * @param s The Command to run
	 * @return if the SQL Executed successfully or not.
	 */
	public boolean executeDebugUpdate(String s)
	{
		boolean result = false;
		lock.lock();
		try
		{
			Statement stmt = c.createStatement();
			stmt.executeUpdate(s);
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
	 * @param filename The ClientID to verify if exists
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
			if (psChangeGuildBlacklistMode.getUpdateCount() > 0)
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
			if (psRemoveGuild.getUpdateCount() > 0)
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
	 * Changes Channel Permission tier in Database, only if Channel is in appropriate Guild
	 * @param channelID ID of Channel to modify
	 * @param guildID Guild
	 * @param tier
	 * @return if a Channel was updated
	 */
	public boolean updateChannelTier(long channelID, long guildID, int tier)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psUpdateChannelTier.setLong(1, channelID);
			psUpdateChannelTier.setLong(2, guildID);
			psUpdateChannelTier.setInt(3, tier);
			psUpdateChannelTier.setInt(4, tier);
			psUpdateChannelTier.setLong(5, channelID);
			psUpdateChannelTier.setLong(6, guildID);
			psUpdateChannelTier.executeUpdate();
			if (psUpdateChannelTier.getUpdateCount() > 0)
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
	 * Changes Channel Announcement Mode in Database, enforces channel guild relationship.
	 * Channels that have no annoucement
	 * @param channelID ID of Channel to modify
	 * @param guildID Guild ID that the channel is in
	 * @param shouldAnnouce if the channel should now be announced in.
	 * @return if a Channel was updated
	 */
	public boolean updateChannelAnnoucement(long channelID, long guildID, boolean shouldAnnouce)
	{
		boolean result = false;
		lock.lock();
		try
		{
			if (shouldAnnouce)
			{
				psSetChannelAnnoucementsTrue.setLong(1, channelID);
				psSetChannelAnnoucementsTrue.setLong(2, guildID);
				psSetChannelAnnoucementsTrue.setLong(3, channelID);
				psSetChannelAnnoucementsTrue.executeUpdate();
				if (psSetChannelAnnoucementsTrue.getUpdateCount() > 0)
					result = true;
			}
			else
			{
				//Check if Tier > 0
				psSetChannelAnnoucementsFalse.setLong(1, channelID);
				psSetChannelAnnoucementsFalse.executeUpdate();
				if (psSetChannelAnnoucementsFalse.getUpdateCount() > 0)
					result = true;
			}
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Remove Channel in Database, used on boot to sync Database with Discord
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
	 * Recognizes a new User as VIP
	 * NOTE: Resets Global Bans
	 * @param userID The User's ID
	 * @return if the SQL executed successfully
	 */
	public boolean addNewVIP(long userID)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psAddNewVIP.setLong(1, userID);
			psAddNewVIP.executeUpdate();
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
	 * Checks if a User is VIP
	 * @param id The ID of the User to search for
	 * @return if VIP or not
	 */
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

	/**
	 * Removes a User as VIP
	 * @param userID The User's ID
	 * @return if a user was found
	 */
	public boolean removeVIP(long userID)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psRemoveVIP.setLong(1, userID);
			psRemoveVIP.executeUpdate();
			if (psRemoveVIP.getUpdateCount() > 0)
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
			if (psRemoveAdminRole.getUpdateCount() > 0)
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
	 * Adds or updates a Database entry into videos as usable
	 * @param filename Primary Key, name of file to add
	 * @param nickname
	 * @param length Length of video time
	 * @param offset Offset to sync with external video source available, should not be null but zero
	 * @param fps frame rate of video
	 * @param restricted if Video should be restricted to VIP users
	 * @return If the video was updated/added. Will fail if a shared nickname conflicts with two different file extensions.
	 */
	public boolean addOrUpdateVideo(String filename, String nickname, long length, long offset, String fps, Boolean restricted)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psAddOrUpdateVideo.setString(1, filename);
			psAddOrUpdateVideo.setString(2, nickname);
			psAddOrUpdateVideo.setLong(3, length);
			psAddOrUpdateVideo.setLong(4, offset);
			psAddOrUpdateVideo.setString(5, fps);

			if (restricted != null)
			{
				psAddOrUpdateVideo.setBoolean(6, restricted);
				psAddOrUpdateVideo.setBoolean(7, restricted);
			}
			else
			{
				psAddOrUpdateVideo.setNull(6, Types.BOOLEAN);
				psAddOrUpdateVideo.setNull(7, Types.BOOLEAN);
			}
			psAddOrUpdateVideo.executeUpdate();
			if (psAddOrUpdateVideo.getUpdateCount() > 0)
				result = true;
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
	public DBVideo getVideoData(String nickname)
	{
		DBVideo result = null;
		lock.lock();
		try
		{
			psGetVideoByFilename.setString(1, nickname);
			ResultSet rs = psGetVideoByFilename.executeQuery();
			if (rs.next())//Exists at all
			{
				result = new DBVideo(rs.getString(1), nickname, rs.getLong(2), rs.getLong(3), rs.getString(4), rs.getBoolean(5), rs.getBoolean(6),
						rs.getBoolean(7));
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
	 * Count the number of usable and unrestricted videos in the Database
	 * @param isVIP if Restricted videos should be counted
	 * @return The number of videos found, -1 meaning SQL Exception
	 */
	public int getVisibleVideoCount(boolean isVIP)
	{
		int result = -1;
		lock.lock();
		try
		{
			psCountVisibleVideos.setBoolean(1, isVIP);
			ResultSet rs = psCountVisibleVideos.executeQuery();
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
				if (perm >= tier)
					result = true;
			}
			else
			{
				if (0 >= tier)
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

	/**
	 * Count the number of visible videos in the Database
	 * @param filename Name of File
	 * @return Object array with the first 3 slots being String, and the last slot being a boolean
	 */
	public ArrayList<DBRolePerm> getReleventRoles(long id)
	{
		ArrayList<DBRolePerm> result = new ArrayList<DBRolePerm>();
		lock.lock();
		try
		{
			psGetRolePerms.setLong(1, id);
			ResultSet rs = psGetRolePerms.executeQuery();
			while (rs.next())//Exists at all
			{
				result.add(new DBRolePerm(rs.getLong(1), rs.getBoolean(2)));
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

	/**
	 * Gets all the videos in the Database, regardless of status
	 * @return DBVideo Object array sorted alphabetically
	 */
	public ArrayList<DBVideo> getVideoList()
	{
		ArrayList<DBVideo> result = new ArrayList<DBVideo>();
		lock.lock();
		try
		{
			ResultSet rs = psGetVideoList.executeQuery();
			while (rs.next())//Exists at all
			{
				result.add(new DBVideo(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getString(5), rs.getBoolean(6),
						rs.getBoolean(7), rs.getBoolean(8)));
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
	 * Changes a Videos Usability status, if deleted, or corrupted (can't probe time length), or reverified
	 * @param nickname the file name
	 * @param usable current usable state
	 * @return if a video was changed
	 */
	public boolean setVideoUnusable(String nickname, boolean usable)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psSetVideoUsability.setBoolean(1, usable);
			psSetVideoUsability.setString(2, nickname);
			psSetVideoUsability.executeUpdate();
			if (psSetVideoUsability.getUpdateCount() > 0)
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
	 * Changes every video to be either Restricted or Unrestricted
	 * @param restricted if the final state should be restricted instead of unrestricted
	 * @return the number of videos changed.
	 */
	public int setAllVideosRestricted(boolean restricted)
	{
		int result = 0;
		lock.lock();
		try
		{
			psSetAllVideosRestricted.setBoolean(1, restricted);
			psSetAllVideosRestricted.executeUpdate();
			result = psSetAllVideosRestricted.getUpdateCount();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Changes a group of Video's Restricted status
	 * @param filenames the list of files to modify
	 * @param restricted if the final state should be restricted instead of unrestricted
	 * @return if a video was changed
	 */
	public ArrayList<String> setVideoRestricted(String[] filenames, boolean restricted)
	{
		ArrayList<String> failures = new ArrayList<String>();
		lock.lock();
		try
		{
			c.setAutoCommit(false);
			for (String f : filenames)
			{
				psSetVideoRestricted.setBoolean(1, restricted);
				psSetVideoRestricted.setString(2, f);
				psSetVideoRestricted.addBatch();
			}
			int[] results = psSetVideoRestricted.executeBatch();
			for (int x = 0; x < results.length; x++)
			{
				if (results[x] == 0)
					failures.add(filenames[x]);
			}
			c.setAutoCommit(true);
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return failures;
	}

	/**
	 * Gets a Users Data in the form of a NormalUser object
	 * @param userID the UserID of the User
	 * @return null if the user does not exist, or a NormalUser object
	 */
	public DBNormalUser getUserUsage(long userID)
	{
		DBNormalUser user = null;
		lock.lock();
		try
		{
			psGetUserData.setLong(1, userID);
			ResultSet rs = psGetUserData.executeQuery();
			if (rs.next())
			{
				user = new DBNormalUser(userID, rs.getInt(1), rs.getInt(2), rs.getBoolean(3), rs.getBoolean(4));
			}
			else
			{
				logger.warn("UserID `{}` does not Exist!", userID);
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return user;
	}

	/**
	 * Get the Number of Extractions a Server has done
	 * @param guildID the UserID of the User
	 * @return null if the user does not exist, or a NormalUser object
	 */
	public DBGuild getServerData(long guildID)
	{
		DBGuild guild = null;
		lock.lock();
		try
		{
			psGetServerUsage.setLong(1, guildID);
			ResultSet rs = psGetServerUsage.executeQuery();
			if (rs.next())
			{
				guild = new DBGuild(rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getBoolean(4), rs.getBoolean(5));
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
		return guild;
	}

	/**
	 * Enters a Frame Extraction into the Database with all these strings. Increments the
	 * @param userID The User Snowflake ID
	 * @param guildID The Guild Snowflake ID of the server the message was sent in
	 * @param filename The File that the Frame was extracted from
	 * @param timecode The timestamp the frame came from, assuming no further framecount
	 * @param frameCount The additional
	 * @param url
	 * @return
	 */
	public boolean declareExtraction(long timestamp, boolean elevated, long userID, long guildID, long channelID, long messageID, String filename,
			String timecode, Integer frameCount, String url)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psRecordExtraction.setLong(1, timestamp);
			psRecordExtraction.setBoolean(2, elevated);
			psRecordExtraction.setLong(3, userID);
			psRecordExtraction.setLong(4, guildID);
			psRecordExtraction.setLong(5, channelID);
			psRecordExtraction.setLong(6, messageID);
			psRecordExtraction.setString(7, filename);
			psRecordExtraction.setString(8, timecode);
			if (frameCount == null)
				psRecordExtraction.setNull(9, Types.INTEGER);
			else
				psRecordExtraction.setInt(9, frameCount);
			psRecordExtraction.setString(10, url);
			psRecordExtraction.executeUpdate();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Modifies a User's Daily Usage
	 * @param newUsage the new value, to set the day's Usage to
	 * @param userID if supplied, restricts the change to this userID only
	 * @return if the SQL executed properly or if the userID was supplied, if a user was modified.
	 */
	public boolean updateUserDailyUsage(int newUsage, Long userID)
	{
		boolean result = false;
		lock.lock();
		try
		{
			if (userID == null)
			{
				psUpdateAllUserDailyUsage.setInt(1, newUsage);
				psUpdateAllUserDailyUsage.executeUpdate();
				result = true;
			}
			else
			{
				psUpdateUserDailyUsage.setInt(1, newUsage);
				psUpdateUserDailyUsage.setLong(2, userID);
				psUpdateUserDailyUsage.executeUpdate();
				if (psUpdateUserDailyUsage.getUpdateCount() > 0)
					result = true;
			}
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Modifies a Server's Daily Usage
	 * @param newUsage the new value, to set the day's Usage to
	 * @param guildID if supplied, restricts the change to this guildID only
	 * @return if the SQL executed properly or if the guildID was supplied, if a guild was modified.
	 */
	public boolean updateServerDailyUsage(int newUsage, Long guildID)
	{
		boolean result = false;
		lock.lock();
		try
		{
			if (guildID == null)
			{
				psUpdateAllServerDailyUsage.setInt(1, newUsage);
				psUpdateAllServerDailyUsage.executeUpdate();
				result = true;
			}
			else
			{
				psUpdateServerDailyUsage.setInt(1, newUsage);
				psUpdateServerDailyUsage.setLong(2, guildID);
				psUpdateServerDailyUsage.executeUpdate();
				if (psUpdateServerDailyUsage.getUpdateCount() > 0)
					result = true;
			}
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Modifies a Server's Daily Limit, does not check if below Hard Limit
	 * @param newUsage the new value, to set the day's Limit to
	 * @param guildID if supplied, restricts the change to this guildID only
	 * @return if a guild was modified.
	 */
	public boolean updateServerDailyLimit(int newUsage, long guildID)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psUpdateServerDailyLimit.setInt(1, newUsage);
			psUpdateServerDailyLimit.setLong(2, guildID);
			psUpdateServerDailyLimit.executeUpdate();
			if (psUpdateServerDailyLimit.getUpdateCount() > 0)
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
	 * Changes if a Server can extract frames at all.
	 * @param toEnable If the Server should now be taken Offline
	 * @param guildID if supplied, restricts the change to this guildID only
	 * @return if a guild was modified.
	 */
	public boolean updateServerStanding(boolean toEnable, long guildID)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psUpdateServerStanding.setBoolean(1, toEnable);
			psUpdateServerStanding.setLong(2, guildID);
			psUpdateServerStanding.executeUpdate();
			if (psUpdateServerStanding.getUpdateCount() > 0)
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
	 * Get the Channels in their permissions
	 * @param guildID the UserID of the User
	 * @return null if the user does not exist, or a NormalUser object
	 */
	public ArrayList<DBChannel> getServerChannels(Long guildID)
	{
		ArrayList<DBChannel> channels = new ArrayList<DBChannel>();
		lock.lock();
		try
		{
			ResultSet rs;
			if (guildID == null)
			{
				rs = psGetAllChannels.executeQuery();
			}
			else
			{
				psListOfConfiguredChannels.setLong(1, guildID);
				rs = psListOfConfiguredChannels.executeQuery();
			}
			while (rs.next())
			{
				channels.add(new DBChannel(rs.getLong(1), rs.getInt(2), rs.getBoolean(3)));
			}
			rs.close();
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return channels;
	}

	/**
	 * Store a Video link to retrieve in the Database
	 * @param link YouTube Links only
	 * @param nickname Video file nickname, attached to Videos Table
	 * @param episodename Secondary Nickname that could be anything
	 * @return if Link was newly added or replaced
	 */
	public boolean createLink(String link, String nickname, String episodename, String description)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psCreateLink.setString(1, episodename);
			psCreateLink.setString(6, episodename);
			psCreateLink.setString(3, link);
			psCreateLink.setString(5, link);
			if (nickname == null)
			{
				psCreateLink.setNull(4, Types.VARCHAR);
			}
			else
			{
				psCreateLink.setString(4, nickname);
			}
			if (description == null)
			{
				psCreateLink.setNull(2, Types.VARCHAR);
				psCreateLink.setNull(7, Types.VARCHAR);
			}
			else
			{
				psCreateLink.setString(2, description);
				psCreateLink.setString(7, description);
			}
			psCreateLink.executeUpdate();
			result = true;
		}
		catch (SQLException e)
		{
			if(e.getMessage().equals("[SQLITE_CONSTRAINT]  Abort due to constraint violation (FOREIGN KEY constraint failed)"))
				logger.warn("Video not found in Database");
			else
				logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Get the Number of Extractions a Server has done
	 * @param guildID the UserID of the User
	 * @return null if the user does not exist, or a NormalUser object
	 */
	public ArrayList<DBLink> getLink(String keyword, boolean isEpisodeKeyword, boolean ignoreUsable)
	{
		ArrayList<DBLink> link = new ArrayList<DBLink>();
		lock.lock();
		try
		{
			if (isEpisodeKeyword)
			{
				keyword = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("[", "![");
				psGetLinkByTitle.setString(1, "%" + keyword + "%");
				ResultSet rs = psGetLinkByTitle.executeQuery();
				while (rs.next())
				{
					if (rs.getBoolean(5) || ignoreUsable)
					{
						link.add(new DBLink(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
					}
				}
				if (link.isEmpty())
				{
					logger.warn("Episode Title {} does not Exist in `Links` Database!", keyword);
				}
				rs.close();
			}
			else
			{
				psGetLinkByName.setString(1, keyword);
				ResultSet rs = psGetLinkByName.executeQuery();
				while (rs.next())
				{
					if (rs.getBoolean(4) || ignoreUsable)
					{
						link.add(new DBLink(rs.getString(1), rs.getString(2), rs.getString(3), keyword));
					}
				}
				if (link.isEmpty())
				{
					logger.warn("Filename `{}` does not Exist in `Links` Database!", keyword);
				}
				rs.close();
			}
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return link;
	}

	/**
	 * Remove a Link from the Database
	 * @param link YouTube Links only
	 * @param nickname Video file nickname, attached to Videos Table
	 * @param episodename Secondary Nickname that could be anything
	 * @return if Link was found
	 */
	public boolean deleteLink(String link, String nickname, String episodename)
	{
		boolean result = false;
		lock.lock();
		try
		{
			if (link != null)
			{
				psDeleteLink1.setString(1, link);
				psDeleteLink1.executeUpdate();
				if (psDeleteLink1.getUpdateCount() > 0)
					result = true;
			}
			else if (nickname != null)
			{
				psDeleteLink2.setString(1, nickname);
				psDeleteLink2.executeUpdate();
				if (psDeleteLink2.getUpdateCount() > 0)
					result = true;
			}
			else if (episodename != null)
			{
				psDeleteLink3.setString(1, episodename);
				psDeleteLink3.executeUpdate();
				if (psDeleteLink3.getUpdateCount() > 0)
					result = true;
			}
			else
			{
				logger.warn("All Fields in Database.deleteLink() method were null, nothing to delete");
			}
		}
		catch (SQLException e)
		{
			logger.catching(e);
		}
		lock.unlock();
		return result;
	}

	/**
	 * Updates a Video's default offset
	 * @param nickname Nickname of Video to affect
	 * @param offset Long of millisecond time to store as offset
	 * @return if the Video was found
	 */
	public boolean updateVideoOffset(String nickname, long offset)
	{
		boolean result = false;
		lock.lock();
		try
		{
			psUpdateOffset.setLong(1, offset);
			psUpdateOffset.setString(2, nickname);
			psUpdateOffset.executeUpdate();
			if (psUpdateOffset.getUpdateCount() > 0)
				result = true;
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
				"CREATE TABLE IF NOT EXISTS `ExtractionRecord` ( `Date` INTEGER NOT NULL UNIQUE, `Elevated` INTEGER NOT NULL DEFAULT 0, `UserID` INTEGER NOT NULL, `GuildID` INTEGER NOT NULL, `ChannelID` INTEGER NOT NULL, `MessageID` INTEGER NOT NULL UNIQUE, `Filename` TEXT NOT NULL, `Timestamp` TEXT NOT NULL, `Frame Count` INTEGER, `Url` TEXT NOT NULL, FOREIGN KEY(`Filename`) REFERENCES `Videos`(`Filename`) ON UPDATE CASCADE ON DELETE NO ACTION, PRIMARY KEY(`Date`), FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE NO ACTION, FOREIGN KEY(`ChannelID`) REFERENCES `ChannelPerms`(`ChannelID`) ON UPDATE CASCADE ON DELETE NO ACTION, FOREIGN KEY(`UserID`) REFERENCES `UserRecord`(`UserID`) ON UPDATE CASCADE ON DELETE NO ACTION )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `Guilds` ( `GuildID` INTEGER NOT NULL UNIQUE, `RequestLimit` INTEGER NOT NULL DEFAULT 1000, `UsedToday` INTEGER NOT NULL DEFAULT 0, `Enabled` INTEGER NOT NULL DEFAULT 1, `isBlacklist` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`GuildID`) )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `Links` ( `Title` TEXT NOT NULL UNIQUE, `Description` TEXT, `Link` TEXT NOT NULL, `Usable` INTEGER NOT NULL, `Nickname` TEXT, PRIMARY KEY(`Nickname`), FOREIGN KEY(`Nickname`) REFERENCES `Videos`(`Nickname`) ON UPDATE CASCADE ON DELETE SET NULL);");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `Moments` ( `GuildID` INTEGER NOT NULL, `Name` TEXT NOT NULL, `Nickname` TEXT NOT NULL, `Timestamp` TEXT NOT NULL, `TimesUsed` INTEGER NOT NULL DEFAULT 1, `Disabled` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE CASCADE, FOREIGN KEY(`Nickname`) REFERENCES `Videos`(`Nickname`) ON UPDATE CASCADE ON DELETE CASCADE );");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `RolePerms` ( `RoleID` INTEGER NOT NULL, `GuildID` INTEGER NOT NULL, `BlackVSWhite` INTEGER NOT NULL, FOREIGN KEY(`GuildID`) REFERENCES `Guilds`(`GuildID`) ON UPDATE CASCADE ON DELETE CASCADE )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `UserRecord` ( `UserID` INTEGER NOT NULL UNIQUE, `TimesUsed` INTEGER NOT NULL, `UsedToday` INTEGER NOT NULL, `GlobalBan` INTEGER NOT NULL DEFAULT 0, `VIP` INTEGER NOT NULL, PRIMARY KEY(`UserID`) )");
		stmt.executeUpdate(
				"CREATE TABLE IF NOT EXISTS `Videos` ( `Filename` TEXT NOT NULL UNIQUE, `Nickname` TEXT NOT NULL UNIQUE, `Length` INTEGER NOT NULL, `Offset` INTEGER NOT NULL, `Fps` STRING NOT NULL, `Restricted` INTEGER NOT NULL DEFAULT 0, `Usable` INTEGER NOT NULL DEFAULT 1, `Linked` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`Filename`) )");
		//CreateTriggers
		stmt.executeUpdate(
				"CREATE TRIGGER IF NOT EXISTS AutoAddExtractionRecordUser BEFORE INSERT ON `ExtractionRecord` BEGIN INSERT INTO UserRecord VALUES(NEW.UserID,1,1,0,0) ON CONFLICT (`UserID`) DO UPDATE SET `TimesUsed` = `TimesUsed` + 1, `UsedToday` = `UsedToday` + 1; UPDATE `Guilds` SET `UsedToday` = `UsedToday` + 1 WHERE NEW.Elevated = 0 AND `GuildID` = NEW.GuildID;END");
		stmt.executeUpdate(
				"CREATE TRIGGER IF NOT EXISTS AutoDisableDeletedVideo BEFORE DELETE ON `Videos` BEGIN UPDATE `Moments` SET `Disabled` = 1 WHERE 'Nickname' = OLD.Nickname; UPDATE `Links` SET `Usable` = 0 WHERE `Nickname` = OLD.Nickname; END");
		stmt.executeUpdate(
				"CREATE TRIGGER IF NOT EXISTS AutoRemoveUpdatedChannel AFTER UPDATE OF `Permission`, `Annoucements` ON `ChannelPerms` WHEN NEW.Permission = 0 AND NEW.Annoucements = 0 BEGIN DELETE FROM `ChannelPerms` WHERE `ChannelID` = NEW.ChannelID; END");
		stmt.executeUpdate(
				"CREATE TRIGGER IF NOT EXISTS AutoRemoveInsertedChannel AFTER INSERT ON `ChannelPerms` WHEN NEW.Permission = 0 AND NEW.Annoucements = 0 BEGIN DELETE FROM `ChannelPerms` WHERE `ChannelID` = NEW.ChannelID; END");
		stmt.executeUpdate(
				"CREATE TRIGGER IF NOT EXISTS AutoLinkVideo AFTER INSERT ON `Links` WHEN NEW.Nickname IS NOT NULL BEGIN UPDATE `Videos` SET `Linked` = 1 WHERE `Nickname` = NEW.Nickname; END");
		stmt.executeUpdate(
				"CREATE TRIGGER IF NOT EXISTS AutoUnlinkVideo AFTER DELETE ON `Links` WHEN OLD.Nickname IS NOT NULL BEGIN UPDATE `Videos` SET `Linked` = 0 WHERE `Nickname` = OLD.Nickname; END");
	}

	private void generateStatements() throws SQLException
	{
		psCheckIfGuildExists = c.prepareStatement("SELECT EXISTS(SELECT 1 FROM `Guilds` WHERE `GuildID` = ? LIMIT 1);");
		psAddNewGuild = c.prepareStatement("INSERT INTO `Guilds` VALUES(?,1000,0,1,1);");
		psRemoveGuild = c.prepareStatement("DELETE FROM `Guilds` WHERE `GuildID` = ?;");
		psGetAllChannels = c.prepareStatement("SELECT `ChannelID`, `Permission`, `Annoucements` FROM `ChannelPerms`;");
		psListOfConfiguredChannels = c.prepareStatement("SELECT `ChannelID`, `Permission`, `Annoucements` FROM `ChannelPerms` WHERE `GuildID` = ?;");
		psRemoveChannel = c.prepareStatement("DELETE FROM `ChannelPerms` WHERE `ChannelID` = ?;");
		psAddNewAdminRole = c.prepareStatement("INSERT INTO `AdminRoles` VALUES(?,?);");
		psListOfAdminRoles = c.prepareStatement("SELECT `RoleID` FROM `AdminRoles` WHERE `GuildID` = ? ORDER BY `RoleID` ASC;");
		psRemoveAdminRole = c.prepareStatement("DELETE FROM `AdminRoles` WHERE `RoleID` = ?;");
		psAddNewUserRole = c.prepareStatement("INSERT INTO `RolePerms` VALUES(?,?,?);");
		psListOfUserRoles = c.prepareStatement("SELECT `RoleID` FROM `RolePerms` WHERE `GuildID` = ?;");
		psRemoveUserRole = c.prepareStatement("DELETE FROM `RolePerms` WHERE `RoleID` = ?;");
		psAddOrUpdateVideo = c.prepareStatement(
				"INSERT INTO `Videos` (`Filename`,`Nickname`,`Length`,`Offset`,`Fps`,`Restricted`,`Usable`,`Linked`) VALUES(?,?,?,?,?,COALESCE(?,0),1,0) ON CONFLICT(`Nickname`) DO UPDATE SET `Length` = excluded.`Length`, `Offset` = excluded.`Offset`, `Fps` = excluded.`Fps`, `Restricted` = COALESCE(?, excluded.`Restricted`), `Usable` = 1 WHERE `Filename` = excluded.`Filename`;");
		psGetVideoByFilename = c.prepareStatement(
				"SELECT `Filename`, `Length`, `Offset`, `Fps`, `Restricted`, `Usable`, `Linked` FROM `Videos` WHERE `Nickname` = ? LIMIT 1");
		psRemoveVideo = c.prepareStatement("DELETE FROM `Videos` WHERE `Nickname` = ?;");
		psCheckIFVIP = c.prepareStatement("SELECT EXISTS(SELECT 1 FROM `UserRecord` WHERE `UserID` = ? AND `VIP` = 1 LIMIT 1);");
		psWipeAllVideos = c.prepareStatement("DELETE FROM `Videos`;");
		psCountVisibleVideos = c.prepareStatement("SELECT COUNT(rowid) FROM `Videos` WHERE `Usable` = 1 AND `Restricted` IN (0,?);");
		psCheckChannelTier = c.prepareStatement("SELECT `Permission` FROM `ChannelPerms` WHERE `ChannelID` = ? LIMIT 1;");
		psGetRolePerms = c.prepareStatement("SELECT `RoleID`, `BlackVSWhite` FROM `RolePerms` WHERE `GuildID`= ? ORDER BY `RoleID` ASC;");
		psCheckGuildBlacklistMode = c.prepareStatement("SELECT `isBlacklist` FROM `Guilds` WHERE `GuildID` = ?;");
		psChangeGuildBlacklistMode = c.prepareStatement("UPDATE `Guilds` SET `isBlacklist` = ? WHERE `GuildID` = ?;");
		psRemoveAllUserRoles = c.prepareStatement("DELETE FROM `RolePerms` WHERE `GuildID` = ?;");
		psAddNewVIP = c.prepareStatement(
				"INSERT INTO `UserRecord` (`UserID`,`TimesUsed`,`UsedToday`,`GlobalBan`,`VIP`) VALUES(?,0,0,0,1) ON CONFLICT(`UserID`) DO UPDATE SET `GlobalBan` = 0, `VIP` = 1;");
		psRemoveVIP = c.prepareStatement("UPDATE `UserRecord` SET `VIP` = 0 WHERE `UserID` = ?;");
		psUpdateChannelTier = c.prepareStatement(
				"INSERT INTO `ChannelPerms` VALUES(?, ?, ?, 0) ON CONFLICT(`ChannelID`) DO UPDATE SET `Permission` = ? WHERE `ChannelID` = ? AND `GuildID` = ?;");
		psSetChannelAnnoucementsTrue = c.prepareStatement(
				"INSERT INTO `ChannelPerms` VALUES(?, ?, 0, 1) ON CONFLICT(`ChannelID`) DO UPDATE SET `Annoucements` = 1 WHERE `ChannelID` = ?;");
		psSetChannelAnnoucementsFalse = c.prepareStatement("UPDATE `ChannelPerms` SET `Annoucements` = 0 WHERE `ChannelID` = ?");
		psGetVideoList = c.prepareStatement(
				"SELECT `Filename`, `Nickname`, `Length`, `Offset`, `Fps`, `Restricted`, `Usable`, `Linked` FROM `Videos` ORDER BY `Filename` ASC;");
		psSetVideoUsability = c.prepareStatement("UPDATE `Videos` SET `Usable` = ? WHERE `Nickname` = ?;");
		psSetVideoRestricted = c.prepareStatement("UPDATE `Videos` SET `Restricted` = ? WHERE `Nickname` = ?;");
		psSetAllVideosRestricted = c.prepareStatement("UPDATE `Videos` SET `Restricted` = ?");
		psGetUserData = c.prepareStatement("SELECT `TimesUsed`, `UsedToday`, `GlobalBan`, `VIP` FROM `UserRecord` WHERE `UserID`= ? LIMIT 1;");
		psRecordExtraction = c.prepareStatement("INSERT INTO `ExtractionRecord` VALUES(?,?,?,?,?,?,?,?,?,?);");
		psUpdateUserDailyUsage = c.prepareStatement("UPDATE `UserRecord` SET `UsedToday` = ? WHERE `UserID` = ?;");
		psUpdateAllUserDailyUsage = c.prepareStatement("UPDATE `UserRecord` SET `UsedToday` = ?;");
		psGetServerUsage = c.prepareStatement(
				"SELECT `GuildID`, `RequestLimit`, `UsedToday`, `Enabled`, `isBlacklist` FROM `Guilds` WHERE `GuildID` = ? LIMIT 1;");
		psUpdateServerDailyUsage = c.prepareStatement("UPDATE `Guilds` SET `UsedToday` = ? WHERE `GuildID` = ?;");
		psUpdateAllServerDailyUsage = c.prepareStatement("UPDATE `Guilds` SET `UsedToday` = ?;");
		psUpdateServerDailyLimit = c.prepareStatement("UPDATE `Guilds` SET `RequestLimit` = ? WHERE `GuildID` = ?;");
		psUpdateServerStanding = c.prepareStatement("UPDATE `Guilds` SET `Enabled` = ? WHERE `GuildID` = ?;");
		psGetLinkByTitle = c
				.prepareStatement("SELECT `Link`, `Title`, `Description`, `Nickname`, `Usable` FROM `Links` WHERE `Title` LIKE ? ESCAPE '!';");
		psGetLinkByName = c.prepareStatement("SELECT `Link`, `Title`, `Description`, `Usable` FROM `Links` WHERE `Nickname` = ? LIMIT 1;");
		psCreateLink = c.prepareStatement(
				"INSERT INTO `Links` VALUES(?, ?, ?, 1, ?) ON CONFLICT(`Nickname`) DO UPDATE SET `Link` = ?, `Title` = ?, `Description` = ?, `Usable` = 1;");
		psDeleteLink1 = c.prepareStatement("DELETE FROM `Links` WHERE `Link` = ?");
		psDeleteLink2 = c.prepareStatement("DELETE FROM `Links` WHERE `Nickname` = ?");
		psDeleteLink3 = c.prepareStatement("DELETE FROM `Links` WHERE `Title` = ?");
		psUpdateOffset = c.prepareStatement("UPDATE `Videos` SET `Offset` = ? WHERE `Nickname` = ?;");
	}

	public class DBChannel
	{

		private long id;
		private int tier;
		private boolean annoucement;

		private DBChannel(long id, int tier, boolean annoucement)
		{
			this.id = id;
			this.tier = tier;
			this.annoucement = annoucement;
		}

		public long getId()
		{
			return id;
		}

		public int getTier()
		{
			return tier;
		}

		public boolean isAnnoucement()
		{
			return annoucement;
		}
	}

	public class DBNormalUser
	{

		private long id;
		private int totalUsed;
		private int usedToday;
		private boolean banned;
		private boolean vip;

		public DBNormalUser(long id)
		{
			this.id = id;
			totalUsed = 0;
			usedToday = 0;
			banned = false;
			vip = false;
		}

		private DBNormalUser(long id, int totalUsed, int usedToday, boolean banned, boolean vip)
		{
			this.id = id;
			this.totalUsed = totalUsed;
			this.usedToday = usedToday;
			this.banned = banned;
			this.vip = vip;
		}

		public long getId()
		{
			return id;
		}

		public int getTotalUsed()
		{
			return totalUsed;
		}

		public int getUsedToday()
		{
			return usedToday;
		}

		public boolean isBanned()
		{
			return banned;
		}

		public boolean isVip()
		{
			return vip;
		}
	}

	public class DBRolePerm
	{

		private long roleID;
		private boolean blackVSwhite;

		private DBRolePerm(long roleID, boolean blackVSwhite)
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

	public class DBVideo
	{

		private String filename;
		private String nickname;
		private long length;
		private long offset;
		private String fps;
		private boolean restricted;
		private boolean usable;
		private boolean linked;

		private DBVideo(String f, String n, long l, long o, String s, boolean r, boolean u, boolean li)
		{
			filename = f;
			nickname = n;
			length = l;
			offset = o;
			fps = s;
			restricted = r;
			usable = u;
			linked = li;
		}

		public String getFilename()
		{
			return filename;
		}

		public String getNickname()
		{
			return nickname;
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

		public boolean isRestricted()
		{
			return restricted;
		}

		public boolean isUsable()
		{
			return usable;
		}

		public boolean isLinked()
		{
			return linked;
		}
		
		public String toString()
		{
			return filename;
		}
	}

	public class DBGuild
	{

		private long guildID;
		private int requestLimit;
		private int usedToday;
		private boolean enabled;
		private boolean isBlacklistMode;

		private DBGuild(long g, int r, int u, boolean e, boolean b)
		{
			guildID = g;
			requestLimit = r;
			usedToday = u;
			enabled = e;
			isBlacklistMode = b;
		}

		public long getGuildID()
		{
			return guildID;
		}

		public int getRequestLimit()
		{
			return requestLimit;
		}

		public int getUsedToday()
		{
			return usedToday;
		}

		public boolean isEnabled()
		{
			return enabled;
		}

		public boolean isBlacklistMode()
		{
			return isBlacklistMode;
		}

	}

	public class DBLink
	{

		private String link;
		private String title;
		private String description;
		private String nickname;

		private DBLink(String l, String t, String d, String n)
		{
			link = l;
			title = t;
			description = d;
			nickname = n;
		}

		public String getLink()
		{
			return link;
		}

		public String getTitle()
		{
			return title;
		}

		public String getDescription()
		{
			return description;
		}

		public String getNickname()
		{
			return nickname;
		}
	}
}
