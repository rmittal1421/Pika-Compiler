package parser;

import java.util.ArrayList;
import java.util.Arrays;

import logging.PikaLogger;
import parseTree.*;
import parseTree.nodeTypes.*;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import tokens.*;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Scanner;

public class Parser {
	private Scanner scanner;
	private Token nowReading;
	private Token previouslyRead;

	public static ParseNode parse(Scanner scanner) {
		Parser parser = new Parser(scanner);
		return parser.parse();
	}

	public Parser(Scanner scanner) {
		super();
		this.scanner = scanner;
	}

	public ParseNode parse() {
		readToken();
		return parseProgram();
	}

	////////////////////////////////////////////////////////////
	// "program" is the start symbol S
	// S -> EXEC blockStatement

	private ParseNode parseProgram() {
		if (!startsProgram(nowReading)) {
			return syntaxErrorNode("program");
		}
		ParseNode program = new ProgramNode(nowReading);
		
		while(startsFunction(nowReading)) {
			program.appendChild(parseFunction());
		}

		expect(Keyword.EXEC);
		ParseNode blockStatement = parseBlockStatement();
		program.appendChild(blockStatement);

		if (!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}

		return program;
	}

	private boolean startsProgram(Token token) {
		return token.isLextant(Keyword.EXEC) || startsFunction(token);
	}
	
	private boolean startsFunction(Token token) {
		return token.isLextant(Keyword.FUNC);
	}
	
	// functionDefination -> func identifier lambda
	// lambda             -> lambdaParamType blockStatement
	// lambdaParamType    -> < parameterList > -> type
	// parameterList      -> parameterSpecification bowtie ,
	// paramaterSpecification -> type identifier
	private ParseNode parseFunction() {
		if(!startsFunction(nowReading)) {
			return syntaxErrorNode("function");
		}
		
		Token funcToken = nowReading;
		readToken();
		ParseNode functionIdentifier = parseIdentifier();
		ParseNode lambdaNode = parseLambda();	
		
		return FunctionNode.withChildren(funcToken, functionIdentifier, lambdaNode);
	}

	private ParseNode parseLambda() {
		if(!startsLambdaExpression(nowReading)) {
			return syntaxErrorNode("Lambda expression");
		}
		
		Token lambdaToken = nowReading;
		readToken();
		
		// Parse parameter list
		ArrayList<ParameterSpecificationNode> parameterNodes = new ArrayList<>();
		
		while(!nowReading.isLextant(Punctuator.GREATER)) {
			ParseNode typeNodeForThisParameter = parseTypeNode();
			Token realToken = nowReading;
			Token funcParameterToken = LextantToken.artificial(realToken, Keyword.CONST);
			ParseNode identifierForThisParameter = parseIdentifier();
			
			if(!(identifierForThisParameter instanceof IdentifierNode)) {
				return syntaxErrorNode("ParameterSpecification expression");
			}
			
			parameterNodes.add(ParameterSpecificationNode.withChildren(funcParameterToken, typeNodeForThisParameter, identifierForThisParameter));
			
			if(nowReading.isLextant(Punctuator.SEPARATOR)) {
				readToken();
			}
		}
		
		expect(Punctuator.GREATER);
		expect(Punctuator.LAMBDA_ARROW);
		
		ParseNode returnTypeNode = isNullType(nowReading) ? parseNullTypeNode() : parseTypeNode();
		ParseNode funcBody = parseBlockStatement();

		return LambdaNode.withChildren(lambdaToken, parameterNodes, returnTypeNode, funcBody);
	}
	
	private boolean startsLambdaExpression(Token token) {
		return token.isLextant(Punctuator.LESS);
	}

	///////////////////////////////////////////////////////////
	// blockStatement

