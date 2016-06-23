package edu.iris.Fissures.seed.builder;

import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.exception.*;
import java.util.*;

/**
 * Concrete builder class for SEED Objects. Registers itself to a concrete
 * Import Director. Creates and writes to a SeedObjectContainer. Optionally
 * implements serialization to disk to conserve memory usage.
 * 
 * @author Robert Casey, IRIS DMC<br>
 *         Sid Hellman, ISTI<br>
 *         Yazan Suleiman, IRIS DMC
 * @version 07/05/2013
 * 
 */

public class SeedObjectBuilder extends ObjectBuilder {

	/**
	 * Create a SEED object builder.
	 */
	public SeedObjectBuilder() {
		buildContainer = (ObjectContainer) new SeedObjectContainer(); // instantiate
																		// an
																		// object
																		// container
																		// when
																		// we
																		// start
																		// up
		builderType = "SEED"; // we are builder type 'SEED'
	}

	/**
	 * Create a SEED Object Builder with serialization to indicated filename.
	 */
	public SeedObjectBuilder(String diskFile) throws BuilderException {
		// alternate calling pattern that allows for Serialization to a disk
		// file to conserve memory
		try {
			buildContainer = (ObjectContainer) new SeedObjectContainer(); // this
																			// container
																			// makes
																			// a
																			// serialization
																			// file
			builderType = "SEED"; // we are builder type 'SEED'
		} catch (Exception e) {
			throw new BuilderException("Exception encountered: " + e);
		}
	}

	// public methods

