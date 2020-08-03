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

public class MapOperatorCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode givenNode) {
		KNaryOperatorNode node = (KNaryOperatorNode) givenNode;
		assert node.getToken().isLextant(Keyword.MAP);
		
		Labeller labeller = new Labeller("map");
		String lambdaAddress = labeller.newLabel("$map-lambda-address");
		String startLoop = labeller.newLabel("loop-start");
		String endLoop = labeller.newLabel("end-loop");
		
		Type arraySubType = ((Array) node.child(0).getType()).getSubtype();
		int arraySubTypeSize = arraySubType.getSize();
		
		Type returnArraySubType = ((Array) node.getType()).getBaseType();
		int returnArraySubTypeSize = returnArraySubType.getSize();
		
		int statusFlags = returnArraySubType instanceof Array 
						  ? ASMCodeGenerationConstants.STATUS_FLAG_FOR_REFERENCE
						  : ASMCodeGenerationConstants.STATUS_FLAG_FOR_NON_REFERENCE;
		
		// Stack: [array lambda]
		ASMCodeFragment code = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// ----------- Save the constants -----------
		Macros.declareI(code, lambdaAddress);
		Macros.storeITo(code, lambdaAddress);
		
		// Check if array given is null
		code.add(Duplicate);
		code.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);
		
		code.add(Duplicate);
		
		Macros.readIOffset(code, ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET);
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);
		
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE);
		code.add(Add);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);
		
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
		code.add(PushI, arraySubTypeSize);
		code.add(Subtract);
		code.add(Duplicate);
		
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);                          // [arr]
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(PushI, arraySubTypeSize);
		code.add(Multiply);
		code.add(Add);                                                                 // [baseForEl]
		code.append(ASMCodeGenerator.opcodeForLoad(arraySubType));                     // [element]
		code.append(ASMCodeGenerator.opcodeForStore(arraySubType));
		Macros.storeITo(code, RunTime.STACK_POINTER);
		// ----------- Parameter placed -----------

		// ----------- Place local variables on stack -----------
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
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
