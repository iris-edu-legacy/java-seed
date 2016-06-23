package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;
import edu.iris.Fissures.seed.util.*;
import java.util.*;
import java.text.*;

/**
 * Factory class for all Blockette objects.  Not only provides the creational method
 * for constructing Blockettes but also acts as the reference engine for all particulars
 * about the different blockette types.  This is by design a static class.
 * @author Robert Casey, IRIS DMC
 * @version 8/12/2010
 */
public class BlocketteFactory {
	
	// creation methods
	
	/**
	 * Instructs the factory to build a Blockette object of the appropriate type
	 * based on SEED binary input.
	 */
	public static Blockette 
	createBlockette(byte[] blocketteStream, boolean swapFlag, boolean isData, float version) 
	throws SeedException {
		return new Blockette(blocketteStream,swapFlag,isData,version);
	}
	
	/**
	 * Instructs the factory to build a Blockette object of the appropriate type
	 * based on SEED binary input.
	 */
	public static Blockette 
	createBlockette(byte[] blocketteStream, boolean swapFlag, boolean isData) 
	throws SeedException {
		return new Blockette(blocketteStream,swapFlag,isData);
	}
	
	/**
	 * Instructs the factory to build a Blockette object of the appropriate type
	 * based on tokenized String input.
	 */
	public static Blockette 
	createBlockette(String inputString, String delimiter, String blank, float version) 
	throws SeedException {
		return new Blockette(inputString,delimiter,blank,version);
	}
	
	/**
	 * Instructs the factory to build a Blockette object of the appropriate type
	 * based on tokenized String input.
	 */
	public static Blockette 
	createBlockette(String inputString, String delimiter, String blank) 
	throws SeedException {
		return new Blockette(inputString,delimiter,blank);
	}
	
	/**
	 * Instructs the factory to build a Blockette object of the appropriate type
	 * based on tokenized String input.
	 */
	public static Blockette 
	createBlockette(String inputString) throws SeedException {
		return new Blockette(inputString);
	}
	
	/**
	 * Instructs the factory to build a Blockette object of the appropriate type
	 * based on tokenized String input.
	 */
	public static Blockette 
	createBlockette(String inputString, float version) throws SeedException {
		return new Blockette(inputString,version);
	}
	
	/**
	 * Instructs the factory to build a BLANK Blockette object of the indicated type
	 * number using the default version.
	 */
	public static Blockette
	createBlockette(int blkType) throws SeedException {
		int numFields = getNumFields(blkType,Blockette.getDefaultVersion());
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
		return new Blockette(inputStringBuf.toString(),delimiter,blank);
	}
	
	
	
	// reference methods
	
	/**
	 * Return the full name of the blockette type
	 */
	public static String getName(int blkType) throws SeedException {
        // return from the first line of the definition (0'th field), 2nd column
	    try {
	        if (optimized) {
	            return arrayBlkName[ blocketteTypeMap[blkType] ];
	        } else
                return getField(blkType,0,2);
	    } catch (Exception e) {
	        throw new SeedFormatException("unable to get name description for blockette " +
	                blkType + ": " + e);
	    }
	}
	
	/**
	 * Return the category (Control Header type) of the blockette type.
	 */
	public static String getCategory(int blkType) throws SeedException {
	    try {
	        if (optimized) {
	            return arrayBlkCategory[ blocketteTypeMap[blkType] ];
	        } else
	            return getField(blkType,0,3);  // return 0th field, 3rd column
	    } catch (Exception e) {
	        throw new SeedFormatException("unable to get category description for blockette " +
	                blkType + ": " + e);
	    }
	}
	
	/**
	 * Return the initial SEED version of the blockette type.  This represents
	 * the version of SEED where this blockette was introduced.
	 */
	public static float getVersion(int blkType) throws SeedException {
		StringTokenizer verTok = new StringTokenizer(getNumFields(blkType),"=");  // tokenize the expression
		String firstVer = null;
		if (verTok.hasMoreTokens()) {
			firstVer = verTok.nextToken();  // get first version number
		} else {
			throw new SeedFormatException("unable to parse NumFields description for blockette " + blkType);
		}
		return Float.parseFloat(firstVer);
	}
	
	public static float getDefaultVersion() {
		// return the default version of SEED blockettes
		return defaultSEEDVersion;
	}
	
	/**
	 * Return the number of fields String, which is a comma-separated list associating
	 * number of fields with the SEED version, for the indicated blockette type.
	 */
	public static String getNumFields(int blkType) throws SeedException {
	    try {
	        if (optimized) {
	            return arrayNumFields[ blocketteTypeMap[blkType] ];
	        } else 
	            return getField(blkType,0,4);  // return the 0th field, 4th column
	    } catch (Exception e) {
	        throw new SeedFormatException("unable to get NumFields description for blockette " +
	                blkType + ": " + e);
	    }
	}
	
	/**
	 * Return the number of fields in this blockette type based
	 * on the indicated SEED version.
	 */
	public static int getNumFields(int blkType, float version) throws SeedException {
		int numFld = 0;  // default value
		String numFieldsStr = getNumFields(blkType);  // get number of fields aggregate
		int begin = 0;
		int end = 0;
		float sampVer = 0.0F;  // sample version numbers here
		String groupStr = null;  // temporary parse string for version groupings
		while (end > -1) {
			end = numFieldsStr.indexOf(",", begin);  // get next grouping
			if (end > -1) {
				groupStr = numFieldsStr.substring(begin,end);  // get next
			} else {
				groupStr = numFieldsStr.substring(begin);  // get last
			}
			int equalsIdx = groupStr.indexOf("=");  // find equal sign separator
			if (equalsIdx > -1) {
				sampVer = Float.parseFloat(groupStr.substring(0,equalsIdx));   // version number
			} else {
				throw new SeedFormatException("unable to get number of fields for version " 
						+ version + " of blockette " + blkType);
			}
			if (version < sampVer) {
				break; // we have hit a boundary, return the last remembered number fields value
			} else {
				// assign latest number fields value
				numFld = Integer.parseInt(groupStr.substring(equalsIdx+1));
			}
			begin = end + 1;
		}
		if (numFld == 0) {
			throw new SeedException("unable to get number of fields for blockette " + blkType + ", version " + version);
		}
		return numFld;  // return the number of fields
	}
	
	/**
	 * Return the full name of the indicated field for the blockette type.
	 * Makes use of array optimization if possible.
	 */
	public static String getFieldName(int blkType, int fieldNum) throws SeedException {
	    try {
	        if (optimized) {
	            return arrayFieldName[ blocketteTypeMap[blkType] ][fieldNum-1];
	        } else
	            return getField(blkType, fieldNum, 2);  // field name is column 2
	    } catch (Exception e) {
	        throw new SeedFormatException("unable to get field name for blockette " +
	                blkType + ", field " + fieldNum + ": " + e);
	    }
	}
	
	/**
	 * Return the data type of the indicated field for the blockette type.
	 * Makes use of array optimization if possible.
	 */
	public static String getFieldType(int blkType, int fieldNum) throws SeedException {
	    try {
	        if (optimized) {
	            return String.valueOf(arrayFieldType[ blocketteTypeMap[blkType] ][fieldNum-1]);
	        } else
	            return getField(blkType, fieldNum, 3);  // field type is column 3
	    } catch (Exception e) {
	        throw new SeedFormatException("unable to get field type for blockette " +
	                blkType + ", field " + fieldNum + ": " + e);
	    }
	}
	
	/**
	 * Return the length in bytes of the indicated field for the blockette type.
	 * Makes use of array optimization if possible.
	 */
	public static String getFieldLength(int blkType, int fieldNum) throws SeedException {
	    try {
	        if (optimized) {
	            return arrayFieldLength[ blocketteTypeMap[blkType] ][fieldNum-1];
	        } else
	            return getField(blkType,fieldNum,4);  // field type is column 4
	    } catch (Exception e) {
	        throw new SeedFormatException("unable to get field length for blockette " +
	                blkType + ", field " + fieldNum + ": " + e);
	    }
	}
	
	/**
	 * Return the field mask or flags for the indicated field based on the
	 * blockette type.  Makes use of array optimization if possible.
	 */
	public static String getFieldMask(int blkType, int fieldNum) throws SeedException {
	    try {
	        if (optimized) {
	            return arrayFieldMask[ blocketteTypeMap[blkType] ][fieldNum-1];
	        } else
	            return getField(blkType,fieldNum,5);  // field type is column 5
	    } catch (Exception e) {
	        throw new SeedFormatException("unable to get field mask for blockette " +
	                blkType + ", field " + fieldNum + ": " + e);
	    }
	}
	
	/**
	 * Indicate whether this is a repeating field.
	 * If the indicated field is a repeating field, it will have a non-zero value that
	 * lists the field number that indicates the number of times this field repeats.
	 * Otherwise the value is zero for non-repeating fields.
	 * Makes use of array optimization if possible.
	 */
	public static int getFieldRepeat(int blkType, int fieldNum) throws SeedException {
	    try {
	        if (optimized) {
	            return arrayFieldRepeat[ blocketteTypeMap[blkType] ][fieldNum-1];
	        } else
	            return Integer.parseInt(getField(blkType,fieldNum,6)); // field repeat is column 6
	    } catch (Exception e) {
	        throw new SeedFormatException("unable to get field repeat for blockette " +
	                blkType + ", field " + fieldNum + ": " + e);
	    }
	}
        
        /**
         * get the field number that contains the response stage number for the indicated blockette type.
         * for registered response blockettes, a non-zero field number will be returned.
         * for everything else, a -1 will be returned, indicating that a stage number is not present.
         */
        public static int getStageField(int blkType) throws SeedException {
            try {
                for (int i = 0; i < stageNumberFields.length; i+=2) {  // increment in pairs
                    if (stageNumberFields[i] == blkType) {
                        return stageNumberFields[i+1];  // return the associated field number
                    } 
                }
                // no blockette matched?  then we must not be a response blockette
                return -1;
            } catch (Exception e) {
                throw new SeedFormatException("error occurred attempting to return stage field number, blktype=" + blkType);
            }
        }
        
        
        /**
         *  return true if this is a response blockette
         * 
         */
        public static boolean isResponseBlkType(int blkType) throws SeedException {
            return (getStageField(blkType) > 0);
        }
	
	// display methods
	//
	
