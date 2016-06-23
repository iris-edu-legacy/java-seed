package edu.iris.Fissures.seed.container;

import java.util.*;

/**
 * Extension class of Vector for the purposes of adding and retrieving Blockette
 * objects.
 * @author Robert Casey (IRIS DMC)
 * @version 11/13/2002
 */
public class BlocketteVector extends Vector {

	/**
	 * Object instantiation with no parameters.
	 */
	BlocketteVector () {
	}

	/**
	 * Object instantiation with indicated capacity and increment size.
	 */
	BlocketteVector (int capacity, int incr) {
		super(capacity,incr);	
	}

	// modify Vector's add Object methods so that we constrain the added
	// Objects to being just Blockette type

	/**
	 * Add a Blockette to the vector at the specified index.
	 */
	void add(int index, Blockette blk) {
		super.add(index,blk);
	}

	/**
	 * Append a Blockette to the vector.
	 */
	boolean add(Blockette blk) {
		// always returns true
		return super.add(blk);
	}

	/**
	 * Append a Blockette to the vector.  Identical to add().
	 */
	void addElement(Blockette blk) {
		super.addElement(blk);
	}

	/**
	 * Get a Blockette object from the vector at the indicated index.
	 */
	Blockette getBlockette(int index) {
		return (Blockette) super.get(index);
	}
	

}
