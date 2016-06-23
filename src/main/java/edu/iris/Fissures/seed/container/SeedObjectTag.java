package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.SeedException;
import java.util.HashMap;
import java.text.*;
import java.util.Comparator;


/**
 * SEED object tag class for MMAP operations
 * @author rob
 * @version 7/23/2009
 */
public class SeedObjectTag implements ObjectTag<String>, Comparable<SeedObjectTag> {

    public SeedObjectTag(SeedObjectTag context, String blkMsg) throws SeedException {
        if (blkMsg == null || blkMsg.length() == 0) {
            throw new SeedException("ERROR: attempted to tag empty blockette string");
        }
        if (context == null) {
            setID(generateID(null, new Blockette(blkMsg)));  // no context
        } else {
            setID(generateID(mapID(context.getID()),new Blockette(blkMsg))); // with context
        }
    }
    

    public SeedObjectTag(String newTagID) {
        setID(newTagID);
    }

    // generate a object tag ID from known
    // SEED label parameters.  This will help us
    // to have a consistent labeling system.

    public String generateID(String type, String network, String station, String instance, 
            String location, String channel, String st_year, String st_jday, String st_hour,
            String st_min, String st_sec, String seq) {
        boolean stop = false;
        //
        // tag formatting -- see idFieldNames[]
        //TYPE.NET.STA.INST.CHNLOC.YEAR_DAY_HH_MM_SS.SEQ
        //
        StringBuffer idGen = new StringBuffer(type);
        idGen.append(idDelimiter);  // add in separator
        idGen.append(network);  // required
        idGen.append(idDelimiter);  // add in separator
        idGen.append(station);  // this is also required
        // the remainder is optional
        // we stop at the first null we see
        if (!stop && instance != null && instance.length() > 0) {
            idGen.append(idDelimiter);  // add in separator
            idGen.append(instance);  // single lower case character
        } else stop = true;
        if (!stop && location != null && location.length() > 0) {
            // modify spaces to dashes
            location = location.replaceAll(" ", "-");
            idGen.append(idDelimiter);
            idGen.append(location);
        } else stop = true;
        if (!stop && channel != null && channel.length() > 0) {
            idGen.append(channel);  // no delimiter used
        } else stop = true;
        if (!stop && st_year != null && st_year.length() > 0) {
            idGen.append(idDelimiter);
            idGen.append(st_year);
        } else stop = true;
        if (!stop && st_year != null) {
            // if we have year...
            // we have to assume that the rest of the time slots
            // need to be filled in.  Use a different separator.
            // insert default values where the values are empty.
            if (st_jday == null || st_jday.equals("")) st_jday = "001";
            idGen.append(timeDelimiter);
            idGen.append(st_jday);
            if (st_hour == null || st_hour.equals("")) st_hour = "00";
            idGen.append(timeDelimiter);
            idGen.append(st_hour);
            if (st_min == null || st_min.equals("")) st_min = "00";
            idGen.append(timeDelimiter);
            idGen.append(st_min);
            if (st_sec == null || st_sec.equals("")) st_sec = "00";
            idGen.append(timeDelimiter);
            idGen.append(st_sec);
        }
        // append a sequence number if we have complete time string (! stop)
        if (! stop && seq != null && seq.length() > 0) {
            idGen.append(idDelimiter);
            idGen.append(seq);
        } else if (! stop) {  // append a default if no value exists
        	idGen.append(idDelimiter);
        	idGen.append("0000");
        }

//System.out.println("DEBUG: generated ID: " + idGen);
        return idGen.toString();

    }

