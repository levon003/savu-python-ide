package org.fife.rtext.plugins.debug;

import java.util.ArrayList;

import sun.awt.SunHints.Value;

public class VariableParser {
	
	/**
	 * Returns a Variable that represents the input string. May be a ComplexVariable that contains nested variables. Recursively calls self to parse the input. 
	 * Input strings must be formatted as either tyoe(value), type[list, of, variables] or type{list:of, key:value, pairs:of_variables}
	 * @param input A string representing a variable in the format specified above.
	 * @return
	 */
	public static Variable parse(String input){
		input = input.trim();
		int position = 0;
		//Inline Iteration to locate start of value
		while (!"({[<".contains(Character.toString(input.charAt(++position))) && position < input.length()){}

		//Error case if we don't recognize the input
		if(position > input.length()-2)
		{
			return new PrimitiveVariable("Error", "Unable to identify this object.");
		}
		
		//Separate out the type
		String type = input.substring(0, position);
		
		//We don't bother checking for a close ({[< - maybe this is a bad idea?
		String valueString = input.substring(position+1, input.length()-1);
		
		//If it's a primitive
		if (input.charAt(position) == '(')
		{
			return new PrimitiveVariable(type, valueString);
		}
		//List
		else if (input.charAt(position) == '[')
		{
			return parseList(type, valueString);
		}
		//Dict or class
		else if (input.charAt(position) == '{' || input.charAt(position) == '<')
		{
			return parseDictOrClass(type, valueString);
		}
		
		return null;
	}

	/**
	 * Helper method. Separates the provided item into a mapping.
	 * Expects a comma separated list of colon separated key-value pairs.
	 * Recursively calls parse on every key and every value
	 * @param type The type to give to the mapping style variable we return
	 * @param input The string representing the contents of the mapping style variable - stripped of {}
	 * @return A complex variable representing the mapping type variable
	 */
	private static Variable parseDictOrClass(String type, String input) {
		ArrayList<Variable> contents = new ArrayList<Variable>();
		ArrayList<Variable> keys = new ArrayList<Variable>();
		
		int startposition = 0;
		int position = 0;
		char stringStarter = '.';
		boolean lastEscaped = false;
		int parenDepth = 0;
		while (position < input.length())
		{
			//Code ensures that we don't accidentally find a comma inside a string
			if (input.charAt(position) == '\'' && stringStarter == '.')
			{
				stringStarter = input.charAt(position);
			}
			else if (input.charAt(position) == '"' && stringStarter == '.')
			{
				stringStarter = input.charAt(position);
			}
			else if (input.charAt(position) == stringStarter && !lastEscaped)
			{
				stringStarter = '.';
			}
			//We also don't want to find commas in sub sequences - nested lists and the like
			//Not matching parenthesis types seems a bit sketchy, but doesn't seem like it'll cause a problem.
			else if ((input.charAt(position) == '[' || input.charAt(position) == '{' || input.charAt(position) == '(' || input.charAt(position) == '<') && stringStarter == '.' )
			{
				++parenDepth;
			}
			else if ((input.charAt(position) == ']' || input.charAt(position) == '}' || input.charAt(position) == ')' || input.charAt(position) == '>') && stringStarter == '.' )
			{
				--parenDepth;
			}
			else if (input.charAt(position) == '\\' && !lastEscaped)
			{
				lastEscaped = true;
			}
			if (lastEscaped)
			{
				lastEscaped = false;
			}
			
			//We found the comma separating the current pair from the next one
			if (stringStarter == '.' && input.charAt(position) == ',' && parenDepth == 0)
			{
				String dictMapping = input.substring(startposition, position);
				Variable[] keyValuePair = parseDictElement(dictMapping);
				keys.add(keyValuePair[0]);
				contents.add(keyValuePair[1]);
				startposition = position+1;
			}
			++position;
		}
		//Add the last pair, if it exists
		if (position > startposition+1)
		{
			String dictMapping = input.substring(startposition, position);
			Variable[] keyValuePair = parseDictElement(dictMapping);
			keys.add(keyValuePair[0]);
			contents.add(keyValuePair[1]);
		}
		//Extract whether or not this is a true mapping type
		boolean isTrueMapping = type.charAt(0) == 't';
		type = type.substring(1);
		return new ComplexVariable(type, keys, contents, isTrueMapping);
	}
	
