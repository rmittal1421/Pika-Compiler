package asmCodeGenerator.specialCodeGenerator;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.DynamicRecordCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class StringCharConcatenationCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// Stack : [string char]
		frag.add(Exchange);                                                         // [char string]
		frag.add(Duplicate);                                                        // [char string string]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);             // [char string string headerSize]
		frag.add(Add);                                                              // [char string firstCharOfOldString]
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);                        // [char string]
		Macros.readIOffset(frag, ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);  // [char len]
		frag.add(Duplicate);                                                        // [char len len]
		frag.add(PushI, 2);                                                         // [char len len 2] 
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);             // [char len len 2 12]
		frag.add(Add);                                                              // [char len len 14]
		frag.add(Add);                                                              // [char len newRecordSize]
		
		DynamicRecordCodeGenerator.createRecord(frag, 
				ASMCodeGenerationConstants.STRING_TYPE_ID, 
				ASMCodeGenerationConstants.STATUS_FLAG_FOR_STRING);
		
		// Stack is [char len]
		frag.add(Duplicate);                                                        // [char len len]
		frag.add(PushI, 1);                                                         // [char len len 1]
		frag.add(Add);                                                              // [char len len+1]
		Macros.writeIPtrOffset(frag,                                                // [char len] 
								RunTime.RECORD_CREATION_TEMPORARY, 
								ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		
		frag.add(Duplicate);                                                        // [char len len]
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                  // [char len len newString]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);             // [char len len newString headerSize]
		frag.add(Add);                                                              // [char len len baseOfNewStringFirstChar]
		frag.add(Exchange);                                                         // [char len baseOfNewStringFirstChar len]
		
		frag.add(Call, RunTime.CLONE_ARRAY);                                        // [char len]
		
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                  // [char len newString]
		frag.add(Exchange);                                                         // [char newString len]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);             // [char newString len headerSize]
		frag.add(Add);                                                              // [char newString offset]
		frag.add(Add);                                                              // [char newStringEnd]
		
		frag.add(Duplicate);                                                        // [char newStringEnd newStringEnd]
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);                        // [char newStringEnd]
		frag.add(Exchange);                                                         // [newStringEnd char]
		frag.add(StoreC);                                                           // []
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);                       // [newStringEnd-1]
		frag.add(PushI, 1);                                                         // [newStringEnd-1 1]
		frag.add(Add);                                                              // [newStringEnd]
		frag.add(PushI, 0);                                                         // [newStringEnd 0]
		frag.add(StoreC);                                                           // [] & string null terminated
		
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                  // [newString]
		
		return frag;
	}

}