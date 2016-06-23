package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.util.*;
import edu.iris.Fissures.seed.exception.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.lang.*;

/**
 * Structure for storing SEED time elements.  Acts as a container for the BTIME structure.
 * Capable of String and Binary operations.
 * @author Robert Casey, IRIS DMC
 * @version 01/14/2008
 */
public class Btime extends SeedObject {

    /**
     * Construct object initialized to the current GMT time.
     */
    public Btime () throws SeedInputException {
	// store formatting errors in this buffer to bypass program crash
	formatErrors = new StringBuffer();
	GregorianCalendar mCal = new GregorianCalendar(tz);  // get the current time
	// generate Btime compatible string expression
	StringBuffer mBuf = new StringBuffer();
	mBuf.append(mCal.get(Calendar.YEAR));
	mBuf.append(",");
	mBuf.append(mCal.get(Calendar.DAY_OF_YEAR));
	mBuf.append(",");
	mBuf.append(mCal.get(Calendar.HOUR_OF_DAY));
	mBuf.append(":");
	mBuf.append(mCal.get(Calendar.MINUTE));
	mBuf.append(":");
	mBuf.append(mCal.get(Calendar.SECOND));
	setString(mBuf.toString());  // initialize object using string expression constructor
	formatCheck();  // check the format of the entered values
    }

    /**
     * Accept input BTIME array and indicated byte swap flag.
     */
    public Btime (byte[] timeArr, boolean byteSwapFlag) throws SeedInputException {
	// check to see that the byte size meets the minimum requirement of 10 bytes
	if (timeArr.length < 10) {
	    throw new SeedInputException ("input array is too small (" + timeArr.length + " bytes)");
	}
	formatErrors = new StringBuffer();  // store formatting errors in this buffer to bypass program crash
	swapFlag = byteSwapFlag;
	year = Utility.uBytesToInt(timeArr[0], timeArr[1], swapFlag);
	jday = Utility.uBytesToInt(timeArr[2], timeArr[3], swapFlag);
	hour = timeArr[4] & 0xff;
	min = timeArr[5] & 0xff;
	sec = timeArr[6] & 0xff;
	// timeArr[7] is unused (alignment)
	tenthMilli = Utility.uBytesToInt(timeArr[8], timeArr[9], swapFlag);
	formatCheck();  // check the format of the entered values
    }

    /**
     * Accept input BTIME array with automatic byte swap checking.
     */
    public Btime (byte[] timeArr) throws SeedInputException {
	// check to see that the byte size meets the minimum requirement of 10 bytes
	if (timeArr.length < 10) {
	    throw new SeedInputException ("input array is too small (" + timeArr.length + " bytes)");
	}
	formatErrors = new StringBuffer();  // store formatting errors in this buffer to bypass program crash
	year = Utility.uBytesToInt(timeArr[0], timeArr[1], false);  // test 68000 word order case
	swapFlag = (year < 1900 || year > 2050);  // swapFlag is true if VAX/8086 word order
	year = Utility.uBytesToInt(timeArr[0], timeArr[1], swapFlag);
	jday = Utility.uBytesToInt(timeArr[2], timeArr[3], swapFlag);
	hour = timeArr[4] & 0xff;
	min = timeArr[5] & 0xff;
	sec = timeArr[6] & 0xff;
	// timeArr[7] is unused (alignment)
	tenthMilli = Utility.uBytesToInt(timeArr[8], timeArr[9], swapFlag);
	formatCheck();  // check the format of the entered values
    }

    /**
     * Create object using string value.
     * String format must be YYYY,DDD,HH:MM:SS.FFFF.
     */
    public Btime (String timeString) throws SeedInputException {
	// store formatting errors in this buffer to bypass program crash
	formatErrors = new StringBuffer();
	setString(timeString);
	formatCheck();  // check the format of the entered values
    }

    // public methods

    /**
     * Return SeedObject type number.
     */
    public int getType() {
	// implement SeedObject abstract method
	return 10000;  // arbitrary value
    }

    /**
     * Return SeedObject lookup ID.
     */
    public int getLookupId() {
	// implement SeedObject abstract method
	return (int) getEpochTime();
    }

    /**
     * Return byte array consisting of SEED BTIME representation of time.
     */
    public byte[] getByteTime(boolean swapFlag) {
	byte[] timeArr = new byte[10];
	System.arraycopy(Utility.intToShortBytes(year,swapFlag),0,timeArr,0,2);
	System.arraycopy(Utility.intToShortBytes(jday,swapFlag),0,timeArr,2,2);
	timeArr[4] = (byte) hour;
	timeArr[5] = (byte) min;
	timeArr[6] = (byte) sec;
	timeArr[7] = 0;
	System.arraycopy(Utility.intToShortBytes(tenthMilli),0,timeArr,8,2);
	return timeArr;
    }

