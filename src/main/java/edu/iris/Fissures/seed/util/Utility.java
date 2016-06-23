package edu.iris.Fissures.seed.util;

/**
 * Generic class providing static methods for converting between integer numbers
 * and byte arrays.
 *
 * @author Philip Crotwell
 * @author Robert Casey
 * @version 6/6/2002
 */

public class Utility  {

	/**
	 * Concatenate two bytes to a short integer value.  Accepts a high and low byte to be
	 * converted to a 16-bit integer.  if swapBytes is true, then <b>b</b> becomes
	 * the high order byte.
	 * @param a high order byte
	 * @param b low order byte
	 * @param swapBytes reverse the roles of the first two parameters
	 * @return short integer representation of the concatenated bytes
	 */
	public static short bytesToShort(byte a, byte b, boolean swapBytes) {
		if (swapBytes) {
			return (short)((a & 0xff) + (b & 0xff) << 8);
		} else {
			return (short)(((a & 0xff) << 8) + (b & 0xff));
		}
	}


	// convert signed bytes to an integer value

	/**
	 * Convert a single byte to a 32-bit int, with sign extension.
	 * @param a signed byte value
	 * @return 32-bit integer
	 */
	public static int bytesToInt(byte a) {
		return (int)a;  // whatever the high-order bit is set to is extended into integer 32-bit space
	}

	/**
	 * Concatenate two bytes to a 32-bit int value.  <b>a</b> is the high order
	 * byte in the resulting int representation, unless swapBytes is true, in
	 * which <b>b</b> is the high order byte.
	 * @param a high order byte
	 * @param b low order byte
	 * @param swapBytes byte order swap flag
	 * @return 32-bit integer
	 */
	public static int bytesToInt(byte a, byte b, boolean swapBytes) {
		// again, high order bit is expressed left into 32-bit form
		if (swapBytes) {
			return (a & 0xff) + ((int)b << 8);
		} else {
			return ((int)a << 8) + (b & 0xff);
		}
	}

	/**
	 * Concatenate three bytes to a 32-bit int value.  Byte order is <b>a,b,c</b>
	 * unless swapBytes is true, in which case the order is <b>c,b,a</b>.
	 * @param a highest order byte
	 * @param b second-highest order byte
	 * @param c lowest order byte
	 * @param swapBytes byte order swap flag
	 * @return 32-bit integer
	 */
	public static int bytesToInt(byte a, byte b, byte c, boolean swapBytes) {
		if (swapBytes) {
			return (a & 0xff ) + ((b & 0xff) << 8 ) + ((int)c  << 16);
		} else {
			return ((int)a  << 16 ) + ((b & 0xff) << 8 ) + (c & 0xff);
		}
	}


	/**
	 * Concatenate four bytes to a 32-bit int value.  Byte order is <b>a,b,c,d</b>
	 * unless swapBytes is true, in which case the order is <b>d,c,b,a</b>.
	 * <i>Note:</i> This method will accept unsigned and signed byte
	 * representations, since high bit extension is not a concern here.
	 * Java does not support unsigned integers, so the maximum value is not as
	 * high as would be the case with an unsigned integer.  To hold an unsigned
	 * 32-bit value, use uBytesToLong().
	 * @param a highest order byte
	 * @param b second-highest order byte
	 * @param c second-lowest order byte
	 * @param d lowest order byte
	 * @param swapBytes byte order swap flag
	 * @return 32-bit integer
	 * @see edu.iris.Fissures.seed.util.Utility#uBytesToLong(byte,byte,byte,byte,boolean)
	 */
	public static int bytesToInt(byte a, byte b, byte c, byte d, boolean swapBytes) {
		if (swapBytes) {
			return ((a & 0xff) ) +
				((b & 0xff) << 8 ) +
				((c & 0xff) << 16 ) +
				((d & 0xff) << 24);
		} else {
			return ((a & 0xff) << 24 ) +
				((b & 0xff) << 16 ) +
				((c & 0xff) << 8 ) +
				((d & 0xff) );
		}    
	}


	// convert unsigned byte representations to an integer value

	/**
	 * Treat byte value as an unsigned value and convert to a 32-bit int value.
	 * @param a unsigned byte value
	 * @return positive 32-bit integer
	 */
	public static int uBytesToInt(byte a) {
		// we "and" with 0xff in order to get the sign correct (pos)
		// this extends zeroes left into 32-bit	space
		return a & 0xff;
	}

	/**
	 * Conatenate two unsigned byte values into a 32-bit integer.
	 * @param a high order unsigned byte
	 * @param b low order unsigned byte
	 * @param swapBytes if true, <b>b</b> becomes the high order byte
	 * @return positive 32-bit integer
	 */
	public static int uBytesToInt(byte a, byte b, boolean swapBytes) {
		// we "and" with 0xff to get the sign correct (pos)
		if (swapBytes) {
			return (a & 0xff) + ((b & 0xff) << 8);
		} else {
			return ((a & 0xff) << 8) + (b & 0xff);
		}
	}

