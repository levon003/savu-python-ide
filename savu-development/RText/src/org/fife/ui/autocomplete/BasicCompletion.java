/*
 * 01/03/2009
 *
 * BasicCompletion.java - A straightforward Completion implementation.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.util.Arrays;


/**
 * A straightforward {@link Completion} implementation.  This implementation
 * can be used if you have a relatively short number of static completions
 * with no (or short) summaries.<p>
 *
 * This implementation uses the replacement text as the input text.  It also
 * includes a "short description" field, which (if non-<code>null</code>), is
 * used in the completion choices list. 
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class BasicCompletion extends AbstractCompletion {

	private String replacementText;
	private String shortDesc;
	private String summary;


	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 */
	public BasicCompletion(CompletionProvider provider, String replacementText){
		this(provider, replacementText, null);
	}


	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 */
	public BasicCompletion(CompletionProvider provider, String replacementText,
							String shortDesc) {
		this(provider, replacementText, shortDesc, null);
	}


	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 * @param summary The summary of this completion.  This should be HTML.
	 *        This may be <code>null</code>.
	 */
	public BasicCompletion(CompletionProvider provider, String replacementText,
							String shortDesc, String summary) {
		super(provider);
		this.replacementText = replacementText;
		this.shortDesc = shortDesc;
		this.summary = summary;
	}


	
	/**
	 * Checks if one Basic Completion is equal to another
	 * Needs to be overriden so "add" and "addAll" don't add duplicates
	 * @param comp
	 * @return whether or not the objects are equal
	 * @author pyDE
	 */
	@Override
	public boolean equals(Object obj){
		//They must be the same class (AttributeCompletion, Basic, etc..)
		if (this.getClass().equals(obj.getClass())){
			//Have the same replacement text
			if (this.getReplacementText().equals(((BasicCompletion)obj).getReplacementText())){
				//Have the same provider
				if (this.getProvider().equals(((BasicCompletion)obj).getProvider())){
					//If they're both Attributes, have the same class. Otherwise they're equal
					if (obj instanceof AttributeCompletion && this instanceof AttributeCompletion){
						if (((AttributeCompletion)this).getClazz().equals(((AttributeCompletion)obj).getClazz())){
							return true;
						}
					}
					else{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Gets the Hashcode for a completion object
	 * Needs to be overriden so "add" and "addAll" don't add duplicates
	 * 
	 * @return The hashcode for this object
	 * @author PyDe
	 */
	@Override
	public int hashCode(){
		//If it's an AttributeCompletion, need to take it's clazz into account otherwise we don't.
		if (this instanceof AttributeCompletion){
			return Arrays.hashCode( new Object[] { this.getClass(), this.getReplacementText(), ((AttributeCompletion)this).getClazz() } );
		}
		return Arrays.hashCode( new Object[] { this.getClass(), this.getReplacementText() } );
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getReplacementText() {
		return replacementText;
	}


	/**
	 * Returns the short description of this completion, usually used in
	 * the completion choices list.
	 *
	 * @return The short description, or <code>null</code> if there is none.
	 * @see #setShortDescription(String)
	 */
	public String getShortDescription() {
		return shortDesc;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getSummary() {
		return summary;
	}


	/**
	 * Sets the short description of this completion.
	 *
	 * @param shortDesc The short description of this completion.
	 * @see #getShortDescription()
	 */
	public void setShortDescription(String shortDesc) {
		this.shortDesc = shortDesc;
	}


	/**
	 * Sets the summary for this completion.
	 *
	 * @param summary The summary for this completion.
	 * @see #getSummary()
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * Sets the replacement text for this completion.
	 *
	 * @param text The replacement text for this completion.
	 * @see #getReplacementText()
	 */
	public void setReplacementText(String text) {
		this.replacementText = text;
	}
	

	/**
	 * Returns a string representation of this completion.  If the short
	 * description is not <code>null</code>, this method will return:
	 * 
	 * <code>getInputText() + " - " + shortDesc</code>
	 * 
	 * otherwise, it will return <tt>getInputText()</tt>.
	 *
	 * @return A string representation of this completion.
	 */
	@Override
	public String toString() {
		if (shortDesc==null) {
			return getInputText();
		}
		return getInputText() + " - " + shortDesc;
	}


}