    public byte[] getByteTime() {
        return getByteTime(false);
    }

    /**
     * Get string representation of time.  Format is YYYY,DDD,HH:MM:SS.FFFF.
     */
    public String getStringTime () {
	// return string in standard jday format
	StringBuffer strTimeBuf = new StringBuffer();
	strTimeBuf.append(fourZero.format(year));
	strTimeBuf.append(","); 
	strTimeBuf.append(threeZero.format(jday));
	strTimeBuf.append(","); 
	strTimeBuf.append(twoZero.format(hour));
	strTimeBuf.append(":");
	strTimeBuf.append(twoZero.format(min));
	strTimeBuf.append(":");
	strTimeBuf.append(twoZero.format(sec));
	strTimeBuf.append(".");
	strTimeBuf.append(fourZero.format(tenthMilli));
	return strTimeBuf.toString();
    }

    /**
     * Get string representation of time.  Same as getStringTime();
     */
    public String toString() {
	return getStringTime();
    }

    /**
     * Return the number of seconds since January 1, 1970.
     * If the year is earlier than 1970, the result is a negative EpochTime.
     */
    public long getEpochTime() {
	long seconds = 0;
	for (int i = year; i < 1970; i++) {    // for each year up to 1970
	    seconds -= 60 * 60 * 24 * 365;     // subtract 365 days worth of seconds
	    boolean is_leap = ((i % 4 == 0) && (i % 100 != 0)) || (i % 400 == 0);
	    if (is_leap) seconds -= 60 * 60 * 24; // subtract a leap day of seconds for a leap year
	}
	// correction here submitted by Sid Hellman - ISTI - 12/16/2002
	for (int i = 1970; i < year; i++) {  // for each year from 1970 to previous year
	    seconds += 60 * 60 * 24 * 365;     // add 365 days worth of seconds
	    boolean is_leap = ((i % 4 == 0) && (i % 100 != 0)) || (i % 400 == 0);
	    if (is_leap) seconds += 60 * 60 * 24;   // add a leap day of seconds for a leap year
	}
	// finally, add the number of seconds within the year to this amount
	// to get the final result
	seconds += (long) (ttConvert() / 10000);
	return seconds;
    }

    /**
     * Compare our object's time with the offered <b>compareTime</b>.  Return true
     * if the two times are equal.
     */
    public boolean equals(Btime compareTime) {
	return (this.toString().equals(compareTime.toString()));
    }

    /**
     * Return time difference in seconds.  Subtract <b>minusTime</b> from our time
     * value and return the result in seconds.  (<i>return = ourTime -
     * minusTime</i>).
     */
    public long diffSeconds(Btime minusTime) {
	return getEpochTime() - minusTime.getEpochTime();
    }

    /**
     * Return the binary word swap flag.  The flag applies to binary reads.  The
     * value is TRUE for VAX/8086 word order.
     */
    public boolean getSwapFlag() {
	return swapFlag;
    }

    /**
     * Return error messages regarding the reading of a time string as input.
     */
    public String getError() {
	return formatErrors.toString();
    }

    /**
     * Compare our value to the provided object, which needs to be a Btime object.
     * Override Comparable method in SeedObject in favor of comparing
     * ten-thousandths of a second.
     * Return an integer indicating the comparison relationship of this
     * object to the object provided.
     * If this object has an earlier time value the the parameter object, return
     * -1.  If the parameter object is earlier than this object, return 1.  If
     *  equal, return 0.
     */
    public int compareTo(Object o) throws ClassCastException {
	if (o == null) throw new ClassCastException("null parameter value");
	Btime compareTime = (Btime) o;  // may throw cast exception
	long lDiff = diffSeconds(compareTime);  // get the difference in seconds
	if (lDiff < 0) return -1;
	if (lDiff > 0) return 1;
	// if they both match in terms of seconds, then compare the tenths of seconds
	double thisTT = ttConvert();  // ten-thousandths for this object
	double thatTT = compareTime.ttConvert(); // ten-thousandths for provided comparison object
	if (thisTT < thatTT) return -1;
	if (thisTT > thatTT) return 1;
	return 0; // all other cases, this must be equal...
    }

