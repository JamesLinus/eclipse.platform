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
package org.eclipse.update.tests.regularInstall;

import java.io.File;

import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.tests.UpdateManagerTestCase;


public class TestExecutableInstall extends UpdateManagerTestCase {

	/**
	 * Constructor for Test1
	 */
	public TestExecutableInstall(String arg0) {
		super(arg0);
	}
	


	public void testFileSite() throws Exception{
		
		//cleanup target 
		File target = new File(TARGET_FILE_SITE.getFile());
		UpdateManagerUtils.removeFromFileSystem(target);		
		InstallRegistry.cleanup();
		
		IUpdateSite remoteSite = SiteManager.getUpdateSite(SOURCE_FILE_SITE_INSTALLED,null);
		IFeatureReference[] remoteFeatureReferences = remoteSite.getFeatureReferences();
		IFeature remoteFeature = remoteSite.getFeature(remoteFeatureReferences[0],null);
		IInstalledSite localSite = SiteManager.getInstalledSite(TARGET_FILE_SITE,null);
		assertNotNull(remoteFeature);
		remove(remoteFeature,localSite);		
		localSite.install(remoteFeature,null,null,null,null);
		
		// verify
		String site = localSite.getURL().getFile();
		IPluginEntry[] entries = remoteFeature.getPluginEntries(false);
		assertTrue("no plugins entry",(entries!=null && entries.length!=0));
		String pluginName= entries[0].getVersionedIdentifier().toString();
		File pluginFile = new File(site,Site.DEFAULT_PLUGIN_PATH+pluginName);
		assertTrue("plugin files not installed locally",pluginFile.exists());

		File featureFile = new File(site,Site.DEFAULT_INSTALLED_FEATURE_PATH+remoteFeature.getVersionedIdentifier().toString());
		assertTrue("feature info not installed locally:"+featureFile,featureFile.exists());
		assertTrue("feature is a file, not a directory:"+featureFile,featureFile.isDirectory());

		
		File featureFileXML = new File(site,Site.DEFAULT_INSTALLED_FEATURE_PATH+remoteFeature.getVersionedIdentifier().toString()+File.separator+"feature.xml");
		assertTrue("feature info not installed locally: no feature.xml",featureFileXML.exists());

		//cleanup target 
		UpdateManagerUtils.removeFromFileSystem(target);
		
	}

}


