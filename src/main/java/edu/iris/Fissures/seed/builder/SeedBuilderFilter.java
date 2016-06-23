package edu.iris.Fissures.seed.builder;

import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.exception.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Concrete Builder Filter class for SEED volumes.
 * @author Robert Casey, IRIS DMC
 * @author Sid Hellman, ISTI
 * @author Kevin Frechette, ISTI
 * @version 8/13/2008
 */
public class SeedBuilderFilter extends BuilderFilter {

	/**
	 * Create new Seed Builder Filter.  Initializes starting constants.
	 */
	public SeedBuilderFilter () {
		// constants for the parameter strings
		// TIME_ZERO --> '0000,000,00:00:00.0000'
		// TIME_INF --> '2500,365,23:59:59.9999'
		addParameter("TIME_ZERO","0000,000,00:00:00.0000");
		addParameter("TIME_INF","2500,365,23:59:59.9999");

		// specify our filter type
		filterType = "SEED";
	}

	// public methods

	/**
	 * Perform filter test on the provided object.
	 * Comparative method that accepts a Blockette
	 * and compares its field values to the parameter list in this filter.
	 * Return TRUE if the Blockette fits the criteria, FALSE if not.<br>
	 * Current parameters implemented:<br>
	 * 'station' -- station ID<br>
	 * 'network' -- network code<br>
	 * 'location' -- location ID<br>
	 * 'channel' -- channel code<br>
	 * 'quality' -- data quality flag<br>
	 * 'start_time' -- start time<br>
	 * 'end_time' -- end time<br>
	 * 'min_lat' -- minimum latitude value<br>
	 * 'max_lat' -- maximum latitude value<br>
	 * 'min_lon' -- minimum longitude value<br>
	 * 'max_lon' -- maximum longitude value<br>
	 * 'min_elev' -- minimum elevation value<br>
	 * 'max_elev' -- maximum elevation value<br>
     * 'min_sample' -- minimum number of samples<br>
     * 'max_sample' -- maximum number of samples
	 * <p>
	 * We will automatically reserve the following parameters
	 * IDs for internal use:<br>
	 * 'TIME_ZERO' -- time value of zero<br>
	 * 'TIME_INF' -- time value of infinite
	 */
	public boolean qualify(Object o) throws BuilderException {
		Blockette blk = (Blockette) o;  // cast to a Blockette
		//
		// we will only operate on certain Blockette types, so if the Blockette type
		// is not what we want, then return true.
		// Primarily, we will be working with 'parent' station Blockettes
		int blkType = blk.getType();
		float blkVersion;
		try {
			blkVersion = blk.getVersion();
		} catch (Exception e) {
			throw new BuilderException(e.toString());
		}
		boolean testStation,testNetwork,testLatitude,testLongitude,testElevation,
		testStart,testEnd,testChannel,testLocation,testTime;
		switch (blkType) {
			case 50:
				// compare station parameter with blk 50 field 3
				testStation = testCompare("station",null,blk,3);
				// compare network code parameter with blk 50 field 10
				testNetwork = true;
				if (blkVersion >= 2.3) {
					testNetwork = testCompare("network",null,blk,16);
				}
				// compare parameter latitude with blk 50 field 4
				testLatitude = testCompare("min_lat","max_lat",blk,4);
				// compare parameter longitude with blk 50 field 5
				testLongitude = testCompare("min_lon","max_lon",blk,5);
				// compare parameter elevation with blk 50 field 6
				testElevation = testCompare("min_elev","max_elev",blk,6);
				// compare parameter start_time with blk 50 field 14 (end effective time)
				testStart = testCompare("start_time","TIME_INF",blk,14);
				// compare parameter end_time with blk 50 field 13 (start effective time)
				testEnd = testCompare("TIME_ZERO","end_time",blk,13);
				return (testStation && testNetwork && testLatitude && testLongitude &&
						testElevation && testStart && testEnd);
			case 52:
				// compare location identifier with blk 52 field 3
				testLocation = testCompare("location",null,blk,3);
				// compare channel code with blk 52 field 4
				testChannel = testCompare("channel",null,blk,4);
				// compare parameter start_time with blk 52 field 23 (end effective time)
				testStart = testCompare("start_time","TIME_INF",blk,23);
				// compare parameter end_time with blk 52 field 22 (start effective time)
				testEnd = testCompare("TIME_ZERO","end_time",blk,22);
				return (testLocation && testChannel && testStart && testEnd);
			case 72:
				// compare station identifier with blk 72 field 3
				testStation = testCompare("station",null,blk,3);
				// compare location identifier with blk 72 field 4
				testLocation = testCompare("location",null,blk,4);
				// compare channel code with blk 72 field 5
				testChannel = testCompare("channel",null,blk,5);
				// compare network code with blk 72 field 12
				testNetwork = true;
				if (blkVersion >= 2.3) {
					testNetwork = testCompare("network",null,blk,12);
				}
				return (testStation && testLocation && testChannel && testNetwork);
			case 73:
				// compare station identifier with blk 73 field 4
				testStation = testCompare("station",null,blk,4);
				// compare location identifier with blk 73 field 5
				testLocation = testCompare("location",null,blk,5);
				// compare channel code with blk 73 field 6
				testChannel = testCompare("channel",null,blk,6);
				// compare start_time and end_time parameters to blk 73 field 7
				testTime = testCompare("start_time","end_time",blk,7);
				return (testStation && testLocation && testChannel && testTime);
			case 74:
				// compare station identifier with blk 74 field 3
				testStation = testCompare("station",null,blk,3);
				// compare location identifier with blk 74 field 4
				testLocation = testCompare("location",null,blk,4);
				// compare channel code with blk 74 field 5
				testChannel = testCompare("channel",null,blk,5);
				// compare parameter start_time with blk 74 field 9 (series end time)
				testStart = testCompare("start_time","TIME_INF",blk,9);
				// compare parameter end_time with blk 74 field 6 (series start time)
				testEnd = testCompare("TIME_ZERO","end_time",blk,6);
				// compare network code with blk 74 field 16
				testNetwork = true;
				if (blkVersion >= 2.3) {
					testNetwork = testCompare("network",null,blk,16);
				}
				return (testStation && testLocation && testChannel && testStart && testEnd && testNetwork);
			case 999:
 				// compare data quality flags with FSDH field 2
 				if (!testCompare("quality",null,blk,2)) return false;
				// compare station identifier with FSDH field 4
				testStation = testCompare("station",null,blk,4);
				// compare location identifier with FSDH field 5
				testLocation = testCompare("location",null,blk,5);
				// compare channel code with blk FSDH field 6
				testChannel = testCompare("channel",null,blk,6);
				// compare network code with blk FSDH field 7
				testNetwork = true;
				if (blkVersion >= 2.3) {
					testNetwork = testCompare("network",null,blk,7);
				}
				// compare start_time and end_time parameters to blk FSDH field 8
				testTime = testCompare("start_time","end_time",blk,8);
                // compare number of samples with FSDH field 9
                if (!testCompare("min_sample","max_sample",blk,9)) return false;
				return (testStation && testLocation && testChannel && testNetwork && testTime);
			default:
				return true;   // otherwise, don't filter out the blockette
		}
	}

