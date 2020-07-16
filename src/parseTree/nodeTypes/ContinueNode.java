package parseTree.nodeTypes;

import asmCodeGenerator.Labeller;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class ContinueNode extends ParseNode {
	
	private String enclosingWhileStartLabel;

	public ContinueNode(ParseNode node) {
		super(node);
	}
	public ContinueNode(Token token) {
		super(token);
	}
	
	public void setEnclosingWhileStartLabel(String label) {
		this.enclosingWhileStartLabel = label;
	}
	
	public String getEnclosingWhileStartLabel() {
		return this.enclosingWhileStartLabel;
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
