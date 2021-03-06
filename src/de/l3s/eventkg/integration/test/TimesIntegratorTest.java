package de.l3s.eventkg.integration.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStore;
import de.l3s.eventkg.integration.integrator.TimesIntegrator;
import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.DateWithGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.relation.StartTime;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class TimesIntegratorTest {

	public static void main(String[] args) throws ParseException {

		DataSets.getInstance().addDataSet(Language.DE, Source.DBPEDIA, "http://de.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.FR, Source.DBPEDIA, "http://fr.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.RU, Source.DBPEDIA, "http://ru.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.PT, Source.DBPEDIA, "http://pt.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.EN, Source.DBPEDIA, "http://dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.DE, Source.WIKIPEDIA, "https://dumps.wikimedia.org/dewiki/");
		DataSets.getInstance().addDataSet(Language.FR, Source.WIKIPEDIA, "https://dumps.wikimedia.org/frwiki/");
		DataSets.getInstance().addDataSet(Language.RU, Source.WIKIPEDIA, "https://dumps.wikimedia.org/ruwiki/");
		DataSets.getInstance().addDataSet(Language.PT, Source.WIKIPEDIA, "https://dumps.wikimedia.org/ptwiki/");
		DataSets.getInstance().addDataSet(Language.EN, Source.WIKIPEDIA, "https://dumps.wikimedia.org/enwiki/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.WIKIDATA,
				"https://dumps.wikimedia.org/wikidatawiki/entities/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.YAGO,
				"https://www.mpi-inf.mpg.de/departments/databases-and-information-systems/research/yago-naga/yago/downloads/");
		DataSets.getInstance().addDataSet(Language.EN, Source.WCE, "http://wikitimes.l3s.de/Resource.jsp");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.EVENT_KG, "http://eventkg.l3s.uni-hannover.de/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_TIME_2,
				"http://eventkg.l3s.uni-hannover.de/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.EVENT_KG, "http://eventkg.l3s.uni-hannover.de/");

		// ~~~

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		List<Language> languages = new ArrayList<Language>();
		languages.add(Language.EN);
		languages.add(Language.DE);

		TimesIntegrator timesIntegrator = new TimesIntegrator(languages, null);
		timesIntegrator.init();

		Entity entity1 = new Entity("Q4118977");
		entity1.addWikipediaLabel(Language.EN, "Withdrawal_of_U.S._troops_from_Iraq");

		// ~~~

		StartTime time1 = new StartTime(entity1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO),
				new DateWithGranularity(dateFormat.parse("1896-12-17"), DateGranularity.DAY));
		StartTime time2 = new StartTime(entity1, DataSets.getInstance().getDataSetWithoutLanguage(Source.YAGO),
				new DateWithGranularity(dateFormat.parse("2009-06-30"), DateGranularity.DAY));
		StartTime time3 = new StartTime(entity1, DataSets.getInstance().getDataSet(Language.EN, Source.DBPEDIA),
				new DateWithGranularity(dateFormat.parse("2011-12-18"), DateGranularity.DAY));
		StartTime time4 = new StartTime(entity1, DataSets.getInstance().getDataSet(Language.DE, Source.DBPEDIA),
				new DateWithGranularity(dateFormat.parse("1895-12-17"), DateGranularity.DAY));

		DataStore.getInstance().addStartTime(time1);
		DataStore.getInstance().addStartTime(time2);
		DataStore.getInstance().addStartTime(time3);
		DataStore.getInstance().addStartTime(time4);

		timesIntegrator.integrateTimes(entity1, true, 0);
		timesIntegrator.addIntegratedTimesToDataStore();
		
		System.out.println("");

		for (StartTime st : DataStore.getInstance().getStartTimes()) {
			System.out.println(st.getDataSet().getSource() + "\t" + dateFormat.format(st.getStartTime().getDate()));
		}
	}

}
