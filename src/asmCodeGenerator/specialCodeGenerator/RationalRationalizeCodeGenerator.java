package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Macros;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class RationalRationalizeCodeGenerator implements SimpleCodeGenerator {
	
	public RationalRationalizeCodeGenerator() {
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);

		// Stack is: [... num den]
		frag.add(Duplicate); // [... num den den]
		frag.add(JumpFalse, RunTime.RATIONAL_NUMBER_GIVEN_ZERO_DENOMINATOR); // [... num den]

		Macros.storeITo(frag, RunTime.EXPRESS_OVER_DEN);
		Macros.storeITo(frag, RunTime.RATIONAL_DEN);
		Macros.storeITo(frag, RunTime.RATIONAL_NUM);

		Macros.loadIFrom(frag, RunTime.RATIONAL_NUM);
		Macros.loadIFrom(frag, RunTime.EXPRESS_OVER_DEN);
		frag.add(Multiply);

		Macros.loadIFrom(frag, RunTime.RATIONAL_DEN);
		frag.add(Divide);
		
		Macros.loadIFrom(frag, RunTime.EXPRESS_OVER_DEN);
		frag.add(Call, RunTime.LOWEST_TERMS);
		
		return frag;
	}

}
