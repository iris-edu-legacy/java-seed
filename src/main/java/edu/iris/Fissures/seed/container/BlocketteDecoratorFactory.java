package edu.iris.Fissures.seed.container;

import java.util.*;

import edu.iris.Fissures.seed.exception.ContainerException;
import edu.iris.Fissures.seed.exception.SeedException;
/**
 * BlocketteDecoratorFactory - singleton factory used for generating
 * new BlocketteDecorators of various flavors.  Also used to track
 * session-specific links, such as pointers to Containers
 * and other necessities that need to be detached from direct
 * reference by the Decorator instance itself.
 */


public class BlocketteDecoratorFactory {
    
    private BlocketteDecoratorFactory () {
        
    }
    
    // public methods
    
    /**
     * return a singleton instance of this factory class
     */
    public static BlocketteDecoratorFactory getInstance() {
        if (factoryInstance == null)
            factoryInstance = new BlocketteDecoratorFactory();
        return factoryInstance;
    }

    /**
     * Reset/initialize the object container map in this factory.
     * Sometimes necessary due to this being a static class instance.
     * Do so with care since the volume count is reset.
     */
    public static void reset() {
        Vector map = getInstance().objectContainerMap;
        map.clear();  // clear the object container map
    }

    
    /**
     * Construct and return a new CachedBlocketteDecorator seeding it with the offered Blockette
     * and indicating true if disk persistence caching is being used.
     * 
     * @param blk
     * @return a new CachedBlocketteDecorator
     * @throws ContainerException
     * @throws SeedException
     */
    public static CachedBlocketteDecorator createCachedDecorator(Blockette blk)
    throws ContainerException, SeedException {
        return new CachedBlocketteDecorator(blk);
    }
    
    /**
     * return a handle to the ObjectContainer that relates to this Volume Number
     */
    public static ObjectContainer getContainerByVol(int volNum) {
        Vector map = getInstance().objectContainerMap;
        if (volNum+1 > map.size()) return null;  // this volume is out of bounds
        return (ObjectContainer) map.get(volNum);
    }
    
    /**
     * Associate an ObjectContainer instance with the indicated volume number.
     * Typically called by the ObjectContainer in question to notify of its current
     * chosen volume number.  To keep the volume numbers from multiple containers
     * colliding with each other, call getNewVolumeNumber() in this class to
     * be handed a new volume number to use.
     * @param oc the ObjectContainer instance
     * @param volNum the volume number that relates to it
     */
    public static void setContainerByVol(ObjectContainer oc, int volNum) {
        Vector map = getInstance().objectContainerMap;
        if (volNum+1 > map.size()) map.setSize(volNum+1);
        map.setElementAt(oc,volNum);
    }
    
    /**
     * Strange but true.  Design smell?
     * Because of the container association by
     * volume number required by this singleton factory, this class
     * becomes an authority for assigning a new volume number
     * when a new container or volume in a container appears.
     * Note that setContainerByVol() must be called to actually carry out
     * assigning a container to this volume number.  Otherwise, the
     * same volume number will be handed to the next caller.
     * @return an incremented volume number to assign to a lookupID
     */
    public static int getNewVolumeNumber() {
        int size = getInstance().objectContainerMap.size();
        if (size == 0) return 1;
        else return size;
    }
    
    
    // instance variables
    private static BlocketteDecoratorFactory factoryInstance = null;  // singleton instance of this class
    
    private Vector objectContainerMap = new Vector(8,8);  // tracks object containers mapped by: volume number = index number
    
}