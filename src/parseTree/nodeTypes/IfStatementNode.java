package parseTree.nodeTypes;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class IfStatementNode extends ParseNode {

	public IfStatementNode(Token token) {
		super(token);
		assert(token.isLextant(Keyword.IF));
	}

	public IfStatementNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes

	public Lextant getIfStatementToken() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	

////////////////////////////////////////////////////////////
// convenience factory

	// withChildren for 2-3 child
	/*
	 * If the length of nodes is 2, it has condition and thenClause. There is no else clause
	 * If the length of nodes is 3, it has all three children. 
	 */
	public static IfStatementNode withChildren(Token token, ParseNode ...nodes) {
		assert(nodes.length >= 2);
		
		ParseNode condition = nodes[0];
		ParseNode thenClause = nodes[1];
		
		IfStatementNode node = new IfStatementNode(token);
		
		node.appendChild(condition);
		node.appendChild(thenClause);
		
		if(nodes.length == 3) {
			ParseNode elseClause = nodes[2];
			node.appendChild(elseClause);
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