package lexicalAnalyzer;

import tokens.LextantToken;
import tokens.Token;

public enum Punctuator implements Lextant {
	// Arithmetic Operators
	ADD("+"), 
	MULTIPLY("*"),
	SUBTRACT("-"), 
	DIVIDE("/"),
	
	// Comparison Operators
	GREATER(">"), 
	EQUAL("=="), 
	NOT_EQUAL("!="), 
	LESS("<"),
	GREATER_OR_EQUAL(">="), 
	LESS_OR_EQUAL("<="),

	// Punctuation
	ASSIGN(":="), 
	SEPARATOR(","), 
	SPACE(";"), 
	TERMINATOR("."), 
	OPEN_BRACE("{"),
	CLOSE_BRACE("}"), 
	NULL_PUNCTUATOR(""),
	OPEN_PARANTHESES("("),
	CLOSE_PARENTHESES(")"),
	OPEN_SQUARE_BRACKET("["),
	CLOSE_SQUARE_BRACKET("]"), 
	CAST("|");
	

	private String lexeme;
	private Token prototype;

	private Punctuator(String lexeme) {
		this.lexeme = lexeme;
		this.prototype = LextantToken.make(null, lexeme, this);
	}

	public String getLexeme() {
		return lexeme;
	}

	public Token prototype() {
		return prototype;
	}

	public static Punctuator forLexeme(String lexeme) {
		for (Punctuator punctuator : values()) {
			if (punctuator.lexeme.equals(lexeme)) {
				return punctuator;
			}
		}
		return NULL_PUNCTUATOR;
	}

	public static final Punctuator[] ComparisonOperators = { GREATER, GREATER_OR_EQUAL, LESS, LESS_OR_EQUAL, EQUAL,
			NOT_EQUAL };

	public static boolean isComparisonPunctuator(String lexeme) {
		for (Punctuator punctuator : ComparisonOperators) {
			if (punctuator.lexeme.equals(lexeme)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * // the following hashtable lookup can replace the implementation of forLexeme
	 * above. It is faster but less clear. private static LexemeMap<Punctuator>
	 * lexemeToPunctuator = new LexemeMap<Punctuator>(values(), NULL_PUNCTUATOR);
	 * public static Punctuator forLexeme(String lexeme) { return
	 * lexemeToPunctuator.forLexeme(lexeme); }
	 */

}