    // generate an ID from the provided hashmap params *merged* with a hashmap
    // of the current ID context.  Where a params field contains a question mark
    // string, the context value will be inserted in its place.
    public String generateID(HashMap<String,String> context,
                             HashMap<String,String> params) {
        if (params == null) return "";  // doesn't make sense to have null params
        if (context == null) {
            context = new HashMap<String,String>();
        }
        String chan = "---";
        String locID = "--";
        // let's merge the param values with the context
        for (int i = 0; i < idFieldNames.length; i++) {  // for each key name
            String param = params.get(idFieldNames[i]);
            // scrub the parameter for delimiter characters
            if (param != null) {
                param = param.replaceAll("[\\.\\_]", "-");
            }
            // check that we have a proper station instance value to inherit
            if (idFieldNames[i].equals("instance")) {  // special handling for 'instance' field
                String cStype = context.get("type");
                int ctype = 0;
                if (cStype != null) {
                    ctype = Integer.parseInt(cStype);
                }

                // type-specific inheritance -- only STATION type blockettes
                // can pass on a pre-existing tag
                if (param == null || (param.equals("?") && (ctype < 50 || ctype > 69))) {
                    param = "a"; // default instance value
                }
            }
            // replace ? in param with context value
            if (context.get(idFieldNames[i]) != null && param.equals("?"))
                params.put(idFieldNames[i],context.get(idFieldNames[i]));
            else  // or just put back scrubbed parameter
                params.put(idFieldNames[i],param);    

            // separate location and channel ids
            if (idFieldNames[i].equals("chanloc")) {
                 String tagField = params.get("chanloc");
                 if (tagField == null || tagField.length() < 3) continue;
                 int chan_offset = tagField.length() - 3;
                 if (chan_offset > 0) {  // we have location code to parse
                    locID = tagField.substring(0, chan_offset);  // should be no more than 2 chars
                 }
                 if (chan_offset >= 0) {
                    chan = tagField.substring(chan_offset,tagField.length()); // should be no more than 3 chars
                 }
            }
        }
        // with the merged maps, write out the string version of this tag
        //"type","network","station","instance","chanloc","year","day","hour","minute","second","sequence"
        return generateID(params.get("type"),params.get("network"),
                params.get("station"),params.get("instance"),
                locID,chan,
                params.get("year"),params.get("day"),
                params.get("hour"),params.get("minute"),params.get("second"),
                params.get("sequence")
                );
    }

    // generate an ID from the provided blockette merged with a hashmap
    // of the current ID context.  If the context is null, we will use
    // the current persistent tag id context.
    // return an ID string from this information
    public String generateID(HashMap<String,String> context, Blockette blk) {
        HashMap<String,String> blkMap = null;

        try {
            blkMap = SeedObjectTagMap.getTagFields(blk);
        } catch (SeedException e) {
            System.err.println("ERROR: unable to create blockette tag.");
            e.printStackTrace();
        }
        return generateID(context,blkMap);
    }

    // this is the sole means of setting the ID for this tag object
    // origins are generally from the generateID() methods
    public void setID(String tagID) {
        // remove colons from the ID - replace with dashes
        tagID = tagID.replaceAll("[\\:]+", "-");
        currentID = tagID;
    }

    public String getID() {
        if (currentID == null) return "";
        return currentID;
    }

    public String getIDField(String fieldName) {
        // fieldName is like "station" or "instance"
        HashMap<String,String> idMap = mapID();
        return idMap.get(fieldName);
    }

    // analogue to getID
    @Override
    public String toString() {
        return getID();
    }

    // get the type number this object represents
    public int getType() {
        HashMap<String,String> idMap = mapID();
        return Integer.parseInt(idMap.get("type"));  // return integer version of type
    }

    // return the category of blockette as defined by a set of constants.
    // return a -1 if the category is unknown.
    public int getCategory() {
        if (getType() < 30) return VOLUME;
        if (getType() < 50 && getType() > 29) return ABBREVIATION;
        if (getType() < 70 && getType() > 49) return STATION;
        if (getType() < 80 && getType() > 69) return TIMESERIES;
        if (getType() > 99) return WAVEFORM;
        return -1;  // UNKNOWN
    }

    // return the sequence number for this tag
    // if it does not have one, return a -1
    public int getSequenceNum() {
        HashMap<String,String> idMap = mapID();
        String seqStr = idMap.get("sequence");
        if (seqStr == null || seqStr.length() == 0) return -1;
        return Integer.parseInt(seqStr);  // return integer version of type
    }

    // assert a new lookup index value if this is an abbreviation tag
    public void setLookupIndex(int indexVal) {
        if (getCategory() == ABBREVIATION) {
            HashMap<String,String> fieldMap = mapID();
            // note that this is a four digit zero padded numeric string for all lookup IDs
            fieldMap.put("station",fourDigit.format(new Integer(indexVal)));  // 'station' field is where the index num is
            setID(generateID(null,fieldMap));  // reset the current ID
        }
    }

    // return a lookup index value if this is an abbreviation tag
    // otherwise return a -1
    public int getLookupIndex() {
        if (getCategory() == ABBREVIATION) {
            HashMap<String,String> fieldMap = mapID();
            return Integer.parseInt(fieldMap.get("station"));  // lookup in 'station' field position of abbreviation ID
        } else return -1;
    }

