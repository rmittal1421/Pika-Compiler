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
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VOID);
		
		Labeller labeller = new Labeller("dealloc-statement");
		String firstCall = labeller.newLabel("base-dealloc-call");
		String deallocStart = labeller.newLabel("dealloc-start");
		String deallocLoopBegin = labeller.newLabel("dealloc-loop-begin");
		String deallocLoopEnd = labeller.newLabel("dealloc-loop-end");
		String nullArray = labeller.newLabel("null-array");
		String notNullArray = labeller.newLabel("not-null-array");
		String doNotDeAllocAndClearUp = labeller.newLabel("dealloc-cancel-clean-up");
		String deallocateMe = labeller.newLabel("deallocate-current-array");
		String coreEnd = labeller.newLabel("core-end");

		// FUTURE TODO: For M4, check if type id is 7 while deallocating (However, it should be taken care of by semantic checker)
		
		frag.add(Jump, firstCall);                                              // [arr]
		frag.add(Label, deallocStart);                                          // [arr retP]
		frag.add(Exchange);                                                     // [retP arr]	
		frag.add(Duplicate);                                                    // [retP arr arr]
		frag.add(JumpFalse, nullArray);                                         // [retP arr]
		frag.add(Jump, notNullArray);                                           // [retP arr]
		
		// If a null array is handled to dealloc, simple don't do anything and return
		frag.add(Label, nullArray);                                             // [retP arr]
		frag.add(Pop);                                                          // [retP]
		frag.add(PopPC);                                                        // [] & returns to the end
		
		// If array is not null, check if we need to delete it
		frag.add(Label, notNullArray);                                          // [retP arr]
		frag.add(Duplicate);                                                    // [retP arr arr]
		
		Macros.readIOffset(frag, ASMConstants.ARRAY_STATUS_FLAGS_OFFSET);       // [retP arr status]
		frag.add(PushI, ASMConstants.STATUS_TO_CHECK_DELETE_OR_PERM);           // [retP arr status checkDelOrPerm]
		frag.add(BTAnd);		                                                // [retP arr 0||!0]
		frag.add(JumpTrue, doNotDeAllocAndClearUp);                              // [retP arr]

		frag.add(Duplicate);                                                    // [retP arr arr]
		frag.add(Duplicate);                                                    // [retP arr arr arr]
		Macros.readIOffset(frag, ASMConstants.ARRAY_STATUS_FLAGS_OFFSET);       // [retP arr arr status]		
		frag.add(PushI, ASMConstants.TURN_DELETE_BIT_1);                        // [retP arr arr status deleteBitChange]
		frag.add(BTOr);		                                                    // [retP arr arr updatedStatus]
		frag.add(Exchange);                                                     // [retP arr updatedStatus arr]
		Macros.writeIOffset(frag, ASMConstants.ARRAY_STATUS_FLAGS_OFFSET);      // [retP arr]

		frag.add(Duplicate);	                                                // [retP arr arr]
		Macros.readIOffset(frag, ASMConstants.ARRAY_STATUS_FLAGS_OFFSET);       // [retP arr status]
		frag.add(PushI, ASMConstants.STATUS_FLAG_FOR_REFERENCE);                // [retP arr subTypeRef]
		frag.add(BTAnd);                                                        // [retP arr 0||!0] -> if 1: array subtype, other not
		
		// If top of stack is 0: I do not need to recurse. I just dealloc this array
		frag.add(JumpFalse, deallocateMe);                                      // [retP arr]

		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);                    // [retP]
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);                   // [retP arr]
		Macros.readIPtrOffset(frag, RunTime.ARRAY_INDEXING_ARRAY, ASMConstants.ARRAY_LENGTH_OFFSET);        // [retP arr len]
		Macros.readIPtrOffset(frag, RunTime.ARRAY_INDEXING_ARRAY, ASMConstants.ARRAY_SUBTYPE_SIZE_OFFSET);  // [retP arr len subTypeSize] 
		Macros.storeITo(frag, RunTime.ARRAY_SUBTYPE_SIZE);                      // [retP arr len]

		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);                   // [retP arr len arr]
		frag.add(PushI, ASMConstants.ARRAY_HEADER_SIZE);                        // [retP arr len arr headerSize]
		frag.add(Add);                                                          // [retP arr len baseForFirstElement]

		// ------------ Start of loop --------------
		// We can start recursing now
		frag.add(Label, deallocLoopBegin);
		frag.add(Exchange);		                                                // [retP arr baseForFirstElement len]
		frag.add(Duplicate);                                                    // [retP arr baseForFirstElement len len]
		// Check condition of while loop
		frag.add(JumpFalse, deallocLoopEnd);                                           // [retP arr baseForFirstElement len]
		// If not yet 0, decrement length
		frag.add(PushI, 1);                                                     // [retP arr baseForFirstElement len 1]
		frag.add(Subtract);		                                                // [retP arr baseForFirstElement len-1] --> Decreemented length
		frag.add(Exchange);                                                     // [retP arr len baseForFirstElement]
		frag.add(Duplicate);                                                    // [retP arr len baseForFirstElement baseForFirstElement]
		Macros.loadIFrom(frag, RunTime.ARRAY_SUBTYPE_SIZE);                     // [retP arr len baseForFirstElement baseForFirstElement subTypeSize]
		frag.add(Exchange);                                                     // [retP arr len baseForFirstElement subTypeSize baseForFirstElement]
		frag.add(LoadI);                                                        // [retP oldData firstElementRecord]
		frag.add(Call, deallocStart);		                                    // [retP oldData firstElementRecord otherRetP]
		
		// When it comes here: Stack : [retP arr len baseForFirstElement subTypeSize]
		frag.add(Duplicate);                                                    // [retP arr len baseForFirstElement subTypeSize subTypeSize]
		Macros.storeITo(frag, RunTime.ARRAY_SUBTYPE_SIZE);                      // [retP arr len baseForFirstElement subTypeSize]
		frag.add(Add);                                                          // [retP arr len baseForNextElement]
		frag.add(Jump, deallocLoopBegin);                                       // [retP arr len baseForNextElement]

		frag.add(Label, deallocLoopEnd);                                        // [retP arr len baseForNextElement]
		frag.add(Pop);
		frag.add(Pop);                                                          // [retP arr]

		frag.add(Label, deallocateMe);                            
		frag.add(Call, MemoryManager.MEM_MANAGER_DEALLOCATE);                   // [retP]
		frag.add(Jump, coreEnd);                                                // [retP]

		frag.add(Label, doNotDeAllocAndClearUp);	                            // [ret arr]
		frag.add(Pop);                                                          // [ret]
		
		// ------------ End Label ------------
		frag.add(Label, coreEnd);                                               // [retP]
		frag.add(Return);                                                       // []
		
		frag.add(Label, firstCall);                                             // [arr]
		frag.add(Call, deallocStart);                                           // [arr retP]
		
		return frag;
	}
	
	
	
	
	
	
	
	
	
	

}
