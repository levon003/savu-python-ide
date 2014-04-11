package org.fife.ui.autocomplete;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

public class PythonCompletionProvider extends DefaultCompletionProvider {
	private Hashtable<String, HashSet<Completion>> importedCompletions;
	private HashSet<Completion> builtInCompletions;
	public PythonCompletionProvider() {
		super();
		importedCompletions = new Hashtable<String, HashSet<Completion>>();
		builtInCompletions = new HashSet<Completion>();
	}

	public PythonCompletionProvider(String[] words) {
		super(words);
		importedCompletions = new Hashtable<String, HashSet<Completion>>();
		builtInCompletions = new HashSet<Completion>();
	}
	
	
	
	/**
	 * Utility function that finds the number of tabs at the beginning of a given <code>line</code>.
	 *
	 * @param line The line that should be searched to find number of tabs
	 * @return int Number of tabs at the start of the line
	 */
	private int findNumTabs(String line){
		int numTabs = 0;
		if (line==null){
			return numTabs;
		}
		char[] chars = line.toCharArray();
		while (numTabs < chars.length && chars[numTabs]==' '){
			numTabs++;
		}
		return numTabs;
	}
	
	
	/**
	 * Returns the text just before the current caret position that could be
	 * the start of something auto-completable.<p>
	 *
	 * This method returns all characters before the caret that are matched
	 * by  {@link #isValidChar(char)}.
	 *
	 * {@inheritDoc}
	 */
	@Override
	public String getAlreadyEnteredText(JTextComponent comp) {
		return getAlreadyEnteredText(comp, false);
	}
	
	
	/**
	 * Returns the text just before the current caret position.<p>
	 *
	 * This method returns all characters before the caret that are matched
	 * by  {@link #isValidChar(char)} if wholeLine is false, returning only text that is auto-completable
	 * 
	 * Else returns all characters before the caret that are matched
	 * by  {@link #isValidCharOrPeriod(char)} if wholeLine is true, returning the whole line before the caret.
	 *
	 * 
	 * @param wholeLine Whether to return only auto-completable text or the whole line
	 */
	@Override
	public String getAlreadyEnteredText(JTextComponent comp, boolean wholeLine) {
		
		Document doc = comp.getDocument();

		int dot = comp.getCaretPosition();
		Element root = doc.getDefaultRootElement();
		int index = root.getElementIndex(dot);
		Element elem = root.getElement(index);
		int start = elem.getStartOffset();
		int len = dot-start;
		try {
			doc.getText(start, len, seg);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			return EMPTY_STRING;
		}

		int segEnd = seg.offset + len;
		start = segEnd - 1;
		if (!wholeLine){
			while (start>=seg.offset && isValidChar(seg.array[start])) {
				start--;
			}
		}
		else{
			while (start>=seg.offset && isValidCharOrPeriod(seg.array[start])) {
				start--;
			}
		}
		start++;

		len = segEnd - start;
		return len==0 ? EMPTY_STRING : new String(seg.array, start, len);

	}
	
	
	/**
	 * Gets the possible BUILTIN completions from built-ins.txt
	 * Uses a regex looking for class names, function names, and variables
	 *
	 */
	public HashSet<Completion> getBuiltInCompletionOptions(){
		if (!builtInCompletions.isEmpty()){
			return builtInCompletions;
		}
		String[] regexes = {"def (\\w+ *[(][\\w, ]*[)])", "(\\w+) *={1}"};
		HashSet<Completion> allMatches = new HashSet<Completion>();
		BufferedReader bufReader = null;
		try {
			bufReader = new BufferedReader(new FileReader("lib/built-ins.txt"));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		int tabLevel;
		try {
			String line = bufReader.readLine();
			while (line != null){
				Pattern p = Pattern.compile("class (\\w+):");
				Matcher m = p.matcher(line);
				if (m.find() != true ){
					line = bufReader.readLine();
				}
				else{
					String clazz = m.group(1);
					allMatches.addAll(getClassCompletionOptionsRecursive(clazz, bufReader, true));
					line = bufReader.readLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		builtInCompletions = allMatches;
		return allMatches;
	}
	
	/**
	 * Gets the possible CLASS completions from the given <code>text</code>.
	 * Uses a regex looking for class names, function names, and variables
	 *
	 * @param text The text that should be searched to find completion options in
	 */
	public HashSet<Completion> getClassCompletionOptions(String text){
		String[] regexes = {"def (\\w+ *[(][\\w, ]*[)])", "(\\w+) *={1}"};
		HashSet<Completion> allMatches = new HashSet<Completion>();
		BufferedReader bufReader = new BufferedReader(new StringReader(text));
		int tabLevel;
		try {
			String line = bufReader.readLine();
			while (line != null){
				Pattern p = Pattern.compile("class ([\\w()]*)");
				Matcher m = p.matcher(line);
				if (m.find() != true ){
					line = bufReader.readLine();
				}
				else{
					String clazz = m.group(1);
					allMatches.addAll(getClassCompletionOptionsRecursive(clazz, bufReader, false));
					line = bufReader.readLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return allMatches;
	}
	
	/**
	 * Utility function to get the attributes of a given class
	 * Has to be recursive cause of stupid nested classes
	 * 
	 * @param clazz
	 * @param bufReader
	 * @param builtIn
	 * @return Arraylist of <code>AttributeCompletion</code> objects
	 */
	//TODO: Implement description grabbing for things other than builtin.txt
	private HashSet<Completion> getClassCompletionOptionsRecursive(String clazz, BufferedReader bufReader, boolean builtIn){
		String[] regexes = {"def (\\w+ *[(][\\w, \\[\\]=<>]*[)])", "(\\w+) *={1}"};
		HashSet<Completion> completions = new HashSet<Completion>();
		Matcher m = null;
		try {
			String line = bufReader.readLine();
			int tabLevel = findNumTabs(line);
			Pattern classPattern = Pattern.compile("class (\\w+):");
			Pattern variablePattern = Pattern.compile("self.(\\w+) *=");
			ArrayList<Pattern> descriptionTerminals = new ArrayList<Pattern>();
			descriptionTerminals.add(classPattern);
			descriptionTerminals.add(Pattern.compile("def (\\w+ *[(][\\w,\\[\\]= ]*[)]):"));
			while (line != null){
				// If we go in, we're out of the class by now
				if (findNumTabs(line)<tabLevel&&!line.equals("")){
					break;
				}
				if ((m=variablePattern.matcher(line)).find()){
					completions.add(new AttributeCompletion(this, m.group(1), clazz));
				}
				if (findNumTabs(line)==tabLevel){
					for (String regex : regexes){
						m = Pattern.compile(regex).matcher(line);
						if (m.find()){
							if (builtIn){
								String desc = parseForDescription(bufReader, descriptionTerminals);
								AttributeCompletion temp = new AttributeCompletion(this, m.group(1), null, desc,  clazz);
								completions.add(temp);
							}
							else{
								AttributeCompletion temp = new AttributeCompletion(this, m.group(1), clazz);
								completions.add(temp);
							}
						}
					}
				}
				m = classPattern.matcher(line);
				//Recurse if you find another class
				if (m.find()&&findNumTabs(line)>=tabLevel){
					completions.addAll(getClassCompletionOptionsRecursive(m.group(1),bufReader, builtIn));
				}
				line = bufReader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return completions;
	}
	
	/**
	 * Gets the possible GLOBAL completions from the given <code>text</code>.
	 * Uses a regex looking for class names, function names, and variables
	 *
	 * @param text The text that should be searched to find completion options in
	 */
	public HashSet<Completion> getGlobalCompletionOptions(String text, int caretPosition){
		String[] regexes = {"class (\\w+):", "def (\\w+ *[(][\\w, ]*[)])", "(\\w+) *={1}"};
		HashSet<Completion> allMatches = new HashSet<Completion>();
		String textInScope;
		try{
			textInScope = text.substring(0,caretPosition);
		}
		//TODO: WHY??????????? Fix this later
		catch (StringIndexOutOfBoundsException e){
			textInScope = "";
		}
		BufferedReader bufReader = new BufferedReader(new StringReader(textInScope));
		String line=null;
		
		// Only parsing lines that are in global scope (0 tabs)
		// Done by checking the current tab level of each line
		try {
			while ((line=bufReader.readLine()) != null )
			{
				if (findNumTabs(line)== 0){
					for (String regex : regexes) {
						Matcher m = Pattern.compile(regex).matcher(line);
						while (m.find()){
							allMatches.add(new BasicCompletion(this, m.group(1)));
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return allMatches;
	}
	
	/**
	 * Looks for the imported completion options. Will call pydoc if necessary or else read through the imported file
	 * 
	 * @param text The text in the document
	 * @param caretPosition Current caret position
	 * @param curDirectory Current directory
	 * @return Hashset of completions
	 */
	public HashSet<Completion> getImportCompletionOptions(String text, int caretPosition, String curDirectory){
		//rtext.getMainView().getCurrentTextArea().getFileFullPath(); put that where this is called and pass in current directory 
		String textInScope;
		try{
			textInScope = text.substring(0,caretPosition);
		}
		//TODO: Investigate the effect of highlighting on caretPosition
		catch (StringIndexOutOfBoundsException e){
			textInScope = "";
		}
		HashSet<Completion> ret = new HashSet<Completion>();
		// Need two different hashtables for the two different methods of importing things
		// Actually two different cases, lumping them to one isn't possible without jankitude
		Hashtable<String, String[]> fileImports = new Hashtable<String, String[]>();
		Hashtable<String, String[]> objectImports = new Hashtable<String, String[]>();
		BufferedReader bufReader = new BufferedReader(new StringReader(textInScope));
		String line, key;
		String[] imports;
		Matcher m;
		try {
			while ((line = bufReader.readLine())!= null){
				if ((m = Pattern.compile("from (\\w+) import ([\\w*, ]+)").matcher(line)).find()){
					key = m.group(1);
					imports = m.group(2).split(" *, *");
					objectImports.put(key, imports);
				}
				else if ((m = Pattern.compile("import ([\\w, ]+)").matcher(line)).find()){
					String[] files = m.group(1).split(",");
					for (String file : files){
						fileImports.put(file.trim(), new String[]{null});
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		PydocExecutor executor = new PydocExecutor(this);
		Enumeration<String> keys = fileImports.keys();
		// Dealing with just the basic "import sys"
		while(keys.hasMoreElements()){
			String module = keys.nextElement();
			File importedFile;
			// If it's cached, just return cache
			if (importedCompletions.containsKey(module)){
				ret.addAll(importedCompletions.get(module));
			}
			//if it's a local file, parse it for autocomplete
			else if ((importedFile = new File(curDirectory + "/" + module + ".py")).exists()){
				ret.addAll(parseFileForAutocomplete(importedFile, module, true, fileImports.get(module)));
			}
			//if it's neither of the above, it's a built-in module
			else{
				String docstring = executor.getDocstring(module);
				//make sure what they're typing is a module (won't be if they're in the middle of typing it)
				if (!docstring.contains("no Python documentation found for")){
					HashSet<Completion> completionsToBeCached = executor.parseDocstringForFile(module, docstring, true);
					ret.addAll(completionsToBeCached);
					importedCompletions.put(module, completionsToBeCached);
				}
			}
		}
		//deals with "from sys import *"
		keys = objectImports.keys();
		while(keys.hasMoreElements()){
			String module = keys.nextElement();
			File importedFile;
			// If it's cached, just return cache
			if (importedCompletions.containsKey(module)){
				ret.addAll(importedCompletions.get(module));
			}
			//if it's a local file, parse it for autocomplete
			else if ((importedFile = new File(curDirectory + "/" + module + ".py")).exists()){
				ret.addAll(parseFileForAutocomplete(importedFile, module, false, objectImports.get(module)));
			}
			//if it's neither of the above, it's a built-in module
			else{
				String docstring = executor.getDocstring(module);
				//make sure what they're typing is a module (won't be if they're in the middle of typing it)
				if (!docstring.contains("no Python documentation found for")){
					HashSet<Completion> completionsToBeCached = executor.parseDocstringForFile(module, docstring, false);
					ret.addAll(completionsToBeCached);
					importedCompletions.put(module, completionsToBeCached);
				}
			}
		}
		return ret;
	}
	
	/**
	 * Gets the possible  LOCAL completions from the given <code>text</code>.
	 * Uses a regex looking for class names, function names, and variables
	 *
	 * @param text The text that should be searched to find completion options in
	 * @param caretPosition The current position of the caret (used to determine local scope)
	 */
	public HashSet<Completion> getLocalCompletionOptions(String text, int caretPosition){
		String[] regexes = {"class (\\w+):", "def (\\w+ *[(][\\w, ]*[)])", "(\\w+) *={1}"};
		HashSet<Completion> allMatches = new HashSet<Completion>();
		String textInLocalScope;
		try{
			textInLocalScope = text.substring(0, caretPosition); //TODO investigate the effect of highlighting on caret position
			//FIX THIS LOLLLLLLLLLLL
		}
		catch(IndexOutOfBoundsException e){
			textInLocalScope = "";
		}
		BufferedReader bufReader = new BufferedReader(new StringReader(textInLocalScope));
		String line=null;
		
		// Reverses the text line by line, to make it easier to keep track of scope

		text = flipTextLines(textInLocalScope);		
		bufReader = new BufferedReader(new StringReader(text));
		int minTabs = Integer.MAX_VALUE;
		int curTabs;
		
		// Only parsing lines that are in scope
		// Done by checking the current tab level
		try {
			while ((line=bufReader.readLine()) != null )
			{
				curTabs = findNumTabs(line);
				if (curTabs < minTabs){
					minTabs = curTabs;
				}
				if (curTabs == minTabs){
					for (String regex : regexes) {
						Matcher m = Pattern.compile(regex).matcher(line);
						while (m.find()){
							allMatches.add(new BasicCompletion(this, m.group(1)));
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return allMatches;
	}
	
	
	
	/**
	 * Returns whether the specified character is valid 
	 * Includes the period character
	 * Used to get the whole text of the current line
	 * The default implementation is equivalent to
	 * "<code>Character.isLetterOrDigit(ch) || ch=='_' || ch=='.'</code>".  Subclasses
	 * can override this method to change what characters are matched.
	 *
	 * @param ch The character.
	 * @return Whether the character is valid.
	 */
	protected boolean isValidCharOrPeriod(char ch) {
		return Character.isLetterOrDigit(ch) || ch=='_' || ch=='.';
	}
	
	/**
	 * If a file is imported, parse it for possible autocompletion options
	 * 
	 * @param importedFile File to parse
	 * @param module Module name (might be needed for attribute completions)
	 * @param createModuleAttributes Whether or not to make completions attributes of the modules
	 * @param matches Stuff to be looking for specifically (if not importing *)
	 * @return an arraylist of completions extracted from the file
	 */
	private ArrayList<Completion> parseFileForAutocomplete(File importedFile, String module, boolean createModuleAttributes, String[] matches){
		ArrayList<Completion> ret = new ArrayList<Completion>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(importedFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String[] regexes = {"def (\\w+ *[(][\\w, ]*[)])", "(\\w+) *={1}"};
		String line, match;
		Matcher m;
		//First comb through the file getting all global stuffs
		try {
			while ((line=br.readLine()) != null )
			{
				if (findNumTabs(line)== 0){
					for (String regex : regexes) {
						m = Pattern.compile(regex).matcher(line);
						while (m.find()){
							match = m.group(1);
							if (createModuleAttributes){
								ret.add(new AttributeCompletion(this,match,module));
							}
							else{//TODO: filter out stuff that's not in matches
								ret.add(new BasicCompletion(this, match));
							}
						}
					}
					m = Pattern.compile("class ([\\w().\\[\\]\'\"]+):").matcher(line);
					if (m.find()){
						match = m.group(1);
						if (createModuleAttributes){
							ret.add(new AttributeCompletion(this,match,module));
							String clazz = m.group(1);
							ret.addAll(getClassCompletionOptionsRecursive(clazz, br, false));
							line = br.readLine();
						}
						else{
							ret.add(new BasicCompletion(this, match));
							String clazz = m.group(1);
							ret.addAll(getClassCompletionOptionsRecursive(clazz, br, false));
							line = br.readLine();
						}
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	/**
	 * Will read lines from the buffered reader until it reaches one of the terminal patterns.
	 * Once it reaches a terminal, returns all the lines that were read.
	 * Has the side effect of advancing the buffered reader object to the terminal pattern as well
	 * 
	 * @param br The buffered reader of the string that contains the docstring
	 * @param terminalPatterns List of patterns to read until
	 * @return The description of the current function/class
	 */
	private String parseForDescription(BufferedReader br, ArrayList<Pattern> terminalPatterns){
		StringBuilder desc = new StringBuilder();
		String line;
		Matcher m;
		try {
			br.mark(1000);
			while ((line=br.readLine())!=null){
				for (Pattern pattern : terminalPatterns){
					m = pattern.matcher(line);
					if (m.find()){
						br.reset();
						return desc.toString().trim();
					}
				}
				line = line.replaceAll("[|]", "");
				//if line is only whitespace, add a space
				if (line.matches("\\s*")){
					line = "<br><br>";
				}
				desc.append(line);
				br.mark(1000);
			}
			br.reset();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return desc.toString().trim();
	}

}
