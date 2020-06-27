package semanticAnalyzer.types;

import lexicalAnalyzer.Keyword;
import tokens.Token;

public enum PrimitiveType implements Type {
	BOOLEAN(1, Keyword.BOOL),
	CHARACTER(1, Keyword.CHAR),
	INTEGER(4, Keyword.INT),
	FLOATING(8, Keyword.FLOAT),
	STRING(4, Keyword.STRING),
	RATIONAL(8, Keyword.RAT),
	
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
	@Override
	public int getSize() {
		return sizeInBytes;
	}
	@Override
	public String infoString() {
		return infoString;
	}
	@Override
	public String pikaNativeString() {
		return toString();
	}
	@Override 
	public boolean equivalent(Type type) {
		return this == type;
	}
	@Override
	public Type getConcreteType() {
		return this;
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
	
	private static final Type[] typesInvolvedInPromotions = { CHARACTER, INTEGER, FLOATING, RATIONAL };
	
	public static boolean isATypeInvolvedInPromotions(Type otherType) {
		for(Type type: typesInvolvedInPromotions) {
			if(type.equals(otherType)) {
				return true;
			}
		}
		return false;
	}
}
