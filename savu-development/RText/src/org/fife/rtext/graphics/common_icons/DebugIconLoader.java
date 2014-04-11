package org.fife.rtext.graphics.common_icons;

import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class DebugIconLoader {
	private Icon complexIcon;
	private Icon primitiveIcon;
	private Icon stepOverIcon;
	private Icon stepIntoIcon;
	private Icon continueRunningIcon;
	private Icon stopIcon; 
	private Icon debugIcon; 
	public DebugIconLoader() {
		// TODO Auto-generated constructor stub
		
		URL url1 = getClass().getResource("primitiveIcon.png");
		URL url2 = getClass().getResource("complexIcon.png");

		URL url3 = getClass().getResource("StepOverIcon.png");

		URL url4 = getClass().getResource("StepIntoIcon.png");
		URL url5 = getClass().getResource("ContinueRunningIcon.png");
		URL url6 = getClass().getResource("stop.png"); 
		URL url7 = getClass().getResource("debug.png");
		//if (url1 != null && url2 != null && url3 != null && url4 != null && url5 != null) { // Should always be true
			try {
				primitiveIcon = new ImageIcon(ImageIO.read(url1));
				complexIcon = new ImageIcon(ImageIO.read(url2));
				stepOverIcon = new ImageIcon(ImageIO.read(url3));
				stepIntoIcon = new ImageIcon(ImageIO.read(url4));
				continueRunningIcon = new ImageIcon(ImageIO.read(url5));
				stopIcon = new ImageIcon(ImageIO.read(url6)); 
				debugIcon = new ImageIcon(ImageIO.read(url7)); 
			} catch (IOException ioe) {
				//app.displayException(ioe);
			}
	}
	
	public Icon getCI() {
		return complexIcon;
	}
	
	public Icon getPI() {
		return primitiveIcon;
	}
	public Icon getSOI() {
		return stepOverIcon;
		
	}
	public Icon getSII() {
		return stepIntoIcon;
	}
	public Icon getCR() {
		return continueRunningIcon;
	}
	public Icon getSI() {
		return stopIcon;
	}

	public Icon getDI() {
		return debugIcon;
	}



}
