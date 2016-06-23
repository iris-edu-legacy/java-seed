package edu.iris.Fissures.seed.builder;

import java.util.*;
import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.exception.*;

/**
 * Concrete builder class for SEED Objects, specifically for creating memory
 * mapped references to the imported file.  Registers itself to a concrete
 * Import Director.  Creates and writes to a SeedVolumeMMAPContainer.
 * @author Robert Casey, IRIS DMC
 * @version 1/15/2010
 */

public class SeedMMAPImportBuilder extends ObjectBuilder {
	
	/**
	 * Create a SEED object builder.
	 */
	public SeedMMAPImportBuilder () {
		buildContainer = (ObjectContainer) new SeedVolumeMMAPContainer();   // instantiate an object container when we start up
		builderType = "SEED";    // we are builder type 'SEED'
	}
	
	/**
	 * Create a SEED Object Builder with reference to the indicated journal backing file
	 */
	public SeedMMAPImportBuilder(String journalFile) throws BuilderException {
		try {
			buildContainer = (ObjectContainer) new SeedVolumeMMAPContainer(journalFile);
			builderType = "SEED";    // we are builder type 'SEED'
		} catch (Exception e) {
			throw new BuilderException ("Exception encountered: " + e);
		}
	}
	
	// public methods
	
	/**
	 * Construct a Blockette object by reading from the start of the indicated
	 * byte array.
         * Result gets assigned to currentObject var.
         * Return the number of bytes processed/read for blockette.
	 */
	public int build (byte[] nextRecord) throws Exception {
		if (nextRecord.length == 0)
			throw new BuilderException("zero length record data in build() call");
		byte[] currentRecord = nextRecord;  // use a separate pointer to nextRecord as the default
                //System.err.println("DEBUG: builder.build() next record...");
		int numBytesRead = 0;
		Blockette newBlockette = null;
                Blockette currentBlockette = null;
                boolean largeCoeffMerge = false;
		// based on record type, which is externally set by the Director, build appropriately...
		switch (recordType) {
		case 'V':
		case 'A':
		case 'S':
		case 'T':
			// control header record

                        //System.err.println("DEBUG: ++ continuationFlag = " + continuationFlag + ", largeCoeffFlag = " + largeCoeffFlag);

			//
			// check to see if this is a continuation record, continuation flag
			// is externally set by Director
			if (prevRecord != null && continuationFlag) {
				// this is a continuation...append this new data to the end of prevRecord's data
				// and build the blockette
				currentRecord = new byte[prevRecord.length + nextRecord.length];
				System.arraycopy(prevRecord,0,currentRecord,0,prevRecord.length);
				System.arraycopy(nextRecord,0,currentRecord,prevRecord.length,nextRecord.length);
			}
			if (prevBlockette != null && !largeCoeffFlag) {  // largeCoeffFlag set by director
				// object reuse if recyclable object is present.
                                // initialize blockette with the Factory method using currentRecord as data stream
				prevBlockette.initialize(currentRecord,false,false,seedVersion);
				currentBlockette = prevBlockette;
			} else {
				// build a blockette with the Factory method using currentRecord as data stream
				newBlockette = BlocketteFactory.createBlockette(currentRecord, false, false, seedVersion);

                                // if we are trying to mash largeCoefficient sets together, we need to put two complete blockettes
                                // together, mainly appending the coefficient fields and getting the counts reset.
                                // prevBlockette still holds our blockette.
                                // we want all changes to append to prevBlockette until the very end, where we switch
                                // to the currentBlockette handle.
                                if (!newBlockette.isIncomplete() && largeCoeffFlag && prevBlockette != null && prevBlockette.isResponseBlockette()) {
                                    int numFields = prevBlockette.getNumFields();
                                    int repeatField = 0;
                                    int repeatCount = 0;
                                    int skip = 0;
                                    Vector groupVec = null;
                                    //System.err.println("DEBUG: builder -- merging response blockettes...");
                                    for (int field = 1; field < numFields; field++) {
                                        if (--skip > 0) {  // pre-decrement
                                            continue;    // skip past this field, it's part of an already transcribed group
                                        }
                                        repeatField = prevBlockette.getFieldRepeat(field);
                                        if (repeatField == 0) {  // stop at the first repeating field
                                            repeatCount = 0;
                                            continue;
                                        }
                                        // get the new repeat count value at the same field
                                        repeatCount = (Integer) newBlockette.getFieldVal(repeatField);
                                        // get each new repeating value and add to the prevBlockette
                                        for (int i = 0; i < repeatCount; i++) {  // for each group index
                                            groupVec = (Vector) newBlockette.getFieldGrp(field,i);
                                            if (groupVec != null && groupVec.size() > 0) {
                                                skip = groupVec.size(); // set up skip value to allow us to advance to the field beyond this group
                                                prevBlockette.addFieldGrp(field,groupVec);
                                            }
                                        }
                                    }
                                    largeCoeffMerge = true;
                                    // have currentBlockette handle to point to the merged data in prevBlockette
                                    // newBlockette is still just the blockette we just created from the data record (non merged)
                                    currentBlockette = prevBlockette;
                                } else {
                                    // else just point to the blockette that came from the BlocketteFactory
                                    currentBlockette = newBlockette;
                                }
			}
                        //
			if (currentBlockette == null) throw new BuilderException("Blockette Factory returned a null value");
			// now let's check to see if this is a 'versioning' volume blockette
			int blocketteNumber = currentBlockette.getType();
			if (blocketteNumber == 5 || blocketteNumber == 8 || blocketteNumber == 10) { // known versioning blockettes
				// so far all of the version specifiers are in field 3
				// set our builder's version state to be the value contained in this blockette.
				seedVersion = Float.parseFloat(currentBlockette.toString(3));
				// because blockettes 8 and 10 have the peculiar nature of being different sizes depending on version,
				// we must build the blockette again in order for some cases to register as a complete build.
				// just reuse the blockette we already have
				currentBlockette.initialize(currentRecord, false, false, seedVersion);
				seedVolumeControlHeaderFound = true;  // signals that we are reading from a full SEED file
			}
                        // Get the number of bytes read and processed into a blockette.
			// Check to see if this is a partial blockette, which means that another build call will
			// be needed to complete it.
			if (currentBlockette.isIncomplete()) {    // we have a partial blockette, save the record section for next read
				prevRecord = new byte[currentRecord.length];    // size the holder to the length of the current record
				System.arraycopy(currentRecord,0,prevRecord,0,currentRecord.length);  // save currentRecord to prevRecord for loop memory
				numBytesRead = currentRecord.length;  // for sanity, assure that the number of bytes read is set to the length of the record 
				currentBlockette = null;          // we will have no current object with a partial blockette
			} else {
                                // we have a complete blockette
                                if (largeCoeffMerge) {  // if we merged two blockettes for a large coefficient representation...
                                    // return the latest blockette length, not the fully merged blockette length
                                    numBytesRead = newBlockette.getNumBytes();
                                } else {
                                    // then return number of bytes in the current, complete blockette
                                    numBytesRead = currentBlockette.getNumBytes();
                                }
                                if (prevRecord != null) {   // did we append to a previous incomplete record?
                                    // yes, then subtract previous record's bytes since we already reported it
                                    numBytesRead -= prevRecord.length;
				}
				prevRecord = null;  // assert a null previous record
			}
                        if (currentBlockette != null) {
                                //System.err.println("DEBUG: builder - currentBlockette: " + currentBlockette.toString());
                                prevBlockette = currentBlockette;  // save for object reuse and loop memory of prior blockette
                        } else {
                                //System.err.println("DEBUG: builder - currentBlockette is **Incomplete**");
                        }
			break;
		case 'D':
		case 'R':
		case 'Q':
                case 'M':
			// data record
			//
			prevRecord = null;
			// if this is the beginning of a data record, then we must be looking at an FSDH.
			// we have to take the incoming data and modify it for the BlocketteFactory before passing it on.
			// Create a blockette 999 and determine the word order of the binary data.
			if (recordBeginFlag) {
				dataBlocketteOffset = 0;
				if (currentRecord.length < 40) {   // this is the length of the FSDH, minus record ID block
					throw new BuilderException("data record is too short (" + currentRecord.length + ")");
				}
				byte[] timeArr = new byte[10];
				System.arraycopy(currentRecord,12,timeArr,0,10);  // pull out binary time entry in the FSDH
				Btime bTime = new Btime(timeArr);   // feed the array to Btime structure and test for byte swap flag
				swapFlag = bTime.getSwapFlag();     // determine the swapFlag value from the Btime object
				// now start to generate a Blockette 999
				byte[] blocketteNumByte = new byte[2];
				if (swapFlag) {
					// binary for 999 in 8086 word order
					blocketteNumByte[0] = (byte) 231;
					blocketteNumByte[1] = (byte) 3;
				} else {
					// binary for 999 in 68000 word order
					blocketteNumByte[0] = (byte) 3;
					blocketteNumByte[1] = (byte) 231;
				}
				byte[] dQFlags = new byte[8];    // data quality flags -- field is size 8
				dQFlags[0] = (byte) recordType;         // record type
				if (dQFlags[1] == '*') {         // continuation flag
					continuationFlag = true;
				} else {
					continuationFlag = false;
				}
				for (int i = 2; i < 8; i++) dQFlags[i] = ' ';  // space padded
				Btime createTime = new Btime();        // get current time for the FSDH creation time
				// transcribe synthesis to currentRecord array	
				currentRecord = new byte[nextRecord.length + 20];  // make new array for synthesis -- affects offset by 12 (-8 + 20)
				System.arraycopy(blocketteNumByte,0,currentRecord,0,2);	
				System.arraycopy(dQFlags,0,currentRecord,2,8);
				System.arraycopy(createTime.getByteTime(),0,currentRecord,10,10);
				// append nextRecord which starts at field 4 of FSDH
				System.arraycopy(nextRecord,0,currentRecord,20,nextRecord.length);
				recordBeginFlag = false;    // toggle this flag off while we look forward to later blockettes in this record
			}
			if (prevBlockette != null) {
				// object reuse
				prevBlockette.initialize(currentRecord,swapFlag,true,seedVersion);
				newBlockette = prevBlockette;
			} else {
				// build a blockette with the Factory method using currentRecord as data stream.
				newBlockette = BlocketteFactory.createBlockette(currentRecord, swapFlag, true, seedVersion);
			}
			if (newBlockette == null) throw new BuilderException("Blockette Factory returned a null value");
			// if this is the FSDH, then the number of bytes consumed will 
			// be equal to the byte number of the first data blockette, minus 8, or in the
			// case of the first blockette pointer being 0, the number of bytes will be the
			// length of the entire record, to negate any further reading
			if (newBlockette.getType() == 999) {  // we are an FSDH blockette
				dataBlocketteOffset = Integer.parseInt(newBlockette.toString(18));  // get the offset of the first data blockette
				numBytesRead = dataBlocketteOffset - 8;  // compensate for 8 bytes already accounted for
				if (dataBlocketteOffset == 0) numBytesRead = currentRecord.length;  // no data blockettes, shift offset to end of record 
				// now we will fetch the waveform data for this FSDH
				int waveformOffset = Integer.parseInt(newBlockette.toString(17));
				if (waveformOffset > 48) {
					int numSamples = Integer.parseInt(newBlockette.toString(9));
					// encoding is currently Unknown, but can be modified later at the application level.
					// waveformOffset for currentRecord adjusted by -8 and + 20 which is +12.
					Waveform newWaveform = new Waveform(currentRecord,waveformOffset+12,numSamples,"UNKNOWN",swapFlag);
					newBlockette.attachWaveform(newWaveform);   // attach waveform to the FSDH
				}
			} else {
				// we are a data blockette.  find the start of the next blockette.
				int nextBlockette = Integer.parseInt(newBlockette.toString(2));
				if (nextBlockette > 0) {
					numBytesRead = nextBlockette - dataBlocketteOffset; // there is another blockette for the next pass
					if (numBytesRead <= 0) throw new BuilderException("regressive data blockette offset " + nextBlockette +
							" at byte offset " + dataBlocketteOffset);
					dataBlocketteOffset = nextBlockette;  // bump the offset up to where we have just read
				} else {
					numBytesRead = currentRecord.length;  // if no other blockettes, cancel out the rest of the record
				}
			}
			currentBlockette = newBlockette;
			break;
		default:
			throw new BuilderException("Unable to identify recordType" + recordType);
		}
		// check the current blockette against the BuilderFilter
		//filterBlockette();
		// finally, return the number of bytes consumed by this build method
		//System.err.println("DEBUG: numBytesRead: " + numBytesRead);

        currentObject = currentBlockette;   // currentObject is what gets written in store()

		return numBytesRead;
	}
	
	
	/**
	 * Construct a Blockette object from the delimited String.  Only accepts standard
	 * delimiters '|' and '^'.  Return the length in bytes of the processed
	 * String as a confirmation of success.  Return -1 on failure.
	 */
	public int build (String blocketteSpec) throws Exception {
		if (blocketteSpec == null)
			throw new BuilderException("blockette specification string in build() call is null");
		int numBytesRead = blocketteSpec.length();
		if (numBytesRead == 0)
			throw new BuilderException("zero length blockette specification string in build() call");
		// create new blockette from specification string
		currentObject = BlocketteFactory.createBlockette(blocketteSpec);
		if (currentObject == null)
			throw new BuilderException("Blockette Factory returned a null value");
		// check the current blockette against the BuilderFilter
		//filterBlockette();
		return numBytesRead;
	}
	
	
	/**
	 * Store created SEED object to the SEED MMAP object container.
	 */
	public int store() throws Exception {
		// because of filtering that may be applied during the build process, it is possible
		// that we were passed a null currentObject...this is perfectly normal.
		if (currentObject == null) return -1;
		//
                if (largeCoeffFlag) {
                    // if we are packing two blockettes into one, then our currentObject is meant to replace the
                    // prior one just stored
                    //System.err.println("DEBUG: builder.store() - largeCoeffFlag detected - updateLatest() called");
                    ((SeedVolumeMMAPContainer) buildContainer).updateLatest(currentObject.toString(),true);
                } else {
                    // the default behavior -- add the blockette to the container
                    ((SeedVolumeMMAPContainer) buildContainer).addData(currentObject.toString());
                }
		// REC2010 turned off //prevBlockette = null;              // make sure there is no object reuse for this instance
		currentObject = null;                 // blank out the current object holder
		return 1;
	}
	
