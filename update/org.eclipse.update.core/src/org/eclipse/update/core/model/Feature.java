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
package org.eclipse.update.core.model;

import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;

/**
 * Convenience implementation of a feature.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.IFeature
 * @see org.eclipse.update.core.model.FeatureModel
 * @since 2.0
 */
public class Feature extends FeatureModel implements IFeature {

	/**
	 * Simple file name of the default feature manifest file
	 * @since 2.0
	 */
	public static final String FEATURE_FILE = "feature"; //$NON-NLS-1$

	/**
	 * File extension of the default feature manifest file
	 * @since 2.0
	 */
	public static final String FEATURE_XML = FEATURE_FILE + ".xml"; //$NON-NLS-1$

	private ISite site; // feature site
	private IFeatureContentProvider featureContentProvider; // content provider
	private List /*of IFeatureReference*/
	includedFeatureReferences;

	//PERF: new instance variable
	private VersionedIdentifier versionId;

	private InstallAbortedException abortedException = null;

	/**
	 * Feature default constructor
	 * 
	 * @since 2.0
	 */
	public Feature() {
	}

	/**
	 * Compares two features for equality
	 * 
	 * @param object feature object to compare with
	 * @return <code>true</code> if the two features are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean equals(Object object) {
		if (!(object instanceof IFeature))
			return false;
		IFeature f = (IFeature) object;
		return getVersionedIdentifier().equals(f.getVersionedIdentifier());
	}

	/**
	 * Returns the feature identifier.
	 * 
	 * @see IFeature#getVersionedIdentifier()
	 * @since 2.0
	 */
	public VersionedIdentifier getVersionedIdentifier() {
		if (versionId != null)
			return versionId;

		String id = getFeatureIdentifier();
		String ver = getFeatureVersion();
		if (id != null && ver != null) {
			try {
				versionId = new VersionedIdentifier(id, ver);
				return versionId;
			} catch (Exception e) {
				UpdateCore.warn(
					"Unable to create versioned identifier:" + id + ":" + ver);
			}
		}

		versionId = new VersionedIdentifier(getURL().toExternalForm(), null);
		return versionId;
	}

	/**
	 * Returns the site this feature is associated with.
	 * 
	 * @see IFeature#getSite()
	 * @since 2.0
	 */
	public ISite getSite() {
		return site;
	}

	/**
	 * Returns the feature URL.
	 * 
	 * @see IFeature#getURL()
	 * @since 2.0
	 */
	public URL getURL() {
		IFeatureContentProvider contentProvider = null;
		try {
			contentProvider = getFeatureContentProvider();
		} catch (CoreException e) {
			UpdateCore.warn("No content Provider", e);
		}
		return (contentProvider != null) ? contentProvider.getURL() : null;
	}

	/**
	 * Returns an information entry referencing the location of the
	 * feature update site. 
	 * 
	 * @see IFeature#getUpdateSiteEntry()
	 * @since 2.0
	 */
	public IURLEntry getUpdateSiteEntry() {
		return (IURLEntry) getUpdateSiteEntry();
	}

	/**
	 * Return an array of information entries referencing locations of other
	 * update sites.
	 * 
	 * @see IFeature#getDiscoverySiteEntries()
	 * @since 2.0
	 */
	public IURLEntry[] getDiscoverySiteEntries() {
		URLEntry[] result = getDiscoverySiteEntries();
		if (result.length == 0)
			return new IURLEntry[0];
		else
			return (IURLEntry[]) result;
	}

	/**
	 * Returns and optional custom install handler entry.
	 * 
	 * @see IFeature#getInstallHandlerEntry()
	 * @since 2.0
	 */
	public IInstallHandlerEntry getInstallHandlerEntry() {
		return (IInstallHandlerEntry) getInstallHandlerModel();
	}

	/**
	 * Returns the feature description.
	 * 
	 * @see IFeature#getDescription()
	 * @since 2.0
	 */
	public IURLEntry getDescription() {
		return (IURLEntry) getDescription();
	}

