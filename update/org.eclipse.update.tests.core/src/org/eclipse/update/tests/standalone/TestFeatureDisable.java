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

package org.eclipse.update.tests.standalone;

import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.boot.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.standalone.*;

public class TestFeatureDisable extends StandaloneManagerTestCase {
	public static boolean isDisabled;
	
	public TestFeatureDisable(String arg0) {
		super(arg0);
		isDisabled = false;
	}

	public void umSetUp() {
		super.umSetUp();
		
		String featureId = "my.alphabet";
		String version = "1.0.0";

		checkConfiguredSites();
		if (!isDisabled) {
			System.out.println(
				"==============" + this.getClass() + "=============");
			StandaloneUpdateApplication app = new StandaloneUpdateApplication();
			try {
				exitValue = (Integer)app.run(getCommand(
					"disable",
					featureId,
					version,
					null,
					null,
					TARGET_FILE_SITE.getFile()));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			isDisabled = true;
			checkConfiguredSites();
		}
	}
	
	// ensure exit without problems
	public void testExitValue() throws Exception {
		System.out.println("exitValue: " + exitValue);
		assertEquals(exitValue, IPlatformRunnable.EXIT_OK);
	}

	public void testFeatureStatus() throws Exception {
		URL configURL = BootLoader.getCurrentPlatformConfiguration().getConfigurationLocation();
		String config = new File(configURL.getFile()).getParent();
		ISite localSite = SiteManager.getSite(new URL("file", null, config), null);

		File localFile =
			new File(
				new URL(
					localSite.getURL(),
					"platform.cfg.metadata/" + SiteLocal.SITE_LOCAL_FILE)
					.getFile());
		System.out.println(localFile.getAbsolutePath());
		assertTrue(localFile.exists());
		File configFile = getLatestConfigurationFile(localFile);
	
		String[] names =
			{
				"features/my.alphabet.round.letters_1.0.0/",
				"features/my.alphabet.straight.letters_1.0.0/",
				"features/my.alphabet_1.0.0/" };
		assertTrue(configFile.exists());
		assertTrue(checkFeaturesDisabled(getArrayList(names),configFile, TARGET_FILE_SITE.getFile()));
	}

	public boolean checkFeaturesDisabled(ArrayList featureNames, File configFile, String siteLocation){
		try {
			FileReader freader = new FileReader(configFile);
			BufferedReader breader = new BufferedReader(freader);
			String line;
			System.out.println("now reading..." + configFile.getAbsolutePath());
			boolean isSiteToCheck = false;
			while (breader.ready()) {
				line = breader.readLine();
				if (line.trim().startsWith("<site url")){
					isSiteToCheck = line.trim().split("\"")[1].replaceFirst("file:","").equals(siteLocation);
				} else if (isSiteToCheck && line.trim().startsWith("<feature configured")){
					System.err.println(line);
					String[] configLine = line.split("\"");
					if (featureNames.contains(configLine[3]) && configLine[1].equals("true")){
						fail(configLine[3] + " has not been disabled.");
					}
				}
			}
		} catch (Exception e) {
			System.err.println(e);
		} 
		return true;
	}

}
