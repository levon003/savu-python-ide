/*
 * 12/22/2010
 *
 * SystemShellTextArea.java - Text component simulating a system shell.
 * Copyright (C) 2010 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.run;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.TextAction;

import org.fife.io.ProcessRunner;
import org.fife.io.ProcessRunnerOutputListener;
import org.fife.rtext.Savu;
import org.fife.ui.rtextfilechooser.Utilities;


/**
 * A text area simulating a system shell.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class OutputTextArea extends RunTextArea {

	private File pwd;
	private File prevDir;
	private final boolean isWindows;
	private transient Thread activeProcessThread;

	private static final boolean CASE_SENSITIVE =
			Utilities.isCaseSensitiveFileSystem();


	public OutputTextArea(RunPlugin plugin) {
		super(plugin);
		isWindows = plugin.getRText().getOS()==Savu.OS_WINDOWS;
	}


	/**
	 * {@inheritDoc}
	 */
	protected void init() {
		pwd = new File(System.getProperty("user.home"));
		prevDir = pwd;
	}

	/**
	 * Listens for output from the currently active process and appends it
	 * to the run.
	 */
	private class ProcessOutputListener implements ProcessRunnerOutputListener{

		public void outputWritten(Process p, String output, boolean stdout) {
			append(output, stdout ? STYLE_STDOUT : STYLE_STDERR);
		}

		public void processCompleted(Process p, int rc, final Throwable e) {
			// Required because of other Swing calls we make inside
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (e!=null) {
						String text = null;
						if (e instanceof InterruptedException) {
							text = plugin.getString("ProcessForciblyTerminated");
						}
						else {
							StringWriter sw = new StringWriter();
							e.printStackTrace(new PrintWriter(sw));
							text = sw.toString();
						}
						append(text, STYLE_EXCEPTION);
					}
					// Not really necessary, should allow GC of Process resources
					activeProcessThread = null;
					setEditable(true);
					firePropertyChange(PROPERTY_PROCESS_RUNNING, true, false);
				}
			});
		}

	}


}