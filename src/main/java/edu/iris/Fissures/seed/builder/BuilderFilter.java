package edu.iris.Fissures.seed.builder;

import edu.iris.Fissures.seed.exception.*;
import edu.iris.Fissures.seed.container.*;
import java.util.*;

/**
 * Abstract class for all filters applied to Builders.
 * These filters act as a 'request template' to help Builders select which
 * objects to keep and which to throw out as they are built.  The filter
 * consists of one or more 'parameters', which are value/key pairs that
 * are used in comparison to the contents of a built object.  When the
 * qualify() method is used, the built object passed to the filter determines
 * whether the object fits the criteria or not.  The list of parameters can
 * have the same key many times over, with different values.  This is interpreted
 * in an OR fashion, where a match to any one of the values will result in a passing
 * grade.  Glob-style wildcards ('?','*') can be used in a parameter 
 * value to generalize the matching criteria even further.
 * @author Robert Casey, IRIS DMC
 * @version 7/17/2002
 */

public abstract class BuilderFilter {

	/**
	 * Create new Builder Filter.
	 */
	public BuilderFilter () {
		parameterMap = new HashMap();
		paramIterator = null;
	}

	// public methods

	/**
	 * Get filter type.
	 * This method returns the type of filter we are in the form of a unique string.
	 * The filter type should match the builder type it is matched to.
	 */
	public String getType() throws BuilderException {
		if (filterType.equals("UNKNOWN")) {
			throw new BuilderException("filter type is UNKNOWN");
		}
		return filterType;
	}

	/**
	 * Add a key/value pair to the filter.
	 * Because we can have more than one value per unique key, we will
	 * make the <b>value</b> parameter be a Vector of Strings.
	 */
	public void addParameter(String key, String value) {
		// first get an existing set of values for this key, should they
		// be present
		Vector parameterGet = getParameter(key);  // get list of values from Map
		if (parameterGet == null) parameterGet = new Vector(); // create new Vector if empty
		parameterGet.add(value);  // add value to vector
		parameterMap.put(key,parameterGet);  // map modified vector to key
	}

	/**
	 * Get filter parameter(s).
	 * Get one or more values from the filter matching the provided key.
	 * Multiple values will result from having different values assigned
	 * to the same key.
	 * Return a Vector of Strings, being the list of values attached to that key.
	 * Return null if the key is not listed.
	 */
	public Vector getParameter(String key) {
		if (key == null) return null;
		Vector parameterGet = (Vector) parameterMap.get(key);
		if (parameterGet == null) return null;
		else return parameterGet;
	}

	/**
	 * Reset the filter.
	 * Erase the parameter map to reset the filter.
	 */
	public void reset() {
		parameterMap = new HashMap();
		paramIterator = null;
	}

	/**
	 * Set up the iterator.
	 * Pass through the key/value pairs one by one
	 * in the case of multiple values, each value associated with a key
	 * will be displayed individually per iteration.
	 * Return the number of iterative elements.
	 */
	public int iterate() {
		Set entries = parameterMap.entrySet();
		Iterator tempIterator = entries.iterator();
		Vector paramVector = new Vector(8,8);  // establish blank iterator vector
		while (tempIterator.hasNext()) {  // next key
			String[] tempArr = new String[2];
			Map.Entry entry = (Map.Entry) tempIterator.next();
			tempArr[0] = entry.getKey().toString();         // get key
			Vector tempVector = (Vector) entry.getValue();  // get value Vector
			for (int i = 0; i < tempVector.size(); i++) {   // for each value...
				tempArr[1] = tempVector.get(i).toString();
				paramVector.add(tempArr); // add new key/value pair to temp vector
			}
		}
		paramIterator = paramVector.iterator();  // get an Iterator interface from assembled vector
		return paramVector.size();   // return the number of elements
	}

	/**
	 * Get the next key/value pair from the iterator.
	 * <b>Returns a two-element array</b>, with the first element being the key
	 * and the second element the value.
	 * In the case of multiple values, each value associated with a key
	 * will be displayed individually per iteration.
	 * Return null if there are no more elements.
	 */
	public String[] getNext() {
		if (paramIterator == null) return null;
		if (paramIterator.hasNext()) {
			return (String[]) paramIterator.next();  // get next
		} else {
			return null;
		}	
	}

	// abstract methods

	/**
	 * Comparative method that accepts and object of expected type and compares
	 * the instance values within to the parameter list in this filter.
	 * Return true if the Object fits the criteria, false if not.
	 */
	public abstract boolean qualify(Object o) throws BuilderException;

	// protected methods

	/**
	 * Glob-match value to criteria.
	 * <b>criteria</b> can have wildcards '*' and '?', whereas
	 * <b>value</b> does not treat these characters as wildcards.
	 * return true if match successful.
	 */
	protected boolean globMatch(String criteria, String value) {
		if (criteria == null || value == null) return false;  // graceful handling of null
		int length = criteria.length();
		while (--length >= 0 && criteria.charAt(length) == '*'); // back up over all stars at the end
		if (length < 0) return true;  // if nothing but stars, then it is a match by default
		length++;
		boolean star_end = (length < criteria.length());
		int i = 0;
		int j = 0;
		while (i < value.length()) {
			if (j >= length) return star_end;
			if (criteria.charAt(j) == '*') {
				j++;
				if (j >= criteria.length()) return true;
				while (criteria.charAt(j) != value.charAt(i) && i < value.length()) i++;
				if (i >= value.length()) return false;
				else {
					j++;
					i++;
					continue;
				}
			}
			if (criteria.charAt(j) == '?') {
				i++;
				j++;
				continue;
			}
			if (criteria.charAt(j) != value.charAt(i)) return false;
			i++;
			j++;
		}
		if (j < length) return false;
		else return true;
	}

	/**
	 * Compare two numeric or Btime strings to each other.
	 * Return -1 if first value less than second value.
	 * Return 1 if first value greater than second value.
	 */
	protected int numCompare(String value1, String value2) {
		if (value1 == null) return -1;  // graceful handling of null
		if (value2 == null) return 1;   // graceful handling of null
		if (value1.indexOf(',') > -1) {
			// this is a time comparison
			Btime bTime1;
			Btime bTime2;
			try {
				bTime1 = new Btime(value1);
			} catch (Exception e) {
				return -1;    // graceful handling of exception
			}
			try {
				bTime2 = new Btime(value2);
			} catch (Exception e) {
				return 1;     // graceful handling of exception
			}
			return bTime1.compareTo(bTime2);
		} else if (value1.indexOf('.') > -1 || value2.indexOf('.') > -1) {
			// this is a floating point comparison
			Double fValue1 = new Double(value1);
			Double fValue2 = new Double(value2);
			return fValue1.compareTo(fValue2);
		} else {
			// this is an integer comparison
			Long iValue1 = new Long(value1);
			Long iValue2 = new Long(value2);
			return iValue1.compareTo(iValue2);
		}
	}

	// instance variables

	protected HashMap parameterMap;
	protected Iterator paramIterator;
	protected String filterType = "UNKNOWN";

}