	/**
	 * Reset the builder to its initial state.  Generally used during volume transitions.
	 */
	public void reset() {
		currentObject = null;
		prevRecord = null;
		seedVersion = defaultSeedVersion;
		swapFlag = false;
		seedVolumeControlHeaderFound = false;
                prevBlockette = null;
    }
	
	
	// private methods
	
	// Now that an object has been built, we will decide here whether to keep it,
	// provided that:
	// a) we have a blockette attached to currentObject
	// b) we have registered BuilderFilters attached
	// return true if the blockette passes the filter, false if rejected by the
	// filter.  The globally visible blockette is nulled out on failure
	// by this method.
	private boolean filterBlockette() throws Exception {
//		int vecSize = buildFilterVector.size();
//		BuilderFilter buildFilter;
//		boolean match = false;  // flag whether we have a match during this loop
//		if (currentObject != null && vecSize != 0) {
//			for (int i = 0; i < vecSize; i++) {  // for each filter in the vector
//				buildFilter = (BuilderFilter) buildFilterVector.get(i);
//				if ((match = buildFilter.qualify(currentObject)) == true) break;  // found a match...quit loop
//			}
//			// another condition to check for is if a dominant ranking (parent)
//			// blockette was filtered out
//			// before this blockette, then also treat this blockette as rejected,
//			// since it is a child of the rejected blockette.
//			int filterRank = ((SeedObjectContainer) buildContainer).getFiltered();  // get indicator of filtered parent
//			int blkRank = SeedBlocketteRankMap.getRank((Blockette)currentObject);   // get rank of current blockette
//			if (filterRank > 0 && blkRank >= filterRank) match = false;  // if a parent was rejected, then mark this as rejected as well
//			if (match == false) {  // if we don't have a match...
//				((SeedObjectContainer) buildContainer).setFiltered((Blockette) currentObject); // tell container this is being filtered out
//				//System.out.println("DEBUG: rejecting blockette " + currentObject.toString());
//				prevBlockette = (Blockette) currentObject;  // save for object reuse
//				currentObject = null;              // deactivate this Blockette
//			} else {
//				// we have a match, so make sure that the Filtered state of the container is
//				// set to a dormant (zero) value.
//				if (filterRank > 0) ((SeedObjectContainer) buildContainer).setFiltered(0);
//			}
//		}
//		return match;
        return false;  // return false until we implement this properly
	}

	
	// instance variables
	
	private byte[] prevRecord;      // holding place for a partial blockette record
	//private byte[] nextRecord;      // holding place for upcoming blockette record
	private static final float defaultSeedVersion =
		BlocketteFactory.getDefaultVersion();  // default version of SEED on reset
	private boolean seedVolumeControlHeaderFound = false;  // flag true to indicate we have encountered a SEED Volume Control Header
	private float seedVersion = defaultSeedVersion;       // version of SEED represented
	private boolean swapFlag = false;      // flag to indicate word order of binary records...set to true for VAX/8086 order
	//private int[] sequenceTracker = {0,0,0,0,0,0,0};  // will track sequence numbers for each header code
	private int dataBlocketteOffset = 0;   // have to cheat here and keep track of our offset in the data record
	protected Blockette prevBlockette = null;  // handle to hold an object for recycle and prior iteration memory
	//private boolean mutateDictionaryLookupKey = true;  // whether to alter dictionary lookup fields to the lookupMap value
	
}
