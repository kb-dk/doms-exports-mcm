package dk.statsbiblioteket.chaos.metadata.migrate.extraction;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionConnectToDOMSException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionParseException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionParseFilenameException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionParsePBCoreException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.exception.DOMSMetadataExtractionUnknownMediaTypeException;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.MediaTypeEnum;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.PBCoreProgramMetadata;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgram;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.MediaClipWav;
import dk.statsbiblioteket.chaos.metadata.migrate.extraction.model.RadioProgramSearchResultItem;
import dk.statsbiblioteket.chaos.metadata.migrate.model.BESClippingConfiguration;
import dk.statsbiblioteket.doms.central.CentralWebservice;
import dk.statsbiblioteket.doms.central.CentralWebserviceService;
import dk.statsbiblioteket.doms.central.InvalidCredentialsException;
import dk.statsbiblioteket.doms.central.InvalidResourceException;
import dk.statsbiblioteket.doms.central.MethodFailedException;
import dk.statsbiblioteket.doms.central.RecordDescription;
import dk.statsbiblioteket.doms.central.Relation;
import dk.statsbiblioteket.doms.central.SearchResult;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;

/**
 * This class is responsible for extracting metadata from DOMS
 * 
 * @author heb@statsbiblioteket.dk
 *
 */
public class DOMSMetadataExtractor {
	
	static Logger logger = Logger.getLogger(DOMSMetadataExtractor.class);
    
	private static final QName CENTRAL_WEBSERVICE_SERVICE = new QName(
    		"http://central.doms.statsbiblioteket.dk/",
    		"CentralWebserviceService");
    private CentralWebservice domsService;

	private BESClippingConfiguration besConfiguration;

	/**
	 * Used at runtime
	 * 
	 * @param properties
	 */
	public DOMSMetadataExtractor(Properties properties) {
		super();
		String domsWSAPIEndpointUrlString = properties.getProperty("domsWSAPIEndpointUrlString");
		String userName = properties.getProperty("userName");
		String password = properties.getProperty("password");
		domsService = createDOMSCentralWebService(domsWSAPIEndpointUrlString, userName, password);
		Long radioStartOffset = Long.valueOf(properties.getProperty("besConfiguredRadioStartOffset"));
		Long radioEndOffset = Long.valueOf(properties.getProperty("besConfiguredRadioEndOffset"));
		this.besConfiguration = new BESClippingConfiguration(radioStartOffset, radioEndOffset);
	}

	/**
	 * Used for test purpose
	 * 
	 * @param domsWSAPIEndpointUrlString
	 * @param userName
	 * @param password
	 * @param besConfiguredRadioStartOffset
	 * @param besConfiguredRadioEndOffset
	 */
	public DOMSMetadataExtractor(String domsWSAPIEndpointUrlString,
			String userName, String password,
			long besConfiguredRadioStartOffset,
			long besConfiguredRadioEndOffset) {
		super();
		domsService = createDOMSCentralWebService(domsWSAPIEndpointUrlString, userName, password);
		this.besConfiguration = new BESClippingConfiguration(besConfiguredRadioStartOffset, besConfiguredRadioEndOffset);
	}
	
	@Deprecated
	public List<String> fetchAllRadioShardPids()
			throws InvalidCredentialsException, MethodFailedException,
			InvalidResourceException {
		List<String> shardPids = new ArrayList<String>();
		List<SearchResult> waveFileObjects = domsService.findObjects("*.wav", 0, 1);
		for (SearchResult searchResult : waveFileObjects) {
			logger.debug("Found wave file with pid: " + searchResult.getPid());
			List<Relation> relationsToFile = domsService.getInverseRelations(searchResult.getPid());
			for (Relation fileRelation : relationsToFile) {
				String shardPid = fileRelation.getSubject();
				shardPids.add(shardPid);
			}
		}
		return shardPids;
	}

