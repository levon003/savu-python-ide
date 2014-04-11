package org.fife.rtext.plugins.run;

import java.util.Observable;

public abstract class PythonProcess extends Observable implements Runnable {
	/**
	 * Writes the provided string to the process' input stream
	 * @param input The data to write to the process' input
	 * @return Whether writing succeeded or not
	 */
	public abstract boolean writeInput(String input);
	
	/**
	 * Determines whether or not the process is running
	 * @return True if the process is still running, false if it's not
	 */
	public abstract boolean isRunning();
	
	/**
	 * Stops the process if it's running
	 */
	public abstract void stop();
}
