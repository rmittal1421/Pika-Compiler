package parseTree;

import parseTree.nodeTypes.*;

// Visitor pattern with pre- and post-order visits
public interface ParseNodeVisitor {
	
	// non-leaf nodes: visitEnter and visitLeave
	void visitEnter(BinaryOperatorNode node);
	void visitLeave(BinaryOperatorNode node);
	
	void visitEnter(UnaryOperatorNode node);
	void visitLeave(UnaryOperatorNode node);
	
	void visitEnter(KNaryOperatorNode node);
	void visitLeave(KNaryOperatorNode node);
	
	void visitEnter(BlockStatementNode node);
	void visitLeave(BlockStatementNode node);
	
	void visitEnter(AssignmentStatementNode node);
	void visitLeave(AssignmentStatementNode node);

	void visitEnter(DeclarationNode node);
	void visitLeave(DeclarationNode node);
	
	void visitEnter(ParseNode node);
	void visitLeave(ParseNode node);
	
	void visitEnter(PrintStatementNode node);
	void visitLeave(PrintStatementNode node);
	
	void visitEnter(ProgramNode node);
	void visitLeave(ProgramNode node);
	
	void visitEnter(FunctionNode node);
	void visitLeave(FunctionNode node);
	
	void visitEnter(LambdaNode node);
	void visitLeave(LambdaNode node);
	
	void visitEnter(ParameterSpecificationNode node);
	void visitLeave(ParameterSpecificationNode node);
	
	void visitEnter(TypeNode node);
	void visitLeave(TypeNode node);
	
	void visitEnter(IfStatementNode node);
	void visitLeave(IfStatementNode node);
	
	void visitEnter(WhileStatementNode node);
	void visitLeave(WhileStatementNode node);
	
	void visitEnter(PopulatedArrayNode node);
	void visitLeave(PopulatedArrayNode node);
	
	void visitEnter(ReturnNode node);
	void visitLeave(ReturnNode node);


	// leaf nodes: visitLeaf only
	void visit(BooleanConstantNode node);
	void visit(ErrorNode node);
	void visit(IdentifierNode node);
	void visit(IntegerConstantNode node);
	void visit(FloatingConstantNode node);
	void visit(CharacterConstantNode node);
	void visit(StringConstantNode node);
	void visit(NewlineNode node);
	void visit(SpaceNode node);
	void visit(TabNode node);
	void visit(BreakNode node);
	void visit(ContinueNode node);

	
	public static class Default implements ParseNodeVisitor
	{
		public void defaultVisit(ParseNode node) {	}
		public void defaultVisitEnter(ParseNode node) {
			defaultVisit(node);
		}
		public void defaultVisitLeave(ParseNode node) {
			defaultVisit(node);
		}		
		public void defaultVisitForLeaf(ParseNode node) {
			defaultVisit(node);
		}
		public void visitEnter(BinaryOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(BinaryOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(UnaryOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(UnaryOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(KNaryOperatorNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(KNaryOperatorNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(DeclarationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(DeclarationNode node) {
			defaultVisitLeave(node);
		}					
		public void visitEnter(BlockStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(BlockStatementNode node) {
			defaultVisitLeave(node);
		}			
		public void visitEnter(AssignmentStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(AssignmentStatementNode node) {
			defaultVisitLeave(node);
		}	
		public void visitEnter(IfStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(IfStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(WhileStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(WhileStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ParseNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ParseNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(TypeNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(TypeNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(PrintStatementNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(PrintStatementNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ProgramNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ProgramNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(FunctionNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(FunctionNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(LambdaNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(LambdaNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ParameterSpecificationNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ParameterSpecificationNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(PopulatedArrayNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(PopulatedArrayNode node) {
			defaultVisitLeave(node);
		}
		public void visitEnter(ReturnNode node) {
			defaultVisitEnter(node);
		}
		public void visitLeave(ReturnNode node) {
			defaultVisitLeave(node);
		}

		public void visit(BooleanConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(ErrorNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(IdentifierNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(IntegerConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(FloatingConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(CharacterConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(StringConstantNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(NewlineNode node) {
			defaultVisitForLeaf(node);
		}	
		public void visit(SpaceNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(TabNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(BreakNode node) {
			defaultVisitForLeaf(node);
		}
		public void visit(ContinueNode node) {
			defaultVisitForLeaf(node);
		}
	}
}
