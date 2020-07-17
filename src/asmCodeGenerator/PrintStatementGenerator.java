package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import parseTree.ParseNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.TabNode;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.Lambda;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import asmCodeGenerator.ASMCodeGenerator.CodeVisitor;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.runtime.RunTime;

public class PrintStatementGenerator {
	ASMCodeFragment code;
	ASMCodeGenerator.CodeVisitor visitor;
	
	
	public PrintStatementGenerator(ASMCodeFragment code, CodeVisitor visitor) {
		super();
		this.code = code;
		this.visitor = visitor;
	}

	public void generate(PrintStatementNode node) {
		for(ParseNode child : node.getChildren()) {
			if(child instanceof NewlineNode || child instanceof SpaceNode || child instanceof TabNode) {
				ASMCodeFragment childCode = visitor.removeVoidCode(child);
				code.append(childCode);
			} else if(child.getType() instanceof Array) {
				appendPrintCodeForArray(child);
			} else if(child.getType() == PrimitiveType.RATIONAL) {
				code.append(visitor.removeValueCode(child));      // [... num den]
				appendPrintCodeForRationalNumbers(child);
			} else if(child.getType() instanceof Lambda) {
				appendPrintCodeForLambda(child);
			} else {
				appendPrintCode(child);
			}
		}
	}
	
	private void appendPrintCodeForArray(ParseNode node) {
		assert node.getType() instanceof Array;
		Type concreteType = ((Array)node.getType()).getBaseType();
		
		Labeller labeller = new Labeller("print-array");
		String firstCall = labeller.newLabel("first-call");
		String printStart = labeller.newLabel("print-start");
		String recursivePrint = labeller.newLabel("recursive-print");
		String notAnArray = labeller.newLabel("not-an-array");
		String commonPart = labeller.newLabel("common-part");
		String endLabel = labeller.newLabel("end-label");
		
		code.append(visitor.removeValueCode(node));

		code.add(Jump, firstCall);
		
		// Stack has : [arrayPointer returnPointer]
		code.add(Label, printStart);
		code.add(Exchange);                                      // [returnPointer arrPointer] -> Will treat as [arrPointer] until endLabel
		
		code.add(Duplicate);
		code.add(JumpFalse, RunTime.NULL_ARRAY_RUNTIME_ERROR);
		
		code.add(Duplicate);                                     // [arrPointer arrPointer]
		code.add(PushD, RunTime.OPEN_BRACKET_SIGN_STRING);
		code.add(Printf);
		Macros.readIOffset(code, ASMConstants.RECORD_STATUS_OFFSET);   // [arrPointer status]
		Macros.storeITo(code, RunTime.ARRAY_STATUS_FLAGS);       // [arrPointer]
		code.add(Duplicate);                                     // [arrPointer arrPointer]
		Macros.readIOffset(code, ASMConstants.ARRAY_SUBTYPE_SIZE_OFFSET);// [arrPointer subtypeSize]
		Macros.storeITo(code, RunTime.ARRAY_SUBTYPE_SIZE);       // [arrPointer]
		code.add(Duplicate);                                     // [arrPointer arrPointer]
		Macros.readIOffset(code, ASMConstants.ARRAY_LENGTH_OFFSET);    // [arrPointer length]
		code.add(Duplicate);                                     // [arrPointer length length]
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);             // [arrPointer length]
		code.add(JumpFalse, endLabel);                           // [arrPointer]
		code.add(PushI, ASMConstants.ARRAY_HEADER_SIZE);              
		code.add(Add);                                           // [baseForFirstElement]
		
		code.add(Label, recursivePrint);
		
		// Now check if status said that elements are arrays
		Macros.loadIFrom(code, RunTime.ARRAY_STATUS_FLAGS);      // [baseForCurrentElement status]
		code.add(PushI, ASMConstants.STATUS_FLAG_FOR_REFERENCE);
		code.add(BTAnd);                                         // [baseForCurrentElement 0 or 1]
		