	/**
	 * Returns the copyright information for the feature.
	 * 
	 * @see IFeature#getCopyright()
	 * @since 2.0
	 */
	public IURLEntry getCopyright() {
		return (IURLEntry) getCopyright();
	}

	/**
	 * Returns the license information for the feature.
	 * 
	 * @see IFeature#getLicense()
	 * @since 2.0
	 */
	public IURLEntry getLicense() {
		return (IURLEntry) getLicense();
	}

	/**
	 * Return optional image for the feature.
	 * 
	 * @see IFeature#getImage()
	 * @since 2.0
	 */
	public URL getImageURL() {
		return getImageURL();
	}

	/**
	 * Return a list of plug-in dependencies for this feature.
	 * 
	 * @see IFeature#getRawImports()
	 * @since 2.0
	 */
	public IImport[] getRawImports() {
		Import[] result = getImports();
		if (result.length == 0)
			return new IImport[0];
		else
			return (IImport[]) result;
	}

	/**
	 * Install the contents of this feature into the specified target feature.
	 * This method is a reference implementation of the feature installation
	 * protocol. Other concrete feature implementation that override this
	 * method need to implement this protocol.
	 * 
	 * @see IFeature#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @since 2.0
	 */
	public IFeatureReference install(
		IFeature targetFeature,
		IVerificationListener verificationListener,
		IProgressMonitor progress)
		throws InstallAbortedException, CoreException {
		// call other API with all optional features, or setup variable meaning install all
		return install(targetFeature, null, verificationListener, progress);
	}

