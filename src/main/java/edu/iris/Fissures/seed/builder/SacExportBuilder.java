package edu.iris.Fissures.seed.builder;

import edu.iris.Fissures.seed.exception.*;
import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.util.*;
import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Concrete Builder class for exporting Blockette objects from
 * the SeedObjectContainer to the SAC file format.  Capable of single output
 * stream or multi-file output.
 * <p>
 * Derived from code developed by:<br>
 * Chris Laughbon<br>
 * Doug Neuhauser<br>
 * Allen Nance<br>
 * Dennis O'Neill
 * @author Robert Casey, IRIS DMC
 * @version 8/12/2010
 */
public class SacExportBuilder extends ExportBuilder {
	
	/**
	 * Create a new Sac Export Buider.
	 */
	public SacExportBuilder() {
		setDefaultMode();  // set up the default SAC output mode (only current option)
		
		// set some default values
		logicalRecordLength = 1024;
		physicalRecordLength = 2000000000;  // set to impossibly high value
		logicalRecords = new Vector(8,8);
		exportMold = new Vector(8,8);
		recordPadding = (byte) 0;  // use nulls for padding, although it probably won't be used
		scriptNesting = new int[8];
		nestingScore = new int[scriptNesting.length];
		builderType = "SAC";       // indicate the type of builder we are
		stationList = new Vector(8,8);   // set up station list vector
	}
	
	// public methods
	
	/**
	 * This String represents the script pattern for binary SAC volume.
	 * Each element is separated by a comma.
	 * @see edu.iris.Fissures.seed.builder.ExportBuilder#getNext()
	 */
	public void setDefaultMode() {
		scriptString = new String ("^,A,(,50,(,52,(,58,),),),71,v,(,999,),B");
		genScriptArray();  // generate array from script string
	}
	
	/**
	 * Get the end time (as a Btime object).  Projected from the start time
	 * based on the number of samples and the calculated sample rate.
	 */	
	public static Btime getEndTime(Btime startTime, int numSamples, int srFactor, int srMult) throws Exception {
		// get the sample rate
		double true_rate;
		if ((srFactor * srMult) == 0.0) {
			true_rate = 10000.0;
		} else {
			true_rate = (Math.pow( (double) (Math.abs(srFactor)), (double) (srFactor / Math.abs(srFactor) ) ) *
					Math.pow( (double) (Math.abs(srMult)), (double) (srMult / Math.abs(srMult) ) ) );
		}
		double ttSeconds = ((double) numSamples) / true_rate * 10000.0;  // find the number of ten-thousands of seconds
		return startTime.projectTime(ttSeconds);  // the end time is the projection from the start time
	}
	
	/**
	 * Output to multiple SAC files.
	 * Pipe the OutputStream assigned to this Builder through a filter that separates
	 * SAC header/data groups into separate output files.  All files are written to local
	 * directory.
	 * Set the verbose flag to true to print file progress to stderr.
	 */
	public void splitToFiles(boolean verboseFlag) throws Exception {
		// close the current output stream in the builder
		close();
		// make a new output stream connected to a pipe
		PipedOutputStream pout = new PipedOutputStream();
		DataOutputStream dout = new DataOutputStream(pout);
		// set up an input stream connected to the pipe
		PipedInputStream pin = new PipedInputStream(pout);
		// open this new output stream in the builder
		open(dout);
		// start up the file split filter
		splitter = new SacSplitFilter(pin,verboseFlag);  // this class extends Thread
		splitter.start();  // start filter thread
	}
	
	/**
	 * Force data encoding of data records.
	 * Forces the encoding of the data records to be of this type, as typified by the
	 * assigned string -- see SeedEncodingResolver.java:encodingArray[] for encoding types.
	 */
	public void setEncoding(String s) {
		fixedEncoding = s;
	}
	
	// protected methods
	
	/**
	 * No function performed.  Overrides ExportBuilder method.  SAC format does
	 * not need padding.
	 */
	protected void padLogical() {
		// override parent class method...
		// do nothing here -- SAC format does not need padding
	}
	
	/**
	 * No function performed.  Overrides ExportBuilder method.  SAC format does
	 * not need padding.
	 */
	protected void padPhysical() {
		// override parent class method...
		// do nothing here -- SAC format does not need padding
	}
	