		// If 0 : it is not an array
		code.add(JumpFalse, notAnArray);                         // [baseForCurrentElement]
		// Else it is an array
		// Put all current variables on stack and load new array's base pointer and call start
		code.add(Duplicate);                                     // [baseForCurrentElement baseForCurrentElement]
		code.add(LoadI); 
		Macros.storeITo(code, RunTime.ARRAY_INDEXING_ARRAY);     // [baseForCurrentElement]
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);
		Macros.loadIFrom(code, RunTime.ARRAY_SUBTYPE_SIZE);
		Macros.loadIFrom(code, RunTime.ARRAY_STATUS_FLAGS);
		Macros.loadIFrom(code, RunTime.ARRAY_INDEXING_ARRAY);
		code.add(Call, printStart);                              // [OldData newArrayBase returnPointer]
		
		// When I will come here, I have successfully printed inner array so old becomes current
		Macros.storeITo(code, RunTime.ARRAY_STATUS_FLAGS);       // [base length subtypeSize]
		Macros.storeITo(code, RunTime.ARRAY_SUBTYPE_SIZE);       // [base length]
		Macros.storeITo(code, RunTime.ARRAY_LENGTH);             // [base]
		code.add(Jump, commonPart);
		
		code.add(Label, notAnArray);
		code.add(Duplicate);                                     // [baseForCurrentEl baseForCurrentEl]
		
		code.append(ASMCodeGenerator.opcodeForLoad(concreteType));// [baseForCurrentEl actualCurrentElement]
		
		if(concreteType.equivalent(PrimitiveType.RATIONAL)) {
			appendPrintCodeForRationalNumbers(node);
		} else if(concreteType instanceof Lambda) {
			code.add(Pop);
			code.add(PushD, RunTime.LAMBDA_PRINT_STRING);
			code.add(Printf);
		} else {
			if(concreteType.equivalent(PrimitiveType.BOOLEAN)) {
				String trueLabel = labeller.newLabel("true");
				String boolEndLabel = labeller.newLabel("join");

				code.add(JumpTrue, trueLabel);
				code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
				code.add(Jump, boolEndLabel);
				code.add(Label, trueLabel);
				code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
				code.add(Label, boolEndLabel);

			}
			code.add(PushD, printFormat(concreteType));
			code.add(Printf);                                        // [... baseForCurrentEl]
		}
		
		code.add(Label, commonPart);
		Macros.decrementInteger(code, RunTime.ARRAY_LENGTH); 
		Macros.loadIFrom(code, RunTime.ARRAY_LENGTH);            // [baseForCurrentEl length]
		code.add(JumpFalse, endLabel);                           // [... baseForCurrentEl]
		code.add(PushD, RunTime.ARRAY_SEPARATOR_SIGN_STRING);
		code.add(Printf);
		Macros.loadIFrom(code, RunTime.ARRAY_SUBTYPE_SIZE);      // [... baseForCurrentEl subtypeSize]
		code.add(Add);                                           // [... baseFor(Current+1)El]
		code.add(Jump, recursivePrint);
		
		// End label
		code.add(Label, endLabel);                               // [... baseForCurrentEl]
		code.add(PushD, RunTime.CLOSE_BRACKET_SIGN_STRING);
		code.add(Printf);
		code.add(Pop);
		code.add(PopPC);
		
		code.add(Label, firstCall);
		code.add(Call, printStart);
	}

	private void appendPrintCode(ParseNode node) {
		String format = printFormat(node.getType());

		code.append(visitor.removeValueCode(node));
		convertToStringIfBoolean(node);
		code.add(PushD, format);
		code.add(Printf);
	}
	
	private void convertToStringIfBoolean(ParseNode node) {
		if(node.getType() != PrimitiveType.BOOLEAN) {
			return;
		}
		
		Labeller labeller = new Labeller("print-boolean");
		String trueLabel = labeller.newLabel("true");
		String endLabel = labeller.newLabel("join");

		code.add(JumpTrue, trueLabel);
		code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
		code.add(Jump, endLabel);
		code.add(Label, trueLabel);
		code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
		code.add(Label, endLabel);
	}
	
	private void appendPrintCodeForLambda(ParseNode node) {
		if(!(node.getType() instanceof Lambda)) {
			return;
		}
		
		code.append(visitor.removeValueCode(node));
		code.add(Pop);
		code.add(PushD, RunTime.LAMBDA_PRINT_STRING);
		code.add(Printf);
	}
	
	private void appendPrintCodeForRationalNumbers(ParseNode node) {
		String intPrintFormat = printFormat(PrimitiveType.INTEGER);
		
		Labeller labeller = new Labeller("print-rational-numbers");
		
		String numIsZero = labeller.newLabel("num-is-zero");
		String handleSign = labeller.newLabel("handle-sign");
		String atleastOneNegative = labeller.newLabel("atleast-one-neg");
		String printNegative = labeller.newLabel("print-negative");
		String signHandled = labeller.newLabel("sign-handled");
		String intHandled = labeller.newLabel("int-handled");
		String printFraction = labeller.newLabel("print-fraction");
		String endLabel = labeller.newLabel("end-label");

		// Stack is [... num den]
		Macros.storeITo(code, RunTime.RATIONAL_DEN);     // [... num]
		code.add(Duplicate);                             // [... num num]
		Macros.storeITo(code, RunTime.RATIONAL_NUM);     // [... num]
		
		// If num is zero, just print 0
		code.add(Duplicate);                             // [... num num]
		code.add(JumpFalse, numIsZero);                  // [... num]
		
		code.add(Duplicate);                             // [... num num]
		code.add(JumpPos, signHandled);                  // [... num]
		code.add(PushD, RunTime.NEGATIVE_SIGN_STRING);   
		code.add(Printf);                                // [...]
		code.add(Jump, signHandled);
		
		code.add(Label, numIsZero);                      // [... 0]
		code.add(PushD, intPrintFormat);
		code.add(Printf);                                // [...]
		code.add(Jump, endLabel);                        // Jumps to endLabel
		
		code.add(Label, signHandled);                    // [...]       
		Macros.loadIFrom(code, RunTime.RATIONAL_NUM);    // [... num]
		Macros.loadIFrom(code, RunTime.RATIONAL_DEN);    // [... num den]
		code.add(Divide);                                // [... num/den]
		code.add(Duplicate);                             // [... num/den num/den]
		
		// If num/den is zero, I don't need to print it. Otherwise print it
		code.add(JumpFalse, intHandled);                 // [... num/den]
		code.add(Duplicate);                             // [... num/den num/den]
		code.add(PushD, intPrintFormat);
		code.add(Printf);
		code.add(Jump, intHandled);                      // [... num/den]
		
		code.add(Label, intHandled);                     // [... num/den]
		code.add(Pop);
		Macros.loadIFrom(code, RunTime.RATIONAL_NUM);    // [... num]
		Macros.loadIFrom(code, RunTime.RATIONAL_DEN);    // [... num den]
		code.add(Remainder);                             // [... num%den]
		code.add(Duplicate);                             // [... num%den num%den]
		code.add(JumpTrue, printFraction);               // [... num%den]
		code.add(JumpFalse, endLabel);                   // [...]
		
		code.add(Label, printFraction);                  // [... num%den]
		Macros.storeITo(code, RunTime.RATIONAL_NUM);     // [...]
		// Print _
		code.add(PushD, RunTime.UNDERSCORE_SIGN_STRING);
		code.add(Printf);
		
		// Print num (Make it positive before hand even if it was not negative)
		Macros.loadMakePositiveStore(code, RunTime.RATIONAL_NUM);
		Macros.loadIFrom(code, RunTime.RATIONAL_NUM);    // [... num]
		code.add(PushD, intPrintFormat);
		code.add(Printf);                                // [...]
		
		// Print /
		code.add(PushD, RunTime.FORWARD_SLASH_SIGN_STRING);
		code.add(Printf);
		
		// Print den
		Macros.loadIFrom(code, RunTime.RATIONAL_DEN);    // [... den]
		code.add(PushD, intPrintFormat);
		code.add(Printf);                                // [...]
		
		code.add(Label, endLabel);
	}


	private static String printFormat(Type type) {
		assert type instanceof PrimitiveType;
		
		switch((PrimitiveType)type) {
		case INTEGER:	return RunTime.INTEGER_PRINT_FORMAT;
		case FLOATING:  return RunTime.FLOATING_PRINT_FORMAT;
		case BOOLEAN:	return RunTime.BOOLEAN_PRINT_FORMAT;
		case CHARACTER: return RunTime.CHARACTER_PRINT_FORMAT;
		case STRING:	return RunTime.STRING_PRINT_FORMAT;
		default:		
			assert false : "Type " + type + " unimplemented in PrintStatementGenerator.printFormat()";
			return "";
		}
	}
}