	/**
	 * Parses one colon separated key/value pair into a pair of Variable objects.
	 * Recurisively calls parse on the key and value
	 * @param dictMapping A colon separated kay/value pair
	 * @return an array of the form [key, value]
	 */
	private static Variable[] parseDictElement(String dictMapping)
	{
		int colonPosition = 0;
		char innerStringStarter = '.';
		boolean innerLastEscaped = false;
		int parenDepth = 0;
		while(colonPosition < dictMapping.length())
		{
			//Make sure we don't accidentally find a colon inside a string
			if (dictMapping.charAt(colonPosition) == '\'' && innerStringStarter == '.')
			{
				innerStringStarter = dictMapping.charAt(colonPosition);
			}
			else if (dictMapping.charAt(colonPosition) == '"' && innerStringStarter == '.')
			{
				innerStringStarter = dictMapping.charAt(colonPosition);
			}
			else if (dictMapping.charAt(colonPosition) == innerStringStarter && !innerLastEscaped)
			{
				innerStringStarter = '.';
			}
			//We also don't want to find commas in sub sequences - nested lists and the like
			//Not matching parenthesis types seems a bit sketchy, but doesn't seem like it'll cause a problem.
			else if ((dictMapping.charAt(colonPosition) == '[' || dictMapping.charAt(colonPosition) == '{' || dictMapping.charAt(colonPosition) == '(' || dictMapping.charAt(colonPosition) == '<') && innerStringStarter == '.' )
			{
				++parenDepth;
			}
			else if ((dictMapping.charAt(colonPosition) == ']' || dictMapping.charAt(colonPosition) == '}' || dictMapping.charAt(colonPosition) == ')' || dictMapping.charAt(colonPosition) == '>') && innerStringStarter == '.' )
			{
				--parenDepth;
			}
			else if (dictMapping.charAt(colonPosition) == '\\' && !innerLastEscaped)
			{
				innerLastEscaped = true;
			}
			if (innerLastEscaped)
			{
				innerLastEscaped = false;
			}
			
			//We found the colon, split into key and value and parse
			if (innerStringStarter == '.' && dictMapping.charAt(colonPosition) == ':' && parenDepth == 0)
			{
				return new Variable[]{parse(dictMapping.substring(0, colonPosition)), parse(dictMapping.substring(colonPosition+1))};
			}
			++colonPosition;
		}
		return null;
	}

	/**
	 * Helper method. Separates out the items in a list (should be a comma separated list of values)
	 * Parses each item into a Variable, and adds them all to a list of Variables in a ComplexVariable, which is then returned
	 * @param type The type to give to the returned ComplexVariable
	 * @param input A comma-separated list of variables stripped of []
	 * @return The ComplexVariable representing the passed in list type
	 */
	private static Variable parseList(String type, String input) {
		ArrayList<Variable> contents = new ArrayList<Variable>();
		
		int startposition = 0;
		int position = 0;
		int parenDepth = 0;
		
		//We need to find the comma separated items, but also ensure that the comma isn't inside a string
		//Thus we track whether we're in a string or not which involves tracking escapes and the type of quote used
		char stringStarter = '.';
		boolean lastEscaped = false;
		while (position < input.length())
		{
			//Start of a string
			if ((input.charAt(position) == '\'' || input.charAt(position) == '"')&& stringStarter == '.')
			{
				stringStarter = input.charAt(position);
			}
			//End of a string
			else if (input.charAt(position) == stringStarter && !lastEscaped)
			{
				stringStarter = '.';
			}
			//We don't want to find commas in sub-lists
			else if ((input.charAt(position) == '[' || input.charAt(position) == '{' || input.charAt(position) == '(') && stringStarter == '.' )
			{
				++parenDepth;
			}
			else if ((input.charAt(position) == ']' || input.charAt(position) == '}' || input.charAt(position) == ')') && stringStarter == '.' )
			{
				--parenDepth;
			}
			else 
			{
				//The next character will be escaped
				if (input.charAt(position) == '\\' && !lastEscaped)
				{
					lastEscaped = true;
				}
				else if(lastEscaped)
				{
					lastEscaped = false;
				}
			}
			
			
			//We found a comma
			if (stringStarter == '.' && input.charAt(position) == ',' && parenDepth == 0)
			{
				contents.add(parse(input.substring(startposition, position)));
				startposition = position+1;
			}
			++position;
		}
		//Add the last element
		if (position > startposition+1)
		{
			contents.add(parse(input.substring(startposition, position)));
		}
		return new ComplexVariable(type, contents);

	}
}