	/**
	 * Test blockette field value against filter parameter.
	 * Provide a method of comparsion between parameter identifier strings (the key values)
	 * and a blockette field, being provided the blockette object and the field number to
	 * compare with.  If param1 has a value and param2 is null, then param1 points to a
	 * a wildcard string that is compared to the blockette field.  If param1 and param2 are
	 * both present, then it is assumed that these parameters represent numbers and the value
	 * in the blockette field must fall between them in value.
	 * <b>REMEMBER:</b>  param1 and param2 are not the values themselves, but the
	 * key names to the values to compare to.
	 * Many times, a key name will point to a list of values, all of
	 * which are included in the comparison here.
	 * Return true if the match is successful.
	 */
	public boolean testCompare (String param1, String param2, Blockette blk, int blkField) {
		String bValue = blk.toString(blkField);  // get field value from the blockette
		return testCompare(param1,param2,bValue);
	}

	/**
	 * Test blockette field value against filter parameter.
	 * Synonym of testCompare that accepts a String value bValue as the
	 * blockette field Value.
	 */
	public boolean testCompare (String param1, String param2, String bValue) {
		Vector vParam1 = getParameter(param1);   // get the value list for parameter key 1
		Vector vParam2 = getParameter(param2);   // get the value list for parameter key 2
		if (vParam1 == null) {
			// this field is not being used in the filter
			return true;
		} else {
			for (int i=0; i < vParam1.size(); i++) {
				String eParam1 = vParam1.get(i).toString();
				if (vParam2 == null) { // param2 is sometimes null
					// check for special case with TIME_ZERO parameter
					if (param1.equals("TIME_ZERO")) {
						// if TIME_ZERO is the first parameter but the second parameter comes up
						// null, then this criteria is not actually being implemented
						return true;
					}
					// this is a glob match to one parameter.
					// return true if the glob match is successful.
					// wildcards '*' and '?' may be used in eParam1
					if (globMatch(eParam1,bValue)) return true;  // we only need one match
				} else {
					// this is a value comparison between the first parameter and the second
					// vParam1 and vParam2 always compare in lockstep.
					// vParam1 and vParam2 must be string representations of integer or float values
					// return true if param1 <= bValue <= param2
					if (i < vParam2.size()) { // do we have the same number of elements as in param1 ?
						String eParam2 = vParam2.get(i).toString();
						if (numCompare(eParam1,bValue) <= 0) {
							if (numCompare(eParam2,bValue) >= 0) {
								return true;
							}
						}
					} else {
						// we apparently have a criteria pairing mismatch,
						// so ignore any further criteria
						// and consider this a false condition
						return false;
					}
				}
			}
		}
		// if we have come this far, then we haven't run into a true condition yet, so therefore
		// the blockette must not meet the criteria...return false
		return false;
	}

}
