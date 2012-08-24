package dk.statsbiblioteket.chaos.metadata.migrate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import dk.statsbiblioteket.chaos.metadata.migrate.exception.MigrationInitializationException;
import dk.statsbiblioteket.chaos.metadata.migrate.export.DOMSMetadataXMLFileOutputService;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.DOMSMetadataExtractor;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionConnectToDOMSException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgram;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgramSearchResultItem;
import dk.statsbiblioteket.chaos.metadata.migrate.filter.FilterService;
import dk.statsbiblioteket.doms.central.InvalidCredentialsException;
import dk.statsbiblioteket.doms.central.InvalidResourceException;
import dk.statsbiblioteket.doms.central.MethodFailedException;

public class DOMSMetadataMigrationService {

	private static Logger logger = Logger.getLogger(DOMSMetadataMigrationService.class);
	private DOMSMetadataExtractor domsMetadataExtractor;
	private String exportLogFile;
	private int pageSize;
	
	/**
	 * TEST constructor
	 * 
	 * @throws MigrationInitializationException
	 */
	public DOMSMetadataMigrationService(Properties properties) throws MigrationInitializationException {
		domsMetadataExtractor = new DOMSMetadataExtractor(properties);
		// assume log4j has been initialized
	}

	public DOMSMetadataMigrationService(String propertyFilename) throws MigrationInitializationException {
		Properties properties = new Properties();
	    try {
			properties.load(new FileInputStream(propertyFilename));
		} catch (IOException e) {
			throw new MigrationInitializationException("Unable to load configuraiton file.", e);
		}
		String log4jConfigurationFilename = properties.getProperty("log4jPropertyFile");
		System.err.println("Looking for log4j configuraiton in file:\n" + new File(log4jConfigurationFilename).getAbsolutePath());
		DOMConfigurator.configure(log4jConfigurationFilename);
		domsMetadataExtractor = new DOMSMetadataExtractor(properties);
		exportLogFile = properties.getProperty("exportLogFile", "export.log");
		pageSize = Integer.parseInt(properties.getProperty("extractionPageSize", "1000"));
	}

	public void migrateMetadataToMCM(Date updateProgramsSince) {
		try {
			
			List<RadioProgramSearchResultItem> searchResultItems = domsMetadataExtractor.fetchRadioProgramMetadataUpdatedSince(updateProgramsSince);
			
		} catch (DOMSMetadataExtractionConnectToDOMSException e) {
			logger.error("Unable to connect to DOMS.");
			throw new RuntimeException("Unable to connect to DOMS. Aborting.", e);
		}
	}
	
	/**
	 * Extracts metadata of radio programs from DOMS related to the shartPids given as input.
	 * 
	 * Filters out radio programs which source files are based on files from the file list given as second argument.
	 * 
	 * Write metadata of remaining radio programs to XML files. One file foreach radio program.
	 * 
	 * @param shardPids List of shard uuid's
	 * @param sourceFilesToFilterOut List of filenames
	 * @throws DOMSMetadataExtractionConnectToDOMSException 
	 */
	public void extractAndExportMetadata(List<String> shardPids, List<String> sourceFilesToFilterOut) throws DOMSMetadataExtractionConnectToDOMSException {
		BufferedWriter out = null;
		try {
			File logFile = new File(exportLogFile);
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile),"UTF8"));
			int okRadioPrograms = 0;
			int failedRadioPrograms = 0;
			int offset = 0;
			int page = 0;
			while (offset < shardPids.size()) {
				int toIndex = Math.min(shardPids.size(), offset + pageSize);
				List<String> pageShardPids = shardPids.subList(offset, toIndex );
				logger.info("Extracting PID's from DOMS - [" + offset + ";" + toIndex + "]");
				List<RadioProgramSearchResultItem> radioProgramSearchResult = domsMetadataExtractor.fetchRadioProgramMetadataFromShardPids(pageShardPids, true);
				logger.info("Filtering out programs based on source files in clips.");
				FilterService.filterResult(sourceFilesToFilterOut, radioProgramSearchResult);
				logger.info("Exporting radio programs to XML files.");
				for (RadioProgramSearchResultItem radioProgramSearchResultItem : radioProgramSearchResult) {
					if (radioProgramSearchResultItem.isReadyForExport()) {
						RadioProgram radioProgram = radioProgramSearchResultItem.getRadioProgram();
						out.write("Exporting radioprogram: " + radioProgram + "\n");
						DOMSMetadataXMLFileOutputService.writeRadioProgramMetadataToXMLFile("data", radioProgram);
						okRadioPrograms++;
					} else {
						RadioProgram radioProgram = radioProgramSearchResultItem.getRadioProgram();
						out.write("Not exporting radioprogram: " + radioProgram + "\n");
						out.write("Reason(s): " + radioProgramSearchResultItem.getReasonForNotReadyForExport() + "\n");
						failedRadioPrograms++;
					}
				}
				out.write("Page " + page + ": Successfully exported number of programs : " + okRadioPrograms + " total: " + (okRadioPrograms + failedRadioPrograms) + "\n");
				out.write("Page " + page + ": Failed to export number of programs      : " + failedRadioPrograms + "\n");
				page++;
				offset = page*pageSize;
			}
			out.write("Finished export. Tatal pages " + page + " of size " + pageSize + "\n");
			out.write("Successfully exported number of programs : " + okRadioPrograms + "\n");
			out.write("Failed to export number of programs      : " + failedRadioPrograms + "\n");
			out.close();
			logger.info("Export to XML files done.");
		} catch (UnsupportedEncodingException e) {
			logger.error("Unable to create log file.", e);
			if (out!=null) {
				try {
					out.close();
				} catch (IOException e1) {
					logger.error("Unable to close writer: " + out);
					throw new RuntimeException("Unable to close writer", e1);
				}
			}
		} catch (FileNotFoundException e) {
			logger.error("Unable to create log file.", e);
			if (out!=null) {
				try {
					out.close();
				} catch (IOException e1) {
					logger.error("Unable to close writer: " + out);
					throw new RuntimeException("Unable to close writer", e1);
				}
			}
		} catch (IOException e) {
			logger.error("Unable to create log file. Stop execution.", e);
			if (out!=null) {
				try {
					out.close();
				} catch (IOException e1) {
					logger.error("Unable to close writer: " + out);
					throw new RuntimeException("Unable to close writer", e1);
				}
			}
		}
	}

	private List<String> extractChannelList(String channelListString) {
		String[] channelArray = channelListString.split(";");
		List<String> channelList = new ArrayList<String>();
		for (String channelID : channelArray) {
			channelList.add(channelID);
			logger.trace("Channel extracted from property: " + channelID);
		}
		return channelList;
	}
}