	/**
	 * Install the contents of this feature into the specified target feature.
	 * This method is a reference implementation of the feature installation
	 * protocol. Other concrete feature implementation that override this
	 * method need to implement this protocol.
	 * 
	 * @see IFeature#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @since 2.0
	 */
	public IFeatureReference install(
		IFeature targetFeature,
		IFeatureReference[] optionalfeatures,
		final IVerificationListener verificationListener,
		IProgressMonitor progress)
		throws InstallAbortedException, CoreException {

		//DEBUG
		debug("Installing...:" + getURL().toExternalForm());
		ErrorRecoveryLog recoveryLog = ErrorRecoveryLog.getLog();

		// make sure we have an InstallMonitor		
		final InstallMonitor monitor;
		if (progress == null)
			monitor = new InstallMonitor(new NullProgressMonitor());
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// Setup optional install handler
		InstallHandlerProxy handler =
			new InstallHandlerProxy(
				IInstallHandler.HANDLER_ACTION_INSTALL,
				this,
				this.getInstallHandlerEntry(),
				monitor);
		boolean success = false;
		Throwable originalException = null;
		abortedException = null;

		// Get source feature provider and verifier.
		// Initialize target variables.
		final IFeatureContentProvider provider = getFeatureContentProvider();
		final IVerifier verifier = provider.getVerifier();
		IFeatureReference result = null;
		IFeatureReference alreadyInstalledFeature = null;
		IFeatureContentConsumer consumer = null;
		IPluginEntry[] targetSitePluginEntries = null;

		try {
			// determine list of plugins to install
			// find the intersection between the plugin entries already contained
			// on the target site, and plugin entries packaged in source feature
			IPluginEntry[] sourceFeaturePluginEntries = getPluginEntries();
			ISite targetSite = targetFeature.getSite();
			if (targetSite == null) {
				debug("The site to install in is null");
				targetSitePluginEntries = new IPluginEntry[0];
			} else {
				targetSitePluginEntries = targetSite.getPluginEntries();
			}
			IPluginEntry[] pluginsToInstall =
				UpdateManagerUtils.diff(
					sourceFeaturePluginEntries,
					targetSitePluginEntries);
			INonPluginEntry[] nonPluginsToInstall = getNonPluginEntries();

			IFeatureReference[] children = getIncludedFeatures();
			if (optionalfeatures != null) {
				children =
					UpdateManagerUtils.optionalChildrenToInstall(
						children,
						optionalfeatures);
			}

			// determine number of monitor tasks
			//   2 tasks for the feature jar (download/verify + install)
			// + 2*n tasks for plugin entries (download/verify + install for each)
			// + 1*m tasks per non-plugin data entry (download for each)
			// + 1 task for custom non-plugin entry handling (1 for all combined)
			// + 5*x tasks for children features (5 subtasks per install)
			int taskCount =
				2
					+ 2 * pluginsToInstall.length
					+ nonPluginsToInstall.length
					+ 1
					+ 5 * children.length;
			monitor.beginTask("", taskCount);
			SubProgressMonitor subMonitor = null;

			// start log
			recoveryLog.open(ErrorRecoveryLog.START_INSTALL_LOG);

			// Start the installation tasks			
			handler.installInitiated();

			// Download and verify feature archive(s)
			ContentReference[] references =
				provider.getFeatureEntryArchiveReferences(monitor);
			verifyReferences(
				verifier,
				references,
				monitor,
				verificationListener,
				true);
			monitorWork(monitor, 1);

			final MultiDownloadMonitor distributedMonitor =
				new MultiDownloadMonitor(monitor, targetFeature);

			final ThreadGroup tgroup =
				new ThreadGroup("Feature " + getURL() + " download");
			// Download and verify plugin archives
			for (int i = 0; i < pluginsToInstall.length; i++) {
				final IPluginEntry pluginToInstall = pluginsToInstall[i];
				Runnable r = new Runnable() {
					public void run() {
						try {
							final ContentReference[] plugin_references =
								provider.getPluginEntryArchiveReferences(
									pluginToInstall,
									distributedMonitor);
							verifyReferences(
								verifier,
								plugin_references,
								distributedMonitor,
								verificationListener,
								false);
							monitorWork(monitor, 1);
						} catch (InstallAbortedException e) {
							abortedException = e;
							MultiDownloadManager.stopThreads(tgroup);
						} catch (CoreException e) {
							UpdateUtils.logException(e);
						} finally {
							MultiDownloadManager.releaseThread(
								Thread.currentThread());
						}
					}
				};
				Thread downloadThread =
					MultiDownloadManager.getThread(
						r,
						"download " + pluginToInstall.toString(),
						tgroup);
				downloadThread.start();
			}

			MultiDownloadManager.waitForThreads(tgroup);

			handler.pluginsDownloaded(pluginsToInstall);

			// Download non-plugin archives. Verification handled by optional install handler
			for (int i = 0; i < nonPluginsToInstall.length; i++) {
				references =
					provider.getNonPluginEntryArchiveReferences(
						nonPluginsToInstall[i],
						monitor);
				monitorWork(monitor, 1);
			}
			handler.nonPluginDataDownloaded(
				nonPluginsToInstall,
				verificationListener);

			// All archives are downloaded and verified. Get ready to install
			consumer = targetFeature.getFeatureContentConsumer();

			// install the children feature
			// check if they are optional, and if they should be installed [2.0.1]
			for (int i = 0; i < children.length; i++) {
				IFeature childFeature = null;
				try {
					childFeature = children[i].getFeature(null);
				} catch (CoreException e) {
					UpdateCore.warn(null, e);
				}
				if (childFeature != null) {
					subMonitor = new SubProgressMonitor(monitor, 5);
					((Site) targetSite).install(// need to cast
					childFeature,
						optionalfeatures,
						consumer,
						verifier,
						verificationListener,
						subMonitor);
				}
			}

			// Install plugin files
			for (int i = 0; i < pluginsToInstall.length; i++) {
				// if another feature has already installed this plugin, skip it
				if (InstallRegistry.getInstance().isPluginJustInstalled(pluginsToInstall[i])) {
					monitor.worked(1);
					continue;
				}
				references =
					provider.getPluginEntryContentReferences(
						pluginsToInstall[i],
						monitor);
				IContentConsumer pluginConsumer =
					consumer.open(pluginsToInstall[i]);

				String msg = "";
				subMonitor = new SubProgressMonitor(monitor, 1);
				VersionedIdentifier pluginVerId =
					pluginsToInstall[i].getVersionedIdentifier();
				String pluginID =
					(pluginVerId == null) ? "" : pluginVerId.getIdentifier();
				msg = Policy.bind("Feature.TaskInstallPluginFiles", pluginID); //$NON-NLS-1$

				for (int j = 0; j < references.length; j++) {
					setMonitorTaskName(
						subMonitor,
						msg + references[j].getIdentifier());
					pluginConsumer.store(references[j], subMonitor);
				}

				InstallRegistry.registerPlugin(pluginsToInstall[i]);
				if (monitor.isCanceled())
					abort();
			}

			// check if we need to install feature files [16718]	
			// store will throw CoreException if another feature is already
			// installed in the same place
			alreadyInstalledFeature = featureAlreadyInstalled(targetSite);
			// 18867
			if (alreadyInstalledFeature == null) {
				//Install feature files
				references = provider.getFeatureEntryContentReferences(monitor);

				String msg = "";
				subMonitor = new SubProgressMonitor(monitor, 1);
				msg = Policy.bind("Feature.TaskInstallFeatureFiles"); //$NON-NLS-1$

				for (int i = 0; i < references.length; i++) {
					setMonitorTaskName(
						subMonitor,
						msg + " " + references[i].getIdentifier());
					consumer.store(references[i], subMonitor);
				}
				InstallRegistry.registerFeature(this);
			} else {
				monitor.worked(1);
			}

			if (monitor.isCanceled())
				abort();

			// call handler to complete installation (eg. handle non-plugin entries)
			handler.completeInstall(consumer);
			monitorWork(monitor, 1);

			// indicate install success
			success = true;

		} catch (InstallAbortedException e) {
			abortedException = e;
		} catch (CoreException e) {
			originalException = e;
		} finally {
			Exception newException = null;
			try {
				if (consumer != null) {
					if (success) {
						result = consumer.close();
						if (result == null) {
							result = alreadyInstalledFeature; // 18867
							if (result != null
								&& optionalfeatures != null
								&& optionalfeatures.length > 0) {
								// reinitialize as new optional children may have been installed
								reinitializeFeature(result);
							}
						}
						// close the log
						recoveryLog.close(ErrorRecoveryLog.END_INSTALL_LOG);
					} else {
						consumer.abort();
					}
				}
				handler.installCompleted(success);
				// if abort is done, no need for the log to stay
				recoveryLog.delete();
			} catch (CoreException e) {
				newException = e;
			}

			// original exception wins unless it is InstallAbortedException
			// and an error occured during abort
			if (originalException != null) {
				throw Utilities.newCoreException(
					Policy.bind("InstallHandler.error", this.getName()),
					originalException);
			}

			if (newException != null)
				throw Utilities.newCoreException(
					Policy.bind("InstallHandler.error", this.getName()),
					newException);

			if (abortedException != null) {
				throw abortedException;
			}

		}
		return result;
	}

