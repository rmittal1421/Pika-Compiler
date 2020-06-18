package asmCodeGenerator.specialCodeGenerator;

import parseTree.ParseNode;
import asmCodeGenerator.codeStorage.ASMCodeFragment;

public interface FullCodeGenerator {
	public ASMCodeFragment generate(ParseNode node, ASMCodeFragment ...operandCode);
}
