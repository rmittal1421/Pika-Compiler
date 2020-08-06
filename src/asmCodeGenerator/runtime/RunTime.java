package asmCodeGenerator.runtime;

import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

import asmCodeGenerator.Labeller;
import asmCodeGenerator.Macros;
import asmCodeGenerator.ASMCodeGenerationConstants;
import asmCodeGenerator.codeStorage.ASMCodeFragment;

public class RunTime {
	public static final String EAT_LOCATION_ZERO = "$eat-location-zero"; // helps us distinguish null pointers from real
																			// ones.
	public static final String INTEGER_PRINT_FORMAT = "$print-format-integer";
	public static final String FLOATING_PRINT_FORMAT = "$print-format-floating";
	public static final String BOOLEAN_PRINT_FORMAT = "$print-format-boolean";
	public static final String CHARACTER_PRINT_FORMAT = "$print-format-character";
	public static final String STRING_PRINT_FORMAT = "$print-format-string";
	public static final String NEWLINE_PRINT_FORMAT = "$print-format-newline";
	public static final String SPACE_PRINT_FORMAT = "$print-format-space";
	public static final String TAB_PRINT_FORMAT = "$print-format-tab";
	public static final String BOOLEAN_TRUE_STRING = "$boolean-true-string";
	public static final String BOOLEAN_FALSE_STRING = "$boolean-false-string";
	public static final String NEGATIVE_SIGN_STRING = "$negative-sign-string";
	public static final String UNDERSCORE_SIGN_STRING = "$underscore-sign-string";
	public static final String FORWARD_SLASH_SIGN_STRING = "$forward-slash-sign-string";
	public static final String OPEN_BRACKET_SIGN_STRING = "$open-bracket-sign-string";
	public static final String CLOSE_BRACKET_SIGN_STRING = "$close-bracket-sign-string";
	public static final String ARRAY_SEPARATOR_SIGN_STRING = "array-separator-sign-string";
	public static final String LAMBDA_PRINT_STRING = "lambda-print-string";
	public static final String RETURN_PC = "return-pc";
	public static final String GLOBAL_MEMORY_BLOCK = "$global-memory-block";
	public static final String GLOBAL_MEMORY_BLOCK2 = "$global-memory-block2";
	public static final String FRAME_POINTER = "$frame-pointer";
	public static final String STACK_POINTER = "$stack-pointer";
	public static final String USABLE_MEMORY_START = "$usable-memory-start";
	public static final String MAIN_PROGRAM_LABEL = "$$main";

	// Array static variables
	public static final String ARRAY_INDEXING_ARRAY = "$a-indexing-array";
	public static final String ARRAY_INDEXING_OTHER_ARRAY = "$a-indexing-other-array";
	public static final String ARRAY_INDEXING_INDEX = "$a-indexing-index";
	public static final String ARRAY_LATER_INDEXING_INDEX = "$a-later-indexing-index";
	public static final String RECORD_CREATION_TEMPORARY = "$record-creation-temp";
	public static final String ARRAY_DATASIZE_TEMPORARY = "$array-datasize-temporary";
	public static final String ARRAY_STATUS_FLAGS = "$array-status-flags";
	public static final String ARRAY_SUBTYPE_SIZE = "$array-subtype-size";
	public static final String ARRAY_LENGTH = "$array-length";
	public static final String CLEAR_N_BYTES = "$clear-n-bytes";
	public static final String CLONE_ARRAY = "$clone-array";

	// Rational static variables
	public static final String LOWEST_TERMS = "$lowest-terms";
	public static final String RATIONAL_NUM = "$rational-num";
	public static final String RATIONAL_DEN = "$rational-den";
	public static final String RATIONAL_GCD = "$rational-gcd";
	public static final String EXPRESS_OVER_DEN = "$express-over-den";
	public static final String RATIONAL_DEN_2_TEMP = "$rational-den-2-temp";

	// For loop static variables
	public static final String FOR_IDENTIFER = "$for-identifier";
	public static final String FOR_INDEX = "$for-index";
	public static final String FOR_SEQUENCE = "$for-sequence";
	public static final String FOR_LENGTH = "$for-length";

