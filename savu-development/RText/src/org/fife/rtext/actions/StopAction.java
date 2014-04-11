/*
 * 12/17/2010
 *
 * StopAction.java - Stops the currently running process, if any.
 * Copyright (C) 2010 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.actions;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.Icon;

import org.fife.rtext.Savu;
import org.fife.rtext.ToolBar;
import org.fife.rtext.plugins.debug.DebugPlugin;
import org.fife.rtext.plugins.run.RunPlugin;
import org.fife.ui.app.Plugin;
import org.fife.ui.app.StandardAction;

/**
 * Stops the currently running process, if any.
 * 
 * @author Robert Futrell Jr.
 * @version 1.0
 */
public class StopAction extends StandardAction {

	/**
	 * The parent plugin.
	 */
	private RunPlugin runPlug;
	private DebugPlugin debugPlug;
	/**
	 * Constructor.
	 * 
	 * @param owner
	 *            The parent RText instance.
	 * @param msg
	 *            The resource bundle to use for localization.
	 * @param plugin
	 *            The parent plugin.
	 */
	public StopAction(Savu owner, ResourceBundle msg, Icon icon) {
		super(owner, msg, "StopAction");
		setEnabled(false);
		runPlug = null;
		setIcon(icon); 
	}

	public void actionPerformed(ActionEvent e) {	
		Savu owner = (Savu)getApplication();
		if (runPlug == null) { //Need to get a reference to the run plugin
			runPlug = RunAction.getRunPlugin(owner);
			if (runPlug == null) { //Plugin hasn't loaded yet or can't load. Should never happen.
				owner.displayException(new Exception("Run plugin not loaded."));
				return;
			}
		}
		runPlug.stopCurrentProcess(); 
	}
	

}