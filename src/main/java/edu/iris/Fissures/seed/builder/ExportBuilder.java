package edu.iris.Fissures.seed.builder;

import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.exception.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
 Generic Builder class for exporting objects from the Object Container
 to a particular file or data stream format.
 @author Robert Casey, IRIS DMC
 @version 7/26/2002
 */

public abstract class ExportBuilder {
    
    /**
     * Create an export builder.
     */
    public ExportBuilder() {
    }
    
    /**
     * Create an export builder connected to an output stream.
     */
    public ExportBuilder(OutputStream outStream) throws BuilderException {
        this();
        open(outStream);
    }
    
    // public methods
    
    /**
     * This method returns the type of builder we are in the form of a unique string.
     */
    public String getType() throws BuilderException {
        if (builderType.equals("UNKNOWN")) {
            throw new BuilderException("builder type is UNKNOWN");
        }
        return builderType;
    }
    
    /**
     * Determines if padding is enabled or not.
     * @return true if padding is enabled, false otherwise.
     */
    public boolean isPaddingEnabled() {
        return padEnabledFlag;
    }

    
    /**
     * Open output stream for export.
     */
    public void open(OutputStream outStream) throws BuilderException {
        if (outStream == null) throw new BuilderException("attempted to open null OutputStream reference");
        outputStream = outStream;  // connect this builder to the provided output stream
    }
    
    /**
     * Close output export stream.
     */
    public void close() {
        outputStream = null;       // disconnect this builder from the current output stream
    }
    
    /**
     * Set the logical record length of the output volume.
     */
    public void setLogicalRecLen(int len) {
        logicalRecordLength = len;  // set the logical record length
        if (physicalRecordLength < logicalRecordLength) {
            physicalRecordLength = logicalRecordLength;  // physical cannot be less than logical
        }
    }
    
    /**
     * Sets if padding is enabled or not.
     * @param b true if padding is enabled, false otherwise.
     */
    public void setPaddingEnabled(boolean b) {
        padEnabledFlag = b;
    }

    
    /**
     * Set the physical record length of the output volume.
     */
    public void setPhysicalRecLen(int len) {
        physicalRecordLength = len;
        if (physicalRecordLength < logicalRecordLength) {
            logicalRecordLength = physicalRecordLength;  // logical cannot be greater than physical
        }
    }
    
    /**
     * Set the word-order flag for the output volume
     * (<b>NOT CURRENTLY IMPLEMENTED</b>).
     */
    public void setVaxOrder(boolean flag) {
        // flag is set to true for VAX/8086 word order in output, false is for Sun/68000 word order
        // *** NOT CURRENTLY IMPLEMENTED ***
        vaxOrder = flag;
    }
    
    /**
     * Trigger start of a new volume.
     */
    public void startVolume() throws BuilderException {
        if (logicalRecordLength <= 0) {
            throw new BuilderException("invalid logical record length: " + logicalRecordLength);
        }
        if (physicalRecordLength <= 0) {
            throw new BuilderException("invalid physical record length: " + physicalRecordLength);
        }
        // physicalRecordLength must be a multiple of the logical record length
        int i;
        for (i = 0; i * logicalRecordLength < physicalRecordLength; i++);
        if (i * logicalRecordLength > physicalRecordLength) {
            throw new BuilderException("physicalRecordLength (" + physicalRecordLength +
                    ") not a multiple of logicalRecordLength (" + logicalRecordLength + ")");
        }
        logicalPerPhysical = physicalRecordLength / logicalRecordLength;  // always 1 or greater...
        logicalRecords = new Vector(8,8);   // set up logical record vector
        scriptCursor = 0;
        nestingDepth = 0;
    }
    