	/**
	 * Convert SEED object info to SAC orientation.
	 */
	protected void packToRecord() throws Exception {
		// refresh logicalPerPhysical value
		logicalPerPhysical = physicalRecordLength/logicalRecordLength;
		if (exportMold == null) {
			throw new BuilderException("null export mold");
		}
		if (exportMold.size() == 0) {
			return;   // empty export mold, then do nothing.
		}
		// our objects arrive in exportMold...
		Blockette blk = (Blockette) exportMold.get(0);   // get the first blockette object
		//System.err.println("DEBUG: blk lookupId: " + blk.getLookupId());
		//System.err.println("DEBUG: export mold size: " + exportMold.size());
		int bType = blk.getType();  // get the blockette type
		//System.err.println("DEBUG: blk type: " + bType);
		if (bType == 50) {
			// when we get a station blockette, tabulate the station name and
			// network code.  Add this new object to the station list
			SacStation newStation = new SacStation();
			//
			newStation.stationName = blk.toString(3);
			newStation.startEffTime = new Btime(blk.toString(13));
			newStation.endEffTime = new Btime(blk.toString(14));
			newStation.networkCode = blk.toString(16);
			//
			stationList.add(newStation);  // add station info to the list vector
			currentStation = newStation;  // handle to current station
		} else if (bType == 52) {
			// when we get a channel blockette, associate it with the station
			// and record appropriate info
			SacChannel newChannel = new SacChannel();
			//
			newChannel.channelName = blk.toString(4);
			newChannel.locationId = blk.toString(3);
			newChannel.latitude = Float.parseFloat(blk.toString(10));
			newChannel.longitude = Float.parseFloat(blk.toString(11));
			newChannel.elevation = Float.parseFloat(blk.toString(12));
			newChannel.depth = Float.parseFloat(blk.toString(13));
			newChannel.azimuth = Float.parseFloat(blk.toString(14));
			newChannel.dip = Float.parseFloat(blk.toString(15));
			newChannel.sampleRate = Float.parseFloat(blk.toString(18));
			newChannel.startEffTime = new Btime(blk.toString(22));
			newChannel.endEffTime = new Btime(blk.toString(23));
			//
			if (currentStation == null) throw new BuilderException("got a channel blockette before a station blockette");
			currentStation.channels.add(newChannel);  // add channel info to the current station's list
			currentChannel = newChannel;  // handle to current channel
		} else if (bType == 58) {
			if (Integer.parseInt(blk.toString(3)) == 0) {   // check to see that this is a Stage 0 (sensitivity) blockette
				if (currentChannel == null) throw new BuilderException("got a sensitivity blockette before a channel blockette");
				currentChannel.scale = Float.parseFloat(blk.toString(4));
			}
		} else if (bType == 71) {
			// if this is the event hypocenter blockette, then take down the event
			// info so we can include it in the SAC header
			sacEvent = new SacEvent();  // assign object to global variable
			//
			sacEvent.eventTime = new Btime(blk.toString(3));
			sacEvent.eventLat = Float.parseFloat(blk.toString(5));
			sacEvent.eventLon = Float.parseFloat(blk.toString(6));
			sacEvent.eventDep = Float.parseFloat(blk.toString(7));
		} else if (bType == 999) {
			// check to see if there is waveform data attached...if not, then we
			// are not interested in this record
			Waveform thisWaveform = blk.getWaveform();
			if (thisWaveform == null) {
				//System.err.println("DEBUG: thisWaveform == null...do nothing");
				exportMold.clear(); // clear the export mold
				return;  // do nothing else
			}
			// get data record parameters
			dataQuality = (byte) blk.toString(2).charAt(0);  // get just the first character
			stationName = blk.toString(4);
			locationId = blk.toString(5);
			channelName = blk.toString(6);
			networkCode = blk.toString(7);
			startTime = new Btime(blk.toString(8));
			numSamples = Integer.parseInt(blk.toString(9));
			srFactor = Integer.parseInt(blk.toString(10));
			srMult = Integer.parseInt(blk.toString(11));
			// check to see if this data record is continuous with the previous
			prevEndTime = endTime;  // save the previous end time
			endTime = getEndTime(startTime,numSamples,srFactor,srMult);
			long timeDiff = startTime.diffSeconds(prevEndTime);
			//System.err.println("DEBUG: startTime = " + startTime);
			//System.err.println("DEBUG: endTime = " + endTime);
			//System.err.println("DEBUG: timeDiff = " + timeDiff);
			if (timeDiff < -1 || timeDiff > 1) {  // if greater than a difference of 1 second, then this trace ends
				// end the current data record.  also ends the current file in multi-file output mode.
				//System.err.println("DEBUG: triggered end of data trace...writing to output...");
				volumeFinish();
				// reset sample count
				sampleCount = 0;
				// reset the last sample value
				lastSampleValue = 0.0F;
				// reset number of logical records stored
				logicalPerPhysical = physicalRecordLength/logicalRecordLength;
				// generate a new (header) logical record.
				startNewLogical(null,false);
				// keep a separate marker on this object for future updates.
				headerRecord = logicalRecord;
				// match data record parameters to SacStation and SacChannel objects.
				SacStation currentStation = null;
				int stationListSize = stationList.size();
				for (int i = 0; i < stationListSize; i++) {  // find station header object
					currentStation = (SacStation) stationList.get(i);
					if (currentStation.stationName.equals(stationName) &&
							currentStation.networkCode.equals(networkCode) &&
							currentStation.endEffTime.diffSeconds(startTime) >= 0 &&
							currentStation.startEffTime.diffSeconds(endTime) <= 0
					) break;   // found match
					currentStation = null;
				}
				if (currentStation == null) throw new BuilderException("data record " + stationName +
						"/" + networkCode + "/" + channelName +
						"/" + locationId + " without station header info.");
				SacChannel currentChannel = null;
				int channelsSize = currentStation.channels.size();
				for (int i = 0; i < channelsSize; i++) {
					currentChannel = (SacChannel) currentStation.channels.get(i);
					if (currentChannel.channelName.equals(channelName) &&
							currentChannel.locationId.equals(locationId) &&
							currentChannel.endEffTime.diffSeconds(startTime) >= 0 &&
							currentChannel.startEffTime.diffSeconds(endTime) <= 0
					) break;  // found match
					currentChannel = null;
				}

				if (currentChannel == null) throw new BuilderException("data record " + stationName +
						"/" + networkCode + "/" + channelName +
						"/" + locationId + " without channel header info.");
				// write header information in proper fields.
				int wordNum = 0;  // this will be the 32-bit word we are writing to
				float wordVal = 0.0F;  // this will be the float value we are writing
				// DELTA -- word 0
				if (currentChannel.sampleRate == 0.0F) throw new BuilderException("Sample rate for " + stationName +
						"/" + networkCode + "/" + channelName +
						"/" + locationId + " has a value of zero.");
				wordVal = 1 / currentChannel.sampleRate;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// SCALE -- word 3
				wordNum = 3;
				wordVal = currentChannel.scale;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// B -- word 5
				wordNum = 5;
				wordVal = 0.0F;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// O -- word 7
				if (sacEvent != null) {
					wordNum = 7;
					wordVal = sacEvent.eventTime.diffSeconds(startTime);
					System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				}
				// INTERNAL (1) -- word 9
				// set to fixed value of 2
				wordNum = 9;
				wordVal = 2.0F;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// STLA -- word 31
				wordNum = 31;
				wordVal = currentChannel.latitude;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// STLO -- word 32
				wordNum = 32;
				wordVal = currentChannel.longitude;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// STEL -- word 33
				wordNum = 33;
				wordVal = currentChannel.elevation;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// STDP -- word 34
				wordNum = 34;
				wordVal = currentChannel.depth;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// EVLA -- word 35
				if (sacEvent != null) {
					wordNum = 35;
					wordVal = sacEvent.eventLat;
					System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
					// EVLO -- word 36
					wordNum = 36;
					wordVal = sacEvent.eventLon;
					System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
					// EVDP -- word 38
					wordNum = 38;
					wordVal = sacEvent.eventDep;
					System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				}
				// CMPAZ -- word 57
				wordNum = 57;
				wordVal = currentChannel.azimuth;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// CMPINC -- word 58
				wordNum = 58;
				wordVal = currentChannel.dip + 90.0F;
				System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
				// NZYEAR -- word 70
				StringTokenizer timeTok = new StringTokenizer(startTime.toString(),",:.");
				int wordInt = 0;  // now inserting int values
				wordNum = 70;
				if (timeTok.hasMoreTokens()) wordInt = Integer.parseInt(timeTok.nextToken());
				else wordInt = 1900;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// NZJDAY -- word 71
				wordNum = 71;
				if (timeTok.hasMoreTokens()) wordInt = Integer.parseInt(timeTok.nextToken());
				else wordInt = 1;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// NZHOUR -- word 72
				wordNum = 72;
				if (timeTok.hasMoreTokens()) wordInt = Integer.parseInt(timeTok.nextToken());
				else wordInt = 0;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// NZMIN -- word 73
				wordNum = 73;
				if (timeTok.hasMoreTokens()) wordInt = Integer.parseInt(timeTok.nextToken());
				else wordInt = 0;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// NZSEC -- word 74
				wordNum = 74;
				if (timeTok.hasMoreTokens()) wordInt = Integer.parseInt(timeTok.nextToken());
				else wordInt = 0;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// NZMSEC -- word 75
				wordNum = 75;
				if (timeTok.hasMoreTokens()) wordInt = (Integer.parseInt(timeTok.nextToken())/10); // milliseconds
				else wordInt = 0;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// NVHDR -- [INTERNAL (4)] -- word 76
				// fixed value of 6
				wordNum = 76;
				wordInt = 6;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// INTERNAL (5) -- word 77
				// fixed value of 0
				wordNum = 77;
				wordInt = 0;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// INTERNAL (6) -- word 78
				// fixed value of 0
				wordNum = 78;
				wordInt = 0;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// IFTYPE -- word 85
				wordNum = 85;
				wordInt = 1;  // default to this value
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// LEVEN -- word 105
				wordNum = 105;
				wordInt = 1;  // default to logical TRUE
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// LCALDA -- word 108
				// set to false at this time
				wordNum = 108;
				wordInt = 0;
				System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
				// KSTNM -- word 110
				wordNum = 110;
				byte[] wordChar = new byte[] {' ',' ',' ',' ',' ',' ',' ',' '};  // now writing char array
				System.arraycopy(stationName.getBytes(),0,wordChar,0,stationName.length());  // fit to 8 char mold
				System.arraycopy(wordChar,0,headerRecord.contents,wordNum*4,8);  // write to SAC header field
				// KHOLE -- word 116
				wordNum = 116;
				wordChar = new byte[] {' ',' ',' ',' ',' ',' ',' ',' '};
				System.arraycopy(locationId.getBytes(),0,wordChar,0,locationId.length());
				System.arraycopy(wordChar,0,headerRecord.contents,wordNum*4,8);
				// KUSER0 - word 144 - placeholder for data quality flag
				wordNum = 144;
				wordChar = new byte[] {' ',' ',' ',' ',' ',' ',' ',' '};
				wordChar[0] = dataQuality;   // set the first byte only
				System.arraycopy(wordChar,0,headerRecord.contents,wordNum*4,8);
				// KCMPNM -- word 150
				wordNum = 150;
				wordChar = new byte[] {' ',' ',' ',' ',' ',' ',' ',' '};
				System.arraycopy(channelName.getBytes(),0,wordChar,0,channelName.length());
				System.arraycopy(wordChar,0,headerRecord.contents,wordNum*4,8);
				// KNETWK -- word 152
				wordNum = 152;
				wordChar = new byte[] {' ',' ',' ',' ',' ',' ',' ',' '};
				System.arraycopy(networkCode.getBytes(),0,wordChar,0,networkCode.length());
				System.arraycopy(wordChar,0,headerRecord.contents,wordNum*4,8);
				//
				// finally, note the position in this record before writing data section
				headerRecord.position = (158*4);   // header offset
			} // end if new trace...
			
			// in all cases...
			// decompress/translate data values to a float array.
			//
			if (fixedEncoding != null)
				thisWaveform.setEncoding(fixedEncoding);    // force the encoding type
			if (thisWaveform.getEncoding().equals("UNKNOWN")) {
				throw new BuilderException("Waveform data encoding is UNKNOWN");
			}
			float[] dataValues;
			try {
				dataValues = thisWaveform.getDecodedFloats(lastSampleValue);  // using lastSampleValue as bias for decoding
			} catch (SeedException e) {
				// SeedException is thrown if we cannot decode using the listed
				// data encoding type.
				// fall back to some default encoding and proceed forward with a
				// printed warning.
				String defaultEncoding = "Steim1";
				System.err.println("WARNING: " + e);
				System.err.println("proceeding using default encoding " + defaultEncoding);
				thisWaveform.setEncoding(defaultEncoding);
				dataValues = thisWaveform.getDecodedFloats(lastSampleValue);  // using lastSampleValue as bias for decoding
			}
			//
			// transcribe float values to a byte array four times as large
			byte[] dataBytes = new byte[4*dataValues.length];
			for (int i = 0; i < dataValues.length; i++) {
				System.arraycopy(floatToBytes(dataValues[i]),0,dataBytes,4*i,4);
				lastSampleValue = dataValues[i];
			}
			// append data values to logical record, keeping count of the number of points.
			// generate new (data) logical record(s) as needed.
			int dataValueOffset = 0;
			while (dataValueOffset < dataBytes.length) {  // while we still have data to write...
				int writeLength = logicalRecordLength - logicalRecord.position;  // how much can we write for this logical record?
				if (writeLength > dataBytes.length - dataValueOffset) writeLength = dataBytes.length - dataValueOffset;  // last of data?
				//System.err.println("DEBUG: dataValueOffset: " + dataValueOffset + ", logicalRecord.position: " + logicalRecord.position + ", writelength: " + writeLength);
				System.arraycopy(dataBytes,dataValueOffset,logicalRecord.contents,logicalRecord.position,writeLength);
				logicalRecord.position += writeLength;  // increment logical record index
				dataValueOffset += writeLength;   // increment data source index
				if (logicalRecord.position >= logicalRecordLength) {
					// if we have a full logical record, then create a new data section logical record
					//System.err.println("DEBUG: starting new logical record...");
					startNewLogical(null,true);
				}
			}
			// increment sample count by the number of samples (32-bit float words) added.
			sampleCount += numSamples;
		} else {
			// we shouldn't be given anything else
			throw new BuilderException("SAC builder given invalid Blockette object type: " + bType);
		}
		// clear out the export mold for the next incoming object(s)
		exportMold.clear();
	}
	
