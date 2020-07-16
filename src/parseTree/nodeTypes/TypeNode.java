package parseTree.nodeTypes;

import java.util.ArrayList;

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
	
	
	// TypeNode for types other than lambda
	public static TypeNode withChildren(Token token, ParseNode child) {
		TypeNode node = new TypeNode(token);
		node.appendChild(child);
		return node;
	}
	
	// TypeNode for lambdaType
	public static TypeNode withChildren(Token token, ArrayList<ParseNode> paramTypes, ParseNode returnType) {
		TypeNode node = new TypeNode(token);
		for(ParseNode child : paramTypes) {
			node.appendChild(child);
		}
		node.appendChild(returnType);
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