    /**
     * Get next script element.
     * Return the next object type in a scripted series object type numbers, representing
     * a concrete subclass of this builder.
     * The script is a String array, with each element containing
     * either a non-zero integer value, a trigger string, or a control character.<br>
     * -- The a positive integer value corresponds to an object type.  This value is
     *    returned to the caller.<br>
     * -- A -1 value indicates that there are no more script elements.  Return this value
     *    to the caller.  The caller should properly interpret this as end of script.<br>
     * -- A -2 value indicates end of pass, which means that there may be
     *    more script elements,
     *    but that the director providing the source data should reset its object sequencer
     *    to release all of its objects again for build() calls.  In Director parlance, its
     *    ExportTemplate has all of its indexes reset to 1.  A -2 may be needed by the
     *    concrete builder to do a two-pass or multi-pass compilation of object data.<br>
     * -- Brackets ('[' and ']') signify that a group of values in between constitute
     *    a single output byte array element.  Brackets cannot be nested.<br>
     * -- Parentheses ('(' and ')') signify that a loop occurs over the elements
     *    between them until no more objects can be drawn out of the loop.
     *    These loops can be nested.<br>
     * -- A less-than symbol ('<') signifies that the logical record should be ended, with
     *    character padding added to fill out the logical record.<br>
     * -- A double less-than ('<<') signifies that the physical record should be ended, with
     *    character padding added to fill out the physical record.  This also forces a write
     *    to the export stream.<br>
     * -- A caret ('^') signals 'pen-up', which means that calls to writeRecord() will not
     *    result in logicalRecord and physicalRecord data going to the OutputStream.<br>
     *    This is typically used for build simulations to get information on the 
     *    volume's structure.<br>
     * -- A lower-case vee ('v') character signals 'pen-down', which resets 
     *    writeRecord() to output
     *    data in the logical and physical records to the OutputStream.  This is the
     *    default mode.<br>
     * -- Other strings can be used to trigger actions between object type acquisition.
     *    These actions must be implemented by the checkTrigger() method in the concrete version
     *    of ExportBuilder.<br>
     * The script represents the pattern of objects required by this builder to construct an
     * output stream of bytes of the intended data format.
     */
    //System.err.println("DEBUG: called builder.getNext()");
    public int getNext() throws Exception {
        boolean searching = true;  // signal true to continue script search loop
        String scriptElem = null;  // this stores our script element
        while (searching) {
            if (scriptCursor == exportScript.length) {
                return -1;  // no more elements...signal this with a -1 return value
            }
            scriptElem = exportScript[scriptCursor]; // get the next script notation from the script array
            //System.err.println("DEBUG: working on scriptElem '" + scriptElem + "'");
            // check for control characters and triggers
            if (scriptElem.equals("(")) {
                // start of loop
                // increase our loop nesting by one
                nestingDepth++;
                if (scriptNesting.length == nestingDepth+1) {
                    throw new BuilderException("scriptNesting array not allocated enough space");
                }
                if (nestingScore.length == nestingDepth+1) {
                    throw new BuilderException("nestingScore array not allocated enough space");
                }
                // mark the cursor position at the next token to note where the loop starts
                scriptCursor++;
                scriptNesting[nestingDepth] = scriptCursor;
                // reset the score for this loop
                // the score greater than zero indicates that data is being retrieved
                // the score gets incremented if build() is called subsequently
                nestingScore[nestingDepth] = 0;
            } else if (scriptElem.equals(")")) {
                // end of loop
                // check to see if exit loop has been flagged
                if (nestingScore[nestingDepth] == 0) {  // exit this loop
                    nestingDepth--;
                    scriptCursor++;
                } else {                                // stay in this loop
                    // add this loop's score to the one above it
                    nestingScore[nestingDepth-1] += nestingScore[nestingDepth];
                    // reset this loop's score to zero
                    nestingScore[nestingDepth] = 0;
                    // return to nested loop start
                    scriptCursor = scriptNesting[nestingDepth];
                }
            } else if (scriptElem.equals("[")) {
                // we are starting a group
                scriptCursor++;
                endOfGroup = false;
            } else if (scriptElem.equals("]")) {
                // we have ended a group
                scriptCursor++;
                endOfGroup = true;
                push(null);   // push coversion of group to byte array
            } else if (scriptElem.equals("<")) {
                // trigger end of a logical record
                scriptCursor++;
                endOfLogical = true;
                push(null);   // push conversion of group to byte array
            } else if (scriptElem.equals("<<")) {
                // trigger end of a physical record
                scriptCursor++;
                endOfPhysical = true;
                push(null);   // push conversion of group to byte array
            } else if (scriptElem.equals("^")) {
                // set pen-up mode -- flush any remaining logical records
                // to OutputStream before continuing.
                scriptCursor++;
                if (logicalRecord != null) padLogical();
                while (writeRecord());
                penDown = false;
            } else if (scriptElem.equals("v")) {
                // set pen-down mode -- flush any remaining logical records
                // from buffer before continuing.
                scriptCursor++;
                if (logicalRecord != null) padLogical();
                while (writeRecord());
                penDown = true;
            } else if (checkTrigger(scriptElem)) {
                // trigger action should have been implemented, so get next script element
                scriptCursor++;
            } else {
                // scriptElem is the next object type to return
                // set flag to break from loop
                scriptCursor++;
                searching = false;
            }
        }
        return Integer.parseInt(scriptElem);
    }
    
