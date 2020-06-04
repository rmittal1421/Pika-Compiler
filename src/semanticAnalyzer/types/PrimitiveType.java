package semanticAnalyzer.types;

import lexicalAnalyzer.Keyword;
import tokens.Token;

public enum PrimitiveType implements Type {
	BOOLEAN(1, Keyword.BOOL),
	CHARACTER(1, Keyword.CHAR),
	INTEGER(4, Keyword.INT),
	FLOATING(8, Keyword.FLOAT),
	STRING(4, Keyword.STRING),
	ERROR(0, Keyword.NULL_KEYWORD),		// use as a value when a syntax error has occurred
	NO_TYPE(0, "");						// use as a value when no type has been assigned.
	
	private int sizeInBytes;
	private String infoString;
	private Keyword type;
	
	private PrimitiveType(int size, Keyword type) {
		this.sizeInBytes = size;
		this.infoString = toString();
		this.type = type;
	}
	private PrimitiveType(int size, String infoString) {
		this.sizeInBytes = size;
		this.infoString = infoString;
	}
	public int getSize() {
		return sizeInBytes;
	}
	public String infoString() {
		return infoString;
	}
	public Keyword getType() {
		return this.type;
	}
	
	public static PrimitiveType fromToken(Token token) {
		for(PrimitiveType p: values()) {
			if(token.isLextant(p.type)) {
				return p;
			}
		}
		return ERROR;
	}
}
