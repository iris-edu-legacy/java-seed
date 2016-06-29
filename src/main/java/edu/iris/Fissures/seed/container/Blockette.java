package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;
import edu.iris.Fissures.seed.util.*;
import java.util.*;
import java.text.DecimalFormat;

/** 
 * Generic class of all SEED blockettes and Fixed Section Data Header (FSDH)
 * contents.  Intended to be instantiated by BlocketteFactory class.  All of
 * the constructors are protected permissions so that we can keep a rein on
 * how the objects are created and invoked...they really have a dependency
 * on the BlocketteFactory, since that class contains the background
 * information on all of the supported blockette types.
 *
 * @author Robert Casey, IRIS DMC<br>
 *          Sid Hellman, ISTI<br>
 *          Kevin Frechette, ISTI
 * @version January 2010
 * 
 */
public class Blockette extends SeedObject {
	
	/**
	 * Create a Blockette object with SEED contents from a byte array.
	 * Accept a stream of bytes that starts at byte zero of a blockette, with indicated
	 * flag to swap bytes to Java-endian (68000), flag indicating a binary data blockette,
	 * and SEED version number.
	 */
	Blockette (byte[] blocketteStream, boolean swapFlag, boolean isData, float version) throws SeedException {
		initialize(blocketteStream, swapFlag, isData, version);
	}
	
	/**
	 * Create a Blockette object with null fields that fits the indicated type.
	 * Used as a precursor for generating a new blockette for incremental field
	 * value entries.
	 * Code contributed by Sid Hellman, ISTI - 1/16/2004.
	 */
	Blockette (int blkType) throws SeedException {
		int numFields = BlocketteFactory.getNumFields(blkType,Blockette.getDefaultVersion());
		String delimiter = "|";
		String blank = "^";
		StringBuffer inputStringBuf =
			new StringBuffer(String.valueOf(blkType));  // start blockette string with type
		// supply nulls to subsequent fields
		for (int i = 1; i < numFields; i++) {
			inputStringBuf.append(delimiter);
			inputStringBuf.append(blank);
		}
		// make a new Blockette from the blank fields
		initialize(inputStringBuf.toString(),delimiter,blank);
	}
	
	/**
	 * Create a Blockette object with SEED contents from a byte array.
	 * Accept a stream of bytes that starts at byte zero of a blockette, with indicated
	 * flag to swap bytes to Java-endian (68000), and flag indicating a binary data blockette
	 * for default SEED version.
	 */
	Blockette (byte[] blocketteStream, boolean swapFlag, boolean isData) throws SeedException {
		initialize(blocketteStream, swapFlag, isData);
	}
	
	/**
	 * Create a Blockette object with SEED contents from a delimited String.
	 * Accept a delimited String as input, along with the SEED version this
	 * blockette will comply with.
	 * Delimiter characters are indicated in the parameters.
	 */
	Blockette (String inputString, String delimiter, String blank, float version) throws SeedException {
		initialize(inputString, delimiter, blank, version);
	}
	
	/**
	 * Create a Blockette object with SEED contents from a delimited String.
	 * SEED version is the default value.
	 * Delimiter characters are indicated in the parameters.
	 */
	Blockette (String inputString, String delimiter, String blank) throws SeedException {
		initialize(inputString, delimiter, blank);
	}
	
	/**
	 * Create a Blockette object with SEED contents from a delimited String.
	 * SEED version is the default value.
	 * Delimiter characters are default.
	 */
	Blockette (String inputString) throws SeedException {
		initialize(inputString);
	}
	
	/**
	 * Create a Blockette object with SEED contents from a delimited String.
	 * Accept a delimited String as input, along with the SEED version this
	 * blockette will comply with.
	 * Delimiter characters are default.
	 */
	Blockette (String inputString, float version) throws SeedException {
		// accept a default delimited string as input, along with the SEED version
		// this blockette will comply with
		initialize(inputString, version);
	}
	
	//
	// initialization methods
	//
	
	/**
	 * Resets this Blockette object to initialization state.
	 * All previous information stored is removed.
	 * Used for optimization through object reuse.
	 * Is publicly callable and readies the object to be used again.
	 * Accept a stream of bytes that starts at byte zero of a blockette, with indicated
	 * flag to swap bytes to Java-endian (68000), flag indicating a binary data blockette,
	 * and SEED version number.
	 */
	public void initialize (byte[] blocketteStream, boolean swapFlag, boolean isData, float version) throws SeedException {
		instanceInit();
		setVersion(version);
		numBytes = setByteStream(blocketteStream,swapFlag,isData);
		//System.err.println("DEBUG returned from setByteStream with " + numBytes + " bytes consumed");
		if (numBytes == 0) incompleteFlag = true;
	}
	
	/**
	 * Resets this Blockette object to initialization state.
	 * All previous information stored is removed.
	 * Used for optimization through object reuse.
	 * Is publicly callable and readies the object to be used again.
	 * Accept a stream of bytes that starts at byte zero of a blockette, with indicated
	 * flag to swap bytes to Java-endian (68000), and flag indicating a binary data blockette
	 * for default SEED version.
	 */
	public void initialize (byte[] blocketteStream, boolean swapFlag, boolean isData) throws SeedException {
		instanceInit();
		numBytes = setByteStream(blocketteStream,swapFlag,isData);
		if (numBytes == 0) incompleteFlag = true;
	}
	
	/**
	 * Resets this Blockette object to initialization state.
	 * All previous information stored is removed.
	 * Used for optimization through object reuse.
	 * Is publicly callable and readies the object to be used again.
	 * Accept a delimited String as input, along with the SEED version this
	 * blockette will comply with.  Delimiter characters are specified.
	 */
	public void initialize (String inputString, String delimiter, String blank, float version) throws SeedException {
		instanceInit();
		setVersion(version);
		setTokenString(inputString,delimiter,blank);
	}
	
	/**
	 * Resets this Blockette object to initialization state.
	 * All previous information stored is removed.
	 * Used for optimization through object reuse.
	 * Is publicly callable and readies the object to be used again.
	 * Accept a delimited String as input.
	 * Default SEED version is used.
	 * Delimiter characters are specified.
	 */
	public void initialize (String inputString, String delimiter, String blank) throws SeedException {
		instanceInit();
		setTokenString(inputString,delimiter,blank);
	}
	
	/**
	 * Resets this Blockette object to initialization state.
	 * All previous information stored is removed.
	 * Used for optimization through object reuse.
	 * Is publicly callable and readies the object to be used again.
	 * Accept a delimited String as input.
	 * Default SEED version is used.
	 * Default delimiter characters are used.
	 */
	public void initialize (String inputString) throws SeedException {
		instanceInit();
		setTokenString(inputString,"|","^");
	}
	
	/**
	 * Resets this Blockette object to initialization state.
	 * All previous information stored is removed.
	 * Used for optimization through object reuse.
	 * Is publicly callable and readies the object to be used again.
	 * Accept a delimited String as input.
	 * SEED version is specified.
	 * Default delimiter characters are used.
	 */
	public void initialize (String inputString, float version) throws SeedException {
		// accept a default delimited string as input, along with the SEED version
		// this blockette will comply with
		instanceInit();
		setVersion(version);
		setTokenString(inputString,"|","^");
	}
	
	
	/** 
	 * Like initialize() except that the Blockette is not fully reset.  Just the field values
	 * are rewritten to be the contents of the offered delimited String.
	 */
	public void setValuesFrom (String inputString, String delimiter, String blank)
	throws SeedException {
		fieldValue = new Vector(8,8);  // just blank out the field values
		setTokenString(inputString,delimiter,blank);
	}
	
	/** 
	 * Like initialize() except that the Blockette is not fully reset.  Just the field values
	 * are rewritten to be the contents of the offered delimited String.
	 */
	public void setValuesFrom (String inputString) throws SeedException {
		setValuesFrom(inputString,"|","^");
	}
	
	
	//
	// public output methods
	//
	
	/**
	 * Return the blockette type number.
	 * 0 (zero) will be returned if the blockette has not been initialized
	 */
	public int getType() {
		return blocketteType;
	}
	
	/**
	 * Return the SEED version represented in this blockette.
	 */
	public float getVersion() throws SeedException {
		if (version < minSEEDVersion || version > maxSEEDVersion) {
			throw new SeedException("Blockette version invalid");
		} else {
			return version;
		}
	}
	
	/**
	 * Return true if this blockette was not completely specified.
	 * Most likely due to a partial record used to create this blockette.
	 */
	public boolean isIncomplete() {
		return incompleteFlag;
	}
        
        /**
         * Return true if this is a response blockette -- i.e. has a stage number
         */
        public boolean isResponseBlockette() throws SeedException {
            return BlocketteFactory.isResponseBlkType(blocketteType);
        }
	
	/**
	 * Returns the number of bytes read to generate this blockette.
	 * When unset, value returned is 0.
	 * This must be used with caution.  Do not interpret the return value
	 * as being the number of bytes that the current value set in the blockette
	 * would output when written as SEED.  This is a value that is set once at
	 * blockette inception and only when generated from byte input.  Use it to test
	 * for the number of bytes read from the byte stream when making this blockette.
	 */
	public int getNumBytes() throws SeedException {
		try {
			return numBytes;
		} catch (Exception e) {
			throw new SeedException(e.toString());
		}
	}
	
	/**
	 * Convert Blockette field value to a properly formatted string.
	 * <b>fieldIndex</b> would be an index number of repeating field
	 * starting with count of 0.
	 */
	public String toString(int fieldNum, int fieldIndex) {
		try {
			// get the String value of the field Object
			Object fieldObj = getFieldVal(fieldNum,fieldIndex);
			if (fieldObj == null) {
				return new String("");
			} else {
				// make sure that numbers fit to mask template such that it is binary-ready.
				return BlocketteFactory.formatDecimal(getType(),fieldNum,fieldObj);
			}
		} catch (SeedException e) {
			//return new String ("field " + fieldNum + ", fieldIndex " + fieldIndex + ": " + e);
			System.err.println("field " + fieldNum + ", fieldIndex " + fieldIndex + ": " + e);
			e.printStackTrace();
			return new String("");
		}
	}
	
	/**
	 * Convert Blockette field value to a properly formatted string.
	 * Shortened version of toString that assumes the field is
	 * a non-repeating field (no fieldIndex).
	 */
	public String toString(int fieldNum) {
		return toString(fieldNum,0);
	}
	
	/**
	 * Convert entire Blockette contents into a tokenized string.
	 * The token delimiter is specified as a string.
	 * <b>blank</b> refers to a character that denotes a blank field value.
	 * The output can potentially be read in by the tokenized string variant
	 * of the Blockette() constructor (or setTokenString() method).
	 */
	public String toString(String delimiter,String blank) {
        return toString(delimiter,blank,null);
    }