	/**
	 * Fetches metadata of radio programs that have been changed sinde the date given as argument.
	 * 
	 * @param updatedSince
	 * @return
	 * @throws DOMSMetadataExtractionException
	 */
	public List<RadioProgramSearchResultItem> fetchRadioProgramMetadataUpdatedSince(Date updatedSince) throws DOMSMetadataExtractionConnectToDOMSException {
		List<RecordDescription> updatedShardPids = fetchUpdatedShardPids(updatedSince);
		List<RadioProgramSearchResultItem> radioProgramMetadataList = fetchRadioProgramMetadataFromShardPids(updatedShardPids);  
		return radioProgramMetadataList;
	}

	protected List<RecordDescription> fetchUpdatedShardPids(Date updatedSince) throws DOMSMetadataExtractionConnectToDOMSException {
		List<RecordDescription> updatedShardPids = new ArrayList<RecordDescription>();
		int pageOffset = 0;
		int pageSize = 10000;
		boolean continueToNextPage = true;
		while (continueToNextPage) {
			List<RecordDescription> newPidsInPage = fetchUpdatedShardPidsPaged(updatedSince, pageOffset*pageSize, pageSize);
			updatedShardPids.addAll(newPidsInPage);
			pageOffset++;
			continueToNextPage = !newPidsInPage.isEmpty();
		}
		return updatedShardPids;
	}

	protected List<RecordDescription> fetchUpdatedShardPids(Date updatedSince, int maxNumberToReturn) throws DOMSMetadataExtractionConnectToDOMSException {
		List<RecordDescription> updatedShardPids = new ArrayList<RecordDescription>();
		int pageOffset = 0;
		int pageSize = 10000;
		boolean continueToNextPage = true;
		while (continueToNextPage) {
			int remaningPageSize = Math.min(pageSize, (pageOffset*pageSize - maxNumberToReturn));
			int offset = pageOffset*pageSize;
			List<RecordDescription> newPidsInPage = fetchUpdatedShardPidsPaged(updatedSince, offset, remaningPageSize);
			updatedShardPids.addAll(newPidsInPage);
			pageOffset++;
			continueToNextPage = (!newPidsInPage.isEmpty() || (maxNumberToReturn < pageOffset*pageSize));
		}
		return updatedShardPids;
	}

	/**
	 * Get shard pids modified since given date. Filter out pids not being shards.
	 * 
	 * @param updatedSince
	 * @param offset
	 * @param pageSize
	 * @return
	 * @throws DOMSMetadataExtractionConnectToDOMSException
	 */
	protected List<RecordDescription> fetchUpdatedShardPidsPaged(Date updatedSince, int offset, int pageSize) throws DOMSMetadataExtractionConnectToDOMSException {
		List<RecordDescription> newShardPidsInPage = new ArrayList<RecordDescription>();
		try {
			List<RecordDescription> domsRecords = domsService.getIDsModified(updatedSince.getTime(), "doms:RadioTV_Collection", "BES", "Published", offset, pageSize);
			logger.info("Page " + offset + " containing " + domsRecords.size() + " records");
		    for (RecordDescription record: domsRecords) {
		    	String recordS = "Found updated pid = '" + record.getPid()
		                + "' Entry CM = '" + record.getEntryContentModelPid()
		                + "' Date = " + (new Date(record.getDate()));
		        //logger.debug(recordS);
		        if ("doms:ContentModel_Shard".equals(record.getEntryContentModelPid())) {
		        	newShardPidsInPage.add(record);
		        } else {
		        	logger.warn("Record with unexpted contentModel: " + recordS);
		        }
		    }
		} catch (InvalidCredentialsException e) {
			throw new DOMSMetadataExtractionConnectToDOMSException("Failed to connect to DOMS.", e);
		} catch (MethodFailedException e) {
			throw new DOMSMetadataExtractionConnectToDOMSException("Failed to connect to DOMS.", e);
		}
		return newShardPidsInPage;
	}

	public List<String> fetchUpdatedShardPidsPagedAsString(Date updatedSince, int offset, int pageSize) throws DOMSMetadataExtractionConnectToDOMSException {
		List<String> shardPids = new ArrayList<String>();
		List<RecordDescription> searchResult = fetchUpdatedShardPidsPaged(updatedSince, offset, pageSize);
		for (RecordDescription recordDescription : searchResult) {
			shardPids.add(recordDescription.getPid());
		}
		return shardPids ;
	}