	/**
	 * Conacatenate four unsigned byte values into a long integer.
	 * This method puts out a long value because a large unsigned 32-bit value would
	 * exceed the capacity of an int, which is considered signed in Java.
	 * @param a highest-order byte
	 * @param b second-highest order byte
	 * @param c second-lowest order byte
	 * @param d lowest order byte
	 * @param swapBytes if true, byte order is <b>d,c,b,a</b>, else order is
	 * <b>a,b,c,d</b>
	 * @return positive long integer
	 */
	public static long uBytesToLong(byte a, byte b, byte c, byte d, boolean swapBytes) {
		if (swapBytes) {
			return ((a & 0xffL) ) +
				((b & 0xffL) << 8 ) +
				((c & 0xffL) << 16 ) +
				((d & 0xffL) << 24);
		} else {
			return ((a & 0xffL) << 24 ) +
				((b & 0xffL) << 16 ) +
				((c & 0xffL) << 8 ) +
				((d & 0xffL) );
		}    
	}

	/**
	 * Convert a long value to a 4-byte array.
	 * @param a long integer
	 * @return byte[4] array
	 */
	public static byte[] longToIntBytes(long a) {
		byte[] returnByteArray = new byte[4]; //int is 4 bytes
		returnByteArray[0] = (byte)((a & 0xff000000)>>24);
		returnByteArray[1] = (byte)((a & 0x00ff0000)>>16);
		returnByteArray[2] = (byte)((a & 0x0000ff00)>>8);
		returnByteArray[3] = (byte)((a & 0x000000ff));
		return returnByteArray;
	}

	/**
	 * Convert an int value to a 2-byte array.
	 * @param a int value
         * @param swapBytes flag whether to byte swap output
	 * @return byte[2] array
	 */
	public static byte[] intToShortBytes(int a, boolean swapBytes) {
		byte[] returnByteArray = new byte[2];  //short is 2 bytes
                if (swapBytes) {
                    returnByteArray[1] = (byte)((a & 0x0000ff00)>>8);
                    returnByteArray[0] = (byte)((a & 0x000000ff));
                } else {
                    returnByteArray[0] = (byte)((a & 0x0000ff00)>>8);
                    returnByteArray[1] = (byte)((a & 0x000000ff));
                }
		return returnByteArray;
	}

        public static byte[] intToShortBytes(int a) {
            return intToShortBytes(a,false);
        }


	// miscellaneous utilities

	/**
	 * Return a byte array of length <b>requiredBytes</b> that contains the
	 * contents of <b>source</b> and is padded on the end with <b>paddingByte</b>.
	 * If <b>requiredBytes</b> is less than or equal to the length of
	 * <b>source</b>, then <b>source</b> will simply be returned.
	 * @param source byte array to have <b>paddingByte</b>(s) appended to
	 * @param requiredBytes the length in bytes of the returned byte array
	 * @param paddingByte the byte value that will be appended to the array to
	 * fill out the required byte size of the return array
	 * @return byte array of size <b>requiredBytes</b>
	 */
	public static byte[]  pad(byte[] source,int requiredBytes, byte paddingByte) {
		if (source.length >= requiredBytes) {
			return source;
		} else {
			byte[] returnByteArray = new byte[requiredBytes];
			System.arraycopy(source, 0, returnByteArray, 0, source.length);
			for(int i = source.length; i<requiredBytes; i++)
			{
				returnByteArray[i] = (byte)paddingByte;
			}
			return returnByteArray;
		}
	}

	/**
	 * Return a byte array which is a subset of bytes from <b>source</b>
	 * beginning with index <b>start</b> and stopping just before index
	 * <b>end</b>.
	 * @param source source byte array
	 * @param start starting index, inclusive
	 * @param end ending index, exclusive
	 * @return byte array of length <b>start</b>-<b>end</b>
	 */
	public static byte[] format(byte[] source, int start, int end) {
		byte[] returnByteArray = new byte[start-end+1];
		int j = 0;
		for(int i = start; i < end; i++,j++) {
			returnByteArray[j] = source[i];
		}
		return returnByteArray;
	}


	/**
	 * Test method.
	 * @param args not used.
	 */
	public static void main (String[] args)
	{
		int a = 256;
		byte a1 = (byte)((a & 0xff000000)>>24);
		byte a2 = (byte)((a & 0x00ff0000)>>16);
		byte a3 = (byte)((a & 0x0000ff00)>>8);
		byte a4 = (byte) ((a & 0x000000ff));
		System.out.println("first byte is " + a1);
		System.out.println("2 byte is " + a2);
		System.out.println("3 byte is " + a3);
		System.out.println("4  byte is " + a4);
		byte[] source = new byte[5];
		for(int i=0; i< 5; i++)
			source[i] = (byte)10;
		byte[] output = Utility.pad(source, 5, (byte)32);
		for(int k=output.length-1; k > -1; k--)
		{
			System.out.println("byte" + k +" " + output[k]);
		}
	}

}