    /**
     * Return a new time value that is a projection from this object's current
     * time.
     * Take the Btime structure and forward-project a new time that is
     * the specified number of ten thousandths of seconds ahead.
     * Return a new Btime with the projected value.
     */
    public Btime projectTime (double tenThousandths) throws SeedInputException {
	Btime bTime = new Btime(this.getStringTime());  // get new copy of this object
	int offset = 0;  // leap year offset
	// check to see if this is a leap year we are starting on
	boolean is_leap = year % 4 == 0 && year % 100 != 0 || year % 400 == 0;
	if (is_leap) offset = 1;
	// convert bTime to tenths of seconds in the current year, then
	// add that value to the incremental time value tenThousandths
	tenThousandths += ttConvert();
	// now increment year if it crosses the year boundary
	if ((tenThousandths) >= (366+offset)*864000000.0) {
	    bTime.year++;
	    tenThousandths -= (365+offset)*864000000.0;
	}
	// increment day
	bTime.jday = (int) (tenThousandths / 864000000.0);
	tenThousandths -= (double) bTime.jday * 864000000.0;
	bTime.jday++;   // jday counts from 1, not 0
	// increment hour
	bTime.hour = (int) (tenThousandths / 36000000.0);
	tenThousandths -= (double) bTime.hour * 36000000.0;
	// increment minutes
	bTime.min = (int) (tenThousandths / 600000.0);
	tenThousandths -= (double) bTime.min * 600000.0;
	// increment seconds
	bTime.sec = (int) (tenThousandths / 10000.0);
	tenThousandths -= (double) bTime.sec * 10000.0;
	// set tenth seconds
	bTime.tenthMilli = (int) tenThousandths;
	// return the resultant value
	return bTime;
    }

    /**
     * Returns the month, day and year.
     * @return the month, day and year.
     */
    public MonthDayYear getMonthDayYear()
    {
      return new MonthDayYear(year,jday);
    }

    /**
     * Returns the month, day and year.
     * @param year the year.
     * @param jday the julian day.
     * @return the month, day and year.
     */
    public static MonthDayYear getMonthDayYear(int year, int jday) {
      return new MonthDayYear(year,jday);
    }

    /**
     * Convert a year and julian day value to a year/month/day
     * representation.  Calendar calculation is set to Lenient
     * which may result in date conversion to proper values.
     * Return string format is "YYYY/MM/DD".
     * @param year the year.
     * @param jday the julian day.
     * @return a year/month/day representation.
     */
    public static String getMonthDay(int year, int jday) {
      return new MonthDayYear(year,jday).toString();
    }

    /**
     * Convert year, month, and day values to a year/julian day
     * representation.  Calendar calculation is set to Lenient,
     * which may result in date conversion to proper values.
     * Return string format is "YYYY/JJJ".
     */
    public static String getJulianDay(int year, int month, int day) {
	// get calendar instance
	GregorianCalendar mCal = new GregorianCalendar(tz);
	// set the calendar values
	mCal.setLenient(true);
	mCal.set(Calendar.YEAR,year);
	mCal.set(Calendar.MONTH,(month-1));  // January is 0
	mCal.set(Calendar.DAY_OF_MONTH,day);
	mCal.set(Calendar.HOUR_OF_DAY,0);
	mCal.set(Calendar.MINUTE,0);
	mCal.set(Calendar.SECOND,0);
	// generate output string
	String mStr = "" + fourZero.format(mCal.get(Calendar.YEAR)) + "/" +
	    threeZero.format(mCal.get(Calendar.DAY_OF_YEAR));
	return mStr;
    }

    /**
      private int year = 2500;
      private int jday = 1;
      private int hour = 0;
      private int min = 0;
      private int sec = 0;
      private int tenthMilli = 0;
      */

    // public field accessor methods
    // (based on contribution from Sid Hellman - ISTI - 8/20/2003)

    public int getYear() {
	return year;
    }

    public int getDayOfYear() {
	return jday;
    }

    public int getHour() {
	return hour;
    }

    public int getMinute() {
	return min;
    }

    public int getSecond() {
	return sec;
    }

    public int getTenthMill() {
	return tenthMilli;
    }

    // public field mutator methods
    // (based on contribution from Sid Hellman - ISTI - 8/20/2003)

    public void setYear(int i) {
	year = i;
    }

    public void setDayOfYear(int i) {
	jday = i;
    }

    public void setHour(int i) {
	hour = i;
    }

    public void setMinute(int i) {
	min = i;
    }

    public void setSecond(int i) {
	sec = i;
    }

    public void setTenthMill(int i) {
	tenthMilli = i;
    }


    // protected methods

    /**
     * Get ten-thousandths representation of time.
     * Convert contents of Btime structure to the number of
     * ten thousandths of seconds it represents within that year.
     * Made a protected method to allow comparison between Btime objects.
     */
    protected double ttConvert () {
	// correction here submitted by Sid Hellman - ISTI - 12/16/2002.
	// must subtract 1 from jday to allow for fact that if we are on day 1,
	// we don't want to add a whole day's worth of seconds to the epoch time.
	double tenThousandths = (jday-1) * 864000000.0;  
	tenThousandths += hour * 36000000.0;
	tenThousandths += min * 600000.0;
	tenThousandths += sec * 10000.0;
	tenThousandths += tenthMilli;
	return tenThousandths;
    }

