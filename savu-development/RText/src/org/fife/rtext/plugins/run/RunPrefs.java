/*
 * 01/04/2011
 *
 * RunPrefs.java - Preferences for the run plugin.
 * Copyright (C) 2011 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.run;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.KeyStroke;

import org.fife.ui.app.Prefs;
import org.fife.ui.dockablewindows.DockableWindow;

/**
 * Preferences for the Run plugin.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
public class RunPrefs extends Prefs {

	/**
	 * Whether the GUI plugin window is active (visible).
	 */
	public boolean windowVisible;

	/**
	 * The location of the dockable run output window.
	 */
	public int windowPosition;

	/**
	 * Key stroke that toggles the run window's visibility.
	 */
	public KeyStroke windowVisibilityAccelerator;

	/**
	 * The color used for stdout in runs.
	 */
	public Color stdoutFG;

	/**
	 * The color used for stderr in runs.
	 */
	public Color stderrFG;

	/**
	 * The color used for exceptions in runs.
	 */
	public Color exceptionFG;

	/**
	 * The color used for prompts in runs.
	 */
	public Color promptFG;

	/**
	 * Whether to clear the output between runs.
	 */
	public boolean clearOutputBeforeRun;
	
	/**
	 * Overridden to validate the dockable window position value.
	 */
	public void load(InputStream in) throws IOException {
		super.load(in);
		// Ensure window position is valid.
		if (!DockableWindow.isValidPosition(windowPosition)) {
			windowPosition = DockableWindow.BOTTOM;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefaults() {
		windowVisible = true;
		windowPosition = DockableWindow.BOTTOM;
		windowVisibilityAccelerator = null;
		stdoutFG = RunTextArea.DEFAULT_STDOUT_FG;
		stderrFG = RunTextArea.DEFAULT_STDERR_FG;
		exceptionFG = RunTextArea.DEFAULT_EXCEPTION_FG;
		promptFG = RunTextArea.DEFAULT_PROMPT_FG;
		clearOutputBeforeRun = true;
	}

}