	/**
	 * Implement export script triggers here.
	 */
	protected boolean checkTrigger(String s) throws Exception {
		if (s.equals("A")) {
			// set the starting value for data trace end time
			endTime = new Btime("1900,001,00:00:00.0000");
		} else if (s.equals("B")) {
		} else if (s.equals("C")) {
		} else return false;   // no trigger, return false
		return true;  // we have found a trigger, so return true
	}
	
	/**
	 * Finish up volume export operations.
	 * We have finished the last SAC 'file', so truncate the data section to the last sample
	 * and write the final values to the header.
	 */
	protected void volumeFinish() throws BuilderException {
		if (logicalRecords.size() > 0) {
			// fill in the Ending Value (E) and Number Points (NPTS) header values
			//
			if (sampleCount == 0) throw new BuilderException("Number of samples for " + stationName +
					"/" + networkCode + "/" + channelName +
					"/" + locationId + " has a value of zero.");
			if (headerRecord == null) throw new BuilderException("null headerRecord during EOF header update");
			if (currentChannel == null) throw new BuilderException("null channel reference during EOF header update");
			// E -- word 6
			int wordNum = 6;  // this will be the 32-bit word we are writing to
			float wordVal = ((float) (sampleCount - 1)) / currentChannel.sampleRate;
			System.arraycopy(floatToBytes(wordVal),0,headerRecord.contents,wordNum*4,4);
			// NPTS -- word 79
			wordNum = 79;
			int wordInt = sampleCount;
			System.arraycopy(Utility.longToIntBytes((long) wordInt),0,headerRecord.contents,wordNum*4,4);
			//
			// resize the last logical array to fit to just the end of the data.
			byte[] resizedRecord = new byte[logicalRecord.position];
			System.arraycopy(logicalRecord.contents,0,resizedRecord,0,logicalRecord.position);
			logicalRecord.contents = resizedRecord;
			// flag end of logical -- set logicalPerPhysical to be the current
			// logical record count so that output writing stops here.
			endOfLogical = true;
			logicalPerPhysical = logicalRecords.size();
			//System.err.println("DEBUG: volumeFinish: number of logical records: " +
			//	    logicalPerPhysical);
		}
		
	}
	
