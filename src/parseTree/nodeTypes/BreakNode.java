package parseTree.nodeTypes;

import asmCodeGenerator.Labeller;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class BreakNode extends ParseNode {
	
	private String enclosingLoopEndLabel;

	public BreakNode(ParseNode node) {
		super(node);
	}
	public BreakNode(Token token) {
		super(token);
	}
	
	public void setEnclosingLoopEndLabel(String label) {
		this.enclosingLoopEndLabel = label;
	}
	
	public String getEnclosingLoopEndLabel() {
		return this.enclosingLoopEndLabel;
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
