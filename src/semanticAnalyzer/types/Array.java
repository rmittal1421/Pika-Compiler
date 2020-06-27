package semanticAnalyzer.types;

import asmCodeGenerator.codeStorage.ASMOpcode;

public class Array implements Type {
	private Type subtype;

	public Array(Type subtype) {
		this.subtype = subtype;
	}
	
	public Type getSubtype() {
		return this.subtype;
	}

	@Override
	public int getSize() {
//		return subtype.getSize();
		return 4;
	}

	@Override
	public String infoString() {
		return toString();
	}

	@Override
	public String pikaNativeString() {
		return toString();
	}
	
	@Override
	public boolean equivalent(Type otherType) {
		if(otherType instanceof Array) {
			Array otherArray = (Array)otherType;
			return subtype.equivalent(otherArray.getSubtype());
		}
		return false;
	}
	
	@Override
	public Type getConcreteType() {
		Type concreteSubtype = subtype.getConcreteType();
		return new Array(concreteSubtype);
	}
	
	public Type getBaseType() {
		return subtype instanceof Array ? ((Array)this.getSubtype()).getBaseType() : this.getSubtype();
	}

}
