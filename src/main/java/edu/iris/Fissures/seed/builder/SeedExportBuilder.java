package edu.iris.Fissures.seed.builder;

import edu.iris.Fissures.seed.exception.*;
import edu.iris.Fissures.seed.container.*;
import java.util.*;

/**
 * Concrete Builder class for exporting Blockette objects from
 * the SeedObjectContainer to a SEED file format.
 * @author Robert Casey, IRIS DMC
 * @version 4/3/2008
 */
public class SeedExportBuilder extends ExportBuilder {
	
	/**
	 * Create new Seed Export Builder.  Set the builder to run in default (full
	 * SEED) mode.
	 */
	public SeedExportBuilder() {
		setDefaultMode();
		// set some default values
		logicalRecordLength = defaultLogicalRecordLength;
		physicalRecordLength = 32768;
		scriptNesting = new int[8];
		nestingScore = new int[scriptNesting.length];
		logicalRecords = new Vector(8,8);
		exportMold = new Vector(8,8);
		recordPadding = (byte) 32;  // use spaces for padding
		newByteBlock = new byte[65536];  // make this a large buffer size
		organizationName = "";
		volumeLabel = "";
		builderType = "SEED";  // indicate the type of builder we are
	}
	
	/**
	 * Create the builder to output one of the SEED variants.
	 * The current variants are: <i>full, mini, dataless.</i>.
	 * Default case is <i>full</i>.
	 * */
	public SeedExportBuilder(String mode) {
		// create the builder to output one of the SEED variants.
		// the current variants are: full, mini, dataless.
		// default case is full.
		this();  // do the other initialization activities first
		// now set the builder scripting mode
		if (mode.equals("mini")) {
			setMiniMode();      // miniSEED
		} else if (mode.equals("dataless")) {
			setDatalessMode();  // dataless SEED
		} else {
			setDefaultMode();   // full SEED
		}
	}
	
	// public methods
	
	/**
	 * Set the builder to datalessSEED output mode.
	 */
	public void setDatalessMode() {
		//datalessMode = true;
		// this String represents the script pattern for a dataless SEED volume.
		// each element is separated by a comma.
		// see getNext() in ExportBuilder.java for details.
		scriptString =
			// build simulation -- triggers are capital letters
			new String ("^,L,(,30,),(,31,),(,32,),(,33,),(,34,)," +
					"(,41,),(,42,),(,43,),(,44,),(,45,),(,46,),(,47,)," +
					"(,48,),<,A,(,B,50,(,51,),(,52,(,60,53,54,61,62,55,56,57,58,)," +
					"(,59,),),H,<,),I,J,<,K," +
					// volume transcription
					"v,-2,M,10,11,<,(,30,),(,31,),(,32,),(,33,),(,34,),(,41,)," +
					"(,42,),(,43,),(,44,),(,45,),(,46,),(,47,)," +
					"(,48,),<,(,50,(,51,),(,52,(,60,53,54,61,62,55,56,57,58,)," +
                                        "(,59,),),<,)");
		genScriptArray();  // generate array from script string
		
	}
	
	/**
	 * Set the builder to miniSEED output mode.
	 */
	public void setMiniMode() {
		//miniMode = true;
		// this String represents the script pattern for a miniSEED volume.
		// each element is separated by a comma.
		// see getNext() in ExportBuilder.java for details.
		scriptString = ("L,(,[,999,100,200,201,202,300,310,320,390,395,400,405,500,1000,1001,],<,)");
		genScriptArray();  // generate array from script string
	}
	
	/**
	 * Set the builder to (default) full SEED mode.
	 */
	public void setDefaultMode() {
		//datalessMode = false;
		//miniMode = false;
		// this String represents the script pattern for a Full SEED volume.
		// each element is separated by a comma.
		// see getNext() in ExportBuilder.java for details.
		scriptString =
			// build simulation -- triggers are capital letters
			new String ("^,L,(,30,),(,31,),(,32,),(,33,),(,34,),(,41,),(,42,),(,43,),(,44,),(,45,),(,46,),(,47,)," +
					"(,48,),<,A,(,B,50,(,51,),(,52,(,60,53,54,61,62,55,56,57,58,)," +
					"(,59,),),H,<,),C,(,D,[,999,100,200,201,202,300,310,320,390,395,400,405,500,1000,1001,],E,<,)," +
					"F,71,(,72,),G,<,I,J,<,K," +
					// volume transcription
					"v,-2,M,10,11,12,<,(,30,),(,31,),(,32,),(,33,),(,34,),(,41,)," +
					"(,42,),(,43,),(,44,),(,45,),(,46,),(,47,)," +
					"(,48,),<,(,50,(,51,),(,52,(,60,53,54,61,62,55,56,57,58,)," +
					"(,59,),),<,),70,71,(,72,),(,74,),<," +
                                        "(,[,999,100,200,201,202,300,310,320,390,395,400,405,500,1000,1001,],<,)");
		genScriptArray();  // generate array from script string
	}
	
	/**
	 * Set the originating organization name for this volume.
	 * The originating organization is the institution authoring this volume.
	 * Max length of 80 characters.
	 */
	public void setOrganizationName(String name) throws SeedFormatException {
		if (name.length() > 80) {
			throw new SeedFormatException("originating organization name cannot be longer than 80 characters");
		}
		organizationName = name;
	}
	
