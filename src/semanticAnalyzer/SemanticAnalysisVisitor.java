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
import parseTree.nodeTypes.*;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.Lambda;
import semanticAnalyzer.types.NullType;
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
		performFirstVisit(node);
	}

	private Lambda fetchLambdaSignature(LambdaNode node) {
		// Fetch the signature of lambda
		List<Type> paramTypes = new ArrayList<>();
		Type returnType = PrimitiveType.ERROR;

		for (ParseNode lChild : node.getChildren()) {
			if (lChild instanceof ParameterSpecificationNode) {
				lChild.child(0).accept(this);
				paramTypes.add(lChild.child(0).getType());
			} else if (lChild instanceof TypeNode) {
				lChild.accept(this);
				returnType = lChild.getType();
			}
		}

		Lambda functionLambdaType = new Lambda(paramTypes, returnType);
		return functionLambdaType;
	}

	private void performFirstVisit(ProgramNode node) {
		for (ParseNode child : node.getChildren()) {
			if (child instanceof FunctionNode) {
				assert child.nChildren() == 2;

				// Fetch the identifier
				IdentifierNode identifierNode = (IdentifierNode) child.child(0);

				Type functionLambdaType = fetchLambdaSignature((LambdaNode) child.child(1));
				child.setType(functionLambdaType);
				child.child(1).setType(functionLambdaType);

				addBinding(identifierNode, functionLambdaType, ((FunctionNode) child).getDeclarationType());
			}
		}
	}

	public void visitLeave(ProgramNode node) {
		leaveScope(node);
	}

	public void visitEnter(BlockStatementNode node) {
		if (node.getParent() instanceof LambdaNode) {
			enterProcedureScope(node);
			return;
		} else if (!(node.getParent() instanceof ForStatementNode)) {
			enterSubscope(node);
		}
	}

	public void visitLeave(BlockStatementNode node) {
		if (!(node.getParent() instanceof ForStatementNode)) {
			leaveScope(node);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// helper methods for scoping.
	private void enterProgramScope(ParseNode node) {
		Scope scope = Scope.createProgramScope();
		node.setScope(scope);
	}

	private void enterParameterScope(ParseNode node) {
		Scope baseScope = node.getBaseScope();
//		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createParameterScope();
		node.setScope(scope);
	}

	private void enterProcedureScope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createProcedureScope();
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
	// function handlers
	@Override
	public void visitEnter(FunctionNode node) {
	}

	@Override
	public void visitLeave(FunctionNode node) {
	}

	@Override
	public void visitEnter(LambdaNode node) {
		enterParameterScope(node);

		// If lambdaNode is not immediate child of functionNode, it has been declared
		// within some local scope which is not global.
		// Hence, fetch it's type
		if (!(node.getParent() instanceof FunctionNode)) {
			Lambda lambdaType = fetchLambdaSignature(node);
			node.setType(lambdaType);

		}
	}

	@Override
	public void visitLeave(LambdaNode node) {
		leaveScope(node);
	}

	@Override
	public void visitEnter(ParameterSpecificationNode node) {
	}

	@Override
	public void visitLeave(ParameterSpecificationNode node) {
		Type paramType = node.child(0).getType();

		if (!(node.child(1) instanceof IdentifierNode)) {
			parameterExpectedIdentifierError(node);
			node.setType(PrimitiveType.ERROR);
			return;
		}

		IdentifierNode identifierNode = (IdentifierNode) node.child(1);
		identifierNode.setType(paramType);
		node.setType(paramType);
		addBinding(identifierNode, paramType, node.getDeclarationType());
	}

	@Override
	public void visitLeave(ReturnNode node) {
		// Find if there is a enclosing lambda; if found, check return type, otherwise
		// wrong usage of return statement.
		for (ParseNode immParent : node.pathToRoot()) {
			Type parentType = immParent.getType();

			if (parentType instanceof Lambda) {
				/*
				 * Found the enclosing parent which is lambda Compare return types. If return
				 * type of lambda is null, there should be no child.
				 */
				Lambda lambdaTypeOfParent = (Lambda) parentType;
				if ((node.nChildren() == 1 && lambdaTypeOfParent.getReturnType().equivalent(node.child(0).getType()))
						|| (node.nChildren() == 0 && lambdaTypeOfParent.getReturnType() instanceof NullType)) {
					node.setType(lambdaTypeOfParent.getReturnType());
					node.setWhereToGoOnReturn(((LambdaNode) immParent).getReturnLabel());
				} else {
					IllegalReturnType(node);
					node.setType(PrimitiveType.ERROR);
				}

				return;
			}
		}

		returnStatementNotInFunction(node);
		node.setType(PrimitiveType.ERROR);
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
		ArrayList<Type> castToTypes = new ArrayList<>(
				Arrays.asList(PrimitiveType.INTEGER, PrimitiveType.FLOATING, PrimitiveType.RATIONAL));

		if (!target.getType().equivalent(expression.getType())) {
			if (castFromTypes.contains(expression.getType()) && castToTypes.contains(target.getType())) {
				promoteNode(node, target.getType(), 1);
			} else {
				typeCheckError(node, Arrays.asList(target.getType(), expression.getType()));
			}
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
		if (node.getToken().isLextant(Punctuator.OPEN_SQUARE_BRACKET)) {
			node.setType(new Array(node.child(0).getType()));
		} else if (node.getToken().isLextant(Punctuator.LESS)) {
			// Lambda Type
			ArrayList<Type> paramTypeList = new ArrayList<>();
			for (int i = 0; i < node.nChildren() - 1; i++) {
				paramTypeList.add(node.child(i).getType());
			}
			Type returnType = node.child(node.nChildren() - 1).getType();
			node.setType(new Lambda(paramTypeList, returnType));
		} else {
			if (node.getToken().isLextant(Keyword.NULL)) {
				node.setType(new NullType());
			} else {
				node.setType(PrimitiveType.fromToken(node.typeToken()));
			}
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

	@Override
	public void visitEnter(ForStatementNode node) {
		enterSubscope(node);
	}

	@Override
	public void visitLeave(ForStatementNode node) {
		leaveScope(node);

		// Type of sequence should have been determined by now.
		if (!(node.getSequenceType() instanceof Array || node.getSequenceType().equivalent(PrimitiveType.STRING))) {
			forLoopForNonRecordTypeError(node);
			node.setType(PrimitiveType.ERROR);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// expressions
	@Override
	public void visitLeave(UnaryOperatorNode node) {
		assert node.nChildren() == 1;
		List<Type> childTypes = Arrays.asList(node.child(0).getType());
		Lextant operator = operatorFor(node);

		if (node.getToken().isLextant(Keyword.CLONE, Keyword.DEALLOC) && !(childTypes.get(0) instanceof Array)) {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		} else if (node.getToken().isLextant(Keyword.LENGTH) && !(childTypes.get(0) instanceof Array
				|| childTypes.get(0).getConcreteType().equivalent(PrimitiveType.STRING))) {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		} else if (node.getToken().isLextant(Punctuator.NOT)
				&& !(childTypes.get(0).equivalent(PrimitiveType.BOOLEAN))) {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		} else if (node.getToken().isLextant(Keyword.CALL) && !(node.child(0) instanceof KNaryOperatorNode
				&& node.child(0).child(0).getType() instanceof Lambda)) {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
			return;
		}

		if (node.getToken().isLextant(Keyword.CLONE)) {
			node.setType(node.child(0).getType());
		} else if (node.getToken().isLextant(Keyword.CALL)) {
			node.setType(node.child(0).getType());
			return;
		}

		FunctionSignatures signatures = FunctionSignatures.signaturesOf(operator);
		FunctionSignature signature = signatures.acceptingSignature(childTypes);

		if (signature.accepts(childTypes)) {
			node.setType(signature.resultType());
			node.setSignature(signature);
		} else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}

	private Lextant operatorFor(UnaryOperatorNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}

	private void visitLeaveForFunctionInvocation(KNaryOperatorNode node) {
		if (!(node.child(0).getType() instanceof Lambda)) {
			notLambdaInvoked(node);
			node.setType(PrimitiveType.ERROR);
			return;
		}

		Lambda lambdaType = (Lambda) node.child(0).getType();

		// Check types of params against the children types
		List<Type> paramTypes = new ArrayList<>();
		for (int i = 1; i < node.nChildren(); i++) {
			paramTypes.add(node.child(i).getType());
		}

		if (lambdaType.equivalentParams(paramTypes)) {
			/*
			 * Two cases: 1. If return type is null and not called by 'call' -> error node
			 * 2. All other cases
			 */

			Type returnType = lambdaType.getReturnType();
			if (!node.getParent().getToken().isLextant(Keyword.CALL) && returnType.equivalent(new NullType())) {
				funcReturnNullUsedAsAssignment(node);
				node.setType(PrimitiveType.ERROR);
			}
			node.setType(lambdaType.getReturnType());
		} else {
			// error
			typeCheckError(node, paramTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}

	private void visitLeaveForSubstringArrayIndexing(KNaryOperatorNode node) {
		// Array indexing not considered as binary if it is substring operation.
		if (!(node.child(0).getType().equivalent(PrimitiveType.STRING))) {
			substringOnNotStringType(node);
			node.setType(PrimitiveType.ERROR);
			return;
		}

		assert node.nChildren() == 3;
		ParseNode base = node.child(0);
		ParseNode firstIndex = node.child(1);
		ParseNode secondIndex = node.child(2);

		List<Type> childTypes = Arrays.asList(base.getType(), firstIndex.getType(), secondIndex.getType());

		Lextant operator = ((LextantToken) (node.getToken())).getLextant();
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(operator);
		FunctionSignature signature = signatures.acceptingSignature(childTypes);

		if (signature.accepts(childTypes)) {
			node.setType(signature.resultType());
			node.setSignature(signature);
		} else {
			// Try promoting and check again
			signature = findSuitableSignature(node, signatures, childTypes);
			if (signature.accepts(childTypes)) {
				node.setType(signature.resultType());
				node.setSignature(signature);
			} else {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			}
		}
	}

	private void visitLeaveForMapOperator(KNaryOperatorNode node) {
		assert node.nChildren() == 2;
		assert node.getToken().isLextant(Keyword.MAP);

		List<Type> childTypes = new ArrayList<>();
		childTypes.add(node.child(0).getType());
		childTypes.add(node.child(1).getType());

		if (childTypes.get(0) instanceof Array && childTypes.get(1) instanceof Lambda) {

			Lambda lambdaType = (Lambda) childTypes.get(1);

			if (lambdaType.getNumberOfParameters() == 1) {

				Type lambdaParamType = lambdaType.getParamTypes().get(0);
				Type subTypeOfArray = ((Array) childTypes.get(0)).getSubtype();

				if (lambdaParamType.equivalent(subTypeOfArray)) {

					if (!(lambdaType.getReturnType() instanceof NullType)) {

						// Semantically, this is a correct map operation
						node.setType(new Array(lambdaType.getReturnType()));
						return;

					} else {
						MapLambdaReturnTypeNullError(node);
					}

				} else {
					MapLambdaParamTypeMismatchArraySubTypeError(node);
				}

			} else {
				MapLambdaNumberOfParametersError(node);
			}

		} else {
			typeCheckError(node, childTypes);
		}

		node.setType(PrimitiveType.ERROR);
		return;
	}

	private void visitLeaveForReduceOperator(KNaryOperatorNode node) {
		assert node.nChildren() == 2;
		assert node.getToken().isLextant(Keyword.REDUCE);

		List<Type> childTypes = new ArrayList<>();
		childTypes.add(node.child(0).getType());
		childTypes.add(node.child(1).getType());

		if (childTypes.get(0) instanceof Array && childTypes.get(1) instanceof Lambda) {

			Lambda lambdaType = (Lambda) childTypes.get(1);

			if (lambdaType.getNumberOfParameters() == 1) {

				Type lambdaParamType = lambdaType.getParamTypes().get(0);
				Type subTypeOfArray = ((Array) childTypes.get(0)).getSubtype();

				if (lambdaParamType.equivalent(subTypeOfArray)) {

					if (lambdaType.getReturnType().equivalent(PrimitiveType.BOOLEAN)) {

						// Semantically, this is a correct map operation
						node.setType(new Array(subTypeOfArray));
						return;

					} else {
						ReduceLambdaReturnTypeNotBooleanError(node);
					}

				} else {
					ReduceLambdaParamTypeMismatchArraySubTypeError(node);
				}

			} else {
				ReduceLambdaNumberOfParametersError(node);
			}

		} else {
			typeCheckError(node, childTypes);
		}

		node.setType(PrimitiveType.ERROR);
		return;
	}

	@Override
	public void visitLeave(KNaryOperatorNode node) {
		assert node.nChildren() > 0;

		if (node.getToken().isLextant(Punctuator.FUNCTION_INVOCATION)) {
			visitLeaveForFunctionInvocation(node);
		} else if (node.getToken().isLextant(Punctuator.ARRAY_INDEXING)) {
			visitLeaveForSubstringArrayIndexing(node);
		} else if (node.getToken().isLextant(Keyword.MAP)) {
			visitLeaveForMapOperator(node);
		} else if (node.getToken().isLextant(Keyword.REDUCE)) {
			visitLeaveForReduceOperator(node);
		} else {
			List<Type> childTypes = new ArrayList<>();
			for (ParseNode child : node.getChildren()) {
				childTypes.add(child.getType());
			}
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}

	private void promoteNode(ParseNode node, Type typeToCastTo, int childNumber) {
		ParseNode childToReplace = node.child(childNumber);

		Type typeToCastFrom = node.child(childNumber).getType();
		TypeNode typeNode;
		if (typeToCastTo.equivalent(PrimitiveType.INTEGER)) {
			typeNode = new TypeNode(Keyword.INT.prototype());
		} else if (typeToCastTo.equivalent(PrimitiveType.FLOATING)) {
			typeNode = new TypeNode(Keyword.FLOAT.prototype());
		} else {
			typeNode = new TypeNode(Keyword.RAT.prototype());
		}

		BinaryOperatorNode nodeToAdd = null;
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(Punctuator.CAST);
		if (typeToCastFrom.equivalent(PrimitiveType.CHARACTER) && !typeToCastTo.equivalent(PrimitiveType.INTEGER)) {
			TypeNode intermediateTypeNode = new TypeNode(Keyword.INT.prototype());
			intermediateTypeNode.setType(PrimitiveType.INTEGER);
			FunctionSignature castSignature = signatures
					.acceptingSignature(Arrays.asList(PrimitiveType.CHARACTER, PrimitiveType.INTEGER));
			nodeToAdd = BinaryOperatorNode.withChildren(Punctuator.CAST.prototype(), childToReplace,
					intermediateTypeNode);
			nodeToAdd.setSignature(castSignature);
			nodeToAdd.setType(castSignature.resultType());
			typeToCastFrom = PrimitiveType.INTEGER;
		}

		typeNode.setType(typeToCastTo);
		FunctionSignature castSignature = signatures
				.acceptingSignature(Arrays.asList(typeToCastFrom, typeNode.getType()));
		nodeToAdd = BinaryOperatorNode.withChildren(Punctuator.CAST.prototype(),
				nodeToAdd != null ? nodeToAdd : childToReplace, typeNode);
		nodeToAdd.setSignature(castSignature);
		nodeToAdd.setType(castSignature.resultType());
		node.replaceChild(childToReplace, nodeToAdd);
	}

	private FunctionSignature findSuitableSignature(ParseNode node, FunctionSignatures signatures,
			List<Type> childTypes) {
		assert childTypes.size() > 0;
		int nChildren = childTypes.size();

		boolean[] elligibleToCast = new boolean[nChildren];
		for (int i = 0; i < nChildren; i++) {
			Type type = childTypes.get(i);
			if (type.equivalent(PrimitiveType.CHARACTER) || type.equivalent(PrimitiveType.INTEGER)) {
				elligibleToCast[i] = true;
			} else {
				elligibleToCast[i] = false;
			}
		}

		for (int i = 0; i < childTypes.size(); i++) {
			List<Type> potentialChildTypes = new ArrayList<>(childTypes);
			if (elligibleToCast[i] && !(node.child(i) instanceof TypeNode)) {
				// First try for left operand
				if (childTypes.get(i).equivalent(PrimitiveType.CHARACTER)) {
					// Try to find a unique signature
					potentialChildTypes.set(i, PrimitiveType.INTEGER);
					FunctionSignature potentialSig = signatures.acceptingSignature(potentialChildTypes);
					if (potentialSig.accepts(potentialChildTypes)) {
						promoteNode(node, potentialSig.getParamTypes()[i], i);
						for (int j = 0; j < childTypes.size(); j++) {
							childTypes.set(j, potentialChildTypes.get(j));
						}
						return potentialSig;
					}
				}

				// Try to find all floating and rational casting matching signatures.
				// If more than 1 found, abort and issue error. If only 1 found, that's the
				// result.
				int matchesFound = 0;
				potentialChildTypes.set(i, PrimitiveType.FLOATING);
				FunctionSignature potentialSig = signatures.acceptingSignature(potentialChildTypes);
				if (potentialSig.accepts(potentialChildTypes)) {
					matchesFound++;
					for (int j = 0; j < childTypes.size(); j++) {
						childTypes.set(j, potentialChildTypes.get(j));
					}
				}
				potentialChildTypes.set(i, PrimitiveType.RATIONAL);
				FunctionSignature anotherPotentialSig = signatures.acceptingSignature(potentialChildTypes);
				if (anotherPotentialSig.accepts(potentialChildTypes)) {
					if (matchesFound == 1) {
						// Issue an error as we matched more than one.
						return FunctionSignature.nullInstance();
					} else {
						// We found the matching signature
						promoteNode(node, anotherPotentialSig.getParamTypes()[i], i);
						for (int j = 0; j < childTypes.size(); j++) {
							childTypes.set(j, potentialChildTypes.get(j));
						}
						return anotherPotentialSig;
					}
				} else if (matchesFound == 1) {
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
			if (signature.accepts(childTypes)) {
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
	public void visit(BreakNode node) {
		for (ParseNode immParent : node.pathToRoot()) {

			if (immParent instanceof WhileStatementNode) {
				node.setEnclosingLoopEndLabel(((WhileStatementNode) immParent).getEndLabel());
				return;
			} else if (immParent instanceof ForStatementNode) {
				node.setEnclosingLoopEndLabel(((ForStatementNode) immParent).getEndLabel());
				return;
			} else if (immParent instanceof LambdaNode) {
				break;
			}
		}

		breakStatementNotInLoop(node);
		node.setType(PrimitiveType.ERROR);
	}

	@Override
	public void visit(ContinueNode node) {
		for (ParseNode immParent : node.pathToRoot()) {

			if (immParent instanceof WhileStatementNode) {
				node.setEnclosingLoopStartLabel(((WhileStatementNode) immParent).getStartLabel());
				return;
			} else if (immParent instanceof ForStatementNode) {
				node.setEnclosingLoopStartLabel(((ForStatementNode) immParent).getReadyForNextIterationLabel());
				return;
			} else if (immParent instanceof LambdaNode) {
				break;
			}
		}

		continueStatementNotInLoop(node);
		node.setType(PrimitiveType.ERROR);
	}

	@Override
	public void visitEnter(PopulatedArrayNode node) {
	}

	@Override
	public void visitLeave(PopulatedArrayNode node) {
		assert node.nChildren() > 0;
		Type type = node.child(0).getType();

		if (type instanceof PrimitiveType) {
			// Only then promotions are allowed
			Type finalType = PrimitiveType.NO_TYPE;
			ArrayList<Type> typesGiven = new ArrayList<>();
			boolean thereIsRational = false;
			boolean thereIsFloating = false;
			boolean invalidTypeFound = false;
			ArrayList<ParseNode> childrenOfThisNode = new ArrayList<>();
			for (ParseNode childNode : node.getChildren()) {
				Type childType = childNode.getType();
				childrenOfThisNode.add(childNode);

				if (!typesGiven.contains(childType)) {
					typesGiven.add(childType);
					thereIsFloating = thereIsFloating || childType == PrimitiveType.FLOATING;
					thereIsRational = thereIsRational || childType == PrimitiveType.RATIONAL;
				}

				if (!PrimitiveType.isATypeInvolvedInPromotions(childType)) {
					// Return as nothing can be done about this.
					invalidTypeFound = true;
				}
			}
			if (typesGiven.size() == 1) {
				finalType = typesGiven.get(0);
				node.setType(new Array(finalType));
				return;
			} else if ((thereIsFloating && thereIsRational) || invalidTypeFound) {
				typeCheckError(node, typesGiven);
				node.setType(PrimitiveType.ERROR);
				return;
			}

			Type typeToCastTo = thereIsRational ? PrimitiveType.RATIONAL
					: thereIsFloating ? PrimitiveType.FLOATING : PrimitiveType.INTEGER;
			for (int i = 0; i < childrenOfThisNode.size(); i++) {
				ParseNode cNode = childrenOfThisNode.get(i);
				if (!cNode.getType().equals(typeToCastTo)) {
					promoteNode(node, typeToCastTo, i);
				}
			}
			node.setType(new Array(typeToCastTo));
		} else {
			boolean unmatchingFound = false;
			ArrayList<Type> childTypes = new ArrayList<>();
			for (ParseNode cNode : node.getChildren()) {
				if (!cNode.getType().equivalent(type)) {
					unmatchingFound = true;
				}
				childTypes.add(cNode.getType());
			}

			if (unmatchingFound) {
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
		if (isUsedAsForLoopIdentifier(node)) {
			ForStatementNode parent = (ForStatementNode) node.getParent();
			Token forLoopToken = parent.getToken();
			LextantToken constIdentifierToken = (LextantToken) LextantToken.artificial(forLoopToken, Keyword.CONST);

			if (parent.forTypeLextantToken().isLextant(Keyword.INDEX)) {

				// Index should be assigned type int
				Type identifierType = PrimitiveType.INTEGER;
				node.setType(identifierType);

				// Add binding for identifier in for loop body's scope
				addBinding(node, identifierType, constIdentifierToken.getLextant());

			} else if (parent.forTypeLextantToken().isLextant(Keyword.ELEM)) {

				// For element, it should be either character or subType of sequence
				if (parent.getSequenceType().equivalent(PrimitiveType.STRING)) {
					node.setType(PrimitiveType.CHARACTER);
				} else {
					Array arrayType = (Array) parent.getSequenceType();
					node.setType(arrayType.getSubtype());
				}

				addBinding(node, node.getType(), constIdentifierToken.getLextant());

			} else {
				throw new RuntimeException("Lextant of type neither index or elem in class: " + node.getClass());
			}
		} else if (!isBeingDeclared(node) && !isUsedAsFunctionParameter(node)) {
			Binding binding = node.findVariableBinding();

			node.setType(binding.getType());
			node.setBinding(binding);
		}
		// else parent DeclarationNode does the processing.
	}

	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof DeclarationNode || parent instanceof FunctionNode) && (node == parent.child(0));
	}

	private boolean isUsedAsFunctionParameter(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof ParameterSpecificationNode);
	}

	private boolean isUsedAsForLoopIdentifier(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof ForStatementNode) && (node == parent.child(1));
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

	@SuppressWarnings("unused")
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

	private void funcReturnNullUsedAsAssignment(ParseNode node) {
		Token token = node.getToken();

		logError("Function returns null which cannot be used in assignment at " + token.getLocation());
	}

	private void IllegalReturnType(ParseNode node) {
		Token token = node.getToken();

		logError("Return argument does not match the return type of the lambda at " + token.getLocation());
	}

	private void returnStatementNotInFunction(ParseNode node) {
		Token token = node.getToken();

		logError("Return sttement found outside of lambda at " + token.getLocation());
	}

	private void breakStatementNotInLoop(ParseNode node) {
		Token token = node.getToken();

		logError("Break statement found outside of loop at " + token.getLocation());
	}

	private void continueStatementNotInLoop(ParseNode node) {
		Token token = node.getToken();

		logError("continue statement found outside of loop at " + token.getLocation());
	}

	private void notLambdaInvoked(ParseNode node) {
		Token token = node.getToken();

		logError("Trying to invoke function on non-lambda type at " + token.getLocation());
	}

	private void substringOnNotStringType(ParseNode node) {
		Token token = node.getToken();

		logError("Trying to get a substring of non-string type at " + token.getLocation());
	}

	private void parameterExpectedIdentifierError(ParseNode node) {
		Token token = node.getToken();

		logError("ParameterSpecification expected identifier node after type at " + token.getLocation());
	}

	private void forLoopForNonRecordTypeError(ParseNode node) {
		Token token = node.getToken();

		logError("For loop expected expression of record type to loop on at " + token.getLocation());
	}

	private void MapLambdaNumberOfParametersError(ParseNode node) {
		Token token = node.getToken();

		logError("Map operator expression expects a lambda with exactly one parameter at " + token.getLocation());
	}

	private void MapLambdaParamTypeMismatchArraySubTypeError(ParseNode node) {
		Token token = node.getToken();

		logError("Map operator expression expects lambda's param type to be equal to array subtype at "
				+ token.getLocation());
	}
	
	private void MapLambdaReturnTypeNullError(ParseNode node) {
		Token token = node.getToken();

		logError("Map operator expression expects lambda's return type to not null at " + token.getLocation());
	}

	private void ReduceLambdaNumberOfParametersError(ParseNode node) {
		Token token = node.getToken();

		logError("Reduce operator expression expects a lambda with exactly one parameter at " + token.getLocation());
	}

	private void ReduceLambdaParamTypeMismatchArraySubTypeError(ParseNode node) {
		Token token = node.getToken();

		logError("Reduce operator expression expects lambda's param type to be equal to array subtype at "
				+ token.getLocation());
	}

	private void ReduceLambdaReturnTypeNotBooleanError(ParseNode node) {
		Token token = node.getToken();

		logError("Reduce operator expression expects lambda's return type to be boolean at " + token.getLocation());
	}

	private void logError(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}