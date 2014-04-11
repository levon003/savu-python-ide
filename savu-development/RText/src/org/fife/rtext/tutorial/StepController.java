package org.fife.rtext.tutorial;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Controls stepping through the tutorial to ensure thread safety
 * 
 * @author nathanroberts
 *
 */
public class StepController {

	protected Lock stepLock = new ReentrantLock();
	final protected Condition finishedStepCondition = stepLock.newCondition();
	protected boolean finishedStep = false;
	protected boolean tutorialExited = false;
	
	
	/**
	 * 
	 * DO NOT CALL THIS UNLESS YOU HAVE ALREADY ACQUIRED stepLock. Thanks.
	 */
	public void waitForNextStep() {
		try {
			while (!finishedStep) {
				finishedStepCondition.await();
				if (tutorialExited)
					break;
			}
		} catch (InterruptedException ex) {
			return;
		} finally {
			finishedStep = false; //Reset the variable
			stepLock.unlock();
		}
	}
	
	/**
	 * Called when the correct component is clicked and the tutorial can progress to the next step
	 */
	public void alertStepFinished() {
		stepLock.lock();
		finishedStep = true;
		finishedStepCondition.signal();
		stepLock.unlock();
	}
	
	/**
	 * Called when the user clicks the exit tutorial button and signals that the tutorial should close
	 */
	public void alertTutorialExited() {
		stepLock.lock();
		tutorialExited = true;
		finishedStepCondition.signal();
		stepLock.unlock();
	}

}