	/**
	 * Create a new logical/physical SAC record and add to logical record vector.
	 * SeedObject will be ignored and should be set to null.
	 * Continuation will be false to build a record with SAC header,
	 * true for just data section.
	 */
	protected void startNewLogical(SeedObject obj, boolean continuation) throws Exception {
		logicalRecord = new LogicalRecord();  // create a new logical record
		if (continuation) {
			// data record
			System.arraycopy(getBlank(false),0,logicalRecord.contents,0,logicalRecordLength);
		} else {
			// header record
			System.arraycopy(getBlank(true),0,logicalRecord.contents,0,logicalRecordLength);
		}
		logicalRecord.position = 0;  // make sure the position is set to 0
		logicalRecords.add(logicalRecord);  // push logical record to vector
	}
	
	
	// private methods
	
	/**
	 * Return a byte array representing a blank SAC record.
	 * If header is true, then the byte array returned is formatted for the SAC header.
	 * When false, it's intended for the data section.
	 */
	private byte[] getBlank(boolean header) throws BuilderException {
		// we only want to generate these blanks once, then save the results
		// to a global variable for reuse.  If it's already been generated, then return
		// the array, else generate it.
		if (header) {
			if (sacHeaderBlank != null) return sacHeaderBlank;
		} else {
			if (sacDataBlank != null) return sacDataBlank;
		}
		byte[] currentBlank = new byte[logicalRecordLength];  // we will write to this temp array
		int position = 0;
		//
		// seed the new logical record with default values.
		// format depends on whether we are looking at the header or data section.
		// the following variables represent the number of 32-bit words.
		int wordStartInteger = 0;
		int wordStartLogical = 0;
		int wordStartCharacter = 0;
		int wordPosLargeChar = 0;
		int wordStartFloat = 0;
		if (header) {    // if building SAC header...
			wordStartInteger = 70;     // where integer values begin
			wordStartLogical = 105;    // where logical values begin
			wordStartCharacter = 110;  // where character values begin
			wordPosLargeChar = 112;    // where large character value is located
			wordStartFloat = 158;      // where float (data section) values begin
		}
		int wordLogicalMax = logicalRecordLength / 4;  // maximum number of 4-byte words allowed
		if (wordStartFloat > wordLogicalMax)
			throw new BuilderException("logical record length configuration too small: " + logicalRecordLength);
		byte[] floatUndef = floatToBytes(-12345.0F);                        // undefined float
		byte[] intUndef = Utility.longToIntBytes(-12345L);                  // undefined integer
		byte[] intFalse = new byte[] {0,0,0,0};                             // false logical
		byte[] charUndef = new byte[] {'-','1','2','3','4','5',' ',' '};    // 8 bytes (2 words)
		byte[] charUndef2 = new byte[] {'-','1','2','3','4','5',' ',' ',' ',' ',' ',' ',' ',' ',' ',' '};  // 16 bytes (4 words)
		for (int i = 0; i < wordStartInteger; i++) {
			// write out float undefined values  (header)
			position = i*4;   // this is our current byte position
			System.arraycopy(floatUndef,0,currentBlank,position,4);  // write word at position i
		}
		for (int i = wordStartInteger; i < wordStartCharacter; i++) {
			// write out integer undefined values  (header)
			position = i*4;   // this is our current byte position
			if (i >= wordStartLogical) {
				// logical field
				System.arraycopy(intFalse,0,currentBlank,position,4);  // write word at position i
			} else {
				// integer field
				System.arraycopy(intUndef,0,currentBlank,position,4);  // write word at position i
			}
		}
		for (int i = wordStartCharacter; i < wordStartFloat; i+=2) {
			// write out character undefined values  (header)
			// note we are incrementing by two words now...
			position = i*4;   // this is our current byte position
			if (i == wordPosLargeChar)             // special case (KEVNM)
				System.arraycopy(charUndef2,0,currentBlank,position,16); // special 4 word field
			else if (i == wordPosLargeChar+2)
				continue;   // skip this offset because of previous 4-word write...
			else
				System.arraycopy(charUndef,0,currentBlank,position,8);   // 2 word field
		}
		for (int i = wordStartFloat; i < wordLogicalMax; i++) {
			// write out float undefined values  (beginning of data section)
			position = i*4;   // this is our current byte position
			System.arraycopy(floatUndef,0,currentBlank,position,4);  // write word at position i
		}
		// now choose which array we return based on the boolean flag
		if (header) {
			sacHeaderBlank = currentBlank;
			return sacHeaderBlank;
		} else {
			sacDataBlank = currentBlank;
			return sacDataBlank;
		}
	}
	
