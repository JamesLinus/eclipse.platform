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
package org.eclipse.update.tests.configurations;
import java.io.File;
import java.net.URL;

import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.configuration.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestBackward extends UpdateManagerTestCase {
	
	/**
	 * Test the getFeatures()
	 */
	public TestBackward(String arg0) {
		super(arg0);
	}
	
	public void testSimpleBackward() throws Exception {

		// cleanup
		LocalSite siteLocal = ((LocalSite)SiteManager.getLocalSite());
		File localFile = new File(new URL(siteLocal.getLocationURL(),LocalSite.SITE_LOCAL_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		UpdateManagerUtils.removeFromFileSystem(new File(((InstallConfiguration)siteLocal.getCurrentConfiguration()).getURL().getFile()));
		InternalSiteManager.localSite=null;		

		ILocalSite site = SiteManager.getLocalSite();
		ISite remoteSite = SiteManager.getSite(SOURCE_HTTP_SITE);
		IFeatureReference featureRef = remoteSite.getFeatureReferences()[0];
		
		IInstallConfiguration oldInstallConfig = site.getCurrentConfiguration();
		IConfiguredSite oldConfigSite = oldInstallConfig.getConfiguredSites()[0];
		
		((ConfiguredSite)oldConfigSite).setUpdatable(true);	
		assertNotNull("Reference is null",featureRef);
		remove(featureRef.getFeature(),oldConfigSite);	
		oldConfigSite.install(featureRef.getFeature(),null,null);
		site.save();
	
		
		// Activity -> InstallConfig
		IInstallConfiguration current = site.getCurrentConfiguration();
		IActivity activity = current.getActivities()[0];	
		assertTrue(activity.getInstallConfiguration().equals(current));
		
		// ConfigSite->InstallConfig
		IConfiguredSite newConfigSite = current.getConfiguredSites()[0];
		assertTrue(newConfigSite.getInstallConfiguration().equals(current));
		
		// cleanup
		localFile = new File(new URL(((LocalSite)SiteManager.getLocalSite()).getLocationURL(),LocalSite.SITE_LOCAL_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);		
		localFile = new File(new URL(((LocalSite)SiteManager.getLocalSite()).getLocationURL(),LocalSite.DEFAULT_CONFIG_FILE).getFile());
		UpdateManagerUtils.removeFromFileSystem(localFile);	
	
	}

}