	// Runtime errors static variables
	public static final String GENERAL_RUNTIME_ERROR = "$$general-runtime-error";
	public static final String INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$i-divide-by-zero";
	public static final String FLOATING_DIVIDE_BY_ZERO_RUNTIME_ERROR = "$$f-divide-by-zero";
	public static final String RATIONAL_NUMBER_GIVEN_ZERO_DENOMINATOR = "$$-rational-given-zero-denominator";
	public static final String NULL_ARRAY_RUNTIME_ERROR = "$$array-null-error";
	public static final String INDEX_OUT_OF_BOUNDS_RUNTIME_ERROR = "$$array-index-out-of-bound-error";
	public static final String NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR = "$$array-length-negative";
	public static final String NO_RETURN_IN_LAMBDA = "$$code-out-of-lambda-without-return";
	public static final String LATER_INDEX_SMALLER_OR_EQUAL_RUNTIME_ERROR = "$$array-second-index-smaller-equal-error";
	public static final String UNEQUAL_LENGTH_ARRAYS_ZIP_OPERATOR_ERROR = "$$unequal-length-array-zip-error";
	public static final String EMPTY_ARRAY_GIVEN_TO_FOLD = "$$fold-operator-on-empty-array";

	private ASMCodeFragment environmentASM() {
		ASMCodeFragment result = new ASMCodeFragment(GENERATES_VOID);
		result.append(pointersForLambdas());
		result.append(jumpToMain());
		result.append(stringsForPrintf());
		result.append(runtimeErrors());
		result.append(temporaryVariables());
		result.append(clearNBytes());
		result.append(cloneArray());
		result.append(lowestTerms());
		result.add(DLabel, USABLE_MEMORY_START);
		return result;
	}

