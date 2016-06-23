package edu.iris.Fissures.seed.director;

import edu.iris.Fissures.seed.builder.*;
import edu.iris.Fissures.seed.exception.*;
import java.io.*;

/**
 * Abstract class representing all Director classes for importing data.
 * @author Robert Casey
 * @version 8/26/2003
 */

public abstract class ImportDirector {
	
	/**
	 * Instantiate without assigning a Builder to it.
	 */
	public ImportDirector () {
	}
	
	/**
	 * Assign a Builder to the Director without yet importing a data stream.
	 */
	public ImportDirector (ObjectBuilder builder) {
		assignBuilder(builder);
	}
	
	/**
	 * Determine and return the length in bytes of the
	 * data record currently in the readBuffer.
	 */
	public abstract int getRecLen() throws Exception;
	
	/**
	 * Assign a builder object to this director
	 */
	public void assignBuilder (ObjectBuilder builder) {
		this.builder = builder;
	}
	
	/**
	 * Get the builder object registered to this director
	 */
	public ObjectBuilder getBuilder() {
		return builder;
	}
	
	/**
	 * Set import logical record length.
	 * The user can force the record length to be fixed in cases where
	 * automatic record length detection is not correctly finding the record
	 * length.
	 */
	public void setRecLen(int length) {
		fixedRecordLength = length;
	}
	
	/**
	 * Constructs objects from the input stream of a single volume.
	 * Optional output stream can capture a string representation of the
	 * objects constructed.
	 * Boolean flag indicates whether to store the generated objects in
	 * an object container or not.
	 * Returns the number of records read from the input stream.
	 */
	public int construct (InputStream inStream, OutputStream outStream,
			boolean storeFlag) throws Exception {
		if (builder == null) {
			throw new BuilderException("a builder has not been assigned to this director");
		}
		printOutStream = null;   // reset to null
		open(inStream);
		if (outStream != null) {	
			printOutStream = new PrintWriter(outStream,true);  // set autoflush mode to true
		}
		// read record, build objects, and optionally store the objects in a container
		while (read(storeFlag)) {
			if (printOutStream != null && outputString != null) {
				printOutStream.print(outputString);  // print the record's strings to output
			}
		}
		if (printOutStream != null) printOutStream.close();  // close the output stream if open
		close();  // close the input stream
		return recCount;
	}
	
	/**
	 * Constructs objects from the input stream of a single volume.
	 * Default version of construct that does not pipe string output and
	 * turns on storage of built objects in an object container.
	 */
	public int construct (InputStream inStream) throws Exception {
		return construct(inStream, (OutputStream) null, true);
	}
	
	/**
	 * Open the input stream for reading.
	 */
	public void open (InputStream inStream) throws Exception {
		this.inStream = new DataInputStream(inStream);
		readBuffer = new ReadBuffer (maxRecordLength*2);
		streamOpen = true;
		recCount = 0; 
		incrementVolume();
	}
	
	/**
	 * Close the input stream.
	 */
	public void close () {
		inStream = null;
		streamOpen = false;
	}
	
	
	/**
	 * Read a single record from the input stream and builds objects.
	 * Returns true if data was read in.
	 * <b>storeFlag</b> whether to tell the ObjectBuilder to store()
	 * built objects or not.  Set to TRUE to store objects.
	 */
	public boolean read(boolean storeFlag) throws Exception {
		StringBuffer strBuf = null;  // might want to accumulate string representations of built objects
		outputString = null;  // reset to null
		if (streamOpen) {
			if (getRecord()) {  // get the next record of data...(assigned to record[] array)
				while (recordOffset < recLen) {  // while we can pull blockettes out of this record...
					//System.err.println("DEBUG: recordOffset=" + recordOffset + ", recLen=" + recLen);
					int bytesRead = build();   // construct objects from the record using the assigned builder
					if (bytesRead == 0)
						throw new BuilderException("call to builder resulted in 0 bytes being processed");
					if (printOutStream != null) {  // check for ASCII printing
						String addStr = builder.toString();  // get string representation of object
						if (addStr.length() > 0) {
                              if (strBuf == null) strBuf = new StringBuffer("");  // start new string buffer
							strBuf.append(addStr + "\n");  // append string representation (CR terminator)
						}
					}
					if (storeFlag) builder.store();  // store object in container
					//System.err.println("DEBUG: bytesRead=" + bytesRead);
					recordOffset += bytesRead;   // increment offset counter by number of bytes read
				}
				if (strBuf != null && strBuf.length() > 0) outputString = strBuf.toString();  // save string representation to instance variable
				return true;   // record read successfully
			} else {
				return false;  // no more records to read
			}
		} else {
			throw new SeedInputException("read called while input stream not open");
		}
	}
	
	/**
	 * Read a single record from the input stream and builds objects.
	 * Returns true if data was read in.
	 * Storage of objects will occur.
	 */
	public boolean read() throws Exception {
		return read(true);
	}
	
	/**
	 * Build a data object.
	 * This method will order the registered builder to construct a data object
	 * from the data in the record array starting at the byte offset indicated by the
	 * instance variable recordOffset.  Builder holds the object until next iteration.
	 * Returns an integer reflecting the number of bytes consumed in the build.
	 */	
	public abstract int build() throws Exception;
	
	
	/**
	 * Get the next record from input.
	 * Iterate the next record of data to the 'record' array from the input stream.
	 * Return true until there is no more data in the buffer.
	 */
	private boolean getRecord () throws Exception {
		if (! readBuffer.reachedEOF()) {
			readBuffer.fill(this.inStream);   // fill the buffer as long as we have not hit EOF
		}
		//System.err.println("DEBUG readBuffer length is " + readBuffer.length());
		if (readBuffer.length() <= 0) return false;  // no more data in buffer
		recLen = getRecLen();   // get the length of the current record
		//System.err.println("DEBUG: recLen is " + recLen);
		record = new byte[recLen];   // set up the record array
		System.arraycopy(readBuffer.buffer,0,record,0,recLen);  // copy from the buffer to the array
		readBuffer.shift(recLen);  // shift the buffer to start the next record
		recCount++;  // increment the record count
		recordOffset = 0;   // reset the offset index for the record[] byte array
		//System.err.println("DEBUG record count is " + recCount);
		return true;
	}
	
	/**
	 * Increment the volume number.  A counter is kept of the volume number
	 * currently being read.  This is called every time a new input volume
	 * is opened.
	 */
	private void incrementVolume() throws Exception {
		builder.incrementVolume();
	}
	
	// instance variables
	
	protected boolean streamOpen = false;       // true if there is a data stream open
	protected DataInputStream inStream = null;  // input data stream
	protected ReadBuffer readBuffer = null;     // read buffer for the data stream that facilitates record reads
	protected byte[] record;                    // array containing record contents
	protected int recCount = 0;                 // counter of the number of records read
	protected int recLen = 0;                   // length of current record in bytes
	protected int recordOffset = 0;             // offset index where we are currently looking in the record array
	protected ObjectBuilder builder = null;     // registered builder to construct objects from the record array
	protected String outputString = null;       // this contains a string expression of a built blockette
	protected int fixedRecordLength = 0;     // user can set this value to fix record length
	public int maxRecordLength = 32768;  // default maximum record length
	private PrintWriter printOutStream = null;   // optional text output stream
	
}