	/**
	 * Returns an array of plug-in entries referenced by this feature
	 * 
	 * @see IFeature#getPluginEntries()
	 * @since 2.0
	 */
	public IPluginEntry[] getRawPluginEntries() {
		PluginEntry[] result = getPluginEntries();
		if (result.length == 0)
			return new IPluginEntry[0];
		else
			return (IPluginEntry[]) result;
	}

	/*
	 * Method filter.
	 * @param result
	 * @return IPluginEntry[]
	 */
	private IPluginEntry[] filterPluginEntry(IPluginEntry[] all) {
		List list = new ArrayList();
		if (all != null) {
			for (int i = 0; i < all.length; i++) {
				if (UpdateManagerUtils.isValidEnvironment(all[i]))
					list.add(all[i]);
			}
		}

		IPluginEntry[] result = new IPluginEntry[list.size()];
		if (!list.isEmpty()) {
			list.toArray(result);
		}

		return result;
	}

	/**
	 * Returns the count of referenced plug-in entries.
	 * 
	 * @see IFeature#getPluginEntryCount()
	 * @since 2.0
	 */
	public int getPluginEntryCount() {
		return getPluginEntries().length;
	}

	/**
	 * Returns an array of non-plug-in entries referenced by this feature
	 * 
	 * @see IFeature#getNonPluginEntries()
	 * @since 2.0
	 */
	public INonPluginEntry[] getRawNonPluginEntries() {
		NonPluginEntry[] result = getNonPluginEntries();
		if (result.length == 0)
			return new INonPluginEntry[0];
		else
			return (INonPluginEntry[]) result;
	}