	/**
	 * 
	 * @param recordDescription
	 * @return List of lists. First list contains successful exported and second list contains failed.
	 * @throws DOMSMetadataExtractionConnectToDOMSException If the connection to DOMS failed
	 * @throws InvalidCredentialsException
	 * @throws InvalidResourceException
	 * @throws MethodFailedException
	 * @throws ParseException
	 */
	public List<RadioProgramSearchResultItem> fetchRadioProgramMetadataFromShardPids(List<RecordDescription> recordDescription) throws DOMSMetadataExtractionConnectToDOMSException  {
		ArrayList<RadioProgramSearchResultItem> searchResults = new ArrayList<RadioProgramSearchResultItem>();
		int i=0;
		for (RecordDescription searchResultRecord : recordDescription) {
			RadioProgramSearchResultItem radioProgramSearchResult = null;
			try {
				logger.info("Handling shard: " + searchResultRecord + " - " + i + "/ " + recordDescription.size());
				RadioProgram radioProgram = new RadioProgram(searchResultRecord.getPid(), searchResultRecord.getDate(), besConfiguration);
				radioProgramSearchResult = fetchRadioProgramMetadataFromShardPid(radioProgram);
			} catch (DOMSMetadataExtractionException e) {
				logger.warn("Unable to extract radio program: " + e.getMessage(), e);
				RadioProgram radioProgram = new RadioProgram(searchResultRecord.getPid(), searchResultRecord.getDate());
				radioProgramSearchResult = new RadioProgramSearchResultItem(radioProgram);
				radioProgramSearchResult.extractionFailed("Unable to extract metadata.");
			}
			searchResults.add(radioProgramSearchResult);
			i++;
		}
		return searchResults;
	}
	
	/**
	 * 
	 * @param shardPids
	 * @param useThisMethod Ignored. Only used so the runtime system can differentiate the method signature from similar method (See java type erasure)
	 * @return
	 * @throws DOMSMetadataExtractionConnectToDOMSException 
	 */
	public List<RadioProgramSearchResultItem> fetchRadioProgramMetadataFromShardPids(List<String> shardPids, boolean useThisMethod) throws DOMSMetadataExtractionConnectToDOMSException {
		ArrayList<RadioProgramSearchResultItem> searchResults = new ArrayList<RadioProgramSearchResultItem>();
		int i=0;
		for (String shardPid: shardPids) {
			RadioProgramSearchResultItem radioProgramSearchResult = null;
			try {
				logger.info("Handling shard: " + shardPid + " - " + i + "/ " + shardPids.size());
				RadioProgram radioProgram = new RadioProgram(shardPid, besConfiguration);
				radioProgramSearchResult = fetchRadioProgramMetadataFromShardPid(radioProgram);
			} catch (DOMSMetadataExtractionException e) {
				logger.warn("Unable to extract radio program: " + e.getMessage(), e);
				RadioProgram radioProgram = new RadioProgram(shardPid);
				radioProgramSearchResult = new RadioProgramSearchResultItem(radioProgram);
				radioProgramSearchResult.extractionFailed("Unable to extract metadata.");
			}
			searchResults.add(radioProgramSearchResult);
			i++;
		}
		return searchResults;
	}

