package lexicalAnalyzer;

import logging.PikaLogger;

import inputHandler.InputHandler;
import inputHandler.LocatedChar;
import inputHandler.LocatedCharStream;
import inputHandler.PushbackCharStream;
import inputHandler.TextLocation;
import tokens.IdentifierToken;
import tokens.LextantToken;
import tokens.NullToken;
import tokens.StringToken;
import tokens.IntegerToken;
import tokens.CharacterToken;
import tokens.FloatingToken;
import tokens.Token;

import static lexicalAnalyzer.PunctuatorScanningAids.*;

public class LexicalAnalyzer extends ScannerImp implements Scanner {
	public static LexicalAnalyzer make(String filename) {
		InputHandler handler = InputHandler.fromFilename(filename);
		PushbackCharStream charStream = PushbackCharStream.make(handler);
		return new LexicalAnalyzer(charStream);
	}

	public LexicalAnalyzer(PushbackCharStream input) {
		super(input);
	}

	//////////////////////////////////////////////////////////////////////////////
	// Token-finding main dispatch

	@Override
	protected Token findNextToken() {
		LocatedChar ch = nextNonWhitespaceChar();

		// Remove comments while scanning instead of doing it while parsing
		while (ch.isCommentToken()) {
			ch = input.next();
			while (!(ch.isCommentToken() || ch.isNewLine())) {
				ch = input.next();
			}
			ch = nextNonWhitespaceChar();
		}

		if (ch.isNumberStart()) {
			return scanNumber(ch);
		} else if (ch.isCharacterSymbol()) {
			return scanCharacter(ch);
		} else if (ch.isIdentifierStart()) {
			return scanIdentifier(ch);
		} else if (ch.isStringSymbol()) {
			return scanString(ch);
		} else if (isPunctuatorStart(ch)) {
			return PunctuatorScanner.scan(ch, input);
		} else if (isEndOfInput(ch)) {
			return NullToken.make(ch.getLocation());
		} else {
			lexicalError(ch);
			return findNextToken();
		}
	}

	private LocatedChar nextNonWhitespaceChar() {
		LocatedChar ch = input.next();
		while (ch.isWhitespace()) {
			ch = input.next();
		}
		return ch;
	}

	//////////////////////////////////////////////////////////////////////////////
	// Integer lexical analysis

	private Token scanNumber(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		boolean digitsScanned = false;

		LocatedChar next = input.peek();

		if (firstChar.isNumericSign() && (!next.isDigit() && !next.isDecimalPoint())) {
			return PunctuatorScanner.scan(firstChar, input);
		} else if(firstChar.isDecimalPoint() && !next.isDigit()) {
			return PunctuatorScanner.scan(firstChar, input);
		}

		buffer.append(firstChar.getCharacter());
		if(firstChar.isDigit()) {
			digitsScanned = true;
		}

		if (firstChar.isDecimalPoint()) {
			return appendFloatingPointAfterDecimal(firstChar, buffer);
		} else {
			int bufferLength = buffer.length();
			appendSubsequentDigits(buffer);
			if(bufferLength < buffer.length()) {
				// Digits have been added
				digitsScanned = true;
			}
			next = input.next();
			if (next.isDecimalPoint() && input.peek().isDigit()) {
				buffer.append(next.getCharacter());
				return appendFloatingPointAfterDecimal(firstChar, buffer);
			} else if(next.isDecimalPoint() && !input.peek().isDigit() && !digitsScanned) {
				next = input.next();
				lexicalErrorFloating(next);
				return findNextToken();
			}
			input.pushback(next);
		}

		return IntegerToken.make(firstChar.getLocation(), buffer.toString());
	}

	private Token appendFloatingPointAfterDecimal(LocatedChar firstChar, StringBuffer buffer) {
		appendSubsequentDigits(buffer);
		LocatedChar next = input.peek();
		
		if (next.getCharacter() == 'E') {
			next = input.next();
			buffer.append(next.getCharacter());
			next = input.peek();
			if (next.isNumericSign()) {
				next = input.next();
				buffer.append(next.getCharacter());
			}

			next = input.peek();
			if (next.isDigit()) {
				appendSubsequentDigits(buffer);
			} else {
				next = input.next();
				lexicalErrorFloating(next);
				return findNextToken();
			}
		}
		
		return FloatingToken.make(firstChar.getLocation(), buffer.toString());
	}

