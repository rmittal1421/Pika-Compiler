package asmCodeGenerator.specialCodeGenerator;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.nodeTypes.UnaryOperatorNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.DynamicRecordCodeGenerator;
import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;

public class ReverseOperatorCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode givenNode) {
		UnaryOperatorNode node = (UnaryOperatorNode) givenNode;
		assert node.getToken().isLextant(Keyword.REVERSE);
		
		Labeller labeller = new Labeller("reverse");
		String startLoop = labeller.newLabel("loop-start");
		String endLoop = labeller.newLabel("end-loop");
		
		boolean isArray = node.getType() instanceof Array;
		
		Type arraySubType = isArray
							? ((Array) node.getType()).getSubtype()
							: PrimitiveType.CHARACTER;
		int arraySubTypeSize = arraySubType.getSize();
		
		int statusFlags = isArray
						  ? arraySubType instanceof Array 
						  ? ASMCodeGenerationConstants.STATUS_FLAG_FOR_REFERENCE
						  : ASMCodeGenerationConstants.STATUS_FLAG_FOR_NON_REFERENCE
						  : ASMCodeGenerationConstants.STATUS_FLAG_FOR_STRING;
		int lengthOffset = isArray
						   ? ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET
						   : ASMCodeGenerationConstants.STRING_LENGTH_OFFSET;
		int headerOffset = isArray
				   ? ASMCodeGenerationConstants.ARRAY_HEADER_SIZE
				   : ASMCodeGenerationConstants.STRING_HEADER_SIZE;
		
		// Stack: [array]
		ASMCodeFragment code = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// ----------- Save the constants -----------
		// Check if array given is null
		code.add(Duplicate);
		code.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);

		code.add(Duplicate);
		Macros.readIOffset(code, lengthOffset);
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);
		
		code.add(PushI, headerOffset);
		code.add(Add);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);
		
		code.add(PushI, 0);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_INDEX);
		
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		code.add(PushI, 1);
		code.add(Subtract);
		Macros.storeITo(code, RunTime.ARRAY_LATER_INDEXING_INDEX);
		
		// ----------- Constants saved -----------
		
		// ----------- Create new record -----------
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		if(isArray) {			
			DynamicRecordCodeGenerator.createEmptyArrayRecord(code, statusFlags, arraySubTypeSize);
		} else {
			code.add(Duplicate);
			code.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE + 1);
			code.add(Add);
			DynamicRecordCodeGenerator.createRecord(code, ASMCodeGenerationConstants.STRING_TYPE_ID, statusFlags);
			Macros.writeIPtrOffset(code, 
					RunTime.RECORD_CREATION_TEMPORARY, 
					ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		}
		// ----------- Record created -----------
		
		// ----------- Start operation -----------
		code.add(Label, startLoop);
		
		// Check if it is termination condition for loop
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(Subtract);
		code.add(JumpFalse, endLoop);
		
		// ----------- Start copying -----------
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		code.add(PushI, headerOffset);
		code.add(Add);
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(PushI, arraySubTypeSize);
		code.add(Multiply);
		code.add(Add);
		
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
		Macros.loadIFrom(code, RunTime.ARRAY_LATER_INDEXING_INDEX);
		code.add(PushI, arraySubTypeSize);
		code.add(Multiply);
		code.add(Add);
		
		code.append(ASMCodeGenerator.opcodeForLoad(arraySubType));		
		code.append(ASMCodeGenerator.opcodeForStore(arraySubType));
		// ----------- Done copying -----------
		
		// Update indices
		Macros.incrementInteger(code, RunTime.ARRAY_INDEXING_INDEX);
		Macros.decrementInteger(code, RunTime.ARRAY_LATER_INDEXING_INDEX);
		code.add(Jump, startLoop);
		
		code.add(Label, endLoop);
		
		if(!isArray) {
			// Null terminate the string
			Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
			Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
			code.add(PushI, headerOffset);
			code.add(Add);
			code.add(Add);
			code.add(PushI, 0);
			code.add(StoreC);
		}
		
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		
		return code;
	}

}
