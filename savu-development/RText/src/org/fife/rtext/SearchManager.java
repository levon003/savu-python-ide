/*
 * 10/13/2013
 *
 * SearchManager.java - Manages search-related UI components for RText.
 * Copyright (C) 2013 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.fife.rsta.ui.CollapsibleSectionPanel;
import org.fife.rsta.ui.search.AbstractFindReplaceDialog;
import org.fife.rsta.ui.search.FindToolBar;
import org.fife.rsta.ui.search.ReplaceToolBar;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import org.fife.ui.search.MarkingFindToolBar;
import org.fife.ui.search.MarkingReplaceToolBar;
import org.fife.ui.search.WrappingSearchEngine;


/**
 * Handles the Find and Replace dialogs and search bars.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class SearchManager {

	private Savu rtext;
	public MarkingFindToolBar findToolBar;
	public MarkingReplaceToolBar replaceToolBar;


	/**
	 * Constructor.
	 *
	 * @param rtext The parent application.
	 */
	public SearchManager(Savu rtext) {
		this.rtext = rtext;
	}


	/**
	 * Changes all listeners registered on search dialogs/tool bars from an
	 * old main view to the new one.
	 *
	 * @param fromView The old main view.
	 */
	public void changeSearchListener(AbstractMainView fromView) {
		AbstractMainView mainView = rtext.getMainView();
		if (findToolBar!=null) {
			findToolBar.removeSearchListener(fromView);
			replaceToolBar.addSearchListener(mainView);
		}
	}


	/**
	 * Configures the Find or Replace dialog.
	 *
	 * @param dialog Either the Find or Replace dialog.
	 */
	private void configureSearchDialog(AbstractFindReplaceDialog dialog) {
		AbstractMainView mainView = rtext.getMainView();
		dialog.setSearchContext(mainView.searchContext);
		rtext.registerChildWindowListeners(dialog);
	}

	/**
	 * Ensures the find and replace tool bars are created.
	 */
	private void ensureToolbarsCreated() {
		if (findToolBar==null) {
			AbstractMainView mainView = rtext.getMainView();
			CollapsibleSectionPanel csp = rtext.getCollapsibleSectionPanel();
			findToolBar = new MarkingFindToolBar(mainView, this);
			//COLORCHANGE
			findToolBar.applyBackgroundColor(Savu.MAIN_BACKGROUND_COLOR, Color.LIGHT_GRAY);
			findToolBar.colorTextFields(Color.LIGHT_GRAY, Savu.ACCENT_BACKGROUND_COLOR);

			findToolBar.setSearchContext(mainView.searchContext);
			//LAST RESORT: SET THIS TO FALSE
			findToolBar.setOpaque(true);
			csp.addBottomComponent(findToolBar);
			replaceToolBar = new MarkingReplaceToolBar(mainView, this);
			//COLORCHANGE 
			replaceToolBar.applyBackgroundColor(Savu.MAIN_BACKGROUND_COLOR, Color.LIGHT_GRAY);
			replaceToolBar.colorTextFields(Color.LIGHT_GRAY, Savu.ACCENT_BACKGROUND_COLOR);
			
			replaceToolBar.setSearchContext(mainView.searchContext);
			//Similarly, reset marking delay
			replaceToolBar.setMarkAllDelay(0);
			csp.addBottomComponent(replaceToolBar);
		}
	}


	/**
	 * Executes a "find" operation in the active editor.
	 *
	 * @see #replaceNext()
	 */
	public void findNext() {

		AbstractMainView mainView = rtext.getMainView();

		// If the current text string is nothing (ie, they haven't searched
		// yet), bring up Find dialog.
		SearchContext context = mainView.searchContext;
		String searchString = mainView.searchContext.getSearchFor();
		if (searchString==null || searchString.length()==0) {
			ensureToolbarsCreated();
			rtext.getCollapsibleSectionPanel().
					showBottomComponent(findToolBar);
			return;
		}

		// Otherwise, repeat the last Find action.
		RTextEditorPane textArea = mainView.getCurrentTextArea();

		try {

			SearchResult result = WrappingSearchEngine.find(textArea, context);
			if (!result.wasFound()) {
				searchString = RTextUtilities.escapeForHTML(searchString, null);
				String temp = rtext.getString("CannotFindString", searchString);
				// "null" parent returns focus to previously focused window,
				// whether it be RText, the Find dialog or the Replace dialog.
				JOptionPane.showMessageDialog(null, temp,
							rtext.getString("InfoDialogHeader"),
							JOptionPane.INFORMATION_MESSAGE);
			}

			// If they used the "find next" tool bar button, make sure the
			// editor gets focused.
			if (isNoSearchUIVisible()) {
				textArea.requestFocusInWindow();
			}

		} catch (PatternSyntaxException pse) {
			// There was a problem with the user's regex search string.
			// Won't usually happen; should be caught earlier.
			JOptionPane.showMessageDialog(rtext,
			"Invalid regular expression:\n" + pse.toString() +
			"\nPlease check your regular expression search string.",
			"Error", JOptionPane.ERROR_MESSAGE);
		}

	}


	/**
	 * Returns whether any search UI (find or replace dialog or tool bar) is
	 * visible.
	 *
	 * @return Whether any search UI is visible.
	 */
	private boolean isNoSearchUIVisible() {
		return 	(rtext.getCollapsibleSectionPanel().
					getDisplayedBottomComponent()==null);
	}


	/**
	 * Executes a "replace" operation in the active editor.
	 *
	 * @see #findNext()
	 */
	public void replaceNext() {

		AbstractMainView mainView = rtext.getMainView();

		// If it's nothing (ie, they haven't searched yet), bring up the
		// Replace dialog.
		SearchContext context = mainView.searchContext;
		String searchString = context.getSearchFor();
		if (searchString==null || searchString.length()==0) {
			ensureToolbarsCreated();
			rtext.getCollapsibleSectionPanel().
					showBottomComponent(replaceToolBar);
			return;
		}

		// Otherwise, repeat the last Replace action.
		RTextEditorPane textArea = mainView.getCurrentTextArea();

		try {

			SearchResult result = WrappingSearchEngine.replace(textArea, context);
			if (!result.wasFound()) {
				searchString = RTextUtilities.escapeForHTML(searchString, null);
				String temp = rtext.getString("CannotFindString", searchString);
				// "null" parent returns focus to previously focused window,
				// whether it be RText, the Find dialog or the Replace dialog.
				JOptionPane.showMessageDialog(null, temp,
							rtext.getString("InfoDialogHeader"),
							JOptionPane.INFORMATION_MESSAGE);
			}

			// If they used the "replace next" tool bar button, make sure the
			// editor gets focused.
			if (isNoSearchUIVisible()) {
				textArea.requestFocusInWindow();
			}

		} catch (PatternSyntaxException pse) {
			// There was a problem with the user's regex search string.
			// Won't usually happen; should be caught earlier.
			JOptionPane.showMessageDialog(rtext,
			"Invalid regular expression:\n" + pse.toString() +
			"\nPlease check your regular expression search string.",
			"Error", JOptionPane.ERROR_MESSAGE);
		} catch (IndexOutOfBoundsException ioobe) {
			// The user's regex replacement string referenced an
			// invalid group.
			JOptionPane.showMessageDialog(rtext,
			"Invalid group reference in replacement string:\n" +
			ioobe.getMessage(),
			"Error", JOptionPane.ERROR_MESSAGE);
		}

	}


	/**
	 * Displays the "find" UI (either a dialog or a tool bar).
	 *
	 * @see #showReplaceUI()
	 */
	public void showFindUI() {
			ensureToolbarsCreated();
			//We always want to mark
			findToolBar.getSearchContext().setMarkAll(true);
			findToolBar.updateMarkings();
			rtext.getCollapsibleSectionPanel().showBottomComponent(findToolBar);
	}


	/**
	 * Displays the "replace" UI (either a dialog or a tool bar).
	 *
	 * @see #showFindUI()
	 */
	public void showReplaceUI() {
			ensureToolbarsCreated();
			//We always want to mark
			replaceToolBar.getSearchContext().setMarkAll(true);
			replaceToolBar.updateMarkings();
			rtext.getCollapsibleSectionPanel().
					showBottomComponent(replaceToolBar);
	}
	
	/**
	 * Handles the changes necessary for hiding the find/replace toolbars
	 * Triggers the actual minimization of the toolbars, and removes the markings
	 */
	public void hideFindReplaceUI()
	{
		//These should be the same, but no harm in making sure
		findToolBar.getSearchContext().setMarkAll(false);
		replaceToolBar.getSearchContext().setMarkAll(false);
		findToolBar.updateMarkings();
		replaceToolBar.updateMarkings();
		rtext.getCollapsibleSectionPanel().hideBottomComponent();
	}


	/**
	 * This method should be called whenever the application Look and Feel
	 * changes.  Updates any realized search-related components.
	 */
	public void updateUI() {
		//Currently there are no such components that need to be updated
	}
	
	public class HideSearchComponentAction extends AbstractAction {

		public void actionPerformed(ActionEvent e) {
			hideFindReplaceUI();
		}

	}
}