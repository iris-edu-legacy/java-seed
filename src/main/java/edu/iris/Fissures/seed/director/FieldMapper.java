package edu.iris.Fissures.seed.director;

import java.io.*;
import java.lang.*;
import java.util.*;
import edu.iris.Fissures.seed.exception.*;

/**
 * @author Robert Casey, IRIS DMC
 * @version 5/16/2003
 */


public abstract class FieldMapper implements Cloneable {

    public FieldMapper() {
	reset();
    }

    // insert values from this object into the mapping
    public abstract void insert(Object sourceObj) throws Exception;

    // return a properly formatted string that is compatible with the intended
    // Builder of the concrete class.
    public abstract String toString();

    // return the string that represents the type of Builder class that the concrete
    // version of this mapper is compatible with.
    public String getType() {
	return mapperType;
    }

    // blank out the mapper so that a new mapping can be made
    public void reset() {
	sourceMap = null;
	destMap = null;
	sourceSize = 0;
	destSize = 0;
	typeNum = 0;
	mapped = false;
    }

    // implement the Cloneable interface
    public Object clone() {
	try {
	    FieldMapper cloneMapper = (FieldMapper) super.clone();
	    return cloneMapper;
	} catch (Exception e) {
	    return null;
	}
    }


    // add the String value to the mapper, providing the index number of the
    // source object.  An exception will be thrown if the field mapping has not
    // yet been generated, using generateMap().
    //
    // If the value provided does not map to the destination object, then return
    // false, else return true.
    public boolean addField(int index, String value) throws Exception {
	if (! mapped)
	    throw new BuilderException("cannot add field value without establishing mapping first");
	//
	// illegal if the index value is less than zero
	if (index < 0)
	    throw new BuilderException("addField index number is less than zero");
	// illegal if the index value is out of bounds for the source array
	if (index >= sourceMap.length)
	    throw new BuilderException("addField index is out of bounds");
	//
	// if we find that this value does not map to the destination object, we
	// will return false.  No mapping is indicated by a 0 value.
	int destIndex = sourceMap[index];
	//System.err.println("DEBUG: sourceMap " + index + " returns destIndex " +
	//	destIndex);
	if (destIndex < 1) return false;
	//
	// check to see if the destination index is out of bounds
	if (destIndex >= destMap.length)
	    throw new BuilderException("destination map index returned from source map is out of bounds");
	//
	// enter the value into the mapping
	destMap[destIndex] = value;
	return true;  // successful mapping of the value
    }


    // get the value at the indicated source field number already mapped in this
    // FieldMapper instance.  Return an empty string if there is nothing mapped
    // here.  In some instances, a previous mapping may have been changed out
    // for a new one, so to get all values, step through the destMap array.
    public String getField(int index) throws Exception {
	if (! mapped)
	    throw new BuilderException("cannot get field value without establishing mapping first");
	//
	// illegal if the index value is less than zero 
	if (index < 0)
	    throw new BuilderException("getField index number is less than zero");
	// illegal if the index value is out of bounds for the source array
	if (index >= sourceMap.length)
	    throw new BuilderException("getField index is out of bounds");
	//
	// if we find that this value does not map to the destination object, we
	// will return false.  No mapping is indicated by a 0 value.
	int destIndex = sourceMap[index];
	if (destIndex < 1) return new String("");
	//
	// check to see if the destination index is out of bounds
	if (destIndex >= destMap.length)
	    throw new BuilderException("destination map index returned from source map is out of bounds");
	// get the field value and return it
	if (destMap[destIndex] == null) return new String("");
	else return destMap[destIndex];
    }


    // accepts a Vector of Integers containing pattern:
    // sourceField1, destField1, sourceField2, destField2, sourceField3,
    // destField3,.....
    //
    // This results in a mapping between the source field number and the
    // destination object field number.
    public void generateMap(Vector fieldMap) throws Exception {
	if (sourceMap == null || destMap == null)
	    throw new BuilderException("sourceMap and/or destMap need to have space allocated");
	for (int i = 0; i < fieldMap.size(); i+=2) {  // for every two values...
	    int sourceIndex = ((Integer) fieldMap.get(i)).intValue();
	    int destIndex = ((Integer) fieldMap.get(i+1)).intValue();
	    if (sourceIndex < 0 || destIndex < 0)
		throw new BuilderException("attempting to map an index value less than zero: ("
			+ sourceIndex + " --> " + destIndex + ")");
	    if (sourceIndex >= sourceMap.length) {
		throw new BuilderException(
			"source mapping index value is out of bounds: (typeNum=" +
			typeNum + ", sourceIndex="
			+ sourceIndex + ", destIndex=" + destIndex + ")");
	    }
	    if (destIndex >= destMap.length) {
		throw new BuilderException(
			"destination mapping index value is out of bounds: (typeNum=" +
			typeNum + ", sourceIndex="
			+ sourceIndex + ", destIndex=" + destIndex + ")");
	    }
	    sourceMap[sourceIndex] = destIndex;  // write in the index mapping
	}
	mapped = true;
    }

    // indicate the size of the array to index to the source fields
    // indicate the size of the array to index to the destination object fields
    public void allocateMapSpace(int sourceSize, int destSize) throws Exception {
	if (sourceSize <= 0 || destSize <= 0)
	    throw new BuilderException("allocated source or destination size is less than or equal to zero");
	// allocate array sizes
	sourceMap = new int[sourceSize];
	destMap = new String[destSize];
	// save sizes for persistence
	this.sourceSize = sourceSize;
	this.destSize = destSize;
    }

    // return the destination object type
    public int getDestObjType() {
	return typeNum;
    }

    // identify the intended destination object by an integer type number
    public void setDestObjType(int typeNum) {
	this.typeNum = typeNum;
    }


    // instance variables

    // the concrete class inheriting from this abstract class should report the
    // type of Builder this class is meant to write to.
    protected String mapperType = "UNKNOWN";
    //
    // index refers to source field number, value inside
    // points to index number in destMap, which contains
    // the destined field value for the object being mapped to.
    protected int[] sourceMap = null;
    //
    // index refers to the desination object field number,
    // value inside is a String value to be inserted in
    // that object field.
    protected String[] destMap = null;
    //
    protected int sourceSize = 0;   // size of the source map array
    protected int destSize = 0;     // size of the dest map array
    //
    protected int typeNum = 0;      // type of destination object to map to
    protected boolean mapped = false;  // flags true if map has been generated

}
