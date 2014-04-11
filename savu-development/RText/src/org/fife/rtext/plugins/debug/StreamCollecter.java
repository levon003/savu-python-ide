package org.fife.rtext.plugins.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class StreamCollecter extends Thread{

	private InputStream inStream;
	private Queue<Character> buffer;
	
	private ReentrantLock bufferLock;
	private Condition notEmpty;
	
	public StreamCollecter(InputStream input)
	{
		this.inStream = input;
		this.buffer = new LinkedList<Character>();
		bufferLock = new ReentrantLock();
		notEmpty = bufferLock.newCondition();
	}
	
	public void run()
	{
		
		BufferedReader input = new BufferedReader(new InputStreamReader(inStream));
		char c;
		try
		{
  			while ((c = (char)input.read()) != 0xffff) {
  				bufferLock.lock();
  				try
  				{
  					buffer.add(c);
  					notEmpty.signal();
  				}
  				finally
  				{
  					bufferLock.unlock();
  				}
  				
  			}
  		}
  		catch (IOException e)
  		{

  		}
	}
	
	public boolean hasOutput()
	{
		bufferLock.lock();
		try
		{
			return buffer.size() != 0;
		}
		finally
		{
			bufferLock.unlock();
		}
	}
	
	public int outputLength()
	{
		bufferLock.lock();
		try
		{
			return buffer.size();
		}
		finally
		{
			bufferLock.unlock();
		}
	}
	
	public char nextChar()
	{
		bufferLock.lock();
		try
		{
			return buffer.remove();
		}
		finally
		{
			bufferLock.unlock();
		}
	}
	
	public void clearOutput()
	{
		bufferLock.lock();
		try
		{
			buffer.clear();
		}
		finally
		{
			bufferLock.unlock();
		}
	}
	
	public String buildString()
	{
		if(!this.hasOutput()) { return ""; }
		
		this.awaitOutput();
		
		StringBuilder builder = new StringBuilder();
		while (this.hasOutput())
		{
			builder.append(this.nextChar());
		}
		return builder.toString();
		
	}
	
	public void awaitOutput()
	{
		bufferLock.lock();
		try
		{
			if (!buffer.isEmpty())
			{
				return;
			}
			notEmpty.awaitUninterruptibly();
		}
		finally
		{
			bufferLock.unlock();
		}
	}

}
