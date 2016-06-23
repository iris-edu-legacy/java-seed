package edu.iris.Fissures.seed.container;

import java.util.HashMap;
import java.util.Map;
import java.text.DecimalFormat;

/**
 *  Factory class for processing numeric formatting of Strings, using the provided
 *  format string.
 *
 * @author Kevin Frechette, ISTI
 */

public abstract class DataFormat {
	/**
	 * Double data format.
	 */
	private static class DoubleDataFormat extends DataFormat {
		/**
		 * Create the double data format.
		 * 
		 * @param df
		 *            the decimal format.
		 */
		public DoubleDataFormat(DecimalFormat df) {
			super(df);
		}

		/**
		 * Formats the number text to produce a string.
		 * 
		 * @param s
		 *            the number text.
		 * @return Formatted string.
		 */
		protected String getFmtVal(String s) {
			return df.format(Double.parseDouble(s));
		}
	}

	/**
	 * Exponent data format.
	 */
	private static class ExpDataFormat extends DoubleDataFormat {
		/**
		 * Create the exponent data format.
		 * 
		 * @param df
		 *            the decimal format.
		 */
		public ExpDataFormat(DecimalFormat df) {
			super(df);
		}

		/**
		 * Formats an object to produce a string.
		 * 
		 * @param obj
		 *            Formats an object to produce a string.
		 * @return Formatted string.
		 */
		protected String getFmtVal(Object obj) {
			String fmtVal = super.getFmtVal(obj);
			// if this value has an exponent, make sure that it has a sign
			// character right after
			// the exponent marker 'E'
			int expPos = -1;
			if ((expPos = fmtVal.indexOf('E')) > 0) {
				if (expPos + 1 < fmtVal.length()
						&& Character.isDigit(fmtVal.charAt(expPos + 1))) {
					// insert a plus symbol after the E
					fmtVal = new StringBuilder(fmtVal).insert(expPos + 1, '+')
							.toString();
				}
			}
			return fmtVal;
		}
	}

	/**
	 * Long data format.
	 */
	private static class LongDataFormat extends DataFormat {
		/**
		 * Create the long data format.
		 * 
		 * @param df
		 *            the decimal format.
		 */
		public LongDataFormat(DecimalFormat df) {
			super(df);
		}

		/**
		 * Formats the number text to produce a string.
		 * 
		 * @param s
		 *            the number text.
		 * @return Formatted string.
		 */
		protected String getFmtVal(String s) {
			return df.format(Long.parseLong(s));
		}
	}

	/** Map of data format with format string as key. */
	private static final Map<String, DataFormat> dataFormatMap;

	private static final boolean fmtValMapFlag;

	/**
	 * Data format option: 0 = No caching at all, 1 = No caching values, other =
	 * Normal.
	 */
	private static final String javaSeedDataFormatOptionKey = "JavaSeedDataFormatOption";

	/** Plus prefix text. */
	private static String PLUS_PREFIX_TEXT = "+";

	static {
		String s = getProperty(javaSeedDataFormatOptionKey);
		if ("0".equals(s)) {
			System.out.println("not using data format map");
			dataFormatMap = null;
			fmtValMapFlag = false;
		} else {
			if ("1".equals(s)) {
				fmtValMapFlag = false;
				System.out.println("using data format map");
			} else {
				fmtValMapFlag = true;
			}
			dataFormatMap = new HashMap<String, DataFormat>();
		}
	}

	/**
	 * Clears the data format map and all of the formatted value maps. This can
	 * be done after loading all the input files.
	 */
	public static void clearCache() {
		if (dataFormatMap != null) {
			for (DataFormat df : dataFormatMap.values()) {
				if (df.fmtValMap != null) {
					df.fmtValMap.clear();
				}
			}
			dataFormatMap.clear();
		}
	}

	/**
	 * Get the data format for the specified format string.
	 * 
	 * @param fmtString
	 *            the format string.
	 * @return the data format.
	 */
	public static DataFormat getDataFormat(final String fmtString) {
		DataFormat ff = null;
		if (dataFormatMap != null) {
			ff = dataFormatMap.get(fmtString);
		}
		if (ff == null) {
			final DecimalFormat df;
			if (fmtString.startsWith(PLUS_PREFIX_TEXT)) {
				df = new DecimalFormat(fmtString.substring(1));
				df.setPositivePrefix(PLUS_PREFIX_TEXT);
			} else {
				df = new DecimalFormat(fmtString);
			}
			// check for exponent in the mask
			if (fmtString.indexOf('E') > 0) {
				ff = new ExpDataFormat(df);
			}
			// check for floating point in the mask
			else if (fmtString.indexOf('.') > 0) {
				// string represents float
				ff = new DoubleDataFormat(df);
			} else {
				// string represents long int
				ff = new LongDataFormat(df);
			}
			if (dataFormatMap != null) {
				// use a new string with trimmed character array
				dataFormatMap.put(new String(fmtString), ff);
			}
		}
		return ff;
	}

	/**
	 * Get the data format for the specified format string.
	 * 
	 * @param fmtString
	 *            the format string.
	 * @param plusPrefixFlag
	 *            true to use plus sign for a positive prefix, false otherwise.
	 * @return the data format.
	 */
	public static DataFormat getDataFormat(String fmtString,
			boolean plusPrefixFlag) {
		if (plusPrefixFlag) {
			fmtString = PLUS_PREFIX_TEXT + fmtString;
		}
		return getDataFormat(fmtString);
	}

	/**
	 * Get the property.
	 * 
	 * @param key
	 *            the key.
	 * 
	 * @return the property or null if none.
	 */
	private static String getProperty(String key) {
		String s = null;
		try {
			s = System.getProperty(key);
		} catch (Exception ex) {
		}
		return s;
	}

	/** The decimal format. */
	protected final DecimalFormat df;

	/** Formatted value map or null if none. */
	private final Map<Object, String> fmtValMap;

	public DataFormat(DecimalFormat df) {
		this.df = df;
		if (fmtValMapFlag) {
			fmtValMap = new HashMap<Object, String>();
		} else {
			fmtValMap = null;
		}
	}

	/**
	 * Formats an object to produce a string.
	 * 
	 * @param obj
	 *            Formats an object to produce a string.
	 * @return Formatted string.
	 */
	public String format(Object obj) {
		String fmtVal = null;
		if (fmtValMap != null) {
			fmtVal = fmtValMap.get(obj);
		}
		if (fmtVal == null) {
			fmtVal = getFmtVal(obj);
			if (fmtValMap != null) {
				fmtValMap.put(obj, fmtVal);
			}
		}
		return fmtVal;
	}

	/**
	 * Formats an object to produce a string.
	 * 
	 * @param obj
	 *            Formats an object to produce a string.
	 * @return Formatted string.
	 */
	protected String getFmtVal(Object obj) {
		String fmtVal;
		if (obj instanceof String) {
			fmtVal = getFmtVal((String) obj);
		} else {
			fmtVal = df.format(obj);
		}
		return fmtVal;
	}

	/**
	 * Formats the number text to produce a string.
	 * 
	 * @param s
	 *            the number text.
	 * @return Formatted string.
	 */
	protected abstract String getFmtVal(String s);
}
