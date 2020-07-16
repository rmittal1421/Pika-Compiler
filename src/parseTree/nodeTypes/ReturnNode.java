package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.signatures.FunctionSignature;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class ReturnNode extends ParseNode {
	
	String whereToOnReturn;
	
	public ReturnNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public ReturnNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}
	
	public String whereToGoOnReturn() {
		assert this.whereToOnReturn != "";
		return this.whereToOnReturn;
	}
	
	public void setWhereToGoOnReturn(String label) {
		this.whereToOnReturn = label;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static ReturnNode withChildren(Token token, ParseNode child) {
		ReturnNode node = new ReturnNode(token);
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
