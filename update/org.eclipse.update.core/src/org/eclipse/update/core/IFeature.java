package org.eclipse.update.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
 
 /**
  * Features define the packaging structure for a group 
  * of related plug-ins, plug-in fragments, and optionally 
  * non-plug-in files. 
  * 
  * <p>
  * Features are treated purely as an installation and packaging construct. 
  * They do not play a role during Eclipse plug-in execution. Features do not nest.
  * They are simply an inclusive "manifest" of the plug-ins, fragments 
  * and other files that make up that feature. If features are logically made 
  * up of plug-ins from "sub-features", the top-level feature "manifest"
  * must be fully resolved at packaging time.
  * </p>
  * 
  */
 
public interface IFeature extends IPluginContainer {

	/**
	 * Returns the Identifier of this Feature.
	 * The Identifier is not intended to be the key of the Feature.
	 * the URL is the key of the feature.
	 * @return the Identifier of this feature.
	 * @see VersionedIdentifier
	 * @since 2.0 
	 */

	VersionedIdentifier getIdentifier();
	
	/**
	 * Returns the Site this Featur belongs to.
	 * @return the site of this feature
	 * @since 2.0 
	 */

	ISite getSite();
	
	/**
	 * Returns the label of the feature.
	 * The label is declared in the <code>feature.xml</code> file.
	 * @return the label of the feature
	 * @since 2.0 
	 */

	String getLabel();
	
	/**
	 * Returns the URL that points at the Feature.
	 * This URL is the unique identifier of the feature
	 * within the site.
	 * 
	 * The URL is declared in the <code>feature.xml</code> file.	
	 * The URL can be relative to the <codesite.xml</code> or absolute.
	 * The Feature knows how to decipher the URL.
	 * 
	 * @return the URL identifying feature in the Site.
	 * @since 2.0 
	 */

	URL getURL();
	
	/**
	 * Returns the Update Information about the Feature.
	 * The Info is usually a URL of a Site in which user 
	 * can find new version of the feature.
	 * 
	 * The Update Information is composed of a URL and short label
	 * for this URL.
	 * 
	 * The URL is declared in the <code>feature.xml</code> file.
	 * 
	 * @see IInfo
	 * @return the IInfo that contains Update Information about this feature
	 * @since 2.0 
	 */

	IInfo getUpdateInfo() ;
	
	/**
	 * Return an array of info where the user can find other features
	 * related to this features.
	 * 
	 * Each Discovery Information is composed of a URL and short label
	 * for this URL.
	 * 
	 * The URLs are declared in the <code>feature.xml</code> file.
	 *  
	 * @see IInfo
	 * @return a Array of discovery info.Returns an empty array
	 * if there are no discovey info.
	 * @since 2.0 
	 */
	//VK: method name: getDiscoveryInfo (singular == plural)
	
	IInfo [] getDiscoveryInfos() ;
	
	/**
	 * Returns the provider of the feature
	 * @return the provider of the feature
	 * @since 2.0 
	 */

	String getProvider() ;
	
	/**
	 * Returns the description of the Feature.
	 * The description can be a short text and/or a
	 * URL pointing to a file.
	 * 
	 * The URL can be absolute or relative to the 
	 * <code> feature.xml. </code>
	 * 
	 * The description is declared in the <code>feature.xml</code> file.
	 * 
	 * @see IInfo
	 * @return the description of this feature
	 * @since 2.0 
	 */

	IInfo getDescription() ;
	
	/**
	 * Returns the copyright of the Feature.
	 * The copyright can be a short text and/or a
	 * URL pointing to a file.
	 * 
	 * The URL can be absolute or relative to the 
	 * <code> feature.xml. </code>
	 * 
	 * The copyright is declared in the <code>feature.xml</code> file.
	 * 
	 * @see IInfo
	 * @return the copyright of this feature
	 * @since 2.0 
	 */

	IInfo getCopyright() ;
	
	/**
	 * Returns the license of the Feature.
	 * The license can be a short text and/or a
	 * URL pointing to a file.
	 * 
	 * The URL can be absolute or relative to the 
	 * <code> feature.xml. </code>
	 * 
	 * The license is declared in the <code>feature.xml</code> file.
	 * 
	 * @see IInfo
	 * @return the license of this feature
	 * @since 2.0 
	 */

	IInfo getLicense() ;
	
