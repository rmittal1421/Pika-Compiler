package asmCodeGenerator;

import parseTree.ParseNode;
import asmCodeGenerator.codeStorage.ASMCodeFragment;

public interface SimpleCodeGenerator {
	public ASMCodeFragment generate(ParseNode node);
}
