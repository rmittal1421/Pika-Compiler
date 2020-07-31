package parseTree.nodeTypes;

import java.util.ArrayList;

import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class KNaryOperatorNode extends ParseNode {
	private FunctionSignature signature = FunctionSignature.nullInstance();
	
	public KNaryOperatorNode(Token token) {
		super(token);
	}
	public KNaryOperatorNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// attributes

	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}
	
	public final FunctionSignature getSignature() {
		return signature;
	}
	public final void setSignature(FunctionSignature signature) {
		this.signature = signature;
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	// For creating an array with specified elements
	public static KNaryOperatorNode withChildren(Token token, ParseNode ...children) {
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