	/**
	 * Optional operating system specification.
	 * A comma-separated list of os designators defined by Eclipse.
	 * Indicates this feature should only be installed on one of the specified
	 * os systems. If this attribute is not specified, the feature can be
	 * installed on all systems (portable implementation).
	 * 
	 * This information is used as a hint by the installation and update
	 * support (user can force installation of feature regardless of this setting).
	 *
	 * @see org.eclipse.core.boot.BootLoader 
	 * @return the operating system specification.
	 * @since 2.0 
	 */

	String getOS()  ;
	
	/**
	 * Optional windowing system specification. 
	 * A comma-separated list of ws designators defined by Eclipse.
	 *  Indicates this feature should only be installed on one of the specified
	 *  ws systems. If this attribute is not specified, the feature can be
	 *  installed on all systems (portable implementation).
	 * 
	 * This information is used as a hint by the installation and update
	 * support (user can force installation of feature regardless of this setting).
	 * 
	 * @see org.eclipse.core.boot.BootLoader 
	 * @return the windowing system specification.
	 * @since 2.0 
	 */

	String getWS()  ;
	
	/**
	 * Optional locale specification. 
	 * A comma-separated list of locale designators defined by Java.
	 * Indicates this feature should only be installed on a system running
	 * with a compatible locale (using Java locale-matching rules).
	 * If this attribute is not specified, the feature can be installed 
	 * on all systems (language-neutral implementation). 
	 * 
	 * This information is used as a hint by the installation and update
	 *  support (user can force installation of feature regardless of this setting).
	 * 
	 * @return the locale specification.
	 * @since 2.0 
	 */

	String getNL() ;
	
	/**
	 * optional image to use when displaying information about the feature.
	 * 
	 * The URL is either absolute or relative to the <code>feature.xml</code> file.
	 * 
	 * @return the URL pointing to the image
	 * @since 2.0 
	 */

	URL getImage() ;
	
	/**
	 * List of plugin the feature require
	 * to be installed in the site before it
	 * can be installed
	 * 
	 * @return the list of required plug-ins. Returns an empty array
	 * if there are no required.
	 * @since 2.0 
	 */

	IImport[] getImports();
	
	/**
	 * Returns <code> true </code> if the feature can
	 * be added to the plugin-path of the workspace.
	 * @since 2.0 
	 */
	// FIXME: javadoc	
	boolean isExecutable();
	
	/**
	 * Returns <code> true </code> if the feature can
	 * be installed.
	 * @since 2.0 
	 */
	// FIXME: javadoc
	boolean isInstallable();


	/**
	 * Returns an array of archives identifier that compose the feature.
	 * 
	 * @return 
	 * @deprecated seems nobody uses it
	 * @since 2.0 
	 */
	// VK: deprecated ???
	// FIXME: javadoc	
	String[] getArchives();
	
	/**
	 * Returns an array of archives identifier that compose the feature.
	 * 
	 * @return 
	 * @since 2.0 
	 */
	// VK: I am confused ... pluginEntry, dataEntry, archive
	// VK: getPluginEntries()
	// VK: getPluginEntryCount()
	// VK: addPluginEntry(...)
	// VK: remove(IPluginEntry)
	// VK: getDataEntries()
	// VK: addDataEntry(...)
	// VK: no count, no remove
	// VK: getArchives() .... no rest
	// FIXME: javadoc	
	IDataEntry[] getDataEntries();
	
	/**
	 * Adds a dataEntry to the list of managed dataEntry
	 * 
	 * @param entry the data entry
	 * @since 2.0 
	 */
	//VK: what is this used for. Why do we not have addArchive as well?
	
	void addDataEntry(IDataEntry dataEntry);
	
	/**
	 * returns the download size
	 * of the feature to be installed on the site.
	 * If the site is <code>null</code> returns the maximum size
	 * 
	 * If one plug-in entry has an unknown size.
	 * then the download size is unknown and equal to <code>-1</code>
	 * 
	 * @since 2.0 
	 */

	long getDownloadSize(ISite site) throws CoreException;
	
	/**
	 * returns the install size
	 * of the feature to be installed on the site.
	 * If the site is <code>null</code> returns the maximum size
	 * 
	 * If one plug-in entry has an unknown size.
	 * then the install size is unknown and equal to <code>-1</code>.
	 * @since 2.0 
	 */

	long getInstallSize(ISite site) throws CoreException;
	
	/**
	 * optional identifier of the Eclipse application that is to be used during
     * startup when the declaring feature is the primary feature.
     *  The application identifier must represent a valid application registered
     *  in the <code>org.eclipse.core.runtime.applications</code> extension point.
     *  Default is <code>org.eclipse.ui.workbench</code>.
     * @since 2.0 
	 */

	String getApplication();
		
}


