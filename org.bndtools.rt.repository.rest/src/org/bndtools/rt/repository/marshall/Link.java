package org.bndtools.rt.repository.marshall;

import java.net.URI;

public class Link {

	private final String relation;
	private final URI href;
	
	public Link(String relation, URI href) {
		this.relation = relation;
		this.href = href;
	}

	public String getRelation() {
		return relation;
	}

	public URI getHref() {
		return href;
	}
	
}
