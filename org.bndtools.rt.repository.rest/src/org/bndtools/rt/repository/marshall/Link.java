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
