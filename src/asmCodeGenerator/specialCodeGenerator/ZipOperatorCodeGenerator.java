package asmCodeGenerator.specialCodeGenerator;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.nodeTypes.KNaryOperatorNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.Type;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.DynamicRecordCodeGenerator;
import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;

public class ZipOperatorCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode givenNode) {
		KNaryOperatorNode node = (KNaryOperatorNode) givenNode;
		assert node.getToken().isLextant(Keyword.ZIP);
		
		Labeller labeller = new Labeller("zip");
		String lambdaAddress = labeller.newLabel("$zip-lambda-address");
		String startLoop = labeller.newLabel("loop-start");
		String endLoop = labeller.newLabel("end-loop");
		String lengthOK = labeller.newLabel("length-ok");
		
		Type array1SubType = ((Array) node.child(0).getType()).getSubtype();
		int array1SubTypeSize = array1SubType.getSize();
		
		Type array2SubType = ((Array) node.child(1).getType()).getSubtype();
		int array2SubTypeSize = array2SubType.getSize();
		
		Type returnArraySubType = ((Array) node.getType()).getSubtype();
		int returnArraySubTypeSize = returnArraySubType.getSize();
		
		int statusFlags = returnArraySubType instanceof Array 
						  ? ASMCodeGenerationConstants.STATUS_FLAG_FOR_REFERENCE
						  : ASMCodeGenerationConstants.STATUS_FLAG_FOR_NON_REFERENCE;
		
		// Stack: [array1 array2 lambda]
		ASMCodeFragment code = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// ----------- Save the constants -----------
		Macros.declareI(code, lambdaAddress);
		Macros.storeITo(code, lambdaAddress);
		
		// Check if array given is null
		// Stack : [arr1 arr2]
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_OTHER_ARRAY);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);
		
		// []
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
		code.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);
		
		// []
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_OTHER_ARRAY);
		code.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);
		
		// []
		Macros.readIPtrOffset(code, RunTime.ARRAY_INDEXING_ARRAY, ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET);
		Macros.readIPtrOffset(code, RunTime.ARRAY_INDEXING_OTHER_ARRAY, ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET);
		code.add(Subtract);
		code.add(JumpFalse, lengthOK);
		code.add(Jump, RunTime.UNEQUAL_LENGTH_ARRAYS_ZIP_OPERATOR_ERROR);
		
		code.add(Label, lengthOK);
		
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
		code.add(Duplicate);
		Macros.readIOffset(code, ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET);
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);
		
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE);
		code.add(Add);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);
		
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_OTHER_ARRAY);
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE);
		code.add(Add);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_OTHER_ARRAY);
		
		code.add(PushI, 0);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_INDEX);
		
		// ----------- Constants saved -----------
		
		// ----------- Create new record -----------
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		DynamicRecordCodeGenerator.createEmptyArrayRecord(code, statusFlags, returnArraySubTypeSize);
		// ----------- Record created -----------
		
		// ----------- Start operation -----------
		code.add(Label, startLoop);
		
		// Check if it is termination condition for loop
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(Subtract);
		code.add(JumpFalse, endLoop);
		
		// ----------- Place parameter on stack pointer -----------
		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		code.add(PushI, array1SubTypeSize);
		code.add(Subtract);
		code.add(Duplicate);
		
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);                          // [arr]
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(PushI, array1SubTypeSize);
		code.add(Multiply);
		code.add(Add);                                                                 // [baseForEl]
		code.append(ASMCodeGenerator.opcodeForLoad(array1SubType));                     // [element]
		code.append(ASMCodeGenerator.opcodeForStore(array1SubType));
		Macros.storeITo(code, RunTime.STACK_POINTER);
		
		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		code.add(PushI, array2SubTypeSize);
		code.add(Subtract);
		code.add(Duplicate);
		
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_OTHER_ARRAY);                    // [arr]
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(PushI, array2SubTypeSize);
		code.add(Multiply);
		code.add(Add);                                                                 // [baseForEl]
		code.append(ASMCodeGenerator.opcodeForLoad(array2SubType));                    // [element]
		code.append(ASMCodeGenerator.opcodeForStore(array2SubType));
		Macros.storeITo(code, RunTime.STACK_POINTER);
		// ----------- Parameter placed -----------

		// ----------- Place local variables on stack -----------
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_OTHER_ARRAY);
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		// ----------- Placed local variables on stack -----------
		
		Macros.loadIFrom(code, lambdaAddress);
		code.add(CallV);
		
		// ----------- Store back local variables from stack -----------
		Macros.storeITo(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_INDEX);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_OTHER_ARRAY);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);
		// ----------- Stored back local variables from stack -----------
		
		// Place address where we want to store returned value on top of stack
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE);
		code.add(Add);
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(PushI, returnArraySubTypeSize);
		code.add(Multiply);
		code.add(Add);
		
		// Fetch returned value
		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		code.add(PushI, returnArraySubTypeSize);
		code.add(Subtract);
		code.append(ASMCodeGenerator.opcodeForLoad(returnArraySubType));
		// After fetching value, place it in the array which is to be returned
		code.append(ASMCodeGenerator.opcodeForStore(returnArraySubType));
		
		Macros.incrementInteger(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(Jump, startLoop);
		
		code.add(Label, endLoop);
		
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		
		return code;
	}

}
