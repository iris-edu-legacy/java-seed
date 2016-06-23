package edu.iris.Fissures.seed.app;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.exception.SeedException;
import edu.iris.Fissures.seed.container.SeedObjectContainer;
import edu.iris.Fissures.seed.container.Waveform;
import edu.iris.Fissures.seed.builder.ExportBuilder;

/**
 * JseedrTest
 *
 * Class used for testing JavaSeed via Jseedr.  Can be set to print waveform
 * data or to alter and write out data.  A good test harness for running
 * diagnostics.
 *
 * @author Kevin Frechette, ISTI
 * @author (modified by) Robert Casey, IRIS DMC
 */

public class JseedrTest  {
 /** SEED type. */
 public static final String SEED_TYPE = "SEED";
 /**
  * Steim1 encoding. Can be encoded to integers with the 'Waveform' class.
  * @see edu.iris.Fissures.seed.container.Waveform
  */
 public static final String STEIM1_ENCODING_METHOD = "Steim1";
 /**
  * Steim1 encoding format (used in B1000.)
  */
 public static final byte STEIM1_ENCODING_FORMAT = 10;
 /**
  * The preferred data record length expressed as an exponent as a power of 2 (2^12=4096.)
  */
 public static final int PREFERRED_DATA_RECORD_LENGTH_EXP = 12;
 /**
  * The preferred data record length in bytes.
  */
 public static final int PREFERRED_DATA_RECORD_LENGTH = 4096;
 /**
  * Blockette 999 Fixed Section Data Header fields.
  */
 public static final int B999_STATION_IDENTIFIER_FIELD = 4;
 public static final int B999_LOCATION_IDENTIFIER_FIELD = 5;
 public static final int B999_CHANNEL_IDENTIFIER_FIELD = 6;
 public static final int B999_RECORD_START_TIME_FIELD = 8;
 public static final int B999_SAMPLE_COUNT_FIELD = 9;
 public static final int B999_SAMPLE_RATE_FACTOR_FIELD = 10;
 public static final int B999_SAMPLE_RATE_MULTIPLIER_FIELD = 11;
 public static final int B999_BEGINNING_OF_DATA = 17;
 /**
  * Blockette 1000 Fixed Section Data Header fields.
  */
 public static final int B1000_NEXT_BLOCKETTE_BYTE_NUMBER = 2;
 public static final int B1000_ENCODING_FORMAT_FIELD = 3;
 public static final int B1000_WORD_ORDER_FIELD = 4;
 public static final int B1000_DATA_RECORD_LENGTH_FIELD = 5;
 public static final int B1000_RESERVED_FIELD = 6;

 /**
  * set to true to turn Jseedr verbose mode ON
  */
 protected boolean verboseMode = true;

 /**
  * set to true to turn Jseedr verbose mode ON when exporting
  */
 protected boolean verboseModeWhenExport = true;

 /**
  * Replace data flag:
  * true to replace the data with re-encoded data or
  * false to leave the data as is.
  */
 protected boolean replaceDataFlag = false;  // see processData()

 /**
  * Use default data record length flag:
  * true to use the default data record length of 4096 bytes or
  * false to use the current value
  */
 private boolean useDefaultDataRecordLengthFlag = true;
 
 /** The last data record length or 0 for none. */
 private int lastDataRecordLength = 0;



