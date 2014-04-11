/*
 * 12/22/2010
 *
 * DebugOptionPanel.java - Option panel for managing the Debug plugin.
 * Copyright (C) 2010 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.debug;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;

import org.fife.ui.RColorSwatchesButton;
import org.fife.ui.UIUtil;
import org.fife.ui.app.PluginOptionsDialogPanel;

/**
 * Options panel for managing the Debug plugin.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
class DebugOptionPanel extends PluginOptionsDialogPanel implements
		ActionListener, ItemListener, PropertyChangeListener {

	private JCheckBox visibleCB;

	private static final String PROPERTY = "Property";

	/**
	 * Constructor.
	 * 
	 * @param plugin
	 *            The plugin.
	 */
	public DebugOptionPanel(DebugPlugin plugin) {

		super(plugin);
		setName(plugin.getString("Options.Title"));
		ComponentOrientation o = ComponentOrientation
				.getOrientation(getLocale());

		// Set up our border and layout.
		setBorder(UIUtil.getEmpty5Border());
		setLayout(new BorderLayout());
		Box topPanel = Box.createVerticalBox();

		// Add the "general" options panel.
		Container generalPanel = createGeneralPanel();
		topPanel.add(generalPanel);
		topPanel.add(Box.createVerticalStrut(5));

		//Add the "behaviors" option panel.
		/*Container behaviorsPanel = createBehaviorsPanel();
		topPanel.add(behaviorsPanel);
		topPanel.add(Box.createVerticalStrut(5));*/
		

		// Put it all together!
		topPanel.add(Box.createVerticalGlue());
		add(topPanel, BorderLayout.NORTH);
		applyComponentOrientation(o);

	}

	/**
	 * Called when the user toggles various properties in this panel.
	 * 
	 * @param e
	 *            The event.
	 */
	public void actionPerformed(ActionEvent e) {

		Object source = e.getSource();

		if (visibleCB == source) {
			setVisibleCBSelected(visibleCB.isSelected());
			hasUnsavedChanges = true;
			boolean visible = visibleCB.isSelected();
			firePropertyChange(PROPERTY, !visible, visible);
		}

	}

	/**
	 * Creates the "Behaviors" section of options for this plugin.
	 * 
	 * @return A panel with the "behavior" options.
	 */
	private Container createBehaviorsPanel() {

		Box temp = Box.createVerticalBox();

		DebugPlugin plugin = (DebugPlugin) getPlugin();
		temp.setBorder(new OptionPanelBorder(plugin.getString("Options.Behaviors")));

		//TODO implement DebugOptionPanel's Behaviors GUI
		
		return temp;

	}
	

	/**
	 * Returns a check box used to toggle whether a color in a debug uses a
	 * special color.
	 * 
	 * @param label
	 *            The label for the check box.
	 * @return The check box.
	 */
	private JCheckBox createColorActivateCB(String label) {
		JCheckBox cb = new JCheckBox(label);
		cb.addActionListener(this);
		return cb;
	}

	/**
	 * Creates a color picker button we're listening for changes on.
	 * 
	 * @return The button.
	 */
	private RColorSwatchesButton createColorSwatchesButton() {
		RColorSwatchesButton button = new RColorSwatchesButton();
		button.addPropertyChangeListener(
				RColorSwatchesButton.COLOR_CHANGED_PROPERTY, this);
		return button;
	}

	/**
	 * Creates the "General" section of options for this plugin.
	 * 
	 * @return A panel with the "general" options.
	 */
	private Container createGeneralPanel() {

		DebugPlugin plugin = (DebugPlugin) getPlugin();
		ResourceBundle gpb = ResourceBundle
				.getBundle("org.fife.ui.app.GUIPlugin");

		Box temp = Box.createVerticalBox();
		temp.setBorder(new OptionPanelBorder(plugin
				.getString("Options.General")));

		// A check box toggling the plugin's visibility.
		visibleCB = new JCheckBox(gpb.getString("Visible"));
		visibleCB.addActionListener(this);
		JPanel temp2 = new JPanel(new BorderLayout());
		temp2.add(visibleCB, BorderLayout.LINE_START);
		temp.add(temp2);
		temp.add(Box.createVerticalStrut(5));

		temp.add(Box.createVerticalGlue());
		return temp;

	}

	/**
	 * {@inheritDoc}
	 */
	protected void doApplyImpl(Frame owner) {

		DebugPlugin plugin = (DebugPlugin) getPlugin();
		DebugWindow window = plugin.getDockableWindow();
		window.setActive(visibleCB.isSelected());
	}

	/**
	 * Always returns <code>null</code>, as the user cannot enter invalid input
	 * on this panel.
	 * 
	 * @return <code>null</code> always.
	 */
	protected OptionsPanelCheckResult ensureValidInputsImpl() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public JComponent getTopJComponent() {
		return visibleCB;
	}

	/**
	 * Called when the user changes the desired location of the dockable window.
	 * 
	 * @param e
	 *            The event.
	 */
	public void itemStateChanged(ItemEvent e) {
		//No longer implemented in Savu
	}

	/**
	 * Returns whether something on this panel is NOT set to its default value.
	 * 
	 * @return Whether some property in this panel is NOT set to its default
	 *         value.
	 */
	private boolean notDefaults() {
		return !visibleCB.isSelected();
	}

	/**
	 * Called when one of our color picker buttons is modified.
	 * 
	 * @param e
	 *            The event.
	 */
	public void propertyChange(PropertyChangeEvent e) {
		hasUnsavedChanges = true;
		firePropertyChange(PROPERTY, false, true);
	}

	/**
	 * Restores all properties on this panel to their default values.
	 */
	private void restoreDefaults() {

		setVisibleCBSelected(true);
		
	}

	/**
	 * {@inheritDoc}
	 */
	protected void setValuesImpl(Frame owner) {

		DebugPlugin plugin = (DebugPlugin) getPlugin();
		DebugWindow window = plugin.getDockableWindow();
		visibleCB.setSelected(window.isActive());

	}

	private void setVisibleCBSelected(boolean selected) {
		visibleCB.setSelected(selected);
	}

}