	protected RadioProgramSearchResultItem fetchRadioProgramMetadataFromShardPid(RadioProgram radioProgram) throws DOMSMetadataExtractionConnectToDOMSException, DOMSMetadataExtractionException {
		try {
			RadioProgramSearchResultItem searchResult = new RadioProgramSearchResultItem(radioProgram);
			// Handle shard metadata
			String shardMetadata = domsService.getDatastreamContents(radioProgram.shardPid, "SHARD_METADATA");
			logger.trace(" - related shard metadata:\n " + shardMetadata);
			List<MediaClipWav> radioClips;
			try {
				radioClips = extractRadioClipMetadata(shardMetadata);
				for (MediaClipWav soundClip : radioClips) {
					radioProgram.addRadioClip(soundClip);
				}
			} catch (DOMSMetadataExtractionException e) {
				logger.warn("Could not extract shard data.", e);
				searchResult.extractionFailed(radioProgram.shardPid + " - Could not extract shard data. Reason: " + e.getMessage());
			}
			// Handle program metadata
			List<Relation> relationsToShard = domsService.getInverseRelations(radioProgram.shardPid);
			if (relationsToShard.size() != 1) {
				throw new DOMSMetadataExtractionException("Unexpected number of relations to the shard with pid: " + radioProgram.shardPid);
			}
			Relation shardRelation = relationsToShard.get(0);
			String programPid = shardRelation.getSubject();
			logger.trace("   - related program: " + programPid);
			String programPBCore = domsService.getDatastreamContents(programPid, "PBCORE");
			logger.trace(programPBCore);
			try {
				PBCoreProgramMetadata pbcoreProgramMetadata = extractMetadataFromPBCore(radioProgram.shardPid, programPBCore);
				radioProgram.setPbcoreProgramMetadata(pbcoreProgramMetadata);
				logger.debug("Found program: " + pbcoreProgramMetadata.titel);
			} catch (DOMSMetadataExtractionParsePBCoreException e) {
				logger.warn(radioProgram.shardPid + " - Could not extract metadata from PBCore", e);
				searchResult.extractionFailed("Could not extract metadata from PBCore");
			}
			if (!searchResult.validate()) {
				logger.warn(radioProgram.shardPid + " - Could not validate search result.");
			}
			return searchResult;
		} catch (InvalidCredentialsException e) {
			logger.error("Invalid configuration. Stopping extraction.", e);
			throw new DOMSMetadataExtractionConnectToDOMSException("Invalid configuration. Stopping extraction.", e);
		} catch (InvalidResourceException e) {
			logger.error("Invalid configuration. Stopping extraction.", e);
			throw new DOMSMetadataExtractionConnectToDOMSException("Invalid configuration. Stopping extraction.", e);
		} catch (MethodFailedException e) {
			logger.error("Invalid configuration. Stopping extraction.", e);
			throw new DOMSMetadataExtractionConnectToDOMSException("Invalid configuration. Stopping extraction.", e);
		}
	}

