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

import java.net.*;
import java.util.*;

import org.eclipse.update.core.*;

/**
 * Feature category definition model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * @see org.eclipse.update.core.Category
 * @since 2.0
 */

public class Category extends ModelObject implements ICategory {

	private String name;
	private String label;
	private String localizedLabel;
	private URLEntry description;
	private List featureReferences;
	private static Comparator comp;

	/**
	 * Creates an uninitialized model object.
	 * 
	 * @since 2.0
	 */
	public Category() {
		super();
	}

	/**
	 * Retrieve the name of the category.
	 * 
	 * @return category name, or <code>null</code>.
	 * @since 2.0
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieve the displayable label for the category. If the model
	 * object has been resolved, the label is localized.
	 * 
	 * @return displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getLabel() {
		if (localizedLabel != null)
			return localizedLabel;
		else
			return label;
	}

	/**
	 * Retrieve the non-localized displayable label for the category.
	 * 
	 * @return non-localized displayable label, or <code>null</code>.
	 * @since 2.0
	 */
	public String getLabelNonLocalized() {
		return label;
	}

	/**
	 * Sets the category displayable label.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param label displayable label, or resource key
	 * @since 2.0
	 */
	void setLabel(String label) {
		assertIsWriteable();
		this.label = label;
		this.localizedLabel = null;
	}

	/**
	 * Sets the category name.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param name category name
	 * @since 2.0
	 */
	void setName(String name) {
		assertIsWriteable();
		this.name = name;
	}

	/**
	 * Sets the category description.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param description category description
	 * @since 2.0
	 */
	void setDescription(URLEntry description) {
		assertIsWriteable();
		this.description = description;
	}

	/**
	 * Marks the model object as read-only.
	 * 
	 * @since 2.0
	 */
	public void markReadOnly() {
		super.markReadOnly();
		markReferenceReadOnly((URLEntry)getDescription());
	}

	/**
	 * Resolve the model object.
	 * Any URL strings in the model are resolved relative to the 
	 * base URL argument. Any translatable strings in the model that are
	 * specified as translation keys are localized using the supplied 
	 * resource bundle.
	 * 
	 * @param base URL
	 * @param bundle resource bundle
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	public void resolve(URL base,URL bundleURL)
		throws MalformedURLException {
		// resolve local elements
		localizedLabel = resolveNLString(bundleURL, label);

		// delegate to references
		resolveReference((URLEntry)getDescription(),base, bundleURL);
	}

	/**
	 * Compare two category models for equality.
	 * 
	 * @see Object#equals(Object)
	 * @since 2.0
	 */
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof Category) {
			Category otherCategory = (Category) obj;
			result = getName().equalsIgnoreCase(otherCategory.getName());
		}
		return result;
	}

	/**
	 * Compute hash code for category model.
	 * 
	 * @see Object#hashCode()
	 * @since 2.0
	 */
	public int hashCode() {
		return getName().hashCode();
	}

	/**
	 * Returns a comparator for category models.
	 * 
	 * @return comparator
	 * @since 2.0
	 */
	public static Comparator getComparator() {
		if (comp == null) {
			comp = new Comparator() {
				/*
				 * @see Comparator#compare(Object,Object)
				 * Returns 0 if versions are equal.
				 * Returns -1 if object1 is after than object2.
				 * Returns +1 if object1 is before than object2.
				 */
				public int compare(Object o1, Object o2) {

					Category cat1 = (Category) o1;
					Category cat2 = (Category) o2;

					if (cat1.equals(cat2))
						return 0;
					return cat1.getName().compareTo(cat2.getName());
				}
			};
		}
		return comp;
	}
	
	/**
	 * @see org.eclipse.update.core.model.ModelObject#getPropertyName()
	 */
	protected String getPropertyName() {
		return Site.SITE_FILE;
	}	
	
	/**
	 * Retrieve the detailed category description
	 * @see ICategory#getDescription()
	 */
	public IURLEntry getDescription() {
		return description;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.update.core.ICategory#getFeatures()
	 */
	public IFeatureReference[] getFeatureReferences() {
		if (featureReferences == null)
			return new FeatureReference[0];
		return (FeatureReference[]) featureReferences.toArray(arrayTypeFor(featureReferences));
	}

	/**
	 * Adds a feature reference model to category.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureReference feature reference model
	 * @since 2.0
	 */
	void addFeatureReference(FeatureReference featureReference) {
		assertIsWriteable();
		if (this.featureReferences == null)
			this.featureReferences = new ArrayList();
		// PERF: do not check if already present 
		//if (!this.featureReferences.contains(featureReference))
			this.featureReferences.add(featureReference);
	}
	

	/**
	 * Removes a feature reference model from category.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureReference feature reference model
	 * @since 2.0
	 */
	void removeFeatureReference(FeatureReference featureReference) {
		assertIsWriteable();
		if (this.featureReferences != null)
			this.featureReferences.remove(featureReference);
	}

}
