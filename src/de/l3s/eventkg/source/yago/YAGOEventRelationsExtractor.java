package de.l3s.eventkg.source.yago;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.yago.model.YAGOMetaFact;
import de.l3s.eventkg.source.yago.util.YAGOLabelExtractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

/**
 * Collects all triples with an event as subject or object. Also loads all facts
 * with temporal meta facts. Results are written to two files.
 */
public class YAGOEventRelationsExtractor extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	public YAGOEventRelationsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("YAGOEventRelationsExtractor", Source.YAGO,
				"Loads all YAGO relations where the subject and/or object is an event or both have an existence time, or where the fact has a temporal scope.",
				languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("Collect YAGO event facts.");
		Map<String, Set<YAGOMetaFact>> temporalMetaFacts = YAGOMetaFactsDataSet.loadMetaFacts(true);

		extractTriples(temporalMetaFacts);
		extractTriplesWithEventsAndLiterals(temporalMetaFacts);
	}

	private void extractTriples(Map<String, Set<YAGOMetaFact>> temporalMetaFacts) {

		System.out.println("Collect YAGO meta facts.");

		// Properties:
		// <isLeaderOf>, <owns>, <isLocatedIn>, <isCitizenOf>, <hasMusicalRole>
		// <hasOfficialLanguage>, <edited>, <isConnectedTo>, <actedIn>,
		// <imports>, <wasBornIn>, <participatedIn>, <dealsWith>, <diedIn>,
		// <created>, <isPoliticianOf>, <wroteMusicFor>, <hasNeighbor>,
		// <isMarriedTo>, <hasChild>, <hasGloss>, <isInterestedIn>,
		// <isAffiliatedTo>, <hasCurrency>, <exports>, <happenedIn>,
		// <hasGender>, <playsFor>, <directed>, <worksAt>, <graduatedFrom>,
		// <hasCapital>, <influences>, <hasWonPrize>, <hasWebsite>, <livesIn>,
		// <hasAcademicAdvisor>, <isKnownFor>

		System.out.println("Start writing to files.");
		PrintWriter eventFactsWriter = null;
		PrintWriter entityFactsWriter = null;
		PrintWriter temporalFactsWriter = null;

		Set<String> forbiddenProperties = new HashSet<String>();
		forbiddenProperties.addAll(YAGOEventLocationsExtractor.loadLocationProperties());

		try {
			eventFactsWriter = FileLoader.getWriter(FileName.YAGO_EVENT_FACTS);
			entityFactsWriter = FileLoader.getWriter(FileName.YAGO_ENTITY_FACTS);
			temporalFactsWriter = FileLoader.getWriter(FileName.YAGO_TEMPORAL_FACTS);

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.YAGO_FACTS);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				String previousId = null;

				while ((line = br.readLine()) != null) {

					if (line.startsWith("#@ <id")) {
						previousId = line.substring(3);
					}

					if (line.isEmpty() || line.startsWith("@") || line.startsWith("#"))
						continue;

					String[] parts = line.split("\t");

					YAGOLabelExtractor yagoLabelExtractor1 = new YAGOLabelExtractor(parts[0], this.languages);
					yagoLabelExtractor1.extractLabel();
					if (!yagoLabelExtractor1.isValid())
						continue;

					YAGOLabelExtractor yagoLabelExtractor2 = new YAGOLabelExtractor(
							parts[2].substring(0, parts[2].length() - 2), this.languages);
					yagoLabelExtractor2.extractLabel();
					if (!yagoLabelExtractor2.isValid())
						continue;

					String wikipediaLabel1 = parts[0].substring(1, parts[0].length() - 1);
					String wikipediaLabel2 = parts[2].substring(1, parts[2].length() - 3);

					// check for events using the actual labels. But write to
					// files the original labels like "de/..." (because
					// transformation is done later).

					String property = parts[1];
					if (forbiddenProperties.contains(property))
						continue;

					Set<YAGOMetaFact> metaFactsOfFact = temporalMetaFacts.get(previousId);

					// a) Relations to events
					if (allEventPagesDataSet.getEventByWikipediaLabel(yagoLabelExtractor1.getLanguage(),
							yagoLabelExtractor1.getWikipediaLabel()) != null
							|| allEventPagesDataSet.getEventByWikipediaLabel(yagoLabelExtractor2.getLanguage(),
									yagoLabelExtractor2.getWikipediaLabel()) != null) {
						eventFactsWriter.write(wikipediaLabel1 + "\t" + property + "\t" + wikipediaLabel2 + "\n");
						if (metaFactsOfFact != null) {
							for (YAGOMetaFact metaFact : metaFactsOfFact) {
								eventFactsWriter
										.write("\t" + metaFact.getProperty() + "\t" + metaFact.getObject() + "\n");
							}
						}
					}
					// b) Facts with temporal scope
					else if (metaFactsOfFact != null) {
						temporalFactsWriter.write(wikipediaLabel1 + "\t" + property + "\t" + wikipediaLabel2 + "\n");
						for (YAGOMetaFact metaFact : metaFactsOfFact) {
							temporalFactsWriter
									.write("\t" + metaFact.getProperty() + "\t" + metaFact.getObject() + "\n");
						}
					}
					// c) non-temporal relations without events
					else if (allEventPagesDataSet.getEntityWithExistenceTimeByWikipediaLabel(
							yagoLabelExtractor1.getLanguage(), yagoLabelExtractor1.getWikipediaLabel()) != null
							|| allEventPagesDataSet.getEntityWithExistenceTimeByWikipediaLabel(
									yagoLabelExtractor2.getLanguage(),
									yagoLabelExtractor2.getWikipediaLabel()) != null) {
						entityFactsWriter.write(wikipediaLabel1 + "\t" + property + "\t" + wikipediaLabel2 + "\n");
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

		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} finally {
			eventFactsWriter.close();
			entityFactsWriter.close();
			temporalFactsWriter.close();
		}

	}

	private void extractTriplesWithEventsAndLiterals(Map<String, Set<YAGOMetaFact>> temporalMetaFacts) {

		System.out.println("Collect YAGO facts with events and literals.");

		Set<String> forbiddenProperties = new HashSet<String>();
		forbiddenProperties.addAll(YAGOEventLocationsExtractor.loadLocationProperties());

		PrintWriter eventLiteralsFactsWriter = null;
		try {
			eventLiteralsFactsWriter = FileLoader.getWriter(FileName.YAGO_EVENT_LITERALS_FACTS);

			BufferedReader br = null;
			try {
				try {
					br = FileLoader.getReader(FileName.YAGO_LITERAL_FACTS);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				String previousId = null;

				while ((line = br.readLine()) != null) {

					if (line.startsWith("#@ <id")) {
						previousId = line.substring(3);
					}

					if (line.isEmpty() || line.startsWith("@") || line.startsWith("#"))
						continue;

					String[] parts = line.split("\t");

					YAGOLabelExtractor yagoLabelExtractor1 = new YAGOLabelExtractor(parts[0], this.languages);
					yagoLabelExtractor1.extractLabel();
					if (!yagoLabelExtractor1.isValid())
						continue;

					String wikipediaLabel1 = parts[0].substring(1, parts[0].length() - 1);

					// check for events using the actual labels. But write to
					// files the original labels like "de/..." (because
					// transformation is done later).

					String property = parts[1];
					if (forbiddenProperties.contains(property))
						continue;

					Set<YAGOMetaFact> metaFactsOfFact = temporalMetaFacts.get(previousId);

					// a) Relations to events
					if (allEventPagesDataSet.getEventByWikipediaLabel(yagoLabelExtractor1.getLanguage(),
							yagoLabelExtractor1.getWikipediaLabel()) != null) {
						eventLiteralsFactsWriter.write(wikipediaLabel1 + "\t" + property + "\t" + parts[2] + "\n");
						if (metaFactsOfFact != null) {
							for (YAGOMetaFact metaFact : metaFactsOfFact) {
								eventLiteralsFactsWriter
										.write("\t" + metaFact.getProperty() + "\t" + metaFact.getObject() + "\n");
							}
						}
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

		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
		} finally {
			eventLiteralsFactsWriter.close();
		}

	}

}
