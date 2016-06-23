package edu.iris.dmc.seedcodec;

//import edu.iris.Fissures.seed.util.*;

/**
 * Class for decoding or encoding Steim1-compressed data blocks
 * to or from an array of integer values.
 * <p>
 * Steim compression scheme Copyrighted by Dr. Joseph Steim.<p>
 * <dl>
 * <dt>Reference material found in:</dt>
 * <dd>
 * Appendix B of SEED Reference Manual, 2nd Ed., pp. 119-125
 * <i>Federation of Digital Seismic Networks, et al.</i>
 * February, 1993
 * </dd>
 * <dt>Coding concepts gleaned from code written by:</dt>
 * <dd>Guy Stewart, IRIS, 1991</dd>
 * <dd>Tom McSweeney, IRIS, 2000</dd>
 * </dl>
 *
 * @author Philip Crotwell (U South Carolina)
 * @author Robert Casey (IRIS DMC)
 * @version 10/22/2002
 */


public class Steim1 {

	/**
	 *  Decode the indicated number of samples from the provided byte array and
	 *  return an integer array of the decompressed values.  Being differencing
	 *  compression, there may be an offset carried over from a previous data
	 *  record.  This offset value can be placed in <b>bias</b>, otherwise leave
	 *  the value as 0.
	 *  @param b input byte array to be decoded
	 *  @param numSamples the number of samples that can be decoded from array
	 *  <b>b</b>
	 *  @param swapBytes if true, swap reverse the endian-ness of the elements of
	 *  byte array <b>b</b>.
	 *  @param bias the first difference value will be computed from this value.
	 *  If set to 0, the method will attempt to use the X(0) constant instead.
	 *  @return int array of length <b>numSamples</b>.
	 *  @throws SteimException - encoded data length is not multiple of 64
	 *  bytes.
	 */
	public static int[] decode(byte[] b, int numSamples, boolean swapBytes, int bias) throws SteimException {
		// Decode Steim1 compression format from the provided byte array, which contains numSamples number
		// of samples.  swapBytes is set to true if the value words are to be byte swapped.  bias represents
		// a previous value which acts as a starting constant for continuing differences integration.  At the
		// very start, bias is set to 0.
		if (b.length % 64 != 0) {
			throw new SteimException("encoded data length is not multiple of 64 bytes (" + b.length + ")"); 
		}
		int[] samples = new int[numSamples];
		int[] tempSamples;
		int numFrames = b.length / 64;
		int current = 0;
		int start=0, end=0;
		int firstData=0;
		int lastValue = 0;

		//System.err.println("DEBUG: number of samples: " + numSamples + ", number of frames: " + numFrames + ", byte array size: " + b.length);
		for (int i=0; i< numFrames; i++ ) {
			//System.err.println("DEBUG: start of frame " + i);
			tempSamples = extractSamples(b, i*64, swapBytes);   // returns only differences except for frame 0
			firstData = 0; // d(0) is byte 0 by default
			if (i==0) {   // special case for first frame
				lastValue = bias; // assign our X(-1)
				// x0 and xn are in 1 and 2 spots
				start = tempSamples[1];  // X(0) is byte 1 for frame 0
				end = tempSamples[2];    // X(n) is byte 2 for frame 0
				firstData = 3; // d(0) is byte 3 for frame 0
				//System.err.println("DEBUG: frame " + i + ", bias = " + bias + ", x(0) = " + start + ", x(n) = " + end);
				// if bias was zero, then we want the first sample to be X(0) constant
				if (bias == 0) lastValue = start - tempSamples[3];  // X(-1) = X(0) - d(0)
			}
			//System.err.print("DEBUG: ");
			for (int j = firstData; j < tempSamples.length && current < numSamples; j++) {
				samples[current] = lastValue + tempSamples[j];  // X(n) = X(n-1) + d(n)
				lastValue = samples[current];
				//System.err.print("d(" + (j-firstData) + ")" + tempSamples[j] + ", x(" + current + ")" + samples[current] + ";"); // DEBUG
				current++;
			}
			//System.err.println("DEBUG: end of frame " + i);
		}  // end for each frame...
        if (current != numSamples) {
            throw new SteimException("Number of samples decompressed doesn't match number in header: "+current+" != "+numSamples);
        }
		return samples;
	}

	/**
	 * Abbreviated, zero-bias version of decode().
	 *
	 * @see edu.iris.Fissures.codec.Steim1#decode(byte[],int,boolean,int)
	 */
	public static int[] decode(byte[] b, int numSamples, boolean swapBytes) throws SteimException {
		// zero-bias version of decode
		return decode(b,numSamples,swapBytes,0);
	}


