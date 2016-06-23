package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;

/**
  Generic Builder Container Interface

  Container facility that organizes the objects by volume number and/or
  object category number.  This class is typically called by a Concrete Builder
  class.  Recommended storage is through the use of HashMaps, although
  other techniques may be used as well.  This class will perform all
  sorting and lookup functions internally.  Each object should be tagged
  with a unique lookup integer, which is indicated by refNum in the
  interface specification below.

  @author Robert Casey, IRIS DMC
  @version 8/7/2003
  */

public interface ObjectContainer {

    /**
     * Add provided object to the container
     */
    public void add(Object addThis) throws Exception;

    /**
     * Find and return the object matching to refNum
     */
    public Object get(int refNum) throws ContainerException;

    /**
     * Remove the object matching to refNum
     */
    public Object remove(int refNum) throws ContainerException;

    /**
     * Locate the object matching to refNum and return true if found
     */
    public boolean locate(int refNum) throws ContainerException;

    /**
     * Locate all objects belonging to volume volNum and of category catNum.
     * Initialize an iteration list that can be traversed by getNext().
     * Return the number of objects in the iterator list.
     */
    public int iterate(int volNum, int catNum);  // specific volNum and catNum
    public int iterate(int catNum);              // specific catNum, all volumes
    public int iterate();                        // all objects

    /**
     * Get next object from established iterator list.
     * If iterator has no objects, then return null.
     */
    public Object getNext() throws ContainerException;

}
