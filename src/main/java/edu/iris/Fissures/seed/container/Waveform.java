package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;
import edu.iris.dmc.seedcodec.*;
import java.io.*;

/**
  Default waveform container.  Data is stored in its native encoded form.
  This object retains information on the encoding format, the number of samples,
  and the byte swapping flag.
  @author Robert Casey, IRIS DMC
  @version 2/4/2010
*/

public class Waveform extends SeedObject {

    /**
     * Initialize the waveform object with an encoded data set starting at the
     * offset index indicated.  The encoding format and byte swap flag 
     * is indicated during build time.  Encoding format is a predefined String pertaining
     * to a SEED-recognized format.  <b>swapBytes</b> is set to true for VAX/8086 word order.
     */
    public Waveform (byte[] record, int dataOffset, int numSamples, String encoding, boolean swapBytes) {
	waveData = new byte[record.length-dataOffset];
	System.arraycopy(record,dataOffset,waveData,0,waveData.length);  // data shifted to start at index 0
	this.numSamples = numSamples;
	this.swapBytes = swapBytes;
	this.encoding = encoding;
        codec = new Codec();
    }

    /**
     * Initialize the waveform object with an array of integer values.
     * Encode the data with the specified encoding format to a maximum 
     * byte length of maxByteLen.  <b>bias</b> is an initial value
     * representing the last sample of the previous Waveform, for use
     * in difference compression.  Default value is 0.
     */
    @Deprecated public Waveform (int[] intData, int maxByteLen, String encoding, int bias)
            throws SeedInputException, SeedException, SteimException, IOException {
        codec = new Codec();
	encodeWaveform(intData,maxByteLen,encoding,bias);
    }

    /**
     * Initialize the waveform object with an array of integer values.
     * Encode the data with the specified encoding format to a maximum 
     * byte length of maxByteLen.  Assumed bias value of 0 for differencing
     * compression.
     */
    public Waveform (int[] intData, int maxByteLen, String encoding)
            throws SeedInputException, SeedException, SteimException, IOException  {
        codec = new Codec();
        //System.err.println("calling encodeWaveform intData.length=" + intData.length + ", maxByteLen=" + maxByteLen + ", encoding=" + encoding);
	encodeWaveform(intData,maxByteLen,encoding);
    }

    /**
     * Initialize the waveform object with an array of float values.  Encode
     * the data with the specified encoding format to a maximum byte length
     * of maxByteLen.  <b>bias</b> is an initial value representing the last
     * sample of the previous Waveform, for use in difference compression.
     * Default value is 0.
     */
    @Deprecated public Waveform (float[] floatData, int maxByteLen, String encoding, float bias) throws SeedInputException, SeedException {
        codec = new Codec();
	encodeWaveform(floatData,maxByteLen,encoding,bias);
    }

    /**
     * Initialize the waveform object with an array of float values.  Encode
     * the data with the specified encoding format to a maximum byte length
     * of maxByteLen. Assumed bias value of 0 for difference compression.
     */
    public Waveform (float[] floatData, int maxByteLen, String encoding) throws SeedInputException, SeedException  {
        codec = new Codec();
	encodeWaveform(floatData,maxByteLen,encoding);
    }

    // public methods

    /**
     * Return a fixed value representing this SeedObject type.
     */
    public int getType() {
	// implement SeedObject abstract method
	return 20000;  // arbitrary value
    }

    /**
     * Return a fixed value for the lookup ID.  Waveform objects do not need to
     * be distinguished from others, since they are always attached to Blockettes.
     */
    public int getLookupId() {
	// implement SeedObject abstract method
	return 0;  // arbitrary value
    }

    /**
     * Decode waveform data to a standard container object.
     * Indicate bias as a carryover constant from a previous data record, but can
     * be set to zero otherwise.
     */
    @Deprecated public DecompressedData getDecompressedData(int bias) throws CodecException, SeedException {
	if (waveData.length == 0 || numSamples < 1) {
	    throw new SeedException ("attempting read on empty data stream");
	}
	// select decode method based on encoding
        //
        /****** OLD WAY
	if (encoding.equals("Steim1")) {
	    return Steim1.decode(waveData,numSamples,swapBytes,bias);
	} else if (encoding.equals("Steim2")) {
	    return Steim2.decode(waveData,numSamples,swapBytes,bias);
	} else {
	    throw new SeedException("format " + encoding + " unavailable for integer decoding");
	}
         * ******/
        String b1000str = SeedEncodingResolver.translate(encoding);  // get the integer representation of the encoding type
        int b1000int = Integer.parseInt(b1000str);
        // return a decompressed data object based on the integer value of the encoding type
        return codec.decompress(b1000int, waveData, numSamples, swapBytes);
    }

    public DecompressedData getDecompressedData() throws CodecException, SeedException  {
        return getDecompressedData(0);
    }

    /**
     * Decode waveform data to an array of integer values.
     * Indicate bias as a carryover constant from a previous data record, but can
     * be set to zero otherwise.  (may be deprecated in the future)
     */
    @Deprecated public int[] getDecodedIntegers(int bias) throws CodecException, SeedException {
	return getDecompressedData(bias).getAsInt();
    }

