package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.PushI;
import static asmCodeGenerator.codeStorage.ASMOpcode.BTAnd;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;

public class IntToCharCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment fragment = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// If we take bitwise AND of the value on top of stack with 0x111111111 or 127 (integer), we will get the last
		// 7 bits of the integer which is the character value of that integer.
		
		fragment.add(PushI, 127);
		fragment.add(BTAnd);
		
		return fragment;
	}
	
}
