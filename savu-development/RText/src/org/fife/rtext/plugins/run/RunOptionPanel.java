/*
 * 12/22/2010
 *
 * RunOptionPanel.java - Option panel for managing the Run plugin.
 * Copyright (C) 2010 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.run;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.fife.ui.RColorSwatchesButton;
import org.fife.ui.UIUtil;
import org.fife.ui.app.PluginOptionsDialogPanel;

/**
 * Options panel for managing the Run plugin.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
class RunOptionPanel extends PluginOptionsDialogPanel implements
		ActionListener, PropertyChangeListener {

	private JLabel stdoutLabel;
	private JLabel stderrLabel;
	private JLabel stdinLabel;
	private JLabel errorlinkLabel;
	private RColorSwatchesButton stdoutButton;
	private RColorSwatchesButton stderrButton;
	private RColorSwatchesButton stdinButton;
	private RColorSwatchesButton errorlinkButton;
	private JCheckBox clearOutputCB;
	private JButton defaultsButton;

	private static final String PROPERTY = "Property";

	/**
	 * Constructor.
	 * 
	 * @param plugin
	 *            The plugin.
	 */
	public RunOptionPanel(RunPlugin plugin) {

		super(plugin);
		setName(plugin.getString("Options.Title"));
		ComponentOrientation o = ComponentOrientation
				.getOrientation(getLocale());

		// Set up our border and layout.
		setBorder(UIUtil.getEmpty5Border());
		setLayout(new BorderLayout());
		Box topPanel = Box.createVerticalBox();

		// Add the "colors" option panel.
		Container colorsPanel = createColorsPanel();
		topPanel.add(colorsPanel);
		topPanel.add(Box.createVerticalStrut(5));

		//Add the "behaviors" option panel.
		Container behaviorsPanel = createBehaviorsPanel();
		topPanel.add(behaviorsPanel);
		topPanel.add(Box.createVerticalStrut(5));
		
		// Add a "Restore Defaults" button
		defaultsButton = new JButton(plugin.getString("RestoreDefaults"));
		defaultsButton.setActionCommand("RestoreDefaults");
		defaultsButton.addActionListener(this);
		JPanel temp = new JPanel(new BorderLayout());
		temp.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		temp.add(defaultsButton, BorderLayout.LINE_START);
		topPanel.add(temp);

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

		if (defaultsButton == source) {
			if (notDefaults()) {
				restoreDefaults();
				hasUnsavedChanges = true;
				firePropertyChange(PROPERTY, false, true);
			}
		}
		else if (clearOutputCB == source) {
			hasUnsavedChanges = true;
			firePropertyChange(PROPERTY, false, true);
		}

	}

	/**
	 * Creates the "Behaviors" section of options for this plugin.
	 * 
	 * @return A panel with the "behavior" options.
	 */
	private Container createBehaviorsPanel() {

		Box temp = Box.createVerticalBox();

		RunPlugin plugin = (RunPlugin) getPlugin();
		temp.setBorder(new OptionPanelBorder(plugin.getString("Options.Behaviors")));

		clearOutputCB = new JCheckBox(plugin.getString("Behaviors.ClearOutput"));
		clearOutputCB.addActionListener(this);
		
		JPanel sp = new JPanel(new SpringLayout());
		sp.add(clearOutputCB);
		UIUtil.makeSpringCompactGrid(sp, 1, 1, 5, 5, 5, 5);

		JPanel temp2 = new JPanel(new BorderLayout());
		temp2.add(sp, BorderLayout.LINE_START);
		temp.add(temp2);
		temp.add(Box.createVerticalGlue());

		return temp;

	}
	
	/**
	 * Creates the "Colors" section of options for this plugin.
	 * 
	 * @return A panel with the "color" options.
	 */
	private Container createColorsPanel() {

		Box temp = Box.createVerticalBox();

		RunPlugin plugin = (RunPlugin) getPlugin();
		temp.setBorder(new OptionPanelBorder(plugin.getString("Options.Colors")));

		stdoutLabel = new JLabel(plugin.getString("Color.Stdout"));
		stdoutButton = createColorSwatchesButton();
		stderrLabel = new JLabel(plugin.getString("Color.Stderr"));
		stderrButton = createColorSwatchesButton();
		stdinLabel = new JLabel(plugin.getString("Color.Stdin"));
		stdinButton = createColorSwatchesButton();
		errorlinkLabel = new JLabel(plugin.getString("Color.Errorlink"));
		errorlinkButton = createColorSwatchesButton();

		JPanel sp = new JPanel(new SpringLayout());
		if (getComponentOrientation().isLeftToRight()) {
			sp.add(stdoutLabel);
			sp.add(stdoutButton);
			sp.add(stderrLabel);
			sp.add(stderrButton);
			sp.add(stdinLabel);
			sp.add(stdinButton);
			sp.add(errorlinkLabel);
			sp.add(errorlinkButton);
		} else {
			sp.add(stdoutButton);
			sp.add(stdoutLabel);
			sp.add(stderrButton);
			sp.add(stderrLabel);
			sp.add(stdinButton);
			sp.add(stdinLabel);
			sp.add(errorlinkButton);
			sp.add(errorlinkLabel);
		}
		UIUtil.makeSpringCompactGrid(sp, 4, 2, 5, 5, 5, 5);

		JPanel temp2 = new JPanel(new BorderLayout());
		temp2.add(sp, BorderLayout.LINE_START);
		temp.add(temp2);
		temp.add(Box.createVerticalGlue());

		return temp;

	}

	/**
	 * Creates a color picker button we're listening for changes on.
	 * 
	 * @return The button.
	 */
	private RColorSwatchesButton createColorSwatchesButton() {
		RColorSwatchesButton button = new RColorSwatchesButton();
		button.addPropertyChangeListener(RColorSwatchesButton.COLOR_CHANGED_PROPERTY, this);
		return button;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doApplyImpl(Frame owner) {

		RunPlugin plugin = (RunPlugin) getPlugin();
		RunWindow window = plugin.getDockableWindow();

		window.setForeground(RunTextArea.STYLE_ERRORLINK, errorlinkButton.getColor());
		window.setForeground(RunTextArea.STYLE_STDIN, stdinButton.getColor());
		window.setForeground(RunTextArea.STYLE_STDOUT, stdoutButton.getColor());
		window.setForeground(RunTextArea.STYLE_STDERR, stderrButton.getColor());
		
		plugin.setClearOutput(clearOutputCB.isSelected());

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
		return stdoutLabel;
	}

	/**
	 * Returns whether something on this panel is NOT set to its default value.
	 * 
	 * @return Whether some property in this panel is NOT set to its default
	 *         value.
	 */
	private boolean notDefaults() {
		return  !RunTextArea.DEFAULT_STDOUT_FG.equals(stdoutButton
						.getColor())
				|| !RunTextArea.DEFAULT_STDERR_FG.equals(stderrButton
						.getColor())
				|| !RunTextArea.DEFAULT_STDIN_FG.equals(stdinButton
						.getColor())
				|| !RunTextArea.DEFAULT_ERRORLINK_FG
						.equals(errorlinkButton.getColor())
				|| !clearOutputCB.isSelected();
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

		stdoutButton.setColor(RunTextArea.DEFAULT_STDOUT_FG);
		stderrButton.setColor(RunTextArea.DEFAULT_STDERR_FG);
		stdinButton.setColor(RunTextArea.DEFAULT_STDIN_FG);
		errorlinkButton.setColor(RunTextArea.DEFAULT_ERRORLINK_FG);
		
		clearOutputCB.setSelected(true);

	}

	/**
	 * {@inheritDoc}
	 */
	protected void setValuesImpl(Frame owner) {
		RunPlugin plugin = (RunPlugin) getPlugin();
		RunWindow window = plugin.getDockableWindow();
		
		stdoutButton.setColor(window.getForeground(RunTextArea.STYLE_STDOUT));
		stderrButton.setColor(window.getForeground(RunTextArea.STYLE_STDERR));
		stdinButton.setColor(window.getForeground(RunTextArea.STYLE_STDIN));
		errorlinkButton.setColor(window.getForeground(RunTextArea.STYLE_ERRORLINK));
		
		clearOutputCB.setSelected(plugin.getClearOutput());
	}


}