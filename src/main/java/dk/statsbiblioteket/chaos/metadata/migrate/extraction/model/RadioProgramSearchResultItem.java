package dk.statsbiblioteket.chaos.metadata.migrate.extraction.model;

import java.util.ArrayList;
import java.util.List;

public class RadioProgramSearchResultItem {

	private RadioProgram radioProgram;
	
	private boolean isFilteredOut;
	private boolean isExtractionFailed;
	private List<String> statusMessages;

	private RadioProgramSearchResultItem() {
		super();
		this.isFilteredOut = false;
		this.isExtractionFailed = false;
		this.statusMessages = new ArrayList<String>();
	}
	
	public RadioProgramSearchResultItem(RadioProgram radioProgramMetadata) {
		this();
		this.radioProgram = radioProgramMetadata;
	}

	public RadioProgram getRadioProgram() {
		return radioProgram;
	}
	
	public boolean validate() {
		boolean validates = true;
		if (radioProgram.subClips.isEmpty()) {
			validates = false;
			extractionFailed("Validation failed: Empty sub-clips");
		}
		if (radioProgram.shardPid == null) {
			validates = false;
			extractionFailed("Validation failed: No shard Pid");
		}
		if (radioProgram.pbcoreProgramMetadata == null) {
			validates = false;
			extractionFailed("Validation failed: No PBCoreMetadata found");
		} else {
			if (radioProgram.pbcoreProgramMetadata.channel == null || radioProgram.pbcoreProgramMetadata.titel == null || radioProgram.pbcoreProgramMetadata.start == null || radioProgram.pbcoreProgramMetadata.end == null) {
				validates = false;
				extractionFailed("Validation failed: Incomplete PBCoreMetadata");
			}
		}
		return validates;
	}


	public boolean isExtractionSuccessful() {
		return !isExtractionFailed;
	}

	private boolean isExtractionFailed() {
		return isExtractionFailed;
	}

	public boolean isFilteredOut() {
		return isFilteredOut;
	}

	public void extractionFailed(String failStatus) {
		this.isExtractionFailed = true;
		this.statusMessages.add(failStatus);
	}

	public void filteredOut(String filterMessage) {
		this.isFilteredOut = true;
		this.statusMessages.add(filterMessage);
	}

	public boolean isReadyForExport() {
		return !isFilteredOut() && !isExtractionFailed();
	}

	public String getReasonForNotReadyForExport() {
		String reasons = "";
		int i = 1;
		for (String reason : statusMessages) {
			if (i>1) {
				reasons+="\n";
			}
			reasons+= "Reason " + i + ": " + reason;
		}
		return reasons;
	}
}
