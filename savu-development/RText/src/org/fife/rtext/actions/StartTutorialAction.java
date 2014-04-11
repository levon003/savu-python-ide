package org.fife.rtext.actions;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import javax.swing.Icon;

import org.fife.rtext.Savu;
import org.fife.rtext.plugins.run.RunPlugin;
import org.fife.rtext.tutorial.Tutorial;
import org.fife.ui.app.GUIApplication;
import org.fife.ui.app.StandardAction;

public class StartTutorialAction extends StandardAction {

	Savu owner;
	
	public StartTutorialAction(Savu owner, ResourceBundle msg, Icon icon) {
		super(owner, msg, "StartTutorialAction");
		setIcon(icon);
		this.owner = owner;
	}

	public void actionPerformed(ActionEvent e) {
		Tutorial newTut = new Tutorial(owner);
		newTut.start();
	}

}
