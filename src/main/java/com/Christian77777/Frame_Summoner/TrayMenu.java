/**
 * 
 */

package com.Christian77777.Frame_Summoner;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Christian77777
 *
 */
public class TrayMenu
{
	private static Logger logger = LogManager.getLogger();
	public static Image image = new ImageIcon(TrayMenu.class.getResource("/evo.gif")).getImage();
	private MenuItem refresh;
	private MenuItem openText;
	private MenuItem request;
	private MenuItem restart;
	private MenuItem quit;
	private TrayIcon icon;
	private DRI controller;

	public TrayMenu(DRI c)
	{
		DRI.menu = this;
		controller = c;
		if (!SystemTray.isSupported())
		{
			System.out.println("System Menu is not supported on this OS");
			return;
		}
		refresh = new MenuItem("File Refresh");
		openText = new MenuItem("Open values.txt");
		request = new MenuItem("Send Message");
		restart = new MenuItem("Restart");
		quit = new MenuItem("Quit");

		PopupMenu menu = new PopupMenu();
		menu.add(refresh);
		menu.addSeparator();
		menu.add(openText);
		menu.add(request);
		menu.addSeparator();
		menu.add(restart);
		menu.add(quit);

		icon = new TrayIcon(image, "Frame-Summoner Discord Bot", menu);
		icon.setImageAutoSize(true);

		setCommands();
	}

	public void showMenu()
	{
		SystemTray tray = SystemTray.getSystemTray();
		try
		{
			tray.add(icon);
		}
		catch (AWTException e1)
		{
			logger.error("Menu Tray not supported on this OS", e1);
		}
	}

	public void setupComplete()
	{
		icon.displayMessage("Setup Complete", "Frame-Summoner is now ready to submit frames to Discord", TrayIcon.MessageType.INFO);
	}

	public void setCommands()
	{

		quit.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					controller.disconnect("Shutting Down...");
				}
				catch (NullPointerException f)
				{
					logger.warn("Listener failed to be removed", f);
				}
				System.exit(0);
			}
		});

		restart.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				controller.disconnect("Rebooting...");
				try
				{
					Thread.sleep(3000);
				}
				catch (InterruptedException e1)
				{
					logger.warn("Bot Reboot Sleep interrupted");
				}
				controller = new DRI();
				controller.connectToDiscord();
			}
		});

		request.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				controller.sendMessage();
			}
		});

		refresh.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				controller.refreshVideos();
			}
		});

		openText.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				Desktop desktop;
				if (Desktop.isDesktopSupported() && (desktop = Desktop.getDesktop()).isSupported(Desktop.Action.EDIT))
				{
					try
					{
						desktop.open(new File(DRI.dir + File.separator + "values.txt"));
					}
					catch (IOException e1)
					{
						logger.error("Failed to open values.txt by Desktop method",e1);
					}
				}
				else
				{
					logger.warn("Desktop not supported");
					try
					{
						//Windows Only
						@SuppressWarnings("unused")
						Process p = new ProcessBuilder("notepad", DRI.dir + File.separator + "values.txt").start();
					}
					catch (IOException e2)
					{
						logger.error("Failed to open values.txt by Process method",e2);
					}
				}
			}
		});
	}

	/**
	 * Allow for Restart from Discord Commands while letting command complete
	 */
	public void manualRestart(int x)
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					Thread.sleep(x);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				restart.dispatchEvent(new ActionEvent(restart, ActionEvent.ACTION_PERFORMED, "simulate Restart"));
			}
		};
		t.start();
	}
}
