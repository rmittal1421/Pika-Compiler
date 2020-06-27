package asmCodeGenerator;

public class Record {
	public static final int RECORD_TYPEID_OFFSET = 0;
	public static final int RECORD_STATUS_OFFSET = 4;
	
	public static final int STRING_TYPE_ID = 6;
	public static final int ARRAY_TYPE_ID = 7;
	public static final int ARRAY_HEADER_SIZE = 16;
	public static final int ARRAY_STATUS_FLAGS_OFFSET = 4;
	public static final int ARRAY_SUBTYPE_SIZE_OFFSET = 8;
	public static final int ARRAY_LENGTH_OFFSET = 12;
	public static final int STATUS_FLAG_FOR_REFERENCE = 0b0010;
	public static final int STATUS_FLAG_FOR_NON_REFERENCE = 0;
	public static final int STATUS_FLAG_FOR_DELETE_AND_PERM = 0b0011;
	public static final int BINARY_F = 0b1111;
	public static final int TURN_DELETE_BIT_1 = 0b0100;
}
