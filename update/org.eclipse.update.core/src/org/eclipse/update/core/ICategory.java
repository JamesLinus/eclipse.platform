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
package org.eclipse.update.core;

import org.eclipse.core.runtime.*;

/**
 * Feature category definition.
 * A site can organize its features into categories. Categories
 * can be further organized into hierarchies. Each category name
 * is a composed of the name of its parent and a simple identifier
 * separated by a slash ("/"). For example <code>tools/utilities/print</code>
 * defines a category that is a child of <code>tools/utilities</code> and
 * grandchild of <code>tools</code>.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.Category
 * @since 2.0
 */
public interface ICategory  extends IAdaptable{

	/** 
	 * Retrieve the name of the category. The name can be a simple
	 * token (root category) or a number of slash-separated ("/") 
	 * tokens.
	 * 
	 * @return the category name
	 * @since 2.0 
	 */
	public String getName();

	/**
	 * Retrieve the displayable label for the category
	 * 
	 * @return displayable category label, or <code>null</code>
	 * @since 2.0 
	 */
	public String getLabel();

	/** 
	 * Retrieve the detailed category description
	 * 
	 * @return category description, or <code>null</code>
	 * @since 2.0 
	 */
	public IURLEntry getDescription();
	
	/**
	 * Retrieves the feature under this category.
	 * @return array of feature references; empty array if no features in this category
	 */
	public IFeatureReference[] getFeatureReferences();
}
