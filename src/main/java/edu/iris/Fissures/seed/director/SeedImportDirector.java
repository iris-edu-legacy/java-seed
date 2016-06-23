package edu.iris.Fissures.seed.director;

import edu.iris.Fissures.seed.builder.*;
import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.exception.*;
import java.io.*;



/**
 This class is the Director for reading SEED file streams into
 the SEED object pool.  It reads in SEED records and contacts
 the SEED Builder to construct the appropriate blockette or
 data record construct.
 <p>
 Look to its abstract superclass, ImportDirector, to get details
 on generic methods run through this class.
 @author Robert Casey, IRIS DMC
 @version 7/5/2013
 */

public class SeedImportDirector extends ImportDirector {
    
    /**
     * Create a SEED import director.
     */
    public SeedImportDirector () {
        super();
        maxRecordLength = 65536;
    }
    
    /**
     * Create a SEED import director and assign the indicated builder.
     */
    public SeedImportDirector (ObjectBuilder builder) {
        super(builder);
        maxRecordLength = 65536;
    }
    
    // public methods
    
    /**
     * Get the length of the next SEED record currently in the read buffer. 
     */
    public int getRecLen() throws Exception {
        // check for fixed record length first...return that value if set
        if (fixedRecordLength > 0) return fixedRecordLength;

        // first, let's try the most common record lengths, based on the
        // 'current' record type
        char recordType = (char) readBuffer.buffer[6];
        if (recordType == 'D' || recordType == 'R' || recordType == 'Q' || recordType == 'M') {
            // data records are typically 512, 1024, and 4096 bytes in length
            int[] tryLen = {512,1024,4096};
            for(int i = 0; i < tryLen.length; i++)
                if (checkRecordBoundary(readBuffer.buffer,tryLen[i]))
                    return tryLen[i];
        }
        if (recordType == 'V' || recordType == 'A' || recordType == 'S' || recordType == 'T') {
            // header control records are nearly always 4096 bytes in length
                if (checkRecordBoundary(readBuffer.buffer,4096))
                    return 4096;
        }
        // this is very conservative record length scanning
        // use as a fallback only
        int recLen = 256;
        int bufferLength = readBuffer.length();  // sample the length of record currently present
        // loop by powers of two until we find a record boundary or reach the max record length
        while (recLen < bufferLength) {
            //System.err.println("DEBUG: check recLen=" + recLen + ", bufferLength=" + bufferLength);
            if (checkRecordBoundary(readBuffer.buffer,recLen)) break;
            recLen *= 2;
        }
        if (recLen > bufferLength || recLen > maxRecordLength)
            throw new SeedInputException("unable to determine record length (recLen=" + recLen +
                    ", maxRecordLength=" + maxRecordLength +
                    ", bufferLength=" + bufferLength + ")");
        return recLen;  // recLen could very well equal bufferLength



    }
    
    // private/protected methods
    