    /**
     * Format the string value according the the field number's mask
     * of the specified blockette type.
     * The formatting will ensure that it is SEED compliant.
     * This method is intended only for numeric fields but will
     * pass alphanumeric fields safely through.
     *
     * This updated version of formatDecimal makes use of the DataFormat factory class
     * developed by ISTI.
     */
    public static String formatDecimal (int blkType, int fieldNum, Object value) throws SeedException {
    	// if value is null, return a blank string
    	if (value == null) return "";
    	// get the field mask
    	String fldMask = getFieldMask(blkType,fieldNum);
    	// decimal and float types will have a mask starting with a double quote
    	if (fldMask.startsWith("\"")) {      // number format string - type D or F
    		int end = fldMask.indexOf("\"",1);  // find ending quote
    		String fmtString = fldMask.substring(1,end); // get the mask string between quotes
    		String fldLen = getFieldLength(blkType,fieldNum);  // get field length
    		int intLen = Integer.parseInt(fldLen);  // type D and F only have one length value
    		boolean plusPrefixFlag = false;
    		if (fmtString.length() < intLen) {
    			plusPrefixFlag = true;  // for signed values, use plus sign for positive values
    		}
    		DataFormat df = DataFormat.getDataFormat(fmtString, plusPrefixFlag);
    		// produce formatted text
    		String fmtVal;
    		try {
    			fmtVal = df.format(value);
    		} catch (Exception e) {
    			throw new SeedException("DecimalFormat exception (" + e + "), blkType = " + blkType +
    			", fieldNum = " + fieldNum + ", value Object = " + value);
    		}

    		if (fmtVal.length() > intLen) { // if resultant string too long, trim off leading ('+') sign character
    			return fmtVal.substring(1);
    		}
    		return fmtVal;
    	} else { // not a decimal value, just pass it through
    		return value.toString();
    	}
    }


	
	/**
	 * Convert a String to SEED binary for one blockette field.
	 * Take the value in <b>stringVal</b> and return a byte array properly
	 * sized and formatted for the indicated blockette type and field number.
	 */
	public static byte[] getBytes (int blkType, int fieldNum, String stringVal) throws SeedException {
		// get the blockette field data type
		String fldType = getFieldType(blkType,fieldNum);
		// get the blockette field byte length
		String fldLen = getFieldLength(blkType,fieldNum);
		// perform byte array formatting based on the data type
		if (fldType.equals("L")) {
			// check for 'blank' values
			if (stringVal.length() == 0) return new byte[0];  // return zero-length array
			// value should be in Vector.toString() format: "[Value1, Value2, Value3, ...]"
			StringTokenizer listTok = new StringTokenizer(stringVal,"[], ");
			// each element in the list should be a fixed-length unsigned integer.
			// ensure that each element is the proper format and length.
			String fldMask = getFieldMask(blkType,fieldNum);
			StringTokenizer ft = new StringTokenizer(fldMask,"/");  // special case for List type
			//String fmtString = "\"" + ft.nextToken() + "\"";  // generate the format string
			String fmtString = ft.nextToken();  // generate the format string
			DecimalFormat df = new DecimalFormat(fmtString);
			int intLen = Integer.parseInt(fldLen);
			Vector fmtVector = new Vector(8,8);  // vector to store formatted strings
			while (listTok.hasMoreTokens()) {
				String nextVal = listTok.nextToken();  // get the next value in the list
				String fmtVal = df.format(Long.parseLong(nextVal));  // format the value
				fmtVector.add(fmtVal);  // add string to vector
			}
			// now initialize the return array
			int vecSize = fmtVector.size();
			byte[] retArray = new byte[vecSize * intLen];  // proper sizing
			// add each element to the return vector
			for (int i = 0; i < vecSize; i++) {
				System.arraycopy(((String)fmtVector.get(i)).getBytes(), 0,
						retArray, (i * intLen), intLen);
			}
			return retArray;
		} else if (fldType.equals("D") || fldType.equals("F")) {
			// check for 'blank' values
			if (stringVal.length() == 0) return new byte[0];  // return zero-length array
			// trim out trailing and leading spaces
			String trimVal = stringVal.trim();
			// check for 'blank' values
			if (trimVal.length() == 0) return new byte[0];  // return zero-length array
			int intLen = Integer.parseInt(fldLen);    // get the length of the blockette field
			String fmtVal = formatDecimal(blkType,fieldNum,trimVal);  // format the string value
			// now convert to bytes and return
			// force the alpha string to fit the fixed byte size -- sanity check
			byte[] tempArray = fmtVal.getBytes();     // convert string to bytes
			int copyLen = tempArray.length;           // get the length of resultant array
			byte[] retArray = new byte[intLen];       // initialize return array for the blockette field size
			if (intLen < copyLen) copyLen = intLen;   // get the smaller number of the two for copy length
			System.arraycopy(tempArray,0,retArray,0,copyLen); // transfer copy length bytes to return array
			for (int i = copyLen; i < intLen; i++) retArray[i] = (byte) 32; // fill the remaining space with SPACE characters
			return retArray;
		} else if (fldType.equals("A")) {
			// check for 'blank' values
			if (stringVal.length() == 0) return new byte[0];  // return zero-length array
			// force the alpha string to fit the fixed byte size
			byte[] tempArray = stringVal.getBytes();  // convert string to bytes
			int copyLen = tempArray.length;           // get the length of resultant array
			int intLen = Integer.parseInt(fldLen);    // get the length of the blockette field
			byte[] retArray = new byte[intLen];       // initialize return array for the blockette field size
			if (intLen < copyLen) copyLen = intLen;   // get the smaller number of the two for copy length
			System.arraycopy(tempArray,0,retArray,0,copyLen); // transfer copy length bytes to return array
			for (int i = copyLen; i < intLen; i++) retArray[i] = (byte) 32; // fill the remaining space with SPACE characters
			return retArray;
		} else if (fldType.equals("V")) {
			// check for 'blank' values
			if (stringVal.length() == 0) return tildeArray;  // return just a tilde
			// this also involves a simple conversion, but we are not concerned about an exact match
			// of byte size, only that it is less than/equal to the maximum
			byte[] tempArray = stringVal.getBytes();
			//System.err.println("DEBUG: V field fldLen=" + fldLen);
			int begin = fldLen.indexOf("-");
			int maxLen = 0;
			if (begin == -1) {
				// no dash? read from beginning of string
				maxLen = Integer.parseInt(fldLen);
			} else {
				// else, get value after the dash for the max value
				maxLen = Integer.parseInt(fldLen.substring(begin+1));
			}
			int copyLen = tempArray.length;
			//System.err.println("DEBUG: copyLen=" + copyLen + ", maxLen=" + maxLen);
			if (copyLen > maxLen) copyLen = maxLen;  // copy no more characters than the max allowable
			byte[] retArray = new byte[copyLen+1];   // allocate space for return array
			System.arraycopy(tempArray,0,retArray,0,copyLen);  // copy over copyLen characters
			retArray[copyLen] = '~';   // append a tilde marker to the end
			return retArray;
		} else if (fldType.equals("B")) {
			// check for 'blank' values
			if (stringVal.length() == 0) return new byte[0];  // return zero-length array
			String fldMask = getFieldMask(blkType,fieldNum);
			int intLen = Integer.parseInt(fldLen);
			if (fldMask.equals("BTIME")) {
				Btime bTime = new Btime(stringVal);
				return bTime.getByteTime();
			} else if (fldMask.equals("UBYTE") || fldMask.equals("BYTE")) {
				// one byte
				// we must take into account a possible array of byte values (space-separated)
				StringTokenizer strTok = new StringTokenizer(stringVal," ");
				byte[] retArray = new byte[intLen];
				byte[] tmpArray = new byte[2];
				int i = 0;
				while (strTok.hasMoreTokens() && i < intLen) {  // for each element in array up to field len
					int intVal = Integer.parseInt(strTok.nextToken());  // get integer value
					tmpArray = Utility.intToShortBytes(intVal); // extracting byte value from integer
					retArray[i] = tmpArray[1];  // retArray gets least significant byte (value 0-255)
					i++;
				}
				return retArray;
			} else if (fldMask.equals("FLOAT")) {
				// four-byte float
				float fVal = Float.parseFloat(stringVal);   // get float value
				int bInt = Float.floatToIntBits(fVal);      // get IEEE byte representation
				return Utility.longToIntBytes(bInt); // return 32-bit array
			} else if (fldMask.equals("UWORD") || fldMask.equals("WORD")) {
				// two-byte word
				int intVal = Integer.parseInt(stringVal);  // get integer value
				return Utility.intToShortBytes(intVal);
			} else if (fldMask.equals("ULONG") || fldMask.equals("LONG")) {
				// four-byte word
				long lngVal = Long.parseLong(stringVal);   // get long value
				return Utility.longToIntBytes(lngVal);
			} else throw new SeedFormatException("unrecognized binary field type: " + fldMask);
		} else throw new SeedFormatException("unrecognized field type: " + fldType);
	}
	
	// if listStr is a toString Vector representation from a List field, then pull out all of the values
	// from the list and return it in Vector form.  Basically the reverse of Vector.toString().  If the string
	// is not in Vector representation, then simply return the single string in a Vector of size 1.
	public static Vector getListVector(String listStr) {
		Vector returnVec = new Vector();
		if (listStr.indexOf('[') == 0) {
			// pull back the listIndex instance in the list
			int begin = 1;
			int end = 1;
			// from 0 to n-1 values...
			while (end > 0) {
				end = listStr.indexOf(',',begin);
				if (end == -1) break;  // out of delimiters?  exit loop
				// extract the value to the vector
				returnVec.add(listStr.substring(begin,end));
				begin = end+1; // advance begin marker
			}
			// add the final value to the vector
			end = listStr.indexOf(']'); // last/only item in the list
			if (end > begin) returnVec.add(listStr.substring(begin,end)); // last/only value in vector
			//else an empty vector is returned...or at least not the last element
		} else {
			returnVec.add(listStr); // single non-vector value
		}
		return returnVec;
	}
	
	// if listStr is a toString Vector representation from a List field, then pull out the value
	// at the indicated index and return that value.  If not a Vector representation, then
	// return the listStr value.  If listIndex exceeds the number of entries in the list, then
	// an empty string is returned.
	public static String getListValue(String listStr, int listIndex) {
		if (listStr.indexOf('[') == 0) {
			// pull back the listIndex instance in the list
			int begin = 1;
			int end = 1;
			int a = 0;
			for (; a < listIndex; a++) {
				//System.err.println("DEBUG: a=" + a + ", begin=" + begin +
				//", end=" + end);
				end = listStr.indexOf(',',begin);
				if (end == -1) break;  // out of delimiters?
				begin = end+1; // advance begin marker
			}
			// set end to its proper value
			//System.err.println("DEBUG: last end=" + end);
			end = listStr.indexOf(',',begin);
			if (end <= begin) end = listStr.indexOf(']'); // last/only item in the list
			// get the substring
			//System.err.println("DEBUG: substring on " + refStr + ": begin=" + begin + ", end=" + end);
			if (end > begin) listStr = listStr.substring(begin,end);
			else listStr = "";  // bad extraction?  make an empty string
		}
		return listStr;
	}
	
	// if listStr is a Vector representation, such as from a List field, then insert and replace
	// the value located at listIndex with the offered String value, returning the result.
	// If listStr is not a Vector representation, then just return the value String parameter.
	public static String setListValue (String listStr, String value, int listIndex) {
		String returnStr = value;  // this is how we will be set if listStr is not a Vector string
		Vector listVec = new Vector();
		if (listStr != null && listStr.indexOf('[') == 0) {
		    // first transcribe the contents of the offered listStr to a new vector
			int begin = 1;
			int end = begin;
			while (begin < listStr.length()) {
				end = listStr.indexOf(',',begin);
				if (end == -1) end = listStr.length() - 1;  // last element
				if (begin < end) listVec.add(listStr.substring(begin,end));  // add to Vector
				begin = end+1; // advance begin marker
			}
			// now...write the provided new value to the vector at the specified list index
			if (listVec.size() <= listIndex) listVec.setSize(listIndex+1);  // increase size?
			listVec.setElementAt(value,listIndex);
			returnStr = listVec.toString();  // take the resulting vector and convert to string
		}
		return returnStr;
	}
	
	//public static String insertListValue()
	
