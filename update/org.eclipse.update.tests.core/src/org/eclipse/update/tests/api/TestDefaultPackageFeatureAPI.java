package org.eclipse.update.tests.api;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.core.obsolete.DefaultFeature;
import org.eclipse.update.internal.core.obsolete.FeaturePackaged;
import org.eclipse.update.tests.UpdateManagerTestCase;



public class TestDefaultPackageFeatureAPI extends UpdateManagerTestCase {
	
	private	DefaultFeature remoteFeature=null;
	
	/**
	 * Test the testFeatures()
	 */
	public TestDefaultPackageFeatureAPI(String arg0) {
		super(arg0);
	}
	
	/**
	 * the feature to test
	 */
	private DefaultFeature getFeature() throws MalformedURLException, CoreException {
		if (remoteFeature == null){
		ISite site = SiteManager.getSite(SOURCE_FILE_SITE);
		URL id = UpdateManagerUtils.getURL(site.getURL(),"org.eclipse.update.core.feature1_1.0.0.jar",null);	
		remoteFeature = new FeaturePackaged(id,site);
		}
		return remoteFeature;
	}
	
	/**
	 * @see IFeature#testIdentifier()
	 */
	public void testIdentifier() throws CoreException, MalformedURLException {
		
		String id1 = "features/org.eclipse.test.feature_1.0.0.jar";
		String id2 = "features/org.eclipse_test_feature.jar";
		VersionedIdentifier ident1 = new VersionedIdentifier("org.test1.ident1","1.0.0");
		VersionedIdentifier ident2 = new VersionedIdentifier("org.test1.ident2","1.0.0");		
		

			ISite site = SiteManager.getSite(SOURCE_FILE_SITE);
			
			URL url1 = UpdateManagerUtils.getURL(site.getURL(),id1,null);			
			remoteFeature = new FeaturePackaged(url1,site);
			remoteFeature.setIdentifier(ident1);
			assertEquals(ident1.toString(),remoteFeature.getVersionIdentifier().toString());
		
			 URL url2 = UpdateManagerUtils.getURL(site.getURL(),id2,null);		
			remoteFeature = new FeaturePackaged(url2,site);
			remoteFeature.setIdentifier(ident2);			
			assertEquals(ident2.toString(),remoteFeature.getVersionIdentifier().toString());
	}


	/**
	 * @see IFeature#testSite()
	 */
	public void testSite() throws MalformedURLException, CoreException {
		String ident1 = "org.eclipse.test.feature_1.0.0.jar";		

			ISite site = SiteManager.getSite(SOURCE_FILE_SITE);
			
			URL id = UpdateManagerUtils.getURL(site.getURL(),ident1,null);		
			remoteFeature = new FeaturePackaged(id,site);
			assertEquals(site,remoteFeature.getSite());

	}


	/**
	 * @see IFeature#testLabel()
	 */
	public void testLabel() throws CoreException, MalformedURLException {
		String label = "dummy label";
		Feature feature = getFeature();
		feature.setLabel(label);
		assertEquals(label,feature.getLabel());
	}


	/**
	 * @see IFeature#testUpdateURL()
	 */
	public void testUpdateURL() throws CoreException, MalformedURLException {
		URL url = null;
		String label = "OTI Site";
		try {
			url = new URL("http://www.oti.com/");
			
		} catch (java.net.MalformedURLException e){} // impossible eh !
		DefaultFeature feature = getFeature();
		feature.setUpdateSiteEntry(new MyURLEntry(label, url));
		assertEquals(url,feature.getUpdateSiteEntry().getURL());
		assertEquals(label,feature.getUpdateSiteEntry().getAnnotation());

	} 


	
	/**
	 * @see IFeature#testDiscoveryURLs()
	 */
	public void testDiscoveryURLs() throws CoreException, MalformedURLException {
		IURLEntry[] urlInfo = new URLEntry[2];
		URL[] url = new URL[2];
		String[] label = new String[2];
		try {
			url[0] = new URL("http://www.oti.com/");
			url[1] = new URL("http://eclipse.org/");
			label[0] = "OTI Site";
			label[1] = "Eclipse Site";
		} catch (java.net.MalformedURLException e){} // impossible eh !
		DefaultFeature feature = getFeature();
		for (int i=0;i<2;i++){
				urlInfo[i] = new MyURLEntry(label[i],url[i]);
		}
		feature.setDiscoverySiteEntries(urlInfo);
		for (int i=0; i<feature.getDiscoverySiteEntries().length;i++){
			assertEquals(urlInfo[i],feature.getDiscoverySiteEntries()[i]);
		}


	}


	/**
	 * @see IFeature#testProvider()
	 */
	public void testProvider() throws CoreException, MalformedURLException {
		String provider = "not so dummy provider";
		Feature feature = getFeature();
		feature.setProvider(provider);
		assertEquals(provider,feature.getProvider());
	}


	/**
	 * @see IFeature#testDescription()
	 */
	public void testDescription() throws CoreException, MalformedURLException {
		String desc = "pretty long description as a string with \r\n and \t and \n";
		IURLEntry info = new MyURLEntry(desc,null);
		DefaultFeature feature = getFeature();
		feature.setDescription(info);
		assertEquals(desc,feature.getDescription().getAnnotation());
	}
	
	/**
	 * @see IFeature#testDescription()
	 */
	public void testDescription1() throws CoreException, MalformedURLException {
		URL url = null;
		try {
			url = new URL("http://www.oti.com");
		} catch (MalformedURLException e){/*pretty impossible*/}
		IURLEntry info = new MyURLEntry(null,url);
		DefaultFeature feature = getFeature();
		feature.setDescription(info);
		assertEquals(url,feature.getDescription().getURL());
	}	
	
	/**
	 * @see IFeature#testDescription()
	 */
	public void testDescription2() throws CoreException, MalformedURLException {
		URL url = null;
		try {
			url = new URL("http://www.oti.com");
		} catch (MalformedURLException e){/*pretty impossible*/}
		String desc = "pretty long description as a string with \r\n and \t and \n";
		IURLEntry info = new MyURLEntry(desc,url);
		DefaultFeature feature = getFeature();
		feature.setDescription(info);
		assertEquals(desc,feature.getDescription().getAnnotation());
		assertEquals(url,feature.getDescription().getURL());
	}



}