	/**
	 * Set the label string for this volume.
	 * Max length of 80 characters.
	 */
	public void setVolumeLabel(String label) throws SeedFormatException {
		if (label.length() > 80) {
			throw new SeedFormatException("volume label cannot be longer than 80 characters");
		}
		volumeLabel = label;
	}
	
	/**
	 * Get the end time (as a Btime object).  Projected from the start time
	 * based on the number of samples and the calculated sample rate.
	 */
	public static Btime getEndTime(Btime startTime, int numSamples, int srFactor, int srMult) throws Exception {
		// get the sample rate
		double true_rate;
		if ((srFactor * srMult) == 0.0) {
			true_rate = 10000.0;
		} else {
			true_rate = (Math.pow( (Math.abs(srFactor)), (srFactor / Math.abs(srFactor) ) ) *
					Math.pow( (Math.abs(srMult)), (srMult / Math.abs(srMult) ) ) );
		}
		double ttSeconds = (numSamples) / true_rate * 10000.0;  // find the number of ten-thousands of seconds
		return startTime.projectTime(ttSeconds);  // the end time is the projection from the start time
	}
	
	// protected methods

    /**
     * Sets the logical record length to the default value.
     */
    protected void setLogicalRecordLength() {
        logicalRecordLength = defaultLogicalRecordLength;
    }

    /**
     * Sets the logical record length.
     * @param waveFrm the Waveform or null if none.
     * @param offset the offset.
     */
    protected void setLogicalRecordLength(Waveform waveFrm, int offset) {
        try {
            final byte[] waveBytes;
            if (waveFrm != null && (waveBytes = waveFrm.getEncodedBytes()) != null) {
               //default to the minimum logical record length
               int lrl = 256;
               final int numBytes = waveBytes.length + offset;
               while (numBytes > lrl) {
                 if (lrl >= defaultLogicalRecordLength) {
                   lrl = defaultLogicalRecordLength;
                   break;
                 }
                 lrl *= 2;
               }
               logicalRecordLength = lrl;
               return;
             }
        }
        catch (Exception ex) {
            // maybe we should put an exception handler here?
        }
        //set the logical record length to the default value
        setLogicalRecordLength();
    }
	
