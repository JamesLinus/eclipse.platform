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
package org.eclipse.update.tests.uivalues;

import java.io.File;
import java.net.URL;

import org.eclipse.update.core.*;
import org.eclipse.update.tests.UpdateManagerTestCase;


public class TestUILabel extends UpdateManagerTestCase {

	/**
	 * Test the getFeatures()
	 */
	public TestUILabel(String arg0) {
		super(arg0);
	}
	
	
	/**
	 * Method testHTTPSite.
	 * @throws Exception
	 */
	public void testUpdateManagerSite() throws Exception{ 
		
		ISite remoteSite = SiteManager.getSite(new URL("http",getHttpHost(),getHttpPort(),bundle.getString("HTTP_PATH_2")));
		ICategory[] categories = remoteSite.getCategories();
		for (int i =0; i<categories.length; i++){
			System.out.println("Category ->"+categories[i].getLabel()+":"+categories[i].getName());
		}
		System.out.println(remoteSite.getDescription().getURL().toExternalForm());
		IFeatureReference[] remoteFeatures = remoteSite.getFeatureReferences();
		if (remoteFeatures==null || remoteFeatures.length==0) fail("No feature available for testing");		
		for (int i=0;i<remoteFeatures.length;i++){
			IFeature feature = remoteFeatures[i].getFeature();
			System.out.println("feature:"+feature.getVersionedIdentifier()+"->"+feature.getName());
			print(feature.getLicense(),"License");
			print(feature.getCopyright(),"Copyright");			
			print(feature.getDescription(),"Description");				
			
			// check that it downloads the feature.jar under the cover
			// and unpack it
			
			URL url = feature.getLicense().getURL();
			if (url!=null){
				assertTrue((new File(url.getFile())).exists());
			}

			url = feature.getCopyright().getURL();
			if (url!=null){
				assertTrue((new File(url.getFile())).exists());
			}
			
			url = feature.getDescription().getURL();
			if (url!=null){
				assertTrue((new File(url.getFile())).exists());
			}
			
		}
	}
	
	
	/**
	 * Method print.
	 * @param info
	 * @param text
	 */
	private void print(IURLEntry info, String text){
		System.out.print("->"+text+":");
		if (info.getURL()!=null) 
			System.out.println("<"+info.getURL().toExternalForm()+">");
		else 
			System.out.println(info.getAnnotation());
	}
} 