	/**
	 * Generate the export script array from the script string.
	 */
	private void genScriptArray() {
		StringTokenizer expTok = new StringTokenizer(scriptString,",");
		exportScript = new String[expTok.countTokens()];
		int i = 0;
		while (expTok.hasMoreTokens()) exportScript[i++] = expTok.nextToken();
	}
	
	/**
	 * Convert float to 4-byte array.
	 */
	private byte[] floatToBytes(float f) {
		long longVal = (long) Float.floatToIntBits(f);
		return Utility.longToIntBytes(longVal);
	}
	
	// inner classes
	
	/**
	 * Store station info for Sac Export.
	 */
	private class SacStation {
		protected String stationName = null;
		protected String networkCode = null;
		protected Btime startEffTime = null;
		protected Btime endEffTime = null;
		protected Vector channels = new Vector(8,8);
	}
	
	/**
	 * Store channel info for SAC export.
	 */
	private class SacChannel {
		protected String channelName = null;
		protected String locationId = null;
		protected float latitude = 0.0F;
		protected float longitude = 0.0F;
		protected float elevation = 0.0F;
		protected float depth = 0.0F;
		protected float azimuth = 0.0F;
		protected float dip = 0.0F;
		protected float scale = 1.0F;
		protected float sampleRate = 0.0F;
		protected Btime startEffTime = null;
		protected Btime endEffTime = null;
	}
	