	//public static String removeListValue()
	
	
	/**
	 * Get blockette definitions.
	 * All information is provided as a set of static String returns in a rudimentary
	 * selection branch so that no time is wasted loading data, since this is
	 * a statically invoked class...the data is effectively bound with the code.
	 * The first record is the general blockette definition, followed by subsequent
	 * records detailing each blockette field.  Each record is separated by a newline
	 * character and the columns of each record are tab-separated.
	 * The <i>Number Fields</i> column is a specially formatted string of
	 * the form a0=b0,a1=b1,...
	 * where (a) is the SEED version and (b) is the number of fields available
	 * at that version.
	 * Newer versions always append fields to the end of the blockette.
	 * The first version number (a0) always represents the initial version of
	 * the blockette.  This method is kept public so that applications have the
	 * option of dumping the entire definition outward for their own purposes,
	 * such as some form of documentation.
	 */	
	public static String getBlocketteDefinition(int blkType) throws SeedException {
		switch (blkType) {
		
		// VOLUME INDEX CONTROL HEADER BLOCKETTES
		
		case 5:
			return
			//Type	Name					Category	Number Fields
			"005	Field Volume Identifier Blockette	Volume Index	2.0=5\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 005		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Version of format		D	4	\"00.0\"		0\n" +
			"4	Logical record length		D	2	\"00\"			0\n" +
			"5	Beginning of volume		V	1-22	TIME			0\n";
		case 8:
			return
			//Type	Name					Category	Number Fields
			"008	Telemetry Volume Identifier Blockette	Volume Index	2.1=11\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 008		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Version of format		D	4	\"00.0\"		0\n" +
			"4	Logical record length		D	2	\"00\"			0\n" +
			"5	Station identifier		A	5	[UN]			0\n" +
			"6	Location identifier		A	2	[UNS]			0\n" +
			"7	Channel identifier		A	3	[UN]			0\n" +
			"8	Beginning of volume		V	1-22	TIME			0\n" +
			"9	End of volume			V	1-22	TIME			0\n" +
			"10	Station info effective date	V	1-22	TIME			0\n" +
			"11	Channel info effective date	V	1-22	TIME			0\n" +
			"12	Network Code			A	2	[UN]			0\n";
		case 10:
			return
			//Type	Name					Category	Number Fields
			"010	Volume Identifier Blockette		Volume Index	2.0=6,2.3=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 010		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Version of format		D	4	\"00.0\"		0\n" +
			"4	Logical record length		D	2	\"00\"			0\n" +
			"5	Beginning time			V	1-22	TIME			0\n" +
			"6	End time			V	1-22	TIME			0\n" +
			"7	Volume Time			V	1-22	TIME			0\n" +
			"8	Originating Organization	V	1-80	[]			0\n" +
			"9	Label				V	1-80	[]			0\n";
		case 11:
			return
			//Type	Name					Category	Number Fields
			"011	Volume Station Header Index Blockette	Volume Index	2.0=5\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 011		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Number of stations		D	3	\"000\"			0\n" +
			// REPEAT fields 4-5 for the Number of stations:
			"4	Station identifier code		A	5	[]			3\n" +
			"5	Sequence no. of station header	D	6	\"000000\"		3\n";
		case 12:
			return
			//Type	Name					Category	Number Fields
			"012	Volume Time Span Index Blockette	Volume Index	2.0=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 012		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Number of spans in table	D	4	\"0000\"		0\n" +
			// REPEAT fields 4-6 for the Number of spans in table:
			"4	Beginning of span		V	1-22	TIME			3\n" +
			"5	End of span			V	1-22	TIME			3\n" +
			"6	Sequence no. of time span hdr.	D	6	\"000000\"		3\n";
			
			// ABBREVIATION DICTIONARY CONTROL HEADER BLOCKETTES
			
		case 30:
			return
			//Type	Name					Category			Number Fields
			"030	Data Format Dictionary Blockette	Abbreviation Dictionary		2.0=7\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 030		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Short descriptive name		V	1-50	[UNLPS]			0\n" +
			"4	Data format identifier code	D	4	\"0000\"		0\n" +
			"5	Data family type		D	3	\"000\"			0\n" +
			"6	Number of decoder keys		D	2	\"00\"			0\n" +
			// REPEAT field 7 for the Number of decoder keys:
			"7	Decoder keys			V	1-9999	[UNLPS]			6\n";
		case 31:
			return
			//Type	Name					Category			Number Fields
			"031	Comment Description Blockette		Abbreviation Dictionary		2.0=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 031		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Comment code key		D	4	\"0000\"		0\n" +
			"4	Comment class code		A	1	[U]			0\n" +
			"5	Description of comment		V	1-70	[UNLPS]			0\n" +
			"6	Units of comment level		D	3	\"000\"			0\n";
		case 32:
			return
			//Type	Name					Category			Number Fields
			"032	Cited Source Dictionary Blockette	Abbreviation Dictionary		2.0=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 032		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Source lookup code		D	2	\"00\"			0\n" +
			"4	Name of publication/author	V	1-70	[UNLPS]			0\n" +
			"5	Date published/catalog		V	1-70	[UNLPS]			0\n" +
			"6	Publisher name			V	1-50	[UNLPS]			0\n";
		case 33:
			return
			//Type	Name					Category			Number Fields
			"033	Generic Abbreviation Blockette		Abbreviation Dictionary		2.0=4\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 033		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Abbreviation lookup code	D	3	\"000\"			0\n" +
			"4	Abbreviation description	V	1-50	[UNLPS]			0\n";
		case 34:
			return
			//Type	Name					Category			Number Fields
			"034	Units Abbreviations Blockette		Abbreviation Dictionary		2.0=5\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 034		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Unit lookup code		D	3	\"000\"			0\n" +
			"4	Unit name			V	1-20	[UNP]			0\n" +
			"5	Unit description		V	0-50	[UNLPS]			0\n";
		case 35:
			return
			//Type	Name					Category			Number Fields
			"035	Beam Configuration Blockette		Abbreviation Dictionary		2.0=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 035		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Unit lookup code		D	3	\"000\"			0\n" +
			"4	Number of components		D	4	\"0000\"		0\n" +
			// REPEAT fields 5-9 for the Number of components
			"5	Station identifier		A	5	[UN]			4\n" +
			"6	Location identifier		A	2	[UNS]			4\n" +
			"7	Channel identifier		A	3	[UN]			4\n" +
			"8	Sub-channel identifier		D	4	\"0000\"		4\n" +
			"9	Component weight		D	5	\"0.000\"		4\n";
		case 41:
			return
			//Type	Name					Category			Number Fields
			"041	FIR Dictionary Blockette		Abbreviation Dictionary		2.2=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 041		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response Lookup Key		D	4	\"0000\"		0\n" +
			"4	Response Name			V	1-25	[UNL_]			0\n" +
			"5	Symmetry Code			A	1	[U]			0\n" +
			"6	Signal In Units			D	3	\"000\"			0\n" +
			"7	Signal Out Units		D	3	\"000\"			0\n" +
			"8	Number of Factors		D	4	\"0000\"		0\n" +
			// REPEAT field 9 for Number of Coefficients
			"9	FIR Coefficient			F	14	\"0.0000000E00\"	8\n";
                case 42:
			return
			//Type	Name						Category		Number Fields
			"042	Response (Polynomial) Dictionary Blockette	Abbreviation Dictionary	2.3=17\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 042		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response Lookup Key		D	4	\"0000\"		0\n" +
			"4	Response name			V	1-25	[UNL_]			0\n" +
			"5	Transfer Function Type		A	1	[U]			0\n" +
			"6	Stage signal input units	D	3	\"000\"			0\n" +
			"7	Stage signal output units	D	3	\"000\"			0\n" +
			"8	Polynomial Approximation Type	A	1	[U]			0\n" +
			"9	Valid Frequency Units   	A	1	[U]			0\n" +
			"10	Lower Valid Frequency Bound		F	12	\"0.00000E00\"		0\n" +
			"11	Upper Valid Frequency Bound		F	12	\"0.00000E00\"		0\n" +
			"12	Lower Bound of Approximation	F	12	\"0.00000E00\"		0\n" +
			"13	Upper Bound of Approximation	F	12	\"0.00000E00\"		0\n" +
			"14	Maximum Absolute Error		F	12	\"0.00000E00\"		0\n" +
			"15	Number of Polynomial Coeff.	D	3	\"000\"			0\n" +
			// REPEAT fields 16-17 for each polynomial coefficient:
			"16	Polynomial Coefficient		F	12	\"0.00000E00\"		15\n" +
			"17	Polynomial Coefficient Error	F	12	\"0.00000E00\"		15\n";
		case 43:
			return
			//Type	Name						Category		Number Fields
			"043	Response (Poles & Zeros) Dictionary Blockette	Abbreviation Dictionary	2.1=19\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 043		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response Lookup Key		D	4	\"0000\"		0\n" +
			"4	Response name			V	1-25	[UNL_]			0\n" +
			"5	Response type			A	1	[U]			0\n" +
			"6	Stage signal input units	D	3	\"000\"			0\n" +
			"7	Stage signal output units	D	3	\"000\"			0\n" +
			"8	AO normalization factor		F	12	\"0.00000E00\"		0\n" +
			"9	Normalization frequency (Hz)	F	12	\"0.00000E00\"		0\n" +
			"10	Number of complex zeros		D	3	\"000\"			0\n" +
			// REPEAT fields 11-14 for the Number of complex zeros:
			"11	Real zero			F	12	\"0.00000E00\"		10\n" +
			"12	Imaginary zero			F	12	\"0.00000E00\"		10\n" +
			"13	Real zero error			F	12	\"0.00000E00\"		10\n" +
			"14	Imaginary zero error		F	12	\"0.00000E00\"		10\n" +
			"15	Number of complex poles		D	3	\"000\"			0\n" +
			// REPEAT fields 16-19 for the Number of complex poles:
			"16	Real pole			F	12	\"0.00000E00\"		15\n" +
			"17	Imaginary pole			F	12	\"0.00000E00\"		15\n" +
			"18	Real pole error			F	12	\"0.00000E00\"		15\n" +
			"19	Imaginary pole error		F	12	\"0.00000E00\"		15\n";
		case 44:
			return
			//Type	Name						Category		Number Fields
			"044	Response (Coefficients) Dictionary Blockette	Abbreviation Dictionary	2.1=13\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 044		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response lookup key		D	4	\"0000\"		0\n" +
			"4	Response name			V	1-25	[UN_]			0\n" +
			"5	Response type			A	1	[U]			0\n" +
			"6	Signal input units		D	3	\"000\"			0\n" +
			"7	Signal output units		D	3	\"000\"			0\n" +
			"8	Number of numerators		D	4	\"0000\"		0\n" +
			// REPEAT fields 9-10 for the Number of numerators:
			"9	Numerator coefficient		F	12	\"0.00000E00\"		8\n" +
			"10	Numerator error			F	12	\"0.00000E00\"		8\n" +
			"11	Number of denominators		D	4	\"0000\"		0\n" +
			// REPEAT fields 12-13 for the Number of denominators:
			"12	Denominator coefficient		F	12	\"0.00000E00\"		11\n" +
			"13	Denominator error		F	12	\"0.00000E00\"		11\n";
		case 45:
			return
			//Type	Name					Category			Number Fields
			"045	Response List Dictionary Blockette	Abbreviation Dictionary		2.1=12\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 045		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response lookup key		D	4	\"0000\"		0\n" +
			"4	Response name			V	1-25	[UNL_]			0\n" +
			"5	Signal input units		D	3	\"000\"			0\n" +
			"6	Signal output units		D	3	\"000\"			0\n" +
			"7	Number of responses listed	D	4	\"0000\"		0\n" +
			// REPEAT fields 8-12 for the Number of responses listed:
			"8	Frequency (Hz)			F	12	\"0.00000E00\"		7\n" +
			"9	Amplitude			F	12	\"0.00000E00\"		7\n" +
			"10	Amplitude error			F	12	\"0.00000E00\"		7\n" +
			"11	Phase angle (degrees)		F	12	\"0.00000E00\"		7\n" +
			"12	Phase error (degrees)		F	12	\"0.00000E00\"		7\n";
		case 46:
			return
			//Type	Name					Category			Number Fields
			"046	Generic Response Dictionary Blockette	Abbreviation Dictionary		2.1=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 046		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response lookup key		D	4	\"0000\"		0\n" +
			"4	Response name			V	1-25	[UNL_]			0\n" +
			"5	Signal input units		D	3	\"000\"			0\n" +
			"6	Signal output units		D	3	\"000\"			0\n" +
			"7	Number of corners listed	D	4	\"0000\"		0\n" +
			// REPEAT fields 8-9 for the Number of corners listed:
			"8	Corner frequency (Hz)		F	12	\"0.00000E00\"		7\n" +
			"9	Corner slope (db/decade)	F	12	\"0.00000E00\"		7\n";
		case 47:
			return
			//Type	Name					Category			Number Fields
			"047	Decimation Dictionary Blockette		Abbreviation Dictionary		2.1=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 047		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response lookup key		D	4	\"0000\"		0\n" +
			"4	Response name			V	1-25	[UNL_]			0\n" +
			"5	Input sample rate		F	10	\"0.0000E00\"		0\n" +
			"6	Decimation factor		D	5	\"00000\"		0\n" +
			"7	Decimation offset		D	5	\"00000\"		0\n" +
			"8	Estimated delay (seconds)	F	11	\"0.0000E00\"		0\n" +
			"9	Correction applied (seconds)	F	11	\"0.0000E00\"		0\n";
		case 48:
			return
			//Type	Name						Category		Number Fields
			"048	Channel Sensitivity/Gain Dictionary Blockette	Abbreviation Dictionary	2.1=10\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 048		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response lookup key		D	4	\"0000\"		0\n" +
			"4	Response name			V	1-25	[UNL_]			0\n" +
			"5	Sensitivity/gain		F	12	\"0.00000E00\"		0\n" +
			"6	Frequency (Hz)			F	12	\"0.00000E00\"		0\n" +
			"7	Number of history values	D	2	\"00\"			0\n" +
			// REPEAT fields 8-10 for the Number of history values:
			"8	Sensitivity for calibration	F	12	\"0.00000E00\"		7\n" +
			"9	Freq. of calib. sensitivity	F	12	\"0.00000E00\"		7\n" +
			"10	Time of above calibration	V	1-22	TIME			7\n";
			
			// STATION CONTROL HEADER BLOCKETTES
			
		case 50:
			return
			//Type	Name					Category	Number Fields
			"050	Station Identifier Blockette		Station		2.0=15,2.3=16\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 050		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Station call letters		A	5	[UN]			0\n" +
			"4	Latitude (degrees)		D	10	\"00.000000\"		0\n" +
			"5	Longitude (degrees)		D	11	\"000.000000\"		0\n" +
			"6	Elevation (m)			D	7	\"0000.0\"		0\n" +
			"7	Number of channels		D	4	\"0000\"		0\n" +
			"8	Number of station comments	D	3	\"000\"			0\n" +
			"9	Site name			V	1-60	[UNLPS]			0\n" +
			"10	Network identifier code		D	3	\"000\"			0\n" +
			"11	32 bit word order		D	4	\"0000\"		0\n" +
			"12	16 bit word order		D	2	\"00\"			0\n" +
			"13	Start effective date		V	1-22	TIME			0\n" +
			"14	End effective date		V	0-22	TIME			0\n" +
			"15	Update flag			A	1	[]			0\n" +
			"16	Network Code			A	2	[UN]			0\n";
		case 51:
			return
			//Type	Name					Category	Number Fields
			"051	Station Comment Blockette		Station		2.0=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 051		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Beginning effective time	V	1-22	TIME			0\n" +
			"4	End effective time		V	1-22	TIME			0\n" +
			"5	Comment code key		D	4	\"0000\"		0\n" +
			"6	Comment level			D	6	\"000000\"		0\n";
		case 52:
			return
			//Type	Name					Category	Number Fields
			"052	Channel Identifier Blockette		Station		2.0=24\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 052		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Location identifier		A	2	[UNS]			0\n" +
			"4	Channel identifier		A	3	[UN]			0\n" +
			"5	Subchannel identifier		D	4	\"0000\"		0\n" +
			"6	Instrument identifier		D	3	\"000\"			0\n" +
			"7	Optional comment		V	0-30	[UNLPS]			0\n" +
			"8	Units of signal response	D	3	\"000\"			0\n" +
			"9	Units of calibration input	D	3	\"000\"			0\n" +
			"10	Latitude (degrees)		D	10	\"00.000000\"		0\n" +
			"11	Longitude (degrees)		D	11	\"000.000000\"		0\n" +
			"12	Elevation (m)			D	7	\"0000.0\"		0\n" +
			"13	Local depth (m)			D	5	\"000.0\"		0\n" +
			"14	Azimuth (degrees)		D	5	\"000.0\"		0\n" +
			"15	Dip (degrees)			D	5	\"00.0\"		0\n" +
			"16	Data format identifier code	D	4	\"0000\"		0\n" +
			"17	Data record length		D	2	\"00\"			0\n" +
			"18	Sample rate (Hz)		F	10	\"0.0000E00\"		0\n" +
			"19	Max clock drift (seconds)	F	10	\"0.0000E00\"		0\n" +
			"20	Number of comments		D	4	\"0000\"		0\n" +
			"21	Channel flags			V	0-26	[U]			0\n" +
			"22	Start date			V	1-22	TIME			0\n" +
			"23	End date			V	0-22	TIME			0\n" +
			"24	Update flag			A	1	[]			0\n";
		case 53:
			return
			//Type	Name					Category	Number Fields
			"053	Response (Poles & Zeros) Blockette	Station		2.0=18\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 053		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Transfer function type		A	1	[U]			0\n" +
			"4	Stage sequence number		D	2	\"00\"			0\n" +
			"5	Stage signal input units	D	3	\"000\"			0\n" +
			"6	Stage signal output units	D	3	\"000\"			0\n" +
			"7	AO normalization factor		F	12	\"0.00000E00\"		0\n" +
			"8	Normalization freq. f(n) (Hz)	F	12	\"0.00000E00\"		0\n" +
			"9	Number of complex zeros		D	3	\"000\"			0\n" +
			// REPEAT fields 10-13 for the Number of complex zeros:
			"10	Real zero			F	12	\"0.00000E00\"		9\n" +
			"11	Imaginary zero			F	12	\"0.00000E00\"		9\n" +
			"12	Real zero error			F	12	\"0.00000E00\"		9\n" +
			"13	Imaginary zero error		F	12	\"0.00000E00\"		9\n" +
			"14	Number of complex poles		D	3	\"000\"			0\n" +
			// REPEAT fields 15-18 for the Number of complex poles:
			"15	Real pole			F	12	\"0.00000E00\"		14\n" +
			"16	Imaginary pole			F	12	\"0.00000E00\"		14\n" +
			"17	Real pole error			F	12	\"0.00000E00\"		14\n" +
			"18	Imaginary pole error		F	12	\"0.00000E00\"		14\n";
		case 54:
			return
			//Type	Name					Category	Number Fields
			"054	Response (Coefficients) Blockette	Station		2.0=12\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 054		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Response type			A	1	[U]			0\n" +
			"4	Stage sequence number		D	2	\"00\"			0\n" +
			"5	Signal input units		D	3	\"000\"			0\n" +
			"6	Signal output units		D	3	\"000\"			0\n" +
			"7	Number of numerators		D	4	\"0000\"		0\n" +
			// REPEAT fields 8-9 for the Number of numerators:
			"8	Numerator coefficient		F	12	\"0.00000E00\"		7\n" +
			"9	Numerator error			F	12	\"0.00000E00\"		7\n" +
			"10	Number of denominators		D	4	\"0000\"		0\n" +
			// REPEAT fields 11-12 for the Number of denominators:
			"11	Denominator coefficient		F	12	\"0.00000E00\"		10\n" +
			"12	Denominator error		F	12	\"0.00000E00\"		10\n";
		case 55:
			return
			//Type	Name					Category	Number Fields
			"055	Response List Blockette			Station		2.0=11\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 055		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Stage sequence number		D	2	\"00\"			0\n" +
			"4	Signal input units		D	3	\"000\"			0\n" +
			"5	Signal output units		D	3	\"000\"			0\n" +
			"6	Number of responses listed	D	4	\"0000\"		0\n" +
			// REPEAT fields 7-11 for the Number of responses listed:
			"7	Frequency (Hz)			F	12	\"0.00000E00\"		6\n" +
			"8	Amplitude			F	12	\"0.00000E00\"		6\n" +
			"9	Amplitude error			F	12	\"0.00000E00\"		6\n" +
			"10	Phase angle (degrees)		F	12	\"0.00000E00\"		6\n" +
			"11	Phase error (degrees)		F	12	\"0.00000E00\"		6\n";
		case 56:
			return
			//Type	Name					Category	Number Fields
			"056	Generic Response Blockette		Station		2.0=8\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 056		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Stage sequence number		D	2	\"00\"			0\n" +
			"4	Signal input units		D	3	\"000\"			0\n" +
			"5	Signal output units		D	3	\"000\"			0\n" +
			"6	Number of corners listed	D	4	\"0000\"		0\n" +
			// REPEAT fields 7-8 for the Number of responses listed:
			"7	Corner frequency (Hz)		F	12	\"0.00000E00\"		6\n" +
			"8	Corner slope (db/decade)	F	12	\"0.00000E00\"		6\n";
		case 57:
			return
			//Type	Name					Category	Number Fields
			"057	Decimation Blockette			Station		2.1=8\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 057		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Stage sequence number		D	2	\"00\"			0\n" +
			"4	Input sample rate (Hz)		F	10	\"0.0000E00\"		0\n" +
			"5	Decimation factor		D	5	\"00000\"		0\n" +
			"6	Decimation offset		D	5	\"00000\"		0\n" +
			"7	Estimated delay (seconds)	F	11	\"0.0000E00\"		0\n" +
			"8	Correction applied (seconds)	F	11	\"0.0000E00\"		0\n";
		case 58:
			return
			//Type	Name					Category	Number Fields
			"058	Channel Sensitivity/Gain Blockette	Station		2.0=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 058		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Stage sequence number		D	2	\"00\"			0\n" +
			"4	Sensitivity/gain S(d)		F	12	\"0.00000E00\"		0\n" +
			"5	Frequency (Hz) f(s)		F	12	\"0.00000E00\"		0\n" +
			"6	Number of history values	D	2	\"00\"			0\n" +
			// REPEAT fields 7-9 for the Number of history values:
			"7	Sensitivity for calibration	F	12	\"0.00000E00\"		6\n" +
			"8	Frequency of calibration (Hz)	F	12	\"0.00000E00\"		6\n" +
			"9	Time of above calibration	V	1-22	TIME			6\n";
		case 59:
			return
			//Type	Name					Category	Number Fields
			"059	Channel Comment Blockette		Station		2.0=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 059		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Beginning effective time	V	1-22	TIME			0\n" +
			"4	End effective time		V	0-22	TIME			0\n" +
			"5	Comment code key		D	4	\"0000\"		0\n" +
			"6	Comment level			D	6	\"000000\"		0\n";
		case 60:
			return
			//Type	Name					Category	Number Fields
			"060	Response Reference Blockette		Station		2.1=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 060		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Number of stages		D	2	\"00\"			0\n" +
			// REPEAT field 4-5, for Number of stages:
			"4	Stage sequence number		D	2	\"00\"			3\n" +
			"5	Number of responses		D	2	\"00\"			3\n" +
			// REPEAT field 6, for Number of responses (within each stage):
			// note special type flag and mask, special to Blockette 60: L = List
			"6	Response lookup key		L	4	/0000/			5\n";
		case 61:
			return
			//Type	Name					Category	Number Fields
			"061	FIR Response Blockette			Station		2.2=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 061		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Stage sequence number		D	2	\"00\"			0\n" +
			"4	Response Name			V	1-25	[ULN_]			0\n" +
			"5	Symmetry Code			A	1	[U]			0\n" +
			"6	Signal In Units			D	3	\"000\"			0\n" +
			"7	Signal Out Units		D	3	\"000\"			0\n" +
			"8	Number of Coefficients		D	4	\"0000\"		0\n" +
			// REPEAT field 9 for the Number of Coefficients
			"9	FIR Coefficient			F	14	\"0.0000000E00\"	8\n";
		case 62:
			return
			//Type	Name					Category	Number Fields
			"062	Response Polynomial Blockette		Station		2.3=16\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 062		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"			0\n" +
			"3	Transfer Function Type	A	1	[U]				0\n" +
			"4	Stage Sequence Number		D	2	\"00\"			0\n" +
			"5	Stage Signal Input Units	D	3	\"000\"			0\n" +
			"6	Stage Signal Output Units	D	3	\"000\"			0\n" +
			"7	Polynomial Approx'n Type	A	1	[U]				0\n" +
			"8	Valid Frequency Units		A	1	[U]				0\n" +
			"9	Lower Valid Freq Bound	F	12	\"0.00000E00\"	0\n" +
			"10	Upper Valid Freq Bound	F	12	\"0.00000E00\"	0\n" +
			"11	Lower Bound of Approx'n	F	12	\"0.00000E00\"	0\n" +
			"12	Upper Bound of Approx'n	F	12	\"0.00000E00\"	0\n" +
			"13	Maximum Absolute Error	F	12	\"0.00000E00\"	0\n" +
			"14	Number of Polynomial Coeff	D	3	\"000\"		0\n" +
			// REPEAT fields 15 and 16 for the Number of Polynomial Coefficients
			"15	Polynomial Coefficient	F	12	\"0.00000E00\"	14\n" +
			"16	Polynomial Coeff Error	F	12	\"0.00000E00\"	14\n";
			
			// TIME SPAN CONTROL HEADER BLOCKETTES
			
		case 70:
			return
			//Type	Name					Category	Number Fields
			"070	Time Span Identifier Blockette		Time Span	2.0=5\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 070		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Time span flag			A	1	[U]			0\n" +
			"4	Beginning time of data span	V	1-22	TIME			0\n" +
			"5	End time of data span		V	1-22	TIME			0\n";
		case 71:
			return
			//Type	Name					Category	Number Fields
			"071	Hypocenter Information Blockette	Time Span	2.0=11,2.3=14\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 071		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Origin time of event		V	1-22	TIME			0\n" +
			"4	Hypocenter source identifier	D	2	\"00\"			0\n" +
			"5	Latitude of event (degrees)	D	10	\"00.000000\"		0\n" +
			"6	Longitude of event (degrees)	D	11	\"000.000000\"		0\n" +
			"7	Depth (Km)			D	7	\"0000.00\"		0\n" +
			"8	Number of magnitudes		D	2	\"00\"			0\n" +
			// REPEAT fields 9-11 for the Number of magnitudes:
			"9	Magnitude			D	5	\"00.00\"		8\n" +
			"10	Magnitude type			V	1-10	[UNLPS]			8\n" +
			"11	Magnitude source		D	2	\"00\"			8\n" +
			"12	Seismic region			D	3	\"000\"			0\n" +
			"13	Seismic Location		D	4	\"0000\"		0\n" +
			"14	Region Name			V	1-40	[UNLPS]			0\n";
		case 72:
			return
			//Type	Name					Category	Number Fields
			"072	Event Phases Blockette			Time Span	2.0=10,2.3=12\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 072		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Station identifier		A	5	[UN]			0\n" +
			"4	Location identifier		A	2	[UNS]			0\n" +
			"5	Channel identifier		A	3	[UN]			0\n" +
			"6	Arrival time of phase		V	1-22	TIME			0\n" +
			"7	Amplitude of signal		F	10	\"0.0000E00\"		0\n" +
			"8	Period of signal (seconds)	F	10	\"0.0000E00\"		0\n" +
			"9	Signal-to-noise ratio		F	10	\"0.0000E00\"		0\n" +
			"10	Name of phase			V	1-20	[UNLP]			0\n" +
			"11	Source				D	2	\"00\"			0\n" +
			"12	Network Code			A	2	[UN]			0\n";
		case 73:
			return
			//Type	Name					Category	Number Fields
			"073	Time Span Data Start Index Blockette	Time Span	2.0=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 073		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Number of data pieces		D	4	\"0000\"		0\n" +
			// REPEAT fields 4-9 for the Number of data pieces:
			"4	Station identifier		A	5	[UN]			3\n" +
			"5	Location identifier		A	2	[UNS]			3\n" +
			"6	Channel identifier		A	3	[UN]			3\n" +
			"7	Time of record			V	1-22	TIME			3\n" +
			"8	Sequence number of first record	D	6	\"000000\"		3\n" +
			"9	Sub-sequence number		D	2	\"00\"			3\n";
		case 74:
			return
			//Type	Name					Category	Number Fields
			"074	Time Series Index Blockette		Time Span	2.1=15,2.3=16\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 074		D	3	\"000\"			0\n" +
			"2	Length of blockette		D	4	\"0000\"		0\n" +
			"3	Station identifier		A	5	[UN]			0\n" +
			"4	Location identifier		A	2	[UNS]			0\n" +
			"5	Channel identifier		A	3	[UN]			0\n" +
			"6	Series start time		V	1-22	TIME			0\n" +
			"7	Sequence number of first data	D	6	\"000000\"		0\n" +
			"8	Sub-sequence number		D	2	\"00\"			0\n" +
			"9	Series end time			V	1-22	TIME			0\n" +
			"10	Sequence number of last record	D	6	\"000000\"		0\n" +
			"11	Sub-sequence number		D	2	\"00\"			0\n" +
			"12	Number of accelerator repeats	D	3	\"000\"			0\n" +
			// REPEAT fields 13-15 for the Number of accelerator repeats:
			"13	Record start time		V	1-22	TIME			12\n" +
			"14	Sequence number of record	D	6	\"000000\"		12\n" +
			"15	Sub-sequence number		D	2	\"00\"			12\n" +
			"16	Network Code			A	2	[UN]			0\n";
			
			// DATA RECORD BLOCKETTES
			
		case 999:     // Fixed Section Data Header -- assigned special number internal to application
			// FSDH has special structure assigned here to make it fit with Blockette model.
			// The first three fields are non-standard to the specification to make it more
			// Blockette-like.  Length is 60 bytes.
			return
			//Type	Name				Category	Number Fields
			"999	Fixed Section of Data Header	Data Record	2.0=18\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			// NONSTANDARD - FSDH is given a numeric designation of 999
			"1	Blockette type - 999		D	2	UWORD			0\n" +
			// NONSTANDARD - letters indicating the level of quality control of the data, which
			// is extracted from field 2 and possibly field 3 of the FSDH, currently.  Each letter
			// should have a unique meaning.  Current proposed letters are:
			// field 2:
			// D = default data (might be QC, might be Real Time un-QC'd)
			// R = real time data (un-QC'd)
			// Q = quality controlled data (QC'd)
                        // M = merged data
			// field 3:
			// P = primary data source
			// S = secondary data source
			//
			// space characters may be used for any leftover space to the right of the flags
			// (left-justified)
			//
			"2	Data Quality Flags		A	8	[UNS]			0\n" +
			// NONSTANDARD - time stamp to indicate approximately when the data record was
			// synthesized, NOT the time of object creation, NOT the start time of the data
			// in this record.  This helps distinguish data records that cover the same time span
			// but are generated at different times, possibly reflecting resubmitted, corrected data.
			"3	Data Arrival Time Stamp		B	10	BTIME			0\n" +
			// BEGIN standard FSDH fields here
			"4	Station identifier code		A	5	[UN]			0\n" +
			"5	Location identifier		A	2	[UNS]			0\n" +
			"6	Channel identifier		A	3	[UN]			0\n" +
			"7	Network Code			A	2	[]			0\n" +
			"8	Record start time		B	10	BTIME			0\n" +
			"9	Number of samples		B	2	UWORD			0\n" +
			"10	Sample rate factor		B	2	WORD			0\n" +
			"11	Sample rate multiplier		B	2	WORD			0\n" +
			"12	Activity flags			B	1	UBYTE			0\n" +
			"13	I/O and clock flags		B	1	UBYTE			0\n" +
			"14	Data quality flags		B	1	UBYTE			0\n" +
			"15	No. of blockettes that follow	B	1	UBYTE			0\n" +
			"16	Time correction			B	4	LONG			0\n" +
			"17	Beginning of data		B	2	UWORD			0\n" +
			"18	First blockette			B	2	UWORD			0\n";
		case 100:
			return
			//Type	Name					Category	Number Fields
			"100	Sample Rate Blockette			Data Record	2.3=5\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 100		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Actual Sample Rate		B	4	FLOAT			0\n" +
			"4	Flags (to be defined)		B	1	BYTE			0\n" +
			"5	Reserved byte			B	3	UBYTE			0\n";
		case 200:
			return
			//Type	Name					Category	Number Fields
			"200	Generic Event Detection Blockette	Data Record	2.0=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 200		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Signal amplitude		B	4	FLOAT			0\n" +
			"4	Signal period			B	4	FLOAT			0\n" +
			"5	Background estimate		B	4	FLOAT			0\n" +
			"6	Event detection flags		B	1	UBYTE			0\n" +
			"7	Reserved byte			B	1	UBYTE			0\n" +
			"8	Signal onset time		B	10	BTIME			0\n" +
			"9	Detector Name			A	24	[]			0\n";
		case 201:
			return
			//Type	Name					Category	Number Fields
			"201	Murdock Event Detection Blockette	Data Record	2.0=12\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 201		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Signal amplitude		B	4	FLOAT			0\n" +
			"4	Signal period			B	4	FLOAT			0\n" +
			"5	Background estimate		B	4	FLOAT			0\n" +
			"6	Event detection flags		B	1	UBYTE			0\n" +
			"7	Reserved byte			B	1	UBYTE			0\n" +
			"8	Signal onset time		B	10	BTIME			0\n" +
			"9	Signal-to-noise ratio values	B	6	UBYTE			0\n" +
			"10	Lookback value			B	1	UBYTE			0\n" +
			"11	Pick algorithm			B	1	UBYTE			0\n" +
			"12	Detector name			A	24	[]			0\n";
		case 300:
			return
			//Type	Name					Category	Number Fields
			"300	Step Calibration Blockette		Data Record	2.0=13\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 300		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Beginning of calibration time	B	10	BTIME			0\n" +
			"4	Number of step calibrations	B	1	UBYTE			0\n" +
			"5	Calibration flags		B	1	UBYTE			0\n" +
			"6	Step duration			B	4	ULONG			0\n" +
			"7	Interval duration		B	4	ULONG			0\n" +
			"8	Calibration signal amplitude	B	4	FLOAT			0\n" +
			"9	Channel with calibration input	A	3	[]			0\n" +
			"10	Reserved byte			B	1	UBYTE			0\n" +
			"11	Reference amplitude		B	4	ULONG			0\n" +
			"12	Coupling			A	12	[]			0\n" +
			"13	Rolloff				A	12	[]			0\n";
		case 310:
			return
			//Type	Name					Category	Number Fields
			"310	Sine Calibration Blockette		Data Record	2.0=13\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 310		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Beginning of calibration time	B	10	BTIME			0\n" +
			"4	Reserved byte			B	1	UBYTE			0\n" +
			"5	Calibration flags		B	1	UBYTE			0\n" +
			"6	Calibration duration		B	4	ULONG			0\n" +
			"7	Period of signal (seconds)	B	4	FLOAT			0\n" +
			"8	Amplitude of signal		B	4	FLOAT			0\n" +
			"9	Channel with calibration input	A	3	[]			0\n" +
			"10	Reserved byte			B	1	UBYTE			0\n" +
			"11	Reference amplitude		B	4	ULONG			0\n" +
			"12	Coupling			A	12	[]			0\n" +
			"13	Rolloff				A	12	[]			0\n";
		case 320:
			return
			//Type	Name					Category	Number Fields
			"320	Pseudo-random Calibration Blockette	Data Record	2.0=13\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 320		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Beginning of calibration time	B	10	BTIME			0\n" +
			"4	Reserved byte			B	1	UBYTE			0\n" +
			"5	Calibration flags		B	1	UBYTE			0\n" +
			"6	Calibration duration		B	4	ULONG			0\n" +
			"7	Peak-to-peak amplitude of steps	B	4	FLOAT			0\n" +
			"8	Channel with calibration output	A	3	[]			0\n" +
			"9	Reserved byte			B	1	UBYTE			0\n" +
			"10	Reference amplitude		B	4	ULONG			0\n" +
			"11	Coupling			A	12	[]			0\n" +
			"12	Rolloff				A	12	[]			0\n" +
			"13	Noise type			A	8	[]			0\n";
		case 390:
			return
			//Type	Name					Category	Number Fields
			"390	Generic Calibration Blockette		Data Record	2.0=9\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 390		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Beginning of calibration time	B	10	BTIME			0\n" +
			"4	Reserved byte			B	1	UBYTE			0\n" +
			"5	Calibration flags		B	1	UBYTE			0\n" +
			"6	Calibration duration		B	4	ULONG			0\n" +
			"7	Calibration signal amplitude	B	4	FLOAT			0\n" +
			"8	Channel with calibration input	A	3	[]			0\n" +
			"9	Reserved byte			B	1	UBYTE			0\n";
		case 395:
			return
			//Type	Name					Category	Number Fields
			"395	Calibration Abort Blockette		Data Record	2.0=4\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 395		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	End of calibration time		B	10	BTIME			0\n" +
			"4	Reserved bytes			B	2	UBYTE			0\n";
		case 400:
			return
			//Type	Name					Category	Number Fields
			"400	Beam Blockette				Data Record	2.0=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 400		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Beam azimuth (degrees)		B	4	FLOAT			0\n" +
			"4	Beam slowness (sec/degree)	B	4	FLOAT			0\n" +
			"5	Beam configuration		B	2	UWORD			0\n" +
			"6	Reserved bytes			B	2	UWORD			0\n";
		case 405:
			return
			//Type	Name					Category	Number Fields
			"405	Beam Delay Blockette			Data Record	2.0=3\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 405		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Array of delay values		B	2	UWORD			0\n";
		case 500:
			return
			//Type	Name					Category	Number Fields
			"500	Timing Blockette			Data Record	2.0=10\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 500		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	VCO correction			B	4	FLOAT			0\n" +
			"4	Time of exception		B	10	BTIME			0\n" +
			"5	Microseconds			B	1	UBYTE			0\n" +
			"6	Reception Quality		B	1	UBYTE			0\n" +
			"7	Exception count			B	4	ULONG			0\n" +
			"8	Exception type			A	16	[]			0\n" +
			"9	Clock model			A	32	[]			0\n" +
			"10	Clock status			A	128	[]			0\n"; 
		case 1000:
			return
			//Type	Name					Category	Number Fields
			"1000	Data Only SEED Blockette		Data Record	2.3=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 1000		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Encoding format			B	1	BYTE			0\n" +
			"4	Word order			B	1	UBYTE			0\n" +
			"5	Data Record Length		B	1	UBYTE			0\n" +
			"6	Reserved			B	1	UBYTE			0\n";
		case 1001:
			return
			//Type	Name					Category	Number Fields
			"1001	Data Extension Blockette		Data Record	2.0=6\n" +
			//Fld	Name				Type	Length	Mask or Flags		Repeat
			"1	Blockette type - 1001		B	2	UWORD			0\n" +
			"2	Next blockette's byte number	B	2	UWORD			0\n" +
			"3	Timing quality			B	1	UBYTE			0\n" +
			"4	Microseconds			B	1	UBYTE			0\n" +
			"5	Reserved			B	1	UBYTE			0\n" +
			"6	Frame Count			B	1	UBYTE			0\n";
			
			// UNKNOWN BLOCKETTE
			
		default:
			throw new SeedException ("Blockette type " + blkType + " not defined");	    
		}
	}
	
