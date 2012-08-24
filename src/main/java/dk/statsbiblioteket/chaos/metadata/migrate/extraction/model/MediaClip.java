package dk.statsbiblioteket.chaos.metadata.migrate.extraction.model;

import java.util.Date;

public abstract class MediaClip implements Comparable<MediaClip> {

	public String sourceFilename;
	public Date clipStart;
	public long clipLength;
	
	public MediaClip(String sourceFilename, Date clipStart, long clipLength) {
		this.sourceFilename = sourceFilename;
		this.clipStart = clipStart;
		this.clipLength = clipLength;
	}

	public abstract MediaTypeEnum getMediaType();

	@Override
	public String toString() {
		return "MediaClip" +
				" [mediaType=" + getMediaType() + 
				", sourceFilename=" + sourceFilename + 
				", clipStart=" + clipStart + 
				", clipLength=" + clipLength + 
				"]";
	}

	@Override
	public int compareTo(MediaClip other) {
		int result;
		if (this.clipStart == null) {
			if (other.clipStart == null) {
				result = 0;
			} else {
				result = -1; // null is less than anything not-null
			}
		} else {
			if (other.clipStart == null) {
				result = 1; // anything not-null is larger than null
			} else {
				result = this.clipStart.compareTo(other.clipStart);
			}
		}
		return result;
	}
}
