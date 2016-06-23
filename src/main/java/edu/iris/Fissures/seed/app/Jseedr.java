package edu.iris.Fissures.seed.app;

import edu.iris.Fissures.seed.builder.*;
import edu.iris.Fissures.seed.director.*;
import edu.iris.Fissures.seed.container.*;
import edu.iris.Fissures.seed.exception.*;
import java.io.*;
import java.util.*;

//import com.isti.shape.javaseed.*;
//import com.opensymphony.oscache.base.*;

/**
 * Application pulling together all functionality of the FISSURES SEED classes
 * to implement front-to-end SEED processing operations.
 * 
 * @author Robert Casey, IRIS DMC
 * @author Sid Hellman, ISTI
 * @author Kevin Frechette, ISTI
 * @version April 2013
 */

public class Jseedr {

	private static final String VERSION = "3.8.3 -- Apr 2013"; // this is the
																// version
																// displayed in
																// the usage
																// message

	private DataInputStream importStream = null;
	private DataOutputStream exportStream = null;
	private DataOutputStream printStream = null;
	private ImportDirector importDirector = null;
	private Vector builderFilterVec = new Vector(8, 8);
	private SeedObjectBuilder objectBuilder = null; // specific to SEED objects
	private SeedObjectContainer objectContainer = null; // specific to SEED
														// objects
	private SeedExportDirector exportDirector = null; // specific to SEED
														// objects
	private ExportTemplate exportTemplate = null;
	private Vector templateFilterVec = new Vector(8, 8);
	private ExportBuilder exportBuilder = null;
	private boolean verboseMode = false; // set to true to turn verbose mode ON

	/**
	 * Create Jseedr session.
	 */
	public Jseedr() throws Exception {
		objectBuilder = new SeedObjectBuilder(); // construct a SEED object
													// builder
		objectContainer = (SeedObjectContainer) objectBuilder.getContainer(); // obtain
																				// a
																				// global
																				// handle
																				// to
																				// the
																				// object
																				// container
		exportDirector = new SeedExportDirector(); // construct a SEED export
													// director
		exportDirector.assignContainer(objectContainer); // assign container to
															// export director
	}

	/**
	 * Create Jseedr session with assigned serialization file. Serialization
	 * storage is managed by the concrete Object Builder class. Stored objects
	 * are moved to the serialization file to conserve memory.
	 */
	public Jseedr(String storeFile) throws Exception {
		objectBuilder = new SeedObjectBuilder(storeFile); // construct a SEED
															// object builder
															// with
															// serialization
															// file
		objectContainer = (SeedObjectContainer) objectBuilder.getContainer(); // obtain
																				// a
																				// global
																				// handle
																				// to
																				// the
																				// object
																				// container
		exportDirector = new SeedExportDirector(); // construct a SEED export
													// director
		exportDirector.assignContainer(objectContainer); // assign container to
															// export director
	}

	/**
	 * Create Jseedr session for export with already provided
	 * SeedObjectContainer
	 */
	public Jseedr(SeedObjectContainer container) throws Exception {
		objectContainer = container; // obtain a global handle to the object
										// container
		exportDirector = new SeedExportDirector(); // construct a SEED export
													// director
		exportDirector.assignContainer(objectContainer); // assign container to
															// export director
	}

	// public methods
	//

	// import

	/**
	 * Import data from specified input stream.
	 * 
	 * @param inStream
	 *            input stream to import from
	 * @throws Exception
	 *             if there is a problem reading the data
	 */
	public void importFrom(DataInputStream inStream) throws Exception {
		importStream = inStream; // accept assigned input stream
		exportStream = null; // assert null state
		printStream = null; // assert null state
		readData(); // begin reading data from the input stream
	}

