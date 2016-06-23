package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Utility class for resolving DDL Dictionary Blockettes or Blockette 1000 integer
 * assigned values to a standard String name for the data encoding in question.
 * The returned String is most readily used by the Waveform class.
 * @author Robert Casey, IRIS DMC
 * @version 11/4/2002
 */
public class SeedEncodingResolver {

	/**
	 * Resolve DDL Blockette to an encoding String name.
	 * Using the provided Data Format Dictionary Blockette (Blockette 30),
	 * interpret and resolve the data encoding format represented based on
	 * a recognizable signature between the data format family type, number of
	 * decoder keys, and the decoder key strings themselves.  Each signature
	 * will translate to a single String keyword representing the name of the
	 * encoding type.  This keyword can be swapped for a Blockette 1000-compatible
	 * String number which also represents the unique encoding type.
	 */
	public static String resolve(Blockette dictBlk) {
		// use a deductive approach somewhat like a dichotomy key.
		String encodeString = "UNKNOWN";
		switch (Integer.parseInt(dictBlk.toString(5))) {  // encoding family
			case 0:
				String key1 = dictBlk.toString(7,1);
				if (key1 == null)
					break;
				else if (key1.equals("W2 D0-15 C2"))
					encodeString = "Int16Bit";
				else if (key1.equals("W3 D0-23 C2") ||
						key1.equals("W3 D24 C2"))
					encodeString = "Int24Bit";
				else if (key1.equals("W4 D0-31 C2"))
					encodeString = "Int32Bit";
				break;
			case 1:
				switch (Integer.parseInt(dictBlk.toString(6))) {  // number of keys
					case 4:
						key1 = dictBlk.toString(7,1);  // declared as String above
						if (key1 == null)
							break;
						if (key1.equals("W2 D0-11 A-2048")) {
							String key2 = dictBlk.toString(7,2);
							if (key2 == null)
								break;
							else if (key2.equals("D12-14"))
								encodeString = "GMuxed16Bit3Exp";
							else if (key2.equals("D12-15"))
								encodeString = "GMuxed16Bit4Exp";
						} else if (key1.equals("W2 D0-11 C2")) {
							encodeString = "SRO";
						} else if (key1.equals("W2 D0-13 A-8191")) {
							String key3 = dictBlk.toString(7,3);
							if (key3 == null)
								break;
							else if (key3.equals("P0:#0,1:#2,2:#4,3:#7"))
								encodeString = "CDSN";
							else if (key3.equals("P0:1,1:4,2:16,3:128"))
								encodeString = "RSTN";
						} else if (key1.equals("W2 D4-15 C2")) {
							encodeString = "GRAEF";
						} else if (key1.equals("W3,0,2,1 D0-15:1:0:-16384")) {
							encodeString = "KNMI";
						} else if (key1.equals("W4 D0-22 S31") ||
								key1.equals("W4, 1, 0, 3,2, D0-22 S31,0")) {
							encodeString = "Float";
						}
						break;
					case 5:
						encodeString = "Float";
						break;
				}
				break;
			case 50:
				switch (Integer.parseInt(dictBlk.toString(6))) {  // number of keys
					case 6:
						encodeString = "Steim1";
						break;
					case 14:
						encodeString = "Steim2";
						break;
					case 19:
						encodeString = "USNSN";
						break;
				}
				break;
			case 80:
				encodeString = "ASCIIText";
				break;
			default:
				encodeString = "UNKNOWN";
		}
		return encodeString;
	}

	/**
	 * Translate from encoding number to String name, or vice versa.
	 * String supplied is either a number or a word representing an encoding type.
	 * if a word is supplied, the corresponding number is returned.
	 * if a number is supplied, the corresponding word is returned.
	 */
	public static String translate (String s) {
		if (s.length() == 0) return new String("");
		if (s.charAt(0) < 65) {
			// if this is a number
			for (int i = 0; i < encodingArray.length; i+=2) {  // offset 0
				if (s.equals(encodingArray[i])) return encodingArray[i+1];  // return word
			}
			return ("UNKNOWN");
		} else {
			// if this is a word
			for (int i = 1; i < encodingArray.length; i+=2) {  // offset 1
				if (s.equals(encodingArray[i])) return encodingArray[i-1];  // return number
			}
			return ("999");   // equivalent to UNKNOWN
		}
	}

	private static final String[] encodingArray = {   // store number to name mappings of waveform encodings
		"0","ASCIIText",
		"1","Int16Bit",  //WAS: "1","16BitInt",
		"2","Int24Bit",  //WAS: "2","24BitInt",
		"3","Int32Bit",  //WAS: "3","32BitInt",
		"4","Float",
		"5","DoubleFloat",
		"10","Steim1",
		"11","Steim2",
		"12","GMuxed24Bit",
		"13","GMuxed16Bit3Exp",
		"14","GMuxed16Bit4Exp",
		"15","USNSN",
		"16","CDSN",
		"17","Graefenberg",
		"18","IPG",
		"19","Steim3",
		"30","SRO",
		"31","HGLP",
		"32","DWWSSN",
		"33","RSTN",
		"999","UNKNOWN"
	};
}
