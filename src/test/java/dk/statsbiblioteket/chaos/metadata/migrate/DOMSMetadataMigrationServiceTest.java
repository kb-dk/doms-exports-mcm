package dk.statsbiblioteket.chaos.metadata.migrate;

import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import dk.statsbiblioteket.chaos.metadata.migrate.exception.MigrationInitializationException;
import junit.framework.TestCase;

public class DOMSMetadataMigrationServiceTest extends TestCase {

	private Level testLogLevel = Level.INFO;
	private Properties properties;

	public DOMSMetadataMigrationServiceTest(String name) {
		super(name);
		properties = new Properties();
		properties.setProperty("log4jPropertyFile", "log4j.exporter_doms_radio_metadata_to_chaos.xml");
		properties.setProperty("domsWSAPIEndpointUrlString", "http://alhena:7880/centralWebservice-service/central/");
		properties.setProperty("userName", "fedoraReadOnlyAdmin");
		properties.setProperty("password", "fedoraReadOnlyPass");
		properties.setProperty("besConfiguredRadioStartOffset", "-20");
		properties.setProperty("besConfiguredRadioEndOffset", "20");
		properties.setProperty("chaosChannelList", "drp1;drp2;drp3");

		BasicConfigurator.resetConfiguration();
		BasicConfigurator.configure();
		System.out.println("Log level set to: " + testLogLevel);
		Logger.getRootLogger().setLevel(testLogLevel);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testConstructor() throws MigrationInitializationException {
		DOMSMetadataMigrationService migrationService = new DOMSMetadataMigrationService(properties);
		assertNotNull("Expecting object", migrationService);
	}
	
	//public void test
}