	/**
	 * Pack currently held Blockette object to a SEED logical record.
	 */
	protected void packToRecord() throws Exception {
		int byteBlockPtr = 0;  // reset position in the newByteBlock...this also represents SEED byte length
		if (exportMold == null) {
			throw new BuilderException("null export mold");
		}
		if (exportMold.size() == 0) {
			return;   // empty export mold, then do nothing.
		}
                setLogicalRecordLength();
		Blockette blk = (Blockette) exportMold.get(0);  // get the first blockette
                // DEBUG
//if (blk.getType() == 54) {
//    System.err.println("DEBUG: pack 54 to record: " + blk.toString());
//}
		//
		boolean twinning = false;  // flag true if we are splitting to multiple blockettes
		int truncate = 0;  // set to getFieldRepeat() result if we truncate the rest of the blockette values to zero (when twinning)
		int truncGrpCount = 0;  // set to group count of first of two twin blockettes
		Blockette saveBlk = null;  // save copy of Blockette to this if we are splitting/twinning
		//
		int curField = 1;  // the current blockette field we are looking at
		int rptCount = 0;  // count of repeating field groups
		int[] fldPtrArr = new int[64];  // current binary positions of fields
                fldPtrArr[0] = 0;  // sanity -- unused
		int waveformOffset = 0;  // offset placement of waveform data
		int numFields = blk.getNumFields();  // get the number of fields in this blockette
		while (curField <= numFields) {  // foreach blockette entry...
                        fldPtrArr[curField] = byteBlockPtr;  // get start byte position of current field
			int rptField = blk.getFieldRepeat(curField);
			if (rptField > 0) {
				// this is a repeating field, part of a repeat field group
				rptCount = Integer.parseInt(blk.toString(rptField));  // get the value in the count field
			} else {
				rptCount = 1;  // else there is just one value for the field
			}
			if (rptCount == 0) {   // if there are zero entries then skip this field altogether
				curField++;
				continue;
			}

			if (twinning) { // we are truncating the blockette because it is getting too long
//System.err.println("DEBUG: TRUNCATE MODE - curField: " + curField + ", rptField: " + rptField);
                                if (rptField > 0 && curField - rptField == 1) {
//System.err.println("DEBUG: TRUNCATE MODE - purge group at field: " + curField);
                                    // we are the first field in a repeat group
                                    // purge the contents of the entire group and 
                                    // zero the repeat count (rptCount will be zero in subsequent passes)
                                    curField += blk.purgeFieldGrp(curField);  // then skip this field from byte output
                                    continue;
                                } else {
                                    // for non-repeat fields, insert a zero count to the ascii representation
//System.err.println("DEBUG: TRUNCATE MODE - insert zero at field: " + curField);
                                    byte[] fldBytes = BlocketteFactory.getBytes(blk.getType(),curField,"0");
                                    System.arraycopy(fldBytes,0,newByteBlock,byteBlockPtr,fldBytes.length); // add to template array
                                    byteBlockPtr += fldBytes.length;  // increment logical record pointer
                                    curField++;
                                    continue;
                                }

			}
                        
                        String fldVal = blk.toString(curField,0);  // get field entry as a string (repeat and non-repeat cases)
//System.err.println("DEBUG: fldVal( " + blk.getType() + ", " + curField + "):" + fldVal);
			//
			// handle special cases to make additional assignments or take additional actions
			if (blk.getType() == 50) {
				// we want to get certain information from Blockette 50
				if (curField == 3) {
					//System.err.println("DEBUG: blk 50 set stationName to: " + fldVal);
					stationName = new String(fldVal);
				}
				// we are only implementing SUN word order for exports at this
				// time...make sure that the word order fields representing the
				// data headers are set appropriately.
				blk.setFieldVal(11,new Integer(3210)); // 32-bit order
				blk.setFieldVal(12,new Integer(10)); // 16-bit order
			}
			if (blk.getType() == 999) {
				// if this is the FSDH blockette, we want to start transcription at field 4
				if (curField < 4) {
					curField++;  // skip current field and go onto the next one
					continue;
				}
				if (curField == 4) {
					// sample the station name
					//System.err.println("DEBUG: blk 999 set stationName to: " + fldVal);
					stationName = new String(fldVal);
				}
				if (curField == 5) {
					// sample the loc id
					locationId = new String(fldVal);
				}
				if (curField == 6) {
					// sample the channel name 
					channelName = new String(fldVal);
				}
				if (curField == 7) {
					// sample the network code
					networkCode = new String(fldVal);
				}
				if (curField == 8) {
					// sample the start time
					startTime = new Btime(fldVal);
				}
				if (curField == 9) {
					// get the number of samples
					numSamples = Integer.parseInt(fldVal);
				}
				if (curField == 10) {
					// get sample rate factor
					srFactor = Integer.parseInt(fldVal);
				}
				if (curField == 11) {
					// get sample rate multiplier
					srMult = Integer.parseInt(fldVal);
				}
				if (curField == 15) {
					// indicate how many data record blockettes are grouped with the FSDH
					blk.setFieldVal(15, new Integer(exportMold.size() - 1));
				}
				if (curField == 17) {
					// if we have data record blockettes, then add up their byte sizes
					// and determine the waveform data offset, which should be in steps of 64 bytes
					int numBlk = exportMold.size();
					int offset = 48;
					for (int i = 1; i < numBlk; i++) {  // for each blockette...
						Blockette dataBlk = (Blockette) exportMold.get(i);
						int numFields2 = dataBlk.getNumFields();
						for (int j = 1; j <= numFields2; j++) {  // for each field...
							offset += Integer.parseInt(dataBlk.getFieldLength(j));  // hopefully we don't get any X-Y ranges
						}
					}
					// check to see that there is waveform data -- sometimes there is a data record with no data
					Waveform waveFrm = blk.getWaveform();
                                        setLogicalRecordLength(waveFrm, offset);
					if (waveFrm == null) {
						waveformOffset = 0;   // set waveform offset value to 0
						blk.setFieldVal(17, new Integer(waveformOffset));
					} else {
						// find a multiple of 64 that works
						int k;
						for (k = 1; (k * 64) < offset && (k * 64) < logicalRecordLength; k++);
						if ((k * 64) >= logicalRecordLength)
							throw new BuilderException("waveform offset value too high: " + offset);
						waveformOffset = k * 64;
						blk.setFieldVal(17, new Integer(waveformOffset));
					}
				}
				if (curField == 18) {
					// if we have data record blockettes, set the first blockette offset value to 48.
					if (exportMold.size() > 1)
						blk.setFieldVal(18, new Integer(48));
					else
						blk.setFieldVal(18, new Integer(0));
				}
			}
			//
			//
			// convert content of the current Blockette field to binary/ascii form
			Vector rptVec = null;
			int vecSize = 0;
			int curGrp = 0;
			for (; curGrp < rptCount; curGrp++) {  // foreach repeat group...
				// if in blockette splitting mode, then halt any further repeat group processing.
				// go with the current count of coefficient groups, indicated by curGroup.
				// it's important that we adjust the group count of our current blockette before going further
				if (twinning) {
					truncGrpCount = curGrp;  // truncated blockette has this many repeat entries in the clipped area
					byte[] fldBytes = BlocketteFactory.getBytes(blk.getType(),rptField,new Integer(truncGrpCount).toString()); // replace w/ shorter count
					System.arraycopy(fldBytes,0,newByteBlock,fldPtrArr[rptField],fldBytes.length); // add to template array at count position
//System.err.println("DEBUG: truncGrpCount = " + truncGrpCount + ", set at offset " + fldPtrArr[rptField] + ", break from loop.");
					break;  // now break from repeat group processing
				}
				if (rptField > 0) {
					// repeating field case...
					rptVec = blk.getFieldGrp(curField,curGrp);  // get vector of entire field group
					vecSize = rptVec.size();  // number of fields in this group
				} else {
					// non-repeating field case...
					vecSize = 1;  // fake value
				}
                                // if not already in twinning (split) mode...
				// check to see whether we are about to exceed the 9999 character limit for blockettes.
				// go for a reasonable limit indicated by twinningTrigger as the trigger point
                                // only trigger this if we are actively in a repeat group
                                // also don't trigger if the group we are on is the very last one
				if (!twinning && rptField > 0 && byteBlockPtr > twinningTrigger && rptCount > curGrp+1) {
					// implement twinning function to split into two or more blockettes to accommodate
					// all of the metadata (i.e. coefficients)
					// this will involve recursive calls to packToRecord to push these partial twins through
					twinning = true;
        //System.err.println("DEBUG: TWINNING set to true");
                                        truncate = rptField;  // remember the repeat counter field where we truncated
        //System.err.println("DEBUG: truncate set to rptField=" + rptField + " at curField=" + curField);
					// let's create a **copy blockette** that we can progressively subtract from:
					saveBlk = BlocketteFactory.createBlockette(blk.toString(), seedVersion);
				}
				for (int vecPos = 0; vecPos < vecSize; vecPos++) {
					if (rptVec != null) fldVal = rptVec.get(vecPos).toString();  // get field value in repeating field case
					// write binary/ASCII version of field value 
					byte[] fldBytes = BlocketteFactory.getBytes(blk.getType(),(curField+vecPos),fldVal); // convert to bytes
					System.arraycopy(fldBytes,0,newByteBlock,byteBlockPtr,fldBytes.length); // add to template array
                                        byteBlockPtr += fldBytes.length;  // increment logical record pointer
				}  // in non-repeating field case, this loop should exit after one pass
			}
			curField += vecSize;  // increment to the next blockette field (jump past repeat group)
			
		} // done with all of the blockette fields -- newByteBlock[] is filled to position byteBlockPtr
		//
		// if we are an FSDH blockette, then we are probably grouped with other blockettes still in the exportMold (100,1000,etc.)
		if (blk.getType() == 999) {
			// go through the rest of the export mold and add data record blockettes to the logical record.
			int numBlk = exportMold.size();
			for (int i = 1; i < numBlk; i++) {
				Blockette dataBlk = (Blockette) exportMold.get(i);
				//System.err.println ("DEBUG: getting data blockette " + i + " of " + numBlk + "...");
				//System.err.println ("DEBUG: which looks like: " + dataBlk.toString());
				numFields = dataBlk.getNumFields();  // reuse numFields variable
				// if this is Blockette 1000, set the byte swapping flag to SUN
				// word order, since we don't currently support byte swapping in file export.
				if (dataBlk.getType() == 1000)
					dataBlk.setFieldVal(4,new Integer(1));
				// if there are more blockettes to follow, indicate the offset in field 2
				// numBlk is the number of data blockettes including the FSDH (+1 offset)
				if (i < (numBlk-1)) {
					//System.err.println ("DEBUG: i = " + i + ", byteBlockPtr = " + byteBlockPtr);
					int offset = byteBlockPtr + 8;  // offset as it will appear in the logical record
					for (int j = 1; j <= numFields; j++) {
						offset += Integer.parseInt(dataBlk.getFieldLength(j));
					}
					dataBlk.setFieldVal(2, new Integer(offset));
					//System.err.println ("DEBUG: data blockette references next blockette at byte = " + offset);
				} else dataBlk.setFieldVal(2, new Integer(0));
				// now write the byte representation of the blockette
				StringTokenizer blkTok = new StringTokenizer(dataBlk.toString(),"|");
				curField = 1;  // reuse curField
				while (blkTok.hasMoreTokens() && curField <= numFields) {
					String fldVal = blkTok.nextToken();
					byte[] fldBytes = BlocketteFactory.getBytes(dataBlk.getType(),curField,fldVal);
					//if (fldBytes == null) System.err.println ("DEBUG: fldBytes == null for curField " + curField);
					System.arraycopy(fldBytes,0,newByteBlock,byteBlockPtr,fldBytes.length);
					byteBlockPtr += fldBytes.length;
					curField++;
				}  // end foreach data blockette field...
			} // end for each data blockette...
		} // end blk type == 999...
		//////////////////////////////////////////////////////////////////////////////
		else {  // other (control header) blockette types
			//////////////////////////////////////////////////////////////////////////
			//
			// here is where we write the array size for the current blockette
			byte[] newSize = BlocketteFactory.getBytes(50,2,(new Integer(byteBlockPtr)).toString()); // 4 byte representation
			System.arraycopy(newSize,0,newByteBlock,3,4);  // write to array at offset 3
//System.err.println("DEBUG: recalc array size: " + byteBlockPtr);
			//
			// IF we are processing a truncated blockette, then perform some prep processing for its followup twin
			// currently stored in 'saveBlk'.
			if (twinning) {	
				curField = 3;  // the current blockette field we are looking at -- start at 3
                                int[] dictionaryFields = SeedDictionaryReferenceMap.lookupSourceFld(blk.getType());  // which fields reference dictionary blockettes?
				while (curField <= numFields) {  // foreach blockette field...	
					// we will loop only as far as the repeat group that was truncated, altering
					// fields to suit being the followup companion to the previous truncated blockette
					//
                                 /**DEACTIVATED -- July 2010**/
//					// first, check to see if this is a dictionary field...if so, we
//					// want to zero out the reference value
//                                        if (dictionaryFields != null) {
//                                            for (int i = 0; i < dictionaryFields.length; i++) {
//                                                if (curField == dictionaryFields[i]) {
//                                                    // this is a dictionary lookup field, zero out the reference
////System.err.println("DEBUG: saveBlk - set lookup field " + curField + " to zero");
//                                                    saveBlk.setFieldVal(curField, "0");
//                                                }
//                                            }
//                                        }
                                 /****************/
					// next, check to see if this is a repeating field...
					// check to see if this is the truncated group...
					// if so, we want to delete the first (truncGrpCount) field groups from saveBlk
					int rptField = saveBlk.getFieldRepeat(curField);
					int grpSize = 1;  // this is the default field incrementation
					if (rptField > 0) {
						// this is a repeating field, part of a repeat field group
//System.err.println("DEBUG: saveBlk - found repeat group starting at field " + curField);
						grpSize = (saveBlk.getFieldGrp(curField,0)).size();  // get the group size by grabbing the very first group
                                                // check to see that the grpSize value is at least 1
//System.err.println("DEBUG: saveBlk - groupsize = " + grpSize);
                                                if (grpSize == 0) grpSize = 1;
						if (curField > truncate) {  // we've reached the truncated group -- truncate == count field in question
//System.err.println("DEBUG: before group reduction: " + saveBlk);
                                                    for (int curGrp=0; curGrp < truncGrpCount; curGrp++) {  // foreach repeat group up to truncation count...
							saveBlk.deleteFieldGrp(curField,0);  // delete the head group off of the vector
							// group count at rptField is decremented automatically
                                                        //DEBUG
//if (curGrp < 1) {
//  System.err.println("DEBUG: start group reduction...");
//}
//if(truncGrpCount - curGrp < 5) {
//  System.err.println("DEBUG: end group reduction: " + saveBlk);
//}
                                                        //
						    }
                                                    // now that we are done with trimming this group, we keep everything
                                                    // else intact, so just exit the field loop
                                                    break;  // break from foreach field loop...
						}
					}
					curField += grpSize;  // increment to the next blockette field (jump past repeat group)
			
				}  // end foreach field
			
			}  // end if twinning
				
		}
		
		// if this byte array is too long for the logical record, then
		// put what we can into the logical record, stack it to the
		// logicalRecords vector, and begin writing to a fresh logical
		// record with the continuation flag.
		// if no logical record exists, then create a new one.
		int transcriptionPtr = 0;  // how far we have transcribed to the logicalRecord set
		if (logicalRecord == null) {
			startNewLogical(blk,false);  // create a new logical record
		}
		boolean continuation = false;  // indicates whether we are writing a continuation flag
		while (byteBlockPtr - transcriptionPtr > 0) {  // until we have written it all from the new byte block...
			// is there not enough room in the logical record?  (minimum 7 bytes left)
			// then push this logicalRecord to the stack and start a new one
			if (logicalRecordLength - logicalRecord.position < 7) {
				padLogical();  // append current logical record to storage vector
				startNewLogical(blk,continuation);  // creates new logicalRecord with continuation flag
			}
			continuation = true;  // any further iterations will be a continuation of the current blockette
			int transcriptionAmount = byteBlockPtr - transcriptionPtr;  // how much we will write this time?
			if (logicalRecordLength - logicalRecord.position < transcriptionAmount)
				transcriptionAmount = logicalRecordLength - logicalRecord.position;  // get minimum value, whatever room is left in logrec
			// fill out a logical record as much as possible
			System.arraycopy(newByteBlock,transcriptionPtr,logicalRecord.contents,logicalRecord.position,transcriptionAmount);
			transcriptionPtr += transcriptionAmount;
			logicalRecord.position += transcriptionAmount;
//System.err.println("DEBUG: continuation (" + transcriptionAmount + "), newByteBlock: " + (new String(newByteBlock)));
		}	
		// for a data record, add the waveform information to the end of the current logical record
		if (blk.getType() == 999) {
			//System.err.println("DEBUG: blk 999, logRec.pos = " + logicalRecord.position +
			//", waveformOffset = " + waveformOffset);
			if (waveformOffset > 0) {
				// pad the bytes between the last blockette entry and the start of the waveform info
				for (int i = logicalRecord.position; i < waveformOffset; i++) logicalRecord.contents[i] = recordPadding;
				logicalRecord.position = waveformOffset;
				Waveform waveFrm = blk.getWaveform();
				if (waveFrm == null) throw new BuilderException("could not find waveform in FSDH blockette when waveform offeset > 0");
				byte[] waveBytes = waveFrm.getEncodedBytes();
				//System.err.println("DEBUG: waveform byte length = " + waveBytes.length);
				if (logicalRecord.position + waveBytes.length > logicalRecord.contents.length) // check for exception
					throw new BuilderException("waveform data exceeds logical record boundaries (offset = " +
							logicalRecord.position + ", length = " + waveBytes.length + ")");
				// write waveform data to logical Record
				System.arraycopy(waveBytes,0,logicalRecord.contents,logicalRecord.position,waveBytes.length);
				// shift logicalRecord position to end of waveform data
				logicalRecord.position += waveBytes.length;
				// HINT: logical record will be automatically padded by ExportBuilder
			}
		}
		
		// clear the export mold
		exportMold.clear();
		
		// if we have a save Blockette from twinning/trancation, then call packToRecord() again
		if (saveBlk != null) {
//System.err.println("DEBUG: pushing saveBlk");
			exportMold.add(saveBlk);
			packToRecord();
		}
		
	}
	
