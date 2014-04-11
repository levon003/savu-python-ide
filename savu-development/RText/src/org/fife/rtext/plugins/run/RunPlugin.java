/*
 * 12/17/2010
 *
 * Plugin.java - Embeds a run-line window in RText.
 * Copyright (C) 2010 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.run;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Observable;
import java.util.Observer;
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

import org.fife.rtext.Savu;
import org.fife.rtext.RTextEditorPane;
import org.fife.rtext.RTextMenuBar;
import org.fife.rtext.RTextUtilities;
import org.fife.rtext.actions.DebugAction;
import org.fife.rtext.plugins.debug.CodePosition;
import org.fife.rtext.plugins.debug.DebugPlugin;
import org.fife.rtext.plugins.debug.PythonDebugger;
import org.fife.ui.app.AbstractPluggableGUIApplication;
import org.fife.ui.app.GUIPlugin;
import org.fife.ui.app.Plugin;
import org.fife.ui.app.PluginOptionsDialogPanel;
import org.fife.ui.app.StandardAction;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.rtext.ToolBar; 

/**
 * A plugin that allows the user to have a command prompt embedded in RText.
 * 
 * @author PyDE Comps Group
 * @version 1.0
 */
public class RunPlugin extends GUIPlugin implements Observer {

	private static final String VERSION = "1.0";
	private static final String DOCKABLE_WINDOW_RUN = "runDockableWindow";

	private Savu app;
	private RunWindow window;
	private Icon icon;

	private static final String MSG = "org.fife.rtext.plugins.run.Plugin";
	protected static final ResourceBundle msg = ResourceBundle.getBundle(MSG);

	private static final String VIEW_RUN_ACTION = "viewRunAction";

	private PythonExecutor executor;
	private PythonProcess curProcess;
	
	private ErrorLinkController errorLinker;
	private boolean clearOutput;
	
	//private Icon highlightIcon;
	//private GutterIconInfo lastHighlight;
	
	/**
	 * Constructor.
	 * 
	 * @param app
	 *            The parent application.
	 */
	public RunPlugin(AbstractPluggableGUIApplication app) {
		
		this.app = (Savu) app;

		// Load the plugin icon.
		URL url = getClass().getResource("run.gif");
		if (url != null) { // Should always be true
			try {
				icon = new ImageIcon(ImageIO.read(url));
			} catch (IOException ioe) {
				app.displayException(ioe);
			}
		}
		
		RunPrefs prefs = loadPrefs();
		
		StandardAction a = new ViewRunAction(this.app, msg, this);
		a.setAccelerator(prefs.windowVisibilityAccelerator);
		app.addAction(VIEW_RUN_ACTION, a);

		//Prepare the error link controller before creating the RunTextArea
		errorLinker = new ErrorLinkController(this);
		
		// Window MUST always be created for preference saving on shutdown
		window = new RunWindow(this.app, this);
		window.setPosition(prefs.windowPosition);
		window.setActive(prefs.windowVisible);
		putDockableWindow(DOCKABLE_WINDOW_RUN, window);

		window.setForeground(RunTextArea.STYLE_EXCEPTION, prefs.exceptionFG);
		window.setForeground(RunTextArea.STYLE_PROMPT, prefs.promptFG);
		window.setForeground(RunTextArea.STYLE_STDERR, prefs.stderrFG);
		window.setForeground(RunTextArea.STYLE_STDOUT, prefs.stdoutFG);
		clearOutput = prefs.clearOutputBeforeRun;
		
		executor = new PythonExecutor(window.getTextArea(), window.getTextArea().STYLE_STDOUT, window.getTextArea().STYLE_STDERR);
		curProcess = null; //Initialize the current process to null
		

	}

	/**
	 * Runs the python file at the specified path.
	 */
	public void runPythonFile(String fullPath, String args) {
		setRunWindowVisible(true);
		window.focused();
		stopCurrentProcess();
		PythonProcess p = executor.run(fullPath, args);
		if (p != null) {
			setCurrentProcess(p);
			clearTextAreaForStart();
			window.setCurrentProcessLabel(fullPath); //Update the label displaying the path of the executing file.
			Thread thread = new Thread(curProcess);
			thread.start();	
		} else {
			window.setCurrentProcessLabelToError("ERROR: Could not execute Python command; check that Python is installed and accessible and try again.");
		}
	}
	
	public void runPythonFile(String fullPath)
	{
		runPythonFile(fullPath, "");
	}

