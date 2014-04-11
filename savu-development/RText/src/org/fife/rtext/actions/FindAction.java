/*
 * 11/14/2003
 *
 * FindAction.java - Action for searching for text in RText.
 * Copyright (C) 2003 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.actions;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.JButton;

import org.fife.rtext.AbstractMainView;
import org.fife.rtext.Savu;
import org.fife.ui.app.StandardAction;


/**
 * Action used by an <code>AbstractMainView</code> to search for text.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class FindAction extends StandardAction {


	/**
	 * Constructor.
	 *
	 * @param owner The parent RText instance.
	 * @param msg The resource bundle to use for localization.
	 * @param icon The icon associated with the action.
	 */
	public FindAction(Savu owner, ResourceBundle msg, Icon icon) {
		this(owner, msg, icon, "FindAction");
	}


	/**
	 * Constructor.
	 *
	 * @param owner The parent RText instance.
	 * @param msg The resource bundle to use for localization.
	 * @param icon The icon associated with the action.
	 * @param nameKey The key for localizing the name of this action.
	 */
	protected FindAction(Savu owner, ResourceBundle msg, Icon icon,
							String nameKey) {
		super(owner, msg, nameKey);
		setIcon(icon);
	}


	/**
	 * Callback routine called when user uses this component.
	 *
	 * @param e The action performed.
	 */
	public void actionPerformed(ActionEvent e) {

		Savu rtext = (Savu)getApplication();

		AbstractMainView mainView = rtext.getMainView();
		mainView.getSearchManager().showFindUI();

	}


}