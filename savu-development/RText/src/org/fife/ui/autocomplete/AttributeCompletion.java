package org.fife.ui.autocomplete;

/**
 * A straightforward {@link Completion} implementation.  This implementation
 * can be used if you have a relatively short number of static completions
 * with no (or short) summaries.<p>
 *
 * This implementation uses the replacement text as the input text.  It also
 * includes a "short description" field, which (if non-<code>null</code>), is
 * used in the completion choices list.   
 *
 * @author PyDE
 * @version 1.0
 */
public class AttributeCompletion extends BasicCompletion{
	String clazz;
	
	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 * @param clazz The Python class that the attribute belongs to.
	 */
	public AttributeCompletion(CompletionProvider provider, String replacementText, String clazz){
		this(provider, replacementText, null, clazz);
	}
	
	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 * @param clazz The Python class that the attribute belongs to.
	 */
	public AttributeCompletion(CompletionProvider provider, String replacementText, String shortDesc, String clazz) {
		this(provider, replacementText, shortDesc, null, clazz);
	}


	/**
	 * Constructor.
	 *
	 * @param provider The parent completion provider.
	 * @param replacementText The text to replace.
	 * @param shortDesc A short description of the completion.  This will be
	 *        displayed in the completion list.  This may be <code>null</code>.
	 * @param summary The summary of this completion.  This should be HTML.
	 *        This may be <code>null</code>
	 * @param clazz The Python class that the attribute belongs to.
	 */
	public AttributeCompletion(CompletionProvider provider, String replacementText, String shortDesc, String summary, String clazz) {
		super(provider, replacementText, shortDesc, summary);
		this.clazz = clazz;
	}
	/**
	 * Basic getter for the attribute's associated class
	 * 
	 * @return The class the Attribute is associated with
	 */
	public String getClazz(){
		return this.clazz;
	}
	
	@Override
	public String toString(){
		//return "("+this.getReplacementText()+", "+clazz+")";
		return this.getReplacementText();
	}

}
