package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class ContinueNode extends ParseNode {
	
	private String enclosingLoopStartLabel;

	public ContinueNode(ParseNode node) {
		super(node);
	}
	public ContinueNode(Token token) {
		super(token);
	}
	
	public void setEnclosingLoopStartLabel(String label) {
		this.enclosingLoopStartLabel = label;
	}
	
	public String getEnclosingLoopStartLabel() {
		return this.enclosingLoopStartLabel;
	}
	
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