    /**
     * Construct a SEED Object from the import record.
     * Order the assigned builder to construct an object from the current record array
     * starting at the index indicated by instance variable 'recordOffset'.
     * Return the number of bytes consumed in the build.
     */
    public int build() throws Exception {
        if (builder == null) throw new BuilderException("Director has not been assigned a builder");
        // set builder state values
        boolean continuationFlag = false;
        if (recordOffset == 0) {
            //System.err.println("DEBUG: director - start of new record...recordOffset == 8");
            builder.setRecordBeginFlag(true);  // we are at the beginning of a record...flag this
            continuationFlag = (record[7] == '*');  // continuation flag (boolean check for asterisk)
            recordOffset = 8; // forward the record offset index to 8 to compensate for record identifier block
        }
        if (record == null) {
            throw new BuilderException("import record is null");
        }
        if (record.length <= recordOffset) {
            throw new BuilderException("insufficient import record length");
        }
        if (recordOffset < 0) {
            throw new BuilderException("record offset less than 0");
        }
        //System.err.println("DEBUG: build():recordOffset=" + recordOffset + ", record=" + record);
        // get the record section that we are passing to the builder
        byte[] recordSection = new byte[record.length-recordOffset];  // remainder of the record length
        // copy record started at recordOffset into recordSection for the remainder of the record length
        System.arraycopy(record,recordOffset,recordSection,0,recordSection.length);

        //System.err.println("DEBUG: director.build(): " + new String(recordSection));

        // call the registered builder and get the number of bytes consumed
        builder.setRecordType(record[6]);               // can represent data quality as well
        builder.setContinuationFlag(continuationFlag);
        if (debug) System.err.println("DEBUG: Director - set builder ContinuationFlag to: " + continuationFlag);
        if (! continuationFlag) {
        	builder.setLargeCoeffFlag(false);  // assert reset of flag only when we don't have a continuation record onset
        	if (debug) System.err.println("DEBUG: Director - set builder ContinuationFlag to FALSE");
        }
        


        // if this record section starts out with blanks, then we end parsing the record here.
        int i;
        for (i = 0; i < 5 && i < recordSection.length; i++) {
            if (recordSection[i] != 32 && recordSection[i] != 0) break;
        }
        int numBytes = 0;
        if (i == 5 || i == recordSection.length) {
            // too many blanks...end of record
            //System.err.println("DEBUG: blanks found -- skip to end of record");
            numBytes = recordSection.length;
            // so that it doesn't appear that we have created a new object, command the
            // builder to blank-out it's current object holder
            builder.removeCurrent();
        } else {
            //
            // build a blockette from the record section
            //

            // check for very long response blockette, broken into more than one blockette of the same type
            // and stage number...trigger large coefficient flag so that we append this data to the current
            // builder blockette
            bType = 0;
            stageNum = -1;
            if (!continuationFlag && recordSection.length > 9) {  // record must be 10 or greater to do this
                Integer bTypeInt = null;
                if (record[6] == 'D' || record[6] == 'R' || record[6] == 'M' || record[6] == 'Q') {
                    // a data record
                    bType = 999;
                } else {
                    // a header record
                    bTypeInt = new Integer(new String(recordSection,0,3).trim());  // get the first three bytes as a String
                    if (bTypeInt != null) {
                      bType = bTypeInt.intValue();
                    }
                }
                if (bType > 52 && bType < 70) {
                    //System.err.println("DEBUG: director - response candidate");
                    int stageOffset = 0;
                    if (bType == 53 || bType == 54 || bType == 62) {
                        stageOffset = 8;
                    } else if (bType == 55 || bType == 56 || bType == 61) {
                        stageOffset = 7;
                    }
                    //System.err.println("DEBUG: director - bType = " + bType + ", stageOffset = " + stageOffset);
                    if (stageOffset > 0) {
                        Integer stageNumInt = new Integer(new String(recordSection,stageOffset,2).trim());  // get the stage number bytes
                        stageNum = stageNumInt.intValue();
                        if (stageNum > -1) {
                            // compare the current btype and stage number to the previous
                            //System.err.println("DEBUG: director - compare to prevBtype = " + prevBType);
                            if (bType == prevBType && stageNum == prevStageNum) {
                                // if we have the same blockette, same stage, we have to assume that we have a blockette
                                // with a large number of coefficients.  Instead of creating a new blockette, we want to
                                // append the data in this new blockette to the previous blockette object.
                                if (debug) System.err.println("DEBUG: director found large coefficient continuation: type=" + bType + ", stage=" + stageNum);
                                builder.setLargeCoeffFlag(true);
                                if (debug) System.err.println("DEBUG: Director - set builder ContinuationFlag to FALSE");
                            }
                        }
                    }
                }
                prevBType = bType;  // remember the blockette type we just looked at
                prevStageNum = stageNum;  // remember the stage number of this blockette (could be -1)
            }
            //if (recordSection != null) System.err.println("DEBUG: director calling builder.build()");
            // build the blockette and get number of bytes read -- fed to ImportDirector.read()
            numBytes = builder.build(recordSection);   
            if (debug) System.err.println("DEBUG:  director: consumed " + numBytes + " bytes");
        }
        if (numBytes < 0) throw new BuilderException("number of bytes is less than zero");
        return numBytes;  // return the number of bytes consumed
    }
    
