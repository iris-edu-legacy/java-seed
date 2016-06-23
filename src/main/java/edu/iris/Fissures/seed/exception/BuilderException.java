package edu.iris.Fissures.seed.exception;

/**
 *  A type of exception specific to problems encountered in
 *  the Builder Pattern (involving Director and Builder classes).
 *
 *  @author Robert Casey
 *  @version 10/9/2001
 */
public class BuilderException extends Exception {

	public BuilderException() {
		super();
	}
	
	public BuilderException(String s) {
		super(s);
	}
}