	private ASMCodeFragment pointersForLambdas() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);

		// Declare and store for frame pointer
		Macros.declareI(frag, FRAME_POINTER);
		frag.add(Memtop);
		Macros.storeITo(frag, FRAME_POINTER);

		// Declare and store for stack pointer
		Macros.declareI(frag, STACK_POINTER);
		frag.add(Memtop);
		Macros.storeITo(frag, STACK_POINTER);

		return frag;
	}

	private ASMCodeFragment jumpToMain() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(Jump, MAIN_PROGRAM_LABEL);
		return frag;
	}

	private ASMCodeFragment stringsForPrintf() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		frag.add(DLabel, EAT_LOCATION_ZERO);
		frag.add(DataZ, 8);
		frag.add(DLabel, INTEGER_PRINT_FORMAT);
		frag.add(DataS, "%d");
		frag.add(DLabel, FLOATING_PRINT_FORMAT);
		frag.add(DataS, "%g");
		frag.add(DLabel, CHARACTER_PRINT_FORMAT);
		frag.add(DataS, "%c");
		frag.add(DLabel, STRING_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, BOOLEAN_PRINT_FORMAT);
		frag.add(DataS, "%s");
		frag.add(DLabel, NEWLINE_PRINT_FORMAT);
		frag.add(DataS, "\n");
		frag.add(DLabel, SPACE_PRINT_FORMAT);
		frag.add(DataS, " ");
		frag.add(DLabel, TAB_PRINT_FORMAT);
		frag.add(DataS, "\t");
		frag.add(DLabel, BOOLEAN_TRUE_STRING);
		frag.add(DataS, "true");
		frag.add(DLabel, BOOLEAN_FALSE_STRING);
		frag.add(DataS, "false");
		frag.add(DLabel, NEGATIVE_SIGN_STRING);
		frag.add(DataS, "-");
		frag.add(DLabel, UNDERSCORE_SIGN_STRING);
		frag.add(DataS, "_");
		frag.add(DLabel, FORWARD_SLASH_SIGN_STRING);
		frag.add(DataS, "/");
		frag.add(DLabel, OPEN_BRACKET_SIGN_STRING);
		frag.add(DataS, "[");
		frag.add(DLabel, CLOSE_BRACKET_SIGN_STRING);
		frag.add(DataS, "]");
		frag.add(DLabel, ARRAY_SEPARATOR_SIGN_STRING);
		frag.add(DataS, ", ");
		frag.add(DLabel, LAMBDA_PRINT_STRING);
		frag.add(DataS, "<lambda>");

		return frag;
	}

	private ASMCodeFragment runtimeErrors() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);

		generalRuntimeError(frag);
		integerDivideByZeroError(frag);
		floatingDivideByZeroError(frag);
		rationalGotZeroDenominatorError(frag);
		nullArrayIndexingError(frag);
		arrayIndexOutOfBoundsError(frag);
		arraySecondIndexSmallerEqualError(frag);
		arrayLengthNegativeError(frag);
		lambdaWithoutReturnError(frag);
		unequalLengthArraysZipOperatorError(frag);
		foldOperatorOnEmptyArrayError(frag);

		return frag;
	}

	private ASMCodeFragment generalRuntimeError(ASMCodeFragment frag) {
		String generalErrorMessage = "$errors-general-message";

		frag.add(DLabel, generalErrorMessage);
		frag.add(DataS, "Runtime error: %s\n");

		frag.add(Label, GENERAL_RUNTIME_ERROR);
		frag.add(PushD, generalErrorMessage);
		frag.add(Printf);
		frag.add(Halt);
		return frag;
	}

	private ASMCodeFragment temporaryVariables() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);
		Macros.declareI(frag, ARRAY_INDEXING_ARRAY);
		Macros.declareI(frag, ARRAY_INDEXING_OTHER_ARRAY);
		Macros.declareI(frag, ARRAY_INDEXING_INDEX);
		Macros.declareI(frag, ARRAY_LATER_INDEXING_INDEX);
		Macros.declareI(frag, RECORD_CREATION_TEMPORARY);
		Macros.declareI(frag, ARRAY_DATASIZE_TEMPORARY);
		Macros.declareI(frag, ARRAY_SUBTYPE_SIZE);
		Macros.declareI(frag, ARRAY_LENGTH);
		Macros.declareI(frag, RATIONAL_NUM);
		Macros.declareI(frag, RATIONAL_DEN);
		Macros.declareI(frag, EXPRESS_OVER_DEN);
		Macros.declareI(frag, RATIONAL_DEN_2_TEMP);
		Macros.declareI(frag, RETURN_PC);
		Macros.declareI(frag, FOR_IDENTIFER);
		Macros.declareI(frag, FOR_INDEX);
		Macros.declareI(frag, FOR_SEQUENCE);
		Macros.declareI(frag, FOR_LENGTH);
		return frag;
	}

	// [ ... nElems baseAddr numBytes returnPointer ] -> [ ... nElems ]
	private ASMCodeFragment clearNBytes() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);

		// Labels required during the process of clearing out the bytes
		Labeller labeller = new Labeller("subroutine-clearNBytes");
		String subroutineStart = labeller.newLabel("start");
		String subroutineEnd = labeller.newLabel("end");
		String PC_TO_RETURN_TO = labeller.newLabel("pc-to-return-to");

		// Label for the function where our PC jumps when called
		frag.add(Label, CLEAR_N_BYTES); // [... baseAddr numBytes returnPointer]

		Macros.declareI(frag, PC_TO_RETURN_TO);
		Macros.storeITo(frag, PC_TO_RETURN_TO); // [... baseAddr numBytes]

		// It is a simulation of a loop
		frag.add(Label, subroutineStart);

		/*
		 * pseudoCode: while(numBytes != 0) { store 0 in base address and numBytes-- }
		 */

		// If numBytes == 0, jump to the end of the subroutine
		frag.add(Duplicate); // [... baseAddr numBytes numBytes]
		frag.add(JumpFalse, subroutineEnd); // [... baseAddr numBytes] && Jumps to end of the function
		// Comes here if there are still more bytes to clear up. First decrease numBytes
		// since it is on top of stack
		frag.add(PushI, 1); // [... baseAddr numBytes 1]
		frag.add(Subtract); // [... baseAddr (numBytes-1)]
		frag.add(Exchange); // [... (numBytes-1) baseAddr]
		frag.add(Duplicate); // [... (numBytes-1) baseAddr baseAddr]
		frag.add(PushI, 0); // [... (numBytes-1) baseAddr baseAddr 0]
		frag.add(StoreC); // [... (numBytes-1) baseAddr] & MEM(baseAddr) <- 0 & 0xff which is just a byte
							// of 0
		// Now increase the base address by 1 byte to place 0 in it if required
		frag.add(PushI, 1); // [... (numBytes-1) baseAddr 1]
		frag.add(Add); // [... (numBytes-1) (baseAddr+1)]
		frag.add(Exchange); // [... (baseAddr+1) (numBytes-1)]
		frag.add(Jump, subroutineStart);

		// End of subroutine
		frag.add(Label, subroutineEnd);
		frag.add(Pop); // [... (baseAddr+numBytes)]
		frag.add(Pop); // [...] and the stack top is nElems

		Macros.loadIFrom(frag, PC_TO_RETURN_TO); // [... returnPointer]
		frag.add(PopPC);
		return frag;
	}

	private ASMCodeFragment cloneArray() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);

		// Labels required during the process of clearing out the bytes
		Labeller labeller = new Labeller("subroutine-cloneArray");
		String subroutineStart = labeller.newLabel("start");
		String subroutineEnd = labeller.newLabel("end");
		String PC_TO_RETURN_TO = labeller.newLabel("pc-to-return-to");

		// Label for the function where our PC jumps when called
		frag.add(Label, CLONE_ARRAY); // [... baseAddr numBytes returnPointer]

		Macros.declareI(frag, PC_TO_RETURN_TO);
		Macros.storeITo(frag, PC_TO_RETURN_TO); // [... baseAddr numBytes]

		// It is a simulation of a loop
		frag.add(Label, subroutineStart);

		/*
		 * pseudoCode: while(numBytes != 0) { copy a byte from other array in new array
		 * and numBytes-- }
		 */

		// If numBytes == 0, jump to the end of the subroutine
		frag.add(Duplicate); // [... baseAddr numBytes numBytes]
		frag.add(JumpFalse, subroutineEnd); // [... baseAddr numBytes] && Jumps to end of the function
		// Comes here if there are still more bytes to copy. First decrease numBytes
		// since it is on top of stack
		frag.add(PushI, 1); // [... baseAddr numBytes 1]
		frag.add(Subtract); // [... baseAddr numBytes-1]
		frag.add(Exchange); // [... numBytes-1 baseAddr]
		frag.add(Duplicate); // [... numBytes-1 baseAddr baseAddr]
		Macros.loadIFrom(frag, ARRAY_INDEXING_ARRAY); // [... numBytes-1 baseAddr baseAddr addressFromWhereToCopy]
		frag.add(Duplicate); // [... numBytes-1 baseAddr baseAddr addressFromWhereToCopy
								// addressFromWhereToCopy]
		frag.add(PushI, 1); // [... numBytes-1 baseAddr baseAddr addressFromWhereToCopy
							// addressFromWhereToCopy 1]
		frag.add(Add); // [... numBytes-1 baseAddr baseAddr addressFromWhereToCopy
						// addressFromWhereToCopy+1]
		Macros.storeITo(frag, ARRAY_INDEXING_ARRAY); // Increment the cloned array pointer by 1 for next turn and store
														// back -> [... numBytes-1 baseAddr baseAddr
														// addressFromWhereToCopy]
		// Add a byte from the other array
		frag.add(LoadC); // [... numBytes-1 baseAddr baseAddr *(addressFromWhereToCopy)]
		frag.add(StoreC); // [... numBytes-1 baseAddr] & Value got store where I wanted
		// Now increase the base address by 1 byte
		frag.add(PushI, 1); // [... (numBytes-1) baseAddr 1]
		frag.add(Add); // [... (numBytes-1) (baseAddr+1)]
		frag.add(Exchange); // [... (baseAddr+1) (numBytes-1)]
		frag.add(Jump, subroutineStart);

		// End of subroutine
		frag.add(Label, subroutineEnd);
		frag.add(Pop); // [... (baseAddr+numBytes)]
		frag.add(Pop); // [...] and the stack top is nElems

		Macros.loadIFrom(frag, PC_TO_RETURN_TO); // [... returnPointer]
		frag.add(PopPC);
		return frag;
	}

	private ASMCodeFragment lowestTerms() {
		ASMCodeFragment frag = new ASMCodeFragment(GENERATES_VOID);

		Labeller labeller = new Labeller("lowest-terms");
		String PC_TO_RETURN_TO = labeller.newLabel("pc-to-return-to");
		String subroutineStart = labeller.newLabel("subroutine-start");
		String numGreaterLabel = labeller.newLabel("num-is-greater");
		String subroutineEnd = labeller.newLabel("subroutine-end");
		String numWasFoundZero = labeller.newLabel("num-was-0");
		String coreEnd = labeller.newLabel("core-end-lowest-term");

		// Stack received: [... num den returnPointer]
		frag.add(Label, LOWEST_TERMS);

		Macros.declareI(frag, PC_TO_RETURN_TO);
		Macros.storeITo(frag, PC_TO_RETURN_TO); // [... num den] & returnPointer saved

		Macros.declareI(frag, RATIONAL_GCD);

		Macros.storeITo(frag, RATIONAL_DEN); // [... num]
		Macros.storeITo(frag, RATIONAL_NUM); // [...]

		// Sanitize denominator
		Macros.loadIFrom(frag, RATIONAL_DEN); // [... den]
		frag.add(JumpFalse, RATIONAL_NUMBER_GIVEN_ZERO_DENOMINATOR); // Goes to runtime error if den evaluates to be
																		// zero

		// Sanitize numerator
		Macros.loadIFrom(frag, RATIONAL_NUM); // [... num]
		frag.add(JumpFalse, numWasFoundZero); // [...] & jumps if num was 0 to the handling of this case

		/*
		 * pseudoCode:
		 * 
		 * while(num != den) { if(num>den) num -= den else den -= num } return num
		 */

		Macros.loadIFrom(frag, RATIONAL_NUM);
		Macros.loadIFrom(frag, RATIONAL_DEN);
		frag.add(Multiply); // [... num*den] but treat as if [...] ----> actual product stored

		Macros.loadMakePositiveStore(frag, RATIONAL_DEN); // [... num] & den made positive
		Macros.loadMakePositiveStore(frag, RATIONAL_NUM); // [...] & num made positive

		Macros.loadIFrom(frag, RATIONAL_NUM); // [... num]
		Macros.loadIFrom(frag, RATIONAL_DEN); // [... num den]
		frag.add(Multiply); // [... num*den] -----> product of num and den both being positive

		// Remember we stored their actual product as well
		frag.add(Divide); // [... -1 or 1] depending on we want -ve or not. (Leave it there)

		Macros.loadIFrom(frag, RATIONAL_NUM); // [... (-1 or 1) num]
		frag.add(Multiply); // [... num] (Maybe +ve or -ve which is final now)
		Macros.loadIFrom(frag, RATIONAL_DEN); // [... num den] and behave as if it it [...]

		frag.add(Label, subroutineStart);

		Macros.loadIFrom(frag, RATIONAL_NUM);
		Macros.loadIFrom(frag, RATIONAL_DEN); // [... num den]

		frag.add(Subtract); // [... num-den]
		frag.add(Duplicate); // [... num-den num-den]
		frag.add(JumpFalse, subroutineEnd); // [... num-den] & Jumps to end with stack [... 0]

		// Comes here which means that num and den are not equal yet
		frag.add(Duplicate); // [... num-den num-den]
		frag.add(JumpPos, numGreaterLabel); // [... num-den]
		// Else den is greater or equal
		frag.add(Negate); // [... den-num]
		Macros.storeITo(frag, RATIONAL_DEN); // [...]
		frag.add(Jump, subroutineStart); // [...]

		frag.add(Label, numGreaterLabel); // [... num-den]
		Macros.storeITo(frag, RATIONAL_NUM); // [...]
		frag.add(Jump, subroutineStart); // [...]

		frag.add(Label, subroutineEnd); // [... 0]
		frag.add(Pop); // [...]
		Macros.loadIFrom(frag, RATIONAL_NUM); // [... gcd] Num & Den both are basically gcd now so load one
		frag.add(Duplicate); // [... gcd gcd]
		Macros.storeITo(frag, RATIONAL_GCD); // [...gcd] but remember we said [... gcd] === [... num den gcd]
		frag.add(Divide); // [... num den/gcd]
		frag.add(Exchange); // [... den/gcd num]
		Macros.loadIFrom(frag, RATIONAL_GCD);
		frag.add(Divide); // [... den/gcd num/gcd]
		frag.add(Exchange); // [... refactoredNum refactoredDen]
		frag.add(Jump, coreEnd);

		frag.add(Label, numWasFoundZero); // [...] & num was 0 den was not
		frag.add(PushI, 0); // [... 0]
		frag.add(PushI, 1); // [... 0 1]

		frag.add(Label, coreEnd);
		Macros.loadIFrom(frag, PC_TO_RETURN_TO); // [... num den returnPointer]
		frag.add(PopPC);
		return frag;
	}

	private void integerDivideByZeroError(ASMCodeFragment frag) {
		String intDivideByZeroMessage = "$errors-int-divide-by-zero";

		frag.add(DLabel, intDivideByZeroMessage);
		frag.add(DataS, "integer divide by zero");

		frag.add(Label, INTEGER_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, intDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void floatingDivideByZeroError(ASMCodeFragment frag) {
		String floatingDivideByZeroMessage = "$errors-floating-divide-by-zero";

		frag.add(DLabel, floatingDivideByZeroMessage);
		frag.add(DataS, "floating divide by zero");

		frag.add(Label, FLOATING_DIVIDE_BY_ZERO_RUNTIME_ERROR);
		frag.add(PushD, floatingDivideByZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void rationalGotZeroDenominatorError(ASMCodeFragment frag) {
		String ratioanalGotZeroMessage = "$errors-rational-denomitor-zero";

		frag.add(DLabel, ratioanalGotZeroMessage);
		frag.add(DataS, "rational number given 0 as denominator");

		frag.add(Label, RATIONAL_NUMBER_GIVEN_ZERO_DENOMINATOR);
		frag.add(PushD, ratioanalGotZeroMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void nullArrayIndexingError(ASMCodeFragment frag) {
		String nullArrayIndexingMessage = "$errors-array-null-indexed";

		frag.add(DLabel, nullArrayIndexingMessage);
		frag.add(DataS, "Null array indexed");

		frag.add(Label, NULL_ARRAY_RUNTIME_ERROR);
		frag.add(PushD, nullArrayIndexingMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void arrayIndexOutOfBoundsError(ASMCodeFragment frag) {
		String arrayIndexOutOfBoundsMessage = "$errors-array-index-out-of-bounds";

		frag.add(DLabel, arrayIndexOutOfBoundsMessage);
		frag.add(DataS, "Index out of bounds");

		frag.add(Label, INDEX_OUT_OF_BOUNDS_RUNTIME_ERROR);
		frag.add(PushD, arrayIndexOutOfBoundsMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void arraySecondIndexSmallerEqualError(ASMCodeFragment frag) {
		String arraySecondIndexSmallerEqualMessage = "$errors-array-second-index-smaller-equal";

		frag.add(DLabel, arraySecondIndexSmallerEqualMessage);
		frag.add(DataS, "Second index smaller or equal to first one in substring");

		frag.add(Label, LATER_INDEX_SMALLER_OR_EQUAL_RUNTIME_ERROR);
		frag.add(PushD, arraySecondIndexSmallerEqualMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void arrayLengthNegativeError(ASMCodeFragment frag) {
		String arrayLengthNegativeMessage = "$errors-array-length-negative";

		frag.add(DLabel, arrayLengthNegativeMessage);
		frag.add(DataS, "Array provided with negative length");

		frag.add(Label, NEGATIVE_LENGTH_ARRAY_RUNTIME_ERROR);
		frag.add(PushD, arrayLengthNegativeMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	private void lambdaWithoutReturnError(ASMCodeFragment frag) {
		String codeOutOfLambdaWithoutReturnMessage = "$code-out-of-lambda-without-return";

		frag.add(DLabel, codeOutOfLambdaWithoutReturnMessage);
		frag.add(DataS, "Reached end of function without return statement");

		frag.add(Label, NO_RETURN_IN_LAMBDA);
		frag.add(PushD, codeOutOfLambdaWithoutReturnMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	public void unequalLengthArraysZipOperatorError(ASMCodeFragment frag) {
		String unequalLengthArraysZipOperatorErrorMessage = "$unequal-length-arrays-zip-operator";

		frag.add(DLabel, unequalLengthArraysZipOperatorErrorMessage);
		frag.add(DataS, "Both arrays given different lengths in zip operator");

		frag.add(Label, UNEQUAL_LENGTH_ARRAYS_ZIP_OPERATOR_ERROR);
		frag.add(PushD, unequalLengthArraysZipOperatorErrorMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}
	
	public void foldOperatorOnEmptyArrayError(ASMCodeFragment frag) {
		String foldOperatorOnEmptyArrayMessage = "$fold-operator-empty-array";
		
		frag.add(DLabel, foldOperatorOnEmptyArrayMessage);
		frag.add(DataS, "Trying to apply fold operation on empty array");
		
		frag.add(Label, EMPTY_ARRAY_GIVEN_TO_FOLD);
		frag.add(PushD, foldOperatorOnEmptyArrayMessage);
		frag.add(Jump, GENERAL_RUNTIME_ERROR);
	}

	public static ASMCodeFragment getEnvironment() {
		RunTime rt = new RunTime();
		return rt.environmentASM();
	}
}
