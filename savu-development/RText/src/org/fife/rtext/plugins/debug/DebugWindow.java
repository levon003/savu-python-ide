/*
 * 12/17/2010
 *
 * DebugWindow.java - Text component for the debug.
 * Copyright (C) 2010 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.debug;
import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.hifi.HiFiDefaultTheme;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import org.fife.rtext.Savu;
import org.fife.rtext.SavuButtonUI;
import org.fife.rtext.actions.StopAction;
import org.fife.rtext.graphics.common_icons.DebugIconLoader;
import org.fife.ui.SavuScrollbarUI;
import org.fife.ui.dockablewindows.DockableWindow;
import org.fife.ui.rtextarea.RTextArea;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;


/**
 * A dockable window that acts as a debug.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class DebugWindow extends DockableWindow implements Observer {

	private DebugPlugin plugin;
	
	private CardLayout cards;
	private JPanel mainPanel;
	
	private JPanel debugHelpPopup;

	private JToolBar debugButtons;
	
	private JButton stepOver;
	private JButton stepInto;
	private JButton continueRunning;
	private JButton stopRunning;
	private JButton debugHelp;

	
	private JXTreeTable treeTable;
	private PythonVariableTreeTableModel  treeModel;
	private Icon complexIcon;
	private Icon primitiveIcon; 
	private Icon debugIcon;
	
	private FontHighlighter fontControl;
	
	public DebugWindow(Savu app, DebugPlugin plugin) {
		DebugIconLoader dl = new DebugIconLoader(); 
		this.plugin = plugin;
		setDockableWindowName(plugin.getString("DockableWindow.Title"));
		setIcon(plugin.getPluginIcon());
		setPosition(DockableWindow.BOTTOM);
		setLayout(new BorderLayout());
		// Create the main panel
		cards = new CardLayout();
		mainPanel = new JPanel(cards);
		//COLORCHANGE
		//mainPanel.setBackground(new Color(70,70,60));
		add(mainPanel);
 
		// Create a "toolbar".
		debugButtons = new JToolBar();
		debugButtons.setFloatable(false);
		//COLORCHANGE
		debugButtons.setBackground(Savu.MAIN_BACKGROUND_COLOR);
		Box temp = new Box(BoxLayout.LINE_AXIS);
		temp.setBackground(Savu.MAIN_BACKGROUND_COLOR);
		
		stepOver = new JButton(dl.getSOI());
		stepOver.setEnabled(false);
		stepOver.setToolTipText("<html>Step Over<br/>" +
				"Click this to to perform the current line of code<br/>" +
				"and advance to the next line.</html>");
		stepOver.addActionListener(new StepOverListener());
		
		stepOver.setUI(new SavuButtonUI());
	    stepOver.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		
		stepInto = new JButton(dl.getSII());
		stepInto.setEnabled(false);
		stepInto.setToolTipText("<html>Step Into<br/>" +
				"Click this to jump into the user defined function<br/>" +
				"on the current line and pause. If no user defined<br/>" +
				"function is present, functions like Step Over.</html>");
		stepInto.addActionListener(new StepIntoListener());
		
		stepInto.setUI(new SavuButtonUI());
	    stepInto.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		
		continueRunning = new JButton(dl.getCR());
		continueRunning.setEnabled(false);
		continueRunning.setToolTipText("<html>Continue Running<br/>" +
				"Click this button to continue running through the<br/>" +
				"program until another breakpoint or the end of the<br>" +
				"file is reached.</html>");
		continueRunning.addActionListener(new ContinueRunningListener());
		
		continueRunning.setUI(new SavuButtonUI());
	    continueRunning.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);

		StopAction stopAction = new StopAction(app, DebugPlugin.msg, dl.getSI());
		stopRunning = new JButton(stopAction);
		stopRunning.setToolTipText("<html>Stop<br/>" +
				"Click this button to end the Python program that<br/>" +
				"is currently running immediately.</html>");
		stopRunning.setText(null);
		
		stopRunning.setUI(new SavuButtonUI());
	    stopRunning.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		
		debugHelp = new JButton("HELP");
		debugHelp.addActionListener(new DebugHelpListener());
		
		
		complexIcon = dl.getCI();
		primitiveIcon = dl.getPI(); 
		debugIcon = dl.getDI(); 
		
		temp.add(stepOver);
		temp.add(stepInto);
		temp.add(continueRunning);
		temp.add(stopRunning);
		setSize(temp);
		//TODO: Implement this button (uncomment it too)
		if(app.isTutorialModeEnabled()){
			//temp.add(debugHelp);
		}
		
		JPanel temp2 = new JPanel(new BorderLayout());
		
		//COLORCHANGE
		temp2.setBackground(Savu.MAIN_BACKGROUND_COLOR);
		//Border buttonsBord = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.WHITE), "Debug Control", 0, 0, null, Color.white);
		
		//temp2.setBorder(buttonsBord);
		temp2.add(temp, BorderLayout.LINE_START);
		debugButtons.add(temp2);
		debugButtons.add(Box.createHorizontalGlue());
		
		add(debugButtons, BorderLayout.NORTH);

		//Initialize the tree tables
		treeModel = new PythonVariableTreeTableModel();
		treeTable = new JXTreeTable(treeModel);
		setIcons(treeTable);
		treeModel.setParentTable(treeTable);
		treeTable.setRootVisible(false);
		
		//Initialize the font
		fontControl = new FontHighlighter(new HighlightPredicate() {
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
				return true; //Update the font in every cell
			}
		}, treeTable.getFont());
		Font treeFont;
		try { 
			treeFont = app.getMainView().getCurrentTextArea().getFont();
		} catch (NullPointerException ex) {
			treeFont = RTextArea.getDefaultFont();
		}
		setTreeTableFont(treeFont);
		treeTable.addHighlighter(fontControl);
		//COLORCHANGE
		treeTable.setBackground(Savu.OUTPUT_AREA_BACKGROUND_COLOR);
		
		//Set selection settings
		treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		treeTable.setSelectionBackground(treeTable.getBackground());
		treeTable.setSelectionForeground(treeTable.getForeground());
		treeTable.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				treeTable.clearSelection();
			}
		});
		
		//Add scrollbars
		JScrollPane scrollPane = new JScrollPane(treeTable);
		treeTable.setForeground(Color.WHITE); 
		
		if (System.getProperty("os.name").startsWith("Mac OS")) {
			treeTable.getTableHeader().setBackground(Savu.ACCENT_BACKGROUND_COLOR);
			treeTable.getTableHeader().setForeground(Color.WHITE);
		} else {
			treeTable.getTableHeader().setBackground(new Color(170, 170, 160));
			treeTable.getTableHeader().setForeground(Color.BLACK);
		}
		
		
		//COLORCHANGE
		scrollPane.setBackground(Savu.MAIN_BACKGROUND_COLOR);
		AbstractLookAndFeel.setTheme(new HiFiDefaultTheme());
		scrollPane.getVerticalScrollBar().setUI(new SavuScrollbarUI());
		scrollPane.getHorizontalScrollBar().setUI(new SavuScrollbarUI());
		scrollPane.getVerticalScrollBar().setBackground(new Color(40, 40, 35));
		scrollPane.getHorizontalScrollBar().setBackground(new Color(40, 40, 35));
		
		add(scrollPane, BorderLayout.CENTER);
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
	
	public Icon getDebugIcon() {
		return debugIcon;
	}
		
	public JButton getStepOverButton(){
		return this.stepOver;
	}
	

	public JButton getStepIntoButton(){
		return this.stepInto;
	}
	
	public JButton getContinueButton(){
		return this.continueRunning;
	}
	
	
	public JButton getHelpButton(){
		return this.debugHelp;
	}
	
	/**
	 * Uses the appropriate icons loaded for use with a variable tree table and inserts them into the given table.
	 * @param table
	 */
	public void setIcons(JXTreeTable table) {
		table.setClosedIcon(complexIcon);
		table.setOpenIcon(complexIcon);
		table.setLeafIcon(primitiveIcon);
		table.setCollapsedIcon(complexIcon);		
	}
	
	/**
	 * Sets the font size to use in the variable cells within the tree table. Bounds it with locally set size bounds.
	 * @param font The new font to use.
	 */
	public void setTreeTableFont(Font font) {
		int newSize = font.getSize();
		if (newSize >= FontHighlighter.MIN && newSize <= FontHighlighter.MAX) {
			Font currFont = fontControl.getFont();
			Font newFont = currFont.deriveFont((float) newSize);
			fontControl.setFont(newFont);
		}
	}
	
	/**
	 * 
	 * @return The tree table model used by the variable tree table.
	 */
	public PythonVariableTreeTableModel getTreeTableModel() {
		return treeModel;
	}
	
	/**
	 * 
	 * @param enabled Whether the Stop button in the Debug window should be active or not.
	 */
	public void setStopButtonEnabled(boolean enabled) {
		stopRunning.setEnabled(enabled);
	}
	
	/**
	 * Redraws the debug toolbar with or without the help button. Should only be called when Tutorial Mode is toggled
	 * May not be necessary depending on the implementation of the tutorial
	 * @param helpState the state that the debug help button will be changed to
	 */
	public void toggleDebugHelp(boolean debugHelpWanted){
		if(debugHelpWanted){
			JPanel x = ((JPanel)this.debugButtons.getComponent(0));
			Box y = (Box) x.getComponent(0);
			JButton z = (JButton) y.getComponent(4);
			y.remove(4);
			x.revalidate();
		}
		else{
			
		}
	
	}
	
	private class StepOverListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			PythonDebugger d = (PythonDebugger) plugin.getCurrentProcess();
			if (d.isReady()) {
				d.stepOver();
				plugin.removeHighlight();
			}
		}
	}
	
	private class StepIntoListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			PythonDebugger d = (PythonDebugger) plugin.getCurrentProcess();
			if (d.isReady()) {
				d.stepIn();
				plugin.removeHighlight();
			}
		}
	}
	
	private class ContinueRunningListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			PythonDebugger d = (PythonDebugger) plugin.getCurrentProcess();
			if (d.isReady()) {
				d.continueRun();
				plugin.removeHighlight();
			}
		}
	}
	
	/**
	 * launches the debug tutorial
	 * @author nathanroberts
	 *
	 */
	private class DebugHelpListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			//Create and set up the window.
	        JFrame frame = new JFrame("Debug Help");

	        //TODO: Format and fill in
	 
	        JLabel label = new JLabel("Debug");
	        label.setPreferredSize(new Dimension(300, 500));
	        frame.getContentPane().add(label, BorderLayout.CENTER);
	 
	        JButton close = new JButton("Close (Not Implemented)");

	        //debugTitle.setPreferredSize(new Dimension(175, 100));
	        frame.getContentPane().add(close, BorderLayout.SOUTH);
	        
	        //TODO: Locate this frame correctly
	        frame.setLocationRelativeTo(null);
	        //Display the window.
	        frame.pack();
	        frame.setVisible(true);
		}
	}
	
	/**
	 * Update used to set stop button enabled or disabled.
	 */
	@Override
	public void update(Observable o, Object arg) {
		PythonDebugger d = (PythonDebugger) o;
		//By default, assume we can't use the debug buttons
		continueRunning.setEnabled(false);
		stepInto.setEnabled(false);
		stepOver.setEnabled(false);
		if (d.isRunning()) {
			plugin.getRText().setGlobalEditingLocked(true);
			if(d.isReady())
			{
				stepInto.setEnabled(true);
				stepOver.setEnabled(true);
				continueRunning.setEnabled(true);
				//Highlight current line
				CodePosition curLine = d.getCurrentCodePosition();
				try {
					plugin.highlightLine(curLine.lineno, curLine.filename);
				} catch (BadLocationException e) {
					// Panic!
				}
			}
		} else { //The debugger process has finished running
			plugin.getRText().setGlobalEditingLocked(false);
		}
		
	}
	
	/**
	 * This FontHighlighter, largely borrowed from the SwingX Highlighter documentation, is applied to the tree table to allow dynamic updating of the 
	 * tree table's font.  Unfortunately, updating the font size does not update the area allocated to each cell.
	 * @author levoniaz
	 *
	 */
	private class FontHighlighter extends AbstractHighlighter {
		 
		public static final int MAX = 16;
		public static final int MIN = 6;
		
	     private Font font;
	 
	     public FontHighlighter(HighlightPredicate predicate, Font font) {
	         super(predicate);
	         setFont(font);
	     }
	 
	     @Override
	     protected Component doHighlight(Component component,
	             ComponentAdapter adapter) {
	         component.setFont(font);
	         return component;
	     }
	     
	     public final void setFont(Font font) {
	        this.font = font;
	        fireStateChanged();
	     }
	     
	     public final Font getFont() {
	    	 return this.font;
	     }
	 
	 }
	
}