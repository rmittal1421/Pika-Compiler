package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class CastFromRationalCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		Type castToType = node.child(1).getType();
		assert castToType.equivalent(PrimitiveType.INTEGER) || castToType.equivalent(PrimitiveType.FLOATING);
		
		// Stack is : [num den]
		
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		boolean isFloating = castToType.equivalent(PrimitiveType.FLOATING);
		
		if(isFloating) {
			frag.add(ConvertF);
			frag.add(Exchange);
			frag.add(ConvertF);
			frag.add(Exchange);
		}
		
		frag.add(isFloating ? FDivide : Divide);
		
		return frag;
	}

}
