package edu.iris.Fissures.seed.director;

import edu.iris.Fissures.seed.builder.*;
import edu.iris.Fissures.seed.exception.*;
import edu.iris.Fissures.seed.container.*;
import java.util.*;

/**
 * Concrete class specifically to handle to direct export of SEED data from
 * a SeedObjectContainer and ExportTemplate.  Can connect to any concrete
 * ExportBuilder.
 * <p>
 * Take special note of the object Type referencing used for the ExportTemplate
 * in the methods getContext() and filterBlockette().  This is necessary for the
 * heirarchichal arrangement of station/channel/response blockettes and data blockettes.
 * This is a SEED-specific case that makes special use of the ExportTemplate tabulation
 * process.
 * @author Robert Casey
 * @version 01/14/08
 */

public class SeedExportDirector extends ExportDirector {
	
	// instantiations of abstract methods
	
	/**
	 * Using the provided BuilderFilter, pick out matching blockette objects
	 * to populate the ExportTemplate.  This process must be implemented by the
	 * concrete ExportDirector so as to query for only relevant object types.
	 * if templateFilter is null, then accept all blockettes of the listed types.
	 */
	public void fillTemplate(BuilderFilter templateFilter) throws Exception {
		if (expTemplate == null) {
			throw new BuilderException("template not assigned to director");
		}
		if (container == null) {
			throw new BuilderException("container not assigned to director");
		}
		if (templateFilter != null) {
			if (! templateFilter.getType().equals("SEED")) {  // needs to be a SEED filter
				throw new BuilderException("indicated filter not of type SEED");
			}
		}
		// parent blockette types to extract from container, headered by category number
		// element 0 is the category number, followed by one or more blockette types
		int[][] blkTypeList = {
				{3,50},     // category 3 (station)
				{4,71,72},  // category 4 (time series)
				{5,999}     // category 5 (data)
		};
		Blockette blk = null;
		// foreach blockette category...
		for (int i = 0; i < blkTypeList.length; i++) {
			// if we have a filter file, then iterate over a list that is confined
			// to the listed station and channel values, provided they are
			// offered.  Pull these values directly from the templateFilter.
			//int numberToIterate = 0;
			if (templateFilter != null) {
				// check to see that there are stations and/or channels listed
				Vector stations = templateFilter.getParameter("station");
				Vector channels = templateFilter.getParameter("channel");
				if (stations == null && channels == null) {
					// We have to go through whole list serially
					//numberToIterate = container.iterate(blkTypeList[i][0]);
					container.iterate(blkTypeList[i][0]);
				} else {
					// perform an optimized search for stations and channels
					// SeedObjectContainer-specific
					//System.err.println("DEBUG: perform optimized iteration in category " +
					//	    blkTypeList[i][0]);
					//numberToIterate = ((SeedObjectContainer) container).iterate(stations,
					((SeedObjectContainer) container).iterate(stations,
							channels,blkTypeList[i][0]);
				}
			} else {
				// else iterate through all of the blockettes in the blockette
				// category.
				//System.err.println("DEBUG: iterate through all blockettes in category " +
				//	blkTypeList[i][0]);
				//numberToIterate = container.iterate(blkTypeList[i][0]);
				container.iterate(blkTypeList[i][0]);
			}
			//System.err.println("DEBUG: number of Blockettes to iterate: " + numberToIterate);
			// foreach parent blockette extracted from container...
			while ((blk = (Blockette) container.getNext()) != null) {
				// foreach blockette type num in the category list, look for a match
				//System.err.println("DEBUG: parent to template: " + blk);
				for (int j = 1; j < blkTypeList[i].length; j++) {
					if (blk.getType() == blkTypeList[i][j]) {
						if (filterBlockette(blk,templateFilter,0)) {
							int numChld = blk.numberofChildBlockettes();  // get number of rank 1 children
							//System.err.println("DEBUG: fillTemplate(), blkType=" + blk.getType() +
									//", number of children=" + numChld);
							for (int idx = 0; idx < numChld; idx++) {
								Blockette chld = null;
								SeedObject childObj = blk.getChildBlockette(idx);
								if (childObj == null) continue;
								if (childObj instanceof SeedObjectProxy) {
									chld = (Blockette) container.get(childObj.getLookupId());  // get Blockette from container
								} else {
									chld = (Blockette) childObj;                   // this is the Blockette
								}
								if (filterBlockette(chld,templateFilter,blk.getLookupId())) {
									int numChld2 = chld.numberofChildBlockettes();  // get number of rank 2 children
									for (int idx2 = 0; idx2 < numChld2; idx2++) {
										Blockette chld2 = null;
										childObj = chld.getChildBlockette(idx2);
										if (childObj == null) continue;
										if (childObj instanceof SeedObjectProxy) {
											chld2 = (Blockette) container.get(childObj.getLookupId());  // get Blockette from container
										} else {
											chld2 = (Blockette) childObj;    // this is the Blockette
										}
										filterBlockette(chld2,templateFilter,chld.getLookupId());
									}
								}
							}
							break;  // shortcut exit
						}
					}
				}
			}  // end foreach parent blockette
		}  // end foreach category
	}
	
