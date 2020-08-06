package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import asmCodeGenerator.Labeller;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class DeclarationNode extends ParseNode {
	boolean isStatic;
	String skipInitializationLabel;

	public DeclarationNode(Token token, boolean isStatic) {
		super(token);
		assert(token.isLextant(Keyword.CONST, Keyword.VAR));
		this.isStatic = isStatic;
		
		if(isStatic) {
			Labeller labeller = new Labeller("static-declaration");
			String label = labeller.newLabel("skip-initialization");
			this.skipInitializationLabel = label;
		} else {
			this.skipInitializationLabel = "";
		}
	}

	public DeclarationNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getDeclarationType() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	public void setStatic(boolean isStatic) {
		this.isStatic = isStatic;
	}
	
	public boolean getIsStatic() {
		return !(this.getParent() instanceof ProgramNode) && this.isStatic;
	}
	
	public String getSkipInitializationLable() {
		return this.skipInitializationLabel;
	}
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static DeclarationNode withChildren(Token token, ParseNode declaredName, ParseNode initializer, boolean isStatic) {
		DeclarationNode node = new DeclarationNode(token, isStatic);
		node.appendChild(declaredName);
		node.appendChild(initializer);
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
