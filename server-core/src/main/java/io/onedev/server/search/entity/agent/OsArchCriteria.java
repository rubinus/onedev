package io.onedev.server.search.entity.agent;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Agent;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.util.match.WildcardUtils;

public class OsArchCriteria extends EntityCriteria<Agent> {

	private static final long serialVersionUID = 1L;

	private String value;
	
	public OsArchCriteria(String value) {
		this.value = value;
	}

	@Override
	public Predicate getPredicate(CriteriaQuery<?> query, Root<Agent> root, CriteriaBuilder builder) {
		Path<String> attribute = root.get(Agent.PROP_OS_ARCH);
		String normalized = value.toLowerCase().replace("*", "%");
		return builder.like(builder.lower(attribute), normalized);
	}

	@Override
	public boolean matches(Agent agent) {
		String name = agent.getName();
		return name != null && WildcardUtils.matchString(value.toLowerCase(), name.toLowerCase());
	}

	@Override
	public String toStringWithoutParens() {
		return quote(Agent.NAME_OS_ARCH) + " " 
				+ AgentQuery.getRuleName(AgentQueryLexer.Is) + " " 
				+ quote(value);
	}
	
}
