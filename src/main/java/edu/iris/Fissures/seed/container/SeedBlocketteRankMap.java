package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;

/**
 * SEED Blockette Ranking Mapper.
 * This class is a collection of static methods for determining the parent-child
 * relationship of blockettes within their own blockette category.  For instance,
 * we can say that Blockette 52 is a child of Blockette 50, and that Blockette 50 will
 * be of higher rank to Blockette 52 in the Station Blockette category.  Blockette 52
 * cannot be compared to another blockette outside of its category, though, such as a
 * Blockette 74.
 * @author Robert Casey, IRIS DMC
 * @version 11/18/2004
 */
public class SeedBlocketteRankMap {

	/**
	 * Return a code number based on the header type of the current blockette object.
	 */
	public static int getHeaderCode (Blockette blockette) throws BuilderException, SeedException {
		return getHeaderCode(blockette.getCategory());  // the string value comes back from BlocketteFactory
	}
	
	
	/**
	 * Return a code number based on the header type of the indicated blockette type
	 */
	public static int getHeaderCode (int blocketteType) throws BuilderException, SeedException {
		return getHeaderCode(BlocketteFactory.getCategory(blocketteType));
	}

	
	/**
	 * Return a code number based on the blockette category string.
	 */
	public static int getHeaderCode (String categoryStr) throws BuilderException {
		if (categoryStr.equals("Volume Index")) return 1;
		if (categoryStr.equals("Abbreviation Dictionary")) return 2;
		if (categoryStr.equals("Station")) return 3;
		if (categoryStr.equals("Time Span")) return 4;
		if (categoryStr.equals("Data Record")) return 5;
		else throw new BuilderException("Unknown blockette category: '" + categoryStr + "'");
	}

	/**
	 * Return the rank value of this blockette within its category.
	 * Rank 0 is the highest, 1 is the next highest, etc...
	 */
	public static int getRank(Blockette blockette) throws BuilderException, SeedException {
		return getRank(blockette.getType());
	}
	
	
	/**
	 * Return the rank value of this blockette type within its category.
	 * Rank 0 is the highest, 1 is the next highest, etc...
	 */
	public static int getRank(int blocketteType) throws BuilderException, SeedException {
		int headerCode = getHeaderCode(blocketteType);
		switch (headerCode) {
			case 1:
				return 0;  // no ranking differentiation among category 1
			case 2:
				return 0;  // no ranking differentiation among category 2
			case 3:
				switch (blocketteType) {
					case 50:
						return 0;
					case 51:
						return 1;
					case 52:
						return 1;
					case 53:
					case 54:
					case 55:
					case 56:
					case 57:
					case 58:
					case 59:
					case 60:
					case 61:
					case 62:
						return 2;
					default:
						throw new BuilderException("Blockette " + blocketteType + " unrecognized");
				}
			case 4:
				return 0;  // no ranking differentiation among category 4
			case 5:
				switch (blocketteType) {
					case 999:
						return 0;
					case 100:
					case 200:
					case 201:
					case 202:
					case 300:
					case 310:
					case 320:
					case 390:
					case 395:
					case 400:
					case 405:
					case 500:
					case 1000:
					case 1001:
						return 1;
					default:
						throw new BuilderException("Blockette " + blocketteType + " unrecognized");
				}
			default:
				throw new BuilderException("Blockette category " + headerCode + " unrecognized");
		}
	}

}