	/**
	 * Return description of value at the indicated field number.
	 * Sometimes fields will have special values that have a more descriptive meaning
	 * attached to them...if such an interpretation is present, return the string
	 * describing the meaning of the value provided.
	 * For multiple flags, each interpretation is shown on a separate line,
	 * newline separated.
	 */
	public static String getTranslation(int blkType, int fieldNum, Object value) {
		// check for null value -- to prevent exceptions in toString calls
		if (value == null) return "No Value";
		switch (blkType) {
		case 30:
			if (fieldNum == 5) {
				int val = Integer.parseInt(value.toString());  // for data type assurance
				if (val == 0) {
					return "Integer format fixed interval data";
				}
				if (val == 1) {
					return "Gain ranged fixed interval data";
				}
				if (val == 50) {
					return "Integer differences compression";
				}
				if (val == 80) {
					return "ASCII text with line control (for console logs)";
				}
				if (val == 81) {
					return "Non-ASCII text (for other language character sets)";
				}
			}
			break;
		case 41:
			if (fieldNum == 5) {
				String val = value.toString();
				if (val.equals("A")) {
					return "No Symmetry - all Coefficients are specified";
				}
				if (val.equals("B")) {
					return "Odd number Coefficients with symmetry";
				}
				if (val.equals("C")) {
					return "Even number Coefficients with symmetry";
				}
			}
			if (fieldNum == 8) {
				String val = value.toString();
				if (val.equals("A")) {
					return "No Symmetry - All Coefficients specified";
				}
				if (val.equals("B")) {
					return "Odd - First half of all coefficients and center coefficient specified";
				}
				if (val.equals("C")) {
					return "Even - First half of all coefficients specified";
				}
			}
			break;
                case 42:
			if (fieldNum == 5) {
				String val = value.toString();
				if (val.equals("P")) {
					return "Polynomial";
				}
			}
                        if (fieldNum == 8) {
				String val = value.toString();
				if (val.equals("M")) {
					return "MacLaurin";
				}
			}
			if (fieldNum == 9) {
				String val = value.toString();
				if (val.equals("A")) {
					return "rad/sec";
				}
				if (val.equals("B")) {
					return "Hz";
				}
			}
			break;
		case 43:
			if (fieldNum == 5) {
				String val = value.toString();
				if (val.equals("A")) {
					return "Laplace transform analog response, in rad/sec";
				}
				if (val.equals("B")) {
					return "Analog response, in Hz";
				}
				if (val.equals("C")) {
					return "Composite (currently undefined)";
				}
				if (val.equals("D")) {
					return "Digital (Z-transform)";
				}
			}
			break;
		case 44:
			if (fieldNum == 5) {
				String val = value.toString();
				if (val.equals("A")) {
					return "Laplace transform analog response, in rad/sec";
				}
				if (val.equals("B")) {
					return "Analog response, in Hz";
				}
				if (val.equals("C")) {
					return "Composite (currently undefined)";
				}
				if (val.equals("D")) {
					return "Digital (Z-transform)";
				}
			}
			break;
		case 50:
			if (fieldNum == 11) {
				int val = Integer.parseInt(value.toString());
				if (val == 123) return "VAX, 8086 series";
				if (val == 3210) return "68000 series";
			}
			if (fieldNum == 12) {
				int val = Integer.parseInt(value.toString());
				if (val == 1) return "VAX, 8086 series";
				if (val == 10) return "68000 series";
			}
			if (fieldNum == 15) {
				String val = value.toString();
				if (val.equals("N") || val.equals("n")) return "Effective dates pertain to these data";
				if (val.equals("U") || val.equals("u")) return "Control header updates information previously sent";
			}
			break;
		case 52:
			if (fieldNum == 21) {
				String val = value.toString();  // sanity
				StringBuffer outVal = new StringBuffer();
				if (val.indexOf("T") >= 0) outVal.append("Channel is triggered\n");
				if (val.indexOf("C") >= 0) outVal.append("Channel is recorded continuously\n");
				if (val.indexOf("H") >= 0) outVal.append("State of health data\n");
				if (val.indexOf("G") >= 0) outVal.append("Geophysical data\n");
				if (val.indexOf("W") >= 0) outVal.append("Weather or environmental data\n");
				if (val.indexOf("F") >= 0) outVal.append("Flag information (nominal, not ordinal)\n");
				if (val.indexOf("S") >= 0) outVal.append("Synthesized data\n");
				if (val.indexOf("I") >= 0) outVal.append("Channel is a calibration input\n");
				if (val.indexOf("E") >= 0) outVal.append("Channel is experimental or temporary\n");
				if (val.indexOf("M") >= 0) 
					outVal.append("Maintenance tests are underway on channel; possible abnormal data\n");
				if (val.indexOf("B") >= 0) outVal.append("Data are a beam synthesis\n");
				return outVal.toString();
			}
			break;
		case 53:
			if (fieldNum == 3) {
				String val = value.toString();
				if (val.equals("A")) {
					return "Laplace transform analog response, in rad/sec";
				}
				if (val.equals("B")) {
					return "Analog response, in Hz";
				}
				if (val.equals("C")) {
					return "Composite (currently undefined)";
				}
				if (val.equals("D")) {
					return "Digital (Z-transform)";
				}
			}
			break;
		case 61:
			if (fieldNum == 5) {
				String val = value.toString();
				if (val.equals("A")) {
					return "No Symmetry - all Coefficients are specified";
				}
				if (val.equals("B")) {
					return "Odd number Coefficients with symmetry";
				}
				if (val.equals("C")) {
					return "Even number Coefficients with symmetry";
				}
			}
			if (fieldNum == 8) {
				String val = value.toString();
				if (val.equals("A")) {
					return "No Symmetry - All Coefficients specified";
				}
				if (val.equals("B")) {
					return "Odd - First half of all coefficients and center coefficient specified";
				}
				if (val.equals("C")) {
					return "Even - First half of all coefficients specified";
				}		    
			}
			break;
		case 62:
			if (fieldNum == 7) {
				String val = value.toString();
				if (val.equals("M")) {
					return "MacLaurin";
				}
			}
			if (fieldNum == 8) {
				String val = value.toString();
				if (val.equals("A")) {
					return "rad/sec";
				}
				if (val.equals("B")) {
					return "Hz";
				}
			}
			break;
		case 70:
			if (fieldNum == 3) {
				String val = value.toString();
				if (val.equals("E")) {
					return "Data are event oriented";
				}
				if (val.equals("P")) {
					return "Data are for a given period";
				}
			}
			break;
		case 999:
			if (fieldNum == 2) {
				// note, these FSDH definitions are preliminary, and might change later
				String val = value.toString();  // sanity
				StringBuffer outVal = new StringBuffer();
				if (val.indexOf("D") >= 0) outVal.append("Default data (might be QC'd, might not be)\n");
				if (val.indexOf("R") >= 0) outVal.append("Real-time data (un-QC'd)\n");
				if (val.indexOf("Q") >= 0) outVal.append("Quality controlled data (QC'd)\n");
                                if (val.indexOf("M") >= 0) outVal.append("Merged data (partially QC'd)\n");
				if (val.indexOf("P") >= 0) outVal.append("Primary data source\n");
				if (val.indexOf("S") >= 0) outVal.append("Secondary data source\n");
				return outVal.toString();
			}
			if (fieldNum == 12) {
				int val = Integer.parseInt(value.toString());
				StringBuffer outVal = new StringBuffer();
				if (val > 127) {
					outVal.append("Bit 7 set (undefined)\n");
					val -= 128;
				}
				if (val > 63) {
					outVal.append("Event in progress\n");
					val -= 64;
				}
				if (val > 31) {
					outVal.append("A negative leap second happened during this record (A 59 second minute)\n");
					val -= 32;
				}
				if (val > 15) {
					outVal.append("A positive leap second happened during this record (A 61 second minute)\n");
					val -= 16;
				}
				if (val > 7) {
					outVal.append("End of the event, station detriggers\n");
					val -= 8;
				}
				if (val > 3) {
					outVal.append("Beginning of an event, station trigger\n");
					val -= 4;
				}
				if (val > 1) {
					outVal.append("Time correction in field 16 applied to field 8\n");
					val -= 2;
				}
				if (val > 0) {
					outVal.append("Calibration signals present\n");
				}
				return outVal.toString();
			}
			if (fieldNum == 13) {
				int val = Integer.parseInt(value.toString());
				StringBuffer outVal = new StringBuffer();
				if (val > 127) {
					outVal.append("Bit 7 set (undefined)\n");
					val -= 128;
				}
				if (val > 63) {
					outVal.append("Bit 6 set (undefined)\n");
					val -= 64;
				}
				if (val > 31) {
					outVal.append("Clock locked\n");
					val -= 32;
				}
				if (val > 15) {
					outVal.append("End of time series\n");
					val -= 16;
				}
				if (val > 7) {
					outVal.append("Start of time series\n");
					val -= 8;
				}
				if (val > 3) {
					outVal.append("Short record read (record padded)\n");
					val -= 4;
				}
				if (val > 1) {
					outVal.append("Long record read (possibly no problem)\n");
					val -= 2;
				}
				if (val > 0) {
					outVal.append("Station volume parity error possibly present\n");
				}
				return outVal.toString();
			}
			if (fieldNum == 14) {
				int val = Integer.parseInt(value.toString());
				StringBuffer outVal = new StringBuffer();
				if (val > 127) {
					outVal.append("Time tag is questionable\n");
					val -= 128;
				}
				if (val > 63) {
					outVal.append("A digital filter may be charging\n");
					val -= 64;
				}
				if (val > 31) {
					outVal.append("Telemetry synchronization error\n");
					val -= 32;
				}
				if (val > 15) {
					outVal.append("Missing/padded data present\n");
					val -= 16;
				}
				if (val > 7) {
					outVal.append("Glitches detected\n");
					val -= 8;
				}
				if (val > 3) {
					outVal.append("Spikes detected\n");
					val -= 4;
				}
				if (val > 1) {
					outVal.append("Digitizer clipping detected\n");
					val -= 2;
				}
				if (val > 0) {
					outVal.append("Amplifier saturation detected (station dependent)\n");
				}
				return outVal.toString();
			}
			break;
		case 200:
			if (fieldNum == 6) {
				int val = Integer.parseInt(value.toString());
				StringBuffer outVal = new StringBuffer();
				if (val > 127) {
					outVal.append("Bit 7 set (undefined)\n");
					val -= 128;
				}
				if (val > 63) {
					outVal.append("Bit 6 set (undefined)\n");
					val -= 64;
				}
				if (val > 31) {
					outVal.append("Bit 5 set (undefined)\n");
					val -= 32;
				}
				if (val > 15) {
					outVal.append("Bit 4 set (undefined)\n");
					val -= 16;
				}
				if (val > 7) {
					outVal.append("Bit 3 set (undefined)\n");
					val -= 8;
				}
				if (val > 3) {
					outVal.append("bit 0 is undetermined\n");
					val -= 4;
				}
				if (val > 1) {
					outVal.append("units above are after deconvolution\n");
					val -= 2;
				} else {
					outVal.append("digital counts\n");
				}
				if (val > 0) {
					outVal.append("dilatation wave\n");
				} else {
					outVal.append("compression\n");
				}
				return outVal.toString();
			}
			break;
		case 201:
			if (fieldNum == 6) {
				int val = Integer.parseInt(value.toString());
				if (val > 0) {
					return "dilatation wave";
				} else {
					return "compression";
				}	
			}
			break;
		case 300:
			if (fieldNum == 5) {
				int val = Integer.parseInt(value.toString());
				StringBuffer outVal = new StringBuffer();
				if (val > 127) {
					outVal.append("Bit 7 set (undefined)\n");
					val -= 128;
				}
				if (val > 63) {
					outVal.append("Bit 6 set (undefined)\n");
					val -= 64;
				}
				if (val > 31) {
					outVal.append("Bit 5 set (undefined)\n");
					val -= 32;
				}
				if (val > 15) {
					outVal.append("Bit 4 set (undefined)\n");
					val -= 16;
				}
				if (val > 7) {
					outVal.append("calibration continued from previous record(s)\n");
					val -= 8;
				}
				if (val > 3) {
					outVal.append("calibration was automatic\n");
					val -= 4;
				} else {
					outVal.append("calibration was manual\n");
				}
				if (val > 1) {
					outVal.append("calibration's alternate sign\n");
					val -= 2;
				}
				if (val > 0) {
					outVal.append("first pulse is positive\n");
				}
				return outVal.toString();
			}
			break;
		case 310:
			if (fieldNum == 5) {
				int val = Integer.parseInt(value.toString());
				StringBuffer outVal = new StringBuffer();
				if (val > 127) {
					outVal.append("Bit 7 set (undefined)\n");
					val -= 128;
				}
				if (val > 63) {
					outVal.append("RMS amplitude\n");
					val -= 64;
				}
				if (val > 31) {
					outVal.append("zero-to-peak amplitude\n");
					val -= 32;
				}
				if (val > 15) {
					outVal.append("peak-to-peak amplitude\n");
					val -= 16;
				}
				if (val > 7) {
					outVal.append("calibration continued from previous record(s)\n");
					val -= 8;
				}
				if (val > 3) {
					outVal.append("calibration was automatic\n");
					val -= 4;
				} else {
					outVal.append("calibration was manual\n");
				}
				if (val > 1) {
					outVal.append("Bit 1 set (undefined)\n");
					val -= 2;
				}
				if (val > 0) {
					outVal.append("Bit 0 set (undefined)\n");
				}
				return outVal.toString();
			}
			break;
		case 320:
			if (fieldNum == 5) {
				int val = Integer.parseInt(value.toString());
				StringBuffer outVal = new StringBuffer();
				if (val > 127) {
					outVal.append("Bit 7 set (undefined)\n");
					val -= 128;
				}
				if (val > 63) {
					outVal.append("Bit 6 set (undefined)\n");
					val -= 64;
				}
				if (val > 31) {
					outVal.append("Bit 5 set (undefined)\n");
					val -= 32;
				}
				if (val > 15) {
					outVal.append("random amplitudes\n");
					val -= 16;
				}
				if (val > 7) {
					outVal.append("calibration continued from previous record(s)\n");
					val -= 8;
				}
				if (val > 3) {
					outVal.append("calibration was automatic\n");
					val -= 4;
				} else {
					outVal.append("calibration was manual\n");
				}
				if (val > 1) {
					outVal.append("Bit 1 set (undefined)\n");
					val -= 2;
				}
				if (val > 0) {
					outVal.append("Bit 0 set (undefined)\n");
				}
				return outVal.toString();
			}
			break;
		case 390:
			if (fieldNum == 5) {
				int val = Integer.parseInt(value.toString());
				StringBuffer outVal = new StringBuffer();
				if (val > 127) {
					outVal.append("Bit 7 set (undefined)\n");
					val -= 128;
				}
				if (val > 63) {
					outVal.append("Bit 6 set (undefined)\n");
					val -= 64;
				}
				if (val > 31) {
					outVal.append("Bit 5 set (undefined)\n");
					val -= 32;
				}
				if (val > 15) {
					outVal.append("Bit 4 set (undefined)\n");
					val -= 16;
				}
				if (val > 7) {
					outVal.append("calibration continued from previous record(s)\n");
					val -= 8;
				}
				if (val > 3) {
					outVal.append("calibration was automatic\n");
					val -= 4;
				} else {
					outVal.append("calibration was manual\n");
				}
				if (val > 1) {
					outVal.append("Bit 1 set (undefined)\n");
					val -= 2;
				}
				if (val > 0) {
					outVal.append("Bit 0 set (undefined)\n");
				}
				return outVal.toString();
			}
			break;
		case 1000:
			if (fieldNum == 3) {
				int val = Integer.parseInt(value.toString());
				switch (val) {
				// GENERAL
				case 0:
					return "ASCII text, byte order as specified in field 4";
				case 1:
					return "16 bit integers";
				case 2:
					return "24 bit integers";
				case 3:
					return "32 bit integers";
				case 4:
					return "IEEE floating point";
				case 5:
					return "IEEE double precision floating point";
					
					// FDSN networks
				case 10:
					return "STEIM (1) Compression";
				case 11:
					return "STEIM (2) Compression";
				case 12:
					return "GEOSCOPE Multiplexed Format 24 bit integer";
				case 13:
					return "GEOSCOPE Multiplexed Format 16 bit gain ranged, 3 bit exponent";
				case 14:
					return "GEOSCOPE Multiplexed Format 16 bit gain ranged, 4 bit exponent";
				case 15:
					return "US National Network compression";
				case 16:
					return "CDSN 16 bit gain ranged";
				case 17:
					return "Graefenberg 16 bit gain ranged";
				case 18:
					return "IPG - Strasbourg 16 bit gain ranged";
				case 19:
					return "STEIM (3) Compression";
					
					// OLDER NETWORKS
				case 30:
					return "SRO Format";
				case 31:
					return "HGLP Format";
				case 32:
					return "DWWSSN Gain Ranged Format";
				case 33:
					return "RSTN 16 bit gain ranged";
				default:
					return "Undefined encoding format";
				}
			}
			if (fieldNum == 4) {
				int val = Integer.parseInt(value.toString());
				if (val > 0) {
					return "68000/SPARC word order";
				} else {
					return "VAX/8086 word order";
				}
			}
			break;
		default:
			// it is not an error to list a blkType that is not present here, since not all
			// blockettes will be present in the translation lookup
			return "Value Undefined";   // if no definition was found
		}
		return "Value Undefined";  // sanity return
	}
	
