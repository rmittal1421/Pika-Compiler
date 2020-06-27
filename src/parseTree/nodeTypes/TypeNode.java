package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class TypeNode extends ParseNode {

	public TypeNode(ParseNode node) {
		super(node);
	}
	public TypeNode(Token token) {
		super(token);
	}
	
	// attributes
	public Token typeToken() {
		return token;
	}
	
	public static TypeNode withChildren(Token token, ParseNode child) {
		TypeNode node = new TypeNode(token);
		node.appendChild(child);
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
