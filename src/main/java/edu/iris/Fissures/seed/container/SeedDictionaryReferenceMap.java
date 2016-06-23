package edu.iris.Fissures.seed.container;

/**
 * SEED Blockette Dictionary Reference Mapper.
 * This class is a collection of static methods for aiding the cross-reference
 * of blockettes to their dictionary lookup counterparts, which we will term
 * Source and Destination Blockettes, respectively.  This serves as a map to say
 * that a certain blockette type and field number will reference a certain blockette
 * type and field number.
 * @author Robert Casey, IRIS DMC
 * @version 6/21/2004
 */
public class SeedDictionaryReferenceMap {

	/**
	 * Return array of Dictionary Source Field numbers.
	 * For a given blockette type, return an array of field numbers that are used for
	 * dictionary blockette reference...these will be termed as Source Fields.
	 * Returns null if there are no dictionary fields for this blockette type.
	 */
	public static int[] lookupSourceFld(int blkType) {
		int[] result = getRefMap(blkType,0,0);
		// generate a new array with length equal to the number of unique,
		// meaningful result returns.
		return returnUniqueList(result);
	}

	/**
	 * Return array of Dictionary Destination Blockettes.
	 * For a given blockette type and field number, return an integer array representing
	 * the dictionary blockette types referenced by that blockette type and field.
	 * These are referred to as Destination Blockettes.
	 */
	public static int[] lookupDestBlk(int blkType, int fieldNum) {
		int[] result = getRefMap(blkType,fieldNum,0);
		return returnUniqueList(result);
	}

	/**
	 * Return Dictionary Destination Field.
	 * For a given blockette type, return a single integer representing the field number
	 * that contains the dictionary reference number referenced by Source Fields.
	 * This will be termed as a Destination Field.  The Source references the Destination.
	 */
	public static int lookupDestFld(int blkType) {
		int [] result = getRefMap(0,0,blkType);
		return result[0];
	}



	/**
	 * Return a selected portion of the Dictionary Reference Map as an integer
	 * array.
	 * This method contains the actual reference table and returns
	 * an integer array of the appropriate field values, depending
	 * on the mode of the call.  The modes are as follows:
	 * <pre>
	 *          srcBlk      srcFld      destBlk
	 * Mode 1:    >0           0           0    -- return list of source lookup fields
	 * Mode 2:    >0          >0           0    -- return list of destination blockettes
	 * Mode 3:     0           0          >0    -- return destination lookup field
	 * </pre>
	 */
	private static int[] getRefMap (int srcBlk, int srcFld, int destBlk) {
		int [][] refMap = {   // this is the reference map
			// the field format of each line is:
			// mapSrcBlk   mapSrcFld   mapDestBlk   mapDestFld
			//     0           1            2            3
			{31,6,34,3},
			{41,6,34,3},
			{41,7,34,3},
                        {42,6,34,3},
			{42,7,34,3},
			{43,6,34,3},
			{43,7,34,3},
			{44,6,34,3},
			{44,7,34,3},
			{45,5,34,3},
			{45,6,34,3},
			{46,5,34,3},
			{46,6,34,3},
			{50,10,33,3},
			{51,5,31,3},
			{52,6,33,3},
			{52,8,34,3},
			{52,9,34,3},
			{52,16,30,4},
			{53,5,34,3},
			{53,6,34,3},
			{54,5,34,3},
			{54,6,34,3},
			{55,4,34,3},
			{55,5,34,3},
			{56,4,34,3},
			{56,5,34,3},
			{59,5,31,3},
			{60,6,41,3},
                        {60,6,42,3},
			{60,6,43,3},
			{60,6,44,3},
			{60,6,45,3},
			{60,6,46,3},
			{60,6,47,3},
			{60,6,48,3},
			{61,6,34,3},
			{61,7,34,3},
			{62,5,34,3},
			{62,6,34,3},
			{71,4,32,3},
			{71,11,32,3},
			{72,11,32,3},
			{400,5,35,3}
		};
		//int mapFields = 4;          // this is the number of fields per map line
		int mapLines = refMap.length;  // this is the number of lines in the map array
		int outputIndex = 0;  // index for our output array
		int [] outputArr = new int[mapLines];    // the output array, max output would be number of map lines
		outputArr[0] = 0;   // zero-initialize the first array element
		//
		// loop over each map line
		for (int i = 0; i < mapLines; i++) {
			if (srcBlk > 0) { 
				if (refMap[i][0] == srcBlk) {   // check that srcBlk matches
					if (srcFld > 0) {
						if (refMap[i][1] == srcFld) {  // check that srcFld matches
							// Mode 2
							outputArr[outputIndex++] = refMap[i][2];  // add dest blockette to output array
						}
					} else {
						// Mode 1
						outputArr[outputIndex++] = refMap[i][1];  // add source field to output array
					}		    
					outputArr[outputIndex] = 0;  // zero-initialize next array element
				}
			} else if (destBlk > 0 && refMap[i][2] == destBlk) {   // check to see if destBlk matches
				outputArr[0] = refMap[i][3];  // add dest field to output array
				outputArr[1] = 0;  // for sanity, make sure second element is 0, there is only one return value
				break;             // shortcut out of loop	
			}
			continue;   // next map line
		}
		return outputArr;   // return the integer array
	}

	/**
	 * Return an array of unique integer values from provided integer array.
	 * Return values are not sorted and do not need to be sorted on input.
	 */
	private static int[] returnUniqueList(int[] listArr) {
		int writeIndex = 0;
		int i = 0, j = 0;
		for (i = 0; i < listArr.length && listArr[i] > 0; i++) {  // left-justify all unique values
			for (j = 0; j < i; j++) {
				if (listArr[i] == listArr[j]) break;  // we have found a twin...throw this one out
			}
			if (j == i) {  // no match was found, which means it is unique...pack it in
				if (writeIndex != i) {
					listArr[writeIndex] = listArr[i];  // i will be ahead or equal to writeIndex
				}
				writeIndex++;  // shift the write index right
			}
		}
		if (writeIndex > 0) {
			int [] newInt = new int[writeIndex];
			System.arraycopy(listArr,0,newInt,0,writeIndex);
			return newInt;
		} else {
			return null;
		}
	}

	/**
	 * Test method for this class.
	 */
	public static void main(String args[]) {
		for (int blkType=1; blkType<80; blkType++) {
			int[] srcFld = lookupSourceFld(blkType);
			for (int i = 0; srcFld != null && i < srcFld.length; i++) {
				int[] destBlk = lookupDestBlk(blkType, srcFld[i]);
				for (int j = 0; destBlk != null && j < destBlk.length; j++) {
					int destFld = lookupDestFld(destBlk[j]);
					System.out.println("Test lookup: Blockette " + blkType + ", Field " + srcFld[i] + " ---> Blockette " + 
							destBlk[j] + ", Field " + destFld);
				}
			}
		}
	}


}  // SeedDictionaryReferenceMap
