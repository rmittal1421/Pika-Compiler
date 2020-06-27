package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Macros;
import asmCodeGenerator.Record;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class ArrayLengthCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		frag.add(Duplicate);
		
		// Check if the array is null
		frag.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);
		Macros.readIOffset(frag, Record.ARRAY_LENGTH_OFFSET);
		return frag;
	}

}