	/**
	 * Returns the count of referenced non-plug-in entries.
	 * 
	 * @see IFeature#getNonPluginEntryCount()
	 * @since 2.0
	 */
	public int getNonPluginEntryCount() {
		return getNonPluginEntries().length;
	}

	/**
	 * Returns an array of feature references included by this feature
	 * 
	 * @return an erray of feature references, or an empty array.
	 * @since 2.0
	 */
	public IIncludedFeatureReference[] getRawIncludedFeatureReferences()
		throws CoreException {
		if (includedFeatureReferences == null)
			initializeIncludedReferences();

		if (includedFeatureReferences.size() == 0)
			return new IncludedFeatureReference[0];

		return (IIncludedFeatureReference[]) includedFeatureReferences.toArray(
			arrayTypeFor(includedFeatureReferences));
	}
	/**
	 * Returns the download size of the feature, if it can be determined.
	 * 
	 * @see IFeature#getDownloadSize()
	 * @since 2.0
	 */
	public long getDownloadSize() {
		try {
			Set allPluginEntries = new HashSet();
			Set allNonPluginEntries = new HashSet();

			IPluginEntry[] plugins = getPluginEntries();
			allPluginEntries.addAll(Arrays.asList(plugins));
			INonPluginEntry[] nonPlugins = getNonPluginEntries();
			allNonPluginEntries.addAll(Arrays.asList(nonPlugins));

			IFeatureReference[] children = getIncludedFeatures();
			for (int i = 0; i < children.length; i++) {
				plugins = children[i].getFeature(null).getPluginEntries();
				allPluginEntries.addAll(Arrays.asList(plugins));
				nonPlugins = children[i].getFeature(null).getNonPluginEntries();
				allNonPluginEntries.addAll(Arrays.asList(nonPlugins));
			}

			IPluginEntry[] totalPlugins =
				new IPluginEntry[allPluginEntries.size()];
			INonPluginEntry[] totalNonPlugins =
				new INonPluginEntry[allNonPluginEntries.size()];
			if (allPluginEntries.size() != 0) {
				allPluginEntries.toArray(totalPlugins);
			}
			if (allNonPluginEntries.size() != 0) {
				allNonPluginEntries.toArray(totalNonPlugins);
			}

			return getFeatureContentProvider().getDownloadSizeFor(
				totalPlugins,
				totalNonPlugins);

		} catch (CoreException e) {
			UpdateCore.warn(null, e);
			return ContentEntry.UNKNOWN_SIZE;
		}
	}

	/**
	 * Returns the install size of the feature, if it can be determined.
	 * 
	 * @see IFeature#getInstallSize()
	 * @since 2.0
	 */
	public long getInstallSize() {
		try {
			Set allPluginEntries = new HashSet();
			Set allNonPluginEntries = new HashSet();

			IPluginEntry[] plugins = getPluginEntries();
			allPluginEntries.addAll(Arrays.asList(plugins));
			INonPluginEntry[] nonPlugins = getNonPluginEntries();
			allNonPluginEntries.addAll(Arrays.asList(nonPlugins));

			IFeatureReference[] children = getIncludedFeatures();
			for (int i = 0; i < children.length; i++) {
				plugins = children[i].getFeature(null).getPluginEntries();
				allPluginEntries.addAll(Arrays.asList(plugins));
				nonPlugins = children[i].getFeature(null).getNonPluginEntries();
				allNonPluginEntries.addAll(Arrays.asList(nonPlugins));
			}

			IPluginEntry[] totalPlugins =
				new IPluginEntry[allPluginEntries.size()];
			INonPluginEntry[] totalNonPlugins =
				new INonPluginEntry[allNonPluginEntries.size()];
			if (allPluginEntries.size() != 0) {
				allPluginEntries.toArray(totalPlugins);
			}
			if (allNonPluginEntries.size() != 0) {
				allNonPluginEntries.toArray(totalNonPlugins);
			}

			return getFeatureContentProvider().getInstallSizeFor(
				totalPlugins,
				totalNonPlugins);

		} catch (CoreException e) {
			UpdateCore.warn(null, e);
			return ContentEntry.UNKNOWN_SIZE;
		}
	}

