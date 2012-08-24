package dk.statsbiblioteket.chaos.metadata.migrate.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgram;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.MediaClipWav;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgramSearchResultItem;

public class FilterService {

	private static Logger logger = Logger.getLogger(FilterService.class);

	/**
	 * Create new list with radio programs that are not based on the source files in the given list of filenames.
	 * 
	 * @param radioPrograms List with all radioprograms
	 * @param sourceFilesToFilterOut List of source filenames that filters out radio programs 
	 * @return List of radio programs that are not based on input filenames
	 */
	public static List<RadioProgram> filterOutRadioProgramsWithClipFromFileList(List<RadioProgram> radioPrograms, List<String> sourceFilesToFilterOut) {
		List<RadioProgram> radioProgramsNotFromSourceFiles = new ArrayList<RadioProgram>();
		SortedSet<String> sortedSourceFilenames = new TreeSet<String>(sourceFilesToFilterOut);
		for (RadioProgram radioProgram : radioPrograms) {
			List<String> filterReasons = areClipsBasedOnFilterFiles(sortedSourceFilenames, radioProgram);
			if (filterReasons.size() == 0) {
				radioProgramsNotFromSourceFiles.add(radioProgram);
			} else {
				String filterReason = "Clip based on source file to filter out.";
				for (String reason: filterReasons) {
					filterReason += " - " + reason;
				}
				logger.info("Filtered out program: " + radioProgram.toString());
				logger.info("Reason: " + filterReason);
			}
		}
		return radioProgramsNotFromSourceFiles;
	}

	public static void filterResult(List<String> sourceFilesToFilterOut, List<RadioProgramSearchResultItem> radioProgramSearchResults) {
		SortedSet<String> sortedSourceFilenames = new TreeSet<String>(sourceFilesToFilterOut);
		for (RadioProgramSearchResultItem radioProgramSearchResult : radioProgramSearchResults) {
			if (radioProgramSearchResult.isExtractionSuccessful()) {
				List<String> filterReasons = areClipsBasedOnFilterFiles(sortedSourceFilenames, radioProgramSearchResult.getRadioProgram());
				if (filterReasons.size() > 0) {
					String filterReason = "Clip based on source file to filter out.";
					for (String reason: filterReasons) {
						filterReason += " - " + reason;
					}
					radioProgramSearchResult.filteredOut(filterReason);
					logger.info("Filtered out program: " + radioProgramSearchResult.getRadioProgram());
					logger.info("Reason: " + filterReason);
				}
			}
		}
	}

	private static List<String> areClipsBasedOnFilterFiles(SortedSet<String> sortedSourceFilenames, RadioProgram radioProgram) {
		List<String> filterReasons = new ArrayList<String>();
		for (MediaClipWav radioClip : radioProgram.subClips) {
			// Convert original filename:
			// drp1_88.100_DR-P1_pcm_20080510045601_20080511045502_encoder5-2.wav to
			// drp1_88.100_DR-P1_pcm_20080510045601_20080511045502.mp3 (filenames used by the first SB-CHAOS files)
			String sourceFilename = radioClip.sourceFilename.replaceAll("_encoder.-.\\.wav", ".mp3");
			if (sortedSourceFilenames.contains(sourceFilename)) {
				filterReasons.add("Clip file: " + radioClip.sourceFilename + " is based on filter file: " + sourceFilename);
			}
		}
		return filterReasons;
	}
}
