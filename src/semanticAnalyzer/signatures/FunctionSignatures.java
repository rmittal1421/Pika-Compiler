package semanticAnalyzer.signatures;

import static asmCodeGenerator.codeStorage.ASMOpcode.ConvertF;
import static asmCodeGenerator.codeStorage.ASMOpcode.ConvertI;
import static asmCodeGenerator.codeStorage.ASMOpcode.Duplicate;
import static asmCodeGenerator.codeStorage.ASMOpcode.JumpFalse;
import static asmCodeGenerator.codeStorage.ASMOpcode.Multiply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.specialCodeGenerator.ArrayAllocationCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.ArrayCloneCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.ArrayDeallocCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.ArrayIndexingCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.ArrayLengthCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.CharToBoolCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.FloatingDivideCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.FloatingExpressOverCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.FloatingRationalizeCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.FormRationalCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.IntToBoolCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.IntToCharCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.IntegerDivideCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.RationalAdditionCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.RationalDivideCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.RationalSubtractionCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.CastFromRationalCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.CastToRationalCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.RationalExpressOverCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.RationalMultiplicationCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.RationalRationalizeCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.ShortCircuitAndCodeGenerator;
import asmCodeGenerator.specialCodeGenerator.ShortCircuitOrCodeGenerator;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Punctuator;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.TypeVariable;
import semanticAnalyzer.types.Array;
import semanticAnalyzer.types.PrimitiveType;

public class FunctionSignatures extends ArrayList<FunctionSignature> {
	private static final long serialVersionUID = -4907792488209670697L;
	private static Map<Object, FunctionSignatures> signaturesForKey = new HashMap<Object, FunctionSignatures>();

	Object key;

	public FunctionSignatures(Object key, FunctionSignature... functionSignatures) {
		this.key = key;
		for (FunctionSignature functionSignature : functionSignatures) {
			add(functionSignature);
		}
		signaturesForKey.put(key, this);
	}

	public Object getKey() {
		return key;
	}

	public boolean hasKey(Object key) {
		return this.key.equals(key);
	}

	public FunctionSignature acceptingSignature(List<Type> types) {
		for (FunctionSignature functionSignature : this) {
			if (functionSignature.accepts(types)) {
				return functionSignature;
			}
		}
		return FunctionSignature.nullInstance();
	}

	public boolean accepts(List<Type> types) {
		return !acceptingSignature(types).isNull();
	}

	/////////////////////////////////////////////////////////////////////////////////
	// access to FunctionSignatures by key object.

	public static FunctionSignatures nullSignatures = new FunctionSignatures(0, FunctionSignature.nullInstance());

	public static FunctionSignatures signaturesOf(Object key) {
		if (signaturesForKey.containsKey(key)) {
			return signaturesForKey.get(key);
		}
		return nullSignatures;
	}

	public static FunctionSignature signature(Object key, List<Type> types) {
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(key);
		return signatures.acceptingSignature(types);
	}

	/////////////////////////////////////////////////////////////////////////////////
	// Put the signatures for operators in the following static block.

