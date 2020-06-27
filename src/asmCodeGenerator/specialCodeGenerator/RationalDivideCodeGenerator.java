package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Macros;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class RationalDivideCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);

		// Stack is: [num1 den1 num2 den2]
		Macros.storeITo(frag, RunTime.RATIONAL_DEN);
		frag.add(Multiply);
		frag.add(Exchange);
		Macros.loadIFrom(frag, RunTime.RATIONAL_DEN);
		frag.add(Multiply);
		frag.add(Exchange);
		
		frag.add(Call, RunTime.LOWEST_TERMS);

		return frag;
	}

}
