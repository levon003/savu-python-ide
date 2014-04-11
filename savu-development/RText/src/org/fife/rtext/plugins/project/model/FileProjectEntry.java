/*
 * 08/28/2012
 *
 * FileProjectEntry.java - A file in a project.
 * Copyright (C) 2012 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.project.model;

import java.io.File;

/**
 * A project entry reflecting a file on the local file system.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
public class FileProjectEntry extends AbstractProjectEntry {

	private File file;

	public FileProjectEntry(ProjectEntryParent parent, String file) {
		this(parent, new File(file));
	}

	public FileProjectEntry(ProjectEntryParent parent, File file) {
		super(parent);
		this.file = file;
	}

	public void accept(WorkspaceVisitor visitor) {
		visitor.visit(this);
		visitor.postVisit(this);
	}

	public int compareTo(Object o) {
		if (o instanceof FileProjectEntry) {
			return file.compareTo(((FileProjectEntry) o).getFile());
		}
		return -1;
	}

	public File getFile() {
		return file;
	}

	public String getSaveData() {
		return getFile().getAbsolutePath();
	}

	public String getType() {
		return FILE_PROJECT_ENTRY;
	}

	public int hashCode() {
		return file.hashCode();
	}

}