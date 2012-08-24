package dk.statsbiblioteket.chaos.metadata.migrate.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.BasicConfigurator;

import dk.statsbiblioteket.chaos.metadata.migrate.extraction.TestObjectFactory;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgram;
import dk.statsbiblioteket.chaos.metadata.migrate.filter.FilterService;

import junit.framework.TestCase;

public class FilterRadioMetadataTest extends TestCase {

	public FilterRadioMetadataTest(String name) {
		super(name);
		BasicConfigurator.resetConfiguration();
		BasicConfigurator.configure();
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFilterOutRadioProgramsWithClipFromFileList() throws Exception {
		List<String> filenamesToFilter = new ArrayList<String>();
		filenamesToFilter.add("drp1_88.100_DR-P1_pcm_20080508045601_20080509045501.mp3");
		List<RadioProgram> radioPrograms = new ArrayList<RadioProgram>();
		// Test that a program does not get filtered out
		RadioProgram radioProgramToPassFilter = new TestObjectFactory().createDefaultRadioProgramMetadata();
		radioPrograms.add(radioProgramToPassFilter);
		List<RadioProgram> filteringResult = FilterService.filterOutRadioProgramsWithClipFromFileList(radioPrograms, filenamesToFilter);
		assertEquals("Expecting all radioprograms", radioPrograms, filteringResult);
		// Test that programs can be filtered out
		RadioProgram radioProgramToGetFiltered = new TestObjectFactory().createRadioProgramMetadataWithTwoClips();
		radioPrograms.add(radioProgramToGetFiltered);
		filteringResult = FilterService.filterOutRadioProgramsWithClipFromFileList(radioPrograms, filenamesToFilter);
		assertEquals("Expecting one of two programs getting filtered out", 1, filteringResult.size());
		assertEquals("Testing that filtered program was the correct one", radioProgramToPassFilter, filteringResult.get(0));
	}
}
