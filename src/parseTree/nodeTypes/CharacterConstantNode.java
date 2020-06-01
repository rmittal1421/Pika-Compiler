package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.CharacterToken;
import tokens.Token;

public class CharacterConstantNode extends ParseNode {
	public CharacterConstantNode(Token token) {
		super(token);
		assert(token instanceof CharacterToken);
	}
	public CharacterConstantNode(ParseNode node) {
		super(node);
	}
	
	public char getValue() {
		return characterToken().getValue();
	}
	public CharacterToken characterToken() {
		return (CharacterToken)token;
	}
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
