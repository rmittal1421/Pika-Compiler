package symbolTable;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import inputHandler.TextLocation;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;

public class Binding {
	private Type type;
	private TextLocation textLocation;
	private MemoryLocation memoryLocation;
	private String lexeme;
	private Lextant declareLextant;
	
	public Binding(Type type, Lextant declareLextant, TextLocation location, MemoryLocation memoryLocation, String lexeme) {
		super();
		this.type = type;
		this.declareLextant = declareLextant;
		this.textLocation = location;
		this.memoryLocation = memoryLocation;
		this.lexeme = lexeme;
	}
	

	public String toString() {
		return "[" + lexeme +
				" " + type +  // " " + textLocation +	
				" " + memoryLocation +
				"]";
	}	
	public String getLexeme() {
		return lexeme;
	}
	public Type getType() {
		return type;
	}
	public Lextant getDeclareLextant() {
		return declareLextant;
	}
	public TextLocation getLocation() {
		return textLocation;
	}
	public MemoryLocation getMemoryLocation() {
		return memoryLocation;
	}
	public void generateAddress(ASMCodeFragment code) {
		memoryLocation.generateAddress(code, "%% " + lexeme);
	}
	
////////////////////////////////////////////////////////////////////////////////////
//Null Binding object
////////////////////////////////////////////////////////////////////////////////////

	public static Binding nullInstance() {
		return NullBinding.getInstance();
	}
	private static class NullBinding extends Binding {
		private static NullBinding instance=null;
		private NullBinding() {
			super(PrimitiveType.ERROR,
					Keyword.CONST,
					TextLocation.nullInstance(),
					MemoryLocation.nullInstance(),
					"the-null-binding");
		}
		public static NullBinding getInstance() {
			if(instance==null)
				instance = new NullBinding();
			return instance;
		}
	}
}
