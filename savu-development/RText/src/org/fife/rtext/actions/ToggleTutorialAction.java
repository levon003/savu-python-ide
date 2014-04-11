/*
 * 2/10/2014
 */
package org.fife.rtext.actions;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.Icon;

import org.fife.rtext.Savu;
import org.fife.ui.app.Plugin;
import org.fife.ui.app.StandardAction;

/**
 * Action used to toggle the Tutorial Mode on and off
 */
class ToggleTutorialAction extends StandardAction {

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
	public ToggleTutorialAction(Savu owner, ResourceBundle msg, Icon icon) {
		super(owner, msg, "ToggleTutorialAction");
		setIcon(icon);
	}

	/**
	 * Called when this action is performed.
	 * 
	 * @param e
	 *            The event.
	 */
	public void actionPerformed(ActionEvent e) {
		Savu owner = (Savu)getApplication();
		System.out.print(owner.isTutorialModeEnabled());
		owner.setTutorialModeEnabled(!owner.isTutorialModeEnabled());
	}

}