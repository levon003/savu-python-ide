/*
 * 11/14/2003
 *
 * ToolBar.java - Toolbar used by RText.
 * Copyright (C) 2003 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.FocusManager;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;

import org.fife.ui.CustomizableToolBar;
import org.fife.ui.SavuScrollbarUI;
import org.fife.ui.rtextarea.IconGroup;
import org.fife.ui.rtextarea.RTextArea;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.hifi.HiFiButtonUI;
import com.jtattoo.plaf.hifi.HiFiComboBoxUI;
import com.jtattoo.plaf.hifi.HiFiDefaultTheme;
import com.jtattoo.plaf.noire.NoireDefaultTheme;


/**
 * The toolbar used by {@link Savu}.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
public class ToolBar extends CustomizableToolBar {
	private static final long serialVersionUID = 1L;
	
	//Constants used to control the display behavior of the arguments box
	private final static String EXAMPLE_ARG = "arg1 arg2 arg3 arg4 arg5 arg6";
	private final static String INITIAL_ARGS_LABEL = "Arguments";
	private final static Color INITIAL_ARGS_LABEL_COLOR = Color.DARK_GRAY;
	
	private JButton newButton;
	private JButton openButton;
	private JButton saveButton;
	private JButton saveAllButton;
	private JButton closeButton;
	private JButton printButton;
	private JButton printPreviewButton;
	private JButton cutButton;
	private JButton copyButton;
	private JButton pasteButton;
	private JButton deleteButton;
	private JButton findButton;
	private JButton findNextButton;
	private JButton replaceButton;
	private JButton replaceNextButton;
	private JButton undoButton;
	private JButton redoButton;
	
	//Added by PyDE
	private JButton runButton;
	private JButton debugButton;
	private JButton stopButton; 
	private JComboBox<String> argumentsBox;
	private boolean displayArgumentsLabel = true;
	
	private Savu owner;
	private boolean mouseInNewButton;

	/**
	 * Creates the tool bar.
	 * 
	 * @param title
	 *            The title of this toolbar when it is floating.
	 * @param rtext
	 *            The main application that owns this toolbar.
	 * @param mouseListener
	 *            The status bar that displays a status message when the mouse
	 *            hovers over this toolbar.
	 */
	public ToolBar(String title, Savu rtext, StatusBar mouseListener) {

		super(title);
		
		this.owner = rtext;
		// Add the standard buttons.
		setBackground(Savu.MAIN_BACKGROUND_COLOR);
		newButton = createButton(rtext.getAction(Savu.NEW_ACTION));
		configure(newButton, mouseListener);

		newButton.setUI(new SavuButtonUI());
	    newButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
	    //newButton.setSize(new Dimension(2,2));
	    //newButton.setPreferredSize(new Dimension(2,2)); 
		
		add(newButton);
		NewButtonListener listener = new NewButtonListener();
		newButton.addFocusListener(listener);
		newButton.addMouseListener(listener);
		openButton = createButton(rtext.getAction(Savu.OPEN_ACTION));
		configure(openButton, mouseListener);
		
		openButton.setUI(new SavuButtonUI());
	    openButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(openButton);

		saveButton = createButton(rtext.getAction(Savu.SAVE_ACTION));
		configure(saveButton, mouseListener);
		saveButton.setUI(new SavuButtonUI());
	    saveButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(saveButton);

		/*saveAllButton = createButton(rtext.getAction(RText.SAVE_ALL_ACTION));
		configure(saveAllButton, mouseListener);
		add(saveAllButton);*/

		/*closeButton = createButton(rtext.getAction(RText.CLOSE_ACTION));
		configure(closeButton, mouseListener);
		add(closeButton);*/

		addSeparator();

		/*printButton = createButton(rtext.getAction(RText.PRINT_ACTION));
		configure(printButton, mouseListener);
		add(printButton);

		printPreviewButton = createButton(rtext
				.getAction(RText.PRINT_PREVIEW_ACTION));
		configure(printPreviewButton, mouseListener);
		add(printPreviewButton);

		addSeparator();*/

		/*cutButton = createButton(RTextArea.getAction(RTextArea.CUT_ACTION));
		configure(cutButton, mouseListener);
		add(cutButton);

		copyButton = createButton(RTextArea.getAction(RTextArea.COPY_ACTION));
		configure(copyButton, mouseListener);
		add(copyButton);

		pasteButton = createButton(RTextArea.getAction(RTextArea.PASTE_ACTION));
		configure(pasteButton, mouseListener);
		add(pasteButton);*/

		/*deleteButton = createButton(RTextArea
				.getAction(RTextArea.DELETE_ACTION));
		configure(deleteButton, mouseListener);
		add(deleteButton);*/

		//addSeparator();

		findButton = createButton(rtext.getAction(Savu.FIND_ACTION));
		configure(findButton, mouseListener);
		findButton.setUI(new SavuButtonUI());
	    findButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(findButton);
/*
		findNextButton = createButton(rtext.getAction(Savu.FIND_NEXT_ACTION));
		configure(findNextButton, mouseListener);
		add(findNextButton);
*/
		replaceButton = createButton(rtext.getAction(Savu.REPLACE_ACTION));
		configure(replaceButton, mouseListener);
		replaceButton.setUI(new SavuButtonUI());
	    replaceButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(replaceButton);
/*
		replaceNextButton = createButton(rtext
				.getAction(Savu.REPLACE_NEXT_ACTION));
		configure(replaceNextButton, mouseListener);
		add(replaceNextButton);
*/
		addSeparator();

		undoButton = createButton(RTextArea.getAction(RTextArea.UNDO_ACTION));
		configure(undoButton, mouseListener);
		// Necessary to keep button size from changing when undo text changes.
		undoButton.putClientProperty("hideActionText", Boolean.TRUE);
		undoButton.setUI(new SavuButtonUI());
	    undoButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(undoButton);

		redoButton = createButton(RTextArea.getAction(RTextArea.REDO_ACTION));
		configure(redoButton, mouseListener);
		// Necessary to keep button size from changing when undo text changes.
		redoButton.putClientProperty("hideActionText", Boolean.TRUE);
		redoButton.setUI(new SavuButtonUI());
	    redoButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(redoButton);
		
		//Added by PyDE
		addSeparator();
		
		
		runButton = createButton(rtext.getAction(Savu.RUN_ACTION));
		runButton.setToolTipText("<html>Run <br/>Runs the Python file open in the current tab.</html>");
		configure(runButton, mouseListener);
		runButton.setUI(new SavuButtonUI());
	    runButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(runButton);
		
		debugButton = createButton(rtext.getAction(Savu.DEBUG_ACTION));
		debugButton.setToolTipText("<html>Debug<br/>Runs the Python file open in the current tab in debug mode.</html>");
		configure(debugButton, mouseListener);
		debugButton.setUI(new SavuButtonUI());
	    debugButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(debugButton);
		
		stopButton = createButton(rtext.getAction(Savu.STOP_ACTION)); 
		stopButton.setToolTipText("<html>Stop<br/>Stops the python file currently running.</html>");
		configure(stopButton, mouseListener);
		stopButton.setUI(new SavuButtonUI());
	    stopButton.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		add(stopButton); 

		addSeparator();
		
		argumentsBox = createArgumentsBox();
		argumentsBox.setToolTipText("<html>Set your command line arguments here, if needed.<br/>" +
				"Each argument should be separated by a space, like this:<br/> arg1 arg2 arg3</html>");
		
		argumentsBox.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		argumentsBox.setBorder(BorderFactory.createLineBorder(Savu.ACCENT_BACKGROUND_COLOR, 1));
		
		add(argumentsBox);
		//COLORCHANGE
		//Border buttonsBord = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "", 0, 0, null, Color.white);
		
		//setBorder(buttonsBord);

		
		// Make the toolbar have the right-click customize menu.
		makeCustomizable();
		setSize(this); 

	}
	
	protected void setSize(Container C) {
		for (int i = 0; i < C.getComponentCount(); ++i) {
			if (C.getComponent(i) instanceof JButton) {
				JButton b = (JButton) C.getComponent(i);
				b.setSize(new Dimension(45, 35));
				b.setPreferredSize(new Dimension(45,35));
				b.setMaximumSize(new Dimension(45, 35));
				Border c = BorderFactory.createSoftBevelBorder(1);
				b.setBorder(c);
			}
		}
	}

	/**
	 * Creates and returns the arguments box, setting up all of its functionality and sizing.
	 * @return The JComboBox itself
	 */
	protected JComboBox<String> createArgumentsBox() {
		final JComboBox<String> argsBox = new JComboBox<String>();
		argsBox.setUI(new HiFiComboBoxUI());
		argsBox.setEditable(true);
		argsBox.setToolTipText(INITIAL_ARGS_LABEL);
		argsBox.getAccessibleContext().setAccessibleDescription("A textbox containing arguments that will be applied when running the current file.");

		//Set the size of the argument box to a size based on the rendering of an example arg-string in its Font
		argsBox.setPrototypeDisplayValue(EXAMPLE_ARG);
		FontMetrics metrics = argsBox.getFontMetrics(argsBox.getFont());
		int width = metrics.stringWidth(EXAMPLE_ARG);
		argsBox.setMaximumSize(new Dimension(width + 10, argsBox.getPreferredSize().height));
		
		//Initialize the text box with a default label
		final JTextComponent editorComponent = (JTextComponent) argsBox.getEditor().getEditorComponent();


		//TO COLORCHANGE ARGTEXT
		final Color defaultColor = Color.WHITE;
		editorComponent.setForeground(Color.LIGHT_GRAY);
		editorComponent.setBackground(Savu.ACCENT_BACKGROUND_COLOR);
		argsBox.setSelectedItem(INITIAL_ARGS_LABEL);
		
		//Delete the INITIAL_ARGS_LABEL string and set the text back to the default color whenever it gets focus
		//Reinstate it whenever the box becomes empty and loses focus
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				if (editorComponent.hasFocus() && displayArgumentsLabel) {
					//If we are displaying the initial label and we get focus, clear it so the user can type
					editorComponent.setForeground(defaultColor);
					argsBox.setSelectedItem(""); //Clear the arguments box
					displayArgumentsLabel = false;
				} else if (e.getOldValue() != null && e.getOldValue() == editorComponent) {
					//If the arguments box is empty and has now lost focus, replace the initial label
					if (editorComponent.getText().equals("")) {
						//COLORCHANGE
						editorComponent.setForeground(Color.LIGHT_GRAY);
						argsBox.setSelectedItem(INITIAL_ARGS_LABEL); //Clear the arguments box
						displayArgumentsLabel = true;
					}
				}
			}
		});
		
		

		return argsBox;
	}
	
	protected void recColorize(Container c, Color background, Color foreground) {
		c.setBackground(background);
		c.setForeground(foreground);
		for (int i = 0; i < c.getComponentCount(); i++) {
			if (c.getComponent(i) instanceof JPanel || c.getComponent(i) instanceof JCheckBox) {
				recColorize((Container) c.getComponent(i), background, foreground);
			}
		}
	}
	
	
	/**
	 * Keeps the toolbar's arguments combo-box updated with user-entered arguments.  Should be called each time the user attempts to run a file.
	 * 
	 * @param recent The argument string most recently used.
	 */
	public void updateArgumentHistory(String recent) { //TODO make the argument history hugely more robust.
		if (recent != "") {
			for (int i = 0; i < argumentsBox.getItemCount(); i++) {
				String str = argumentsBox.getItemAt(i);
				if (str.equals(recent)) {
					argumentsBox.removeItemAt(i);
					argumentsBox.insertItemAt(recent, 0);
					argumentsBox.setSelectedIndex(0);
					return;
				}
			}
			argumentsBox.insertItemAt(recent, 0);
			argumentsBox.setSelectedIndex(0);
		}
	}
	
	/**
	 * 
	 * @return The currently selected args as they appear in the textbox.
	 */
	public String getCurrentArgs() {
		if (displayArgumentsLabel || !argumentsBox.isVisible()) { //Only return arguments if the user has entered some and the component is on the toolbar
			return "";
		}
		String toReturn = (String) argumentsBox.getSelectedItem();
		if (toReturn == null) {
			return "";
		}
		return toReturn;
	}
	
	/**
	 * Returns the run button for use by the tutorial.
	 */
	public JButton getRunButton() {
		return runButton;
	}
	
	/**
	 * Returns the debug button for use by the tutorial.
	 */
	public JButton getDebugButton() {
		return debugButton;
	}
	
	public JButton getStopButton() {
		return stopButton;
	}
	
	public JComboBox<String> getArgumentsBox(){
		return this.argumentsBox;
	}
	
	/**
	 * Sets the Stop button in the toolbar as enabled or disabled.
	 * @param b
	 */
	public void enableStopButton(Boolean b) {
		stopButton.setEnabled(b); 
	}
	
	/**
	 * Checks whether the current icon group has large icons, and if it does,
	 * uses these large icons for the toolbar.
	 */
	void checkForLargeIcons() {
		IconGroup group = owner.getIconGroup();
		if (group.hasSeparateLargeIcons()) {
			Icon icon = group.getLargeIcon("new");
			newButton.setIcon(icon);
			icon = group.getLargeIcon("open");
			openButton.setIcon(icon);
			icon = group.getLargeIcon("save");
			saveButton.setIcon(icon);
			//icon = group.getLargeIcon("saveall");
			//saveAllButton.setIcon(icon);
			//icon = group.getLargeIcon("close");
			//closeButton.setIcon(icon);
			//icon = group.getLargeIcon("print");
			//printButton.setIcon(icon);
			//icon = group.getLargeIcon("printpreview");
			//printPreviewButton.setIcon(icon);
			//icon = group.getLargeIcon("cut");
			//cutButton.setIcon(icon);
			//icon = group.getLargeIcon("copy");
			//copyButton.setIcon(icon);
			//icon = group.getLargeIcon("paste");
			//pasteButton.setIcon(icon);
			//icon = group.getLargeIcon("delete");
			//deleteButton.setIcon(icon);
			icon = group.getLargeIcon("find");
			findButton.setIcon(icon);
			//icon = group.getLargeIcon("findnext");
			//findNextButton.setIcon(icon);
			//icon = group.getLargeIcon("replace");
			//replaceButton.setIcon(icon);
			//icon = group.getLargeIcon("replacenext");
			//replaceNextButton.setIcon(icon);
			icon = group.getLargeIcon("undo");
			undoButton.setIcon(icon);
			icon = group.getLargeIcon("redo");
			redoButton.setIcon(icon);
			icon = group.getLargeIcon("run"); //Added by PyDE
			runButton.setIcon(icon);
			icon = group.getLargeIcon("debug");
			debugButton.setIcon(icon);
			
		}
	}

	/**
	 * Configures the specified menu bar button.
	 * 
	 * @param button
	 *            The button.
	 * @param mouseListener
	 *            A mouse listener to add.
	 */
	private final void configure(JButton button, StatusBar mouseListener) {
		// Bug in Windows 1.4 and some 1.5 JRE's - changing LAF to
		// windows LAF causes margin to become much too wide.
		if (owner.getOS() == Savu.OS_WINDOWS) {
			button.setMargin(new Insets(0, 0, 0, 0));
		}
		button.addMouseListener(mouseListener);
	}

	/**
	 * This class keeps track of whether or not the mouse position is inside the
	 * "New" button's bounds. This is part of an elaborate hack to fix what
	 * seems to be a focus issue (bug) in JRE1.4. Note that in JRE 1.5, it does
	 * not happen.
	 * 
	 * What happens is this: Whenever the user clicks on a tab to change the
	 * current document, the focused component gets switched not to the text
	 * area corresponding to the tab they clicked, but rather the "New" button
	 * on the toolbar (actually, it gets switched to the text area, then to the
	 * New button). This behavior stops if the user changes the Look and Feel.
	 * 
	 * This is the second part of the elaborate focus hack. Whenever the New
	 * toolbar button gets focus, we check to see if the mouse position is
	 * inside the New button's bounds. If it isn't, then we assume that the
	 * event was fired as a result of the situation described in
	 * NewButtonMouseListener's blurb, and so we give focus back to the current
	 * text area.
	 */
	private class NewButtonListener extends MouseAdapter implements
			FocusListener {

		public void focusGained(FocusEvent e) {
			RTextEditorPane textArea = owner.getMainView().getCurrentTextArea();
			if (!mouseInNewButton && textArea != null) {
				textArea.requestFocusInWindow();
			}
		}

		public void focusLost(FocusEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
			mouseInNewButton = true;
		}

		public void mouseExited(MouseEvent e) {
			mouseInNewButton = false;
		}

	}

}