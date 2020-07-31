package asmCodeGenerator.specialCodeGenerator;

import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import parseTree.ParseNode;

public class StringLengthCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		Macros.readIOffset(frag, ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		
		return frag;
	}

}