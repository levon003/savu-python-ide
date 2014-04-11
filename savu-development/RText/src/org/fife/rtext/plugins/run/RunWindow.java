/*
d * 12/17/2010
 *
 * RunWindow.java - Text component for the run.
 * Copyright (C) 2010 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.run;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.fife.rtext.Savu;
import org.fife.rtext.RTextUtilities;
import org.fife.ui.RScrollPane;
import org.fife.ui.dockablewindows.DockableWindow;



/**
 * A dockable window that acts as a run output window.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RunWindow extends DockableWindow implements PropertyChangeListener {

	public static final String PROCESS_LABEL_DEFAULT = "<none>";
	public static final float PROCESS_LABEL_FONT_SIZE = 10f;
	public static final Color PROCESS_LABEL_COLOR = Color.LIGHT_GRAY;
	public static final Color PROCESS_LABEL_COLOR_ERROR = new Color(243, 103, 103);
	
	private static final Color BACKGROUND_COLOR = new Color(50,50,40); 
	private static final int FORCE_WINDOW_HEIGHT = 100;
	
	private CardLayout cards;
	private JPanel mainPanel;
	private OutputTextArea outputTextArea;

	private JToolBar toolbar;
	private Savu owner; 

	private JLabel currentProcessPathLabel;
	private String programOutputLabelStart;
	
	public RunWindow(Savu app, RunPlugin plugin) {
		owner = (Savu) app; 
		setDockableWindowName(plugin.getString("DockableWindow.Title"));
		setIcon(plugin.getPluginIcon());
		setPosition(DockableWindow.BOTTOM);
		setLayout(new BorderLayout());

		add(Box.createRigidArea(new Dimension(1, FORCE_WINDOW_HEIGHT)), BorderLayout.EAST);
		setBackground(Savu.ACCENT_BACKGROUND_COLOR);
		
		// Create the main panel, containing the shells.
		cards = new CardLayout();
		mainPanel = new JPanel(cards);
		add(mainPanel);
		outputTextArea = new OutputTextArea(plugin);
		setPrimaryComponent(outputTextArea);
		outputTextArea.addPropertyChangeListener(RunTextArea.PROPERTY_PROCESS_RUNNING, this);
		RScrollPane sp = new RScrollPane(outputTextArea);
		//COLORCHANGE
		sp.setBackground(new Color (40,40,30));
		sp.getVerticalScrollBar().setBackground(new Color(40,40,35));
		sp.getHorizontalScrollBar().setBackground(new Color(40,40,35));

		RTextUtilities.removeTabbedPaneFocusTraversalKeyBindings(sp);
		mainPanel.add(sp, "System");
		
		// Create a "toolbar" for the shells.
		toolbar = new JToolBar();
		toolbar.setFloatable(false);
		//COLORCHANGE - toolbar in run window
		toolbar.setBackground(Savu.ACCENT_BACKGROUND_COLOR); 
		toolbar.setForeground(Color.WHITE); 

		programOutputLabelStart = plugin.getString("ProgramOutput");
		currentProcessPathLabel = new JLabel(programOutputLabelStart + PROCESS_LABEL_DEFAULT);
		//COLORCHANGE - jlabel in run window
		currentProcessPathLabel.setForeground(Color.LIGHT_GRAY); 
		
		Box temp = new Box(BoxLayout.LINE_AXIS);
		temp.add(currentProcessPathLabel);
		currentProcessPathLabel.setFont(currentProcessPathLabel.getFont().deriveFont(PROCESS_LABEL_FONT_SIZE)); //Set size of label.
		//currentProcessPathLabel.setForeground(PROCESS_LABEL_COLOR);
		currentProcessPathLabel.setBackground(new Color(50,50,40));
		temp.add(Box.createHorizontalStrut(5));
		temp.add(Box.createHorizontalGlue());
		JPanel temp2 = new JPanel(new BorderLayout());
		temp2.add(temp, BorderLayout.LINE_START);
		//COLORCHANGE
		temp2.setBackground(Savu.ACCENT_BACKGROUND_COLOR); 
		toolbar.add(temp2);
		toolbar.add(Box.createHorizontalGlue());

		add(toolbar, BorderLayout.NORTH);

	}
	
	public RunTextArea getTextArea()
	{
		return outputTextArea;
	}

	/**
	 * Sets the font to use in this RunWindow's text area.
	 * @param f The new font to use in this RunWindow's text area.
	 */
	public void setTextAreaFont(Font f) {
		this.getTextArea().setFont(f);
	}
	
	/**
	 * Sets the label immediately above the run output to a new string.
	 * 
	 * @param newLabel The filepath of the current process to display.
	 */
	public void setCurrentProcessLabel(final String newLabel) {
		if (SwingUtilities.isEventDispatchThread()) {
			currentProcessPathLabel.setText(programOutputLabelStart+newLabel);
			currentProcessPathLabel.setForeground(PROCESS_LABEL_COLOR);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					currentProcessPathLabel.setText(programOutputLabelStart+newLabel);
					currentProcessPathLabel.setForeground(PROCESS_LABEL_COLOR);
				}
			});
		}
	}
	
	/**
	 * Sets the label above the run output to an error message.
	 * 
	 * @param newLabel The error message to display.
	 */
	public void setCurrentProcessLabelToError(final String newLabel) {
		if (SwingUtilities.isEventDispatchThread()) {
			currentProcessPathLabel.setText(newLabel);
			currentProcessPathLabel.setForeground(PROCESS_LABEL_COLOR_ERROR);
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					currentProcessPathLabel.setText(newLabel);
					currentProcessPathLabel.setForeground(PROCESS_LABEL_COLOR_ERROR);
				}
			});
		}
	}

	/**
	 * Clears any text from all runs.
	 */
	public void clearRuns() {
		outputTextArea.clear();
	}



	/**
	 * Called whenever a process starts or completes.
	 */
	public void propertyChange(PropertyChangeEvent e) {
		//Unneeded
	}


	/**
	 * Returns the color used for a given type of text in the run output.
	 */
	public Color getForeground(String style) {
		Style s = outputTextArea.getStyle(style);
		return StyleConstants.getForeground(s);
	}
	
	/**
	 * Sets the color used for a given type of text in the run output.
	 *
	 * @param style The style; e.g. {@link RunTextArea#STYLE_STDOUT}.
	 * @param fg The new foreground color to use, or <code>null</code> to
	 *        use the system default foreground color.
	 * @see #getForeground(String)
	 */
	public void setForeground(String style, Color fg) {
		setForegroundImpl(style, fg, outputTextArea);
	}

	/**
	 * Sets a color for a given type of a text in a single run.
	 *
	 * @param style
	 * @param fg
	 * @param textArea
	 */
	private static final void setForegroundImpl(String style, Color fg,
									RunTextArea textArea) {
		Style s = textArea.getStyle(style);
		if (s!=null) {
			if (fg!=null) {
				StyleConstants.setForeground(s, fg);
			}
			else {
				s.removeAttribute(StyleConstants.Foreground);
			}
		}
	}

}