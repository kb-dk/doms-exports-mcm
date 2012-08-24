package dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception;

@SuppressWarnings("serial")
public class DOMSMetadataExtractionException extends Exception {

	public DOMSMetadataExtractionException(String msg, Throwable t) {
		super(msg, t);
	}

	public DOMSMetadataExtractionException(String msg) {
		super(msg);
	}

}
