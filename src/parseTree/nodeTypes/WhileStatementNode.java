package parseTree.nodeTypes;

import asmCodeGenerator.Labeller;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class WhileStatementNode extends ParseNode {
	
	private String startLabel;
	private String endLabel;

	public WhileStatementNode(Token token) {
		super(token);
		assert (token.isLextant(Keyword.WHILE));
		initializeLabels();
	}

	public WhileStatementNode(ParseNode node) {
		super(node);
		initializeLabels();
	}
	
	private void initializeLabels() {
		Labeller labeller = new Labeller("while");
		this.startLabel = labeller.newLabel("start");
		this.endLabel = labeller.newLabel("end");
	}

////////////////////////////////////////////////////////////
//attributes

	public Lextant getWhileStatementToken() {
		return lextantToken().getLextant();
	}

	public LextantToken lextantToken() {
		return (LextantToken) token;
	}
	
	public String getStartLabel() {
		return this.startLabel;
	}
	
	public String getEndLabel() {
		return this.endLabel;
	}

////////////////////////////////////////////////////////////
//convenience factory
	
	public static WhileStatementNode withChildren(Token token, ParseNode condition, ParseNode whileBlock) {
		WhileStatementNode node = new WhileStatementNode(token);
		node.appendChild(condition);
		node.appendChild(whileBlock);
		return node;
	}

///////////////////////////////////////////////////////////
//boilerplate for visitors

	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}

}