	/**
	 * Import data from specified input stream. Print ASCII representation of
	 * input stream to specified output stream.
	 * 
	 * @param inStream
	 *            input stream to import from
	 * @param outStream
	 *            ASCII representation of input stream will go through here
	 * @throws Exception
	 *             if there is a problem reading the data
	 */
	public void importFrom(DataInputStream inStream, DataOutputStream outStream) throws Exception {
		importStream = inStream; // accept assigned input stream
		exportStream = null; // assert null state
		printStream = outStream; // accept assigned output stream
		readData(); // begin reading data from the input stream
	}

	/**
	 * Import data from specified input filename.
	 * 
	 * @param inFile
	 *            the input filename to read from
	 * @throws Exception
	 *             if there is a problem reading the file
	 */
	public void importFrom(String inFile) throws Exception {
		DataInputStream fileIn = new DataInputStream(new FileInputStream(inFile));
		importFrom(fileIn);
	}

	/**
	 * Import data from specified input filename. Print ASCII representation of
	 * input file to specified output filename.
	 * 
	 * @param inFile
	 *            the input filename to read from
	 * @param outFile
	 *            ASCII representation of input stream will be written to this
	 *            file
	 * @throws Exception
	 *             if there is a problem reading the file
	 */
	public void importFrom(String inFile, String outFile) throws Exception {
		DataInputStream fileIn = new DataInputStream(new FileInputStream(inFile));
		DataOutputStream fileOut = new DataOutputStream(new FileOutputStream(outFile));
		importFrom(fileIn, fileOut);
	}

	// export

	/**
	 * Export data in container to specified output stream.
	 * 
	 * @param outStream
	 *            exported data format will be written to this stream
	 * @throws Exception
	 *             if there is a problem writing the data
	 */
	public void exportTo(DataOutputStream outStream) throws Exception {
		importStream = null; // assert null state
		exportStream = outStream; // accept assigned output stream
		printStream = null; // assert null state
		writeData();
	}

	/**
	 * Export data in container to specified output filename. Use the parameter
	 * "MULTI" to override the opening of a single file, since some data types
	 * need to build multiple files using export builder.
	 */
	public void exportTo(String outFile) throws Exception {
		if (outFile.equals("MULTI")) {
			this.exportTo((DataOutputStream) null);
		} else {
			DataOutputStream fileOut = new DataOutputStream(new FileOutputStream(outFile));
			this.exportTo(fileOut);
		}
	}

	// component registry

	/**
	 * Generate and return a new instance of import director of the specified
	 * type.
	 */
	public ImportDirector getNewImportDirector(String type) throws BuilderException {
		if (type.equals("SEED") || type.equals("miniSEED") || type.equals("dataless") || type.equals("seed")
				|| type.equals("miniseed")) {
			importDirector = new SeedImportDirector();
		} else {
			throw new BuilderException("Import Director type " + type + " unrecognized");
		}
		return importDirector;
	}

	/**
	 * Generate and return a new instance of export builder of the specified
	 * type.
	 */
	public ExportBuilder getNewExportBuilder(String type) throws Exception {
		if (type.equals("SEED") || type.equals("seed")) {
			exportBuilder = new SeedExportBuilder();
		} else if (type.equals("miniSEED") || type.equals("miniseed")) {
			exportBuilder = new SeedExportBuilder("mini");
		} else if (type.equals("dataless")) {
			exportBuilder = new SeedExportBuilder("dataless");
		} else if (type.equals("SAC") || type.equals("sac")) {
			exportBuilder = new SacExportBuilder();
		} else if (type.equals("AH") || type.equals("ah")) {
			exportBuilder = new AhExportBuilder();
		} else if (type.equals("CSS") || type.equals("css")) {
			exportBuilder = new CssExportBuilder();
		} else {
			throw new BuilderException("Export Builder type " + type + " unrecognized");
		}
		return exportBuilder;
	}

	/**
	 * Append a builder filter to a list of filters and return a handle to it.
	 */
	public SeedBuilderFilter addNewFilter() {
		SeedBuilderFilter newFilter = new SeedBuilderFilter();
		if (builderFilterVec == null)
			builderFilterVec = new Vector(8, 8);
		builderFilterVec.add(newFilter);
		return newFilter;
	}

