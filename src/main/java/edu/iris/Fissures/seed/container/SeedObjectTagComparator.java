package edu.iris.Fissures.seed.container;

import java.util.*;

/**
 * This class is the implementation to allow a collection of SeedObjectTags to be
 * sorted by their ID contents.
 * @author rob
 * @version 7/23/09
 */

public class SeedObjectTagComparator implements Comparator<SeedObjectTag> {
    // return a value less than 0, equal to 0, or greater than 0 depending on
    // whether tag1 is less than, equal to, or greater than tag2
    public int compare(SeedObjectTag tag1, SeedObjectTag tag2) {
        //TYPE.NET.STA.INST.CHNLOC.YEAR_DAY_HH_MM_SS.SEQ
        // break out the fields into String arrays
        String tag1String = tag1.toString();
        String tag2String = tag2.toString();
       
        //System.err.println("DEBUG: compare(t1: " + tag1String + ", t2: " + tag2String + ")");
        
        // do an equals comparator first
        if (tag1String.equals(tag2String)) return 0;
        // otherwise, a more sophisticated sorting result must be worked out
        String[] tag1Fields = tag1String.split("[" + idDelimiter + "]");
        String[] tag2Fields = tag2String.split("[" + idDelimiter + "]");
        int type1 = Integer.parseInt(tag1Fields[0]);
        int type2 = Integer.parseInt(tag2Fields[0]);
        //
        // lexically compare volume and abbreviation blockettes
        if (type1 < 50 || type2 < 50)
            return tag1String.compareTo(tag2String);
        //
        // lexically compare time series header blockettes
        if ( (type1 > 69 && type1 < 100) || (type2 > 69 && type2 < 100) )
            return tag1String.compareTo(tag2String);
        //
        // lexically compare blockette 51 to surrounding blockettes
        if (type1 == 51 || type2 == 51) {
        	return tag1String.compareTo(tag2String);
        }
        try {
            // station/channel/response blockettes, special sorting
            for (int i = 0; i < tag1Fields.length; i++) {
                switch(i) {
                    case 0:   // type
                    	// Do nothing at this step
                        break;
                    // order by lexical comparison of the following fields
                    case 1:   // network
                    case 2:   // station
                    case 3:   // instance
                        if (tag1Fields[i].compareTo(tag2Fields[i]) != 0)
                            return tag1Fields[i].compareTo(tag2Fields[i]);
                        break;
                    case 4:   // compare field 5 (date and time) first
                        if (tag1Fields[5].compareTo(tag2Fields[5]) != 0) {
                        	if (type1 == 59 || type2 == 59) {
                        		// for blockette 59, let's ignore minutes and seconds in the comparison
                        		// as we are using this for differentiation between 59's
                        		String[] tag1Arr = tag1Fields[5].split("_");
                        		String tag1Time = String.join("_", Arrays.copyOf(tag1Arr, 3));  // shorten to length 3: yr_dy_hr
                        		String[] tag2Arr = tag2Fields[5].split("_");
                        		String tag2Time = String.join("_", Arrays.copyOf(tag2Arr, 3));
                        		// now check again
                        		if (tag1Time.compareTo(tag2Time) != 0)
                        			return tag1Time.compareTo(tag2Time);
                        	} else
                        		return tag1Fields[5].compareTo(tag2Fields[5]);
                        }
                        break;
                    case 5:   // compare field 4 (channel and location)
                        if (tag1Fields[4].compareTo(tag2Fields[4]) != 0)
                            return tag1Fields[4].compareTo(tag2Fields[4]);
                        break;
                    case 6:   // sequence
                        int seq1 = Integer.parseInt(tag1Fields[i]);  // get integer value
                        int seq2 = Integer.parseInt(tag2Fields[i]);  // get integer value
                        // precise control over blockette ordering
                        // order == respBlkOrder[]
                        int rank1 = 0;
                        int rank2 = 0;
                        for (int j = 0; j < respBlkOrder.length; j++) {
                            if (type1 == respBlkOrder[j]) rank1 = j;  // get ordinal
                            if (type2 == respBlkOrder[j]) rank2 = j;  // get ordinal
                        }
                        // sequence represents the stage number -- ranks sort within
                        // so seq is multiplier (group) and rank is additive
                        if (rank1 > 0 && rank2 > 0) {
                            // put stage zero sensitivity at the end of the response group
                            // bump up sensitivity blockette (blk 58, stage 0) for comparison purposes
                        	// bump up all blockette 59's past sensitivity blockette
                            if ((type1 == 58 && seq1 == 0) || type1 == 59) seq1 = (seq1 + 1) * 100;
                            if ((type2 == 58 && seq2 == 0) || type2 == 59) seq2 = (seq2 + 1) * 100;
                            // in all cases, multiply seq1 and 2 by 100 to place all blockette orderings within a stage
                            seq1 *= 100;
                            seq2 *= 100;
                            rank1 += seq1;  // add rank for blockette ordering
                            rank2 += seq2;
                            // result should determine which comes before the other
                            return Integer.compare(rank1, rank2);
                        }
                } // switch
            } // for
        } // try
        catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("ERROR: ArrayIndexOutOfBoundsException thrown comparing:");
            System.err.println("------ tag1 = " + tag1String);
            System.err.println("------ tag2 = " + tag2String);
            e.printStackTrace();
        }
        //
        // if all else fails
        return tag1String.compareTo(tag2String);
    }

    private static final char idDelimiter = '.';
    private static final int[] respBlkOrder = {-1,60,53,54,61,62,55,56,57,58,59}; // 59 is a channel comment
    
}