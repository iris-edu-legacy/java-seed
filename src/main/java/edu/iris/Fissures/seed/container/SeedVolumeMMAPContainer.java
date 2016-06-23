package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.io.*;
//import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.*;

/**
 * Container for maintaining ID annotated edit logs using memory map indices
 * for retrieval.  We make this container an Observable one, so that outside
 * observers, such as the view layer, can know that something has changed
 * with the container.
 * @author Robert Casey, IRIS DMC
 * @version 9/15/2015
 */
public class SeedVolumeMMAPContainer extends Observable
        implements MMAPContainer<SeedObjectTag,String>, ObjectContainer {


    // CONSTRUCTORS

    
    public SeedVolumeMMAPContainer() {
        // create a new red-black sorted tree map, using tag-specific comparator.
        journalMap = new TreeMap<SeedObjectTag,MappedByteBuffer>(new SeedObjectTagComparator());
        // create a hash map to track the active state of mapped tags.
        activeStateMap = new HashMap<String,Boolean>();
        // track lookup ID transitions
        lookupIDTransfer = new HashMap<Integer,HashMap<Integer,Integer>>();
    }

    public SeedVolumeMMAPContainer(String pathName) throws FileNotFoundException, IOException, ContainerException {
        this(new File(pathName));
    }

    public SeedVolumeMMAPContainer(String baseDir, String name) throws FileNotFoundException, IOException, ContainerException {
        this(new File(baseDir,name));
    }
    
    public SeedVolumeMMAPContainer(File filePath) throws FileNotFoundException, IOException, ContainerException {
        this();
        // open the journal mapping file
        journalFilePath = filePath;  // save a persistent reference
        		// **until we are ready to use the journal file as a backing store, let's rename the file to a .old pathname
        		//deactivated        if (journalFilePath.exists()) journalFilePath.renameTo(new File(filePath + ".old"));
        openJournal(journalFilePath);
        		// **deactivated until we can process this reliably // acquire(0);  // get all contents in current journal
    }


    // PUSH DIRECTIVES TO THE JOURNAL


    // this is the primary means of appending new entries to the container.
    // the blockette entry will be written directly to the journal and the log
    // consumer thread queue.  completely asynchronous.
    //
    // make use of setUpdateLookup() boolean toggle when add()'ing copies of blockettes
    // from the same container.  This avoids the danger of double-remapping of lookups
    // since abbreviations always have their lookup IDs remapped.
    public void addData(String blocketteStr) throws ContainerException, IOException {
        MMAPAtom matom = add(blocketteStr);  // first, add the data to the journal
        // push the journal mapped contents to the log consumer thread
        pushToConsumer(matom);      // push journal entry to consumer thread
    }

    // for more synchronous, single blockette additions, use this method instead of
    // addData.  This will trigger an Observer notification after the add goes through.
    public void addDataAndNotify(String blocketteStr) throws ContainerException, IOException {
        addData(blocketteStr);  // add the data
        consumerCloseAndWait(); // wait until the consumer thread has processed, then notify observers
    }


    // synchronously add a new abbreviation blockette and then apply its lookup ID value to the
    // current context blockette in the specified lookup field.
    public void addAbbrevToContext(int lookupField, String abbrevBlocketteStr)
            throws SeedException, ContainerException, IOException {
        
        // synchronous wait for queue to finish, prior to context update
        journalConsumer.closeQueue();
        synchronized (journalConsumer.mmapQueue) {
            while (! journalConsumer.isOpen()) {   // queue will reopen when consumer has emptied it
                try {
                    journalConsumer.mmapQueue.wait();  // wait for open notify from consumer thread
                } catch (InterruptedException e) {
                    throw new ContainerException ("ERROR: InterruptedException: " + e);
                }
            }
        }
        // get context blockette
        SeedObjectTag context = getContext();
        String contextBlkStr = get(get(context));
        Blockette contextBlk = BlocketteFactory.createBlockette(contextBlkStr);

        // add our new abbreviation independent of the consumer thread
        MMAPAtom matom = add(abbrevBlocketteStr);
        decodeMessage(matom);  // add message gets recorded synchronously (bypass consumer thread)

        // update the context blockette with the lookup ID assigned
        // latestLookupID contains lookup value from decodeMessage() stack
        contextBlk.setFieldVal(lookupField, latestLookupID);

        // reassert context, just to be safe for caller
        setContext(context);

        // async update of context blockette
        update(context,contextBlk.toString());
        
        
        
    }


    // supporting private operation that takes care of memory mapping new adds, writes to journal
    // return memory map atom structure to indicate the mmap details.
    private MMAPAtom add(String blocketteStr) throws ContainerException, IOException {
        if (journalChannel == null)
            throw new ContainerException("ERROR: no journal file is open");
        if (blocketteStr == null ||
                blocketteStr.length() == 0) {
            throw new ContainerException("ERROR: empty blockette passed to add method");
        }
        // add the blockette entry to the journal file queue
        try {
            while (fileLock == true) Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new IOException("ERROR: file lock acquisition interrupted: " + e);
        }
        fileLock = true;
        journalFile.seek(journalFile.length());  // position to EOF
        String output = blocketteStr;
        MMAPAtom matom = new MMAPAtom();
        matom.msg = output;
        matom.pos = journalChannel.position();
        matom.length = blocketteStr.length();
        journalFile.writeBytes(output + "\n");  // write the string contents plus CR
        fileLock = false;
        return matom;
    }


    // interface-compliant variant of update()
    public void update(SeedObjectTag tag, String blocketteStr) throws ContainerException, IOException {
        update(tag,blocketteStr,false);
    }

    // this is the critical data mutability function.
    // update the current tag reference with the new blockette string entry.
    // tag: same, data: changed
    // write a change directive to the journal and the log consumer thread queue
    // if the reinstate flag is true, then we will reactivate an inactive tag
    public void update(SeedObjectTag tag, String blocketteStr, boolean reinstate) throws ContainerException, IOException {
        if (tag==null) 
            throw new ContainerException("ERROR: update operation passed a null tag.");
        if (journalChannel == null)
            throw new ContainerException("ERROR: no journal file is open");
        if (! isActive(tag) && ! reinstate)
            throw new ContainerException("ERROR: cannot update an inactive tag");
        // check to see that we have an entry for this tag
        if (! containsKey(tag)) {
            // if the tag is not present, then perform an 'upsert'
            addData(blocketteStr);
        } else {
            // append a 'change' entry to the journal file queue
            try {
                while (fileLock == true) Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new IOException("ERROR: file lock acquisition interrupted: " + e);
            }
            fileLock = true;
            journalFile.seek(journalFile.length());  // position to EOF
            String output = "::c::" + tag + "::" + blocketteStr;
            MMAPAtom matom = new MMAPAtom();
            matom.msg = output;
            matom.pos = journalChannel.position();
            matom.length = output.length();
            journalFile.writeBytes(output + "\n");  // write output plus CR
            fileLock = false;
            // push the journal mapped contents to the log consumer thread
            pushToConsumer(matom);      // push journal entry to consumer thread
        }
    }

    // for more synchronous, single blockette updates, use this method instead of
    // update.  This will trigger an Observer notification after the update goes through.
    public void updateAndNotify(SeedObjectTag tag, String blocketteStr, boolean reinstate)
            throws ContainerException, IOException {
        update(tag,blocketteStr,reinstate);
        consumerCloseAndWait(); // wait until the consumer thread has processed, then notify observers
    }

    // Update the most recent entry, which should resolve to the same tag, with this updated
    // blockette string.  The replacement will be dependent on the tag resolving to the
    // same value.
    // Flag true for updateLookup if the blocketteStr still needs its lookup IDs remapped.
    public void updateLatest(String blocketteStr, Boolean updateLookup) throws ContainerException, IOException, SeedException {
        consumerCloseAndWait();  // wait for consumer thread to halt for current tag
        //SeedObjectTag tag = new SeedObjectTag(null,blocketteStr);
        SeedObjectTag tag = getContext();  // get the most recent blockette tag to update it
        update(tag,blocketteStr,false);  // update the most recent object with a new string
        if (updateLookup) {  // if we need up remap the lookup IDs
            consumerCloseAndWait();  // wait again for this change to go through
            MappedByteBuffer mbb = get(tag);  // get our buffered string again
            updateForLookup(previousTag,mbb); // take care of the remapping
        }
    }

    // this is the primary remote stream source append function.
    // import journal data from the indicated source and add() this to the journal and container.
    // **do not use this channel for journal command encoding...just blockette strings.
    // returns the number of records read.
    // openJournal() must be invoked before any importData() operations are performed.  A journal
    // must be present for transcription and subsequent MMAP-ing.
    public int importData(InputStream inStream) throws ContainerException, IOException, SeedException {
        updateLookup = true;  // assert remapping of lookup IDs
        if (inStream == null) throw new ContainerException("null InputStream");
        if (journalChannel == null)
            throw new ContainerException("ERROR: no journal file is open");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        String nextLine = null;
        stationsPending = 0;
        stationCount = 0;
        int counter = 0;
        while ((nextLine = reader.readLine()) != null) {   // for each line
            addData(nextLine);
            counter++;
        }
        inStream.close();
        // close the queue after all lines have been read in and wait for the queue
        // to empty itself
        consumerCloseAndWait();  // notifies an observer when completed
        stationsPending = 0;  // reset
        return counter;
    }

    // import from a file source
    public int importData(File file) throws ContainerException, IOException, SeedException {
            FileInputStream importFileStream = new FileInputStream(file);  // open as readonly input stream
            return importData(importFileStream);
    }

    // import from a file source -- use full pathname
    public int importData(String fileName) throws ContainerException, IOException, SeedException {
        return importData(new File(fileName));
    }

    // a special version of importData().
    // treat the current context as the parent to the incoming data.
    // throw out all blockettes that are a parent to the context, so as to prevent context overwrite.
    // however, allow new dictionary entries that are referenced.
    public int importChildren(InputStream inStream) throws ContainerException, IOException, SeedException {
        updateLookup = true;  // assert remapping of lookup IDs
        if (inStream == null) throw new ContainerException("null InputStream");
        if (journalChannel == null)
            throw new ContainerException("ERROR: no journal file is open");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        String nextLine = null;
        int counter = 0;
        // let's save a reference to the current context, since it
        // will probably be overwritten by the abbreviations, empty parents, etc.
        SeedObjectTag savedContext = previousTag;
        boolean abbreviationFlag = false;  // abbrev flag - should this start out 'true'?
        while ((nextLine = reader.readLine()) != null) {   // for each line
            // accept or reject lines based on the blockette type
            String splitArr[] = nextLine.split("\\|");
            int type = 0;
            boolean skipData = true;
            if (splitArr.length > 0 && splitArr[0].length() == 3) {
                type = Integer.parseInt(splitArr[0]);  // the first field will be the blockette type
            }
            if (type > 0) {
                if (BlocketteFactory.getCategory(type).equals("Abbreviation Dictionary")) {
                    skipData = false;
                    abbreviationFlag = true;
                } else if (BlocketteFactory.getCategory(type).equals("Station")) { // stations/channels/responses
                    if (type > savedContext.getType()) { // if the data line is a child of current selection...
                        // if switching from abbreviation to children
                        // execute this block just once.
                        if (abbreviationFlag == true) {
                            // wait for the current consumer queue to empty
                            consumerCloseAndWait();
                            abbreviationFlag = false;
                        }
                        skipData = false;  // we'll write this
                        setContext(savedContext);  // reassert parent context
                    }
                }
            }
            // add this line to the container
            // if skipData is not set to true
            // increase the counter
            if (! skipData) {
                addData(nextLine);
                counter++;
            }

        }
        inStream.close();  // close our input stream
        consumerCloseAndWait();  // close and wait on the consumer again
        
        return counter;
    }


    // soft-delete the entry for the given tag.
    // can be reinstate()-ed later
    public void delete(SeedObjectTag tag) throws ContainerException, IOException {
        if (tag==null)
            throw new ContainerException("ERROR: update operation passed a null tag.");
        if (journalChannel == null)
            throw new ContainerException("ERROR: no journal file is open");
        // check to see that we have an entry for this tag
        if (! containsKey(tag))
            throw new ContainerException("ERROR: no key found");
        if (!isActive(tag)) { // if already inactive, do not do again
            return;
        }
        // append a 'delete' entry to the journal file queue
        try {
            while (fileLock == true) Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new IOException("ERROR: file lock acquisition interrupted");
        }
        fileLock = true;
        journalFile.seek(journalFile.length());  // position to EOF
        String output = "::d::" + tag;
        MMAPAtom matom = new MMAPAtom();
        matom.msg = output;   // the equivalent of what you'd get from readLine()
        matom.pos = journalChannel.position();
        matom.length = output.length();
        journalFile.writeBytes(output + "\n");  // write the directive contents to the file
        fileLock = false;

        // push these same contents to the log consumer thread
        pushToConsumer(matom);      // push journal entry to consumer thread
        
    }

    // a synonym for delete()
    public void deleteData(SeedObjectTag tag) throws ContainerException, IOException {
        delete(tag);
    }
    
    // just like delete(), but perform a wait on the consumer thread to make sure we're done
    public void deleteAndNotify(SeedObjectTag tag) throws ContainerException, IOException {
    	delete(tag);
    	consumerCloseAndWait(); // wait until the consumer thread has processed, then notify observers
    }

    // restore the current value entry for this tag to an active state.
    // usually as a result of a rollback over a delete directive.
    public boolean reinstate(SeedObjectTag tag) throws ContainerException, IOException {
        if (tag==null) {
            throw new ContainerException("ERROR: reinstate() operation provided a null tag.");
        }
        if (journalChannel == null)
            throw new ContainerException("ERROR: no journal file is open");
        // check to see that we have an entry for this tag
        if (! containsKey(tag))
            throw new ContainerException("ERROR: no reference found for: " + tag);
        // a tag that is active does not need to be reinstated
        if (isActive(tag)) return true;
        //
        //
        MappedByteBuffer mBuf;
        synchronized (journalMap) {
            mBuf = journalMap.get(tag);
        }
        if (mBuf == null) {
            // this shouldn't typically happen, but we'll be fault tolerant
            // in this case
            try {
                acquire(tag,0);  // go rifling through the pile to find the last buf
                mBuf = acquireBuf;
            } catch (Exception e) {
                throw new ContainerException(e.getMessage());
            }
        }
        if (mBuf != null) {  // check again
            // make an update call that will reinstate the tag and its data
            String mStr = getString(mBuf);  // get string value from mapped buffer
            update(tag,mStr,true);  // update with reinstate enabled
            return true;
        } else {
            return false;  // could not find a buf value
        }
    }

    // associate the data for oldTag with a new tag and write the directive
    // to the journal file.  This should generally not be driven manually,
    // but only by automated log playback systems.
    // tag: changes  data: stays
    public void rename(SeedObjectTag oldTag, SeedObjectTag newTag) throws ContainerException, IOException {
        if (oldTag == null || newTag == null)
            throw new ContainerException("ERROR: rename operation passed a null tag.");
        if (journalChannel == null)
            throw new ContainerException("ERROR: no journal file is open");
        // check to see that we have an entry for this tag -- if not, then fail
        if (! containsKey(oldTag))
            throw new ContainerException("ERROR: original tag for rename not found:" + oldTag);
        if (! isActive(oldTag))
            throw new ContainerException("ERROR: cannot rename an inactive tag, reinstate first: " + oldTag);
        // append a 'retag' entry to the journal file queue
        try {
            while (fileLock == true) Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new IOException("ERROR: file lock acquisition interrupted");
        }
        fileLock = true;
        journalFile.seek(journalFile.length());  // position to EOF
        String output = "::r::" + oldTag + "::" + newTag;
        MMAPAtom matom = new MMAPAtom();
        matom.msg = output;
        matom.pos = journalChannel.position();
        matom.length = output.length();
        journalFile.writeBytes(output + "\n");  // write output plus CR
        fileLock = false;

        // push the journal mapped contents to the log consumer thread
        pushToConsumer(matom);      // push journal entry to consumer thread
    }


    // rename operation, but signal observer when completed
    public void renameAndNotify(SeedObjectTag oldTag, SeedObjectTag newTag) throws ContainerException, IOException {
        rename(oldTag,newTag);
        consumerCloseAndWait(); // wait until the consumer thread has processed, then notify observers
    }

    
    // push to the journal that we have asserted a value under the indicated tag.
    //**NOT ACTIVE**
    public void logAssertion(SeedObjectTag tag, MappedByteBuffer mbb) throws ContainerException, IOException {
        // append an 'assert' entry to the journal file queue
//        String output = "::a::" + tag + "::" + getString(mbb);
        // etc....
    }

    // append an inert 'collision' entry to the journal file queue.
    public void logCollision(SeedObjectTag tag) throws IOException, ContainerException {
        if (journalChannel == null)
            throw new ContainerException("ERROR: no journal file is open");
        // append a 'collision' entry to the journal file queue
        try {
            while (fileLock == true) Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new IOException("ERROR: file lock acquisition interrupted");
        }
        fileLock = true;
        journalFile.seek(journalFile.length());  // position to EOF
        String output = "::x::" + tag;
        MMAPAtom matom = new MMAPAtom();
        matom.msg = output;
        matom.pos = journalChannel.position();
        matom.length = output.length();
        journalFile.writeBytes(output + "\n");  // write output plus CR
        fileLock = false;

        // push the journal mapped contents to the log consumer thread
        pushToConsumer(matom);      // push journal entry to consumer thread
    }

        // append an inert 'context' entry to the journal file queue.
    public void logContext(SeedObjectTag tag) throws IOException, ContainerException {
        // WE NEED TO VERIFY THAT THIS DOESN'T CAUSE CONCURRENCY PROBLEMS
//        if (journalChannel == null)
//            throw new ContainerException("ERROR: no journal file is open");
//        // append a 'collision' entry to the journal file queue
//        try {
//            while (fileLock == true) Thread.sleep(50);
//        } catch (InterruptedException e) {
//            throw new IOException("ERROR: file lock acquisition interrupted");
//        }
//        fileLock = true;
//        journalFile.seek(journalFile.length());  // position to EOF
//        String output = "::t::" + tag;
//        MMAPAtom matom = new MMAPAtom();
//        matom.msg = output;
//        matom.pos = journalChannel.position();
//        matom.length = output.length();
//        journalFile.writeBytes(output + "\n");  // write output plus CR
//        fileLock = false;
//
//        // push the journal mapped contents to the log consumer thread
//        pushToConsumer(matom);      // push journal entry to consumer thread
    }

    // other possible methods:
    // reparent()  move()   -- for case where data migrates


    // ACCESSOR METHODS

    // return the MBB for this tag
    public MappedByteBuffer get(SeedObjectTag tag) throws ContainerException {
        if (tag==null) {
            throw new ContainerException("ERROR: get operation passed a null tag");
        }
        if (! isActive(tag)) {
            throw new ContainerException("ERROR: attempted to get an inactive data object: " + tag);
        }
        MappedByteBuffer returnBuf;
        synchronized(journalMap) {
            returnBuf = journalMap.get(tag);
        }
        if (returnBuf == null) {
            throw new ContainerException("ERROR: buffer entry to tag >" + tag + "< not found.");
        }
        return returnBuf;
    }

    // Return the Blockette string expression for the data located in the
    // MappedByteBuffer m.
    public String get(MappedByteBuffer buf) throws ContainerException {
        return getString(buf);
    }

    // an easy way to generate a blockette from a provided tag
    public Blockette getBlockette(SeedObjectTag tag) throws ContainerException, SeedException {
        return BlocketteFactory.createBlockette(get(get(tag)));
    }

    // this will establish a parent context for anything new added or imported.
    // can be set to null to clear out context as well.
    public void setContext(SeedObjectTag context) {
        try {
        	previousTag = context;    // assign to global context tag
            logContext(previousTag);  // write to journal file
        } catch (Exception e) {
            System.err.println("WARNING: exception thrown when logging container context");
            System.err.println(e.getMessage());
        }

    }

    // return the current container context tag -- signifies the last added
    // blockette
    public SeedObjectTag getContext() {
        return previousTag;
    }

    // Set up iteration over all mapped entries in the container.
    // This will allow iteration over all values.
    // return the number of elements in set
    public int iterate() {
        return iterate(SeedObjectTag.ALL);  // use category filter of ALL
    }

    public int iterate(int volNum, int catNum) {
        throw new UnsupportedOperationException("Not supported with this container.");
    }

    // filter the return to only get the indicated category of items.
    // the category integers are defined by global static names.
    // This will allow iteration over matching values.
    // return the size of the resultant match set.
    public int iterate(int catNum) {
        int matchCount = 0;
        try {
            ArrayList<SeedObjectTag> tagList = (ArrayList<SeedObjectTag>) getTagList();
            Iterator<SeedObjectTag> tagIterator = (Iterator<SeedObjectTag>) tagList.iterator();
            ArrayList<MappedByteBuffer> matchList = new ArrayList<MappedByteBuffer>();
            while(tagIterator.hasNext()) {
                SeedObjectTag nextTag = tagIterator.next();
                if (! isActive(nextTag)) {
                    continue;  // do not include tags that are considered inactive
                }
                //int type = nextTag.getType();
                if (catNum == nextTag.getCategory() || catNum == SeedObjectTag.ALL) {
                    matchList.add( get(nextTag) );  // get the memory map buffer
                    matchCount++;
                }
            }
            // finally, apply the match list to the iterator
            listIterator = null;
            if (matchCount > 0) {
                listIterator = matchList.iterator();
            }
        } catch (ContainerException e) {
            System.err.println("ERROR: encountered while getting tag list (iterate): " + e);
        }
        return matchCount;
    }

    // filter the iterator results to only get items that match to the query string.
    // The query string is in the form of a SeedObjectTag, where all fields of interest
    // will be matched to literals in each field, and treating question marks as an open
    // wildcard for that field.  Partial wildcarding of a field is not yet supported.
    // 000|?|This|IS|a|Sample
    //
    public int iterate(String queryStr) {
        throw new UnsupportedOperationException("Not supported with this container.");
    }

    // set up an iterator over the provided tag list to pull back blockettes.
    // tags that are inactive or are referencing empty data are skipped.
    public int iterate(List<SeedObjectTag> tagList) {
        int returnCount = 0;
        try {
            Iterator<SeedObjectTag> tagIterator = (Iterator<SeedObjectTag>) tagList.iterator();
            ArrayList<MappedByteBuffer> returnList = new ArrayList<MappedByteBuffer>();
            while(tagIterator.hasNext()) {
                SeedObjectTag nextTag = tagIterator.next();
                if (! isActive(nextTag)) {
                    // do not include tags that are considered inactive
                    continue;
                }
                // get the data attached to this tag
                MappedByteBuffer mbb = get(nextTag);
                if (mbb != null) {
                    returnList.add( get(nextTag) );  // get the memory map buffer
                    returnCount++;
                }
            }
            // finally, apply the match list to the iterator
            listIterator = null;
            if (returnCount > 0) {
                listIterator = returnList.iterator();
            }
        } catch (ContainerException e) {
            System.err.println("ERROR: encountered while getting tag list (iterate): " + e);
        }
        return returnCount;
    }


    // get next MBB element from the current iterate() session.
    // return a null value if there are no more elements.
    public MappedByteBuffer getNext() {
        if (listIterator == null) return null;
        if (listIterator.hasNext()) {
            MappedByteBuffer mbb = listIterator.next();
            return mbb;
            //return listIterator.next();
        }
        else return null;
    }

    // alternative to getNext that returns a Blockette rendered from the
    // accessed mapped byte buffer...or returns null
    public Blockette getNextBlockette() throws SeedException {
        String str = getString( getNext() );
        if (str.length() > 0) {
            return new Blockette(str);
        } else return null;
    }

    // returns a sort-ordered list of ALL tags, **whether active or inactive**
    // try using getTagList(catNum) or similar to get a list of just active tags
    public List<SeedObjectTag> getTagList() throws ContainerException {
        synchronized(journalMap) {
            return new ArrayList<SeedObjectTag>(journalMap.keySet());
        }
    }

    // get a list of call tags that are a member of the indicated category
    // filter out tags that are inactive
    public List<SeedObjectTag> getTagList(int catNum) throws ContainerException {
        ArrayList<SeedObjectTag> tagList = (ArrayList<SeedObjectTag>) getTagList();
        Iterator<SeedObjectTag> tagIterator = (Iterator<SeedObjectTag>) tagList.iterator();
        ArrayList<SeedObjectTag> matchList = new ArrayList<SeedObjectTag>();
        while(tagIterator.hasNext()) {
            SeedObjectTag nextTag = tagIterator.next();
            if (! isActive(nextTag)) {
                continue;  // do not include tags that are considered inactive
            }
            if (catNum == nextTag.getCategory() || catNum == SeedObjectTag.ALL) {
                matchList.add( nextTag );
            }
        }
        return matchList;
    }

    // get a list of call tags that match to the indicated blockette type.
    // only active tags are returned
    public List<SeedObjectTag> getTagListByType(int blkType) throws ContainerException {
        ArrayList<SeedObjectTag> tagList = (ArrayList<SeedObjectTag>) getTagList();
        Iterator<SeedObjectTag> tagIterator = (Iterator<SeedObjectTag>) tagList.iterator();
        ArrayList<SeedObjectTag> matchList = new ArrayList<SeedObjectTag>();
        while(tagIterator.hasNext()) {
            SeedObjectTag nextTag = tagIterator.next();
            if (! isActive(nextTag)) {
                continue;  // do not include tags that are considered inactive
            }
            if (blkType == nextTag.getType()) {
                matchList.add( nextTag );
            }
        }
        return matchList;
    }


    
    // get a list of all the child tags for the given tag ID
    public List<SeedObjectTag> getChildTags(String parentID) throws ContainerException {
        if (parentID == null) return null;
        return getChildTags(new SeedObjectTag(parentID));
    }



    // synonymous to the above method call, except for the parameter type
    public List<SeedObjectTag> getChildTags(SeedObjectTag parentTag) throws ContainerException {
        if (parentTag == null) return null;
        // only station and waveform category tags can have children
        if (parentTag.getCategory() != SeedObjectTag.STATION &&
                parentTag.getCategory() != SeedObjectTag.WAVEFORM) return null;
        int type = parentTag.getType();
        if (type == 51 || (type > 52 && type < 999) || type > 999) return null;  // these blockettes cannot have children either
        // the idea is to have the parent tag and find its nearest neighbor.
        // get a tailMap list of only active tags and then find the neighbor.
        // matchlist will be all elements between parent tag and following tag, exclusive of either.
        ArrayList<SeedObjectTag> tagList = (ArrayList<SeedObjectTag>) getTagsBetween(parentTag,null);  // tailMap - excludes parentTag in return
        ArrayList<SeedObjectTag> matchList = new ArrayList<SeedObjectTag>();  // this is the list we will return
        SeedObjectTag followingTag = null;
        boolean found = false;
        // we have special matching cases to determine what a neighbor is
        for (int i = 0; i < tagList.size(); i++) {
            followingTag = tagList.get(i);
            // if we reach the metadata/data boundary, we've found the neighbor
            if (parentTag.getCategory() == SeedObjectTag.STATION &&
                    followingTag.getCategory() == SeedObjectTag.WAVEFORM) {
                found = true;
                break;
            }
            // else compare types for the neighbor
            switch (type) {
                case 50:
                    if (followingTag.getType() == 50) {
                        found = true;  // found our neighbor
                    }
                    break;
                case 52:
                    if (followingTag.getType() == 50 ||
                            followingTag.getType() == 52) {
                        found = true;  // found our neighbor
                    }
                    break;
                case 999:
                    if (followingTag.getType() == 999) {
                        found = true;
                    }
                    break;
            }

            if (found) break;  // ...from loop
            matchList.add(followingTag);  // else, add this tag to the return list
        }
        return matchList;  // return what we collected

    }


    // a variation on getChildTags where we know the start and end tags for the subset of tags that we want.
    // if endTagExclusive is null, then we will just return the tail map, starting at startTag.
    // There's no guarantee that the tags returned are children of startTag.  You have to be aware of
    // what the endtag represents.
    // Unlike a subMap, this does not inclue the startTag, but returns only tags between start and end.
    public List<SeedObjectTag> getTagsBetween(SeedObjectTag startTag, SeedObjectTag endTag) throws ContainerException {
        if (startTag == null) return null;
        // the idea is to have the parent tag and its nearest neighbor
        ArrayList<SeedObjectTag> tagList;
        ArrayList<SeedObjectTag> matchList = new ArrayList<SeedObjectTag>();
        SortedMap<SeedObjectTag,MappedByteBuffer> childMap = null;
        synchronized(journalMap) {
            if (endTag == null) { // if we're the last node at this level
                childMap = journalMap.tailMap(startTag);
                tagList = new ArrayList<SeedObjectTag>(childMap.keySet());  // get keys
            } else { // we have a start and end node to demarcate the children
                childMap = journalMap.subMap(startTag,endTag);
                tagList = new ArrayList<SeedObjectTag>(childMap.keySet());  // get keys
            }
        }
        // our matchList will consist of **only active tags**.
        // start at element 1 because element 0 is the parent itself.
        for (int i = 1; i < tagList.size(); i++) {
            SeedObjectTag nextTag = tagList.get(i);
            if (isActive(nextTag)) {
                matchList.add( nextTag );
            }
        }
        return matchList;  // done
    }


    public int getStationsPending() {
        return stationsPending;
    }

    public int getStationCount() {
        return stationCount;
    }

    private void incrementStationCount() {
        if (stationCount < stationsPending) stationCount++;
        if (stationsPending > 0) {
            setChanged();
            notifyObservers(); // observer might be progress meter
        }
    }

    
    // report true if there is already an active tag listed that matches
    // the provided one.
    public boolean checkForCollision(SeedObjectTag tag) {
        if (containsKey(tag) && isActive(tag)) return true;
        else return false;
    }

    // check to see if a tag already exists.  Return a tag that does not collide
    // with another...which could be the same tag
    public SeedObjectTag tagForCollision(SeedObjectTag tag) throws ContainerException,IOException {
        boolean collides = checkForCollision(tag);
        SeedObjectTag newTag = tag;
        if (collides) {
            int insanityCounter = 0;  // prevent infinite loops
            newTag = new SeedObjectTag(tag.getID());  // make a copy of the original tag
            while (collides && insanityCounter < 500) {
                // modify the tag so that
                // we have a unique entry
                logCollision(newTag);
                newTag.increment();  // increment tag value for non abbreviation tags
                insanityCounter++;
                // check for collision with new tag
                collides = checkForCollision(newTag);
            }
        }
        return newTag;
    }

    // based on the current parent context that this container stores,
    // generate a tag resulting from merging context to the blockette string.
    // Does not update the current context as in a typical linear update.
    public SeedObjectTag generateContextTag(String blocketteStr) throws SeedException {
        return new SeedObjectTag(previousTag,blocketteStr);
    }



    // ESTABLISH JOURNAL FILE

    public void openJournal(File filePath, boolean overwrite) throws IOException, ContainerException {
        // check to see if we already have a journalFile
        if (journalFile != null) {
            throw new IOException("Container already has an open file: " + journalFile);
        }
        if (filePath == null) {
            throw new IOException("ERROR: filePath parameter is null");
        }
        if (journalFilePath == null) {
            journalFilePath = filePath;  // save a persistent reference
        }
        // if overwrite == true
        // ensure overwrite using FileWriter (kludge)
        // apparently, Solaris and Linux do not overwrite a file with RAF("rws") mode!
        int numTries = 0;
        while (numTries < 12) {  // try for 11 iterations
            try {
                if (numTries == 11) throw new ContainerException("ERROR (openJournal): unable to open/overwrite journal file at " + filePath.toString());
                if (overwrite) { 
                    FileWriter tempOpenFW = new FileWriter(filePath,false);  // append == false
                    tempOpenFW.close();  // now close this connection
                    // now open RAF session
                    journalFile = new RandomAccessFile(filePath,"rws");  // read/write/synchronous
                    journalChannel = journalFile.getChannel();
                } else {
                    // just open the file for read -- probably using acquire() afterward
                    // may want to append to it, but we start at position 0
                    journalFile = new RandomAccessFile(filePath,"rws");  // read/write/synchronous
                    journalChannel = journalFile.getChannel();
                }
                numTries = 12;  // to pop us out of the try loop if no exceptions thrown
            } catch (IOException e) {
                //System.err.println("ERROR (openJournal): unable to open/overwrite journal file at " + filePath.toString());
                //e.printStackTrace();

                //do garbage collection to ensure that the file is no longer mapped
                //this kludge may or may not be necessary for file re-use situations
                // since Windows cannot re-access a file that is currently mapped.
                // I think that closeJournal() and nulling out this container is sufficient to not
                // necessitate this action.
                System.runFinalization();
                System.gc();
                numTries ++;
            }
        }
        //journalChannel = journalFile.getChannel();
        // set up consumer thread for the journal
        ConcurrentLinkedQueue mq = new ConcurrentLinkedQueue<MMAPAtom>();  // create a new mmap queue
        journalConsumer = new LogConsumer(mq);  // set up consumer thread
        journalConsumer.start();  // start consumer thread

    }
    
    public void openJournal(String baseDir, String name, boolean overwrite) throws IOException, ContainerException {
               openJournal(new File(baseDir,name),overwrite);
    }
    
    public void openJournal(String pathName, boolean overwrite) throws IOException, ContainerException {
        // this version assumes that the journal file indicated is to be overwritten
        openJournal(new File(pathName),overwrite);
    }
    
    public void openJournal(File filePath) throws IOException, ContainerException {
        // this version assumes that the journal file indicated is to be overwritten
        openJournal(filePath,true);
    }
    public void openJournal(String baseDir, String name) throws IOException, ContainerException {
        // this version assumes that the journal file indicated is to be overwritten
        openJournal(new File(baseDir,name));
    }
    public void openJournal(String pathName) throws IOException, ContainerException {
        // this version assumes that the journal file indicated is to be overwritten
        openJournal(new File(pathName));
    }

    // close the currently opened journal file, this means that this container is effectively shut down!
    // doesn't seem to help for Windows filesystems currently, since the channel buffer itself has to get garbage
    // collected.  Putting a kludge in the openJournal routine for exception handling to force the gc, though this
    // is ugly.
    public void closeJournal() throws IOException {
        if (journalChannel != null) {
            // stop and remove the consumer thread
            journalConsumer.closeQueue();
            journalConsumer.yield();
            journalConsumer = null;

            // null out global references to MMAP objects
            acquireBuf = null;
            journalMap = null;
            listIterator = null;

            // close the IO channel and random access to file
            journalChannel.close();
            journalChannel = null;
            journalFile.close();
            journalFile = null;

            // send hint to the garbage collector
            //System.runFinalization();
            //System.gc();

        }
    }

    // undo the entry at the current tag to the previous value
    public void rollBack(SeedObjectTag tag) throws ContainerException, IOException {
        throw new ContainerException("ERROR: rollback not currently implemented");
    }

    // undo the previous data change action
    public void rollBack() throws ContainerException, IOException {
        throw new ContainerException("ERROR: rollback not currently implemented");
    }

    // provides a synchronized interface to checking whether the journal tree
    // map contains the indicated key or not.
    public boolean containsKey(SeedObjectTag tag) {
        synchronized(journalMap) {
            return journalMap.containsKey(tag);
        }
    }

    // return true of the listed SeedObjectTag is currently active.
    // non-existing tags will simply return false.
    public boolean isActive(SeedObjectTag tag) {
        synchronized(activeStateMap) {
            Boolean isActive = activeStateMap.get(tag.toString());
            if (isActive == null || isActive.booleanValue() == false)
                return false;
            else
              return true;
        }
    }

    // set the active/inactive state associated with this tag
    public void setActive(SeedObjectTag tag, boolean isActive) {
        synchronized(activeStateMap) {
            activeStateMap.put(tag.toString(), new Boolean(isActive));
        }
    }


    // register an observer with this container that will watch for signaled changes
    public void registerObserver(Observer o) {
        this.deleteObservers();  // delete any previous observers
        this.addObserver(o);

    }


    // toggle the state of the update lookup flag.
    // when true, add() operations will perform remapping of lookup IDs to
    // local assignments.
    // when false, the lookup IDs are assumed to be aligned to local assignments
    // and are not put through a remap again.
    public void setUpdateLookup(boolean b) {
        updateLookup = b;
    }
    
    
    public int acquire() throws ContainerException, SeedException {
        return acquire(null,0L);
    }

    // load and map all data from the journal starting from the indicated byte offset position
    // if acquireTag is set, then only get data for that tag.
    public int acquire(long pos) throws ContainerException, SeedException {
        return acquire(null,pos);  // null tag field means get all
    }
    
    // rediscover the latest value associated with this tag from the attached journal file
    // starting at the indicated byte offset position
    // returns the total number of records read, despite the tag match
    public int acquire(SeedObjectTag tag, long pos) throws ContainerException, SeedException {
        acquireTag = tag;  // if non-null, this will signal to only get the latest data for this tag
        acquireBuf = null; // empty the capture buf -- populated if we find the tag item
        if (journalFile == null || journalChannel == null || journalFilePath == null) {
            acquireTag = null;  // reset to empty filter
            throw new ContainerException("WARNING: No journal available for acquire operation.  Use openFile().");
        }
        int recordCounter = 0;
        String nextLine = null;
        long currentPosition = pos;        
        SeedObjectTag savedContext = previousTag;
        boolean abbreviationFlag = false;
        try {
            journalFile.seek(currentPosition);  // move to indicated journal position
            while ((nextLine = journalFile.readLine()) != null) {
                // check the type of blockette if an add line, which affects
                // our context monitoring behavior
                String splitArr[] = nextLine.split("\\|");
                int type = 0;
                boolean skipData = true;
                if (splitArr.length > 0 && splitArr[0].length() == 3) {
                    type = Integer.parseInt(splitArr[0]);  // the first field will be the blockette type
                }
                if (type > 0 && BlocketteFactory.getCategory(type).equals("Abbreviation Dictionary")) {
                    // if an abbreviation
                    if (! abbreviationFlag) {
                        // when transitioning to abbreviations, save last non-abbrev context
                        savedContext = previousTag;
                        abbreviationFlag = true;
                    }
                } else {
                    if (abbreviationFlag) {
                        // wait for the current consumer queue to empty
                        consumerCloseAndWait();
                        abbreviationFlag = false;
                        setContext(savedContext);  // reassert parent context 
                    }
                }
                // push MBB triplets via an object to the log consumer thread
                MMAPAtom mmapTriplet = new MMAPAtom();
                // take previous position value, get current length, map it
                int length = nextLine.length();
                mmapTriplet.msg = nextLine;
                mmapTriplet.pos = currentPosition;  // remember start of line, not end of line
                mmapTriplet.length = length;
                //
                pushToConsumer(mmapTriplet);      // push journal entry to consumer thread
                currentPosition = journalChannel.position();  // the current position value is the start of the next read
                recordCounter++;
            }
            // close the queue after all lines have been read in and wait for the queue
            // to empty itself
            consumerCloseAndWait();
            // done
        } catch (IOException e) {
            System.err.println("ERROR: IOException encountered: " + e);
            recordCounter = -1;  // error code??
        }
        acquireTag = null;  // reset filter tag
        return recordCounter;
    }
    


    // METHODS DIRECTED BY THE JOURNAL INPUT QUEUE

    // this is the critical map add function.
    // apply tag and mmap to this object's tree map for later lookup
    // use update() to change the mmap buf for a given tag
    // asserts both tag and buffer values
    private SeedObjectTag set(SeedObjectTag tag, MappedByteBuffer buf) throws ContainerException {
        if (tag==null || buf == null) {
            throw new ContainerException("ERROR: set operation provided null tag or buffer.");
        }
        try {
        	// assert active state for new tag
        	setActive(tag,true);
        	// we don't check for tag collisions here, so the tag selection must be made carefully.
        	synchronized(journalMap) {
        		journalMap.put(tag, buf);
        	}
        	if (tag.getType() == 11) {
        		stationsPending += Integer.parseInt(tag.getIDField("instance"));
        	}
        	if (tag.getType() == 50) {
        		incrementStationCount();  // increment global station count
        	}
        } catch (Exception e) {
        	setActive(tag, false);
            throw new ContainerException("ERROR: set operation threw an unexpected exception: " + e);
        }
        return tag;
    }


    // supporting update(), this will change the mbb offset value attached to the given tag,
    // the source data is already found in the journal file at the newBuf offset
    // tag: same,  newBuf: changes
    private MappedByteBuffer change(SeedObjectTag tag, MappedByteBuffer newBuf) throws ContainerException {
        if (tag==null || newBuf == null)
            throw new ContainerException("ERROR: change operation passed a null tag or buffer.");
        // a tag that is inactive may be repurposed with new buf entry
        setActive(tag,true);
        synchronized(journalMap) {
            MappedByteBuffer oldBuf = journalMap.get(tag);  // get the old mmap value and return it
            journalMap.put(tag,newBuf);  // assert new mbb offset mapping to tag
            return oldBuf;
        }
    }


    // supporting delete() via the log consumer thread
    // perform a soft-delete of the data at tag...log the tag as inactive
    // return the mapped buffer at the tagged location
    private MappedByteBuffer remove(SeedObjectTag tag) throws ContainerException {
        // we will not physically delete the entry, but mark it inactive.
        // It can only be recalled to duty via reinstate(),
        // which is typically activated via a log rollback (undo).
        if (tag==null) {
            throw new ContainerException("ERROR: remove operation passed a null tag.");
        }
        MappedByteBuffer rBuf;
        synchronized(journalMap) {
            rBuf = journalMap.get(tag);
        }
        setActive(tag,false);  // make the tag inactive -- soft delete
        // return the 'removed' buf entry
        return rBuf;
    }


    // this is the direct tag changing method
    // tag: changes   data: stays the same
    private MappedByteBuffer retag(SeedObjectTag currentTag, SeedObjectTag newTag) throws ContainerException, IOException {
        if (currentTag == null || newTag == null) throw new ContainerException("ERROR: retag operation passed a null tag.");
        MappedByteBuffer mbb = null;
        // we deactivate the old tag
        // and pass the memory map to the new tag
        try {
            mbb = remove(currentTag);
        	set(newTag,mbb);
        } catch (Exception e) {
        	// if we have an error during set, then we should restore the earlier
        	// tag to active state
        	setActive(currentTag,true);
        	throw new ContainerException("ERROR: retag operation failed...restoring prior tag.");
        }
        return mbb;  // return the memory map in question
    }
    
 


    // interpret Log message based on leading command tags contained in the
    // mmapTriplet.msg content.
    private void decodeMessage(MMAPAtom mmapTriplet) throws ContainerException, IOException, SeedException {
        // msg content may be tagged
        // <data>                    implicit add
        // ::a::<tag>::<data>        assert data for tag  (not currently used)
        // ::c::<tag>::<data>        change data for tag
        // ::d::<tag>                delete data at tag
        // ::r::<tag1>::<tag2>       retag tag1 to tag2
        // ::t::<tag>                this tag is the current context  (not currently used)
        // ::x::<tag>                log collision on tag  (not currently used)
        String msg = mmapTriplet.msg;
        MappedByteBuffer mbb = null;
        if (msg.charAt(0) == ':' && msg.charAt(1) == ':') {     // check for a directive
            String[] fields = msg.split("::");  // split on this
            char code = fields[1].charAt(0);  // get the directive code - fields[0] is blank
            String tagStr = null;
            String data = null;
            switch (code) {
                case 'a':   // ASSERT
                    // mostly informational
                // currently deactivated
//                        tagStr = fields[2];  // <tag>
//                        data = fields[3];   // <data>
//                        SeedObjectTag tag = new SeedObjectTag(tagStr);
//                        mbb = get(tag);  // get data
//                        if (! getString(mbb).equals(data)) {
//                            set(tag,mbb); // force the setting
//                        }
                    break;
                case 'c':   // CHANGE
                    tagStr = fields[2];  // <tag>
                    data = fields[3];   // <data> - not really needed except for debug
                    int offset = 7 + tagStr.length();   // text offset to data field '::c::<tag>::'
                    mbb = journalChannel.map(   // map new <data> from file
                            FileChannel.MapMode.READ_ONLY,
                            mmapTriplet.pos+offset,
                            mmapTriplet.length-offset);
                    mbb = change(new SeedObjectTag(tagStr),mbb);
                    if (mbb != null) mbb.clear();  // clear old map
                    //
                    break;
                case 'd':   // DELETE
                    tagStr = fields[2];  // <tag>
                    mbb = remove(new SeedObjectTag(tagStr));  // map the remove request
                    if (mbb != null) mbb.clear();  // clear removed map
                    break;
                case 'r':   // RENAME -- change tags
                    tagStr = fields[2];  // <tag1>
                    String tagStr2 = fields[3];  // <tag2>
                    // make SeedObjectTags of both string references
                    SeedObjectTag oldTag = new SeedObjectTag(tagStr);
                    SeedObjectTag newTag = new SeedObjectTag(tagStr2);
                    setContext(newTag);    //was:  previousTag = newTag;  // change the context to the new tag
                    if (! containsKey(oldTag))  // the old tag has to exist for this to work
                        throw new ContainerException("ERROR: attempt to rename from tag not found in container: " + oldTag);
                    try {
                    		mbb = get(oldTag);  // get data buffer from original tag
                    		checkForCollisionAndSet(newTag,mbb);  // set data to new tag -- implicitly sets previousTag (context)
                    		remove(oldTag);  // now soft-delete tag
                    } catch (Exception e) {
                    		setActive(oldTag,true);  // restore old tag's active status
                    		throw new ContainerException("ERROR: attempt to rename " + oldTag + " threw an exception: " + e);
                    }
                    break;
                case 't':  // CONTEXT
                    // informational -- signals a setting of context to this tag
                    // do no actions.
                    break;
                case 'x':  // COLLISION
                    // informational -- signals a collision, indicating the tag on which the collision occurred
                    // do no actions.
                    break;
            }
        } else {           // ADD
            mbb = journalChannel.map(
                            FileChannel.MapMode.READ_ONLY,
                            mmapTriplet.pos,
                            mmapTriplet.length);
            // generate a tag for the first time -- context-oriented on previous tag.
            // also see setContext()
            SeedObjectTag newTag = new SeedObjectTag(previousTag,mmapTriplet.msg);
            // check acquireTag to see if we are trying to generate just a single tag
            // see acquire() methods
            if (acquireTag != null) {
                if (acquireTag.equals(newTag)) {
                    acquireBuf = mbb;  // save buffer to global handle
                    // move on to setting this buffer to the tree map
                } else {
                    return;  // take no further action if we do not match
                    // stop here.
                }
            }
            // set this as now being the context tag (previousTag)
            setContext(newTag);
            if (newTag.getCategory() == SeedObjectTag.ABBREVIATION) {
                //for abbreviation tags, do a reindex scan to get a unique lookup ID
                if (updateLookup) setAbbreviation(newTag, mbb);  // REC - Dec 2011 -- added updateLookup flag check
            } else {
                // check for a tag collision and modify if needed...resets the context if necessary
                checkForCollisionAndSet(newTag,mbb);  // this will take care of the mbb mapping
                // update the lookup key based on current abbreviation transfer
                if (updateLookup) updateForLookup(previousTag,mbb);  // previousTag is an increment of newTag if a collision was detected
            }
                
        }  // end ADD

    }



    // check to see if a tag already exists.  If so, then find a new tag and
    // set with the mapped byte buffer supplied.
    private void checkForCollisionAndSet(SeedObjectTag tag, MappedByteBuffer mbb) throws SeedException, ContainerException, IOException {
        boolean collides = checkForCollision(tag);
        SeedObjectTag newTag = tag;
        if (collides) {
            int insanityCounter = 0;  // prevent infinite loops
            newTag = new SeedObjectTag(tag.getID());  // make a copy of the original tag
            while (collides && insanityCounter < 100) {
                // modify the tag and possibly the buffer contents so that
                // we have a unique entry
                logCollision(newTag);  // note the collision in the log
                newTag.increment();  // increment tag value for non abbreviation tags
                insanityCounter++;
                // check for collision with new tag
                collides = checkForCollision(newTag);
            }
            //** NOT ACTIVE ** logAssertion(newTag,mbb);
        }
        // now we will perform the set operation with the resultant tag ID
        set(newTag,mbb);
        // set this as now being the context tag
        //old:    previousTag = newTag;
        setContext(newTag);
    }


    // update a blockette for its lookup reference field to an abbreviation.
    // the abbreviation lookup ID may have been updated.
    // tag and mbb are for the referencing blockette, not for the abbreviation referenced
    private void updateForLookup(SeedObjectTag tag, MappedByteBuffer mbb) throws SeedException, ContainerException, IOException {
        if (lookupIDTransfer == null) return;
        int type = tag.getType();
        int[] fieldNum = SeedDictionaryReferenceMap.lookupSourceFld(type);
        if (fieldNum == null) return;  // we don't have any dictionary lookups
        Blockette blk = BlocketteFactory.createBlockette(getString(mbb));  // get blockette object from mbb

        for (int f: fieldNum) {  // for each referencing field
            int lookup = (Integer) blk.getFieldVal(f);  // get the current lookup
            if (lookup == 0) continue;  // a lookup of zero means nothing to reference
            int[] destBlk = SeedDictionaryReferenceMap.lookupDestBlk(type, f);  // get list of referenced blockette types
            for (int d: destBlk) {  // foreach abbrev blockette type
                // consult the translation map and see if something comes up
                HashMap lookupIDMap = lookupIDTransfer.get(d);  //HoH(type,H)
                if (lookupIDMap == null) continue;  // not for this abbrev type
                Object xferObj = lookupIDMap.get(lookup);  // H(old,new) -- get new lookup
                if (xferObj == null) continue;       // might be null
                int xferLookup = (Integer) xferObj;  // H(old,new) -- get new lookup
                if (lookup == xferLookup) continue;  // we don't have to change anything
                blk.setFieldVal(f,xferLookup);  // set to the new lookup value
            }
        }
        // if we're all done, trigger an update
        update(tag,blk.toString(),true);
    }

    // This will find a new dictionary lookup value for the indicated tag and its associated data.
    // First the tag will be renamed, then the data itself will be updated.
    public void setAbbreviation(SeedObjectTag tag, MappedByteBuffer mbb) throws SeedException, ContainerException, IOException {
        SeedObjectTag newTag = new SeedObjectTag(tag.toString());
        if (tag.getCategory() != SeedObjectTag.ABBREVIATION) return;  // abbreviations only
        int newVal = getUniqueAbbreviationIndex(newTag);  // this will find a new or matching entry (matchingAbbreviationFound)
        if (newVal > -1 && !matchingAbbreviationFound) {  // if a new index was created
            // STEP 1 - rename the tag and assert data entry to our journal map
            newTag.setLookupIndex(newVal);  // update the lookup ID
            set(newTag,mbb);  // use the current mbb for now
            //
            // STEP 2 - create an altered data entry from the mbb
            // and push an update to the journal and log consumer
            String blocketteStr = getString(mbb);
            if (blocketteStr == null || blocketteStr.length() == 0)
                throw new ContainerException("ERROR: cannot get mapped byte buffer for rewriting lookup index");
            Blockette blk = new Blockette(blocketteStr);
            int type = blk.getType();
            int referenceField = SeedDictionaryReferenceMap.lookupDestFld(type);
            if (referenceField > 1) {
                int oldVal = (Integer) blk.getFieldVal(referenceField);  // get old index value
                blk.setFieldVal(referenceField,newVal);  // set with new index value
                latestLookupID = newVal;  // save a global version of the new index for synchronous post-reference
                // update the mapped byte buffer entry by logging to journal
                update(newTag,blk.toString(),true);  // log a change in the data
                // STEP 3 - record the lookup translation, so we can alter lookups for later  referencing blockettes
                // ready our lookupID translation map
                if (lookupIDTransfer != null) {
                    HashMap<Integer,Integer> lookupIDMap = lookupIDTransfer.get(type);  //HoH(type,H)
                    if (lookupIDMap == null) {  // no lookup?  then make a new one
                        lookupIDMap = new HashMap<Integer,Integer>();
                    }
                    lookupIDMap.put(oldVal,newVal);     // H(old,new)
                    lookupIDTransfer.put(type, lookupIDMap);
                }
            } else {
                throw new ContainerException("ERROR: failure to update reference field for data belonging to: " + newTag);
            }
        } else if (matchingAbbreviationFound) {  // a new number was not used, do we have a matching one instead?
            // we will simply not include this entry but use the existing one
            latestLookupID = newVal;  // save a global version for synchronous post-reference
            int oldVal = tag.getLookupIndex();  // get old index value
            //record the lookup translation...
            if (lookupIDTransfer != null) {
                HashMap<Integer,Integer> lookupIDMap = lookupIDTransfer.get(tag.getType());  //HoH(type,H)
                if (lookupIDMap == null) {  // no lookup?  then make a new one
                    lookupIDMap = new HashMap<Integer,Integer>();
                }
                lookupIDMap.put(oldVal,newVal);     // H(old,new)
                lookupIDTransfer.put(tag.getType(), lookupIDMap);
            }
        } else {
            // no abbreviation change, just set
            set(newTag,mbb);
        }
        // set this as now being the context tag
        //old:    previousTag = newTag;
        setContext(newTag);
        matchingAbbreviationFound = false;  // reset global flag
    }


    // return the tag for the abbreviation blockette that is referenced by the sourceTag
    // (blockette) at the indicated lookup field (referenceField).  SeedObjectTag returns a null value if
    // nothing can be found for the lookup key at referenceField.
    public SeedObjectTag getAbbreviation(SeedObjectTag sourceTag, int referenceField)
            throws SeedException, ContainerException {

        // get the source blockette
        Blockette sourceBlk = getBlockette(sourceTag);
        if (sourceBlk == null) throw new ContainerException("ERROR: unable to get source blk for tag: " + sourceTag);
        
        // get the lookup key at the reference field as well as the destination blockette type + field
        Integer lookupKey = (Integer) sourceBlk.getFieldVal(referenceField);
        if (lookupKey == null) throw new SeedException("ERROR: lookup key value at field (" +
                referenceField + ") returns null.");
        int lookupInt = lookupKey.intValue();
        if (lookupInt == 0) return null;  // this field is not referencing any abbreviation
        int sourceType = sourceBlk.getType();
        int[] destBlkType = SeedDictionaryReferenceMap.lookupDestBlk(sourceType, referenceField);
        if (destBlkType.length == 0) throw new SeedException("ERROR: could not get abbrev blockette type for blk(" +
                sourceType + "," + referenceField + ")");
        int destField = SeedDictionaryReferenceMap.lookupDestFld(destBlkType[0]);

        // get the list of abbreviations for that type
        ArrayList<SeedObjectTag> tagList = (ArrayList<SeedObjectTag>) getTagListByType(destBlkType[0]);
        Iterator<SeedObjectTag> tagIterator = (Iterator<SeedObjectTag>) tagList.iterator();
        int idx = -1;
        while(tagIterator.hasNext()) {
            SeedObjectTag nextTag = tagIterator.next();
            if (! isActive(nextTag)) continue;  // does not count
            if ((idx = nextTag.getLookupIndex()) > -1) {
                // see if the lookup keys match
                if (idx == lookupInt)
                    return nextTag;  // we've found it; return the tag
            }
        }
        // if we haven't returned a tag value in the loop, then we don't
        // have the abbreviation in our records
        return null;

    }


    // Return a unique abbreviation index value based on the current population of keys.
    // No modification will occur if this is not an abbreviation blockette.
    // Return the new index value, or -1 if nothing was changed.
    //
    // Also look for any matching tags/blockettes that already exist...if found, set global flag to true
    // and return this index number instead.
    public int getUniqueAbbreviationIndex(SeedObjectTag tag) throws SeedException, ContainerException {
        int type = tag.getType();
        if (type > 29 && type < 50) {
            // filter for first unique index number -- grab a list of tags of the same type
            ArrayList<SeedObjectTag> tagList = (ArrayList<SeedObjectTag>) getTagListByType(type);
            if (tagList.size() == 0) return 1;  // empty set?  starting number is 1
            Iterator<SeedObjectTag> tagIterator = (Iterator<SeedObjectTag>) tagList.iterator();
            boolean[] numbersTaken = new boolean[tagList.size()+1];  // initialized as false??
            // mark the index numbers already taken in a boolean array
            // also note the first case of a matching abbreviation entry
            int idx = -1;
            matchingAbbreviationFound = false;  // assert false before checking for matches
            while(tagIterator.hasNext()) {
                SeedObjectTag nextTag = tagIterator.next();  // foreach tag...
                if (! isActive(nextTag)) continue;  // does not count
                if ((idx = nextTag.getLookupIndex()) > -1) { // get its lookup index
                    // does this tag resemble our current parameter tag?
                    // example:    030.ABBREV.0002.Steim2 Integer C.b4c5f
                    // get last two tag fields to compare (offset 16)
                    if ( nextTag.toString().substring(16).equals(
                            tag.toString().substring(16) ) ) {
                        // we treat this as a match
                        matchingAbbreviationFound = true;  // set the global flag
                        return idx;  // shortcut return this lookup index
                    }
                    // flag the index number as in use
                    if(idx < numbersTaken.length) numbersTaken[idx] = true;
                }
            }  // covered all lookups for that blockette type
            // first, check to see whether our current slot is open.
            // if so, we just keep our number. (signified by returning -1)
            if (tag.getLookupIndex() > 0 && tag.getLookupIndex() < numbersTaken.length && numbersTaken[tag.getLookupIndex()] == false) return -1;
            // now search for the first number we can use
            for (int i = 1; i < numbersTaken.length; i++) { // start from count of 1
                if (! numbersTaken[i]) return i;  // the first one we find is false, take it
            }
            return numbersTaken.length;  // last resort
        } else return -1;
    }

    // parent must be a channel -- if not, return a -1
    // count up all blockette 58s...return the highest recorded stage number
    public int getLatestStageNum(SeedObjectTag parentChannel) throws ContainerException {
        if (parentChannel.getType() != 52) return -1;
        ArrayList<SeedObjectTag> tagList = (ArrayList<SeedObjectTag>) getChildTags(parentChannel);
        Iterator<SeedObjectTag> tagIterator = (Iterator<SeedObjectTag>) tagList.iterator();
        int stageNum = -1;
        while(tagIterator.hasNext()) {
            SeedObjectTag nextTag = tagIterator.next();
            if (! isActive(nextTag)) continue;  // does not count
            if (nextTag.getType() != 58) continue;  // only count blk 58s
            int sequence = nextTag.getSequenceNum();
            if (sequence > stageNum) stageNum = sequence;
        }
        return stageNum;
    }

    
    public static String getString(MappedByteBuffer mbb) {
        if (mbb == null) return "";
        int idx = 0;
        //if (! mbb.isLoaded()) mbb.load();  // make sure buffer is loaded
        mbb.load();  // make sure buffer is loaded
        //now moved to global// byte[] extractByte = new byte[65536];
        mbb.rewind();  // iterate from beginning
        while (mbb.hasRemaining())   // read the contents of the mapped buffer
            extractByte[idx++] = mbb.get();
        String returnContents = new String(extractByte,0,idx);
        // clear the extractByte contents for next use
        Arrays.fill(extractByte, 0, idx, ((byte)0));
        // return the contents
        return returnContents;
    }


    // LOG CONSUMER THREAD TO CONTAINER MAP

    // We have a message from the journal file that we want to push to the
    // container's memory map.  We have a running thread to consume
    // these messages and process the mapping.
    private void pushToConsumer(MMAPAtom m) {
        synchronized(journalConsumer.mmapQueue) {
            journalConsumer.mmapQueue.add(m); // add mmap atom to the queue, to be processed by the LogConsumer
            journalConsumer.mmapQueue.notify();  // ping the listener thread
        }
    }

    // close the queue after all lines have been read in and wait for the queue
    // to empty itself.
    // make this public so that external callers can synchronize transitions via
    // Observer notification.
    public void consumerCloseAndWait() throws ContainerException {
        journalConsumer.closeQueue();
        synchronized (journalConsumer.mmapQueue) {
            while (! journalConsumer.isOpen()) {   // queue will reopen when consumer has emptied it
                try {
                    journalConsumer.mmapQueue.wait();  // wait for open notify from consumer thread
                } catch (InterruptedException e) {
                    throw new ContainerException ("ERROR: InterruptedException: " + e);
                }
            }
            // since we are an observable class, notify observers of a state change
            setChanged();
            notifyObservers();
        }
    }


    // for ObjectContainer compatibility
    public void add(Object addThis) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object get(int refNum) throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Object remove(int refNum) throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean locate(int refNum) throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
   
    // inner classes

    // threaded loop that accepts journal entries, generates a tag,
    // and appends the two to the container's journal map.
    class LogConsumer extends Thread {

        public LogConsumer (ConcurrentLinkedQueue<MMAPAtom> mq) {
            mmapQueue = mq;
        }

        @Override
        public void run() {
            try {
                // main consumer loop
                while (! interrupted()) {
                    synchronized (mmapQueue) {
                        while (mmapQueue.isEmpty()) {
                            openQueue();   // assert queue to open state
                            mmapQueue.wait();  // wait for a signal that something has been inserted
                        }
                        // when queue not empty...
                        mmapTriplet = mmapQueue.poll();  // pop the next line off of the queue and decode it
                        decodeMessage(mmapTriplet);
                    }

                }
                //
            } catch (InterruptedException e) {
                System.err.println("log read interrupted -- ending loop");
            } catch (Exception e) {
                System.err.println("Exception in LogConsumer Thread...");
                e.printStackTrace();
                if (mmapTriplet == null) {
                    System.err.println("mmapTriplet is null");
                } else {
                    String msg = mmapTriplet.msg;
                    if (msg == null) {
                        System.err.println("mmap message is null");
                    } else {
                        System.err.println("mmap message is: " + msg);
                    }
                }
            }
        }

        public boolean isOpen() {
            return (closeQueue == false);
        }

        private void openQueue() {
            closeQueue = false;
            synchronized(mmapQueue) {
                mmapQueue.notify();  // notify waiting thread
            }
        }

        public void closeQueue() {
            closeQueue = true;
            synchronized(mmapQueue) {
                mmapQueue.notify();  // notify waiting thread
            }
        }

        private boolean closeQueue = false;
        public ConcurrentLinkedQueue<MMAPAtom> mmapQueue = null;
        MMAPAtom mmapTriplet = null;
    } //
    // END Class LogConsumer

    // a nugget for containing mmap triplets
    class MMAPAtom {
        protected String msg; // the file buffer contents
        protected long pos;  // file position
        protected int length;  // the buffer content length
    }//
    // END Class MMAPAtom





    
    // MAIN for testing purposes -- perhaps later for command line capacity
    public static void main(String args[]) {
        // test import of the indicated file
        // arg0 == "import"
        // arg1 == "filepath"
        if (args.length == 0) {
            System.out.println("usage: SeedVolumeMMAPContainer acquire <filepath> (treat filepath as journal)");
            System.out.println("     : SeedVolumeMMAPContainer import <filepath> (treat filepath as read-only source)");
            System.exit(1);
        }
        String filepath = null;
        SeedVolumeMMAPContainer seedContainer = null;

        // load the data
        try {
            if(args[0].equals("acquire")) {
                filepath = args[1];
                System.out.println("opening file " + filepath + "...");
                seedContainer = new SeedVolumeMMAPContainer(filepath);
                System.out.println("file read complete");
            } else if(args[0].equals("import")) {
                filepath = args[1];
                System.out.println("importing from file " + filepath + "...");
                seedContainer = new SeedVolumeMMAPContainer();
                seedContainer.openJournal(filepath + ".jrn");  // create a journal file
                seedContainer.importData(filepath);  // import the source data
                System.out.println("file import complete");
            } else {
                System.err.println("command not recognized: " + args[0]);
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Exception thrown in main()-load: " + e);
            e.printStackTrace();
            System.exit(1);
        }

        // play back the data
        try {

            System.out.println("file read complete");

            System.out.println("tag list:");
            ArrayList tagList = (ArrayList) seedContainer.getTagList();
            for (int i = 0; i < tagList.size(); i++) {
                System.out.println("-->" + tagList.get(i));
            }

            seedContainer.iterate();
            System.out.println("iterating through imported objects...");
            MappedByteBuffer mbb = null;
            byte[] extraktByte = new byte[262144];  // 256K
            while ((mbb = seedContainer.getNext()) != null) {   // returns a mapped byte buffer
                if (! mbb.isLoaded())  {
                    System.out.println("*** mbb load() engaged ***");
                    mbb.load();
                }
                int idx = 0;
                while (mbb.hasRemaining())   // read the contents of the mapped buffer
                    extraktByte[idx++] = mbb.get();
                String dataMsg = new String(extraktByte,0,idx);
                System.out.println("++>" + dataMsg);
            }  // done with data iteration

        } catch (Exception e) {
            System.err.println("ERROR: Exception thrown in main()-playback: " + e);
            e.printStackTrace();
            System.exit(1);
        }

        System.exit(0);
        
    }



    // instance vars

    private File journalFilePath = null;  // filename reference to the journal
    private RandomAccessFile journalFile = null;  // maintain reference to our journal file
    private FileChannel journalChannel = null;    // journal file channel used for memory maps
    private LogConsumer journalConsumer = null;   // this will point to the journal consumer thread
    private boolean fileLock = false;   // used for internal file lock management (false == unlocked)

    private SeedObjectTag acquireTag = null;      // populated by query tag for specific journal grab
    private MappedByteBuffer acquireBuf = null;   // populated by tag-specific mmap grab from journal

    private SeedObjectTag previousTag = null;   // used for putting future tags in context with a previous one

    // this is a red-black tree ordered map associating SeedObjectTags to its corresponding
    // memory mapped buffer handle
    private TreeMap<SeedObjectTag,MappedByteBuffer> journalMap = null;

    // for tracking the active state of tags
    private HashMap<String,Boolean> activeStateMap = null;

    // for iterating through all volume mapped buffers
    private Iterator<MappedByteBuffer> listIterator = null;

    // for tracking transfer of lookupIDs
    // H<type,H<old,new>>
    private HashMap<Integer,HashMap<Integer,Integer>> lookupIDTransfer = null;  //Hash of Hashes

    private static byte[] extractByte = new byte[262144];  // 256K -- try to reuse this

    private int latestLookupID = 0;  // a global view of the latest added abbreviation lookup ID

    public int stationsPending = 0;    // global tally of the number of stations to be loaded
    public int stationCount = 0;       // global tally of the number of stations currently loaded

    private boolean updateLookup = true;  // flag indicating the lookup IDs need to be remapped upon blockette entry
    private boolean matchingAbbreviationFound = false;  // flag indicating that we have found a pre-existing abbreviation

}