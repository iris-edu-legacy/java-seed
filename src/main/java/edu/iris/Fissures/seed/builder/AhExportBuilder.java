package edu.iris.Fissures.seed.builder;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.util.Format;
import edu.iris.Fissures.seed.container.Btime;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

/**
 * Concrete Builder class for exporting Blockette objects from
 * the SeedObjectContainer to the AH file format.
 */
public class AhExportBuilder
    extends BasicExportBuilder
{
  /**
   * Create an AH export builder.
   */
  public AhExportBuilder()
  {
    //init with no padding
    super(false, "^,A,(,33,34,50,(,52,(,43,48,53,58,60,),),),71,v,(,999,)");
    // set some default values
    builderType = "AH"; // indicate the type of builder we are
  }

  /**
   * Convert the A0 value.
   * @param A0 the A0 value.
   * @param number_poles the number of poles.
   * @param number_zeroes the number of zeroes.
   * @return the new A0 value.
   */
  protected static double covertA0(double A0, int number_poles,
                                   int number_zeroes)
  {
    // A0   = A0 * (2*pi)**(Np-Nz)
    return A0 * Math.pow(TWO_PI,
                         (double) (number_poles - number_zeroes));
  }

  /**
   * Convert Type B (Hz) to Type A (rad/sec).
   * @param n the Type B (Hz) complex number.
   */
  protected static void convertNumber(ComplexNumber n)
  {
    n.r = convertNumber(n.r);
    n.i = convertNumber(n.i);
  }

  /**
   * Output the AH information.
   */
  protected void output_info()
  {
    int dataIndex = 0;
    DataInfo dataInfo = getDataInfo(dataIndex);
    if (dataInfo == null)
      return;

    final double sampleRate = dataInfo.sampleRate;
    final String stationName = dataInfo.stationName;
    final String channelName = dataInfo.channelName;
    final char dataQuality = dataInfo.dataQuality;
    final Btime startTime = dataInfo.startTime;

    //get the data and find out the total number of samples
    final float[] floatData = getFloatData();
    final int totalNumSamples = floatData.length;
    Format pf;

    final AhInfo.AhHed hed = new AhInfo.AhHed();
    // Fill AH staion instrument response
    // if not channels == AT, SOH, LOG
    if (!channelName.equals("AT") &&
        !channelName.equals("LOG") &&
        !channelName.equals("SHO"))
    {
      fill_ah_resp(hed);
    }

    // place the azimuth and the dip into record comment.
    // Units is always DISP (m)
    pf = new Format("%3.1f");
    hed.record.rcomment =
        "Comp azm=" + pf.format(currentChannel.azimuth) +
        ",inc=" + pf.format(currentChannel.dip) + "; Disp (m);";

    // AH record information
    hed.record.delta = (float) (1.0 / sampleRate);
    hed.record.ndata = totalNumSamples;

    hed.record.abstime.setTime(startTime);
    hed.record.type = AhInfo.FLOAT;
    hed.record.floatData = floatData;

    // AH station information
    pf = new Format("%-5.5s\0");
    hed.station.code = pf.format(stationName);
    hed.station.chan = pf.format(channelName);
    final Blockette type_33 = find_type_33(currentStation.ownerCode);
    if (type_33 != null)
      hed.station.stype = type_33.toString(4); //Abbreviation description
    else
      hed.station.stype = "";
    hed.station.slat = currentChannel.latitude.floatValue();
    hed.station.slon = currentChannel.longitude.floatValue();
    hed.station.elev = currentChannel.elevation.floatValue();

    if (eventInfo != null)
    {
      hed.event.lat = eventInfo.latitude.floatValue();
      hed.event.lon = eventInfo.longitude.floatValue();
      hed.event.dep = eventInfo.depth.floatValue();

      // convert date/time
      hed.event.ot.setTime(eventInfo.originTime);
    }

    final StringBuffer outfile_name = new StringBuffer();
    final Format f02d = new Format("%02d");
    final Format f03d = new Format("%03d");
    final Format f04d = new Format("%04d");
    final String year = f04d.format(startTime.getYear());
    final String day = f03d.format(startTime.getDayOfYear());
    final String hour = f02d.format(startTime.getHour());
    final String minute = f02d.format(startTime.getMinute());
    final String second = f02d.format(startTime.getSecond());
    final String fracsec = f04d.format(startTime.getTenthMill());
    outfile_name.append(year);
    outfile_name.append(".");
    outfile_name.append(day);
    outfile_name.append(".");
    outfile_name.append(hour);
    outfile_name.append(".");
    outfile_name.append(minute);
    outfile_name.append(".");
    outfile_name.append(second);
    outfile_name.append(".");
    outfile_name.append(fracsec);
    outfile_name.append(".");
    outfile_name.append(currentStation.networkCode);
    outfile_name.append(".");
    outfile_name.append(currentStation.stationName);
    outfile_name.append(".");
    outfile_name.append(currentChannel.locationId);
    outfile_name.append(".");
    outfile_name.append(currentChannel.channelName);
    outfile_name.append(".");
    outfile_name.append(dataQuality); //input.type
    outfile_name.append(".AH");

    // describe the file being written
    System.err.print(
        "Writing " + currentStation.networkCode + ": " +
        currentStation.stationName + ": " +
        currentChannel.locationId + ": " + currentChannel.channelName + ", " +
        totalNumSamples +
        " samples (binary),");
    System.err.println(" starting " + year + "," + day + " " + hour + ":" +
                       minute + ":" + second + "." + fracsec + " UT");

    // find max amplitude
    maxamp(hed);

    // write ah header and data
    try
    {
      final XdrOutput xdr = AhInfo.creatXdrOutput(hed);
      final DataOutputStream dout =
          new DataOutputStream(new FileOutputStream(outfile_name.toString()));
      if (!AhInfo.xdr_puthead(hed,xdr))
      {
        System.err.println("Error writing header; output_ah");
        System.exit( -3);
      }
      if (!AhInfo.xdr_putdata(hed,xdr))
      {
        System.err.println("Error writing data; output_ah");
        System.exit( -3);
      }
      dout.write(xdr.getXdrData());
      dout.flush();
      dout.close();
    }
    catch (Exception ex)
    {
      System.err.println(
          "Error writing to file (" + outfile_name + "); output_ah: " + ex);
      ex.printStackTrace();
      System.exit( -3);
    }
  }

  /**
   * Fill the response information.
   * @param hed the AH header.
   */
  protected void fill_ah_resp(AhInfo.AhHed hed)
  {
    // find out the response type - set the gamma, etc
    final Blockette type_34 = find_type_34(currentChannel.signalUnitsCode);

    final int gamma;
    if (type_34 != null)
      gamma = determine_gamma(currentChannel, type_34);
    else
    {
      System.err.print(
          "Warning - couldn't find the abbrevation for the signal units code! Signal units code =" +
          currentChannel.signalUnitsCode + "\n");
      System.err.print("For station: " + currentChannel.station.stationName +
                       "; channel: " +
                       currentChannel.channelName + "\n\n");
      System.err.print("Setting the number of zeros to add to 0\n");
      gamma = 0;
    }

    final Blockette type_33 = find_type_33(currentChannel.instrumentCode);

    // transfer abbreviations
    if (type_33 != null)
    {
      // instrument name
      hed.record.rcomment += type_33.toString(4); //Abbreviation description
    }
    else
    {
      hed.station.stype = "N/A";
      hed.record.rcomment = "Not Found";
    }

    if (currentChannel.responses == null ||
        currentChannel.responses.size() <= 0)
    {
      System.err.print(
          "AH output(): Unable to continue! Unable to calulate A0. No responses.\n");
      return;
    }

    final ComplexNumberList responsePoles = new ComplexNumberList(); //list of ComplexNumber poles
    final ComplexNumberList responseZeroes = new ComplexNumberList(); //list of ComplexNumber zeroes
    if (get_A0(responsePoles, responseZeroes,
               currentChannel.responses, gamma, AhInfo.AhStationInfo.NOCALPTS,
               hed) == -1) // error
    {
      return;
    }

    // load up the poles and zeros
    ComplexNumber cn;
    final int num_poles = responsePoles.size();
    final int num_zeros = responseZeroes.size();
    hed.station.cal[0].pole.r = num_poles;
    hed.station.cal[0].zero.r = num_zeros;

    for (int i = 0; i < num_poles; i++)
    {
      cn = responsePoles.get(i);
      hed.station.cal[i + 1].pole.r = cn.r.floatValue();
      hed.station.cal[i + 1].pole.i = cn.i.floatValue();
    }

    for (int i = 0; i < num_zeros; i++)
    {
      cn = responseZeroes.get(i);
      hed.station.cal[i + 1].zero.r = cn.r.floatValue();
      hed.station.cal[i + 1].zero.i = cn.i.floatValue();
    }
  }

  /**
   * Get the A0 value.
   * @param responsePoles the response poles.
   * @param responseZeroes the response zeroes.
   * @param responses the responses.
   * @param gamma the gamma.
   * @param max_pzs the maximum poles and zeroes.
   * @param hed the AH header.
   * @return the A0 value.
   */
  protected float get_A0(ComplexNumberList responsePoles,
                         ComplexNumberList responseZeroes,
                         List responses, int gamma, int max_pzs,
                         AhInfo.AhHed hed)
  {
    if (responses == null || responses.size() <= 0)
    {
      System.err.println("Warning - couldn't find the Poles and Zeros!!\n");

      System.err.println(
          "For station: " + currentStation.stationName + "; channel: " +
          currentChannel.channelName);

      return -1; /* flag error condition */
    }

    ResponseInfo ri;
    final Iterator responseIterator = responses.iterator();
    float fn = Float.NaN;
    float sd = 0;
    float A0 = 1;
    float calculated_A0 = 0;

    while (responseIterator.hasNext())
    {
      ri = (ResponseInfo) responseIterator.next();

      if (ri.stage.intValue() == 1)
        fn = ri.normFreq.floatValue();

      A0 *= ri.aONorm.floatValue();

      if (responsePoles.size() + ri.poles.length >= max_pzs)
      {

        System.err.println(
            "Warning, exceeded maximum number of poles. Clipping at stage =" +
            ri.stage);
        break;
      }

      if (responseZeroes.size() + ri.zeroes.length >= max_pzs)
      {
        System.err.println(
            "Warning, exceeded maximum number of zeros. Clipping at stage =" +
            ri.stage);
        System.err.println("Network: " + currentStation.networkCode +
                           ", Station: " + currentStation.stationName +
                           ", Channel: " + currentChannel.channelName +
                           ", Location:" + currentChannel.locationId);
        break;
      }

      /*
       * First, AH assumes the units of the poles and zeros are rad/sec,
       * so we convert Type B (Hz) to Type A (rad/sec) if necessary.
       *
       * If Type==B then convert to type A format by:
       *
       * P(n) = 2*pi*P(n)      { n=1...Np }
       * Z(m) = 2*pi*Z(m)      { m=1...Nz }
       * A0   = A0 * (2*pi)**(Np-Nz)
       */

      if (ri.type == 'B') //type is analog - 'B'
      {
        for (int i = 0; i < ri.poles.length; i++)
        {
          convertNumber(ri.poles[i]);
        }

        for (int i = 0; i < ri.zeroes.length; i++)
        {
          convertNumber(ri.zeroes[i]);
        }

        // A0   = A0 * (2*pi)**(Np-Nz)
        A0 = (float) covertA0(A0, ri.poles.length, ri.zeroes.length);
      }

      for (int i = 0; i < ri.poles.length; i++)
      {
        responsePoles.add(ri.poles[i]);
      }

      for (int i = 0; i < ri.zeroes.length; i++)
      {
        responseZeroes.add(ri.zeroes[i]);
      }

      /* add zeros */
      if (ri.stage.intValue() == 1)
      {
        for (int i = 0; i < gamma; i++)
        {
          responseZeroes.add(new ComplexNumber());
        }
      }
    }

    /*
     * Second, there is no place to specify the units of the response.
     * An AH file assumes that if an instrument response is deconvolved,
     * the seismogram will be displacement in meters.
     * Convert velocity or acceleration to displacement:
     *
     * Convert to displacement:
     *
     * if acceleration, gamma=2    \
     * elseif velocity,     gamma=1 \
     * elseif displacement, gamma=0  \___Done above
     * else  print error message     /
     * endif                        /
     *
     * Sd = Sd * (2*pi*fs)**gamma
     * Nz = Nz + gamma
     * set values of new zeros equal zero
     * A0 = A0 / (2*pi*fn)**gamma
     * Units = M - Displacement Meters
     */
    if (currentChannel.sensitivity == null || currentChannel.frequency == null)
    {
      System.err.println(
          "WARNING - couldn't find - blockette (58/48) stage zero!!");
      System.err.println("For station: " + currentStation.stationName +
                         "; channel: " + currentChannel.channelName);
      return -1; // error condition
    }
    if (Float.isNaN(fn))
    {
      System.err.println(
          "WARNING - couldn't find norm freq!!");
      System.err.println("For station: " + currentStation.stationName +
                         "; channel: " + currentChannel.channelName);
      return -1; // error condition
    }
    sd = currentChannel.sensitivity.floatValue();
    final float fs = currentChannel.frequency.floatValue();
    sd *= Math.pow(TWO_PI * fs, (double) gamma);
    A0 = (float) (A0 / Math.pow( (double) (TWO_PI * fn), (double) gamma));

    /*
     * Third, there is no place in the AH header to specify either
     * the frequency of normalization or the frequency of the
     * digital sensitivity.  This is not a problem as long as these
     * two are the same.  If they are different then evaluate the
     * normalization at the frequency of the digital sensitivity.
     *
     *
     * if fn is not equal to fs then
     *  A0 = abs(prod{n=1...Np} [2*pi*i*fs - P(n)] /
                        prod{m=1..Nz} [2*pi*i*fs - Z(m)])
     *
     * endif
     * i = sqrt(-1)
     */

    // default to the A0 as computed above
    if ( (responseZeroes.size() == 0) || (responsePoles.size() == 0))
      calculated_A0 = A0;
    else
      calculated_A0 = (float) calc_A0(responsePoles, responseZeroes, fs);

    if (!cmp_floats(fn, fs))
      A0 = calculated_A0;
    else
    // they are the same, perform consistancy check
    {
      // check to see if they differ by greater than .5%
      if (Math.abs( (A0 - calculated_A0) / calculated_A0) > .005)
      {
        System.err.println(
            "Warning, Normalization given for station: " +
            currentStation.stationName +
            ", channel " + currentChannel.channelName + " is :" + A0 + ".");
        System.err.println(
            "This is inconsistent with the value calculated from poles and zeros: " +
            calculated_A0 + ".");
        // use the calculated A0
        A0 = calculated_A0;
      }
    }

    hed.station.DS = sd;
    hed.station.A0 = A0;
    return A0;
  }

  /**
   * Calculates the A0 value.
   * @param responsePoles the response poles.
   * @param responseZeroes the response zeroes.
   * @param ref_freq the reference frequency.
   * @return the A0 value.
   */
  protected double calc_A0(ComplexNumberList responsePoles,
                           ComplexNumberList responseZeroes, float ref_freq)
  {
    int i;

    final int n_ps = responsePoles.size();
    final int n_zs = responseZeroes.size();
    DoubleComplexNumber f0 = new DoubleComplexNumber();
    DoubleComplexNumber hold = new DoubleComplexNumber();
    double a0;

    f0.r = 0;

    f0.i = TWO_PI * ref_freq;

    hold.i = responseZeroes.get(0).i.doubleValue();
    hold.r = responseZeroes.get(0).r.doubleValue();

    DoubleComplexNumber denom = DoubleComplexNumber.sub(f0, hold);
    for (i = 1; i < n_zs; i++)
    {
      hold.i = responseZeroes.get(i).i.doubleValue();
      hold.r = responseZeroes.get(i).r.doubleValue();
      denom = DoubleComplexNumber.mult(denom, DoubleComplexNumber.sub(f0, hold));
    }

    hold.i = responsePoles.get(0).i.doubleValue();
    hold.r = responsePoles.get(0).r.doubleValue();

    DoubleComplexNumber numer = DoubleComplexNumber.sub(f0, hold);
    for (i = 1; i < n_ps; i++)
    {
      hold.i = responsePoles.get(i).i.doubleValue();
      hold.r = responsePoles.get(i).r.doubleValue();
      numer = DoubleComplexNumber.mult(numer, DoubleComplexNumber.sub(f0, hold));
    }

    a0 = DoubleComplexNumber.div(numer, denom).abs();
    return a0;
  }

  protected static boolean cmp_floats(float f1, float f2)
  {
    Format f = new Format("%6.6f");
    String f_1 = f.format(f1);
    String f_2 = f.format(f2);
    return f_1.equals(f_2);
  }

  /**
   * Determines the gamma value.
   * @param ci the channel information.
   * @param blk the Blockette.
   * @return the gamma value.
   */
  protected static int determine_gamma(ChannelInfo ci, Blockette blk)
  {
    String p = null;

    if (blk != null && blk.getType() == 34)
    {
      if ( (p = blk.toString(5)) != null && p.length() > 0) //if description exists
      {
        final String description = p.toUpperCase();
        if (description.indexOf("VEL") >= 0)
        {
          return 1;
        }
        if (description.indexOf("ACCEL") >= 0)
        {
          return 2;
        }

        if (description.indexOf("DISP") >= 0)
        {
          // no zeros - just go on
          return 0;
        }
      }
      else if ( (p = blk.toString(4)) != null && p.length() > 0) //if name exists
      {
        if (p.equals("M"))
          return 0; // displacement

        if (p.equals("M/S"))
        {
          return 1; // Velocity
        }

        if (p.equals("M/S**2"))
        {
          return 2; // Acceleration
        }
      }
    }

    // if we got here - flag error !
    if (p != null)
      System.err.print(
          "WARNING - unknown response type - we only know acceleration, velocity, and displacement.\nFound: " +
          p + "\n");
    System.err.print(
        "For station: " + ci.station.stationName + "; channel: " +
        ci.channelName + "\n");
    System.err.print(
        "Assuming a gamma of zero!\n");
    return 0;
  }

  /**
   * Determines the maximum absolute amplitude of the data array, and
   *		places that number in hed.record.maxamp.
   * @param hed the AH header.
   */
  public void maxamp(AhInfo.AhHed hed)
  {
    if (hed.record.type == AhInfo.DOUBLE)
    {
      double min, max;
      double[] data = hed.record.doubleData;
      if (data == null || data.length <= 0)
        return;
      min = max = data[0];
      for (int i = 1; i < data.length; i++)
      {
        max = Math.max(max, data[i]);
        min = Math.min(min, data[i]);
      }
      if (Math.abs(max) > Math.abs(min))
        hed.record.maxamp = (float) max;
      else
        hed.record.maxamp = (float) - min;
    }
    else
    {
      float min, max;
      float[] data = hed.record.floatData;
      if (data == null || data.length <= 0)
        return;
      min = max = data[0];
      for (int i = 1; i < data.length; i++)
      {
        max = Math.max(max, data[i]);
        min = Math.min(min, data[i]);
      }
      if (Math.abs(max) > Math.abs(min))
        hed.record.maxamp = max;
      else
        hed.record.maxamp = -min;
    }
  }

  // inner classes

  /**
   * The complex number list.
   */
  protected static class ComplexNumberList
  {
    protected final List list = new Vector(8, 8);

    /**
     * Adds a complex number to the list.
     * @param n the complex number.
     */
    public void add(ComplexNumber n)
    {
      list.add(n);
    }

    /**
     * Gets the complex number from the list.
     * @param index the index.
     * @return the complex number or null if none.
     */
    public ComplexNumber get(int index)
    {
      if (index < list.size())
        return (ComplexNumber) list.get(index);
      return null;
    }

    /**
     * Gets the size of the list.
     * @return the size of the list.
     */
    public int size()
    {
      return list.size();
    }
  };

  /**
   * Double precision complex number.
   */
  protected static class DoubleComplexNumber
  {
    double r = 0;
    double i = 0;

    /**
     * Gets the absolute value of the complex number.
     * @return the absolute value of the complex number.
     */
    public double abs()
    {
      return (Math.pow( (double) (r * r) + (i * i), 0.5));
    }

    /**
     * Divides the complex numbers.
     * @param a a complex number.
     * @param b a complex number.
     * @return the result.
     */
    public static DoubleComplexNumber div(DoubleComplexNumber a,
                                          DoubleComplexNumber b)
    {
      DoubleComplexNumber div = new DoubleComplexNumber();
      b.i = -b.i;
      div = mult(a, b);
      div.r /= b.r * b.r + b.i * b.i;
      div.i /= b.r * b.r + b.i * b.i;
      return div;
    }

    /**
     * Multiplies the complex numbers.
     * @param a a complex number.
     * @param b a complex number.
     * @return the result.
     */
    public static DoubleComplexNumber mult(DoubleComplexNumber a,
                                           DoubleComplexNumber b)
    {
      DoubleComplexNumber mult = new DoubleComplexNumber();
      mult.r = (a.r * b.r) - (a.i * b.i);
      mult.i = (a.r * b.i) + (a.i * b.r);
      return mult;
    }

    /**
     * Subtracts the complex numbers.
     * @param a a complex number.
     * @param b a complex number.
     * @return the result.
     */
    public static DoubleComplexNumber sub(DoubleComplexNumber a,
                                          DoubleComplexNumber b)
    {
      DoubleComplexNumber sub = new DoubleComplexNumber();
      sub.r = a.r - b.r;
      sub.i = a.i - b.i;
      return sub;
    }
  }
}