	/**
	 * Store optional event info for SAC export.
	 */
	private class SacEvent {
		protected Btime eventTime = null;
		protected float eventLat = 0.0F;
		protected float eventLon = 0.0F;
		protected float eventDep = 0.0F;
	}
	
	
	/**
	 * Filter thread that separates SAC header/data groups into individual files
	 * with unique names.
	 */
	class SacSplitFilter extends Thread {
		
		/**
		 * Create a split filter connected to a Piped Input Stream with optional
		 * verbosity.
		 */
		public SacSplitFilter (InputStream is, boolean verboseFlag) {
			din = new DataInputStream(new BufferedInputStream(is));
			verbose = verboseFlag;
		}
		
		/**
		 * Thread run call.
		 */
		public void run() {
			try {
				while (! interrupted()) {
					float[] floatVal = new float[floatMax];
					int[] intVal = new int[integerMax-floatMax];
					byte[] byteVal = new byte[(headerMax-integerMax)*4];
					counter = 0;  // word counter
					while (counter < floatMax) {  // read in header floats
						//System.err.println("DEBUG: reading header floats");
						floatVal[counter++] = din.readFloat();
					}
					while (counter < integerMax) { // read in header integers and logicals
						//System.err.println("DEBUG: reading header ints");
						intVal[counter-floatMax] = din.readInt();
						counter++;
					}
					while (counter < headerMax) {  // read in header characters
						//System.err.println("DEBUG: reading header chars");
						din.readFully(byteVal,(counter-integerMax)*4,4);  // read 4 bytes at a time
						counter++;
					}
					// now that we have the header in its entirety
					// let's figure out the filename
					//System.err.println("DEBUG: generating filename");
					filenameBuffer = new StringBuffer("");
					timeBuffer = new StringBuffer("");
					filenameBuffer.append(intVal[70-floatMax]); // NZYEAR
					filenameBuffer.append(".");
					timeBuffer.append(intVal[70-floatMax]);
					timeBuffer.append(",");
					//
					DecimalFormat df = new DecimalFormat("000"); // 3 digit zero-padded
					filenameBuffer.append(df.format(intVal[71-floatMax])); // NZJDAY
					filenameBuffer.append(".");
					timeBuffer.append(df.format(intVal[71-floatMax]));
					timeBuffer.append(" ");
					//
					df = new DecimalFormat("00");  // 2 digit zero-padded
					filenameBuffer.append(df.format(intVal[72-floatMax])); // NZHOUR
					filenameBuffer.append(".");
					timeBuffer.append(df.format(intVal[72-floatMax]));
					timeBuffer.append(":");
					//
					filenameBuffer.append(df.format(intVal[73-floatMax])); // NZMIN
					filenameBuffer.append(".");
					timeBuffer.append(df.format(intVal[73-floatMax]));
					timeBuffer.append(":");
					//
					filenameBuffer.append(df.format(intVal[74-floatMax])); // NZSEC
					filenameBuffer.append(".");
					timeBuffer.append(df.format(intVal[74-floatMax]));
					timeBuffer.append(".");
					//
					df = new DecimalFormat("000");  // 3 digit zero-padded
					filenameBuffer.append(df.format(intVal[75-floatMax])); // NZMSEC
					filenameBuffer.append("0."); // extend msec by an extra decimal place
					timeBuffer.append(df.format(intVal[75-floatMax]));
					timeBuffer.append("0 UT");  // extend msec extra decimal and add UT suffix
					//
					chanBuffer = new StringBuffer("");  // storing station/channel name here
					// KNETWK -- first 2 chars
					int byteOffset = (152-integerMax) * 4; 
					String stringVal = (new String(byteVal,byteOffset,2)).trim();
					filenameBuffer.append(stringVal);
					chanBuffer.append(stringVal);
					filenameBuffer.append(".");
					chanBuffer.append(".");
					//
					// KSTNM -- first 5 chars
					byteOffset = (110-integerMax) * 4;
					stringVal = (new String(byteVal,byteOffset,5)).trim();
					filenameBuffer.append(stringVal);
					chanBuffer.append(stringVal);
					filenameBuffer.append(".");
					chanBuffer.append(".");
					//	
					// KHOLE -- first 2 chars
					byteOffset = (116-integerMax) * 4;
					stringVal = (new String(byteVal,byteOffset,2)).trim();
					filenameBuffer.append(stringVal);
					chanBuffer.append(stringVal);
					filenameBuffer.append(".");
					chanBuffer.append(".");
					//	
					// KCMPNM -- first 3 chars
					byteOffset = (150-integerMax) * 4;
					stringVal = (new String(byteVal,byteOffset,3)).trim();
					filenameBuffer.append(stringVal);
					chanBuffer.append(stringVal);
					filenameBuffer.append(".");
					chanBuffer.append(",");
					//	
					// KUSER0 -- quality code might be here
					byteOffset = (144-integerMax) * 4;
					if (byteVal[byteOffset] == 'Q' || byteVal[byteOffset] == 'R' || byteVal[byteOffset] == 'D' || byteVal[byteOffset] == 'M' ) {
						filenameBuffer.append(new String(byteVal,byteOffset,1));
					} else {
						// default to 'U' if quality unknown
						filenameBuffer.append("U");
					}
					//
					filenameBuffer.append(".SAC");  // the file suffix
					//	
					// now we have the filename, let's open it
					// 'dout' is our filehandle
					this.open(filenameBuffer.toString());
					
					// transcribe header info to the file first
					for (int i=0; i<floatVal.length; i++) {
						dout.writeFloat(floatVal[i]);  // float values
					}
					for (int i=0; i<intVal.length; i++) {
						dout.writeInt(intVal[i]);      // int values
					}
					for (int i=0; i<byteVal.length; i++) {
						dout.writeByte(byteVal[i]);    // int values
					}
					
					// transcribe data info
					numPts = intVal[79-floatMax]; // get the number of data points
					// verbose output: meant to mimic what rdseed displays
					if (verbose) System.err.println("Writing " + chanBuffer.toString() + " " +
							numPts + " samples (binary), starting " +
							timeBuffer);
					counter = 0;  // reset our word counter
					float[] dataVal = new float[8];  // store data values here temporarily
					int readSize = 8;
					while (counter < numPts) { // begin transcribing data points
						// read/write data in groups of 8
						readSize = 8;
						if (numPts - counter < 8) readSize = numPts - counter;  // the last values
						for (int j=0; j < readSize; j++) {
							dataVal[j] = din.readFloat();  // read
						}
						for (int j=0; j < readSize; j++) {
							dout.writeFloat(dataVal[j]);   // write
						}
						counter += readSize;
					}
					this.close();   // close the current file
				}
			} catch (IOException e1) {
				System.err.println("I/O Error in SAC file splitter: " + e1);
			}
		}
		