	/**
	 * Get the blockette definition line for the listed blockette type and field.
	 * Field Num of 0 refers to the Blockette definition header.
	 */
	private static String getField(int blkType, int fieldNum) throws SeedException {
		String blkDefString = getBlocketteDefinition(blkType); // get the blockette definition
		int begin = 0;
		int end = 0;
		// count through the lines up to fieldNum
		for (int i = 0; i < fieldNum; i++) {
			end = blkDefString.indexOf("\n",begin);  // carriage-return separated
			if (end == -1) { // no more delimiters
				throw new SeedException("field number " + fieldNum +
						"not defined for blockette " + blkType);
			}
			begin = end + 1;
		}
		end = blkDefString.indexOf("\n",begin);
		if (end == -1) {  // no more delimiters
			return blkDefString.substring(begin);      // return last token
		} else {
			return blkDefString.substring(begin,end);  // return next token
		}
	}
	
	/**
	 * Get the blockette definition line and column for the listed blockette type
	 * and field.  Columns start at 1.
	 */
	private static String getField(int blkType, int fieldNum, int colNum) throws SeedException {
		String defLine = getField(blkType, fieldNum);  // get definition line
		//System.err.println("DEBUG: getField(" + blkType + "," +
		//	fieldNum + "," + colNum + "):defLine=" + defLine);
		int begin = 0;
		int end = 0;
		int prevBegin = 0;
		for (int i = 1; i <= colNum; i++) {  // count through the columns up through colNum
			// next column...
			prevBegin = end;
			while (prevBegin == end) {  // pass through consecutive tabs
				end = defLine.indexOf("\t",begin);  // tab-separated fields
				//System.err.println("DEBUG: col=" + i +
				//	", begin=" + begin + ", end=" + end);
				prevBegin = begin;  // remember where we were
				begin = end + 1;  // leapfrog begin just past end
			}
		}
		// we are at the right column, get the value here
		begin = prevBegin;  // recover pre-increment begin index for this column
		//System.err.println("DEBUG: final begin=" + begin + ", end=" + end);
		if (end == -1) {  // no more delimiters
			return defLine.substring(begin);      // return last token
		} else {
			return defLine.substring(begin,end);  // return next token
		}
	}
	
	
	// test case main method
	