	// blockStatement -> { statement* }
	private ParseNode parseBlockStatement() {
		if (!startsBlockStatement(nowReading)) {
			return syntaxErrorNode("mainBlock");
		}
		ParseNode blockStatement = new BlockStatementNode(nowReading);
		expect(Punctuator.OPEN_BRACE);

		while (startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			blockStatement.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return blockStatement;
	}

	private boolean startsBlockStatement(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}

	///////////////////////////////////////////////////////////
	// statements

	// statement-> declaration | printStmt | assignmentStatement | blockStatement |
	// ifStatement | whileStatement | deallocStatement
	private ParseNode parseStatement() {
		if (!startsStatement(nowReading)) {
			return syntaxErrorNode("statement");
		}
		if (startsDeclaration(nowReading)) {
			return parseDeclaration();
		}
		if (startsPrintStatement(nowReading)) {
			return parsePrintStatement();
		}
		if (startsBlockStatement(nowReading)) {
			return parseBlockStatement();
		}
		if (startsAssignmentStatement(nowReading)) {
			return parseAssignmentStatement();
		}
		if (startsIfStatement(nowReading)) {
			return parseIfStatement();
		}
		if (startsWhileStatement(nowReading)) {
			return parseWhileStatement();
		}
		if (startsDeallocStatement(nowReading)) {
			return parseDeallocStatement();
		}
		if (startsCallStatement(nowReading)) {
			return parseCallStatement();
		}
		if (startsReturnStatement(nowReading)) {
			return parseReturnStatement();
		}
		if (startsBreakStatement(nowReading)) {
			return parseBreakStatement();
		}
		if (startsContinueStatement(nowReading)) {
			return parseContinueStatement();
		}
		return syntaxErrorNode("statement");
	}

	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) || startsDeclaration(token) || startsBlockStatement(token) || startsAssignmentStatement(token) 
				|| startsIfStatement(token) || startsWhileStatement(token) || startsDeallocStatement(token) || startsCallStatement(token)
				|| startsReturnStatement(token) || startsBreakStatement(token) || startsContinueStatement(token);
	}

	// assignmentStatement -> target := expr .
	private ParseNode parseAssignmentStatement() {
		if (!startsAssignmentStatement(nowReading)) {
			return syntaxErrorNode("assignment statement");
		}
		
		ParseNode target = parseExpression();
		Token assnToken = nowReading;
		expect(Punctuator.ASSIGN);
		ParseNode expr = parseExpression();
		expect(Punctuator.TERMINATOR);

		return AssignmentStatementNode.withChildren(assnToken, target, expr);
	}

	private boolean startsAssignmentStatement(Token token) {
		return startsExpression(token);
	}

	// target -> identifier
	@SuppressWarnings("unused")
	private ParseNode parseTarget() {
		if (!startsTarget(nowReading)) {
			return syntaxErrorNode("target");
		}
		return parseIdentifier();
	}

	private boolean startsTarget(Token token) {
		return startsIdentifier(token);
	}

	// printStmt -> PRINT printExpressionList .
	private ParseNode parsePrintStatement() {
		if (!startsPrintStatement(nowReading)) {
			return syntaxErrorNode("print statement");
		}
		PrintStatementNode result = new PrintStatementNode(nowReading);

		readToken();
		result = parsePrintExpressionList(result);

		expect(Punctuator.TERMINATOR);
		return result;
	}

	private boolean startsPrintStatement(Token token) {
		return token.isLextant(Keyword.PRINT);
	}

	// This adds the printExpressions it parses to the children of the given parent
	// printExpressionList -> printExpression* bowtie (,|;) (note that this is
	// nullable)

	private PrintStatementNode parsePrintExpressionList(PrintStatementNode parent) {
		while (startsPrintExpression(nowReading) || startsPrintSeparator(nowReading)) {
			parsePrintExpression(parent);
			parsePrintSeparator(parent);
		}
		return parent;
	}

	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> (expr | nl | tab)? (nullable)

	private void parsePrintExpression(PrintStatementNode parent) {
		if (startsExpression(nowReading)) {
			ParseNode child = parseExpression();
			parent.appendChild(child);
		} else if (nowReading.isLextant(Keyword.NEWLINE)) {
			readToken();
			ParseNode child = new NewlineNode(previouslyRead);
			parent.appendChild(child);
		} else if (nowReading.isLextant(Keyword.TAB)) {
			readToken();
			ParseNode child = new TabNode(previouslyRead);
			parent.appendChild(child);
		}
		// else we interpret the printExpression as epsilon, and do nothing
	}

	private boolean startsPrintExpression(Token token) {
		return startsExpression(token) || token.isLextant(Keyword.NEWLINE) || token.isLextant(Keyword.TAB);
	}

	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> expr? ,? nl?

	private void parsePrintSeparator(PrintStatementNode parent) {
		if (!startsPrintSeparator(nowReading) && !nowReading.isLextant(Punctuator.TERMINATOR)) {
			ParseNode child = syntaxErrorNode("print separator");
			parent.appendChild(child);
			return;
		}

		if (nowReading.isLextant(Punctuator.SPACE)) {
			readToken();
			ParseNode child = new SpaceNode(previouslyRead);
			parent.appendChild(child);
		} else if (nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
		} else if (nowReading.isLextant(Punctuator.TERMINATOR)) {
			// we're at the end of the bowtie and this printSeparator is not required.
			// do nothing. Terminator is handled in a higher-level nonterminal.
		}
	}

	private boolean startsPrintSeparator(Token token) {
		return token.isLextant(Punctuator.SEPARATOR, Punctuator.SPACE);
	}

	// declaration -> (CONST | VAR) identifier := expression .
	private ParseNode parseDeclaration() {
		if (!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		Token declarationToken = nowReading;
		readToken();

		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);

		return DeclarationNode.withChildren(declarationToken, identifier, initializer);
	}

	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.CONST, Keyword.VAR);
	}

	private ParseNode parseIfStatement() {
		if (!startsIfStatement(nowReading)) {
			return syntaxErrorNode("if statement");
		}
		Token ifToken = nowReading;
		readToken();

		expect(Punctuator.OPEN_PARANTHESES);
		ParseNode condition = parseExpression();
		expect(Punctuator.CLOSE_PARENTHESES);
		ParseNode thenClause = parseBlockStatement();

		if (startsElseStatement(nowReading)) {
			@SuppressWarnings("unused")
			Token elseToken = nowReading;
			readToken();

			ParseNode elseClause = parseBlockStatement();

			return IfStatementNode.withChildren(ifToken, condition, thenClause, elseClause);
		}

		return IfStatementNode.withChildren(ifToken, condition, thenClause);
	}

	private boolean startsIfStatement(Token token) {
		return token.isLextant(Keyword.IF);
	}

	private boolean startsElseStatement(Token token) {
		return token.isLextant(Keyword.ELSE);
	}

	private ParseNode parseWhileStatement() {
		if (!startsWhileStatement(nowReading)) {
			return syntaxErrorNode("while statement");
		}
		Token whileToken = nowReading;
		readToken();

		expect(Punctuator.OPEN_PARANTHESES);
		ParseNode condition = parseExpression();
		expect(Punctuator.CLOSE_PARENTHESES);
		ParseNode whileBlock = parseBlockStatement();

		return WhileStatementNode.withChildren(whileToken, condition, whileBlock);
	}

	private boolean startsWhileStatement(Token token) {
		return token.isLextant(Keyword.WHILE);
	}
	
	private ParseNode parseDeallocStatement() {
		if(!startsDeallocStatement(nowReading)) {
			return syntaxErrorNode("dealloc statement");
		}
		
		Token deallocToken = nowReading;
		readToken();
		
		ParseNode expression = parseExpression();
		expect(Punctuator.TERMINATOR);
		return UnaryOperatorNode.withChildren(deallocToken, expression);
	}
	
	private boolean startsDeallocStatement(Token token) {
		return token.isLextant(Keyword.DEALLOC);
	}
	
	private ParseNode parseCallStatement() {
		if(!startsCallStatement(nowReading)) {
			return syntaxErrorNode("Call statement");
		}
		
		Token callToken = nowReading;
		readToken();
		
//		ParseNode functionInvocation = parseFunctionInvocationExpression(parseAtomicExpression());
		ParseNode functionInvocation = parseExpression();
		expect(Punctuator.TERMINATOR);
		return UnaryOperatorNode.withChildren(callToken, functionInvocation);
	}
	
	private boolean startsCallStatement(Token token) {
		return token.isLextant(Keyword.CALL);
	}
	
	private ParseNode parseReturnStatement() {
		if(!startsReturnStatement(nowReading)) {
			return syntaxErrorNode("return statement");
		}
		
		Token returnToken = nowReading;
		readToken();
		
		ParseNode returnNode = new ReturnNode(returnToken);
		
		if(!nowReading.isLextant(Punctuator.TERMINATOR)) {
			returnNode.appendChild(parseExpression());
		}
		
		expect(Punctuator.TERMINATOR);
		
		return returnNode;
	}
	
	private boolean startsReturnStatement(Token token) {
		return token.isLextant(Keyword.RETURN);
	}
	
	private ParseNode parseBreakStatement() {
		if(!startsBreakStatement(nowReading)) {
			return syntaxErrorNode("Break statement");
		}
		
		Token breakToken = nowReading;
		readToken();
		
		expect(Punctuator.TERMINATOR);
		
		return new BreakNode(breakToken);
	}
	
	private boolean startsBreakStatement(Token token) {
		return token.isLextant(Keyword.BREAK);
	}
	
	private ParseNode parseContinueStatement() {
		if(!startsContinueStatement(nowReading)) {
			return syntaxErrorNode("Continue statement");
		}
		
		Token continueToken = nowReading;
		readToken();
		
		expect(Punctuator.TERMINATOR);
		
		return new ContinueNode(continueToken);
	}
	
	private boolean startsContinueStatement(Token token) {
		return token.isLextant(Keyword.CONTINUE);
	}

	///////////////////////////////////////////////////////////
	// expressions
	// expr -> comparisonExpression
	// comparisonExpression -> additiveExpression [> additiveExpression]?
	// additiveExpression -> multiplicativeExpression [+ multiplicativeExpression]*
	/////////////////////////////////////////////////////////// (left-assoc)
	// multiplicativeExpression -> atomicExpression [MULT atomicExpression]*
	/////////////////////////////////////////////////////////// (left-assoc)
	// atomicExpression -> literal
	// literal -> intNumber | floatingNumber | identifier | booleanConstant |
	/////////////////////////////////////////////////////////// character | string

	// expr -> comparisonExpression
	private ParseNode parseExpression() {
		if (!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
		return parseShortCircuitOrExpression();
	}

	private boolean startsExpression(Token token) {
		return startsShortCircuitOrExpression(token);
	}

	private ParseNode parseShortCircuitOrExpression() {
		if (!startsShortCircuitOrExpression(nowReading)) {
			return syntaxErrorNode("or(||) expression");
		}

		ParseNode left = parseShortCircuitAndExpression();

		while (nowReading.isLextant(Punctuator.OR)) {
			Token orToken = nowReading;
			readToken();
			ParseNode right = parseShortCircuitAndExpression();

			left = BinaryOperatorNode.withChildren(orToken, left, right);
		}
		return left;
	}

	private boolean startsShortCircuitOrExpression(Token token) {
		return startsShortCircuitAndExpression(token);
	}

	private ParseNode parseShortCircuitAndExpression() {
		if (!startsShortCircuitAndExpression(nowReading)) {
			return syntaxErrorNode("and(&&) expression");
		}

		ParseNode left = parseComparisonExpression();

		while (nowReading.isLextant(Punctuator.AND)) {
			Token andToken = nowReading;
			readToken();
			ParseNode right = parseComparisonExpression();

			left = BinaryOperatorNode.withChildren(andToken, left, right);
		}

		return left;
	}

	private boolean startsShortCircuitAndExpression(Token token) {
		return startsComparisonExpression(token);
	}

	// comparisonExpression -> additiveExpression [> additiveExpression]?
	private ParseNode parseComparisonExpression() {
		if (!startsComparisonExpression(nowReading)) {
			return syntaxErrorNode("comparison expression");
		}

		ParseNode left = parseAdditiveExpression();

		while (Punctuator.isComparisonPunctuator(nowReading.getLexeme())) {
			Token compareToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();

			left = BinaryOperatorNode.withChildren(compareToken, left, right);
		}
		return left;

	}

	private boolean startsComparisonExpression(Token token) {
		return startsAdditiveExpression(token);
	}

	// additiveExpression -> multiplicativeExpression [+ multiplicativeExpression]*
	// (left-assoc)
	private ParseNode parseAdditiveExpression() {
		if (!startsAdditiveExpression(nowReading)) {
			return syntaxErrorNode("additiveExpression");
		}

		ParseNode left = parseMultiplicativeExpression();
		while (nowReading.isLextant(Punctuator.ADD, Punctuator.SUBTRACT)) {
			Token additiveToken = nowReading;
			readToken();
			ParseNode right = parseMultiplicativeExpression();

			left = BinaryOperatorNode.withChildren(additiveToken, left, right);
		}
		return left;
	}

	private boolean startsAdditiveExpression(Token token) {
		return startsMultiplicativeExpression(token);
	}

	// multiplicativeExpression -> atomicExpression [MULT atomicExpression]*
	// (left-assoc)
	private ParseNode parseMultiplicativeExpression() {
		if (!startsMultiplicativeExpression(nowReading)) {
			return syntaxErrorNode("multiplicativeExpression");
		}

		ParseNode left = parseUnaryOperatorExpression();
		while (nowReading.isLextant(Punctuator.MULTIPLY, Punctuator.DIVIDE, Punctuator.OVER, Punctuator.EXPRESS_OVER,
				Punctuator.RATIONALIZE)) {
			Token multiplicativeToken = nowReading;
			readToken();
			ParseNode right = parseUnaryOperatorExpression();

			left = BinaryOperatorNode.withChildren(multiplicativeToken, left, right);
		}
		return left;
	}

	private boolean startsMultiplicativeExpression(Token token) {
		return startsUnaryOperatorExpression(token);
	}

	private ParseNode parseUnaryOperatorExpression() {
		if (!startsUnaryOperatorExpression(nowReading)) {
			return syntaxErrorNode("UnaryOperatorExpression");
		}

		if (startsExplicitUnaryOperatorExpression(nowReading)) {
			Token unaryOperationToken = nowReading;
			readToken();
			ParseNode child = parseExpression();

			return UnaryOperatorNode.withChildren(unaryOperationToken, child);
		} else {
			return parseIndexingOrInvocationExpression();
		}
	}

	private boolean startsUnaryOperatorExpression(Token token) {
		return startsIndexingOrInvocationExpression(token) || startsExplicitUnaryOperatorExpression(token);
	}

	private boolean startsExplicitUnaryOperatorExpression(Token token) {
		return token.isLextant(Keyword.LENGTH, Keyword.CLONE, Punctuator.NOT);
	}
	
	private ParseNode parseIndexingOrInvocationExpression() {
		if(!startsIndexingOrInvocationExpression(nowReading)) {
			return syntaxErrorNode("Indexing or Invocation");
		}
		
		ParseNode base = parseAtomicExpression();
		if(isArrayIndexingExpression(nowReading)) {
			return parseArrayIndexingExpression(base);
		} else if(isFunctionInvocationExpression(nowReading)) {
			return parseFunctionInvocationExpression(base);
		} 

		return base;
	}
	
	private boolean startsIndexingOrInvocationExpression(Token token) {
		return startsArrayIndexingExpression(token) || startsFunctionInvocationExpression(token);
	}
	
	private boolean startsArrayIndexingExpression(Token token) {
		return startsAtomicExpression(token);
	}
	
	private boolean startsFunctionInvocationExpression(Token token) {
		return startsAtomicExpression(token);
	}

	private ParseNode parseArrayIndexingExpression(ParseNode base) {
		if (!isArrayIndexingExpression(nowReading)) {
			return syntaxErrorNode("ArrayIndexing expression");
		}

		while (nowReading.isLextant(Punctuator.OPEN_SQUARE_BRACKET)) {
			Token realToken = nowReading;
			Token indexToken = LextantToken.artificial(realToken, Punctuator.ARRAY_INDEXING);
			readToken();
			ParseNode index = parseExpression();
			expect(Punctuator.CLOSE_SQUARE_BRACKET);

			base = BinaryOperatorNode.withChildren(indexToken, base, index);
		}

		return base;
	}
	
	private boolean isArrayIndexingExpression(Token token) {
		return token.isLextant(Punctuator.OPEN_SQUARE_BRACKET);
	}
	
	private ParseNode parseFunctionInvocationExpression(ParseNode base) {
		if(!isFunctionInvocationExpression(nowReading)) {
			return syntaxErrorNode("Function invocation");
		}
		
		Token realToken = nowReading;
		Token invocationToken = LextantToken.artificial(realToken, Punctuator.FUNCTION_INVOCATION);
		readToken();
		
		ArrayList<ParseNode> children = new ArrayList<>();
		children.add(base);
		
		if(!nowReading.isLextant(Punctuator.CLOSE_PARENTHESES)) {
			children.add(parseExpression());
		}
		
		while(!nowReading.isLextant(Punctuator.CLOSE_PARENTHESES)) {
			expect(Punctuator.SEPARATOR);
			children.add(parseExpression());
		}
		
		readToken();
		
		return KNaryOperatorNode.withChildren(invocationToken, children);
	}
	
	private boolean isFunctionInvocationExpression(Token token) {
		return token.isLextant(Punctuator.OPEN_PARANTHESES);
	}

	// atomicExpression -> literal
	private ParseNode parseAtomicExpression() {
		if (!startsAtomicExpression(nowReading)) {
			return syntaxErrorNode("atomic expression");
		}
		if (startsParenthesizedExpression(nowReading)) {
			return parseParenthesizedExpression();
		}
		if (startsEmptyArrayInitialization(nowReading)) {
			return parseEmptyArray();
		}
		if (startsAtomicExpressionWithSquareBracket(nowReading)) {
			return parseAtomicExpressionWithSquareBracket();
		}
		if (startsLambdaExpression(nowReading)) {
			return parseLambda();
		}
		return parseLiteral();
	}

	private boolean startsAtomicExpression(Token token) {
		return startsLiteral(token) || 
			   startsParenthesizedExpression(token) ||
			   startsEmptyArrayInitialization(token) || 
			   startsAtomicExpressionWithSquareBracket(token) ||
			   startsLambdaExpression(token);
	}

	// Parenthesized Expression parsing
	private ParseNode parseParenthesizedExpression() {
		if (!startsParenthesizedExpression(nowReading)) {
			return syntaxErrorNode("paranthesis expression");
		}
		readToken();
		ParseNode expression = parseExpression();
		expect(Punctuator.CLOSE_PARENTHESES);
		return expression;
	}

	private boolean startsParenthesizedExpression(Token token) {
		return token.isLextant(Punctuator.OPEN_PARANTHESES);
	}

	private ParseNode parseEmptyArray() {
		if (!startsEmptyArrayInitialization(nowReading)) {
			return syntaxErrorNode("Empty array initialization using alloc");
		}

		Token allocToken = nowReading;
		readToken();
		ParseNode type = parseArrayType();
		expect(Punctuator.OPEN_PARANTHESES);
		ParseNode sizeOfArray = parseExpression();
		expect(Punctuator.CLOSE_PARENTHESES);

		return BinaryOperatorNode.withChildren(allocToken, type, sizeOfArray);
	}

	private boolean startsEmptyArrayInitialization(Token token) {
		return token.isLextant(Keyword.ALLOC);
	}
	
	private ParseNode parseAtomicExpressionWithSquareBracket() {
		if(!startsAtomicExpressionWithSquareBracket(nowReading)) {
			return syntaxErrorNode("Expression starting with [");
		}
		
		Token realToken = nowReading;
		readToken();
		ParseNode expression = parseExpression();
		
		if(isPopulatedArrayInitializationExpression(nowReading)) {
			return parsePopulatedArrayExpression(realToken, expression);
		} else if(isCastingExpression(nowReading)) {
			return parseCastingExpression(realToken, expression);
		} else {
			// Error
			return syntaxErrorNode("Expression starting with [");
		}
	}

	private boolean startsAtomicExpressionWithSquareBracket(Token token) {
		return token.isLextant(Punctuator.OPEN_SQUARE_BRACKET);
	}
	
	private boolean isPopulatedArrayInitializationExpression(Token token) {
		return token.isLextant(Punctuator.SEPARATOR, Punctuator.CLOSE_SQUARE_BRACKET);
	}

	private boolean isCastingExpression(Token token) {
		return token.isLextant(Punctuator.CAST);
	}
	
	private ParseNode parsePopulatedArrayExpression(Token realToken, ParseNode expression) {
		if(!isPopulatedArrayInitializationExpression(nowReading)) {
			return syntaxErrorNode("Populated array creation expression");
		}
		
		Token arrayIndexToken = LextantToken.artificial(realToken, Punctuator.ARRAY_INDEXING);

		ArrayList<ParseNode> arrayElements = new ArrayList<>();
		arrayElements.add(expression);

		while (nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
			arrayElements.add(parseExpression());
		}

		expect(Punctuator.CLOSE_SQUARE_BRACKET);

		return PopulatedArrayNode.withChildren(arrayIndexToken, arrayElements);
	}

	// Casting E parsing
	private ParseNode parseCastingExpression(Token realToken, ParseNode expression) {
		if (!isCastingExpression(nowReading)) {
			return syntaxErrorNode("casting expression");
		}
		
		expect(Punctuator.CAST);
		Token castOperator = previouslyRead;
		ParseNode type = parseTypeNode();
		expect(Punctuator.CLOSE_SQUARE_BRACKET);
		return BinaryOperatorNode.withChildren(castOperator, expression, type);
	}

	// Type parsing
	private ParseNode parseTypeNode() {
		if (!startsTypeNode(nowReading)) {
			return syntaxErrorNode("Type node");
		}

		if (startsArrayType(nowReading)) {
			return parseArrayType();
		} else if (startsPrimitiveType(nowReading)) {
			return parsePrimitiveType();
		} else if(startsLambdaType(nowReading)) {
			return parseLambdaType();
		} else {
			// Should never reach here. If does, then it is an error
			return syntaxErrorNode("TypeNode");
		}
	}

	private boolean startsTypeNode(Token token) {
		return startsArrayType(token) || startsPrimitiveType(token) || startsLambdaType(token);
	}
	
	private ParseNode parseArrayType() {
		if(!startsArrayType(nowReading)) {
			return syntaxErrorNode("Array Type");
		}
		
		Token arrayToken = nowReading;
		readToken();
		ParseNode subTypeNode = parseTypeNode();
		expect(Punctuator.CLOSE_SQUARE_BRACKET);
		return TypeNode.withChildren(arrayToken, subTypeNode);
	}

	private boolean startsArrayType(Token token) {
		return token.isLextant(Punctuator.OPEN_SQUARE_BRACKET);
	}
	
	private ParseNode parsePrimitiveType() {
		if(!startsPrimitiveType(nowReading)) {
			return syntaxErrorNode("Primitive Type");
		}
		
		Token primitiveTypeToken = nowReading;
		readToken();
		return new TypeNode(primitiveTypeToken);
	}

	private boolean startsPrimitiveType(Token token) {
		return token.isLextant(Keyword.BOOL, Keyword.CHAR, Keyword.STRING, Keyword.INT, Keyword.FLOAT, Keyword.RAT);
	}
	
	private ParseNode parseLambdaType() {
		if(!startsLambdaType(nowReading)) {
			return syntaxErrorNode("Lambda Type");
		}
		
		ArrayList<ParseNode> paramTypeList = new ArrayList<>();
		Token lambdaToken = nowReading;
		readToken();
		
		while(!nowReading.isLextant(Punctuator.GREATER)) {
			paramTypeList.add(parseTypeNode());
			
			if(nowReading.isLextant(Punctuator.SEPARATOR)) {
				readToken();
			}
		}
		
		expect(Punctuator.GREATER);
		expect(Punctuator.LAMBDA_ARROW);
		
		ParseNode returnTypeNode = isNullType(nowReading) ? parseNullTypeNode() : parseTypeNode();
		
		return TypeNode.withChildren(lambdaToken, paramTypeList, returnTypeNode);
	}
	
	private boolean startsLambdaType(Token token) {
		return token.isLextant(Punctuator.LESS);
	}
	
	private ParseNode parseNullTypeNode() {
		if(!isNullType(nowReading)) {
			return syntaxErrorNode("Null type node");
		}
		
		Token nullTypeToken = nowReading;
		readToken();
		return new TypeNode(nullTypeToken);
	}
	
	private boolean isNullType(Token token) {
		return token.isLextant(Keyword.NULL);
	}

	// literal -> integer | float | identifier | booleanConstant | string
	private ParseNode parseLiteral() {
		if (!startsLiteral(nowReading)) {
			return syntaxErrorNode("literal");
		}
		if (startsIntNumber(nowReading)) {
			return parseIntNumber();
		}
		if (startsFloatNumber(nowReading)) {
			return parseFloatNumber();
		}
		if (startsCharacter(nowReading)) {
			return parseCharacter();
		}
		if (startsString(nowReading)) {
			return parseString();
		}
		if (startsIdentifier(nowReading)) {
			return parseIdentifier();
		}
		if (startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}

		return syntaxErrorNode("literal");
	}

	private boolean startsLiteral(Token token) {
		return startsIntNumber(token) || startsFloatNumber(token) || startsCharacter(token) || startsString(token)
				|| startsIdentifier(token) || startsBooleanConstant(token);
	}

	// integer (terminal)
	private ParseNode parseIntNumber() {
		if (!startsIntNumber(nowReading)) {
			return syntaxErrorNode("integer constant");
		}
		readToken();
		return new IntegerConstantNode(previouslyRead);
	}

	private boolean startsIntNumber(Token token) {
		return token instanceof IntegerToken;
	}

	// floating (terminal)
	private ParseNode parseFloatNumber() {
		if (!startsFloatNumber(nowReading)) {
			return syntaxErrorNode("float constant");
		}
		readToken();
		return new FloatingConstantNode(previouslyRead);
	}

	private boolean startsFloatNumber(Token token) {
		return token instanceof FloatingToken;
	}

	// character (terminal)
	private ParseNode parseCharacter() {
		if (!startsCharacter(nowReading)) {
			return syntaxErrorNode("character const");
		}
		readToken();
		return new CharacterConstantNode(previouslyRead);
	}

	private boolean startsCharacter(Token token) {
		return token instanceof CharacterToken;
	}

	// string (terminal)
	private ParseNode parseString() {
		if (!startsString(nowReading)) {
			return syntaxErrorNode("string constant");
		}
		readToken();
		return new StringConstantNode(previouslyRead);
	}

	private boolean startsString(Token token) {
		return token instanceof StringToken;
	}

	// identifier (terminal)
	private ParseNode parseIdentifier() {
		if (!startsIdentifier(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		ParseNode identifier = new IdentifierNode(previouslyRead);

//		if(nowReading.isLextant(Punctuator.OPEN_SQUARE_BRACKET)) {
//			// We have array indexing here
//			readToken();
//			ParseNode index = parseExpression();
//			expect(Punctuator.CLOSE_SQUARE_BRACKET);
//		}
		return identifier;
	}

	private boolean startsIdentifier(Token token) {
		return token instanceof IdentifierToken;
	}

	// boolean constant (terminal)
	private ParseNode parseBooleanConstant() {
		if (!startsBooleanConstant(nowReading)) {
			return syntaxErrorNode("boolean constant");
		}
		readToken();
		return new BooleanConstantNode(previouslyRead);
	}

	private boolean startsBooleanConstant(Token token) {
		return token.isLextant(Keyword.TRUE, Keyword.FALSE);
	}

	private void readToken() {
		previouslyRead = nowReading;
		nowReading = scanner.next();
	}

	// if the current token is one of the given lextants, read the next token.
	// otherwise, give a syntax error and read next token (to avoid endless
	// looping).
	private void expect(Lextant... lextants) {
		if (!nowReading.isLextant(lextants)) {
			syntaxError(nowReading, "expecting " + Arrays.toString(lextants));
		}
		readToken();
	}

	private ErrorNode syntaxErrorNode(String expectedSymbol) {
		syntaxError(nowReading, "expecting " + expectedSymbol);
		ErrorNode errorNode = new ErrorNode(nowReading);
		readToken();
		return errorNode;
	}

	private void syntaxError(Token token, String errorDescription) {
		String message = "" + token.getLocation() + " " + errorDescription;
		error(message);
	}

	private void error(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.Parser");
		log.severe("syntax error: " + message);
	}
}
