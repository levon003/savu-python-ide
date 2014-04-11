/*
 * 12/21/2008
 *
 * AbstractCompletionProvider.java - Base class for completion providers.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.autocomplete;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;


/**
 * A base class for completion providers.  {@link Completion}s are kept in
 * a sorted list.  To get the list of completions that match a given input,
 * a binary search is done to find the first matching completion, then all
 * succeeding completions that also match are also returned.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class AbstractCompletionProvider
								extends CompletionProviderBase {

	/**
	 * The completions this provider is aware of.  Subclasses should ensure
	 * that this list is sorted alphabetically (case-insensitively).
	 */
	protected List<Completion> completions;

	/**
	 * Compares a {@link Completion} against a String.
	 */
	protected CaseInsensitiveComparator comparator;


	/**
	 * Constructor.
	 */
	public AbstractCompletionProvider() {
		comparator = new CaseInsensitiveComparator();
		clearParameterizedCompletionParams();
		completions = new ArrayList<Completion>();
	}


	/**
	 * Adds a single completion to this provider.  If you are adding multiple
	 * completions to this provider, for efficiency reasons please consider
	 * using {@link #addCompletions(List)} instead.
	 *
	 * @param c The completion to add.
	 * @throws IllegalArgumentException If the completion's provider isn't
	 *         this <tt>CompletionProvider</tt>.
	 * @see #addCompletions(List)
	 * @see #removeCompletion(Completion)
	 * @see #clear()
	 */
	public void addCompletion(Completion c) {
		checkProviderAndAdd(c);
		Collections.sort(completions);
	}


	/**
	 * Adds {@link Completion}s to this provider.
	 *
	 * @param completions The completions to add.  This cannot be
	 *        <code>null</code>.
	 * @throws IllegalArgumentException If a completion's provider isn't
	 *         this <tt>CompletionProvider</tt>.
	 * @see #addCompletion(Completion)
	 * @see #removeCompletion(Completion)
	 * @see #clear()
	 */
	public void addCompletions(List<Completion> completions) {
		//this.completions.addAll(completions);
		for (Completion c : completions) {
			checkProviderAndAdd(c);
		}
		Collections.sort(this.completions);
	}


	/**
	 * Adds simple completions for a list of words.
	 *
	 * @param words The words.
	 * @see BasicCompletion
	 */
	protected void addWordCompletions(String[] words) {
		int count = words==null ? 0 : words.length;
		for (int i=0; i<count; i++) {
			completions.add(new BasicCompletion(this, words[i]));
		}
		Collections.sort(completions);
	}

	/**
	 * Checks if a given line is blank 
	 * (contains only whitespace/newline)
	 * @param line Line to check if blank
	 */
	protected boolean blankLine(String line){
		Matcher m = Pattern.compile("\\S+").matcher(line);
		return !m.find();
	} 
	
	
	protected void checkProviderAndAdd(Completion c) {
		if (c.getProvider()!=this) {
			throw new IllegalArgumentException("Invalid CompletionProvider");
		}
		completions.add(c);
	}


	/**
	 * Removes all completions from this provider.  This does not affect
	 * the parent <tt>CompletionProvider</tt>, if there is one.
	 *
	 * @see #addCompletion(Completion)
	 * @see #addCompletions(List)
	 * @see #removeCompletion(Completion)
	 */
	public void clear() {
		completions.clear();
	}


	/**
	 * Returns a list of <tt>Completion</tt>s in this provider with the
	 * specified input text.
	 *
	 * @param inputText The input text to search for.
	 * @return A list of {@link Completion}s, or <code>null</code> if there
	 *         are no matching <tt>Completion</tt>s.
	 */
	@SuppressWarnings("unchecked")
	public List<Completion> getCompletionByInputText(String inputText) {

		// Find any entry that matches this input text (there may be > 1).
		int end = Collections.binarySearch(completions, inputText, comparator);
		if (end<0) {
			return null;
		}

		// There might be multiple entries with the same input text.
		int start = end;
		while (start>0 &&
				comparator.compare(completions.get(start-1), inputText)==0) {
			start--;
		}
		int count = completions.size();
		while (++end<count &&
				comparator.compare(completions.get(end), inputText)==0);

		return completions.subList(start, end); // (inclusive, exclusive)

	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected List<Completion> getCompletionsImpl(JTextComponent comp) {
		boolean classCompletion = false;
		List<Completion> retVal = new ArrayList<Completion>();
		String text = getAlreadyEnteredText(comp);
		String fullLine = getAlreadyEnteredText(comp,true);
		String object="";
		String clazz = "";
		Matcher m = Pattern.compile("(\\w+)[.]").matcher(fullLine);
		while (m.find()){
			object = m.group(1);
		}
		if (object!=""){
			classCompletion = true;
			clazz = getClassOfObject(comp, object,comp.getCaret().getDot());
		}
		if (fullLine!=null) {

			int index = Collections.binarySearch(completions, text, comparator);
			if (index<0) { // No exact match
				index = -index - 1;
			}
			else {
				// If there are several overloads for the function being
				// completed, Collections.binarySearch() will return the index
				// of one of those overloads, but we must return all of them,
				// so search backward until we find the first one.
				int pos = index - 1;
				while (pos>0 &&
						comparator.compare(completions.get(pos), text)==0) {
					retVal.add(completions.get(pos));
					pos--;
				}
			}
			if (!classCompletion){
				while (index<completions.size()) {
					Completion c = completions.get(index);
					if (Util.startsWithIgnoreCase(c.getInputText(), text) && !(c instanceof AttributeCompletion)) {
						retVal.add(c);
						index++;
					}
					else {
						//break;
						index++;
					}
				}
			}
			else{
				while (index<completions.size()) {
					Completion c = completions.get(index);
					AttributeCompletion attr;
					if (Util.startsWithIgnoreCase(c.getInputText(), text) && (c instanceof AttributeCompletion) && ((AttributeCompletion)c).getClazz().equals(clazz)) {
						attr = (AttributeCompletion)c;
						retVal.add(attr);
						index++;
					}
					else {
						//break;
						index++;
					}
				}
			}

		}
		return retVal;

	}
	
	
	/**
	 * Method to determine the class of a variable!
	 * 
	 * @param comp The text component
	 * @param object The variable we need to determine the class of
	 * @return The class of the variable
	 */
	@SuppressWarnings("unchecked")
	protected String getClassOfObject(JTextComponent comp, String object, int endIndex) {
		String clazz="";
		String definitionChars = "[\\w .,\\[\\]\\{\\}\"()]";
		String text = "";
		try {
			text = comp.getText(0, endIndex);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		Matcher importRenameMatcher = Pattern.compile("import +(\\w+) +as +"+object).matcher(text);
		Matcher importMatcher = Pattern.compile("import +"+object).matcher(text);
		if (importRenameMatcher.find()){
			System.out.println(importRenameMatcher.group(1));
			return importRenameMatcher.group(1);
		}
		else if(importMatcher.find()){
			return object;
		}
		Matcher m = Pattern.compile(object+" *= *("+definitionChars+"+)").matcher(text);
		while(m.find()){
			clazz = m.group(1);
		}
		if ((m = Pattern.compile("[.](\\w+)[(][()\\w .,\"']*[)]$").matcher(clazz)).find()){
			return m.group(1);
		}
		if ((m = Pattern.compile("^\"\\w*\"$").matcher(clazz)).find()){
			return "String";
		}
		else if ((m = Pattern.compile("^\\["+definitionChars+"*\\]$").matcher(clazz)).find()){
			return "list";
		}
		else if ((m = Pattern.compile("^\\{"+definitionChars+"*\\}$").matcher(clazz)).find()){
			return "dict";
		}
		else if ((m = Pattern.compile("^ *(\\w+)[(]"+definitionChars+"*[)] *$").matcher(clazz)).find()){
			if (isFunction(m.group(1),text)!=-1){
				return getReturnTypeOfFunction(comp, text.substring(m.start()));
			}
			return m.group(1);
		}
		else{
			return "";
		}
	}

	/**
	 * Checks to see if the given string is a function
	 * 
	 * @return True if a function, false otherwise
	 */
	protected int isFunction(String object, String text){
		Matcher m = Pattern.compile("def *"+object+"[(][\\w, ]*[)]").matcher(text);
		if (m.find()){
			return m.start();
		}
		return -1;
	}
	
	/**
	 * Method to get the return type of a function if given the text of that function
	 * 
	 * @param comp The active component window
	 * @param text The text of the function we're trying to get the return type of
	 * @return A string representation of the return type of the function
	 */
	protected String getReturnTypeOfFunction(JTextComponent comp, String text){
		String definitionChars = "[\\w .,\\[\\]\\{\\}\"()]";
		Matcher m = Pattern.compile("return +("+definitionChars+"*)").matcher(text);
		if (m.find()){
			String var = m.group(1);
			Matcher typeMatcher;
			if ((typeMatcher = Pattern.compile("^\"\\w*\"$").matcher(var)).find()){
				return "String";
			}
			else if ((typeMatcher = Pattern.compile("^\\["+definitionChars+"*\\]$").matcher(var)).find()){
				return "list";
			}
			else if ((typeMatcher = Pattern.compile("^\\{"+definitionChars+"*\\}$").matcher(var)).find()){
				return "dict";
			}
			return getClassOfObject(comp, var, m.end(1));
		}
		return "";
	}
	
	/**
	 * Removes the specified completion from this provider.  This method
	 * will not remove completions from the parent provider, if there is one.
	 *
	 * @param c The completion to remove.
	 * @return <code>true</code> if this provider contained the specified
	 *         completion.
	 * @see #clear()
	 * @see #addCompletion(Completion)
	 * @see #addCompletions(List)
	 */
	public boolean removeCompletion(Completion c) {
		// Don't just call completions.remove(c) as it'll be a linear search.
		int index = Collections.binarySearch(completions, c);
		if (index<0) {
			return false;
		}
		completions.remove(index);
		return true;
	}
	
	/**
	 * Utility method to reverse given text line by line, or flip it upside down
	 * Useful for finding things in scope
	 * 
	 * @param text
	 * @return The text flipped upside down
	 */
	protected String flipTextLines(String text){
		BufferedReader bufReader = new BufferedReader(new StringReader(text));
		String line=null;
		
		// Reverses the text line by line, to make it easier to keep track of scope
		StringBuilder reverser = new StringBuilder();
		try {
			reverser.insert(0, '\n');
			while((line=bufReader.readLine())!=null){
				if (!blankLine(line)){
					reverser.insert(0, line+'\n');
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return reverser.toString();		
	}


	/**
	 * A comparator that compares the input text of a {@link Completion}
	 * against a String lexicographically, ignoring case.
	 */
	@SuppressWarnings("rawtypes")
	public static class CaseInsensitiveComparator implements Comparator,
														Serializable {

		public int compare(Object o1, Object o2) {
			Completion c = (Completion)o1;
			// o2.toString() needed to help compile with 1.5+.
			return String.CASE_INSENSITIVE_ORDER.compare(
									c.getInputText(), o2.toString());
		}

	}


}