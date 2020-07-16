package asmCodeGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.MemoryManager;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.specialCodeGenerator.FullCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.SimpleCodeGenerator;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.*;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.Lambda;
import semanticAnalyzer.types.NullType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	ParseNode root;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}

	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
	}

	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

		// Code which should be in the pathway before actual application code
		code.append(MemoryManager.codeForInitialization());

		code.append(RunTime.getEnvironment());
		code.append(globalVariableBlockASM());
		code.append(programASM());

		// Memory heap manger goes after the main program where main program is the
		// programASM
		code.append(MemoryManager.codeForAfterApplication());

		return code;
	}

	private ASMCodeFragment globalVariableBlockASM() {
		assert root.hasScope();
		Scope scope = root.getScope();
		int globalBlockSize = scope.getAllocatedSize();

		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
		code.add(DataZ, globalBlockSize);
		return code;
	}

	private ASMCodeFragment programASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

		code.add(Label, RunTime.MAIN_PROGRAM_LABEL);
		code.append(programCode());
		code.add(Halt);

		return code;
	}

	private ASMCodeFragment programCode() {
		CodeVisitor visitor = new CodeVisitor();
		root.accept(visitor);
		return visitor.removeRootCode(root);
	}

	public static void createRecord(ASMCodeFragment code, int typecode, int statusFlags) {
		code.add(Call, MemoryManager.MEM_MANAGER_ALLOCATE);
		Macros.storeITo(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, ASMConstants.RECORD_TYPEID_OFFSET, typecode);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, ASMConstants.RECORD_STATUS_OFFSET, statusFlags);
	}

	// Stack: [... nElems]
	public static void createEmptyArrayRecord(ASMCodeFragment code, int statusFlags, int subtypeSize) {
		final int typecode = ASMConstants.ARRAY_TYPE_ID;

		// Give error if nElements < 0
		code.add(Duplicate); // [... nElems nElems]
		code.add(JumpNeg, RunTime.NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR); // [... nElems]

		// Compute record size (Includes ASM header + aggregated size of elements)
		code.add(Duplicate); // [... nElems nElems]
		code.add(PushI, subtypeSize); // [... nElems nElems subtypeSize]
		code.add(Multiply); // [... nElems nElems*subtypeSize] === [... nElems elemsSize]
		code.add(Duplicate); // [... nElems elemsSize elemsSize]
		Macros.storeITo(code, RunTime.ARRAY_DATASIZE_TEMPORARY); // [... nElems elemsSize]
		code.add(PushI, ASMConstants.ARRAY_HEADER_SIZE); // [... nElems elemsSize headerSize]
		code.add(Add); // [... nElems recordSize]

		// Call createRecord
		createRecord(code, typecode, statusFlags); // [... nElems]

		// Zero out elements of array
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY); // [... nElems recordPointer]
		code.add(PushI, ASMConstants.ARRAY_HEADER_SIZE); // [... nElems recordPointer arrayHeaderSize]
		code.add(Add); // [... nElems baseAddressToAddFirstElement]
		Macros.loadIFrom(code, RunTime.ARRAY_DATASIZE_TEMPORARY); // [... nElems baseAddressToAddFirstElement numBytes]
		code.add(Call, RunTime.CLEAR_N_BYTES);

		// Fill in array header
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, ASMConstants.ARRAY_SUBTYPE_SIZE_OFFSET,
				subtypeSize);
		Macros.writeIPtrOffset(code, RunTime.RECORD_CREATION_TEMPORARY, ASMConstants.ARRAY_LENGTH_OFFSET);
	}

	// Stack: [... recordSize]
	public static void createPopulatedArrayRecord(ASMCodeFragment code, int statusFlags, Type subtype,
			List<ASMCodeFragment> codeForChildren) {
		assert codeForChildren.size() > 0;

		int subtypeSize = subtype.getSize();
		final int typecode = ASMConstants.ARRAY_TYPE_ID;

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

		code.add(PushI, ASMConstants.ARRAY_HEADER_SIZE); // [... recordPointer arrayHeaderSize]
		code.add(Add); // [... baseAddressForFirstElement]

//		ASMCodeFragment opcodeForStore = opcodeForStore(subtype);

		for (ASMCodeFragment cCode : codeForChildren) { // [... baseAddressforNthElement]
			code.add(Duplicate); // [... baseAddressForNthElement baseAddressForNthElement]
			code.append(cCode); // [... baseAddressForNthElement baseAddressForNthElement cCode]
			code.append(opcodeForStore(subtype)); // [... baseAddressForNthElement]
//			code.add(StoreI);
			code.add(PushI, subtypeSize); // [... baseAddressForNthElement subtypeSize]
			code.add(Add); // [... baseAddressFor(N+1)thElement]
		}
		code.add(Pop); // [... baseAddressForFirstElement]

		// Fill in array header
		Macros.storeITo(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, ASMConstants.ARRAY_SUBTYPE_SIZE_OFFSET,
				subtypeSize);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, ASMConstants.ARRAY_LENGTH_OFFSET,
				codeForChildren.size());
	}

	// Spits out fragment for storing a value
	public static ASMCodeFragment opcodeForStore(Type type) {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		if (type == PrimitiveType.INTEGER || type == PrimitiveType.STRING || type instanceof Array || type instanceof Lambda) {
			frag.add(StoreI);
		} else if (type == PrimitiveType.FLOATING) {
			frag.add(StoreF);
		} else if (type == PrimitiveType.BOOLEAN || type == PrimitiveType.CHARACTER) {
			frag.add(StoreC);
		} else if (type == PrimitiveType.RATIONAL) {
			// Stack is: [.. pointerToStoreIn num den]
			Macros.storeITo(frag, RunTime.RATIONAL_DEN);
			Macros.storeITo(frag, RunTime.RATIONAL_NUM);
			frag.add(Duplicate);
			frag.add(PushI, 4);
			frag.add(Add);
			Macros.loadIFrom(frag, RunTime.RATIONAL_DEN);
			frag.add(StoreI);
			Macros.loadIFrom(frag, RunTime.RATIONAL_NUM);
			frag.add(StoreI);
		} else {
			assert false : "Type " + type + " unimplemented in opcodeForStore()";
		}
		return frag;
	}

	public static ASMCodeFragment opcodeForLoad(Type type) {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		if (type == PrimitiveType.INTEGER || type == PrimitiveType.STRING || type instanceof Array || type instanceof Lambda) {
			frag.add(LoadI);
		} else if (type == PrimitiveType.FLOATING) {
			frag.add(LoadF);
		} else if (type == PrimitiveType.BOOLEAN || type == PrimitiveType.CHARACTER) {
			frag.add(LoadC);
		} else if (type == PrimitiveType.RATIONAL) {
			// Stack : [... addressOfRationalNumber]
			frag.add(Duplicate);
			frag.add(PushI, 4);
			frag.add(Add);
			frag.add(LoadI);
			frag.add(Exchange);
			frag.add(LoadI);
			frag.add(Exchange);
		} else {
			assert false : "Type " + type + " unimplemented in opcodeForLoad()";
		}
		return frag;
	}

	protected class CodeVisitor extends ParseNodeVisitor.Default {
		private Map<ParseNode, ASMCodeFragment> codeMap;
		ASMCodeFragment code;

		public CodeVisitor() {
			codeMap = new HashMap<ParseNode, ASMCodeFragment>();
		}

		////////////////////////////////////////////////////////////////////
		// Make the field "code" refer to a new fragment of different sorts.
		private void newAddressCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_ADDRESS);
			codeMap.put(node, code);
		}

		private void newValueCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VALUE);
			codeMap.put(node, code);
		}

		private void newVoidCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VOID);
			codeMap.put(node, code);
		}

		////////////////////////////////////////////////////////////////////
		// Get code from the map.
		private ASMCodeFragment getAndRemoveCode(ParseNode node) {
			ASMCodeFragment result = codeMap.get(node);
			codeMap.remove(result);
			return result;
		}

		public ASMCodeFragment removeRootCode(ParseNode tree) {
			return getAndRemoveCode(tree);
		}

		ASMCodeFragment removeValueCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			makeFragmentValueCode(frag, node);
			return frag;
		}

		private ASMCodeFragment removeAddressCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isAddress();
			return frag;
		}

		ASMCodeFragment removeVoidCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isVoid();
			return frag;
		}

		////////////////////////////////////////////////////////////////////
		// convert code to value-generating code.
		private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
			assert !code.isVoid();

			if (code.isAddress()) {
				turnAddressIntoValue(code, node);
			}
		}

		private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
			if (node.getType() == PrimitiveType.INTEGER || node.getType() == PrimitiveType.STRING
					|| node.getType() instanceof Array || node.getType() instanceof Lambda) {
				code.add(LoadI);
			} else if (node.getType() == PrimitiveType.FLOATING) {
				code.add(LoadF);
			} else if (node.getType() == PrimitiveType.BOOLEAN || node.getType() == PrimitiveType.CHARACTER) {
				code.add(LoadC);
			} else if (node.getType() == PrimitiveType.RATIONAL) {
				// Stack : [... addressOfRationalNumber]
				code.add(Duplicate);
				code.add(PushI, 4);
				code.add(Add);
				code.add(LoadI);
				code.add(Exchange);
				code.add(LoadI);
				code.add(Exchange);
			} else {
				assert false : "node " + node;
			}
			code.markAsValue();
		}
		
		private ASMCodeFragment opcodeForStore(Type type) {
			ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
			if (type == PrimitiveType.INTEGER || type == PrimitiveType.STRING || type instanceof Array
					|| type instanceof Lambda) {
				frag.add(StoreI);
			} else if (type == PrimitiveType.FLOATING) {
				frag.add(StoreF);
			} else if (type == PrimitiveType.BOOLEAN || type == PrimitiveType.CHARACTER) {
				frag.add(StoreC);
			} else if (type == PrimitiveType.RATIONAL) {
				// Stack is: [.. pointerToStoreIn num den]
				Macros.storeITo(frag, RunTime.RATIONAL_DEN);
				Macros.storeITo(frag, RunTime.RATIONAL_NUM);
				frag.add(Duplicate);
				frag.add(PushI, 4);
				frag.add(Add);
				Macros.loadIFrom(frag, RunTime.RATIONAL_DEN);
				frag.add(StoreI);
				Macros.loadIFrom(frag, RunTime.RATIONAL_NUM);
				frag.add(StoreI);
			} else {
				assert false : "Type " + type + " unimplemented in opcodeForStore()";
			}
			return frag;
		}

		////////////////////////////////////////////////////////////////////
		// ensures all types of ParseNode in given AST have at least a visitLeave
		public void visitLeave(ParseNode node) {
			assert false : "node " + node + " not handled in ASMCodeGenerator";
		}

		///////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		public void visitLeave(ProgramNode node) {
			newVoidCode(node);
			for (ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}

		public void visitLeave(BlockStatementNode node) {
			newVoidCode(node);
			for (ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}

		///////////////////////////////////////////////////////////////////////////
		// statements and declarations

		public void visitLeave(PrintStatementNode node) {
			newVoidCode(node);
			new PrintStatementGenerator(code, this).generate(node);
		}

		public void visit(NewlineNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.NEWLINE_PRINT_FORMAT);
			code.add(Printf);
		}

		public void visit(SpaceNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.SPACE_PRINT_FORMAT);
			code.add(Printf);
		}

		public void visit(TabNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.TAB_PRINT_FORMAT);
			code.add(Printf);
		}

		public void visitLeave(LambdaNode node) {
			newValueCode(node);
			
			String startLabel = node.getStartLabel();
			String endLabel = node.getEndLabel();
			String returnLabel = node.getReturnLabel();
			
			/*
			 * Taking the approach of directly jumping over function code without letting anyone who has not called function run the code
			 */
			code.add(Jump, endLabel);
			
			code.add(Label, startLabel);
			
			/*
			 * Stack while starting the function call: [... returnAddress]
			 * 
			 * Steps to follow:
			 * 1. Store information of old frame pointer and return address in memory
			 * 2. Set frame pointer to be equal to stack pointer (Make sure stack pointer location is not changed as it was before function call)
			 * 3. Subtract the size of lambda's body from stack pointer (which includes the additional bytes added above)
			 * 4. Merge the code of the body of this lambda and hence, it will run (scoping has been taken care of)
			 * 5. Within the body, if the code reaches return statement, the visitLeave for returnNode sends it to returnLabel.
			 * 	  Otherwise, runtime error
			 * 6. At returnLabel, the return value must be at top of stack
			 * 		- Push the return address from memory (FramePointer - 8) on top of stack
			 * 		- Update frame pointer with the address stored at FramePointer - 4
			 * 		- Push exchange instruction which brings returned value on top of stack
			 * 		- Increase stack pointer to it's old value by adding (sizeOfFuncFrame + Additional bytes required)
			 * 		- Decrease the stack pointer by size of returned value and place the top of stack's value at SP.
			 * 7. After this, we should have return address on top of stack where we want to go back from where the function was called. (Push Return).
			 */
			
			// Step 1.
			Macros.loadIFrom(code, RunTime.STACK_POINTER);                                    // [... returnAddress SP]
			code.add(Duplicate);                                                              // [... returnAddress SP SP]
			code.add(PushI, ASMConstants.FRAME_POINTER_SIZE);                                 // [... returnAddress SP SP 4]
			code.add(Subtract);                                                               // [... returnAddress SP SP-4]
			Macros.loadIFrom(code, RunTime.FRAME_POINTER);                                    // [... returnAddress SP SP-4 FP]
			code.add(StoreI);                                                                 // [... returnAddress SP]
			code.add(PushI, ASMConstants.FUNCTION_CALL_EXTRA_BYTES);                          // [... returnAddress SP 8]
			code.add(Subtract);                                                               // [... returnAddress SP-8]
			code.add(Exchange);                                                               // [... SP-8 returnAddress]
			code.add(StoreI);                                                                 // [...] and old FP and returnAddress are in memory now
			
			// Step 2.
			Macros.loadIFrom(code, RunTime.STACK_POINTER);                                    // [... SP]
			Macros.storeITo(code, RunTime.FRAME_POINTER);                                     // [...] and now FP == SP
			
			// Step 3.
			Macros.loadIFrom(code, RunTime.STACK_POINTER);                                    // [... SP]
			int functionOverallSize = ASMConstants.FUNCTION_CALL_EXTRA_BYTES + node.child(node.nChildren() - 1).getScope().getAllocatedSize();
			code.add(PushI, functionOverallSize);                                             // [... SP sizeOfFunctionCall]
			code.add(Subtract);                                                               // [... newSP]
			Macros.storeITo(code, RunTime.STACK_POINTER);                                     // [...]
			
			// Step 4.
			code.append(removeVoidCode(node.child(node.nChildren() - 1)));
			
			// Step 5.
			// RunTime error if comes here (which means that code took a path which was not followed by a return statement in the body)
			code.add(Jump, RunTime.NO_RETURN_IN_LAMBDA);
			
			// Code should reach here if there was a valid return statement
			code.add(Label, returnLabel);
			
			// Step 6.                                                                         // [... returnedValue]
			// 6.1
			Macros.loadIFrom(code, RunTime.FRAME_POINTER);                                     // [... returnedValue FP]
			code.add(Duplicate);                                                               // [... returnedValue FP FP]
			code.add(PushI, ASMConstants.FUNCTION_CALL_EXTRA_BYTES);                           // [... returnedValue FP FP 8]
			code.add(Subtract);                                                                // [... returnedValue FP FP-8]
			code.add(LoadI);                                                                   // [... returnedValue FP whereToReturnAddr]
			code.add(Exchange);                                                                // [... returnedValue whereToReturnAddr FP]
			
			// 6.2
			code.add(PushI, ASMConstants.FRAME_POINTER_SIZE);                                  // [... returnedValue whereToReturnAddr FP 4]
			code.add(Subtract);                                                                // [... returnedValue whereToReturnAddr FP-4]
			code.add(LoadI);                                                                   // [... returnedValue whereToReturnAddr oldFramePointer]
			Macros.storeITo(code, RunTime.FRAME_POINTER);                                      // [... returnedValue whereToReturnAddr]
			
			// 6.3
			flipValueAndAddress(node.getReturnType());                                         // [... whereToReturnAddr returnedValue]
			
			// 6.4
			Macros.loadIFrom(code, RunTime.STACK_POINTER);                                     // [... whereToReturnAddr returnedValue SP]
			
			int parameterScopeSize = node.getScope().getAllocatedSize();
			code.add(PushI, functionOverallSize + parameterScopeSize);                         // [... whereToReturnAddr returnedValue SP funcSize+paramScopeSize]
			code.add(Add);                                                                     // [... whereToReturnAddr returnedValue SP+sizeToUpgrade]
			code.add(Duplicate);                                                               // [... whereToReturnAddr returnedValue newSP newSP]
			Macros.storeITo(code, RunTime.STACK_POINTER);                                      // [... whereToReturnAddr returnedValue newSP]
			
			// 6.5
			code.add(PushI, node.getReturnType().getSize());                                   // [... whereToReturnAddr returnedValue newSP returnTypeSize]
			code.add(Subtract);                                                                // [... whereToReturnAddr returnedValue newSP-returnTypeSize]
			placeReturnedValue(node.getReturnType());
			
			// Step 7
			code.add(Return);                                                                  // [... whereToReturnAddr] -> [...]
			
			// ------------ End of function --------------
			
			// Code should come directly here if function's code was not called
			code.add(Label, endLabel);
			
			// Marking the location of the code for the function
			code.add(PushD, startLabel);
		}
		
		private void flipValueAndAddress(Type returnType) {
			/*
			 * This assumes you have a situation like this:
			 * Stack : [... value address] and you want to switch them
			 * 
			 * Two cases:
			 * If rational, exchange twice
			 * Else, exchange once
			 */
			
			if(!(returnType instanceof NullType)) {
				code.add(Exchange);                                                            // [... address value]
			}
			
			// If returnType was rational, stack must be like this: [... numerator address denominator]
			if(returnType == PrimitiveType.RATIONAL) {
				Macros.storeITo(code, RunTime.RATIONAL_DEN);                                   // [... numerator address]
				code.add(Exchange);                                                            // [... address numerator]
				Macros.loadIFrom(code, RunTime.RATIONAL_DEN);                                  // [... address value]
			}
		}
		
		private void placeReturnedValue(Type returnType) {
			// Stack: [... whereToReturnAddr returnedValue newSP-returnTypeSize]
			if(returnType instanceof NullType) {
				code.add(Pop);                                                                 // [... whereToReturnAddr] (Should be no returned value in null case)
			} else {
				// Stack should be : [... whereToReturnAddress value SP]
				flipValueAndAddress(returnType);                                               // [... whereToReturnAddr SP value]
				code.append(opcodeForStore(returnType));                                       // [... whereToReturnAddr]
			}
		}

		public void visitLeave(ParameterSpecificationNode node) {
			newVoidCode(node);
		}

		public void visitLeave(FunctionNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));

			code.append(lvalue);
			code.append(rvalue);

			Type type = node.getType();
			code.append(opcodeForStore(type));
		}

		public void visitLeave(DeclarationNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));

			code.append(lvalue);
			code.append(rvalue);

			Type type = node.getType();
			code.append(opcodeForStore(type));
		}

		public void visitLeave(AssignmentStatementNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));

			code.append(lvalue);
			code.append(rvalue);

			Type type = node.getType();
			code.append(opcodeForStore(type));
		}

		public void visitLeave(TypeNode node) {
			newValueCode(node);
		}

		public void visitLeave(IfStatementNode node) {
			Labeller labeller = new Labeller("if");
			String falseLabel = labeller.newLabel("false");
			String endLabel = labeller.newLabel("end");

			newVoidCode(node);

			ASMCodeFragment condition = removeValueCode(node.child(0));
			ASMCodeFragment thenClause = removeVoidCode(node.child(1));

			code.append(condition);
			code.add(JumpFalse, falseLabel);
			code.append(thenClause);
			code.add(Jump, endLabel);

			code.add(Label, falseLabel);

			if (hasElseClause(node)) {
				ASMCodeFragment elseClause = removeVoidCode(node.child(2));
				code.append(elseClause);
			}

			code.add(Label, endLabel);
		}

		private boolean hasElseClause(IfStatementNode node) {
			return node.nChildren() == 3;
		}

		public void visitLeave(WhileStatementNode node) {
			String startLabel = node.getStartLabel();
			String falseAndEndLabel = node.getEndLabel();

			newVoidCode(node);

			ASMCodeFragment condition = removeValueCode(node.child(0));
			ASMCodeFragment whileBlock = removeVoidCode(node.child(1));

			code.add(Label, startLabel);
			code.append(condition);
			code.add(JumpFalse, falseAndEndLabel);
			code.append(whileBlock);
			code.add(Jump, startLabel);

			code.add(Label, falseAndEndLabel);
		}
		
		public void visit(BreakNode node) {
			newVoidCode(node);
			
			code.add(Jump, node.getEnclosingWhileEndLabel());
		}
		
		public void visit(ContinueNode node) {
			newVoidCode(node);
			
			code.add(Jump, node.getEnclosingWhileStartLabel());
		}
		
		public void visitLeave(ReturnNode node) {
			newVoidCode(node);
			
			if(node.nChildren() > 0) {
				code.append(removeValueCode(node.child(0)));
			}
			
			code.add(Jump, node.whereToGoOnReturn());
		}
		
		private void handleFunctionInvocationForCall(UnaryOperatorNode node) {
			ASMCodeFragment inCaseNotNull = new ASMCodeFragment(GENERATES_VOID);
			Type childType = node.child(0).getType();

			if (!(childType instanceof NullType)) {
				// We will need to remove the appended values!!
				inCaseNotNull.add(Pop);
				if (node.getType() == PrimitiveType.RATIONAL) {
					inCaseNotNull.add(Pop);
				}
			}

			newVoidCode(node);
			code.append((childType instanceof NullType) ? removeVoidCode(node.child(0)) : removeValueCode(node.child(0)));
			
			// TODO: Check if this is working fine in case of no instruction added in inCaseNotNull
			code.append(inCaseNotNull);
		}

		///////////////////////////////////////////////////////////////////////////
		// expressions
		public void visitLeave(UnaryOperatorNode node) {
			if (node.getToken().isLextant(Keyword.CALL)) {
				handleFunctionInvocationForCall(node);
				return;
			}

			newValueCode(node);

			ASMCodeFragment arg = removeValueCode(node.child(0));
			code.append(arg);

			Object variant = node.getSignature().getVariant();
			if (variant instanceof ASMOpcode) {
				ASMOpcode opcode = (ASMOpcode) variant;
				code.add(opcode);
			} else if (variant instanceof SimpleCodeGenerator) {
				SimpleCodeGenerator generator = (SimpleCodeGenerator) variant;
				ASMCodeFragment fragment = generator.generate(node);
				code.append(fragment);

				if (fragment.isAddress()) {
					// Which means that the fragment returns a pointer
					code.markAsAddress(); // Now we know that this code is a pointer
				} else if (fragment.isVoid()) {
					code.markAsVoid();
				}

			}
		}

		public void visitLeave(KNaryOperatorNode node) {
			// Handle visit for function call here (Since it is the only use of
			// KNaryOperatorNode for now)
			assert node.getToken().isLextant(Punctuator.FUNCTION_INVOCATION);
			
			if(node.getType() instanceof NullType) {
				newVoidCode(node);
			} else {
				newValueCode(node);
			}

			/*
			 * To handle function calls, First, store all the parameter values on the memory
			 * stack using stack pointer (if any)
			 */

			Macros.loadIFrom(code, RunTime.STACK_POINTER);                    // [... SP]
			
			for (int i = 1; i < node.nChildren(); i++) {
				code.add(PushI, node.child(i).getType().getSize());           // [... SP sizeOfType]
				code.add(Subtract);                                           // [... SP-sizeOfType] which is the location where will store our parameter
				code.add(Duplicate);                                          // [... nSP nSP]
				code.append(removeValueCode(node.child(i)));                  // [... nSP nSP pValue]
				code.append(opcodeForStore(node.child(i).getType()));         // [... nSP]
			}
			
			Macros.storeITo(code, RunTime.STACK_POINTER);                     // [...]
			
			// Call the function using address from first child!
			code.append(removeValueCode(node.child(0)));                      // [... lambdaAddress]
			code.add(CallV);                                                  // Jumps to lambdaAddress and stack becomes [... returnAddr]
			
			// After completing the function call, the code comes here with stack:
			// [...] and with the value returned from function (if any) at stack pointer location
			
			Type returnType = node.getType();
			int sizeOfReturnValue = returnType.getSize();
			if(sizeOfReturnValue > 0) {
				// We need the value
				Macros.loadIFrom(code, RunTime.STACK_POINTER);                // [... SP] 
				code.add(PushI, sizeOfReturnValue);                           // [... SP returnValueSize]
				code.add(Subtract);                                           // [... SP-returnValueSize(newSP)]
				code.append(opcodeForLoad(returnType));                       // [... valueReturnedFromFunctionCall]
			}
		}

		public void visitLeave(BinaryOperatorNode node) {
			Lextant operator = node.getOperator();

			if (Punctuator.isComparisonPunctuator(operator.getLexeme())) {
				visitComparisonOperatorNode(node, operator);
			} else {
				visitNormalBinaryOperatorNode(node);
			}
		}

		private void visitComparisonOperatorNode(BinaryOperatorNode node, Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));

			Labeller labeller = new Labeller("compare");

			String startLabel = labeller.newLabel("arg1");
			String arg2Label = labeller.newLabel("arg2");
			String subLabel = labeller.newLabel("sub");
			String trueLabel = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel = labeller.newLabel("join");

			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);

			assert (node.child(0).getType().equivalent(node.child(1).getType()));
			Type type = node.child(0).getType();
			assert (operator instanceof Punctuator);

			Punctuator punctuator = (Punctuator) operator;

			boolean isFloating = type == PrimitiveType.FLOATING;

			if (type == PrimitiveType.RATIONAL) {
				// Stack currently has: [num1 den1 num2 den2] and I want [num1*den2 num2*den1]
				// for comparisons
				Macros.storeITo(code, RunTime.RATIONAL_DEN); // [num1 den1 num2]
				code.add(Multiply); // [num1 den1*num2]
				code.add(Exchange); // [den1*num2 num1]
				Macros.loadIFrom(code, RunTime.RATIONAL_DEN); // [den1*num2 num1 den2]
				code.add(Multiply); // [den1*num2 num1*den2]
				code.add(Exchange); // [num1*den2 den1*num2]
			}

			code.add(isFloating ? FSubtract : Subtract);

			// Change the type checker to something better
			switch (punctuator) {
			case GREATER:
				code.add(isFloating ? JumpFPos : JumpPos, trueLabel);
				code.add(Jump, falseLabel);
				break;
			case GREATER_OR_EQUAL:
				code.add(isFloating ? JumpFNeg : JumpNeg, falseLabel);
				code.add(Jump, trueLabel);
				break;
			case LESS:
				code.add(isFloating ? JumpFNeg : JumpNeg, trueLabel);
				code.add(Jump, falseLabel);
				break;
			case LESS_OR_EQUAL:
				code.add(isFloating ? JumpFPos : JumpPos, falseLabel);
				code.add(Jump, trueLabel);
				break;
			case EQUAL:
				code.add(isFloating ? JumpFZero : JumpFalse, trueLabel);
				code.add(Jump, falseLabel);
				break;
			case NOT_EQUAL:
				code.add(isFloating ? JumpFZero : JumpFalse, falseLabel);
				code.add(Jump, trueLabel);
				break;
			default:
				// do nothing
			}

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}

		private void visitNormalBinaryOperatorNode(BinaryOperatorNode node) {
			newValueCode(node);
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));

			Object variant = node.getSignature().getVariant();
			if (variant instanceof ASMOpcode) {
				code.append(arg1);
				code.append(arg2);

				ASMOpcode opcode = (ASMOpcode) variant;
				code.add(opcode);
			} else if (variant instanceof SimpleCodeGenerator) {
				code.append(arg1);
				code.append(arg2);

				SimpleCodeGenerator generator = (SimpleCodeGenerator) variant;
				ASMCodeFragment fragment = generator.generate(node);
				code.append(fragment);

				if (fragment.isAddress()) {
					// Which means that the fragment returns a pointer
					code.markAsAddress(); // Now we know that this code is a pointer
				}
			} else if (variant instanceof FullCodeGenerator) {
				FullCodeGenerator generator = (FullCodeGenerator) variant;
				ASMCodeFragment fragment = generator.generate(node, arg1, arg2);

				code.append(fragment);

				if (fragment.isAddress()) {
					code.markAsAddress();
				}
			} else {
				throw new UnsupportedOperationException("No ASMOpcode or fragment code matches the provided variant");
			}
		}

		@SuppressWarnings("unused")
		private ASMOpcode opcodeForOperator(Lextant lextant) {
			assert (lextant instanceof Punctuator);
			Punctuator punctuator = (Punctuator) lextant;
			switch (punctuator) {
			case ADD:
				return Add; // type-dependent!
			case MULTIPLY:
				return Multiply; // type-dependent!
			default:
				assert false : "unimplemented operator in opcodeForOperator";
			}
			return null;
		}

		///////////////////////////////////////////////////////////////////////////
		// leaf nodes (ErrorNode not necessary)
		public void visit(BooleanConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue() ? 1 : 0);
		}

		public void visit(IdentifierNode node) {
			newAddressCode(node);
			Binding binding = node.getBinding();

			binding.generateAddress(code);
		}

		public void visit(IntegerConstantNode node) {
			newValueCode(node);

			code.add(PushI, node.getValue());
		}

		public void visit(FloatingConstantNode node) {
			newValueCode(node);

			code.add(PushF, node.getValue());
		}

		public void visit(CharacterConstantNode node) {
			newValueCode(node);

			code.add(PushI, node.getValue());
		}

		public void visit(StringConstantNode node) {
			newValueCode(node);

			Labeller labeller = new Labeller("string-constant");
			String thisStringLabel = labeller.newLabel("");

			code.add(DLabel, thisStringLabel);
			code.add(DataS, node.getValue());
			code.add(PushD, thisStringLabel);
		}

		public void visitLeave(PopulatedArrayNode node) {
			newValueCode(node);

			Type subtype = ((Array) node.getType()).getSubtype();
			int statusFlags = subtype instanceof Array ? ASMConstants.STATUS_FLAG_FOR_REFERENCE
					: ASMConstants.STATUS_FLAG_FOR_NON_REFERENCE;
			int nElems = node.nChildren();
			ArrayList<ASMCodeFragment> codeForChildren = new ArrayList<>(nElems);
			for (ParseNode cNode : node.getChildren()) {
				codeForChildren.add(removeValueCode(cNode));
			}
			int recordSize = ASMConstants.ARRAY_HEADER_SIZE + nElems * subtype.getSize();
			code.add(PushI, recordSize); // [... recordSize] before calling to create a record
			createPopulatedArrayRecord(code, statusFlags, subtype, codeForChildren);

			// Place the created record's address on the stack
			Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		}
	}

}
