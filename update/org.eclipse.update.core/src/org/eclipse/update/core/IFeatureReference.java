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

import java.net.*;

import org.eclipse.core.runtime.*;

/**
 * Feature reference.
 * A reference to a feature.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.FeatureReference
 * @since 2.0
 */
public interface IFeatureReference extends IAdaptable,IPlatformEnvironment {

	/**
	 * Returns the referenced feature URL.
	 * 
	 * @return feature URL 
	 * @since 2.0 
	 */
	public URL getURL();

	/**
	 * Returns the update site for the referenced feature
	 * 
	 * @return feature site
	 * @since 2.0 
	 */
	public ISite getSite();

	/**
	 * Returns the label for the referenced feature
	 *
	 * @return the label
	 * @since 2.1
	 */
	public String getName();


	/**
	 * Returns the feature identifier.
	 * 
	 * @return the feature identifier.
	 * @exception CoreException
	 * @since 2.0 
	 */
	public VersionedIdentifier getVersionedIdentifier();

	/**
	 * Returns <code>true</code> if this feature is patching another feature,
	 * <code>false</code> otherwise
	 * @return boolean
	 * @since 2.1
	 */
	public boolean isPatch();
	
	/**
	 * Returns the referenced feature.
	 * This is a factory method that creates the full feature object.
	 * 
	 * @param monitor the progress monitor
	 * @return the feature referenced by this feature ref
	 * @since 3.0 
	 */
	public IFeature getFeature(IProgressMonitor monitor) throws CoreException;
	
}
