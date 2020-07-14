package semanticAnalyzer.types;

import lexicalAnalyzer.Keyword;

public class NullType implements Type {
	private String infoString = (Keyword.NULL).toString();
	private Keyword type = Keyword.NULL;

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public String infoString() {
		return this.infoString;
	}

	@Override
	public String pikaNativeString() {
		return this.infoString;
	}

	@Override
	public boolean equivalent(Type type) {
		return type instanceof NullType;
	}

	@Override
	public Type getConcreteType() {
		return this;
	}
	public Keyword getType() {
		return this.type;
	}

}
