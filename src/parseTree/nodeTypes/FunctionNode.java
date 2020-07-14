package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class FunctionNode extends ParseNode {

	public FunctionNode(Token token) {
		super(token);
	}

	public FunctionNode(ParseNode node) {
		super(node);
	}
	
	public Lextant getDeclarationType() {
		return lextantToken().getLextant();
	}
	
	public LextantToken lextantToken() {
		return (LextantToken) token;
	}

	
////////////////////////////////////////////////////////////
//convenience factory

	public static FunctionNode withChildren(Token token, ParseNode identifierNode, ParseNode lambdaNode) {
		FunctionNode node = new FunctionNode(token);
		node.appendChild(identifierNode);
		node.appendChild(lambdaNode);
		return node;
	}

///////////////////////////////////////////////////////////
//boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

}
