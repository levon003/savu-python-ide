import sys
import inspect
import collections

'''
Accepts two arguments: the dictionary generated by a call to locals() and the dictionary generated by a call to globals()
Both of these functions need to be called at the point this function is called, so they must be passed as arguments
Returns a string formatted as translateObject would return that represents all the items in localVariables that are not imported from modules
This method is not perfect - if the user defines a global variable that perfectly replicates the name and value of a variable defined in a module, it will be excluded
There seems no good way around this limitation
'''
def parseLocalVariables(localVariables, globalVariables):
	toReturn = {}
	#Filter out things that are included because of from _ import *
	for variable in localVariables:
		if not isModulePair(variable, localVariables[variable], globalVariables):
			toReturn[variable] = localVariables[variable]
	return translateObject(toReturn)


#Checks if a given pair of items is defined in any imported modules. If so, this probably means they are the result of an from _ import *
def isModulePair(key, value, globalVariables):
	for module in sys.modules:
		#sys.modules contains __main__ and __builtins__, neither of which we want to exclude items from
		if module[0:2] != "__" and module[-2:] != "__":
			if (key, value) in inspect.getmembers(sys.modules[module]):
				#This ensures that we can have local variables with the same name and value as something in a module without trouble.
				#Globals with identical name and value to a module will choke, but there doesn't seem a way around that
				if key in globalVariables and globalVariables[key] == value:
					return True
	return False

'''
Returns the first dictionary with all items present in the second dictionary removed
'''
def subtractDictionaries(dict1, dict2):
	toReturn = {}
	for key in dict1:
		if key not in dict2:
			toReturn[key] = dict1[key]
	return toReturn

'''
Accepts a python object (so literally anything) and returns a string that is formatted according to the following specification
every object is of the form type(repr) where type is the string representation of the object type and repr is the representation of its contents
Sequences are represented as a comma-separated list of values, and maps are represented as a comma separated list of colon-separated key:value pairs
'''

#"When you look into the dir(), the dir() looks into you." - Daniel SM
def translateObject(structure):
	#If it's a sequence type (or a set type, because they can be treated identically)
	if (isinstance(structure, collections.Sequence) or isinstance(structure, collections.Set)) and not isinstance(structure, basestring):
		toReturn = str(type(structure)).split("'")[1]+"["
		for thing in structure:
			translatedThing = translateObject(thing)
			if translatedThing is not None:
				toReturn += translatedThing+","
		return toReturn.rstrip(",")+"]"

	#Mapping type
	#The first letter of the type is a t to indicate that this is a true mapping type and not a class
	elif isinstance(structure, collections.Mapping):
		toReturn = "t"+str(type(structure)).split("'")[1]+"{"
		for key in structure:
			#Ignore magic variables
			if isinstance(key, basestring) and key[0:2] == "__" and key[-2:] == "__":
				continue
			translatedKey = translateObject(key)
			translatedValue = translateObject(structure[key])
			#print translatedKey, translatedValue
			if translatedKey is not None and translatedValue is not None:
				toReturn += translatedKey+":"+translatedValue+","
		return toReturn.rstrip(",")+"}"

	#If it's a class we need to examine internally
	#The first letter of the type is a f to indicate that this is a class and not a true mapping type
	elif hasattr(structure, '__dict__') and not inspect.ismodule(structure) and not inspect.isclass(structure) and not inspect.isfunction(structure):
		return "f"+structure.__class__.__name__+"<"+translateObject(structure.__dict__).lstrip("dict{").rstrip("}")+">"

	#If it's a function module or class we don't want to show it in our debug output
	elif inspect.ismodule(structure) or inspect.isclass(structure) or inspect.isfunction(structure):
		return None

	#Otherwise, we can hopefully deal with it (or we have no hope of dealing with it)
	else:
		return str(type(structure)).split("'")[1]+"("+repr(structure)+")"