	/**
	* Encode the array of integer values into a Steim 1 * compressed byte frame block.
	* This algorithm will not create a byte block any greater * than 63 64-byte frames.
	* <b>frames</b> represents the number of frames to be written.
	* This number should be determined from the desired logical record length
	* <i>minus</i> the data offset from the record header (modulo 64)
	* If <b>samples</b> is exhausted before all frames are filled, the remaining frames
	* will be nulls.
	* <b>bias</b> is a value carried over from a previous data record, representing
	* X(-1)...set to 0 otherwise
	* @param samples the data points represented as signed integers
	* @param frames the number of Steim frames to use in the encoding
	* @param bias offset for use as a constant for the first difference, otherwise
	* set to 0
	* @return SteimFrameBlock containing encoded byte array
	* @throws SteimException samples array is zero size
	* @throws SteimException number of frames is not a positive value
	* @throws SteimException cannot encode more than 63 frames
	*/
	public static SteimFrameBlock encode(int[] samples, int frames, int bias) throws SteimException {
		if (samples.length == 0) {
			throw new SteimException("samples array is zero size");
		}
		if (frames <= 0) {
			throw new SteimException("number of frames is not a positive value");
		}
		if (frames > 63) {
			throw new SteimException("cannot encode more than 63 frames, you asked for " + frames);
		}
		// all encoding will be contained within a frame block
		// Steim encoding 1
		SteimFrameBlock frameBlock = new SteimFrameBlock(frames,1);
		//
		// pass through the list of samples, and pass encoded words
		// to frame block
		// end loop if we run out of samples or the frame block
		// fills up
		// .............................................................
		// first initialize the first frame with integration constant X(0)
		// and reverse integration constant X(N)
		// ...reverse integration constant may need to be changed if 
		// the frameBlock fills up.
		frameBlock.addEncodedWord(samples[0],0,0);                // X(0) -- first sample value
		frameBlock.addEncodedWord(samples[samples.length-1],0,0); // X(N) -- last sample value
		//
		// now begin looping over differences
		int sampleIndex = 0;  // where we are in the sample array
		int[] diff = new int[4]; // store differences here
		int diffCount = 0;  // how many sample diffs we put into current word
		int maxSize = 0;    // the maximum diff value size encountered
		int curSize = 0;    // size of diff value currently looked at
		while(sampleIndex < samples.length) {
			// look at the next (up to four) differences
			// and assess the number that can be put into
			// the upcoming word
			diffCount = 0;
			maxSize = 0;
			for (int i=0; i<4; i++) {
				if (sampleIndex+i < samples.length) {
					// as long as there are still samples
					// get next difference  X[i] - X[i-1]
					if (sampleIndex+i == 0) {
						// special case for d(0) = x(0) - x(-1).
						diff[0] = samples[0] - bias;
					} else {
						diff[i] = samples[sampleIndex+i] - samples[sampleIndex+i-1];
					}
					// and increment the counter
					diffCount++;
				} else break;  // no more samples, leave for loop
				// curSize indicates how many bytes the number would fill
				if (diff[i] <= 127 && diff[i] >= -128) curSize = 1;
				else if (diff[i] <= 32767 && diff[i] >= -32768) curSize = 2;
				else curSize = 4;
				// get the maximum size
				if (curSize > maxSize) maxSize = curSize;
				// now we multiply the maximum size encountered so far
				// by the number of differences examined so far
				// if the number is less than 4, we move on to the next diff
				// if the number is equal to 4, then we stop with the 
				// current count
				// if the number is greater than 4, then back off one count
				// and if the count is 3 (cannot end with a 3 byte count), 
				// then back off one count again
				// (the whole idea is we are looking for the proper fit)
				if (maxSize * diffCount == 4) break;
				else if (maxSize * diffCount > 4) {
					diffCount--;
					if (diffCount == 3) diffCount--;
					break;
				}
			} // end for (0..3)

			// generate the encoded word and the nibble value
			int nibble = 0;
			int word = 0;
			if (diffCount == 1) {
				word = diff[0];
				nibble = 3;  // size 4 = 11
			} else if (diffCount == 2) {
				word = (diff[0] & 0xffff) << 16;  // clip to 16 bits, then shift
				word |= (diff[1] & 0xffff);
				nibble = 2;  // size 2 = 10
			} else {  // diffCount == 4
				word = (diff[0] & 0xff) << 24;    // clip to 8 bits, then shift
				word |= (diff[1] & 0xff) << 16;
				word |= (diff[2] & 0xff) << 8;
				word |= (diff[3] & 0xff);
				nibble = 1;  // size 1 = 01
			}

			// add the encoded word to the frame block
			if (frameBlock.addEncodedWord(word,diffCount,nibble)) {
				// frame block is full (but the value did get added) 
				// so modify reverse integration constant to be the very last value added
				// and break out of loop (read no more samples)
				frameBlock.setXsubN(samples[sampleIndex+diffCount-1]); // X(N)
				break;
			}

			// increment the sampleIndex by the diffCount
			sampleIndex += diffCount;
		} // end while next sample

		return frameBlock;
	}