	/**
	 * Reset filter list and return a new filter.
	 */
	public SeedBuilderFilter getNewFilter() {
		builderFilterVec = new Vector(8, 8); // start with new vector
		return addNewFilter();
	}

	/**
	 * Create a new export template and return a handle to it.
	 */
	public ExportTemplate getNewTemplate() {
		exportTemplate = new ExportTemplate();
		return exportTemplate;
	}

	/**
	 * Create a new template filter and add it to the template filter vector. If
	 * a vector does not exist, create one. Return a handle to the new filter.
	 */
	public SeedBuilderFilter addTemplateFilter() {
		SeedBuilderFilter newFilter = new SeedBuilderFilter();
		if (builderFilterVec == null)
			templateFilterVec = new Vector(8, 8);
		templateFilterVec.add(newFilter);
		return newFilter;
	}

	/**
	 * Force the creation of a new, empty filter vector. A new template filter
	 * is created, added to the vector, and returned.
	 */
	public SeedBuilderFilter getTemplateFilter() {
		templateFilterVec = new Vector(8, 8); // start with new vector
		return addTemplateFilter();
	}

	/**
	 * Get the SEED object container used for this session. This is created by
	 * the assigned concrete Object Builder.
	 */
	public SeedObjectContainer getContainer() {
		return objectContainer;
	}

	/**
	 * Set to TRUE to turn verbose mode on. Verbose mode prints that status of
	 * running to stderr.
	 */
	public void setVerboseMode(boolean mode) {
		verboseMode = mode;
	}

	// private methods

	/**
	 * Read in the data through the import stream.
	 */
	private void readData() throws Exception {
		if (importDirector == null)
			throw new BuilderException("import director not assigned");
		int vecSize = builderFilterVec.size();
		for (int i = 0; i < vecSize; i++) { // assign all filters to builder
			objectBuilder.registerFilter((SeedBuilderFilter) builderFilterVec.get(i));
		}
		importDirector.assignBuilder(objectBuilder); // register the builder
														// with the director
		// begin reading the stream with the construct command
		if (printStream == null) {
			importDirector.construct(importStream); // construct SEED objects
													// silently
		} else {
			importDirector.construct(importStream, printStream, false); // display
																		// SEED
																		// objects
																		// as
																		// ASCII
																		// without
																		// storage
		}
	}

	/**
	 * Write the export data to the export stream. Certain file types affect the
	 * behavior of the export.
	 */
	private void writeData() throws Exception {
		if (exportBuilder == null)
			throw new BuilderException("export builder not assigned");
		String builderType = exportBuilder.getType();
		//
		// alter export streaming behavior here, depending on the export file
		// type
		if (builderType.equals("SAC")) {
			// export builder sets up export stream switching thread
			((SacExportBuilder) exportBuilder).splitToFiles(verboseMode);
		} else {
			if (exportStream == null)
				throw new BuilderException("export stream not assigned");
			exportBuilder.open(exportStream); // connect builder to export
												// stream
		}
		//
		// default behavior for all file types
		exportDirector.assignBuilder(exportBuilder); // assign builder to the
														// director
		if (exportTemplate == null)
			throw new BuilderException("export template not assigned");
		exportDirector.assignTemplate(exportTemplate); // assign export template
														// to director
		int vecSize = templateFilterVec.size();
		for (int i = 0; i < vecSize; i++) { // assign all filters to builder
			// fill template with parameters in export filters
			exportDirector.fillTemplate((SeedBuilderFilter) templateFilterVec.get(i));
		}
		// no filters, then pass null value to fill template with all container
		// contents
		if (vecSize == 0)
			exportDirector.fillTemplate(null);
		exportDirector.construct(); // write export volume
	}

