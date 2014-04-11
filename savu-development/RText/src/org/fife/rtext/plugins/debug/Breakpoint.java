package org.fife.rtext.plugins.debug;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;

import org.fife.rtext.RTextEditorPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;

/**
 * Data storage class that contains information about user-placed breakpoints.  
 * 
 * Each breakpoint is associated with a particular file, a particular offset in a document, and a particular Icon image reference.
 * 
 * Its position will update as the associated Document changes.
 * @author PyDe
 *
 */
public class Breakpoint implements PropertyChangeListener {
	private String filePath;
	private GutterIconInfo gii;
	private Position pos;
	private Integer lineNum;

	private RTextEditorPane textArea;
	
	private boolean isValid = true; //Used for internal deletion
	
	public Breakpoint(RTextEditorPane textArea, Position pos, GutterIconInfo gii) {
		this.filePath = textArea.getFileFullPath();
		this.pos = pos;
		this.gii = gii;
		this.textArea = textArea;
		textArea.addPropertyChangeListener(this); //Listen for property changes in order to dynamically update the filepath.
	}
	
	/**
	 * Returns the line number, based on the stored offset.
	 * Note: This is currently an expensive operation, as it must call to the EDT.
	 * 
	 * @return The line number as an Integer.
	 */
	public Integer getLineNum() {
		if (SwingUtilities.isEventDispatchThread()) {
			computeLineNum();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() { 
					public void run() {
						computeLineNum();
				}});
			} catch (Exception ex) {
				ex.printStackTrace();
				return -1;
			}
		}
		return lineNum + 1;
	}
	
	/**
	 * Computes and stores the line number of this breakpoint based on the current stored offset.
	 * Must be run on the EDT.
	 */
	private void computeLineNum() {
		try {
			Element root = textArea.getDocument().getDefaultRootElement();
			lineNum = root.getElementIndex(getLineStartOffset());
			if (lineNum < 0) {
				throw new BadLocationException("Breakpoint position invalid.", getLineStartOffset());
			}
		} catch (BadLocationException e) {
			e.printStackTrace(); //Can never happen.
		}
	}
	
	/**
	 * Checks if the line this Breakpoint is tracking is valid.
	 * 
	 * @return True if the line is valid, False otherwise
	 */
	public boolean isLineValid() {
		if (SwingUtilities.isEventDispatchThread()) {
			isValid = checkLineValid();
		} else {
			try {
				SwingUtilities.invokeAndWait(new Runnable() { 
					public void run() {
						isValid = checkLineValid();
				}});
			} catch (Exception ex) {
				ex.printStackTrace();
				isValid = false;
			}
		}
		return isValid;
	}
	
	/**
	 * Helper method for isLineComment() to actually perform the analysis of the line based on its text and its tokens. 
	 * Should only be called from the EDT.
	 * @return True if the line is valid, False otherwise.
	 */
	private boolean checkLineValid() {
		String lineText = "";
		try { //Try to extract this line's text
			int start = textArea.getLineStartOffset(getLineNum()-1);
			int lineLength = textArea.getLineEndOffset(getLineNum()-1) - start;
			lineText = textArea.getText(start, lineLength).trim();
			if (lineText.equals("")) { //Handle empty lines
				return false;
			}
			//Check for a line that is only a comment
			if (lineText.startsWith("#") || lineText.startsWith("'''") || lineText.startsWith("\"\"\"")) {
				return false;
			}
		} catch (BadLocationException ex) {
			//If this happens (it shouldn't), just continue on to check the tokens
		}
		//Check for a multiline comment
		RSyntaxDocument rd = (RSyntaxDocument) textArea.getDocument();
		int lineNum = getLineNum();
		Token firstTokenInLine = null;
		try {
			firstTokenInLine = rd.getTokenListForLine(lineNum); //Returns the first token on this line
		} catch (NullPointerException ex) {
			//Just continue; need to catch this due to an unknown bug in the RSyntaxTextArea code.
		}		
		//Bug in implementation of getTokenListForLine(); need to use the "previous" line's tokens and check for another round of errors.
		if (firstTokenInLine == null) {
			lineNum -= 1;
			if (lineNum >= 0) {
				try { 
					firstTokenInLine = rd.getTokenListForLine(lineNum);
				} catch (NullPointerException ex) {
					return false;
				}
			} else {
				return false;
			}
		}
		switch (firstTokenInLine.getType()) {
			case TokenTypes.COMMENT_DOCUMENTATION:
			case TokenTypes.COMMENT_EOL:
			case TokenTypes.COMMENT_MULTILINE:
			case TokenTypes.COMMENT_KEYWORD:
			case TokenTypes.COMMENT_MARKUP:
			case TokenTypes.LITERAL_CHAR: //Line has become commented
				return false;
			default:
				return true; //Line is valid!
		}
	}
	
	/**
	 * Attempts to return the Gutter component this Breakpoint's icon is being displayed in.
	 * 
	 * Must be called from the EDT.
	 * 
	 * @return The Gutter, or null.
	 */
	public Gutter getGutter() {
		if (SwingUtilities.isEventDispatchThread()) {
			return RSyntaxUtilities.getGutter(textArea);
		} else {
			return null;
		}
	}
	
	/**
	 * @return The current offset of the start of this Breakpoint's line.
	 */
	private int getLineStartOffset() {
		return pos.getOffset();
	}
	
	/**
	 * @return The full file path for the file this Breakpoint is within.
	 */
	public String getFilePath() {
		return filePath;
	}
	
	/**
	 * @return The icon info used by the <code>Gutter</code> to maintain and draw a visual representation of this breakpoint.
	 */
	public GutterIconInfo getGutterIconInfo() {
		return gii;
	}

	/**
	 * If the path of the rtexteditorpane this breakpoint is in changes, need to update our own line number.
	 */
	@Override
	public void propertyChange(PropertyChangeEvent ev) {
		if (ev.getPropertyName().equals(TextEditorPane.FULL_PATH_PROPERTY)) {
			filePath = (String) ev.getNewValue();
		}
		
	}

}
