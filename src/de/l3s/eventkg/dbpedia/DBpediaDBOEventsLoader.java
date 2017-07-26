package de.l3s.eventkg.dbpedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaDBOEventsLoader extends Extractor {

	private PrintWriter resultsWriter;
	private PrintWriter resultsWriterBlacklist;

	public DBpediaDBOEventsLoader(List<Language> languages) {
		super("DBpediaDBOEventsLoader", Source.DBPEDIA, "?", languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Set<String> foundEvents = new HashSet<String>();
		Set<String> targetObjects = loadEventObjects();
		Set<String> blacklistObjects = loadBlacklistObjects();

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_DBO_EVENTS_FILE_NAME, language);
			resultsWriterBlacklist = FileLoader.getWriter(FileName.DBPEDIA_DBO_NO_EVENTS_FILE_NAME, language);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BufferedReader br = null;
		try {
			try {
				br = FileLoader.getReader(FileName.DBPEDIA_RELATIONS_TRANSITIVE, language);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] parts = line.split(" ");
				String object = parts[2];

				if (targetObjects.contains(object) || blacklistObjects.contains(object)) {

					String subject = parts[0];
					String property = parts[1];

					if (!subject.contains("resource"))
						continue;

					subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));
					object = object.substring(object.lastIndexOf("/") + 1, object.lastIndexOf(">"));

					String fileLine = subject + Config.TAB + property + Config.TAB + object;
					if (foundEvents.contains(fileLine))
						continue;

					if (blacklistObjects.contains(object))
						resultsWriterBlacklist.write(fileLine + Config.NL);
					else
						resultsWriter.write(fileLine + Config.NL);

					foundEvents.add(fileLine);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				resultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static Set<String> loadEventObjects() {

		Set<String> targetProperties = new HashSet<String>();

		targetProperties.add("<http://dbpedia.org/ontology/Event>");
		targetProperties.add("<http://schema.org/Event>");
		targetProperties.add("<http://dbpedia.org/ontology/SocietalEvent>");
		targetProperties.add("<http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#Event>");

		return targetProperties;
	}

	public static Set<String> loadBlacklistObjects() {

		// create a set of entities that cannot be events. This e.g. applies to
		// football leagues which are seen as events in the French DBpedia, but
		// nowhere else (English DBpedia has them as Organization).

		Set<String> blacklistObjects = new HashSet<String>();

		blacklistObjects.add("<http://dbpedia.org/ontology/Organisation>");
		blacklistObjects.add("<http://schema.org/Organization>");

		return blacklistObjects;
	}
}
