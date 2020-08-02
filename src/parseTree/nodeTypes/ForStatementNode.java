package parseTree.nodeTypes;

import asmCodeGenerator.Labeller;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import semanticAnalyzer.types.Type;
import tokens.LextantToken;
import tokens.Token;

public class ForStatementNode extends ParseNode {
	
	Token forTypeToken;
	private String startLabel;
	private String readyForNextIterationLabel;
	private String endLabel;

	public ForStatementNode(Token token, Token forTypeToken) {
		super(token);
		assert (token.isLextant(Keyword.FOR));
		
		this.forTypeToken = forTypeToken;
		assert(this.forTypeToken.isLextant(Keyword.INDEX, Keyword.ELEM));
		
		initializeLabels();
	}

	public ForStatementNode(ParseNode node) {
		super(node);
		initializeLabels();
	}
	
	private void initializeLabels() {
		Labeller labeller = new Labeller("for");
		this.startLabel = labeller.newLabel("start");
		this.readyForNextIterationLabel = labeller.newLabel("next-iteration");
		this.endLabel = labeller.newLabel("end");
	}

////////////////////////////////////////////////////////////
//attributes

	public Lextant getForStatementToken() {
		return lextantToken().getLextant();
	}

	public LextantToken lextantToken() {
		return (LextantToken) token;
	}
	
	public Lextant getForTypeToken() {
		return forTypeLextantToken().getLextant();
	}
	
	public LextantToken forTypeLextantToken() {
		return (LextantToken) forTypeToken;
	}
	
	public String getStartLabel() {
		return this.startLabel;
	}
	
	public String getReadyForNextIterationLabel() {
		return this.readyForNextIterationLabel;
	}
	
	public String getEndLabel() {
		return this.endLabel;
	}
	
	public Type getSequenceType() {
		return this.child(0).getType();
	}
	

////////////////////////////////////////////////////////////
//convenience factory
	
	public static ForStatementNode withChildren(Token token, Token forTypeToken, ParseNode sequence, ParseNode identifier, ParseNode forBlock) {
		ForStatementNode node = new ForStatementNode(token, forTypeToken);
		node.appendChild(sequence);
		node.appendChild(identifier);
		node.appendChild(forBlock);
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
