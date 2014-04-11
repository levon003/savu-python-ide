/*
 * 01/04/2011
 *
 * DebugPrefs.java - Preferences for the debug plugin.
 * Copyright (C) 2011 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.debug;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.KeyStroke;

import org.fife.ui.app.Prefs;
import org.fife.ui.dockablewindows.DockableWindow;

/**
 * Preferences for the Debug plugin.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
public class DebugPrefs extends Prefs {

	/**
	 * Whether the GUI plugin window is active (visible).
	 */
	public boolean windowVisible;

	/**
	 * The location of the dockable debug output window.
	 */
	public int windowPosition;

	/**
	 * Key stroke that toggles the debug window's visibility.
	 */
	public KeyStroke windowVisibilityAccelerator;


	
	/**
	 * Overridden to validate the dockable window position value.
	 */
	public void load(InputStream in) throws IOException {
		super.load(in);
		// Ensure window position is valid.
		if (!DockableWindow.isValidPosition(windowPosition)) {
			windowPosition = DockableWindow.RIGHT;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDefaults() {
		windowVisible = true;
		windowPosition = DockableWindow.RIGHT;
		windowVisibilityAccelerator = null;
	}

}