	/**
	 *  Performs the necessary steps to set up a debugging session as the current thing that's being run
	 *  Sets the current process to the debug process, and links up the stop button
	 */
	public void handleDebugging(InputStream stdout, InputStream stderr, PythonProcess debugProcess, String filePath) {
		stopCurrentProcess();
		//If the stop button isn't already enabled, we need to so that we can stop the debugger
		new DoubleOutputStreamSiphon(stdout, stderr, window.getTextArea(), window.getTextArea().STYLE_STDOUT, window.getTextArea().STYLE_STDERR, debugProcess).start();
		setCurrentProcess(debugProcess);
		window.setCurrentProcessLabel(filePath); //Update the label to the filepath of the file being debugged
	}
	
	/**
	 * Sets the current process and sets this as an observer of it
	 */
	private void setCurrentProcess(PythonProcess p)
	{
		if (curProcess != null)
		{
			curProcess.deleteObserver(this);
		}
		curProcess = p;
		curProcess.addObserver(this);
	}
	
	/**
	 * Clears all text in the output area if the plugins specify to do so before each run
	 */
	public void clearTextAreaForStart()
	{
		if (clearOutput)
		{
			//Clear the text area - must be run on EDT
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					window.getTextArea().clear();
				}
			});
		}
	}
	
	public boolean getClearOutput() {
		return clearOutput;
	}
	
	/**
	 * Returns the dockable window containing the runs.
	 * 
	 * @return The dockable window.
	 */
	public RunWindow getDockableWindow() {
		return window;
	}

	public ErrorLinkController getErrorLinkController() {
		return errorLinker;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public PluginOptionsDialogPanel getOptionsDialogPanel() {
		return new RunOptionPanel(this);
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
				"run.properties");
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

		// Add an item to the "View" menu to toggle run visibility
		final JMenu menu = mb.getMenuByName(RTextMenuBar.MENU_DOCKED_WINDOWS);
		Action a = rtext.getAction(VIEW_RUN_ACTION);
		final JCheckBoxMenuItem item = new JCheckBoxMenuItem(a);
		item.setToolTipText(null);
		item.applyComponentOrientation(app.getComponentOrientation());
		//menu.add(item);
		//JPopupMenu popup = menu.getPopupMenu();
		//popup.pack();
		// Only needed for pre-1.6 support
		/*popup.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuCanceled(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				item.setSelected(isRunWindowVisible());
			}
		});*/

		window.clearRuns(); // Needed to pick up styles

	}

	/**
	 * Returns whether the run window is visible.
	 * 
	 * @return Whether the run window is visible.
	 * @see #setRunWindowVisible(boolean)
	 */
	boolean isRunWindowVisible() {
		return window != null && window.isActive();
	}

	/**
	 * Loads saved preferences. If this is the first time through, default
	 * values will be returned.
	 * 
	 * @return The preferences.
	 */
	private RunPrefs loadPrefs() {
		RunPrefs prefs = new RunPrefs();
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

		RunPrefs prefs = new RunPrefs();
		prefs.windowPosition = window.getPosition();
		StandardAction a = (StandardAction) app.getAction(VIEW_RUN_ACTION);
		prefs.windowVisibilityAccelerator = a.getAccelerator();
		prefs.windowVisible = window.isActive();
		prefs.clearOutputBeforeRun = clearOutput;

		File prefsFile = getPrefsFile();
		try {
			prefs.save(prefsFile);
		} catch (IOException ioe) {
			app.displayException(ioe);
		}

	}

	/**
	 * Sets whether the output should be cleared between runs.
	 */
	protected void setClearOutput(boolean b) {
		clearOutput = b;
	}
	
	/**
	 * Sets the visibility of the run window.
	 * 
	 * @param visible
	 *            Whether the window should be visible.
	 * @see #isRunWindowVisible()
	 */
	void setRunWindowVisible(boolean visible) {
		if (visible != isRunWindowVisible()) {
			if (visible && window == null) {
				window = new RunWindow(app, this);
				app.addDockableWindow(window);
			}
			window.setActive(visible);
		}
	}
	
	/**
	 * Stops the currently running process, if any.
	 */
	public void stopCurrentProcess() {
		if (curProcess != null)
		{
			curProcess.stop();
		}
		DebugPlugin debug = DebugAction.getDebugPlugin(app);
		if (debug != null)
		{
			debug.removeHighlight();
		}
	}

	/**
	 * Gets the currently running process, if any
	 * @return The currently running process
	 */
	public PythonProcess getCurrentProcess() {
		return curProcess;
	}
	
	public void update(Observable observable, Object arg) //Indicates a state change in the current process
	{
		if (this.getCurrentProcess().isRunning()) {
			app.setStoppingEnabled(true);
		} else {
			app.setStoppingEnabled(false);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean uninstall() {
		return true;
	}

}