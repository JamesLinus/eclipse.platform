package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InvalidSiteTypeException;
import org.eclipse.update.internal.core.obsolete.*;

public class SiteURL extends DefaultSite {
	
	public static final String SITE_TYPE = "org.eclipse.update.core.http";

	/**
	 * plugin entries
	 */
	private List pluginEntries = new ArrayList(0);
	/**
	 * Constructor for HTTPSite
	 */
	public SiteURL() throws CoreException, InvalidSiteTypeException {
		super();
	}

	/**
	 * @see AbstractSite#getURL(IFeature, String)
	 * In this default implementation we can deduct the URL of the 
	 * archive based on teh name of teh ID
	 * In other implementations, we may have to use the site.xml archive tag, that maps
	 * an id and a URL
	 */
	public URL getURL(String archiveId) throws CoreException {
		URL contentURL = null;
		try {
			contentURL = getArchiveURLfor(archiveId);
			
			// if there is no mapping in the site.xml
			// for this archiveId, use the default one
			if (contentURL==null) {
				String protocol = getURL().getProtocol();
				String host = getURL().getHost();
				String path = UpdateManagerUtils.getPath(getURL());			
				contentURL = new URL(protocol,host,path+archiveId);
			}
		} catch (MalformedURLException e){
			String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
			IStatus status = new Status(IStatus.ERROR,id,IStatus.OK,"Error creating URL",e);
			throw new CoreException(status);	
		}		
		return contentURL;
	}

	/**
	 * @see IPluginContainer#getPluginEntries()
	 */
	public IPluginEntry[] getPluginEntries() {
		IPluginEntry[] result = new IPluginEntry[0];
		if (!(pluginEntries==null || pluginEntries.isEmpty())){
			result = new IPluginEntry[pluginEntries.size()];
			pluginEntries.toArray(result);
		}
		return result;
	}

	/**
	 * @see IPluginContainer#getPluginEntryCount()
	 */
	public int getPluginEntryCount() {
		return getPluginEntries().length;
	}
	/**
	 * @see IPluginContainer#getDownloadSize(IPluginEntry)
	 */
	public long getDownloadSize(IPluginEntry entry) {
		Assert.isTrue(entry instanceof PluginEntry);
		return ((PluginEntry)entry).getDownloadSize();
	}


	/**
	 * @see IPluginContainer#getInstallSize(IPluginEntry)
	 */
	public long getInstallSize(IPluginEntry entry) {
		Assert.isTrue(entry instanceof PluginEntry);
		return ((PluginEntry)entry).getInstallSize();
	}
	
	/**
	 * @see IPluginContainer#addPluginEntry(IPluginEntry)
	 */
	public void addPluginEntry(IPluginEntry pluginEntry) {
		pluginEntries.add(pluginEntry);
	}

	/**
	 * @see IPluginContainer#store(IPluginEntry, String, InputStream)
	 */
	public void store(IPluginEntry pluginEntry,String contentKey,InputStream inStream) throws CoreException{
		//FIXME: should not be called should it ? Can I store in any URL Site ?
	}
	
	/**
	 * store DefaultFeature files
	 */
	public void storeFeatureInfo(VersionedIdentifier featureIdentifier,String contentKey,InputStream inStream) throws CoreException {
		//FIXME: should not be called should it ?
	}

	/**
	 * Method parseSite.
	 */
	protected void parseSite() throws CoreException {
		//does nothing
	}

	/*
	 * @see ISite#getType()
	 */
	public String getType() {
		return SITE_TYPE;
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	/*
	 * @see Site#removeFeatureInfo(VersionedIdentifier)
	 */
	protected void removeFeatureInfo(VersionedIdentifier featureIdentifier) throws CoreException {
	}

	/*
	 * @see IPluginContainer#remove(IPluginEntry)
	 */
	public void remove(IPluginEntry entry) throws CoreException {
		//FIXME: should not be called should it ? Can I remove from any URL Site ?		
	}

	/*
	 * @see ISite#getDefaultExecutableFeatureType()
	 */
	public String getDefaultExecutableFeatureType() {
		return null;
	}

	/*
	 * @see ISite#getDefaultInstallableFeatureType()
	 */
	public String getDefaultInstallableFeatureType() {
		String pluginID = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier()+".";		
		return pluginID+IFeatureFactory.INSTALLABLE_FEATURE_TYPE;
	}

	/*
	 * @see ISite#store(IFeature, String, InputStream, IProgressMonitor)
	 */
	public void store(IFeature feature, String name, InputStream inStream, IProgressMonitor monitor) throws CoreException {
	}

	/*
	 * @see IPluginContainer#store(IPluginEntry, String, InputStream, IProgressMonitor)
	 */
	public void store(IPluginEntry entry, String name, InputStream inStream, IProgressMonitor monitor) throws CoreException {
	}

	/*
	 * @see IPluginContainer#remove(IPluginEntry, IProgressMonitor)
	 */
	public void remove(IPluginEntry entry, IProgressMonitor monitor) throws CoreException {
	}

}

