package org.fife.rtext.plugins.debug;

/**
 * This class contains static methods to match lines of input against known patterns of pdb output.
 * Every method will return true if its input is a complete example of the pattern, or if it is the beginning of a string that would match the pattern.
 * @author PyDe
 *
 */
public class PDBRecognizer {
	
	/**
	 * An array of all built-in exceptions types.
	 */
	private static String[] exceptionTypes = {
		"Exception:",
		"StandardError:",
		"ArithmeticError:",
		"BufferError:",
		"LookupError:",
		"EnvironmentError:",
		"AssertionError:",
		"AttributeError:",
		"EOFError:",
		"FloatingPointError:",
		"IOError:",
		"ImportError:",
		"IndexError:",
		"KeyError:",
		"MemoryError:",
		"NameError:",
		"NotImplementedError:",
		"OSError:",
		"OverflowError:",
		"ReferenceError:",
		"RuntimeError:",
		"SyntaxError:",
		"IndentationError:",
		"TabError:",
		"SystemError:",
		"TypeError:",
		"UnboundLocalError:",
		"UnicodeError:",
		"UnicodeEncodeError:",
		"UnicodeDecodeError:",
		"UnicodeTranslateError:",
		"ValueError:",
		"VMSError:",
		"WindowsError:",
		"ZeroDivisionError:"
	};
	
	/**
	 * The string used to start all savupdb output lines
	 */
	public static String pdbPrefix = "Savu"+(char)0x19+"Pdb"+(char)0x15+"Output";
	
	/**
	 * Tests whether the data string starts with the given prefix, or could be the start of a string that start with the prefix
	 * @param data The string to test
	 * @param prefix The prefix that might start data
	 */
	private static boolean couldStartWith(String data, String prefix)
	{
		if (data.length() >= prefix.length())
		{
			return data.startsWith(prefix);
		}
		else
		{
			return prefix.substring(0, data.length()).equals(data);
		}
	}
	
	/**
	 * 	Checks to see if the line is confirming initialization of a breakpoint or partial version of such a line
	 * @param response
	 * @return
	 */
	public static boolean isPartialBreakpoint(String response)
	{
		if (couldStartWith(response, pdbPrefix))
		{
			String[] splitResponse = response.substring(pdbPrefix.length()).split(" ");
			if (splitResponse.length > 1 && splitResponse[0].equals("Breakpoint"))
			{
				return true;
			}
			else if ("Breakpoint ".contains(response))
			{
				return true;
			}
			}
		return false;
	}
	
	/**
	 * Checks to see if the line is a code position or part of one
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialCodePosition(String outputLine)
	{
		if (couldStartWith(outputLine, pdbPrefix))
		{
			String truncatedOutput = outputLine.substring(pdbPrefix.length());
			if (truncatedOutput.length() == 1)
			{
				return truncatedOutput.charAt(0) == '>';
			}
			else
			{
				return truncatedOutput.charAt(0) == '>' && truncatedOutput.charAt(1) == ' ';
			}
		}
		return false;
		
	}
	
	/**
	 * Checks to see if the input matches the pdb output pattern for a line to display the code position
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialCodeLine(String outputLine)
	{
		if (couldStartWith(outputLine, pdbPrefix))
		{
			String truncatedOutput = outputLine.substring(pdbPrefix.length());
			if (truncatedOutput.length() == 1)
			{
				return truncatedOutput.charAt(0) == '-';
			}
			else if (truncatedOutput.length() == 2)
			{
				return truncatedOutput.charAt(0) == '-' && truncatedOutput.charAt(1) == '>';
			}
			else
			{
				return truncatedOutput.charAt(0) == '-' && truncatedOutput.charAt(1) == '>' && truncatedOutput.charAt(2) == ' ';
			}
		}
		return false;
	}
	
	/**
	 * Checks to see if the input matches the pdb finished executing output
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialDone(String outputLine)
	{
		if ((pdbPrefix+"The program finished and will be restarted\n").contains(outputLine))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Checks to see if the input mentions an uncaught exception
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialError(String outputLine)
	{
		if ((pdbPrefix+"Uncaught exception. Entering post mortem debugging\n").contains(outputLine))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Checks to see if the line indicates a built-in type of exception
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialException(String outputLine)
	{
		for (int i = 0; i<exceptionTypes.length; ++i)
		{
			int minLength = Math.min(outputLine.length(), exceptionTypes[i].length());
			if (outputLine.substring(0, minLength).equals(exceptionTypes[i].substring(0, minLength)))
				return true;
		}
		return false;
	}
	
	/**
	 * checks to see if the input matches a pdb response to step in
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialCall(String outputLine)
	{
		if ((pdbPrefix+"--Call--\n").contains(outputLine))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * checks to see if the input matches a pdb response to step out
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialReturn(String outputLine)
	{
		if ((pdbPrefix+"--Return--\n").contains(outputLine))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * checks to see if the input matches a pdb blank line notification
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialBlank(String outputLine)
	{
		if ((pdbPrefix+"*** Blank or comment\n").contains(outputLine))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Checks to see if the input matches a pdb prompt
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialPrompt(String outputLine)
	{
		if ((pdbPrefix+"(Pdb) ").contains(outputLine))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Checks to see if the input matches a full pdb prompt
	 * @param outputLine
	 * @return
	 */
	public static boolean isPDBPrompt(String outputLine)
	{
		return outputLine.equals((pdbPrefix+"(Pdb) "));
	}
	
	/**
	 * Determines if the input is pdb output that we do not need
	 * @param outputLine
	 * @return
	 */
	public static boolean isIgnorablePDBLine(String outputLine)
	{
		return isPartialPrompt(outputLine) || isPartialCall(outputLine) || 
				 isPartialReturn(outputLine) || isPartialCodeLine(outputLine) ||
				 isPartialBlank(outputLine);
	}
	
	/**
	 * Determines if the input is something that PDB produced
	 * @param outputLine
	 * @return
	 */
	public static boolean isPartialPDBLine(String outputLine)
	{
		return couldStartWith(outputLine, pdbPrefix) || isPartialException(outputLine);
	}
}
