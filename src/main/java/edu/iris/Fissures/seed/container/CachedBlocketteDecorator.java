package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;

import java.util.*;

/**
 * Decorator class for Blockette objects. Cooperates with an ObjectContainer to
 * manage Blockettes through a persistence cache. Serves as an indirect
 * interface to the assigned Blockette. Since this must be serializable, we
 * cannot keep a persistent link to the ObjectContainer. Instead, we track
 * container association by volume number, which is mapped by the
 * BlocketteDecoratorFactory. The BlocketteDecoratorFactory is authorized to
 * create new instances of this class.
 * 
 * @author Robert Casey, IRIS DMC
 * @version 4/28/2005
 */
public class CachedBlocketteDecorator extends Blockette implements BlocketteDecorator {

	///////////////////////
	// Constructors
	////////////////////////

	/**
	 * Constructor for the Blockette Decorator.
	 * 
	 * @param blk
	 *            Blockette to represent
	 * @param persistenceCached
	 *            true if we are using caching to disk to conserve memory
	 * @throws ContainerException
	 *             if cont is null
	 * @throws SeedException
	 *             if blk is null
	 */
	CachedBlocketteDecorator(Blockette blk) throws ContainerException, SeedException {
		super(blk.getType()); // this decorator must always retain the same type
		// System.err.println("DEBUG: making new CachedBlocketteDecorator");
		// set decorator-specific variables
		assignBlockette(blk);
	}

	///////////////////////////
	// decorator-specific methods
	///////////////////////////

	/**
	 * Assign the indicated blockette to this decorator. Typically just done at
	 * initialization.
	 * 
	 * @param blk
	 *            Blockette to represent
	 */
	public void assignBlockette(Blockette blk) throws SeedException {
		if (blk == null)
			throw new SeedException("Assigned blockette is null");
		// assigned blockette must always be of the same type as the decorator
		// once non-zero
		if (getType() != 0 && blk.getType() != getType())
			throw new SeedException("Assigned blockette type=" + blk.getType() + ", decorator type=" + getType());
		// otherwise, assert the assigned blockette type
		setType(blk.getType());
		sourceBlockette = blk; // link to the blockette when not caching
		// set the lookupId of this decorator to be the same as the attached
		// blockette
		setLookupId(blk.getLookupId());
		volumeNumber = -1; // gets determined later
		// let's know whether this blockette has a parent
		hasParent = blk.hasParent();
	}

	/**
	 * Get the blockette object assigned to this decorator...either attached
	 * directly to this object or extracted from the supporting
	 * SeedObjectContainer
	 * 
	 * @return Blockette object
	 */
	public Blockette getBlockette() {
		Blockette blk = null;
		SeedObjectContainer soc = getContainer();

		blk = sourceBlockette;
		// System.err.println("DEBUG: getBlockette() " + blk);
		if (blk == null) {
			// we should not get a null here...print a message
			System.err.println("CachedBlocketteDecorator: referenced Blockette " + getLookupId() + " is null");
		}
		return blk;
	}

	/**
	 * Get the SeedObjectContainer assigned to this decorator. This can not be
	 * saved because of serialization requirements.
	 * 
	 * @return the SeedObjectContainer
	 */
	public SeedObjectContainer getContainer() {
		return (SeedObjectContainer) BlocketteDecoratorFactory.getContainerByVol(getVolumeNumber());
	}

	////////////////////////////
	// Blockette overlay methods
	////////////////////////////

	public void initialize(byte[] blocketteStream, boolean swapFlag, boolean isData, float version)
			throws SeedException {
		if (sourceBlockette != null)
			getBlockette().initialize(blocketteStream, swapFlag, isData, version);
		instanceInit();
		setVersion(version);
	}

	public void initialize(byte[] blocketteStream, boolean swapFlag, boolean isData) throws SeedException {
		if (sourceBlockette != null)
			getBlockette().initialize(blocketteStream, swapFlag, isData);
		instanceInit();
	}

	public void initialize(String inputString, String delimiter, String blank, float version) throws SeedException {
		if (sourceBlockette != null)
			getBlockette().initialize(inputString, delimiter, blank, version);
		instanceInit();
		setVersion(version);
	}

	public void initialize(String inputString, String delimiter, String blank) throws SeedException {
		if (sourceBlockette != null)
			getBlockette().initialize(inputString, delimiter, blank);
		instanceInit();
	}

	public void initialize(String inputString) throws SeedException {
		if (sourceBlockette != null)
			getBlockette().initialize(inputString);
		instanceInit();
	}

	public void initialize(String inputString, float version) throws SeedException {
		if (sourceBlockette != null)
			getBlockette().initialize(inputString, version);
		instanceInit();
		setVersion(version);
	}

	public void setValuesFrom(String inputString, String delimiter, String blank) throws SeedException {
		getBlockette().setValuesFrom(inputString, delimiter, blank);
	}

	public void setValuesFrom(String inputString) throws SeedException {
		getBlockette().setValuesFrom(inputString);
	}

	public boolean isIncomplete() {
		return getBlockette().isIncomplete();
	}

	public int getNumBytes() throws SeedException {
		return getBlockette().getNumBytes();
	}

	public String toString(int fieldNum, int fieldIndex) {
		return getBlockette().toString(fieldNum, fieldIndex);
	}

	public String toString(int fieldNum) {
		return getBlockette().toString(fieldNum);
	}

	public String toString(String delimiter, String blank) {
		return getBlockette().toString(delimiter, blank);
	}

	public String toString() {
		return getBlockette().toString();
	}

	public String getName() throws SeedException {
		return BlocketteFactory.getName(getType());
	}

	public String getCategory() throws SeedException {
		return BlocketteFactory.getCategory(getType());
	}

