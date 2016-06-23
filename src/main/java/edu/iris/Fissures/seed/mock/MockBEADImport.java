package edu.iris.Fissures.seed.mock;

import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.exception.*;
import java.io.*;
import java.util.concurrent.*;

//import com.isti.shape.javaseed.*;
//import com.opensymphony.oscache.base.*;


/**
 * Mock application to simulate BEAD (blockette string) formatted
 * input to a container entity from an external tool.
 * The BEAD format is the tentative name for the delimited string representation
 * for SEED blockettes supported by JavaSeed.  This is theorized as an effective
 * means of decoupled tool interchange and data persistence.
 * @author Robert Casey, IRIS DMC
 * @version 01/02/09
 */


public class MockBEADImport {

    /**
     * Create new instance of the mock import.  This is private to facilitate
     * singleton instantiation by getInstance()
     */
	private MockBEADImport () {
        queue = new ConcurrentLinkedQueue<String>();  // create a new message queue
        consumer = new BEADConsumer(queue);
        consumer.start();  // start consumer thread

	}

    /**
     * Generate and return a singleton instance of this class.  Future calls will return the
     * already present instance.
     * @return a singleton intance of this class.
     */
    public static MockBEADImport getInstance() {
        if (mockInstance == null) {
            mockInstance = new MockBEADImport();
        }
        return mockInstance;
    }

    /**
     * Trigger import of blockette strings from the indicated file.
     * @param fileName the file containing blockette strings.
     */
    public static void importFrom(String fileName) {
        try {
            MockBEADImport.importFrom(new DataInputStream(new FileInputStream(fileName)));
        } catch (FileNotFoundException e) {
            System.err.println("**ERROR: file not found: " + fileName);
        } catch (IOException e) {
            System.err.println("**IOException encountered: " + e.getStackTrace());
        }
    }

    /**
     * Trigger import of blockette strings from the indicated input stream.
     * @param inStream an instance of InputStream providing blockette strings.
     */
	public static void importFrom(InputStream inStream) {
        try {
            MockBEADImport mockImporter = getInstance();
            if (inStream == null) throw new SeedException("null InputStream");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            String nextLine = null;
            while ((nextLine = reader.readLine()) != null) {   // for each line
                System.err.println("DEBUG: grabbing next line...");
                synchronized (mockImporter.queue) {
                    System.err.println("DEBUG: consumer open?: " + mockImporter.consumer.isOpen());
                    while (! mockImporter.consumer.isOpen()) {     // check to see that queue is open, else wait
                        mockImporter.queue.wait();  // this should end when a notify() is signaled
                    }
                    push(nextLine);     // push next BEAD string to interpreter
                }
            }
            // close the queue after all lines have been read in and wait for the queue
            // to empty itself
            close();
            synchronized (mockImporter.queue) {
                while (! mockImporter.consumer.isOpen()) {   // queue will reopen when consumer has emptied it
                    mockImporter.queue.wait();  // wait for open notify from consumer thread
                }
            }
            // now we can end
        } catch (IOException e) {
            System.err.println("**IOException encountered: " + e.getStackTrace());
        } catch (SeedException e) {
            System.err.println("**SEED Exception encountered: " + e.getStackTrace());
        } catch (InterruptedException e) {
            System.err.println("**interruption encountered during import: " + e.getStackTrace());
        }
	}

    /**
     * Push a single blockette string to the import consumer.
     * @param BEADstr a single blockette string
     * @throws edu.iris.Fissures.seed.exception.SeedException
     */
    /* could also call this 'enqueue', but 'push' is shorter */
    public static void push(String BEADstr) throws SeedException {
        MockBEADImport mockImporter = getInstance();
        if (! mockImporter.consumer.isOpen()) {
            throw new SeedException("**consumer thread is currently closed...let it empty first");
        }
        System.err.println("DEBUG: add to queue...");
        mockImporter.queue.add(BEADstr);  // add the string to the queue
        mockImporter.queue.notify();   // signal to waiting consumer thread
    }

    /**
     * Signal the import consumer thread that we have completed our import of
     * blockette strings.  To close the queue means to accept no new additions to the queue
     * until it has been flushed of all entries.  The consumer thread will reopen the queue
     * when it finds it to be empty.
     * @throws java.io.IOException
     * @throws edu.iris.Fissures.seed.exception.SeedException
     */
    public static void close() throws IOException, SeedException {
        MockBEADImport mockImporter = getInstance();
        mockImporter.consumer.closeQueue();  // signal a close on the queue.
    }
	
   /**
     * Inner class to act as consumer thread.  Pull entries off of the provided queue and
     * run through a mock object instance and then display the contents in string form.
     */
    class BEADConsumer extends Thread {

        public BEADConsumer (ConcurrentLinkedQueue<String> readFrom) {
            q = readFrom;  // get queue instance to consume from
        }

        public void run() {
            try {
                while (! interrupted()) {
                    synchronized (q) {
                        System.err.println("DEBUG: queue size: " + q.size());
                        while (q.isEmpty()) { 
                            openQueue();   // assert queue to open state
                            q.wait();  // wait for a signal that something has been inserted
                        }
                        // when queue not empty...
                        System.err.println("DEBUG: render next line...");
                        display(q.poll());  // pop the next line off of the queue and 'render' to screen
                    }
                }
            } catch (InterruptedException e) {  // exit failsafe
                System.err.println("**interrupted -- ending loop");
                System.exit(0);
            }
        }
        
        public boolean isOpen() {
            return (closeQueue == false);
        }

        private void openQueue() {
            System.err.println("DEBUG: open queue signaled");
            closeQueue = false;
            synchronized(q) {
                q.notify();  // notify waiting thread
            }
        }
        
        public void closeQueue() {
            System.err.println("DEBUG: close queue signaled");
            closeQueue = true;
            synchronized(q) {
                q.notify();  // notify waiting thread
            }
        }

        /**
         * create a Blockette object from the offered String and parrot its contents back.
         * Also report if there was a Blockette 'rendering' problem.
         * @param s the string to render as a Blockette
         */
        public void display(String s) {
            try {
                if (s != null) {
                    Blockette newBlk = BlocketteFactory.createBlockette(s);
                    System.out.println(newBlk.toString());
                }
            } catch (SeedException e) {
                System.err.println("**" + e.getMessage() + " - unable to process: " + s);
            }
        }

        private boolean closeQueue = false;
        private ConcurrentLinkedQueue<String> q = null;
    }


        /**
         * Just display a usage message here.
	 */
	public static void main (String[] args) {
            System.err.println("MockBEADImport -- version " + VERSION + " -- IRIS DMC");
            System.err.println("usage: MockBEADImport <filename>");
            if (args.length > 0) {
                // attempt to process first argument as a file
                System.err.println("Reading file: " + args[0] + "...");
                MockBEADImport.importFrom(args[0]);
            }
            System.exit(0);
        }
        
	// instance variables
	private static final String VERSION = "2009.01.02";
    private static MockBEADImport mockInstance = null;   // singleton instance of this class
    private ConcurrentLinkedQueue<String> queue = null;
    private BEADConsumer consumer = null;
}