    /**
     * Decode waveform data to an array of integer values.
     * Bias is assumed to be zero.
     */
    public int[] getDecodedIntegers() throws CodecException, SeedException  {
	return getDecodedIntegers(0);
    }

    /**
     * Decode waveform data to an array of float values.
     * Indicate bias as a carryover constant from a previous data record, but can
     * be set to zero otherwise.  (may be deprecated in the future)
     */
    @Deprecated public float[] getDecodedFloats(float bias) throws SteimException, SeedException, CodecException {
	// decode waveform data to an array of float values.
        //
        /******** OLD WAY
	int[] intDecode = null;  // some formats decompress to ints
	if (encoding.equals("Steim1")) {
	    intDecode = Steim1.decode(waveData,numSamples,swapBytes,(int) bias);
	} else if (encoding.equals("Steim2")) {
	    intDecode = Steim2.decode(waveData,numSamples,swapBytes, (int) bias);
	} else {
	    throw new SeedException("format " + encoding + " unavailable for float decoding");
	}
	if (intDecode != null) {
	    // if it was an integer decode result, then map it over to a float array
	    float [] floatDecode = new float[intDecode.length];
	    for (int i = 0; i < intDecode.length; i++) {
		floatDecode[i] = (float) intDecode[i];  // cast int as float
	    }
	    return floatDecode;
	} else return null;
         * ********/
        // just throw out bias value
        return getDecompressedData().getAsFloat();
    }

    /**
     * Decode waveform data to an array of float values.
     */
    public float[] getDecodedFloats() throws SteimException, SeedException, CodecException  {
	return getDecodedFloats(0.0F);  // default to 0 bias value
    }

    /**
     * Return the raw encoded byte stream.
     */
    public byte[] getEncodedBytes() {
	return waveData;
    }

    /**
     * Return the number of samples of this waveform.
     */
    public int getNumSamples() {
	return numSamples;
    }

    /**
     * Return the byte swap flag.  Indicates true if in VAX/8086 order, since
     * Java is by nature 68000/Sun order.
     */
    public boolean getSwapBytes() {
	return swapBytes;
    }

    /**
     * Return the String representing the encoding format of the data.
     * contained.
     */
    public String getEncoding() {
	return encoding;
    }

    /**
     * Change the number of samples representing this data, if you dare.
     */
    public void setNumSamples(int samples) {
	numSamples = samples;
    }

    /**
     * Toggle the swap byte flag to true or false.
     */
    public void setSwapBytes(boolean flag) {
	swapBytes = flag;
    }

    /**
     * Identify the encoding of the data contained in this object.
     */
    public void setEncoding(String format) {
	// we don't always know the encoding of the data that we read in, so
	// allow the application to change it after the fact
	encoding = format;
    }

    /**
     * Display a string description of this Waveform object's contents.
     */
    public String toString() {
	return "Waveform data with " + numSamples + " samples in " + waveData.length + " bytes with " +
	    encoding + " encoding and byte swap is " + swapBytes + ".";
    }

    // private methods

    /**
     * Given the integer array, encode it with the specified encoding type and store the
     * result in this object's byte array.  Also note the number of 
     * samples and the compression type.
     * <b>bias</b> can be set to the last value of the previous encoding
     * for differences continuity.
     */
    private void encodeWaveform(int[] data, int maxByteLen, String encoding, int bias) 
            throws SeedInputException, SeedException, SteimException, IOException  {
	if (data.length == 0) {
	    throw new SeedInputException("data length is zero");
	}
	if (encoding.equals("Steim1")) {
	    int numFrames = maxByteLen / steimFrameLen;
	    SteimFrameBlock steimFrames = Steim1.encode(data,numFrames,bias);
	    waveData = steimFrames.getEncodedData();
	    numSamples = steimFrames.getNumSamples();
	    this.encoding = encoding;
	    swapBytes = false;  // always generate Sun word order
	} else if (encoding.equals("Steim2")) {
          int numFrames = maxByteLen / steimFrameLen;
          SteimFrameBlock steimFrames = Steim2.encode(data, numFrames, bias);
          waveData = steimFrames.getEncodedData();
          numSamples = steimFrames.getNumSamples();
          this.encoding = encoding;
          swapBytes = false;  // always generate Sun word order
        } else if (encoding.equals("Int32Bit")) {  // we should have a standard method in Codec for this
            // write out the integers as a byte array
            // set up a byte array to write int words to
            ByteArrayOutputStream encodedData =
                    new ByteArrayOutputStream(maxByteLen);  // limit the size of return array by indicated max
            int maxNumInts = data.length;
            if (data.length > (maxByteLen/4)) maxNumInts = maxByteLen / 4;  // delimit max number of integers
            // set up interface to the array for writing the ints
            DataOutputStream intSerializer = new DataOutputStream(encodedData);
            for (int i = 0; i < maxNumInts; i++) {  // for each int word
                // write integer to byte stream
                intSerializer.writeInt(data[i]);
            }
            waveData = encodedData.toByteArray(); // return byte stream as array
            numSamples = (waveData.length / 4);  // length of byte array divided by 4 (32 bits)
            this.encoding = encoding;
            swapBytes = false;
	} else {
	    throw new SeedException("format " + encoding + " unavailable for integer encoding");
	}
	return;

    }