	public int getNumFields() throws SeedException {
		return getBlockette().getNumFields(); // based on the SEED version of
												// this Blockette instance
	}

	public String getFieldName(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldName(getType(), fieldNum);
	}

	public String getFieldType(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldType(getType(), fieldNum);
	}

	public String getFieldLength(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldLength(getType(), fieldNum);
	}

	public String getFieldMask(int fieldNum) throws SeedException {
		return BlocketteFactory.getFieldMask(getType(), fieldNum);
	}

	public String getDefinition() throws SeedException {
		return BlocketteFactory.getBlocketteDefinition(getType());
	}

	public Object getFieldVal(int fieldNum, int fieldIndex) throws SeedException {
		return getBlockette().getFieldVal(fieldNum, fieldIndex);
	}

	public Object getFieldVal(int fieldNum) throws SeedException {
		return getBlockette().getFieldVal(fieldNum);
	}

	public Vector getFieldGrp(int fieldNum, int fieldIndex) throws SeedException {
		return getBlockette().getFieldGrp(fieldNum, fieldIndex);
	}

	public Object getFieldObject(int fieldNum) {
		return getBlockette().getFieldObject(fieldNum);
	}

	public String translate(int fieldNum) throws SeedException {
		return getBlockette().translate(fieldNum);
	}

	public Blockette getChildBlockette(int index) {
		// System.err.println("DEBUG: getChildBlockette( " + index + " )");
		return getBlockette().getChildBlockette(index);
	}

	public int numberofChildBlockettes() {
		return getBlockette().numberofChildBlockettes();
	}

	public boolean hasParent() {
		return hasParent; // Decorator shortcut
	}

	public Blockette getParentBlockette() {
		return getBlockette().getParentBlockette();
	}

	public Waveform getWaveform() {
		return getBlockette().getWaveform();
	}

	public int getDictionaryLookup(int index) {
		return getBlockette().getDictionaryLookup(index);
	}

	public int numberofDictionaryLookups() {
		return getBlockette().numberofDictionaryLookups();
	}

	public void setFieldVal(int fieldNum, int fieldIndex, Object value, boolean lenient) throws SeedException {
		getBlockette().setFieldVal(fieldNum, fieldIndex, value, lenient);
	}

	public void setFieldVal(int fieldNum, int fieldIndex, Object value) throws SeedException {
		getBlockette().setFieldVal(fieldNum, fieldIndex, value);
	}

	public void setFieldVal(int fieldNum, Object value) throws SeedException {
		getBlockette().setFieldVal(fieldNum, value);
	}

	public void setFieldObject(int fieldNum, Object fieldObj) {
		getBlockette().setFieldObject(fieldNum, fieldObj);
	}

	public void addFieldGrp(int fieldNum, Vector valueVec) throws SeedException {
		getBlockette().addFieldGrp(fieldNum, valueVec);
	}

	public void insertFieldGrp(int fieldNum, int fieldIndex, Vector valueVec) throws SeedException {
		getBlockette().insertFieldGrp(fieldNum, fieldIndex, valueVec);
	}

	public void deleteFieldGrp(int fieldNum, int fieldIndex) throws SeedException {
		getBlockette().deleteFieldGrp(fieldNum, fieldIndex);
	}

	public int purgeFieldGrp(int fieldNum) throws SeedException {
		return getBlockette().purgeFieldGrp(fieldNum);
	}

	public int addChildBlockette(Blockette addBlockette) {
		return getBlockette().addChildBlockette(addBlockette);
	}

	public void removeChildBlockette(int index) {
		getBlockette().removeChildBlockette(index);
	}

	public void replaceChildBlockette(int index, Blockette newBlockette) {
		getBlockette().replaceChildBlockette(index, newBlockette);
	}

	public void attachParent(Blockette parent) {
		getBlockette().attachParent(parent);
		hasParent = true; // decorator shortcut
	}

	public void removeParent() {
		getBlockette().removeParent();
		hasParent = false; // decorator shortcut
	}

	public void attachWaveform(Waveform data) {
		getBlockette().attachWaveform(data);
	}

	public void removeWaveform() {
		getBlockette().removeWaveform();
	}

	public int addDictionaryLookupIfNeeded(int abbrevLookupId) {
		return getBlockette().addDictionaryLookupIfNeeded(abbrevLookupId);
	}

	public int addDictionaryLookup(int abbrevLookupId) {
		return getBlockette().addDictionaryLookup(abbrevLookupId);
	}

	public void setDictionaryLookup(int index, int abbrevLookupId) {
		getBlockette().setDictionaryLookup(index, abbrevLookupId);
	}

	// public Btime getMTime () {
	// return getBlockette().getMTime();
	// }

	//////////////////////////
	// Private methods
	//////////////////////////

	private Blockette getFromCache(SeedObjectContainer soc) {
		if (soc == null) {
			System.err.println(
					"WARNING: Blockette Decorator attempting to access null container reference -- forced to return null Blockette");
			return null;
		}
		Blockette getBlk = null;
		// System.err.println("DEBUG: getLookupId for lookup=" + getLookupId());
		getBlk = (Blockette) soc.lookup(getLookupId()); // get the non-decorated
														// blockette
		return getBlk;
	}

	private int getVolumeNumber() {
		if (volumeNumber < 0)
			volumeNumber = SeedObjectContainer.getVolumeNumberFromId(getLookupId());
		return volumeNumber;
	}

	// for other instance variables...refer to Blockette.java
	// private boolean isCached = false; // flag if this blockette has been
	// cached to queue memory from disk
	private Blockette sourceBlockette = null;
	private boolean hasParent = false;
	private int volumeNumber = -1;

}
