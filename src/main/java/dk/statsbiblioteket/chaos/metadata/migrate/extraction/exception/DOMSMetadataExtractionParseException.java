package dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception;


@SuppressWarnings("serial")
public class DOMSMetadataExtractionParseException extends DOMSMetadataExtractionException {

	public DOMSMetadataExtractionParseException(String msg) {
		super(msg);
	}

	public DOMSMetadataExtractionParseException(String msg, Throwable t) {
		super(msg, t);
	}

}
