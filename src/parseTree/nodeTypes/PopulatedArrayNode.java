package parseTree.nodeTypes;

import java.util.ArrayList;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class PopulatedArrayNode extends ParseNode {
	
	public PopulatedArrayNode(Token token) {
		super(token);
	}
	public PopulatedArrayNode(ParseNode node) {
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
	public static PopulatedArrayNode withChildren(Token token, ArrayList<ParseNode> arrayElements) {
		PopulatedArrayNode node = new PopulatedArrayNode(token);
		for(ParseNode pNode: arrayElements) {
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
