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
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.PopulatedArrayNode;
import parseTree.nodeTypes.AssignmentStatementNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.BlockStatementNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.FloatingConstantNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.TypeNode;
import parseTree.nodeTypes.UnaryOperatorNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.types.Array;
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
		
		// Memory heap manger goes after the main program where main program is the programASM
		code.append( MemoryManager.codeForAfterApplication() );

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
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, Record.RECORD_TYPEID_OFFSET, typecode);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, Record.RECORD_STATUS_OFFSET, statusFlags);
	}
	
	// Stack: [... nElems]
	public static void createEmptyArrayRecord(ASMCodeFragment code, int statusFlags, int subtypeSize) {
		final int typecode = Record.ARRAY_TYPE_ID;
		
		// Give error if nElements < 0
		code.add(Duplicate);           // [... nElems nElems]
		code.add(JumpNeg, RunTime.NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR);   // [... nElems]
		
		// Compute record size (Includes ASM header + aggregated size of elements)
		code.add(Duplicate);           // [... nElems nElems]
		code.add(PushI, subtypeSize);  // [... nElems nElems subtypeSize]
		code.add(Multiply);            // [... nElems nElems*subtypeSize] === [... nElems elemsSize]
		code.add(Duplicate);           // [... nElems elemsSize elemsSize]
		Macros.storeITo(code, RunTime.ARRAY_DATASIZE_TEMPORARY);  // [... nElems elemsSize]
		code.add(PushI, Record.ARRAY_HEADER_SIZE); // [... nElems elemsSize headerSize]
		code.add(Add);     // [... nElems recordSize]
		
		// Call createRecord
		createRecord(code, typecode, statusFlags);   // [... nElems]
		
		// Zero out elements of array
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);  // [... nElems recordPointer]
		code.add(PushI, Record.ARRAY_HEADER_SIZE);    // [... nElems recordPointer arrayHeaderSize]
		code.add(Add);                                // [... nElems baseAddressToAddFirstElement]
		Macros.loadIFrom(code, RunTime.ARRAY_DATASIZE_TEMPORARY);  // [... nElems baseAddressToAddFirstElement numBytes]
		code.add(Call, RunTime.CLEAR_N_BYTES);
		
		// Fill in array header
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, Record.ARRAY_SUBTYPE_SIZE_OFFSET, subtypeSize);
		Macros.writeIPtrOffset(code, RunTime.RECORD_CREATION_TEMPORARY, Record.ARRAY_LENGTH_OFFSET);
	}
	
	// Stack: [... recordSize]
	public static void createPopulatedArrayRecord(ASMCodeFragment code, int statusFlags, Type subtype, List<ASMCodeFragment> codeForChildren) {
		assert codeForChildren.size() > 0;
	
		int subtypeSize = subtype.getSize();
		final int typecode = Record.ARRAY_TYPE_ID;
		
		createRecord(code, typecode, statusFlags);                     // [...]
		Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);     // [... recordPointer]
		
		// We need to duplicate this since the recordPointer stored in RECORD_CREATION_TEMPORARY might be replaced by one of 
		// children of this node as they can be an array too and they will use that runtime variable. Hence, later when we want
		// to put that on stack, we will load the wrong one as it is replaced. So duplicate it, after adding all the children's code
		// storeItBack and then load later.
		code.add(Duplicate);
		
		code.add(PushI, Record.ARRAY_HEADER_SIZE);                     // [... recordPointer arrayHeaderSize]
		code.add(Add);                                                 // [... baseAddressForFirstElement]
		