	private void appendSubsequentDigits(StringBuffer buffer) {
		LocatedChar c = input.next();
		while (c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}

	//////////////////////////////////////////////////////////////////////////////
	// Identifier and keyword lexical analysis

	private Token scanIdentifier(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		appendSubsequentIdentifierCharacters(buffer);

		String lexeme = buffer.toString();
		if (Keyword.isAKeyword(lexeme)) {
			return LextantToken.make(firstChar.getLocation(), lexeme, Keyword.forLexeme(lexeme));
		} else {
			return IdentifierToken.make(firstChar.getLocation(), lexeme);
		}
	}

	private void appendSubsequentIdentifierCharacters(StringBuffer buffer) {
		int identifierLength = buffer.length();

		LocatedChar c = input.next();
		LocatedChar faultyChar = c;

		while (c.isValidIdentifierCharacter()) {
			buffer.append(c.getCharacter());
			identifierLength++;

			if (identifierLength == 33) {
				faultyChar = c;
			}
			c = input.next();
		}
		input.pushback(c);

		if (identifierLength > 32) {
			lexicalErrorIdentifier(faultyChar);
		}
	}

	private Token scanCharacter(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		boolean characterScannedProperly = appendSubsequentCharacterToken(buffer);
		
		if(characterScannedProperly) {
			String lexeme = buffer.toString();
			return CharacterToken.make(firstChar.getLocation(), lexeme);
		} else {
			return findNextToken();
		}
	}

	private boolean appendSubsequentCharacterToken(StringBuffer buffer) {
		LocatedChar c = input.next();
		if (c.isCharacter()) {
			buffer.append(c.getCharacter());
			c = input.peek();

			if (c.isCharacterSymbol()) {
				buffer.append(input.next().getCharacter());
				return true;
			} 
		}
		
		lexicalErrorCharacter(c);
		return false;
	}

	private Token scanString(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		appendSubsequentStringCharacters(buffer);

		String lexeme = buffer.toString();
		return StringToken.make(firstChar.getLocation(), lexeme);
	}

	private void appendSubsequentStringCharacters(StringBuffer buffer) {
		LocatedChar c = input.next();

		while (!c.isStringTerminator()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		if (!c.isStringSymbol()) {
			// String is ended by endline which should be an error
			lexicalErrorString(c);
		}
	}

	//////////////////////////////////////////////////////////////////////////////
	// Punctuator lexical analysis
	// old method left in to show a simple scanning method.
	// current method is the algorithm object PunctuatorScanner.java

	@SuppressWarnings("unused")
	private Token oldScanPunctuator(LocatedChar ch) {
		TextLocation location = ch.getLocation();

		switch (ch.getCharacter()) {
		case '*':
			return LextantToken.make(location, "*", Punctuator.MULTIPLY);
		case '+':
			return LextantToken.make(location, "+", Punctuator.ADD);
		case '>':
			return LextantToken.make(location, ">", Punctuator.GREATER);
		case ':':
			if (ch.getCharacter() == '=') {
				return LextantToken.make(location, ":=", Punctuator.ASSIGN);
			} else {
				throw new IllegalArgumentException("found : not followed by = in scanOperator");
			}
		case ',':
			return LextantToken.make(location, ",", Punctuator.SEPARATOR);
		case ';':
			return LextantToken.make(location, ";", Punctuator.TERMINATOR);
		default:
			throw new IllegalArgumentException("bad LocatedChar " + ch + "in scanOperator");
		}
	}

	//////////////////////////////////////////////////////////////////////////////
	// Character-classification routines specific to Pika scanning.

	private boolean isPunctuatorStart(LocatedChar lc) {
		char c = lc.getCharacter();
		return isPunctuatorStartingCharacter(c);
	}

	private boolean isEndOfInput(LocatedChar lc) {
		return lc == LocatedCharStream.FLAG_END_OF_INPUT;
	}

	//////////////////////////////////////////////////////////////////////////////
	// Error-reporting

	private void lexicalError(LocatedChar ch) {
		PikaLogger log = PikaLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: invalid character " + ch);
	}

	private void lexicalErrorIdentifier(LocatedChar ch) {
		PikaLogger log = PikaLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: identifier length exceeded than 32 at invalid character " + ch);
	}
	
	private void lexicalErrorCharacter(LocatedChar ch) {
		PikaLogger log = PikaLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: Initialization of character token contains unexpected character " + ch);
	}

	private void lexicalErrorString(LocatedChar ch) {
		PikaLogger log = PikaLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: String token contains an unexpected character " + ch);
	}
	
	private void lexicalErrorFloating(LocatedChar ch) {
		PikaLogger log = PikaLogger.getLogger("compiler.lexicalAnalyzer");
		log.severe("Lexical error: Floating number contains an unexpected characters at " + ch);
	}

}