	/**
	 * Construct a Blockette object by reading from the start of the indicated
	 * byte array.
	 */
	public int build(byte[] nextRecord) throws Exception {
		if (debug)
			System.err.println("DEBUG: import call to build(), recordType: " + recordType);
		if (nextRecord.length == 0)
			throw new BuilderException("zero length record data in build() call");
		byte[] currentRecord = nextRecord; // use a separate pointer to
											// nextRecord as the default
		if (debug)
			System.err.println("DEBUG: build() nextRecord: " + new String(nextRecord));
		int numBytesRead = 0;
		Blockette newBlockette = null;
		// before processing this current record, assess whether we have a large
		// coefficient blockette
		// accumulation to store.
		// Trigger on largeCoeffFlag to largeCoeffStore latch.
		if (debug && prevBlockette != null)
			System.err.println("DEBUG:  prevBlockette isResponse? : " + prevBlockette.isResponseBlockette());
		// STORAGE OF LARGE COEFFICIENT BLOCKETTES contingent on:
		// 1. there being a prevBlockette in holding which is a response
		// blockette
		// 2. the largeCoeffFlag is set to FALSE -- presumably at last build
		// pass
		// 3. the largeCoeffStore is set to TRUE -- a lagging indicator of
		// previous largeCoeffFlag setting
		if (prevBlockette != null && prevBlockette.isResponseBlockette() && !largeCoeffFlag && largeCoeffStore) {
			if (debug)
				System.err.println("DEBUG: Trigger largeCoeffStore latch on: " + prevBlockette.toString());
			currentObject = prevBlockette;
			store(); // call the store routine internally, currentObject and
						// recycleBlockette will be nulled
			prevBlockette = null; // blank out our blockette memory
			largeCoeffStore = false; // reset this flag
			// resume normal operation
		}
		// based on record type, which is externally set by the Director, build
		// appropriately...
		switch (recordType) {
		case 'V':
		case 'A':
		case 'S':
		case 'T':
			// control header record
			//
			// check to see if this is a continuation record, continuation flag
			// is externally set by Director
			if (prevRecord != null && continuationFlag) {
				// this is a continuation...append this new data to the end of
				// prevRecord's data
				// and build the blockette
				if (debug)
					System.err.println("DEBUG:  prevRecord is non-null and continuationFlag is set");
				currentRecord = new byte[prevRecord.length + nextRecord.length];
				System.arraycopy(prevRecord, 0, currentRecord, 0, prevRecord.length);
				System.arraycopy(nextRecord, 0, currentRecord, prevRecord.length, nextRecord.length);
			}
			if (debug)
				System.err.println("DEBUG: build() currentRecord: " + new String(currentRecord));
			if (recycleBlockette != null) {
				// object reuse if recyclable object is present
				recycleBlockette.initialize(currentRecord, false, false, seedVersion);
				newBlockette = recycleBlockette;
			} else {
				// build a blockette with the Factory method using currentRecord
				// as data stream
				newBlockette = BlocketteFactory.createBlockette(currentRecord, false, false, seedVersion);
			}
			if (newBlockette == null)
				throw new BuilderException("Blockette Factory returned a null value");
			// now let's check to see if this is a 'versioning' volume blockette
			int blocketteNumber = newBlockette.getType();
			if (blocketteNumber == 5 || blocketteNumber == 8 || blocketteNumber == 10) { // known
																							// versioning
																							// blockettes
				// so far all of the version specifiers are in field 3
				// set our builder's version state to be the value contained in
				// this blockette.
				seedVersion = Float.parseFloat(newBlockette.toString(3));
				// because blockettes 8 and 10 have the peculiar nature of being
				// different sizes depending on version,
				// we must build the blockette again in order for some cases to
				// register as a complete build.
				// just reuse the blockette we already have
				newBlockette.initialize(currentRecord, false, false, seedVersion);
				seedVolumeControlHeaderFound = true; // signals that we are
														// reading from a full
														// SEED file
			}
			//
			// we will keep count of the continuation blockette flags until
			// there is a break
			// this will help us to predict large coefficient blockettes on the
			// first blockette
			// to prevent premature storing
			if (continuationFlag) {
				continuationFlagCount++;
			} else {
				continuationFlagCount = 0;
			}
			if (debug)
				System.err.println("DEBUG:   continuationFlagCount == " + continuationFlagCount);
			// check to see if this is a partial blockette, which means that
			// another build call will
			// be needed to complete it.
			if (newBlockette.isIncomplete()) { // we have a partial blockette,
												// save the record section for
												// next read
				prevRecord = new byte[currentRecord.length]; // size the holder
																// to the length
																// of the
																// current
																// record
				System.arraycopy(currentRecord, 0, prevRecord, 0, currentRecord.length);
				numBytesRead = currentRecord.length; // for sanity, assure that
														// the number of bytes
														// read is set to the
														// length of the record
				currentObject = null; // we will have no current object with a
										// partial blockette
				recycleBlockette = newBlockette; // save for object reuse
			} else { // we have a complete blockette
				if (prevRecord == null) { // did we append to a previous
											// incomplete record?
					// no, then return number of bytes read
					numBytesRead = newBlockette.getNumBytes();
				} else {
					// yes, then subtract previous record's bytes since we
					// already reported it
					numBytesRead = newBlockette.getNumBytes() - prevRecord.length;
					prevRecord = null;
				}
				// is this blockette an addendum to the previous response
				// blockette with added coefficients?
				// largeCoeffFlag is stored in ObjectBuilder.java
				if (largeCoeffFlag && prevBlockette != null && prevBlockette.isResponseBlockette()) {
					int numFields = prevBlockette.getNumFields();
					int repeatField = 0;
					int repeatCount = 0;
					int skip = 0;
					Vector groupVec = null;
					if (debug)
						System.err.println("DEBUG: builder -- merging response blockettes. largeCoeffStore = true.");
					for (int field = 1; field < numFields; field++) {
						if (--skip > 0) { // pre-decrement
							continue; // skip past this field, it's part of an
										// already transcribed group
						}
						repeatField = prevBlockette.getFieldRepeat(field);
						if (repeatField == 0) { // stop at the first repeating
												// field
							repeatCount = 0;
							continue;
						}
						// get the new repeat count value at the same field
						repeatCount = (Integer) newBlockette.getFieldVal(repeatField);
						// get each new repeating value and add to the
						// prevBlockette
						for (int i = 0; i < repeatCount; i++) { // for each
																// group index
							groupVec = (Vector) newBlockette.getFieldGrp(field, i);
							if (groupVec != null && groupVec.size() > 0) {
								skip = groupVec.size(); // set up skip value to
														// allow us to advance
														// to the field beyond
														// this group
								prevBlockette.addFieldGrp(field, groupVec);
							}
						}
					}
					largeCoeffStore = true; // we want to trip this flag as a
											// lagging indicator for when
											// largeCoeffFlag is dropped
					currentObject = null; // we will not store our current
											// object but hold onto
											// prevBlockette for the meantime
				} else if (continuationFlagCount > 1 && newBlockette != null && newBlockette.isResponseBlockette()) {
					// a leading indicator of a large coefficient blockette
					// (which comes as a set of smaller blockettes
					// from SEED, is a continuationFlagCount of 2 or greater and
					// newBlockette being a response blockette
					if (debug)
						System.err.println("DEBUG: continuationFlag count -- triggering largeCoeffFlag...");
					largeCoeffFlag = true; // force this flag setting now
					prevBlockette = BlocketteFactory.createBlockette(newBlockette.toString()); // remember
																								// this
																								// for
																								// future
																								// pass
					currentObject = null; // delay the storing of what is
											// (possibly) a preliminary
											// coefficient blockette object
				} else {
					// this is a normal store() situation, where currentObject
					// will get passed on to end up in the container
					currentObject = newBlockette;
				}
			}
			break;
		case 'D':
		case 'R':
		case 'Q':
		case 'M':
			// data record
			//
			prevRecord = null;
			// if this is the beginning of a data record, then we must be
			// looking at an FSDH.
			// we have to take the incoming data and modify it for the
			// BlocketteFactory before passing it on.
			// Create a blockette 999 and determine the word order of the binary
			// data.
			if (recordBeginFlag) {
				dataBlocketteOffset = 0;
				if (currentRecord.length < 40) { // this is the length of the
													// FSDH, minus record ID
													// block
					throw new BuilderException("data record is too short (" + currentRecord.length + ")");
				}
				byte[] timeArr = new byte[10];
				System.arraycopy(currentRecord, 12, timeArr, 0, 10); // pull out
																		// binary
																		// time
																		// entry
																		// in
																		// the
																		// FSDH
				Btime bTime = new Btime(timeArr); // feed the array to Btime
													// structure and test for
													// byte swap flag
				swapFlag = bTime.getSwapFlag(); // determine the swapFlag value
												// from the Btime object
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
				byte[] dQFlags = new byte[8]; // data quality flags -- field is
												// size 8
				dQFlags[0] = (byte) recordType; // record type
				if (dQFlags[1] == '*') { // continuation flag
					continuationFlag = true;
				} else {
					continuationFlag = false;
				}
				for (int i = 2; i < 8; i++)
					dQFlags[i] = ' '; // space padded
				Btime createTime = new Btime(); // get current time for the FSDH
												// creation time
				// transcribe synthesis to currentRecord array
				currentRecord = new byte[nextRecord.length + 20]; // make new
																	// array for
																	// synthesis
																	// --
																	// affects
																	// offset by
																	// 12 (-8 +
																	// 20)
				System.arraycopy(blocketteNumByte, 0, currentRecord, 0, 2);
				System.arraycopy(dQFlags, 0, currentRecord, 2, 8);
				System.arraycopy(createTime.getByteTime(swapFlag), 0, currentRecord, 10, 10);
				// append nextRecord which starts at field 4 of FSDH
				System.arraycopy(nextRecord, 0, currentRecord, 20, nextRecord.length);
				recordBeginFlag = false; // toggle this flag off while we look
											// forward to later blockettes in
											// this record
			}
			if (recycleBlockette != null) {
				// object reuse
				recycleBlockette.initialize(currentRecord, swapFlag, true, seedVersion);
				newBlockette = recycleBlockette;
			} else {
				// build a blockette with the Factory method using currentRecord
				// as data stream.
				newBlockette = BlocketteFactory.createBlockette(currentRecord, swapFlag, true, seedVersion);
			}
			if (newBlockette == null)
				throw new BuilderException("Blockette Factory returned a null value");
			// if this is the FSDH, then the number of bytes consumed will
			// be equal to the byte number of the first data blockette, minus 8,
			// or in the
			// case of the first blockette pointer being 0, the number of bytes
			// will be the
			// length of the entire record, to negate any further reading
			if (newBlockette.getType() == 999) { // we are an FSDH blockette
				dataBlocketteOffset = Integer.parseInt(newBlockette.toString(18)); // get
																					// the
																					// offset
																					// of
																					// the
																					// first
																					// data
																					// blockette
				numBytesRead = dataBlocketteOffset - 8; // compensate for 8
														// bytes already
														// accounted for
				if (dataBlocketteOffset == 0)
					numBytesRead = currentRecord.length; // no data blockettes,
															// shift offset to
															// end of record
				// now we will fetch the waveform data for this FSDH
				int waveformOffset = Integer.parseInt(newBlockette.toString(17));
				if (waveformOffset > 48) {
					int numSamples = Integer.parseInt(newBlockette.toString(9));
					// encoding is currently Unknown, but can be modified later
					// at the application level.
					// waveformOffset for currentRecord adjusted by -8 and + 20
					// which is +12.
					Waveform newWaveform = new Waveform(currentRecord, waveformOffset + 12, numSamples, "UNKNOWN",
							swapFlag);
					newBlockette.attachWaveform(newWaveform); // attach waveform
																// to the FSDH
				}
			} else {
				// we are a data blockette. find the start of the next
				// blockette.
				int nextBlockette = Integer.parseInt(newBlockette.toString(2));
				if (nextBlockette > 0) {
					numBytesRead = nextBlockette - dataBlocketteOffset; // there
																		// is
																		// another
																		// blockette
																		// for
																		// the
																		// next
																		// pass
					if (numBytesRead <= 0)
						throw new BuilderException("regressive data blockette offset " + nextBlockette
								+ " at byte offset " + dataBlocketteOffset);
					dataBlocketteOffset = nextBlockette; // bump the offset up
															// to where we have
															// just read
				} else {
					numBytesRead = currentRecord.length; // if no other
															// blockettes,
															// cancel out the
															// rest of the
															// record
				}
			}
			currentObject = newBlockette;
			break;
		default:
			throw new BuilderException("Unable to identify recordType" + recordType);
		}
		// check the current blockette against the BuilderFilter
		filterBlockette();
		// finally, return the number of bytes consumed by this build method
		if (debug)
			System.err.println("DEBUG: numBytesRead: " + numBytesRead);
		return numBytesRead;
	}

