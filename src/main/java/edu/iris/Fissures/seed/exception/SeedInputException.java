package edu.iris.Fissures.seed.exception;

/**
 *  A type of exception specific to problems encountered while
 *  reading SEED-formatted data.
 *
 *  @author Robert Casey
 *  @version 10/9/2001
 */
public class SeedInputException extends SeedException {
  public SeedInputException () {
          super();
  }
  public SeedInputException (String s) {
          super(s);
  }
}
