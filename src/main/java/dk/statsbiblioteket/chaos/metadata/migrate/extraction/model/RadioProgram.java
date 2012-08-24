package dk.statsbiblioteket.chaos.metadata.migrate.extraction.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataIncompleteDataException;
import dk.statsbiblioteket.chaos.metadata.migrate.model.BESClippingConfiguration;
import dk.statsbiblioteket.doms.central.RecordDescription;

public class RadioProgram implements Comparable<RadioProgram> {

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public String shardPid;
	public PBCoreProgramMetadata pbcoreProgramMetadata;
	public List<MediaClipWav> subClips;
	private BESClippingConfiguration besConfiguration;
	private long updatedInDOMSDate;
	
	private RadioProgram() {
		this.subClips = new ArrayList<MediaClipWav>();
	}

	public RadioProgram(String shardPid) {
		this();
		this.shardPid = shardPid;
	}

	public RadioProgram(String shardPid, BESClippingConfiguration besConfiguration) {
		this(shardPid);
		this.besConfiguration = besConfiguration;
	}

	public RadioProgram(String shardPid, long updatedInDOMSDate) {
		this(shardPid);
		this.updatedInDOMSDate = updatedInDOMSDate;
	}

	public RadioProgram(String shardPid, long updatedInDOMSDate, BESClippingConfiguration besConfiguration) {
		this(shardPid, updatedInDOMSDate);
		this.besConfiguration = besConfiguration;
	}

	public void addRadioClip(MediaClipWav radioClip) {
		subClips.add(radioClip);
	}

	public long getUpdatedInDOMSDate() {
		return updatedInDOMSDate;
	}
	
	public PBCoreProgramMetadata getPbcoreProgramMetadata() {
		return pbcoreProgramMetadata;
	}

	public void setPbcoreProgramMetadata(PBCoreProgramMetadata pbcoreProgramMetadata) {
		this.pbcoreProgramMetadata = pbcoreProgramMetadata;
	}

	public String getPresentationFilename() {
		return shardPid.substring(5, shardPid.length()) + ".mp3";
	}

	public long getTotalClipDurationInSeconds() {
		long duration = 0;
		for (MediaClipWav clip : subClips) {
			duration += clip.clipLength;
		}
		duration += -besConfiguration.radioStartOffset + besConfiguration.radioEndOffset;
		return duration;
	}

	public Date getClipStartTime() throws DOMSMetadataIncompleteDataException {
		if (subClips.isEmpty()) {
			throw new DOMSMetadataIncompleteDataException("Clips expected: " + subClips);
		}
		Collections.sort(subClips);
		return new Date(subClips.get(0).clipStart.getTime() + besConfiguration.radioStartOffset*1000);
	}

	public Date getClipEndTime() throws DOMSMetadataIncompleteDataException {
		if (subClips.isEmpty()) {
			throw new DOMSMetadataIncompleteDataException("Clips expected: " + subClips);
		}
		Date clipEndTime = new Date(getClipStartTime().getTime() + getTotalClipDurationInSeconds()*1000);
		return clipEndTime;
	}

	public String toString() {
		String s = this.getClass().getSimpleName()
			+ " [uuid : " + shardPid 
			+ ", pbcoreMetadata : " + pbcoreProgramMetadata;
		try {
			s += ", clip [" + 
				(getClipStartTime()!=null ? sdf.format(getClipStartTime()): getClipStartTime()) + " : " + 
				(getClipEndTime()!=null ? sdf.format(getClipEndTime()): getClipEndTime()) + "]";
			s += ", sub-clips : " + subClips;
		} catch (DOMSMetadataIncompleteDataException e) {
			s += ", clip [no-clip-info]"; 
		}
		s += "]";
		return s;
	}

	@Override
	public int compareTo(RadioProgram other) {
		int result;
		if (this.pbcoreProgramMetadata.start == null) {
			if (other.pbcoreProgramMetadata.start == null) {
				result = 0;
			} else {
				result = -1; // null is less than anything not-null
			}
		} else {
			if (other.pbcoreProgramMetadata.start == null) {
				result = 1; // anything not-null is larger than null
			} else {
				result = this.pbcoreProgramMetadata.start.compareTo(other.pbcoreProgramMetadata.start);
			}
		}
		return result;
	}
}
