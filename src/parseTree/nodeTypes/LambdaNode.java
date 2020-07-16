package parseTree.nodeTypes;

import java.util.ArrayList;

import asmCodeGenerator.Labeller;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class LambdaNode extends ParseNode {
	private int numberOfParameters = 0;
	private String lambdaStartLabel;
	private String lambdaEndLabel;
	private String codeAfterReturnLabel;

	public LambdaNode(Token token) {
		super(token);
		initializeLabels();
	}

	public LambdaNode(ParseNode node) {
		super(node);
		initializeLabels();
	}
	
	private void initializeLabels() {
		Labeller labeller = new Labeller("function-call");
		this.lambdaStartLabel = labeller.newLabel("start");
		this.lambdaEndLabel = labeller.newLabel("end");
		this.codeAfterReturnLabel = labeller.newLabel("return-lambda");
	}

	public LextantToken lextantToken() {
		return (LextantToken) token;
	}
	
	public void setNumberOfParameters(int num) {
		this.numberOfParameters = num;
	}
	
	public Type getReturnType() {
		return this.child(this.numberOfParameters).getType();
	}
	
	public String getStartLabel() {
		return this.lambdaStartLabel;
	}
	
	public String getEndLabel() {
		return this.lambdaEndLabel;
	}
	
	public String getReturnLabel() {
		return this.codeAfterReturnLabel;
	}

////////////////////////////////////////////////////////////
// convenience factory

	public static LambdaNode withChildren(Token token, 
										  ArrayList<ParameterSpecificationNode> parameterNodes, 
										  ParseNode returnNode, 
										  ParseNode blockStatementNode) {
		LambdaNode node = new LambdaNode(token);
		for(ParseNode nChild: parameterNodes) {
			node.appendChild(nChild);
		}
		node.appendChild(returnNode);
		node.appendChild(blockStatementNode);
		node.setNumberOfParameters(parameterNodes.size());
		return node;
	}

///////////////////////////////////////////////////////////
// boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
