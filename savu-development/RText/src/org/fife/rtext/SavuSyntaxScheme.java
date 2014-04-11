package org.fife.rtext;

import java.awt.Color;
import java.awt.Font;

import javax.swing.text.StyleContext;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;

public class SavuSyntaxScheme extends SyntaxScheme {

	public SavuSyntaxScheme(boolean useDefaults) {
		super(useDefaults);
		// TODO Auto-generated constructor stub
	}

	public SavuSyntaxScheme(Font baseFont) {
		super(baseFont);
		// TODO Auto-generated constructor stub
	}

	public SavuSyntaxScheme(Font baseFont, boolean fontStyles) {
		super(baseFont, fontStyles);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Restores all colors and fonts to their default values.
	 *
	 * @param baseFont The base font to use when creating this scheme.  If
	 *        this is <code>null</code>, then a default monospaced font is
	 *        used.
	 * @param fontStyles Whether bold and italic should be used in the scheme
	 *        (vs. all tokens using a plain font).
	 */
	@Override
	public void restoreDefaults(Font baseFont, boolean fontStyles) {
		
		//COLORCHANGE supposedly changes the syntax highlighting 
		// Colors used by tokens.
		
		Color comment			= new Color(170,170,150);
		Color docComment		= new Color(170,170,150);
		Color markupComment		= new Color(170,170,150);
		Color keyword			= new Color(125,210,250);
		Color dataType			= new Color(0,130,130);
		Color function			= new Color(255,150,60);
		Color preprocessor		= new Color(128,128,128);
		Color operator			= new Color(220,5,110);
		Color regex				= new Color(0,130,160);
		Color variable			= new Color(255,50,130);
		Color literalNumber		= new Color(255,128,128);
		Color literalString		= new Color(120,200,110);
		Color error			= new Color(148,148,0);

		// (Possible) special font getStyles() for keywords and comments.
		if (baseFont==null) {
			baseFont = RSyntaxTextArea.getDefaultFont();
		}
		Font commentFont = baseFont;
		Font keywordFont = baseFont;
		if (fontStyles) {
			// WORKAROUND for Sun JRE bug 6282887 (Asian font bug in 1.4/1.5)
			// That bug seems to be hidden now, see 6289072 instead.
			StyleContext sc = StyleContext.getDefaultStyleContext();
			Font boldFont = sc.getFont(baseFont.getFamily(), Font.BOLD,
					baseFont.getSize());
			Font italicFont = sc.getFont(baseFont.getFamily(), Font.ITALIC,
					baseFont.getSize());
			commentFont = italicFont;//baseFont.deriveFont(Font.ITALIC);
			keywordFont = boldFont;//baseFont.deriveFont(Font.BOLD);
		}

		getStyles()[COMMENT_EOL]				= new Style(comment, null, commentFont);
		getStyles()[COMMENT_MULTILINE]			= new Style(comment, null, commentFont);
		getStyles()[COMMENT_DOCUMENTATION]		= new Style(docComment, null, commentFont);
		getStyles()[COMMENT_KEYWORD]			= new Style(new Color(255,152,0), null, commentFont);
		getStyles()[COMMENT_MARKUP]			= new Style(Color.gray, null, commentFont);
		getStyles()[RESERVED_WORD]				= new Style(keyword, null, keywordFont);
		getStyles()[RESERVED_WORD_2]			= new Style(new Color(170,140,220));
		getStyles()[FUNCTION]					= new Style(function);
		getStyles()[LITERAL_BOOLEAN]			= new Style(literalNumber);
		getStyles()[LITERAL_NUMBER_DECIMAL_INT]	= new Style(literalNumber);
		getStyles()[LITERAL_NUMBER_FLOAT]		= new Style(literalNumber);
		getStyles()[LITERAL_NUMBER_HEXADECIMAL]	= new Style(literalNumber);
		getStyles()[LITERAL_STRING_DOUBLE_QUOTE]	= new Style(literalString);
		getStyles()[LITERAL_CHAR]				= new Style(new Color(200,160,150));
		getStyles()[LITERAL_BACKQUOTE]			= new Style(literalString);
		getStyles()[DATA_TYPE]				= new Style(dataType, null, keywordFont);
		getStyles()[VARIABLE]					= new Style(variable);
		getStyles()[REGEX]						= new Style(regex);
		getStyles()[ANNOTATION]				= new Style(Color.gray);
		getStyles()[IDENTIFIER]				= new Style(new Color(255,240,210));
		getStyles()[WHITESPACE]				= new Style(Color.gray);
		getStyles()[SEPARATOR]				= new Style(new Color(0,160,200));
		getStyles()[OPERATOR]					= new Style(operator);
		getStyles()[PREPROCESSOR]				= new Style(preprocessor);
		getStyles()[MARKUP_TAG_DELIMITER]		= new Style(Color.RED);
		getStyles()[MARKUP_TAG_NAME]			= new Style(Color.BLUE);
		getStyles()[MARKUP_TAG_ATTRIBUTE]		= new Style(new Color(63,127,127));
		getStyles()[MARKUP_TAG_ATTRIBUTE_VALUE]= new Style(literalString);
		getStyles()[MARKUP_COMMENT]              = new Style(markupComment, null, commentFont);
		getStyles()[MARKUP_DTD]              = new Style(function);
		getStyles()[MARKUP_PROCESSING_INSTRUCTION] = new Style(preprocessor);
		getStyles()[MARKUP_CDATA]				= new Style(new Color(0xcc6600));
		getStyles()[MARKUP_CDATA_DELIMITER]		= new Style(new Color(0x008080));
		getStyles()[MARKUP_ENTITY_REFERENCE]		= new Style(dataType);
		getStyles()[ERROR_IDENTIFIER]			= new Style(error);
		getStyles()[ERROR_NUMBER_FORMAT]		= new Style(error);
		getStyles()[ERROR_STRING_DOUBLE]		= new Style(error);
		getStyles()[ERROR_CHAR]				= new Style(error);

	}

}
