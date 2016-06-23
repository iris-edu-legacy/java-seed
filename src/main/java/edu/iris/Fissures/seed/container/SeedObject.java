package edu.iris.Fissures.seed.container;

import java.io.*;

/**
 * Abstract class for all SEED object entities.
 * This abstract class represents properties that are common among all of the
 * SeedObjects.
 * @author Robert Casey (IRIS DMC)
 * @version 10/23/2002
 */
public abstract class SeedObject implements Comparable, Serializable {

	/**
	 * Return a number that represents the type of SeedObject this is.
	 * Blockette's, for instance, return their Blockette type number.
	 */
	public abstract int getType();  // each SEED entity should have a type number

	/**
	 * Express the contents of this SeedObject as a String.
	 */
	public abstract String toString();  // each SEED entity should be able to express itself in a String

	/**
	 * Each SEED object should have a unique Lookup ID if they are to be
	 * distinguished uniquely from one another.
	 */
	public abstract int getLookupId();  // return the lookup ID number for this SEED object

	/**
	 * Compare and sort SeedObjects relative to each other.  Implements the
	 * Comparable interface.  SeedObjects will compare their lookup ID's.
	 */
	public int compareTo(Object o) throws ClassCastException {
		SeedObject compareObject = (SeedObject) o;  // cast parameter Object to a Blockette
		Integer ourId = new Integer(getLookupId());    // generate Integer object from int lookupId
		Integer compareId = new Integer(compareObject.getLookupId());  // also for parameter lookupId
		return ourId.compareTo(compareId);    // return the comparison value
	}
}
