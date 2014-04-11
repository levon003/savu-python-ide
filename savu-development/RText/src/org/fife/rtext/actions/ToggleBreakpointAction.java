package org.fife.rtext.actions;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;

import org.fife.rtext.Savu;
import org.fife.rtext.RTextEditorPane;
import org.fife.rtext.plugins.debug.Breakpoint;
import org.fife.rtext.plugins.debug.DebugPlugin;
import org.fife.rtext.plugins.debug.PythonDebugger;
import org.fife.rtext.plugins.run.PythonProcess;
import org.fife.ui.app.StandardAction;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.IconRowHeader;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RecordableTextAction;

/**
 * This action toggles a breakpoint at the cursor's current location (if invoked from the MenuBar) or the mouse's current line location (if invoked by a mouse click in the IconRowHeader of an RTextArea).
 * @author PyDe
 *
 */
public class ToggleBreakpointAction extends StandardAction implements MouseListener {
	
	/**
	 * The number of lines to lookahead when the user tries to place a breakpoint on an invalid line.
	 */
	public static final int LOOKAHEAD = 8;
	
	/**
	 * A reference to the parent process that creates and registers this MouseListener.
	 */
	private Savu rtext;
	private Icon breakpointIcon; 
	
	private DebugPlugin debugPlug; //Need a reference to the debug plugin in order to send breakpoint changes directly to the process.
	
	private boolean mousePressed = false;
	
	/**
	 * Constructor used for the Action used by the mouse.
	 * @param rtext
	 * @param breakpointIcon
	 */
	public ToggleBreakpointAction(Savu rtext, Icon breakpointIcon) {
		//Don't actually want to load the resource bundle for ToggleBreakpointAction here; otherwise use of the hotkey fires two events.
		super(rtext);
		this.rtext = rtext;
		this.breakpointIcon = breakpointIcon;
		this.debugPlug = null;
	}
	
	/**
	 * Constructor used for the Action used by the MenuBar and hotkey.
	 * @param rtext
	 * @param msg
	 * @param breakpointIcon
	 */
	public ToggleBreakpointAction(Savu rtext, ResourceBundle msg, Icon breakpointIcon) {
		super(rtext, msg, "ToggleBreakpointAction");
		setIcon(breakpointIcon);
		this.rtext = rtext;
		this.breakpointIcon = breakpointIcon;
		this.debugPlug = null;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		mousePressed = true; //Mouse down in our area; could be the start of a click
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
		mousePressed = false; //Even if the user pressed down the mouse button in the gutter, want to reset if the mouse leaves the gutter
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if (mousePressed) {
			processMouseClick(e); //The mouse was released in the gutter after first being pressed in the gutter; we're good to process this as a click.
		}
	}
	
	//User clicked in the FoldingAwareIconRowHeader in the Gutter of an RSyntaxTextArea.
	public void processMouseClick(MouseEvent e) {
		try {
			//Assumption: Only the current text area's gutter can/should be clicked
			RTextEditorPane textArea = rtext.getMainView().getCurrentTextArea();
			int offs = textArea.viewToModel(e.getPoint());
			if (offs == -1) { //No line number; should never happen
				return;
			}
			int line = textArea.getLineOfOffset(offs);
			toggleBreakpoint(line, textArea);
		} catch (BadLocationException ble) { //Should never happen
			ble.printStackTrace();
		}		
	}
	
	//User invoked a menu action to toggle breakpoint based on the current cursor position.
	@Override
	public void actionPerformed(ActionEvent e) {
		final RTextEditorPane textArea = rtext.getMainView().getCurrentTextArea();
		try {
			int line = textArea.getCaretLineNumber();
			toggleBreakpoint(line, textArea);
			if (SwingUtilities.isEventDispatchThread()) {
				RSyntaxUtilities.getGutter(textArea).updateUI();
			} else {
				SwingUtilities.invokeLater(new Runnable() { public void run() { 
					RSyntaxUtilities.getGutter(textArea).updateUI();
				}});
			}
		} catch (BadLocationException ble) { //Should never happen
			ble.printStackTrace();
		}
	}
	
