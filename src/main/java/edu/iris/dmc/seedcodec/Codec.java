package edu.iris.dmc.seedcodec;

/**
 * Codec.java
 * 
 * 
 * Created: Thu Nov 21 13:01:20 2002
 * 
 * @author <a href="mailto:crotwell@seis.sc.edu">Philip Crotwell</a>
 * @version 1.0.5
 */
public class Codec implements B1000Types {

    public Codec() {}
    
    /**
     * Decompresses the data into the best java primitive type for the given
     * compression and returns it.
     * 
     * @param type integer representation of the encoding type
     * @param b wave data
     * @param numSamples number of samples
     * @param swapBytes The swap order of the data itself is specified for the channel in the data format dictionary
     * @return
     * @throws CodecException
     * @throws UnsupportedCompressionType
     */
    public DecompressedData decompress(int type,
                                       byte[] b,
                                       int numSamples,
                                       boolean swapBytes)
            throws CodecException, UnsupportedCompressionType {
        DecompressedData out;
        int[] itemp;
        short[] stemp;
        float[] ftemp;

        int offset = 0;
        switch(type){
            case SHORT:
            case DWWSSN:
                // 16 bit values
                if(b.length < 2 * numSamples) {
                    throw new CodecException("Not enough bytes for "
                            + numSamples + " 16 bit data points, only "
                            + b.length + " bytes.");
                }
                stemp = new short[numSamples];
                for(int i = 0; i < stemp.length; i++) {
                    stemp[i] = Utility.bytesToShort(b[offset],
                                                    b[offset + 1],
                                                    swapBytes);
                    offset += 2;
                }
                out = new DecompressedData(stemp);
                break;
            case 2:
                // 24 bit values
                if(b.length < 3 * numSamples) {
                    throw new CodecException("Not enough bytes for "
                            + numSamples + " 24 bit data points, only "
                            + b.length + " bytes.");
                }
                itemp = new int[numSamples];
                for(int i = 0; i < numSamples; i++) {
                    itemp[i] = Utility.bytesToInt(b[offset],
                                                  b[offset + 1],
                                                  b[offset + 2],
                                                  swapBytes);
                    offset += 3;
                }
                out = new DecompressedData(itemp);
                break;
            case 3:
                // 32 bit integers
                if(b.length < 4 * numSamples) {
                    throw new CodecException("Not enough bytes for "
                            + numSamples + " 32 bit data points, only "
                            + b.length + " bytes.");
                }
                itemp = new int[numSamples];
                for(int i = 0; i < numSamples; i++) {
                    itemp[i] = Utility.bytesToInt(b[offset],
                                                  b[offset + 1],
                                                  b[offset + 2],
                                                  b[offset + 3],
                                                  swapBytes);
                    offset += 4;
                }
                out = new DecompressedData(itemp);
                break;
            case 4:
                // 32 bit floats
                if(b.length < 4 * numSamples) {
                    throw new CodecException("Not enough bytes for "
                            + numSamples + " 32 bit data points, only "
                            + b.length + " bytes.");
                }
                ftemp = new float[numSamples];
                for(int i = 0; i < numSamples; i++) {
                    ftemp[i] = Float.intBitsToFloat(Utility.bytesToInt(b[offset],
                                                                       b[offset + 1],
                                                                       b[offset + 2],
                                                                       b[offset + 3],
                                                                       swapBytes));
                    offset += 4;
                }
                out = new DecompressedData(ftemp);
                break;
            case 5:
                // 64 bit doubles
                if(b.length < 8 * numSamples) {
                    throw new CodecException("Not enough bytes for "
                            + numSamples + " 64 bit data points, only "
                            + b.length + " bytes.");
                }
                // ToDo .. implement this type....
                throw new UnsupportedCompressionType("Type " + type
                        + " is not supported at this time.");
                // break;
            case STEIM1:
                // steim 1
                itemp = Steim1.decode(b, numSamples, false, 0);  // swapBytes field always false for Steim Blocks
                out = new DecompressedData(itemp);
                break;
            case STEIM2:
                // steim 2
                itemp = Steim2.decode(b, numSamples, false, 0);  // swapBytes field always false for Steim Blocks
                out = new DecompressedData(itemp);
                break;
            case CDSN:
                itemp = Cdsn.decode(b, numSamples, swapBytes);
                out = new DecompressedData(itemp);
                break;
            case SRO:
                itemp = Sro.decode(b, numSamples, swapBytes);
                out = new DecompressedData(itemp);
                break;    
            default:
                // unknown format????
                throw new UnsupportedCompressionType("Type " + type
                        + " is not supported at this time.");
        } // end of switch ()
        return out;
    }

    
    /**
     * returns an integer that represent the java primitive that the data will
     * decompress to. This is to allow for SEED types 4 and 5, float and
     * double, which cannot be represented as int without a loss of precision.
     * 
     * @param type integer representation of the encoding type
     * @return
     * @throws UnsupportedCompressionType
     */
    public int getDecompressedType(int type) throws UnsupportedCompressionType {
        if(type == INT24 || type == INTEGER || type == STEIM1 || type == STEIM2 || type == CDSN || type == SRO) {
            return INTEGER;
        } else if(type == SHORT || type == DWWSSN) {
            return SHORT;
        } else if(type == FLOAT) {
            return FLOAT;
        } else if(type == DOUBLE) {
            return DOUBLE;
        } // end of if ()
        // ????
        throw new UnsupportedCompressionType("Type " + type
                + " is not supported at this time.");
    }
}// Codec
