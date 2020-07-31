package asmCodeGenerator.specialCodeGenerator;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.DynamicRecordCodeGenerator;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import parseTree.ParseNode;

public class StringStringConcatenationCodeGenerator implements SimpleCodeGenerator {

	@Override
	public ASMCodeFragment generate(ParseNode node) {
		ASMCodeFragment frag = new ASMCodeFragment(CodeType.GENERATES_VALUE);
		
		// Stack : [string1 string2]
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_OTHER_ARRAY);                      // [string1]
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);                            // []
		
		Macros.readIPtrOffset(frag,                                                     // [len1] 
							  RunTime.ARRAY_INDEXING_ARRAY, 
							  ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		Macros.readIPtrOffset(frag,                                                     // [len1 len2]
							  RunTime.ARRAY_INDEXING_OTHER_ARRAY, 
							  ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		frag.add(Add);                                                                  // [overallLen]
		frag.add(Duplicate);                                                            // [len]
		frag.add(PushI, 1);                                                             // [len len 1]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);                 // [len len 1 headerSize]
		frag.add(Add);                                                                  // [len len 1+headerSize]
		frag.add(Add);                                                                  // [len recordSize]
		
		DynamicRecordCodeGenerator.createRecord(frag, 
				ASMCodeGenerationConstants.STRING_TYPE_ID, 
				ASMCodeGenerationConstants.STATUS_FLAG_FOR_STRING);
		
		// Stack is [len]
		frag.add(Duplicate);                                                        // [len len]
		Macros.writeIPtrOffset(frag,                                                // [len] 
								RunTime.RECORD_CREATION_TEMPORARY, 
								ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		
		Macros.readIPtrOffset(frag,                                                 // [len len1] 
				  RunTime.ARRAY_INDEXING_ARRAY, 
				  ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		frag.add(Duplicate);                                                        // [len len1 len1]
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                  // [len len1 len1 newString]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);             // [len len1 len1 newString headerSize]
		frag.add(Add);                                                              // [len len1 len1 baseForFirstChar]
		frag.add(Exchange);                                                         // [len len1 baseForFirstChar len1]
		
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);
		frag.add(Add);
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);
		
		frag.add(Call, RunTime.CLONE_ARRAY);                                        // [len len1]
		
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                  // [len len1 newString]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);             // [len len1 newString headerSize]
		frag.add(Add);                                                              // [len len1 baseForFirstEl]
		frag.add(Exchange);                                                         // [len baseForFirstEl len1]
		frag.add(Add);                                                              // [len baseForFirstElOfSecondSt]
		Macros.readIPtrOffset(frag,                                                 // [len baseForFirstElOfSecondSt len2] 
				  RunTime.ARRAY_INDEXING_OTHER_ARRAY, 
				  ASMCodeGenerationConstants.STRING_LENGTH_OFFSET);
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_OTHER_ARRAY);                 // [len baseForFirstElOfSecondSt len2 string2]
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);                        // [len baseForFirstElOfSecondSt len2]
		
		Macros.loadIFrom(frag, RunTime.ARRAY_INDEXING_ARRAY);
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);
		frag.add(Add);
		Macros.storeITo(frag, RunTime.ARRAY_INDEXING_ARRAY);
		
		frag.add(Call, RunTime.CLONE_ARRAY);                                        // [len]
		
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                  // [len newString]
		frag.add(PushI, ASMCodeGenerationConstants.STRING_HEADER_SIZE);             // [len newString headerSize]
		frag.add(Add);                                                              // [len newString+headerSize]
		frag.add(Exchange);                                                         // [newString+headerSize len]
		frag.add(Add);                                                              // [newStringEnd]
		
		frag.add(PushI, 0);                                                         // [newStringEnd 0]
		frag.add(StoreC);                                                           // []
		
		Macros.loadIFrom(frag, RunTime.RECORD_CREATION_TEMPORARY);                  // [newString]
		
		return frag;
	}

}