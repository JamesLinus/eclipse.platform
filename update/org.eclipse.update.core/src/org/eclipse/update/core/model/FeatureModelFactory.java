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

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.xml.sax.*;

/**
 * Default feature model factory.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead subclass the provided base implementation 
 * of this factory.
 * </p>
 * @see org.eclipse.update.core.BaseFeatureFactory
 * @since 2.0
 */

public class FeatureModelFactory {


	private static FeatureParser parser = new FeatureParser();

	/**
	 * Creates a default model factory.
	 * 
	 * @since 2.0
	 */
	public FeatureModelFactory() {
		super();
	}

	/**
	 * Creates and populates a default feature from stream.
	 * The parser assumes the stream contains a default feature manifest
	 * (feature.xml) as documented by the platform.
	 * 
	 * @param stream feature stream
	 * @return populated feature model
	 * @exception ParsingException
	 * @exception IOException
	 * @exception SAXException
	 * @since 2.0
	 */
	public FeatureModel parseFeature(InputStream stream)
		throws CoreException, SAXException {
		parser.init(this);
		FeatureModel featureModel = null;
		try {
			featureModel = parser.parse(stream);
			if (parser.getStatus()!=null) {
				// some internalError were detected
				IStatus status = parser.getStatus();
				throw new CoreException(status);
			}
		} catch (IOException e) {
			throw Utilities.newCoreException(Policy.bind("FeatureModelFactory.ErrorAccesingFeatureStream"), e); //$NON-NLS-1$
		}
		return featureModel;
	}

	/**
	 * Create a default feature model.
	 * 
	 * @see FeatureModel
	 * @return feature model
	 * @since 2.0
	 */
	public FeatureModel createFeatureModel() {
		return new FeatureModel();
	}

	/**
	 * Create a default included feature reference model.
	 * 
	 * @see IncludedFeatureReferenceModel
	 * @return feature model
	 * @since 2.1
	 */
	public IncludedFeatureReferenceModel createIncludedFeatureReferenceModel() {
		return new IncludedFeatureReferenceModel();
	}


	/**
	 * Create a default install handler model.
	 * 
	 * @see InstallHandlerEntryModel
	 * @return install handler entry model
	 * @since 2.0
	 */
	public InstallHandlerEntry createInstallHandlerEntryModel() {
		return new InstallHandlerEntry();
	}

	/**
	 * Create a default import dependency model.
	 * 
	 * @see ImportModel
	 * @return import dependency model
	 * @since 2.0
	 */
	public Import createImportModel() {
		return new Import();
	}

	/**
	 * Create a default plug-in entry model.
	 * 
	 * @see PluginEntryModel
	 * @return plug-in entry model
	 * @since 2.0
	 */
	public PluginEntry createPluginEntryModel() {
		return new PluginEntry();
	}

	/**
	 * Create a default non-plug-in entry model.
	 * 
	 * @see NonPluginEntryModel
	 * @return non-plug-in entry model
	 * @since 2.0
	 */
	public NonPluginEntry createNonPluginEntryModel() {
		return new NonPluginEntry();
	}

	/**
	 * Create a default annotated URL model.
	 * 
	 * @see URLEntryModel
	 * @return annotated URL model
	 * @since 2.0
	 */
	public URLEntry createURLEntryModel() {
		return new URLEntry();
	}
}