	/**
	 * Toggles a breakpoint at a specified line number in a specified file.
	 * 
	 * @param line The line number to toggle breakpoint of, 0-based.
	 * @param textArea The editorpane of the file we are toggling a breakpoint in.
	 * @throws BadLocationException If the specified line is after the end of the file.
	 */
	protected void toggleBreakpoint(Integer line, RTextEditorPane textArea) throws BadLocationException {
		Gutter gutter = RSyntaxUtilities.getGutter(textArea);
		if (gutter != null) {
			HashMap<RTextEditorPane, ArrayList<Breakpoint>> breakpoints = rtext.getBreakpointsDictionary();
			ArrayList<Breakpoint> existingBreakpoints = breakpoints.get(textArea);
			//Check this line to see if it's a valid breakpoint location; if it isn't, it may suggest a new one.
			try {
				int newline = checkBreakpointLocation(line, textArea);
				if (newline == -1) {
					return; //Concurrency error
				}
				if (newline != line) {
					//Before using the new proposed line, make sure there wasn't actually an invalid breakpoint to remove here first
					if (attemptRemovalAtLine(line, existingBreakpoints, gutter))
						return;
					line = newline;
				}
			} catch (BadLocationException ble) { //No suitable line found
				if (attemptRemovalAtLine(line, existingBreakpoints, gutter)) //It's a bad location, but the user may have clicked here because of an erroneous breakpoint
					return; //There actually was a breakpoint to remove at this point in the code
				rtext.displayException(ble);
				return; //Couldn't do anything.
			}
			Breakpoint prevBreak; //Stores the Breakpoint that existed at line, or null
			if (existingBreakpoints == null) {
				existingBreakpoints = new ArrayList<Breakpoint>();
				breakpoints.put(textArea, existingBreakpoints);
				prevBreak = null;
			} else { //Previous breakpoints in this file
				prevBreak = breakpointAtLine(existingBreakpoints, line+1);
			}
			if (prevBreak == null) { //No breakpoint at this line; create and add one.
				Breakpoint newBreak = createBreakpointAtLine(line, gutter, textArea);
				if (newBreak != null) {
					existingBreakpoints.add(newBreak);
					addBreakpointToDebugProcess(newBreak);
					textArea.firePropertyChange(RTextEditorPane.BREAKPOINT_ADDED_PROPERTY, -1, newBreak.getLineNum());
				}
			} else { //Breakpoint existed; remove it.
				while (prevBreak != null) {
					gutter.removeTrackingIcon(prevBreak.getGutterIconInfo());
					existingBreakpoints.remove(prevBreak);
					removeBreakpointFromDebugProcess(prevBreak);
					//We need to loop here to clean up multiple breakpoints that may have become stacked on the same line. 
					//It doesn't matter that they're stacked for pdb, but we don't want to confuse the user (into thinking they misclicked or something).
					prevBreak = breakpointAtLine(existingBreakpoints, line+1);
				}
			}
		}
	}
	
	
	/**
	 * Helper method that creates a new breakpoint associated with a line's start offset.
	 * @return The new Breakpoint
	 */
	private Breakpoint createBreakpointAtLine(Integer line, Gutter gutter, RTextEditorPane textArea) throws BadLocationException {
		//Actually create the breakpoint at the Document offset at this line's start
		GutterIconInfo info = gutter.addLineTrackingIcon(line, breakpointIcon, "Breakpoint");
		int offs = textArea.getLineStartOffset(line);
		Document doc = textArea.getDocument(); //The document can give us a dynamic position marking this breakpoint's location in the file.
		Breakpoint newBreak = new Breakpoint(textArea, doc.createPosition(offs), info);
		return newBreak;
	}
	
