package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ProgramNode extends ParseNode {

	public ProgramNode(Token token) {
		super(token);
	}
	public ProgramNode(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// no attributes

	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
//		firstVisitor(visitor);
//		secondVisitor(visitor);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
	
//	private void firstVisitor(ParseNodeVisitor visitor) {
//		for(ParseNode child : getChildren()) {
//			if(child instanceof FunctionNode) {
//				assert child.nChildren() == 2;
//				
//				// Visit function's identifier node
//				child.child(0).accept(visitor);
//				child.child(1).accept(visitor);
//			}
//		}
//	}
//	
//	private void secondVisitor(ParseNodeVisitor visitor) {
//		for(ParseNode child: getChildren()) {
//			if(child instanceof FunctionNode) {
//				ParseNode lambda = child.child(1);
//				
//				// Visit just the block child of lambda (last child)
//				lambda.child(lambda.nChildren() - 1).accept(visitor);
//			} else {
//				child.accept(visitor);
//			}
//		}
//	}
}
