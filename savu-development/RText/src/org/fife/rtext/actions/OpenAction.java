/*
 * 11/14/2003
 *
 * OpenAction.java - Action to open an old text file in RText.
 * Copyright (C) 2003 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ResourceBundle;

import javax.swing.Icon;

import org.fife.rtext.AbstractMainView;
import org.fife.rtext.Savu;
import org.fife.ui.app.StandardAction;
import org.fife.ui.rtextfilechooser.RTextFileChooser;

/**
 * Action used by an <code>AbstractMainView</code> to open a document from a
 * file on disk.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
class OpenAction extends StandardAction {

	private String previousOpenLocation;
	
	/**
	 * Constructor.
	 * 
	 * @param owner
	 *            The parent RText instance.
	 * @param msg
	 *            The resource bundle to use for localization.
	 * @param icon
	 *            The icon associated with the action.
	 */
	public OpenAction(Savu owner, ResourceBundle msg, Icon icon) {
		super(owner, msg, "OpenAction");
		setIcon(icon);
		previousOpenLocation = null;
	}

	public void actionPerformed(ActionEvent e) {

		Savu owner = (Savu) getApplication();
		FileDialog fd = new FileDialog(owner, "Open File", FileDialog.LOAD);
		if (previousOpenLocation == null) {
			fd.setDirectory(System.getProperty("user.home"));
		} else {
			fd.setDirectory(previousOpenLocation);
		}
		fd.setMode(FileDialog.LOAD);
		fd.setMultipleMode(true);
		fd.setVisible(true);
		File[] files = fd.getFiles(); //Actually block until the files are returned.
		if (files.length == 0) {
			return;
		}
		previousOpenLocation = fd.getDirectory(); // Save the directory the user selected for future Open calls.
		AbstractMainView mainView = owner.getMainView();
		for (File filename : files) {
			String fileFullPath = filename.getAbsolutePath();
			mainView.openFile(fileFullPath, RTextFileChooser.getDefaultEncoding());
		}

	}

}