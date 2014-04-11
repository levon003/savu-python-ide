/*
 * 08/28/2012
 *
 * Project.java - A logical representation of a programming project.
 * Copyright (C) 2012 Robert Futrell
 * http://fifesoft.com/rtext
 * Licensed under a modified BSD license.
 * See the included license file for details.
 */
package org.fife.rtext.plugins.project.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A project is a logical collection of {@link ProjectEntry}s. For maximum
 * flexibility, this plugin does not require all entries in a project to live in
 * a common root folder.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
public class Project implements Comparable, ProjectEntryParent {

	private Workspace workspace;
	private String name;
	private List entries;

	public Project(Workspace workspace, String name) {
		this.workspace = workspace;
		setName(name);
		entries = new ArrayList();
	}

	public void accept(WorkspaceVisitor visitor) {
		visitor.visit(this);
		for (Iterator i = getEntryIterator(); i.hasNext();) {
			((ProjectEntry) i.next()).accept(visitor);
		}
		visitor.postVisit(this);
	}

	public void addEntry(ProjectEntry entry) {
		entries.add(entry);
	}

	public int compareTo(Object o) {
		if (o instanceof Project) {
			return getName().compareTo(((Project) o).getName());
		}
		return -1;
	}

	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		return compareTo(o) == 0;
	}

	/**
	 * Returns the index of the specified project entry.
	 * 
	 * @param entry
	 *            The entry to look for.
	 * @return The index of the entry, or <code>-1</code> if it is not contained
	 *         in this project.
	 */
	private int getEntryIndex(ProjectEntry entry) {
		for (int i = 0; i < entries.size(); i++) {
			if (entry == entries.get(i)) {
				return i;
			}
		}
		return -1;
	}

	public Iterator getEntryIterator() {
		return entries.iterator();
	}

	public String getName() {
		return name;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public int hashCode() {
		return getName().hashCode();
	}

	public boolean moveProjectEntryDown(ProjectEntry entry) {
		int index = getEntryIndex(entry);
		if (index > -1 && index < entries.size() - 1) {
			entries.remove(index);
			entries.add(index + 1, entry);
			return true;
		}
		return false;
	}

	public boolean moveProjectEntryUp(ProjectEntry entry) {
		int index = getEntryIndex(entry);
		if (index > 0) {
			entries.remove(index);
			entries.add(index - 1, entry);
			return true;
		}
		return false;
	}

	public void removeEntry(ProjectEntry entry) {
		entries.remove(entry);
	}

	public void removeFromWorkspace() {
		workspace.removeProject(this);
	}

	public void setName(String name) {
		this.name = name;
	}

}