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
package org.eclipse.update.internal.configuration;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.internal.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * parse the default site.xml
 */

public class SiteLocalParser extends DefaultHandler {

	/**
	 * return the appropriate resource bundle for this sitelocal
	 */
	private ResourceBundle getResourceBundle() throws CoreException {
		ResourceBundle bundle = null;
		URL url = null;
		try {
			url = UpdateManagerUtils.asDirectoryURL(site.getLocationURL());
			ClassLoader l = new URLClassLoader(new URL[] { url }, null);
			bundle = ResourceBundle.getBundle(SiteLocalModel.SITE_LOCAL_FILE, Locale.getDefault(), l);
		} catch (MissingResourceException e) {
			UpdateCore.warn(e.getLocalizedMessage() + ":" + url.toExternalForm()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			UpdateCore.warn(e.getLocalizedMessage()); //$NON-NLS-1$
		}
		return bundle;
	}
	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();
	
	private SAXParser parser;
	private InputStream siteStream;
	private SiteLocalModel site;
	public static final String SITE = "localsite"; //$NON-NLS-1$
	public static final String CONFIG = "config"; //$NON-NLS-1$
	public static final String PRESERVED_CONFIGURATIONS = "preservedConfigurations"; //$NON-NLS-1$
	private ResourceBundle bundle;

	// trus if we are now parsing preserved config
	private boolean preserved = false;

	/**
	 * Constructor for DefaultSiteParser
	 */
	public SiteLocalParser(InputStream siteStream, ILocalSite site) throws IOException, SAXException, CoreException {
		super();
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			UpdateCore.log(e);
		} catch (SAXException e) {
			UpdateCore.log(e);
		}

		this.siteStream = siteStream;
		Assert.isTrue(site instanceof SiteLocalModel);
		this.site = (SiteLocalModel) site;

		// DEBUG:		
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
			UpdateCore.debug("Start parsing localsite:" + ((SiteLocalModel) site).getLocationURLString()); //$NON-NLS-1$
		}
		
		bundle = getResourceBundle();

		parser.parse(new InputSource(this.siteStream),this);
	}

	/**
	 * return the appropriate resource bundle for this sitelocal
	 */
	private URL getResourceBundleURL() throws CoreException {
		URL url = null;
		try {
			url = UpdateManagerUtils.asDirectoryURL(site.getLocationURL());
		} catch (MissingResourceException e) {
			UpdateCore.warn(e.getLocalizedMessage() + ":" + url.toExternalForm()); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			UpdateCore.warn(e.getLocalizedMessage()); //$NON-NLS-1$
		}
		return url;
	}

	/**
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		// DEBUG:		
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
			UpdateCore.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		try {

			String tag = localName.trim();

			if (tag.equalsIgnoreCase(SITE)) {
				processSite(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(CONFIG)) {
				processConfig(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(PRESERVED_CONFIGURATIONS)) {
				preserved = true;
				return;
			}

		} catch (MalformedURLException e) {
			throw new SAXException(Policy.bind("Parser.UnableToCreateURL", e.getMessage()), e); //$NON-NLS-1$
		} catch (CoreException e) {
			throw new SAXException(Policy.bind("Parser.InternalError", e.toString()), e); //$NON-NLS-1$
		}

	}

	/** 
	 * process the Site info
	 */
	private void processSite(Attributes attributes) throws MalformedURLException {
		//
		String info = attributes.getValue("label"); //$NON-NLS-1$
		info = UpdateManagerUtils.getResourceString(info, bundle);
		site.setLabel(info);
	
		// history
		String historyString = attributes.getValue("history"); //$NON-NLS-1$
		int history;
		if (historyString == null || historyString.equals("")) { //$NON-NLS-1$
			history = SiteLocalModel.DEFAULT_HISTORY;
		} else {
			history = Integer.parseInt(historyString);
		}
		site.setMaximumHistoryCount(history);
	
		//stamp
		String stampString = attributes.getValue("stamp"); //$NON-NLS-1$
		long stamp = Long.parseLong(stampString);
		site.setStamp(stamp);
	
		// DEBUG:		
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
			UpdateCore.debug("End process Site label:" + info); //$NON-NLS-1$
		}
	
	}

	/** 
	 * process the Config info
	 */
	private void processConfig(Attributes attributes) throws MalformedURLException, CoreException {
		// url
		URL url = UpdateManagerUtils.getURL(site.getLocationURL(), attributes.getValue("url"), null); //$NON-NLS-1$
		String label = attributes.getValue("label"); //$NON-NLS-1$
		label = UpdateManagerUtils.getResourceString(label, bundle);

		InstallConfigurationModel config = new BaseSiteLocalFactory().createInstallConfigurationModel();
		config.setLocationURLString(url.toExternalForm());
		config.setLabel(label);
		config.resolve(url, getResourceBundleURL());

		// add the config
		if (preserved) {
			site.addPreservedInstallConfigurationModel(config);
		} else {
			site.addConfigurationModel(config);
		}
		// DEBUG:		
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
			UpdateCore.debug("End Processing Config Tag: url:" + url.toExternalForm()); //$NON-NLS-1$
		}
	}

	/*
	 * @see ContentHandler#endElement(String, String, String)
	 */
	public void endElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// DEBUG:		
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
			UpdateCore.debug("End Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String tag = localName.trim();

		if (tag.equalsIgnoreCase(PRESERVED_CONFIGURATIONS)) {
			preserved = false;
			return;
		}

	}

}
