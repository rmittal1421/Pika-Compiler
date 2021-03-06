package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import asmCodeGenerator.codeStorage.ASMCodeFragment;

public class Macros {
	
	public static void addITo(ASMCodeFragment frag, String location) {
		loadIFrom(frag, location);
		frag.add(Add);
		storeITo(frag, location);
	}
	public static void incrementInteger(ASMCodeFragment frag, String location) {
		frag.add(PushI, 1);
		addITo(frag, location);
	}
	public static void decrementInteger(ASMCodeFragment frag, String location) {
		frag.add(PushI, -1);
		addITo(frag, location);
	}
	
	public static void loadIFrom(ASMCodeFragment frag, String location) {
		frag.add(PushD, location);
		frag.add(LoadI);
	}
	public static void storeITo(ASMCodeFragment frag, String location) {
		frag.add(PushD, location);
		frag.add(Exchange);
		frag.add(StoreI);
	}
	public static void declareI(ASMCodeFragment frag, String variableName) {
		frag.add(DLabel, variableName);
		frag.add(DataZ, 4);
	}
	
	/** [... baseLocation] -> [... intValue]
	 * @param frag ASMCodeFragment to add code to
	 * @param offset amount to add to the base location before reading
	 */
	public static void readIOffset(ASMCodeFragment frag, int offset) {
		frag.add(PushI, offset);	// [base offset]
		frag.add(Add);				// [base+off]
		frag.add(LoadI);			// [*(base+off)]
	}
	/** [... baseLocation] -> [... charValue]
	 * @param frag ASMCodeFragment to add code to
	 * @param offset amount to add to the base location before reading
	 */
	public static void readCOffset(ASMCodeFragment frag, int offset) {
		frag.add(PushI, offset);	// [base offset]
		frag.add(Add);				// [base+off]
		frag.add(LoadC);			// [*(base+off)]
	}
	/** [...] -> [... readInt]
	 * 	@param frag ASMCodeFragment to add code to
	 *  @param baseLocation BaseLocation of the record
	 *  @param offset The offset from base address where to read the readInt from
	 */
	public static void readIPtrOffset(ASMCodeFragment frag, String baseLocation, int offset) {
		loadIFrom(frag, baseLocation);
		readIOffset(frag, offset);
	}
	/** [... intToWrite baseLocation] -> [...]
	 * @param frag ASMCodeFragment to add code to
	 * @param offset amount to add to the base location before writing 
	 */
	public static void writeIOffset(ASMCodeFragment frag, int offset) {
		frag.add(PushI, offset);	// [datum base offset]
		frag.add(Add);				// [datum base+off]
		frag.add(Exchange);			// [base+off datum]
		frag.add(StoreI);			// []
	}
	
	/** [... charToWrite baseLocation] -> [...]
	 * @param frag ASMCodeFragment to add code to
	 * @param offset amount to add to the base location before writing 
	 */
	public static void writeCOffset(ASMCodeFragment frag, int offset) {
		frag.add(PushI, offset);	// [datum base offset]
		frag.add(Add);				// [datum base+off]
		frag.add(Exchange);			// [base+off datum]
		frag.add(StoreC);			// []
	}
	
	/** [...] -> [...]
	 * 	@param frag ASMCodeFragment to add code to
	 *  @param baseLocation BaseLocation of the record
	 *  @param offset The offset from base address where the intToWrite needs to go
	 *  @param intToWrite Value of int which needs to be added
	 */
	public static void writeIPBaseOffset(ASMCodeFragment frag, String baseLocation, int offset, int intToWrite) {
		frag.add(PushI, intToWrite); // [... intToWrite]
		loadIFrom(frag, baseLocation); // [... intToWrite baseLocation]
		writeIOffset(frag, offset); // [...]
	}
	
	/** [... intToWrite] -> [...]
	 * 	@param frag ASMCodeFragment to add code to
	 *  @param baseLocation BaseLocation of the record
	 *  @param offset The offset from base address where the intToWrite needs to go
	 */
	public static void writeIPtrOffset(ASMCodeFragment frag, String baseLocation, int offset) {
		loadIFrom(frag, baseLocation); // [... intToWrite baseLocation]
		writeIOffset(frag, offset); // [...]
	}
	
	public static void loadMakePositiveStore(ASMCodeFragment frag, String baseLocation) {
		Labeller labeller = new Labeller("macro-make-positive");
		String makePositive = labeller.newLabel("make-positive");
		String endLabel = labeller.newLabel("end-label");
		
		loadIFrom(frag, baseLocation);    // [... val]
		frag.add(Duplicate);              // [... val val]
		frag.add(JumpNeg, makePositive);  // [... val] & jump if val was -ve
		frag.add(Pop);                    // [...]
		frag.add(Jump, endLabel);
		
		frag.add(Label, makePositive);    // [... val] and val is -ve
		frag.add(Negate);                 // [... val] and val is +ve
		storeITo(frag, baseLocation);     // [...]
		
		frag.add(Label, endLabel);
	}
	
	
	////////////////////////////////////////////////////////////////////
    // debugging aids

	// does not disturb accumulator.  Takes a format string - no %'s!
	public static void printString(ASMCodeFragment code, String format) {
		String stringLabel = new Labeller("pstring").newLabel("");
		code.add(DLabel, stringLabel);
		code.add(DataS, format);
		code.add(PushD, stringLabel);
		code.add(Printf);
	}
	// does not disturb accumulator.  Takes a format string
	public static void printAccumulatorTop(ASMCodeFragment code, String format) {
		String stringLabel = new Labeller("ptop").newLabel("");
		code.add(Duplicate);
		code.add(DLabel, stringLabel);
		code.add(DataS, format);
		code.add(PushD, stringLabel);
		code.add(Printf);
	}
	public static void printAccumulator(ASMCodeFragment code, String string) {
		String stringLabel = new Labeller("pstack").newLabel("");
		code.add(DLabel, stringLabel);
		code.add(DataS, string + " ");
		code.add(PushD, stringLabel);
		code.add(Printf);
		code.add(PStack);
	}
}
