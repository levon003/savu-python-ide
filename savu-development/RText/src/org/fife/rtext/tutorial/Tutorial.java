package org.fife.rtext.tutorial;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.fife.rtext.AbstractMainView;
import org.fife.rtext.CurrentTextAreaEvent;
import org.fife.rtext.CurrentTextAreaListener;
import org.fife.rtext.RTextEditorPane;
import org.fife.rtext.Savu;
import org.fife.rtext.ToolBar;
import org.fife.rtext.plugins.debug.DebugPlugin;
import org.fife.rtext.plugins.run.PythonProcess;
import org.fife.rtext.plugins.run.RunPlugin;
import org.fife.rtext.actions.RunAction;
import org.fife.rtext.actions.DebugAction;
import org.fife.rtext.actions.ToggleBreakpointAction;
import org.fife.ui.app.InvalidPluginException;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.IconRowHeader;

public class Tutorial extends Thread {
	
	

	/**
	 * The file path to a python file that can be used by the tutorial. Should have a bug
	 * NOTE: If you change this file path, you will need to change it in
	 * Common.org.fife.ui.RecentFilesMenu.getShouldIgnoreFile(String fileFullPath)
	 */
	private static final String TUTORIAL_FILE_NAME_PART_1 = "lib/SavuTutorialPart1.py";
	/**
	 * The file path to a python file that can be used by the tutorial. Should be functioning python code
	 * NOTE: If you change this file path, you will need to change it in
	 * Common.org.fife.ui.RecentFilesMenu.getShouldIgnoreFile(String fileFullPath)
	 */
	private static final String TUTORIAL_FILE_NAME_PART_2 = "lib/SavuTutorialPart2.py";
	
	private Savu rtext;
	private ToolBar toolbar;
	private RunPlugin runPlug;
	private DebugPlugin debugPlug;
	
	private TutorialGlassPane view;
	private StepController stepController;
	
	private ChangedCurrentTextAreaListener maintainTutorialTextAreaListener;
	
	/**
	 * A list of the accelerators used by each action, so they can be restored during clean up
	 */
	HashMap<Action, KeyStroke> actionAccelerators;
	
	/**
	 * A reference to the Action listener attached to the run button, so it can be removed during clean up
	 */
	private ActionListener runActionListener;
	
	public Tutorial(Savu rtext) {
		this.rtext = rtext;
		this.rtext.setTutorial(this);
		toolbar = null;
		runPlug = null;
		debugPlug = null;
		stepController = new StepController();
		
	}
	
	@Override
	public void run() {
		if (getReferences()) {
			createAndShowGUI();
			runTutorial();
			cleanUp();
		}
	}
	
	/**
	 * Attempts to get most of the references that will be needed by the tutorial and store them in class variables.
	 */
	private boolean getReferences() {
		//Get the toolbar
		toolbar = (ToolBar) rtext.getToolBar();
		//Get the run plugin
		runPlug = RunAction.getRunPlugin(rtext);
		if (runPlug == null) {
			rtext.displayException(new InvalidPluginException("Could not find run plugin."));
			return false;
		}
		//Get the debug plugin
		debugPlug = DebugAction.getDebugPlugin(rtext);
		if (debugPlug == null) {
			rtext.displayException(new InvalidPluginException("Could not find debug plugin."));
			return false;
		}
		return true;
	}
	
