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
package org.eclipse.update.tests.api;
import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.internal.configuration.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestSiteManagerAPI extends UpdateManagerTestCase {
	
	/**
	 * Test the getFeatures()
	 */
	public TestSiteManagerAPI(String arg0) {
		super(arg0);
	}
	
	public void testFile() throws Exception {
		ISite fileSite = SiteManager.getSite(TARGET_FILE_SITE);
		String site = fileSite.getURL().toExternalForm();		
		assertEquals(TARGET_FILE_SITE.toExternalForm(), site);
	}
	
	public void testUnknown() throws Exception {
		URL url = new URL("ftp://255.255.255.255/");
		try {
		SiteManager.getSite(url);
		fail("Connected to ftp://255.255.255.255/, should not happen");
		} catch (CoreException e){
			// expected
		} catch (IllegalArgumentException e){
			// expected as the version of the WebDav HTTP Connection returns so
		}
	}
	
	public void testLocalSite() throws Exception {
		
		ILocalSite site = SiteManager.getLocalSite();
		IConfiguredSite[] instSites = site.getCurrentConfiguration().getConfiguredSites();
		assertTrue(instSites.length>0);
		System.out.println("Local Site:"+instSites[0].getSite().getURL().toExternalForm());
		
		ISite remoteSite = SiteManager.getSite(SOURCE_FILE_SITE_INSTALLED);
		IFeature remoteFeature = remoteSite.getFeatureReferences()[0].getFeature();
		remove(remoteFeature,instSites[0].getSite());		
		instSites[0].getSite().install(remoteFeature,null,null);
		
		IFeatureReference[] features = site.getCurrentConfiguration().getConfiguredSites()[0].getSite().getFeatureReferences();
		assertTrue(features.length>0);

		//cleanup
		assertNotNull(remoteFeature);		
		File file = new File(instSites[0].getSite().getURL().getFile()+File.separator+Site.DEFAULT_INSTALLED_FEATURE_PATH+remoteFeature.getVersionedIdentifier());
		UpdateManagerUtils.removeFromFileSystem(file);
		file = new File(instSites[0].getSite().getURL().getFile()+File.separator+Site.DEFAULT_PLUGIN_PATH+"org.eclipse.update.plugin1_1.1.1");
		UpdateManagerUtils.removeFromFileSystem(file);		
		File localFile = new File(new URL(((LocalSite)SiteManager.getLocalSite()).getLocationURL(),LocalSite.SITE_LOCAL_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);	
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)site.getCurrentConfiguration()).getURL().getFile()));				
		

	}
	

}

