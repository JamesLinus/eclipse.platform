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
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * parse the default site.xml
 */

public class InstallChangeParser extends DefaultHandler {
	private final static SAXParserFactory parserFactory =
		SAXParserFactory.newInstance();
	private SAXParser parser;
	private SessionDelta change;

	public static final String CHANGE = "change"; //$NON-NLS-1$
	public static final String NEW_FEATURE = "newFeatures"; //$NON-NLS-1$
	public static final String REFERENCE = "reference"; //$NON-NLS-1$

	/**
	 * Constructor for InstallChangeParser
	 */
	public InstallChangeParser(File file)
		throws IOException, SAXException, CoreException {
		super();
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			UpdateCore.log(e);
		} catch (SAXException e) {
			UpdateCore.log(e);
		}

		InputStream changeStream = new FileInputStream(file);

		// DEBUG:		
		if (UpdateCore.DEBUG
			&& UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug(
				"Start parsing Install Change:");
			//$NON-NLS-1$
		}

		InputSource source = new InputSource(changeStream);
		parser.parse(source,this);
		
		// 16366
		if (change!=null) change.setFile(file);
	}

	/*
	 * returns the parsed InstallChanged
	 */
	public ISessionDelta getInstallChange() {
		return change;
	}

	/*
	 * @see DefaultHandler#startElement(String, String, String, Attributes)
	 */
	public void startElement(
		String uri,
		String localName,
		String qName,
		Attributes attributes)
		throws SAXException {

		// DEBUG:		
		if (UpdateCore.DEBUG
			&& UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug(
				"Start Element: uri:"
					+ uri
					+ " local Name:"
					+ localName
					+ " qName:"
					+ qName);
			//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		try {

			String tag = localName.trim();

			if (tag.equalsIgnoreCase(CHANGE)) {
				processChange(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(NEW_FEATURE)) {
				processNewFeature(attributes);
				return;
			}

			if (tag.equalsIgnoreCase(REFERENCE)) {
				processFeatureReference(attributes);
				return;
			}
		} catch (MalformedURLException e) {
			throw new SAXException(
				Policy.bind("Parser.UnableToCreateURL", e.getMessage()),
				e);
			//$NON-NLS-1$
		} catch (CoreException e) {
			throw new SAXException(
				Policy.bind("Parser.InternalError", e.toString()),
				e);
			//$NON-NLS-1$
		}
	}

	/* 
	 * 
	 */
	private void processFeatureReference(Attributes attributes)
		throws MalformedURLException, CoreException {

		//site url
		String siteUrlPath = attributes.getValue("siteURL"); //$NON-NLS-1$
		URL siteURL = new URL(siteUrlPath);
		try {
			siteURL = Platform.resolve(siteURL); 
		} catch (IOException e) {
			throw Utilities.newCoreException(
				Policy.bind("Parser.UnableToResolveURL", new Object[]{siteURL}),
				e);
			//$NON-NLS-1$
		}
		ISite currentSite = SiteManager.getSite(siteURL,null);

		// feature url
		String featureUrlPath = attributes.getValue("featureURL"); //$NON-NLS-1$
		URL featureURL = new URL(siteURL, featureUrlPath);

		if (featureURL != null) {
			SiteFeatureReference ref = new SiteFeatureReference();
			ref.setSite(currentSite);
			ref.setURL(featureURL);
			change.addReference(ref);

			// DEBUG:		
			if (UpdateCore.DEBUG
				&& UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug(
					"End Processing Feature Reference: url:"
						+ featureURL.toExternalForm());
				//$NON-NLS-1$
			}
		} else {
			String msg=
					Policy.bind(
						"InstallConfigurationParser.FeatureReferenceNoURL");
			//$NON-NLS-1$
			UpdateCore.log(msg,new Exception());
		}
	}

	/* 
	 * 
	 */
	private void processNewFeature(Attributes attributes) {

		// DEBUG:		
		if (UpdateCore.DEBUG
			&& UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug(
				"End Processing New Features:");
			//$NON-NLS-1$
		}
	}

	/* 
	 * 
	 */
	private void processChange(Attributes attributes) {

		change = new SessionDelta();
			
		// date
		long date = Long.parseLong(attributes.getValue("date")); //$NON-NLS-1$
		change.setCreationDate(new Date(date));

		// DEBUG:		
		if (UpdateCore.DEBUG
			&& UpdateCore.DEBUG_SHOW_PARSING) {
			UpdateCore.debug(
				"End Processing Change: date:" + date);
			//$NON-NLS-1$
		}

	}

}