    // map a provided SeedObjectID to a HashMap for individual field parsing
    public static HashMap<String,String> mapID(String theID) {
        HashMap<String,String> returnMap = new HashMap<String,String>();
        String[] fieldVals = theID.split("[" + idDelimiter + timeDelimiter + "]");  // split out the fields by the tokenizers
        for (int i = 0; i < fieldVals.length; i++) {  // for each ID string field
            // extract the value to the vector
            returnMap.put(idFieldNames[i],fieldVals[i]);  // map this field
        }
        return returnMap;
    }

    // provide a HashMap representation for this object's ID
    public HashMap<String,String> mapID () {
        return mapID(getID());
    }
    
    // used for sorting (also see SeedObjectTagComparitor)
    public int compareTo(SeedObjectTag otherTag) {
        return this.compareTo(otherTag);
    }

    // use a specified comparitor to replace the rudimentary alpha sort
    // see SeedObjectTagComparitor
    public int compareTo(SeedObjectTag otherTag, Comparator<SeedObjectTag> c) {
        return c.compare(this, otherTag);
    }

    // modify the current tag so that it represents a new, hopefully unique
    // incremented ID representation from the one it currently has.  Used to
    // resolve collisions of two separate pieces of data attempting to share the
    // same storage space.
    // Return an integer representation of what was incremented in the ID.
    public void increment() {
        HashMap<String,String> idMap = mapID();  // get map of current fields
        int type = getType();  // get integer version of type
        int category = getCategory();
        if (category == VOLUME) {
            // volume header...
            // increment the last char
            char[] idArr = getID().toCharArray();
            idArr[idArr.length - 1]++;  // increment last char
            if (idArr[idArr.length - 1] > 90 || idArr[idArr.length - 1] < 65) idArr[idArr.length - 1] = 65;  // rewind to A after Z
            setID(new String(idArr));
        } else if (type == 50) {
            // stations...
            // increment the tag's 'instance' field by the next lower-case letter
            String instStr = (String) idMap.get("instance");
            char[] instArr = instStr.toCharArray();
            instArr[instArr.length - 1]++;
            if (instArr[instArr.length - 1] > 122 || instArr[instArr.length - 1] < 97) {
                instArr[instArr.length - 1] = 97;  // cycle back to 'a'
                // append a new letter and write back to map
                idMap.put("instance", new String(instArr) + "a");
            } else {
                // write back to map
                idMap.put("instance",new String(instArr));
            }
            setID(generateID(null,idMap));  // establish incremented ID string
         } else if (type == 52) {
            // channels...
            // increment the tag's time value by one second
            // for tag, okay to go past value of 59
            int sec = Integer.parseInt(idMap.get("second"));
            sec++;
            idMap.put("second",twoDigit.format(sec));
            setID(generateID(null,idMap));  // establish incremented ID string
        } else if (type == 51 || (type > 52 && type < 70)) {
            // responses and comments...
            // increment the seq number
            String seqStr = idMap.get("sequence");
            if (seqStr == null) seqStr = "0000";  // default to 0000, increment will bump it to 0001
            int seq = Integer.parseInt(seqStr);
            seq++;
            idMap.put("sequence", fourDigit.format(seq));
            setID(generateID(null,idMap));  // establish incremented ID string
        } else if (category == ABBREVIATION) {
            // for *dictionary blockettes*, we must find a new, unique lookup ID
            // this happens to be the 'station' field in the HashMap
            int seq = Integer.parseInt(idMap.get("station"));
            seq++; // increment the value by one -- validity much be checked external to this method
            idMap.put("station", String.valueOf(seq));
            setID(generateID(null,idMap));  // establish incremented ID string
        }
    }
    
    
    // instance vars

    private String currentID = null;
    private static final char idDelimiter = '.';
    private static final char timeDelimiter = '_';
    private static final String[] idFieldNames = {  //TYPE.NET.STA.INST.CHNLOC.YEAR_DAY_HH_MM_SS.SEQ
        "type","network","station","instance","chanloc","year","day","hour","minute","second","sequence"
    };

    // constants to attribute to categories of SEED blockette
    public static final int ALL          = 0;
    public static final int VOLUME       = 1;
    public static final int ABBREVIATION = 2;
    public static final int STATION      = 3;
    public static final int TIMESERIES   = 4;
    public static final int WAVEFORM     = 5;

    private static final DecimalFormat twoDigit = new DecimalFormat("00");
    private static final DecimalFormat fourDigit = new DecimalFormat("0000");

}