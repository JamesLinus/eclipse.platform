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
package org.eclipse.update.tests.implementation;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.URLEncoder;

public class SiteFTPFactory extends SiteFactory implements ISiteFactory {

	public static final String FILE = "a/b/c/";

	/*
	 * @see ISiteFactory#createSite(URL, boolean)
	 */
	public ISite createSite(URL url)
		throws CoreException{
		ISite site = null;
		InputStream siteStream = null;

		try {
			URL resolvedURL = URLEncoder.encode(url);
			siteStream = resolvedURL.openStream();

			SiteFactory factory = (SiteFactory) this;
			factory.parse(siteStream);

			site = new SiteFTP(new URL("http://eclipse.org/" + FILE));
			
		} catch (MalformedURLException e) {
			throw Utilities.newCoreException("Unable to create URL", e);
		} catch (IOException e) {
			throw Utilities.newCoreException("Unable to access URL",ISite.SITE_ACCESS_EXCEPTION, e);
		} finally {
			try {
				siteStream.close();
			} catch (Exception e) {
			}
		}
		return site;
	}

	/*
	 * @see SiteModelFactory#canParseSiteType(String)
	 */
	public boolean canParseSiteType(String type) {
		return "org.eclipse.update.tests.core.ftp".equalsIgnoreCase(type);
	}

}