	/**
	 * Signal the director that we are starting a new volume.
	 * Perform necessary initializations.
	 */
	protected void startVolume() {
		cacheLookup = new Vector(8,8);
		cacheObject = new Vector(8,8);
		dictionarySequence = new HashMap();
		dictionaryMap = new HashMap();
		parent50 = 0;
		parent52 = 0;
		parent999 = 0;
	}
	
	/**
	 * Return a 'flat' copy of the Blockette object associated with the refNum ID.
	 * If disk persistent caching (serialization) is not being used, Objects pulled from the Object
	 * Container are copied, dictionary indices are resequenced and the resultant
	 * Blockette is saved to a temporary cache before it is returned.
	 * In the case of disk persistent caching, copying of the blockette is not necessary.
	 * 
	 * The blockette objects returned are considered 'flat' because they may no
	 * longer retain their original child blockette tree links or dictionary
	 * mapping references, depending on the caching mode of the attached container.
	 * These 'flat' blockettes are appropriate for passing
	 * to the assigned export builder since only the field values will
	 * need to be accessed by the Export Builder.
	 */	
	protected Object getObject(int refNum) throws Exception {
		// check locally to see whether this refNum is in our cache
		Object nextObject = getFromCache(refNum);
		if (nextObject == null) {  // else, ask container for this object
			nextObject = container.get(refNum);
			if (nextObject != null) {
				cacheLookup = new Vector(8,8);   // reinitialize the cache
				cacheObject = new Vector(8,8);
				// cache the lookup ID
				cacheLookup.add(new Integer(refNum));
				// create a new 'flat' copy of the Blockette which we will then resequence
				Blockette newBlockette = null;
				newBlockette = BlocketteFactory.createBlockette( ((Blockette) nextObject).toString() );
				// have the lookup Id be identical to the original blockette
				newBlockette.setLookupId(refNum);
				// repopulate the lookup Id translation table in the new blockette
				int numLookups = ((Blockette) nextObject).numberofDictionaryLookups();
				for (int i = 0; i < numLookups; i++) {
					int lookupId = ((Blockette) nextObject).getDictionaryLookup(i+1); // indices start at 1
					newBlockette.addDictionaryLookup(lookupId); // returned index value should match i+1
				}
				// resequence dictionary lookups
				resequence(newBlockette);
				// cache the blockette object
				cacheObject.add(newBlockette);
				// then recurse through each of the child objects in the original Blockette and cache those too
				cacheChildren ((Blockette) nextObject);
				//	
				// special case for FSDH type 999 blockettes, copy and atttach the waveform data
				if (newBlockette.getType() == 999) {
					// source waveform object
					Waveform waveform = ((Blockette) nextObject).getWaveform();
					//
					// if waveform comes back null, then the resultant blockette
					// will also have a null waveform reference
					if (waveform == null) {
						// DEBUG
						System.err.println("Warning: waveform == null in Director");
					} else {
						// copy the encoded bytes to a new array
						byte[] wavebytes = waveform.getEncodedBytes();
						byte[] newWavebytes = new byte[wavebytes.length];
						System.arraycopy(wavebytes,0,newWavebytes,0,wavebytes.length);
						
						// attach waveform copy to FSDH Blockette
						newBlockette.attachWaveform(new Waveform(newWavebytes,0,waveform.getNumSamples(),
								waveform.getEncoding(),waveform.getSwapBytes()));
						//System.err.println("DEBUG: attached non-null waveform to newBlockette: " +
						//        wavebytes.length + "bytes");
					}
				}
				// finally, reassign our handle to the new Blockette
				nextObject = newBlockette;
			} else {
				// else if null, then report an exception, since all refNums should come
				// back with a corresponding object.
				throw new BuilderException("ERROR: unable to locate object for reference number " + refNum);
			}
		}
		// check to see if the result is a context-oriented parent blockette (see getContext())
		// if so, store into variable so we remember this is the latest parent
		// of that type accessed.
		int blkType = ((Blockette)nextObject).getType();
		if (blkType == 50) parent50 = ((Blockette) nextObject).getLookupId();
		if (blkType == 52) parent52 = ((Blockette) nextObject).getLookupId();
		if (blkType == 999) parent999 = ((Blockette) nextObject).getLookupId();
		//System.err.println("DEBUG: nextObject lookupId: " + ((Blockette) nextObject).getLookupId());
		return nextObject;
	}
	
	
	/**
	 * Get a context ID value.
	 * Track parent-child context and possibly modify the object type number to include
	 * parent reference.  Helps with templates that index heirarchy relationships.
	 * Other blockettes that do not meet the criteria for context modification
	 * (like parent blockettes) will simply have the identical value returned as objType.
	 */
	protected long getContext(int objType) throws BuilderException {
		int parentId = 0;  // this will point to the parent blockette's lookup ID value
		// the parentXX variables get set and updated by the getObject() method,
		// reflecting the latest parent object accessed for each XX type.
		if (objType == 51 || objType == 52) parentId = parent50;  // station comment and channel
		else if (objType > 52 && objType < 70) parentId = parent52;    // channel comment and response
		else if (objType > 99 && objType != 999) parentId = parent999; // data record blockette
		// generate a context ID value and return it
		return generateContext(objType, parentId);
	}
	
	
	// private methods
	