	/**
	 * Check for script triggers.  Implement export script triggers here.
	 */
	protected boolean checkTrigger(String s) throws Exception {
		if (s.equals("A")) {
			// note record number of beginning of station metadata
			//stationBegins = logicalRecordCount;
		} else if (s.equals("B")) {
			// note record number of each new Blockette 50
			currentRecord = logicalRecordCount;
		} else if (s.equals("C")) {
			// note record number of beginning of data section
			dataBegins = logicalRecordCount;
			// set the starting value for data trace end time
			endTime = new Btime("1900,001,00:00:00.0000");
		} else if (s.equals("D")) {
			// note record number of data record
			currentRecord = logicalRecordCount;
		} else if (s.equals("E")) {
			// get station, channel, network
			// get start time, calculate end time
			// skip if startTime == null (means there are no more data records)
			//
			if (startTime != null) {
				Btime prevEndTime = endTime;  // save the previous end time
				endTime = getEndTime(startTime,numSamples,srFactor,srMult);
				// min/max times for blockette 70
				Btime b70Start = (Btime) volBlk70.getFieldVal(4);
				if (startTime.compareTo(b70Start) < 0) {
					volBlk70.setFieldVal(4,startTime);
				}
				Btime b70End = (Btime) volBlk70.getFieldVal(5);
				if (endTime.compareTo(b70End) > 0) {
					volBlk70.setFieldVal(5,endTime);
				}
				// data trace for blockette 74
				if (volBlk74 != null) {
					// for a current data trace, see whether we have reached the end of it.
					// if so, set the end time of the current blockette 74 to the previous
					// record's end time and trigger the generation of a new blockette 74.
					long timeDiff = startTime.diffSeconds(prevEndTime);
					if (timeDiff < -1 || timeDiff > 1) {  // if greater than a difference of 1 second, then this trace ends
						timeSpanBlockettes.add(volBlk74);     // add to vector of 74's
						volBlk74 = null;   // this will trigger the generation of a new blk 74
					} else {
						// still part of the same time series...
						volBlk74.setFieldVal(9,endTime);    // update the end time of the series
						volBlk74.setFieldVal(10,new Integer(currentRecord));   // update the ending record number
					}
				}
				if (volBlk74 == null) {
					// generate a new blockette 74 for a new time series
					volBlk74 = BlocketteFactory.createBlockette("074|0000|TEMP|OR|ARY|1900,001,00:00:00.0000|000000|01|1900,001,00:00:00.0000|000000|01|000|^|^|^|XX");
					volBlk74.setFieldVal(3,stationName);  // set the station name
					volBlk74.setFieldVal(4,locationId);   // set the location id
					volBlk74.setFieldVal(5,channelName);  // set the channel name
					volBlk74.setFieldVal(6,startTime);    // set the start time of the series
					volBlk74.setFieldVal(7,new Integer(currentRecord));   // set the starting record number
					volBlk74.setFieldVal(9,endTime);      // set the end time of the series (preliminary)
					volBlk74.setFieldVal(10,new Integer(currentRecord));  // set the ending record number (preliminary)
					volBlk74.setFieldVal(16,networkCode); // set the network code value
					// assign lookupId for synthesized blockettes (volume 0, category 0)
					volBlk74.setLookupId(synthSeqNum);
					synthSeqNum++;
				}
				startTime = null;  // reset start time to null so that we can detect if the subsequent pass found a data record
			}
		} else if (s.equals("F")) {
			// if there is a final blockette 74, add it to the
			// timeSpanBlockettes vector
			if (volBlk74 != null) timeSpanBlockettes.add(volBlk74);
			// push Blockette 70 to logical record
			// begin counting number of logical records generated
			timeSpanOffset = 0;   // initialize to zero
			timeSpanStart = currentRecord;  // mark our starting point
			if (timeSpanBlockettes.size() > 0) this.push(volBlk70);
		} else if (s.equals("G")) {
			// push Blockette 74s to logical records
			// record time series offset
			int blk74VecSize = timeSpanBlockettes.size();
			for (int i=0; i < blk74VecSize; i++) {
				volBlk74 = (Blockette) timeSpanBlockettes.get(i);
				this.push(volBlk74);
			}
			// record the number of records generated for time span control header
			timeSpanOffset = logicalRecordCount - timeSpanStart + 1;
		} else if (s.equals("H")) {
			// update blockette 11 with new station entry
			if (stationName == null) {
				throw new BuilderException("ERROR: no station names passed export filter.");
			}
			if (! stationName.equals(prevStationName)) {  // must be a new station name
				Vector stationVec = new Vector(2);
				stationVec.add(stationName);
				stationVec.add(new Integer(currentRecord));
				volBlk11.addFieldGrp(4,stationVec); // add a new entry to Blockette 11
			}
			prevStationName = new String(stationName);
		} else if (s.equals("I")) {
			// generate blockette 12
			// write to logical record -- order does not matter at this point
			// continue counting number of logical records generated
			volumeOffset = 0;
			volumeStart = logicalRecordCount;
			if (timeSpanBlockettes.size() > 0) {
				// if there are data records
				volBlk12.setFieldVal(4,volBlk70.getFieldVal(4));
				volBlk12.setFieldVal(5,volBlk70.getFieldVal(5));
				this.push(volBlk12);
			} else {
				// if there aren't data records
				volBlk12.setFieldVal(4,new Btime("1950,001,00:00:00.0000")); //verseed minimum acceptable time
				volBlk12.setFieldVal(5,new Btime("2500,001,00:00:00.0000"));
			}
		} else if (s.equals("J")) {
			// generate blockette 10
			// write to logical record -- order does not matter at this point
			volBlk10.setFieldVal(3,new Double(seedVersion));   // set the version number
			// get record length power of 2
			int compare = 1;
			int logRecExp = 0;
			while (compare < logicalRecordLength) {
				compare *= 2;
				logRecExp++;
			}
			volBlk10.setFieldVal(4,new Integer(logRecExp));  // logical record length (exponent)
			volBlk10.setFieldVal(5,volBlk12.getFieldVal(4));  // start time of volume
			volBlk10.setFieldVal(6,volBlk12.getFieldVal(5));  // end time of volume
			volBlk10.setFieldVal(7,new Btime());  // current time of volume writing
			volBlk10.setFieldVal(8,organizationName);  // originating organization
			volBlk10.setFieldVal(9,volumeLabel);  // volume label
			this.push(volBlk10);
			// write blockette 11 to logical record
			this.push(volBlk11);
			// find the record volume header offset (the number of records in the volume header)
			volumeOffset = logicalRecordCount - volumeStart + 1;
		} else if (s.equals("K")) {
			// adjust record numbers in blockettes 11, 12, and 74
			//
			// the station metadata numbers will be offset by the volume control header offset
			int numStations = ((Integer) volBlk11.getFieldVal(3)).intValue();
			for (int i = 0; i < numStations; i++) {
				int recordNum = ((Integer) volBlk11.getFieldVal(5,i)).intValue();
				recordNum += volumeOffset;
				volBlk11.setFieldVal(5,i,new Integer(recordNum));
			}
			// the time span header record number will be located where the data records
			// started in simulation (insertion) *plus* volume control header offset
			volBlk12.setFieldVal(6,new Integer(dataBegins + volumeOffset));
			// the data time series starting and ending record numbers will be
			// offset from their current values by the volume control header
			// offset *plus* the time series header offset.
			int numTimes = timeSpanBlockettes.size();
			for (int i = 0; i < numTimes; i++) {
				volBlk74 = (Blockette) timeSpanBlockettes.get(i);
				
				int recordNum = ((Integer) volBlk74.getFieldVal(7)).intValue();  // start record
				recordNum += volumeOffset + timeSpanOffset;
				volBlk74.setFieldVal(7,new Integer(recordNum));
				
				recordNum = ((Integer) volBlk74.getFieldVal(10)).intValue(); // end record
				recordNum += volumeOffset + timeSpanOffset;
				volBlk74.setFieldVal(10,new Integer(recordNum));
			}
		} else if (s.equals("L")) {
			// reset simulation instance variables
			//stationBegins = 0;
			dataBegins = 0;
			currentRecord = 0;
			timeSpanBlockettes = new Vector(8,8);
			// create starter Blockette 10
			volBlk10 = BlocketteFactory.createBlockette("010|0000|02.3|12|1900,001,00:00:00.0000|1900,001,00:00:00.0000|1900,001,00:00:00.0000|TEMPORARY|^");
			// assign lookupId for synthesized blockettes (volume 0, category 0)
			volBlk10.setLookupId(synthSeqNum);
			synthSeqNum++;
			// create starter Blockette 11
			volBlk11 = BlocketteFactory.createBlockette("011|0000|000|^|^");
			// assign lookupId for synthesized blockettes (volume 0, category 0)
			volBlk11.setLookupId(synthSeqNum);
			synthSeqNum++;
			// create starter Blockette 12
			volBlk12 = BlocketteFactory.createBlockette("012|0000|0001|1900,001,00:00:00.0000|1900,001,00:00:00.0000|000000");
			// assign lookupId for synthesized blockettes (volume 0, category 0)
			volBlk12.setLookupId(synthSeqNum);
			synthSeqNum++;
			// create starter Blockette 70
			volBlk70 = BlocketteFactory.createBlockette("070|0000|P|2500,001,00:00:00.0000|1900,001,00:00:00.0000");
			// assign lookupId for synthesized blockettes (volume 0, category 0)
			volBlk70.setLookupId(synthSeqNum);
			synthSeqNum++;
			// null Blockette 74 reference
			volBlk74 = null;
			prevStationName = new String("");
			logicalRecordCount = 1;
		} else if (s.equals("M")) {
			// reset logical record count since file transcription will follow
			logicalRecordCount = 1;
		} else if (s.equals("10")) {
			// write internal blockette 10 to logical record
			this.push(volBlk10);
		} else if (s.equals("11")) {
			// write internal blockette 11 to logical record
			this.push(volBlk11);
		} else if (s.equals("12")) {
			// write internal blockette 12 to logical record
			// but only if there are data records in this volume
			if (timeSpanBlockettes.size() > 0) this.push(volBlk12);
		} else if (s.equals("70")) {
			// write internal blockette 70 to logical record
			// but only if there are data records in this volume
			if (timeSpanBlockettes.size() > 0) this.push(volBlk70);
		} else if (s.equals("74")) {
			// write internal blockette 74s to logical record
			// but only if there are data records in this volume
			int numTimes = timeSpanBlockettes.size();
			for (int i = 0; i < numTimes; i++) {
				volBlk74 = (Blockette) timeSpanBlockettes.get(i);
				this.push(volBlk74);
			}
		} else return false;  // no trigger, return false
		return true;  // we have found a trigger, so return true
	}
	
