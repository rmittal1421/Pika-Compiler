package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.DynamicRecordCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class StringSubstringCodeGenerator implements SimpleCodeGenerator {
	
	public StringSubstringCodeGenerator() {
	}

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// Stack: [string index1 index2]
		
		// Store the array value and index value
		Macros.storeITo(frag, RunTime.ARRAY_LATER_INDEXING_INDEX);                     // [string index1]
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_INDEX);                           // [string]
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);                           // []
		
		// Check if the index is less than zero
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_INDEX);                          // [ind1]
		frag.add(JumpNeg, RunTime.INDEX_OUT_OF_BOUNDS_RUNTIME_ERROR);
		
		// Check if the index is beyond the array length
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_INDEX);                          // [ind1]
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);                          // [ind1 string]
		Macros.readIOffset(frag, ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);     // [ind1 len]
		frag.add(Subtract);                                                            // [ind1-len]
		
		Labeller labeller = new Labeller("array-indexing");
		String checkOtherIndex = labeller.newLabel("check-second-index-bound");
		String label = labeller.newLabel("in-bound");
		
		// Jump to label if index is fine
		frag.add(JumpNeg, checkOtherIndex);                                            // []
		// If the previous jump did not happen, it means index out of bound
		frag.add(Jump, RunTime.INDEX_OUT_OF_BOUNDS_RUNTIME_ERROR);
		
		// Check other index
		frag.add(Label, checkOtherIndex);                                              // []
		Macros.loadIFrom(frag, RunTime.ARRAY_LATER_INDEXING_INDEX);                    // [ind2]
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);                          // [ind2 string]
		Macros.readIOffset(frag, ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);     // [ind2 len]
		frag.add(Subtract);                                                            // [ind2-len]
		
		// If top of stack is positive, it is a problem
		frag.add(JumpPos, RunTime.INDEX_OUT_OF_BOUNDS_RUNTIME_ERROR);                  // []
		
		// Check if later index is strictly greater than first index
		Macros.loadIFrom(frag, RunTime.ARRAY_LATER_INDEXING_INDEX);                    // [ind2]
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_INDEX);                          // [ind2 ind1]
		frag.add(Subtract);                                                            // [ind2-ind1]
		frag.add(Duplicate);                                                           // [ind2-ind1 ind2-ind1]
		
		frag.add(JumpPos, label);                                                      // [ind2-ind1]
		frag.add(Jump, RunTime.LATER_INDEX_SMALLER_OR_EQUAL_RUNTIME_ERROR);
		
		// Index is fine. Now read the value at given index
		frag.add(Label, label);                                                        // [ind2-ind1] -> [lenToCopy]
		frag.add(Nop);
		
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);                          // [lenToCopy string]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);                // [lenToCopy string headerSize]
		frag.add(PushI, PrimitiveType.CHARACTER.getSize());                            // [lenToCopy string headerSize 1]
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_INDEX);                          // [lenToCopy string headerSize 1 ind1]
		frag.add(Multiply);                                                            // [lenToCopy string headerSize 1*ind1]
		frag.add(Add);                                                                 // [lenToCopy string offset]
		frag.add(Add);                                                                 // [lenToCopy charToCopy]
		
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);                           // [lenToCopy]
		
		frag.add(Duplicate);                                                           // [lenToCopy lenToCopy]
		frag.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE + 1);             // [lenToCopy lenToCopy + 17]
		frag.add(Add);                                                                 // [lenToCopy recordSize]
		
		DynamicRecordCodeGenerator.createRecord(frag,                                  // [lenToCopy] 
												ASMCodeGenerationConstants.STRING_TYPE_ID, 
												ASMCodeGenerationConstants.STATUS_FLAG_FOR_STRING);
		
		frag.add(Duplicate);                                                           // [lenToCopy lenToCopy]
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                     // [lenToCopy lenToCopy newString]
		Macros.writeIOffset(frag, ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);    // [lenToCopy]
		
		frag.add(Duplicate);                                                           // [lenToCopy lenToCopy]
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                     // [lenToCopy lenToCopy newString]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);                // [lenToCopy lenToCopy newString headerSize]
		frag.add(Add);                                                                 // [lenToCopy lenToCopy baseWhereToAdd]
		frag.add(Exchange);                                                            // [lenToCopy baseWhereToAdd lenToCopy]
		
		frag.add(Call, RunTime.CLONE_ARRAY);                                           // [lenToCopy baseWhereToAdd lenToCopy returnPointer]
		
		// When the code returns here, the new string is ready. We need to null terminate it.
		// Stack: [lenToCopy]
		
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                     // [lenToCopy newString]
		frag.add(Exchange);                                                            // [newString lenToCopy]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);                // [newString lenToCopy header]
		frag.add(Add);                                                                 // [newString recordSize]
		frag.add(Add);                                                                 // [endOfString]
		frag.add(PushI, 0);                                                            // [endOfString 0]
		frag.add(StoreC);                                                              // []
		
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                     // [newString] which is null terminated
		
		return frag;
	}

}