    /**
     * Build object into the export volume.
     * Director passes next object to the builder to be incorporated into the export volume.
     * Return the number of *new* logical records constructed during this method call.
     * <i>pen-up</i> mode (simulation) object processing will not count the logical 
     * records produced.
     */
    public int build(Object obj) throws Exception {
        if (obj == null) {
            // a null object reference means that there was nothing of that object type to
            // provide...
            return 0;
        }
        //System.err.println("DEBUG: build object: " + obj.toString());
        // increment scoring for the current loop nesting level
        nestingScore[nestingDepth]++;
        int oldRecordCount = logicalRecordCount;  // remember the current count of logical records
        // push the object to the export mold
        push(obj);
        if (penDown) return logicalRecordCount - oldRecordCount;  // return the number of new logical records since the push
        else return 0;   // but only when those records are being written to the output stream
    }
    
    /**
     * Implement final volume closure operations.
     */
    public void finish() throws Exception {
        //System.err.println("DEBUG: ExportBuilder.finish() called");
        volumeFinish();  // complete volume export operations, specific to export format
        if (logicalRecord != null) padLogical();
        while (writeRecord());   // write out the last physical record(s)
        if (outputStream != null) outputStream.flush();  // flush remaining data
    }
    
    // protected methods
    
    /**
     * Push next source object into export mold. This is followed by packing to a byte
     * array, followed by packing to a physical record, followed by output
     * to the output stream.
     */
    protected void push(Object obj) throws Exception {
        // DEBUG
        //if (obj != null) {
        //System.err.println("DEBUG: push(" + ((Blockette) obj).getLookupId() + ")");
        //} else {
        //System.err.println("DEBUG: push(null)");
        //}
        //
        // add object to the export mold.  if null, then do nothing here.
        if (obj != null) exportMold.add(obj);
        // if we are in the middle of an object group, then we want to just store
        // the object in the export mold and not write out a byte array yet.
        // therefore end here.
        if (endOfGroup == false) return;
        //
        // we are at the end of an object group (perhaps group of one object), so take the
        // export mold and use the data there to create the proper byte array, then
        // append it to the current logical record or start a new logical record.
        // if endOfLogical was triggered, then pad the current logical record and start
        // a new one.
        if (endOfLogical) {
            //System.err.println("DEBUG: padLogical");
            padLogical();
        } else {
            //System.err.println("DEBUG: packToRecord");
            packToRecord();
        }
        endOfLogical = false;  // reset flag
        //
        // check to see if we have more than the requisite number of logical records to fill
        // a physical record.  If so, then write the physical record to the output stream.
        // also check for end of physical record trigger.
        if (logicalRecords.size() > logicalPerPhysical || endOfPhysical) {
            writeRecord();  // write physical record
            endOfPhysical = false;  // reset flag
        }
    }
    
    /**
     * Write a physical record of logical records to the output stream.
     * Dequeue the logical records used in the physical record from the logicalRecords vector.
     * if penDown is false, then nothing is written to the output stream, and the logical
     * records are simply dequeued.
     * return true if there is still logicalRecord data remaining.
     */
    protected boolean writeRecord() throws Exception {
        padPhysical();  // this will pad out a physical record if necessary
        int logRecCount = logicalRecords.size();  // get logical record vector size
        //System.err.println("DEBUG: writeRecord() logRecCount before = " + logRecCount);
        if (logRecCount == 0) return false;
        if (outputStream == null && penDown) {
            throw new BuilderException("no open OutputStream available...");
        }
        int i;
        byte[] outputRecord = null;
        for (i = 0; i < logicalPerPhysical && i < logRecCount; i++) {
            if (penDown) {  // only actually write the records if the pen is down
                outputRecord = ((LogicalRecord)logicalRecords.get(i)).contents;
                //System.err.println("DEBUG: writeRecord " + outputRecord.length + " bytes");
                outputStream.write(outputRecord);
            }
        }
        // pop those logical records off of the logicalRecord vector and reset the vector
        // to contain the remaining records.
        List subList = logicalRecords.subList(i,logRecCount);
        logicalRecords = new Vector(subList);
        logRecCount = logicalRecords.size();  // get new logical record vector size
        //System.err.println("DEBUG: writeRecord() logRecCount after = " + logRecCount);
        return (logRecCount > 0);
    }
    