		/**
		 * Open a new file with the indicated filename.
		 */
		public void open(String filename) throws IOException {
			dout = new DataOutputStream(new FileOutputStream(filename));
		}
		
		/**
		 * Close the output file.
		 */
		public void close() throws IOException {
			dout.flush();
			dout.close();
			dout = null;
		}
		
		DataInputStream din = null;
		DataOutputStream dout = null;
		int counter = 0;  // word counter
		int floatMax = 70;     // maximum byte count for reading floats
		int integerMax = 110;  // maximum byte count for reading integers/logical values
		int headerMax = 158;   // maximum number of bytes in SAC header
		int numPts = 0;  // number of data points in current SAC file
		StringBuffer filenameBuffer;  // store the generated filename here
		StringBuffer chanBuffer; // store a copy of the network/station/location/channel name here
		StringBuffer timeBuffer; // store a copy of the time string here
		boolean verbose = false;  // set to true to print to stderr the SAC traces we are writing
		
	}
	
	// instance variables
	
	private String scriptString;    // string to be translated to a builder script
	private byte dataQuality;
	private String stationName = null;
	private String locationId = null;
	private String channelName = null;
	private String networkCode = null;
	private int numSamples = 0;
	private int srFactor = 0;
	private int srMult = 0;
	private Btime startTime = null;           
	private Btime endTime = null;
	private Btime prevEndTime = null;
	private byte[] sacHeaderBlank = null;
	private byte[] sacDataBlank = null;
	private Vector stationList = null;
	private SacStation currentStation = null;
	private SacChannel currentChannel = null;
	private SacEvent sacEvent = null;
	private LogicalRecord headerRecord = null;
	private int sampleCount = 0;
	private float lastSampleValue = 0.0F;
	private String fixedEncoding = null;
	private SacSplitFilter splitter = null;
	
}
