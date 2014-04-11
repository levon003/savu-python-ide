package org.fife.ui.autocomplete;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.Document;
import javax.swing.text.Style;

/**
 * Runs a Python program based on file name, outputting to the specified RunTextArea
 * 
 * @author PyDe
 *
 */
class PydocExecutor
{
	PythonCompletionProvider provider;
	
	public PydocExecutor(PythonCompletionProvider provider)
	{
		this.provider = provider;
	}
	
	/**
	 * Gets the docstring for the given module by checking its pydoc
	 * 
	 * @param moduleName
	 * @return the docstring for <code>moduleName</code>
	 */
	public String getDocstring(String moduleName)
	{
		try
		{
			boolean startedReading = false;
			Process p = Runtime.getRuntime().exec("pydoc "+moduleName);
			InputStream processOutput = p.getInputStream();
			StringBuilder outputCatcher = new StringBuilder();
			char c;
			//Loop while waiting for there to be output
			while(!startedReading){
				//once we start reading, read until there's no more available
				while(processOutput.available()>0){
					startedReading=true;
					c = (char)processOutput.read();
					outputCatcher.append(c);
				}
			}
			String fullOutput = outputCatcher.toString();
			return fullOutput;
		}
		catch (IOException  e)
		{
			e.printStackTrace();
		}
		return "";
	}	
	
	/**
	 * Parses the given docstring and returns a hashset of completions for it.
	 * If createModuleAttributes is true, will make all completions attributes of the module. 
	 * i.e. If the import statement is "import random" then the completion we want is "Random" as
	 * an attribute of "random" that way it will show up when they type "random.Random()"
	 * 
	 * If the import is "from random import *", then we don't want it as an attribute. Instead
	 * of typing "random.Random()", the user can just type "Random()"
	 * 
	 * @param module The module name
	 * @param docstring The docstring to parse
	 * @param createModuleAttributes Whether or not to make completions attributes of a module or regular completions
	 */
	public HashSet<Completion> parseDocstringForFile(String module, String docstring, boolean createModuleAttributes){
		HashSet<Completion> ret = new HashSet<Completion>();
		BufferedReader br = new BufferedReader(new StringReader(docstring));
		String line;
		try {
			Pattern classPattern = Pattern.compile("class ([\\w,()<>=\\[\\]._]+)$");
			Pattern functionPattern = Pattern.compile("^    (\\w+[(][\\w, \'\".=<>\\[\\]]*[)])");
			
			//Pattern2 matches functions that are inherited
			Pattern functionPattern2 = Pattern.compile("^        (\\w+[(][\\w=, \'\"<>\\[\\]]*[)])");
			Pattern variablePattern = Pattern.compile("^    (\\w+) = [\\w'\" =()<>\\[\\]]+");
			Matcher m;
			String desc;
			boolean inClasses = false;
			boolean inFunctions = false;
			boolean inData = false;
			while((line = br.readLine())!=null){
				if (line.matches("CLASSES")){
					inClasses = true;
					continue;
				}
				else if (line.matches("FUNCTIONS")){
					inFunctions = true;
					continue;
				}
				else if (line.matches("DATA")){
					inData = true;
					continue;
				}
				if (inClasses&&(m = classPattern.matcher(line)).find()){
					String clazz = m.group(1);
					clazz = clazz.replaceAll("[(][\\w,()\\[\\]._ ]+[)]", "");
					ArrayList<Pattern> terminals = new ArrayList<Pattern>();
					terminals.add(Pattern.compile("Methods defined here"));
					desc = parseForDescription(br, terminals);
					if (createModuleAttributes){
						ret.add(new AttributeCompletion(provider, clazz, null, desc, module));
					}
					else{
						ret.add(new BasicCompletion(provider, clazz, null, desc));
					}
					ret.addAll(parseClass(br, clazz));
				}
				else if(inFunctions&&(m = functionPattern.matcher(line)).find()){
					String funct = m.group(1);
					ArrayList<Pattern> terminals = new ArrayList<Pattern>();
					if (funct.contains("...")){
						br.mark(1000);
						line = br.readLine();
						if ((m =functionPattern2.matcher(line)).find()){
							funct = m.group(1);
						}
						br.reset();
					}
					terminals.add(Pattern.compile("^    (\\w+[(][\\w., \'\"=<>\\[\\]]*[)])"));
					terminals.add(Pattern.compile("^$"));
					//terminals.add(functionPattern2);
					desc = parseForDescription(br, terminals);
					if (createModuleAttributes){
						ret.add(new AttributeCompletion(provider, funct, null, desc, module));
					}
					else{
						ret.add(new BasicCompletion(provider, funct, null, desc));
					}
				}
				/*else if(inFunctions&&(m = functionPattern2.matcher(line)).find()){
					String funct = m.group(1);
					ArrayList<Pattern> terminals = new ArrayList<Pattern>();
					terminals.add(Pattern.compile("^    (\\w+[(][\\w., =<>\\[\\]]*[)])"));
					terminals.add(Pattern.compile("^$"));
					//terminals.add(functionPattern2);
					if (funct.equals("displayhook(object)")){
						System.out.println("hey2");
					}
					desc = parseForDescription(br, terminals);
					if (createModuleAttributes){
						ret.add(new AttributeCompletion(provider, funct, null, desc, module));
					}
					else{
						ret.add(new BasicCompletion(provider, funct, null, desc));
					}
				}*/
				else if(inData&&(m = variablePattern.matcher(line)).find()){
					String var = m.group(1);
					if (createModuleAttributes){
						ret.add(new AttributeCompletion(provider, var, module));
					}
					else{
						ret.add(new BasicCompletion(provider, var));
					}
				}
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	/**
	 * Parses a class within a pydoc string, returning all of it's attributes as attribute completions of that class
	 * Has the side effect of advancing the buffered reader to the end of the class' documentation
	 * 
	 * @param br The buffered reader for the docstring
	 * @param clazz The class that the attributes should be associated with
	 * @return an arraylist of completions for the current class
	 */
	private ArrayList<Completion> parseClass(BufferedReader br, String clazz){
		ArrayList<Completion> ret = new ArrayList<Completion>();
		Pattern functionPattern = Pattern.compile("\\|  (\\w+[(][\\w, _()]+[)])$");
		Pattern inheritedFunctionPattern =  Pattern.compile("\\|      (\\w+[(][\\w, _()]+[)]) *");
		Pattern variablePattern = Pattern.compile("\\|  (\\w+)$");
		Matcher m;
		ArrayList<Pattern> terminals = new ArrayList<Pattern>();
		terminals.add(functionPattern);
		terminals.add(variablePattern);
		terminals.add(Pattern.compile("^     \\|  [-]{5}"));
		String line;
		String desc;
		try {
			while((line = br.readLine())!=null){
				//If we've reached the end of the class documentation
				if (!line.contains("|")){
					break;
				}
				if((m = functionPattern.matcher(line)).find()){
					desc = parseForDescription(br, terminals);
					ret.add(new AttributeCompletion(provider, m.group(1), null, desc, clazz));
				}
				else if((m = inheritedFunctionPattern.matcher(line)).find()){
					desc = parseForDescription(br, terminals);
					ret.add(new AttributeCompletion(provider, m.group(1), null, desc, clazz));
				}
				else if((m = variablePattern.matcher(line)).find()){
					ret.add(new AttributeCompletion(provider, m.group(1), clazz));
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