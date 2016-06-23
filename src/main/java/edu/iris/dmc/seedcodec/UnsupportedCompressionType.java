package edu.iris.dmc.seedcodec;


/**
 * UnsupportedCompressionType.java
 *
 *
 * Created: Thu Nov 21 16:50:33 2002
 *
 * @author <a href="mailto:crotwell@owl.seis.sc.edu">Philip Crotwell</a>
 * @version 1.0.5
 */

public class UnsupportedCompressionType extends CodecException {
    public UnsupportedCompressionType() {
	
    }

    public UnsupportedCompressionType(String reason) {
	super(reason);
    }
    
}// UnsupportedCompressionType
