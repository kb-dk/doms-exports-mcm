package dk.statsbiblioteket.chaos.metadata.migrate.model;

public class BESClippingConfiguration {

	public long radioStartOffset; // Seconds
	public long radioEndOffset; // Seconds
	
	public BESClippingConfiguration(long radioStartOffset, long radioEndOffset) {
		super();
		this.radioStartOffset = radioStartOffset;
		this.radioEndOffset = radioEndOffset;
	}
}
