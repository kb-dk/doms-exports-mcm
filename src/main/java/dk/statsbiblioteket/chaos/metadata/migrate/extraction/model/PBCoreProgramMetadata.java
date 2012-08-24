package dk.statsbiblioteket.chaos.metadata.migrate.extraction.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PBCoreProgramMetadata {

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public String channel;
	public String titel;
	public String originaltitel;
	public String episodetitel;
	public Date start;
	public Date end;
	public String descriptionKortOmtale;
	public String descriptionLangOmtale1;
	public String descriptionLangOmtale2;
	public String forfattere;
	public String medvirkende;
	public String instruktion;

	public PBCoreProgramMetadata(String channel, String titel,
			String originaltitel, String episodetitel, Date start, Date end,
			String descriptionKortOmtale, String descriptionLangOmtale1,
			String descriptionLangOmtale2, String forfattere,
			String medvirkende, String instruktion) {
		super();
		this.channel = channel;
		this.titel = titel;
		this.originaltitel = originaltitel;
		this.episodetitel = episodetitel;
		this.start = start;
		this.end = end;
		this.descriptionKortOmtale = descriptionKortOmtale;
		this.descriptionLangOmtale1 = descriptionLangOmtale1;
		this.descriptionLangOmtale2 = descriptionLangOmtale2;
		this.forfattere = forfattere;
		this.medvirkende = medvirkende;
		this.instruktion = instruktion;
	}

	@Override
	public String toString() {
		return "PBCoreProgramMetadata"
				+ " [channel : "       + String.format("%-8s", channel).substring(0, 8)
				+ ", titel : "         + String.format("%-30s", titel).substring(0, 30)
				+ ", originaltitel : " + String.format("%-30s", originaltitel).substring(0, 30)
				+ ", episodetitel : "  + String.format("%-30s", episodetitel).substring(0, 30)
				+ ", start : "         + (start!=null ? sdf.format(start) : start)
				+ ", end : "           + (end!=null ? sdf.format(end): end)
				+ ", kortomtal : "     + String.format("%-30s", descriptionKortOmtale).substring(0, 30)
				+ ", langomtale1 : "   + String.format("%-30s", descriptionLangOmtale1).substring(0, 30)
				+ ", langomtale2 : "   + String.format("%-30s", descriptionLangOmtale2).substring(0, 30)
				+ ", forfattere : "    + String.format("%-30s", forfattere).substring(0, 30)
				+ ", medvirkende : "   + String.format("%-30s", medvirkende).substring(0, 30)
				+ ", instruktion : "   + String.format("%-30s", instruktion).substring(0, 30)
				+ "]";
	}

}
