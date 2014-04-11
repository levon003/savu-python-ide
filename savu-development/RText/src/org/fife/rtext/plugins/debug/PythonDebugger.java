package org.fife.rtext.plugins.debug;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.fife.rtext.plugins.run.PythonProcess;

/**
 * Class that runs a pdb session on the specified python file
 * A new PythonDebugger object must be created for each run
 * Implements Observable, and will notify observers whenever its state changes
 * Commands can only be issued to the debugger if it is in the ready state
 * @author PyDe
 *
 */
public class PythonDebugger extends PythonProcess{
	
	/**
	 * The output of this debugger. The output of the program is written to this stream.
	 */
	private OutputStream output;
	
	/**
	 * The pdb process
	 */
	private Process p;
	/**
	 * The streams communicating to the pdb process. Data written to this stream is passed to pdb.
	 */
	private OutputStream debugInput;
	/**
	 * The error output stream of the pdb process. This stream is not modified by the debugger
	 */
	private InputStream errorStream;
	/**
	 * The stdout of pdb that is read by this debugger
	 */
	private InputStream debugOutput;
	
	/**
	 * The current position in the code
	 */
	private CodePosition curCodePosition;
	private Lock codePositionLock;
	
	/**
	 * A lock that must be held to read or write from the input queue.
	 * If there is no input in the queue, we will wait on the condition. The condition must be notified when input is added.
	 */
	private Lock inputLock;
	private Condition commandPresent;
	
	/**
	 * Indicates whether or not initial setup (queuing breakpoints) has been completed yet
	 */
	private boolean setup;
	
	/**
	 * A helper class representing a command to be passed to the debugger.
	 * @author PyDe
	 *
	 */
	private class DebuggerCommand{
		//The actual command to be written to pdb
		private byte[] command;
		//The debugger state that should result from executing this command
		private DebuggerState resultState;
		//If this command produces a response it will be stored here when it is available
		private String response = "";
		
		//The reponseComplete condition will be notified when the response has been produced
		private Lock notificationLock;
		private Condition responseComplete;
		
		//Call this method when a response has been provided
		public void alertResponseComplete()
		{
			this.notificationLock.lock();
			this.responseComplete.signal();
			this.notificationLock.unlock();
		}
	}
	
	/**
	 * The list of all commands to be executed by the debugger. If commands are in the queue the debugger will execute the next without proceeding to the ready state
	 */
	private Queue<DebuggerCommand> inputCommands;
	
	
	/**
	 * The list of all states the debugger can be in.
	 * START - the debugger has not started running
	 * READY - pdb is waiting for input, it is legal to call commands such as step on the debugger
	 * RUNNING - the program is executing
	 * DONE - the program has finished executing
	 * BREAKPOINTSET - a breakpoint has been set but pdb has not yet returned to the prompt
	 * AWAITINGRESPONSE - a command has been issued that demands a response, that response has not yet been delivered
	 * HIDDENRUNNING - used for commands that should be "invisible" to observers. In general, if the state is ready and 
	 * the user inputs a command, they should not be updated when the state changes away from ready because they requested 
	 * data from the debugger
	 * @author PyDe
	 *
	 */
	private enum DebuggerState 
	{
		START, READY, RUNNING, BREAKPOINTSET, DONE, AWAITINGRESPONSE, HIDDENRUNNING
	}
	
	/**
	 * This variable must only be modified from the PythonDebugger's own thread. All such modifications must be within a synchronized block.
	 * Reads within the PythonDebugger's own thread do not need to be synchronized, as the variable cannot be modified elsewhere
	 */
	private volatile DebuggerState state;
	
	/**
	 * Used during parsing. Indicates whether the current line could be the output of the pdb process. False positives are allowed, false negatives are not.
	 * Needs to be reset to true after every line. The only case in which it is acceptable to modify it outside the debugger thread is if user input ends the line.
	 */
	private Boolean lineCouldBePDB;
	
	/**
	 * The arguments for the debugging process
	 */
	private String fileName;
	private String args;
	private List<Breakpoint> breakpoints;
	
