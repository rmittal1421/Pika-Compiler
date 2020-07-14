package semanticAnalyzer.types;

import java.util.ArrayList;
import java.util.List;

import asmCodeGenerator.codeStorage.ASMOpcode;

public class Lambda implements Type {
	private List<Type> paramTypes;
	private Type returnType;

	public Lambda(List<Type> paramTypes, Type returnType) {
		this.paramTypes = paramTypes;
		this.returnType = returnType;
	}
	
	public List<Type> getParamTypes() {
		return this.paramTypes;
	}
	
	public Type getReturnType() {
		return this.returnType;
	}

	@Override
	public int getSize() {
//		return subtype.getSize();
		return 4;
	}

	@Override
	public String infoString() {
		// TODO: Make this return the actual thing
		return toString();
	}

	@Override
	public String pikaNativeString() {
		return toString();
	}
	
	@Override
	public boolean equivalent(Type otherType) {
		if(otherType instanceof Lambda) {
			Lambda otherLambda = (Lambda)otherType;
			// TODO: Check if equals is calling equivalent or not
			return paramTypes.equals(otherLambda.getParamTypes()) && 
					returnType.equivalent(otherLambda.getReturnType());
		}
		return false;
	}
	
	public boolean equivalentParams(ArrayList<Type> paramTypes) {
		return this.paramTypes.equals(paramTypes);
	}
	
	@Override
	public Type getConcreteType() {
		List<Type> concreteParamTypes = new ArrayList<>();
		for(Type type: paramTypes) {
			concreteParamTypes.add(type.getConcreteType());
		}
		Type concreteReturnType = returnType.getConcreteType();
		return new Lambda(concreteParamTypes, concreteReturnType);
	}

}
