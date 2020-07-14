package parseTree.nodeTypes;

import java.util.ArrayList;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class KNaryOperatorNode extends ParseNode {
	
	public KNaryOperatorNode(Token token) {
		super(token);
	}
	public KNaryOperatorNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes

	public LextantToken lextantToken() {
		return (LextantToken)token;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	// For creating an array with specified elements
	public static KNaryOperatorNode withChildren(Token token, ArrayList<ParseNode> children) {
		KNaryOperatorNode node = new KNaryOperatorNode(token);
		for(ParseNode pNode: children) {
			node.appendChild(pNode);
		}
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
