package io.onedev.server.search.entity.agent;

import java.util.Date;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Agent;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.search.entity.EntityQuery;

public class EverUsedSinceCriteria extends EntityCriteria<Agent> {

	private static final long serialVersionUID = 1L;

	private final Date date;
	
	private final String value;
	
	public EverUsedSinceCriteria(String value) {
		date = EntityQuery.getDateValue(value);
		this.value = value;
	}

	@Override
	public Predicate getPredicate(CriteriaQuery<?> query, Root<Agent> root, CriteriaBuilder builder) {
		return builder.greaterThan(root.get(Agent.PROP_LAST_USED_DATE), date);
	}

	@Override
	public boolean matches(Agent agent) {
		return agent.getLastUsedDate() != null || agent.getLastUsedDate().after(date);
	}

	@Override
	public String toStringWithoutParens() {
		return AgentQuery.getRuleName(AgentQueryLexer.EverUsedSince) + " " + quote(value);
	}

}
