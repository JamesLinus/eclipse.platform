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
package org.eclipse.update.tests.parser;
import java.net.URL;

import org.eclipse.update.core.*;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestCategories extends UpdateManagerTestCase {
	/**
	 * Constructor for Test1
	 */
	public TestCategories(String arg0) {
		super(arg0);
	}

	public void testCategories() throws Exception {

		URL remoteUrl = new URL(SOURCE_FILE_SITE + "xmls/site1/");
		IUpdateSite remoteSite = SiteManager.getUpdateSite(remoteUrl,null);

		IFeatureReference[] refs = remoteSite.getFeatureReferences();
		//ICategory[] categories = remoteSite.getCategories();

		ICategory featureCategory = remoteSite.getCategories(refs[0])[0];

		assertEquals("UML tools", featureCategory.getLabel());

	}

	public void testOrderedCategories() throws Exception {

		URL remoteUrl = new URL(SOURCE_FILE_SITE + "xmls/site1/");
		IUpdateSite remoteSite = SiteManager.getUpdateSite(remoteUrl,null);

		ICategory[] categories = remoteSite.getCategories();
		for (int i = 0; i < categories.length; i++) {
			System.out.println("Cat ordered->" + categories[i].getName());
		}

		assertEquals("Eclipse tools", categories[1].getLabel());

	}

	public void testTranslatedCategories() throws Exception {

		IUpdateSite remoteSite = SiteManager.getUpdateSite(SOURCE_HTTP_SITE,null);

		ICategory[] categories = remoteSite.getCategories();

		assertEquals("Required Drivers", categories[0].getLabel());

	}
}
