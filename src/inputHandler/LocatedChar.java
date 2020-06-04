package inputHandler;

/**
 * Value object for holding a character and its location in the input text.
 * Contains delegates to select character operations.
 *
 */
public class LocatedChar {
	Character character;
	TextLocation location;

	public LocatedChar(Character character, TextLocation location) {
		super();
		this.character = character;
		this.location = location;
	}

	//////////////////////////////////////////////////////////////////////////////
	// getters

	public Character getCharacter() {
		return character;
	}

	public TextLocation getLocation() {
		return location;
	}

	public boolean isChar(char c) {
		return character == c;
	}

	//////////////////////////////////////////////////////////////////////////////
	// toString

	public String toString() {
		return "(" + charString() + ", " + location + ")";
	}

	private String charString() {
		if (Character.isWhitespace(character)) {
			int i = character;
			return String.format("'\\%d'", i);
		} else {
			return character.toString();
		}
	}

	//////////////////////////////////////////////////////////////////////////////
	// delegates

	public boolean isLowerCase() {
		return Character.isLowerCase(character) || (character == '_');
	}

	public boolean isDigit() {
		return Character.isDigit(character);
	}

	public boolean isWhitespace() {
		return Character.isWhitespace(character);
	}

	public boolean isCommentToken() {
		return character == '#';
	}

	public boolean isNewLine() {
		return character == '\n';
	}

	public boolean isNumericSign() {
		return character == '+' || character == '-';
	}

	public boolean isPotentialNumStartAndNotDigit() {
		return isNumericSign() || character == '.';
	}
	
	public boolean isNumberStart() {
		return Character.isDigit(character) || isPotentialNumStartAndNotDigit();
	}

	public boolean isDecimalPoint() {
		return character == '.';
	}

	public boolean isCharacterSymbol() {
		return character == '^';
	}

	public boolean isCharacter() {
		return character > 31 && character < 127;
	}

	public boolean isIdentifierStart() {
		return Character.isLowerCase(character) || Character.isUpperCase(character) || (character == '_');
	}

	public boolean isValidIdentifierCharacter() {
		return isIdentifierStart() || isDigit() || (character == '$');
	}
	public boolean isStringSymbol() {
		return character == '"';
	}
	public boolean isStringTerminator() {
		return isStringSymbol() || isNewLine();
	}
}
