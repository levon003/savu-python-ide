package org.fife.rtext.plugins.run;

import java.io.*;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

public class DoubleOutputStreamSiphon extends Thread
	{
		private RunTextArea outArea;
		private InputStream primaryStream;
		private InputStream secondaryStream;
		private String primaryStyle;
		private String secondaryStyle;
		private PythonProcess waitFor;
		private StringBuilder primaryBuffer;
		private StringBuilder secondaryBuffer;
		long lastime;
		
		/**
		 * Class takes inputs from two streams and appends it to a RunTextArea. 
		 * Attempts to ensure that input from the streams is not interleaved character by character.
		 * Runs as a thread, and automatically terminates when the process whose input is is handling closes 
		 * @param is The stdout stream of the process
		 * @param secondaryStream The stderr stream of the process
		 * @param output The RunTextArea to append the text to
		 * @param style The style to print the primary stream's input to the RunTextArea
		 * @param secondaryStyle The style to print the secondary stream's input to the RunTextArea
		 * @param waitFor The PythonProcess producing the output. When it is finished, the thread will halt itself
		 */
		public DoubleOutputStreamSiphon (InputStream is, InputStream secondaryStream, RunTextArea output, String style, String secondaryStyle, PythonProcess waitFor) {
			//Output to the process' output stream
			outArea = output;
			//inStream is going to be the system.in 
			primaryStream = is;
			this.secondaryStream = secondaryStream;
			//Apparently we need this?
			this.primaryStyle = style; 
			this.secondaryStyle = secondaryStyle;
			//This will show when we're done
			this.waitFor = waitFor;
			
			primaryBuffer = new StringBuilder();
			secondaryBuffer = new StringBuilder();
			lastime = System.currentTimeMillis();
			
		}
		
		public void run() {
			int c;
			boolean primaryStreamNotClosed = true;
			boolean secondaryStreamNotClosed = true;
  			while (waitFor.isRunning()) {
  				//Get input from the streams, if there is any, and add it to the appropriate buffer
  				try
  				{
	  				if (primaryStream.available() > 0)
	  				{
						c = primaryStream.read();
						if (c == -1)
						{
							primaryStreamNotClosed = false;
	  						System.out.println("Found end of stream");
						}
						else
						{
							primaryBuffer.append((char)c);
						}
	  				}
	  				if (secondaryStream.available() > 0)
	  				{
	  					c = secondaryStream.read();
						if (c != -1)
						{
							secondaryBuffer.append((char)c);
						}
						else
						{
							secondaryStreamNotClosed = false;
						}
	  				}
  				}
  				catch (IOException e)
  				{
  					//Doesn't matter. We continue running until the process we're watching stops, even if we start getting exceptions
  				}
  				
  				//If enough time has passed, we append the output from the streams to the text areas.
  				//We only append every 200 milliseconds, both to avoid excessive interleaving, and to avoid overloading the EDT
  				//Secondary input is always put after primary input in the RunTextArea
  				if (System.currentTimeMillis() - lastime > 200)
  				{
  					if (primaryBuffer.length() > 0)
  					{
  						outArea.append(primaryBuffer.toString() , primaryStyle);
  						primaryBuffer.delete(0, primaryBuffer.length());
  					}
  					if (secondaryBuffer.length() > 0)
  					{
  						outArea.append(secondaryBuffer.toString() , secondaryStyle);
  						secondaryBuffer.delete(0, secondaryBuffer.length());
  					}
  					lastime = System.currentTimeMillis();
  				}
  			}
  			//Once the process has ended, its output streams should have closed so we can read from them until the end
  			if (primaryStreamNotClosed)
  			{
  				try {
					while ((c = primaryStream.read()) != -1)
					{
						primaryBuffer.append((char)c);
					}
				} catch (IOException e) {
					//At this point the stream isn't being modified, so if we get an IOException we simply print what we have and end
				}
  				outArea.append(primaryBuffer.toString(), primaryStyle);
  			}
  			if (secondaryStreamNotClosed)
  			{
  				try {
					while ((c = secondaryStream.read()) != -1)
					{
						secondaryBuffer.append((char)c);
					}
				} catch (IOException e) {
					//At this point the stream isn't being modified, so if we get an IOException we simply print what we have and end
				}
  				outArea.append(secondaryBuffer.toString(), secondaryStyle);
  			}
	  			
		}

		public byte[] getBytes(char c) throws UnsupportedEncodingException
		{
			char[] ca = new char[1];
			ca[0] = c;
			return new String(ca).getBytes("UTF-8");

		}
	}