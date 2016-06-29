package edu.iris.dmc.seedcodec;

/**
 * Constants for the various data compression types in seed blockette 1000.
 * http://www.fdsn.org
 *
 *
 * Created: Thu Nov 21 16:35:37 2002
 *
 * @author <a href="mailto:crotwell@seis.sc.edu">Philip Crotwell</a>
 * @version 1.0.5
 */

public interface B1000Types {

    /** ascii */
    public static final int ASCII = 0;

    /** 16 bit integer, or java short */
    public static final int SHORT = 1;

    /** 24 bit integer */
    public static final int INT24 = 2;

    /** 32 bit integer, or java int */
    public static final int INTEGER = 3;

    /** ieee float */
    public static final int FLOAT = 4;

    /** ieee double*/
    public static final int DOUBLE = 5;

    /** Steim1 compression */
    public static final int STEIM1 = 10;

    /** Steim2 compression */
    public static final int STEIM2 = 11;
    
    /** CDSN 16 bit gain ranged */
    public static final int CDSN = 16;
        
    /** (A)SRO */
    public static final int SRO = 30;
    
    /** DWWSSN 16 bit */
    public static final int DWWSSN = 32;
}// B1000Types