	/**
	 * Blindly checks the given breakpoint list for any Breakpoints with the given line num.  If it finds any, it removes them.
	 * 
	 * @param line The zero-indexed line within the file.
	 * @param breakpoints Breakpoints within this file.
	 * @param g The gutter containing these breakpoints.
	 * @return True if a removal occurred, false otherwise.
	 */
	private boolean attemptRemovalAtLine(int line, ArrayList<Breakpoint> breakpoints, Gutter g) {
		if (breakpoints == null) {
			return false;
		}
		ArrayList<Breakpoint> toDel = new ArrayList<Breakpoint>();
		for (Breakpoint b : breakpoints) {
			//Search for breakpoints on this line
			if (b.getLineNum() == (line+1)) {
				toDel.add(b);
				g.removeTrackingIcon(b.getGutterIconInfo());
			}
		}
		if (toDel.size() > 0) {
			//Actually delete the marked breakpoints from the data model
			for (Breakpoint b : toDel) {
				breakpoints.remove(b);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Using knowledge of PDB and Python, checks to see if this is a valid location for a breakpoint in the currently focused Python file.
	 * If it isn't, it looks LOOKAHEAD positions ahead to find a valid position, and returns that instead.
	 * 
	 * @param line Line number on which we hope to toggle the breakpoint.
	 * @param filePath Full file path to the location we are toggling the breakpoint.
	 * @return -1 if failed, or the line to place the breakpoint.
	 */
	private int checkBreakpointLocation(Integer line, RTextEditorPane textArea) throws BadLocationException {
		String lineText = "";
		try {
			int start = textArea.getLineStartOffset(line);
			int lineLength = textArea.getLineEndOffset(line) - start;
			lineText = textArea.getText(start, lineLength).trim();
		} catch (BadLocationException ex) {
			return -1; //Never happens
		}
		BadLocationException failReason = null;
		if (lineText.equals("")) { //Handle empty lines
			failReason = new BadLocationException("Breakpoints cannot be placed on empty lines.", line);
		}
		//Check for a line that is only a comment
		if (lineText.startsWith("#") || lineText.startsWith("'''") || lineText.startsWith("\"\"\"")) {
			failReason = new BadLocationException("Breakpoints cannot be placed on lines that only contain comments.", line);
		}
		//Check to see if this line is entirely comment
		RSyntaxDocument d = (RSyntaxDocument) textArea.getDocument();
		Token firstTokenInLine = null;
		try {
			firstTokenInLine = d.getTokenListForLine(line); //Returns the first token on this line
		} catch (NullPointerException ex) {
			//Just continue; need to catch this due to an unknown bug in the RSyntaxTextArea code.
		}
		if (firstTokenInLine != null) {
			switch (firstTokenInLine.getType()) {
				case TokenTypes.COMMENT_DOCUMENTATION:
				case TokenTypes.COMMENT_EOL:
				case TokenTypes.COMMENT_MULTILINE:
				case TokenTypes.COMMENT_KEYWORD:
				case TokenTypes.COMMENT_MARKUP:
				case TokenTypes.LITERAL_CHAR: //This is used as the doc-string style by the Python tokenizer
					failReason = new BadLocationException("Breakpoints cannot be placed on lines that only contain comments.", line);
					break;
				default:
					break;
			}
		} else {
			failReason = new BadLocationException("Breakpoints cannot be placed on empty lines.", line);
		}
		if (failReason != null) {
			int proposedLine = checkBreakpointLocationRecursive(line, line+1, textArea); //Try to find a suitable line nearby
			if (proposedLine < 0) {
				throw failReason; //Alert the user that they screwed up
			} else {
				line = proposedLine;
			}
		}
		return line;
	}
	
	/**
	 * A recursive helper function for checkBreakpointLocation() that looks for a valid line to place this breakpoint on instead.
	 * Will search down in the file up to LOOKAHEAD extra lines.  
	 */
	private int checkBreakpointLocationRecursive(int startLine, int line, RTextEditorPane textArea) {
		if (line > startLine + LOOKAHEAD) {
			return -1; //Base case 1: We looked ahead LOOKAHEAD number of times and couldn't find a valid line.
		}
		String lineText = "";
		try { //Try to extract this line's text
			int start = textArea.getLineStartOffset(line);
			int lineLength = textArea.getLineEndOffset(line) - start;
			lineText = textArea.getText(start, lineLength).trim();
		} catch (BadLocationException ex) {
			return -1; //Base case 2: End of document.
		}
		if (lineText.equals("")) { //Handle empty lines
			return checkBreakpointLocationRecursive(startLine, line+1, textArea);
		}
		//Check for a line that is only a comment
		if (lineText.startsWith("#") || lineText.startsWith("'''") || lineText.startsWith("\"\"\"")) {
			return checkBreakpointLocationRecursive(startLine, line+1, textArea);
		}
		//Check to see if this line is entirely comment
		RSyntaxDocument d = (RSyntaxDocument) textArea.getDocument();
		Token firstTokenInLine = null;
		try {
			firstTokenInLine = d.getTokenListForLine(line); //Returns the first token on this line
		} catch (NullPointerException ex) {
			//Just continue; need to catch this due to an unknown bug in the RSyntaxTextArea code.
		}		if (firstTokenInLine == null) {
			return checkBreakpointLocationRecursive(startLine, line+1, textArea);
		}
		switch (firstTokenInLine.getType()) {
			case TokenTypes.COMMENT_DOCUMENTATION:
			case TokenTypes.COMMENT_EOL:
			case TokenTypes.COMMENT_MULTILINE:
			case TokenTypes.COMMENT_KEYWORD:
			case TokenTypes.COMMENT_MARKUP:
			case TokenTypes.LITERAL_CHAR: //This is used as the doc-string style by the Python tokenizer
				return checkBreakpointLocationRecursive(startLine, line+1, textArea);
			default:
				break;
		}
		return line; //This line is okay for a comment!
	}
	
		
	/**
	 * Dynamically adds a breakpoint to the PythonDebugger currently executing. If no debugger is active, it will do nothing.
	 * @param b The breakpoint to add.
	 */
	protected void addBreakpointToDebugProcess(Breakpoint b) {
		if (confirmDebugPlugin()) {
			PythonProcess p = debugPlug.getCurrentProcess();
			if (p != null && p instanceof PythonDebugger) {
				PythonDebugger debugProcess = (PythonDebugger) p;
				debugProcess.setBreakpoint(b);
			}
		}
	}
	
	/**
	 * Dynamically removes a breakpoint from the PythonDebugger currently executing. If no debugger is active, it will do nothing.
	 * @param b The breakpoint to remove.
	 */
	protected void removeBreakpointFromDebugProcess(Breakpoint b) {
		if (confirmDebugPlugin()) {
			PythonProcess p = debugPlug.getCurrentProcess();
			if (p != null && p instanceof PythonDebugger) {
				PythonDebugger debugProcess = (PythonDebugger) p;
				debugProcess.clearBreakpoint(b);
			}
		}
	}
	
	/**
	 * Determines whether we have a reference to the DebugPlugin of our associated RText, and tries to get it if we don't.
	 * 
	 * @return True if we have a set reference to the debug plugin, False otherwise
	 */
	private boolean confirmDebugPlugin() {
		if (debugPlug == null) {
			debugPlug = DebugAction.getDebugPlugin(rtext);
			if (debugPlug == null) {
				return false; //Couldn't get a reference to the debug plugin, so we just won't communicate with the process.
			}
		}
		return true;
	}
	
	//Want to intercept calls to putValue in order to use whichever Icon is assigned to this Action
	@Override
	public void putValue(String key, Object value) {
		super.putValue(key, value);
		if (key.equals(Action.SMALL_ICON)) {
			this.breakpointIcon = (Icon) super.getValue(key); 
		}
	}
	
	/**
	 * 
	 * @param linesNums The array of Breakpoints already present in this file.
	 * @param line The line number of the file we are investigating.
	 * @return null if no breakpoint currently exists at the given line, or the Breakpoint if one exists
	 */
	private Breakpoint breakpointAtLine(ArrayList<Breakpoint> existingBreakpoints, int line) {
		for (Breakpoint breakpoint : existingBreakpoints) {
			if (breakpoint.getLineNum() == line) {
				return breakpoint;
			}
		}
		return null;
	}

	//Unused MouseListener methods
	public void mouseEntered(MouseEvent e) {} //Don't need to know about mouse entry
	public void mouseClicked(MouseEvent e) {} //Click is unreliable for actually registering clicks; it doesn't capture slow or "sliding" clicks.
}
