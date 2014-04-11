package org.fife.rtext.plugins.run;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fife.rtext.AbstractMainView;
import org.fife.rtext.RTextEditorPane;

/**
 * ErrorLinkController coordinates the creation and following of links between errors shown in the RunTextArea output and actual locations in Savu files.
 * 
 * @author levoniaz
 */
public class ErrorLinkController {
	
	private RunPlugin runPlug;
		
	private ArrayList<ErrorLink> errorLinks;
	private ReentrantLock errorLinksLock;
	
	public ErrorLinkController(RunPlugin plugin) {
		this.runPlug = plugin;
		errorLinks = new ArrayList<ErrorLink>();
		errorLinksLock = new ReentrantLock();
	}
		
	/**
	 * Report a range of output as stderr. Should be called each time stderr is written to the output window.
	 * 
	 * @param start
	 * @param end
	 */
	public void reportErrorOutput(String text, int start, int end, RunTextArea textArea) {
		//addErrorRange(new OffsetRange(start, end));
		String[] lines = text.split("\n");
		int currentOffset = 0;
		for (String line : lines) {
			ErrorLink newLink = parseLine(line);
			if (newLink != null) {
				//Set the offsets of this link in the document
				int linkStartOffset = newLink.getLinkRangeInOutput().start;
				int linkedLine = 0;
				int c = 0;
				while (c <= linkStartOffset) {
					c += lines[linkedLine].length();
					if (c > linkStartOffset) {
						break; //clickLine is the line the user clicked.
					}
					linkedLine++;
				}
				//At this point, linkedLine is the line the user's link is on.
				newLink.getLinkRangeInOutput().shiftRange(start + currentOffset + 7);
				//Add it to our errorLinks list
				addErrorLink(newLink);
				//Turn the style of the document at the relevant position into hyperlink
				StyledDocument doc = textArea.getStyledDocument();
				int linkStart = newLink.getLinkRangeInOutput().start;
				int linkEnd = newLink.getLinkRangeInOutput().end;
				doc.setCharacterAttributes(linkStart, linkEnd - linkStart, textArea.getStyle(RunTextArea.STYLE_ERRORLINK), true);
			}
			currentOffset += line.length() + 1;
		}
	}
	
	/**
	 * Parses the given line and attempts to return an error-link to the source of the problem.
	 * 
	 * @param line A string of Python stderr to parse.
	 * @return An ErrorLink if this line was properly formatted, or null otherwise.
	 */
	private ErrorLink parseLine(String line) {
		String regex = "File \"(.+)\", line (\\d+)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(line);
		try {
			if (matcher.find()) {
				if (matcher.groupCount() != 2) {
					return null; //Should never happen; if it does somehow happen then it wasn't what we wanted anyway.
				}
				String filePath = matcher.group(1);
				int lineNum = Integer.parseInt(matcher.group(2));
				File f = new File(filePath); //Don't want to create ErrorLinks to files that don't exist.
				if (f.exists()) {
					ErrorLink newLink =  new ErrorLink(filePath, lineNum-1); //Line numbers are indexed from 0
					String linkString = matcher.group(0).substring(5, matcher.group(0).length());
					newLink.setLinkRangeInOutput(new OffsetRange(0, linkString.length())); 
					return newLink;
				} else { //The pathname doesn't refer to an existing file.
					return null;
				}
			} else {
				return null;
			}
		} catch (Exception ex) { //Something went wrong during parsing
			return null;
		}
	}
	
	/**
	 * Clears currently stored errorlinks.
	 */
	public void clearLinks() { //TODO call this whenever the run output text area is cleared
		errorLinksLock.lock();
		errorLinks.clear();
		errorLinksLock.unlock();
	}
	
	/**
	 * Adds an ErrorLink to the list of error links.  Thread safe.
	 * 
	 * @param o
	 */
	private void addErrorLink(ErrorLink l) {
		errorLinksLock.lock();
		errorLinks.add(l);
		errorLinksLock.unlock();
	}
		
