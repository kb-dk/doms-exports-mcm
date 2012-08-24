package dk.statsbiblioteket.chaos.metadata.migrate.extraction.model;

import java.util.Date;

public class MediaClipWav extends MediaClip {
	
	public MediaClipWav(String sourceFilename, Date clipStart, long clipLength) {
		super(sourceFilename, clipStart, clipLength);
	}

	@Override
	public MediaTypeEnum getMediaType() {
		return MediaTypeEnum.WAV;
	}

}