	/**
	 * Method used in shell invocation of Jseedr. String arguments can be
	 * supplied. Run without arguments to get usage message.
	 */
	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				System.err.println("Jseedr -- version " + VERSION + " -- IRIS DMC");
				System.err.println("usage: Jseedr <tag1> <value1> <tag2> <value2> ...");
				System.err.println("where: tagX is one of the following:");
				System.err.println("-i = import filename");
				System.err.println("-I = import file type (default=SEED)");
				System.err.println("-f = import filter filename");
				System.err.println("-r = force import logical record length in bytes");
				System.err.println("-s = serialization storage file (only one allowed)");
				System.err.println("-x = export filename (only one allowed)");
				System.err.println("-X = export file type (default=SEED)");
				System.err.println("-F = export filter filename");
				System.err.println("-l = export volume label");
				System.err.println("-o = export volume institution name");
				// System.err.println("-P = export physical record length in
				// bytes");
				// System.err.println("-R = export logical record length in
				// bytes");
				// System.err.println("-w = export word order (VAX,SUN)
				// (default=SUN)");
				System.err.println("-a = (no value) ASCII print to stdout (overrides export file)");
				System.err.println("-v = (no value) verbose mode");
				System.err.println("\nnote: allowed file types:");
				System.err.println("      SEED, miniSEED, dataless, SAC, RESP, AH, CSS");
				System.err.println("note: filter file format (tab-separated fields):");
				System.err.println("      <name1>\\t<value1>");
				System.err.println("      <name2>\\t<value2>");
				System.err.println("      ....... ........");
				System.err.println("      where: nameX is one of:");
				System.err.println("         'station' -- station ID");
				System.err.println("         'network' -- network code");
				System.err.println("        'location' -- location ID");
				System.err.println("         'channel' -- channel code");
				System.err.println("         'quality' -- data quality flag");
				System.err.println("      'start_time' -- start time (YYYY,DDD,HH:MM:SS.FFFF)");
				System.err.println("        'end_time' -- end time (YYYY,DDD,HH:MM:SS.FFFF)");
				// System.err.println(" 'min_lat' -- minimum latitude value");
				// System.err.println(" 'max_lat' -- maximum latitude value");
				// System.err.println(" 'min_lon' -- minimum longitude value");
				// System.err.println(" 'max_lon' -- maximum longitude value");
				// System.err.println(" 'min_elev' -- minimum elevation value");
				// System.err.println(" 'max_elev' -- maximum elevation value");
				System.exit(1);
			}

			// set up some parameter storage
			// indicate default values here, if there are any
			Vector fileNames = new Vector();
			Vector fileTypes = new Vector();
			Vector filters = new Vector();
			String serialFile = null;
			String exportFile = null;
			String exportType = "SEED";
			Vector exportFilters = new Vector();
			String exportLabel = "Jseedr";
			String instName = "Jseedr User";
			String strPhysRecLen = "32768";
			String strLogRecLen = "4096";
			boolean assignedLogRecLen = false;
			String wordOrder = "SUN";
			boolean asciiOut = false;
			boolean verboseMode = false;
			// step through the arguments, using an index counter
			int counter = 0;
			while (counter < args.length) { // for the next argument...
				String flag = args[counter++];
				if (flag.charAt(0) != '-') {
					System.err.println("Flag argument " + (counter - 1) + " must be proceeded by a '-' character.");
					System.exit(1);
				}
				int char_idx = 1;
				while (char_idx < flag.length()) { // for each subsequent flag
													// character
					switch (flag.charAt(char_idx++)) {
					case '-':
						continue;
					case 'i':
						// check to see if the fileTypes vector is in line with
						// fileNames
						// default to type SEED if not
						if (fileTypes.size() < fileNames.size()) {
							fileTypes.add("SEED");
						}
						// next argument is an import filename
						fileNames.add(args[counter++]);
						break;
					case 'I':
						// next argument is a filetype
						fileTypes.add(args[counter++]);
						break;
					case 'f':
						// next argument is a filter file
						filters.add(args[counter++]);
						break;
					case 's':
						// next argument is the serialization file
						serialFile = args[counter++];
						break;
					case 'x':
						// next argument is the export filename
						exportFile = args[counter++];
						break;
					case 'X':
						// next argument is the export file type
						exportType = args[counter++];
						break;
					case 'F':
						// next argument is an export filter filename
						exportFilters.add(args[counter++]);
						break;
					case 'l':
						// next argument is the export volume label
						exportLabel = args[counter++];
						break;
					case 'o':
						// next argument is the institution name producing the
						// volume
						instName = args[counter++];
						break;
					case 'p':
						// next argument is the output physical record length
						// (String numeric)
						strPhysRecLen = args[counter++];
						break;
					case 'r':
						// next argument is the output logical record length
						// (String numeric)
						strLogRecLen = args[counter++];
						assignedLogRecLen = true;
						break;
					case 'w':
						// next argument is the export volume word order
						wordOrder = args[counter++];
						break;
					case 'a':
						// flags that we print ascii output to stdout
						asciiOut = true;
						break;
					case 'v':
						// flags verbose mode
						verboseMode = true;
						break;
					default:
						System.err.println("Flag value '" + flag.charAt(char_idx - 1) + "' not recognized.");
						System.exit(1);
					}
				}
			}

			// check to see that we have specified some sort of output.
			// SAC generates its own files, so it's not needed then.
			if (exportFile == null && !asciiOut) {
				if (exportType.equals("SAC") || exportType.equals("sac")) {
					exportFile = "MULTI"; // this is a keyword for multiple file
											// output
				} else {
					System.err.println(
							"ERROR: no export file specified (-x option) and not flagged for ASCII out (-a option).");
					System.exit(1);
				}
			}

			// we are done reading the arguments, now let's step through the
			// values we have and set up the run in the proper order
			Jseedr jseedr = null; // this is our run's object instance
			if (serialFile != null) {
				if (verboseMode)
					System.err.println("Starting Jseedr with serialization file...");
				jseedr = new Jseedr(serialFile); // with serialization
			} else {
				if (verboseMode)
					System.err.println("Starting Jseedr...");
				jseedr = new Jseedr(); // w/o serialization
			}
			jseedr.setVerboseMode(verboseMode); // turn on verbosity for the
												// jseedr instance

			// read in Builder Filter files
			for (int i = 0; i < filters.size(); i++) {
				// get next filename
				String filterFile = (String) filters.get(i);
				if (verboseMode)
					System.err.println("\treading import filter file " + filterFile + "...");
				// create a new filter instance
				SeedBuilderFilter newFilter = null;
				if (i == 0)
					newFilter = jseedr.getNewFilter(); // start off with a clean
														// slate
				else
					newFilter = jseedr.addNewFilter(); // append to existing
														// vector
				// open the file and read the parameter list
				BufferedReader filterReader = null;
				try {
					filterReader = new BufferedReader(new FileReader(filterFile));
				} catch (FileNotFoundException e) { // non-fatal exception
					System.err.println("ERROR: File not found: " + filterFile);
					continue;
				}
				// foreach line...
				String filterLine = null;
				while ((filterLine = filterReader.readLine()) != null) {
					// tokenize tab-separated elements
					StringTokenizer filterTok = new StringTokenizer(filterLine, "\t\n");
					String param1 = null;
					String param2 = null;
					if (filterTok.countTokens() > 1) { // need at least two
														// tokens
						param1 = filterTok.nextToken();
						param2 = filterTok.nextToken();
					} else
						continue; // else just go to the next line
					newFilter.addParameter(param1, param2); // add the key/value
															// pair to the
															// filter
				} // next line in the filter file...
				filterReader.close(); // close the file
				filterReader = null; // null reference
			} // next filter file...

			// now begin importing the data files...
			for (int i = 0; i < fileNames.size(); i++) {
				// if there are fileTypes entries still not accounted for, then
				// add the default value to the vector until we match up to the
				// size
				// of fileNames.
				if (i == 0)
					while (fileTypes.size() < fileNames.size())
						fileTypes.add("SEED");
				// let's go through each file name and type, and ingest the data
				String fileName = (String) fileNames.get(i);
				String fileType = (String) fileTypes.get(i);
				if (verboseMode)
					System.err.println("\treading data file " + fileName + " of type " + fileType + "...");
				// establish a new director to read this data type
				ImportDirector impDir = jseedr.getNewImportDirector(fileType);
				// check for forced data length triggers
				if (assignedLogRecLen) {
					if (verboseMode)
						System.err.println("\timport record length forced to " + strLogRecLen + " bytes.");
					impDir.setRecLen(Integer.parseInt(strLogRecLen)); // force
																		// import
																		// record
																		// length
				}
				// have the director import the data file
				try {
					if (asciiOut) { // do we set this up for ASCII output?
						// yes
						DataInputStream fileIn = new DataInputStream(new FileInputStream(fileName));
						DataOutputStream asciiOutStream = new DataOutputStream(System.out);
						jseedr.importFrom(fileIn, asciiOutStream);
					} else {
						// no, we are building an object collection
						jseedr.importFrom(fileName);
					}
				} catch (FileNotFoundException e) { // non-fatal exception
					System.err.println("ERROR: File not found: " + fileName);
					continue;
				}
			}
			if (fileNames.size() == 0) {
				System.err.println("ERROR: no input data files specified (-i option).");
				System.exit(1);
			}

			// if we were doing ascii output, then exit here
			// else, move onto export options

			if (asciiOut)
				System.exit(0);

			// let's get the export template filters, if there are any
			for (int i = 0; i < exportFilters.size(); i++) {
				String filterFile = (String) exportFilters.get(i);
				if (verboseMode)
					System.err.println("\treading export filter file " + filterFile + "...");
				// create a new filter instance
				SeedBuilderFilter newFilter = null;
				if (i == 0)
					newFilter = jseedr.getTemplateFilter(); // start off with a
															// clean slate
				else
					newFilter = jseedr.addTemplateFilter(); // append to
															// existing vector
				// open the file and read the parameter list
				BufferedReader filterReader = null;
				try {
					filterReader = new BufferedReader(new FileReader(filterFile));
				} catch (FileNotFoundException e) { // non-fatal exception
					System.err.println("ERROR: File not found: " + filterFile);
					continue;
				}
				// foreach line...
				String filterLine = null;
				while ((filterLine = filterReader.readLine()) != null) {
					// tokenize tab-separated elements
					StringTokenizer filterTok = new StringTokenizer(filterLine, "\t\n");
					String param1 = null;
					String param2 = null;
					if (filterTok.countTokens() > 1) { // need at least two
														// tokens
						param1 = filterTok.nextToken();
						param2 = filterTok.nextToken();
					} else
						continue; // else just go to the next line
					newFilter.addParameter(param1, param2); // add the key/value
															// pair to the
															// filter
				} // next line in the filter file...
				filterReader.close(); // close the file
				filterReader = null; // null reference
			}
			// get an export template for designating what we want to export
			jseedr.getNewTemplate();
			// now get the export filename, establish the proper builder for the
			// file
			// and write out the data
			ExportBuilder expBuilder = jseedr.getNewExportBuilder(exportType);
			//
			// perform any export builder mutations here
			if (expBuilder.getType().equals("SEED")) {
				if (exportLabel.length() > 0)
					((SeedExportBuilder) expBuilder).setVolumeLabel(exportLabel); // volume
																					// label
				if (instName.length() > 0)
					((SeedExportBuilder) expBuilder).setOrganizationName(instName); // institution
																					// name
			}
			//
			if (!exportFile.equals("MULTI") && verboseMode)
				System.err.println("\twriting to data file " + exportFile + " of type " + exportType + "...");
			jseedr.exportTo(exportFile);
		} catch (Exception e) { // generic exception catcher
			System.err.println("Caught exception: " + e);
			e.printStackTrace();
		}

		System.exit(0); // exit normally
	}

	// instance variables

}