	/**
	 * Convert entire Blockette contents into a tokenized string.
	 * The token delimiter is specified as a string.
	 * <b>blank</b> refers to a character that denotes a blank field value.
	 * The output can potentially be read in by the tokenized string variant
	 * of the Blockette() constructor (or setTokenString() method).
	 */
    public String toString(String delimiter,String blank,String blkFldSpace) {
        final int startField;  //the starting field
        if (blkFldSpace != null) {  //if printing block/field then start at field 3
           startField = 3;
        } else {
           startField = 1;
        }
        int curField = startField; // keep track of which blockette field we are looking at
		int fieldOffset = 0; // field offset for repeat groups
                String blkFldStr = null; //block field string
                DecimalFormat blkFormat = null; //block type format
                DecimalFormat fldFormat = null; //field number format
		try {
                        if (blkFldSpace != null) { //if printing block/field
                          blkFormat = new DecimalFormat("000");
                          fldFormat = new DecimalFormat("00");
                        }
			StringBuffer outputStr = new StringBuffer("");  // this will be our output
			int numFields = getNumFields();
                        
                        //////////////////////////////////
                        // DEBUG
                        //System.err.println("DEBUG (to_string): blockette type=" + getType() + ", id=" + getLookupId());
                        //while (curField <= numFields) {
                        //    Object rptVal = getFieldVal(curField);
                        //    System.err.println("DEBUG (to_string): field=" + curField + ", val=" + rptVal);
                        //    curField++;
                        //}
                        //curField = 1;  // reset curField counter
                        // end DEBUG
                        //////////////////////////////////
                        
			while (curField <= numFields) {
				if (curField > 1) {
					// put our delimiter in between the field values
					outputStr.append(delimiter);
				}
                                if (blkFldSpace != null) { //if printing block/field
                                  blkFldStr = "B" + blkFormat.format(getType()) + "F" +
                                      fldFormat.format(curField);
                                }
				int rptIndex = getFieldRepeat(curField);
				if (rptIndex > 0) {
					// if this is a repeat field, we are dealing with multiple values
					// reference the field number listed in rptIndex
					Object rptVal = getFieldVal(rptIndex);  // get field value object
					if (rptVal == null) {
						throw new SeedException("Number of repeat fields is null: field num = "
								+ rptIndex);
//						setFieldVal(rptIndex,"0");  // force this value to be zero
					}
					int rptSize = Integer.parseInt(rptVal.toString()); // get the number of repeats
					int grpSize = 0;  // number of fields in the group
					for (int i = 0; i < rptSize; i++) {
						// iterate through each of the values
						Vector grpVec = getFieldGrp(curField,i); // we get an entire repeat group of fields
						grpSize = grpVec.size();                 // and derive the number of fields from the Vector size
                                                if (blkFldSpace != null) { //if printing block/field
                                                  if (i > 0)
                                                    outputStr.append(delimiter);
                                                  outputStr.append(blkFldStr + "-" +
                                                      fldFormat.format(curField + grpSize - 1) +
                                                      blkFldSpace);
                                                  outputStr.append(i + blkFldSpace);
                                                }
						for (int j = 0; j < grpSize; j++) {
							if (j > 0 || i > 0) {
                                                          if (blkFldSpace != null) { //if printing block/field
                                                            if (j > 0)
                                                            {
                                                              outputStr.append(blkFldSpace);
                                                            }
                                                          } else {
								outputStr.append(delimiter);
							}
							}
							fieldOffset = j;  // remember this in case of exception reporting
							// get each object and format the string derived from the object
							String fmtStr = BlocketteFactory.formatDecimal(getType(),curField+j,grpVec.get(j));
							// check to see if the string is empty (blank), which means that we fill the field
							// with the special character 'blank'
							if (fmtStr.length() == 0) fmtStr = new String(blank);
							outputStr.append(fmtStr);
						}
					}
					// in the case of there being zero repeating field groups, grpSize will be zero
					// so instead fill the current field with blank and increment the curField by 1
					if (grpSize == 0) {
                                                if (blkFldSpace != null) { //if printing block/field
                                                  outputStr.append(blkFldStr + blkFldSpace + blkFldSpace);
                                                }
						outputStr.append(blank);
						curField ++;  // increment the current field by 1
					} else {
						curField += grpSize;   // increment the current field by the last sample group size
					}
				} else {
					// we are dealing with a single value
                                        if (blkFldSpace != null) { //if printing block/field
                                          outputStr.append(blkFldStr + blkFldSpace + blkFldSpace);
                                        }
					String fmtStr = toString(curField,0);
					// check to see if the string is empty (blank), which means that we fill the field
					// with the special character 'blank'
					if (fmtStr.length() == 0) fmtStr = new String(blank);
					outputStr.append(fmtStr);
					curField++;
				}
			}
			return outputStr.toString();
		} catch (SeedException e) {
			//return new String ("field " + curField + ": " + e);
			int fldNum = curField + fieldOffset;  // which field did the error occur on?
			System.err.println("field " + fldNum + ": " + e);
			e.printStackTrace();
			return new String("");
		}
	}
	
	/**
	 * Convert entire Blockette contents into a block/field string.
         * The output has a line for each field in the following format:
         * F__B___ VALUE
	 */
	public String toBlkFldString() {
		String delimiter = "\n";
		String blank = "(null)";
                String blkFldSpace = "\t";
		return toString(delimiter,blank,blkFldSpace);
	}

	/**
	 * Convert entire Blockette contents into a tokenized string.
	 * Uses a default token and default blank marker.
	 */
	public String toString() {
		String delimiter = "|";
		String blank = "^";
		return toString(delimiter,blank);
	}
	
	/**
	 * Get the full name of this blockette.
	 */
	public String getName() throws SeedException {
		return BlocketteFactory.getName(getType());
	}
	
	/**
	 * Get the control header type of this blockette.
	 */
	public String getCategory() throws SeedException {
		return BlocketteFactory.getCategory(getType());
	}
	
	/**
	 * Get the number of available fields in this blockette.
	 */
	public int getNumFields() throws SeedException {
		return BlocketteFactory.getNumFields(getType(),version);
	}
	
	/**
	 * Get the full name of the specified field.
	 */
	public String getFieldName(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldName(getType(),fieldNum);
	}
	
	/**
	 * Get the letter that represents the field type.
	 */
	public String getFieldType(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldType(getType(),fieldNum);
	}
	
	/**
	 * Get the length of the field in bytes.
	 * This is a string return because the field length for type V
	 * fields is of the form 'm-n', where m is the minimum value
	 * and n is the maximum value.
	 */
	public String getFieldLength(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldLength(getType(),fieldNum);
	}
	
	/**
	 * Get the string expression of the mask or flag for this field.
	 */
	public String getFieldMask(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldMask(getType(),fieldNum);
	}
	
	/**
	 * Get the field number that indicates the number of times this field repeats.
	 * A value of 0 means that this field does not repeat.
	 */
	public int getFieldRepeat(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldRepeat(getType(),fieldNum);
	}
	
	/**
	 * Return the entire description of this blockette type.
	 */
	public String getDefinition() throws SeedException {
		return BlocketteFactory.getBlocketteDefinition(getType());
	}
	
	/**
	 * Returns the value at the field number...NOTE that this is an Object.
	 * Use toString() to get string equivalent of returned Object
	 * and indexed iteration of that field if it is repeating.
	 * For a non-repeating field, fieldIndex is ignored.
	 * Null is returned if a stored null value is encountered.
	 * An out of bounds field number results in a thrown exception.
	 */
	public Object getFieldVal(int fieldNum, int fieldIndex) throws SeedException {
		int maxFieldNum = fieldValue.size() - 1;  // no fieldNum greater than this number allowed
		if (fieldNum > maxFieldNum || fieldNum < 1) {
			throw new SeedException("field number " + fieldNum + " out of bounds (blktype = " + getType() +
					"max = " + maxFieldNum + ")");
		}
		// get the existing value at that location (fieldIndex 0)
		Object fieldVal = fieldValue.get(fieldNum);
		if (fieldVal == null) return null;  // return null if there is a null value here
		int repeatField = getFieldRepeat(fieldNum);
		if (repeatField > 0) {
			// get value at fieldIndex of fieldNum
			Vector fieldVec = (Vector) fieldVal;
			if (fieldIndex >= fieldVec.size()) {
				fieldVal = null;   // referencing too high a field index
			} else {
				fieldVal = fieldVec.get(fieldIndex);
			}
		} else if (fieldIndex > 0) return null;  // non-repeating; nothing beyond field index of 0
		return fieldVal;
	}
	
	/**
	 * Returns a single object value at the field number indicated.
	 * For repeating fields, the object returned is the first value
	 * in the repeating field.
	 */
	public Object getFieldVal(int fieldNum) throws SeedException {
		return getFieldVal(fieldNum,0);
	}
	
	/**
	 * Get a repeating field group cross-section in the form of a vector.
	 * Returns a Vector that contains all of the fields in a repeat group,
	 * returning the value at <b>fieldIndex</b> in each, starting at
	 * <b>fieldNum</b>.  An example of a repeat group is Response Blockette
	 * 53, fields 10 through 13.  In wanting to get the third quartet of zero
	 * coefficients, the call to getFieldGrp would be fieldNum=10, fieldIndex=2.
	 * The result would be a Vector of size 4, with the third value from fields
	 * 10,11,12, and 13, in order.
	 */
	public Vector getFieldGrp(int fieldNum, int fieldIndex) throws SeedException {
		int numFields = getNumFields();   // get the number of fields in this blockette
		if (fieldNum > numFields) {
			throw new SeedException ("fieldNum is larger than the number of blockette fields (" + numFields + ")");
		}
		int repeatField = getFieldRepeat(fieldNum);   // field number indicating number of groups
		if (repeatField == 0) {  // throw an exception if this field is not part of a repeat group
			throw new SeedException("This field is not part of a repeat group");
		}
		Object rptVal = getFieldVal(repeatField);  // get field value object
		if (rptVal == null) {
			throw new SeedException("Number of repeat fields is null: field num = "
					+ repeatField);
//			setFieldVal(repeatField,"0");  // force this value to be zero
		}
		int grpSize = Integer.parseInt(rptVal.toString());  // number of groups
		if (fieldIndex > grpSize - 1) {
			throw new SeedException ("fieldIndex is larger than the index of the last repeating field (" +
					new Integer(grpSize-1) + ")");
		}
		Vector returnVector = new Vector();
		// pass through the group - boundary detect at fieldRepeat value of 0 OR end of numFields
		for (int i = 0; fieldNum+i <= numFields; i++) {
			if (getFieldRepeat(fieldNum+i) == 0) break;
			Object fieldVal = getFieldVal(fieldNum+i,fieldIndex);
			returnVector.add(fieldVal);
		}
		return returnVector;
	}
	