	/**
	 * Checks to see if the clicked offset is an error link.  If it is, searches for the specific error link and follows it.
	 * Note: Will only be called from the EDT, and so should avoid intense processing.
	 * @param e
	 */
	public void mouseClicked(MouseEvent e) {
		if (runPlug.getCurrentProcess() != null && runPlug.getCurrentProcess().isRunning()) {
			//Only look for a link click while the process has stopped.
			return;
		}
		RunTextArea textArea = (RunTextArea) e.getSource();
		int offs = textArea.viewToModel(e.getPoint());
		if (offs == -1) { //No line number; should never happen
			return;
		}
		StyledDocument doc = textArea.getStyledDocument();
		if (textArea.getStyle(RunTextArea.STYLE_ERRORLINK).isEqual(doc.getCharacterElement(offs).getAttributes())) {
			ErrorLink toFollow = getErrorLinkAtOffset(offs);
			if (toFollow != null) {
				followLink(toFollow);
			}
		}
	}
	
	private ErrorLink getErrorLinkAtOffset(int offset) {
		errorLinksLock.lock();
		try {
			for (ErrorLink link : errorLinks) {
				if (link.getLinkRangeInOutput().contains(offset)) {
					return link;
				}
			}
		} finally {
			errorLinksLock.unlock();
		}
		return null;
	}
	
	/**
	 * Actually try to follow an error link by switching to the tab of this document or opening it if it is closed.
	 * @param toFollow The ErrorLink to follow
	 */
	private void followLink(final ErrorLink toFollow) {
		SwingUtilities.invokeLater(new Runnable() { //We successfully created an ErrorLink; to follow it will need update GUI elements and thus get on the EDT
			@Override
			public void run() {
				AbstractMainView view = runPlug.getRText().getMainView();
				int index = view.getFileIndex(toFollow.getFilePath());
				if (index == -1) {
					boolean openSuccessful = view.openFile(toFollow.getFilePath(), null);
					if (openSuccessful) {
						//TODO LOW PRIO - Figure out how to make the highlighting text bar also shift along with the cursor (Daniel, this is all you, baby. Love, The Levonian and Yevgeni)
					} else {
						runPlug.getRText().displayException(new FileNotFoundException("Could not open file in Savu. The file may have moved, be improperly formatted, or require higher access privileges."));
						return; //Exit abruptly
					}
				} else {
					view.setSelectedIndex(index);
				}
				RTextEditorPane editor = view.getCurrentTextArea();
				try {
					editor.setCaretPosition(editor.getLineEndOffset(toFollow.getLineNum()) - 1);
				} catch (Exception ex) {
					runPlug.getRText().displayException(new BadLocationException("Line location malformatted or no longer exists in this file.", toFollow.getLineNum()));
				}
			}});
	}
	
		
	/**
	 * Internal convenience class that stores a range of two integer offsets.
	 * 
	 * @author PyDe
	 */
	private class OffsetRange implements Comparable<OffsetRange> {
		int start;
		int end;
		
		public OffsetRange(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		public void shiftRange(int amount) {
			this.start += amount;
			this.end += amount;
		}
		
		/**
		 * @param o An offset integer value
		 * @return True if the given integer is within this range
		 */
		public boolean contains(int o) {
			if ((o >= start) && (o <= end)) {
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		public String toString() {
			return "(" + Integer.toString(start) + ", " + Integer.toString(end) + ")";
		}
		
		//Overridden compareTo enables quick comparison within a data struct
		@Override
		public int compareTo(OffsetRange o) {
			return Integer.compare(o.start, start);
		}
		
	}
	
	/**
	 * ErrorLink represents a link within the Run Output text area to some source file and line number that spawned the error.
	 * 
	 * @author levoniaz
	 *
	 */
	private class ErrorLink {
		
		/**
		 * The full filepath to the file where the error occurred.  
		 */
		private String filePath;
		
		/**
		 * The line number the Python error indicates is the source of this error.
		 */
		private int destLineNum;
		
		private OffsetRange linkRangeInOutput;
		
		
		public ErrorLink(String filePath, int destLineNum) {
			this.filePath = filePath;
			this.destLineNum = destLineNum;
			linkRangeInOutput = null;
		}
		
		public String getFilePath() {
			return filePath;
		}
		
		public int getLineNum() {
			return destLineNum;
		}
		
		public void setLinkRangeInOutput(OffsetRange range) {
			linkRangeInOutput = range;
		}
		
		public OffsetRange getLinkRangeInOutput() {
			return linkRangeInOutput;
		}
		
	}
}
