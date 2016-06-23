package edu.iris.Fissures.seed.container;

/**
 * Proxy class for SeedObjects.  Used as a card file for cached
 * SeedObjects that may or may not be in memory.
 * @author Robert Casey, IRIS DMC
 * @version 10/22/2004
 */
public class SeedObjectProxy extends SeedObject {
	
	public SeedObjectProxy(int type, int id) {
		super();
		objType = type;
		lookupId = id;
	}
	
	// implement the SeedObject abstract methods
	/**
	 * Return the object type represented by this proxy.
	 */
	public int getType() {
		return objType;
	}
	/**
	 * Return the lookup Id for this proxy.
	 */
	public int getLookupId() {
		return lookupId;
	}
	/**
	 * Give a string representation of this proxy.
	 */
	public String toString() {
		return new String("SeedObjectProxy: type=" + objType + ", lookupId=" + lookupId);
	}
	//
	// instance variables
	private int objType = 0;     // object type
	private int lookupId = 0;    // lookup ID of object
}