	/**
	 * Returns the content provider for this feature.
	 * 
	 * @see IFeature#getFeatureContentProvider()
	 * @since 2.0
	 */
	public IFeatureContentProvider getFeatureContentProvider()
		throws CoreException {
		if (featureContentProvider == null) {
			throw Utilities.newCoreException(
				Policy.bind(
					"Feature.NoContentProvider",
					getVersionedIdentifier().toString()),
				null);
			//$NON-NLS-1$
		}
		return this.featureContentProvider;
	}

	/**
	 * Returns the content consumer for this feature.
	 * 
	 * @see IFeature#getFeatureContentConsumer()
	 * @since 2.0
	 */
	public IFeatureContentConsumer getFeatureContentConsumer()
		throws CoreException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the site for this feature.
	 * 
	 * @see IFeature#setSite(ISite)
	 * @since 2.0
	 */
	public void setSite(ISite site) throws CoreException {
		if (this.site != null) {
			String featureURLString =
				(getURL() != null) ? getURL().toExternalForm() : "";
			throw Utilities.newCoreException(
				Policy.bind("Feature.SiteAlreadySet", featureURLString),
				null);
			//$NON-NLS-1$
		}
		this.site = site;
	}

	/**
	 * Sets the content provider for this feature.
	 * 
	 * @see IFeature#setFeatureContentProvider(IFeatureContentProvider)
	 * @since 2.0
	 */
	public void setFeatureContentProvider(IFeatureContentProvider featureContentProvider) {
		this.featureContentProvider = featureContentProvider;
		featureContentProvider.setFeature(this);
	}

	/**
	 * Return the string representation of this fetaure
	 * 
	 * @return feature as string
	 * @since 2.0
	 */
	public String toString() {
		String URLString =
			(getURL() == null)
				? Policy.bind("Feature.NoURL")
				: getURL().toExternalForm();
		//$NON-NLS-1$
		String verString =
			Policy.bind(
				"Feature.FeatureVersionToString",
				URLString,
				getVersionedIdentifier().toString());
		//$NON-NLS-1$
		String label = getName() == null ? "" : getName();
		return verString + " [" + label + "]";
	}

	/*
	 * Installation has been cancelled, abort and revert
	 */
	private void abort() throws CoreException {
		String msg = Policy.bind("Feature.InstallationCancelled"); //$NON-NLS-1$
		throw new InstallAbortedException(msg, null);
	}

	/*
	 * Initializes includes feature references
	 * If the included feature reference is found on the site, add it to the List
	 * Otherwise attempt to instanciate it using the same type as this feature and
	 * using the default location on the site.
	 */
	private void initializeIncludedReferences() throws CoreException {
		includedFeatureReferences = new ArrayList();

		IIncludedFeatureReference[] nestedFeatures = getFeatureIncluded();
		if (nestedFeatures.length == 0)
			return;

		ISite site = getSite();
		if (site == null)
			return;

		for (int i = 0; i < nestedFeatures.length; i++) {
			IIncludedFeatureReference include = nestedFeatures[i];
			IIncludedFeatureReference newRef =
				getPerfectIncludeFeature(site, include);
			includedFeatureReferences.add(newRef);
		}
	}

