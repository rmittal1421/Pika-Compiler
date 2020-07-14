package parseTree.nodeTypes;

import java.util.ArrayList;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class LambdaNode extends ParseNode {
	private int numberOfParameters = 0;

	public LambdaNode(Token token) {
		super(token);
	}

	public LambdaNode(ParseNode node) {
		super(node);
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
