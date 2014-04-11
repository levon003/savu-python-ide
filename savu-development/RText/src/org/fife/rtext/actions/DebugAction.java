/*
 * 11/14/2003
 *
 * RunAction.java
 *
 */
package org.fife.rtext.actions;

import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.Icon;

import org.fife.rtext.plugins.debug.Breakpoint;
import org.fife.rtext.plugins.debug.DebugPlugin;
import org.fife.rtext.AbstractMainView;
import org.fife.rtext.Savu;
import org.fife.rtext.RTextEditorPane;
import org.fife.rtext.plugins.run.RunPlugin;
import org.fife.ui.app.Plugin;
import org.fife.ui.app.StandardAction;


/**
 * Action used to run a python file
 *
 * @author PyDE
 * @version 0.1
 */
public class DebugAction extends StandardAction {

	/**
	 * A reference to the RunPlugin this action is connected to.
	 */
	private DebugPlugin debugPlug;
	private RunPlugin runPlug;
	
	/**
	 * Constructor.
	 *
	 * @param owner The parent RText instance.
	 * @param msg The resource bundle to use for localization.
	 * @param icon The icon associated with the action.
	 */
	public DebugAction(Savu owner, ResourceBundle msg, Icon icon) {
		super(owner, msg, "DebugAction");
		setIcon(icon);
		debugPlug = null;
		runPlug = null;
	}

	public void actionPerformed(ActionEvent e) {

		
		Savu owner = (Savu)getApplication();
		if (debugPlug == null) { //Need to get a reference to the debug plugin
			debugPlug = DebugAction.getDebugPlugin(owner);
			if (debugPlug == null) { //Plugin hasn't loaded yet or can't load.
				owner.displayException(new Exception("Debug plugin not loaded."));
				return;
			}
		}
		if (runPlug == null) { //Need to get a reference to the run plugin
			runPlug = RunAction.getRunPlugin(owner);
			if (runPlug == null) { //Plugin hasn't loaded yet or can't load.
				owner.displayException(new Exception("Run plugin not loaded."));
				return;
			}
		}
		
		AbstractMainView mainView = owner.getMainView();
		if (mainView.saveCurrentFile()) { //Save the file before running it.
			RTextEditorPane pane = mainView.getCurrentTextArea();
			pane.setEditable(false);
			try {
				String args = owner.getAndSaveCurrentArguments();
				ArrayList<Breakpoint> breakpoints = owner.getBreakpoints();
				runPlug.clearTextAreaForStart();
				InputStream[] debugStreams = debugPlug.debugPythonFile(pane.getFileFullPath(), breakpoints, args); 
				if (debugStreams != null) {
					InputStream stdout = debugStreams[0];
					InputStream stderr = debugStreams[1];
					runPlug.handleDebugging(stdout, stderr, debugPlug.getCurrentProcess(), pane.getFileFullPath());
				} else {
					runPlug.getDockableWindow().setCurrentProcessLabelToError("ERROR: Could not execute Python command; check that Python is installed and accessible and try again.");
				}
			} finally {
				pane.setEditable(true);
			}
		}
		
		
	}

	/**
	 * Gets a reference to the DebugPlugin if it's been loaded by the parent application.
	 * 
	 * @param owner The application that queued the DebugPlugin
	 * @return a reference to the DebugPlugin or null
	 */
	public static DebugPlugin getDebugPlugin(Savu owner) {
		if (owner.isPluginLoadingComplete()) {
			Plugin[] plugs = owner.getPlugins();
			for (int i = 0; i<plugs.length; ++i) {
				if (plugs[i] instanceof DebugPlugin)
					return (DebugPlugin) plugs[i];
			}
			return null;
		}
		return null;
	}
	
}