    /**
     * Look for the record boundary at the indicated offset.
     * Return true if a new record is detected at the offset byte in the buffer.
     */
    private boolean checkRecordBoundary(byte[] buffer, int offset) throws Exception {
        char recordType = (char) buffer[offset+6];
        char contFlag = (char) buffer[offset+7];
        byte[] tempTime = new byte[10];
        int i;
        // the first six bytes should be ascii digits
        // note: relaxing requirements a bit and allowing nulls
        for (i = 0; (i < 6 &&(isDigit(buffer[offset+i]) || buffer[offset+i] == 0)); i++);
        //if (i < 6) System.err.println("DEBUG: i<6: buffer[" + offset +
        //	"+" + i + "] fails: (" + (char) buffer[offset+i] + ")");
        //
        if (recordType == 'D' || recordType == 'R' || recordType == 'Q' || recordType == 'M') {  // is a data record?
            if (i == 6 && buffer[offset+7] == 32) {
//System.err.println("DEBUG: (1) data record header at offset: " + offset);
                int start = 8;
                // special case for PFO_T, if it matches, then offset + 5 bytes
                if ( (new String(buffer,offset+8,5)).compareTo("PFO_T") == 0 ) start = 13;
                // scan station, location, channel, and network fields
                for (i = start; i < 20; i++) {
                    // they must all be either space or capital-alpha or digit
                    if (buffer[offset+i] != 32 &&
                            (buffer[offset+i] < 48 ||
                                    buffer[offset+i] > 90)) return false; // failed check
                }
                //System.err.println("DEBUG: (2) passed alpha/digit check");
                // do a time string check to see if we have reasonable values
                System.arraycopy(buffer,offset+20,tempTime,0,10);
                Btime timeVal = new Btime(tempTime);
                if (timeVal.getError().length() > 0) return false;  // improper time string
//System.err.println("DEBUG: (3) passed time check");
            } else if (i == 6 && buffer[offset+7] == '*') {
                // did they put a continuation field here?
                // we can't accept this, but let's print a warning
                System.err.println("WARNING: possible data record of type '" + recordType
                        + "' with an asterisk continuation flag (should be SPACE)...rejecting this as a viable record");
                return false;  // do not accept this
            } else return false; // this did not start with six ascii digits followed by a space
            return true;   // passed the FSDH test
        } else if (recordType == 'V' || recordType == 'A' ||
                recordType == 'S' || recordType == 'T') {  // is this a control header record?
//System.err.println("DEBUG: control header: " + recordType + " at offset " + offset);
            if ( ( contFlag == 32 && 
                    (isDigit(buffer[offset+8]) || buffer[offset+8] == 32) &&
                    (isDigit(buffer[offset+9]) || buffer[offset+9] == 32) &&
                    isDigit(buffer[offset+10]) &&
                    (isDigit(buffer[offset+11]) || buffer[offset+11] == 32) &&
                    (isDigit(buffer[offset+12]) || buffer[offset+12] == 32) &&
                    (isDigit(buffer[offset+13]) || buffer[offset+13] == 32) &&
                    isDigit(buffer[offset+14])
            ) ||
            ( contFlag == '*' &&
                    isPrintable(buffer[offset+8]) && isPrintable(buffer[offset+9])
                    && isPrintable(buffer[offset+10]) && isPrintable(buffer[offset+11])
                    && isPrintable(buffer[offset+12]) && isPrintable(buffer[offset+13])
                    && isPrintable(buffer[offset+14])
            )
            ) return true;
        } else if (recordType == 32) {   // is this a 'blank' record?
//System.err.println("DEBUG: recordType == 32 at offset " + offset + "...check for 'blank' record");
//System.err.println("DEBUG: looking at: >" + (new String(buffer,0,48)) + "<");
            if (i < 6) {
                // also allow there to be just spaces for the first 6 chars.
                for (i = 0; i < 6 && buffer[offset+i] == 32; i++);
            }
            if (i == 6) {
//System.err.println("DEBUG: first 6 chars are digits");
                for (i = 7; i < 48 && buffer[offset+i] == 32; i++);  // up to 48 should all be spaces
                if (i == 48) {
                    //System.err.println("DEBUG: this IS a blank record");
                    // this IS a blank record.  return true so that just the
                    // minimum offset is incremented...the resulting blank record
                    // will be ignored...we must move forward by small increments
                    // because of possible odd byte counts of spaces at the end
                    // of a data record block.
                    return true;
                }
//System.err.println("DEBUG: this is not a blank record");
            }
        }
//System.err.println("DEBUG: not recognizable as a SEED record");
        return false;  // this is not a recognizable SEED record
    }
    
