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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.RequestBuffer;

/**
 * @author Christian77777
 *
 */
public class LocalInterface implements IListener<ReadyEvent>
{

	private static Logger logger = LogManager.getLogger();
	public static Image image = new ImageIcon(LocalInterface.class.getResource("/evo.gif")).getImage();
	private PopupMenu menu;
	private MenuItem refresh;
	private MenuItem openText;
	private MenuItem annoucement;
	private MenuItem reinit;
	private MenuItem quit;
	private TrayIcon icon;
	private DRI controller;
	private boolean useGUI = true;

	/**
	 * Allocate UI elements and initalize as much as possible before Connection Initiated
	 * @param c
	 */
	public LocalInterface(DRI c)
	{
		controller = c;
		menu = new PopupMenu();//Must exist for boolean down the road
		menu.setEnabled(false);
		if (SystemTray.isSupported())
		{
			//Create Menu Items and build Tray icon
			logger.info("Tray Menu Supported");
			refresh = new MenuItem("File Refresh");
			openText = new MenuItem("Open values.txt");
			annoucement = new MenuItem("Send Message");
			reinit = new MenuItem("Reinitalize Properties");
			quit = new MenuItem("Quit");

			menu.add(refresh);
			menu.addSeparator();
			menu.add(openText);
			menu.add(annoucement);
			menu.addSeparator();
			menu.add(reinit);
			menu.add(quit);

			icon = new TrayIcon(image, "Frame-Summoner Discord Bot", menu);
			icon.setImageAutoSize(true);
			//Install Commands into Menu Items
			setGUICommands();
		}
		else
		{
			logger.warn("Tray Menu is not supported on this OS");
			useGUI = false;
		}
	}