	/**
	 * Test case method.
	 */
	public static void main(String[] args) {
		// try to create a blockette object and have that object print it's
		// contents
		try {
			int blkType = 10;
			String blkStr = "010009502.3121992,001,00:00:00.0000~1992,002,00:00:00.0000~1993,029~IRIS_DMC~Data for 1992,001~";
			byte[] blkArr = blkStr.getBytes();
			Blockette myBlockette = createBlockette(blkArr,false,false);
			System.out.println("I am blockette type " + myBlockette.getType());
			System.out.println("In field 4, the logical record length exponent is " + myBlockette.getFieldVal(4).toString());
			System.out.println("In field 6, the end time is " + myBlockette.getFieldVal(6).toString());
			System.out.println("In field 9, the label is " + myBlockette.getFieldVal(9).toString());
			
			blkType = 50;
			blkStr = "0500098ANMO +34.946200-106.456700+1740.00006001Albuquerque, New Mexico, USA~0013210101989,241~~NIU";
			blkArr = blkStr.getBytes();
			myBlockette = createBlockette(blkArr,false,false);
			System.out.println("I am blockette type " + myBlockette.getType());
			System.out.println("The station name is " + myBlockette.getFieldVal(3).toString());
			System.out.println("The longitude is " + myBlockette.getFieldVal(5).toString());
			System.out.println("The elevation is " + myBlockette.getFieldVal(6).toString());
			System.out.println("The start effective date is " + myBlockette.getFieldVal(13).toString());
			System.out.println("The update flag is " + myBlockette.getFieldVal(15).toString());
			System.out.println("Which translates to " + myBlockette.translate(15));
			
			blkType = 52;
			blkStr = "0520119  BHE0000004~001002+34.946200-106.456700+1740.0100.0090.0+00.0000112 2.000E+01 2.000E-030000CG~1991,042,20:48~~N";
			blkArr = blkStr.getBytes();
			Blockette chanBlockette = createBlockette(blkArr,false,false);
			int whereIndex = myBlockette.addChildBlockette(chanBlockette);
			System.out.println("Added a child blockette.  myBlockette contains " + myBlockette.numberofChildBlockettes() + " blockettes.");
			Blockette grabChild = (Blockette) myBlockette.getChildBlockette(whereIndex);
			System.out.println("I am blockette type " + grabChild.getType());
			System.out.println("My definition is as follows:\n" + grabChild.getDefinition());
			System.out.println("The channel name is " + grabChild.getFieldVal(4).toString());
			System.out.println("using the shorter toString() call, the value is " + grabChild.toString(4));
			System.out.println("The " + grabChild.getFieldName(8) + " is " + grabChild.getFieldVal(8).toString());
			System.out.println("using the shorter toString() call, the value is " + grabChild.toString(8));
			System.out.println("The " + grabChild.getFieldName(18) + " is " + grabChild.getFieldVal(18).toString());
			System.out.println("using the shorter toString() call, the value is " + grabChild.toString(18));
			System.out.println("The " + grabChild.getFieldName(19) + " is " + grabChild.getFieldVal(19).toString());
			System.out.println("using the shorter toString() call, the value is " + grabChild.toString(19));
			System.out.println("The " + grabChild.getFieldName(21) + " is " + grabChild.toString(21));
			System.out.println("which translated means: " + grabChild.translate(21));
			
			blkType = 53;
			blkStr = "0530382B 1007008 7.87395E+00 5.00000E-02  3 0.00000E+00 0.00000E+00 0.00000E+00 0.00000E+00 0.00000E+00 0.00000E+00 0.00000E+00 0.00000E+00-1.27000E+01 0.00000E+00 0.00000E+00 0.00000E+00  4-1.96418E-03 1.96418E-03 0.00000E+00 0.00000E+00-1.96418E-03-1.96418E-03 0.00000E+00 0.00000E+00-6.23500E+00 7.81823E+00 0.00000E+00 0.00000E+00-6.23500E+00-7.81823E+00 0.00000E+00 0.00000E+00";
			blkArr = blkStr.getBytes();
			chanBlockette = createBlockette(blkArr,false,false);
			whereIndex = myBlockette.addChildBlockette(chanBlockette);
			System.out.println("Added a child blockette.  myBlockette contains " + myBlockette.numberofChildBlockettes() + " blockettes.");
			grabChild = (Blockette) myBlockette.getChildBlockette(whereIndex);
			System.out.println("I am blockette type " + grabChild.getType());
			System.out.println("The " + grabChild.getFieldName(14) + " is " + grabChild.getFieldVal(14).toString());
			System.out.println("The " + grabChild.getFieldName(15) + " at index 2 is " + grabChild.getFieldVal(15,2).toString());
			System.out.println("which I can also print with the toString() method as " + grabChild.toString(15,2));
			System.out.println("The " + grabChild.getFieldName(16) + " at index 0 is " + grabChild.getFieldVal(16,0).toString());
			System.out.println("which I can also print with the toString() method as " + grabChild.toString(16,0));
			System.out.println("Now I want to add an additional coefficient set to the complex zeros...");
			Vector zerosVec = new Vector(4);
			zerosVec.add(new Float(1.732E-2));
			zerosVec.add(new Float(-1.732E-2));
			zerosVec.add(new Float(0));
			zerosVec.add(new Float(0));
			grabChild.addFieldGrp(10,zerosVec);
			System.out.println("...and then I want to read back all of the values");
			System.out.println("there are " + grabChild.toString(9) + " field groups here");
			int numGrps = Integer.parseInt(grabChild.toString(9));
			for (int i = 0; i < numGrps; i++) {
				Vector fieldVec = grabChild.getFieldGrp(10,i);
				System.out.print ("" + i + ": ");
				for (int j=0;j < fieldVec.size();j++) {
					System.out.print ("" + BlocketteFactory.formatDecimal(blkType,10+j,fieldVec.get(j).toString()) + ",");
				}
				System.out.println ("");
			}
			
			
			// FSDH - blk type 999
			blkStr = "999|D|1900|ANMO|  |BHZ|IU|1998,001|3849|20|1|68|12|144|0|2829|256|0";
			Blockette fsdhBlockette = createBlockette(blkStr);
			System.out.println("\n\nI am blockette type " + fsdhBlockette.getType());
			System.out.println("which means I am called a " + fsdhBlockette.getName());
			for (int i=2;i<19;i++) {
				System.out.println("The " + fsdhBlockette.getFieldName(i) + " is " + fsdhBlockette.toString(i));
				System.out.println("which translates to: " + fsdhBlockette.translate(i));
			}
			
			// wierd special case, Blockette 60
			// binary input
			blkStr = "06000650402030098007600430301003404040033004400550066050200030002";
			blkArr = blkStr.getBytes();
			Blockette blockette60 = createBlockette(blkArr,false,false);
			System.out.println("\n\nI am blockette type " + blockette60.getType());
			System.out.println("which means I am called a " + blockette60.getName());
			for (int i=2;i<=blockette60.getNumFields();i++) {
				System.out.println("The " + blockette60.getFieldName(i) + " is " + blockette60.toString(i));
				System.out.println("which translates to: " + blockette60.translate(i));
				if (i == 3) {
					int numStages = Integer.parseInt(blockette60.toString(i));
					for (int j = 0; j < numStages; j++) {
						Vector stageVec = blockette60.getFieldGrp(4,j);
						System.out.print ("Stage " + j + ": ");
						for (int k=0;k < stageVec.size();k++) {
							System.out.print ("" + BlocketteFactory.formatDecimal(60,4+k,stageVec.get(k).toString()) + ",");
						}
						System.out.println ("");
					}
					break;
				}
			}
			
		} catch (SeedException e) {
			System.out.println("Caught exception: " + e);
		}
	}
	
