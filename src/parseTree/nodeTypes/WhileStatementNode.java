package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class WhileStatementNode extends ParseNode {

	public WhileStatementNode(Token token) {
		super(token);
		assert (token.isLextant(Keyword.WHILE));
	}

	public WhileStatementNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
//attributes

	public Lextant getWhileStatementToken() {
		return lextantToken().getLextant();
	}

	public LextantToken lextantToken() {
		return (LextantToken) token;
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