	/**
	 * Construct a Blockette object from the delimited String. Only accepts
	 * standard delimiters '|' and '^'. Return the length in bytes of the
	 * processed String as a confirmation of success. Return -1 on failure.
	 */
	public int build(String blocketteSpec) throws Exception {
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
		filterBlockette();
		return numBytesRead;
	}

	/**
	 * Store created SEED object to the SEED object container. Return the
	 * blockette object's lookup ID number, assigned in this method. Returns a
	 * -1 on failure. <b>volumeNumber</b> is used for indexing blockettes in the
	 * container.
	 * 
	 * lookupID pattern for dictionary blockettes is this: Integer:
	 * V,VVT,BBN,NNN (positive value must not exceed (2,147,483,647) where V =
	 * volume number 000-214 T = header category 0-6
	 * (0=reserved,1=volume,2=dictionary,3=station,4=timespan,5=data,6=reserved)
	 * B = blockette type number 00-99 N = sequence number per header category
	 * 0000-9999
	 *
	 * lookupID pattern for non-dictionary blockettes is this: Integer:
	 * V,VVT,NNN,NNN (positive value must not exceed (2,147,483,647) where V =
	 * volume number 000-214 T = header category 0-6
	 * (0=reserved,1=volume,2=dictionary,3=station,4=timespan,5=data,6=reserved)
	 * N = sequence number per header category 000,000-999,999
	 */
	public int store() throws Exception {
		// because of filtering that may be applied during the build process, it
		// is possible
		// that we were passed a null currentObject...this is perfectly normal.
		if (currentObject == null) {
			if (debug)
				System.err.println("DEBUG: store() called with currentObject == null");
			if (debug && prevBlockette != null)
				System.err.println("DEBUG:         : prevBlockette == " + prevBlockette.toString());
			if (debug)
				System.err.println("DEBUG:         : largeCoeffFlag == " + largeCoeffFlag + ", largeCoeffStore == "
						+ largeCoeffStore);
			// have largeCoeffStore set to the same value as largeCoeffFlag when
			// we reach this null store condition
			largeCoeffStore = largeCoeffFlag;
			return -1; // return without storing anything yet
		}
		if (debug)
			System.err.println("DEBUG: store() called with currentObject == " + currentObject.toString());
		if (debug)
			System.err.println(
					"DEBUG:         : largeCoeffFlag == " + largeCoeffFlag + ", largeCoeffStore == " + largeCoeffStore);
		// make sure this builder is aware of the current volume number
		volumeNumber = ((SeedObjectContainer) buildContainer).getVolumeNumber();
		if (volumeNumber > 214)
			throw new BuilderException("volumeNumber is too high.  must be value 000-214");
		int volumeVal = volumeNumber * 1000 * 1000 * 10; // multipliers
															// left-shift the
															// volume value in
															// the integer
															// representation
		// get the header category number
		int headerCategory = SeedBlocketteRankMap.getHeaderCode((Blockette) currentObject);
		if (headerCategory > 6)
			throw new BuilderException("header category code for lookup ID exceeds 6");
		// get the blockette type
		int blocketteNumber = ((Blockette) currentObject).getType();
		// branch here based on header category -- dictionaries use their lookup
		// fields to generate the ID
		int lookupId = -1; // this will be the blockette's assigned ID number

		lookupId = ((SeedObjectContainer) buildContainer).getNewId((Blockette) currentObject, true);

		((Blockette) currentObject).setLookupId(lookupId); // apply the ID value
															// to the blockette
		//
		// now that we have identified this blockette, we need to see if there
		// are any dictionary lookups for this blockette that need resolved
		int[] lookupFields = SeedDictionaryReferenceMap.lookupSourceFld(blocketteNumber);
		for (int i = 0; lookupFields != null && i < lookupFields.length; i++) { // for
																				// each
																				// lookup
																				// field
			int[] lookupDictBlk = SeedDictionaryReferenceMap.lookupDestBlk(blocketteNumber, lookupFields[i]);
			// now we want to get the lookup values in the referencing blockette
			// the field may be a repeating one, so we have to check for that
			int repeatFld = ((Blockette) currentObject).getFieldRepeat(lookupFields[i]);
			int numRepeat = 0;
			// Blockette 60 case
			if (((Blockette) currentObject).getFieldType(lookupFields[i]).equals("L")) { // list
																							// (Vector)
																							// data
																							// type
				// let's get the lookup field of the lookup field instead
				if (debug)
					System.err.println("DEBUG: store() field " + i + ", repeatFld = " + repeatFld);
				repeatFld = ((Blockette) currentObject).getFieldRepeat(repeatFld);
				if (debug)
					System.err.println("DEBUG: store() repeatFld of repeatFld = " + repeatFld);
			}
			if (repeatFld == 0) {
				numRepeat = 1; // not a repeating field, just 1 entry
			} else {
				numRepeat = Integer.parseInt(((Blockette) currentObject).toString(repeatFld));
			}
			Vector dictLookupVect = null;
			int[] dictLookupArr = null;
			Object saveLookupIndex = null;
			for (int k = 0; k < numRepeat; k++) { // for each repeat (or
													// non-repeat) field
				if (((Blockette) currentObject).getFieldType(lookupFields[i]).equals("L")) { // list
																								// (Vector)
																								// data
																								// type
																								// (Blockette
																								// 60)
					dictLookupVect = (Vector) ((Blockette) currentObject).getFieldVal(lookupFields[i], k); // get
																											// the
																											// dictionary
																											// lookup
																											// Vector
					dictLookupArr = new int[dictLookupVect.size()]; // set up
																	// integer
																	// array
					for (int m = 0; m < dictLookupVect.size(); m++) {
						dictLookupArr[m] = Integer.parseInt(dictLookupVect.get(m).toString()); // populate
																								// the
																								// integer
																								// array
						if (debug)
							System.err.println("DEBUG: store() dictLookupArr[" + m + "] =" + dictLookupArr[m]);
					}
				} else {
					dictLookupArr = new int[1]; // set up integer array for
												// compatibility with above case
					String dictLookupStr = ((Blockette) currentObject).toString(lookupFields[i], k);
					if (dictLookupStr.length() == 0)
						dictLookupStr = "0"; // comes up blank? insert safe
												// value
					dictLookupArr[0] = Integer.parseInt(dictLookupStr); // get
																		// the
																		// dictionary
																		// lookup
																		// value
				}
				for (int m = 0; mutateDictionaryLookupKey && m < dictLookupArr.length; m++) {
					int dictLookupVal = dictLookupArr[m]; // get the next
															// dictionary
															// reference
					int lookupIndex = 0;
					if (dictLookupVal > 0) { // if the lookup value is zero,
												// then it references nothing
						// *predictively* determine the lookup ID of the
						// dictionary blockette that this field references.
						// don't bother checking to see if the dictionary
						// blockette with that ID actually exists...we have
						// to assume that there is sufficient internal
						// consistency in the SEED data...if it is not
						// consistent,
						// that will have to be borne out at the application
						// level.
						// header category is automatically assumed to be
						// category 2.
						//
						// just get the first element of the array
						int bTypeNumber = lookupDictBlk[0]; // this is the
															// dictionary
															// blockette type

						int dictLookupId = volumeVal + (2 * 1000 * 1000) + (bTypeNumber * 1000 * 10) + dictLookupVal;
						// with this ID tag, do a mapping conversion within the
						// current blockette, reindexing its
						// field value to point to its lookupMap

						lookupIndex = ((Blockette) currentObject).addDictionaryLookup(dictLookupId);
					}
					// save this lookup index to the dictionary reference field
					// of the blockette
					if (dictLookupArr.length > 1) { // list (Vector) data type
													// (Blockette 60)
						// insert value into extracted Vector
						dictLookupVect.set(m, new Integer(lookupIndex));
						saveLookupIndex = dictLookupVect;
					} else {
						saveLookupIndex = new Integer(lookupIndex);
					}
				}
				if (debug)
					System.err.println("DEBUG: setFieldVal blktype=" + blocketteNumber + ", field=" + lookupFields[i]
							+ ", " + k + ", saveLookupIndex=" + saveLookupIndex);
				if (mutateDictionaryLookupKey)
					((Blockette) currentObject).setFieldVal(lookupFields[i], k, saveLookupIndex); // push
																									// the
																									// index
																									// value(s)
																									// to
																									// the
																									// blockette
			}
		}
		//
		buildContainer.add(currentObject); // add the blockette to the container
		recycleBlockette = null; // make sure there is no object reuse for this
									// instance
		currentObject = null; // blank out the current object holder
		prevBlockette = null;
		return lookupId; // return the assigned lookupId for this blockette
							// object
	}

