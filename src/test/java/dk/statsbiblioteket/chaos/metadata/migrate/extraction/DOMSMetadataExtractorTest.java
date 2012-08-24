package dk.statsbiblioteket.chaos.metadata.migrate.extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import dk.statsbiblioteket.chaos.metadata.migrate.CommandLineHandler;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionConnectToDOMSException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionParsePBCoreException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.PBCoreProgramMetadata;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgram;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.MediaClipWav;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgramSearchResultItem;
import dk.statsbiblioteket.chaos.metadata.migrate.model.BESClippingConfiguration;
import dk.statsbiblioteket.doms.central.InvalidCredentialsException;
import dk.statsbiblioteket.doms.central.InvalidResourceException;
import dk.statsbiblioteket.doms.central.MethodFailedException;
import dk.statsbiblioteket.doms.central.RecordDescription;

import junit.framework.TestCase;

public class DOMSMetadataExtractorTest extends TestCase {

	private static List<RadioProgram> radioPrograms = null;
	private static DOMSMetadataExtractor domsMetadataFinder;
	private static List<String> testShardPids;

	private String domsWSAPIEndpointUrlString="http://alhena:7880/centralWebservice-service/central/";
	private String userName="fedoraReadOnlyAdmin";
	private String password="fedoraReadOnlyPass";

	long besConfiguredRadioStartOffset=-20;
	long besConfiguredRadioEndOffset=20;
	private BESClippingConfiguration besConfiguration = new BESClippingConfiguration(besConfiguredRadioStartOffset, besConfiguredRadioEndOffset);
	
	private Level testLogLevel;



	public DOMSMetadataExtractorTest(String name) {
		super(name);
		BasicConfigurator.resetConfiguration();
		BasicConfigurator.configure();
		testLogLevel = Level.INFO;
		System.out.println("Log level set to: " + testLogLevel);
		Logger.getRootLogger().setLevel(testLogLevel);
		domsMetadataFinder = new DOMSMetadataExtractor(
				domsWSAPIEndpointUrlString,
				userName,
				password,
				besConfiguredRadioStartOffset, 
				besConfiguredRadioEndOffset);
	}

	protected synchronized void setUp() throws Exception {
		super.setUp();
	}

