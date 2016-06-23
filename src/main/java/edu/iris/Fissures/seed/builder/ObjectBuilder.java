package edu.iris.Fissures.seed.builder;

import edu.iris.Fissures.seed.exception.*;
import edu.iris.Fissures.seed.container.*;
import java.util.*;

/**
 * This class is the abstract representation of all builders for the SEED classes
 * framework.  The primary builder class derived from this will be for
 * building SEED objects, but the design is being left open for other types of
 * things to be built on the import end of things.
 * @author Robert Casey, IRIS DMC
 * @version 1/12/2010
 */
public abstract class ObjectBuilder {
    
    /**
     * Create a new object builder.
     */
    public ObjectBuilder () {
    }
    
    // public methods
    
    
    /**
     * Register an instantiated BuilderFilter with this builder.
     * The filter 'type' pertains to the format of data that the filter compares against,
     * and must match the type of the concrete builder class.
     * So, a SeedObjectBuilder will only accept BuilderFilters that report a
     * filterType of "SEED".
     * <p>
     * The BuilderFilter acts as a 'request module' or 'query module' that will restrict
     * what objects are built and stored, based on the restriction parameters contained
     * in the registered BuilderFilter object attached to this Builder.
     * <p>	
     * Because a single BuilderFilter parameter is an OR condition (such as
     * you get a positive response for a station match if ANY ONE of the stations in the list
     * matches), to have a list of separate, internally bound conditions (such as one station
     * ask for long-period data, but another station ask for broadband data), each condition
     * 'cluster' must be contained in a separate BuilderFilter.
     * <p>
     * For this reason, the registerFilter method accepts multiple builderFilters to be registered
     * with it, each representing a separate nucleus of acceptable values all bound to each other.
     * Simply call registerFilter multiple times, each with a different BuilderFilter instance and
     * each will be added to a Vector.  Once registered, a BuilderFilter cannot be unregistered.
     */
    public void registerFilter (BuilderFilter bf) throws BuilderException {
        if (bf.getType().equals(builderType)) {
            buildFilterVector.add(bf);  // register the filter...append to vector
        } else {
            // filter type mismatch, throw exception
            throw new BuilderException ("attempted to assign filter type " + bf.getType() +
                    "to this builder of type " + builderType);
        }
    }
    
    /**
     * Get Object Builder type.
     * This method returns the type of builder we are in the form of a unique string.
     */
    public String getType() throws BuilderException {
        if (builderType.equals("UNKNOWN")) {
            throw new BuilderException("builder type is UNKNOWN");
        }
        return builderType;
    }
    
    /**
     * Return the most recently built object to the caller.
     * Returns null if there is no current object available.
     */
    public Object getObject() {
        return currentObject;
    }
    
    /**
     * Return the persistent storage container to the caller.
     */
    public ObjectContainer getContainer() throws BuilderException {
        if (buildContainer == null) {
            throw new BuilderException("unable to locate object container");
        }
        return buildContainer;
    }
    
    /**
     * Display a String representation of the Builder's current object.
     * Once an object is built, we have the option of viewing a String
     * of the object's contents.
     */
    public String toString() {
        Object curObj = null;
        curObj = getObject();
        if (curObj != null) {
            return curObj.toString();   // the object had better be able to express itself
        } else {
            return "";
        }
    }
    
    /**
     * Indicate the type of record the object is being built from.
     * Use this method to indicate the type of data record, using a letter or digit
     * interpretation is left to child builder class.
     */
    public void setRecordType(byte recType) {
        recordType = (char) recType;
    }
    
    /**
     * This method flags that the arriving record section is a continuation
     * of a previous record section.  Usually happens at logical record boundaries
     * interpretation is left to child builder class.
     */
    public void setContinuationFlag(boolean flag) {
        continuationFlag = flag;
    }
    
    /**
     * Set this flag to true if the incoming record section originated from the beginning
     * of a logical record.  This helps to orient the builder as to where in the logical
     * record the record section came from.
     * Interpretation is left to concrete Builder class
     */
    public void setRecordBeginFlag (boolean flag) {
        recordBeginFlag = flag;
    }

    /**
     * Set this flag to true when the current input group represents a continuation of coefficients,
     * where the previous group ran out of characters to represent the entire dataset.  This
     * allows the two coefficient sets to merge into a single object.
     */
    public void setLargeCoeffFlag (boolean flag) {
        largeCoeffFlag = flag;
    }
    
    /**
     * Delete current object from the Builder.  Set's current object pointer to
     * null.
     */
    public void removeCurrent() {
        // reset the current object holder to blank...meant to be called externally, especially by the
        // calling director class.
        currentObject = null;
    }
    
    /**
     * Get the volume number that this builder is currently set at.
     * @return the volume number that the builder is set to
     */
    public int getVolume() {
        return volumeNumber;
    }
    
    /**
     * Increment the volume number when a new data stream is being read in.
     * Also reset state of the Builder.
     */
    public void incrementVolume() {
        volumeNumber++;
        reset();
    }
    
    /**
     * Set the volume number to specific starting value.  Generally this is just done at the start of the Builder.
     * @param volNum volume number to set to
     */
    public void setVolume(int volNum) {
        volumeNumber = volNum;
        reset();
    }
    
    
    // abstract methods
    
    /**
     * This is the generic build function that the caller uses to have objects
     * constructed.  Returns the number of bytes consumed by the builder when reading
     * from the record.
     */
    public abstract int build(byte[] record) throws Exception;
    
    /**
     * Construct an object or objects from the delimited String.
     * Return the length in bytes of the processed String as a
     * confirmation of success.  Return -1 on failure.
     */
    public abstract int build (String delStr) throws Exception;
    
    
    /**
     * Store object to concrete Object Container.
     * Once an object is built, we have the option of storing it into some
     * form of persistent container for later reference.  If another build is
     * called before the currently created object is stored, the current object
     * is discarded.  Returns an integer reflecting the object's assigned ID
     * number, or a -1 on failure.
     */
    public abstract int store() throws Exception;
    
    /**
     * Reset the builder to its initial state.
     * Generally used during volume transitions.
     */
    public abstract void reset();
    
    
    // instance variables
    protected String builderType = "UNKNOWN";   // stores the builder type string, which is assigned by child class
    protected ObjectContainer buildContainer = null;  // generic container handle for storing and organizing built objects
    protected Object currentObject = null;  // generic handle for an object constructed by build();
    protected Vector buildFilterVector = new Vector(8,8);  // contains a list of zero to many BuilderFilters
    protected char recordType = ' ';        // current record type
    protected boolean continuationFlag = false;  // flags that record section is continuation to previous
    protected boolean recordBeginFlag = false;  // flags that record section is from beginning of logical record
    protected boolean largeCoeffFlag = false;  // flags that we have more coefficients for the current object
    protected int volumeNumber = 0;  // volume number for the current data import
    
}