	protected PBCoreProgramMetadata extractMetadataFromPBCore(String shardPid, String programPBCore) throws DOMSMetadataExtractionParsePBCoreException {
		String channel;
		String titleTitel;
		String titleOriginaltitel;
		String titleEpisodetitel;
		Date dateAvailableStart;
		Date dateAvailableEnd;
		String creatorForfattere;
		String contributerMedvirkende;
		String contributerInstruktion;
		String descriptionKortOmtale;
		String descriptionLangOmtale1;
		String descriptionLangOmtale2;
		XPathSelector xpath = DOM.createXPathSelector("pb", "http://www.pbcore.org/PBCore/PBCoreNamespace.html");
		Document doc = DOM.stringToDOM(programPBCore, true);
		
		//channel = getChannelId(programPBCore);
		channel = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcorePublisher[pb:publisherRole='channel_name']/pb:publisher");
		logger.debug("Parsed channel: " + channel);
		
		//titleTitel = getProgramTitleFromPBCore(programPBCore);
		titleTitel = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreTitle[pb:titleType='titel']/pb:title");
		logger.debug("Parsed titleTitel: " + titleTitel);

		//titleOriginaltitel = getTitleOriginaltitelFromPBCore(programPBCore);
		titleOriginaltitel = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreTitle[pb:titleType='originaltitel']/pb:title");
		logger.debug("Parsed titleOriginaltitel: " + titleOriginaltitel);
		
		//titleEpisodetitel = getTitleEpisodetitelFromPBCore(programPBCore);
		titleEpisodetitel = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreTitle[pb:titleType='episodetitel']/pb:title");
		logger.debug("Parsed titleEpisodetitel: " + titleEpisodetitel);
		
		dateAvailableStart = getStartDateFromPBCore(programPBCore);
		dateAvailableEnd = getEndDateFromPBCore(programPBCore);

		//descriptionKortOmtale = getDescriptionKortOmtaleFromPBCore(programPBCore);
		descriptionKortOmtale = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreDescription[pb:descriptionType='kortomtale']/pb:description");
		logger.debug("Parsed descriptionKortOmtale: " + descriptionKortOmtale);
		
		//descriptionLangOmtale1 = getDescriptionLangOmtale1FromPBCore(programPBCore);
		descriptionLangOmtale1 = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreDescription[pb:descriptionType='langomtale1']/pb:description");
		logger.debug("Parsed descriptionLangOmtale1: " + descriptionLangOmtale1);
		
		//descriptionLangOmtale2 = getDescriptionLangOmtale2FromPBCore(programPBCore);
		descriptionLangOmtale2 = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreDescription[pb:descriptionType='langomtale2']/pb:description");
		logger.debug("Parsed descriptionLangOmtale2: " + descriptionLangOmtale2);

		//creatorForfattere = getCreatorForfattereFromPBCore(programPBCore);
		creatorForfattere = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreCreator[pb:creatorRole='forfatter']/pb:creator");
		logger.debug("Parsed creatorForfattere: " + creatorForfattere);
		
		//contributerMedvirkende = getContributorMedvirkendeFromPBCore(programPBCore);
		contributerMedvirkende = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreContributor[pb:contributorRole='medvirkende']/pb:contributor");
		logger.debug("Parsed contributerMedvirkende: " + contributerMedvirkende);
		
		//contributerInstruktion = getContributorInstruktionFromPBCore(programPBCore);
		contributerInstruktion = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreContributor[pb:contributorRole='instruktion']/pb:contributor");
		logger.debug("Parsed contributerInstruktion: " + contributerInstruktion);
		
		PBCoreProgramMetadata pbcoreProgramMetadata = new PBCoreProgramMetadata( 
				channel, 
				titleTitel,
				titleOriginaltitel,
				titleEpisodetitel,
				dateAvailableStart, 
				dateAvailableEnd, 
				descriptionKortOmtale,
				descriptionLangOmtale1,
				descriptionLangOmtale2,
				creatorForfattere,
				contributerMedvirkende,
				contributerInstruktion);
		return pbcoreProgramMetadata;
	}
	