    /**
     * Pad the end of a logical record with recordPadding bytes.
     * Shift the logical record position to equal logicalRecordLength.
     * Push the logical record onto the vector and clear the current register.
     */
    protected void padLogical() {
        if (logicalRecord == null) return;
        for (int i = logicalRecord.position; i < logicalRecordLength; i++) logicalRecord.contents[i] = recordPadding;
        logicalRecord.position = logicalRecordLength;
        logicalRecords.add(logicalRecord);  // add logical record to vector
        logicalRecordCount++;  // increment counter of logical records
        logicalRecord = null;  // reset current logical record to null state
    }
    
    /**
     * Pad the end of a physical record with recordPadding bytes.
     * Push the latest partial logical record onto the logical records vector and pad it.
     * Generate extra empty logical records if necessary.
     */
    protected void padPhysical() throws Exception {
        if (logicalRecords.size() == 0 && logicalRecord == null)
          return;  //nothing to pad, then do nothing
        if (!padEnabledFlag)  //if padding is not enabled, only pad logical
        {
          if (logicalRecords.size() == 0 || logicalRecord == null)
            return;  //nothing to pad, then do nothing
        }
        boolean padded = false;
        while (logicalPerPhysical > logicalRecords.size()) {  // keep padding until we reach physical record size
            padded = true;
            padLogical();  // pad current logical record and store to vector
            startNewLogical(null,false);  // create new (blank) logical record for padding (puts it in logicalRecord)
        }
        if (padded) logicalRecord = null; // if padding occurred, null out logicalRecord, which currently contains a blank record

    }
    
    
    // abstract methods
    
    /**
     * Pack (append) a byte array into a logical record.
     * Use objects from the exportMold as the data source.
     * If the current logical record is full, append the current one to the
     * logicalRecords vector and start a new one.
     * This method knows how to properly start, preface, and finish a
     * complete logical record.
     */
    protected abstract void packToRecord() throws Exception;
    
    /**
     * Check provided string from export script to see if it's meant to trigger a
     * special action.  If so, implement the intended action.
     * Return true if the string is a trigger.
     */
    protected abstract boolean checkTrigger(String s) throws Exception;
    
    /**
     * Perform any finishing operations to the export volume
     */
    protected abstract void volumeFinish() throws BuilderException;
    
    /**
     * Create a new logical record and place it in the logicalRecord instance variable
     * SeedObject is an object to be put into the new logical record, which can help to
     * identify the traits that that logical record will have.
     * If SeedObject is null, this implies that a blank logical record is being created.
     * <b>continuation</b>, when set to true, can mark the logical record as a
     * continuation of the last one.
     */
    protected abstract void startNewLogical(SeedObject obj, boolean continuation) throws Exception;
    
    // inner classes
    
    /**
     * Inner class representing a single logical record.
     */
    protected class LogicalRecord {
        /**
         * Create a new logical record.
         */
        public LogicalRecord() {
        }
        public int position = 0;   // byte position in the logical record
        public byte[] contents = new byte[logicalRecordLength];  // logical record contents
    }
    
    
    // instance variables
    protected String builderType = "UNKNOWN"; // each concrete builder will set this value to indicate its type
    
    protected OutputStream outputStream = null;  // data stream for physical records to be exported through
    
    protected String[] exportScript = null;   // array of characters indicating pattern of object references for export
    
    protected int scriptCursor = 0;           // current index position in export script array
    protected int[] scriptNesting = null;     // array of index numbers indicating the start of a nested loop
    protected int nestingDepth = 0;           // current level of loop nesting in export script
    protected int[] nestingScore = null;      // array tracking the number of objects returned for each loop depth
    protected int logicalRecordLength = 0;    // length in bytes of all logical (data) record blocks
    protected int physicalRecordLength = 0;   // length in bytes of all physical (media) record blocks
    protected int logicalPerPhysical = 0;     // number of logical records per physical record
    protected int logicalRecordCount = 0;       // the total number of logical records created
    
    protected Vector logicalRecords = null;   // keep a vector of logical records to be written to output
    protected Vector exportMold = null;       // hold a group of objects to form into a single byte array for export
    
    protected byte recordPadding = (byte) 0;  // character to use for padding logical/physical records (default: binary zero)
    protected boolean padEnabledFlag = true;
    
    protected boolean vaxOrder = false;       // flag true of using VAX byte order
    protected boolean endOfGroup = true;      // signals true if we have reached the end of a group of objects
    protected boolean endOfLogical = false;   // set true to force the end of a logical record
    protected boolean endOfPhysical = false;  // set true to force the end of a physical record
    protected boolean penDown = true;         // set true to write data to OutputStream...false to stop writing to OutputStream
    
    protected LogicalRecord logicalRecord = null;    // object containing the current formatted logical record
}
