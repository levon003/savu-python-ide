/*
 * 11/14/2003
 *
 * RTextEditorPane.java - The text editor used by RText.
 * Copyright (C) 2003 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent.ElementChange;
import javax.swing.event.DocumentListener;

import org.fife.print.RPrintUtilities;
import org.fife.rtext.actions.ToggleBreakpointAction;
import org.fife.rtext.plugins.debug.Breakpoint;
import org.fife.ui.autocomplete.AutoCompleteRunner;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.rsyntaxtextarea.FileLocation;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTATextTransferHandler;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Element;

import org.fife.ui.autocomplete.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;


/**
 * An extension of {@link TextEditorPane} that adds RText-specific features.
 *
 * @author Robert Futrell
 * @version 1.2
 */
public class RTextEditorPane extends TextEditorPane {

	/**
	 * Property name used for events when new breakpoints are added.
	 */
	public static final String BREAKPOINT_ADDED_PROPERTY = "ToggleBreakpointAction.addedBreakpoint";
	
	private Savu rtext;
	private PythonCompletionProvider provider;

	/**
	 * This flag determines whether edits can happen to the document.
	 * 
	 * It is set to volatile because writes are independent of its value and its usage does not require syncronicity but it could still be accessed concurrently.
	 */
	private volatile boolean editingEnabled = true;

	/**
	 * Creates a new <code>RTextEditorPane</code>.  Syntax highlighting will
	 * be selected as follows:  filenames ending in <code>".java"</code>
	 * default to Java syntax highlighting; all others default to no syntax
	 * highlighting.
	 *
	 * @param rtext The owning RText instance.
	 * @param wordWrapEnabled Whether or not to use word wrap in this pane.
	 * @param textMode Either <code>INSERT_MODE</code> or
	 *        <code>OVERWRITE_MODE</code>.
	 * @param loc The location of the file to open.
	 * @param encoding The encoding of the file.
	 * @throws IOException If an IO error occurs reading the file to load.
	 */
	public RTextEditorPane(Savu rtext, boolean wordWrapEnabled,
		int textMode, FileLocation loc, String encoding) throws IOException {
		super(textMode, wordWrapEnabled, loc, encoding);
		this.rtext = rtext;
		// Change the transfer handler to one that recognizes drag-and-dropped
		// files as needing to be opened in the parent main view.
		setTransferHandler(new RTextEditorPaneTransferHandler());
		setTabsEmulated(false); //Ensure that this text area uses tabs so we can catch them in our custom filter
		//Add a document filter to provide custom spacing options and to control breakpoints
		((AbstractDocument) this.getDocument()).setDocumentFilter(new SpaceControlFilter());
	}


