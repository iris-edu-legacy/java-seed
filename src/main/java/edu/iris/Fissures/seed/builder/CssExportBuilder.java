package edu.iris.Fissures.seed.builder;

import java.io.File;
import edu.iris.Fissures.seed.util.Format;
import edu.iris.Fissures.seed.container.Btime;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import edu.iris.Fissures.seed.container.Blockette;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.BufferedReader;
import java.io.FileReader;

public class CssExportBuilder
    extends BasicExportBuilder
{
  /**
   * Create a CSS export builder.
   */
  public CssExportBuilder()
  {
    //init with no padding
    super(false, "^,A,(,32,33,34,50,(,52,(,43,48,53,58,60,),),),71,v,(,999,)");
    // set some default values
    builderType = "CSS"; // indicate the type of builder we are
  }

  /**
   * Writes the data.
   * @param outfile_name the output filename.
   * @param intData the integer data values.
   * @return true if successful, false otherwise.
   */
  protected boolean writeData(String outfile_name, int[] intData)
  {
    final DataOutputStream dout;
    try
    {
      dout = new DataOutputStream(new FileOutputStream(outfile_name));
    }
    catch (Exception ex)
    {
      System.err.print("\tWARNING (output_css):  ");
      System.err.print("Unable to open output file " + outfile_name + ": " + ex);
      System.err.print("\tExecution aborted.\n");
      return false;
    }

    try
    {
      int v;
      for (int i = 0; i < intData.length; i++)
      {
        dout.writeInt(intData[i]);
      }
      dout.flush();
      dout.close();
    }
    catch (Exception ex)
    {
      System.err.print("WARNING (output_css):  ");
      System.err.print("failed to properly write CSS data to " + outfile_name +
                       ": " + ex);
      System.err.print("\tExecution aborted.\n");
      return false;
    }
    return true;
  }

  /**
   * Writes the text file.
   * @param textFile the text file.
   * @param text the text.
   * @return true if successful, false otherwise.
   */
  protected boolean writeTextFile(File textFile, String text)
  {
    final PrintStream pout;
    try
    {
      pout = new PrintStream(new FileOutputStream(textFile, true));
    }
    catch (Exception ex)
    {
      System.err.print("\tWARNING (output_css):  ");
      System.err.print("Output file " + textFile.getPath() +
                       " is not available for writing : " + ex);
      System.err.print("\tExecution continuing.\n");
      return false;
    }

    try
    {
      pout.print(text);
      pout.flush();
      pout.close();
    }
    catch (Exception ex)
    {
      System.err.print("WARNING (output_css):  ");
      System.err.print("failed to properly write to " +
                       textFile.getPath() + ": " + ex);
      System.err.print("\tExecution continuing.\n");
      return false;
    }
    return true;
  }

  /**
   * Output the CSS information.
   */
  protected void output_info()
  {
    File outputFile;
    StringBuffer Wfdisc_buf;
    int dataIndex = 0;
    DataInfo dataInfo = getDataInfo(dataIndex);
    if (dataInfo == null)
      return;

    final String stationName = dataInfo.stationName;
    final String channelName = dataInfo.channelName;
    final char dataQuality = dataInfo.dataQuality;
    final double sampleRate = dataInfo.sampleRate;
    final Btime stime = currentChannel.startEffTime; // start time of channel info

    //get the data and find out the total number of samples
    final int[] intData = getIntegerData();
    final int totalNumSamples = intData.length;

    // WFDISC information
    outputFile = new File("rdseed.wfdisc");
    wfid = 1;
    if (outputFile.canRead())
    {
      wfid = (int) (outputFile.length() / 284) + 1;
    }
    Wfdisc_buf = new StringBuffer(Wfdisc_buf_size);

    Format pf;
    pf = new Format("%-6s ");
    Wfdisc_buf.append(pf.format(stationName));
    pf = new Format("%-8s ");
    Wfdisc_buf.append(pf.format(channelName));
    pf = new Format("%17.5f ");
    final Btime startTime = dataInfo.startTime;
    final double startEpochTime = getEpochTime(startTime);
    Wfdisc_buf.append(pf.format(startEpochTime));
    pf = new Format("%8d ");
    Wfdisc_buf.append(pf.format(wfid));
    Wfdisc_buf.append(pf.format( -1));
    Wfdisc_buf.append(pf.format(startTime.getYear() * 1000
                                + startTime.getDayOfYear()));
    pf = new Format("%17.5f ");
    Wfdisc_buf.append(pf.format(
        startEpochTime + (totalNumSamples - 1) / sampleRate));
    pf = new Format("%8d ");
    Wfdisc_buf.append(pf.format(totalNumSamples));
    pf = new Format("%11.7f ");
    Wfdisc_buf.append(pf.format(sampleRate));
    pf = new Format("%16.6f ");
    double calib = 0;
    double calper = 0;

    //get calibration information
    if (currentChannel.sensitivity == null || currentChannel.frequency == null)
    {
      System.err.print(
          "Warning - couldn't find stage 0 - blockette (58)!!\n");
      System.err.print(
          "For station: " + currentStation.stationName + "; channel: " +
          currentChannel.channelName + "\n");
      System.err.print("Calibration variable will be set to Zero\n");
    }
    else
    {
      // look for velocity or acceleration channels, AH only likes displacement
      // so integrate response by deleting zeroes
      double disp_factor = 1.0;
      // find out the response type - set the gamma, etc
      final Blockette blk = find_type_34(currentChannel.signalUnitsCode);
      String p = null;
      if (blk != null && blk.getType() == 34)
      {
        if ( (p = blk.toString(5)) != null && p.length() > 0) //if description exists
        {
          final String description = p.toUpperCase();
          if (description.indexOf("VEL") >= 0)
          {
            disp_factor = TWO_PI * currentChannel.frequency.doubleValue();
          }
          else if (description.indexOf("ACCEL") >= 0)
          {
            disp_factor = 4.0 * Math.PI * Math.PI *
                currentChannel.frequency.doubleValue();
          }
        }
      }
      calib = 1.0 /
          ( (currentChannel.sensitivity.doubleValue() * disp_factor) /
           1000000000.0);
      calper = 1.0 / (currentChannel.frequency.doubleValue());
    }

    Wfdisc_buf.append(pf.format(calib));
    Wfdisc_buf.append(pf.format(calper));
    pf = new Format("%-6s ");
    Wfdisc_buf.append(pf.format("-"));
    pf = new Format("%1s ");
    Wfdisc_buf.append(pf.format("o"));
    pf = new Format("%-2s ");
    Wfdisc_buf.append(pf.format("s4"));
    pf = new Format("%1s ");
    Wfdisc_buf.append(pf.format("-"));
    Wfdisc_buf.append(
        ".                                                                 ");

    StringBuffer outfile_name = new StringBuffer(18);
    pf = new Format("rdseed%08d");
    outfile_name.append(pf.format(wfid));
    pf = new Format(".%c.w");
    outfile_name.append(pf.format(dataQuality));
    Wfdisc_buf.append(outfile_name);
    Wfdisc_buf.append("              ");

    pf = new Format("%10d ");
    Wfdisc_buf.append(pf.format(0));
    pf = new Format("%8d ");
    Wfdisc_buf.append(pf.format( -1));
    pf = new Format("%-17s\n");
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
    String lddate = sdf.format(new Date()); // current date and time
    Wfdisc_buf.append(pf.format(lddate));

    // describe the file being written

    pf = new Format("%5d");
    System.out.print(
        "Writing " + currentStation.stationName + ", " +
        currentChannel.channelName +
        ", " + pf.format(totalNumSamples) + " samples (binary),");
    final Format f02d = new Format("%02d");
    final Format f03d = new Format("%03d");
    final Format f04d = new Format("%04d");
    final String year = f04d.format(startTime.getYear());
    final String day = f03d.format(startTime.getDayOfYear());
    final String hour = f02d.format(startTime.getHour());
    final String minute = f02d.format(startTime.getMinute());
    final String second = f02d.format(startTime.getSecond());
    final String fracsec = f04d.format(startTime.getTenthMill());
    System.out.print(
        " starting " + year + "," + day + " " + hour + ":" + minute +
        ":" + second + "." + fracsec + " UT\n");

    // write data
    writeData(outfile_name.toString(), intData);

    // write WFDISC data
    writeTextFile(outputFile, Wfdisc_buf.toString());

    // write SITE
    outputFile = new File("rdseed.site");
    Wfdisc_buf = new StringBuffer(Wfdisc_buf_size);
    pf = new Format("%-6s ");
    Wfdisc_buf.append(pf.format(dataInfo.stationName));
    pf = new Format(" %04d");
    Wfdisc_buf.append(pf.format(stime.getYear()));
    pf = new Format("%03d ");
    Wfdisc_buf.append(pf.format(stime.getDayOfYear()));
    pf = new Format("%8d ");
    Wfdisc_buf.append(pf.format( -1));

    pf = new Format("%9.4f ");
    Wfdisc_buf.append(pf.format(currentChannel.latitude));
    pf = new Format("%9.4f ");
    Wfdisc_buf.append(pf.format(currentChannel.longitude));
    pf = new Format("%9.4f ");
    Wfdisc_buf.append(pf.format(currentChannel.elevation.doubleValue() / 1000.0));
    pf = new Format("%-50.50s ");
    Wfdisc_buf.append(pf.format(currentStation.siteName));
    pf = new Format("%-4s ");
    Wfdisc_buf.append(pf.format("-"));
    pf = new Format("%-6s ");
    Wfdisc_buf.append(pf.format("-"));
    pf = new Format("%9.4f ");
    Wfdisc_buf.append(pf.format(0.0));
    pf = new Format("%9.4f ");
    Wfdisc_buf.append(pf.format(0.0));
    pf = new Format("%-17s\n");
    Wfdisc_buf.append(pf.format(lddate));
    writeTextFile(outputFile, Wfdisc_buf.toString());

    // write SITE channel
    outputFile = new File("rdseed.sitechan");
    Wfdisc_buf = new StringBuffer(Wfdisc_buf_size);
    pf = new Format("%-6s ");
    Wfdisc_buf.append(pf.format(dataInfo.stationName));
    pf = new Format("%-8s ");
    Wfdisc_buf.append(pf.format(channelName));
    pf = new Format(" %04d");
    Wfdisc_buf.append(pf.format(stime.getYear()));
    pf = new Format("%03d ");
    Wfdisc_buf.append(pf.format(stime.getDayOfYear()));
    pf = new Format("%8d ");
    Wfdisc_buf.append(pf.format( -1));
    pf = new Format("%8d ");
    Wfdisc_buf.append(pf.format( -1)); // off date
    pf = new Format("%-4s ");
    Wfdisc_buf.append(pf.format("n"));
    pf = new Format("%9.4f ");
    Wfdisc_buf.append(pf.format(currentChannel.depth.doubleValue()));
    pf = new Format("%6.1f ");
    Wfdisc_buf.append(pf.format(currentChannel.azimuth.doubleValue()));
    pf = new Format("%6.1f ");
    Wfdisc_buf.append(pf.format(currentChannel.dip.doubleValue() + 90.0));
    pf = new Format("%-50s ");
    Wfdisc_buf.append(pf.format("-"));
    pf = new Format("%17s\n");
    Wfdisc_buf.append(pf.format(lddate));
    writeTextFile(outputFile, Wfdisc_buf.toString());

    // scan for network/station already there
    outputFile = new File("rdseed.affiliation");
    Wfdisc_buf = new StringBuffer(Wfdisc_buf_size);
    pf = new Format("%-8.8s");
    Wfdisc_buf.append(pf.format(currentStation.networkCode));
    pf = new Format("%-6.6s");
    Wfdisc_buf.append(pf.format(currentStation.stationName));
    if (!scanFile(outputFile, Wfdisc_buf.toString()))
    {
      pf = new Format("%17s\n");
      Wfdisc_buf.append(pf.format(lddate));
      writeTextFile(outputFile, Wfdisc_buf.toString());
    }

    // network
    outputFile = new File("rdseed.network");
    Wfdisc_buf = new StringBuffer(Wfdisc_buf_size);
    pf = new Format("%-8.8s");
    Wfdisc_buf.append(pf.format(currentStation.networkCode));
    if (!scanFile(outputFile, Wfdisc_buf.toString()))
    {
      pf = new Format("%-80.80s");
      Wfdisc_buf.append(pf.format(getNet(currentStation.ownerCode)));
      pf = new Format("%-4.4s");
      Wfdisc_buf.append(pf.format("-1"));
      pf = new Format("%-15.15s");
      Wfdisc_buf.append(pf.format("-1"));
//        pf = new Format("%8.8d");
//        Wfdisc_buf.append(pf.format(-1));
      Wfdisc_buf.append("-00000001"); //hard code output since it isn't correct
      pf = new Format("%-17s\n");
      Wfdisc_buf.append(pf.format(lddate));
      writeTextFile(outputFile, Wfdisc_buf.toString());
    }

    // origin
    outputFile = new File("rdseed.origin");
    // no need to create file more than once
    if (!outputFile.exists() && eventInfo != null)
    {
      Wfdisc_buf = new StringBuffer(Wfdisc_buf_size);
      pf = new Format("%9.4f ");
      Wfdisc_buf.append(pf.format(eventInfo.latitude));
      pf = new Format("%9.4f ");
      Wfdisc_buf.append(pf.format(eventInfo.longitude));
      pf = new Format("%9.4f ");
      Wfdisc_buf.append(pf.format(eventInfo.depth));
      pf = new Format("%17.5f ");
      final double etime = getEpochTime(eventInfo.originTime);
      Wfdisc_buf.append(pf.format(etime));
      pf = new Format("%8d ");
      Wfdisc_buf.append(pf.format(ORID));
      pf = new Format("%8d ");
      Wfdisc_buf.append(pf.format( -1));
      pf = new Format(" %04d");
      Wfdisc_buf.append(pf.format(eventInfo.originTime.getYear()));
      pf = new Format("%03d ");
      Wfdisc_buf.append(pf.format(eventInfo.originTime.getDayOfYear()));
      pf = new Format("%4d ");
      Wfdisc_buf.append(pf.format( -1)); // nass
      pf = new Format("%4d ");
      Wfdisc_buf.append(pf.format( -1)); // ndef
      pf = new Format("%4d ");
      Wfdisc_buf.append(pf.format( -1)); // ndp
      pf = new Format("%8d ");
      Wfdisc_buf.append(pf.format(eventInfo.region));
      pf = new Format("%8d ");
      Wfdisc_buf.append(pf.format(eventInfo.location));
      pf = new Format("%-7s ");
      Wfdisc_buf.append(pf.format("-")); // event type
      pf = new Format("%9.4f ");
      Wfdisc_buf.append(pf.format( -999.9)); // estimated phase depth
      Wfdisc_buf.append("-"); // depth method used
      Wfdisc_buf.append(" ");
      double this_mag;
      Format magPf = new Format("%7.2f");
      pf = new Format(" %8d ");
      for (int i = 0; i < magnitudeTypes.length; i++)
      {
        this_mag = getMagnitude(magnitudeTypes[i]);
        Wfdisc_buf.append(magPf.format(this_mag));
        Wfdisc_buf.append(pf.format( -1));
      }
      pf = new Format("%-15s ");
      Wfdisc_buf.append(pf.format("-"));
      pf = new Format("%-15s ");
      Wfdisc_buf.append(pf.format(getSrcName(eventInfo.sourceCode)));
      pf = new Format("%8d ");
      Wfdisc_buf.append(pf.format( -1));
      pf = new Format("%-17s\n");
      Wfdisc_buf.append(pf.format(lddate));
      writeTextFile(outputFile, Wfdisc_buf.toString());
    }
  }

  /**
   * Gets the epoch time in seconds.
   * @param btime the Btime.
   * @return the epoch time in seconds.
   */
  protected static double getEpochTime(Btime btime)
  {
    return btime.getEpochTime() + btime.getTenthMill() * 0.0001;
  }

  /**
   * Gets the magnitude for the specified magnitude type.
   * @param magType the magnitude type.
   * @return the magnitude or -999 if none.
   */
  protected double getMagnitude(String magType)
  {
    final Number this_mag = eventInfo.getMagnitude(magType);
    if (this_mag == null)
      return -999.0;
    return this_mag.doubleValue();
  }

  /**
   * Gets the abbreviation for the specified instrument code.
   * @param instrument_code the instrument code.
   * @return the abbreviation.
   */
  protected String getNet(String instrument_code)
  {
    final Blockette type_33 = find_type_33(instrument_code);
    if (type_33 != null)
      return type_33.toString(4); //Abbreviation description
    return "";
  }

  /**
   * Gets the source name for the specified source lookup code.
   * @param source_lookup_code the source lookup code.
   * @return the source name.
   */
  protected String getSrcName(String source_lookup_code)
  {
    final Blockette type_32 = find_type_32(source_lookup_code);
    if (type_32 != null)
      return type_32.toString(4); //Name of publication/author
    return "";
  }

  /**
   * Scan the file for the specified text.
   * @param textFile the text file.
   * @param textStr the text.
   * @return true if the file contains the text, false otherwise.
   */
  protected static boolean scanFile(File textFile, String textStr)
  {
    BufferedReader rdrObj = null;
    try
    {
      if (textFile.canRead()) //if the file can be read
      {
        String line;
        //create file input reader
        rdrObj = new BufferedReader(new FileReader(textFile));
        while ( (line = rdrObj.readLine()) != null)
        {
          if (line.startsWith(textStr))
            return true;
        }
      }
    }
    catch (Exception ex)
    {
      System.err.print("WARNING (output_css):  ");
      System.err.print("failed to properly read from " +
                       textFile.getPath() + ": " + ex);
      System.err.print("\tExecution continuing.\n");
    }
    try
    {
      if (rdrObj != null)
        rdrObj.close();
    }
    catch (Exception ex)
    {}
    return false;
  }

  // constants

  protected static int Wfdisc_buf_size = 288;
  protected static int ORID = 1;
  protected static String[] magnitudeTypes =
      {
      "mb",
      "ms",
      "ml"
  };

  // instance variables

  protected int wfid; // wfdisc id variable
}
