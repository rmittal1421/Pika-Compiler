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
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
