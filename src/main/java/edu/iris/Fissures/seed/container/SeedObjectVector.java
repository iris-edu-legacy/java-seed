package edu.iris.Fissures.seed.container;

import java.util.*;

/**
 * Extension class of Vector for the purposes of adding and retrieving SeedObjects.
 * @author Robert Casey (IRIS DMC)
 * @version 10/22/2004
 */
class SeedObjectVector extends Vector {

	/**
	 * Object instantiation with no parameters.
	 */
	SeedObjectVector () {
	}

	/**
	 * Object instantiation with indicated capacity and increment size.
	 */
	SeedObjectVector (int capacity, int incr) {
		super(capacity,incr);	
	}

	// modify Vector's add Object methods so that we constrain the added
	// Objects to being just SeedObject type

	/**
	 * Add a SeedObject to the vector at the specified index.
	 */
	void add(int index, SeedObject blk) {
		super.add(index,blk);
	}

	/**
	 * Append a SeedObject to the vector.
	 */
	boolean add(SeedObject blk) {
		// always returns true
		return super.add(blk);
	}

	/**
	 * Append a SeedObject to the vector.  Identical to add().
	 */
	void addElement(SeedObject blk) {
		super.addElement(blk);
	}

	/**
	 * Get a SeedObject object from the vector at the indicated index.
	 */
	SeedObject getObject(int index) {
		return (SeedObject) super.get(index);
	}

}
