package edu.iris.Fissures.seed.container;

import edu.iris.Fissures.seed.exception.*;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.io.*;

/**
 * An alternative object container that maintains a memory mapping on source files.
 * This is an interface representation for how all MMAPContainers should behave.
 * Concrete containers of this type will be specifically geared to a particular
 * file type.
 * The generic type for this interface refers to a tagging mechanism used as a
 * key for referencing the memory map to data on request.
 * @author Robert Casey
 * @version 01/15/2009
 */
public interface MMAPContainer<T extends ObjectTag, O extends Object> {

    // This is the primary means of appending new entries to the container.
    // The object entry will be written directly to a file journal and
    // the resultant memory map will be tagged and recorded in the container.
    public void addData(O entry) throws ContainerException, IOException;

    // This is the critical data mutability function.
    // Update the current tag reference with the new object string entry.
    // write a change directive to the journal and the log consumer thread queue.
    public void update(T tag, O newEntry) throws ContainerException, IOException;

    // this will establish a parent tagging context for anything new added.
    // Can be set to null for no context.
    public void setContext(T context);

    // remove the MappedByteBuffer indexed by tag from this container.  The mapping
    // is dereferenced from further access unless reinstate() is initiated.  This means
    // that the MappedByteBuffer will remain intact but be flagged as removed
    public void delete(T tag) throws ContainerException, IOException;

    // un-delete the listed tag and value last attached to that tag
    public boolean reinstate(T tag) throws ContainerException, IOException;

    // associate the data for oldTag with a new tag and write the directive
    // to the journal file.  This should generally not be driven manually,
    // but only by automated log playback systems.
    public void rename(T oldTag, T newTag) throws ContainerException, IOException;

    // This is the primary accessor function.  Get the memory map buffer
    // associated with the given tag.
    public MappedByteBuffer get(T tag) throws ContainerException;

    // Return the native object type for the data space located in the
    // MappedByteBuffer m.
    public O get(MappedByteBuffer m) throws ContainerException;

    // Set up iteration over all mapped entries in the container.
    public int iterate();

    // get the next ObjectTag from the currently established iterator session
    public MappedByteBuffer getNext() throws ContainerException;

    // retreive the entire ObjectTag list from this container
    public List<T> getTagList() throws ContainerException;

    // open the journal file for this container using the provided File object
    public void openJournal(File filePath) throws IOException, ContainerException;

    // open the journal file for this container using the provided base directory
    // and desired filename
    public void openJournal(String baseDir, String name) throws IOException, ContainerException;

    // open the journal file for this container using the provided full pathname
    public void openJournal(String pathName) throws IOException, ContainerException;

    // undo the entry at the current tag to the previous value
    public void rollBack(SeedObjectTag tag) throws ContainerException, IOException;

    // undo the previous data change operation
    public void rollBack() throws ContainerException, IOException;


}
