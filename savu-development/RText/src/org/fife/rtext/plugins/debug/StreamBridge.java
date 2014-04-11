package org.fife.rtext.plugins.debug;

import java.io.*;

public class StreamBridge extends Thread
	{
		private OutputStream outStream;
		private InputStream inStream;
		private BufferedReader input;

		public StreamBridge (InputStream is, OutputStream os)
		{
			//Output to the process' output stream
			outStream = os;
			//inStream is going to be the system.in 
			inStream = is;

		}
		public void run()
		{
			input = new BufferedReader(new InputStreamReader(inStream));
			char c;
			try
			{
	  			while ((c = (char)input.read()) != 0xffff) {
	  				byte[] ca = getBytes(c);
	  				for (int i = 0; i< ca.length; ++i)
	  				{
	    				outStream.write(ca[i]);
	    				outStream.flush();
	    			}
	  			}
	  		}
	  		catch (IOException e)
	  		{
	  		}
		}

		public void terminate()
		{
			try {
				inStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}

		public byte[] getBytes(char c) throws UnsupportedEncodingException
		{
			char[] ca = new char[1];
			ca[0] = c;
			return new String(ca).getBytes("UTF-8");

		}
	}