	/**
	 * 
	 * <shard_metadata>
 	 *  <file>
 	 *    <file_url>http://bitfinder.statsbiblioteket.dk/bart/drp1_88.100_DR-P1_pcm_20080508045601_20080509045501_encoder5-2.wav</file_url>
 	 *    <program_start_offset>85739</program_start_offset>
	 *    <program_clip_length>601</program_clip_length>
 	 *    <file_name>drp1_88.100_DR-P1_pcm_20080508045601_20080509045501_encoder5-2.wav</file_name>
	 *    <format_uri>info:pronom/fmt/6</format_uri>
	 *   </file>
	 *   <file>
	 *     <file_url>http://bitfinder.statsbiblioteket.dk/bart/drp1_88.100_DR-P1_pcm_20080509045602_20080510045501_encoder5-2.wav</file_url>
	 *     <program_start_offset>0</program_start_offset>
	 *     <program_clip_length>238</program_clip_length>
	 *     <file_name>drp1_88.100_DR-P1_pcm_20080509045602_20080510045501_encoder5-2.wav</file_name>
	 *     <format_uri>info:pronom/fmt/6</format_uri>
	 *   </file>
	 * </shard_metadata>
	 * @param radioProgramMetadata is augmented with clip information from shardMetadata
	 * @param shardMetadata
	 * @throws ShardExtractionException If the shard could not be parsed.
	 */
	protected List<MediaClipWav> extractRadioClipMetadata(String shardMetadata) throws DOMSMetadataExtractionException {
		Document doc = DOM.stringToDOM(shardMetadata, true);
		XPathSelector xpath = DOM.createXPathSelector();
		NodeList fileNodes = xpath.selectNodeList(doc, "//file");
		//NodeList fileNodes = (NodeList) XPathFactory.newInstance().newXPath().compile("//file").evaluate(doc, XPathConstants.NODESET);
		int numberOfFileNode = fileNodes.getLength();
		logger.trace("Number of file nodes: " + numberOfFileNode);
		List<MediaClipWav> radioClips = new ArrayList<MediaClipWav>();
		for (int i=0;i<numberOfFileNode; i++) {
			Node filenode = fileNodes.item(i);
			String filename = xpath.selectString(filenode, "file_name");
			String clipStartOffsetString = xpath.selectString(filenode, "program_start_offset");
			String clipLengthString = xpath.selectString(filenode, "program_clip_length");
			long clipStartOffset = Long.parseLong(clipStartOffsetString);
			long clipLength = Long.parseLong(clipLengthString);
			/*
			Node clipFileNameNode = (Node) XPathFactory.newInstance().newXPath().compile("//file["+(i+1)+"]/file_name").evaluate(doc, XPathConstants.NODE);
			Node clipStartOffsetNode = (Node) XPathFactory.newInstance().newXPath().compile("//file["+(i+1)+"]/program_start_offset").evaluate(doc, XPathConstants.NODE);
			Node clipLengthNode = (Node) XPathFactory.newInstance().newXPath().compile("//file["+(i+1)+"]/program_clip_length").evaluate(doc, XPathConstants.NODE);
			String filename = clipFileNameNode.getTextContent();
			long clipStartOffset = Long.parseLong(clipStartOffsetNode.getTextContent());
			long clipLength = Long.parseLong(clipLengthNode.getTextContent());
			*/
			logger.trace(i + " Filename node value     : " + filename);
			logger.trace(i + " Start offset node value : " + clipStartOffset);
			logger.trace(i + " Clip length node value  : " + clipLength);
			MediaTypeEnum fileMedia = extractMediaTypeFromFilename(filename);
			if (!fileMedia.equals(MediaTypeEnum.WAV)) {
				throw new DOMSMetadataExtractionException("Invalid media type. Expecting " + MediaTypeEnum.WAV + ". Was: " + fileMedia);
			}
			Date clipStartDate = extractClipStartDateFromWavFilename(filename, clipStartOffset);
			radioClips.add(new MediaClipWav(filename, clipStartDate, clipLength));
		}
		return radioClips;
	}
	
	protected Date extractClipStartDateFromWavFilename(String filename, long offset) throws DOMSMetadataExtractionParseFilenameException, DOMSMetadataExtractionParseException {
		Pattern p = Pattern.compile(".*_([0-9]{14})_[0-9]{14}_encoder[0-9]-[0-9]\\.wav", Pattern.DOTALL);
		Matcher m = p.matcher(filename);
		if (!m.find()) {
			throw new DOMSMetadataExtractionParseFilenameException("Could not parse filename: " + filename);
		}
		String sourceStartString = m.group(1);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Date sourceStart;
		try {
			sourceStart = sdf.parse(sourceStartString);
		} catch (ParseException e) {
			logger.warn("Could not parse date from string: " + sourceStartString + " from filename: " + filename);
			throw new DOMSMetadataExtractionParseException("Could not parse date from string: " + sourceStartString);
		}
		Date clipStartDate = new Date(sourceStart.getTime() + offset*1000);
		return clipStartDate;
	}

	protected MediaTypeEnum extractMediaTypeFromFilename(String filename) throws DOMSMetadataExtractionUnknownMediaTypeException, DOMSMetadataExtractionParseFilenameException {
		Pattern p = Pattern.compile(".*\\.(.*)", Pattern.DOTALL);
		Matcher m = p.matcher(filename);
		if (!m.find()) {
			throw new DOMSMetadataExtractionParseFilenameException("Could not parse filename: " + filename);
		}
		String filenameExtension = m.group(1);
		MediaTypeEnum type = null;
		if ("wav".equals(filenameExtension)) {
			type = MediaTypeEnum.WAV;
		} else if ("mp3".equals(filenameExtension)) {
			type = MediaTypeEnum.MP3;
		} else if ("jpg".equals(filenameExtension)) {
			type = MediaTypeEnum.JPG;
		} else if ("mpeg".equals(filenameExtension)) {
			type = MediaTypeEnum.MPEG;
		} else if ("ts".equals(filenameExtension)) {
			type = MediaTypeEnum.TS;
		} else if ("flv".equals(filenameExtension)) {
			type = MediaTypeEnum.FLV;
		} else {
			throw new DOMSMetadataExtractionUnknownMediaTypeException("Unknown media type of file: " + filename + ". " +
					"Infered extension was: " + filenameExtension);
		}
		return type;
	}

