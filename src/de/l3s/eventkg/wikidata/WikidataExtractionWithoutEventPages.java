package de.l3s.eventkg.wikidata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.wikidata.processors.EventSubClassProcessor;
import de.l3s.eventkg.wikidata.processors.FactsWithTemporalSnaksProcessor;
import de.l3s.eventkg.wikidata.processors.TemporalPropertiesExtractor;
import de.l3s.eventkg.wikidata.processors.TitlesAndDescriptionsExtractor;
import de.l3s.eventkg.wikidata.processors.WikidataLocationsExtractor;

/**
 * Runs through the Wikidata dump file to extract: <br>
 * + id to label mappings for items <br>
 * + locations of items <br>
 * + start, end and point of times of items <br>
 */
public class WikidataExtractionWithoutEventPages extends Extractor {

	public static void main(String[] args) {
		Config.init("config_eventkb_local.txt");
		List<Language> languages = new ArrayList<Language>();
		for (String language : Config.getValue("languages").split(",")) {
			languages.add(Language.getLanguage(language));
		}
		WikidataExtractionWithoutEventPages w = new WikidataExtractionWithoutEventPages(languages);
		w.run();
	}

	public WikidataExtractionWithoutEventPages(List<Language> languages) {
		super("WikidataExtractionWithoutEventPages", Source.WIKIDATA,
				"Extract id to label mappings, locations and times.", languages);
	}

	public void run() {

		// ID to label mapping
		TitlesAndDescriptionsExtractor idToWikipediaMappingExtractor = null;
		try {
			idToWikipediaMappingExtractor = new TitlesAndDescriptionsExtractor(languages);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Locations
		WikidataLocationsExtractor locationsExtractor = null;
		try {
			locationsExtractor = new WikidataLocationsExtractor();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// temporal properties
		TemporalPropertiesExtractor temporalPropertiesExtractor = null;
		try {
			temporalPropertiesExtractor = new TemporalPropertiesExtractor();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// facts with temporal qualifiers
		FactsWithTemporalSnaksProcessor factsWithTemporalSnaksProcessor = null;
		try {
			factsWithTemporalSnaksProcessor = new FactsWithTemporalSnaksProcessor();
		} catch (IOException e) {
			e.printStackTrace();
		}

		EventSubClassProcessor subclassPropertiesProcessor = null;
		try {
			subclassPropertiesProcessor = new EventSubClassProcessor();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Set<EntityDocumentDumpProcessor> processors = new HashSet<EntityDocumentDumpProcessor>();
		processors.add(idToWikipediaMappingExtractor);
		processors.add(temporalPropertiesExtractor);
		processors.add(locationsExtractor);
		processors.add(subclassPropertiesProcessor);
		processors.add(factsWithTemporalSnaksProcessor);

		WikidataLoader loader = new WikidataLoader();
		loader.loadWikidataDumpFromFile(processors);
	}

}