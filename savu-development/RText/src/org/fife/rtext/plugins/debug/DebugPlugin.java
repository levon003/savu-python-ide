/*
 * 12/17/2010
 *
 * Plugin.java - Embeds a debug options panel in RText.
 * Copyright (C) 2010 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.debug;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;

import org.fife.rtext.AbstractMainView;
import org.fife.rtext.Savu;
import org.fife.rtext.RTextEditorPane;
import org.fife.rtext.RTextMenuBar;
import org.fife.rtext.RTextUtilities;
import org.fife.rtext.plugins.run.PythonProcess;
import org.fife.ui.app.AbstractPluggableGUIApplication;
import org.fife.ui.app.GUIPlugin;
import org.fife.ui.app.PluginOptionsDialogPanel;
import org.fife.ui.app.StandardAction;

/**
 * A plugin that allows the user control of a pdb debugger
 * 
 * @author PyDE Comps Group
 * @version 1.0
 */
public class DebugPlugin extends GUIPlugin {

	private static final String VERSION = "1.0";
	private static final String DOCKABLE_WINDOW_DEBUG = "debugDockableWindow";
	private static final Color DEBUG_HIGHLIGHT_COLOR = new Color(50, 120, 55);
	
	private Savu app;
	private DebugWindow window;
	private Icon icon;

	private static final String MSG = "org.fife.rtext.plugins.debug.Plugin";
	protected static final ResourceBundle msg = ResourceBundle.getBundle(MSG);

	private static final String VIEW_DEBUG_ACTION = "viewDebugAction";

	private PythonDebugger pyDebugger;
	//An object representing the currently highlighted line
	private Object highlightedLine = null;
	//The file in which the currently highlighted line is located
	private String highlightedFilePath = null;
	
	
	/**
	 * Constructor.
	 * 
	 * @param app
	 *            The parent application.
	 */
	public DebugPlugin(AbstractPluggableGUIApplication app) {

		this.app = (Savu) app;
		// Load the plugin icon.

		DebugPrefs prefs = loadPrefs();

		StandardAction a = new ViewDebugAction(this.app, msg, this);
		a.setAccelerator(prefs.windowVisibilityAccelerator);
		app.addAction(VIEW_DEBUG_ACTION, a);

		// Window MUST always be created for preference saving on shutdown
		window = new DebugWindow(this.app, this);
		window.setPosition(prefs.windowPosition);
		window.setActive(prefs.windowVisible);
		window.setBackground(Color.BLACK); 
		putDockableWindow(DOCKABLE_WINDOW_DEBUG, window);
		pyDebugger = null;
		icon = window.getDebugIcon();
	}
	
	/**
	 * Starts debugging the specified python file
	 * Returns an object containing the input, output and error streams
	 */
	public InputStream[] debugPythonFile(String fileName, List<Breakpoint> breaks, String args) {
		PipedOutputStream debugOutput = new PipedOutputStream();
		try {
			pyDebugger = new PythonDebugger(debugOutput, fileName, breaks, args);
		} catch (NullPointerException ex) { //Couldn't exec process.
			return null;
		}
		pyDebugger.addObserver(window.getTreeTableModel());
		pyDebugger.addObserver(window); 
		
		Thread debuggerThread = new Thread(pyDebugger);
		debuggerThread.start();
		PipedInputStream consoleInput = null;
		try
		{
			consoleInput = new PipedInputStream(debugOutput);
		}
		catch (IOException e)
		{
			//Panic!
			//TODO: Something other than panic?
		}
		
		return new InputStream[]{consoleInput, pyDebugger.getErrorStream()};
	}
	
	/**
	 * Returns a reference to the most recently run process, which may or may not be stopped. Can also return null if no process has been run.
	 * @return
	 */
	public PythonProcess getCurrentProcess()
	{
		return pyDebugger;
	}
	
	public void setDebugStopButtonEnabled(boolean enabled) {
		window.setStopButtonEnabled(enabled);
	}
	
	/**
	 * Returns the dockable window containing the debug display.
	 * 
	 * @return The dockable window.
	 */
	public DebugWindow getDockableWindow() {
		return window;
	}

	/**
	 * {@inheritDoc}
	 */
	public PluginOptionsDialogPanel getOptionsDialogPanel() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPluginAuthor() {
		return "Robert Futrelle Jr.";
	}

