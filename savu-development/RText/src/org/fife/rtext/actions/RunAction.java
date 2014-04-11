/*
 * 11/14/2003
 *
 * RunAction.java
 *
 */
package org.fife.rtext.actions;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.swing.Icon;

import org.fife.rtext.AbstractMainView;
import org.fife.rtext.Savu;
import org.fife.rtext.RTextEditorPane;
import org.fife.rtext.plugins.debug.Breakpoint;
import org.fife.rtext.plugins.run.RunPlugin;
import org.fife.ui.app.Plugin;
import org.fife.ui.app.StandardAction;


/**
 * Action used to run a python file
 *
 * @author PyDE
 * @version 0.1
 */
public class RunAction extends StandardAction {

	/**
	 * A reference to the RunPlugin this action is connected to.
	 */
	private RunPlugin runPlug;
	
	/**
	 * Constructor.
	 *
	 * @param owner The parent RText instance.
	 * @param msg The resource bundle to use for localization.
	 * @param icon The icon associated with the action.
	 */
	public RunAction(Savu owner, ResourceBundle msg, Icon icon) {
		super(owner, msg, "RunAction");
		setIcon(icon);
		runPlug = null;
	}

	public void actionPerformed(ActionEvent e) {

		System.out.print("Run action started: 1");
		
		Savu owner = (Savu)getApplication();
		System.out.print("2");
		if (runPlug == null) { //Need to get a reference to the run plugin
			runPlug = RunAction.getRunPlugin(owner);
			System.out.print("3");
			if (runPlug == null) { //Plugin hasn't loaded yet or can't load.
				owner.displayException(new Exception("Run plugin not loaded."));
				return;
			}
		}
		
		AbstractMainView mainView = owner.getMainView();
		System.out.print("4");
		if (mainView.saveCurrentFile()) { //Save the file before running it.
			System.out.print("5");
			RTextEditorPane pane = mainView.getCurrentTextArea();
			String args = owner.getAndSaveCurrentArguments();
			System.out.print("6");
			pane.setEditable(false);
			runPlug.runPythonFile(pane.getFileFullPath(), args); //Queue the python file in the output window
			pane.setEditable(true);
		}
		
		
	}

	/**
	 * Gets a reference to the RunPlugin if it's been loaded by the parent application.
	 * 
	 * @param owner The application that queued the RunAction
	 * @return a reference to the RunPlugin or null
	 */
	public static RunPlugin getRunPlugin(Savu owner) {
		if (owner.isPluginLoadingComplete()) {
			Plugin[] plugs = owner.getPlugins();
			for (int i = 0; i<plugs.length; ++i) {
				if (plugs[i] instanceof RunPlugin)
					return (RunPlugin) plugs[i];
			}
			return null;
		}
		return null;
	}
	
}