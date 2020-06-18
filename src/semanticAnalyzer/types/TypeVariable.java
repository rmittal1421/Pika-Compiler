package semanticAnalyzer.types;

import asmCodeGenerator.codeStorage.ASMOpcode;

public class TypeVariable implements Type {
	private String name; 
	private Type typeConstraint;

	public TypeVariable(String name) {
		this.name = name;
		this.typeConstraint = PrimitiveType.NO_TYPE;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Type getType() {
		return this.typeConstraint;
	}
	
	private void setType(Type typeConstraint) {
		this.typeConstraint = typeConstraint;
	}

	@Override
	public int getSize() {
		return 0;
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
	public Type getConcreteType() {
		return getType().getConcreteType();
	}

	public String toString() {
		return "<" + getName() + ">";
	}
	
	public boolean equivalent(Type otherType) {
		if(otherType instanceof TypeVariable) {
			throw new RuntimeException("equivalent attempted on two types containing type variables");
		}
		if(this.getType() == PrimitiveType.NO_TYPE) {
			setType(otherType);
			return true;
		}
		return this.getType().equivalent(otherType);
	}

	public void reset() {
		setType(PrimitiveType.NO_TYPE);
	}
}