	// this is the default SEED version, barring other assigned value
	private static final float defaultSEEDVersion = 2.4F;
	// static fields for lookup optimization
	private static final int[] blocketteTypes = {
			5,8,10,11,12,30,31,32,33,34,35,41,42,43,44,45,46,47,48,
			50,51,52,53,54,55,56,57,58,59,60,61,62,70,71,72,73,74,
			999,100,200,201,300,310,320,390,395,400,405,500,1000,1001
	};
        // integer pairs that associate integer type with a field number -- for getting a response stage number
        private static final int[] stageNumberFields = {
            53,4,
            54,4,
            55,3,
            56,3,
            57,3,
            58,3,
            60,4,
            61,3,
            62,4
        };
	private static final int highestBlocketteType = 1001;  // make sure this equals the highest type number!
	private static final int numberOfBlocketteTypes = blocketteTypes.length;
	private static boolean optimized = false;  // flags that optimization has been activated
	//
	// look at array index equal to blockette type, returns index number for the
	// two dimensional field arrays -- this prevents linear searches for blockette
	// info, which is CPU costly.
	private static int[] blocketteTypeMap = new int[highestBlocketteType+1];
	//
	// Blockette definition tables optimized to array storage
	private static String[][] arrayFieldName = new String[numberOfBlocketteTypes][];
	private static char[][] arrayFieldType = new char[numberOfBlocketteTypes][];
	private static String[][] arrayFieldLength = new String[numberOfBlocketteTypes][];
	private static String[][] arrayFieldMask = new String[numberOfBlocketteTypes][];
	private static int[][] arrayFieldRepeat = new int[numberOfBlocketteTypes][];
	private static String[] arrayBlkName = new String[numberOfBlocketteTypes];
	private static String[] arrayBlkCategory = new String[numberOfBlocketteTypes];
	private static String[] arrayNumFields = new String[numberOfBlocketteTypes];
	private static final byte[] tildeArray = {'~'};
	
