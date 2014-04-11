package org.fife.rtext.plugins.debug;

/**
 * Represents a position in a given python source file. Includes a method that the position is in, a filename, and a line number
 * @author PyDe
 *
 */
public class CodePosition {
	
	public String filename;
	public int lineno;
	public String method;
	
	public CodePosition(String filename, int lineno)
	{
		this.filename = filename;
		this.lineno = lineno;
		method = null;
	}
	
	public CodePosition(String filename, int lineno, String method)
	{
		this.filename = filename;
		this.lineno = lineno;
		this.method = method;
	}
	
	public CodePosition()
	{}
	
	public String toString()
	{
		return filename+" "+lineno+" "+method;
	
	}
}
