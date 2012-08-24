package dk.statsbiblioteket.chaos.metadata.migrate.exception;

@SuppressWarnings("serial")
public class MigrationInitializationException extends Exception {

	public MigrationInitializationException(String msg, Throwable t) {
		super(msg, t);
	}

}
