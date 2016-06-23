package edu.iris.dmc.seedcodec;

//import edu.iris.Fissures.seed.util.*;

/**
 *  Class for decoding or encoding Steim2-compressed data blocks
 *  to or from an array of integer values.
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
 * <dd>Doug Neuhauser, UC Berkeley, 2010</dd>
 * <dd>Kevin Frechette, ISTI, 2010</dd>
 * </dl>
 *
 * @author Philip Crotwell (U South Carolina)
 * @author Robert Casey (IRIS DMC)
 * @author Doug Neuhauser (UC Berkeley)
 * @author Kevin Frechette (ISTI)
 * @version 9/13/2010
 */

public class Steim2 {
        public static java.io.PrintStream debug = null;

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
        
		if (debug != null) {
		  debug.println("number of samples: " + numSamples + ", number of frames: " + numFrames + ", byte array size: " + b.length);
		}
		for (int i=0; i< numFrames; i++ ) {
		        if (debug != null) {
		          debug.println("start of frame " + i);
		        }
			tempSamples = extractSamples(b, i*64, swapBytes);   // returns only differences except for frame 0
			firstData = 0; // d(0) is byte 0 by default
			if (i==0) {   // special case for first frame
				lastValue = bias; // assign our X(-1)
				// x0 and xn are in 1 and 2 spots
				start = tempSamples[1];  // X(0) is byte 1 for frame 0
				end = tempSamples[2];    // X(n) is byte 2 for frame 0
				firstData = 3; // d(0) is byte 3 for frame 0
				if (debug != null) {
				  debug.println("frame " + i + ", bias = " + bias + ", x(0) = " + start + ", x(n) = " + end);
				}
				// if bias was zero, then we want the first sample to be X(0) constant
				if (bias == 0) lastValue = start - tempSamples[3];  // X(-1) = X(0) - d(0)
			}
			for (int j = firstData; j < tempSamples.length && current < numSamples; j++) {
				samples[current] = lastValue + tempSamples[j];  // X(n) = X(n-1) + d(n)
				lastValue = samples[current];
				if (debug != null) {
				  debug.print("d(" + (j-firstData) + ")" + tempSamples[j] + ", x(" + current + ")" + samples[current] + ";");
				}
				current++;
			}
                        if (debug != null) {
                          debug.println(": end of frame " + i);
                        }
		}  // end for each frame...
		if (current != numSamples) {
                  throw new SteimException("Number of samples decompressed doesn't match number in header: "+current+" != "+numSamples);
		}
		return samples;
	}

	/**
	 * Abbreviated, zero-bias version of decode().
	 *
	 * @see edu.iris.Fissures.codec.Steim2#decode(byte[],int,boolean,int)
	 */
	public static int[] decode(byte[] b, int numSamples, boolean swapBytes) throws SteimException {
		// zero-bias version of decode
		return decode(b,numSamples,swapBytes,0);
	}

        /**
         * Abbreviated zero-bias version of encode().
         * @see edu.iris.Fissures.codec.Steim2#encode(int[],int,int)
         */
        public static SteimFrameBlock encode(int[] samples, int frames) throws SteimException {
                return encode(samples,frames,0);   // zero-bias version of encode
        }

        /**
         * Encode the array of integer values into a Steim 2 * compressed byte frame block.
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
           return encode(samples, frames, bias, samples.length);
         }
         
         /**
          * Encode the array of integer values into a Steim 2 * compressed byte frame block.
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
          * @param samplesLength the samples length
          * @return SteimFrameBlock containing encoded byte array
          * @throws SteimException samples array is zero size
          * @throws SteimException number of frames is not a positive value
          * @throws SteimException cannot encode more than 63 frames
          */
          public static SteimFrameBlock encode(int[] samples, int frames, int bias, int samplesLength) throws SteimException {
                  if (samplesLength == 0) {
                          throw new SteimException("samples array is zero size");
                  }
                  if (frames <= 0) {
                          throw new SteimException("number of frames is not a positive value");
                  }
                  if (frames > 63) {
                          throw new SteimException("cannot encode more than 63 frames, you asked for " + frames);
                  }
                  // all encoding will be contained within a frame block
                  // Steim encoding 2
                  SteimFrameBlock frameBlock = new SteimFrameBlock(frames,2);
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
                  frameBlock.addEncodedWord(samples[samplesLength-1],0,0); // X(N) -- last sample value
                  //
                  // now begin looping over differences
                  int sampleIndex = 0;  // where we are in the sample array
                  int[] diff = new int[7]; // store differences here
                  int[] minbits = new int[7]; // store min # bits required to represent diff here
                  int points_remaining;  // the number of points remaining
                  while(sampleIndex < samplesLength) {
                          // look at the next (up to seven) differences
                          // and assess the number that can be put into
                          // the upcoming word
                          points_remaining = 0;
                          for (int i=0; i<7; i++) {
                                  if (sampleIndex+i < samplesLength) {
                                          // as long as there are still samples
                                          // get next difference  X[i] - X[i-1]
                                          if (sampleIndex+i == 0) {
                                                  // special case for d(0) = x(0) - x(-1).
                                                  diff[0] = samples[0] - bias;
                                          } else {
                                                  diff[i] = samples[sampleIndex+i] - samples[sampleIndex+i-1];
                                          }
                                          // and increment the counter
                                          minbits[i] = minBitsNeeded(diff[i]);
                                          points_remaining++;
                                  } else {
                                    break;  // no more samples, leave for loop
                                  }
                          } // end for (0..7)

                          // Determine the packing required for the next compressed word in the SteimFrame.
                          final int nbits = bitsForPack(minbits, points_remaining);
                          
                          // generate the encoded word and the nibble value
                          final int ndiff;  // the number of differences
                          final int bitmask;
                          final int submask;
                          final int nibble;
                          switch (nbits) {
                          case 4:
                            ndiff = 7;
                            bitmask = 0x0000000f;
                            submask = 0x02;
                            nibble = 3;
                            break;
                          case 5:
                            ndiff = 6;
                            bitmask = 0x0000001f;
                            submask = 0x01;
                            nibble = 3;
                            break;
                          case 6:
                            ndiff = 5;
                            bitmask = 0x0000003f;
                            submask = 0x00;
                            nibble = 3;
                            break;
                          case 8:
                            ndiff = 4;
                            bitmask = 0x000000ff;
                            submask = 0;
                            nibble = 1;
                            break;
                          case 10:
                            ndiff = 3;
                            bitmask = 0x000003ff;
                            submask = 0x03;
                            nibble = 2;
                            break;
                          case 15:
                            ndiff = 2;
                            bitmask = 0x00007fff;
                            submask = 0x02;
                            nibble = 2;
                            break;
                          case 30:
                            ndiff = 1;
                            bitmask = 0x3fffffff;
                            submask = 0x01;
                            nibble = 2;
                            break;
                          default:
                            throw new SteimException("Unable to encode " + nbits + " bit difference in Steim2 format");
                          }
                          
                          final int word = steimPackWord(diff, nbits, ndiff, bitmask, submask);

                          // add the encoded word to the frame block
                          if (frameBlock.addEncodedWord(word,ndiff,nibble)) {
                                  // frame block is full (but the value did get added) 
                                  // so modify reverse integration constant to be the very last value added
                                  // and break out of loop (read no more samples)
                                  frameBlock.setXsubN(samples[sampleIndex+ndiff-1]); // X(N)
                                  break;
                          }

                          // increment the sampleIndex by the number of differences
                          sampleIndex += ndiff;
                  } // end while next sample

                  return frameBlock;
          }

          private static int minBitsNeeded(int diff) {
                  int minbits = 0;
                  if (diff >= -8 && diff < 8) minbits= 4;
                  else if (diff >= -16 && diff < 16) minbits = 5;
                  else if (diff >= -32 && diff < 32) minbits = 6;
                  else if (diff >= -128 && diff < 128) minbits = 8;
                  else if (diff >= -512 && diff < 512) minbits = 10;
                  else if (diff >= -16384 && diff < 16384) minbits = 15;
                  else if (diff >= -536870912 && diff < 536870912) minbits = 30;
                  else minbits = 32;
                  return minbits;
          }

          private static int bitsForPack(int[] minbits, int points_remaining) {
                  if (points_remaining >= 7 &&
                          (minbits[0] <= 4) && (minbits[1] <= 4) &&
                          (minbits[2] <= 4) && (minbits[3] <= 4) &&
                          (minbits[4] <= 4) && (minbits[5] <= 4) &&
                          (minbits[6] <= 4)) return 4;
                  if (points_remaining >= 6 &&
                          (minbits[0] <= 5) && (minbits[1] <= 5) &&
                          (minbits[2] <= 5) && (minbits[3] <= 5) &&
                          (minbits[4] <= 5) && (minbits[5] <= 5)) return 5;
                  if (points_remaining >= 5 &&
                          (minbits[0] <= 6) && (minbits[1] <= 6) &&
                          (minbits[2] <= 6) && (minbits[3] <= 6) &&
                          (minbits[4] <= 6)) return 6;
                  if (points_remaining >= 4 &&
                          (minbits[0] <= 8) && (minbits[1] <= 8) &&
                          (minbits[2] <= 8) && (minbits[3] <= 8)) return 8;
                  if (points_remaining >= 3 &&
                          (minbits[0] <= 10) && (minbits[1] <= 10) &&
                          (minbits[2] <= 10)) return 10;
                  if (points_remaining >= 2 &&
                          (minbits[0] <= 15) && (minbits[1] <= 15)) return 15;
                  if (points_remaining >= 1 &&
                          (minbits[0] <= 30)) return 30;
                  return 32;
          }

          /**
           * Pack Steim2 compressed word with optional submask.
           * @param diff the differences
           * @param nbits the number of bits
           * @param ndiff the number of differences
           * @param bitmask the bit mask
           * @param submask the sub mask or 0 if none
           */
          private static int steimPackWord(int[] diff, int nbits, int ndiff, int bitmask, int submask) {
                  int val = 0;
                  int i = 0;
                  for (i=0; i<ndiff; i++) {
                          val = (val<<nbits) | (diff[i] & bitmask);
                  }
                  if (submask != 0) {
                    val |= (submask << 30);
                  }
                  return val;
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
			boolean swapBytes) 
	{
		/* get nibbles */
		int nibbles = Utility.bytesToInt(bytes[offset], 
				bytes[offset+1], 
				bytes[offset+2], 
				bytes[offset+3], 
				swapBytes);
		if (debug != null) {
                  debug.printf("extractSamples: %d bytes, offset is %d, byte swap is %b, nibbles is %08X\n",
                      bytes.length, offset, swapBytes, nibbles);
		}
		int currNibble = 0;
		int dnib = 0;
		int[] temp = new int[106]; // 7 samples * 15 long words + 1 nibble int
		int tempInt;
                int startNum;
		int currNum = 0;
		for (int i=0; i<16; i++) {
			currNibble = (nibbles >> (30 - i*2 ) ) & 0x03;
			startNum = currNum;
			switch (currNibble) {
				case 0:
					// only include header info if offset is 0
					if (offset == 0) {
					        if (debug != null) {
					          debug.print("0 means header info");
					        }
						temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)],
								bytes[offset+(i*4)+1],
								bytes[offset+(i*4)+2],
								bytes[offset+(i*4)+3],
								swapBytes);
					}
					break;
				case 1:
                                        if (debug != null) {
                                          debug.print("1 means 4 one byte differences");
                                        }
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)]);
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)+1]);
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)+2]);
					temp[currNum++] = Utility.bytesToInt(bytes[offset+(i*4)+3]);
					break;
				case 2:
					tempInt = Utility.bytesToInt(bytes[offset+(i*4)], 
							bytes[offset+(i*4)+1],
							bytes[offset+(i*4)+2], 
							bytes[offset+(i*4)+3], 
							swapBytes);
					dnib = (tempInt >> 30) & 0x03;
					switch (dnib) {
						case 1:
                                                        if (debug != null) {
                                                          debug.print("2,1 means 1 thirty bit difference");
                                                        }
							temp[currNum++] = (tempInt << 2) >> 2;
							break;
						case 2:
                                                        if (debug != null) {
                                                          debug.print("2,2 means 2 fifteen bit differences");
                                                        }
							temp[currNum++] = (tempInt << 2) >> 17;  // d0
							temp[currNum++] = (tempInt << 17) >> 17; // d1
							break;
						case 3:
                                                        if (debug != null) {
                                                          debug.print("2,3 means 3 ten bit differences");
                                                        }
							temp[currNum++] = (tempInt << 2) >> 22;  // d0
							temp[currNum++] = (tempInt << 12) >> 22; // d1
							temp[currNum++] = (tempInt << 22) >> 22; // d2
							break;
						default:
                                                        if (debug != null) {
                                                          debug.print("2," + dnib + " default");
                                                        }
					}
					break;
				case 3:
					tempInt = Utility.bytesToInt(bytes[offset+(i*4)], 
							bytes[offset+(i*4)+1],
							bytes[offset+(i*4)+2], 
							bytes[offset+(i*4)+3],
							swapBytes);
					dnib = (tempInt >> 30) & 0x03;
					// for case 3, we are going to use a for-loop formulation that
					// accomplishes the same thing as case 2, just less verbose.
					int diffCount = 0;  // number of differences
					int bitSize = 0;    // bit size
					int headerSize = 0; // number of header/unused bits at top
					switch (dnib) {
						case 0:
                                                        if (debug != null) {
                                                          debug.print("3,0 means 5 six bit differences");
                                                        }
							headerSize = 2;
							diffCount = 5;
							bitSize = 6;
							break;
						case 1:
                                                        if (debug != null) {
                                                          debug.print("3,1 means 6 five bit differences");
                                                        }
							headerSize = 2;
							diffCount = 6;
							bitSize = 5;
							break;
						case 2:
                                                        if (debug != null) {
                                                          debug.print("3,2 means 7 four bit differences, with 2 unused bits");
                                                        }
							headerSize = 4;
							diffCount = 7;
							bitSize = 4;
							break;
						default:
                                                        if (debug != null) {
                                                          debug.print("3," + dnib + " default");
                                                        }
					}
					if (diffCount > 0) {
						for (int d=0; d<diffCount; d++) {  // for-loop formulation
							temp[currNum++] = ( tempInt << (headerSize+(d*bitSize)) ) >> (((diffCount-1)*bitSize) + headerSize);
						}
					}
					break;
			}
			if (debug != null && startNum != currNum) {
			  debug.print("{");
			  for (int tempIndex = startNum; tempIndex < currNum; tempIndex++) {
			    if (tempIndex > startNum) {
			      debug.print(",");
                            }
			    debug.print(temp[tempIndex]);
                          }
			  debug.println("}");
                        }
		}
		int[] out = new int[currNum];
		System.arraycopy(temp, 0, out, 0, currNum);
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
		temp = Steim2.decode(b, 17, false);
	}

}