//		ASMCodeFragment opcodeForStore = opcodeForStore(subtype);
		
		for(ASMCodeFragment cCode: codeForChildren) {                  // [... baseAddressforNthElement]
			code.add(Duplicate);                                       // [... baseAddressForNthElement baseAddressForNthElement]
			code.append(cCode);                                        // [... baseAddressForNthElement baseAddressForNthElement cCode]
			code.append(opcodeForStore(subtype));                               // [... baseAddressForNthElement]
//			code.add(StoreI);
			code.add(PushI, subtypeSize);                              // [... baseAddressForNthElement subtypeSize]
			code.add(Add);                                             // [... baseAddressFor(N+1)thElement]
		}
		code.add(Pop);                                                 // [... baseAddressForFirstElement]
		
		// Fill in array header
		Macros.storeITo(code, RunTime.RECORD_CREATION_TEMPORARY);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, Record.ARRAY_SUBTYPE_SIZE_OFFSET, subtypeSize);
		Macros.writeIPBaseOffset(code, RunTime.RECORD_CREATION_TEMPORARY, Record.ARRAY_LENGTH_OFFSET, codeForChildren.size());
	}
	
	// Spits out fragment for storing a value
	public static ASMCodeFragment opcodeForStore(Type type) {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		if (type == PrimitiveType.INTEGER  || type == PrimitiveType.STRING || type instanceof Array) {
			frag.add(StoreI);
		}
		else if (type == PrimitiveType.FLOATING) {
			frag.add(StoreF);
		}
		else if (type == PrimitiveType.BOOLEAN || type == PrimitiveType.CHARACTER) {
			frag.add(StoreC);
		}
		else if (type == PrimitiveType.RATIONAL) {
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
		if (type == PrimitiveType.INTEGER || type == PrimitiveType.STRING || type instanceof Array) {
			frag.add(LoadI);
		} else if (type == PrimitiveType.FLOATING) {
			frag.add(LoadF);
		} else if (type == PrimitiveType.BOOLEAN || type == PrimitiveType.CHARACTER) {
			frag.add(LoadC);
		} else if(type == PrimitiveType.RATIONAL) {
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
			if (node.getType() == PrimitiveType.INTEGER || node.getType() == PrimitiveType.STRING || node.getType() instanceof Array) {
				code.add(LoadI);
			} else if (node.getType() == PrimitiveType.FLOATING) {
				code.add(LoadF);
			} else if (node.getType() == PrimitiveType.BOOLEAN || node.getType() == PrimitiveType.CHARACTER) {
				code.add(LoadC);
			} else if(node.getType() == PrimitiveType.RATIONAL) {
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
			
			if(hasElseClause(node)) {
				ASMCodeFragment elseClause = removeVoidCode(node.child(2));
				code.append(elseClause);
			}
			
			code.add(Label, endLabel);
		}
		private boolean hasElseClause(IfStatementNode node) {
			return node.nChildren() == 3;
		}
		
		public void visitLeave(WhileStatementNode node) {
			Labeller labeller = new Labeller("while");
			String startLabel = labeller.newLabel("start");
			String falseAndEndLabel = labeller.newLabel("falseAndEnd");
			
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

		private ASMCodeFragment opcodeForStore(Type type) {
			ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
			if (type == PrimitiveType.INTEGER  || type == PrimitiveType.STRING || type instanceof Array) {
				frag.add(StoreI);
			}
			else if (type == PrimitiveType.FLOATING) {
				frag.add(StoreF);
			}
			else if (type == PrimitiveType.BOOLEAN || type == PrimitiveType.CHARACTER) {
				frag.add(StoreC);
			}
			else if (type == PrimitiveType.RATIONAL) {
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

		///////////////////////////////////////////////////////////////////////////
		// expressions
		public void visitLeave(UnaryOperatorNode node) {
			newValueCode(node);
			
			ASMCodeFragment arg = removeValueCode(node.child(0));
			code.append(arg);
			
			Object variant = node.getSignature().getVariant();
			if (variant instanceof ASMOpcode) {
				ASMOpcode opcode = (ASMOpcode) variant;
				code.add(opcode);
			} else if(variant instanceof SimpleCodeGenerator) {
				SimpleCodeGenerator generator = (SimpleCodeGenerator) variant;
				ASMCodeFragment fragment = generator.generate(node);
				code.append(fragment);

				if (fragment.isAddress()) {
					// Which means that the fragment returns a pointer
					code.markAsAddress(); // Now we know that this code is a pointer
				} else if(fragment.isVoid()) {
					code.markAsVoid();
				}

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
			
			if(type == PrimitiveType.RATIONAL) {
				// Stack currently has: [num1 den1 num2 den2] and I want [num1*den2 num2*den1] for comparisons
				Macros.storeITo(code, RunTime.RATIONAL_DEN);      // [num1 den1 num2]
				code.add(Multiply);                               // [num1 den1*num2]
				code.add(Exchange);                               // [den1*num2 num1]
				Macros.loadIFrom(code, RunTime.RATIONAL_DEN);     // [den1*num2 num1 den2]
				code.add(Multiply);                               // [den1*num2 num1*den2]
				code.add(Exchange);                               // [num1*den2 den1*num2]
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
				
				if(fragment.isAddress()) {
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
			
			Type subtype = ((Array)node.getType()).getSubtype();
			int statusFlags = subtype instanceof Array ? Record.STATUS_FLAG_FOR_REFERENCE : Record.STATUS_FLAG_FOR_NON_REFERENCE;
			int nElems = node.nChildren();
			ArrayList<ASMCodeFragment> codeForChildren = new ArrayList<>(nElems);
			for(ParseNode cNode: node.getChildren()) {
				codeForChildren.add(removeValueCode(cNode));
			}
			int recordSize = Record.ARRAY_HEADER_SIZE + nElems * subtype.getSize();
			code.add(PushI, recordSize); // [... recordSize] before calling to create a record
			createPopulatedArrayRecord(code, statusFlags, subtype, codeForChildren);
			
			// Place the created record's address on the stack
			Macros.loadIFrom(code, RunTime.RECORD_CREATION_TEMPORARY);
		}
	}

}
