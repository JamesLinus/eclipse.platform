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
package org.eclipse.update.tests.types;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.FeaturePackagedContentProvider;
import org.eclipse.update.internal.core.SiteContentProvider;
import org.eclipse.update.tests.UpdateManagerTestCase;
import org.eclipse.update.tests.implementation.SiteFTPFactory;

public class TestSiteType extends UpdateManagerTestCase {

	/**
	 * Test the getFeatures()
	 */
	public TestSiteType(String arg0) {
		super(arg0);
	}

	/**
	 * @throws Exception
	 */
	public void testSiteType() throws Exception {

		String featurePath = dataPath + "SiteTypeExamples/site1/site.xml";
		ISite site = SiteManager.getSite(new File(featurePath).toURL());
		IFeatureReference ref = site.getFeatureReferences()[0];
		IFeature feature = ref.getFeature();

		assertTrue(site.getSiteContentProvider() instanceof SiteContentProvider);
		assertTrue(((Site) site).getType().equals("org.eclipse.update.core.http"));
		assertTrue(feature.getFeatureContentProvider() instanceof FeaturePackagedContentProvider);

	}

	/**
		 * @throws Exception
		 */
	public void testFTPSiteType() throws Exception {

		ISite site = SiteManager.getSite(new URL(SOURCE_FILE_SITE + "FTPLikeSite/"));

		// should not find the mapping
		// but then should attempt to read the XML file
		// found a new type
		// call the new type
		assertTrue(
			"Wrong site type",
			site.getType().equals("org.eclipse.update.tests.ftp"));
		assertTrue(
			"Wrong file",
			site.getURL().getFile().equals("/" + SiteFTPFactory.FILE));

	}

	public void testParseValid1() throws Exception {
		try {
			URL remoteURL = new URL(SOURCE_FILE_SITE + "parsertests/siteftp.xml");
			ISite site = SiteManager.getSite(remoteURL);
			site.getArchives();
		} catch (CoreException e) {
			if (e.getMessage().indexOf("</feature>") == -1) {
				throw e;
			}
		}
	}

}
