package dk.statsbiblioteket.chaos.metadata.migrate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

import dk.statsbiblioteket.chaos.metadata.migrate.exception.MigrationInitializationException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionConnectToDOMSException;
import dk.statsbiblioteket.doms.central.InvalidCredentialsException;
import dk.statsbiblioteket.doms.central.InvalidResourceException;
import dk.statsbiblioteket.doms.central.MethodFailedException;

public class CommandLineHandler {

	private static Logger logger = Logger.getLogger(CommandLineHandler.class);

	public static void main(String[] args) throws InvalidCredentialsException, MethodFailedException, InvalidResourceException, ParseException {
		if (args.length == 0) {
			printCLIParameters();
			System.exit(1);
		}
		List<String> shardPids = null;
		List<String> sourceFilesToFilterOut = null;
		String propertyFilename = null;
		if ((args.length == 4) && args[0].equalsIgnoreCase("-shardpidfile") && args[2].equalsIgnoreCase("-propfile")) {
			String pidfilename = args[1];
			propertyFilename = args[3];
			shardPids = extractShardPidsFromTextFile(pidfilename);
			sourceFilesToFilterOut = new ArrayList<String>();
		} else if ((args.length == 6) && args[0].equalsIgnoreCase("-shardpidfile") && args[2].equalsIgnoreCase("-filterfile") && args[4].equalsIgnoreCase("-propfile")) {
			String pidfilename = args[1];
			String filterfilename = args[3];
			propertyFilename = args[5];
			shardPids = extractShardPidsFromTextFile(pidfilename);
			sourceFilesToFilterOut = extractFilterFilenamesFromTextFile(filterfilename);
		} else if ((args.length == 3) && args[0].equalsIgnoreCase("-stdin") && args[1].equalsIgnoreCase("-propfile")) {
			propertyFilename = args[2];
		    try {
				shardPids = extractShardPidsFromStdIn();
			} catch (IOException e) {
				System.err.println("Could not read stdin. Cause: " + e.getMessage());
				System.exit(2);
			};
		} else {
			System.err.println("Unexpected number of arguments: " + args.length);
			printCLIParameters();
			System.exit(1);
		}
		try {
			new DOMSMetadataMigrationService(propertyFilename).extractAndExportMetadata(shardPids, sourceFilesToFilterOut);
		} catch (MigrationInitializationException e) {
			System.err.println("Could not read property file. Cause: " + e.getMessage());
			logger.error("Could not read property file. Cause: " + e.getMessage(), e);
			System.exit(2);
		} catch (DOMSMetadataExtractionConnectToDOMSException e) {
			System.err.println("Could not Connect to DOMS. Cause: " + e.getMessage());
			logger.error("Could not Connect to DOMS." + e.getMessage(), e);
			System.exit(3);
		}
	}

	private static void printCLIParameters() {
		System.err.println("Parameter required:");
		System.err.println(" -stdin -propfile <path_to_property_file>");
		System.err.println(" -shardpidfile <filename_of_pid_list> -propfile <path_to_property_file>");
		System.err.println(" -shardpidfile <filename_of_pid_list> -filterfile <filename_of_filter_list> -propfile <path_to_property_file>");
		System.err.println("");
		System.err.println("Shardpidfile : Contains list of shard uuids.");
		System.err.println("Filterfile   : Contains list of source file names. Programs originating from these are excluded from the export.");
		System.err.println("Propfile     : Contains path to property file.");
	}

	private static List<String> extractShardPidsFromStdIn() throws IOException {
		List<String> stdInList = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s;
		while ((s = in.readLine()) != null && s.length() != 0) {
			stdInList.add(s);
		}
		return stdInList;
	}

	public static List<String> extractShardPidsFromTextFile(String filename) {
		List<String> shardPids;
		File shardPidFile = new File(filename);
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(shardPidFile));
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file: " + shardPidFile.getAbsolutePath());
			System.exit(2);
		}
		String shardPid;
		shardPids = new ArrayList<String>(); 
		try {
			while ((shardPid = in.readLine()) != null) {
				shardPids.add(shardPid);
			}
		} catch (IOException e) {
			System.err.println("Could not read file: " + shardPidFile.getAbsolutePath());
			System.exit(3);
		}
		return shardPids;
	}

	protected static List<String> extractFilterFilenamesFromTextFile(String filename) {
		List<String> filterFilenames;
		File filterFilenameFile = new File(filename);
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(filterFilenameFile));
		} catch (FileNotFoundException e) {
			System.err.println("Could not find file: " + filterFilenameFile.getAbsolutePath());
			System.exit(2);
		}
		String filenameToFilter;
		filterFilenames = new ArrayList<String>(); 
		try {
			while ((filenameToFilter = in.readLine()) != null) {
				filterFilenames.add(filenameToFilter);
			}
		} catch (IOException e) {
			System.err.println("Could not read file: " + filterFilenameFile.getAbsolutePath());
			System.exit(3);
		}
		return filterFilenames;
	}

}
