package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.update.core.*;

/**
 * Default implementation of an Executable DefaultFeature
 */

public class FeatureExecutable extends DefaultFeature {

	/**
	 * URL of the feature, used to create other URLs
	 */
	private URL rootURL;

	/**
	 * Constructor for DefaultExecutableFeature
	 */
	public FeatureExecutable(IFeature sourceFeature, ISite targetSite) throws CoreException {
		super(sourceFeature, targetSite);
	}

	/**
	 * Constructor for DefaultExecutableFeature
	 */
	public FeatureExecutable(URL url, ISite targetSite) throws CoreException {
		super(targetSite);
	}

	/**
	 * @see AbstractFeature#getInputStreamFor(IPluginEntry,String)
	 */
	public InputStream getInputStreamFor(IPluginEntry pluginEntry, String name) throws CoreException {
		
		InputStream result = null;

		try {
			File entry = new File(getPath(pluginEntry), name);
			result = new FileInputStream(entry);
			
		} catch (Exception e) {
			String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
			IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, "Error during retrieving the Stream of :" + name + " in plugin:" + pluginEntry.getIdentifier().toString(), e);
			throw new CoreException(status);
		}
		return result;
	}

	/**
	 * @see AbstractFeature#getStorageUnitNames(IPluginEntry)
	 */
	public String[] getStorageUnitNames(IPluginEntry pluginEntry) throws CoreException {
		
		String[] result = new String[0];
		
		try {
			// return the list of all subdirectories
			File pluginDir = new File(getPath(pluginEntry));			
			List files = getFileNames(pluginDir);
			result = new String[files.size()];
			files.toArray(result);
			
		} catch (Exception e) {
			String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
			IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, "Error retrieving archive names for:" + pluginEntry.getIdentifier().toString(), e);
			throw new CoreException(status);
		}
		return result;
	}

	/**
	 * return the path for a pluginEntry
	 */
	private String getPath(IPluginEntry pluginEntry) throws Exception {
		String result = null;
				
 		// get the URL of the Archive file that contains the plugin entry
		URL fileURL = getSite().getSiteContentProvider().getArchivesReferences(getArchiveID(pluginEntry));
		result = UpdateManagerUtils.getPath(fileURL);		

		// return the list of all subdirectories
		if (!result.endsWith(File.separator)) result += File.separator;		
		File pluginDir = new File(result);			
		if (!pluginDir.exists())
			throw new IOException("The File:" + result + "does not exist.");
			
 		return result;
	}

	/**
	 * return the archive ID for a plugin
	 * The id is based on the feature
	 * the default ID is plugins/pluginId_pluginVer or
	 * the default ID is fragments/pluginId_pluginVer or
		*/
	public String getArchiveID(IPluginEntry entry) {
		//FIXME: fragments
		String type = (entry.isFragment()) ? Site.DEFAULT_FRAGMENT_PATH : Site.DEFAULT_PLUGIN_PATH;
		return type + entry.getIdentifier().toString();
	}

	/**
	 * return the path for the DefaultFeature
	 */
	private String getFeaturePath() throws IOException {
		String result = UpdateManagerUtils.getPath(getURL());;		

		// return the list of all subdirectories
		if (!(result.endsWith(File.separator) || result.endsWith("/"))) result += File.separator;		
		File pluginDir = new File(result);			
		if (!pluginDir.exists())
			throw new IOException("The File:" + result + "does not exist.");
			
 		return result;
	}

	/**
	 * @see AbstractFeature#isExecutable()
	 */
	public boolean isExecutable() {
		return true;
	}

	/**
	 * @see IFeature#getRootURL()
	 * Make sure the rotURL ends with '/' as it shoudl be a directory
	 */
	public URL getRootURL() throws MalformedURLException {
		if (rootURL == null) {
			rootURL = getURL();
			if (!rootURL.getFile().endsWith("/")) {
				rootURL = new URL(rootURL.getProtocol(), rootURL.getHost(), rootURL.getFile() + "/");
			}
		}
		return rootURL;
	}
	/**
	 * @see AbstractFeature#getInputStreamFor(String)
	 */
	protected InputStream getInputStreamFor(IFeature feature,String name) throws IOException {
	
		InputStream result = null;
		try {
			File entry = new File(getFeaturePath(), name);
			result = new FileInputStream(entry);
		} catch (IOException e) {
			throw new IOException( "Error during retrieving the Stream of :" + name + " in feature:" + feature.getURL().toExternalForm()+"\r\n"+e.toString());
		}
		return result;
	}

	/**
	 * @see AbstractFeature#getStorageUnitNames()
	 */
	protected String[] getStorageUnitNames(IFeature feature)  throws CoreException {
		String[] result = new String[0];
		try {
			File featureDir = new File(getFeaturePath());
			List files = getFileNames(featureDir);
			result = new String[files.size()];
			files.toArray(result);
		} catch (Exception e){
			String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
			IStatus status = new Status(IStatus.ERROR, id, IStatus.OK, "Error retrieving archive names for:" + feature.getVersionIdentifier().toString(), e);
			throw new CoreException(status);
		}
		return result;
	}

	/**
	 * return the names of the files under the directory, relative to teh directory
	 * if teh Directory is /home/user and the file is /home/user/hello, then retrun hello
	 * This name is teh id that will be used to references the file again
	 */
	private List getFileNames(File dir) throws IOException{
		List result = new ArrayList();
		List files = getFiles(dir);
		if (!(files==null || files.isEmpty())) {
			int root = dir.getPath().length()+1;
			File currentFile = null;
			
			Iterator iter = files.iterator();
			while(iter.hasNext()){
				currentFile = (File)iter.next();
				result.add(currentFile.getPath().substring(root));
			}
		}
		return result;
	}

	/**
	 * return all teh files under the directory
	 */
	private List getFiles(File dir) throws IOException {
		List result = new ArrayList();
		
		if (!dir.isDirectory()) throw new IOException(dir.getPath()+" is not a valid directory");
		
		File[] files = dir.listFiles();
		if (files != null) // be careful since it can be null
			for (int i = 0; i < files.length; ++i){
				if (files[i].isDirectory()){
					result.addAll(getFiles(files[i]));
				} else {
					result.add(files[i]);
				}
			}
		return result;
	}

}