package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class CastToRationalCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		Type castToType = node.child(0).getType();
		assert castToType.equivalent(PrimitiveType.CHARACTER) || castToType.equivalent(PrimitiveType.INTEGER) || castToType.equivalent(PrimitiveType.FLOATING);
		
		// Stack is : [num den]
		
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		boolean isFloating = castToType.equivalent(PrimitiveType.FLOATING);
		int denominator = isFloating ? 223092870 : 1;
		
		frag.add(PushI, denominator);
		
		if(isFloating) {
			frag.append(new FloatingRationalizeCodeGenerator().generate(node));
		}
		
		return frag;
	}

}
