package dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception;


@SuppressWarnings("serial")
public class DOMSMetadataExtractionParseFilenameException extends DOMSMetadataExtractionParseException {

	public DOMSMetadataExtractionParseFilenameException(String msg) {
		super(msg);
	}

	public DOMSMetadataExtractionParseFilenameException(String msg, Throwable t) {
		super(msg, t);
	}

}
