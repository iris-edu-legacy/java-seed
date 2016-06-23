package edu.iris.Fissures.seed.builder;

import edu.iris.Fissures.seed.container.Btime;

public class AhInfo
{
  public static class AhTime
  {
    public AhTime()
    {
      yr = 0; // year
      mo = 0; // month
      day = 0; // day
      hr = 0; // hour
      mn = 0; // minute
      sec = 0; // second
    }

    public AhTime(Btime bt)
    {
      setTime(bt);
    }

    public void setTime(Btime bt)
    {
      yr = (short) bt.getYear();
      // given Julian day get Month and Day
      final Btime.MonthDayYear mdy = bt.getMonthDayYear();
      mo = (short) mdy.month;
      day = (short) mdy.dayOfMonth;
      hr = (short) bt.getHour();
      mn = (short) bt.getMinute();
      sec = (float) (bt.getSecond() + bt.getTenthMill() * 0.0001);
    }

    short yr; // year
    short mo; // month
    short day; // day
    short hr; // hour
    short mn; // minute
    float sec; // second
  };

  protected static class AhComplexNumber
  {
    float r = 0;
    float i = 0;
  }

  public static class AhCalibInfo
  {
    AhComplexNumber pole = new AhComplexNumber(); // pole
    AhComplexNumber zero = new AhComplexNumber(); // zero
  };

  public static class AhStationInfo
  {
    public AhStationInfo()
    {
      for (int i = 0; i < NOCALPTS; i++)
      {
        cal[i] = new AhCalibInfo();
      }
    }

    final static int NOCALPTS = 30;
    String code = NULL_TEXT; // station code
    String chan = NULL_TEXT; ; // lpz,spn, etc.
    String stype = NULL_TEXT; // wwssn,hglp,etc.
    float slat = 0; // station latitude
    float slon = 0; //    "    longitude
    float elev = 0; //    "    elevation
    float DS = 0; // gain
    float A0 = 0; // normalization
    AhCalibInfo[] cal = new AhCalibInfo[NOCALPTS]; // calibration info
  };

  public static class AhEventInfo
  {
    float lat = 0; // event latitude
    float lon = 0; //   "   longitude
    float dep = 0; //   "   depth
    AhTime ot = new AhTime(); //   "   origin time
    String ecomment = NULL_TEXT; //	comment line
  };

  /** data types */
  public final static short FLOAT = 1;
  public final static short COMPLEX = 2;
  public final static short VECTOR = 3;
  public final static short TENSOR = 4;
  public final static short FLOAT4 = 5;
  public final static short DOUBLE = 6;

  public static class AhRecordInfo
  {
    short type; // data type (int,float,...)
    int ndata = 0; // number of samples
    float delta = 0; // sampling interval
    float maxamp = 0; // maximum amplitude of record
    AhTime abstime = new AhTime(); // start time of record section
    float rmin = 0; // minimum value of abscissa
    String rcomment = EMPTY_TEXT; // comment line
    String log = NULL_TEXT; // log of data manipulations

    double[] doubleData; //DOUBLE data type
    float[] floatData; //FLOAT data types
                       // (FLOAT4 array size should be ndata * 4 and
                       //  TENSOR array size should be ndata * 3)
  };

  public static class AhHed
  {
    AhStationInfo station = new AhStationInfo(); // station info
    AhEventInfo event = new AhEventInfo(); // event info
    AhRecordInfo record = new AhRecordInfo(); // record info
    float[] extra = new float[NEXTRAS]; // freebies
  };

  /**
   * Creates the XDR output.
   * @param hed the AH header.
   * @return the XDR output.
   */
  public static XdrOutput creatXdrOutput(AhHed hed)
  {
    final int maxDataIndex;
    final int dataSize;
    switch (hed.record.type)
    {
      case FLOAT:
        maxDataIndex = hed.record.ndata;
        dataSize = maxDataIndex * 4;
        break;
      case COMPLEX:
      case VECTOR:
        maxDataIndex = hed.record.ndata * 2;
        dataSize = maxDataIndex * 4;
        break;
      case TENSOR:
        maxDataIndex = hed.record.ndata * 3;
        dataSize = maxDataIndex * 4;
        break;
      case FLOAT4:
        maxDataIndex = hed.record.ndata * 4;
        dataSize = maxDataIndex * 4;
        break;
      case DOUBLE:
        maxDataIndex = hed.record.ndata;
        dataSize = maxDataIndex * 8;
        break;
      default:
        maxDataIndex = 0;
        dataSize = 0;
        break;
    }
    //make sure the buffer is a multiple of 4
    return new XdrOutput(AHHEADSIZE+((dataSize+4)/4)*4);
  }

