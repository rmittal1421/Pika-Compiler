package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Macros;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class FloatingRationalizeCodeGenerator implements SimpleCodeGenerator {

	public FloatingRationalizeCodeGenerator() {
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);

		frag.add(Duplicate);
		frag.add(JumpFalse, RunTime.RATIONAL_NUMBER_GIVEN_ZERO_DENOMINATOR);

		frag.add(Duplicate);
		Macros.storeITo(frag, RunTime.EXPRESS_OVER_DEN);
		
		frag.add(ConvertF);
		frag.add(FMultiply);
		frag.add(ConvertI);
		
		Macros.loadIFrom(frag, RunTime.EXPRESS_OVER_DEN);
		frag.add(Call, RunTime.LOWEST_TERMS);

		return frag;
	}

}