	private void fetchMetadataIfNotAlreadyDone()
			throws InvalidCredentialsException, InvalidResourceException,
			MethodFailedException, DOMSMetadataExtractionConnectToDOMSException {
		if (radioPrograms == null) {
			List<String> fullListOfShardPids = CommandLineHandler.extractShardPidsFromTextFile("src/test/resources/alhena_doms_radio_shard_pid.txt");
			testShardPids = new ArrayList<String>(fullListOfShardPids.subList(0, Math.min(10, fullListOfShardPids.size())));
			List<RadioProgramSearchResultItem> searchResultItems = domsMetadataFinder.fetchRadioProgramMetadataFromShardPids(testShardPids, true);
			radioPrograms = new ArrayList<RadioProgram>();
			for (RadioProgramSearchResultItem radioProgramSearchResultItem : searchResultItems) {
				if (radioProgramSearchResultItem.isReadyForExport()) {
					radioPrograms.add(radioProgramSearchResultItem.getRadioProgram());
				}
			}
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFindProgramsFromShardPidList() throws InvalidCredentialsException, MethodFailedException, InvalidResourceException, ParseException, DOMSMetadataExtractionConnectToDOMSException {
		fetchMetadataIfNotAlreadyDone();
		assertTrue("Empty result not expected", !radioPrograms.isEmpty());
		System.out.println("Found radio programs:");
		int i=0;
		for (RadioProgram radioProgramMetadata : radioPrograms) {
			System.out.println(i + " - " + radioProgramMetadata);
			i++;
		}
	}

	public void testGetClipStartDateTest() throws Exception {
		RadioProgram radioProgramMetadata = new TestObjectFactory().createDefaultRadioProgramMetadata();
		Date clipStartDate = radioProgramMetadata.subClips.get(0).clipStart;
		assertEquals("Expected start date of Radioavis", "Sat May 10 00:00:00 CEST 2008", clipStartDate.toString());
		Date programClipStartDate = radioProgramMetadata.getClipStartTime();
		assertEquals("Expected start date of Radioavis", "Fri May 09 23:59:40 CEST 2008", programClipStartDate.toString());
	}

	public void testGetClipEndDateTest() throws Exception {
		RadioProgram radioProgramMetadata = new TestObjectFactory().createDefaultRadioProgramMetadata();
		Date clipStartDate = radioProgramMetadata.subClips.get(0).clipStart;
		assertEquals("Expected start date of Radioavis", "Sat May 10 00:00:00 CEST 2008", clipStartDate.toString());
		Date programClipStartDate = radioProgramMetadata.getClipStartTime();
		assertEquals("Expected start date of Radioavis", "Fri May 09 23:59:40 CEST 2008", programClipStartDate.toString());
	}

	public void testGetShardMetadataSeveralSourceFiles() throws Exception {
		RadioProgram radioProgramMetadata = new TestObjectFactory().createRadioProgramWithNoClipInfo();
		String shardMetadata = new TestObjectFactory().createShardInfoXMLTextDoubleClip();
		List<MediaClipWav> clips = domsMetadataFinder.extractRadioClipMetadata(shardMetadata);
		for (MediaClipWav radioClip : clips) {
			radioProgramMetadata.addRadioClip(radioClip);
		}
		assertEquals("Expected number of radio clips", 2, radioProgramMetadata.subClips.size());
		assertEquals("Expected clip start", "Fri May 09 04:45:00 CEST 2008", radioProgramMetadata.subClips.get(0).clipStart.toString());
		assertEquals("Expected clip lenght", 601, radioProgramMetadata.subClips.get(0).clipLength);
		Date endTest = new Date(radioProgramMetadata.subClips.get(0).clipStart.getTime() + radioProgramMetadata.subClips.get(0).clipLength*1000);
        //SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS");
		assertEquals("Expected sub-clip start", "Fri May 09 04:55:01 CEST 2008", endTest.toString());//formatter.format(endTest));
		assertEquals("Expected sub-clip start", "Fri May 09 04:56:02 CEST 2008", radioProgramMetadata.subClips.get(1).clipStart.toString());
		assertEquals("Expected sub-clip lenght", 238, radioProgramMetadata.subClips.get(1).clipLength);
		assertEquals("Expected clip lenght including offset", 238+601+20+20, radioProgramMetadata.getTotalClipDurationInSeconds());
	}

	public void testFindAllRadioavis () throws InvalidCredentialsException, MethodFailedException, InvalidResourceException, ParseException, DOMSMetadataExtractionConnectToDOMSException {
		fetchMetadataIfNotAlreadyDone();
		int i = 0;
		for (RadioProgram radioProgramMetadata : radioPrograms) {
			if (radioProgramMetadata.pbcoreProgramMetadata.titel.contains("Radioavis")) {
				System.out.println( i++ + " - " + radioProgramMetadata);
			}
			
		}
	}
	
	public void testExtractMetadataFromPBCore() throws DOMSMetadataExtractionParsePBCoreException {
		StringBuffer pBCoreStringBuffer = new StringBuffer();
		File shardPidFile = new File("src/test/resources/pbCoreTestdata.xml");
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(shardPidFile));
		} catch (FileNotFoundException e) {
			fail("Could not find file: " + shardPidFile.getAbsolutePath());
		}
		String lineOfText;
		try {
			while ((lineOfText = in.readLine()) != null) {
				pBCoreStringBuffer.append(lineOfText);
			}
		} catch (IOException e) {
			System.err.println("Could not read file: " + shardPidFile.getAbsolutePath());
			System.exit(3);
		}
		String pBCore = pBCoreStringBuffer.toString();
		System.out.println("PBCore test output: \n" + pBCore);
		String shardPid = "1234567890abcdef";
		RadioProgram radioProgram = new RadioProgram(shardPid, besConfiguration);
		PBCoreProgramMetadata pbcoreProgramMetadata = domsMetadataFinder.extractMetadataFromPBCore(shardPid, pBCore);
		radioProgram.setPbcoreProgramMetadata(pbcoreProgramMetadata);
		assertEquals("RadioProgram" +
				" [uuid : 1234567890abcdef" +
				", pbcoreMetadata : PBCoreProgramMetadata" +
				" [channel : drp1    " +
				", titel : Radioavis                     " +
				", originaltitel : Radioavis originaltitel       " +
				", episodetitel : Radioavis episodetitel        " +
				", start : 2008-05-10 18:00:00" +
				", end : 2008-05-10 18:03:00" +
				", kortomtal : Vigtigste emner               " +
				", langomtale1 : Dagens vigtigste emner        " +
				", langomtale2 : Dagens vigtigste nyheder      " +
				", forfattere : Jes Dorph                     " +
				", medvirkende : Statsministeren               " +
				", instruktion : Jes Dorph og Co.              ]" +
				", clip [no-clip-info]]"
				, radioProgram.toString());
	}
	
	public void testFetchUpdatedShardPids() throws ParseException, DOMSMetadataExtractionException, DOMSMetadataExtractionConnectToDOMSException {
		Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01");
		System.out.println("Finding programs updated since: " + date);
		List<String> radioProgramShardPids = domsMetadataFinder.fetchUpdatedShardPidsPagedAsString(date, 0, 1);
		assertTrue("Expecting one change.", radioProgramShardPids.size()>0);
	}

	public void testFetchRadioProgramMetadataFromShardPid() throws ParseException, DOMSMetadataExtractionException, DOMSMetadataExtractionConnectToDOMSException {
		Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2000-01-01");
		System.out.println("Finding pids of programs updated since: " + date);
		List<RecordDescription> radioProgramShardPids = domsMetadataFinder.fetchUpdatedShardPidsPaged(date, 0, 20);
		System.out.println("Found pids: " + radioProgramShardPids);
		assertTrue("Expecting at least one change.", radioProgramShardPids.size()>0);
		List<RadioProgramSearchResultItem> radioProgramSearchResult = domsMetadataFinder.fetchRadioProgramMetadataFromShardPids(radioProgramShardPids);
		System.out.println("Fetched radio program: " + radioProgramSearchResult);
		System.out.println("Succesfully fetched radio program: ");
		int SuccessNr = 0;
		for (RadioProgramSearchResultItem radioProgramSearchResultItem : radioProgramSearchResult) {
			if (radioProgramSearchResultItem.isReadyForExport()) {
				System.out.println("Success: " + radioProgramSearchResultItem);
				SuccessNr++;
			}
		}
		System.out.println("Failed fetched radio program: ");
		int failedNr = 0;
		for (RadioProgramSearchResultItem radioProgramSearchResultItem : radioProgramSearchResult) {
			if (!radioProgramSearchResultItem.isReadyForExport()) {
				System.out.println("Failed: " + radioProgramSearchResultItem);
				failedNr++;
			}
		}
		assertTrue("Expecting at least one succesfully imported.", SuccessNr>0);
	}
}
