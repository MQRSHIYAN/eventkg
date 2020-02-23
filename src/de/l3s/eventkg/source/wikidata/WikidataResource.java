package de.l3s.eventkg.source.wikidata;

public enum WikidataResource {

	WIKINEWS_ARTICLE("Q17633526"),
	SCIENTIFIC_ARTICLE("Q13442814"),
	OCCURRENCE("Q1190554"),
	RECURRING_EVENT("Q15275719"),
	EVENT("Q1656682"),
	DETERMINATOR_FOR_DATE_OF_PERIODIC_OCCURRENCE("Q14795564"),
	HUMAN("Q5"),
	FICTIONAL_HUMAN("Q15632617"),
	WIKIMEDIA_INTERNAL_STUFF("Q17442446"),
	RECURRENT_EVENT_EDITION("Q27968055"),
	SPORTS_SEASON("Q27020041"),
	PROPERTY_EDITION_NUMBER("P393"),
	PROPERTY_SPORTS_SEASON_OF_LEAGUE_OR_COMPETITION("P3450"),
	PROPERTY_SEASON_OF_CLUB_OR_TEAM("P5138"),
	PROPERTY_PART_OF_THE_SERIES("P179");

	private String id;

	WikidataResource(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

}
