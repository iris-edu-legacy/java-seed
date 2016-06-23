/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.iris.Fissures.seed.mock;

import edu.iris.Fissures.seed.director.*;
import edu.iris.Fissures.seed.builder.*;
import edu.iris.Fissures.seed.container.*;
import java.nio.*;
import java.io.*;
import java.util.*;
/**
 *
 * @author rob
 */
public class MockMMAPImportDriver {

    public static void main (String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("usage: MockMMAPImportDriver <input_SEED_file> <journal_file>");
                System.err.println("   or: MockMMAPImportDriver <input_SEED_file1> <input_SEED_file2> <journal_file>");
                System.exit(1);
            }
            String inputFileStr = args[0];
            String inputFileStr2 = null;
            if (args.length > 2)
                inputFileStr2 = args[1];
            String journalFileStr = args[args.length -1];
            //
            //  create a Builder
            System.out.print("instantiating builder...");
            SeedMMAPImportBuilder seedBuilder = new SeedMMAPImportBuilder();
            // create a Director
            System.out.println("instantiating director and registering builder...");
            SeedImportDirector seedDirector = new SeedImportDirector(seedBuilder);
            // read the SEED data
            System.out.println("opening SEED file " + inputFileStr + " for reading...");
            SeedVolumeMMAPContainer seedContainer = (SeedVolumeMMAPContainer) seedBuilder.getContainer();
            File journalFile = new File(journalFileStr);
            if (journalFile.exists()) journalFile.delete();  // delete pre-existing journal file
            seedContainer.openJournal(journalFile);  // create a new journal file
            DataInputStream seedIn = new DataInputStream(new FileInputStream(inputFileStr));
            System.out.println("accessing SEED data for object generation...");
            seedDirector.construct(seedIn);
            seedIn.close();
            if (inputFileStr2 != null) {
                seedIn = new DataInputStream(new FileInputStream(inputFileStr2));
                System.out.println("accessing second SEED data source for object generation...");
                seedDirector.construct(seedIn);
                seedIn.close();
            }
            System.out.println("going through object container to display everything we have there...");
            seedContainer.iterate();
            MappedByteBuffer buf = null;
            while ((buf = seedContainer.getNext()) != null) {
                System.out.println(SeedVolumeMMAPContainer.getString(buf));  // static method -- display buffer contents
            }
            System.out.println("Listing stations...getting first one for channel test");
            ArrayList arrList = (ArrayList) seedContainer.getTagListByType(50);
            Iterator arrIter = arrList.iterator();
            SeedObjectTag stationTag = null;
            if (arrIter.hasNext()) {
                // get just the first station
                stationTag = (SeedObjectTag) arrIter.next();
            }
            System.out.println("found station: " + stationTag);
            System.out.println("getting children...");
            arrList = (ArrayList) seedContainer.getChildTags(stationTag);
            if (arrList == null) {
                System.out.println("empty list returned.");
            } else {
                arrIter = arrList.iterator();
                while (arrIter.hasNext()) {
                    System.out.println("child blk: " + arrIter.next());
                }
            }
            System.out.println("done.");
            System.exit(0);  // exit sample test
        } catch (Exception e) {
            System.out.println("Caught exception: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

}