  /**
   * Writes the header to the xdr stream.
   * @param hed the AH header.
   * @param xdr the XDR output.
   * @return true if success or false if error.
   */
  public static boolean xdr_puthead(AhHed hed,XdrOutput xdr)
  {
    if (!xdr.xdr_bytes(hed.station.code, CODESIZE))
      return false;
    if (!xdr.xdr_bytes(hed.station.chan, CHANSIZE))
      return false;
    if (!xdr.xdr_bytes(hed.station.stype, STYPESIZE))
      return false;
    if (!xdr.xdr_float(hed.station.slat))
      return false;
    if (!xdr.xdr_float(hed.station.slon))
      return false;
    if (!xdr.xdr_float(hed.station.elev))
      return false;
    if (!xdr.xdr_float(hed.station.DS))
      return false;
    if (!xdr.xdr_float(hed.station.A0))
      return false;
    for (int l = 0; l < NOCALPTS; l++)
    {
      if (!xdr.xdr_float(hed.station.cal[l].pole.r))
        return false;
      if (!xdr.xdr_float(hed.station.cal[l].pole.i))
        return false;
      if (!xdr.xdr_float(hed.station.cal[l].zero.r))
        return false;
      if (!xdr.xdr_float(hed.station.cal[l].zero.i))
        return false;
    }
    if (!xdr.xdr_float(hed.event.lat))
      return false;
    if (!xdr.xdr_float(hed.event.lon))
      return false;
    if (!xdr.xdr_float(hed.event.dep))
      return false;
    if (!xdr.xdr_short(hed.event.ot.yr))
      return false;
    if (!xdr.xdr_short(hed.event.ot.mo))
      return false;
    if (!xdr.xdr_short(hed.event.ot.day))
      return false;
    if (!xdr.xdr_short(hed.event.ot.hr))
      return false;
    if (!xdr.xdr_short(hed.event.ot.mn))
      return false;
    if (!xdr.xdr_float(hed.event.ot.sec))
      return false;
    if (!xdr.xdr_bytes(hed.event.ecomment, COMSIZE))
      return false;
    if (!xdr.xdr_short(hed.record.type))
      return false;
    if (!xdr.xdr_int(hed.record.ndata))
      return false;
    if (!xdr.xdr_float(hed.record.delta))
      return false;
    if (!xdr.xdr_float(hed.record.maxamp))
      return false;
    if (!xdr.xdr_short(hed.record.abstime.yr))
      return false;
    if (!xdr.xdr_short(hed.record.abstime.mo))
      return false;
    if (!xdr.xdr_short(hed.record.abstime.day))
      return false;
    if (!xdr.xdr_short(hed.record.abstime.hr))
      return false;
    if (!xdr.xdr_short(hed.record.abstime.mn))
      return false;
    if (!xdr.xdr_float(hed.record.abstime.sec))
      return false;
    if (!xdr.xdr_float(hed.record.rmin))
      return false;
    if (!xdr.xdr_bytes(hed.record.rcomment, COMSIZE))
      return false;
    if (!xdr.xdr_bytes(hed.record.log, LOGSIZE))
      return false;
    if (!xdr.xdr_array(hed.extra))
      return false;
    return true;
  }

  /**
   * Writes the data array to the xdr stream.
   * @param hed the AH header.
   * @param xdr the XDR output.
   * @return true if success or false if error.
   */
  public static boolean xdr_putdata(AhHed hed,XdrOutput xdr)
  {
    final int maxIndex;
    switch (hed.record.type)
    {
      case FLOAT:
      case COMPLEX:
      case VECTOR:
      case TENSOR:
      case FLOAT4:
        maxIndex = hed.record.floatData.length;
        for (int i = 0; i < maxIndex; i++)
        {
          if (!xdr.xdr_float(hed.record.floatData[i]))
          {
            return false;
          }
        }
        break;
      case DOUBLE:
        maxIndex = hed.record.doubleData.length;
        for (int i = 0; i < maxIndex; i++)
        {
          if (!xdr.xdr_double(hed.record.doubleData[i]))
          {
            return false;
          }
        }
        break;
      default:
        return false;
    }
    return true;
  }

  private final static int AHHEADSIZE = 1024;
  private final static int CHANSIZE = 6;
  private final static int CODESIZE = 6;
  private final static int COMSIZE = 80;
  private final static int LOGSIZE = 202;
  private final static int NEXTRAS = 21;
  private final static int NOCALPTS = 30;
  private final static int STYPESIZE = 8;

  /** Null text */
  public final static String NULL_TEXT = "null";

  /** Empty text */
  public final static String EMPTY_TEXT = "";
}
