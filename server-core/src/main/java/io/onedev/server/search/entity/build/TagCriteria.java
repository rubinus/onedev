package io.onedev.server.search.entity.build;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.jgit.lib.Constants;

import io.onedev.server.model.Build;
import io.onedev.server.search.entity.EntityCriteria;
import io.onedev.server.util.match.WildcardUtils;

public class TagCriteria extends EntityCriteria<Build> {

	private static final long serialVersionUID = 1L;

	private final String value;
	
	public TagCriteria(String value) {
		this.value = value;
	}

	@Override
	public Predicate getPredicate(CriteriaQuery<?> query, Root<Build> root, CriteriaBuilder builder) {
		Path<String> attribute = root.get(Build.PROP_REF_NAME);
		String normalized = Constants.R_TAGS + value.toLowerCase().replace("*", "%");
		return builder.like(builder.lower(attribute), normalized);
	}

	@Override
	public boolean matches(Build build) {
		String tag = build.getTag();
		return tag != null && WildcardUtils.matchString(value.toLowerCase(), tag.toLowerCase());
	}

	@Override
	public String toStringWithoutParens() {
		return quote(Build.NAME_TAG) + " " 
				+ BuildQuery.getRuleName(BuildQueryLexer.Is) + " " 
				+ quote(value);
	}

}
