package edu.iris.Fissures.seed.exception;

/**
 *  A type of exception specific to problems encountered with
 *  SEED formatting conventions.
 *
 *  @author Robert Casey
 *  @version 10/9/2001
 */
public class SeedFormatException extends SeedException {

	public SeedFormatException() {
		super();
	}
	
	public SeedFormatException(String s) {
		super(s);
	}
}