	/**
	 * Return the raw object assigned to this field number, regardless of the contents, or whether it is
	 * a repeat field or a repeat field with list values.
	 * 
	 * @param fieldNum field number to return the assigned object from
	 * @return row object assigned to this field number
	 */
	public Object getFieldObject(int fieldNum) {
		return fieldValue.get(fieldNum);
	}
	
	/**
	 * Translate the value located at the blockette's field number, if there is one.
	 * Some values in blockettes translate to a more elaborate description.
	 * Translate will return a specific String if there is a description to
	 * match the value at the blockette's field number.  Otherwise, a default
	 * return such as 'Value Undefined' is returned.
	 */
	public String translate(int fieldNum) throws SeedException {
		return BlocketteFactory.getTranslation(getType(),fieldNum,getFieldVal(fieldNum));
	}
	
	/**
	 * Get the child blockette associated with this blockette from the specified
	 * Vector index.  Returns null in the case of the index exceeding the size of
	 * the Vector.
	 */
	public Blockette getChildBlockette (int index) {
        // DEBUG
        //System.err.println("DEBUG: childBlocketteVec: " + childBlocketteVec);
        //
		if (index+1 > childBlocketteVec.size()) {
			return null;
		} else {
			return childBlocketteVec.getBlockette(index);
		}
	}
	
	/**
	 * Return the number of child blockettes stored in this blockette.
	 */
	public int numberofChildBlockettes() {
        // DEBUG
        //if (getType() == 50)
        //    System.err.println("DEBUG: " + lookupId + ", class: " + this.getClass().getName());
        //
		return (childBlocketteVec.size());
	}
	
	/**
	 * Return true if this Blockette has a designated parent.
	 */
	public boolean hasParent() {
		return (parentBlockette != null);
	}
	
	/**
	 * Return the designated parent Blockette object.  The value could be null,
	 * which means that as far as this Blockette is concerned, there is no parent.
	 */
	public Blockette getParentBlockette() {
		return parentBlockette;
	}
	
	/**
	 * Get the attached waveform object.  Return null if no Waveform object is
	 * attached.
	 */
	public Waveform getWaveform () {
		return waveformData;
	}
	
	/**
	 * Get the dictionary lookup value from the specified Vector index.
	 * Returns 0 in the case of the index exceeding the size of the Vector or
	 * no lookup present.
	 */
	public int getDictionaryLookup (int index) {
		//System.err.println("DEBUG: " + lookupId + " dictionary lookupMap=" + lookupMap + ", get index=" + index);
		if (index >= lookupMap.size()) {  // map size exceeded?
			return 0;
		} else {
			Object lookupObj = lookupMap.get(index);
			if (lookupObj == null) return 0;
			return Integer.parseInt(lookupObj.toString());
		}
	}
	
	/**
	 * Return the number of dictionary entries in the lookupMap.
	 * Note that the lookupMap starts counting from 1, not 0.  Position 0 is
	 * reserved for internal use.
	 */
	public int numberofDictionaryLookups () {
		return (lookupMap.size()-1);
	}
	
	/**
	 * Return the lookupId for this blockette.
	 */
	public int getLookupId () {
		return lookupId;
	}
        
    /**
     * Return the stage number of this blockette IF it is a response blockette.
     * Otherwise, return a -1
     */
    public int getStageNumber() throws SeedException {
        int fieldNum = BlocketteFactory.getStageField(blocketteType);
        if (fieldNum < 1) return -1;
        return ((Integer)(getFieldVal(fieldNum))).intValue();
    }
	
	//
	// public input methods
	//

    /**
     * Set the stage number of this blockette to the indicated value, but only
     * if it is a response blockette.
     */
    public void setStageNumber(Integer stageNum) throws SeedException {
        int fieldNum = BlocketteFactory.getStageField(blocketteType);
        if (fieldNum < 1) return;  // not present
        setFieldVal(fieldNum, stageNum);  // set the stage number
    }

    // synonym for the above method
    public void setStageNumber(int stageNum) throws SeedException {
        setStageNumber(new Integer(stageNum));
    }


	/**
	 * Set the SEED version that this blockette represents.
	 */
	public void setVersion(float ver) throws SeedException {
		if (ver < minSEEDVersion || ver > maxSEEDVersion) {
			throw new SeedException("Blockette version invalid");
		} else {
			version = ver;
		}
	}
	
	/**
	 * Apply value contained in the provided object to the indicated field
	 * position in the blockette.
	 * Sets the value at the blockette's field number (and repeat index
	 * of that field if it is repeating).
	 * For a non-repeating field, fieldIndex is ignored.
	 * The lenient flag, when set to true, means this method will not return
	 * an exception with an improperly formatted value, but instead place
	 * a substitute character before adding it to the blockette.
	 */
	public void setFieldVal(int fieldNum, int fieldIndex,
			Object value, boolean lenient) throws SeedException {
		// not allowed to change field 1, which is the blockette type 
		if (fieldNum == 1) {
			throw new SeedInputException("Cannot alter field number " + fieldNum);
		}
		// write the field value, whether a repeating field or non-repeating field
		if (value == null) value = "";  // fault-tolerance...set a null to empty String
		String stringVal = value.toString();
		setFieldString(getType(),fieldNum,fieldIndex,stringVal,lenient);
		update();
	}
	
	/**
	 * Apply value contained in the provided object to the indicated field
	 * position in the blockette.
	 * Sets the value at the blockette's field number (and repeat index
	 * of that field if it is repeating).
	 * For a non-repeating field, fieldIndex is ignored.
	 */
	public void setFieldVal(int fieldNum, int fieldIndex, Object value) throws SeedException {
		setFieldVal(fieldNum, fieldIndex, value, true);  // defaults to lenient
	}
	
	/**
	 * Apply value contained in the provided object to the indicated field
	 * position in the blockette.  This method variant assumes the indicated field
	 * is not a repeat field.
	 */
	public void setFieldVal(int fieldNum, Object value) throws SeedException {
		setFieldVal(fieldNum,0,value);
	}
	
	/**
	 * Similar to setFieldVal, except that the provided object can be whatever construct is needed to
	 * reflect the field contents.  A single value, a vector of values, or a vector of vectors with values.  This
	 * allows bulk setting of a field, but must be used with caution.  getFieldObject is the accessor method that
	 * complements this.
	 * @param fieldNum field number to apply this object to
	 * @param fieldObj object to apply to the indicated field number
	 */
	public void setFieldObject(int fieldNum, Object fieldObj) {
		fieldValue.setElementAt(fieldObj, fieldNum);
	}
	
	/**
	 * Add a new repeat field group starting at the specified field number.
	 * <b>valueVec</b> should be a Vector of sufficient size to contain Objects
	 * to fill all of the fields in the repeat field group.
	 * i.e. if a repeat field group starts at field 7 and goes through field 9,
	 * then fieldNum should be 7 and valueVec should be size 3 with 3 assigned
	 * Objects to place in those fields...otherwise, an exception will be thrown.
	 * <b>repeatField</b> contains the field number that indicates the number of
	 * Repeating fields currently present in the blockette...it also acts to 
	 * verify that this field is a repeating field.
	 * For the special case of Blockette 60, field 6 must be a Vector of Integers placed 
	 * in valueVec.
	 * Be careful using this so that you get the intended result!
	 */
	public void addFieldGrp(int fieldNum, Vector valueVec) throws SeedException {
		int repeatField = getFieldRepeat(fieldNum);
		if (repeatField == 0) {
			throw new SeedInputException("Blockette field " + fieldNum + " not listed as a repeating field");
		}
		// we need to track the number of fields we have in this blockette
		int numFields = getNumFields();
		// we need to record the repeat field value for each field number iteration below
		int curRepeatField = repeatField;
		// track what fieldnum we are currently referencing
		int lastField = fieldNum;
		// go forward through each field until the curRepeatField reference changes or
		// we run out of blockette fields...record the last field number
		while (curRepeatField == repeatField) {
			// peek ahead to the next field
			lastField++;
			if (lastField > numFields) break;  // no more fields
			curRepeatField = getFieldRepeat(lastField);
		}
		// now that we know how many fields are in the group, do a size compare
		// to the input value vector
		int grpSize = lastField - fieldNum;
		if (valueVec.size() != grpSize) {
			throw new SeedInputException ("invalid Vector size " + valueVec.size() + 
					" (need " + grpSize + ")");
		}
		// now add the values to each field
		int fSize = 0;
		for (int i=0; i<grpSize; i++) {
			Vector tmpVec = (Vector) fieldValue.get(fieldNum+i);
			if (tmpVec == null) {
				// needs to be initialized with Vector
				fieldValue.set(fieldNum+i, new Vector(1,1));
				tmpVec = (Vector) fieldValue.get(fieldNum+i);
			}
			fSize = tmpVec.size();  // get the number of entries in vector
			// write the value...the following method will resize the field Vector plus one
			String stringVal = valueVec.get(i).toString();
			// DEBUG
			//System.err.println("DEBUG:addFldGrp(type=" + getType() +
			//		", fieldNum+i=" + fieldNum+i +
			//		", fSize=" + fSize + ", stringVal=" + stringVal + ")");
			setFieldString(getType(),fieldNum+i,fSize,stringVal);
		}
		// increment the blockette's number of repeating fields for the group
                // max value of 9999
                fSize++;
                if (fSize > 9999) fSize = 9999;
		String stringVal = Integer.toString(fSize);
		//System.err.println("DEBUG: addFldGrp increment number of repeating fields for blkType=" +
		//		getType() + ", fieldNumber=" + repeatField + " to " + stringVal);
		setFieldString(getType(),repeatField,0,stringVal);
		// update the blockette mtime
		update();
	}
	
