package org.fife.rtext.plugins.run;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Document;
import javax.swing.text.Style;

/**
 * Runs a Python program based on file name, outputting to the specified RunTextArea
 * 
 * @author PyDe
 *
 */
class PythonExecutor
{
	
	private RunTextArea output;
	private OutputStream processInput;
	private String outputStyle;
	private String errorStyle;
	
	public PythonExecutor(RunTextArea output, String outputStyle, String errorStyle)
	{
		this.output = output;
		processInput = null;
		this.outputStyle = outputStyle;
		this.errorStyle = errorStyle;
	}
	
	public RunningPythonProcess run(String fileName)
	{
		return run(fileName, "");
	}
	
	public RunningPythonProcess run(String fileName, String args)
	{
		try
		{
			String[] splitArgs = args.split("\\s");
			String[] cmd = new String[3+splitArgs.length];
			cmd[0] = "python";
			cmd[1] = "-u";
			cmd[2] = fileName;
			for (int i = 0; i<splitArgs.length; ++i)
			{
				cmd[i+3] = splitArgs[i];
			}
			Process p = Runtime.getRuntime().exec(cmd);
			
			if (p == null)
				return null; //Python not available or exec failing for some other reason.
			
			processInput = p.getOutputStream();
			
			RunningPythonProcess toReturn = new RunningPythonProcess(p);
			
			DoubleOutputStreamSiphon outputSiphon = new DoubleOutputStreamSiphon(p.getInputStream(), p.getErrorStream(), output, outputStyle, errorStyle, toReturn);
			outputSiphon.start();

  			return toReturn;

		}
		catch (IOException  e)
		{
			return null;
		}
	}	
}