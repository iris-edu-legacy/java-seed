package edu.iris.Fissures.seed.director;

import edu.iris.Fissures.seed.exception.*;
import java.io.*;
import java.util.*;

/**
 * This class is a generic file read buffer that takes on the 
 * task of keeping a full byte array buffer available for Director 
 * classes to extract from.<br>
 * This class can be used for any file format. <br>
 * This class does not perform the tasks of determining 
 * record lengths or verification of the records it is reading in.<br>
 * What it does is fill the buffer to its initialized capacity, which should be
 * the maximum possible logical record length, or even the 
 * maximum possible physical record length of the volume.<br>
 * Outside routines will examine the buffer and decide where to stop reading, 
 * whether it is at the edge of a logical record, or otherwise.<br>
 * When a portion of the buffer has been read, this class can be told to shift 
 * the data by that amount to allow space for more data to be read in.<br>
 * A good recommendation is to make the buffer size two times the expected 
 * logical record length.
 * @author Robert Casey, IRIS DMC
 * @version 11/14/2002
 */
public class ReadBuffer {

	// Constructors

	/**
	 * Create a ReadBuffer with the specified buffer size.
	 * @param bufSize buffer size in bytes
	 */
	public ReadBuffer (int bufSize) {
		buffer = new byte[bufSize];
		Arrays.fill(buffer,(byte) 0);
		this.bufSize = bufSize;  // record the buffer size
	}	


	/**
	 * A default buffer size will be used here (defaultBufSize).
	 */
	public ReadBuffer () {
		this(defaultBufSize);
	}


	// Public methods

	/**
	 * Return the current length of data present.
	 * @return length of data currently in buffer
	 */
	public int length() {
		return dataLength;
	}

	/**
	  * Return the size of the buffer.
	  * @return size of the buffer in bytes
	  */
	public int bufSize() {
		return bufSize;
	}

	/**
	 * Fill the empty space of the buffer with new data
	 * @param inStream input stream to fill the buffer with
	 * @throws SeedInputException if bufSize is less than dataLength
	 * @throws IOException
	 */
	public void fill(DataInputStream inStream) 
		throws SeedException, IOException {
		int emptySpace = bufSize - dataLength;
		if (emptySpace < 0) {
			throw new SeedInputException("bufSize less than dataLength");
		}
		// read more data from stream if there is still more data
		if (! reachedEOF() ) {
			// check to see what is available, if it is smaller than the empty
			// buffer space, only read back that much then and flag EOF
			// if there is 0 bytes left in the stream, then flag EOF and simply return
			int available = inStream.available();
			if (available == 0) {
				reachedEOF = true;
				return;
			}
			if (available < emptySpace) {
				reachedEOF = true;
				emptySpace = available;
			}
			inStream.readFully(buffer,dataLength,emptySpace);  // read in the data with offset and len
			dataLength += emptySpace;  // increment the available data length
			return;
		}
		}


	/**
	 * Left-shift the data in the buffer by a certain number of bytes to
	 * make room for new incoming data
	 * @param amount number of bytes to shift the buffer by
	 */
	public void shift (int amount) {
		// shift all of the existing data by amount
		System.arraycopy(buffer,amount,buffer,0,bufSize-amount);
		// pad the remaining gap with zeroes
		Arrays.fill(buffer,bufSize-amount,bufSize,(byte) 0);
		// deduct dataLength by shift amount
		dataLength -= amount;
		if (dataLength < 0) dataLength = 0;  // a very large 'amount' value could do this
	}


	/**
	 * Return true if the input stream is at EOF
	 * @return true if the input stream is at EOF
	 */
	public boolean reachedEOF () {
		return reachedEOF;
	}

	// Instance variables

	/**
	 * Buffer holding the data from the input stream
	 */
	protected byte[] buffer; 
	private int dataLength = 0;   // the number of data bytes present in the buffer
	private int bufSize = 0;      // the capacity, in bytes, of the buffer
	/**
	 * Default buffer capacity (32768)
	 */
	public static final int defaultBufSize = 32768;
	private boolean reachedEOF = false;  // flagged to true when the input stream reaches EOF


}