	/*
	 * 
	 */
	private IIncludedFeatureReference getPerfectIncludeFeature(
		ISite site,
		IIncludedFeatureReference include)
		throws CoreException {

		// [20367] no site, cannot initialize nested references
		ISiteFeatureReference[] refs = site.getFeatureReferences();
		VersionedIdentifier identifier = include.getVersionedIdentifier();

		// too long to compute if not a file system
		// other solution would be to parse feature.xml
		// when parsing file system to create archive features/FeatureId_Ver.jar
		if ("file".equals(site.getURL().getProtocol())) {
			// check if declared on the Site
			if (refs != null) {
				for (int ref = 0; ref < refs.length; ref++) {
					if (refs[ref] != null) {
						VersionedIdentifier id =
							refs[ref].getVersionedIdentifier();
						if (identifier.equals(id)) {
							// found a ISiteFeatureReference that matches our IIncludedFeatureReference
							IncludedFeatureReference newRef =
								new IncludedFeatureReference(refs[ref]);
							newRef.isOptional(include.isOptional());
							if (include instanceof FeatureReference)
								newRef.setLabel(
									((FeatureReference) include)
										.getLabel());
							newRef.setMatchingRule(include.getMatch());
							newRef.setSearchLocation(
								include.getSearchLocation());
							return newRef;
						}
					}
				}
			}
		}

		// instanciate by mapping it based on the site.xml
		// in future we may ask for a factory to create the feature ref
		IncludedFeatureReference newRef = new IncludedFeatureReference(include);
		newRef.setSite(getSite());
		IFeatureReference parentRef = getSite().getFeatureReference(this);
		if (parentRef instanceof FeatureReference) {
			newRef.setType(((FeatureReference) parentRef).getType());
		}
		String featureID =
			Site.DEFAULT_FEATURE_PATH + identifier.toString() + ".jar";
		URL featureURL =
			getSite().getSiteContentProvider().getArchiveReference(featureID);
		newRef.setURL(featureURL);
		newRef.setFeatureIdentifier(identifier.getIdentifier());
		newRef.setFeatureVersion(identifier.getVersion().toString());
		try {
			newRef.resolve(getSite().getURL(), null);
			// no need to get the bundle
			return newRef;
		} catch (Exception e) {
			throw Utilities.newCoreException(
				Policy.bind(
					"Feature.UnableToInitializeFeatureReference",
					identifier.toString()),
				e);
		}
	}

