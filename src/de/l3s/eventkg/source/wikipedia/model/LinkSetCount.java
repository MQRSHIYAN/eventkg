package de.l3s.eventkg.source.wikipedia.model;

import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.integration.model.relation.GenericRelation;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixEnum;
import de.l3s.eventkg.integration.model.relation.prefix.PrefixList;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class LinkSetCount {

	private Event event;

	private Entity entity;

	private int count;

	private Language language;

	public LinkSetCount(Event event, Entity entity, int count, Language language) {
		super();
		this.event = event;
		this.entity = entity;
		this.count = count;
		this.language = language;
	}

	public Event getEvent() {
		return event;
	}

	public Entity getEntity() {
		return entity;
	}

	public int getCount() {
		return count;
	}

	public Language getLanguage() {
		return language;
	}

	public GenericRelation toGenericRelation() {
		// we only store links where at least one event is involved
		return new GenericRelation(event, DataSets.getInstance().getDataSet(this.language, Source.WIKIPEDIA),
				PrefixList.getInstance().getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "linked_with", entity, (double) count,
				false);
	}
	
	public GenericRelation toGenericRelationSubjectObjectReverted() {
		// we only store links where at least one event is involved
		return new GenericRelation(entity, DataSets.getInstance().getDataSet(this.language, Source.WIKIPEDIA),
				PrefixList.getInstance().getPrefix(PrefixEnum.EVENT_KG_SCHEMA), "linked_with", event, (double) count,
				false);
	}


}