package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class FormRationalCodeGenerator implements SimpleCodeGenerator {
	
	public FormRationalCodeGenerator() {
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		Labeller labeller = new Labeller("random-rn");
		String numeratorGiven0 = labeller.newLabel("numerator-given-zero");
		String ending = labeller.newLabel("end-label-form");
		frag.add(Exchange);
		frag.add(Duplicate);
		frag.add(JumpFalse, numeratorGiven0);
		frag.add(Exchange);
		frag.add(Call, RunTime.LOWEST_TERMS);
		frag.add(Jump, ending);
		frag.add(Label, numeratorGiven0);
		frag.add(Pop);
		frag.add(Pop);
		frag.add(PushI, 0);
		frag.add(PushI, 1);
		frag.add(Label, ending);
		return frag;
	}

}