 public void printData(File seedVolume) throws Exception {
     
    Jseedr jseedr = new Jseedr();
    jseedr.setVerboseMode(verboseMode); // set verbosity for jseedr

    //import data
    jseedr.getNewImportDirector(SEED_TYPE);
    jseedr.importFrom(new DataInputStream(new FileInputStream(seedVolume)));

    final SeedObjectContainer seedContainer = jseedr.getContainer();
    int numberofChildBlockettes;
    int countBlk10 = 0;  // count blockette 10
    int countBlk11 = 0;  // count blockette 11
    int countBlk12 = 0;  // count blockette 12
    int countBlk70 = 0;  // count blockette 70
    int countBlk74 = 0;  // count blockette 74
    int countBlk999 = 0;  // count blockette 999
    int countBlk1000 = 0;  // count blockette 1000
    seedContainer.iterate();
    for (Blockette dataBlk; (dataBlk = ( (Blockette) seedContainer.getNext())) != null; ) {
     switch (dataBlk.getType())
     {
       case 10:
         countBlk10++;
         break;
       case 11:
         countBlk11++;
         break;
       case 12:
         countBlk12++;
         break;
       case 70:
         countBlk70++;
         break;
       case 74:
         countBlk74++;
         break;
       case 999:
         countBlk999++;
         numberofChildBlockettes = dataBlk.numberofChildBlockettes();
         for (int i = 0; i < numberofChildBlockettes; i++) {
           final Blockette childBlk = dataBlk.getChildBlockette(i);
           switch (childBlk.getType())
           {
             case 1000:
               countBlk1000++;
               //System.out.println(childBlk);
               break;
           }
         }
         break;
      }

    }

    System.out.println(
     "blockette count(10,11,12,70,74,999,1000): " +
     countBlk10 + "," +
     countBlk11 + "," +
     countBlk12 + "," +
     countBlk70 + "," +
     countBlk74 + "," +
     countBlk999 + "," +
     countBlk1000);


     seedContainer.iterate(5);  // iterate over category 5 (waveform) blockettes in container
     int blocketteCounter = 0;
     for (Blockette dataBlk; (dataBlk = ( (Blockette) seedContainer.getNext())) != null; ) {

       blocketteCounter++;
       if (dataBlk.getType() != 999) {
         System.err.println("Data record is not prefaced with FDSH (not type 999): " + dataBlk);
         continue;
       }

       System.out.println("FSDH: " + dataBlk.toString());

       Waveform wf = dataBlk.getWaveform();
       if (wf == null) {
         System.err.println("Data blockette does not contain a waveform: " + dataBlk);
         continue;
       }
       // non-terminating try-catch
       try {
           final int[] data = wf.getDecodedIntegers();
           final int beginningOfData = getInteger(dataBlk, B999_BEGINNING_OF_DATA);
           System.out.println("pulled " + data.length + " decoded integers from byte offset " + beginningOfData);
       } catch (Exception e) {
           e.printStackTrace();
       }

     }

 }


 
 public void processData(File seedVolume) throws Exception
 {
   Jseedr jseedr = new Jseedr();
   jseedr.setVerboseMode(verboseMode); // set verbosity for jseedr

   //import data
   jseedr.getNewImportDirector(SEED_TYPE);
   jseedr.importFrom(new DataInputStream(new FileInputStream(seedVolume)));

   final SeedObjectContainer seedContainer = jseedr.getContainer();
   int numberofChildBlockettes;
   int countBlk10 = 0;  // count blockette 10
   int countBlk11 = 0;  // count blockette 11
   int countBlk12 = 0;  // count blockette 12
   int countBlk70 = 0;  // count blockette 70
   int countBlk74 = 0;  // count blockette 74
   int countBlk999 = 0;  // count blockette 999
   int countBlk1000 = 0;  // count blockette 1000
   seedContainer.iterate();
   for (Blockette dataBlk; (dataBlk = ( (Blockette) seedContainer.getNext())) != null; )
   {
     switch (dataBlk.getType())
     {
       case 10:
         countBlk10++;
         break;
       case 11:
         countBlk11++;
         break;
       case 12:
         countBlk12++;
         break;
       case 70:
         countBlk70++;
         break;
       case 74:
         countBlk74++;
         break;
       case 999:
         countBlk999++;
         numberofChildBlockettes = dataBlk.numberofChildBlockettes();
         for (int i = 0; i < numberofChildBlockettes; i++) {
           final Blockette childBlk = dataBlk.getChildBlockette(i);
           switch (childBlk.getType())
           {
             case 1000:
               countBlk1000++;
               System.out.println(childBlk);
               break;
           }
         }
         break;
     }
   }

   System.out.println(
     "count(10,11,12,70,74,999,1000): " +
     countBlk10 + "," +
     countBlk11 + "," +
     countBlk12 + "," +
     countBlk70 + "," +
     countBlk74 + "," +
     countBlk999 + "," +
     countBlk1000);

   if (replaceDataFlag)
   {
     final int numBlockettes = seedContainer.iterate(5);
     int blocketteCounter = 0;
     int lastEncodedValue = 0; // becomes the bias for the next compression scheme
     for (Blockette dataBlk; (dataBlk = ( (Blockette) seedContainer.getNext())) != null; )
     {
       blocketteCounter++;
       if (dataBlk.getType() != 999)
       {
         System.err.println("Data blockette is invalid: " + dataBlk);
         continue;
       }
       Waveform wf = dataBlk.getWaveform();
       if (wf == null)
       {
         System.err.println("Data blockette does not waveform: " + dataBlk);
         continue;
       }
       final int newDataRecordLength;
       final int[] data = wf.getDecodedIntegers();
       final int beginningOfData = getInteger(dataBlk, B999_BEGINNING_OF_DATA);
       int dataRecordLengthExp = modifyBlock1000(dataBlk);
       final int currentDataRecordLength = getDataRecordLength(dataRecordLengthExp);
       if (lastDataRecordLength == 0)
       {
         System.out.println(
             "The current data record length is " + currentDataRecordLength);
       }
       if (useDefaultDataRecordLengthFlag)
       {
         dataRecordLengthExp = PREFERRED_DATA_RECORD_LENGTH_EXP;
         newDataRecordLength = getDataRecordLength(dataRecordLengthExp);
       }
       else
       {
         newDataRecordLength = currentDataRecordLength;
       }
       if (lastDataRecordLength != newDataRecordLength)
       {
         if (lastDataRecordLength != 0)
         {
           System.out.println(
               "Data record length changed from " + lastDataRecordLength +
               " to " + newDataRecordLength);
         }
         else
         {
           System.out.println(
               "The new data record length is " + newDataRecordLength);
         }
         lastDataRecordLength = newDataRecordLength;
       }
       final int maxByteLen = newDataRecordLength - beginningOfData;
       dataBlk.removeWaveform();
       wf = null;
       wf = new Waveform(data,
                         maxByteLen,
                         STEIM1_ENCODING_METHOD,
                         lastEncodedValue);
       lastEncodedValue = data[data.length - 1];
       dataBlk.attachWaveform(wf);
     }
     if (blocketteCounter != numBlockettes)
     {
       System.err.println("Expected " + numBlockettes +
                          " blockettes but found " +
                          blocketteCounter);
     }
   }

   //export data
   jseedr.getNewTemplate(); // set up a new export template
   // establish export builder
   final ExportBuilder exportBuilder =
       jseedr.getNewExportBuilder(SEED_TYPE);
   exportBuilder.setPaddingEnabled(false);
   if (verboseMode != verboseModeWhenExport)
   {
     jseedr.setVerboseMode(verboseModeWhenExport); // set verbosity for jseedr
   }
   final File outputFile =
       new File(".", seedVolume.getName() + ".jseedr").getAbsoluteFile();
   jseedr.exportTo(new DataOutputStream(new FileOutputStream(outputFile)));
   if (verboseMode != verboseModeWhenExport)
   {
     jseedr.setVerboseMode(verboseMode); // set verbosity for jseedr
   }
 }

