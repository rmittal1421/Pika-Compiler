package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class StringIndexingCodeGenerator implements SimpleCodeGenerator {
	
	public StringIndexingCodeGenerator() {
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_ADDRESS);
		
		// Store the array value and index value
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_INDEX);
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);
		
		// Check if the index is less than zero
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_INDEX);
		frag.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_RUNTIME_ERROR);
		
		// Check if the index is beyond the array length
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_INDEX);
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);
		Macros.readIOffset(frag, ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		frag.add(Subtract);
		
		Labeller labeller = new Labeller("array-indexing");
		String label = labeller.newLabel("in-bound");
		
		// Jump to label if index is fine
		frag.add(JumpNeg, label);
		// If the previous jump did not happen, it means index out of bound
		frag.add(Jump, RunTime.INDEX_OUT_OF_BOUNDS_RUNTIME_ERROR);
		
		// Label if everything is fine
		frag.add(Label, label);
		frag.add(Nop);
		
		// Index is fine. Now read the value at given index
		
		// Base address where the first element is sitting
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);
		frag.add(Add);
		
		// Calculate how many bytes we need to move right form the just added address to get to the indexed element
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_INDEX);
		frag.add(PushI, PrimitiveType.CHARACTER.getSize());
		frag.add(Multiply);
		
		// Add the bytes to be moved right to the base address of the first element
		frag.add(Add);
		
		return frag;
	}

}
