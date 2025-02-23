package io.onedev.server.search.entity.build;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import io.onedev.server.model.Build;

import io.onedev.server.search.entity.EntityCriteria;

public class SuccessfulCriteria extends EntityCriteria<Build> {

	private static final long serialVersionUID = 1L;

	@Override
	public Predicate getPredicate(CriteriaQuery<?> query, Root<Build> root, CriteriaBuilder builder) {
		Path<?> attribute = root.get(Build.PROP_STATUS);
		return builder.equal(attribute, Build.Status.SUCCESSFUL);
	}

	@Override
	public boolean matches(Build build) {
		return build.getStatus() == Build.Status.SUCCESSFUL;
	}

	@Override
	public String toStringWithoutParens() {
		return BuildQuery.getRuleName(BuildQueryLexer.Successful);
	}

}
