package asmCodeGenerator.specialCodeGenerator;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;

import static asmCodeGenerator.codeStorage.ASMOpcode.Duplicate;
import static asmCodeGenerator.codeStorage.ASMOpcode.JumpFalse;
import static asmCodeGenerator.codeStorage.ASMOpcode.Pop;
import static asmCodeGenerator.codeStorage.ASMOpcode.Jump;
import static asmCodeGenerator.codeStorage.ASMOpcode.Label;

public class ShortCircuitAndCodeGenerator implements FullCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node, ASMCodeFragment... args) {
		ASMCodeFragment fragment = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		Labeller labeller = new Labeller("SC-and");
		
		final String falseLabel = labeller.newLabel("short-circuit-false");
		final String endLabel = labeller.newLabel("end");
		
		// compute arg1
		fragment.append(args[0]);
		
		// short circuiting test
		fragment.add(Duplicate);
		fragment.add(JumpFalse, falseLabel);
		fragment.add(Pop);
		
		// compute arg2
		fragment.append(args[1]);
		fragment.add(Jump, endLabel);
		
		// the end
		fragment.add(Label, falseLabel);
		fragment.add(Label, endLabel);
		
		return fragment;
	}

}

