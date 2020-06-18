package asmCodeGenerator.specialCodeGenerator;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;

import static asmCodeGenerator.codeStorage.ASMOpcode.Duplicate;
import static asmCodeGenerator.codeStorage.ASMOpcode.JumpTrue;
import static asmCodeGenerator.codeStorage.ASMOpcode.Pop;
import static asmCodeGenerator.codeStorage.ASMOpcode.Jump;
import static asmCodeGenerator.codeStorage.ASMOpcode.Label;

public class ShortCircuitOrCodeGenerator implements FullCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node, ASMCodeFragment... args) {
		ASMCodeFragment fragment = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		Labeller labeller = new Labeller("SC-or");
		
		final String trueLabel = labeller.newLabel("short-circuit-true");
		final String endLabel = labeller.newLabel("end");
		
		// compute arg1
		fragment.append(args[0]);
		
		// short circuiting test
		fragment.add(Duplicate);
		fragment.add(JumpTrue, trueLabel);
		fragment.add(Pop);
		
		// compute arg2
		fragment.append(args[1]);
		fragment.add(Jump, endLabel);
		
		// the end
		fragment.add(Label, trueLabel);
		fragment.add(Label, endLabel);
		
		return fragment;
	}

}
