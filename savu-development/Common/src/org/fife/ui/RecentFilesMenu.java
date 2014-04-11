/*
 * 04/14/2012
 *
 * RecentFilesMenu.java - A menu with items to open recently-opened files.
 * Copyright (C) 2012 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;


/**
 * A menu that keeps track of recently opened files.  Whenever a file is
 * opened, it is moved to the "top" of the list, as it is assumed that popular
 * files will get re-opened over and over again.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class RecentFilesMenu extends JMenu {

	/**
	 * The maximum length we will display in the menu. All filepaths or truncated filepaths with prepended prefixes must be <= this value
	 */
	private int MAX_FILEPATH_LENGTH = 30;
	/**
	 * The prefix with which to start file paths than have been truncated
	 */
	private String FILE_TRUNCATION_PREFIX = "...";
	
	private int maxFileHistorySize;
	
	/**
	 * A cache of the full paths of files in this menu, to allow us to know
	 * the index of a specific item to move it to the top later.
	 * Each item is a 2-element array, with the first element the full file path and the second the truncatedfile path
	 */
	private List<String[]> fileHistory;

	private static final int DEFAULT_MAX_SIZE = 20;
	

	/**
	 * Constructor.
	 *
	 * @param name The name for this menu item, e.g. "Recent Files".
	 */
	public RecentFilesMenu(String name) {
		this(name, (List<String>)null);
	}


	/**
	 * Constructor.
	 *
	 * @param name The name for this menu item, e.g. "Recent Files".
	 * @param initialContents The initial contents of this menu.
	 */
	public RecentFilesMenu(String name, List<String> initialContents) {
		super(name);
		this.maxFileHistorySize = DEFAULT_MAX_SIZE;
		fileHistory = new ArrayList<String[]>(maxFileHistorySize);
		if (initialContents!=null) {
			for (Iterator<String> i=initialContents.iterator(); i.hasNext(); ) {
				addFileToFileHistory(i.next());
			}
		}
	}


	/**
	 * Constructor.
	 *
	 * @param name The name for this menu item, e.g. "Recent Files".
	 * @param initialContents The initial contents of this menu.
	 */
	public RecentFilesMenu(String name, String[] initialContents) {
		this(name, initialContents!=null ?
				Arrays.asList(initialContents) : (List)null);
	}


	/**
	 * Adds the file specified to the file history.
	 *
	 * @param fileFullPath Full path to a file to add to the file history in
	 *        the File menu.
	 * @see #getShouldIgnoreFile(String)
	 */
	public void addFileToFileHistory(String fileFullPath) {

		if (getShouldIgnoreFile(fileFullPath)) {
			return;
		}
		
		String[] filePaths = new String[2];
		filePaths[0] = fileFullPath;
		filePaths[1] = truncateFilePath(fileFullPath);

		JMenuItem menuItem;

		// If the file history already contains this file, remove it and add
		// it back to the top of the list; this keeps the list in a "most
		// recently opened" order.
		int index = -1;
		for (int i = 0; i<fileHistory.size(); ++i)
		{
			if (Arrays.equals(filePaths, fileHistory.get(i)))
			{
				index = i;
				break;
			}
		}
		if (index>-1) {
			// Remove it physically from the menu and add it back at the
			// top, then remove it from the path history and it its path
			// to the top of that.
			menuItem = (JMenuItem)getMenuComponent(index);
			remove(index);
			insert(menuItem, 0);
			String[] temp = fileHistory.remove(index);
			fileHistory.add(0, temp);
			return;
		}

		// Add the new file to the top of the file history list.
		Action openAction = createOpenAction(fileFullPath);
		openAction.putValue(Action.NAME, filePaths[1]);
		menuItem = new JMenuItem(openAction);
		insert(menuItem, 0);
		fileHistory.add(0, filePaths);

		// Too many files?  Oust the file in history added least recently.
		if (getItemCount()>maxFileHistorySize) {
			remove(getItemCount()-1);
			fileHistory.remove(fileHistory.size()-1);
		}

	}
	
	/**
	 * Truncates a file name so it is less than MAX_FILEPATH_LENGTH characters long
	 * Will attempt to truncate the path on path separator characters, but if this fails it will blindly truncate
	 * @param fileFulePath The file path to truncate
	 * @return The truncated file path
	 */
	protected String truncateFilePath(String fileFullPath)
	{
		if (fileFullPath.length() <= MAX_FILEPATH_LENGTH)
		{
			return fileFullPath;
		}
		int truncationIndex = 0;
		while (fileFullPath.length() - truncationIndex > MAX_FILEPATH_LENGTH-FILE_TRUNCATION_PREFIX.length())
		{
			int newTruncationPoint = fileFullPath.indexOf((int)File.separatorChar, truncationIndex+1);
			if (newTruncationPoint < 0) break;
			truncationIndex = newTruncationPoint;
		}
		//Determine whether the nice truncation works, or whether we have to truncate badly.
		if (fileFullPath.length() - truncationIndex <= MAX_FILEPATH_LENGTH-FILE_TRUNCATION_PREFIX.length() )
		{
			return FILE_TRUNCATION_PREFIX+fileFullPath.substring(truncationIndex);
		}
		else
		{
			return FILE_TRUNCATION_PREFIX+fileFullPath.substring(fileFullPath.length() - MAX_FILEPATH_LENGTH-FILE_TRUNCATION_PREFIX.length());
		}
	}


	/**
	 * Creates the action that will open the specified file in the application
	 * when its menu item is selected.  Subclasses should override.
	 *
	 * @param fileFullPath The selected file.
	 * @return The action that will be invoked.
	 */
	protected abstract Action createOpenAction(String fileFullPath);


	/**
	 * Returns the full path to the specified file in this history.
	 *
	 * @param index The index of the file.
	 * @return The file.
	 * @see #getFileHistory()
	 */
	public String getFileFullPath(int index) {
		return fileHistory.get(index)[0];
	}


	/**
	 * Returns an ordered copy of the recent files.  This can be used
	 * to save them when closing an application.
	 *
	 * @return The file history.  This will be an empty list if no files
	 *         have been opened.
	 * @see #getFileFullPath(int)
	 */
	public List getFileHistory() {
		return new ArrayList(fileHistory);
	}


	/**
	 * Returns the maximum number of files the file history in the File menu
	 * will remember.
	 *
	 * @return The maximum size of the file history.
	 */
	public int getMaximumFileHistorySize() {
		return maxFileHistorySize;
	}


	/**
	 * Provides a hook for subclasses to not remember specific files.  If
	 * <code>addFileToFileHistory()</code> is called but the file specified
	 * does not pass this method's criteria, it will not get added to history.
	 * The default implementation does nothing.
	 *
	 * @param fileFullPath The file to (possibly) ignore.
	 * @return Whether to ignore the file.  The default implementation always
	 *         returns <code>false</code>.
	 * @see #addFileToFileHistory(String)
	 */
	protected boolean getShouldIgnoreFile(String fileFullPath) {
		//Make sure that the python tutorial files are not added to the recent files list
		//Set the filepaths here
		String PATH_TO_BUGGED_FILE = "lib/SavuTutorialPart1.py";
		String PATH_TO_CLEAN_FILE = "lib/SavuTutorialPart2.py";
		int FFPLength = fileFullPath.length();
		if  (FFPLength < PATH_TO_BUGGED_FILE.length() || FFPLength < PATH_TO_CLEAN_FILE.length())
			return false;
		String finalXcharsPart1 = fileFullPath.substring(FFPLength-PATH_TO_BUGGED_FILE.length(), FFPLength);
		String finalXcharsPart2 = fileFullPath.substring(FFPLength-PATH_TO_CLEAN_FILE.length(), FFPLength);
		if(finalXcharsPart1.equals(PATH_TO_BUGGED_FILE) || finalXcharsPart2.equals(PATH_TO_CLEAN_FILE)){
			return true;
		}

		
		return false;
	}


	/**
	 * Sets the maximum number of files to remember in history.
	 *
	 * @param newSize The new size of the file history.
	 */
	public void setMaximumFileHistorySize(int newSize) {

		if (newSize<0)
			return;

		// Remember the new size.
		maxFileHistorySize = newSize;

		// If we're bigger than the new max size, trim down
		while (getItemCount()>maxFileHistorySize) {
			remove(getItemCount()-1);
			fileHistory.remove(fileHistory.size()-1);
		}

	}


}