	/**
	 * Finishing actions for exporting to SEED.  Nothing currently performed here.
	 */
	protected void volumeFinish() throws BuilderException {
		// nothing performed here
		try {
		} catch (Exception e) {
			throw new BuilderException();
		}
	}
	
	/**
	 * Start a new logical record.
	 * Preface record with sequence number and record type/quality code.
	 * The SeedObject parameter is one that defines the type of record this is.
	 * The continuation boolean triggers the marking of the record as a continuation record.
	 */
	protected void startNewLogical(SeedObject obj, boolean continuation) throws Exception {
		// if SeedObject is null, then we create a blank logical record
		//
		// our SeedObject in this class is actually a Blockette
		Blockette blk = null;
		if (obj != null) blk = (Blockette) obj;  // cast object as Blockette
		logicalRecord = new LogicalRecord();
		// preface the logical record with the sequence number...
		// a 6-digit numeric formatted string
		//System.err.println("DEBUG: start new logical record #" + logicalRecordCount);
		String sequenceNum = BlocketteFactory.formatDecimal(12,6,(new Integer(logicalRecordCount)).toString());
		if (sequenceNum.length() != 6) {
			throw new BuilderException("improper length for logical record sequence number: " + sequenceNum);
		}
		System.arraycopy(sequenceNum.getBytes(),0,logicalRecord.contents,0,6);
		// now add the record type/quality code
		//
		// first, let's jump ahead to the 'reserved' byte and mark if this is a continuation record
		if (continuation) logicalRecord.contents[7] = '*';
		else logicalRecord.contents[7] = ' ';
		// what kind of record are we building?
		String recType = null;
		if (obj != null) recType = blk.getCategory();
		if (recType == null) {
			// blank record uses a space character
			logicalRecord.contents[6] = ' ';
		} else if (recType.equals("Volume Index")) {
			logicalRecord.contents[6] = 'V';   // place record type code in byte 6
		} else if (recType.equals("Abbreviation Dictionary")) {
			logicalRecord.contents[6] = 'A';
		} else if (recType.equals("Station")) {
			logicalRecord.contents[6] = 'S';
		} else if (recType.equals("Time Span")) {
			logicalRecord.contents[6] = 'T';
		} else if (recType.equals("Data Record")) {
			String dataQualStr = blk.toString(2);  // data quality field in Blockette 999 (FSDH)
			logicalRecord.contents[6] = (byte) dataQualStr.charAt(0);  // reset byte 6 to first char
			if (dataQualStr.length() > 1) {
				logicalRecord.contents[7] = (byte) dataQualStr.charAt(1);  // reset reserved byte 7 to second char if present
			} else {
				logicalRecord.contents[7] = (byte) 32;  // else byte 7 defaults to space
			}
		} else throw new BuilderException ("Unknown category of blockette: " + recType);
		// adjust the logical record pointer
		logicalRecord.position = 8;
	}
	
