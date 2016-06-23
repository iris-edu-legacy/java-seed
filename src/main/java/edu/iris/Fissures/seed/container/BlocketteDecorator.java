package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.SeedException;

/**
 * BlocketteDecorator - interface representing all flavors of Blockette
 * Decorator classes
 */


public interface BlocketteDecorator {
    
    public void assignBlockette(Blockette blk) throws SeedException;
    
    public Blockette getBlockette();
    
}
