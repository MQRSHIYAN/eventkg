package de.l3s.eventkg.integration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.WikidataIdMappings.TemporalPropertyType;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Relation;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import de.l3s.eventkg.util.TimeTransformer;

public class TemporalRelationsCollector extends Extractor {

	public static void main(String[] args) {
		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.DE);

		Config.init("config_eventkb_local.txt");

		AllEventPagesDataSet allEventPagesDataSet = new AllEventPagesDataSet(languages);
		allEventPagesDataSet.init();

		TemporalRelationsCollector extr = new TemporalRelationsCollector(languages, allEventPagesDataSet);
		extr.run();
	}

	public TemporalRelationsCollector(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("TemporalRelationsCollector", Source.ALL, "?", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	private AllEventPagesDataSet allEventPagesDataSet;
	private Set<Relation> relations = new HashSet<Relation>();

	public void run() {
		System.out.println("Load YAGO.");
		loadYAGO();
		System.out.println("Load Wikidata.");
		loadWikidata();
		System.out.println("Load DBpedia.");
		loadDBpedia();
		System.out.println("Write.");
		writeToFiles();
	}

	private void writeToFiles() {
		try {
			PrintWriter writer = FileLoader.getWriter(FileName.ALL_TEMPORAL_RELATIONS);
			for (Relation relation : relations) {
				
				GenericRelation genericRelation = new GenericRelation(relation.getEntity1(),
						DataSets.getInstance().getDataSet(relation.getSourceLanguage(), relation.getSource()),
						relation.getProperty(), relation.getEntity2(), null);
				DataStore.getInstance().addGenericRelation(genericRelation);
				// Wikidata: Add labels
				if (relation.getSource() == Source.WIKIDATA) {
					Map<Language, String> propertyLabels = new HashMap<Language, String>();
					for (Language language : this.languages) {
						if (allEventPagesDataSet.getWikidataIdMappings().getWikidataPropertysByID(language,
								relation.getProperty()) != null)
							propertyLabels.put(language, allEventPagesDataSet.getWikidataIdMappings()
									.getWikidataPropertysByID(language, relation.getProperty()));
					}
					genericRelation.setPropertyLabels(propertyLabels);
				}

				if (relation.getEntity1().getEventEntity() != null) {

					String time1 = "\\N";
					if (relation.getStartTime() != null)
						time1 = FileLoader.PARSE_DATE_FORMAT.format(relation.getStartTime());
					String time2 = "\\N";
					if (relation.getEndTime() != null) {
						time2 = FileLoader.PARSE_DATE_FORMAT.format(relation.getEndTime());
					}

					if (relation.getProperty() == null) {
						System.out.println("PROP IS NULL: " + relation.getSource());
						continue;
					}

					writer.write(relation.getEntity1().getWikidataId());
					writer.write(Config.TAB);
					writer.write(relation.getEntity1().getWikipediaLabelsString(languages));
					writer.write(Config.TAB);

					writer.write(relation.getProperty());
					writer.write(Config.TAB);
					writer.write(relation.getEntity2().getWikidataId());
					writer.write(Config.TAB);
					writer.write(relation.getEntity2().getWikipediaLabelsString(languages));
					writer.write(Config.TAB);
					writer.write(time1);
					writer.write(Config.TAB);
					writer.write(time2);
					writer.write(Config.TAB);
					writer.write(relation.getSource().toString());
					writer.write(Config.NL);

				}

				if (relation.getEntity2().getEventEntity() != null) {
					String time1 = "\\N";
					if (relation.getStartTime() != null)
						time1 = FileLoader.PARSE_DATE_FORMAT.format(relation.getStartTime());
					String time2 = "\\N";
					if (relation.getEndTime() != null) {
						time2 = FileLoader.PARSE_DATE_FORMAT.format(relation.getEndTime());
					}

					writer.write(relation.getEntity2().getWikidataId());
					writer.write(Config.TAB);
					writer.write(relation.getEntity2().getWikipediaLabelsString(languages));
					writer.write(Config.TAB);
					writer.write(relation.getProperty() + "^-1");
					writer.write(Config.TAB);
					writer.write(relation.getEntity1().getWikidataId());
					writer.write(Config.TAB);
					writer.write(relation.getEntity1().getWikipediaLabelsString(languages));
					writer.write(Config.TAB);
					writer.write(time1);
					writer.write(Config.TAB);
					writer.write(time2);
					writer.write(Config.TAB);
					writer.write(relation.getSource().toString());
					writer.write(Config.NL);
				}

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	private void loadYAGO() {
		loadYAGOTemporalFacts();
		loadYAGOEventRelations();
	}

	private void loadYAGOEventRelations() {
		FileName fileName = FileName.YAGO_EVENT_FACTS;
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;

			Relation previousRelation = null;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				if (line.startsWith("\t")) {

					String timeString = parts[2];

					if (parts[1].equals("<occursSince>")) {
						Date date;
						try {
							date = TimeTransformer.generateEarliestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else if (parts[1].equals("<occursUntil>")) {
						Date date;
						try {
							date = TimeTransformer.generateLatestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} else {

					String entityLabel1 = parts[0];
					String entityLabel2 = parts[2];
					String property = parts[1];

					Entity entity1 = buildEntity(Language.EN, entityLabel1);
					Entity entity2 = buildEntity(Language.EN, entityLabel2);

					previousRelation = buildRelation(entity1, entity2, null, null, property, Source.YAGO, Language.EN);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadYAGOTemporalFacts() {
		FileName fileName = FileName.YAGO_TEMPORAL_FACTS;
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;

			Relation previousRelation = null;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				if (line.startsWith("\t")) {

					String timeString = parts[2];

					if (parts[1].equals("<occursSince>")) {
						Date date;
						try {
							date = TimeTransformer.generateEarliestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					} else if (parts[1].equals("<occursUntil>")) {
						Date date;
						try {
							date = TimeTransformer.generateLatestTimeFromXsd(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(date);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} else {

					String entityLabel1 = parts[0];
					String entityLabel2 = parts[2];
					String property = parts[1];

					Entity entity1 = buildEntity(Language.EN, entityLabel1);
					Entity entity2 = buildEntity(Language.EN, entityLabel2);

					previousRelation = buildRelation(entity1, entity2, null, null, property, Source.YAGO, Language.EN);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadWikidata() {
		loadWikidataTemporalRelations();
		loadWikidataAtemporalRelations();
	}

	private void loadWikidataTemporalRelations() {
		FileName fileName = FileName.WIKIDATA_TEMPORAL_FACTS;
		BufferedReader br = null;
		Set<String> props = new HashSet<String>();
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			Relation previousRelation = null;
			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				if (line.startsWith("\t")) {

					if (previousRelation == null)
						continue;

					String propertyWikidataId = parts[1];
					String timeString = parts[2];

					props.add(propertyWikidataId);

					TemporalPropertyType type = this.allEventPagesDataSet.getWikidataIdMappings()
							.getWikidataTemporalPropertyTypeById(propertyWikidataId);

					if (type == TemporalPropertyType.START || type == TemporalPropertyType.BOTH) {
						Date startTime;
						try {
							startTime = TimeTransformer.generateEarliestTimeForWikidata(timeString);
							if (previousRelation != null)
								previousRelation.setStartTime(startTime);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}
					if (type == TemporalPropertyType.END || type == TemporalPropertyType.BOTH) {
						Date endTime;
						try {
							endTime = TimeTransformer.generateLatestTimeForWikidata(timeString);
							if (previousRelation != null)
								previousRelation.setEndTime(endTime);
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

				} else {

					String entity1WikidataId = parts[1];
					String entity2WikidataId = parts[3];
					String propertyWikidataId = parts[2];

					Entity entity1 = buildEntityByWikidataId(entity1WikidataId);

					// ignore Wikidata items without English Wikipedia label
					// if (entity1 == null ||
					// entity1.getWikipediaLabel().equals("\\N")) {
					// previousRelation = null;
					// continue;
					// }

					Entity entity2 = buildEntityByWikidataId(entity2WikidataId);
					// ignore Wikidata items without English Wikipedia label
					// if (entity2 == null ||
					// entity2.getWikipediaLabel().equals("\\N")) {
					// previousRelation = null;
					// continue;
					// }

					// String property =
					// this.allEventPagesDataSet.getWikidataIdMappings()
					// .getWikidataPropertyById(propertyWikidataId);

					previousRelation = buildRelation(entity1, entity2, null, null, propertyWikidataId, Source.WIKIDATA,
							Language.EN);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(props);
	}

	private void loadWikidataAtemporalRelations() {
		FileName fileName = FileName.WIKIDATA_EVENT_RELATIONS;
		BufferedReader br = null;
		Set<String> props = new HashSet<String>();
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String entity1WikidataId = parts[1];
				String entity2WikidataId = parts[3];
				String propertyWikidataId = parts[2];

				Entity entity1 = buildEntityByWikidataId(entity1WikidataId);
				Entity entity2 = buildEntityByWikidataId(entity2WikidataId);

				// String property =
				// this.allEventPagesDataSet.getWikidataIdMappings()
				// .getWikidataPropertyById(propertyWikidataId);

				buildRelation(entity1, entity2, null, null, propertyWikidataId, Source.WIKIDATA, Language.EN);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(props);
	}

	private Entity buildEntityByWikidataId(String entityWikidataId) {
		Entity entity = this.allEventPagesDataSet.getWikidataIdMappings().getEntityByWikidataId(entityWikidataId);
		return entity;
	}

	private void loadDBpedia() {

		for (Language language : this.languages) {
			FileName fileName = FileName.DBPEDIA_EVENT_RELATIONS;
			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(fileName, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {

					String[] parts = line.split("\t");

					try {
						String entityLabel1 = parts[0];
						String entityLabel2 = parts[2];
						String property = parts[1].substring(parts[1].lastIndexOf("/") + 1, parts[1].lastIndexOf(">"));

						// TODO: Do this during extraction
						if (property.equals("type") || property.equals("location") || property.equals("place"))
							continue;

						// TODO: Check before
						if (property.equals("isPartOfMilitaryConflict") || property.equals("isPartOf")
								|| property.equals("isPartOfWineRegion")
								|| property.equals("isPartOfAnatomicalStructure"))
							continue;

						Entity entity1 = buildEntity(language, entityLabel1);
						Entity entity2 = buildEntity(language, entityLabel2);

						buildRelation(entity1, entity2, null, null, property, Source.DBPEDIA, language);
					} catch (ArrayIndexOutOfBoundsException e) {
						// TODO: Why? foaf homepage
						// System.out.println("Warning: " + line);
						continue;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void loadWCE() {
		FileName fileName = FileName.WCE_EVENT_RELATIONS;
		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(fileName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {

				String[] parts = line.split("\t");

				String entityLabel1 = parts[0];
				String entityLabel2 = parts[1];

				String timeString = parts[2];

				try {
					Date time = FileLoader.PARSE_DATE_FORMAT.parse(timeString);
					Entity entity1 = buildEntity(Language.EN, entityLabel1);
					Entity entity2 = buildEntity(Language.EN, entityLabel2);

					buildRelation(entity1, entity2, time, time, "related", Source.WCE, Language.EN);

				} catch (ParseException e) {
					e.printStackTrace();
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// private Entity buildEntity(String entityLabel, String wikidataId) {
	//
	// entityLabel = entityLabel.replaceAll(" ", "_");
	//
	// if (this.allEventPagesDataSet.getEvents().containsKey(entityLabel)) {
	// return this.allEventPagesDataSet.getEvents().get(entityLabel);
	// }
	//
	// if (this.entities.containsKey(entityLabel)) {
	// return this.entities.get(entityLabel);
	// }
	//
	// Entity entity = new Entity(entityLabel, wikidataId);
	// this.entities.put(entityLabel, entity);
	//
	// return entity;
	// }

	private Entity buildEntity(Language language, String wikipediaLabel) {
		return this.allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language, wikipediaLabel);
	}

	private Relation buildRelation(Entity entity1, Entity entity2, Date startTime, Date endTime, String property,
			Source source, Language sourceLanguage) {

		if (entity1 == null || entity2 == null)
			return null;

		Relation relation = new Relation(entity1, entity2, startTime, endTime, property, source, sourceLanguage);

		this.relations.add(relation);

		return relation;
	}

}