package edu.iris.dmc.seedcodec;

/**
 *  A type of exception specific to problems encountered with
 *  Steim compression.
 *
 *  @author Robert Casey
 *  @version 11/20/2002
 */
public class SteimException extends CodecException {

	public SteimException() {
		super();
	}
	
	public SteimException(String s) {
		super(s);
	}
}

