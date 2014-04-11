package org.fife.rtext.plugins.debug;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

/**
 * Data model for a JXTreeTable intented to store a tree of Variables.  Also handles self-updating from a PythonDebugger's updates.
 * @author PyDe
 *
 */
public class PythonVariableTreeTableModel extends AbstractTreeTableModel implements Observer {

	private final static String[] COLUMN_NAMES = {"Name", "Value", "Type"};
	
	private ComplexVariable root;
	private Icon icon; 
	private JXTreeTable parentTable;
	private ArrayList<TreePath> prevPaths;
	
	public PythonVariableTreeTableModel() {
		super(new Object());
		root = new ComplexVariable("", new ArrayList<Variable>(), new ArrayList<Variable>(), false);
		prevPaths = new ArrayList<TreePath>();
	}
	
	/**
	 * Sets the reference to the JXTreeTable to update when update() is called on this data model.
	 * @param table
	 */
	public void setParentTable(JXTreeTable table) {
		this.parentTable = table;
		table.addTreeExpansionListener(new VariableTreeTableExpansionListener());
	}
	
	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}
	
	//All cells not editable
	@Override
	public boolean isCellEditable(Object node, int column) {
		return false;
	}
	
	//Major method used by TreeTable; will return the actual value for a given Variable "row" and column.
	@Override
	public Object getValueAt(Object node, int column) {
		if (node instanceof ComplexVariable) {
			ComplexVariable c = (ComplexVariable) node;
			switch (column) {
			case 0: //Name
				return c.identifier;
			case 1: //Value
				return c.toString();
			case 2: //Type
				return c.type;
			}
		} else if (node instanceof PrimitiveVariable) {
			PrimitiveVariable p = (PrimitiveVariable) node;
			switch (column) {
			case 0: //Name
				return p.identifier;
			case 1: //Value
				return p.value;
			case 2: //Type
				return p.type;
			}
		}
		return null;
	}

	//Called to fill the root of the tree and whenever a sub-tree is expanded. Will also ensure that identifiers are filled in the returned child.
	@Override
	public Object getChild(Object parent, int index) {
		if (parent != null && parent instanceof ComplexVariable) {
			ComplexVariable c = (ComplexVariable) parent;
			c.fillChildIdentifiers();
			return c.values.get(index);
		}
		root.fillChildIdentifiers();
		return root.values.get(index);
	}

	//Returns the number of children contained by this ComplexVariable
	@Override
	public int getChildCount(Object parent) {
		if (parent instanceof ComplexVariable) {
			ComplexVariable c = (ComplexVariable) parent;
			return c.values.size();
		}
		return root.values.size();
	}

	//Returns the integer index of a particular child Variable within a ComplexVariable's values.
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (parent == null || child == null || isLeaf(parent)) {
			return -1;
		}
		if (!(parent instanceof ComplexVariable)) {
			return -1; //Unclear why we need this check; JXTreeTable apparently calls this with a generic Object sometimes.
		}
		ComplexVariable c = (ComplexVariable) parent;
		return c.values.indexOf((Variable) child);
	}
	
	//Consider all PrimitiveVariables to be leaves.
	@Override
	public boolean isLeaf(Object node) {
		return node instanceof PrimitiveVariable;
	}
	
	/**
	 * Fired whenever the Debugger changes state. If state is READY, will create a separate thread to extract variables from the PythonDebugger and update the model view in the associated tree table.
	 */
	@Override
	public void update(Observable o, Object arg) {
		final PythonDebugger debugger = (PythonDebugger) o;
		//If the debugger is not running, then we need to reset/clear the table of its contents. Just setting root to null causes errors. 
		if (!debugger.isRunning()) {
			root = new ComplexVariable("", new ArrayList<Variable>(), new ArrayList<Variable>(), false);
			parentTable.updateUI();
			prevPaths.clear();
			return;
		} else if (!debugger.isReady()) {
			return;
		}
		
		//This needs to be in a runnable because we're executing in the PythonDebugger thread and this method blocks waiting for the pythondebugger thread to finish
		new Thread(new Runnable() { public void run() {
			Variable[] variables = debugger.getVariables();
			ComplexVariable head = null;
			if (variables[0] instanceof ComplexVariable) {
				head = (ComplexVariable) variables[0];
				ComplexVariable globals = (ComplexVariable) variables[1];
				for (int i = 0; i < globals.values.size(); i++) {
					head.keys.add(globals.keys.get(i));
					head.values.add(globals.values.get(i));
				}
			} else {
				System.err.println("Error: Received a primitive variable while expecting an environment.");
				return; //Received a primitive variable while expecting an environment.
			}
			if (head == null) {
				return;
			}
			root = head;
			
			//Now, update the TreeTable on the EDT
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					parentTable.updateUI();
					if (prevPaths.size() > 0) { //User had previously expanded sub-trees of the data model
						ArrayList<TreePath> paths = new ArrayList<TreePath>(prevPaths);
						prevPaths.clear(); //Clear the previous paths so that new expansions can be saved
						for (TreePath path : paths) {
							parentTable.expandPath(path);
						}
						parentTable.updateUI();
					}
				}});
		}}).start();
	} //End of update
	
	/**
	 * This class records user expansions of container types so that stepping forward will maintain the same expansions.
	 * 
	 * @author levoniaz
	 */
	class VariableTreeTableExpansionListener implements TreeExpansionListener {

		@Override
		public void treeCollapsed(TreeExpansionEvent ev) {
			if (ev != null && prevPaths.contains(ev.getPath()))
				prevPaths.remove(ev.getPath());
		}

		@Override
		public void treeExpanded(TreeExpansionEvent ev) {
			if (ev != null) {
				TreePath path = ev.getPath();
				prevPaths.add(path);
			}
		}
	}

}
