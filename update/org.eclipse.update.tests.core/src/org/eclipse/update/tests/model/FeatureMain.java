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
package org.eclipse.update.tests.model;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

import org.eclipse.update.core.model.*;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class FeatureMain extends UpdateManagerTestCase {

	public FeatureMain(String testcase){
		super(testcase);
	}

	public void testMain() throws Exception {
		
		FeatureModelFactory factory = new FeatureModelFactory();
		InputStream is = null;
		Feature feature = null;
		PrintWriter w = new PrintWriter(System.out);
		
		w.println("Parsing feature ...");
		try {
			is = FeatureMain.class.getResourceAsStream("feature.xml");		
			feature = factory.parseFeature(is);
		} finally {
			if (is != null) {
				try { is.close();} catch(IOException e) {}
			}
		}
		
		String base = "http://another.server/feature.xml";
		w.println("Resolving feature using "+base+" ...");
		ResourceBundle bundle = null;
		bundle = ResourceBundle.getBundle("org/eclipse/update/tests/model/test");
		feature.resolve(new URL(base), new URL(base));
				
		w.close();
	}
	
	private static void writeFeature(PrintWriter w, int level, Feature feature) {
		if (feature == null) return;
		
		String in = getIndent(level);
		w.println(in+"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		
		w.println(in+"<feature");
		w.println(in+"   "+"id=\""+feature.getFeatureIdentifier()+"\"");
		w.println(in+"   "+"version=\""+feature.getFeatureVersion()+"\"");
		w.println(in+"   "+"label=\""+feature.getName()+"\"");
		w.println(in+"   "+"provider-name=\""+feature.getProvider()+"\"");
		w.println(in+"   "+"image=\""+feature.getImageURLString()+"\" -> "+feature.getImageURL());
		w.println(in+"   "+"os=\""+feature.getOS()+"\"");
		w.println(in+"   "+"ws=\""+feature.getWS()+"\"");
		w.println(in+"   "+"nl=\""+feature.getNL()+"\"");
		w.println(in+"   "+"application=\""+feature.getApplication()+"\"");
		w.println(in+"   "+">");
		
		writeDescription(w, level+1, feature.getDescription());
		writeCopyright(w, level+1, feature.getCopyright());
		writeLicense(w, level+1, feature.getLicense());
		writeURLs(w, level+1, feature);
		writePrereqs(w, level+1, feature);
		writePluginEntries(w, level+1, feature);
		writeNonPluginEntries(w, level+1, feature);
		         
        w.println(in+"</feature>");
	}
	
	private static void writeDescription(PrintWriter w, int level, URLEntry ue) {
		String in = getIndent(level);
		w.println("");
		w.println(in+"<description url=\""+ue.getURLString()+"\" -> "+ue.getURL()+">");
		w.println(in+ue.getAnnotation());
		w.println(in+"</description>");
	}
	
	private static void writeCopyright(PrintWriter w, int level, URLEntry ue) {
		String in = getIndent(level);
		w.println("");
		w.println(in+"<copyright url=\""+ue.getURLString()+"\" -> "+ue.getURL()+">");
		w.println(in+ue.getAnnotation());
		w.println(in+"</copyright>");
	}
	
	private static void writeLicense(PrintWriter w, int level, URLEntry ue) {
		String in = getIndent(level);
		w.println("");
		w.println(in+"<license url=\""+ue.getURLString()+"\" -> "+ue.getURL()+">");
		w.println(in+ue.getAnnotation());
		w.println(in+"</license>");
	}
	
	private static void writeURLs(PrintWriter w, int level, Feature feature) {
		String in = getIndent(level);
		String in2 = getIndent(level+1);
		w.println("");
		w.println(in+"<url>");
		
		URLEntry update = feature.getUpdateSiteEntry();
		w.println(in2+"<update");
		w.println(in2+"   "+"url=\""+update.getURLString()+"\" -> "+update.getURL());
		w.println(in2+"   "+"label=\""+update.getAnnotation()+"\"");
		w.println(in2+"   "+"/>");
		
		URLEntry[] discovery = feature.getDiscoverySiteEntries();
		for (int i=0; i<discovery.length; i++) {
			w.println(in2+"<discovery");
			w.println(in2+"   "+"url=\""+discovery[i].getURLString()+"\" -> "+discovery[i].getURL());
			w.println(in2+"   "+"label=\""+discovery[i].getAnnotation()+"\"");
			w.println(in2+"   "+"/>");
		}
		
		w.println(in+"</url>");
	}
	
	private static void writePrereqs(PrintWriter w, int level, Feature feature) {
		String in = getIndent(level);
		String in2 = getIndent(level+1);
		w.println("");
		w.println(in+"<requires>");
		
		Import[] imp = feature.getImports();
		for (int i=0; i<imp.length; i++) {
			w.println(in2+"<import");
			w.println(in2+"   "+"plugin=\""+imp[i].getIdentifier()+"\"");
			w.println(in2+"   "+"version=\""+imp[i].getVersion()+"\"");
			w.println(in2+"   "+"match=\""+imp[i].getMatchingRuleName()+"\"");
			w.println(in2+"   "+"/>");
		}
		
		w.println(in+"</requires>");
	}
	
	private static void writePluginEntries(PrintWriter w, int level, Feature feature) {
		String in = getIndent(level);
		w.println("");
		
		PluginEntry[] plugin = feature.getPluginEntries();
		for (int i=0; i<plugin.length; i++) {
			w.println(in+"<plugin");
			w.println(in+"   "+"id=\""+plugin[i].getPluginIdentifier()+"\"");
			w.println(in+"   "+"version=\""+plugin[i].getPluginVersion()+"\"");
			w.println(in+"   "+"fragment=\""+plugin[i].isFragment()+"\"");
			w.println(in+"   "+"os=\""+plugin[i].getOS()+"\"");
			w.println(in+"   "+"ws=\""+plugin[i].getWS()+"\"");
			w.println(in+"   "+"nl=\""+plugin[i].getNL()+"\"");
			w.println(in+"   "+"download-size=\""+plugin[i].getDownloadSize()+"\"");
			w.println(in+"   "+"install-size=\""+plugin[i].getInstallSize()+"\"");
			w.println(in+"   "+"/>");
		}
	}
	
	private static void writeNonPluginEntries(PrintWriter w, int level, Feature feature) {
		String in = getIndent(level);
		w.println("");
		
		NonPluginEntry[] data = feature.getNonPluginEntries();
		for (int i=0; i<data.length; i++) {
			w.println(in+"<data");
			w.println(in+"   "+"id=\""+data[i].getIdentifier()+"\"");
			w.println(in+"   "+"download-size=\""+data[i].getDownloadSize()+"\"");
			w.println(in+"   "+"install-size=\""+data[i].getInstallSize()+"\"");
			w.println(in+"   "+"/>");
		}
	}
	
	
	private static String getIndent(int level) {
		String indent = "";
		for (int i=0; i<level; i++) 
			indent += "   ";
		return indent;
	}
}
