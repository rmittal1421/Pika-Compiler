package asmCodeGenerator.specialCodeGenerator;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import lexicalAnalyzer.Keyword;
import parseTree.ParseNode;
import parseTree.nodeTypes.KNaryOperatorNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.NullType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.DynamicRecordCodeGenerator;
import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;

public class FoldOperatorCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode givenNode) {
		KNaryOperatorNode node = (KNaryOperatorNode) givenNode;
		assert node.getToken().isLextant(Keyword.FOLD);
		
		Labeller labeller = new Labeller("fold");
		String lambdaAddress = labeller.newLabel("$fold-lambda-address");
		String startLoop = labeller.newLabel("loop-start");
		String endLoop = labeller.newLabel("end-loop");
		
		Type arg2Type = ((Array) node.child(0).getType()).getSubtype();
		int arg2TypeSize = arg2Type.getSize();

		boolean basePresent = node.nChildren() == 3;
		Type arg1Type = basePresent ? node.child(1).getType() : arg2Type;
		int arg1TypeSize = basePresent ? arg1Type.getSize() : arg2TypeSize;
		
		Type returnType = node.getType();
		int returnTypeSize = returnType.getSize();
		
		// Stack: [base? array lambda]
		ASMCodeFragment code = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// ----------- Save the constants -----------
		Macros.declareI(code, lambdaAddress);
		Macros.storeITo(code, lambdaAddress);
		
		// Check if array given is null
		code.add(Duplicate);                                                          // [base? array array]
		code.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);                        // [base? array]
		
		code.add(Duplicate);                                                          // [base? array array]
		Macros.readIOffset(code, ASMCodeGenerationConstants.ARRAY_LENGTH_OFFSET);     // [base? array length]
		
		code.add(Duplicate);                                                          // [base? array length length]
		code.add(JumpFalse, RunTime.EMPTY_ARRAY_GIVEN_TO_FOLD);                       // [base? array length]
		
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);                                  // [base? array]
		
		code.add(PushI, ASMCodeGenerationConstants.ARRAY_HEADER_SIZE);                // [base? array headerSize]
		code.add(Add);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);                          // [base?]
		
		code.add(PushI, 1);                                                           // [base? 1]
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_INDEX);                          // [base?]
		
		// ----------- Constants saved -----------
		
		// ----------- Start operation -----------
		// Stack : [base?]
		
		if(basePresent) {
			Macros.loadIFrom(code, RunTime.STACK_POINTER);                         // [base SP]
			
			code.add(PushI, arg1TypeSize);                                         // [base SP size1]                                         
			code.add(Subtract);                                                    // [base SP-size1]
			code.add(Duplicate);                                                   // [base newSP]
			Macros.storeITo(code, RunTime.STACK_POINTER);                          // [base newSP]
			flipValueAndAddress(code, arg1Type);                                   // [newSP base]
			code.append(ASMCodeGenerator.opcodeForStore(arg1Type));                // []
			
			Macros.loadIFrom(code, RunTime.STACK_POINTER);                         // [SP]
			code.add(PushI, arg2TypeSize);                                         // [SP size2]
			code.add(Subtract);                                                    // [newSP]
			code.add(Duplicate);                                                   // [newSP newSP]
			Macros.storeITo(code, RunTime.STACK_POINTER);                          // [newSP]
			Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);                  // [newSP baseOfEl]
			code.append(ASMCodeGenerator.opcodeForLoad(arg2Type));                 // [newSP el]
			code.append(ASMCodeGenerator.opcodeForStore(arg2Type));                // []
			
			// ----------- Place local variables on stack -----------
			Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
			Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
			Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
			// ----------- Placed local variables on stack -----------
			
			Macros.loadIFrom(code, lambdaAddress);
			code.add(CallV);
			
			// ----------- Store back local variables from stack -----------
			Macros.storeITo(code, RunTime.ARRAY_LENGTH);
			Macros.storeITo(code, RunTime.ARRAY_INDEXING_INDEX);
			Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);
			// ----------- Stored back local variables from stack -----------
			
			Macros.loadIFrom(code, RunTime.STACK_POINTER);                         // [SP]
			code.add(PushI, returnTypeSize);                                       // [SP returnSize]
			code.add(Subtract);                                                    // [SP-returnSize]
			code.append(ASMCodeGenerator.opcodeForLoad(returnType));               // [baseForNextIteration]
		} else {
			Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
			code.append(ASMCodeGenerator.opcodeForLoad(arg2Type));                 // [firstEl]
		}
		
		// ----------- Start operation -----------
		code.add(Label, startLoop);
		
		// Check if it is termination condition for loop
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);                              // [el arrayLen]
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);                      // [el arrayLen index]
		code.add(Subtract);                                                        // [el arrayLen-index]
		code.add(JumpFalse, endLoop);                                              // [el]
			
		// ----------- Place parameter on stack pointer -----------
		Macros.loadIFrom(code, RunTime.STACK_POINTER);
		
		code.add(PushI, arg1TypeSize);                                         // [base SP size1]                                         
		code.add(Subtract);                                                    // [base SP-size1]
		code.add(Duplicate);                                                   // [base newSP]
		Macros.storeITo(code, RunTime.STACK_POINTER);                          // [base newSP]
		flipValueAndAddress(code, arg1Type);                                   // [newSP base]
		code.append(ASMCodeGenerator.opcodeForStore(arg1Type));                // []
		
		Macros.loadIFrom(code, RunTime.STACK_POINTER);                         // [SP]
		code.add(PushI, arg2TypeSize);                                         // [SP size2]
		code.add(Subtract);                                                    // [newSP]
		code.add(Duplicate);                                                   // [newSP newSP]
		Macros.storeITo(code, RunTime.STACK_POINTER);                          // [newSP]
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);                  // [newSP baseOfEl]
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(PushI, arg2TypeSize);
		code.add(Multiply);
		code.add(Add);
		code.append(ASMCodeGenerator.opcodeForLoad(arg2Type));                 // [newSP el]
		code.append(ASMCodeGenerator.opcodeForStore(arg2Type));                // []
		
		// ----------- Place local variables on stack -----------
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_INDEX);
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		// ----------- Placed local variables on stack -----------
		
		Macros.loadIFrom(code, lambdaAddress);
		code.add(CallV);
		
		// ----------- Store back local variables from stack -----------
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_INDEX);
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);
		// ----------- Stored back local variables from stack -----------
		
		Macros.loadIFrom(code, RunTime.STACK_POINTER);                         // [SP]
		code.add(PushI, returnTypeSize);                                       // [SP returnSize]
		code.add(Subtract);                                                    // [SP-returnSize]
		code.append(ASMCodeGenerator.opcodeForLoad(returnType));               // [baseForNextIteration]
		// ----------- Parameter placed -----------
		
		Macros.incrementInteger(code, RunTime.ARRAY_INDEXING_INDEX);
		code.add(Jump, startLoop);

		code.add(Label, endLoop);
		
		return code;
	}
	
	private static void flipValueAndAddress(ASMCodeFragment frag, Type type) {
		frag.add(Exchange);
		if (type == PrimitiveType.RATIONAL) {
			Macros.storeITo(frag, RunTime.RATIONAL_DEN); // [... numerator address]
			frag.add(Exchange); // [... address numerator]
			Macros.loadIFrom(frag, RunTime.RATIONAL_DEN); // [... address value]
		}
	}
}