	/*
	 * 
	 */
	private void debug(String trace) {
		//DEBUG
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_INSTALL) {
			UpdateCore.debug(trace);
		}
	}

	/*
	 * 
	 */
	private void setMonitorTaskName(
		IProgressMonitor monitor,
		String taskName) {
		if (monitor != null)
			monitor.setTaskName(taskName);
	}

	/*
	 *
	 */
	private void monitorWork(IProgressMonitor monitor, int tick)
		throws CoreException {
		if (monitor != null) {
			monitor.worked(tick);
			if (monitor.isCanceled()) {
				abort();
			}
		}
	}

	/*
	 * 
	 */
	private void verifyReferences(
		IVerifier verifier,
		ContentReference[] references,
		InstallMonitor monitor,
		IVerificationListener verificationListener,
		boolean isFeature)
		throws CoreException {
		IVerificationResult vr = null;
		if (verifier != null) {
			for (int j = 0; j < references.length; j++) {
				vr = verifier.verify(this, references[j], isFeature, monitor);
				if (vr != null) {
					if (verificationListener == null)
						return;

					int result = verificationListener.prompt(vr);

					if (result == IVerificationListener.CHOICE_ABORT) {
						String msg = Policy.bind("JarVerificationService.CancelInstall"); //$NON-NLS-1$
						Exception e = vr.getVerificationException();
						throw new InstallAbortedException(msg, e);
					}
					if (result == IVerificationListener.CHOICE_ERROR) {
						throw Utilities
							.newCoreException(
								Policy.bind(
									"JarVerificationService.UnsucessfulVerification"),
						//$NON-NLS-1$
						vr.getVerificationException());
					}
				}
			}
		}
	}

	/*
	 * returns reference if the same feature is installed on the site
	 * [18867]
	 */
	private IFeatureReference featureAlreadyInstalled(ISite targetSite) {

		ISiteFeatureReference[] references = targetSite.getFeatureReferences();
		IFeatureReference currentReference = null;
		for (int i = 0; i < references.length; i++) {
			currentReference = references[i];
			// do not compare URL
			try {
				if (this.equals(currentReference.getFeature(null)))
					return currentReference; // 18867
			} catch (CoreException e) {
				UpdateCore.warn(null, e);
			}
		}

		UpdateCore.warn(
			"ValidateAlreadyInstalled:Feature "
				+ this
				+ " not found on site:"
				+ this.getURL());
		return null;
	}

	/*
	 * re initialize children of the feature, invalidate the cache
	 * @param result FeatureReference to reinitialize.
	 */
	private void reinitializeFeature(IFeatureReference referenceToReinitialize) {

		if (referenceToReinitialize == null)
			return;

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
			UpdateCore.debug(
				"Re initialize feature reference:" + referenceToReinitialize);

		IFeature feature = null;
		try {
			feature = referenceToReinitialize.getFeature(null);
			if (feature != null && feature instanceof Feature) {
				((Feature) feature).initializeIncludedReferences();
			}
			// bug 24981 - recursively go into hierarchy
			// only if site if file 
			ISite site = referenceToReinitialize.getSite();
			if (site == null)
				return;
			URL url = site.getURL();
			if (url == null)
				return;
			if ("file".equals(url.getProtocol())) {
				IFeatureReference[] included =
					feature.getIncludedFeatures();
				for (int i = 0; i < included.length; i++) {
					reinitializeFeature(included[i]);
				}
			}
		} catch (CoreException e) {
			UpdateCore.warn("", e);
		}
	}

	/**
	 * @see org.eclipse.update.core.IFeature#getRawIncludedFeatureReferences()
	 */
	public IIncludedFeatureReference[] getIncludedFeatures()
		throws CoreException {
		return filterFeatures(getRawIncludedFeatureReferences());
	}

	/*
	 * Method filterFeatures.
	 * Also implemented in Site
	 * 
	 * @param list
	 * @return List
	 */
	private IIncludedFeatureReference[] filterFeatures(IIncludedFeatureReference[] allIncluded) {
		List list = new ArrayList();
		if (allIncluded != null) {
			for (int i = 0; i < allIncluded.length; i++) {
				IIncludedFeatureReference included = allIncluded[i];
				if (UpdateManagerUtils.isValidEnvironment(included))
					list.add(included);
				else {
					if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_WARNINGS) {
						UpdateCore.warn(
							"Filtered out feature reference:" + included);
					}
				}
			}
		}

		IIncludedFeatureReference[] result =
			new IIncludedFeatureReference[list.size()];
		if (!list.isEmpty()) {
			list.toArray(result);
		}

		return result;
	}

	/**
	 * @see org.eclipse.update.core.IFeature#getRawNonPluginEntries()
	 */
	public INonPluginEntry[] getNonPluginEntries() {
		return filterNonPluginEntry(getRawNonPluginEntries());
	}

	/**
	 * Method filterPluginEntry.
	 * @param iNonPluginEntrys
	 * @return INonPluginEntry[]
	 */
	private INonPluginEntry[] filterNonPluginEntry(INonPluginEntry[] all) {
		List list = new ArrayList();
		if (all != null) {
			for (int i = 0; i < all.length; i++) {
				if (UpdateManagerUtils.isValidEnvironment(all[i]))
					list.add(all[i]);
			}
		}

		INonPluginEntry[] result = new INonPluginEntry[list.size()];
		if (!list.isEmpty()) {
			list.toArray(result);
		}

		return result;
	}

	/**
	 * @see org.eclipse.update.core.IFeature#getRawPluginEntries()
	 */
	public IPluginEntry[] getPluginEntries() {
		return filterPluginEntry(getRawPluginEntries());
	}

	/**
	 * @see org.eclipse.update.core.IFeature#getImports()
	 */
	public IImport[] getImports() {
		return filterImports(getRawImports());
	}

	/**
	 * Method filterImports.
	 * @param iImports
	 * @return IImport[]
	 */
	private IImport[] filterImports(IImport[] all) {
		List list = new ArrayList();
		if (all != null) {
			for (int i = 0; i < all.length; i++) {
				if (UpdateManagerUtils.isValidEnvironment(all[i]))
					list.add(all[i]);
			}
		}

		IImport[] result = new IImport[list.size()];
		if (!list.isEmpty()) {
			list.toArray(result);
		}

		return result;
	}

}