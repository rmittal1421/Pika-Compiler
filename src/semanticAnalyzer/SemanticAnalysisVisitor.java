package semanticAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.PikaLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.PopulatedArrayNode;
import parseTree.nodeTypes.AssignmentStatementNode;
import parseTree.nodeTypes.BinaryOperatorNode;
import parseTree.nodeTypes.BooleanConstantNode;
import parseTree.nodeTypes.CharacterConstantNode;
import parseTree.nodeTypes.BlockStatementNode;
import parseTree.nodeTypes.DeclarationNode;
import parseTree.nodeTypes.ErrorNode;
import parseTree.nodeTypes.IdentifierNode;
import parseTree.nodeTypes.IfStatementNode;
import parseTree.nodeTypes.IntegerConstantNode;
import parseTree.nodeTypes.FloatingConstantNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.ProgramNode;
import parseTree.nodeTypes.SpaceNode;
import parseTree.nodeTypes.StringConstantNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.TypeNode;
import parseTree.nodeTypes.UnaryOperatorNode;
import parseTree.nodeTypes.WhileStatementNode;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.LextantToken;
import tokens.Token;

class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}

	///////////////////////////////////////////////////////////////////////////
	// constructs larger than statements
	@Override
	public void visitEnter(ProgramNode node) {
		enterProgramScope(node);
	}

	public void visitLeave(ProgramNode node) {
		leaveScope(node);
	}

	public void visitEnter(BlockStatementNode node) {
		enterSubscope(node);
	}

	public void visitLeave(BlockStatementNode node) {
		leaveScope(node);
	}

	///////////////////////////////////////////////////////////////////////////
	// helper methods for scoping.
	private void enterProgramScope(ParseNode node) {
		Scope scope = Scope.createProgramScope();
		node.setScope(scope);
	}

	private void enterSubscope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}

	private void leaveScope(ParseNode node) {
		node.getScope().leave();
	}

	///////////////////////////////////////////////////////////////////////////
	// statements and declarations
	@Override
	public void visitLeave(PrintStatementNode node) {
	}

	@Override
	public void visitLeave(DeclarationNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);

		Type declarationType = initializer.getType();
		node.setType(declarationType);

		identifier.setType(declarationType);
		addBinding(identifier, declarationType, node.getDeclarationType());
	}

	@Override
	public void visitLeave(AssignmentStatementNode node) {
		ParseNode target = node.child(0);
		ParseNode expression = node.child(1);
		
		ArrayList<Type> castFromTypes = new ArrayList<>(Arrays.asList(PrimitiveType.CHARACTER, PrimitiveType.INTEGER));
		ArrayList<Type> castToTypes = new ArrayList<>(Arrays.asList(PrimitiveType.INTEGER, PrimitiveType.FLOATING, PrimitiveType.RATIONAL));
		
		if(!target.getType().equivalent(expression.getType())) {
			if(castFromTypes.contains(expression.getType()) && castToTypes.contains(target.getType())) {
				promoteNode(node, target.getType(), 1);
			} else {
				typeCheckError(node, Arrays.asList(target.getType(), expression.getType()));
			}
//			typeCheckError(node, Arrays.asList(target.getType(), expression.getType()));
		}

		if (target instanceof IdentifierNode) {
			IdentifierNode identifier = (IdentifierNode) target;
			if (identifier.getBinding().getDeclareLextant() != Keyword.VAR) {
				assignToConstError(node);
			}
		}

		expression = node.child(1);
		Type assignmentType = expression.getType();
		node.setType(assignmentType);
	}

	@Override
	public void visitLeave(TypeNode node) {
		if(!node.getToken().isLextant(Punctuator.OPEN_SQUARE_BRACKET)) {
			node.setType(PrimitiveType.fromToken(node.typeToken()));
		} else { 
			node.setType(new Array(node.child(0).getType()));
		}
	}

	@Override
	public void visitEnter(IfStatementNode node) {
		enterSubscope(node);
	}

	@Override
	public void visitLeave(IfStatementNode node) {
		leaveScope(node);

		if (!node.child(0).getType().equivalent(PrimitiveType.BOOLEAN)) {
			typeCheckErrorForControlFlow(node);
		}
	}

	@Override
	public void visitEnter(WhileStatementNode node) {
		enterSubscope(node);
	}

	@Override
	public void visitLeave(WhileStatementNode node) {
		leaveScope(node);

		if (!node.child(0).getType().equivalent(PrimitiveType.BOOLEAN)) {
			typeCheckErrorForControlFlow(node);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// expressions
	@Override
	public void visitLeave(UnaryOperatorNode node) {
		assert node.nChildren() == 1;
		List<Type> childTypes = Arrays.asList(node.child(0).getType());
		Lextant operator = operatorFor(node);

		if(node.getToken().isLextant(Keyword.LENGTH, Keyword.CLONE, Keyword.DEALLOC) && !(childTypes.get(0) instanceof Array)) {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		} else if(node.getToken().isLextant(Punctuator.NOT) && !(childTypes.get(0).equivalent(PrimitiveType.BOOLEAN))) {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
		
		if(node.getToken().isLextant(Keyword.CLONE)) {
			node.setType(node.child(0).getType());
		}

		FunctionSignatures signatures = FunctionSignatures.signaturesOf(operator);
		FunctionSignature signature = signatures.acceptingSignature(childTypes);

		if (signature.accepts(childTypes)) {
			node.setType(signature.resultType());
			node.setSignature(signature);
		} else {
			// Try promoting and check again
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}

	private Lextant operatorFor(UnaryOperatorNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}
	
//	private Type[] charCast = {PrimitiveType.INTEGER, PrimitiveType.FLOATING, PrimitiveType.RATIONAL};
//	private Type[] intCast = {PrimitiveType.FLOATING, PrimitiveType.RATIONAL};
	
	private void promoteNode(ParseNode node, Type typeToCastTo, int childNumber) {
		ParseNode childToReplace = node.child(childNumber);
		
		Type typeToCastFrom = node.child(childNumber).getType();
		TypeNode typeNode;
		if(typeToCastTo.equivalent(PrimitiveType.INTEGER)) {
			typeNode = new TypeNode(Keyword.INT.prototype());
		} else if(typeToCastTo.equivalent(PrimitiveType.FLOATING)) {
			typeNode = new TypeNode(Keyword.FLOAT.prototype());
		} else  {
			typeNode = new TypeNode(Keyword.RAT.prototype());
		}
		
		BinaryOperatorNode nodeToAdd = null;
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(Punctuator.CAST);
		if(typeToCastFrom.equivalent(PrimitiveType.CHARACTER) && !typeToCastTo.equivalent(PrimitiveType.INTEGER)) {
			TypeNode intermediateTypeNode = new TypeNode(Keyword.INT.prototype());
			intermediateTypeNode.setType(PrimitiveType.INTEGER);
			FunctionSignature castSignature = signatures.acceptingSignature(Arrays.asList(PrimitiveType.CHARACTER, PrimitiveType.INTEGER));
			nodeToAdd = BinaryOperatorNode.withChildren(Punctuator.CAST.prototype(), childToReplace, intermediateTypeNode);
			nodeToAdd.setSignature(castSignature);
			nodeToAdd.setType(castSignature.resultType());
			typeToCastFrom = PrimitiveType.INTEGER;
		}
		
		typeNode.setType(typeToCastTo);
		FunctionSignature castSignature = signatures.acceptingSignature(Arrays.asList(typeToCastFrom, typeNode.getType()));
		nodeToAdd = BinaryOperatorNode.withChildren(Punctuator.CAST.prototype(), nodeToAdd != null ? nodeToAdd : childToReplace, typeNode);
		nodeToAdd.setSignature(castSignature);
		nodeToAdd.setType(castSignature.resultType());
		node.replaceChild(childToReplace, nodeToAdd);
	}
	
	private FunctionSignature findSuitableSignature(ParseNode node, FunctionSignatures signatures, List<Type> childTypes) {
		assert childTypes.size() > 0;
		int nChildren = childTypes.size();

		boolean[] elligibleToCast = new boolean[nChildren];
		for(int i = 0; i < nChildren; i++) {
			Type type = childTypes.get(i);
			if(type.equivalent(PrimitiveType.CHARACTER) || type.equivalent(PrimitiveType.INTEGER)) {
				elligibleToCast[i] = true;
			} else {
				elligibleToCast[i] = false;
			}
		}
		
		for(int i = 0; i < childTypes.size(); i++) {
			List<Type> potentialChildTypes = new ArrayList<>(childTypes);
			if(elligibleToCast[i] && !(node.child(i) instanceof TypeNode)) {
				// First try for left operand
				if(childTypes.get(i).equivalent(PrimitiveType.CHARACTER)) {
					// Try to find a unique signature
					potentialChildTypes.set(i, PrimitiveType.INTEGER);
					FunctionSignature potentialSig = signatures.acceptingSignature(potentialChildTypes);
					if(potentialSig.accepts(potentialChildTypes)) {
						promoteNode(node, potentialSig.getParamTypes()[i], i);
						for(int j = 0; j < childTypes.size(); j++) {
							childTypes.set(j, potentialChildTypes.get(j));
						}
						return potentialSig;
					}
				} 

				// Try to find all floating and rational casting matching signatures.
				// If more than 1 found, abort and issue error. If only 1 found, that's the result.
				int matchesFound = 0;
				potentialChildTypes.set(i, PrimitiveType.FLOATING);
				FunctionSignature potentialSig = signatures.acceptingSignature(potentialChildTypes);
				if(potentialSig.accepts(potentialChildTypes)) { 
					matchesFound++;
					for(int j = 0; j < childTypes.size(); j++) {
						childTypes.set(j, potentialChildTypes.get(j));
					}
				}
				potentialChildTypes.set(i, PrimitiveType.RATIONAL);
				FunctionSignature anotherPotentialSig = signatures.acceptingSignature(potentialChildTypes);
				if(anotherPotentialSig.accepts(potentialChildTypes)) {
					if(matchesFound == 1) {
						// Issue an error as we matched more than one.
						return FunctionSignature.nullInstance();
					} else {
						// We found the matching signature
						promoteNode(node, anotherPotentialSig.getParamTypes()[i], i);
						for(int j = 0; j < childTypes.size(); j++) {
							childTypes.set(j, potentialChildTypes.get(j));
						}
						return anotherPotentialSig;
					}
				} else if(matchesFound == 1) {
					promoteNode(node, potentialSig.getParamTypes()[i], i);
					return potentialSig;
				}
			}
		}
		return FunctionSignature.nullInstance();
	}

	@Override
	public void visitLeave(BinaryOperatorNode node) {
		assert node.nChildren() == 2;
		ParseNode left = node.child(0);
		ParseNode right = node.child(1);
		List<Type> childTypes = Arrays.asList(left.getType(), right.getType());

		Lextant operator = operatorFor(node);
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(operator);
		FunctionSignature signature = signatures.acceptingSignature(childTypes);

		if (signature.accepts(childTypes)) {
			node.setType(signature.resultType());
			node.setSignature(signature);
		} else {
			// Try promoting and check again
			signature = findSuitableSignature(node, signatures, childTypes);
//			if(!signature.isNull()) {
			if(signature.accepts(childTypes)) {
				node.setType(signature.resultType());
				node.setSignature(signature);
			} else {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			}
		}
	}

	private Lextant operatorFor(BinaryOperatorNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}

	///////////////////////////////////////////////////////////////////////////
	// simple leaf nodes
	@Override
	public void visit(BooleanConstantNode node) {
		node.setType(PrimitiveType.BOOLEAN);
	}

	@Override
	public void visit(ErrorNode node) {
		node.setType(PrimitiveType.ERROR);
	}

	@Override
	public void visit(IntegerConstantNode node) {
		node.setType(PrimitiveType.INTEGER);
	}

	@Override
	public void visit(FloatingConstantNode node) {
		node.setType(PrimitiveType.FLOATING);
	}

	@Override
	public void visit(CharacterConstantNode node) {
		node.setType(PrimitiveType.CHARACTER);
	}

	@Override
	public void visit(StringConstantNode node) {
		node.setType(PrimitiveType.STRING);
	}

	@Override
	public void visit(NewlineNode node) {
	}

	@Override
	public void visit(SpaceNode node) {
	}

	@Override
	public void visit(TabNode node) {
	}

	@Override
	public void visitEnter(PopulatedArrayNode node) {
	}

	@Override
	public void visitLeave(PopulatedArrayNode node) {
		assert node.nChildren() > 0;
		Type type = node.child(0).getType();
//		for(ParseNode child: node.getChildren()) {
//			if(!child.getType().equivalent(type)) {
//				// typeCheckError
//				typeCheckError(node, )
//			}
//		}
//		node.setType(new Array(type));
//		return;
		
		if(type instanceof PrimitiveType) {
			// Only then promotions are allowed
			Type finalType = PrimitiveType.NO_TYPE;
			ArrayList<Type> typesGiven = new ArrayList<>();
			boolean thereIsRational = false;
			boolean thereIsFloating = false;
			boolean invalidTypeFound = false;
			ArrayList<ParseNode> childrenOfThisNode = new ArrayList<>();
			for(ParseNode childNode: node.getChildren()) {
				Type childType = childNode.getType();
				childrenOfThisNode.add(childNode);
				
				if(!typesGiven.contains(childType)) {
					typesGiven.add(childType);
					thereIsFloating = thereIsFloating || childType == PrimitiveType.FLOATING;
					thereIsRational = thereIsRational || childType == PrimitiveType.RATIONAL;
				}
				
				if(!PrimitiveType.isATypeInvolvedInPromotions(childType)) {
					// Return as nothing can be done about this.
					invalidTypeFound = true;
				}
			}
			if(typesGiven.size() == 1) {
				finalType = typesGiven.get(0);
				node.setType(new Array(finalType));
				return;
			} else if((thereIsFloating && thereIsRational) || invalidTypeFound) {
				typeCheckError(node, typesGiven);
				node.setType(PrimitiveType.ERROR);
				return;
			}
			
			Type typeToCastTo = thereIsRational ? PrimitiveType.RATIONAL : thereIsFloating ? PrimitiveType.FLOATING : PrimitiveType.INTEGER;
			for(int i = 0; i < childrenOfThisNode.size(); i++) {
				ParseNode cNode = childrenOfThisNode.get(i);
				if(!cNode.getType().equals(typeToCastTo)) {
					promoteNode(node, typeToCastTo, i);
				}
			}
			node.setType(new Array(typeToCastTo));
		} else {
			boolean unmatchingFound = false;
			ArrayList<Type> childTypes = new ArrayList<>();
			for(ParseNode cNode: node.getChildren()) {
				if(!cNode.getType().equivalent(type)) {
					unmatchingFound = true;
				}
				childTypes.add(cNode.getType());
			}
			
			if(unmatchingFound) {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			} else {
				node.setType(new Array(type));
			}
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// IdentifierNodes, with helper methods
	@Override
	public void visit(IdentifierNode node) {
		if (!isBeingDeclared(node)) {
			Binding binding = node.findVariableBinding();

			node.setType(binding.getType());
			node.setBinding(binding);
		}
		// else parent DeclarationNode does the processing.
	}

	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof DeclarationNode) && (node == parent.child(0));
	}

	private void addBinding(IdentifierNode identifierNode, Type type, Lextant declareLextant) {
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type, declareLextant);
		identifierNode.setBinding(binding);
	}

	///////////////////////////////////////////////////////////////////////////
	// error logging/printing

	private void assignToConstError(ParseNode node) {
		Token token = node.getToken();

		logError("attempt to assign to CONST-declared variable at " + token.getLocation());
	}

	private void typeCheckErrorForControlFlow(ParseNode node) {
		Token token = node.getToken();

		logError("Control-flow statement '" + token.getLexeme() + "' needs the condition of type BOOLEAN at "
				+ token.getLocation());
	}

	private void typeCheckERrorForArrayInitialization(ParseNode node) {
		Token token = node.getToken();

		logError("Array initialization statement '" + token.getLexeme()
				+ "' needs to have size mentioned of type INTEGER at " + token.getLocation());
	}

	private void typeCheckError(ParseNode node, List<Type> operandTypes) {
		Token token = node.getToken();

		logError("operator " + token.getLexeme() + " not defined for types " + operandTypes + " at "
				+ token.getLocation());
	}

	private void logError(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}