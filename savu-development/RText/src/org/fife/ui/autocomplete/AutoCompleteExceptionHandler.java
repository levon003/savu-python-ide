package org.fife.ui.autocomplete;

import java.util.*;
import java.lang.*;

import org.fife.rtext.Savu;
public class AutoCompleteExceptionHandler implements Thread.UncaughtExceptionHandler {
	public AutoCompleteExceptionHandler() {
		super();	
	}
	

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		System.out.println("The autocomplete thread crashed with the following exception, restarting...");
		e.printStackTrace();
		AutoCompleteRunner failedThread = (AutoCompleteRunner)t;
		AutoCompleteRunner newRunner = new AutoCompleteRunner(failedThread.getOwner(), failedThread.getDelay());
		newRunner.setUncaughtExceptionHandler(this);
		newRunner.start();
	}
}
