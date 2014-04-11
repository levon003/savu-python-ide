package org.fife.rtext.plugins.project.tree;

import java.util.List;
import javax.swing.Icon;
import javax.swing.tree.TreeNode;

import org.fife.rtext.plugins.project.ProjectPlugin;

/**
 * Marker interface for a tree node representing a physical folder.
 * 
 * @author Robert Futrell
 * @version 1.0
 */
public interface PhysicalLocationTreeNode extends TreeNode {

	/**
	 * Refreshes the children of this node.
	 */
	void handleRefresh();

	/**
	 * Returns whether this folder tree node has not yet been populated
	 * (expanded).
	 * 
	 * @return Whether this node has not yet been populated.
	 */
	boolean isNotPopulated();

	/**
	 * Refreshes the child nodes of this node. The tree model will need to be
	 * reloaded after making this call.
	 */
	void refreshChildren();

	/**
	 * Dummy class signifying that this tree node has not yet had its children
	 * calculated.
	 */
	static class NotYetPopulatedChild extends AbstractWorkspaceTreeNode {

		public NotYetPopulatedChild(ProjectPlugin plugin) {
			super(plugin);
		}

		public String getDisplayName() {
			return null;
		}

		public Icon getIcon() {
			return null;
		}

		public List getPopupActions() {
			return null;
		}

		public String getToolTipText() {
			return null;
		}

		protected void handleDelete() {
		}

		protected void handleProperties() {
		}

		protected void handleRefresh() {
		}

		protected void handleRename() {
		}

	}

}