	// private methods
	
	/**
	 * Generate the export script array from the script string.
	 */
	private void genScriptArray() {
		StringTokenizer expTok = new StringTokenizer(scriptString,",");
		exportScript = new String[expTok.countTokens()];
		int i = 0;
		while (expTok.hasMoreTokens()) exportScript[i++] = expTok.nextToken();
	}
	
	
	// instance variables
        
        private int twinningTrigger = 9900;  // number of characters in blockette before (coefficient) splitting must begin
	
	private float seedVersion = BlocketteFactory.getDefaultVersion();// the version of SEED we are writing
	private byte[] newByteBlock = null;  // new bytes get written to this temporary array before going to logical record array
	private String organizationName = null;  // originating organization to get written to SEED volume
	private String volumeLabel = null;   // volume label
	private String scriptString;    // string to be translated to a builder script
	
	//private int stationBegins = 0;  // record number where station header information begins
	private int dataBegins = 0;     // record number where data records begin
	private int currentRecord = 0;  // record current record number
	private int volumeStart = 0;    // marker for record number that volume headers start at
	private int volumeOffset = 0;   // record count of volume headers
	private int timeSpanStart = 0;  // marker for record number that time span headers start at
	private int timeSpanOffset = 0; // record count of time span headers
	private int synthSeqNum = 0;  // sequence number for synthesized blockette IDs (use category 0)
	private Vector timeSpanBlockettes = null;  // store all blockette 74s here
	private Blockette volBlk10 = null;  // store generated blockette 10
	private Blockette volBlk11 = null;  // store generated blockette 11
	private Blockette volBlk12 = null;  // store generated blockette 12
	private Blockette volBlk70 = null;  // store generated blockette 70
	private Blockette volBlk74 = null;  // store generated blockette 74
	//private boolean datalessMode = false;  // flags builder to make dataless SEED
	//private boolean miniMode = false;      // flags builder to make miniSEED
	
	// temporary variables for sampling incoming metadata
	private String stationName = null;
	private String prevStationName = null;
	private String locationId = null;
	private String channelName = null;
	private String networkCode = null;
	private int numSamples = 0;
	private int srFactor = 0;
	private int srMult = 0;
	private Btime startTime = null;           
	private Btime endTime = null;

    private final int defaultLogicalRecordLength = 4096;
	
}
