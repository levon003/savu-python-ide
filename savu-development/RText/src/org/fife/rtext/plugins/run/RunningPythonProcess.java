package org.fife.rtext.plugins.run;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Observable;

import org.fife.rtext.ToolBar;

public class RunningPythonProcess extends PythonProcess {
	
	private Process process;
	private OutputStream processInput;
	
	public RunningPythonProcess(Process p) {
		process = p;
		processInput = p.getOutputStream();
	}
	
	public void run()
	{	
		setChanged();
		notifyObservers(); //Let observers know that we are now "started"
		waitForPython();
		setChanged();
		notifyObservers(); //Let observers know that we are now finished
	}
	
	private void waitForPython()
	{
		try {
			process.waitFor();
		} catch (InterruptedException e) {
			waitForPython();
		}
	}
	
	public boolean writeInput(String input)
	{
		byte[] inputBytes = input.getBytes();
		try
		{
			processInput.write(inputBytes);
			processInput.flush();
		}
		catch (IOException e)
		{
			return false;
		}
		return true;
	}
	
	public boolean isRunning()
	{
		try
		{
			process.exitValue();
		}
		catch (IllegalThreadStateException e)
		{
			return true;
		}
		return false;
	}
	
	public void stop()
	{
		if (isRunning())
		{
			process.destroy();
			setChanged();
			notifyObservers();
		}

	}
}
