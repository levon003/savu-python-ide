/*
 * 11/3/2009
 *
 * NewToolAction.java - Action that creates a new user tool
 * Copyright (C) 2009 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.actions;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.Icon;

import org.fife.rtext.Savu;
import org.fife.ui.app.StandardAction;

/**
 * Action that creates a new user tool.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
class NewToolAction extends StandardAction {

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
	public NewToolAction(Savu owner, ResourceBundle msg, Icon icon) {
		super(owner, msg, "NewToolAction");
		setIcon(icon);
	}

	/**
	 * {@inheritDoc}
	 */
	public void actionPerformed(ActionEvent e) {

		// RText owner = (RText)getApplication();

	}

}