	/**
	 * Insert a repeat group before the indicated index.  This is much like
	 * addFieldGrp, except that it serves as an insert function instead of an
	 * append function.  Insert group at fieldIndex, with the first field of the
	 * group being fieldNum, consisting of the values in valueVec.
	 */
	public void insertFieldGrp(int fieldNum, int fieldIndex,
			Vector valueVec) throws SeedException {
		int repeatField = getFieldRepeat(fieldNum);  // find the field where we track quantity
		if (repeatField == 0) {
			throw new SeedInputException("Blockette field " + fieldNum +
			" not listed as a repeating field");
		}
		// we need to track the number of fields we have in this blockette
		int numFields = getNumFields();
		// we need to record the repeat field value for each field number iteration below
		int curRepeatField = repeatField;
		// track what fieldnum we are currently referencing
		int lastField = fieldNum;
		// go forward through each field until the curRepeatField reference changes or
		// we run out of blockette fields...record the last field number
		while (curRepeatField == repeatField) {
			// peek ahead to the next field
			lastField++;
			if (lastField > numFields) break;  // no more fields
			curRepeatField = getFieldRepeat(lastField);
		}
		// now that we know how many fields are in the group, do a size compare
		// to the input value vector
		int grpSize = lastField - fieldNum;
		if (valueVec.size() != grpSize) {
			throw new SeedInputException ("invalid Vector size " + valueVec.size() + 
					" (need " + grpSize + ")");
		}
		// now insert the values in valueVec to a new group
		// at fieldIndex, right-shifting the original fieldIndex
		// and remainder groups.
		int fSize = 0;
		for (int i=0; i<grpSize; i++) {    // for each field num in group
			Vector tmpVec = (Vector) fieldValue.get(fieldNum+i); // vector of multiple values for this field
			if (tmpVec == null) {
				// needs to be initialized with Vector
				fieldValue.set(fieldNum+i, new Vector(1,1));
				tmpVec = (Vector) fieldValue.get(fieldNum+i);
			}
			fSize = tmpVec.size();  // get the number of entries in vector
			String stringVal = valueVec.get(i).toString();  // string value of what we want to insert
			//System.err.println("DEBUG: stringVal = " + stringVal);
			if (fSize == 0 || fieldIndex >= fSize) {
				// insertions at the beginning or very end
				//System.err.println("DEBUG: insert at beginning or end: setFieldString(" +
				//	getType() + ", " + (fieldNum+i) + ", " + fSize + ", " +
				//	stringVal + ")");
				setFieldString(getType(),fieldNum+i,fSize,stringVal);
			} else {
				Object copyObj = tmpVec.get(fieldIndex);
				tmpVec.insertElementAt(copyObj,fieldIndex);    // insert copy element at fieldIndex
				//System.err.println("DEBUG: insert in middle: setFieldString(" +
				//	getType() + ", " + (fieldNum+i) + ", " + fieldIndex + ", " +
				//	stringVal + ")");
				setFieldString(getType(),fieldNum+i,fieldIndex,stringVal); // copy new value to fieldIndex
			}
		}
		// increment the blockette's number of repeating fields for the group
                // max value of 9999
                fSize++;
                if (fSize > 9999) fSize = 9999;
		String stringVal = Integer.toString(fSize);
		//System.err.println("DEBUG: insFldGrp increment number of repeating fields for blkType=" +
		//		getType() + ", fieldNumber=" + repeatField + " to " + stringVal);
		setFieldString(getType(),repeatField,0,stringVal);
		// update the blockette mtime
		update();
	}
	
	
	/**
	 * Remove a repeat group at the indicated index.  Causes the number of repeat
	 * groups to decrease by 1.  Delete the group at fieldIndex, where the first
	 * field of the group being fieldNum.
	 */
	public void deleteFieldGrp(int fieldNum, int fieldIndex) throws SeedException {
		int repeatField = getFieldRepeat(fieldNum);
		if (repeatField == 0) {
			throw new SeedInputException("Blockette field " + fieldNum +
			" not listed as a repeating field");
		}
		int numFields = getNumFields();
		int curRepeatField = repeatField;
		int lastField = fieldNum;
		while (curRepeatField == repeatField) {
			lastField++;
			if (lastField > numFields) break;  // no more fields
			curRepeatField = getFieldRepeat(lastField);
		}
		int grpSize = lastField - fieldNum;
		int fSize = 0;
		for (int i=0; i<grpSize; i++) {    // for each field num in group
			Vector tmpVec = (Vector) fieldValue.get(fieldNum+i); // vector of multiple values for this field
			if (tmpVec == null) {
				// needs to be initialized with Vector
				fieldValue.set(fieldNum+i, new Vector(1,1));
				tmpVec = (Vector) fieldValue.get(fieldNum+i);
			}
			fSize = tmpVec.size();  // get the number of entries in vector
			if (fSize == 0) return; // there is nothing here to delete
			if (fieldIndex >= fSize) {
				tmpVec.remove(fSize-1);  // index too high, but remove from end
			} else {
				tmpVec.remove(fieldIndex);  // remove where indicated
			}
		}
		// decrement the blockette's number of repeating fields for the group 
		String stringVal = Integer.toString(fSize-1);
		setFieldString(getType(),repeatField,0,stringVal);
		// update the blockette mtime
		update();
	}
        
        	/**
	 * Completely erase contents of a field group where the first
	 * field of the group is fieldNum.  Sets the repeatField count to zero.
         * @return number of fields in group purged
	 */
	public int purgeFieldGrp(int fieldNum) throws SeedException {
		int repeatField = getFieldRepeat(fieldNum);
		if (repeatField == 0) {
			throw new SeedInputException("Blockette field " + fieldNum +
			" not listed as a repeating field");
		}
		int numFields = getNumFields();
		int curRepeatField = repeatField;
		int lastField = fieldNum;
		while (curRepeatField == repeatField) {
			lastField++;
			if (lastField > numFields) break;  // no more fields
			curRepeatField = getFieldRepeat(lastField);
		}
		int grpSize = lastField - fieldNum;  // get the repeat group size
		for (int i=0; i<grpSize; i++) {    // for each field num in group
			fieldValue.set(fieldNum+i, null);  // null out the repeat vector for this fieldnum
		}
		// set the number of repeating fields to zero 
		String stringVal = "0";
		setFieldString(getType(),repeatField,0,stringVal);
		// update the blockette mtime
		update();
                return grpSize;
	}
	
	
	/**
	 * Add a child blockette to this blockette.
	 * Append another already-created blockette to the Vector
	 * list of this blockette, indicating that the added blockette
	 * is heirarchically a 'child' of this blockette, whose data is
	 * a subcomponent of this blockette.
	 * @param addBlockette the blockette to be added as a child to this blockette
	 * @return the index where the addition was made.
	 */
	public int addChildBlockette (Blockette addBlockette) {
		childBlocketteVec.add(addBlockette);   // class BlocketteVector
		// DEBUG
        //if (getType() == 50) {
        //    System.err.println("DEBUG: addChildBlockette...");
        //}
        //
		return (childBlocketteVec.size() - 1);
	}
	
	/**
	 * Remove child blockette reference at the indicated index.
	 * Use getChildBlockette() to find which blockette is at 
	 * which index value.	
	 */
	public void removeChildBlockette (int index) {
		childBlocketteVec.remove(index);  // shifts remaining blockettes to the left
	}
	
	/**
	 * Replace child blockette at the indicated index with a new
	 * blockette.
	 */
	public void replaceChildBlockette(int index, Blockette newBlockette) {
		childBlocketteVec.set(index,newBlockette);
	}
	
	/**
	 * Designate the provided Blockette as this Blockette's parent.
	 */
	public void attachParent(Blockette parent) {
		parentBlockette = parent;
	}
	
	/**
	 * Remove the parent reference in this Blockette.
	 */
	public void removeParent() {
		parentBlockette = null;
	}
	
	/**
	 * Attach waveform object to this blockette.  Usually this applies
	 * only to FSDH blockette (999).
	 */
	public void attachWaveform (Waveform data) {
		waveformData = data;
		//System.err.println("DEBUG: " + waveformData.toString());
	}
	
	/**
	 * Remove the attached waveform object from this blockette.
	 */
	public void removeWaveform () {
		waveformData = null;
	}
	
	/**
	 * Add lookupId value from an outside abbreviation dictionary blockette
	 * to the lookupMap Vector in this blockette.  The number returned is
	 * the index value where the lookupId has been placed.  It is recommended
	 * that this return value is written to the appropriate blockette field
	 * that is used to reference the dictionary blockette.
	 */
	public int addDictionaryLookup(int abbrevLookupId) {
		if (lookupMap.size() == 0) lookupMap.add(new Integer(0));  // 0th index reserved
		lookupMap.add(new Integer(abbrevLookupId));  // store in Integer form
		//System.err.println("DEBUG: " + lookupId + " dictionary lookupMap add: " + lookupMap);
		return (lookupMap.size() - 1);
	}
	
	/**
	 * Add lookupId value from an outside abbreviation dictionary blockette
	 * to the lookupMap Vector in this blockette if it is not already there.  
	 * The number returned is the index value where the lookupId has been
	 * placed.  It is recommended that this return value is written to the
	 * appropriate blockette field that is used to reference the dictionary
	 * blockette.
	 * Contributed by Sid Hellman, ISTI - 1/16/2004
	 */
	public int addDictionaryLookupIfNeeded(int abbrevLookupId) {
		if (lookupMap.size() == 0) lookupMap.add(new Integer(0));  // 0th index reserved
		int lucount = this.numberofDictionaryLookups();
		int curid = -1;
		for (int i = 1; i <= lucount; i++) {
			curid = getDictionaryLookup(i);
			if (curid == abbrevLookupId) {
				return i;
			}
		}
		return this.addDictionaryLookup(abbrevLookupId);
	}
	
	/**
	 * Set the lookupId value of an abbreviation dictionary blockette for
	 * a corresponding index value, replacing an existing entry at that index.
	 * Typically used for changing an index reference.
	 */
	public void setDictionaryLookup(int index, int abbrevLookupId) {
		if (index >= lookupMap.size()) {
			lookupMap.setSize(index+1);
		}
		lookupMap.set(index,new Integer(abbrevLookupId));
	}
	
	/**
	 * Set the lookupId value of this blockette.
	 * It can be used as a reference from other blockettes.
	 * Typically used by dictionary blockettes as their reference number.
	 */
	public void setLookupId (int idValue) {
		lookupId = idValue;
	}
	
	/**
	 * Get the modification time of the blockette.
	 * This is the timestamp of the last known instance of modifying the
	 * contents of the blockette.
	 */
//	public Btime getMTime () {
//		return (Btime) fieldValue.get(0);  // the 0th field contains the modification time
//	}
	
	
	/**
	 * Get the default SEED version for Blockettes.
	 * This is a static method!
	 */
	public static float getDefaultVersion() {
		return defaultSEEDVersion;
	}
	
	
	//
	// private / protected methods
	//
	
