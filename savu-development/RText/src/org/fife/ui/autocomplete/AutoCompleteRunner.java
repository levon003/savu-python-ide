package org.fife.ui.autocomplete;

import java.util.Timer;

import javax.swing.text.Caret;

import org.fife.rtext.AbstractMainView;
import org.fife.rtext.Savu;
import org.fife.rtext.RTextEditorPane;

//FIXME Why was this being imported? it was breaking our build >:(
//import sun.text.normalizer.Replaceable;


/**
 * Class that will be scanning the current document in the background, making changes
 * to the autocomplete possibilities every <code>delay</code> milliseconds based on where the cursor is. 
 * 
 * @author pyDE
 * @version 0.00001
 */
public class AutoCompleteRunner extends Thread{
	Savu owner;
	long delay;
	
	public AutoCompleteRunner(Savu owner, long delay){
		this.delay = delay;
		this.owner = owner;
	}
	
	
	/**
	 * Makes changes to the autocomplete possibilities every <code>delay</code> 
	 * milliseconds based on where the cursor is. 
	 */
	public void run(){
		while(true){
			AbstractMainView view;
			RTextEditorPane pane;
			// So long as the view and area are initialized (after loading screens, not during dialogs, etc)
			// Populate AutoComplete
			if ((view=owner.getMainView()) != null && (pane = view.getCurrentTextArea()) != null)
			{
				pane = view.getCurrentTextArea();
				pane.populateAutoComplete();
			}
			// Don't need to do it repeatedly, can take a rest and grab some food
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Getter for the parent Savu object
	 * 
	 * @return The <code>Savu</code> object that created this thread
	 */
	public Savu getOwner(){
		return owner;
	}
	
	/**
	 * Getter for the time delay
	 * 
	 * @return Gets the millisecond time delay of the thread
	 */
	public long getDelay(){
		return delay;
	}
	
}
