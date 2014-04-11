package org.fife.rtext.plugins.debug;

import java.util.Arrays;

/**
 * Represents a variable in a python environment
 * All variables are subclasses of this, and thus all variables have types
 * @author PyDe
 *
 */
public abstract class Variable{
	public String type;
	public String identifier = null;
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof Variable)
		{
			boolean sameType = (this instanceof ComplexVariable && o instanceof ComplexVariable) || (this instanceof PrimitiveVariable && o instanceof PrimitiveVariable);
			return sameType && this.type.equals(((Variable)o).type) && this.identifier.equals(((Variable)o).identifier);
		}
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return Arrays.hashCode(new String[] {type, identifier});
	}
}