	/**
	 * Establish the blockette contents through a byte stream written
	 * in SEED blockette format.
	 * <b>swapFlag</b> will indicate that word groups need to be byte swapped.
	 * <b>isData</b> flags that we are loading a data blockette, which 
	 * has a 2-byte type value instead of 3.
	 * The value returned is the number of bytes read from the beginning of
	 * <b>blocketteStream</b>.
	 */
	protected int setByteStream(byte[] blocketteStream,boolean swapFlag,boolean isData) throws SeedException {
		//DEBUG
		//String debugStr = new String(blocketteStream,0,blocketteStream.length);
		//System.err.println("DEBUG blocketteStream: >" + debugStr + "< -- swapFlag = " + swapFlag + ", isData = " + isData);
		int curField = 0;  // points to which field number we are working on
		//int fieldIndex = 0;  // this is the index for repeating fields
		int fldCount = 0;  // number of fields to fill
		int arrIndex = 0;  // points to where we are in the input array
		int indexCount = 0;  // this will be the expected count of repeating fields
		int indexCountStart = 0;  // keeps track of where the count started
		int rptGrpStart = 0;  // what field the repeat group starts on
		int blkType = 0;   // this is the blockette type
		int bInt = 0;      // integer representation of a binary word
		String blkStr = "";    // String representation of blockette value
		String fldType = "";  // stores the field type flag for a given field
		//
		// number of bytes suggested for this blockette -- used as a hint for binary
		// import operations.
		int suggestBytes = 0;
		//
		// loop until we reach the end of the blockette stream.
		// (less-than-equals done on purpose, we will check the equals case again later)
		//
		while (arrIndex <= blocketteStream.length) {
			// For unexpectedly short blockettes, we also check against the suggested
			// blockette length.  If we break, treat is as a complete blockette
			// with null trailing fields.
			//System.err.println("DEBUG: arrIndex=" + arrIndex +
			//	    " .lt. suggestBytes=" + suggestBytes + "?");
			if (suggestBytes > 0 && arrIndex >= suggestBytes) { // ran out of bytes, unexpectedly
				break;
			}
			curField++;
			if (curField == 1) {  // field 1 indicates the blockette type
				blkStr = null;
				int indexIncrement = 0;
				if (isData) {
					// data blockette type
					if (blocketteStream.length-arrIndex < 2) {    // severely truncated
						incompleteFlag = true;
						return blocketteStream.length;
					}
					byte[] sampArr = new byte[2];
					System.arraycopy(blocketteStream,arrIndex,sampArr,0,2);
					bInt = Utility.uBytesToInt(sampArr[0],sampArr[1],swapFlag);
					blkStr = Integer.toString(bInt);
					indexIncrement = 2;
				} else {
					// metadata blockette type
					if (blocketteStream.length-arrIndex < 3) {   // severely truncated
						incompleteFlag = true;
						return blocketteStream.length;
					}
					blkStr = new String(blocketteStream,arrIndex,3);
					indexIncrement = 3;
				}
				// make sure that the characters are digits or digits with leading spaces
				boolean retryLoop = false;
				for (int d = 0; d < blkStr.length(); d++) {
					char a = blkStr.charAt(d);
					if (! Character.isDigit(a)) {
						// perhaps this is a leading space...okay until we get to the last digit
						if (a == ' ' && d+1 < blkStr.length()) continue;
						// this is not a correct condition...maybe an improperly
						// trailing character from the last blockette, or something we didn't account for.
						// Let's try to remedy this.
						System.err.println("WARNING: encountered odd character where expecting blockette type: '" + a + "'.");
						System.err.println("LOCATION: " +
								new String(blocketteStream,arrIndex,blocketteStream.length));
						System.err.println("CORRECTION: retry reading SEED record, byte shifted by one.");
						indexIncrement = 1;
						curField--;  // this will be re-incremented
						retryLoop = true;
						break;
					}
				}
				arrIndex += indexIncrement;  // possible index increment before continuing
				if (retryLoop) {
					continue;  // go back to top of loop
				}
				try {
					//System.err.println("DEBUG: blkStr=" + blkStr);
					blkType = Integer.parseInt(blkStr.trim());    // now we have the blockette type
					// DEBUG
					//if (blkType == 70) System.err.println("DEBUG: blkStr=" + blkStr);
				} catch (Exception e) {
					throw new SeedInputException("ERROR in reading next blockette type from string: '" +
							blkStr + "' in byte stream: " +
							new String(blocketteStream,0,blocketteStream.length));
				}
				// DEBUG
				//if (blkType == 70) System.err.println("DEBUG: blkType = " + blkType);
				float blkVer = BlocketteFactory.getVersion(blkType);
				if (blkVer > version) {   // is our version designation too old for this declared blockette?
					throw new SeedInputException("blockette type " + blkType + " is a version " + blkVer + " blockette");
				}
				fldCount = BlocketteFactory.getNumFields(blkType,version);  // and the number of fields to fill
				fieldValue.setSize(fldCount+1);  // set blockette Vector to be properly sized
			} else {
				// we have to know which field we are supposed to be looking at so that we can both
				// read from the byte stream correctly and write to the proper field in this object
				//
				// field count boundary check
				if (curField > fldCount) {  // there may be bytes beyond the field count, which is acceptable and ignored
					if (indexCount < 2) {   // 0 or 1, we have no more repeating fields
						break;   // break from while loop
					} else {  // else we still have some repeat fields to enter
						indexCount--;
						curField = rptGrpStart;  // rewind to beginning of repeat group
					}
				}
				// see if this field is repeating, and make sure a Vector is present there
				int fldRpt = BlocketteFactory.getFieldRepeat(blkType,curField);
				if (fldRpt > 0) {  // this is a repeating field, fldRpt indicates the count field number
					if (fieldValue.get(curField) == null) {
						// needs to be initialized with Vector
						fieldValue.set(curField, new Vector(1,1));
					}
					if (indexCount == 0) {  // starting a new repeat group?
						indexCount = Integer.parseInt(fieldValue.get(fldRpt).toString());  // get the count value - initialize repeat index
						indexCountStart = indexCount;  // remember the high count
						rptGrpStart = curField;  // remember where the repeat group starts
						// special case -- blockettes like 48 and 58 can have a value of zero for the number of repeats
						// which means that we have to skip parsing over the remainder of the repeat group.  If
						// indexCountStart is zero, then we simply continue the loop.
						if (indexCountStart == 0) {
							// DEBUG
							//if (blkType == 70) {
//								System.out.println("DEBUG: encountered number of repeats == 0, arrIndex == " + arrIndex);
							//   }
							continue;
						}
					}
				} else {
					if (indexCount > 0) {  // checking for repeat group boundary
						indexCount--;
						// a non-zero value of indexCount after decrement here means there is another
						// repeat group to do, otherwise, an indexCount of 0 flags the end of repeat
						// group processing
						if (indexCount > 0) {
							curField = rptGrpStart;  // rewind to beginning of repeat group
						} else {
							indexCountStart = 0;     // set total repeat count to zero - rest state
						}
					}
				}
				if (arrIndex == blocketteStream.length) {
					// special case can occur where a blockette ends exactly on
					// the end of a record, but trailing
					// some null fields.  In this case arrIndex==blocketteStream.length,
					// so we want the while loop to
					// continue until all fields are accounted for.
					// In all other cases, this equality would lead to
					// an array out of bounds exception, so we will
					// avoid that by breaking here.  Also, we wouldn't get
					// here unless we were looking for more fields,
					// only to find none, so trigger the incompleteFlag.
					//if (blkType == 70) {
					//	System.err.println("DEBUG: arrIndex == blocketteStream.length: " + arrIndex);
					//   }
					incompleteFlag = true;
					break;
				}
				// now that we are sure of the field we are addressing with the upcoming binary data
				// we will parse it from the byte field
				//
				// get the blockette field data type
				fldType = BlocketteFactory.getFieldType(blkType,curField);
				// get the blockette field byte length
				String fldLen = BlocketteFactory.getFieldLength(blkType,curField);
				// based on the data type, determine the byte length of the extraction
				if (fldType.equals("L")) { // special data type (List), which is a repeating list of decimal values
					// meant for Blockette 60
					// create a Vector-formatted string
					Vector listVec = new Vector();
					int rptFld = BlocketteFactory.getFieldRepeat(blkType,curField);
					Vector fieldVec = (Vector) fieldValue.get(rptFld);  // vector of number of repeat values
					if (fieldVec == null) {
						// needs to be initialized with Vector
						fieldValue.set(rptFld, new Vector(1,1));
						fieldVec = (Vector) fieldValue.get(rptFld);
					}
					int numValues = Integer.parseInt(fieldVec.get(indexCountStart - indexCount).toString());  // how many values in list
					for (int i = 0; i < numValues; i++) {  // get each list value from binary and add to list vector
						int intLen = Integer.parseInt(fldLen);
						// check to see if this field is truncated
						if (intLen > blocketteStream.length - arrIndex) { // incomplete blockette
							incompleteFlag = true;
							break;
						}
						String sampStr = new String(blocketteStream,arrIndex,intLen);
						Integer intVal = new Integer(sampStr.trim());
						listVec.add(intVal);
						arrIndex += intLen;
					}
					blkStr = listVec.toString();
				} else if (fldType.equals("V")) { // variable alpha string
					StringTokenizer tok = new StringTokenizer(fldLen,"-"); // length is two numbers
					String sLen = tok.nextToken();   // ignore the first number
					sLen = tok.nextToken();   // look at the second number
					int intLen = Integer.parseInt(sLen);  // get integer equivalent of field length string
					if (intLen >= blocketteStream.length - arrIndex) {
						intLen = blocketteStream.length - arrIndex - 1;  // adjust byte sample length to prevent array overrun
					}
					String sampStr = new String(blocketteStream,arrIndex,intLen+1);  // get a supersample - add 1 for tilde
					int endIndex = sampStr.indexOf("~");   // locate the tilde end marker
					if (endIndex == -1) {
						// tilde not found, which means either truncated blockette, continued on next record,
						// or improper formatting...we will treat this as an incomplete blockette unless we
						// have read to the max size of the field, in that case, find the tilde past
						// the established field size
						//
						//System.err.println("DEBUG: V field tilde not found...attempting fix...");
						if (intLen < Integer.parseInt(sLen)) { // less than max field size remains in record?
                              //System.err.println("DEBUG: I think this is incomplete...setting flag");
							incompleteFlag = true;
							break;  // break from while loop
						} else {
							// the tilde must be here, just not within field bounds, try finding
							// the index of the tilde outside of sampStr bounds
                              //System.err.println("DEBUG: look outside of sampStr bounds for tilde...");
//							Object lenObj = fieldValue.get(2);  // try to get blockette size
//							if (lenObj == null) {
//								// can't get it, mark incomplete and break
//								incompleteFlag = true;
//								break;
//							}
//							int blkLen = Integer.parseInt(lenObj.toString());  // get blockette length
                              // TODO -- fixing this implementation
                            int blkLen = suggestBytes;
                            int remainder = blocketteStream.length - arrIndex;  // remainder of blockette stream (record)
                            //System.err.println("DEBUG: a) blkLen: " + blkLen + ", remainder: " + remainder + ", arrIndex: " + arrIndex);
							if (blkLen < remainder && blkLen > arrIndex) {
								remainder = blkLen - arrIndex;  // adjust remainder of record to be relative to blockette length
							}
                              //System.err.println("DEBUG: b) blkLen: " + blkLen + ", remainder: " + remainder);
							sampStr = new String(blocketteStream,arrIndex,remainder);
                              //System.err.println("DEBUG: sampStr: " + sampStr);
							endIndex = sampStr.indexOf("~",arrIndex);
							if (endIndex == -1) {
								// still no tilde?
                                  //
                                  // mark this incomplete if we ran into a record boundary trying to accomodate
                                  // the blockette length
								if (remainder + arrIndex < blkLen) {  // not enough record for the blockette length
                                      //System.err.println("DEBUG: not enough bytes, marking incomplete");
									incompleteFlag = true;
									break;
								} else {  // we are at the end of the blockette length
									// else, we make the endIndex go to the specified blockette length
									// (minus 1 to simulate a count from 0 index value)
                                      //System.err.println("DEBUG: setting endIndex to remainder minus one");
									endIndex = remainder - 1;
								}
							}
						}
					}
					blkStr = sampStr.substring(0,endIndex); // get the text up to the marker
					arrIndex += endIndex+1;  // push byte index just past the tilde
				} else if (fldType.equals("B")) {  // binary data
					String fldMask = BlocketteFactory.getFieldMask(blkType,curField);
					int intLen = Integer.parseInt(fldLen);
					// check to see if this field is truncated
					if (intLen > blocketteStream.length - arrIndex) {  // incomplete blockette
						incompleteFlag = true;
						break;
					}
					byte[] sampArr = new byte[intLen];
					System.arraycopy(blocketteStream,arrIndex,sampArr,0,intLen);
					if (fldMask.equals("BTIME")) {
						Btime bTime = new Btime(sampArr,swapFlag);
						blkStr = bTime.getStringTime();
						//System.err.println("DEBUG: blkStr = " + blkStr);
					} else if (fldMask.equals("FLOAT")) {
						// get properly swapped bits through integer conversion to float
						bInt = Utility.bytesToInt(sampArr[0],sampArr[1],sampArr[2],sampArr[3],swapFlag);
						Float bFloat = new Float(Float.intBitsToFloat(bInt));
						blkStr = bFloat.toString();
						//System.err.println("DEBUG: FLOAT value is: " + blkStr);
					} else if (fldMask.equals("UBYTE") || fldMask.equals("BYTE")) {
						// must take into account 'arrays' like BYTE*6
						StringBuffer sbuf = new StringBuffer();
						for (int i = 0; i < sampArr.length; i++) {
							if (i > 0) sbuf.append(" ");  // space separated values
							if (fldMask.equals("UBYTE")) {
								bInt = Utility.uBytesToInt(sampArr[i]);
							} else {
								bInt = Utility.bytesToInt(sampArr[i]);
							}
							sbuf.append(Integer.toString(bInt));
						}
						blkStr = sbuf.toString();
					} else if (fldMask.equals("WORD") || fldMask.equals("UWORD")) {
						if (fldMask.equals("UWORD")) {
							bInt = Utility.uBytesToInt(sampArr[0],sampArr[1],swapFlag);
						} else {
							bInt = Utility.bytesToInt(sampArr[0],sampArr[1],swapFlag);
						}
						blkStr = Integer.toString(bInt);
					} else if (fldMask.equals("LONG") || fldMask.equals("ULONG")) {
						if (fldMask.equals("ULONG")) {
							// we use a Long here instead
							long bLong = Utility.uBytesToLong(sampArr[0],sampArr[1],sampArr[2],sampArr[3],swapFlag);
							blkStr = Long.toString(bLong);
						} else {
							bInt = Utility.bytesToInt(sampArr[0],sampArr[1],sampArr[2],sampArr[3],swapFlag);
							blkStr = Integer.toString(bInt);
						}
					} else {
						throw new SeedInputException("unrecognized field mask: " + fldMask);  // binary fields MUST have a mask
					}
					arrIndex += intLen;
				} else {  // for type A, D, and F...
					// extract the byte section into a string
					int intLen = Integer.parseInt(fldLen);
					// DEBUG
					//if (blkType == 70) {
					//	System.err.println("DEBUG: fldLen=" + fldLen + ", intLen=" + intLen);
					//   }
					//
					// check to see if this field is truncated
					if (intLen > blocketteStream.length - arrIndex) { // incomplete blockette
						//System.err.println("DEBUG: triggering A,D,F incomplete flag due to field length overrun");
						incompleteFlag = true;
						break;
					}
					blkStr = new String(blocketteStream,arrIndex,intLen);
					//
					// if this is field 2 and not a data blockette, then get the
					// length value as a hint of the size of data we are reading in.
					// This may help in certain cases of unexpectedly short blockette
					// entries (discovered this with a strange Blockette 12 having
					// zero timestamp entries).
					if (curField == 2 && ! isData && blocketteStream.length >= 7) {
						// DEBUG
						//if (blkType == 70) {
						//	System.err.println("DEBUG: before suggestBytes: blkStr=" + blkStr.trim());
						//}
						// use trim() because parseInt doesn't like leading spaces
						suggestBytes = Integer.parseInt(blkStr.trim());
						// DEBUG
						//if (blkType == 70) {
						//	System.err.println("DEBUG: suggestBytes=" + suggestBytes);
						//}
					}
					// increment array index
					arrIndex += intLen;
				}
			}
			// write the string extracted from the byte section to the proper field reference.
			//
			// DEBUG
			//if (blkType == 70) {
			//	System.err.println("DEBUG: setFieldString: blkType=" + blkType + ", curField=" +
			//		curField + ", blkStr=" + blkStr);
			//   }
			//
			setFieldString(blkType,curField,indexCountStart-indexCount,blkStr);
		}  //...next field
		// check to see if we have a complete blockette input -- were we missing
		// anything?  Check to see that we processed all expected fields, but also
		// temper this against the number of bytes that was specified by the
		// blockette entry.
		if ( (curField < fldCount || indexCount > 1) && (arrIndex < suggestBytes) ) {
			//System.err.println("DEBUG: curField < fldCount");
			incompleteFlag = true;   // mark blockette as being incomplete
		}
		setType();
		update();
		//System.err.println("DEBUG doublecheck blockette type: " + getType() + ", isIncomplete: " + incompleteFlag);
		return arrIndex;
	}
	