	static {
		// here's one example to get you started with FunctionSignatures: the signatures
		// for addition.
		// for this to work, you should statically import PrimitiveType.*

		new FunctionSignatures(Punctuator.ADD,
				new FunctionSignature(ASMOpcode.Add, PrimitiveType.INTEGER, PrimitiveType.INTEGER,
						PrimitiveType.INTEGER),
				new FunctionSignature(ASMOpcode.FAdd, PrimitiveType.FLOATING, PrimitiveType.FLOATING,
						PrimitiveType.FLOATING),
				new FunctionSignature(new RationalAdditionCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.RATIONAL,
						PrimitiveType.RATIONAL));
		new FunctionSignatures(Punctuator.MULTIPLY,
				new FunctionSignature(ASMOpcode.Multiply, PrimitiveType.INTEGER, PrimitiveType.INTEGER,
						PrimitiveType.INTEGER),
				new FunctionSignature(ASMOpcode.FMultiply, PrimitiveType.FLOATING, PrimitiveType.FLOATING,
						PrimitiveType.FLOATING),
				new FunctionSignature(new RationalMultiplicationCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.RATIONAL,
						PrimitiveType.RATIONAL));
		new FunctionSignatures(Punctuator.SUBTRACT,
				new FunctionSignature(ASMOpcode.Subtract, PrimitiveType.INTEGER, PrimitiveType.INTEGER,
						PrimitiveType.INTEGER),
				new FunctionSignature(ASMOpcode.FSubtract, PrimitiveType.FLOATING, PrimitiveType.FLOATING,
						PrimitiveType.FLOATING),
				new FunctionSignature(new RationalSubtractionCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.RATIONAL,
						PrimitiveType.RATIONAL));
		new FunctionSignatures(Punctuator.DIVIDE,
				new FunctionSignature(new IntegerDivideCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.INTEGER,
						PrimitiveType.INTEGER),
				new FunctionSignature(new FloatingDivideCodeGenerator(), PrimitiveType.FLOATING, PrimitiveType.FLOATING,
						PrimitiveType.FLOATING),
				new FunctionSignature(new RationalDivideCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.RATIONAL,
						PrimitiveType.RATIONAL));
		new FunctionSignatures(Punctuator.OVER,
				new FunctionSignature(new FormRationalCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.INTEGER,
						PrimitiveType.RATIONAL)
				);
		new FunctionSignatures(Punctuator.EXPRESS_OVER,
				new FunctionSignature(new RationalExpressOverCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.INTEGER,
						PrimitiveType.INTEGER),
				new FunctionSignature(new FloatingExpressOverCodeGenerator(), PrimitiveType.FLOATING, PrimitiveType.INTEGER,
						PrimitiveType.INTEGER)
				);
		new FunctionSignatures(Punctuator.RATIONALIZE,
				new FunctionSignature(new RationalRationalizeCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.INTEGER,
						PrimitiveType.RATIONAL),
				new FunctionSignature(new FloatingRationalizeCodeGenerator(), PrimitiveType.FLOATING, PrimitiveType.INTEGER,
						PrimitiveType.RATIONAL)
				);
		
		TypeVariable S = new TypeVariable("S");
		List<TypeVariable> setS = Arrays.asList(S);
		
		new FunctionSignatures(Punctuator.CAST,
				// Casting to itself
				new FunctionSignature(ASMOpcode.Nop, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
				new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER),
				new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
				new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOATING, PrimitiveType.FLOATING, PrimitiveType.FLOATING),
				new FunctionSignature(ASMOpcode.Nop, PrimitiveType.STRING, PrimitiveType.STRING, PrimitiveType.STRING),
				new FunctionSignature(ASMOpcode.Nop, setS, new Array(S), new Array(S), new Array(S)),
				// Casting using existing Opcodes
				new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHARACTER, PrimitiveType.INTEGER, 
						PrimitiveType.INTEGER),
				new FunctionSignature(ASMOpcode.ConvertF, PrimitiveType.INTEGER, PrimitiveType.FLOATING,
						PrimitiveType.FLOATING),
				new FunctionSignature(ASMOpcode.ConvertI, PrimitiveType.FLOATING, PrimitiveType.INTEGER,
						PrimitiveType.INTEGER),
				// Casting using specially generated fragmented code
				new FunctionSignature(new IntToBoolCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.BOOLEAN, 
						PrimitiveType.BOOLEAN),
				new FunctionSignature(new CharToBoolCodeGenerator(), PrimitiveType.CHARACTER, PrimitiveType.BOOLEAN,
						PrimitiveType.BOOLEAN),
				new FunctionSignature(new IntToCharCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.CHARACTER, 
						PrimitiveType.CHARACTER),
				new FunctionSignature(new CastFromRationalCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.INTEGER,
						PrimitiveType.INTEGER),
				new FunctionSignature(new CastFromRationalCodeGenerator(), PrimitiveType.RATIONAL, PrimitiveType.FLOATING,
						PrimitiveType.FLOATING),
				new FunctionSignature(new CastToRationalCodeGenerator(), PrimitiveType.INTEGER, PrimitiveType.RATIONAL,
						PrimitiveType.RATIONAL),
				new FunctionSignature(new CastToRationalCodeGenerator(), PrimitiveType.CHARACTER, PrimitiveType.RATIONAL,
						PrimitiveType.RATIONAL),
				new FunctionSignature(new CastToRationalCodeGenerator(), PrimitiveType.FLOATING, PrimitiveType.RATIONAL,
						PrimitiveType.RATIONAL)
				);
		new FunctionSignatures(Punctuator.OR,
				new FunctionSignature(new ShortCircuitOrCodeGenerator(), PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN,
						PrimitiveType.BOOLEAN)
				);
		new FunctionSignatures(Punctuator.AND,
				new FunctionSignature(new ShortCircuitAndCodeGenerator(), PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN,
						PrimitiveType.BOOLEAN)
				);
		
		new FunctionSignatures(Punctuator.ARRAY_INDEXING, 
				new FunctionSignature(
					new ArrayIndexingCodeGenerator(), setS, new Array(S), PrimitiveType.INTEGER, S
				));

		for (Punctuator comparison : Punctuator.ComparisonOperators) {
			FunctionSignature iSignature = new FunctionSignature(1, PrimitiveType.INTEGER, PrimitiveType.INTEGER,
					PrimitiveType.BOOLEAN);
			FunctionSignature cSignature = new FunctionSignature(1, PrimitiveType.CHARACTER, PrimitiveType.CHARACTER,
					PrimitiveType.BOOLEAN);
			FunctionSignature fSignature = new FunctionSignature(1, PrimitiveType.FLOATING, PrimitiveType.FLOATING,
					PrimitiveType.BOOLEAN);
			FunctionSignature bSignature = new FunctionSignature(1, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN,
					PrimitiveType.BOOLEAN);
			FunctionSignature sSignature = new FunctionSignature(1, PrimitiveType.STRING, PrimitiveType.STRING,
					PrimitiveType.BOOLEAN);
			FunctionSignature rSignature = new FunctionSignature(1, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL,
					PrimitiveType.BOOLEAN);
			FunctionSignature aSignature = new FunctionSignature(1, setS, new Array(S), new Array(S),
					PrimitiveType.BOOLEAN);

			if (comparison == Punctuator.EQUAL || comparison == Punctuator.NOT_EQUAL) {
				new FunctionSignatures(comparison, iSignature, cSignature, fSignature, bSignature, sSignature, rSignature, aSignature);
			} else {
				new FunctionSignatures(comparison, iSignature, cSignature, fSignature, rSignature);
			}
		}
		
		// Signatures for unary operators
		new FunctionSignatures(Keyword.LENGTH,
				new FunctionSignature(new ArrayLengthCodeGenerator(), setS, new Array(S), PrimitiveType.INTEGER)
				);
		new FunctionSignatures(Keyword.CLONE,
				new FunctionSignature(new ArrayCloneCodeGenerator(), setS, new Array(S), new Array(S))
				);
		new FunctionSignatures(Punctuator.NOT,
				new FunctionSignature(ASMOpcode.BNegate, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
				);
		new FunctionSignatures(Keyword.DEALLOC,
				new FunctionSignature(new ArrayDeallocCodeGenerator(), setS, new Array(S), PrimitiveType.NO_TYPE)
				);
		
		// Array Signatures
		new FunctionSignatures(Keyword.ALLOC,
				new FunctionSignature(new ArrayAllocationCodeGenerator(), setS, new Array(S), PrimitiveType.INTEGER, new Array(S))
				);

		// First, we use the operator itself (in this case the Punctuator ADD) as the
		// key.
		// Then, we give that key two signatures: one an (INT x INT -> INT) and the
		// other
		// a (FLOAT x FLOAT -> FLOAT). Each signature has a "whichVariant" parameter
		// where
		// I'm placing the instruction (ASMOpcode) that needs to be executed.
		//
		// I'll follow the convention that if a signature has an ASMOpcode for its
		// whichVariant,
		// then to generate code for the operation, one only needs to generate the code
		// for
		// the operands (in order) and then add to that the Opcode. For instance, the
		// code for
		// floating addition should look like:
		//
		// (generate argument 1) : may be many instructions
		// (generate argument 2) : ditto
		// FAdd : just one instruction
		//
		// If the code that an operator should generate is more complicated than this,
		// then
		// I will not use an ASMOpcode for the whichVariant. In these cases I typically
		// use
		// a small object with one method (the "Command" design pattern) that generates
		// the
		// required code.

	}

}
