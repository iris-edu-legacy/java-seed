package edu.iris.Fissures.seed.director;

import edu.iris.Fissures.seed.exception.*;
import java.util.*;

/**
 * Template class used to arrange objects for export.
 * ExportTemplate class accepts an ordered list of reference numbers (lookup IDs)
 * that have been selected for inclusion in an export volume.
 * The ExportTemplate registers itself with an Export Director, which will 
 * extract a sequence of reference numbers relating to a particular object type.
 * <p>
 * The list of reference numbers represents a subset of the current Object Container
 * set of objects and confines the contents of the export volume to those objects referenced
 * by this subset.  No direct object references are stored here, which facilitates an
 * additional level of indirection that the ExportDirector can use for object access
 * optimization.
 * <p>
 * It may be necessary for the calling application to synthesize objects specifically
 * for the export volume, storing them in the Object Container used by the Export Director
 * and listing their reference numbers in this template.  Such synthesized objects must
 * have unique reference numbers.
 * @author Robert Casey, IRIS DMC
 * @version 11/5/2004
 */
public class ExportTemplate {
	
	/**
	 * Create Export Template instance.  Internal mappings are initialized.
	 */
	public ExportTemplate() {
		refNumMap = new HashMap();  // initialize the reference number map
		uniqMap = new HashMap();  // initialize the reference number map
	}
	
	// public methods
	
	/**
	 * Add a reference number (refNum) to the template, grouped by
	 * the indicated object type, which may be contextual to a parent.
	 * Adding a value of 0 is legal and marks a 'hole' in the group sequence.
	 * <b>objType</b> is a long value to allow for a large value space.
	 */
	public void addRefNum(long objType, int refNum) throws BuilderException {
		// check to see that this is a sane reference.
		if (refNum < 0) {
			throw new BuilderException("illegal reference number: " + refNum + " for objType " + objType);
		}
		// check to see that this is a sane object type
		if (objType <= 0) {
			throw new BuilderException("illegal object type: " + objType + " for refNum " + refNum);
		}
		// check to see if this reference number has already been entered.
		// we only want to add the entry once, as in the case of dictionary
		// objects.  Return silently if the reference already exists.
		// the exception is with the 0 marker.
		if (refNum != 0 && uniqMap.containsKey(new Integer(refNum))) return;
		// get the Vector mapped to this object type
		Vector mapVec = (Vector) refNumMap.get(new Long(objType));
		if (mapVec == null) {
			// if a Vector has not been mapped yet, then create a new one
			mapVec = new Vector();
			// the first value is always an integer value to act as a persistent index.
			// the persistent index always points to the next reference number to return
			// during a getNext() call.  Initialized to a value of 1 for the first
			// element we are about to add.
			mapVec.add(new Integer(1));
			refNumMap.put(new Long(objType),mapVec);  // map the new vector to the object type
		}
		mapVec.add(new Integer(refNum));  // append the reference number to the vector
		uniqMap.put(new Integer(refNum), new Integer(1));   // enter refNum into uniqueness map
		//System.err.println("DEBUG: ExportTemplate: type=" + objType + ", add=" + refNum +
		//		", add result=" + mapVec);
	}
	
	/**
	 * Reset all of the indexes in the object type maps to 0, which
	 * places all getNext() reads at the starting point.
	 */
	public void reset() throws BuilderException {
		long[] objTypes = getObjectTypes();
		int objTypesSize = objTypes.length; 
		for (int i=0; i < objTypesSize; i++) {
			Vector mapVec = (Vector) refNumMap.get(new Long(objTypes[i]));
			if (mapVec.size() > 0) mapVec.set(0,new Integer(1));  // reset index to 1
		}
	}
	
	/**
	 * Return the next reference number in an ordered list that
	 * corresponds to an object of the specified type.
	 * objType is of type long due to the addition of parent context lookupID number
	 * ...so type is parent context specific (Blk 54 of parent A different than Blk 54 of parent B).
	 * Typically called by the concrete Export Director.
	 * Return -1 if there is not a reference Id to return.
	 */
	public int getNext(long objType) throws BuilderException {
		// check to see that this is a sane object type
		if (objType <= 0) {
			throw new BuilderException("illegal object type: " + objType);
		}
		// get the Vector mapped to this object type (or reference number)
		Vector mapVec = (Vector) refNumMap.get(new Long(objType));
		//System.err.println("DEBUG: template.getNext mapVec(" + objType + "): " + mapVec);
		if (mapVec == null) {
			// if a vector has not been mapped to this object type, then we treat it as an empty
			// set...return -1.
			return -1;
		}
		// now that we have the reference number vector, we will consult the Integer value at
		// index 0 to find the index number we want to return next...then we increment the
		// index number by one and put it back into index 0.
		// when the index number exceeds the number of elements, return a -1.
		//
		// the reference number stored at a given index might be the value 0, which is used
		// as a break point for a group of objects of the same type that might be associated with
		// a parent object.  Encountering a 0 signals the end of the group, and the index still increments
		// to point to the next value, which is probably the start of a new group.  An empty group of that
		// type can be signified by a single 0 entry being added to the vector.
		//
		int nextIdx = ((Integer) mapVec.get(0)).intValue();
		if (nextIdx < 1) throw new BuilderException("illegal refNum vector index: " + nextIdx);  // sanity check
		int refNum = 0;
		if (nextIdx >= mapVec.size()) {
			// this object type has exhausted its list of reference numbers...return -1
			refNum = -1;
		} else {
			//System.err.println("DEBUG: template getNext objType " + objType + " index number " + nextIdx);
			refNum = Integer.parseInt(mapVec.get(nextIdx).toString());  // get the reference number
			nextIdx++;  // increment index number
			mapVec.set(0,new Integer(nextIdx));  // place incremented index number back into vector index 0
		}
		return refNum;  // return the reference number
	}
	
	/**
	 * Return an array of object type numbers pertaining to our current holdings.
	 */
	public long[] getObjectTypes() {
		Set mapEntries = refNumMap.entrySet();
		long[] returnTypes = new long[mapEntries.size()];
		Iterator mapIter = mapEntries.iterator();
		int typesIdx = 0;
		int returnSize = returnTypes.length;
		while (mapIter.hasNext() && typesIdx < returnSize) {
			Map.Entry entry = (Map.Entry) mapIter.next();
			long objType = ((Long) entry.getKey()).longValue();
			returnTypes[typesIdx++] = objType;
		}
		return returnTypes;
	}
	
	/**
	 * This method is for debugging purposes only.  It dumps all of the
	 * contents of refNumMap and uniqMap to stderr.
	 */
	public void debugDump() {
		// this method is for debugging purposes only...it dumps all of the
		// contents of refNumMap and uniqMap to stderr.
		System.err.println ("refNumMap debug dump:\n" + refNumMap.toString());
		System.err.println ("uniqMap debug dump:\n" + uniqMap.toString());
	}
	
	// instance variables
	//
	// we will keep these values in protected mode in case we need to make a
	// subclass of this template.
	protected HashMap refNumMap = null;   // each key is an object type, pointing to a Vector of reference numbers with persistent index
	protected HashMap uniqMap = null;     // each key is a reference number, indicating that the blockette has already been listed
	
}
