/*
 * Savu.java - A Python Beginner's IDE
 * Heavily edited version of:
 * RText.java
 * Copyright (C) 2003 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.text.Element;

import org.fife.help.HelpDialog;
import org.fife.jgoodies.looks.common.ShadowPopupFactory;
import org.fife.rsta.ui.CollapsibleSectionPanel;
import org.fife.rtext.actions.ActionFactory;
import org.fife.rtext.actions.DebugAction;
import org.fife.rtext.plugins.debug.Breakpoint;
import org.fife.rtext.plugins.debug.DebugPlugin;
import org.fife.rtext.plugins.run.RunPlugin;
import org.fife.rtext.tutorial.Tutorial;
import org.fife.ui.CustomizableToolBar;
import org.fife.ui.OptionsDialog;
import org.fife.ui.SplashScreen;
import org.fife.ui.SubstanceUtils;
import org.fife.ui.UIUtil;
import org.fife.ui.WebLookAndFeelUtils;
import org.fife.ui.app.AbstractGUIApplication;
import org.fife.ui.app.AbstractPluggableGUIApplication;
import org.fife.ui.app.ExceptionDialog;
import org.fife.ui.app.GUIApplicationPreferences;
import org.fife.ui.app.GUIPlugin;
import org.fife.ui.app.Plugin;
import org.fife.ui.app.StatusBarPlugin;
import org.fife.ui.app.ThirdPartyLookAndFeelManager;
import org.fife.ui.dockablewindows.DockableWindow;
import org.fife.ui.dockablewindows.DockableWindowConstants;
import org.fife.ui.dockablewindows.DockableWindowGroup;
import org.fife.ui.dockablewindows.DockableWindowGroup.DockedTabbedPane;
import org.fife.ui.dockablewindows.DockableWindowPanel;
import org.fife.ui.dockablewindows.DockableWindowPanel.ContentPanel;
import org.fife.ui.rsyntaxtextarea.CodeTemplateManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.SavuDefaultTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.IconGroup;
import org.fife.ui.rtextfilechooser.FileChooserOwner;
import org.fife.ui.rtextfilechooser.RTextFileChooser;
import org.fife.util.TranslucencyUtil;


/**
 * An instance of the RText text editor.  <code>RText</code> is a programmer's
 * text editor with the following features:
 *
 * <ul>
 *   <li>Syntax highlighting for 30+ languages.
 *   <li>Code folding.
 *   <li>Edit multiple documents simultaneously.
 *   <li>Auto-indent.
 *   <li>Find/Replace/Find in Files, with regular expression functionality.
 *   <li>Printing and Print Preview.
 *   <li>Online help.
 *   <li>Intelligent source browsing via Exuberant Ctags.
 *   <li>Macros.
 *   <li>Code templates.
 *   <li>Many other features.
 * </ul>
 *
 * At the heart of this program is
 * {@link org.fife.ui.rsyntaxtextarea.RSyntaxTextArea}, a fully-featured,
 * syntax highlighting text component.  That's where most of the meat is.
 * All text areas are contained in a subclass of
 * {@link org.fife.rtext.AbstractMainView}, which keeps the state of all of the
 * text areas in synch (fonts used, colors, etc.).  This class (RText) contains
 * an instance of a subclass of {@link org.fife.rtext.AbstractMainView} (which
 * contains all of the text areas) as well as the menu, source browser, and
 * status bar.
 *
 * @author Robert Futrell
 * @version 2.0.7
 */
