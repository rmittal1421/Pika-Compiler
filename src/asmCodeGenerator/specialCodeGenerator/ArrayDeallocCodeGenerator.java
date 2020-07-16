package asmCodeGenerator.specialCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMConstants;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.MemoryManager;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class ArrayDeallocCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		Labeller labeller = new Labeller("dealloc-statement");
		String firstCall = labeller.newLabel("first-call-dealloc");
		String deallocStart = labeller.newLabel("dealloc-start");
		String nullArray = labeller.newLabel("null-array");
		String notNullArray = labeller.newLabel("not-null-array");
		String recursiveDealloc = labeller.newLabel("recursive-dealloc");
		String doNotDeallocate = labeller.newLabel("do-not-deallocate");
		String commonPart = labeller.newLabel("common-part-dealloc");
		String endLabel = labeller.newLabel("end-label-dealloc");
		
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VOID);
		
		// Stack currently is: [... arrayToDeallocBaseAddress]
		frag.add(Jump, firstCall);                                    // [... arr]
		
		frag.add(Call, deallocStart);                                 // [... arr returnPointer]
		
		frag.add(Label, deallocStart);                                // [arr returnPointer]
		frag.add(Exchange);                                           // [returnPointer arr] -> Treat as [returnPointer arr] as [arr]
		
		frag.add(Duplicate);
		frag.add(JumpFalse, nullArray);
		frag.add(Jump, notNullArray);
		
		frag.add(Label, nullArray);
		frag.add(Pop);
		frag.add(PopPC);
		
		frag.add(Label, notNullArray);
		frag.add(Duplicate);                                          // [arr arr]
		Macros.readIOffset(frag, ASMConstants.RECORD_STATUS_OFFSET);        // [arrPointer status]
		Macros.storeITo(frag, RunTime.ARRAY_STATUS_FLAGS);            // [arrPointer]
		frag.add(Duplicate);                                          // [arrPointer arrPointer]
		Macros.readIOffset(frag, ASMConstants.ARRAY_SUBTYPE_SIZE_OFFSET);   // [arrPointer subtypeSize]
		Macros.storeITo(frag, RunTime.ARRAY_SUBTYPE_SIZE);            // [arrPointer]
		frag.add(Duplicate);                                          // [arrPointer arrPointer]
		Macros.readIOffset(frag, ASMConstants.ARRAY_LENGTH_OFFSET);         // [arrPointer length]
		frag.add(Duplicate);                                          // [arrPointer length length]
		Macros.storeITo(frag, RunTime.ARRAY_LENGTH);                  // [arrPointer length]
		frag.add(JumpFalse, endLabel);                                // [arrPointer]
		frag.add(PushI, ASMConstants.ARRAY_HEADER_SIZE);              
		frag.add(Add);                                                // [baseForFirstElement]
		
		frag.add(Label, recursiveDealloc);
		
		// Now check if status said that elements are arrays
		Macros.loadIFrom(frag, RunTime.ARRAY_STATUS_FLAGS);           // [baseForCurrentElement status]
		frag.add(PushI, ASMConstants.STATUS_FLAG_FOR_REFERENCE);
		frag.add(BTAnd);                                              // [baseForCurrentElement 0 or 1]
		
		// If 0 : it is not an array
		frag.add(JumpFalse, doNotDeallocate);                         // [baseForCurrentElement]
		// Else it is an array
		// Now check if both is-deleted-bit and permanent-status bit are not 0
		Macros.loadIFrom(frag, RunTime.ARRAY_STATUS_FLAGS);           // [baseForCurrentElement status]
		frag.add(PushI, ASMConstants.STATUS_FLAG_FOR_DELETE_AND_PERM);      // [baseForCurrentElement 0b0011]
		frag.add(BTOr);                                               // [base 1 or _]
		frag.add(PushI, ASMConstants.BINARY_F);                             // [base 1/_ 1]
		frag.add(Subtract);                                           // [base 0/_]
		// if top is 0, both bits were not 0. Otherwise, perform dealloc
		frag.add(JumpFalse, doNotDeallocate);                         // [base]
		
		// Put all current variables on stack and load new array's base pointer and call start
		frag.add(Duplicate);                                          // [baseForCurrentElement baseForCurrentElement]
		frag.add(LoadI); 
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);          // [baseForCurrentElement]
		Macros.loadIFrom(frag, RunTime.ARRAY_LENGTH);
		Macros.loadIFrom(frag, RunTime.ARRAY_SUBTYPE_SIZE);
		Macros.loadIFrom(frag, RunTime.ARRAY_STATUS_FLAGS);
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);
//		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);
		frag.add(Call, deallocStart);                                 // [OldData newArrayBase returnPointer]
		
		// When I will come here, I have deallocated inner array so old becomes current
		Macros.storeITo(frag, RunTime.ARRAY_STATUS_FLAGS);            // [base length subtypeSize]
		Macros.storeITo(frag, RunTime.ARRAY_SUBTYPE_SIZE);            // [base length]
		Macros.storeITo(frag, RunTime.ARRAY_LENGTH);                  // [base]
		frag.add(Jump, commonPart);
		
		// Label for doNotDeallocate
		frag.add(Label, doNotDeallocate);                             // [base]
		// No operation then
		frag.add(Nop);                                                // [base]
		
		frag.add(Label, commonPart);
		Macros.decrementInteger(frag, RunTime.ARRAY_LENGTH); 
		Macros.loadIFrom(frag, RunTime.ARRAY_LENGTH);            // [baseForCurrentEl length]
		frag.add(JumpFalse, endLabel);                           // [... baseForCurrentEl]
		Macros.loadIFrom(frag, RunTime.ARRAY_SUBTYPE_SIZE);      // [... baseForCurrentEl subtypeSize]
		frag.add(Add);                                           // [... baseFor(Current+1)El]
		frag.add(Jump, recursiveDealloc);
		
		// End label
		frag.add(Label, endLabel);                               // [... baseForCurrentEl]
		frag.add(Pop);
		
		// Now on top, we have the pointer to the array's base which we want to deallocate
		Macros.loadIFrom(frag, RunTime.ARRAY_STATUS_FLAGS);
		frag.add(PushI, ASMConstants.TURN_DELETE_BIT_1);
		frag.add(BTOr);
		Macros.writeIPtrOffset(frag, RunTime.ARRAY_INDEXING_ARRAY, ASMConstants.ARRAY_STATUS_FLAGS_OFFSET);
		
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);
		frag.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);
		frag.add(PopPC);
		
		frag.add(Label, firstCall);
		frag.add(Call, deallocStart);
		
		return frag;
	}
	
	
	
	
	
	
	
	
	
	

}