 /**
  * Gets the data record length.
  * @param dataRecordLengthExp the dataRecordLength exponent.
  * @return the data record length.
  */
 public int getDataRecordLength(int dataRecordLengthExp)
 {
   return (int) Math.pow(2., dataRecordLengthExp);
 }

 /**
  * Get the integer from the blockette field.
  * @param blk the blockette.
  * @param fieldNum the field number.
  * @return the integer or 0 if none.
  */
 public static int getInteger(Blockette blk, int fieldNum)
 {
   Number n = getNumber(blk, fieldNum);
   if (n != null)
   {
     return n.intValue();
   }
   return 0;
 }

 /**
  * Get the number from the blockette field.
  * @param blk the blockette.
  * @param fieldNum the field number.
  * @return the number or null if none.
  */
 public static Number getNumber(Blockette blk, int fieldNum)
 {
   Object value;
   try
   {
     value = blk.getFieldVal(fieldNum);
     if (value instanceof Number)
     {
       return (Number) value;
     }
     else if (value != null)
     {
       return new Double(value.toString());
     }
   }
   catch (Exception e)
   {
   }
   return null;
 }

 /**
  * Modifies the blockette 1000 based upon the waveform.
  * @param dataBlk the data blockette.
  * @return the existing data record length exponent or 0 if none.
  * @throws SeedException if error.
  */
 public int modifyBlock1000(Blockette dataBlk) throws
     SeedException
 {
   Blockette blk;
   final Waveform w = dataBlk.getWaveform();
   final int numberofChildBlockettes = dataBlk.numberofChildBlockettes();
   for (int i = 0; i < numberofChildBlockettes; i++)
   {
     blk = dataBlk.getChildBlockette(i);
     if (blk != null && blk.getType() == 1000)
     {
       final int dataRecordLengthExp = getInteger(blk,
           B1000_DATA_RECORD_LENGTH_FIELD);
       setFieldVal(blk, B1000_ENCODING_FORMAT_FIELD, STEIM1_ENCODING_FORMAT);
       setFieldVal(blk, B1000_WORD_ORDER_FIELD, w.getSwapBytes() ? 0 : 1);
       if (useDefaultDataRecordLengthFlag)
       {
         setFieldVal(blk, B1000_DATA_RECORD_LENGTH_FIELD,
                     PREFERRED_DATA_RECORD_LENGTH_EXP);
       }
       return dataRecordLengthExp;
     }
   }
   return 0;
 }

 public static void setFieldVal(Blockette blk, int fieldNum, byte value) throws
     SeedException
 {
   blk.setFieldVal(fieldNum, new Byte(value));
 }

 public static void setFieldVal(Blockette blk, int fieldNum, int value) throws
     SeedException
 {
   blk.setFieldVal(fieldNum, new Integer(value));
 }

 public static void main(String[] args)
 {
   try
   {
     final String seedFilename;
     final File seedVolume;
     final String defaultSeedVolume = "mySEED.seed";
     if (args.length >= 1)
     {
       seedFilename = args[0];
     }
     else
     {
       System.out.print(
           "Enter file name (or empty for \'" + defaultSeedVolume + "\'): ");
       BufferedReader stdin = new BufferedReader(new InputStreamReader(
           System.in));
       seedFilename = stdin.readLine();
     }
     if (seedFilename.length() > 0)
     {
       seedVolume = new File(seedFilename);

     }
     else
     {
       seedVolume = new File(defaultSeedVolume);
     }
     final JseedrTest test = new JseedrTest();
     //test.replaceDataFlag = true;    // set to true if we are rewriting the data records
     test.useDefaultDataRecordLengthFlag = true;


     // CHOOSE WHAT KIND OF PROCESSING OR DIAGNOSTICS TO PERFORM
     //test.processData(seedVolume);
     test.printData(seedVolume);

     

     System.exit(0);
   }
   catch (Exception ex)
   {
     ex.printStackTrace();
   }
 }
}