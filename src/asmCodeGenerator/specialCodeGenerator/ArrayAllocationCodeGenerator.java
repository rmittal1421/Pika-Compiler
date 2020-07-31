package asmCodeGenerator.specialCodeGenerator;

import asmCodeGenerator.DynamicRecordCodeGenerator;
import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.Array;

public class ArrayAllocationCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		
		
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		Type subtype = ((Array)node.getType()).getSubtype();
		int statusFlags = subtype instanceof Array ? ASMCodeGenerationConstants.STATUS_FLAG_FOR_REFERENCE : ASMCodeGenerationConstants.STATUS_FLAG_FOR_NON_REFERENCE;
		int subtypeSize = subtype.getSize();
		
		// Stack currently: [nElems]
		
		DynamicRecordCodeGenerator.createEmptyArrayRecord(frag, statusFlags, subtypeSize);
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);
		return frag;
	}

}