	/**
	 * End Initialization, will hold thread for CUI input
	 */
	@Override
	public void handle(ReadyEvent event)
	{
		if (menu.isEnabled())
		{
			logger.info("Frame-Summoner rebooted");
		}
		else
		{
			logger.info("Frame-Summoner is now ready");
			if (useGUI)
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
				menu.setEnabled(true);
				icon.displayMessage("Setup Complete", "Frame-Summoner is now ready", TrayIcon.MessageType.INFO);
			}
			else
			{
				startCLICommands();
			}
		}
	}

	private void startCLICommands()
	{
		Thread t = new Thread()
		{

			public void run()
			{
				Thread.currentThread().setName("ConsoleUI");
				boolean running = true;
				System.out.print("\nConsole Interface\n======================\n1. Refresh Filesystem\n2. Send Message\n3. Reinitalize Properties\n4. Quit\n\n > ");
				Scanner s = new Scanner(System.in);
				while (running)
				{
					String answer = s.nextLine();
					switch (answer)
					{
						case "1":
							controller.refreshVideos();
							System.out.print("\nVideos Refreshed\n\n> ");
							break;
						case "2":
							ArrayList<IChannel> channels = controller.getAnnoucementChannels();
							if (channels.isEmpty())
							{
								System.out.print("\nNo avaliable Channels\n\n> ");
								return;
							}
							System.out.print("\nPick a Channel or ALL Channels\n0. <ALL CHANNELS>\n");
							for (int x = 0; x < channels.size(); x++)
							{
								System.out.println((x + 1) + ". " + channels.get(x).getGuild().getName() + " - " + channels.get(x).getName());
							}
							System.out.print("\nChannel > ");
							String server = s.nextLine();
							try
							{
								int index = Integer.parseInt(server);
								if (index == 0)//All Channels
								{
									System.out.print("\nMessage > ");
									String message = s.nextLine();
									for (IChannel c : channels)
									{
										RequestBuffer.request(() -> {
											c.sendMessage(message);
										}).get();
									}
									System.out.print("\nMessage Sent to all Channels\n\n> ");
								}
								else if (index > 0 || index < channels.size())//Single Channel
								{
									final int index2 = index - 1;
									System.out.print("\nMessage > ");
									String message = s.nextLine();
									RequestBuffer.request(() -> {
										channels.get(index2).sendMessage(message);
									}).get();
									System.out.print("\nMessage Sent to " + channels.get(index2).getName() + "\n\n> ");
								}
								else//Out Of Range
								{
									System.out.print("\nChannel Not Selected, ignoring Command\n\n> ");
								}
							}
							catch (NumberFormatException e)
							{
								System.out.print("\nInvalid Input\n\n> ");
							}
							break;
						case "3":
							System.out.print("\nReading Config...\n");
							restartUI();
							System.out.print("\n> ");
							break;
						case "4":
							System.out.print("\nShutting Down...\n");
							quitUI();
							running = false;
							break;
						default:
							System.out.print("\nInvalid Command\n1. Refresh Filesystem\n2. Send Message\n3. Reinitalize Properties\n4. Quit\n\n > ");
							break;
					}
				}
				s.close();
			}
		};
		t.start();
	}

	private void setGUICommands()
	{
		quit.addActionListener(e -> {
			quitUI();
		});

		reinit.addActionListener(e -> {
			restartUI();
		});

		annoucement.addActionListener(e -> {
			ArrayList<IChannel> channels = controller.getAnnoucementChannels();
			if (channels.isEmpty())
			{
				logger.warn("No Annoucement Channels found for \"Send Message\" GUI Command");
				JOptionPane.showMessageDialog(null, "No Channels were found where the message could be sent", "No Channels",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			String[] options = new String[channels.size() + 1];
			options[0] = "<ALL CHANNELS>";
			for (int x = 0; x < channels.size(); x++)
			{
				options[x + 1] = (x + 1) + ". " + channels.get(x).getGuild().getName() + " - " + channels.get(x).getName();
			}
			String s = (String) JOptionPane.showInputDialog(null, "Pick the Channel where the Message is to be Sent", "Channel Selection",
					JOptionPane.QUESTION_MESSAGE, new ImageIcon(LocalInterface.image), options, options[0]);
			if (s == null)
			{
				logger.warn("Command Cancelled, no channel was selected");
			}
			else
			{
				try
				{
					if (s.equals("<ALL CHANNELS>"))
					{
						String message = (String) JOptionPane.showInputDialog(null, "Enter Text for message to Send", "Frame-Summoner Message",
								JOptionPane.QUESTION_MESSAGE, new ImageIcon(LocalInterface.image), null, null);
						for (IChannel c : channels)
						{
							RequestBuffer.request(() -> {
								c.sendMessage(message);
							});
						}
						logger.info("Sent Message to ALL annoucement channels: {}",message);
						JOptionPane.showMessageDialog(null, "All messages were sent successfully", "Message Sent", JOptionPane.INFORMATION_MESSAGE);
					}
					else
					{
						final int index = Integer.parseInt(s.substring(0, s.indexOf("."))) - 1;
						String message = (String) JOptionPane.showInputDialog(null, "Enter Text for message to Send", "Frame-Summoner Message",
								JOptionPane.QUESTION_MESSAGE, new ImageIcon(LocalInterface.image), null, null);
						RequestBuffer.request(() -> {
							channels.get(index).sendMessage(message);
						});
						logger.info("Sent Message to \"{}\": {}",channels.get(index),message);
						JOptionPane.showMessageDialog(null, "Message was sent Successfully", "Message Sent", JOptionPane.INFORMATION_MESSAGE);
					}
				}
				catch (NumberFormatException ex)
				{
					logger.fatal("Somehow number format exception for GUI attempting to parse; {}", s);
				}
				catch (Exception ex)//Message send Failure
				{
					JOptionPane.showMessageDialog(null, "Message Failed to Send", "Send Failure", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		refresh.addActionListener(e -> {
			controller.refreshVideos();
		});

		//GUI Only
		openText.addActionListener(e -> {
			Desktop desktop;
			if (Desktop.isDesktopSupported() && (desktop = Desktop.getDesktop()).isSupported(Desktop.Action.EDIT))
			{
				try
				{
					desktop.open(new File(DRI.dir + File.separator + "config.properties"));
					logger.debug("Properties Opened");
				}
				catch (IOException e1)
				{
					logger.error("Failed to open values.txt by Desktop method", e1);
				}
			}
			else
			{
				logger.warn("Desktop not supported");
			}
		});
	}

	public void restartUI()
	{
		controller.readConfig();
		controller.refreshVideos();
	}

	public void quitUI()
	{
		try
		{
			controller.disconnect("Shutting Down...");
			Thread.sleep(3000);
		}
		catch (NullPointerException e)
		{
			logger.warn("Listener failed to be removed", e);
		}
		catch (InterruptedException e)
		{
			//Doesnt Matter
		}
		System.exit(0);
	}
}
