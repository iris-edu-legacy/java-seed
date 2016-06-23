package edu.iris.Fissures.seed.builder;

import org.acplt.oncrpc.XdrBufferEncodingStream;

/**
 * Concrete Builder class for writing data in the XDR format.
 */
public class XdrOutput
{
  private final XdrBufferEncodingStream xdr; //the xdr buffer encoding stream

  /**
   * Constructs a new <code>XdrOutput</code> with a given
   * buffer.
   * @param bufferSize Size of buffer to store encoded data in.
   * @throws IllegalArgumentException if <code>bufferSize</code> is not
   *   a multiple of four.
   */
  public XdrOutput(int bufferSize)
  {
    xdr = new XdrBufferEncodingStream(bufferSize);
  }

  /**
   * Returns the buffer holding encoded data.
   * @return Buffer with encoded data.
   */
  public byte[] getXdrData()
  {
    final int dataLength = xdr.getXdrLength();
    final byte[] bufferData = xdr.getXdrData();
    if (dataLength >= bufferData.length)
    {
      return bufferData;
    }
    byte[] data = new byte[dataLength];
    for (int i = 0; i < dataLength; i++)
    {
      data[i] = bufferData[i];
    }
    return data;
  }

  /**
   * Returns the amount of encoded data in the buffer.
   * @return length of data encoded in buffer.
   */
  public int getXdrLength()
  {
    return xdr.getXdrLength();
  }

  /**
   * Encode the array of floats.
   * @param arr the array of floats.
   * @return true if successful.
   */
  public boolean xdr_array(float[] arr)
  {
    try
    {
      xdr.xdrEncodeFloatVector(arr);
    }
    catch (Exception ex)
    {
      return false;
    }
    return true;
  }

  /**
   * Encode the string.
   * @param s the string.
   * @param maxlength the maximum length of the string.
   * @return true if successful.
   */
  public boolean xdr_bytes(String s, int maxlength)
  {
    try
    {
      if (s.length() > maxlength)
        s = s.substring(0, maxlength);
      xdr.xdrEncodeString(s);
    }
    catch (Exception ex)
    {
      return false;
    }
    return true;
  }

  /**
   * Encode the double value.
   * @param n the double value.
   * @return true if successful.
   */
  public boolean xdr_double(double n)
  {
    try
    {
      xdr.xdrEncodeDouble(n);
    }
    catch (Exception ex)
    {
      return false;
    }
    return true;
  }

  /**
   * Encode the float value.
   * @param n the float value.
   * @return true if successful.
   */
  public boolean xdr_float(float n)
  {
    try
    {
      xdr.xdrEncodeFloat(n);
    }
    catch (Exception ex)
    {
      return false;
    }
    return true;
  }

  /**
   * Encode the int value.
   * @param n the int value.
   * @return true if successful.
   */
  public boolean xdr_int(int n)
  {
    try
    {
      xdr.xdrEncodeInt(n);
    }
    catch (Exception ex)
    {
      return false;
    }
    return true;
  }

  /**
   * Encode the long value.
   * @param n the long value.
   * @return true if successful.
   */
  public boolean xdr_long(long n)
  {
    try
    {
      xdr.xdrEncodeLong(n);
    }
    catch (Exception ex)
    {
      return false;
    }
    return true;
  }

  /**
   * Encode the short value.
   * @param n the short value.
   * @return true if successful.
   */
  public boolean xdr_short(short n)
  {
    try
    {
      xdr.xdrEncodeShort(n);
    }
    catch (Exception ex)
    {
      return false;
    }
    return true;
  }
}