	private void createAndShowGUI() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					addGlassPane();
			}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void cleanUp() {
		AbstractMainView mainView = rtext.getMainView();
		mainView.removeCurrentTextAreaListener(maintainTutorialTextAreaListener);
		mainView.getCurrentTextArea().enableEditing();
		toolbar.getRunButton().removeActionListener(runActionListener);
		//Stops the RunOuput panel from taking over the screen when the tutorial closes
		//This is not the case when it is quit out of
		this.runPlug.getDockableWindow().setPreferredSize(null);
		//Reenable the menu items
		MenuBar mb = rtext.getMenuBar();
		for (int i = 0; i < mb.getMenuCount(); i++) {
			Menu m = mb.getMenu(i);
			m.setEnabled(true);
		}
		//Restore the menu accelerators
		for (Object o : rtext.getActionKeys()) {
			String key = (String) o;
			Action a = rtext.getAction(key);
			a.putValue(Action.ACCELERATOR_KEY, actionAccelerators.get(a));
		}
		//Clean up the GUI
		view.cleanUp();
		
		//Stop any execution that may be occurring
		PythonProcess p = runPlug.getCurrentProcess();
		if (p != null && p.isRunning())
			p.stop();
		//remove Savu's reference to this tutorial
		this.rtext.setTutorial(null);
	}
	
	/**
	 * Instantiates a new glass pane and sets it visible and ready to receive commands. Requires EDT.
	 */
	private void addGlassPane() {
		TutorialGlassPane glassPane = new TutorialGlassPane(rtext, rtext.getTutorialPanel(), stepController, debugPlug);
		rtext.setGlassPane(glassPane);
		glassPane.setVisible(true);
		view = glassPane;
		//TODO ensure plugins are appropriately sized; they cannot be resized during the tutorial.
	}
	
	private void runTutorial() {
		//Stop any execution that may be occurring
		PythonProcess p = runPlug.getCurrentProcess();
		if (p != null && p.isRunning())
			p.stop();
		//Open the tutorial file on the EDT
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					rtext.openFile(TUTORIAL_FILE_NAME_PART_1);
				}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace(); //Seems bad
		}
		//Lock the text area from being edited or losing main-view focus during the course of the test
		lockTextArea(); 
		lockMenuBar(); //Lock the menu bar as well.
		
		ArrayList<TutorialStep> steps = getTutorialSteps();
		for (TutorialStep step : steps) {
			stepController.stepLock.lock();
			if(step.TheFileSwap){
				swapTutorialFiles();
			}
			String msg = "<html><b>Tutorial</b><br/>"+step.message.substring(6, step.message.length());
			view.setMessage(msg);
			if(step.isBreakpointStep){
				step.initializeBreakpointStep();
			}
			view.highlightComponent(step);
			view.setNextVisibility(step.drawNext);
			stepController.waitForNextStep();
			if (stepController.tutorialExited) {
				break;
			}
		}
	}
	
	/**
	 * "Locks" the text area by disabling editing and trying to switch back to it when the user switches away from the tutorial text area.
	 */
	private void lockTextArea() {
		AbstractMainView mainView = rtext.getMainView();
		RTextEditorPane pane = mainView.getCurrentTextArea();
		pane.disableEditing(); //Disallow editing until the tutorial is over
		maintainTutorialTextAreaListener = new ChangedCurrentTextAreaListener(pane);
		mainView.addCurrentTextAreaListener(maintainTutorialTextAreaListener);
	}
	
	/**
	 * "Locks" the menu bar by disabling the menus.
	 */
	private void lockMenuBar() {
		//Disable the menu bars, preventing clicking
		MenuBar mb = rtext.getMenuBar();
		for (int i = 0; i < mb.getMenuCount(); i++) {
			Menu m = mb.getMenu(i);
			m.setEnabled(false);
		}
		//Remove the accelerator keys for every action, temporarily removing all hotkeys
		actionAccelerators = new HashMap<Action, KeyStroke>();
		for (Object o : rtext.getActionKeys()) {
			String key = (String) o;
			Action a = rtext.getAction(key);
			actionAccelerators.put(a, (KeyStroke) a.getValue(Action.ACCELERATOR_KEY));
			a.putValue(Action.ACCELERATOR_KEY, null);
		}
	}
	
	/**
	 * Switches the current open file (set as TUTORIAL_FILE_NAME_PART_1) to
	 * the second tutorial file (set as TUTORIAL_FILE_NAME_PART_2)
	 * @author nathanroberts
	 */
	private void swapTutorialFiles(){
		rtext.getMainView().closeCurrentDocument();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					rtext.openFile(TUTORIAL_FILE_NAME_PART_2);
				}});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace(); //Seems bad (classic ZL coding standard)
		}
		
		lockTextArea(); 
	}
	

	
	/**
	 * Create all tutorial steps here.
	 * @return a list (in order) of tutorial steps
	 */
	private ArrayList<TutorialStep> getTutorialSteps() {
		ArrayList<TutorialStep> steps = new ArrayList<TutorialStep>();
		TutorialStep step = null;
		
		
		/*
		 * NOTE:
		 * All text in a JLabel (and apparently almost all Swing components)
		 * follows HTML conventions and not standard ASCII ones.
		 * If you need newlines, encase the entire string to display in <html></html> tags
		 * and use <br /> tags instead of \n characters.
		 * EX: "<html>I want a newline here. <br/> See! Newline!</html>"
		 */
		
		//Initialize a next button for use whenever relevant in following code
		final JButton nextButton = view.getNextButton();
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
            	stepController.finishedStep = true;
				stepController.alertStepFinished();
				//nextButton.removeActionListener(this);
			}
		});
		
		step = new TutorialStep("<html>Welcome to the Savu Tutorial! <br />" +
				"After completing this tutorial you should have a better idea of how to use the features in Savu for fun and profit. Please note that " +
				"during the tutorial, most of the buttons and options on screen will be disabled. Any buttons that you need to click or areas that you need " +
				"to look at will be indicated with a red box and an arrow. <br/>Please click Next Step to proceed.</html>", null, true, null);
		
		steps.add(step);
		
		
		final JButton runButton = toolbar.getRunButton();
		step = new TutorialStep("<html>We have already loaded a simple Python program for you. Things like opening and saving files work exactly the same as in other text editors." +
				" In Savu it is possible to run your code without having to switch to the terminal by clicking the Run Button at the top of the screen. <br />" +
				"Please click the Run Button to continue.</html>", runButton, ArrowOrientation.UP);
		runActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
            	stepController.finishedStep = true;
				stepController.alertStepFinished();
			}
		};
		runButton.addActionListener(runActionListener);
		steps.add(step);
		
		step = new TutorialStep("<html>The Output pane at the bottom of the screen will display all of the output from your program, including errors. " +
				"This file has an error in it. You can click the blue, underlined text in the error output and Savu will take you to the line that the error is on. " +
				"<br/>Click the error link to see what line the error is on, then click Next Step to proceed.</html>", runPlug.getDockableWindow(), true, ArrowOrientation.OUTPUT);
		steps.add(step);
		
		step = new TutorialStep("<html>The error has been fixed. Try running the code again using the same run button as before." +
				"<br/>Click the Run Button at the top of the screen to continue.</html>", runButton, ArrowOrientation.UP);

		step.initTheFileSwapStep();
		steps.add(step);
		
		step = new TutorialStep("<html>Without errors, the output from your code will appear normally in the Output pane."+
				"<br/>Click Next Step to proceed.</html>", runPlug.getDockableWindow(), true, ArrowOrientation.OUTPUT);
		steps.add(step);
		
		final JButton stopButton = toolbar.getStopButton();
		step = new TutorialStep("<html>If you need to stop a program while it is being executed, click the Stop Button. This can save you if you get caught in something like an infinite loop. Nothing is running right now, so it is disabled."
				+ "There is a stop button close to the run button at the top of the screen as well as in the debug panel. <br/>Click Next Step to proceed.</html>", stopButton, true, ArrowOrientation.UP);
		steps.add(step);
		
		step = new TutorialStep("<html>If you need to set command line arguments, you can do so in the Arguments box at the top of the top of the screen. " +
				"Simply click inside the box and enter your arguments, and then run the program like you would normally." +
				"<br/>Click Next Step to proceed.</html>", toolbar.getArgumentsBox(), true, ArrowOrientation.UP);
		steps.add(step);
		
		step = new TutorialStep("<html>One of the powerful tools in Savu is the Debugger. The Debugger allows you to pause your code while it is running, "
				+ "see how Python performs each line of your code individually, and check what values are assigned to variables. <br/>Click Next Step to proceed.</html>", null, true, null);
		steps.add(step);
		
		//BreakpointStep: The component to highlight is set to null here. It will be set to the appropriate IconRowHeader
		// when the file that we will be debugging is loaded
		step = new TutorialStep("<html>The first step to using the Debugger is to set a breakpoint. A breakpoint is a marker that tells " +
				"the Debugger where you would like it to pause while executing your program. When you start debugging, Savu will run your code " +
				"until it reaches a line with a breakpoint. When it does, it will pause and wait for you to tell it what to do next. You can place a breakpoint by clicking in the "
				+ "gray area to the left of the line numbers, on a line with Python code (no comments or blank lines). Once you place a breakpoint, you can delete it by clicking it again.<br/>"
				+ "Please place a breakpoint on line 7.</html>", null, ArrowOrientation.DOWN);
		
		step.setIsBreakpointSet(true);
		steps.add(step);
		
		
		
		final JButton debugButton = toolbar.getDebugButton();
		step = new TutorialStep("<html>Now that you have a breakpoint, click the Debug button in the top toolbar to run the program in Debug Mode. " +
				"Breakpoints are only effective when you are running in debug mode. Running normally (with the Run Button) ignores breakpoints.</html>", debugButton, ArrowOrientation.UP);
		debugButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
            	stepController.finishedStep = true;
				stepController.alertStepFinished();
				debugButton.removeActionListener(this);
			}
		});
		steps.add(step);
		
		step = new TutorialStep("<html>Notice how the line that you placed the breakpoint on is highlighted. This shows what line the Debugger " +
				"is paused on. All of the code before this line has been run by Python. <br/>Click Next Step to proceed.</html>", null, true, null);
		
		steps.add(step);
		
		step = new TutorialStep("<html>On the right side of the screen is the Debug panel. It contains the navigation controls for the " +
				"Debugger, as well as a table to display all of the values of the variables that have been set up to this line in the code. " +
				"Notice that variable a and variable b have values assigned to them, but variable c and variable d have not yet been set. <br/>" +
				"Click Next Step to proceed.</html>", this.debugPlug.getDockableWindow(), true, ArrowOrientation.RIGHT);
		
		steps.add(step);
		
		final JButton stepOverButton = debugPlug.getDockableWindow().getStepOverButton();
		step = new TutorialStep("<html>Above this table are the buttons that allow you to control the Debugger. When you click Step Over, " +
				"the Debugger runs only the line that you are currently paused on, then advances to the next line and waits." +
				"<br/>Click Step Over to continue.</html>", stepOverButton, false, ArrowOrientation.UP);
		stepOverButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
            	stepController.finishedStep = true;
				stepController.alertStepFinished();
				debugButton.removeActionListener(this);
			}
		});
		steps.add(step);
		
		step = new TutorialStep("<html>Look at the variables. Since we are now one line further in the Python file, variable c has now been set. " +
				"Variable c is a list, and is represented differently in the table. It has a different icon and has an arrow to its left. If you click the " +
				"arrow, you can check what each item in the list is set to. Any variable that contains other objects will be displayed this way." +
				"<br/>Once you've explored the values of variable c, click Next Step to proceed.</html>", this.debugPlug.getDockableWindow(), true, ArrowOrientation.RIGHT);
		
		steps.add(step);
		
		final JButton stepIntoButton = debugPlug.getDockableWindow().getStepIntoButton();
		//Original, more descriptive Step Into lesson:
		//"<html>The Step Into button works almost the same as the Step Over button. However, if the Debugger is currently on a line with " +
		//"a function that you have defined the Debugger will jump into the function and stop on the first line. Since there are no user defined functions " +
		//"in this file, it will advance to the next line. <br/>Click Step Into to continue.</html>"
		step = new TutorialStep("<html>The Step Into button works almost the same as the Step Over button. Clicking it will execute the current line of code " +
				"and advance to the next line. It also does something extra that you will learn to use at a later point.<br/>Click Step Into to continue.</html>", stepIntoButton, false, ArrowOrientation.UP);
		stepIntoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
            	stepController.finishedStep = true;
				stepController.alertStepFinished();
				debugButton.removeActionListener(this);
			}
		});
		steps.add(step);
		
		final JButton continueButton = debugPlug.getDockableWindow().getContinueButton();
		step = new TutorialStep("<html>The Continue Button tells the Debugger to continue running until it reaches another breakpoint or the end of the file. <br/>" +
				"Click it to continue.</html>", continueButton, false, null);
		continueButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
            	stepController.finishedStep = true;
				stepController.alertStepFinished();
				debugButton.removeActionListener(this);
			}
		});
		steps.add(step);
		
		step = new TutorialStep("<html><h2>Thanks for taking the time to learn how to use the features of Savu.</h2></html>", view.getExitButton(), null);
		
		steps.add(step);
		
		return steps;
	}
	
	class TutorialStep {
		String message;
		Component toHighlight;
		boolean drawNext;
		ArrowOrientation orientation = null;
		boolean TheFileSwap = false;
		boolean isBreakpointStep = false;
		
		/**
		 * Primary constructor for Tutorial entries that do not need a Next Step button
		 * @param msg text to display
		 * @param toHigh component to highlight
		 * @param or the direction to draw the arrow, if null no arrow will be drawn
		 */
		public TutorialStep(String msg, Component toHigh, ArrowOrientation or) {
			this.message = msg;
			this.toHighlight = toHigh;
			this.drawNext = false;
			this.orientation = or;
		}
		
		/**
		 * Secondary constructor for Tutorial entries that need a Next Step button.
		 * @param msg text to display
		 * @param toHigh component to highlight
		 * @param dnext set to true to draw a next button
		 * @param or the direction to draw the arrow, if null no arrow will be drawn
		 */
		public TutorialStep(String msg, Component toHigh, boolean dnext, ArrowOrientation or) {
			this.message = msg;
			this.toHighlight = toHigh;
			this.drawNext = dnext;
			this.orientation = or;
		}
		
		/**
		 * I'm not proud of this one. Basically, call only one time in getTutorialSteps(), 
		 * on the step where the files should swap from
		 * the buggy first file to the non-buggy second file.
		 * @author nathanroberts
		 */
		public void initTheFileSwapStep(){
			this.TheFileSwap = true;
		}
		
		public void setIsBreakpointSet(boolean b){
			this.isBreakpointStep = b;
		}
		
		/**
		 * Because the file used by the tutorial changes midway through, the breakpoint step cannot be
		 * properly initialized until the text area that it will be interacting with is open.
		 * This function should be called after the correct file (that breakpoints will be set in)
		 * has been opened.
		 */
		public void initializeBreakpointStep(){
			if(this.isBreakpointStep){
				//Set the component to highlight to the gutter in the correct file.
				this.toHighlight = retrieveIconRowHeader();
				
				//Set the action listener
				final RTextEditorPane textArea = rtext.getMainView().getCurrentTextArea();
				textArea.addPropertyChangeListener(new PropertyChangeListener() {
					//Listen for a property change indicating that a breakpoint was added to the file.
					@Override
					public void propertyChange(PropertyChangeEvent ev) {
						if (ev.getPropertyName().equals(RTextEditorPane.BREAKPOINT_ADDED_PROPERTY)) {
							int line = (Integer) ev.getNewValue();
							if (line == 7) { //A breakpoint was added to line 7! The user successfully completed this step.
								stepController.finishedStep = true;
								stepController.alertStepFinished();
								textArea.removePropertyChangeListener(this);
							} else { //Breakpoint added to the wrong line, we should remove it.
								rtext.clearBreakpointsInEditorPane(textArea);
							}
						}
					}
				});
			}
		}
		
		/**
		 * Helper function to get the IconRowHeader
		 * @return
		 */
		private IconRowHeader retrieveIconRowHeader(){
			IconRowHeader iconRow = null;
			Gutter g = RSyntaxUtilities.getGutter(rtext.getMainView().getCurrentTextArea());
			for(Component c : g.getComponents() ) { //Extract the IconRowHeader from the gutter
				if (c instanceof IconRowHeader) {
					iconRow = (IconRowHeader) c;
				}
			}
			System.out.println("IRH retrieved");
			return iconRow;
		}
		
	}
	
	class ChangedCurrentTextAreaListener implements CurrentTextAreaListener {
		
		RTextEditorPane tutEditorPane;
		
		public ChangedCurrentTextAreaListener(RTextEditorPane tutPane) {
			tutEditorPane = tutPane;
		}
		
		@Override
		public void currentTextAreaPropertyChanged(CurrentTextAreaEvent e) {
			if ((e.getNewValue() instanceof RTextEditorPane)  && !e.getNewValue().equals(tutEditorPane)) {
				e.getMainView().setSelectedTextArea(tutEditorPane);
			}
		}

	}

}