	// static intialization block -- runs when class first loaded
	static {
		try {
			for (int i = 0; i < numberOfBlocketteTypes; i++) {
				int bType = blocketteTypes[i];  // blockette type
				blocketteTypeMap[bType] = i;    // provide mapping of blockette type to this index
				// populate blockette arrays
				arrayBlkName[i] = getName(bType);
				arrayBlkCategory[i] = getCategory(bType);
				arrayNumFields[i] = getNumFields(bType);
				// populate blockette field arrays
				int numFields = getNumFields(bType,99.9F);  // get max number of possible fields
				arrayFieldName[i] = new String[numFields];
				arrayFieldType[i] = new char[numFields];
				arrayFieldLength[i] = new String[numFields];
				arrayFieldMask[i] = new String[numFields];
				arrayFieldRepeat[i] = new int[numFields];
				for (int j = 0; j < numFields; j++) {  // field number minus 1
					arrayFieldName[i][j] = getFieldName(bType,j+1);
					arrayFieldType[i][j] = getFieldType(bType,j+1).charAt(0);
					arrayFieldLength[i][j] = getFieldLength(bType,j+1);
					arrayFieldMask[i][j] = getFieldMask(bType,j+1);
					arrayFieldRepeat[i][j] = getFieldRepeat(bType,j+1);
				}
			}
		} catch (Exception e) {
			// report and exit upon encountering an exception
			System.out.println("Caught exception: " + e);
			((Throwable) e).printStackTrace();
			System.exit (1);
		}
		optimized = true;  // set optimization flag to true
	}
	
}