	/**
	 * Reset the builder to its initial state. Generally used during volume
	 * transitions.
	 */
	public void reset() {
		currentObject = null;
		prevRecord = null;
		// nextRecord = null;
		seedVersion = defaultSeedVersion;
		swapFlag = false;
		seedVolumeControlHeaderFound = false;
		// for (int i = 0; i < sequenceTracker.length; i++) sequenceTracker[i] =
		// 0;
		prevBlockette = null;
	}

	/**
	 * Increment the volume number when a new data stream is being read in. Also
	 * reset state of the Builder. This method supercedes the ObjectBuilder
	 * implementation
	 */
	public void incrementVolume() {
		volumeNumber = BlocketteDecoratorFactory
				.getNewVolumeNumber(); /** NOT GOOD - static var in factory **/
		reset();
	}

	// flag the store() method to alter the lookup code in dictionary lookup
	// fields to use the
	// container lookup map. default is to set this to true
	// set this to false if an application wishes to maintain the original
	// lookup key and perform
	// its own lookup outside of the container methods.
	public void dictionaryLookupMutation(boolean b) {
		mutateDictionaryLookupKey = b;
	}

	// private methods

	// Now that an object has been built, we will decide here whether to keep
	// it,
	// provided that:
	// a) we have a blockette attached to currentObject
	// b) we have registered BuilderFilters attached
	// return true if the blockette passes the filter, false if rejected by the
	// filter. The globally visible blockette is nulled out on failure
	// by this method.
	private boolean filterBlockette() throws Exception {
		int vecSize = buildFilterVector.size();
		BuilderFilter buildFilter;
		boolean match = false; // flag whether we have a match during this loop
		if (currentObject != null && vecSize != 0) {
			for (int i = 0; i < vecSize; i++) { // for each filter in the vector
				buildFilter = (BuilderFilter) buildFilterVector.get(i);
				if ((match = buildFilter.qualify(currentObject)) == true)
					break; // found a match...quit loop
			}
			// another condition to check for is if a dominant ranking (parent)
			// blockette was filtered out
			// before this blockette, then also treat this blockette as
			// rejected,
			// since it is a child of the rejected blockette.
			int filterRank = ((SeedObjectContainer) buildContainer).getFiltered(); // get
																					// indicator
																					// of
																					// filtered
																					// parent
			int blkRank = SeedBlocketteRankMap.getRank((Blockette) currentObject); // get
																					// rank
																					// of
																					// current
																					// blockette
			if (filterRank > 0 && blkRank >= filterRank)
				match = false; // if a parent was rejected, then mark this as
								// rejected as well
			if (match == false) { // if we don't have a match...
				((SeedObjectContainer) buildContainer).setFiltered((Blockette) currentObject); // tell
																								// container
																								// this
																								// is
																								// being
																								// filtered
																								// out
				if (debug)
					System.out.println("DEBUG: rejecting blockette " + currentObject.toString());
				recycleBlockette = (Blockette) currentObject; // save for object
																// reuse
				currentObject = null; // deactivate this Blockette
			} else {
				// we have a match, so make sure that the Filtered state of the
				// container is
				// set to a dormant (zero) value.
				if (filterRank > 0)
					((SeedObjectContainer) buildContainer).setFiltered(0);
			}
		}
		return match;
	}