	/**
	 * Abbreviated zero-bias version of encode().
	 * @see edu.iris.Fissures.codec.Steim1#encode(int[],int,int)
	 */
	public static SteimFrameBlock encode(int[] samples, int frames) throws SteimException {
		return encode(samples,frames,0);   // zero-bias version of encode
	}


	/**
	 * Extracts differences from the next 64 byte frame of the given compressed
	 * byte array (starting at offset) and returns those differences in an int
	 * array.
	 * An offset of 0 means that we are at the first frame, so include the header
	 * bytes in the returned int array...else, do not include the header bytes
	 * in the returned array.
	 * @param bytes byte array of compressed data differences
	 * @param offset index to begin reading compressed bytes for decoding
	 * @param swapBytes reverse the endian-ness of the compressed bytes being read
	 * @return integer array of difference (and constant) values
	 */
	protected static int[] extractSamples(byte[] bytes,
			int offset, 
			boolean swapBytes) {
		/* get nibbles */
		int nibbles = Utility.bytesToInt(bytes[offset], 
				bytes[offset+1], 
				bytes[offset+2], 
				bytes[offset+3], 
				swapBytes);
		int currNibble = 0;
		int[] temp = new int[64];  // 4 samples * 16 longwords, can't be more
		int currNum = 0;
		//System.err.print ("DEBUG: ");
		for (int i=0; i<16; i++) {   // i is the word number of the frame starting at 0
			//currNibble = (nibbles >>> (30 - i*2 ) ) & 0x03; // count from top to bottom each nibble in W(0)
			currNibble = (nibbles >> (30 - i*2) ) & 0x03; // count from top to bottom each nibble in W(0)
			//System.err.print("c(" + i + ")" + currNibble + ",");  // DEBUG
			// Rule appears to be:
			// only check for byte-swap on actual value-atoms, so a 32-bit word in of itself
			// is not swapped, but two 16-bit short *values* are or a single
			// 32-bit int *value* is, if the flag is set to TRUE.  8-bit values
			// are naturally not swapped.
			// It would seem that the W(0) word is swap-checked, though, which is confusing...
			// maybe it has to do with the reference to high-order bits for c(0)
			switch (currNibble) {
				case 0:
					//System.out.println("0 means header info");
					// only include header info if offset is 0
					if (offset == 0) {
						temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)], 
								bytes[offset+(i*4)+1],
								bytes[offset+(i*4)+2],
								bytes[offset+(i*4)+3],
								swapBytes);
					}
					break;
				case 1:
					//System.out.println("1 means 4 one byte differences");
					for (int n=0; n<4; n++) {
						temp[currNum] = Utility.bytesToInt(bytes[offset+(i*4)+n]);
						currNum++;
					}
					break;
				case 2:
					//System.out.println("2 means 2 two byte differences");
					for (int n=0; n<4; n+=2) {
						temp[currNum] = Utility.bytesToInt(bytes[offset+(i*4)+n], 
								bytes[offset+(i*4)+n+1],
								swapBytes);
						currNum++;
					}
					break;
				case 3:
					//System.out.println("3 means 1 four byte difference");
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)], 
							bytes[offset+(i*4)+1],
							bytes[offset+(i*4)+2], 
							bytes[offset+(i*4)+3],
							swapBytes);
					break;
				default:
					//System.out.println("default");
			}
		}
		//System.err.println(".");   // DEBUG
		int[] out = new int[currNum];
		System.arraycopy(temp, 0, out, 0, currNum);  // trim array to number of values
		return out;
	}

	/**
	 * Static method for testing the decode() method.
	 * @param args not used
	 * @throws SteimException from called method(s)
	*/
	public static void main(String[] args) throws SteimException {

		byte[] b = new byte[64];
		int[] temp;

		for (int i=0; i< 64 ; i++) {
			b[i] = 0x00;
		}
		b[0] = 0x01;
		b[1] = (byte)0xb0;
		System.out.println(b[1]);
		b[2] = (byte)0xff;
		b[3] = (byte)0xff;

		b[4] = 0;
		b[5] = 0;
		b[6] = 0;
		b[7] = 0;

		b[8] = 0;
		b[9] = 0;
		b[10] = 0;
		b[11] = 0;

		b[12] = 1;
		b[13] = 2;
		b[14] = 3;
		b[15] = 0;

		b[16] = 1;
		b[17] = 1;
		b[18] = 0;
		b[19] = 0;

		b[20] = 0;
		b[21] = 1;
		b[22] = 0;
		b[23] = 0;
		temp = Steim1.decode(b, 17, false);
	}
}