	/**
	 * {@inheritDoc}
	 */
	public Icon getPluginIcon() {
		return icon;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPluginName() {
		return msg.getString("Plugin.Name");
	}

	/**
	 * {@inheritDoc}
	 */
	public String getPluginVersion() {
		return VERSION;
	}

	/**
	 * Returns the file preferences for this plugin are saved in.
	 * 
	 * @return The file.
	 */
	private static final File getPrefsFile() {
		return new File(RTextUtilities.getPreferencesDirectory(),
				"debug.properties");
	}

	/**
	 * Returns the parent application.
	 * 
	 * @return The parent application.
	 */
	public Savu getRText() {
		return app;
	}

	/**
	 * Returns a localized message.
	 * 
	 * @param key
	 *            The key.
	 * @return The localized message.
	 * @see #getString(String, String)
	 * @see #getString(String, String, String)
	 */
	public String getString(String key) {
		return msg.getString(key);
	}

	/**
	 * Returns a localized message.
	 * 
	 * @param key
	 *            The key.
	 * @param param
	 *            A parameter for the localized message.
	 * @return The localized message.
	 * @see #getString(String)
	 * @see #getString(String, String, String)
	 */
	public String getString(String key, String param) {
		String temp = msg.getString(key);
		return MessageFormat.format(temp, new String[] { param });
	}

	/**
	 * Returns a localized message.
	 * 
	 * @param key
	 *            The key.
	 * @param param1
	 *            A parameter for the localized message.
	 * @param param2
	 *            A parameter for the localized message.
	 * @return The localized message.
	 * @see #getString(String)
	 * @see #getString(String, String)
	 */
	public String getString(String key, String param1, String param2) {
		String temp = msg.getString(key);
		return MessageFormat.format(temp, new String[] { param1, param2 });
	}

	public void install(AbstractPluggableGUIApplication app) {

		Savu rtext = (Savu) app;
		RTextMenuBar mb = (RTextMenuBar) app.getJMenuBar();

		// Add an item to the "View" menu to toggle debug visibility
		final JMenu menu = mb.getMenuByName(RTextMenuBar.MENU_DOCKED_WINDOWS);
		Action a = rtext.getAction(VIEW_DEBUG_ACTION);
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(a);
		item.setToolTipText(null);
		item.applyComponentOrientation(app.getComponentOrientation());
		/*menu.add(item);
		JPopupMenu popup = menu.getPopupMenu();
		popup.pack();
		// Only needed for pre-1.6 support
		popup.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				item.setSelected(isDebugWindowVisible());
			}
		}); */

	}

	/**
	 * Returns whether the debug window is visible.
	 * 
	 * @return Whether the debug window is visible.
	 * @see #setDebugWindowVisible(boolean)
	 */
	boolean isDebugWindowVisible() {
		return window != null && window.isActive();
	}

	/**
	 * Loads saved preferences. If this is the first time through, default
	 * values will be returned.
	 * 
	 * @return The preferences.
	 */
	private DebugPrefs loadPrefs() {
		DebugPrefs prefs = new DebugPrefs();
		File prefsFile = getPrefsFile();
		if (prefsFile.isFile()) {
			try {
				prefs.load(prefsFile);
			} catch (IOException ioe) {
				app.displayException(ioe);
				// (Some) defaults will be used
			}
		}
		return prefs;
	}

	/**
	 * {@inheritDoc}
	 */
	public void savePreferences() {

		DebugPrefs prefs = new DebugPrefs();
		prefs.windowPosition = window.getPosition();
		StandardAction a = (StandardAction) app.getAction(VIEW_DEBUG_ACTION);
		prefs.windowVisibilityAccelerator = a.getAccelerator();
		prefs.windowVisible = window.isActive();
		
		File prefsFile = getPrefsFile();
		try {
			prefs.save(prefsFile);
		} catch (IOException ioe) {
			app.displayException(ioe);
		}

	}

	/**
	 * Sets the visibility of the debug window.
	 * 
	 * @param visible
	 *            Whether the window should be visible.
	 * @see #isDebugWindowVisible()
	 */
	void setDebugWindowVisible(boolean visible) {
		if (visible != isDebugWindowVisible()) {
			if (visible && window == null) {
				window = new DebugWindow(app, this);
				app.addDockableWindow(window);
			}
			window.setActive(visible);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean uninstall() {
		return true;
	}
	
	/**
	 * Clears the currently highlighted line, if any, and highlights the given line.
	 * Will remove the breakpoint icon for the line, if any.
	 * Will open the file containing the line to be highlighted, if not open, and scroll to the location.
	 * @param line The line to highlight
	 * @param filePath The file in which to highlight the line
	 * @throws BadLocationException
	 */
	public void highlightLine(Integer line, String filePath) throws BadLocationException
	{		
		final String path = filePath;
		final Integer lineNo = line;
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				AbstractMainView view = app.getMainView();
				//If a line is currently highlighted, we need to switch to its file to remove it before adding the new highlight
				if(highlightedFilePath != null)
				{
					int index = view.getFileIndex(highlightedFilePath);
					//We don't care if the file isn't open
					if (index != -1) {
						view.setSelectedIndex(index);
					}
					removeHighlight();
				}
				
				int index = view.getFileIndex(path);
				File f = new File(path);
				if (index == -1) {
					
					if (!f.exists() || !f.isFile())
					{
						return;
					}
					view.openFile(path, null);
				} else {
					view.setSelectedIndex(index);
				}
				try {
					highlightedLine = getRText().getMainView().getCurrentTextArea() .addLineHighlight(lineNo-1, DEBUG_HIGHLIGHT_COLOR);
					highlightedFilePath = path;
				} catch (BadLocationException e) {
					//Shouldn't happen
				}
			}});
	}
	
	/**
	 * Clears the currently highlighted line, if any
	 */
	public void removeHighlight()
	{
		if (highlightedLine != null)
		{
			getRText().getMainView().getCurrentTextArea().removeLineHighlight(highlightedLine);
		}
	}

}