package edu.iris.Fissures.seed.container;

import java.util.*;
import java.text.*;
import java.security.*;
import java.math.*;
import edu.iris.Fissures.seed.exception.*;

/**
 * This class provides a mapping between blockettes and the field values they would use
 * for memory map or other persistence tagging.  (see SeedObjectTag)
 * This class will return only the information it can glean from the current blockette
 * and is not aware of its current station, channel, and epoch context in the rest
 * of the SEED volume.  Therefore, this just supplies supplementary information.
 * @author Robert Casey, IRIS DMC
 * @version 7/28/10
 *
 */
public class SeedObjectTagMap {

    /**
     * Return a hashmap that represents the per tag field information available
     * in the current blockette provided.  This hashmap is compliant with that
     * used by SeedObjectTag (see idFieldNames).
     *
     * For context fill by a parent blockette, use a "?" in the field value to
     * signal value replacement by parent context.
     *
     * Stations have an instance field that follows it.  The convention is to use a single lower
     * case character to denote a unique instance of the station name.  Use the tag's increment()
     * method to seek a unique instance tag [a-z].
     */
    // "type","network","station","instance","chanloc","year","day","hour","minute","second","sequence"
    public static HashMap<String,String> getTagFields(Blockette blk) throws SeedException {
        HashMap<String,String> returnMap = new HashMap<String,String>();
        Btime timeVal = null;
        for (int i = 0; i < idFieldNames.length; i++) {
            int blkType = blk.getType();
            String fieldVal = "";  // default return
            switch(i) {
                case 0:   // type
                    fieldVal = blk.toString(1);
                    break;
                case 1:   // network
                    if (blkType == 8) fieldVal = blk.toString(12);
                    if (blkType == 10) {
                        fieldVal = blk.toString(8);  // organization name
                        if (fieldVal.length() > 16)   // max 16 chars
                            fieldVal = fieldVal.substring(0,16);
                    }
                    if (blkType == 11) fieldVal = "STATIONS";  // assert fixed value
                    if (blkType == 12) fieldVal = "TIMESPANS"; // assert fixed value
                    if (blkType > 29 && blkType < 50) fieldVal = "ABBREV";  // assert fixed value
                    if (blkType == 50) fieldVal = blk.toString(16);
                    if (blkType > 50 && blkType < 70) fieldVal = "?";  // inherit parent network name
                    if (blkType == 70 || blkType == 73) fieldVal = "TIME";
                    if (blkType == 71) fieldVal = "HYPO";
                    if (blkType == 72) fieldVal = blk.toString(12);
                    if (blkType == 74) fieldVal = blk.toString(16);
                    if (blkType == 999) fieldVal = blk.toString(7);
                    if (blkType > 999) fieldVal = "?";  // inherit parent network name
                    break;
                case 2:   // station -- also for lookup ID in abbreviations
                    if (blkType == 8) fieldVal = blk.toString(5);
                    if (blkType == 10) {
                        fieldVal = blk.toString(9);  // volume label
                        if (fieldVal.length() > 16)   // max 16 chars
                            fieldVal = fieldVal.substring(0,16);
                    }
                    if (blkType == 11) fieldVal = blk.toString(5);  // first sequence number
                    if (blkType == 12) fieldVal = blk.toString(6);  // first sequence number
                    if (blkType > 29 && blkType < 50) {
                        // use this field to express the lookup ID
                        fieldVal = "";  // default
                        switch(blkType) {
                            case 30:
                                fieldVal = blk.toString(4); // identifier code
                                try {
                                    fieldVal = fourDigit.format(fourDigit.parse(fieldVal));
                                } catch (ParseException e) {
                                    System.err.println("ERROR: ParseException thrown on blk 30 identifier code conversion");
                                    fieldVal = "0000";
                                }
                                break;
                            case 31:
                            case 32:
                            case 33:
                            case 34:
                            case 35:
                            case 41:
                            case 42:
                            case 49:
                            case 43:
                            case 44:
                            case 45:
                            case 46:
                            case 47:
                            case 48:
                                fieldVal = blk.toString(3); // lookup key
                                try {
                                    fieldVal = fourDigit.format(fourDigit.parse(fieldVal));
                                } catch (ParseException e) {
                                    System.err.println("ERROR: ParseException thrown on abbreviation identifier code conversion");
                                    fieldVal = "0000";
                                }
                                break;
                        }
                    }
                    if (blkType == 50) fieldVal =  blk.toString(3);
                    if (blkType > 50 && blkType < 70) fieldVal = "?";  // inherit parent station name
                    if (blkType == 70) fieldVal = "SPAN";
                    if (blkType == 71) fieldVal = "INFO";
                    if (blkType == 72) fieldVal = blk.toString(3);
                    if (blkType == 73) fieldVal = blk.toString(4,0);  // get only first example
                    if (blkType == 74) fieldVal = blk.toString(3);
                    if (blkType == 999) fieldVal = blk.toString(4);
                    if (blkType > 999) fieldVal = "?";  // inherit parent station name
                    break;
                case 3:   // instance -- also abbrevation string for dictionary
                    if (blkType == 8) fieldVal = blk.toString(6);
                    if (blkType == 11) fieldVal = blk.toString(3);  // station count
                    // use this field to show a bit of the abbreviation value
                    if (blkType > 29 && blkType < 50) {
                        fieldVal = "DICTIONARY";  // default
                        switch(blkType) {
                            case 30:
                                fieldVal = blk.toString(3);  // abbreviation
                                if (fieldVal.length() > 16)   // max 16 chars
                                    fieldVal = fieldVal.substring(0,16);
                                break;
                            case 31:
                                fieldVal = blk.toString(5);  // abbreviation
                                if (fieldVal.length() > 16)   // max 16 chars
                                    fieldVal = fieldVal.substring(0,16);
                                break;
                            case 32:
                                fieldVal = blk.toString(4);  // abbreviation
                                if (fieldVal.length() > 16)   // max 16 chars
                                    fieldVal = fieldVal.substring(0,16);
                                break;
                            case 33:
                                fieldVal = blk.toString(4);  // abbreviation
                                if (fieldVal.length() > 16)   // max 16 chars
                                    fieldVal = fieldVal.substring(0,16);
                                break;
                            case 34:
                                fieldVal = blk.toString(4);  // abbreviation
                                if (fieldVal.length() > 16)   // max 16 chars
                                    fieldVal = fieldVal.substring(0,16);
                                break;
                            case 35:
                                fieldVal = blk.toString(5) + blk.toString(6) + blk.toString(7) + blk.toString(8);  // abbreviation
                                if (fieldVal.length() > 16)   // max 16 chars
                                    fieldVal = fieldVal.substring(0,16);
                                break;
                            case 41:
                            case 42:
                            case 43:
                            case 44:
                            case 45:
                            case 46:
                            case 47:
                            case 48:
                            case 49:
                                fieldVal = blk.toString(4);  // abbreviation
                                if (fieldVal.length() > 16)   // max 16 chars
                                    fieldVal = fieldVal.substring(0,16);
                                break;
                        }
                    }
                    if (blkType == 50) fieldVal = "?";  // inherit prior instance
//                        if (blkType == 52) fieldVal = blk.toString(3);  // location code
                    if (blkType > 50 && blkType < 70) fieldVal = "?";  // inherit parent instance
                    if (blkType == 70) fieldVal = blk.toString(3);  // time span flag -- filler
                    if (blkType == 71) fieldVal = blk.toString(4);  // source identifier -- filler
                    if (blkType == 72) fieldVal = blk.toString(4);
                    if (blkType == 73) fieldVal = blk.toString(5,0);
                    if (blkType == 74) fieldVal = blk.toString(4);
                    if (blkType == 999) fieldVal = blk.toString(5);
                    if (blkType > 999) fieldVal = "?";  // inherit parent instance
                    break;
                case 4:   // chanloc -- can we supply short checksum for abbreviations here?
                    if (blkType == 8) fieldVal = blk.toString(7);

                    if (blkType > 29 && blkType < 50) {
                        String abbrevStr = "ABBREV";  // this will be used for formulating a hash
                        switch(blkType) {
                            case 30:
                                abbrevStr = blk.toString(3) + blk.toString(5) + blk.toString(6) + blk.toString(7);
                                break;
                            case 31:
                                abbrevStr = blk.toString(5);   // comment description
                                break;
                            case 32:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6);  // publication/date/publisher
                                break;
                            case 33:
                                abbrevStr = blk.toString(4);  // description
                                break;
                            case 34:
                                abbrevStr = blk.toString(4) + blk.toString(5);  // unit shorthand and description
                                break;
                            case 35:
                                abbrevStr = blk.toString(5) + blk.toString(6) + blk.toString(7) + blk.toString(8);  // identifiers
                                break;
                            case 41:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7) + blk.toString(8);
                                break;
                            case 42:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7) + blk.toString(8) +
                                        blk.toString(9) + blk.toString(10) + blk.toString(11) + blk.toString(12) + blk.toString(13) +
                                        blk.toString(14) + blk.toString(15);
                                break;
                            case 43:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7) + blk.toString(8) +
                                        blk.toString(9) + blk.toString(10) + blk.toString(15);
                                break;
                            case 44:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7) + blk.toString(8) +
                                        blk.toString(11);
                                break;
                            case 45:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7);
                                break;
                            case 46:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7);
                                break;
                            case 47:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7) + blk.toString(8) +
                                        blk.toString(9);
                            case 48:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7);
                                break;
                            case 49:
                                abbrevStr = blk.toString(4) + blk.toString(5) + blk.toString(6) + blk.toString(7) + blk.toString(8) +
                                        blk.toString(9) + blk.toString(10) + blk.toString(11) + blk.toString(12) + blk.toString(13) +
                                        blk.toString(14) + blk.toString(15);
                                break;
                        }
                        try {
                            //System.err.println("DEBUG: calling getShortHash for string: " + abbrevStr);
                            fieldVal = getShortHash(abbrevStr,5);  // fill this field with a hash snippet of 5 chars
                        } catch (NoSuchAlgorithmException e) {
                            System.err.println("WARNING: Cannot create MD5 hash for abbreviation tag, using substring instead");
                            fieldVal = abbrevStr.substring(0,5);
                        }
                    }

                    if (blkType == 50) fieldVal = "--"; // dashes -- filler
                    if (blkType == 51) fieldVal = "--"; // dashes -- filler
                    if (blkType == 52) fieldVal = blk.toString(3) + blk.toString(4);   // location + channel
                    if (blkType > 52 && blkType < 70) fieldVal = "?";  // inherit parent chanloc
                    if (blkType == 70) fieldVal = "DATA";
                    if (blkType == 71) fieldVal = blk.toString(12); // region code -- filler
                    if (blkType == 72) fieldVal = blk.toString(5);
                    if (blkType == 73) fieldVal = blk.toString(6,0);
                    if (blkType == 74) fieldVal = blk.toString(5);
                    if (blkType == 999) fieldVal = blk.toString(6);
                    if (blkType > 999) fieldVal = "?";  // inherit parent chanloc
                    break;
                case 5:   // year
                    // get the time field once for subsequent time fields
                    if (blkType == 8) timeVal = (Btime) blk.getFieldVal(8);
                            //if (blkType == 10) timeVal = (Btime) blk.getFieldObject(7);
                    if (blkType == 50) timeVal = (Btime) blk.getFieldVal(13);
                    		//if (blkType == 51) timeVal = (Btime) blk.getFieldVal(3);
                    if (blkType == 52) timeVal = (Btime) blk.getFieldVal(22);
                    		//if (blkType == 59) timeVal = (Btime) blk.getFieldVal(3);
                    if (blkType == 70) timeVal = (Btime) blk.getFieldVal(4);
                    if (blkType == 71) timeVal = (Btime) blk.getFieldVal(3);
                    if (blkType == 72) timeVal = (Btime) blk.getFieldVal(6);
                    if (blkType == 73) timeVal = (Btime) blk.getFieldVal(7,0);
                    if (blkType == 74) timeVal = (Btime) blk.getFieldVal(6);
                    if (blkType == 999) timeVal = (Btime) blk.getFieldVal(8);
                    // now get the field value for year
                    if (timeVal != null)
                        fieldVal = fourDigit.format(timeVal.getYear());  // assume four digit representation
                    else if (blkType == 51 || blkType > 52 && blkType < 70) fieldVal = "?";  // inherit channel year
                    else if (blkType > 999) fieldVal = "?";  // inherit channel year
                    break;
                case 6:   // day
                    if (timeVal != null)
                        fieldVal = threeDigit.format(timeVal.getDayOfYear());
                    else if (blkType == 51 || blkType > 52 && blkType < 70) fieldVal = "?";  // inherit
                    else if (blkType > 999) fieldVal = "?";  // inherit
                    break;
                case 7:   // hour
                   if (timeVal != null)
                        fieldVal = twoDigit.format(timeVal.getHour());
                    else if (blkType == 51 || blkType > 52 && blkType < 70) fieldVal = "?";  // inherit
                    else if (blkType > 999) fieldVal = "?";  // inherit
                    break;
                case 8:   // minute
                	if (timeVal != null)
                		fieldVal = twoDigit.format(timeVal.getMinute());
                	else if (blkType == 51 || blkType == 59) {
                		fieldVal = twoDigit.format( (Integer) blk.getFieldVal(5) / 100 ); // lookup code -- first two digits
                	}
                	else if (blkType > 52 && blkType < 70) fieldVal = "?";  // inherit
                	else if (blkType > 999) fieldVal = "?";  // inherit
                	break;
                case 9:  // second
                	if (timeVal != null)
                		fieldVal = twoDigit.format(timeVal.getSecond());
                	else if (blkType == 51 || blkType == 59) {
                		fieldVal = twoDigit.format( (Integer) blk.getFieldVal(5) % 100 ); // lookup code -- last two digits
                	}
                	else if (blkType > 52 && blkType < 70) fieldVal = "?";  // inherit
                	else if (blkType > 999) fieldVal = "?";  // inherit
                	break;
                case 10:  // sequence
                	int stageNum = blk.getStageNumber();
                	if (stageNum > -1) {
                		fieldVal = "" + stageNum;  // use response stage number for blockettes 53 through 62
                	} else {
                		if (blkType == 51 || blkType == 59) {
                			fieldVal = blk.toString(5);  // lookup code
                		}
                		if (blkType == 73) {
                			fieldVal = blk.toString(9,0); // subsequence num
                		}
                		if (blkType == 74) {
                			fieldVal = blk.toString(8);  // subsequence num
                		}
                		if (blkType == 999) {
                			fieldVal = blk.toString(18); // use byte offset val
                		} else if (blkType > 99) {
                			fieldVal = blk.toString(2); // all other data blockettes - byte offset val
                		} else if (timeVal != null) {   // if we have a time component in the tag ID...
                			fieldVal = "0000";  // apply a default value of zero
                		}
                	}
                	if (fieldVal.length() > 0) fieldVal = fourDigit.format(new Integer(fieldVal));  // make this a four digit number
                	break;
                default:  // empty response
                	fieldVal = "";
            }
            returnMap.put(idFieldNames[i],fieldVal);  // assign value to tag map
        }
        //
        return returnMap;  // return the tag hash map
    }


    // Take the provided blockette and modify certain fields based on the provided tag
    // "type","network","station","instance","chanloc","year","day","hour","minute","second","sequence"
    public static Blockette setTagFieldsToBlk(Blockette blk, SeedObjectTag tag) throws SeedException {
        HashMap<String,String> tagMap = SeedObjectTag.mapID(tag.toString());
        Blockette returnBlk = new Blockette(blk.toString());  // create a clone of the provided blockette
        Btime timeVal = new Btime();
        int blkType = blk.getType();
        for (int i = 0; i < idFieldNames.length; i++) {
            String tagField = tagMap.get(idFieldNames[i]);
            if (tagField == null || tagField.length() == 0) continue;
            switch(i) {
                case 0:   // type
                    // no change
                    break;
                case 1:   // network
                    if (blkType == 50) returnBlk.setFieldVal(16,tagField);
                    if (blkType == 72) returnBlk.setFieldVal(12,tagField);
                    if (blkType == 74) returnBlk.setFieldVal(16,tagField);
                    break;
                case 2:   // station
                    if (blkType > 29 && blkType < 50) {
                        // use this field to express the lookup ID
                        switch(blkType) {
                            case 30:
                                returnBlk.setFieldVal(4, tagField);
                                break;
                            case 31:
                            case 32:
                            case 33:
                            case 34:
                            case 35:
                            case 41:
                            case 42:
                            case 49:
                            case 43:
                            case 44:
                            case 45:
                            case 46:
                            case 47:
                            case 48:
                                returnBlk.setFieldVal(3, tagField);
                                break;
                        }
                    }
                    if (blkType == 50) returnBlk.setFieldVal(3, tagField);
                    break;
                case 3:   // instance  (was location)
                    break;
                case 4:   // channel + (location)
                    if (blkType == 52) {
                        if (tagField.length() == 0) continue;
                        int chan_offset = tagField.length() - 3;
                        if (chan_offset > 0) {  // we have location code to parse
                            String locID = tagField.substring(0, chan_offset);  // should be no more than 2 chars
                            locID = locID.replace('-',' ');  // replace dash characters with spaces
                            returnBlk.setFieldVal(3, locID);  // write to blockette field
                        } else {
                            returnBlk.setFieldVal(3,"  ");  // default to two spaces in location code field
                        }
                        String chan = tagField.substring(chan_offset,chan_offset+3); // should be no more than 3 chars
                        chan = chan.replace('-','X'); // replace dash characters with X
                        while (chan.length() < 3) chan = chan.concat("X");  // fill out to at least three characters
                        returnBlk.setFieldVal(4, chan);  // write to blockette field
                    }
                    break;
                case 5:   // year
                    timeVal.setYear(Integer.parseInt(tagField));
                    break;
                case 6:   // day   
                    timeVal.setDayOfYear(Integer.parseInt(tagField));
                    break;
                case 7:   // hour
                    timeVal.setHour(Integer.parseInt(tagField));
                    break;
                case 8:   // minute
                    timeVal.setMinute(Integer.parseInt(tagField));
                    break;
                case 9:
                    timeVal.setSecond(Integer.parseInt(tagField));
                    // will only assert time if we have all the time components
                    if (blkType == 50) returnBlk.setFieldVal(13,timeVal.toString());
                    //REC//if (blkType == 51) returnBlk.setFieldVal(3,timeVal.toString());
                    if (blkType == 52) returnBlk.setFieldVal(22,timeVal.toString());
                    //REC//if (blkType == 59) returnBlk.setFieldVal(3,timeVal.toString());
                    break;
                case 10:  // sequence
                    break;
            }
        }
        //
        return returnBlk;
    }


    // This code for getting an MD5 hash was modified from a snippet provided by
    //  http://snippets.dzone.com/user/cholland
    public static String getShortHash(String performedOn, int fieldSize) throws NoSuchAlgorithmException {
                if (messageDigest == null) messageDigest = MessageDigest.getInstance("MD5");
		byte[] data = performedOn.getBytes();
		messageDigest.update(data,0,data.length);
		BigInteger i = new BigInteger(1,messageDigest.digest());
		String md5result = String.format("%1$032X", i);

        // shorten this hash value so it is appropriate to the tag field
        // get the fieldSize number of characters starting at index 15
        return md5result.substring(15,15+fieldSize);
    }


    // key labels for the output hashmap
    private static final String[] idFieldNames = {  //TYPE.NET.STA.INST.CHNLOC.YEAR_DAY_HH_MM_SS.SEQ
        "type","network","station","instance","chanloc","year","day","hour","minute","second","sequence"
    };

    private static final DecimalFormat fourDigit = new DecimalFormat("0000");
    private static final DecimalFormat threeDigit = new DecimalFormat("000");
    private static final DecimalFormat twoDigit = new DecimalFormat("00");
    private static MessageDigest messageDigest;

}
