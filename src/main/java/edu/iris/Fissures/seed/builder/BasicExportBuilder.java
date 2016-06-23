package edu.iris.Fissures.seed.builder;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.container.Btime;
import edu.iris.Fissures.seed.exception.BuilderException;
import edu.iris.Fissures.seed.container.SeedObject;
import edu.iris.Fissures.seed.container.Waveform;
import edu.iris.Fissures.seed.exception.SeedException;
import java.util.Map;
import java.util.HashMap;

public class BasicExportBuilder
    extends ExportBuilder
{
  protected final boolean padFlag;

  /**
   * Create a basic export builder.
   */
  public BasicExportBuilder()
  {
    this(false, "^,A,(,32,33,34,50,(,52,(,43,48,53,58,60,),),),71,v,(,999,)");
    builderType = "basic"; // indicate the type of builder we are
  }

  /**
   * Create a basic export builder.
   * @param padFlag true to pad, false otherwise.
   * @param scriptString the script string.
   * @see getNext
   */
  protected BasicExportBuilder(boolean padFlag, String scriptString)
  {
    this.padFlag = padFlag;
    // set some default values
    logicalRecordLength = 1024;
    physicalRecordLength = 2000000000; // set to impossibly high value
    logicalRecords = new Vector(8, 8);
    exportMold = new Vector(8, 8);
    recordPadding = (byte) 0; // use nulls for padding
    scriptNesting = new int[8];
    nestingScore = new int[scriptNesting.length];
    initExportScript(scriptString);
  }

  /** Zero number. */
  public static Double ZERO_NUMBER = new Double(0);

  /** Empty text. */
  public static String EMPTY_TEXT = "";

  /**
   * Gets the number value from the Blockette.
   * @param blk the Blockette.
   * @param fieldNum the field number.
   * @return the number value or 0 if error.
   */
  public static Number blkGetNumber(
      Blockette blk, int fieldNum)
  {
    return blkGetNumber(blk, fieldNum, 0);
  }

  /**
   * Gets the number value from the Blockette.
   * @param blk the Blockette.
   * @param fieldNum the field number.
   * @param fieldIndex the field index or 0 if none.
   * @return the number value or 0 if error.
   */
  public static Number blkGetNumber(
      Blockette blk, int fieldNum, int fieldIndex)
  {
    return blkGetNumber(blk, fieldNum, fieldIndex, ZERO_NUMBER);
  }

  /**
   * Gets the number value from the Blockette.
   * @param blk the Blockette.
   * @param fieldNum the field number.
   * @param fieldIndex the field index or 0 if none.
   * @param defaultValue the default value to use for error.
   * @return the number value or the default value if error.
   */
  public static Number blkGetNumber(
      Blockette blk, int fieldNum, int fieldIndex, Number defaultValue)
  {
    try
    {
      Object fieldVal = blk.getFieldVal(fieldNum, fieldIndex);
      if (! (fieldVal instanceof Number)) //should be a number but just in case
      {
        fieldVal = new Double(fieldVal.toString());
      }
      return (Number) fieldVal;
    }
    catch (Exception ex)
    {}
    return defaultValue;
  }

  /**
   * Gets the string value from the Blockette.
   * @param blk the Blockette.
   * @param fieldNum the field number.
   * @return the string value.
   */
  public static String blkGetString(Blockette blk, int fieldNum)
  {
    return blkGetString(blk, fieldNum, 0);
  }

  /**
   * Gets the string value from the Blockette.
   * @param blk the Blockette.
   * @param fieldNum the field number.
   * @param fieldIndex the field index or 0 if none.
   * @return the string value.
   */
  public static String blkGetString(
      Blockette blk, int fieldNum, int fieldIndex)
  {
    // get the String value of the field Object
    try
    {
      final Object fieldObj = blk.getFieldVal(fieldNum, fieldIndex);
      if (fieldObj != null)
      {
        return fieldObj.toString().trim();
      }
    }
    catch (Exception ex) {}
    return EMPTY_TEXT;
  }

  /**
   * Gets the time value from the Blockette.
   * @param blk the Blockette.
   * @param fieldNum the field number.
   * @return the time value.
   * @throws Exception if error.
   */
  public static Btime blkGetTime(Blockette blk, int fieldNum) throws Exception
  {
    return blkGetTime(blk, fieldNum, 0);
  }

  /**
   * Gets the time value from the Blockette.
   * @param blk the Blockette.
   * @param fieldNum the field number.
   * @param fieldIndex the field index or 0 if none.
   * @return the time value.
   * @throws Exception if error.
   */
  public static Btime blkGetTime(Blockette blk, int fieldNum, int fieldIndex) throws
      Exception
  {
    Object fieldVal = blk.getFieldVal(fieldNum, fieldIndex);
    if (! (fieldVal instanceof Btime)) //should be a time but just in case
    {
      if (fieldVal == null)
        fieldVal = EMPTY_TEXT;
      fieldVal = new Btime(fieldVal.toString());
    }
    return (Btime) fieldVal;
  }

  /**
   * Convert Type B (Hz) to Type A (rad/sec).
   * @param n the Type B (Hz) number.
   * @return the Type A (rad/sec) number.
   */
  public static Double convertNumber(Number n)
  {
    return new Double(n.doubleValue() * TWO_PI);
  }

  /**
   * Get the end time (as a Btime object).  Projected from the start time
   * based on the number of samples and the calculated sample rate.
   * @param startTime the start time.
   * @param numSamples the number of samples.
   * @param srFactor sample rate factor.
   * @param srMult sample rate multiplier.
   * @return the end time.
   * @throws Exception if error.
   */
  public static Btime getEndTime(Btime startTime, Number numSamples,
                                 Number srFactor,
                                 Number srMult) throws Exception
  {
    return getEndTime(startTime, numSamples.intValue(), srFactor.intValue(),
                      srMult.intValue());
  }

  /**
   * Get the end time (as a Btime object).  Projected from the start time
   * based on the number of samples and the calculated sample rate.
   * @param startTime the start time.
   * @param numSamples the number of samples.
   * @param srFactor sample rate factor.
   * @param srMult sample rate multiplier.
   * @return the end time.
   * @throws Exception if error.
   */
  public static Btime getEndTime(Btime startTime, int numSamples, int srFactor,
                                 int srMult) throws Exception
  {
    // get the sample rate
    double true_rate;
    if ( (srFactor * srMult) == 0.0)
    {
      true_rate = 10000.0;
    }
    else
    {
      true_rate = (Math.pow( (double) (Math.abs(srFactor)),
                            (double) (srFactor / Math.abs(srFactor))) *
                   Math.pow( (double) (Math.abs(srMult)),
                            (double) (srMult / Math.abs(srMult))));
    }
    double ttSeconds = ( (double) numSamples) / true_rate * 10000.0; // find the number of ten-thousands of seconds
    return startTime.projectTime(ttSeconds); // the end time is the projection from the start time
  }

  // protected methods

  /**
   * Find blockette type 32.
   * @param source_lookup_code the source lookup code.
   * @return the Blockette.
   */
  protected Blockette find_type_32(String source_lookup_code)
  {
    return (Blockette) type_32_map.get(source_lookup_code);
  }

  /**
   * Find blockette type 33.
   * @param instrument_code the instrument code.
   * @return the Blockette.
   */
  protected Blockette find_type_33(String instrument_code)
  {
    return (Blockette) type_33_map.get(instrument_code);
  }

  /**
   * Find blockette type 34.
   * @param signal_units_code the signal units code.
   * @return the Blockette.
   */
  protected Blockette find_type_34(String signal_units_code)
  {
    return (Blockette) type_34_map.get(signal_units_code);
  }

  /**
   * Initializes the export script array from the script string.
   * @param scriptString the script string.
   * @see getNext
   */
  protected final void initExportScript(String scriptString)
  {
    final StringTokenizer expTok = new StringTokenizer(scriptString, ",");
    exportScript = new String[expTok.countTokens()];
    int i = 0;
    while (expTok.hasMoreTokens())
      exportScript[i++] = expTok.nextToken();
  }

  /**
   * Pad the end of a logical record with recordPadding bytes.
   * Shift the logical record position to equal logicalRecordLength.
   * Push the logical record onto the vector and clear the current register.
   */
  protected void padLogical()
  {
    if (padFlag)
    {
      super.padLogical();
    }
  }

  /**
   * Pad the end of a physical record with recordPadding bytes.
   * Push the latest partial logical record onto the logical records vector and pad it.
   * Generate extra empty logical records if necessary.
   * @throws Exception if error.
   */
  protected void padPhysical() throws Exception
  {
    if (padFlag)
    {
      super.padPhysical();
    }
  }

  /**
   * Pack (append) a byte array into a logical record.
   * Use objects from the exportMold as the data source.
   * If the current logical record is full, append the current one to the
   * logicalRecords vector and start a new one.
   * This method knows how to properly start, preface, and finish a
   * complete logical record.
   * @throws Exception if error.
   */
  protected void packToRecord() throws Exception
  {
    // refresh logicalPerPhysical value
    logicalPerPhysical = physicalRecordLength / logicalRecordLength;
    if (exportMold == null)
    {
      throw new BuilderException("null export mold");
    }
    if (exportMold.size() == 0)
    {
      return; // empty export mold, then do nothing.
    }
    // our objects arrive in exportMold...
    packToRecord( (Blockette) exportMold.get(0));
    // clear out the export mold for the next incoming object(s)
    exportMold.clear();
  }

  /**
   * Pack (append) a byte array into a logical record.
   * @param blk the blockette.
   * @throws Exception if error.
   */
  protected void packToRecord(Blockette blk) throws Exception
  {
    String key;
    switch (blk.getType())
    {
      case 32:
        //source lookup code key
        key = blkGetString(blk, 3);
        type_32_map.put(key, blk);
        break;
      case 33:
        //instrument code key
        key = blkGetString(blk, 3);
        type_33_map.put(key, blk);
        break;
      case 34:

        //signal units code key
        key = blkGetString(blk, 3);
        type_34_map.put(key, blk);
        break;
      case 43:
      case 53:
        if (currentChannel == null)
          throw new BuilderException(
              "got a response blockette before a channel blockette");
        currentChannel.addResponse(new ResponseInfo(blk));
        break;
      case 50:
        StationInfo newStation = new StationInfo(blk);
        stationList.add(newStation); // add station info to the list vector
        currentStation = newStation; // handle to current station
        break;
      case 52:
        if (currentStation == null)
          throw new BuilderException(
              "got a channel blockette before a station blockette");
        ChannelInfo newChannel = new ChannelInfo(currentStation, blk);
        currentStation.addChannel(newChannel); // add channel info to the current station's list
        currentChannel = newChannel; // handle to current channel
        break;
      case 58:
        if (blkGetNumber(blk, 3).intValue() == 0)
        { // check to see that this is a Stage 0 (sensitivity) blockette
          if (currentChannel == null)
            throw new BuilderException(
                "got a sensitivity blockette before a channel blockette");
          currentChannel.sensitivity = blkGetNumber(blk, 4);
          currentChannel.frequency = blkGetNumber(blk, 5);
        }
        break;
      case 71:
        eventInfo = new EventInfo(blk); // save event information
        break;
      case 999:

        // check to see if there is waveform data attached...if not, then we
        // are not interested in this record
        Waveform thisWaveform = blk.getWaveform();
        if (thisWaveform == null)
        {
          //System.err.println("DEBUG: thisWaveform == null...do nothing");
          exportMold.clear(); // clear the export mold
          return; // do nothing else
        }
        prevEndTime = endTime; // save the previous end time
        DataInfo dataInfo = new DataInfo(blk);
        endTime = dataInfo.endTime;
        final long timeDiff = dataInfo.startTime.diffSeconds(prevEndTime);

        if (timeDiff < -1 || timeDiff > 1)
        { // if greater than a difference of 1 second, then this trace ends
          // end the current data record.
          volumeFinish();
          // match data record parameters to StationInfo and ChannelInfo objects.
          final StationInfo stationInfo = findStationInfo(dataInfo);
          if (stationInfo == null)
            throw new BuilderException("data record " + dataInfo.stationName +
                                       "/" + dataInfo.networkCode + "/" +
                                       dataInfo.channelName +
                                       "/" + dataInfo.locationId +
                                       " without station header info.");
          final ChannelInfo channelInfo = findChannelInfo(dataInfo, stationInfo);
          if (channelInfo == null)
            throw new BuilderException("data record " + dataInfo.stationName +
                                       "/" + dataInfo.networkCode + "/" +
                                       dataInfo.channelName +
                                       "/" + dataInfo.locationId +
                                       " without channel header info.");
          currentStation = stationInfo; //save the current station
          currentChannel = channelInfo; //save the current channel
        }
        addDataInfo(dataInfo); //add the info to the list
        break;
    }
  }

  /**
   * Adds the DataInfo to the list.
   * @param dataInfo the DataInfo.
   */
  private void addDataInfo(DataInfo dataInfo)
  {
    dataInfoList.add(dataInfo);
  }

  /**
   * Clears the data info.
   */
  protected void clearDataInfo()
  {
    dataInfoList.clear();
  }

  /**
   * Gets the data information for the specified index.
   * @param index the index.
   * @return the DataInfo or null if none.
   */
  protected DataInfo getDataInfo(int index)
  {
    if (index < dataInfoList.size())
      return (DataInfo) dataInfoList.get(index);
    return null;
  }

  /**
   * Get the waveform data as floating point values.
   * @return the waveform data as floating point values.
   */
  protected float[] getFloatData()
  {
    int dataIndex = 0;
    DataInfo dataInfo = getDataInfo(dataIndex);
    if (dataInfo == null)
      return new float[0];

    //find out the total number of samples
    int totalNumSamples = 0;
    do
    {
      totalNumSamples += dataInfo.numSamples.intValue();
    }
    while ( (dataInfo = getDataInfo(++dataIndex)) != null);

    float[] floatData = new float[totalNumSamples];
    int floatDataOffset = 0;
    float[] waveFormData;

    //copy all of the data
    dataIndex = 0;
    while ( (dataInfo = getDataInfo(dataIndex++)) != null)
    {
      try
      {
        waveFormData = dataInfo.getFloatData();
        for (int i = 0; i < waveFormData.length; i++)
        {
          floatData[floatDataOffset++] = waveFormData[i];
        }
      }
      catch (Exception ex)
      {
        System.err.println("Error getting waveform data: " + ex);
      }
    }
    return floatData;
  }

  /**
   * Get the waveform data as integer values.
   * @return the waveform data as integer values.
   */
  protected int[] getIntegerData()
  {
    int dataIndex = 0;
    DataInfo dataInfo = getDataInfo(dataIndex);
    if (dataInfo == null)
      return new int[0];

    //find out the total number of samples
    int totalNumSamples = 0;
    do
    {
      totalNumSamples += dataInfo.numSamples.intValue();
    }
    while ( (dataInfo = getDataInfo(++dataIndex)) != null);

    int[] intData = new int[totalNumSamples];
    int floatDataOffset = 0;
    int[] waveFormData;

    //copy all of the data
    dataIndex = 0;
    while ( (dataInfo = getDataInfo(dataIndex++)) != null)
    {
      try
      {
        waveFormData = dataInfo.getIntegerData();
        for (int i = 0; i < waveFormData.length; i++)
        {
          intData[floatDataOffset++] = waveFormData[i];
        }
      }
      catch (Exception ex)
      {
        System.err.println("Error getting waveform data: " + ex);
      }
    }
    return intData;
  }

  /**
   * Check provided string from export script to see if it's meant to trigger a
   * special action.  If so, implement the intended action.
   * @param s the string.
   * @return true if the string is a trigger.
   * @throws Exception if error.
   */
  protected boolean checkTrigger(String s) throws Exception
  {
    if (s.equals("A"))
    {
      // set the starting value for data trace end time
      endTime = new Btime("1900,001,00:00:00.0000");
    }
    else
      return false; // no trigger, return false
    return true; // we have found a trigger, so return true
  }

  /**
   * Finish up volume export operations.
   * @throws BuilderException if error.
   */
  protected void volumeFinish() throws BuilderException
  {
    output_info();
    clearDataInfo(); //clear the data info
  }

  /**
   * Output the information.
   */
  protected void output_info()
  {
    System.out.println("# *** New information ***");
  }

  /**
   * Create a new logical record and place it in the logicalRecord instance variable
   * SeedObject is an object to be put into the new logical record, which can help to
   * identify the traits that that logical record will have.
   * @param obj the SEED object.
   * @param continuation true if continuation, false otherwise.
   * If SeedObject is null, this implies that a blank logical record is being created.
   * <b>continuation</b>, when set to true, can mark the logical record as a
   * continuation of the last one.
   * @throws Exception if error.
   */
  protected void startNewLogical(SeedObject obj, boolean continuation) throws
      Exception
  {
    System.out.println("# *** New logical ***");
  }

  /**
   * Gets the channel information for the data.
   * @param dataInfo the data information.
   * @param stationInfo the station information.
   * @return the channel information.
   */
  protected ChannelInfo findChannelInfo(DataInfo dataInfo,
                                        StationInfo stationInfo)
  {
    ChannelInfo channelInfo = null;
    int channelsSize = stationInfo.getNumChannels();
    for (int i = 0; i < channelsSize; i++)
    {
      channelInfo = stationInfo.getChannel(i);
      if (channelInfo.channelName.equals(dataInfo.channelName) &&
          channelInfo.locationId.equals(dataInfo.locationId) &&
          channelInfo.endEffTime.diffSeconds(dataInfo.startTime) >= 0 &&
          channelInfo.startEffTime.diffSeconds(dataInfo.endTime) <= 0
          )
        break; // found match
      channelInfo = null;
    }
    return channelInfo;
  }

  /**
   * Gets the station information for the data.
   * @param dataInfo the data information.
   * @return the station information.
   */
  protected StationInfo findStationInfo(DataInfo dataInfo)
  {
    StationInfo stationInfo = null;
    int stationListSize = stationList.size();
    for (int i = 0; i < stationListSize; i++)
    { // find station header object
      stationInfo = (StationInfo) stationList.get(i);
      if (stationInfo.stationName.equals(dataInfo.stationName) &&
          stationInfo.networkCode.equals(dataInfo.networkCode) &&
          stationInfo.endEffTime.diffSeconds(dataInfo.startTime) >= 0 &&
          stationInfo.startEffTime.diffSeconds(dataInfo.endTime) <= 0
          )
        break; // found match
      stationInfo = null;
    }
    return stationInfo;
  }

  // inner classes

  /**
   * Store station info for Export.
   */
  protected static class StationInfo
  {
    /**
     * Creates the station information from the blockette.
     * @param blk the Blockette.
     * @throws Exception if error.
     */
    public StationInfo(Blockette blk) throws Exception
    {
      if (blk.getType() != 50)
        throw new BuilderException(
            "StationInfo could not be created from type " + blk.getType());
      stationName = blkGetString(blk, 3);
      siteName = blkGetString(blk, 9);
      ownerCode = blkGetString(blk, 10); //owner (network identifier) code
      startEffTime = blkGetTime(blk, 13);
      endEffTime = blkGetTime(blk, 14);
      networkCode = blkGetString(blk, 16);
    }

    /**
     * Adds the channel to the list.
     * @param channelInfo the ChannelInfo.
     */
    public void addChannel(ChannelInfo channelInfo)
    {
      channels.add(channelInfo);
    }

    /**
     * Gets the number of channels.
     * @return the number of channels.
     */
    public int getNumChannels()
    {
      return channels.size();
    }

    /**
     * Gets the channel.
     * @param index the index.
     * @return the ChannelInfo or null if none.
     */
    public ChannelInfo getChannel(int index)
    {
      if (index < channels.size())
        return (ChannelInfo) channels.get(index);
      return null;
    }

    protected final String stationName;
    protected final String siteName;
    protected final String ownerCode;
    protected final String networkCode;
    protected final Btime startEffTime;
    protected final Btime endEffTime;
    protected final List channels = new Vector(8, 8);
  }

  /**
   * Store channel info for export.
   */
  protected static class ChannelInfo
  {
    /**
     * Creates the channel information from the blockette.
     * @param station the station information.
     * @param blk the Blockette.
     * @throws Exception if error.
     */
    public ChannelInfo(StationInfo station, Blockette blk) throws Exception
    {
      if (blk.getType() != 52)
        throw new BuilderException(
            "ChannelInfo could not be created from type " + blk.getType());
      this.station = station;
      channelName = blkGetString(blk, 4);
      locationId = blkGetString(blk, 3);
      instrumentCode = blkGetString(blk, 6);
      signalUnitsCode = blkGetString(blk, 8);
      latitude = blkGetNumber(blk, 10);
      longitude = blkGetNumber(blk, 11);
      elevation = blkGetNumber(blk, 12);
      depth = blkGetNumber(blk, 13);
      azimuth = blkGetNumber(blk, 14);
      dip = blkGetNumber(blk, 15);
      sampleRate = blkGetNumber(blk, 18);
      startEffTime = blkGetTime(blk, 22);
      endEffTime = blkGetTime(blk, 23);

    }

    /**
     * Adds the response information.
     * @param responseInfo the ResponseInfo.
     */
    public void addResponse(ResponseInfo responseInfo)
    {
      responses.add(responseInfo);
    }

    /**
     * Gets the number of responses.
     * @return the number of responses.
     */
    public int getNumResponses()
    {
      return responses.size();
    }

    /**
     * Gets the response information.
     * @param index the index.
     * @return the response information or null if none.
     */
    public ResponseInfo getResponse(int index)
    {
      if (index < responses.size())
        return (ResponseInfo) responses.get(index);
      return null;
    }

    /**
     * Gets the response list.
     * @return the response list.
     */
    public List getResponseList()
    {
      return responses;
    }

    protected final StationInfo station;
    protected final String channelName;
    protected final String locationId;
    protected final String instrumentCode;
    protected final String signalUnitsCode;
    protected final Number latitude;
    protected final Number longitude;
    protected final Number elevation;
    protected final Number depth;
    protected final Number azimuth;
    protected final Number dip;
    protected final Number sampleRate;
    protected final Btime startEffTime;
    protected final Btime endEffTime;
    protected final List responses = new Vector(8, 8); //list of ResponseInfo

    protected Number sensitivity = null;
    protected Number frequency = null;
  }

  /**
   * Store optional event info for export.
   */
  protected static class EventInfo
  {
    /**
     * Creates the event information from the blockette.
     * @param blk the Blockette.
     * @throws Exception if error.
     */
    public EventInfo(Blockette blk) throws Exception
    {
      if (blk.getType() != 71)
        throw new BuilderException(
            "ChannelInfo could not be created from type " + blk.getType());
      this.blk = blk;
      originTime = blkGetTime(blk, 3);
      sourceCode = blkGetString(blk, 4);  //Hypocenter source identifier
      latitude = blkGetNumber(blk, 5);
      longitude = blkGetNumber(blk, 6);
      depth = blkGetNumber(blk, 7);
      numMags = blkGetNumber(blk, 8).intValue();  //Number of magnitudes
      region = blkGetNumber(blk, 12).intValue();  //Seismic region
      location = blkGetNumber(blk, 13).intValue();  //Seismic Location
    }

    /**
     * Gets the magnitude for the specified magnitude type.
     * @param magType the magnitude type.
     * @return the magnitude or null if none.
     */
    public Number getMagnitude(String magType)
    {
      String currentMagType;
      for (int fieldIndex = 0; fieldIndex < numMags; fieldIndex++)
      {
        currentMagType = blkGetString(blk, 10, fieldIndex);
        if (currentMagType != null && currentMagType.indexOf(magType) >= 0)
          return blkGetNumber(blk, 9, fieldIndex);
      }
      return null;
    }

    protected final Blockette blk;
    protected final Btime originTime;
    protected final String sourceCode;
    protected final Number latitude;
    protected final Number longitude;
    protected final Number depth;
    protected final int numMags;
    protected final int region;
    protected final int location;
  }

  protected static class ComplexNumber
  {
    Number r = ZERO_NUMBER;
    Number i = ZERO_NUMBER;
  }

  /**
   * Store optional response info for export.
   */
  protected static class ResponseInfo
  {
    private static int blk43Count = 0;

    /**
     * Creates the response information from the blockette.
     * @param blk the Blockette.
     * @throws Exception if error.
     */
    public ResponseInfo(Blockette blk) throws Exception
    {
      ComplexNumber complexNumber;

      switch (blkType = blk.getType())
      {
        case 43:
          stage = new Integer(++blk43Count); //use count
          type = blkGetString(blk, 5).charAt(0); //Response type
          aONorm = blkGetNumber(blk, 8); //AO normalization factor
          normFreq = blkGetNumber(blk, 9); //Normalization frequency (Hz)
          numberZeroes = blkGetNumber(blk, 10); //Number of complex zeros
          numberPoles = blkGetNumber(blk, 15); //Number of complex poles
          poles = new ComplexNumber[numberPoles.intValue()];
          for (int i = 0; i < poles.length; i++)
          {
            complexNumber = new ComplexNumber();
            complexNumber.r = blkGetNumber(blk, 16, i); //Real pole
            complexNumber.i = blkGetNumber(blk, 17, i); //Imaginary pole
            poles[i] = complexNumber;
          }
          zeroes = new ComplexNumber[numberZeroes.intValue()];
          for (int i = 0; i < zeroes.length; i++)
          {
            complexNumber = new ComplexNumber();
            complexNumber.r = blkGetNumber(blk, 11, i); //Real zero
            complexNumber.i = blkGetNumber(blk, 12, i); //Imaginary zero
            zeroes[i] = complexNumber;
          }
          break;

        case 53:
          type = blkGetString(blk, 3).charAt(0); //Transfer function type
          stage = blkGetNumber(blk, 4); //Stage sequence number
          aONorm = blkGetNumber(blk, 7); //AO normalization factor
          normFreq = blkGetNumber(blk, 8); //Normalization freq. f(n) (Hz)
          numberZeroes = blkGetNumber(blk, 9); //Number of complex zeros
          numberPoles = blkGetNumber(blk, 14); //Number of complex poles
          poles = new ComplexNumber[numberPoles.intValue()];
          for (int i = 0; i < poles.length; i++)
          {
            complexNumber = new ComplexNumber();
            complexNumber.r = blkGetNumber(blk, 15, i); //Real pole
            complexNumber.i = blkGetNumber(blk, 16, i); //Imaginary pole
            poles[i] = complexNumber;
          }
          zeroes = new ComplexNumber[numberZeroes.intValue()];
          for (int i = 0; i < zeroes.length; i++)
          {
            complexNumber = new ComplexNumber();
            complexNumber.r = blkGetNumber(blk, 10, i); //Real zero
            complexNumber.i = blkGetNumber(blk, 11, i); //Imaginary zero
            zeroes[i] = complexNumber;
          }
          break;
        default:
          throw new BuilderException(
              "ResponseInfo could not be created from type " + blk.getType());
      }
    }

    protected final int blkType;
    protected final char type;
    protected final Number stage;
    protected final Number aONorm;
    protected final Number normFreq;
    protected final Number numberZeroes;
    protected final Number numberPoles;
    protected ComplexNumber[] poles;
    protected ComplexNumber[] zeroes;
  }

  /**
   * Data info for export.
   */
  protected class DataInfo
  {
    /**
     * Creates the data information from the blockette.
     * @param blk the Blockette.
     * @throws Exception if error.
     */
    public DataInfo(Blockette blk) throws Exception
    {
      if (blk.getType() != 999)
        throw new BuilderException("DataInfo could not be created from type " +
                                   blk.getType());
      this.blk = blk;
      dataQuality = blkGetString(blk, 2).charAt(0); // get just the first character

      stationName = blkGetString(blk, 4);
      locationId = blkGetString(blk, 5);
      channelName = blkGetString(blk, 6);
      networkCode = blkGetString(blk, 7);
      startTime = blkGetTime(blk, 8);
      numSamples = blkGetNumber(blk, 9);
      srFactor = blkGetNumber(blk, 10);
      srMult = blkGetNumber(blk, 11);
      endTime = getEndTime(startTime, numSamples, srFactor, srMult);

      // recover this block's sample rate
      double this_sample_rate = srFactor.doubleValue();
      double sample_rate_multiplier = srMult.doubleValue();
      if (this_sample_rate < 0.0)
        this_sample_rate = 1.0 / ( -this_sample_rate);
      if (sample_rate_multiplier > 0)
        this_sample_rate = this_sample_rate * sample_rate_multiplier;
      else
      if (sample_rate_multiplier < 0.0)
        this_sample_rate = this_sample_rate / ( -sample_rate_multiplier);
      sampleRate = this_sample_rate;
    }

    /**
     * Gets the waveform data as floating point values.
     * @return the waveform data as floating point values.
     * @throws Exception if error.
     */
    public float[] getFloatData() throws Exception
    {
      final Waveform thisWaveform = blk.getWaveform();
      if (thisWaveform.getEncoding().equals("UNKNOWN"))
      {
        throw new BuilderException("Waveform data encoding is UNKNOWN");
      }
      float[] dataValues;
      try
      {
        dataValues = thisWaveform.getDecodedFloats();
      }
      catch (SeedException e)
      {
        // SeedException is thrown if we cannot decode using the listed
        // data encoding type.
        // fall back to some default encoding and proceed forward with a
        // printed warning.
        String defaultEncoding = "Steim1";
        System.err.println("WARNING: " + e);
        System.err.println("proceeding using default encoding " +
                           defaultEncoding);
        thisWaveform.setEncoding(defaultEncoding);
        dataValues = thisWaveform.getDecodedFloats();
      }
      return dataValues;
    }

    /**
     * Gets the waveform data as integer values.
     * @return the waveform data as integer values.
     * @throws Exception if error.
     */
    public int[] getIntegerData() throws Exception
    {
      final Waveform thisWaveform = blk.getWaveform();
      if (thisWaveform.getEncoding().equals("UNKNOWN"))
      {
        throw new BuilderException("Waveform data encoding is UNKNOWN");
      }
      int[] dataValues;
      try
      {
        dataValues = thisWaveform.getDecodedIntegers();
      }
      catch (SeedException e)
      {
        // SeedException is thrown if we cannot decode using the listed
        // data encoding type.
        // fall back to some default encoding and proceed forward with a
        // printed warning.
        String defaultEncoding = "Steim1";
        System.err.println("WARNING: " + e);
        System.err.println("proceeding using default encoding " +
                           defaultEncoding);
        thisWaveform.setEncoding(defaultEncoding);
        dataValues = thisWaveform.getDecodedIntegers();
      }
      return dataValues;
    }

    protected final Blockette blk;
    protected final char dataQuality;
    protected final String stationName;
    protected final String locationId;
    protected final String channelName;
    protected final String networkCode;
    protected final Number numSamples;
    protected final Number srFactor;
    protected final Number srMult;
    protected final Btime startTime;
    protected final Btime endTime;
    protected final double sampleRate;
  }

  // constants

  //* 2 * PI
   public final static double TWO_PI = Math.PI * 2.0;

  // instance variables

  protected List stationList = new Vector(8, 8); //list of StationInfo
  protected StationInfo currentStation = null;
  protected ChannelInfo currentChannel = null;
  protected EventInfo eventInfo = null;
  private List dataInfoList = new Vector(8, 8); //list of DataInfo

  private Btime endTime = null;
  private Btime prevEndTime = null;
  private final Map type_32_map = new HashMap(); //Blockette 32 with source lookup code key
  private final Map type_33_map = new HashMap(); //Blockette 33 with instrument code key
  private final Map type_34_map = new HashMap(); //Blockette 34 with signal units code key
}