    // private methods

    /**
     * Set the time value using a formatted string.
     * String format must be YYYY,DDD,HH:MM:SS.FFFF.
     */
    private void setString (String timeString) {
	// tokenize the input string and begin adjusting time fields
	int begin = 0;
	int end = 0;
	int strLen = timeString.length();
	for (int i = 0; i < defaultTimes.length; i++) {  // for each time field
	    timeArr[i] = defaultTimes[i];  // assign default time first
	    // did we move beyond the string length? just use default time
	    if (begin >= strLen) continue;
	    end = timeString.indexOf(tokenizeTimes[i],begin);  // find next delimiter
	    if (end <= begin) {
		// no matching separator? get int value from remainder of string
		timeArr[i] = Integer.parseInt(timeString.substring(begin));
		begin = strLen;
	    } else {
		// substring next time token and get int value
		timeArr[i] = Integer.parseInt(timeString.substring(begin,end));
		begin = end + 1;
	    }
	}
	// assign time values to the object's time variables
	year = timeArr[0];
	jday = timeArr[1];
	hour = timeArr[2];
	min = timeArr[3];
	sec = timeArr[4];
	tenthMilli = timeArr[5];

	return;
    }


    /**
     * Check the format of the time fields.
     * Append error values to global <b>formatErrors</b> string buffer,
     * but do not trigger an exception.
     */
    private void formatCheck() {
	if (year < 1900 || year > 9999) {
	    formatErrors.append("Invalid year " + year);
	}
	if (jday < 1 || jday > 366) {
	    formatErrors.append("Invalid day of year " + jday);
	}
	if (hour < 0 || hour > 23) {
	    formatErrors.append("Invalid hour " + hour);
	}
	if (min < 0 || min > 59) {
	    formatErrors.append("Invalid minute " + min);
	}
	if (sec < 0 || sec > 59) {
	    formatErrors.append("Invalid second " + sec);
	}
	if (tenthMilli < 0 || tenthMilli > 9999) {
	    formatErrors.append("Invalid fraction second " + tenthMilli);
	}
	return;
    }

    // inner classes

    /**
     * Month, day and year.
     */
    public static class MonthDayYear
    {
      public MonthDayYear(int year,int jday)
      {
	// get calendar instance
	GregorianCalendar mCal = new GregorianCalendar(tz);
	// set the calendar values
	mCal.setLenient(true);
	mCal.set(Calendar.YEAR,year);
	mCal.set(Calendar.DAY_OF_YEAR,jday);
	mCal.set(Calendar.HOUR_OF_DAY,0);
	mCal.set(Calendar.MINUTE,0);
	mCal.set(Calendar.SECOND,0);

	month = mCal.get(Calendar.MONTH) + 1;  // January is 0 so add 1
        dayOfMonth = mCal.get(Calendar.DAY_OF_MONTH);
        this.year = mCal.get(Calendar.YEAR);
      }

      /**
       * Return a string representation of the month/day/year.
       * @return a string representation of the month/day/year.
       */
      public String toString()
      {
	return "" + fourZero.format(year) + "/" +
	    twoZero.format(month) + "/" +
	    twoZero.format(dayOfMonth);
      }

      /** the month (January is 1) */
      public final int month;

      /** the day of the month */
      public final int dayOfMonth;

      /** the year */
      public final int year;
    };

    // instance variables

    private int year = 2500;
    private int jday = 1;
    private int hour = 0;
    private int min = 0;
    private int sec = 0;
    private int tenthMilli = 0;
    private boolean swapFlag = false;  // flags word order of binary input to this object...true for VAX/8086 word order
    private StringBuffer formatErrors;   // this stores string reports of improper format in the time string
    private int[] timeArr = {2500,1,0,0,0,0};  // temporarily store time fields

    // static variables
    private static TimeZone tz = null;   // set this to our desired time zone
    private static DecimalFormat twoZero = null;
    private static DecimalFormat threeZero = null;
    private static DecimalFormat fourZero = null;
    private static final int[] defaultTimes = {2500,1,0,0,0,0};
    private static final String[] tokenizeTimes = {",",",",":",":","."," "};

    // static initialization block
    static {
	// set to GMT time zone
	tz = TimeZone.getTimeZone("GMT+00");
	// zero padding format of output numbers
	twoZero = new DecimalFormat("00");
	threeZero = new DecimalFormat("000");
	fourZero = new DecimalFormat("0000");
    }

}
