package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.ContainerException;
import edu.iris.Fissures.seed.exception.SeedException;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE
 * Container for maintaining ID annotated edit logs using memory map indexes
 * for retrieval
 * OBSOLETE OBSOLETE OBSOLETE OBSOLETE OBSOLETE
 * @author rob
 */
public class LogMMAPContainer {    ///implements MMAPContainer {

    public LogMMAPContainer(String baseDir, String name) throws FileNotFoundException, IOException, ContainerException {
        mmapReference = new LinkedHashMap();  // this is where we store our log references
        // a LogMMAPContainer creates and/or maintains a reference to its own file.
        // access a log file at the base directory and filename based on the tag.
        // absorb and index all entries or access.
        // if the file does not exist, then create a new one.
        File filePath = new File(baseDir,name);  // access file at directory baseDir
        // rws mode will force sychronous updates to file content and file
        // metadata...
        logRWFile = new RandomAccessFile(filePath,"rws");
        logChannel = logRWFile.getChannel();
        // is this a new file or an existing file?
        // if exists, then acquire all current references and place into a lookup stack
        if (logRWFile.length() > 0) {
            acquireAll(); // load all maps into this container
        } else {  // else, create an empty mapping stack
//      TODO  //logBuf = logChannel.map(MapMode.READ_WRITE, position, size)
        }
    }


    public boolean add(ObjectTag tag, MappedByteBuffer buf) throws ContainerException {
return false;
    }

    public MappedByteBuffer get(ObjectTag tag) throws ContainerException {
return null;
    }




    public boolean update(ObjectTag tag, MappedByteBuffer buf) throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MappedByteBuffer remove(ObjectTag tag) throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean reinstate(ObjectTag tag) throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MappedByteBuffer acquire(ObjectTag tag) throws ContainerException {
return null;
    }
    
    // return value will be the number of unique records mapped
    public int acquireAll() throws ContainerException {
        int recordCounter = 0;
        String nextLine = null;
        try {
            // push MBB triplets via an object to the log consumer thread
            ConcurrentLinkedQueue mmapQueue = new ConcurrentLinkedQueue<MMAPAtom>();  // create a new mmap queue
            LogConsumer consumer = new LogConsumer(mmapQueue);  // set up consumer thread
            MMAPAtom mmapTriplet = new MMAPAtom();
            consumer.start();  // start consumer thread
            long pos = 0L;  // always keep track of byte offset of current read
            while ((nextLine = logRWFile.readLine()) != null) {
                // take previous position value, get current length, map it
                int length = nextLine.length();
                //MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, pos, length);  // create mmap reference
                mmapTriplet.msg = nextLine;
                mmapTriplet.pos = pos;
                mmapTriplet.length = length;
                mmapQueue.add(mmapTriplet); // add mmap atom to the queue
                //
                pos = logChannel.position();  //  the current position value is the start of the next read
                recordCounter++;
            }
            return recordCounter;
        } catch (IOException e) {
            System.err.println("ERROR: IOException encountered: " + e);
            return -1;  // error code??
        }
    }

    public boolean iterate() throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MappedByteBuffer getNext() throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List getTagList() throws ContainerException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

//    public boolean setTagList(List list) throws ContainerException {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    public boolean sort() throws ContainerException {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

     /**
     * Inner class to act as consumer thread.  Grab incoming entries and perform
     * memory mapping operations for later access.
     */
    class LogConsumer extends Thread {

        // the LogConsumer is designed to accept three queues running concurrently
        // String data
        // Long file position
        // Integer length
        public LogConsumer (
            ConcurrentLinkedQueue<MMAPAtom> mq) {
            mmapQueue = mq;
        }

        @Override
        public void run() {
            String msg = null;
            Long pos = null;
            Integer len = null;
            try {
                // main consumer loop
                while (! interrupted()) {
                    synchronized (mmapQueue) {
                        //System.err.println("DEBUG: queue size: " + q.size());
                        while (mmapQueue.isEmpty()) {
                            openQueue();   // assert queue to open state
                            mmapQueue.wait();  // wait for a signal that something has been inserted
                        }
                        // when queue not empty...
                        //System.err.println("DEBUG: next line in queue...");
                        mmapTriplet = mmapQueue.poll();  // pop the next line off of the queue and map it
                        // create a memory map from this
                        MappedByteBuffer mbb = logChannel.map(FileChannel.MapMode.READ_ONLY,
                                mmapTriplet.pos,
                                mmapTriplet.length);
                        // push the memory map to our mmap list using a tag
                        //SeedObjectTag.generateID()
                    }

                }
                //
            } catch (InterruptedException e) {  // exit failsafe
                System.err.println("log read interrupted -- ending loop");
            } catch (IOException e) {
                System.err.println("ERROR: " + e.getStackTrace());
            }
        }

        public boolean isOpen() {
            return (closeQueue == false);
        }

        private void openQueue() {
            //System.err.println("DEBUG: open queue signaled");
            closeQueue = false;
            synchronized(mmapQueue) {
                mmapQueue.notify();  // notify waiting thread
            }
        }

        public void closeQueue() {
            //System.err.println("DEBUG: close queue signaled");
            closeQueue = true;
            synchronized(mmapQueue) {
                mmapQueue.notify();  // notify waiting thread
            }
        }

        private boolean closeQueue = false;
        private ConcurrentLinkedQueue<MMAPAtom> mmapQueue = null;
        MMAPAtom mmapTriplet = null;
    } //
    // END Class LogConsumer

    // a nugget for containing mmap triplets
    class MMAPAtom {
        protected String msg; // the file buffer contents
        protected Long pos;  // file position
        protected Integer length;  // the buffer content length
    }//
    // END Class MMAPAtom

    // instance variables

    // the log container is established for a single journal file during
    // object instantiation
    private RandomAccessFile logRWFile = null;
    private FileChannel logChannel = null;

    // persistent mapping of all MBBs generated
    private LinkedHashMap mmapReference = null;

}