	/**
	 * Set a specific blockette field to a value specified as a String.
	 * Also ensures the the input value is properly formatted for the field.
	 * This is kept private because it does not check that the blockette is initialized.
	 * It is in fact one method used to initialize the blockette.
	 * The public variant of this method, that demands that the blockette is
	 * first initialized through the constructor, is String variant of setFieldVal().
	 * For the unique case of Blockette 60, field 6, the most straightforward solution
	 * is to require that <b>addVal</b> represent the entire set of response keys
	 * for a given stage as a single string in Vector.toString() format, which is
	 * "[value1, value2, value3, ...]".
	 * A string value of zero length results in a null entry.  If
	 * <b>fieldIndex</b> is 0, the result is a zero-size repeat vector.
	 * If <b>lenient</b> is set to true, then don't throw an exception in certain
	 * input value formatting cases, but perform a workaround to continue the
	 * processing.
	 */
	protected void setFieldString(int blkType, int fieldNum, int fieldIndex,
			String addVal, boolean lenient) throws SeedException {
		// we will get the field data type for future reference and to verify
		// that the blockette type listed in blkType is a known blockette type
		String fldType = BlocketteFactory.getFieldType(blkType,fieldNum);
		// also get the mask/flags for this field
		String fldMask = BlocketteFactory.getFieldMask(blkType,fieldNum);
		// get the length of data for this field
		String fldLen = BlocketteFactory.getFieldLength(blkType,fieldNum);
		// see if this field repeats
		int fldRpt = BlocketteFactory.getFieldRepeat(blkType,fieldNum);
		//
		// DEBUG
		//if (blkType == 60) {
			//new Throwable().printStackTrace();
			//System.err.println("DEBUG: setFieldString() for blkType=" + blkType +
					//", fieldNum=" + fieldNum + ", fieldIndex=" + fieldIndex + ", addVal=" + addVal);
		//}
		//
		boolean isRepeating = (fldRpt > 0);
		//
		// check to see if this is an empty string
		// if so, add a null entry or zero-size repeat vector
		boolean isBlank = (addVal.length() == 0);
		if (isBlank) {
			if (isRepeating && fieldIndex == 0) {
				fieldValue.set(fieldNum,new Vector(1,1));  // reinitialize with zero-size vector
			} else {
				fieldValue.set(fieldNum,null);
			}
			return;
		}
		// first, lets fit the data to the specified mask
		String addTrim = addVal.trim();              // trim space off of front and back...
		if (addTrim.length() > 0) addVal = addTrim;  // but only if it doesn't result in a blank field
		if (fldMask.charAt(0) == '"') {         // number format string
			try {
				// if addVal is spaces, then the entry should represent a numerical zero
				// test addTrim for this
				if (addTrim.length() == 0) addVal = new String("0");
				if (! lenient) BlocketteFactory.formatDecimal(blkType,fieldNum,addVal); // non-lenient - doublecheck formatting
			} catch (SeedException e) {  // SeedException is thrown by formatDecimal()
				throw new SeedFormatException(
						//"Encountered NumberFormatException for blockette " + 
						//blkType + ", field " + fieldNum + ", value '" + addVal + "'"
						"Value '" + addVal + "' is an improper entry for this decimal field (blockette " +
						blkType + ", field " + fieldNum + ")"
						);
			}
		} else if (fldMask.charAt(0) == '[') {  // character flags
			char c;
			boolean testUpper = false;
			boolean testLower = false;
			boolean testDigit = false;
			boolean testPunct = false;
			boolean testSpace = false;
			boolean testUnder = false;
			boolean allowAll = true;
			for (int i = 1; i < fldMask.length() && (c = fldMask.charAt(i)) != ']'; i++) {
				allowAll = false;
				switch (c) {
				case 'U':
					testUpper = true;
					break;
				case 'L':
					testLower = true;
					break;
				case 'N':
					testDigit = true;
					break;
				case 'P':
					testPunct = true;
					break;
				case 'S':
					testSpace = true;
					break;
				case '_':
					testUnder = true;
					break;
				}
			}
			for (int i = 0; !allowAll && i < addVal.length(); i++) {
				char a = addVal.charAt(i);
				if (testUpper && Character.isUpperCase(a)) continue;
				if (testLower && Character.isLowerCase(a)) continue;
				if (testDigit && Character.isDigit(a)) continue;
				if (testPunct && isPunctuation(a)) continue;
				if (testSpace && a == 32) continue;
				if (testUnder && a == 95) continue;
				StringBuffer exString = new StringBuffer();
				Byte charByte = new Byte((byte) a);
				exString.append("character '" + a + "' (value=" + charByte.intValue() +
						") is not acceptable for Blockette " + 
						blkType + " field " + fieldNum + "\nExpected one of:\n");
				char substitute = ' ';  // substitution string when encountering violation
				// proceed with checks from least desirable substitution to most
				// desirable...
				if (testUpper) {
					exString.append("\tUpperCase\n");
					substitute = 'Z';
				}
				if (testLower) {
					exString.append("\tLowerCase\n");
					substitute = 'z';
				}
				if (testDigit) {
					exString.append("\tDigit\n");
					substitute = '0';
				}
				if (testPunct) {
					exString.append("\tPunctuation\n");
					substitute = '.';
				}
				if (testUnder) {
					exString.append("\tUnderscore\n");
					substitute = '_';
				}
				if (testSpace) exString.append("\tSpace\n");
				// if lenient flag is true...
				if (lenient) {
					// display a warning message to the user and do a substitution
					System.err.println("WARNING: " + exString.toString());
					System.err.println("CORRECTION: substituting with character '" + substitute + "'");
                     //System.err.println("DEBUG: blk=" + toString());
					String newVal = addVal.replace(a,substitute); // replace all instances of faulty char a with substitute
					addVal = newVal;
				} else {
					// if non-lenient, thrown an exception
					throw new SeedInputException(exString.toString());
				}
				continue;  // resume processing
			}
		}  // other mask patterns are ignored
		
		// now fit the data to the data type expression and write to blockette vector.
		// Repeating fields must be initialized with Vectors, others can be null initialized.
		int fvIndex = 0;
		Vector fvVec = null;
		if (isRepeating) {  // adding to repeating field vector?  get a generic object handle and index
			fvVec = (Vector) fieldValue.get(fieldNum);  // get vector
			if (fvVec == null) {
				// needs to be initialized with Vector
				fieldValue.set(fieldNum, new Vector(1,1));
				fvVec = (Vector) fieldValue.get(fieldNum);
			}
			if (fvVec.size() < (fieldIndex+1)) fvVec.setSize(fieldIndex+1); // make sure large enough
			fvIndex = fieldIndex;
		} else {
			fvVec = fieldValue;  // this should already be properly sized
			fvIndex = fieldNum;
		}
		try {  // trap exceptions so that we can report blockette type and field number
			if (fldType.equals("L")) {  // special 'List' case for Blockette 60
				//int intLen = Integer.parseInt(fldLen);
				if (addVal.charAt(0) != '[') {
					//new Throwable().printStackTrace();
					//throw new SeedInputException("Incorrect format for List field type: (" + addVal +
							//") ...need to enclose list in square brackets");
					//
					// to counter a phenomenon of a single value in the list not having brackets, just
					// tack the brackets onto both sides of the value string and push it through
					addVal = "[" + addVal + "]";
				}
				StringTokenizer listTok = new StringTokenizer(addVal,"[], ");
				Vector listVec = new Vector();
				while (listTok.hasMoreTokens()) {
					try {
						listVec.add(new Integer(listTok.nextToken()));  // store each element as an Integer
					} catch (NumberFormatException e) {
                                                if (lenient) {
                                                    System.err.println("ERROR with Integer list value for fieldNum=" +
                                                                    fieldNum + " and fieldIndex=" + fieldIndex +
                                                                    ": forcing value of zero");
                                                    System.err.println("blktype=" + getType() + ", value=" + addVal);
                                                    listVec.add(new Integer(0));
                                                } else {
                                                    // if non-lenient, thrown an exception
                                                    throw new SeedInputException("ERROR with Integer list value for fieldNum=" +
                                                                    fieldNum + " and fieldIndex=" + fieldIndex +
                                                                    ": blktype=" + getType() + ", value=" + addVal);
                                                }
					}
				}
				// store a Vector inside of fvVec at fvIndex, which is unique to L-type
				fvVec.set(fvIndex,listVec);
			} else if (fldType.equals("A")) {
				// fixed length alpha - store as a String
				int intLen = Integer.parseInt(fldLen);
				if (intLen < addVal.length()) {
                                        if (lenient) {
                                            String trimVal = addVal.substring(0,intLen);
                                            addVal = trimVal;
                                        } else {
                                            // if non-lenient, thrown an exception
                                            throw new SeedInputException("ERROR - Alpha value too long for fieldNum=" +
                                                fieldNum + " and fieldIndex=" + fieldIndex +
                                                ": blktype=" + getType() + ", value=" + addVal + ", max length = " + intLen);
                                        }
				}
				fvVec.set(fvIndex,addVal);
			} else if (fldType.equals("D")) {
				// decimal integer or fixed floating point
				if (fldMask.indexOf('.') > 0) { // check for floating point
					try {
						fvVec.set(fvIndex, new Double(addVal));
					} catch (NumberFormatException e) {
                                                if (lenient) {
                                                    System.err.println("ERROR with Double field value for fieldNum=" +
                                                                    fieldNum + " and fieldIndex=" + fieldIndex +
                                                    ": forcing value of zero");
                                                    System.err.println("blktype=" + getType() + ", value=" + addVal);
                                                } else {
                                                    // if non-lenient, thrown an exception
                                                    throw new SeedInputException("ERROR with Double field value for fieldNum=" +
                                                                    fieldNum + " and fieldIndex=" + fieldIndex +
                                                                    ": blktype=" + getType() + ", value=" + addVal);
                                                }
						fvVec.set(fvIndex, new Double(0.0F));
					}
				} else {
					try {
						fvVec.set(fvIndex, new Integer(addVal));
					} catch (NumberFormatException e) {
                                                if (lenient) {
                                                    System.err.println("ERROR with Integer field value for fieldNum=" +
                                                                    fieldNum + " and fieldIndex=" + fieldIndex +
                                                    ": forcing value of zero");
                                                    System.err.println("blktype=" + getType() + ", value=" + addVal);
                                                } else {
                                                    // if non-lenient, thrown an exception
                                                    throw new SeedInputException("ERROR with Integer field value for fieldNum=" +
                                                                    fieldNum + " and fieldIndex=" + fieldIndex +
                                                                    ": blktype=" + getType() + ", value=" + addVal);
                                                }
						fvVec.set(fvIndex, new Integer(0));
					}
				}
			} else if (fldType.equals("F")) {
				// floating point with exponent
				try {
					fvVec.set(fvIndex, new Double(addVal));
				} catch (NumberFormatException e) {
                                        if (lenient) {
                                            System.err.println("ERROR with Double field value for fieldNum=" +
                                                            fieldNum + " and fieldIndex=" + fieldIndex +
                                            ": forcing value of zero");
                                            System.err.println("blktype=" + getType() + ", value=" + addVal);
                                        } else {
                                            // if non-lenient, thrown an exception
                                            throw new SeedInputException("ERROR with Double field value for fieldNum=" +
                                                            fieldNum + " and fieldIndex=" + fieldIndex +
                                                            ": blktype=" + getType() + ", value=" + addVal);
                                        }
					fvVec.set(fvIndex, new Double(0.0F));
				}
			} else if (fldType.equals("V")) {
				// variable length alpha
				StringTokenizer tok = new StringTokenizer(fldLen,"-"); // length is two numbers
				String sLen = tok.nextToken();   // ignore the first number
				sLen = tok.nextToken();   // look at the second number
				int intLen = Integer.parseInt(sLen);
				if (intLen < addVal.length()) {
                                    if (lenient) {
                                        String trimVal = addVal.substring(0,intLen);  // trim input string to maximum length
                                        addVal = trimVal;
                                    } else {
                                        // if non-lenient, thrown an exception
                                        throw new SeedInputException("ERROR - Alpha value too long for fieldNum=" +
                                            fieldNum + " and fieldIndex=" + fieldIndex +
                                            ": blktype=" + getType() + ", value=" + addVal + ", max length = " + intLen);
                                    }
				}
				if (fldMask.equals("TIME")) {   // storing a time value
				    // TODO -- we might get a number format exception here with a bad addVal
				    try {
				        Btime bTime = new Btime(addVal);
				        fvVec.set(fvIndex,bTime);
				    } catch (NumberFormatException e) {
                                        if (lenient) {
                                            System.err.println("ERROR with Time field value for fieldNum=" +
                                                    fieldNum + " and fieldIndex=" + fieldIndex +
                                            ": forcing default time value");
                                            System.err.println("blktype=" + getType() + ", value=" + addVal);
                                            fvVec.set(fvIndex, new Btime());
                                        } else {
                                            // if non-lenient, thrown an exception
                                            throw new SeedInputException("ERROR with Time field value for fieldNum=" +
                                                            fieldNum + " and fieldIndex=" + fieldIndex +
                                                            ": blktype=" + getType() + ", value=" + addVal);
                                        }
				    }
				} else {     // storing a string value
				    fvVec.set(fvIndex,addVal);
				}
			} else if (fldType.equals("B")) {
			    // fixed byte binary -- unique to data blockettes
			    // a time (BTIME) representation would be in the form of "YYYY,DDD,HH:MM:SS.FFFF".
			    // FLOATs, WORDs, and BYTEs must be entered as a decimal representation ("23").
			    // An 'array' of UBYTEs would be a space-separated decimal representation 
			    // (i.e. UBYTE*6 would be "0 127 34 0 255 1").
			    // (CHARs are fldType A, so they would be written as Strings in the code above here.)
			    //
				if (fldMask.equals("BTIME")) {
					Btime bTime = new Btime(addVal);
					fvVec.set(fvIndex,bTime);    // store as Btime
				} else if (fldMask.equals("UBYTE")) {    // unsigned byte
					int intLen = Integer.parseInt(fldLen);  // count down the number of bytes
					StringTokenizer tok = new StringTokenizer(addVal," ");
					while (intLen > 0 && tok.hasMoreTokens()) {  // check values of 1 to many UBYTEs
						String tokVal = tok.nextToken();
						int byteVal = Integer.parseInt(tokVal);
						if (byteVal < 0 || byteVal > 255) {
							throw new SeedInputException("input value " + tokVal + " exceeds UBYTE value boundary");
						}
						intLen--;
					}
					if (intLen > 0) {
						throw new SeedInputException("not enough UBYTEs in String entry (need " + fldLen + ")");
					}
					fvVec.set(fvIndex,addVal);  // write UBYTEs as a String of decimal value(s)
				} else if (fldMask.equals("BYTE")) {     // signed byte
					int intLen = Integer.parseInt(fldLen);
					StringTokenizer tok = new StringTokenizer(addVal," ");
					while (intLen > 0 && tok.hasMoreTokens()) {  // check values of 1 to many BYTEs
						String tokVal = tok.nextToken();
						int byteVal = Integer.parseInt(tokVal);
						if (byteVal < -128 || byteVal > 127) {
							throw new SeedInputException("input value " + tokVal + " exceeds BYTE value boundary");
						}
						intLen--;
					}
					if (intLen > 0) {
						throw new SeedInputException("not enough BYTEs in String entry (need " + fldLen + ")");
					}
					fvVec.set(fvIndex,addVal);  // write BYTEs as a String of decimal value(s)
				} else if (fldMask.equals("UWORD")) {    // unsigned word, equivalent to a 16-bit short
					int wordVal = Integer.parseInt(addVal);
					if (wordVal < 0 || wordVal > 65535) {
						throw new SeedInputException("input value " + addVal + " exceeds UWORD value boundary");
					}
					fvVec.set(fvIndex, new Integer(wordVal));
				} else if (fldMask.equals("WORD")) {     // signed word
					int wordVal = Integer.parseInt(addVal);
					if (wordVal < -32768 || wordVal > 32767) {
						throw new SeedInputException("input value " + addVal + " exceeds WORD value boundary");
					}
					fvVec.set(fvIndex, new Integer(wordVal));
				} else if (fldMask.equals("ULONG") || fldMask.equals("LONG")) {   // signed or unsigned long, equiv to 32-bit int
					fvVec.set(fvIndex, new Long(addVal));  // store to a Long class to accomodate a large unsigned value
				} else if (fldMask.equals("FLOAT")) {    // float value
					fvVec.set(fvIndex, new Float(addVal));
				}
			} else {
				throw new SeedInputException ("unknown blockette " + blkType + " field type: " + fldType);
			}
		} catch (Exception e) {
			throw new SeedException("Exception encountered for blockette type " +
					blkType + " and field number " + fieldNum +
					"and field index " + fieldIndex + ": " + e);
		}
	}
	