    /**
     * Return true if the provided byte represents an ASCII digit character.
     */
    private boolean isDigit (byte ascii) {
        if (ascii > 47 && ascii < 58) return true;
        return false;
    }
    
    /**
     * Return true if the provided byte represents an ASCII alphabet character.
     */
//    private boolean isAlpha (byte ascii) {
//        if (ascii > 64 && ascii < 91) return true;   // matches to caps
//        if (ascii > 97 && ascii < 123) return true;  // matches to underscore
//        return false;
//    }
    
    /**
     * Return true if this is a printable character.
     */
    private boolean isPrintable (byte c) {
        if (c > 31 && c < 127) return true;
        return false;
    }
    
    
    /**
     * Test method.
     * Accept a SEED filename to read as the first argument.
     * Accept another filename for writing ASCII output as a second argument.
     * To trigger object storage and retrieval in memory, there must be just one argument.
     * To trigger printing of SEED info without object storage, provide the
     * output file argument.
     * To trigger object storage and retrieval using disk storage (serialization),
     * make the output file parameter "object_store.ser".
     * Four other parameters may be added that allow filtering to a specific
     * station, channel, start time, and end time.
     */
    public static void main (String[] args) {
        int runmode = 0;  // what mode we are running in.  0 = 'store'
        try {
            if (args.length == 0) {
                System.err.println("usage: SeedImportDirector <input_file> [<output_file>] [STN] [CHN] [START] [END]");
                System.exit(1);
            }
            String fileName = new String(args[0]);
            if (fileName.length() == 0) {
                System.err.println("ERROR: empty first argument");
                System.exit(1);
            }
            String fileOut = new String("");;
            if (args.length > 1) {
                runmode = 1;   // mode 1 = print
                fileOut = new String(args[1]);
                if (fileOut.length() == 0) {
                    System.err.println("ERROR: empty second argument");
                    System.exit(1);
                }
                // check for serialization mode
                if (fileOut.equals("object_store.ser")) runmode = 0;  // resume object storage mode
            }
            // check for filter parameters
            BuilderFilter buildFilter = null;
            if (args.length > 2) {  // station argument...minimum need for filtering
                System.out.println("instantiating builder filter...");
                buildFilter = new SeedBuilderFilter();
                buildFilter.addParameter("station",args[2]);
            }
            if (args.length > 3) {  // channel argument
                buildFilter.addParameter("channel",args[3]);
            }
            if (args.length > 4) {  // start time argument
                // create a Btime object with the argument to 'clean things up'
                Btime bTime = new Btime(args[4]);
                buildFilter.addParameter("start_time",bTime.toString());
            }
            if (args.length > 5) {  // end time argument
                // create a Btime object with the argument to 'clean things up'
                Btime bTime = new Btime(args[5]);
                buildFilter.addParameter("end_time",bTime.toString());
            }
            //
            //  create a Builder
            System.out.print("instantiating builder...");
            SeedObjectBuilder seedBuilder = null;
            if (fileOut.equals("object_store.ser")) {
                seedBuilder = new SeedObjectBuilder(fileOut);
                System.out.println("(disk mode)");
            } else {
                seedBuilder = new SeedObjectBuilder();
                System.out.println("(memory mode)");
            }
            // check to see if we need to register the filter with the builder
            if (buildFilter != null) {
                // get the filter parameters for printing
                buildFilter.iterate();
                String[] keyAndValue = null;
                while ((keyAndValue = buildFilter.getNext()) != null) {
                    System.out.println("filter parameter: " + keyAndValue[0] + " = " + keyAndValue[1]);
                }
                System.out.println("registering filter with builder...");
                // register filter
                seedBuilder.registerFilter(buildFilter);
            }
            // create a Director
            System.out.println("instantiating director and registering builder...");
            SeedImportDirector seedDirector = new SeedImportDirector(seedBuilder);
            // read the SEED data
            System.out.println("opening SEED file " + fileName + " for reading...");
            DataInputStream seedIn = new DataInputStream(new FileInputStream(fileName));
            if (runmode == 0) {
                System.out.println("accessing SEED data for object generation...");
                seedDirector.construct(seedIn);
            } else {
                System.out.println("accessing SEED data for printing to output file " + fileOut + "...");
                System.out.println("opening output file...");
                DataOutputStream printOut = new DataOutputStream(new FileOutputStream(fileOut));
                System.out.println("printing SEED info...");
                seedDirector.construct(seedIn,printOut,false);
            }
            System.out.println("done.");
            if (runmode == 1) {  // for print mode, exit here.
                System.exit(0);  // exit sample test
            }
            System.out.println("fetching object container...");
            // using long reference to this container class because normally the director does not need
            // to be aware of it...we are simulating an application, though, so we will use long references
            // here.
            SeedObjectContainer container = 
                (SeedObjectContainer) seedBuilder.getContainer();
            System.out.println("going through object container to display everything we have there...");
            for (int i = 1; i < 6; i++) {  // go through the five types
                int numElem = container.iterate(1,i); // volume 1, type i
                if (numElem > 0) {
                    // same idea with the long Blockette reference.
                    Blockette nextBlockette = null;
                    while ((nextBlockette = (Blockette) container.getNext()) != null) {
                        System.out.println(nextBlockette.toString());
                        int numChild1 = nextBlockette.numberofChildBlockettes();  // number of Rank 1 children
                        if (numChild1 > 0) {
                            for (int j = 0; j < numChild1; j++) {
                                Blockette child1Blk = null;
                                SeedObject childObj = nextBlockette.getChildBlockette(j);
                                if (childObj == null) {
                                    System.out.println("++++> null child blockette encountered");
                                    continue;
                                }
                                if (childObj instanceof SeedObjectProxy) {
                                    child1Blk = (Blockette) container.get(childObj.getLookupId());
                                }  else {
                                    child1Blk = (Blockette) childObj;
                                }
                                System.out.println("++++> " + child1Blk.toString());
                                if (child1Blk.getType() == 52) {  // demonstrate abbreviation lookup for Blockette 52's
                                    Blockette blk52 = child1Blk;  // create a synonym for clarity
                                    int[] refFields = {5,6,8,9,16};
                                    for (int r=0;r<refFields.length;r++) {
                                        System.out.print("++++|++++> Dictionary lookup for field " + refFields[r] + "(" +
                                                blk52.getFieldName(refFields[r]) + "): ");
                                        // get field value
                                        int refVal = Integer.parseInt(blk52.toString(refFields[r]));
                                        // get lookup id based on field value
                                        int lookupId = blk52.getDictionaryLookup(refVal);
                                        // ask the object container for the blockette matching this id
                                        if (lookupId > 0) {
                                            Blockette dictBlk = 
                                                (Blockette) container.get(lookupId);
                                            if (dictBlk != null) {
                                                // we have found the dictionary blockette, let's print the contents
                                                System.out.println(dictBlk.toString());
                                            } else {
                                                System.out.println("-- dictionary blockette not found --");
                                            }
                                        } else {
                                            System.out.println("-- lookupId == 0 --");
                                        }
                                    }
                                }
                                int numChild2 = child1Blk.numberofChildBlockettes();  // number of Rank 2 children
                                if (numChild2 > 0) {
                                    for (int k = 0; k < numChild2; k++) {
                                        Blockette child2Blk = null;
                                        childObj = child1Blk.getChildBlockette(k);
                                        if (childObj == null) {
                                            System.out.println("++++|++++> null child blockette encountered");
                                            continue;
                                        }
                                        if (childObj instanceof SeedObjectProxy) {
                                            child2Blk = (Blockette) container.get(childObj.getLookupId());
                                        } else {
                                            child2Blk = (Blockette) childObj;
                                        }
                                        System.out.println("++++|++++> " + child2Blk.toString());
                                        // no more child ranks past this...
                                    }
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("There are 0 type " + i + " elements.");
                }
            }
            System.out.println("done.");
            System.exit(0);  // exit sample test
        } catch (Exception e) {
            System.out.println("Caught exception: " + e);
            ((Throwable) e).printStackTrace();
        }
    }


    //////////////////////////////////////
    // Instance Variables
    protected int bType = 0, prevBType = 0;
    protected int stageNum = 0, prevStageNum = 0;
    
    private boolean debug = false;   // set to true if you want debug output to be displayed


}


