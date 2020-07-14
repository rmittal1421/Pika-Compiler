package parseTree.nodeTypes;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class ParameterSpecificationNode extends ParseNode {

	public ParameterSpecificationNode(Token token) {
		super(token);
	}

	public ParameterSpecificationNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getDeclarationType() {
		return lextantToken().getLextant();
	}
	
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory

	public static ParameterSpecificationNode withChildren(Token token, ParseNode typeNode, ParseNode identifierNode) {
		ParameterSpecificationNode node = new ParameterSpecificationNode(token);
		node.appendChild(typeNode);
		node.appendChild(identifierNode);
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
