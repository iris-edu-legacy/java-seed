package edu.iris.dmc.seedcodec;

/**
 * DecompressedData.java
 *
 *
 * Created: Thu Nov 21 13:03:44 2002
 *
 * @author <a href="mailto:crotwell@seis.sc.edu">Philip Crotwell</a>
 * @version 1.0.5
 */

public class DecompressedData implements B1000Types {

	public DecompressedData(int[] data) {
		this.iData = data;
	}

	public DecompressedData(short[] data) {
		this.sData = data;
	}

	public DecompressedData(float[] data) {
		this.fData = data;
	}

	public DecompressedData(double[] data) {
		this.dData = data;
	}



	/**
	 * returns an integer that represent the java primitive that the data
	 * decompresses to. This is to allow for SEED types 4 and 5, float and
	 * double, which cannot be represented as int without a loss of precision.
	 * 
	 * @return
	 */
	public int getType() {
		if (iData != null) {
			return INTEGER;
		} else if (sData != null) {
			return SHORT;
		} else if (fData != null) {
			return FLOAT;
		} else {
			// assume double
			return DOUBLE;
		} // end of else
	}

	
	/**
	 * a string version of the type for printing in error messages
	 * 
	 * @return
	 */
	public String getTypeString() {
		if (iData != null) {
			return "INTEGER";
		} else if (sData != null) {
			return "SHORT";
		} else if (fData != null) {
			return "FLOAT";
		} else {
			// assume double
			return "DOUBLE";
		} // end of else
	}

	
	/**
	 * Converts the data to an int array if possible without loss. Otherwise
	 * 
	 * @return
	 */
	public int[] getAsInt() {
		int[] temp;
		if (iData != null) {
			return iData;
		} else if (sData != null) {
			temp = new int[sData.length];
			for (int i = 0; i < sData.length; i++) {
				temp[i] = sData[i];
			}
			return temp;
		}
		return null;
	}


	/**
	 * Converts the data to a short array if possible without loss. Otherwise
	 * 
	 * @return
	 */
	public short[] getAsShort() {
		if (sData != null) {
			return sData;
		}
		return null;
	}


	/**
	 * Converts the data to a float array if possible without loss. Otherwise
	 * 
	 * @return
	 */
	public float[] getAsFloat() {
		float[] temp;
		if (fData != null) {
			return fData;
		} else if (iData != null) {
			temp = new float[iData.length];
			for (int i = 0; i < iData.length; i++) {
				temp[i] = iData[i];
			}
			return temp;
		} else if (sData != null) {
			temp = new float[sData.length];
			for (int i = 0; i < sData.length; i++) {
				temp[i] = sData[i];
			}
			return temp;
		}
		return null;
	}

	
	/**
	 * Converts the data to a double array if possible without loss. Otherwise
	 * 
	 * @return
	 */
	public double[] getAsDouble() {
		double[] temp;
		if (dData != null) {
			return dData;
		} else if (fData != null) {
			dData = new double[fData.length];
			for (int i = 0; i < fData.length; i++) {
				dData[i] = fData[i];
			}
			return dData;
		} else if (iData != null) {
			dData = new double[iData.length];
			for (int i = 0; i < iData.length; i++) {
				dData[i] = iData[i];
			}
			return dData;
		} else if (sData != null) {
			dData = new double[sData.length];
			for (int i = 0; i < sData.length; i++) {
				dData[i] = sData[i];
			}
			return dData;
		}
		return null;
	}

	/**
	 * holds a temp int array of the data elements.
	 */
	protected int[] iData = null;

	/**
	 * holds a temp short array of the data elements.
	 */
	protected short[] sData = null;

	/**
	 * holds a temp float array of the data elements.
	 */
	protected float[] fData = null;

	/**
	 * holds a temp double array of the data elements.
	 */
	protected double[] dData = null;

}// DecompressedData