public class Savu extends AbstractPluggableGUIApplication
			implements ActionListener, CaretListener, PropertyChangeListener,
						RTextActionInfo, FileChooserOwner {

	// Constants specifying the current view style.
	public static final int TABBED_VIEW				= 0;
	public static final int SPLIT_PANE_VIEW				= 1;
	public static final int MDI_VIEW					= 2;
	public static final Color OUTPUT_AREA_BACKGROUND_COLOR = new Color(50, 50, 40);
	public static final Color BORDER_AREA_BACKGROUND_COLOR = new Color(25, 25, 15);
	public static final Color ACCENT_BACKGROUND_COLOR = new Color(70, 70, 60);
	public static final Color MAIN_BACKGROUND_COLOR = new Color(30, 30, 20);
	// Properties fired.
	public static final String ICON_STYLE_PROPERTY		= "RText.iconStyle";
	public static final String MAIN_VIEW_STYLE_PROPERTY	= "RText.mainViewStyle";

	private Map iconGroupMap;

	private RTextMenuBar menuBar;

	public OptionsDialog optionsDialog;

	private CollapsibleSectionPanel csp; // Contains the AbstractMainView
	private AbstractMainView mainView;	// Component showing all open documents.
	private int mainViewStyle;

	private RTextFileChooser chooser;
	private RemoteFileChooser rfc;

	private HelpDialog helpDialog;

	private SyntaxScheme colorScheme;

	private IconGroup iconGroup;

	private String workingDirectory;	// The directory for new empty files.

	private String newFileName;		// The name for new empty text files.

	private boolean showHostName;
	
	private ToolBar toolbarRestricted;

	//Added by PyDE
	/**
	 * Stores Breakpoints set by the user, keyed by the rtexteditorpane for that file.
	 */
	private HashMap<RTextEditorPane, ArrayList<Breakpoint>> breakpoints;
	
	/**
	 * This boolean is set to True while RTextEditorPane edits should be blocked, and False at all other times.
	 * 
	 * It is expected that relevant objects will respect its value and behave accordingly; set to volatile to function
	 * as a fast and simple concurrency protection.
	 */
	private volatile boolean globalEditingLocked = false;
	
	private boolean stopActionsEnabled = false;
	
	/**
	 * Whether <code>searchWindowOpacityListener</code> has been attempted to be
	 * created yet. This is kept in a variable instead of checking for
	 * <code>null</code> because the creation is done via reflection (since
	 * we're 1.4-compatible), so it is a fairly common case that creation is
	 * attempted but fails.
	 */
	private boolean windowListenersInited;

	/**
	 * Listens for focus events of certain child windows (those that can
	 * be made translucent on focus lost).
	 */
	private ChildWindowListener searchWindowOpacityListener;

	/**
	 * Whether the Find and Replace dialogs can have their opacity changed.
	 */
	private boolean searchWindowOpacityEnabled;

	/**
	 * The opacity with which to render unfocused child windows that support
	 * opacity changes.
	 */
	private float searchWindowOpacity;

	/**
	 * The rule used for making certain unfocused child windows translucent.
	 */
	private int searchWindowOpacityRule;

	/**
	 * The (lazily created) name of localhost.  Do not access this field
	 * directly; instead, use {@link #getHostName()}.
	 */
	private String hostName;

	/**
	 * Used as a "hack" to re-load the Options dialog if the user opens it
	 * too early, before all plugins have added their options to it.
	 */
	private int lastPluginCount;
	
	private boolean tutorialModeEnabled = true;
	
	private Tutorial theTutorial = null;

	/**
	 * System property that, if set, causes RText to print timing information
	 * while it is starting up.
	 */
	public static final String PROPERTY_PRINT_START_TIMES = "printStartTimes";

	public static final String VERSION_STRING		= "2.0.7.20130428";
	public Icon stopIcon; //TODO: get this in icon group

	/**
	 * Creates an instance of the <code>RText</code> editor.
	 *
	 * @param filesToOpen Array of <code>java.lang.String</code>s containing
	 *        the files we want to open initially.  This can be
	 *        <code>null</code> if no files are to be opened.
	 */
	public Savu(String[] filesToOpen) {
		super("Savu", "RText.jar");
		TokenMakerFactory.setDefaultInstance(new SavuDefaultTokenMakerFactory());
		init(filesToOpen);
	}


	/**
	 * Creates an instance of the <code>RText</code> editor.
	 *
	 * @param filesToOpen Array of <code>java.lang.String</code>s containing
	 *        the files we want to open initially.  This can be
	 *        <code>null</code> if no files are to be opened.
	 * @param preferences The preferences with which to initialize this RText.
	 */
	public Savu(String[] filesToOpen, RTextPreferences preferences) {
		super("Savu", "RText.jar", preferences);
		init(filesToOpen);
	}


	// What to do when user does something.
	public void actionPerformed(ActionEvent e) {

		String command = e.getActionCommand();

		if (command.equals("TileVertically")) {
			((RTextMDIView)mainView).tileWindowsVertically();
		}

		else if (command.equals("TileHorizontally")) {
			((RTextMDIView)mainView).tileWindowsHorizontally();
		}

		else if (command.equals("Cascade")) {
			((RTextMDIView)mainView).cascadeWindows();
		}

	}


	// TODO
	public void addDockableWindow(DockableWindow wind) {
		((DockableWindowPanel)mainContentPanel).addDockableWindow(wind);
	}


	/**
	 * Returns whether or not tabs are emulated with spaces (i.e. "soft" tabs).
	 * This simply calls <code>mainView.areTabsEmulated</code>.
	 *
	 * @return <code>true</code> if tabs are emulated with spaces;
	 *         <code>false</code> if they aren't.
	 */
	public boolean areTabsEmulated() {
		return mainView.areTabsEmulated();
	}


	/**
	 * Called when cursor in text editor changes position.
	 *
	 * @param e The caret event.
	 */
	public void caretUpdate(CaretEvent e) {

		// NOTE: e may be "null"; we do this sometimes to force caret
		// updates to update e.g. the current line highlight.
		RTextEditorPane textArea = mainView.getCurrentTextArea();
		int dot = textArea.getCaretPosition();//e.getDot();

		// Update row/column information in status field.
		Element map = textArea.getDocument().getDefaultRootElement();
		int line = map.getElementIndex(dot);
		int lineStartOffset = map.getElement(line).getStartOffset();
		((StatusBar)getStatusBar()).setRowAndColumn(
									line+1, dot-lineStartOffset+1);

	}


	/**
	 * Converts all instances of a number of spaces equal to a tab in all open
	 * documents into tabs.
	 *
	 * @see #convertOpenFilesTabsToSpaces
	 */
	public void convertOpenFilesSpacesToTabs() {
		mainView.convertOpenFilesSpacesToTabs();
	}


	/**
	 * Converts all tabs in all open documents into an equivalent number of
	 * spaces.
	 *
	 * @see #convertOpenFilesSpacesToTabs
	 */
	public void convertOpenFilesTabsToSpaces() {
		mainView.convertOpenFilesTabsToSpaces();
	}


	/**
	 * Returns the About dialog for this application.
	 *
	 * @return The About dialog.
	 */
	protected JDialog createAboutDialog() {
		return new AboutDialog(this);
	}


	/**
	 * Creates the array of actions used by this RText.
	 *
	 * @param prefs The RText properties for this RText instance.
	 */
	protected void createActions(GUIApplicationPreferences prefs) {
		ActionFactory.addActions(this, (RTextPreferences)prefs);
	}


	/**
	 * Creates and returns the menu bar used in this application.
	 *
	 * @param prefs This GUI application's preferences.
	 * @return The menu bar.
	 */
	protected JMenuBar createMenuBar(GUIApplicationPreferences prefs) {

		RTextPreferences properties = (RTextPreferences)prefs;

		//splashScreen.updateStatus(msg.getString("CreatingMenuBar"), 75);

		// Create the menu bar.
		menuBar = new RTextMenuBar(this, UIManager.getLookAndFeel().getName(),
									properties);
		mainView.addPropertyChangeListener(menuBar);

		menuBar.setWindowMenuVisible(properties.mainView==MDI_VIEW);

		return menuBar;

	}


	/**
	 * Returns the splash screen to display while this GUI application is
	 * loading.
	 *
	 * @return The splash screen.  If <code>null</code> is returned, no
	 *         splash screen is displayed.
	 */
	protected SplashScreen createSplashScreen() {
		String img = "org/fife/rtext/graphics/" + getString("Splash");
		return new SplashScreen(img, getString("Initializing"));
	}


	/**
	 * Returns the status bar to be used by this application.
	 *
	 * @param prefs This GUI application's preferences.
	 * @return The status bar.
	 */
	protected org.fife.ui.StatusBar createStatusBar(
							GUIApplicationPreferences prefs) {
		RTextPreferences properties = (RTextPreferences)prefs;
		StatusBar sb = new StatusBar(this, getString("Ready"),
					!properties.wordWrap, 1,1,
					properties.textMode==RTextEditorPane.OVERWRITE_MODE);
		sb.setStyle(properties.statusBarStyle);
		return sb;
	}


	/**
	 * Creates and returns the toolbar to be used by this application.
	 *
	 * @param prefs This GUI application's preferences.
	 * @return The toolbar.
	 */
	protected CustomizableToolBar createToolBar(
						GUIApplicationPreferences prefs) {

		ToolBar toolBar = new ToolBar("Savu - Toolbar", this,
								(StatusBar)getStatusBar());
		toolBar.setSize(toolBar); 

		// Make the toolbar use the large versions of the icons if available.
		// FIXME:  Make toggle-able.
		toolBar.checkForLargeIcons();
		
		this.toolbarRestricted = toolBar;

		return toolBar;

	}
	
	/**
	 * Returns the current arguments listed in the toolbar's box and updates the argument history.
	 * @param recent
	 */
	public String getAndSaveCurrentArguments() {
		String currentArgs = this.toolbarRestricted.getCurrentArgs();
		this.toolbarRestricted.updateArgumentHistory(currentArgs);
		return currentArgs;
	}
	
	/**
	 * Returns the dictionary containing the Breakpoint objects for each open file in RText.
	 * 
	 * Should only be called from the EDT.
	 * 
	 * @return The HashMap of <RTextEditorPane file, ArrayList<Breakpoint> breakpointsInFile>.
	 */
	public HashMap<RTextEditorPane, ArrayList<Breakpoint>> getBreakpointsDictionary() {
		return breakpoints;
	}
	
	/**
	 * Returns a list of the current Breakpoints, across all currently open files.  Simultaneously detects and removes invalid breakpoints.
	 * 
	 * Should only be called from the EDT.
	 * 
	 */
	public ArrayList<Breakpoint> getBreakpoints() {
		ArrayList<Breakpoint> toReturn = new ArrayList<Breakpoint>();
		ArrayList<Breakpoint> toDel = new ArrayList<Breakpoint>();
		for (ArrayList<Breakpoint> breaks : breakpoints.values()) {
			for (Breakpoint b : breaks) {
				if (b.isLineValid()) {
					toReturn.add(b);
				} else { //This is a valid breakpoint
					toDel.add(b);
				}
			}
			for (Breakpoint b : toDel) {
				//We found invalid breakpoints to remove; remove them visually and in the data model
				b.getGutter().removeTrackingIcon(b.getGutterIconInfo());
				breaks.remove(b);
			}
			toDel.clear();
		}
		return toReturn;
	}
	
	/**
	 * Returns a list of the current Breakpoints in a single specified file.
	 * 
	 * Should only be called on the EDT.
	 * 
	 * @param pane The RTextEditorPane to get breakpoints from.
	 * @return A list of Breakpoints registered in this file.
	 */
	public ArrayList<Breakpoint> getBreakpointsInFile(RTextEditorPane pane) {
		if (!breakpoints.containsKey(pane)) {
			return null;
		} else {
			return breakpoints.get(pane);
		}
	}
	
	/**
	 * Returns the current arguments in the toolbar without updating the argument history.
	 */
	public String getCurrentArguments() {
		return this.toolbarRestricted.getCurrentArgs();
	}

	/**
	 * Clears breakpoints in the specified file.  Does nothing if no breakpoints exist for the specified file.
	 */
	public void clearBreakpointsInEditorPane(RTextEditorPane pane) {
		ArrayList<Breakpoint> breaks = breakpoints.get(pane);
		if (breaks == null || breaks.size() == 0) {
			return;
		}
		Gutter g = null;
		try { //Try to get the gutter
			g = RSyntaxUtilities.getGutter(pane);
		} catch (Exception ex) {
			//if we can't get the gutter, there are no visible breakpoints in this file (because it isn't open).
			breaks.clear();
			return;
		}
		for (Breakpoint b : breaks) {
			g.removeTrackingIcon(b.getGutterIconInfo()); //Remove the visual icon for each of the breakpoints
		}
		breaks.clear(); //Actually delete the references to the breakpoints
	}
	

	/**
	 * Overridden so we can syntax highlight the Java exception displayed.
	 *
	 * @param owner The dialog that threw the Exception.
	 * @param t The exception/throwable that occurred.
	 * @param desc A short description of the error.  This can be
	 *        <code>null</code>.
	 */
	public void displayException(Dialog owner, Throwable t, String desc) {
		ExceptionDialog ed = new ExceptionDialog(owner, t);
		if (desc!=null) {
			ed.setDescription(desc);
		}
		ed.setLocationRelativeTo(owner);
		ed.setTitle(getString("ErrorDialogTitle"));
		ed.setVisible(true);
	}


	/**
	 * Overridden so we can syntax highlight the Java exception displayed.
	 *
	 * @param owner The child frame that threw the Exception.
	 * @param t The exception/throwable that occurred.
	 * @param desc A short description of the error.  This can be
	 *        <code>null</code>.
	 */
	public void displayException(Frame owner, Throwable t, String desc) {
		ExceptionDialog ed = new ExceptionDialog(owner, t);
		if (desc!=null) {
			ed.setDescription(desc);
		}
		ed.setLocationRelativeTo(owner);
		ed.setTitle(getString("ErrorDialogTitle"));
		ed.setVisible(true);
	}


	/**
	 * Called when the user attempts to close the application, whether from
	 * an "Exit" menu item, closing the main application window, or any other
	 * means.  The user is prompted to save any dirty documents, and this
	 * RText instance is closed.
	 */
	public void doExit() {

		// Attempt to close all open documents.
		boolean allDocumentsClosed = getMainView().closeAllDocuments();

		// Assuming all documents closed okay (ie, the user
		// didn't click "Cancel")...
		if (allDocumentsClosed==true) {

			// If there will be no more rtext's running, stop the JVM.
			if (StoreKeeper.getInstanceCount()==1) {
				//clean up the tutorial if it's running
				if (theTutorial != null){
					theTutorial.cleanUp();
				}
					
				saveRTextPreferences();	// Save the user's running preferences.
				boolean saved = RTextEditorPane.saveTemplates();
				if (!saved) {
					String title = getString("ErrorDialogTitle");
					String text = getString("TemplateSaveError");
					JOptionPane.showMessageDialog(this, text, title,
										JOptionPane.ERROR_MESSAGE);
				}
				// Save file chooser "Favorite Directories".  It is
				// important to check that the chooser exists here, as
				// if it doesn't, there's no need to do this!  If we
				// don't, the saveFileChooseFavorites() method will
				// create the file chooser itself just to save the
				// favorites!
				if (chooser!=null) {
					RTextUtilities.saveFileChooserFavorites(this);
				}
				AWTExceptionHandler.shutdown();
				System.exit(0);
			}

			// If there will still be some RText instances running, just
			// stop this instance.
			else {
				setVisible(false);
				StoreKeeper.removeRTextInstance(this);
				this.dispose();
			}

		}

	}

	/**
	 * Sets GUI components that would enable stop components as enabled or not.
	 * @param stopActionEnabled
	 */
	public void setStoppingEnabled(final boolean stopActionEnabled) {
		if (stopActionEnabled != stopActionsEnabled) { //An actual change
			stopActionsEnabled = stopActionEnabled;
			if (SwingUtilities.isEventDispatchThread()) {
				setStoppingEnabledEDTHelper(stopActionEnabled);
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						setStoppingEnabledEDTHelper(stopActionEnabled);
					}
				});
			}
		}
	}
	
	/**
	 * Simple helper function of setStoppingEnabled() to reduce code repetition; assumes running on the EDT.
	 * @param stopActionEnabled
	 */
	private void setStoppingEnabledEDTHelper(boolean stopActionEnabled) {
		//Enable or disable the button in the toolbar
		((ToolBar) this.getToolBar()).enableStopButton(stopActionEnabled);
		//Enable or disable the button in the debug plugin
		DebugAction.getDebugPlugin(this).setDebugStopButtonEnabled(stopActionEnabled);
		//Enable or disable the menu item
		menuBar.setStopMenuItemEnabled(stopActionEnabled);
	}
	
	/**
	 * Focuses the specified dockable window group.  Does nothing if there
	 * are no dockable windows at the location specified.
	 *
	 * @param group The dockable window group to focus.
	 */
	public void focusDockableWindowGroup(int group) {
		DockableWindowPanel dwp = (DockableWindowPanel)mainContentPanel;
		if (!dwp.focusDockableWindowGroup(group)) { // Should never happen
			UIManager.getLookAndFeel().provideErrorFeedback(this);
		}
	}


	/**
	 * Returns the filename used for newly created, empty text files.  This
	 * value is locale-specific.
	 *
	 * @return The new text file name.
	 */
	public String getNewFileName() {
		return newFileName;
	}


	/**
	 * {@inheritDoc}
	 */
	public OptionsDialog getOptionsDialog() {

		int pluginCount = getPlugins().length;

		// Check plugin count and re-create dialog if it has changed.  This
		// is because the user can be quick and open the Options dialog before
		// all plugins have loaded.  A real solution is to have some sort of
		// options manager that plugins can add options panels to.
		if (optionsDialog==null || pluginCount!=lastPluginCount) {
			optionsDialog = new org.fife.rtext.optionsdialog.
												OptionsDialog(this);
			optionsDialog.setLocationRelativeTo(this);
		}

		return optionsDialog;

	}
	
	/**
	 * Returns the application's "collapsible section panel;" that is, the
	 * panel containing the main view and possible find/replace tool bars.
	 *
	 * @return The collapsible section panel.
	 * @see #getMainView()
	 */
	public CollapsibleSectionPanel getCollapsibleSectionPanel() {
		return csp;
	}


	/**
	 * Returns the file chooser being used by this RText instance.
	 *
	 * @return The file chooser.
	 * @see #getRemoteFileChooser()
	 */
	public RTextFileChooser getFileChooser() {
		if (chooser==null) {
			chooser = RTextUtilities.createFileChooser(this);
		}
		return chooser;
	}


	/**
	 * Returns the focused dockable window group.
	 *
	 * @return The focused window group, or <code>-1</code> if no dockable
	 *         window group is focused.
	 * @see DockableWindowConstants
	 */
	public int getFocusedDockableWindowGroup() {
		DockableWindowPanel dwp = (DockableWindowPanel)mainContentPanel;
		return dwp.getFocusedDockableWindowGroup();
	}


	/**
	 * Returns the Help dialog for RText.
	 *
	 * @return The Help dialog.
	 * @see org.fife.ui.app.GUIApplication#getHelpDialog
	 */
	public HelpDialog getHelpDialog() {
		// Create the help dialog if it hasn't already been.
		if (helpDialog==null) {
			String contentsPath = getInstallLocation() + "/doc/";
			String helpPath = contentsPath + getLanguage() + "/";
			// If localized help does not exist, default to English.
			File test = new File(helpPath);
			if (!test.isDirectory())
				helpPath = contentsPath + "en/";
			helpDialog = new HelpDialog(this,
						contentsPath + "HelpDialogContents.xml",
						helpPath);
			helpDialog.setBackButtonIcon(iconGroup.getIcon("back"));
			helpDialog.setForwardButtonIcon(iconGroup.getIcon("forward"));
		}
		helpDialog.setLocationRelativeTo(this);
		return helpDialog;
	}


	/**
	 * Returns the name of the local host.  This is lazily discovered.
	 *
	 * @return The name of the local host.
	 */
	public synchronized String getHostName() {
		if (hostName==null) {
			try {
				hostName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException uhe) { // Should never happen
				hostName = "Unknown";
			}
		}
		return hostName;
	}


	/**
	 * Returns the icon group being used for icons for actions.
	 *
	 * @return The icon group.
	 */
	public IconGroup getIconGroup() {
		return iconGroup;
	}


	/**
	 * Returns the icon groups available to RText.
	 *
	 * @return The icon groups.
	 */
	public Map getIconGroupMap() {
		return iconGroupMap;
	}


	/**
	 * Returns the actual main view.
	 *
	 * @return The main view.
	 * @see #getMainViewStyle()
	 * @see #setMainViewStyle(int)
	 */
	public AbstractMainView getMainView() {
		return mainView;
	}


	/**
	 * Returns the main view style.
	 *
	 * @return The main view style, one of {@link #TABBED_VIEW},
	 *         {@link #SPLIT_PANE_VIEW} or {@link #MDI_VIEW}.
	 * @see #setMainViewStyle(int)
	 * @see #getMainView()
	 */
	public int getMainViewStyle() {
		return mainViewStyle;
	}


	/**
	 * Returns the name of the preferences class for this application.  This
	 * class must be a subclass of <code>GUIApplicationPreferences</code>.
	 *
	 * @return The class name, or <code>null</code> if this GUI application
	 *         does not save preferences.
	 */
	protected String getPreferencesClassName() {
		return "org.fife.rtext.RTextPreferences";
	}


	/**
	 * Returns the file chooser used to select remote files.
	 *
	 * @return The file chooser.
	 * @see #getFileChooser()
	 */
	public RemoteFileChooser getRemoteFileChooser() {
		if (rfc==null) {
			rfc = new RemoteFileChooser(this);
		}
		return rfc;
	}


	/**
	 * Returns the fully-qualified class name of the resource bundle for this
	 * application.  This is used by {@link #getResourceBundle()} to locate
	 * the class.
	 *
	 * @return The fully-qualified class name of the resource bundle.
	 * @see #getResourceBundle()
	 */
	public String getResourceBundleClassName() {
		return "org.fife.rtext.RText";
	}


	/**
	 * Returns the opacity with which to render unfocused child windows, if
	 * this option is enabled.
	 *
	 * @return The opacity.
	 * @see #setSearchWindowOpacity(float)
	 */
	public float getSearchWindowOpacity() {
		return searchWindowOpacity;
	}


	/**
	 * Returns the rule used for making certain child windows translucent.
	 *
	 * @return The rule.
	 * @see #setSearchWindowOpacityRule(int)
	 * @see #getSearchWindowOpacity()
	 */
	public int getSearchWindowOpacityRule() {
		return searchWindowOpacityRule;
	}


	/**
	 * Returns whether the hostname should be shown in the title of the
	 * main RText window.
	 *
	 * @return Whether the hostname should be shown.
	 * @see #setShowHostName(boolean)
	 */
	public boolean getShowHostName() {
		return showHostName;
	}


	/**
	 * Returns the syntax highlighting color scheme being used.
	 *
	 * @return The syntax highlighting color scheme.
	 */
	public SyntaxScheme getSyntaxScheme() {
		return colorScheme;
	}


	/**
	 * Returns the tab size (in spaces) currently being used.
	 *
	 * @return The tab size (in spaces) currently being used.
	 * @see #setTabSize(int)
	 */
	public int getTabSize() {
		return mainView.getTabSize();
	}


	/**
	 * Returns the title of this window, less any "header" information
	 * (e.g. without the leading "<code>rtext - </code>").
	 *
	 * @return The title of this window.
	 * @see #setTitle(String)
	 */
	public String getTitle() {
		String title = super.getTitle();
		int hyphen = title.indexOf("- ");
		if (hyphen>-1) { // Should always be true
			title = title.substring(hyphen+2);
		}
		return title;
	}


	/**
	 * Returns the version string for this application.
	 *
	 * @return The version string.
	 */
	public String getVersionString() {
		return VERSION_STRING;
	}


	/**
	 * Returns the "working directory;" that is, the directory that new, empty
	 * files are created in.
	 *
	 * @return The working directory.  There will be no trailing '/' or '\'.
	 * @see #setWorkingDirectory
	 */
	public String getWorkingDirectory() {
		return workingDirectory;
	}


	/**
	 * Does the dirty work of actually installing a plugin.  This method
	 * ensures the current text area retains focus even after a GUI plugin
	 * is added.
	 *
	 * @param plugin The plugin to install.
	 */
	protected void handleInstallPlugin(Plugin plugin) {
		// Normally we don't have to check currentTextArea for null, but in
		// this case, we do.  Plugins are installed at startup, after the main
		// window is displayed.  If the user passes in a filename to open, but
		// that file doesn't exist, RText will prompt with "File xxx does not
		// exist, create it?", and in that time, currentTextArea will be null.
		// Plugins, in the meantime, will try to load and find the null value.
		RTextEditorPane textArea = getMainView().getCurrentTextArea();
		if (textArea!=null) {
			textArea.requestFocusInWindow();
		}
		
		if (plugin instanceof RunPlugin || plugin instanceof DebugPlugin) {
			//COLORCHANGE
			GUIPlugin gp = (GUIPlugin) plugin;
			Iterator i = gp.dockableWindowIterator();
			if (i.hasNext()) {
				DockableWindow w = (DockableWindow) gp.dockableWindowIterator().next();
				try {
					w.getParent().setBackground(Savu.MAIN_BACKGROUND_COLOR);
					//COLORCHANGE: MainContentPanel
					w.getParent().getParent().getParent().getParent().setBackground(Savu.BORDER_AREA_BACKGROUND_COLOR);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			

		} 
	}


	/**
	 * Returns whether dockable windows are at the specified location.
	 *
	 * @param group A constant from {@link DockableWindowConstants}
	 * @return Whether dockable windows are at the specified location.
	 */
	public boolean hasDockableWindowGroup(int group) {
		DockableWindowPanel dwp = (DockableWindowPanel)mainContentPanel;
		return dwp.hasDockableWindowGroup(group);
	}


	/**
	 * Called at the end of RText constructors.  Does common initialization
	 * for RText.
	 *
	 * @param filesToOpen Any files to open.  This can be <code>null</code>.
	 */
	private void init(String[] filesToOpen) {
		lastPluginCount = -1;
		breakpoints = new HashMap<RTextEditorPane, ArrayList<Breakpoint>>();
		openFiles(filesToOpen);
	}

	/**
	 * Gets the value of the <code>globalEditingLocked</code> variable.
	 * @see #globalEditingLocked
	 */
	public boolean isGlobalEditingLocked() {
		return globalEditingLocked;
	}
	
	/**
	 * Sets the value of the <code>globalEditingLocked</code> variable.
	 * @see #globalEditingLocked
	 */
	public void setGlobalEditingLocked(boolean isLocked) {
		globalEditingLocked = isLocked; //TODO update visuals here
		StatusBar sb = (StatusBar) getStatusBar();
		if (isLocked) {
			sb.setReadOnlyIndicatorEnabled(true);
		} else {
			sb.setReadOnlyIndicatorEnabled(false);
		}
	}
	

	/**
	 * Returns whether search window opacity is enabled.
	 *
	 * @return Whether search window opacity is enabled.
	 * @see #setSearchWindowOpacityEnabled(boolean)
	 */
	public boolean isSearchWindowOpacityEnabled() {
		return searchWindowOpacityEnabled;
	}


	/**
	 * Loads and validates the icon groups available to RText.
	 */
	private void loadPossibleIconGroups() {
		iconGroupMap = IconGroupLoader.loadIconGroups(this,
					getInstallLocation() + "/icongroups/ExtraIcons.xml");
	}


	/**
	 * Thanks to Java Bug ID 5026829, JMenuItems (among other Swing components)
	 * don't update their accelerators, etc. when the properties on which they
	 * were created update them.  Thus, we have to do this manually.  This is
	 * still broken as of 1.5.
	 */
	public void menuItemAcceleratorWorkaround() {
		menuBar.menuItemAcceleratorWorkaround();
	}


	/**
	 * Opens the specified files.
	 *
	 * @param filesToOpen The files to open.  This can be <code>null</code>.
	 * @see #openFile
	 */
	public void openFiles(String[] filesToOpen) {
		int count = filesToOpen==null ? 0 : filesToOpen.length;
		for (int i=0; i<count; i++) {
			openFile(filesToOpen[i]);
		}
	}


	/**
	 * This is called in the GUI application's constructor.  It is a chance
	 * to do initialization of stuff that will be needed before RText is
	 * displayed on-screen.
	 *
	 * @param prefs The preferences of the application.
	 * @param splashScreen The "splash screen" for this application.  This
	 *        value may be <code>null</code>.
	 */
	protected void preDisplayInit(GUIApplicationPreferences prefs,
								SplashScreen splashScreen) {

		long start = System.currentTimeMillis();

		// Some stuff down the line may assume this directory exists!
		File prefsDir = RTextUtilities.getPreferencesDirectory();
		if (!prefsDir.isDirectory()) {
			prefsDir.mkdirs();
		}

		// Install any plugins.
		super.preDisplayInit(prefs, splashScreen);

		RTextPreferences props = (RTextPreferences)prefs;

		splashScreen.updateStatus(getString("AddingFinalTouches"), 90);

		// If the user clicks the "X" in the top-right of the window, do nothing.
		// (We'll clean up in our window listener).
		addWindowListener( new RTextWindowListener(this) );
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		mainView.setLineNumbersEnabled(props.lineNumbersVisible);

		// Enable templates in text areas.
		if (RTextUtilities.enableTemplates(this, true)) {
			// If there are no templates, assume this is the user's first
			// time in RText and add some "standard" templates.
			CodeTemplateManager ctm = RTextEditorPane.getCodeTemplateManager();
			if (ctm.getTemplateCount()==0) {
				RTextUtilities.addDefaultCodeTemplates();
			}
		}

		setSearchWindowOpacityEnabled(props.searchWindowOpacityEnabled);
		setSearchWindowOpacity(props.searchWindowOpacity);
		setSearchWindowOpacityRule(props.searchWindowOpacityRule);

		if (Boolean.getBoolean(PROPERTY_PRINT_START_TIMES)) {
			System.err.println("preDisplayInit: " + (System.currentTimeMillis()-start));
		}

		ShadowPopupFactory.install();
		RTextUtilities.setDropShadowsEnabledInEditor(props.dropShadowsInEditor);

		//TODO check to see if the tutorial is turned on; if it is, run it.
	}


	/**
	 * This is called in the GUI application's constructor.  It is a chance
	 * to do initialization of stuff that will be needed by the menu bar
	 * before it gets created.
	 *
	 * @param prefs The preferences of the application.
	 * @param splashScreen The "splash screen" for this application.  This
	 *        value may be <code>null</code>.
	 */
	protected void preMenuBarInit(GUIApplicationPreferences prefs,
							SplashScreen splashScreen) {

		long start = System.currentTimeMillis();

		// Make the split pane positions same as last time.
		RTextPreferences rtp = (RTextPreferences)prefs;
		setSplitPaneDividerLocation(TOP, rtp.dividerLocations[TOP]);
		setSplitPaneDividerLocation(LEFT, rtp.dividerLocations[LEFT]);
		setSplitPaneDividerLocation(BOTTOM, rtp.dividerLocations[BOTTOM]);
		setSplitPaneDividerLocation(RIGHT, rtp.dividerLocations[RIGHT]);

		setShowHostName(rtp.showHostName);

		if (Boolean.getBoolean(PROPERTY_PRINT_START_TIMES)) {
			System.err.println("preMenuBarInit: " + (System.currentTimeMillis()-start));
		}

	}


	/**
	 * This is called in the GUI application's constructor.  It is a chance
	 * to do initialization of stuff that will be needed by the status bar
	 * bar before it gets created.
	 *
	 * @param prefs The preferences of the application.
	 * @param splashScreen The "splash screen" for this application.  This
	 *        value may be <code>null</code>.
	 */
	protected void preStatusBarInit(GUIApplicationPreferences prefs,
							SplashScreen splashScreen) {

		long start = System.currentTimeMillis();

		final RTextPreferences properties = (RTextPreferences)prefs;
		final String[] filesToOpen = null;

		// Initialize our "new, empty text file" name.
		newFileName = getString("NewFileName");

		splashScreen.updateStatus(getString("SettingSHColors"), 10);
		setSyntaxScheme(properties.colorScheme);

		setWorkingDirectory(properties.workingDirectory);

		splashScreen.updateStatus(getString("CreatingView"), 20);

		// Initialize our view object.
		switch (properties.mainView) {
			case TABBED_VIEW:
				mainViewStyle = TABBED_VIEW;
				mainView = new RTextTabbedPaneView(Savu.this, filesToOpen, properties);
				break;
			case SPLIT_PANE_VIEW:
				mainViewStyle = SPLIT_PANE_VIEW;
				mainView = new RTextSplitPaneView(Savu.this, filesToOpen, properties);
				break;
			default:
				mainViewStyle = MDI_VIEW;
				mainView = new RTextMDIView(Savu.this, filesToOpen, properties);
				break;
		}
		getContentPane().add(mainView);
		csp = new CollapsibleSectionPanel();
		csp.add(mainView);
		getContentPane().add(csp);
		//Modified by PyDE
		//The default behavior isn't sufficiently complex, so we reassign it here
		ActionMap actionMap = csp.getActionMap();
		actionMap.remove("onEscape");
		actionMap.put("onEscape", getMainView().getSearchManager().new HideSearchComponentAction());
		
		splashScreen.updateStatus(getString("CreatingStatusBar"), 25);

		if (Boolean.getBoolean(PROPERTY_PRINT_START_TIMES)) {
			System.err.println("preStatusBarInit: " + (System.currentTimeMillis()-start));
		}

	}


	/**
	 * This is called in the GUI application's constructor.  It is a chance
	 * for to do initialization of stuff that will be needed by the toolbar
	 * before it gets created.
	 *
	 * @param prefs The preferences of the application.
	 * @param splashScreen The "splash screen" for this application.  This
	 *        value may be <code>null</code>.
	 */
	protected void preToolBarInit(GUIApplicationPreferences prefs,
							final SplashScreen splashScreen) {

		long start = System.currentTimeMillis();

		final RTextPreferences properties = (RTextPreferences)prefs;

		StatusBar statusBar = (StatusBar)getStatusBar();
		//COLORCHANGE
		statusBar.setBackground(Savu.MAIN_BACKGROUND_COLOR);
		mainView.addPropertyChangeListener(statusBar);

		loadPossibleIconGroups();
		try {
			setIconGroupByName(properties.iconGroupName);
		} catch (InternalError ie) {
			displayException(ie);
			System.exit(0);
		}

		splashScreen.updateStatus(getString("CreatingToolBar"), 60);
		if (Boolean.getBoolean(PROPERTY_PRINT_START_TIMES)) {
			System.err.println("preToolbarInit: " + (System.currentTimeMillis()-start));
		}

	}


	/**
	 * Called whenever a property changes for a component we are registered
	 * as listening to.
	 */
	public void propertyChange(PropertyChangeEvent e) {

		String propertyName = e.getPropertyName();

		// If the file's path is changing (must be caused by the file being saved(?))...
		if (propertyName.equals(RTextEditorPane.FULL_PATH_PROPERTY)) {
			setTitle((String)e.getNewValue());
		}

		// If the file's modification status is changing...
		else if (propertyName.equals(RTextEditorPane.DIRTY_PROPERTY)) {
			String oldTitle = getTitle();
			boolean newValue = ((Boolean)e.getNewValue()).booleanValue();
			if (newValue==false) {
				setTitle(oldTitle.substring(0,oldTitle.length()-1));
			}
			else {
				setTitle(oldTitle + '*');
			}
		}

	}


	public void registerChildWindowListeners(Window w) {

		if (!windowListenersInited) {
			windowListenersInited = true;
			if (TranslucencyUtil.get().isTranslucencySupported(false)) {
				searchWindowOpacityListener = new ChildWindowListener(this);
				searchWindowOpacityListener.setTranslucencyRule(
												searchWindowOpacityRule);
			}
		}

		if (searchWindowOpacityListener!=null) {
			w.addWindowFocusListener(searchWindowOpacityListener);
			w.addComponentListener(searchWindowOpacityListener);
		}

	}

	public void removeDockableWindow(DockableWindow wind) {
		((DockableWindowPanel)mainContentPanel).removeDockableWindow(wind);
	}

	/**
	 * Makes all actions use default accelerators.
	 */
	public void restoreDefaultAccelerators() {

		int num = defaultActionAccelerators.length;
		for (int i=0; i<num; i++) {
			Action a = getAction(actionNames[i]);
			// Check for a null action because sometimes we have new actions
			// "declared" but not "defined" (e.g. OpenRemote).
			if (a!=null) {
				a.putValue(Action.ACCELERATOR_KEY,defaultActionAccelerators[i]);
			}
		}

		menuItemAcceleratorWorkaround();

	}


	/**
	 * Attempts to write this RText instance's properties to wherever the OS
	 * writes Java Preferences stuff.
	 */
	public void saveRTextPreferences() {

		// Save preferences for RText itself.
		RTextPreferences prefs = (RTextPreferences)RTextPreferences.
										generatePreferences(this);
		prefs.savePreferences(this);

		// Save preferences for any plugins.
		Plugin[] plugins = getPlugins();
		int count = plugins.length;
		for (int i=0; i<count; i++) {
			plugins[i].savePreferences();
		}

		// Save the file chooser's properties, if it has been instantiated.
		if (chooser!=null)
			chooser.savePreferences();

	}
	
	/**
	 * Getter for whether tutorial mode is enabled
	 * @return state of tutorial mode
	 */
	public boolean isTutorialModeEnabled() {
		return tutorialModeEnabled;
	}

	/**
	 * Toggles the Tutorial mode enabled boolean to the opposite state.
	 */
	public void setTutorialModeEnabled(boolean tutorialModeEnabled) {
		this.tutorialModeEnabled = tutorialModeEnabled;
	}


	/**
	 * Changes the style of icons used by <code>rtext</code>.<p>
	 *
	 * This method fires a property change of type
	 * <code>ICON_STYLE_PROPERTY</code>.
	 *
	 * @param name The name of the icon group to use.  If this name is not
	 *        recognized, a default icon set will be used.
	 */
	public void setIconGroupByName(String name) {

		IconGroup newGroup = (IconGroup)iconGroupMap.get(name);
		if (newGroup==null)
			newGroup = (IconGroup)iconGroupMap.get(
							IconGroupLoader.DEFAULT_ICON_GROUP_NAME);
		if (newGroup==null)
			throw new InternalError("No icon groups!");
		if (iconGroup!=null && iconGroup.equals(newGroup))
			return;

		Dimension size = getSize();
		IconGroup old = iconGroup;
		iconGroup = newGroup;

		Icon icon = iconGroup.getIcon("new");
		getAction(NEW_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("open");
		getAction(OPEN_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("save");
		getAction(SAVE_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("saveall");
		getAction(SAVE_ALL_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("openinnewwindow");
		getAction(OPEN_NEWWIN_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("saveas");
		getAction(SAVE_AS_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("options");
		getAction(OPTIONS_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("help");
		getAction(HELP_ACTION_KEY).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("about");
		getAction(ABOUT_ACTION_KEY).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("close");
		getAction(CLOSE_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("find");
		getAction(FIND_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("findnext");
		getAction(FIND_NEXT_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("replace");
		getAction(REPLACE_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("replacenext");
		getAction(REPLACE_NEXT_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("print");
		getAction(PRINT_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("printpreview");
		getAction(PRINT_PREVIEW_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("closeall");
		getAction(CLOSE_ALL_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("run"); //Added by PyDE
		getAction(RUN_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("debug");
		getAction(DEBUG_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = mainView.getBreakpointIcon(); //Use the icon actually used in the gutter
		getAction(TOGGLE_BREAKPOINT_ACTION).putValue(Action.SMALL_ICON, icon);
		icon = iconGroup.getIcon("stop");
		getAction(STOP_ACTION).putValue(Action.SMALL_ICON,  icon);
		
		icon = iconGroup.getIcon("startTutorial"); //TODO If we want the Start Tutorial menu item to have an associated icon, add one to the icon group.
		getAction(START_TUTORIAL_ACTION).putValue(Action.SMALL_ICON, icon);
		

		// Change all RTextAreas' open documents' icon sets.
		RTextEditorPane.setIconGroup(iconGroup);

		// The toolbar uses the large versions of the icons, if available.
		// fixme:  Make this toggle-able.
		ToolBar toolBar = (ToolBar)getToolBar();
		if (toolBar!=null)
			toolBar.checkForLargeIcons();

		// Do this because the toolbar has changed it's size.
		if (isDisplayable()) {
			pack();
			setSize(size);
		}

		// Make the help dialog use appropriate "back" and "forward" icons.
		if (helpDialog!=null) {
			helpDialog.setBackButtonIcon(iconGroup.getIcon("back"));
			helpDialog.setForwardButtonIcon(iconGroup.getIcon("forward"));
		}

		firePropertyChange(ICON_STYLE_PROPERTY, old, iconGroup);

	}
	
	/**
	 * added by pyde
	 * Sets theTutorial to the tutorial.
	 */
	public void setTutorial(Tutorial tut){
		this.theTutorial = tut;
	}


	/**
	 * Sets the main view style.  This method fires a property change of type
	 * {@link #MAIN_VIEW_STYLE_PROPERTY}.
	 *
	 * @param viewStyle One of {@link #TABBED_VIEW}, {@link #SPLIT_PANE_VIEW}
	 *        or {@link #MDI_VIEW}.  If this value is invalid, nothing happens.
	 * @see #getMainViewStyle()
	 */
	public void setMainViewStyle(int viewStyle) {

		if ((viewStyle==TABBED_VIEW || viewStyle==SPLIT_PANE_VIEW ||
				viewStyle==MDI_VIEW) && viewStyle!=mainViewStyle) {

			int oldMainViewStyle = mainViewStyle;
			mainViewStyle = viewStyle;
			AbstractMainView fromView = mainView;

			RTextPreferences props = (RTextPreferences)RTextPreferences.
									generatePreferences(this);

			
			//added by PyDE
			this.tutorialModeEnabled = props.tutorialEnabled;
			// Create the new view.
			switch (viewStyle) {
				case TABBED_VIEW:
					mainView = new RTextTabbedPaneView(this, null, props);
					menuBar.setWindowMenuVisible(false);
					break;
				case SPLIT_PANE_VIEW:
					mainView = new RTextSplitPaneView(this, null, props);
					menuBar.setWindowMenuVisible(false);
					break;
				case MDI_VIEW:
					mainView = new RTextMDIView(this, null, props);
					menuBar.setWindowMenuVisible(true);
					break;
			}

			// Update property change listeners.
			PropertyChangeListener[] propertyChangeListeners =
								fromView.getPropertyChangeListeners();
			int length = propertyChangeListeners.length;
			for (int i=0; i<length; i++) {
				fromView.removePropertyChangeListener(propertyChangeListeners[i]);
				mainView.addPropertyChangeListener(propertyChangeListeners[i]);
			}

			// Keep find/replace dialogs working, if they've been created.
			// Make the new dialog listen to actions from the find/replace
			// dialogs.
			// NOTE:  The find and replace dialogs will be moved to mainView
			// in the copyData method below.
			mainView.getSearchManager().changeSearchListener(fromView);

			// Make mainView have all the properties of the old panel.
			mainView.copyData(fromView);

			// If we have switched to a tabbed view, artificially
			// fire stateChanged if the last document is selected,
			// because it isn't fired naturally if this is so.
			if ((mainView instanceof RTextTabbedPaneView) &&
				mainView.getSelectedIndex()==mainView.getNumDocuments()-1)
				((RTextTabbedPaneView)mainView).stateChanged(new ChangeEvent(mainView));


			// Physically replace the old main view with the new one.
			// NOTE: We need to remember previous size and restore it
			// because center collapses if changed to MDI otherwise.
			Dimension size = getSize();
			Container contentPane = getContentPane();

			contentPane.remove(fromView);
			contentPane.add(mainView);
			fromView.dispose();
			fromView = null;
			//contentPane.add(mainView, BorderLayout.CENTER);
			pack();
			setSize(size);
			// For some reason we have to reselect the currently-selected
			// window to have it actually active in an MDI view.
			if (mainView instanceof RTextMDIView)
				mainView.setSelectedIndex(mainView.getSelectedIndex());


			firePropertyChange(MAIN_VIEW_STYLE_PROPERTY, oldMainViewStyle,
												mainViewStyle);

		} // End of if ((viewStyle==TABBED_VIEW || ...

	}


	/**
	 * This method changes both the active file name in the title bar, and the
	 * status message in the status bar.
	 *
	 * @param fileFullPath Full path to the text file currently being edited
	 *        (to be displayed in the window's title bar).  If
	 *        <code>null</code>, the currently displayed message is not
	 *        changed.
	 * @param statusMessage The message to be displayed in the status bar.
	 *        If <code>null</code>, the status bar message is not changed.
	 */
	public void setMessages(String fileFullPath, String statusMessage) {
		if (fileFullPath != null)
			setTitle(fileFullPath);
		StatusBar statusBar = (StatusBar)getStatusBar();
		if (statusBar!=null && statusMessage != null)
			statusBar.setStatusMessage(statusMessage);
	}


	/**
	 * Enables or disables the row/column indicator in the status bar.
	 *
	 * @param isVisible Whether or not the row/column indicator should be
	 *        visible.
	 */
	public void setRowColumnIndicatorVisible(boolean isVisible) {
		((StatusBar)getStatusBar()).setRowColumnIndicatorVisible(isVisible);
	}


	/**
	 * Sets whether the hostname should be shown in the title of the main
	 * RText window.
	 *
	 * @param show Whether the hostname should be shown.
	 * @see #getShowHostName()
	 */
	public void setShowHostName(boolean show) {
		if (this.showHostName!=show) {
			this.showHostName = show;
			setTitle(getTitle()); // Cause title to refresh.
		}
	}


	/**
	 * Sets whether the read-only indicator in the status bar is enabled.
	 *
	 * @param enabled Whether or not the read-only indicator is enabled.
	 */
	public void setStatusBarReadOnlyIndicatorEnabled(boolean enabled) {
		((StatusBar)getStatusBar()).setReadOnlyIndicatorEnabled(enabled);
	}


	/**
	 * Sets the syntax highlighting color scheme being used.
	 *
	 * @param colorScheme The new color scheme to use.  If
	 *        <code>null</code>, nothing changes.
	 */
	public void setSyntaxScheme(SyntaxScheme colorScheme) {
		if (colorScheme!=null && !colorScheme.equals(this.colorScheme)) {
			// Make a deep copy for our copy.  We must be careful to do this
			// and pass our newly-created deep copy to mainView so that we
			// do not end up with the same copy passed to us (which could be
			// in the process of being edited in an options dialog).
			this.colorScheme = (SyntaxScheme)colorScheme.clone();
			if (mainView!=null)
				mainView.setSyntaxScheme(this.colorScheme);
		}
	}


	/**
	 * Changes whether or not tabs should be emulated with spaces
	 * (i.e., soft tabs).
	 * This simply calls <code>mainView.setTabsEmulated</code>.
	 *
	 * @param areEmulated Whether or not tabs should be emulated with spaces.
	 */
	public void setTabsEmulated(boolean areEmulated) {
		mainView.setTabsEmulated(areEmulated);
	}


	/**
	 * Sets the tab size to be used on all documents.
	 *
	 * @param newSize The tab size to use.
	 * @see #getTabSize()
	 */
	public void setTabSize(int newSize) {
		mainView.setTabSize(newSize);
	}


	/**
	 * Sets the title of the application window.  This title is prefixed
	 * with the application name.
	 *
	 * @param title The new title.
	 * @see #getTitle()
	 */
	public void setTitle(String title) {
		if (getShowHostName()) {
			title = "Savu (" + getHostName() + ") - " + title;
		}
		else {
			title = "Savu - " + title;
		}
		super.setTitle(title);
	}


	/**
	 * Sets the opacity with which to render unfocused child windows, if this
	 * option is enabled.
	 *
	 * @param opacity The opacity.  This should be between <code>0</code> and
	 *        <code>1</code>.
	 * @see #getSearchWindowOpacity()
	 * @see #setSearchWindowOpacityRule(int)
	 */
	public void setSearchWindowOpacity(float opacity) {
		searchWindowOpacity = Math.max(0, Math.min(opacity, 1));
		if (windowListenersInited && isSearchWindowOpacityEnabled()) {
			searchWindowOpacityListener.refreshTranslucencies();
		}
	}


	/**
	 * Toggles whether search window opacity is enabled.
	 *
	 * @param enabled Whether search window opacity should be enabled.
	 * @see #isSearchWindowOpacityEnabled()
	 */
	public void setSearchWindowOpacityEnabled(boolean enabled) {
		if (enabled!=searchWindowOpacityEnabled) {
			searchWindowOpacityEnabled = enabled;
			// Toggled either on or off
			// Must check searchWindowOpacityListener since in pre 6u10,
			// we'll be inited, but listener isn't created.
			if (windowListenersInited &&
					searchWindowOpacityListener!=null) {
				searchWindowOpacityListener.refreshTranslucencies();
			}
		}
	}


	/**
	 * Toggles whether certain child windows should be made translucent.
	 *
	 * @param rule The new opacity rule.
	 * @see #getSearchWindowOpacityRule()
	 * @see #setSearchWindowOpacity(float)
	 */
	public void setSearchWindowOpacityRule(int rule) {
		if (rule!=searchWindowOpacityRule) {
			searchWindowOpacityRule = rule;
			if (windowListenersInited) {
				searchWindowOpacityListener.setTranslucencyRule(rule);
			}
		}
	}



	/**
	 * Sets the "working directory;" that is, the directory in which
	 * new, empty files are placed.
	 *
	 * @param directory The new working directory.  If this directory does
	 *        not exist, the Java property "user.dir" is used.
	 * @see #getWorkingDirectory
	 */
	public void setWorkingDirectory(String directory) {
		File test = new File(directory);
		if (test.isDirectory())
			workingDirectory = directory;
		else
			workingDirectory = System.getProperty("user.dir");
	}


	/**
	 * {@inheritDoc}
	 */
	public void updateLookAndFeel(LookAndFeel lnf) {

		super.updateLookAndFeel(lnf);

		try {

			Dimension size = this.getSize();

			// Update all components in this frame.
			SwingUtilities.updateComponentTreeUI(this);
			this.pack();
			this.setSize(size);

			// So mainView knows to update it's popup menus, etc.
			mainView.updateLookAndFeel();
			
			// Update any dialogs.
			if (optionsDialog != null) {
				SwingUtilities.updateComponentTreeUI(optionsDialog);
				optionsDialog.pack();
			}
			if (helpDialog != null) {
				SwingUtilities.updateComponentTreeUI(helpDialog);
				helpDialog.pack();
			}

			if (chooser!=null) {
				SwingUtilities.updateComponentTreeUI(chooser);
				chooser.updateUI(); // So the popup menu gets updated.
	 		}
			if (rfc!=null) {
				SwingUtilities.updateComponentTreeUI(rfc);
				rfc.updateUI(); // Not JDialog API; specific to this class
			}

		} catch (Exception f) {
			displayException(f);
		}

	}


	/**
	 * 1.5.2004/pwy: The following two functions are called from the
	 * OSXAdapter and provide the hooks for the functions from the standard
	 * Apple application menu.  The "about()" OSX hook is in
	 * AbstractGUIApplication.
	 */
	public void preferences() {
		getAction(OPTIONS_ACTION).actionPerformed(new ActionEvent(this,0,"unused"));
	}

	public void openFile(final String filename) {
		//gets called when we receive an open event from the finder on OS X
		if (SwingUtilities.isEventDispatchThread()) {
			mainView.openFile(filename, null);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// null encoding means check for Unicode before using
					// system default encoding.
					mainView.openFile(filename, null);
				}
			});
		}
	}


	/**
	 * Program entry point.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(final String[] args) {

		// 1.5.2004/pwy: Setting this property makes the menu appear on top
		// of the screen on Apple Mac OS X systems. It is ignored by all other
		// other Java implementations.
		System.setProperty("apple.laf.useScreenMenuBar","true");

		// Catch any uncaught Throwables on the EDT and log them.
		AWTExceptionHandler.register();

		// 1.5.2004/pwy: Setting this property defines the standard
		// Application menu name on Apple Mac OS X systems. It is ignored by
		// all other Java implementations.
		// NOTE: Although you can set the useScreenMenuBar property above at
		// runtime, it appears that for this one, you must set it before
		// (such as in your *.app definition).
		//System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Savu");

		// Swing stuff should always be done on the EDT...
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				String lafName = RTextPreferences.getLookAndFeelToLoad();

				// Allow Substance to paint window titles, etc.  We don't allow
				// Metal (for example) to do this, because setting these
				// properties to "true", then toggling to a LAF that doesn't
				// support this property, such as Windows, causes the
				// OS-supplied frame to not appear (as of 6u20).
				if (SubstanceUtils.isASubstanceLookAndFeel(lafName)) {
					JFrame.setDefaultLookAndFeelDecorated(true);
					JDialog.setDefaultLookAndFeelDecorated(true);
				}

				String rootDir = AbstractGUIApplication.
											getLocationOfJar("RText.jar");
				ThirdPartyLookAndFeelManager lafManager =
					new ThirdPartyLookAndFeelManager(rootDir);

				try {
					ClassLoader cl = lafManager.getLAFClassLoader();
					// Set these properties before instantiating WebLookAndFeel
					if (WebLookAndFeelUtils.isWebLookAndFeel(lafName)) {
						WebLookAndFeelUtils.installWebLookAndFeelProperties(cl);
					}
					// Must set UIManager's ClassLoader before instantiating
					// the LAF.  Substance is so high-maintenance!
					UIManager.getLookAndFeelDefaults().put("ClassLoader", cl);
					Class clazz = null;
					try {
						clazz = cl.loadClass(lafName);
					} catch (UnsupportedClassVersionError ucve) {
						// Previously opened with e.g. Java 6/Substance, now
						// restarting with Java 1.4 or 1.5.
						lafName = UIManager.getSystemLookAndFeelClassName();
						clazz = cl.loadClass(lafName);
					}
					LookAndFeel laf = (LookAndFeel)clazz.newInstance();
					UIManager.setLookAndFeel(laf);
					UIManager.getLookAndFeelDefaults().put("ClassLoader", cl);
					UIUtil.installOsSpecificLafTweaks();
				} catch (RuntimeException re) { // FindBugs
					throw re;
				} catch (Exception e) {
					e.printStackTrace();
				}

				// The default speed of Substance animations is too slow
				// (200ms), looks bad moving through JMenuItems quickly.
				if (SubstanceUtils.isSubstanceInstalled()) {
					try {
						SubstanceUtils.setAnimationSpeed(100);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				Savu rtext = new Savu(args);
				rtext.setLookAndFeelManager(lafManager);
				rtext.setBackground(Color.BLACK);

				// For some reason, when using MDI_VIEW, the first window
				// isn't selected (although it is activated)...
				// INVESTIGATE ME!!
				if (rtext.getMainViewStyle()==MDI_VIEW) {
					rtext.getMainView().setSelectedIndex(0);
				}

				// We currently have one RText instance running.
				StoreKeeper.addRTextInstance(rtext);
				


			}
		});
		

	}




}