package org.fife.ui.search;

import java.util.regex.Matcher;

import javax.swing.JTextArea;
import javax.swing.text.Caret;

import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

/**
 * A wrapper class for the RSyntaxTextArea SearchEngine class
 * Behavior is almost identical, but finding an entry wraps past the end of the file
 * Signatures are identical to SearchEngine, so can be used as an easy replacement
 * For documentation on individual methods, see SearchEngine
 * @author maegereg
 *
 */
public class WrappingSearchEngine{
	
	/**
	 * Protected constructor to prevent instantiation
	 */
	protected WrappingSearchEngine()
	{}
	
	public static SearchResult find(JTextArea textArea, SearchContext context)
	{
		SearchResult result = SearchEngine.find(textArea, context);
		//Wrap past the end of the file
		if (! result.wasFound())
		{
			Caret caret = textArea.getCaret();
			int oldDot = caret.getDot();
			
			//We just start search again at the top of the file
			caret.setDot(0);
			textArea.setCaret(caret);
			result = SearchEngine.find(textArea, context);
			
			//Ensure that the cursor isn't moved by a failed find
			if (! result.wasFound())
			{
				caret.setDot(oldDot);
				textArea.setCaret(caret);
			}
		}
		return result;
	}
	
	public static int getNextMatchPos(String searchFor, String searchIn, boolean forward, boolean matchcase, boolean wholeWord)
	{
		return SearchEngine.getNextMatchPos(searchFor, searchIn, forward, matchcase, wholeWord);
	}
	
	public static String getReplacementText(Matcher m, CharSequence template)
	{
		return SearchEngine.getReplacementText(m, template);
	}
	
	public static SearchResult markAll(RTextArea textArea, SearchContext context)
	{
		return SearchEngine.markAll(textArea, context);
	}
	
	public static SearchResult replace(RTextArea textArea, SearchContext context)
	{
		return SearchEngine.replace(textArea, context);
	}
	
	public static SearchResult replaceAll(RTextArea textArea, SearchContext context)
	{
		return SearchEngine.replaceAll(textArea, context);
	}
}