	/**
	 * Public constructor. Creates a new debugger for the specified file and queues up pdb to run.
	 * The caller must call start() on the thread of this runnable to begin debugging
	 * @param output The stream to which stdout from the program is written
	 * @param fileName The file name to be debugged
	 * @param breakpoints A list of breakpoints to add to the file at the beginning of the run
	 * @param args A list of command line arguments to pass to the program
	 */
	public PythonDebugger(OutputStream output, String fileName, List<Breakpoint> breakpoints, String args) throws NullPointerException
	{
		super();
		this.output = output;
		this.fileName = fileName;
		this.args = args;
		this.breakpoints = breakpoints;
		codePositionLock = new ReentrantLock();
		inputLock = new ReentrantLock();
		commandPresent = inputLock.newCondition();
		inputCommands = new LinkedList<DebuggerCommand>();
		state = DebuggerState.START;
		setup = false;
		
		synchronized(state)
		{
			state = DebuggerState.START;
		}
		//Start the pdb process
		p = null;
		try {
			String[] splitArgs = args.split("\\s");
			String[] cmd = new String[5+splitArgs.length];
			cmd[0] = "python";
			cmd[1] = "-u";
			cmd[2] = "-m";
			cmd[3] = "savupdb";
			cmd[4] = fileName;
			for (int i = 0; i<splitArgs.length; ++i) {
				cmd[i+5] = splitArgs[i];
			}
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList(cmd));
			//Modify the environment so we can find the modified pdb module
			Map<String, String> env = pb.environment();
			env.put("PYTHONPATH", env.get("PYTHONPATH")+File.pathSeparator+System.getProperty("user.dir")+File.separator+"lib");
			p = pb.start();
		} catch (IOException e) {
			// PANIC
			e.printStackTrace();
		}
		debugInput = p.getOutputStream();
		debugOutput = p.getInputStream();
		errorStream = p.getErrorStream();
	
