/*******************************************************************************
 * Copyright (c) 2012 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package org.bndtools.rt.repository.marshall;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

public class LinkGenerator {
	private JsonGenerator out;

	public LinkGenerator(JsonGenerator out) {
		this.out = out;
	}
	
	public void writeLink(Link link) throws JsonGenerationException, IOException {
		out.writeStartObject();
		out.writeStringField("rel", link.getRelation());
		out.writeStringField("href", link.getHref().toString());
		out.writeEndObject();
	}
	
	public void writeLinkArrayField(Collection<Link> links) throws JsonGenerationException, IOException {
		out.writeArrayFieldStart("links");
		for (Link link : links) {
			writeLink(link);
		}
		out.writeEndArray();
	}
}
