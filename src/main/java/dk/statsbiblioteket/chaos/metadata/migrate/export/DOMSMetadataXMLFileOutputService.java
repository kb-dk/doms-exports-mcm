package dk.statsbiblioteket.chaos.metadata.migrate.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.List;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataIncompleteDataException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgram;


public class DOMSMetadataXMLFileOutputService {

	static Logger logger = Logger.getLogger(DOMSMetadataXMLFileOutputService.class);

	public DOMSMetadataXMLFileOutputService() {
		super();
	}

	public static void writeToFile(String filename, List<RadioProgram> radioProgramsSucces) {
		for (RadioProgram radioProgramMetadata : radioProgramsSucces) {
			writeRadioProgramMetadataToXMLFile(radioProgramMetadata);
		}
	}

	public static void writeRadioProgramMetadataToXMLFile(RadioProgram radioProgramMetadata) {
		writeRadioProgramMetadataToXMLFile("", radioProgramMetadata);
	}

	public static void writeRadioProgramMetadataToXMLFile(String subFolderName, RadioProgram radioProgramMetadata) {
		try {
			SimpleDateFormat sdfStart = new SimpleDateFormat("yyyyMMdd_HHmm");
			SimpleDateFormat sdfEnd = new SimpleDateFormat("HHmm");
			String filename = radioProgramMetadata.getPbcoreProgramMetadata().channel
				+ "_"
				+ sdfStart.format(radioProgramMetadata.getPbcoreProgramMetadata().start) + "_"
				+ sdfEnd.format(radioProgramMetadata.getPbcoreProgramMetadata().end)
				+ "_"
				+ radioProgramMetadata.getPbcoreProgramMetadata().titel.substring(0, Math.min(15, radioProgramMetadata.getPbcoreProgramMetadata().titel.length())).replaceAll("\\W", "_")
				+ ".xml";
			DateTimeFormatter fmt = ISODateTimeFormat.dateTime(); // ISO8601 (XML) Date/time
			if (subFolderName != null && subFolderName.length() > 0) {
				File subfolder = new File(subFolderName);
				subfolder.mkdirs();
			}
			File xmlFile = new File(subFolderName, filename);
			BufferedWriter xmlFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(xmlFile), Charset.forName("UTF-8")));
			logger.info("Writing to file: " + xmlFile.getPath());
			StringBuffer sb = new StringBuffer();
			sb =  sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
					.append("<radio_program>\n")
					.append("  <program_info>\n")
					.append("    <doms_id>"              + radioProgramMetadata.shardPid                                              + "</doms_id>\n")
					.append("    <channel_name>"         + radioProgramMetadata.getPbcoreProgramMetadata().channel                    + "</channel_name>\n")
					.append("    <title>"                + radioProgramMetadata.getPbcoreProgramMetadata().titel                      + "</title>\n")
					.append("    <title_original>"       + radioProgramMetadata.getPbcoreProgramMetadata().originaltitel              + "</title_original>\n")
					.append("    <title_episode>"        + radioProgramMetadata.getPbcoreProgramMetadata().episodetitel               + "</title_episode>\n")
					.append("    <start_time>"           + fmt.print(radioProgramMetadata.getPbcoreProgramMetadata().start.getTime()) + "</start_time>\n")
					.append("    <end_time>"             + fmt.print(radioProgramMetadata.getPbcoreProgramMetadata().end.getTime())   + "</end_time>\n")
					.append("    <description_short>"    + radioProgramMetadata.getPbcoreProgramMetadata().descriptionKortOmtale      + "</description_short>\n")
					.append("    <description_long1>"    + radioProgramMetadata.getPbcoreProgramMetadata().descriptionLangOmtale1     + "</description_long1>\n")
					.append("    <description_long2>"    + radioProgramMetadata.getPbcoreProgramMetadata().descriptionLangOmtale2     + "</description_long2>\n")
					.append("    <creator>"              + radioProgramMetadata.getPbcoreProgramMetadata().forfattere                 + "</creator>\n")
					.append("    <contributor>"          + radioProgramMetadata.getPbcoreProgramMetadata().medvirkende                + "</contributor>\n")
					.append("    <contributor_director>" + radioProgramMetadata.getPbcoreProgramMetadata().instruktion                + "</contributor_director>\n")
					.append("  </program_info>\n")
					.append("  <clip_info>\n")
					.append("    <filename>" +      radioProgramMetadata.getPresentationFilename()               + "</filename>\n")
					.append("    <clip_duration>" + radioProgramMetadata.getTotalClipDurationInSeconds()         + "</clip_duration>\n")
					.append("    <clip_start>" +    fmt.print(radioProgramMetadata.getClipStartTime().getTime()) + "</clip_start>\n")
					.append("    <clip_end>" +      fmt.print(radioProgramMetadata.getClipEndTime().getTime())   + "</clip_end>\n")
					.append("  </clip_info>\n")
					.append("</radio_program>\n");
			xmlFileWriter.write(sb.toString());
			xmlFileWriter.close();
		} catch (IOException e) {
			logger.error("Error writing log file for program: " + radioProgramMetadata.toString(), e);
		} catch (DOMSMetadataIncompleteDataException e) {
			logger.error("Error writing log file for program: " + radioProgramMetadata.toString(), e);
		}
	}
}