	/**
	 * Check the local cache for an object handle corresponding to the
	 * offered refNum.
	 */
	private Object getFromCache(int refNum) {
		int vectorSize = cacheLookup.size();
		for (int i = 0; i < vectorSize; i++) {
			int lookupId = ((Integer) cacheLookup.get(i)).intValue();  // get next lookup Id
			if (lookupId == refNum) {  // if the desired refNum equals the lookup Id...
				return cacheObject.get(i);  // return the corresponding Blockette object
			}
		}
		return null;   // if no match was found, then return null
	}
	
	/**
	 * Recursively cache all of the children of this blockette,
	 * and the children's children.
	 */
	private void cacheChildren(Blockette blk) throws BuilderException, SeedException, ContainerException {
		int numChildren = blk.numberofChildBlockettes();
		for (int i = 0; i < numChildren; i++) {
			Blockette childBlk = null;
			SeedObject childObj = blk.getChildBlockette(i);  // get next child
			//System.err.println("DEBUG: cacheChildren get next childObj = " + childObj);
			if (childObj == null) continue;
			if (childObj instanceof SeedObjectProxy) {
				childBlk = (Blockette) container.get(childObj.getLookupId());
			} else {
				childBlk = (Blockette) childObj;
			}
			if (childBlk != null) {
				int refNum = childBlk.getLookupId();  // get the lookup ID of that child
				//System.err.println("DEBUG: caching child: " + refNum + ", type: " + childBlk.getType());
				// cache lookup ID
				cacheLookup.add(new Integer(refNum));
				// create a new copy of the Blockette which we will resequence
				Blockette newBlockette = null;
				newBlockette = BlocketteFactory.createBlockette( childBlk.toString() );
				// have the lookup Id be identical to the original blockette
				newBlockette.setLookupId(refNum);
				// repopulate the lookup Id translation table in the new blockette
				int numLookups = childBlk.numberofDictionaryLookups();
				for (int j = 0; j < numLookups; j++) {
					int lookupId = childBlk.getDictionaryLookup(j+1); // indices start at 1
					newBlockette.addDictionaryLookup(lookupId); // returned index value should match j+1
				}
				// resequence dictionary lookups
				resequence(newBlockette);
				// cache the blockette object
				cacheObject.add(newBlockette);
				// then recursively cache this child's children, using the original object
				cacheChildren(childBlk);
			}
		}
	}
	
	
	/**
	 * Change dictionary references in the Blockette contents to a new index
	 * sequence for the current output volume.
	 */
	private void resequence(Blockette blk) throws BuilderException, SeedException {
		if (dictionarySequence == null) {
			throw new BuilderException("dictionarySequence map not initialized");
		}
		if (dictionaryMap == null) {
			throw new BuilderException("dictionaryMap not initialized");
		}
		
		// PART ONE: alter dictionary referencing (source) fields
		int blkType = blk.getType();  // get our current blockette's type
		//System.err.println("DEBUG: PART ONE - blkType=" + blkType);
		int[] srcFld = SeedDictionaryReferenceMap.lookupSourceFld(blkType);  // check for source fields
		for (int i = 0; srcFld != null && i < srcFld.length; i++) {          // for each source (referencing) field...
			// String-tokenize field value in case it's a type 'L' field (List)
			StringTokenizer strTok = new StringTokenizer(blk.toString(srcFld[i]),"[], "); // list comes back as a Vector.toString()
			int listIndex = 0;  // index value for tracking multiple values in a field
			while (strTok.hasMoreTokens()) {   // an 'L' (List) type field will potentially have multiple value entries (think blk 60)
				String strFieldVal = strTok.nextToken();     // get next string token
				int refVal = Integer.parseInt(strFieldVal);  // get integer value of string token
				if (refVal > 0) {  // if field index value is greater than zero...
					Integer seqNum = null;  // the sequence number is the lookup value we will write to the ref field
					int refId = blk.getDictionaryLookup(refVal);  // get lookup id based on field value   // SOURCE OF BUG?  // CACHING?
					if (refId == 0) {
						// lookup references nothing? just warn user and put a zero in that field
						System.err.println("Warning: lookupId value = 0 for blkType = " +
							blkType + ", field = " + srcFld[i] + ", ref value = " + refVal);
						seqNum = new Integer(0);  // set sequence number to 0
					} else {
						seqNum = (Integer) dictionaryMap.get(new Integer(refId));  // placeholder for incremented index value
					}
					//System.err.println("DEBUG: blkType = " + blkType + ", field = " + srcFld[i] + ", refId = " + refId);
					//if (seqNum != null) System.err.println("DEBUG: --- seqNum = " + seqNum);
					if (seqNum == null) {  // if lookup Id not index mapped...
						int[] destBlk = SeedDictionaryReferenceMap.lookupDestBlk(blkType, srcFld[i]);  // get destination blockette types
						for (int j = 0; destBlk != null && j < destBlk.length; j++) {    // for each destination (referenced) blockette type
							// get the next (incremented) index for that blockette type
							seqNum = (Integer) dictionarySequence.get(new Integer(destBlk[j]));  // get current index
							if (seqNum == null) {                            // if no index...
								seqNum = new Integer(1);                     // initialize new sequence number with 1
							} else {
								seqNum = new Integer(seqNum.intValue() + 1); // else increment sequence number by 1
							}
							//System.err.println("DEBUG: --- new seqNum = " + seqNum + " to blkType = " + destBlk[j] + " and refId = " + refId);
							dictionarySequence.put(new Integer(destBlk[j]), seqNum);  // map the sequence number to the destination blockette type
						}
						dictionaryMap.put(new Integer(refId), seqNum);  // map the sequence number to the lookupId
					}  // end lookup Id not index mapped
					//System.err.println("DEBUG: blk " + blk.getLookupId() + " setFieldVal(" + srcFld[i] + ", " + listIndex + ") to value " + seqNum);
					blk.setFieldVal(srcFld[i], listIndex, seqNum);  // alter source field to index value
					listIndex++;  // increment listIndex
				} // end field index > 0
			}  // end while each field value token
		}  // end for each source field
		
		// PART TWO: alter dictionary referenced (destination) fields
		// (exclusive to dictionary blockettes)
		//System.err.println("DEBUG: PART TWO - blkType=" + blkType);
		int destFld = SeedDictionaryReferenceMap.lookupDestFld(blkType);  // get destination field
		if (destFld > 0) {  // if this blockette is a referenced dictionary blockette...
			int lookupId = blk.getLookupId();   // get the blockette's lookup Id
			Integer seqNum = (Integer) dictionaryMap.get(new Integer(lookupId));  // check to see if this lookupId has a sequence number
			//System.err.println("DEBUG: blkType = " + blkType + ", field = " + destFld + ", refId = " + lookupId);
			//if (seqNum != null) System.err.println("DEBUG: --- seqNum = " + seqNum);
			if (seqNum == null) {  // if lookup Id not index mapped...
				// get the next (incremented) index for that blockette type
				seqNum = (Integer) dictionarySequence.get(new Integer(blkType));  // get highest index value for this blockette type
				if (seqNum == null) {                            // if no index...
					seqNum = new Integer(1);                     // initialize new sequence number with 1
				} else {
					seqNum = new Integer(seqNum.intValue() + 1); // else increment sequence number by 1
				}
				//System.err.println("DEBUG: --- new seqNum = " + seqNum + " to blkType = " + blkType + " and refId = " + lookupId);
				dictionarySequence.put(new Integer(blkType), seqNum);  // map the incremented number to the destination blockette type
				dictionaryMap.put(new Integer(lookupId), seqNum);      // map the sequence number to the lookupId
			}  // end lookup Id not index mapped
			blk.setFieldVal(destFld,seqNum);  // alter destination field to index value
			//System.err.println("DEBUG: blk " + lookupId + " setFieldVal(" + destFld + ") to value " + seqNum);
		}  // end if dictionary blockette
		
	}
	
	
	/**
	 * Pass blockette through a Template Filter.
	 * Pass the provided blockette through a qualification check and add it to the template
	 * lookup list if it passes.  Return true if the blockette was added to the template. 
	 * Optional <b>parentId</b>, if non-zero, will be appended (at least in part)
	 * to the objType value written to the template, which is intended to
	 * foster better heirarchy coupling of blockettes in the station-channel-response arena.
	 */
	private boolean filterBlockette(Blockette blk, BuilderFilter templateFilter, int parentId)
	throws BuilderException, ContainerException {
		if (templateFilter == null || templateFilter.qualify(blk)) {
			// this is a matching blockette, write reference to the template
			//
			int blkType = blk.getType();  // get blockette type
                        long refId = 0L;  // this will get populated with a reference ID
			//System.err.println("DEBUG: filterBlockette called for type " + blkType);
			// special case for blockette 50 -- if there are no child blockettes attached to this
			// blockette object AND there is a templateFilter, then all of the channels must
			// have been filtered out.  It is sensible, then, to reject this parent blockette
			// from being exported.
			if (blkType == 50 && templateFilter != null) {
				if (blk.numberofChildBlockettes() == 0) {
					//System.err.println("DEBUG:		...filtered out...");
					return false;  // filter this station out
				}
			}
                        //
                        // check for a stage number -- this will be greater than -1 if a response blockette
                        int thisStageNumber = -1;
                        try {
                            thisStageNumber = blk.getStageNumber();
                        } catch (SeedException e) {
                            throw new ContainerException("Error getting stage number from blkType " + blkType + ",  Exception: " + e);
                        }
                        // generate an object ID, which identifies which 'bin' to put the
			// blockette's lookup ID into.  Normally, this is the blockette type,
			// so that we group lookup ID's by their blockette type.  However, in
			// some cases, we need to establish a parent-child relationship, so a
			// more unique coding must be done to group lookupIds with a specific
			// parent.  This is known as a 'context ID', which is generated by
			// generateContext().
			refId = generateContext(blkType,parentId);
                        // add the blockettes lookup id to the template, mapping to the object Id.
                        // This may stack in a vector with a key of refId.
                        expTemplate.addRefNum(refId,blk.getLookupId());
			//
			// for response blockettes...
			// we need to fill in zeroes around the current blockette to create 'holes'
			// in the linear list...helps to cement the order
			if (thisStageNumber > -1) {  // if it has a (non-negative) stage number, then it is a response blockette
                                // loop through sequence of response blockettes.
                                // pad a zero to all blockettes but the current one
                                for (int responseBlkIdx = 0; responseBlkIdx < responseBlkSize; responseBlkIdx++) {
                                    if (responseBlockettes[responseBlkIdx] != blkType) {
                                        // insert a zero for this blockette type
					refId = generateContext(responseBlockettes[responseBlkIdx],parentId);
					expTemplate.addRefNum(refId,0);  // padding a zero in lieu of this blockette type
                                    }
                                }
			}
                        //
			// get dictionary reference numbers from this blockette and add to template
			// the dictionary blockettes will not append parentId to the objType value
			// since they are independent of heirarchy
			int numLookups = blk.numberofDictionaryLookups();
			for (int k = 0; k < numLookups; k++) {
				int dictLookupId = blk.getDictionaryLookup(k+1);  // indexing always starts at 1
				Blockette dictBlk;
				if ((dictBlk = (Blockette) container.get(dictLookupId)) != null) {
					refId = generateContext(dictBlk.getType(),0);  // context with no parent
					expTemplate.addRefNum(refId,dictLookupId); // dictionary ref num
				}
			}
			return true;
		}
		return false;
	}
	
	
	/**
	 * Generate context ID value from object type and parent ID.
	 * The Export Template stores each Blockette ID in a series vector with a
	 * long value as a key.  This long value can either be the blockette type,
	 * or in the case of a child blockette, a much larger 'context' reference
	 * value which consists of a blockette type number appended to
	 * the parent's Blockette ID.  This context reference
	 * will be a unique number that associates a parent ID with a sequence of
	 * child Blockettes of a certain type (like a series of type 53's).
	 * <p>
	 * The reason for using a long value is that the number space required to
	 * represent the full parent lookup ID plus the blockette type is too
	 * large to be accomodated by an integer.  The pattern is:
	 * <p>
	 * B B B B V V V T N N N N N N
	 * <p>
	 * which is 4 digits for the blockette number followed by the full parent
	 * lookup ID.  The long value has a lot of room to expand for other kinds
	 * of mappings (max value = 9 223 372 036 854 775 807).
	 */
	public long generateContext(int childObjType, int parentId) throws BuilderException {
		try {
		if (parentId == 0) {  // if there is no parent
			// return a long version of the child object type
			return childObjType;
		} else {
			// there is a parent, combine values...
			// first, apply the child object's type number
			long contextRef = childObjType * 10000000000L;
			//System.err.println("DEBUG: contextRef A: " + contextRef);
			// second, add the parent's lookupId
			contextRef += parentId;
			//System.err.println("DEBUG: contextRef B: " + contextRef);
			return contextRef;  // return the resulting value
		}
		} catch (Exception e) {
			throw new BuilderException("(generateContext): " + e);
		}
	}
	
	
	// instance variables
	
	// caching is most important for Blockettes for child Blockette visibility
	private Vector cacheLookup = null;         // cache vector holding reference lookup numbers
	private Vector cacheObject = null;         // cache vector holding blockette object references
	// resequencing of reference numbers is necessary for dictionary references during export
	private HashMap dictionarySequence = null;  // for each blockette type, track sequential reference number assignments
	private HashMap dictionaryMap = null;  // for each lookup ID, associate with an assigned reference number for output
	// list of response blockette types in expected order -- 2nd rank child blockettes
	private static final int[] responseBlockettes = {60,53,54,61,62,55,56,57,58};
	private static final int responseBlkSize = responseBlockettes.length;
	// list of data blockette types -- 1st rank child blockettes
	//private static final int[] dataBlockettes = {100,200,201,202,300,310,320,390,395,400,405,500,1000,1001};
	//private static final int dataBlkSize = dataBlockettes.length;
	// parent context IDs
	private int parent50 = 0;
	private int parent52 = 0;
	private int parent999 = 0;
	
}
