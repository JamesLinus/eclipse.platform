package org.eclipse.update.tests.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;

import org.eclipse.update.core.model.ArchiveReferenceModel;
import org.eclipse.update.core.model.DefaultSiteParser;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.SiteCategoryModel;
import org.eclipse.update.core.model.SiteMapModel;
import org.eclipse.update.core.model.SiteModelFactory;
import org.eclipse.update.core.model.URLEntryModel;

public class SiteMain {

	public static void main(String[] args) {
		
		PrintWriter w = new PrintWriter(System.out);
		process("site_old_format.xml",w);
		process("site.xml",w);
		process("site_with_type.xml",w);
		w.close();
	}
	
	private static void process(String xml, PrintWriter w) {
		
		SiteModelFactory factory = new SiteModelFactory();
		DefaultSiteParser.DEBUG = false;
		InputStream is = null;
		SiteMapModel site = null;
		
		w.println("");
		w.println("Parsing site map ...");
		try {
			is = SiteMain.class.getResourceAsStream(xml);		
			site = factory.parseSite(is);
		} catch(Exception e) {
			w.println(e);
		} finally {
			if (is != null) {
				try { is.close();} catch(IOException e) {}
			}
		}
		
		if (site == null) return;
		
		String base = "http://another.server/site.xml";
		w.println("Resolving site using "+base+" ...");
		try {
			site.resolve(new URL(base), null);
		} catch(Exception e) {
			w.println(e);
		}
		
		w.println("Writing site ...");
		writeSite(w,0,site);
	}
	
	private static void writeSite(PrintWriter w, int level, SiteMapModel site) {
		if (site == null) return;
		
		String in = getIndent(level);
		w.println(in+"<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		
		w.println(in+"<site");
		w.println(in+"   "+"type=\""+site.getType()+"\"");
		w.println(in+"   "+">");
		
		writeDescription(w, level+1, site.getDescriptionModel());
		writeFeatures(w, level+1, site);
		writeArchives(w, level+1, site);
		writeCategoryDefs(w, level+1, site);
         
        w.println(in+"</feature>");
	}
	
	private static void writeDescription(PrintWriter w, int level, URLEntryModel ue) {
		if (ue == null) return;
		String in = getIndent(level);
		w.println("");
		w.println(in+"<description url=\""+ue.getURLString()+"\" -> "+ue.getURL()+">");
		w.println(in+ue.getAnnotation());
		w.println(in+"</description>");
	}
	
	private static void writeFeatures(PrintWriter w, int level, SiteMapModel site) {
		String in = getIndent(level);
		String in2 = getIndent(level+1);
		w.println("");
		
		FeatureReferenceModel[] features = site.getFeatureReferenceModels();
		for (int i=0; i<features.length; i++) {
			w.println(in+"<feature");
			w.println(in+"   "+"type=\""+features[i].getType()+"\"");
			w.println(in+"   "+"url=\""+features[i].getURLString()+"\" -> "+features[i].getURL());
			w.println(in+"   "+">");
			writeCategories(w, level+1, features[i]);
			w.println(in+"</feature>");
		}
	}
	
	private static void writeArchives(PrintWriter w, int level, SiteMapModel site) {
		String in = getIndent(level);
		w.println("");
		
		ArchiveReferenceModel[] archive = site.getArchiveReferenceModels();
		for (int i=0; i<archive.length; i++) {
			w.println(in+"<archive");
			w.println(in+"   "+"path=\""+archive[i].getPath()+"\"");
			w.println(in+"   "+"url=\""+archive[i].getURLString()+"\" -> "+archive[i].getURL());
			w.println(in+"   "+"/>");
		}
	}
	
	private static void writeCategoryDefs(PrintWriter w, int level, SiteMapModel site) {
		String in = getIndent(level);
		String in2 = getIndent(level+1);
		w.println("");
		
		SiteCategoryModel[] cat = site.getCategoryModels();
		for (int i=0; i<cat.length; i++) {
			w.println(in+"<category-def");
			w.println(in+"   "+"name=\""+cat[i].getName()+"\"");
			w.println(in+"   "+"label=\""+cat[i].getLabel()+"\"");
			w.println(in+"   "+">");
			writeDescription(w, level+1, cat[i].getDescriptionModel());
			w.println(in+"</category-def>");
		}
	}
	
	private static void writeCategories(PrintWriter w, int level, FeatureReferenceModel feature) {
		String in = getIndent(level);
		String[] cat = feature.getCategoryNames();
		for (int i=0; i<cat.length; i++) {
			w.println(in+"<category name=\""+cat[i]+"\"");
		}
	}
	
	private static String getIndent(int level) {
		String indent = "";
		for (int i=0; i<level; i++) 
			indent += "   ";
		return indent;
	}
}

