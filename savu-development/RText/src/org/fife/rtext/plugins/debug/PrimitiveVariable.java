package org.fife.rtext.plugins.debug;

/**
 * Represents a "primitive" variable in python.
 * In this case, primitive means that it has only a single value.
 * Most simple data types (strings, ints, etc) are considered to be primitives.
 * @author PyDe
 *
 */
public class PrimitiveVariable extends Variable{
		
	/**
	 * All PrimitiveVariables will have this value instantiated. It is the string representation of the variable's value.
	 */
	public String value;

	/**
	 * Construct a primitive type variable
	 * @param type The variable's type, as determined by python
	 * @param value The string representation of the variable's value (produced by repr).
	 */
	public PrimitiveVariable(String type, String value){
		this.type = type;
		this.value = value;
	}
	
	/**
	 * Represent a primitive variable with its value.
	 */
	@Override
	public String toString()
	{
		return this.value;
	}
	
}
