package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Macros;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class RationalSubtractionCodeGenerator implements SimpleCodeGenerator {
	
	public RationalSubtractionCodeGenerator() {
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// Stack is: [num1 den1 num2 den2]
		Macros.storeITo(frag, RunTime.RATIONAL_DEN); // [num1 den1 num2]
		frag.add(Exchange);                          // [num1 num2 den1]
		frag.add(Duplicate);                         // [num1 num2 den1 den1]
		Macros.storeITo(frag, RunTime.RATIONAL_DEN_2_TEMP); // [num1 num2 den1]
		frag.add(Multiply);                          // [num1 num2*den1]
		frag.add(Exchange);                          // [num2*den1 num1]
		Macros.loadIFrom(frag, RunTime.RATIONAL_DEN);// [num2*den1 num1 den2]
		frag.add(Multiply);                          // [num2*den1 num1*den2]
		frag.add(Subtract);                          // [num2*den1-num1*den2]
		frag.add(Negate);                            // [-(num2*den1-num1*den2)]
	    Macros.loadIFrom(frag, RunTime.RATIONAL_DEN_2_TEMP); // [num2*den1-num1*den2 den1]
	    Macros.loadIFrom(frag, RunTime.RATIONAL_DEN);// [num2*den1-num1*den2 den1 den2]
	    frag.add(Multiply);                          // [num2*den1-num1*den2 den1*den2]
	    frag.add(Call, RunTime.LOWEST_TERMS);
	    
		return frag;
	}

}
