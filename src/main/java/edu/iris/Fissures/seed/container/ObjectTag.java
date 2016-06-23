package edu.iris.Fissures.seed.container;

/**
 * Generic class for MMAP buffer ID tags
 * @author rob
 * @version 1/28/2009
 */
public interface ObjectTag<N> {

    // create a new instance of ObjectTag assigned to the following
    // tag ID

    public void setID(N tagID);

    // get the current ID tag in its native object form
    public N getID();

    // get a string representation of the tag ID
    @Override
    public String toString();


}