    /**
     * Given the integer array, encode it with the specified encoding type and store
     * the result in this object's byte array.  Bias is assumed to be 0.
     */
    private void encodeWaveform(int[] data, int maxByteLen, String encoding)
	throws SeedInputException, SeedException, SteimException, IOException {
	encodeWaveform(data,maxByteLen,encoding,0);
    }

    /**
     * Given the float array, encode it with the specified encoding and store the
     * result in this object's byte array.  Also note the number of samples and
     * the compression type.  <b>bias</b> can be set to the last value of the
     * previous encoding for differences continuity.
     */
    private void encodeWaveform(float[] data, int maxByteLen, String encoding, float bias)
	throws SeedInputException, SeedException {
	if (data.length == 0) {
	    throw new SeedInputException("data length is zero");
	}
	if (encoding.equals("someEncoding")) {
	    return;
	} else {
	    throw new SeedException("format " + encoding + " unavailable for float encoding");
	}
    }

    /**
     * Given the float array, encode it with the specified encoding and store the
     * result in this object's byte array.  Bias is assumed to be 0.
     */
    private void encodeWaveform(float[] data, int maxByteLen, String encoding)
	throws SeedInputException, SeedException {
	encodeWaveform(data,maxByteLen,encoding,0.0F);
    }

    // instance variables
    private final static int steimFrameLen = 64;
    /** The default maximum byte length which is the maximum value for Steim1/2 encoding. */
    public final static int defaultMaxByteLen = steimFrameLen * 63;
    private byte[] waveData = null;  // this holds the encoded waveform data
    private int numSamples = 0;      // this is the number of samples represented by the waveform data
    private String encoding = "none"; // this is the form of encoding that the data is stored in
    private boolean swapBytes = false; // this is the flag to indicate true if VAX/8086 byte order
    private Codec codec = null;     // this will drive all of our data decoders


    /**
     * Test method.
     */
    public static void main(String[] args) {
	try {
	    //int[] intData = {20,21,31,41,56,77,88,234,234,234,1,1,0,0,0,98,97,96,95,94,64,32,16,8,4,2,1,
	    //124093254,343241,31455,1554556,15535245,45,252465,23,24524,43463,5677546,343434,63443,663636,
	    //55633,73353434,5563563,35566356,346634,5345,43534,34,345634,34,326,346346,67,457,8,151345,
	    //3453236,46,3,314534,3534436,2465,3346346,3432,877,44224,5678,46735,252737,223,35246373,2247132,
	    //37572,56775346,54354,34624245,54622465,65632643,6456476,3451345,53454,3453456,4435,34534,466662,
	    //754434,644523,6454624,7766,2464343,46345626,354522,534,4434,5,3443,4,4543546,44356623,64224,24436344};
            //
            // args[0] is the encode type
            // currently support: Steim1, Steim2, Int32Bit
            String encodeType = "Steim1";  // default encoding for test
            if (args.length > 0) encodeType = args[0];  // get the first argument as an encoding type (See SeedEncodingResolver.java)
	    int[] intData = new int[4096];
	    int curNum = 45623;
	    for (int i = 0; i < 4096; i++) {
		intData[i] = curNum;
		curNum += 9731;
		if (curNum > 54622465) {
		    curNum -= 91;
		}
	    }
	    Waveform myWaveform = new Waveform(intData,defaultMaxByteLen,encodeType);  // encodes integer data into specified compression format
	    if (myWaveform == null) {
	    	throw new SeedException("NULL myWaveform returned");
	    }
	    int numSamples = myWaveform.getNumSamples();
	    System.out.println ("I have " + myWaveform.getEncoding() + " encoded " + numSamples + " samples out of " + intData.length + " samples entered.");
	    int[] intDecode = myWaveform.getDecodedIntegers();  // decodes waveform data into integer array
/*	    
	    StringBuffer intBuffer = new StringBuffer();
	    for (int i = numSamples-100; i < numSamples; i++) {
		if (i < 0) i = 0;
		intBuffer.append(intDecode[i]); 
		intBuffer.append(",");
	    }
	    System.out.println ("Decoding those values gives me this: " + intBuffer.toString());
	    intBuffer = new StringBuffer();
	    for (int i = numSamples-100; i < numSamples; i++) {
		if (i < 0) i = 0;
		intBuffer.append(intData[i]); 
		intBuffer.append(",");
	    }
	    System.out.println ("The original values consisted of   : " + intBuffer.toString());
*/
	    for (int i = 0; i < numSamples; i++) {
	      if (intData[i] != intDecode[i]) {
	        System.out.println("intData[" + i + "] = " + intData[i] + "  intDecode[" + i
	            + "] = " + intDecode[i]);
	      }
	    }
	    System.out.println("Done");
	} catch (Exception e) {
	    System.out.println("Caught exception: " + e);
	    ((Throwable) e).printStackTrace();
	}
    }

}