	/**
	 * Set a specific blockette field to a value specified as a String.
	 * Abbreviated version the defaults to lenient processing of the provided
	 * value.
	 */
	protected void setFieldString(int blkType, int fieldNum, int fieldIndex,
			String addVal) throws SeedException {
		setFieldString(blkType,fieldNum,fieldIndex,addVal,true);
	}
	
	/**
	 * Initialize the blockette with a character-delimited string.
	 * Delimiter should be a single character distinguishable from the data.
	 * <b>blank</b> is a character distinct from delimiter that marks a zero-size
	 * (blank) variable length field entry.
	 * Implementer should be careful that there are enough columns
	 * in the string to fill the blockette sufficiently.
	 * Repeat fields are entered in multiplexed order:
	 * i.e.  field7(0),field8(0),field9(0),field7(1),field8(1),field9(1),field7(2),...
	 * when there are zero repeat fields, the incoming string will still list each
	 * field, but only once and will each contain a 'blank' character.
	 * Blockette 60 is a special case where the repeat fields are two-dimensional, so
	 * you will need to enter each field 6 as a special string in Vector.toString() format, 
	 * which is "[value1, value2, value3, ...]".  This represents a case where it would be
	 * good for the delimiter (or blank) to not be a comma or square bracket.
	 */	
	protected void setTokenString(String inputString, String delimiter, String blank) throws SeedException {
		StringTokenizer tn = new StringTokenizer(inputString,delimiter);
		int curField = 0;   // track our current field to write to
		int fldCount = 0;   // this will be the expected field count for this blockette
		int indexCount = 0; // this will be the expected count of repeating fields
		int indexCountStart = 0;  // keeps track of where the count started
		int rptGrpStart = 0;  // marker for which field is the start of the repeat group
		int blkType = 0;   // this is the blockette type
		while (tn.hasMoreTokens()) {
			String addVal = tn.nextToken();  // get next string value
			curField++;  // increment the current blockette field
			if (curField == 1) {  // first field will tell us the blockette type
				blkType = Integer.parseInt(addVal);	// get the blockette type
				float blkVer = BlocketteFactory.getVersion(blkType);
				if (blkVer > version) {   // is out version designation too old for this declared blockette?
					throw new SeedInputException("blockette type " + blkType + " is a version " + blkVer + " blockette");
				}
				fldCount = BlocketteFactory.getNumFields(blkType,version);  // get the number of fields
				fieldValue.setSize(fldCount+1);  // set blockette Vector to be properly sized
			} else if (curField > fldCount) {  // else have we exceeded the blockette field count?
				if (indexCount < 2) {   // 0 or 1, we have too many string tokens
					throw new SeedInputException("input string has too many fields for blockette " + blkType);
				} else {  // else we still have some repeat fields to enter
					indexCount--;
					curField = rptGrpStart;  // rewind to beginning of repeat group
				}
			}
			// check to see if this is a blank field.
			// if so, translate to a zero-length string.
			if (addVal.equals(blank)) addVal = new String("");
			// see if this field is repeating, and make sure a Vector is present there
			int fldRpt = BlocketteFactory.getFieldRepeat(blkType,curField);
			if (fldRpt > 0) {   // this field repeats
				if (fieldValue.get(curField) == null) {
					fieldValue.set(curField, new Vector(1,1));  // needs to be initialized with Vector
				}
				if (indexCount == 0) {  // starting a new repeat group?
					indexCount = Integer.parseInt(fieldValue.get(fldRpt).toString());  // initialize repeat index
					indexCountStart = indexCount;  // remember the high count
					rptGrpStart = curField;  // remember where the repeat group starts
				}
			} else {
				if (indexCount > 0) {  // have we reached a repeat group boundary?
					indexCount--;
					// a non-zero value of indexCount after decrement here means there is another
					// repeat group to do, otherwise, an indexCount of 0 flags the end of repeat
					// group processing
					if (indexCount > 0) {
						curField = rptGrpStart;  // rewind to beginning of repeat group
					} else {
						indexCountStart = 0;     // reset this value to rest-state
					}
				}
			}
			setFieldString(blkType,curField,indexCountStart-indexCount,addVal);  // write the value
		}
		if (curField < fldCount || indexCount > 1) {   // check to see that we had enough string fields
			incompleteFlag = true;   // mark this blockette as being incomplete
		}
		setType();
		update();
	}
	