	/**
	 * Method called when it's time to print this badboy (the old-school, AWT
	 * way).  This method overrides <code>RTextArea</code>'s <code>print</code>
	 * method so that we can use the font specified in RText when printing.
	 *
	 * @param g The context into which the page is drawn.
	 * @param pageFormat The size and orientation of the page being drawn.
	 * @param pageIndex The zero based index of the page to be drawn.
	 */
	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
		Font printWithMeFont = rtext.getMainView().getPrintFont();
		if (printWithMeFont==null)	// null => print with the current font.
			printWithMeFont = this.getFont();
		return RPrintUtilities.printDocumentWordWrap(g, this,
				printWithMeFont, pageIndex, pageFormat, this.getTabSize());
	}
	
	/**
	 * Overridden to force text anti-aliasing.  
	 */
	@Override
	//Credit to http://mindprod.com/jgloss/antialiasing.html for this solution
	protected void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		
		// for antialiasing geometric shapes
		//g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON );
		
		
		 //TODO Make anti-aliasing hints monitor-dependent or toggleable in the preferences panel. Or just remove this code.
		// for antialiasing text
		g2d.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON );

		// to go for quality over speed
		//g2d.setRenderingHint( RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY );
		super.paintComponent( g2d );
	}

	/**
	 * Method called to add autocomplete objects to this pane
	 * Also will start the <code>AutoCompleteRunner</code> running in the background
	 * 
	 */
	public void addAutoComplete(){
		provider = createCompletionProvider();
		provider.setAutoActivationRules(true, ".");
		AutoCompletion ac = new AutoCompletion(provider);
		ac.install(this);
		ac.setAutoActivationEnabled(true);
		AutoCompleteRunner runner = new AutoCompleteRunner(rtext, 200);
		//Thread runnerThread = new Thread(runner);
		runner.setUncaughtExceptionHandler(new AutoCompleteExceptionHandler());
		runner.start();
	}
		
	
	/**
	 * Method called to create a <code>PythonCompletionProvider</code> object
	 * Exists in case we want to add things to the initialization of every completion provider
	 * like default completions (def, range, etc)
	 * 
	 */
	private PythonCompletionProvider createCompletionProvider(){
		provider = new PythonCompletionProvider();
		return provider;
	}
	
	
	/**
	 * Populates the autocompletion options 
	 */
	public void populateAutoComplete(){
		Document doc = this.getDocument();
		String text = "";
		try {
			// Only give it the text up to the current caret position
			// Keeps things in scope
			text = doc.getText(0, doc.getLength());
		} catch (BadLocationException e) {
			//Make sure the location exists (lol)
			e.printStackTrace();
		}
		
		HashSet<Completion> completionOptions;

		//All the methods return a hashset to make sure that duplicates don't get added
		completionOptions = provider.getLocalCompletionOptions(text,this.getCaretPosition());
		completionOptions.addAll(provider.getGlobalCompletionOptions(text, doc.getLength()));
		completionOptions.addAll(provider.getClassCompletionOptions(text));
		completionOptions.addAll(provider.getBuiltInCompletionOptions());
		String filePath = rtext.getMainView().getCurrentTextArea().getFileFullPath();
		String dirPath = "";
		Matcher m = Pattern.compile("(.*)/.*[.].*").matcher(filePath);
		if (m.find()){
			dirPath = m.group(1);
		}
		completionOptions.addAll(provider.getImportCompletionOptions(text, this.getCaretPosition(), dirPath));
		//need to convert to an arraylist at the end to feed to addCompletions
		ArrayList<Completion> completions = new ArrayList<Completion>(completionOptions);
		
		//Removing "self"
		String replacementText;
		for (Completion comp : completions){
			replacementText = comp.getReplacementText();
			m = Pattern.compile("(self, *)|(self)").matcher(replacementText);
			((BasicCompletion)comp).setReplacementText(m.replaceAll(""));
		}
		
		
		// Not too sure why this needs to be synchronized as no other thread should be accessing this, but I guess it is somehow
		synchronized(provider){
			provider.clear();
			provider.addCompletions(completions);
		}
	}
	
	/**
	 * 
	 * @return True if this RTextEditorPane can make external changes to its Document, and False otherwise.
	 */
	public boolean isEditingEnabled() {
		return editingEnabled;
	}
	
	/**
	 * Re-enable text insertion, deletion, and replacement.
	 */
	public void enableEditing() {
		editingEnabled = true;
	}
	
	/**
	 * Prevent all text insertion, deletion, and replacement in this RTextEditorPane.
	 */
	public void disableEditing() {
		editingEnabled = false;
	}
	
	/**
	 * Transfer handler for editor panes.  Overrides the default transfer
	 * handler so we can drag-and-drop files into a text area, and know to
	 * open it in the parent main view.
	 */
	class RTextEditorPaneTransferHandler extends RTATextTransferHandler {

		public boolean canImport(JComponent c, DataFlavor[] flavors) {
			return MainPanelTransferHandler.hasFileFlavor(flavors) ||
					super.canImport(c, flavors);
		}

		public boolean importData(JComponent c, Transferable t) {
			return MainPanelTransferHandler.
				importDataImpl(rtext.getMainView(), c, t) ||
						super.importData(c, t);
		}

	}
	
	/**
	 * A class that extends <code>DocumentFilter</code> looking for times when the user removes a single space
	 * at the start of a line and adding "back-tab" functionality.
	 * 
	 * @author PyDe
	 *
	 */
	class SpaceControlFilter extends DocumentFilter {
		
		@Override
		public void insertString(DocumentFilter.FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
			if (!editingEnabled || rtext.isGlobalEditingLocked())
				return;
			StringBuilder spaces = new StringBuilder();
			for (int i = 0; i < rtext.getTabSize(); i++) {
				spaces.append(" ");
			}
			String newString = string.replaceAll("\t", spaces.toString()); //Replace with a tab composed of spaces equal to the tab-stop size.
			int charsAdded = newString.length() - string.length();
			super.insertString(fb, offset, newString, attr);
		}
		
		@Override
		public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			if (!editingEnabled || rtext.isGlobalEditingLocked())
				return;
		
			//Breakpoint updating code
			int changedLine = getLineOfOffset(offset);	
			int lineStart = getLineStartOffset(changedLine); 
			int line = getLineOfOffset(lineStart);
			//String fullLineText = getText(lineStart, getLineEndOffset(changedLine) - lineStart).trim();
			RSyntaxDocument rd = (RSyntaxDocument) getDocument();
			Token firstTokenInLine = null;
			try {
				firstTokenInLine = rd.getTokenListForLine(line); //Returns the first token on this line
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
					case TokenTypes.LITERAL_CHAR: //Line has become commented
						updateBreakpoints(line+1);
						break;
					default:
						break;
				}
			}
			
			//Tab replacement code
			if (text.contains("\t")) { //Replacement contains a tab
				if (length != 0) { //This is a replacement of a text selection on a single line
					StringBuilder spaces = new StringBuilder();
					for (int i = 0; i < rtext.getTabSize(); i++) {
						spaces.append(" ");
					}
					text = text.replaceAll("\t", spaces.toString()); //Replace with a tab composed of spaces equal to the tab-stop size.
					super.replace(fb, offset, length, text, attrs);
				} else { //Normal tab key press
					int tabSize = rtext.getTabSize();
					Document d = getDocument();
					Element root = d.getDefaultRootElement();
					Element elem = root.getElement(root.getElementIndex(offset)); //Extract the element for this line from the text
					int start = elem.getStartOffset();
					String lineText = d.getText(start, elem.getEndOffset() - start); //Extract the line text itself
					Matcher m = Pattern.compile("[^ ]").matcher(lineText);
					int firstNonWhitespaceOffset;
					if (m.find()) { //Found a non-space character on the line
						firstNonWhitespaceOffset =  m.start();
					} else { //Line contains only whitespace; appears to never happen, as each Element contains an implicit or literal \n
						firstNonWhitespaceOffset = 0;
					}
					int distFromStop = firstNonWhitespaceOffset % tabSize;
					int whitespaceToInsert = tabSize - distFromStop; //Use the separation between the first non-space character and the nearest tab stop as the number of spaces to insert
					StringBuilder spaces = new StringBuilder();
					for (int i = 0; i < whitespaceToInsert; i++) {
						spaces.append(" ");
					}
					text = text.replaceAll("\t", spaces.toString()); //Replace any tabs
					if (text.contains("\n")) {
						super.replace(fb, offset, length, text, attrs); //Multi-line insert, so insert at cursor position
					} else {
						super.replace(fb, start, length, text, attrs); //insert at the start of the line
					}
				}
			} else { //No tab
				super.replace(fb, offset, length, text, attrs);	
			}
		}
		
		@Override
		public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException{
			if (!editingEnabled || rtext.isGlobalEditingLocked())
				return;
			
			//Breakpoint updating code
			int changedLine = getLineOfOffset(offset);	
			int lineStart = getLineStartOffset(changedLine); 
			String text = getText(lineStart, getLineEndOffset(changedLine) - lineStart).trim();
			String removedText = "";
			try {
				removedText = text.substring(offset - lineStart, offset - lineStart + length).trim();
			} catch (StringIndexOutOfBoundsException e) {
				//can continue with removedText = ""
			}
			if (text.equals(removedText)) { //Line has become empty / only whitespace
				updateBreakpoints(changedLine+1);
			}
			
			//Tab deletion code
			// If only greater than one character is removed, it's not a space and we can remove as normal
			if (length > 1) {
				super.remove(fb, offset, length);
			}
			// If only one character was removed, check if it's a space then proceed accordingly
			else {
				Document d = getDocument();
				String deletedText = d.getText(offset, length);
				// The character removed was a space
				if (deletedText.equals(" ")) {
					//Get the text on the line before the completion
					Element root = d.getDefaultRootElement();
					int index = root.getElementIndex(offset);
					Element elem = root.getElement(index);
					int start = elem.getStartOffset();
					int lineLength = offset-start;
					String lineText = d.getText(start, lineLength);
					// Is entire line prior to this point whitespace? If not, remove as normal
					if (!lineText.matches("^ +$")) {
						super.remove(fb, offset, length);
					} else {
						// If entire line is whitespace, remove spaces until we're at the last tab stop
						int tabSize = rtext.getTabSize();
						int distFromStop = lineLength % tabSize;
						super.remove(fb, offset-distFromStop, length+distFromStop);
					}
				} else { //A non-space character was deleted.
					super.remove(fb, offset, length);
				}
			}
		}
		
		private void updateBreakpoints(int line) {
			ArrayList<Breakpoint> breakpoints = rtext.getBreakpointsDictionary().get(RTextEditorPane.this);
			if (breakpoints == null)
				return;
			ArrayList<Breakpoint> toDel = new ArrayList<Breakpoint>();
			//Search for breakpoints that may need to be removed
			for (Breakpoint b : breakpoints) {
				if (b.getLineNum() == line) {

					toDel.add(b);
				}
			}
			//Actually remove the breakpoints
			Gutter g = RSyntaxUtilities.getGutter(RTextEditorPane.this);
			for (Breakpoint b : toDel) {
				breakpoints.remove(b);
				g.removeTrackingIcon(b.getGutterIconInfo());
			}
		}
	}
}