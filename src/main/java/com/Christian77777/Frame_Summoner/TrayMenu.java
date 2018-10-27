/**
 * 
 */
package com.Christian77777.Frame_Summoner;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 * @author Christian77777
 *
 */
public class TrayMenu
{
	public static Image image = new ImageIcon(DRI.class.getResource("/evo.gif")).getImage();
	private MenuItem refresh;
	private MenuItem request;
	private MenuItem restart;
	private MenuItem quit;
	private TrayIcon icon;
	private DRI controller;
	
	public TrayMenu()
	{
		if (!SystemTray.isSupported())
		{
			System.out.println("System Menu is not supported on this OS");
			return;
		}
		refresh = new MenuItem("File Refresh");
		request = new MenuItem("Send Message");
		restart = new MenuItem("Restart");
		quit = new MenuItem("Quit");
		
		PopupMenu menu = new PopupMenu();
		menu.add(refresh);
		menu.addSeparator();
		menu.add(request);
		menu.addSeparator();
		menu.add(restart);
		menu.add(quit);
		
		icon = new TrayIcon(image, "Frame-Summoner Discord Bot", menu);
		icon.setImageAutoSize(true);
		SystemTray tray = SystemTray.getSystemTray();
		try
		{
			tray.add(icon);
			icon.displayMessage("Setup Complete", "Frame-Summoner is now ready to submit frames to Discord", TrayIcon.MessageType.INFO);
		}
		catch (AWTException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public void setCommands()
	{
		quit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				api.getDispatcher().unregisterListener(actions);
				api.logout();
				System.exit(0);
			}
		});

		restart.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				api.getDispatcher().unregisterListener(actions);
				api.logout();
				readConfig();
				connectToDiscord();
			}
		});

		request.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String s = (String) JOptionPane.showInputDialog(null, "Send Message or Command from Frame-Summoner Discord Bot",
						"Frame-Summoner Message", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(image), null, null);
				adminChannel.sendMessage(s.substring(0, Math.min(s.length(), 2000)));
			}
		});
		refresh.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					int fileCount = new File(videoDir).listFiles().length;
				}
				catch (NullPointerException f)
				{
					logger.error("File Path Provided in values.txt invalid", f);
				}
			}
		});
	}
}