	/**
	 * DEPRECATED
	 * 
	 * Return a six-digit sequence number based on the header code (category
	 * number) of the current blockette object.
	 */
	protected int getSequenceNum(int headerCode) throws BuilderException {
		// if (headerCode > 6) throw new BuilderException ("header code greater
		// than 6");
		// int sequenceNum = sequenceTracker[headerCode]; // get the current
		// sequence number
		// sequenceTracker[headerCode]++; // increment the sequence number
		// return sequenceNum;
		try {
			if (headerCode > 6)
				throw new BuilderException("header code greater than 6");
			int seqNum = ((SeedObjectContainer) buildContainer).getNewId(headerCode);
			if (seqNum > 999999)
				seqNum -= (seqNum / 1000000) * 1000000; // get just lower 6
														// digits
			return seqNum;
		} catch (Exception e) {
			e.printStackTrace();
			throw new BuilderException("ERROR in getSequenceNum: " + e);
		}
	}

	// instance variables

	// prevRecord is used to remember a previous incomplete record for suturing
	// to next record
	// prevRecord will be used in record length calculations
	// prevRecord will be nulled when complete blockette has been achieved
	// prevBlockette is used to remember a previous complete blockette that may
	// be short of coefficients
	// prevBlockette will not be used in record length calculations
	// prevBlockette will not be nulled until largeCoeff flag is turned off
	private byte[] prevRecord; // holding place for a partial blockette record
	private Blockette prevBlockette;
	private boolean largeCoeffStore = false;
	int continuationFlagCount = 0; // this counter will help us to detect large
									// coefficient blockettes at the outset
	//
	//
	// private byte[] nextRecord; // holding place for upcoming blockette record
	private static final float defaultSeedVersion = BlocketteFactory.getDefaultVersion(); // default
																							// version
																							// of
																							// SEED
																							// on
																							// reset
	private boolean seedVolumeControlHeaderFound = false; // flag true to
															// indicate we have
															// encountered a
															// SEED Volume
															// Control Header
	private float seedVersion = defaultSeedVersion; // version of SEED
													// represented
	private boolean swapFlag = false; // flag to indicate word order of binary
										// records...set to true for VAX/8086
										// order
	// private int[] sequenceTracker = {0,0,0,0,0,0,0}; // will track sequence
	// numbers for each header code
	private int dataBlocketteOffset = 0; // have to cheat here and keep track of
											// our offset in the data record
	protected Blockette recycleBlockette = null; // handle to hold an object for
													// possible recycle
													// (prevents object churn)
	private boolean mutateDictionaryLookupKey = true; // whether to alter
														// dictionary lookup
														// fields to the
														// lookupMap value
	//
	private boolean debug = false; // set to true to get debug messages
}
