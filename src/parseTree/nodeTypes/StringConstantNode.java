package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.StringToken;
import tokens.Token;

public class StringConstantNode extends ParseNode {
	public StringConstantNode(Token token) {
		super(token);
		assert(token instanceof StringToken);
	}
	public StringConstantNode(ParseNode node) {
		super(node);
	}
	
	public String getValue() {
		return stringToken().getValue();
	}
	public StringToken stringToken() {
		return (StringToken)token;
	}
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
