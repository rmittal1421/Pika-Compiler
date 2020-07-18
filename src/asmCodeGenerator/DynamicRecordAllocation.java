package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.Add;
import static asmCodeGenerator.codeStorage.ASMOpcode.Call;
import static asmCodeGenerator.codeStorage.ASMOpcode.Duplicate;
import static asmCodeGenerator.codeStorage.ASMOpcode.JumpNeg;
import static asmCodeGenerator.codeStorage.ASMOpcode.Multiply;
import static asmCodeGenerator.codeStorage.ASMOpcode.Pop;
import static asmCodeGenerator.codeStorage.ASMOpcode.PushI;

import java.util.List;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.runtime.MemoryManager;
import asmCodeGenerator.runtime.RunTime;
import semanticAnalyzer.types.Type;

public class DynamicRecordAllocation {

	public static void createRecord(ASMCodeFragment code, int typecode, int statusFlags) {
		code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
		Macros.storeITo(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY,
				ASMCodeGenerationConstants.RECORD_TYPEID_OFFSET, typecode);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY,
				ASMCodeGenerationConstants.RECORD_STATUS_OFFSET, statusFlags);
	}

	// Stack: [... nElems]
	public static void createEmptyArrayRecord(ASMCodeFragment code, int statusFlags, int subtypeSize) {
		final int typecode = ASMCodeGenerationConstants.ARRAY_TYPE_ID;

		// Give error if nElements < 0
		code.add(Duplicate); // [... nElems nElems]
		code.add(JumpNeg, RunTime.NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR); // [... nElems]

		// Compute record size (Includes ASM header + aggregated size of elements)
		code.add(Duplicate); // [... nElems nElems]
		code.add(PushI, subtypeSize); // [... nElems nElems subtypeSize]
		code.add(Multiply); // [... nElems nElems*subtypeSize] === [... nElems elemsSize]
		code.add(Duplicate); // [... nElems elemsSize elemsSize]
		Macros.storeITo(code, RunTime.ARRAY_DATASIZE_TEMPORARY); // [... nElems elemsSize]
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE); // [... nElems elemsSize headerSize]
		code.add(Add); // [... nElems recordSize]

		// Call createRecord
		createRecord(code, typecode, statusFlags); // [... nElems]

		// Zero out elements of array
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY); // [... nElems recordPointer]
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE); // [... nElems recordPointer arrayHeaderSize]
		code.add(Add); // [... nElems baseAddressToAddFirstElement]
		Macros.loadIFrom(code, RunTime.ARRAY_DATASIZE_TEMPORARY); // [... nElems baseAddressToAddFirstElement numBytes]
		code.add(Call, RunTime.CLEAR_N_BYTES);

		// Fill in array header
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY,
				ASMCodeGenerationConstants.ARRAY_SUBTYPE_SIZE_OFFSET, subtypeSize);
		Macros.writeIPtrOffset(code, RunTime.RECORD_CREATION_TEMPORARY, ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET);
	}

	// Stack: [... recordSize]
	public static void createPopulatedArrayRecord(ASMCodeFragment code, int statusFlags, Type subtype,
			List<ASMCodeFragment> codeForChildren) {
		assert codeForChildren.size() > 0;

		int subtypeSize = subtype.getSize();
		final int typecode = ASMCodeGenerationConstants.ARRAY_TYPE_ID;

		createRecord(code, typecode, statusFlags); // [...]
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY); // [... recordPointer]

		// We need to duplicate this since the recordPointer stored in
		// RECORD_CREATION_TEMPORARY might be replaced by one of
		// children of this node as they can be an array too and they will use that
		// runtime variable. Hence, later when we want
		// to put that on stack, we will load the wrong one as it is replaced. So
		// duplicate it, after adding all the children's code
		// storeItBack and then load later.
		code.add(Duplicate);

		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE); // [... recordPointer arrayHeaderSize]
		code.add(Add); // [... baseAddressForFirstElement]

		for (ASMCodeFragment cCode : codeForChildren) { // [... baseAddressforNthElement]
			code.add(Duplicate); // [... baseAddressForNthElement baseAddressForNthElement]
			code.append(cCode); // [... baseAddressForNthElement baseAddressForNthElement cCode]
			code.append(ASMCodeGenerator.opcodeForStore(subtype)); // [... baseAddressForNthElement]
			code.add(PushI, subtypeSize); // [... baseAddressForNthElement subtypeSize]
			code.add(Add); // [... baseAddressFor(N+1)thElement]
		}
		code.add(Pop); // [... baseAddressForFirstElement]

		// Fill in array header
		Macros.storeITo(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY,
				ASMCodeGenerationConstants.ARRAY_SUBTYPE_SIZE_OFFSET, subtypeSize);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY,
				ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET, codeForChildren.size());
	}
}
