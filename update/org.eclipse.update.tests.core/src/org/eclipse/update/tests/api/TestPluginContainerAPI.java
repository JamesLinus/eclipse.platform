package org.eclipse.update.tests.api;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestPluginContainerAPI extends UpdateManagerTestCase {

	private Site site;
	private DefaultFeature feature;

	/**
	 * the Site to test
	 */
	private Site getSite() throws CoreException {
		if (site == null) {

			site = (Site)SiteManager.getSite(SOURCE_FILE_SITE);

		}
		return site;
	}

	/**
	 * the feature to test
	 */
	private DefaultFeature getFeature() throws MalformedURLException, CoreException {
		if (feature == null) {
			ISite site = getSite();
			URL id = UpdateManagerUtils.getURL(site.getURL(),"org.eclipse.update.core.feature1_1.0.0.jar",null);						
			feature = new FeaturePackaged(id, site);
		}
		return feature;
	}

	/**
	 * Test the getFeatures()
	 */
	public TestPluginContainerAPI(String arg0) throws CoreException {
		super(arg0);
	}

	public void testAbstractFeature() throws CoreException, MalformedURLException {
		PluginEntry pluginEntry = new PluginEntry("id", "ver");
		Feature _feature = getFeature();
		_feature.addPluginEntry(pluginEntry);
		assertEquals(_feature.getPluginEntryCount(), 1);
		assertEquals(_feature.getPluginEntries()[0], pluginEntry);

	}

	public void testAbstactSite() throws CoreException {
		PluginEntry pluginEntry = new PluginEntry("id", "ver");
		Site _site = getSite();
		_site.addPluginEntry(pluginEntry);
		assertEquals(_site.getPluginEntryCount(), 1);
		assertEquals(_site.getPluginEntries()[0], pluginEntry);

	}

}