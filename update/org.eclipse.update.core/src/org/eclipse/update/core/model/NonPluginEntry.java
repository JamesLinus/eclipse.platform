/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;

import org.eclipse.update.core.*;

/**
 * Non-plug-in entry model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * @see org.eclipse.update.core.NonPluginEntry
 * @since 2.0
 */
public class NonPluginEntry extends ContentEntry implements INonPluginEntry {

	private String id = null;

	/**
	 * Creates a uninitialized non-plug-in entry model object.
	 * 
	 * @since 2.0
	 */
	public NonPluginEntry() {
		super();
	}

	/**
	 * Returns the entry identifier.
	 *
	 * @return entry identifier, or <code>null</code>
	 * @since 2.0
	 */
	public String getIdentifier() {
		return id;
	}

	/**
	 * Sets the entry identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 *
	 * @param id entry identifier.
	 * @since 2.0
	 */
	void setIdentifier(String id) {
		assertIsWriteable();
		this.id = id;
	}
}
