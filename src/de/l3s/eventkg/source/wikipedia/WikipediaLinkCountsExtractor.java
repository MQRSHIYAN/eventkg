package de.l3s.eventkg.source.wikipedia;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.LineIterator;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.source.wikipedia.model.LinksToCount;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;
import gnu.trove.set.hash.THashSet;

public class WikipediaLinkCountsExtractor extends Extractor {

	private AllEventPagesDataSet allEventPagesDataSet;

	private Set<LinksToCount> linksToCounts;

	private static final boolean WRITE_TO_FILES = false;

	public WikipediaLinkCountsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("WikipediaLinkCountsExtractor", de.l3s.eventkg.meta.Source.WIKIPEDIA,
				"Extract Wikipedia link counts between entities and events.", languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		System.out.println("Collect links from and to event pages.");
		extractRelations();
	}

	private void extractRelations() {

		this.linksToCounts = new THashSet<LinksToCount>();

		for (Language language : this.languages) {
			for (File child : FileLoader.getFilesList(FileName.WIKIPEDIA_LINK_COUNTS, language)) {
				processFileIterator(child, language);
			}
		}

		writeResults();
	}

	private void writeResults() {

		if (!WRITE_TO_FILES) {

			int i = 0;
			int size = this.linksToCounts.size();

			System.out.println("Found " + size + " relations.");
			for (Iterator<LinksToCount> it = this.linksToCounts.iterator(); it.hasNext();) {
				LinksToCount linkCount = it.next();
				if (i % 100000 == 0)
					System.out.println("\t" + i + "/" + size + " (" + ((double) i / size) + ")");
				i += 1;
				DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());
				it.remove();
			}
			System.out.println(" Finished loading link counts.");

			this.linksToCounts = null;

		} else {

			System.out.println("Write results: Link counts");
			PrintWriter writer = null;
			try {
				writer = FileLoader.getWriter(FileName.ALL_LINK_COUNTS);

				for (LinksToCount linkCount : this.linksToCounts) {

					DataStore.getInstance().addLinkRelation(linkCount.toGenericRelation());

					writer.write(linkCount.getEvent().getWikidataId());
					writer.write(Config.TAB);
					writer.write(linkCount.getEvent().getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(linkCount.getEntity().getWikidataId());
					writer.write(Config.TAB);
					writer.write(linkCount.getEntity().getWikipediaLabelsString(this.languages));
					writer.write(Config.TAB);
					writer.write(String.valueOf(String.valueOf(linkCount.getCount())));
					writer.write(Config.TAB);
					writer.write(linkCount.getLanguage().getLanguageLowerCase());
					writer.write(Config.NL);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				writer.close();
			}

		}
	}

	private void processFileIterator(File file, Language language) {

		LineIterator it = null;
		try {
			it = FileLoader.getLineIterator(file.getAbsolutePath(), false);
			while (it.hasNext()) {
				String line = it.nextLine();
				processLine(line, language, file);
			}
			System.out.println(this.linksToCounts.size() + "\t" + file.getName());

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				it.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void processLine(String line, Language language, File file) {
		String[] parts = line.split(Config.TAB);
		String pageTitle = parts[1].replaceAll(" ", "_");
		Entity pageEntity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language, pageTitle);

		if (pageEntity != null) {

			for (int i = 2; i < parts.length; i++) {
				String linkedPageTitle = parts[i].split(" ")[0].replaceAll(" ", "_");
				Entity linkedEntity = allEventPagesDataSet.getWikidataIdMappings().getEntityByWikipediaLabel(language,
						linkedPageTitle);
				int count = 0;
				if (linkedEntity != null) {

					try {
						count = Integer.valueOf(parts[i].split(" ")[1]);
					} catch (Exception e) {
						System.out.println("Warning: Error in file " + file.getName() + " for " + pageTitle + ".");
						continue;
					}

					if (pageEntity.isEvent() || linkedEntity.isEvent()) {

						Entity pageEntity2 = pageEntity;
						Entity linkedEntity2 = linkedEntity;

						this.linksToCounts.add(new LinksToCount(pageEntity2, linkedEntity2, count, language, true));

						// this.linkedByCounts.add(
						// new LinkedByCount(linkedEntity2, pageEntity2,
						// count, language));
					} else {

						if (areConnectedViaRelation(pageEntity, linkedEntity)) {
							this.linksToCounts.add(new LinksToCount(pageEntity, linkedEntity, count, language, false));
						}

					}

				}
			}
		}
	}

	private boolean areConnectedViaRelation(Entity entity1, Entity entity2) {
		return DataStore.getInstance().getConnectedEntities().containsKey(entity1)
				&& DataStore.getInstance().getConnectedEntities().get(entity1).contains(entity2);
	}

}
