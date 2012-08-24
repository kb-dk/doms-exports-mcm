package dk.statsbiblioteket.chaos.metadata.migrate.extraction;

import java.util.Date;
import java.util.List;

import dk.statsbiblioteket.chaos.metadata.migrate.extraction.DOMSMetadataExtractor;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.PBCoreProgramMetadata;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgram;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.MediaClipWav;
import dk.statsbiblioteket.chaos.metadata.migrate.model.BESClippingConfiguration;

public class TestObjectFactory {

	private DOMSMetadataExtractor domsMetadataFinder;

	private String domsWSAPIEndpointUrlString="http://alhena:7880/centralWebservice-service/central/";
	private String userName="fedoraReadOnlyAdmin";
	private String password="fedoraReadOnlyPass";

	long besConfiguredRadioStartOffset=-20;
	long besConfiguredRadioEndOffset=20;

	public TestObjectFactory() {
		domsMetadataFinder = new DOMSMetadataExtractor(
				domsWSAPIEndpointUrlString,
				userName,
				password,
				besConfiguredRadioStartOffset, 
				besConfiguredRadioEndOffset);

	}
	
	public RadioProgram createDefaultRadioProgramMetadata() throws Exception {
		RadioProgram radioProgramMetadata = createRadioProgramWithNoClipInfo();
		String shardMetadata = createShardInfoXMLTextSingleClip();
		List<MediaClipWav> clips = domsMetadataFinder.extractRadioClipMetadata(shardMetadata);
		
		for (MediaClipWav radioClip : clips) {
			radioProgramMetadata.addRadioClip(radioClip);
		}
		return radioProgramMetadata;
	}

	public RadioProgram createRadioProgramMetadataWithTwoClips() throws Exception {
		RadioProgram radioProgramMetadata = createRadioProgramWithNoClipInfo();
		String shardMetadata = createShardInfoXMLTextDoubleClip();
		List<MediaClipWav> clips = domsMetadataFinder.extractRadioClipMetadata(shardMetadata);
		for (MediaClipWav radioClip : clips) {
			radioProgramMetadata.addRadioClip(radioClip);
		}
		return radioProgramMetadata;
	}

	public RadioProgram createRadioProgramWithNoClipInfo() {
		String shardPid = "1234567890abcdef";
		String channel = "dr1";
		String title = "radioavis";
		String originaltitel = "En originaltitel";
		String episodetitel = "En episodetitel";
		Date start = new Date();
		Date end = new Date();
		String kortomtale = "Kort omtale";
		String langomtale1 = "Lang omtale 1";
		String langomtale2 = "Lang omtale 2";
		String forfattere = "Troels forfatter";
		String medvirkende = "Søren skuespiller";
		String instruktion = "Morten instruktør";
		BESClippingConfiguration besConfiguration = new BESClippingConfiguration(-20, 20);
		PBCoreProgramMetadata pbcoreProgramMetadata = new PBCoreProgramMetadata(
				channel, title, originaltitel, episodetitel, start, end, 
				kortomtale, langomtale1, langomtale2,
				forfattere, medvirkende, instruktion);
		RadioProgram radioProgramMetadata = new RadioProgram(shardPid, besConfiguration);
		radioProgramMetadata.setPbcoreProgramMetadata(pbcoreProgramMetadata);
		return radioProgramMetadata;
	}

	public String createShardInfoXMLTextSingleClip() {
		String shardMetadata = ""
			+ "<shard_metadata>"
			+ "  <file>"
			+ "    <file_url>http://bitfinder.statsbiblioteket.dk/bart/drp1_88.100_DR-P1_pcm_20080509045602_20080510045501_encoder5-2.wav</file_url>"
			+ "    <program_start_offset>68638</program_start_offset>"
			+ "    <program_clip_length>300</program_clip_length>"
			+ "    <file_name>drp1_88.100_DR-P1_pcm_20080509045602_20080510045501_encoder5-2.wav</file_name>"
			+ "    <format_uri>info:pronom/fmt/6</format_uri>"
			+ "  </file>"
			+ "</shard_metadata>";
		return shardMetadata;
	}

	public String createShardInfoXMLTextDoubleClip() {
		String shardMetadata = ""
			+ "<shard_metadata>"
			+ "  <file>"
			+ "   <file_url>http://bitfinder.statsbiblioteket.dk/bart/drp1_88.100_DR-P1_pcm_20080508045601_20080509045501_encoder5-2.wav</file_url>"
			+ "    <program_start_offset>85739</program_start_offset>"
			+ "    <program_clip_length>601</program_clip_length>"
			+ "    <file_name>drp1_88.100_DR-P1_pcm_20080508045601_20080509045501_encoder5-2.wav</file_name>"
			+ "    <format_uri>info:pronom/fmt/6</format_uri>"
			+ "   </file>"
			+ "   <file>"
			+ "     <file_url>http://bitfinder.statsbiblioteket.dk/bart/drp1_88.100_DR-P1_pcm_20080509045602_20080510045501_encoder5-2.wav</file_url>"
			+ "     <program_start_offset>0</program_start_offset>"
			+ "     <program_clip_length>238</program_clip_length>"
			+ "     <file_name>drp1_88.100_DR-P1_pcm_20080509045602_20080510045501_encoder5-2.wav</file_name>"
			+ "     <format_uri>info:pronom/fmt/6</format_uri>"
			+ "  </file>"
			+ "</shard_metadata>";
		return shardMetadata;
	}

}