		//Indicates whether we think the portion of the current line we've seen so far is a pdb message
		lineCouldBePDB = new Boolean(true);
	}
	
	/**
	 * Monitors the pdb output and updates state
	 * As usual, call Thread.start() instead of this method
	 */
	public void run()
	{
		try
		{				
			StringBuilder charBuffer = new StringBuilder();
			int temp;
			
			DebuggerCommand lastCommand = null;
			
			//Parse loop
			while(state != DebuggerState.DONE)
			{
				//We read one character at a time
				temp = debugOutput.read();
				
				//If the stream has closed, we're done. Also probably screwed.
				if (temp == -1)
				{
					this.output.close();
					charBuffer.delete(0, charBuffer.length());
					break;
				}
				charBuffer.append((char)temp);
				//System.out.print((char)temp);
				
				String lineFragment = charBuffer.toString();

				//This variable should not change while we're processing a character
				synchronized(lineCouldBePDB)
				{
					//If we've now processed a full line
					if (temp=='\n')
					{
						//System.out.print(lineFragment);
						//If awaiting a response to a stack trace or variable call we ignore partial lines
						//Full lines should be appended to the response object, not handled
						if (state == DebuggerState.AWAITINGRESPONSE)
						{
							lastCommand.response += charBuffer.toString();
							charBuffer.delete(0, charBuffer.length());
						}
						else
						{
							//Determine whether this is a pdb produced line or something we should pass to the user
							//A pdb message we should ignore
							if (lineCouldBePDB && PDBRecognizer.isIgnorablePDBLine(lineFragment))
							{
								charBuffer.delete(0, charBuffer.length());
							}
							//Special cases need special parsing
							else if (lineCouldBePDB && PDBRecognizer.isPartialCodePosition(lineFragment))
							{
								codePositionLock.lock();
								curCodePosition = getCodePosition(lineFragment);
								codePositionLock.unlock();
								charBuffer.delete(0, charBuffer.length());
							}
							else if (lineCouldBePDB && PDBRecognizer.isPartialBreakpoint(lineFragment) && state == DebuggerState.BREAKPOINTSET)
							{
								charBuffer.delete(0, charBuffer.length());
								//The next line should be a prompt, so we don't need to do anything here
							}
							else if (lineCouldBePDB && PDBRecognizer.isPartialException(lineFragment))
							{
								//This implies that we've hit an exception while doing a step over, or equivalent. If we continue, the exception will become unhandled
								inputLock.lock();
								inputCommands.clear();
								DebuggerCommand continueCommand = new DebuggerCommand();
								continueCommand.command = "c\n".getBytes();
								continueCommand.resultState = DebuggerState.RUNNING;
								inputCommands.add(continueCommand);
								inputLock.unlock();
								charBuffer.delete(0, charBuffer.length());
							}
							else if (lineCouldBePDB && (PDBRecognizer.isPartialDone(lineFragment) || PDBRecognizer.isPartialError(lineFragment)))
							{
								//If we crashed, we must send two quit commands to pdb before it will exit
								if (PDBRecognizer.isPartialError(lineFragment))
								{
									debugInput.write("quit\n".getBytes());
									debugInput.flush();
								}
								synchronized(state)
								{
									state = DebuggerState.DONE;
								}
								setChanged();
								notifyObservers();
							}
							//In these cases, we pass the output to the user
							else if (!lineCouldBePDB)
							{
								this.output.write(lineFragment.getBytes());
								charBuffer.delete(0, charBuffer.length());
								lineCouldBePDB = true;
							}
							else
							{
								this.output.write(lineFragment.getBytes());
								charBuffer.delete(0, charBuffer.length());
							}
						}
						
					}
					//Otherwise, we don't have a complete line
					else
					{
						//If it's a prompt, we need to provide it with input of some sort (or alert the gui that we need input)
						if (PDBRecognizer.isPDBPrompt(lineFragment))
						{
							if (!setup)
							{
								performSetup();
							}
							
							if (state == DebuggerState.AWAITINGRESPONSE)
							{
								lastCommand.alertResponseComplete();
							}
							
							//If we have a prompt, check to see if we have a command to provide
							inputLock.lock();
							//If there are queued commands, we skip ready and execute the command immediately
							if (inputCommands.size() == 0)
							{
								CodePosition curPosition = getCurrentCodePosition();
								File test = null;
								if (curPosition != null)
								{
									test = new File(curPosition.filename);
								}
								//PDB occasionally stops on lines that are not in any file. We don't want to stop on them
								if (test == null || !test.exists() || !test.isFile())
								{
									DebuggerCommand takeStep = new DebuggerCommand();
									takeStep.resultState = DebuggerState.RUNNING;
									takeStep.command = "n\n".getBytes();
									inputCommands.add(takeStep);
								}
								else
								{
								
									//Update the state to ready so the user knows to provide a command
									DebuggerState lastState;
									synchronized(state)
									{
										lastState = state;
										state = DebuggerState.READY;
									}
									//If we just ran a user command they already know that the state is ready and we don't need to re-update them
									if (lastState != DebuggerState.AWAITINGRESPONSE)
									{
										setChanged();
										notifyObservers();
									}
									
									//Wait for the next user command
									commandPresent.await();
								}
								
							}
							
							//Execute the next command
							DebuggerCommand toRun = inputCommands.remove();
							inputLock.unlock();
							
							if (toRun.resultState == DebuggerState.DONE && toRun.command[0] == -1)
							{
								break;
							}
							
							debugInput.write(toRun.command);
							debugInput.flush();
							lastCommand = toRun;
							
							
							synchronized(state)
							{
								state = toRun.resultState;
							}
							
							//If we're executing a command that returns output, the observers shouldn't be notified that running is taking place
							if (state != DebuggerState.AWAITINGRESPONSE && state != DebuggerState.HIDDENRUNNING)
							{
								setChanged();
								notifyObservers();
							}
							charBuffer.delete(0, charBuffer.length());
							lineCouldBePDB = true;
						}
						//Check if it could be a pdb line
						else if (!PDBRecognizer.isPartialPDBLine(lineFragment))
						{
							lineCouldBePDB = false;
						}
						//If it can't be a pdb line we can start passing output to the user immediately
						if (!lineCouldBePDB && state != DebuggerState.AWAITINGRESPONSE)
						{
							this.output.write(charBuffer.toString().getBytes());
							charBuffer.delete(0, charBuffer.length());
						}
					}
				}
			}
			
			//We need to write a final quit to close out pdb
			debugInput.write("quit\n".getBytes());
			debugInput.flush();
			p.waitFor();
  						
		}
		catch (IOException | InterruptedException e)
		{
		}
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Performs initial setup, enqueueing breakpoints and a continue, if applicable.
	 * Also sets setup to true.
	 */
	private void performSetup()
	{
		if (enqueueInitialBreakpoints())
		{
			//Once we've set all the breakpoints we actually want to start running the program, but only if we aren't supposed to be paused
			enqueueContinue();
		}
		setup = true;
	}
	
	/**
	 * Enqueues breakpoint commands for all breakpoints in the class variable. Returns whether or not a continue should also be enqueued.
	 * @return false if the debugger is currently paused on a line with a breakpoint set on it, true if not. Indicates if a continue should be enqueued afterward.
	 */
	private boolean enqueueInitialBreakpoints()
	{
		boolean shouldContinue = true;
		CodePosition curPosition = getCurrentCodePosition();
		//Set the breakpoints provided as arguments
		for (int i = 0; i<breakpoints.size(); ++i)
		{
			enqueueBreakpoint(breakpoints.get(i));
			//If we're trying to set a breakpoint on the current position, we should not continue after enqueueing breakpoints
			if (breakpoints.get(i).getLineNum() == curPosition.lineno && breakpoints.get(i).getFilePath().equals(curPosition.filename))
			{
				shouldContinue = false;
			}
		}
		return shouldContinue;
	}
	
	/**
	 * Creates a code position object from a pdb output line indicating the position in the code
	 * @param response The output from pdb to be processed into a codeposition object
	 * @return A CodePosition object that represents the location described in the pdb output line
	 */
	private CodePosition getCodePosition(String response)
	{
		String[] splitPosition = response.split("\\(\\d+\\)");
		if (splitPosition.length < 2)
		{
			return null;
		}
		String method = splitPosition[splitPosition.length-1];
		
		int stopIndex = response.substring(0, splitPosition[0].length()+2).lastIndexOf('(');
		//We start the substring at 2 to clip off the initial '> ' and the savupdb prefix
		String filePath = response.substring(PDBRecognizer.pdbPrefix.length()+2, stopIndex);
		
		int lineNumber = Integer.parseInt(response.substring(stopIndex+1, response.length()-method.length() - 1));
	
		return new CodePosition(filePath, lineNumber, method);
	}
	
	/**
	 * Adds a command to the queue of commands to be executed when pdb gives us a prompt
	 * @param command The bytes representing the command to be executed (should be a valid pdb command. see: http://docs.python.org/2/library/pdb.html#debugger-commands)
	 * @param resultState The state the debugger should be in after executing this command. In most cases, this will probably be RUNNING
	 * @param responseExpected Whether or not the command expects a response
	 * @return The DebuggerCommand object representing the command that was enqueued. This object will be updated with the response, if any. The notificationLock on the response object will be pre-locked.
	 */
	private DebuggerCommand enqueueCommand(byte[] command, DebuggerState resultState, boolean responseExpected)
	{
		inputLock.lock();
		//Create a new DebuggerCommand object to return
		DebuggerCommand toAdd = new DebuggerCommand();
		toAdd.command = command;
		toAdd.resultState = resultState;
		if (responseExpected)
		{
			toAdd.notificationLock = new ReentrantLock();
			//We need to lock here so that we don't miss being notified of the command's execution
			toAdd.notificationLock.lock();
			toAdd.responseComplete = toAdd.notificationLock.newCondition();
		}
		inputCommands.add(toAdd);
		//Alert the Debugger that a new command is present
		commandPresent.signal();
		inputLock.unlock();
		return toAdd;
	}
	
	/*
	 * Special enqueue methods - all call enqueueCommand
	 */
	
	/**
	 * Enqueues a command that will cease execution if it is run
	 */
	private void enqueueTerminate()
	{
		enqueueCommand(new byte[]{-1}, DebuggerState.DONE, false);
	}

	/**
	 * Adds a breakpoint command to the command queue
	 * @param bp The Breakpoint object representing the breakpoint to add
	 */
	private void enqueueBreakpoint(Breakpoint bp)
	{
		byte[] breakPointCommand;
		if (bp.getFilePath() == null || bp.getFilePath().equals(""))
		{
			breakPointCommand = ("b "+bp.getLineNum()+"\n").getBytes();
		}
		else
		{
			breakPointCommand = ("b "+bp.getFilePath()+':'+bp.getLineNum()+"\n").getBytes();
		}
		enqueueCommand(breakPointCommand, DebuggerState.BREAKPOINTSET, false);
	}
	
	/**
	 * Removes a breakpoint command to the command queue
	 * Fails if the breakpoint does not contain both a filepath and line number
	 * @param bp The Breakpoint object representing the breakpoint to remove
	 */
	private void enqueueBreakpointRemove(Breakpoint bp)
	{
		byte[] breakPointCommand;
		if (bp.getFilePath() == null || bp.getFilePath().equals(""))
		{
			return;
		}
		else
		{
			breakPointCommand = ("cl "+bp.getFilePath()+':'+bp.getLineNum()+"\n").getBytes();
		}
		enqueueCommand(breakPointCommand, DebuggerState.RUNNING, false);
	}
	
	/**
	 * Adds an enqueue command to the command queue
	 */
	private void enqueueContinue()
	{
		enqueueCommand("c\n".getBytes(), DebuggerState.RUNNING, false);
	}
	
	/**
	 * Adds a step over command to the command queue
	 */
	private void enqueueStepOver()
	{
		enqueueCommand("n\n".getBytes(), DebuggerState.RUNNING, false);
	}
	
	/**
	 * Adds a step over command to the command queue
	 */
	private void enqueueStepIn()
	{
		enqueueCommand("s\n".getBytes(), DebuggerState.RUNNING, false);
	}
	
	/**
	 * Adds a command to import the OutputExpander module required for variabkes to the command queue
	 */
	private void enqueueModuleSetup()
	{
		enqueueCommand("import imp\n".getBytes(), DebuggerState.HIDDENRUNNING, false);
		enqueueCommand(("__SavuOutputExpander__ = imp.load_source('LocalVariables', '"+System.getProperty("user.dir")+File.separator+"lib"+File.separator+"LocalVariables.py')\n").getBytes(), DebuggerState.HIDDENRUNNING, false);
	}
	
	/**
	 * Adds a stack trace command to the command queue, waits for it to be executed, and returns the results
	 * @return
	 */
	private String enqueueStackTrace()
	{
		DebuggerCommand toWait = enqueueCommand("w\n".getBytes(), DebuggerState.AWAITINGRESPONSE, true);
		toWait.notificationLock.lock();
		toWait.responseComplete.awaitUninterruptibly();
		toWait.notificationLock.unlock();
		return toWait.response;
	}
	
	/**
	 * Adds a variable request command to the command queue, waits for it to be executed, and returns the results
	 * @return
	 */
	private String enqueueLocalVariableRequest()
	{
		//We need to make sure that __SavuOutputExpander__ is defined - if we ever change scope, it needs to be redefined, so we'll constantly redefine it
		enqueueModuleSetup();
		DebuggerCommand toWait = enqueueCommand("print __SavuOutputExpander__.parseLocalVariables(locals(), globals())\n".getBytes(), DebuggerState.AWAITINGRESPONSE, true);
		toWait.notificationLock.lock();
		toWait.responseComplete.awaitUninterruptibly();
		toWait.notificationLock.unlock();
		return toWait.response;
	}
	
	private String enqueueGlobalVariableRequest()
	{
		//We need to make sure that __SavuOutputExpander__ is defined - if we ever change scope, it needs to be redefined, so we'll constantly redefine it
		enqueueModuleSetup();
		DebuggerCommand toWait = enqueueCommand("print __SavuOutputExpander__.parseLocalVariables(__SavuOutputExpander__.subtractDictionaries(globals(), locals()), globals())\n".getBytes(), DebuggerState.AWAITINGRESPONSE, true);
		toWait.notificationLock.lock();
		toWait.responseComplete.awaitUninterruptibly();
		toWait.notificationLock.unlock();

		return toWait.response;
	}
	
	/*
	 * Public accessor methods
	 */
	
	/**
	 * Checks whether the debugger is paused (at a breakpoint) waiting for user input
	 * @return true if the debugger is paused, otherwise false
	 */
	public boolean isReady()
	{
		synchronized(state)
		{
			return state == DebuggerState.READY;
		}
	}
	
	/**
	 * Checks whether or not the program has finished
	 * @return true if the program has not finished, false if it has
	 */
	public boolean isRunning()
	{
		synchronized(state)
		{
			return state != DebuggerState.DONE;
		}
	}
		
	/**
	 * Returns a variable representing the current environment.
	 * Ignored functions, modules, classes (not objects)
	 * This method should not be called from this thread, which also means it should not be called directly from the update method on an observer of this class.
	 * This method will block until this thread does work - if it is called from this thread, deadlock will result.
	 * If it needs to be called from an update method, create a new thread to do the work.
	 * @return Variable[] with two elements - both are dictionaries of names and Variables and contains entries for items in the current python environment. The first is the locals, the second the globals.
	 */
	public Variable[] getVariables()
	{
		boolean stateCondition;
		synchronized(state)
		{
			stateCondition = (state == DebuggerState.READY);
		}
		if (stateCondition)
		{
			//This will have quotes around it - we strip those off
			String localVariables =  enqueueLocalVariableRequest().trim();
			String globalVariables = enqueueGlobalVariableRequest().trim();
			Variable[] toReturn = new Variable[2];
			toReturn[0] = VariableParser.parse(localVariables);
			toReturn[1] = VariableParser.parse(globalVariables);
			return toReturn;
		}
		return null;
	}
	
	/**
	 * Get the current stack trace. No parsing is done of the pdb output.
	 * This method should not be called from this thread, which also means it should not be called directly from the update method on an observer of this class.
	 * This method will block until this thread does work - if it is called from this thread, deadlock will result.
	 * If it needs to be called from an update method, create a new thread to do the work.
	 * @return A string containing the stack trace output by pdb
	 */
	public String getStackTrace()
	{
		boolean stateCondition;
		synchronized(state)
		{
			stateCondition = state == DebuggerState.READY;
		}
		if (stateCondition)
		{
			return enqueueStackTrace();
		}
		return null;
	}
	
	/**
	 * Gets the current position of the debugger in the python program. Is not guaranteed to be accurate if the debugger is not in the READY state.
	 * @return A CodePosition object representing the current position in the code.
	 */
	public CodePosition getCurrentCodePosition()
	{
		codePositionLock.lock();
		try
		{
			return curCodePosition;
		}
		finally
		{
			codePositionLock.unlock();
		}
	}
	
	public String getState() {
		switch(state) {
		case READY:
			return "Ready";
		case START:
			return "Start";
		case RUNNING:
			return "Running";
		case BREAKPOINTSET:
			return "Breakpoint set";
		case DONE:
			return "Done";
		case AWAITINGRESPONSE:
			return "Awaiting response";
		default:
			return "Could not get state.";
		}
	}
	
	/**
	 * Sets a breakpoint at the given location.
	 * Will never be called since bp's are passed in on start. Leaving this here to test enqueue breakpoint though
	 * @param bp The location to insert the breakpoint
	 */
	public void setBreakpoint(Breakpoint bp)
	{
		synchronized(state)
		{
			enqueueBreakpoint(bp);
		}
	}
	
	/**
	 * Removes a breakpoint at the given location. Requires that the breakpoint object contains both a line number and filepath
	 * @param bp The location to remove the breakpoint
	 */
	public void clearBreakpoint(Breakpoint bp)
	{
		synchronized(state)
		{
			enqueueBreakpointRemove(bp);
		}
	}
	
	/**
	 * Steps in if the debugger is ready for user input. Will fail silently if the debugger state is not ready.
	 */
	public void stepIn()
	{
		synchronized(state)
		{
			if (state == DebuggerState.READY)
			{
				enqueueStepIn();
			}
		}
	}
	
	/**
	 * Steps over if the debugger is ready for user input. Will fail silently if the debugger state is not ready.
	 */
	public void stepOver()
	{
		synchronized(state)
		{
			if (state == DebuggerState.READY)
			{
				enqueueStepOver();
			}
		}
	}
	
	/**
	 * Continues the debugger is ready for user input. Will fail silently if the debugger state is not ready.
	 */
	public void continueRun()
	{
		synchronized(state)
		{
			if (state == DebuggerState.READY)
			{
				enqueueContinue();
			}
		}
	}
	
	/**
	 * Stops the pdb process, and therefore the running program.
	 */
	public void stop()
	{
		p.destroy();
		synchronized(state)
		{
			state = DebuggerState.DONE;
		}
		setChanged();
		notifyObservers();
		enqueueTerminate();
	}
	
	//Other public methods
	
	/**
	 * Attempts to write to the process' input. If the process is paused this operation will fail.
	 * @return true if the write succeeded, false otherwise
	 */
	public boolean writeInput(String input)
	{
		synchronized(state)
		{
			if (state == DebuggerState.RUNNING)
			{
				try
				{
					debugInput.write(input.getBytes());
					debugInput.flush();
					//If this ended the line we need to reset linecouldbepdb to true, because it's now a new line
					if (input.contains("\n"))
					{
						synchronized(lineCouldBePDB)
						{
							lineCouldBePDB = true;
						}
					}
					return true;
				}
				catch (IOException e)
				{
					return false;
				}
			}
			return false;
		}
	}
	
	/**
	 * 
	 * @return The stderr stream for the underlying python process. This is not inspected by the debugger.
	 */
	public InputStream getErrorStream()
	{
		return errorStream;
	}
	
	//Test code
	public static void main(String[] args)
	{
		List<Breakpoint> bps = new ArrayList<Breakpoint>();
		bps.add(new Breakpoint(null, null, null)); //Test code broken after changes to Breakpoint to make it more secure
		PythonDebugger test = new PythonDebugger(System.out, "/Users/PyDE/pyDE/PythonExecuter/src/testFileCaller.py", bps, "");
		test.run();
	}
}