	/**
	 * Indicate TRUE if this is character is some form of punctuation.
	 */
	protected boolean isPunctuation (char c) {
		if (c > 32 && c < 48 || c > 57 && c < 65 || c > 90 && c < 97 || c > 122 && c < 127) {
			return true;
		}
		return false;
	}
	
	/**
	 * update the state of the blockette after changes
	 */
	private void update() throws SeedException {
		// update the state of the blockette after changes
		//blocketteType = Integer.parseInt(fieldValue.get(1).toString());
		//resetMTime();  // REC-- turned this off, Mar 22 2006
	}
	
	/**
	 * update/set the blockette type
	 * @throws SeedException
	 */
	protected void setType() throws SeedException {
	    // set the blockette type
	    blocketteType = Integer.parseInt(fieldValue.get(1).toString());
	}

	
	/**
	 * allow explicit setting of the type -- use with caution!
	 * 
	 * @param i
	 */
	protected void setType(int i) {
	    // set the blockette type explicitly
	    blocketteType = i;
	}
	
	
	
	/**
	 * Initialize instance variables.
	 */
	protected void instanceInit() {
		fieldValue = new Vector(8,8);
		childBlocketteVec = new BlocketteVector(1,1);
		parentBlockette = null;
		waveformData = null;
		lookupMap = new Vector(1,1);
		lookupId = 0;
		blocketteType = 0;
		version = BlocketteFactory.getDefaultVersion();
		numBytes = 0;
		incompleteFlag = false;
	}
	
	// instance variables
	
	// this is where all of the blockette values are stored.
	// index 1 is for blockette field 1.
	// index 0 is reserved for a Btime object showing instantiated object's
	// modification date (mtime).
	// each value in this vector should be an Object.
	// a field that can have repeating values will contain a Vector
	// Object which will consist of N number of Objects, as specified
	// by the blockette field indicating the number of repeats.
	// This repeating field Vector will be referenced by a fieldIndex
	// value which starts at count of 0 for the first entry.
	protected Vector fieldValue;
	
	// this vector will contain references to child blockettes that are positionally
	// encapsulated by this blockette in a SEED file (i.e. blk 52 encapsulated by blk 50)
	protected BlocketteVector childBlocketteVec;
	
	// this points to the designated Parent blockette...if set to null, then it is
	// implied, at least from this object's perspective, that there is no parent.
	protected Blockette parentBlockette;
	
	// this is a handle for attaching a waveform data object...pretty much just
	// used by the FSDH Blockette (999)
	protected Waveform waveformData;
	
	// count starts from 1 here, index 0 is reserved.
	// the index number for this vector is stored in the 
	// blockette's lookup field when referencing a dictionary
	// blockette.
	// the value at the index in this Vector reveals a lookup
	// number in the form of an Integer that points to the lookupId
	// value of another Blockette Object...this Blockette which 
	// should be stored in some form of Container that allows such
	// blockettes to be searched and found.  This form of redirection
	// helps to overcome the problems of dictionary lookup numbers
	// which can repeat as multiple SEED volumes are read into the
	// Blockette pool and ensures unique lookups in that pool.
	protected Vector lookupMap;
	
	// space for a unique lookup ID number for this blockette
	protected int lookupId;
	
	// this must be set when the blockette has been initialized
	protected int blocketteType;
	
	// current SEED version of this blockette
	protected float version;
	// minimum SEED version allowed
	protected static final float minSEEDVersion = 2.0F;
	// maximum SEED version allowed
	protected static final float maxSEEDVersion = 2.4F;
	// default SEED version assumed
	protected static final float defaultSEEDVersion = BlocketteFactory.getDefaultVersion();
	
	// this indicates the number of bytes read for input to this blockette
	// it does not indicate the number of bytes that the blockette would output, necessarily.
	// this variable remains at zero if blockette created through String input.
	// very useful if the calling class needs to know how many bytes were read in a SEED record
	// to produce this blockette so that it can adjust its read offset for the next blockette.
	// use getNumBytes to retrieve this value publicly.
	protected int numBytes;
	
	// flag true if this blockette was not initialized fully...this could happen with a blockette
	// split into a continuation record.
	protected boolean incompleteFlag;
    
    // set serialization version tag
    static final long serialVersionUID = 42L;
	
}
