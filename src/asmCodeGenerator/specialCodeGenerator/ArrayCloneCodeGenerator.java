package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.DynamicRecordCodeGenerator;
import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class ArrayCloneCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		// Stack: [... toBeClonedArrayPointer]
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		this.createCloneArrayRecord(frag);
		return frag;
	}
	
	private void createCloneArrayRecord(ASMCodeFragment code) {
		final int typecode = ASMCodeGenerationConstants.ARRAY_TYPE_ID;
		
		code.add(Duplicate);
		code.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);
		
		// Stack: [... toBeClonedArrayPointer]
		code.add(Duplicate);                                        // [... toBeClonedArrayPointer toBeClonedArrayPointer]
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE);                  // [... toBeClonedArrayPointer toBeClonedArrayPointer headerSize]
		code.add(Add);                                              // [... toBeClonedArrayPointer baseAddressOfFirstElementOfOtherArray]
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);        // [... toBeClonedArrayPointer] & save the base of other array
		code.add(Duplicate);                                        // [... toBeClonedArrayPointer toBeClonedArrayPointer]
		Macros.readIOffset(code, ASMCodeGenerationConstants.ARRAY_STATUS_FLAGS_OFFSET); // [... toBeClonedArrayPointer statusFlagOfOtherArray]
		Macros.storeITo(code, RunTime.ARRAY_STATUS_FLAGS);    // [... toBeClonedArrayPointer]
		code.add(Duplicate);                                        // [... toBeClonedArrayPointer toBeClonedArrayPointer]
		Macros.readIOffset(code, ASMCodeGenerationConstants.ARRAY_SUBTYPE_SIZE_OFFSET); // [... toBeClonedArrayPointer subtypeSizeOfToBeClonedArray]
		Macros.storeITo(code, RunTime.ARRAY_SUBTYPE_SIZE);    // [... toBeClonedArrayPointer]
//		code.add(Duplicate);                                        // [... toBeClonedArrayPointer]
		Macros.readIOffset(code, ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET);       // [... lengthOfToBeClonedArray]
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);          // [...]
		Macros.loadIFrom(code, RunTime.ARRAY_SUBTYPE_SIZE);   // [... subtypeSize]
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);         // [... subtypeSize length]
		code.add(Multiply);                                         // [... numBytesToCopy]
		code.add(Duplicate);                                        // [... numBytesToCopy numBytesToCopy]
		Macros.storeITo(code, RunTime.ARRAY_DATASIZE_TEMPORARY);    // [... numBytesToCopy] & stored dataSize
		code.add(Duplicate);                                        // [... numBytesToCopy numBytesToCopy]
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE);                  // [... numBytesToCopy numBytesToCopy headerSize]
		code.add(Add);                                              // [... numBytesToCopy totalRecordSize]
		
		// Need record size on top of stack which is the case
		DynamicRecordCodeGenerator.createRecord(code, typecode, 0);    // This stores a pointer to an array in RECORD_CREATION_TEMPORARY
		
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);  // [... numBytesToCopy thisArrayPointer]
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE);                  // [... numBytesToCopy thisArrayPointer headerSize]
		code.add(Add);                                              // [... numBytesToCopy addressForFirstElement]
		code.add(Exchange);                                         // [... addressForFirstElement numBytesToCopy]
		
		code.add(Call, RunTime.CLONE_ARRAY);                        // [... baseAddressForFirstElement numBytesToCopy returnPointer]
		
		// Fix the header (write correct statusFlags, subTypeSize, Length)
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		Macros.loadIFrom(code, RunTime.ARRAY_SUBTYPE_SIZE);
		Macros.loadIFrom(code, RunTime.ARRAY_STATUS_FLAGS);
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.writeIOffset(code, ASMCodeGenerationConstants.ARRAY_STATUS_FLAGS_OFFSET);
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.writeIOffset(code, ASMCodeGenerationConstants.ARRAY_SUBTYPE_SIZE_OFFSET);
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.writeIOffset(code, ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET);
		
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
	}

}