	protected static String getChannelId(String programPBCore) {
		XPathSelector xpath = DOM.createXPathSelector("pb", "http://www.pbcore.org/PBCore/PBCoreNamespace.html");
		Document doc = DOM.stringToDOM(programPBCore, true);
		String channelID = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcorePublisher[pb:publisherRole='channel_name']/pb:publisher");
		logger.debug("Parsed channelID: " + channelID);
		return channelID; //extractStringContent(programPBCore, "<pbcorePublisher>.*<publisher>(.*)</publisher>.*<publisherRole>channel_name</publisherRole>.*</pbcorePublisher>");
	}

	private static Date getStartDateFromPBCore(String programPBCore) throws DOMSMetadataExtractionParsePBCoreException {
		XPathSelector xpath = DOM.createXPathSelector("pb", "http://www.pbcore.org/PBCore/PBCoreNamespace.html");
		Document doc = DOM.stringToDOM(programPBCore, true);
		String startDateString = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreInstantiation/pb:pbcoreDateAvailable/pb:dateAvailableStart");
		logger.debug("Parsed startDate: " + startDateString);
        // Format example 2008-05-09T18:00:00+0200
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
        Date date;
		try {
			date = formatter.parse(startDateString);
		} catch (ParseException e) {
			logger.warn("Unable to parse start date '" + startDateString + "' from PBCore.\n" + programPBCore );
			throw new DOMSMetadataExtractionParsePBCoreException("Unable to parse start date " + startDateString + " from PBCore.", e);
		}
		return date;
	}

	private static Date getEndDateFromPBCore(String programPBCore) throws DOMSMetadataExtractionParsePBCoreException {
		XPathSelector xpath = DOM.createXPathSelector("pb", "http://www.pbcore.org/PBCore/PBCoreNamespace.html");
		Document doc = DOM.stringToDOM(programPBCore, true);
		String endDateString = xpath.selectString(doc, "/pb:PBCoreDescriptionDocument/pb:pbcoreInstantiation/pb:pbcoreDateAvailable/pb:dateAvailableEnd");
		logger.debug("Parsed endDate: " + endDateString);
        // Format example 2008-05-09T18:00:00+0200
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ");
        Date date;
		try {
			date = formatter.parse(endDateString);
		} catch (ParseException e) {
			logger.warn("Unable to parse end date " + endDateString + " from PBCore.\n" + programPBCore );
			throw new DOMSMetadataExtractionParsePBCoreException("Unable to parse end date " + endDateString + " from PBCore.", e);
		}
		return date;
	}

	private CentralWebservice createDOMSCentralWebService(
			String domsWSAPIEndpointUrlString, String userName, String password) {
		logger.debug("Creating DOMS Client");
		logger.debug("domsWSAPIEndpointUrlString, " + domsWSAPIEndpointUrlString);
		logger.debug("userName: " + userName);
		logger.debug("password: " + password);
		URL domsWSAPIEndpoint;
		try {
			domsWSAPIEndpoint = new URL(domsWSAPIEndpointUrlString);
		} catch (MalformedURLException e) {
			throw new RuntimeException("URL to DOMS not configured correctly. Was: " + domsWSAPIEndpointUrlString, e);
		}
		CentralWebservice domsAPI = new CentralWebserviceService(domsWSAPIEndpoint, CENTRAL_WEBSERVICE_SERVICE).getCentralWebservicePort();
		Map<String, Object>  domsAPILogin = ((BindingProvider) domsAPI).getRequestContext();
		domsAPILogin.put(BindingProvider.USERNAME_PROPERTY, userName);
		domsAPILogin.put(BindingProvider.PASSWORD_PROPERTY, password);
		return domsAPI;
	}
}
