package org.fife.rtext.plugins.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.sun.jndi.url.iiopname.iiopnameURLContextFactory;

/**
 * Represents a complex variable (sequence or mapping type) in the python environment.
 * Has an arraylist of variables that are the values and one for the keys.
 * The values arraylist will always be populated.
 * The keys arraylist will only be populated if this is a mapping type.
 * Classes are represented as a mapping type.
 * @author PyDe
 */
public class ComplexVariable extends Variable {
	
	public ArrayList<Variable> values;
	
	/**
	 * Used to provide the keys in Dictionaries, and the names of variables in other non-list constructions e.g. classes
	 */
	public ArrayList<Variable> keys;
	
	/**
	 * Used to indicate whether this is a "true" mapping type (e.g. dictionary) or not
	 */
	private boolean trueMapping;
	
	/**
	 * 
	 */
	private boolean childIdentifiers = false;
	
	/**
	 * Used to construct a sequence type complex variable. 
	 * @param type The type of the variable, as identified by java
	 * @param values The list of items in the sequence
	 */
	public ComplexVariable(String type, ArrayList<Variable> values) {
		this.type  = type;
		this.values = values;
		this.keys = null;
		trueMapping = false;
	}
	
	/**
	 * Used to construct a mapping type complex variable The key at a given index is assumed to be the key for the value at that same index.
	 * @param type The type of the variable, as identified by Python.
	 * @param keys The keys of the mapping
	 * @param values The values of the mapping.
	 */
	public ComplexVariable(String type, ArrayList<Variable> keys, ArrayList<Variable> values, boolean trueMapping)
	{
		this.type = type;
		this.values = values;
		this.keys = keys;
		this.trueMapping = trueMapping;
	}
	
	/**
	 * Returns an appropriate representation of this complex variable.
	 */
	@Override
	public String toString()
	{
		String returnValueString = "[";
		for (int i = 0; i<this.values.size(); ++i)
		{
			if (keys != null)
			{
				Variable key = keys.get(i);
				if (key instanceof PrimitiveVariable) {
					returnValueString = returnValueString+((PrimitiveVariable) key).value;
				} else { //ComplexVariable
					returnValueString = returnValueString+key.type;
				}
				returnValueString = returnValueString+": ";
			}
			Variable value = values.get(i);
			if (value instanceof PrimitiveVariable) {
				returnValueString = returnValueString+((PrimitiveVariable) value).value;
			} else { //ComplexVariable
				returnValueString = returnValueString+value.type;
			}
			if (i != values.size() - 1) {
				returnValueString += ", ";
			}
		}
		if (!isTrueMapping() && keys != null)
		{
			return type+returnValueString + "]";
		}
		return returnValueString + "]";
	}
	
	/**
	 * 
	 * @return True if identifiers have been calculated for this Variable's children, False otherwise.
	 */
	public boolean areChildIdentifiersFilled() {
		if (this.childIdentifiers) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * If the child Variables of this complex variable have not been filled, copies from keys or generates names to use as an identifier for that variable.
	 */
	public void fillChildIdentifiers() {
		if (areChildIdentifiersFilled()) {
			return;
		}
		if (keys == null) {
			for (int i = 0; i < values.size(); i++) {
				values.get(i).identifier = "[" + Integer.toString(i) + "]";
			}			
		} else { //Keys have named values
			for (int i = 0; i < values.size(); i++) {
				Variable nameVar = keys.get(i);
				Variable childVar = values.get(i);
				if (nameVar instanceof PrimitiveVariable) {
					String identifier = ((PrimitiveVariable)nameVar).value;
					if (identifier.startsWith("'") && identifier.endsWith("'")) { //Strip single quotes from non-strings, to avoid visual confusion.
						identifier = identifier.substring(1, identifier.length()-1);
					}
					childVar.identifier = identifier;
				} else { //Complex Variable; this case means that this parent is a dict object and this child is a value keyed by a hashable non-primitive object.
					if (!((ComplexVariable)nameVar).isTrueMapping()) {
						childVar.identifier = nameVar.type;
					} else { //Not a true mapping; e.g. a class
						childVar.identifier = nameVar.toString();
					}
				}
			}
			//TODO LOW PRIO Implement a sorting here if we want alphabetic rather than local -> global sorting.
			/*Collections.sort(values, new Comparator<Variable>() {
				@Override
		        public int compare(Variable  v1, Variable  v2)
		        {
					return v1.identifier.compareTo(v2.identifier);
		        }
			});*/
		}		
	}
	
	/**
	 * Checks whether this is a "true" mapping type (e.g. dictionary)
	 * @return True if a true mapping type, false otherwise
	 */
	public boolean isTrueMapping()
	{
		//return false;
		return trueMapping; //TODO LOW PRIO determine if a better true mapping system is needed
	}
	
	/**
	 * Allows an external source to set the value of the true mapping variable. Use with caution.
	 * @param isTrueMapping The new value that will be returned by isTrueMapping.
	 */
	public void setTrueMapping(boolean isTrueMapping)
	{
		trueMapping = isTrueMapping;
	}
}
