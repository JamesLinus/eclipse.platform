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
package org.eclipse.update.internal.core;
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.boot.*;
import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.model.*;


/**
 * This class manages the reconciliation.
 */

public class SiteReconciler extends ModelObject implements IWritable {

	private SiteLocal siteLocal;
	private List newFoundFeatures;
	private Date date;
	private static final String DEFAULT_INSTALL_CHANGE_NAME = "delta.xml";
	//$NON-NLS-1$	

	/**
	 * 
	 */
	public SiteReconciler(SiteLocal siteLocal) {
		this.siteLocal = siteLocal;
	}

	/*
	 * Reconciliation is the comparison between the old preserved state and the new one from platform.cfg
	 * 
	 * If the old state contained sites that are not in the new state, the old sites are not added to the state
	 * If the new state contains sites that were not in the old state, configure the site and configure all the found features
	 * If the sites are in both states, verify the features
	 * if the old site contained features that are not in the new site, the features are not added to the site
	 * if the new site contains feature that were not in the old site, configure the new feature
	 * if the feature is in both site (old and new), use old feature state
	 * 
	 * When adding a feature to a site, we will check if the feature is broken or not. 
	 * A feature is broken when at least one of its plugin is not installed on the site.
	 * 
	 * At the end, go over all the site, get the configured features and make sure that if we find duplicates
	 * only one feature is configured
	 * 
	 * returns true if new features have been found during a pessimistic reconcile
	 * otherwise returns false
	 */
	public boolean reconcile(boolean isOptimistic) throws CoreException {

		IPlatformConfiguration platformConfig = BootLoader.getCurrentPlatformConfiguration();
		IPlatformConfiguration.ISiteEntry[] newSiteEntries = platformConfig.getConfiguredSites();
		IInstallConfiguration newInstallConfiguration = siteLocal.createNewInstallConfiguration();

		IInstallConfiguration oldInstallConfiguration = siteLocal.getCurrentConfiguration();
		IConfiguredSite[] oldConfiguredSites = new IConfiguredSite[0];
		newFoundFeatures = new ArrayList();

		// TRACE
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("Old install configuration" + ((oldInstallConfiguration == null) ? "NULL" : oldInstallConfiguration.getLabel()));
		}

		// sites from the current configuration
		if (oldInstallConfiguration != null) {
			oldConfiguredSites = oldInstallConfiguration.getConfiguredSites();

			// TRACE
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				for (int i = 0; i < oldConfiguredSites.length; i++) {
					UpdateCore.debug("Old Site :" + oldConfiguredSites[i].getSite().getURL());
				}
			}
		}

		// 16215
		// 22913, if already optimistic, do not check
		if (!isOptimistic) {
			isOptimistic = platformBaseChanged(oldConfiguredSites);
			// TRACE
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("Platform has changed? :" + isOptimistic);
			}
		}

		// check if sites from the platform are new sites or modified sites
		// if they are new add them, if they are modified, compare them with the old
		// one and add them
		for (int siteIndex = 0; siteIndex < newSiteEntries.length; siteIndex++) {

			IPlatformConfiguration.ISiteEntry currentSiteEntry = newSiteEntries[siteIndex];
			URL resolvedURL = resolveSiteEntry(currentSiteEntry);
			boolean found = false;
			IConfiguredSite currentConfigurationSite = null;

			// TRACE
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("Checking if:" + resolvedURL + " is a new site or a site to reconcile.");
			}

			// check if SiteEntry has been possibly modified
			// if it was part of the previously known configuredSite; reconcile
			// bug 33493, do not attempt to preserve old state if optimistic.Site is considered new
			if (!isOptimistic) {
				for (int index = 0; index < oldConfiguredSites.length && !found; index++) {
					currentConfigurationSite = oldConfiguredSites[index];
					URL currentConfigURL = currentConfigurationSite.getSite().getURL();

					if (UpdateManagerUtils.sameURL(resolvedURL, currentConfigURL)) {
						found = true;
						ConfiguredSite reconciledConfiguredSite = reconcile(currentConfigurationSite, isOptimistic);
						reconciledConfiguredSite.setPreviousPluginPath(currentSiteEntry.getSitePolicy().getList());
						newInstallConfiguration.addConfiguredSite(reconciledConfiguredSite);
					}
				}
			}

			// old site not found, this is a new site, create it
			if (!found) {
				// TRACE
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
					UpdateCore.debug("Site not found in previous configurations.Create new Configured Site:" + resolvedURL);
				}
				ISite site = SiteManager.getSite(resolvedURL, null);

				//site policy
				IPlatformConfiguration.ISitePolicy sitePolicy = currentSiteEntry.getSitePolicy();
				ConfiguredSite configSite = (ConfiguredSite) new BaseSiteLocalFactory().createConfigurationSiteModel((Site) site, sitePolicy.getType());
				configSite.setPlatformURLString(currentSiteEntry.getURL().toExternalForm());
				configSite.setPreviousPluginPath(currentSiteEntry.getSitePolicy().getList());
				configSite.setUpdatable(currentSiteEntry.isUpdateable());

				// Add the features to the list of new found features
				// and configure it based on reconciliation type
				ISiteFeatureReference[] newFeaturesRef = site.getFeatureReferences();
				for (int i = 0; i < newFeaturesRef.length; i++) {
					// TRACE
					if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
						String reconciliationType = isOptimistic ? "enable (optimistic)" : "disable (pessimistic)";
						UpdateCore.debug("New Site Found:New Feature to create: " + newFeaturesRef[i].getURL() + " as " + reconciliationType);
					}

					if (isOptimistic) {
						configSite.getConfigurationPolicy().configure(newFeaturesRef[i], true, false);
					} else {
						configSite.getConfigurationPolicy().unconfigure(newFeaturesRef[i], true, false);
						newFoundFeatures.add(newFeaturesRef[i]);
					}
				}
				newInstallConfiguration.addConfiguredSite(configSite);
			}
		}

		// verify we do not have 2 features with different version that
		// are configured 
		checkConfiguredFeatures(newInstallConfiguration);

		// add Activity reconciliation
		BaseSiteLocalFactory siteLocalFactory = new BaseSiteLocalFactory();
		ConfigurationActivity activity = siteLocalFactory.createConfigurationActivityModel();
		activity.setAction(IActivity.ACTION_RECONCILIATION);
		activity.setDate(new Date());
		activity.setLabel(siteLocal.getLocationURLString());
		((InstallConfiguration) newInstallConfiguration).addActivityModel(activity);

		// [22993] set the timeline to the previous InstallConfiguration
		// if the reconciliation is not optimistic (if the world hasn't changed)
		if (!isOptimistic) {
			if (oldInstallConfiguration != null) {
				if (newInstallConfiguration instanceof InstallConfiguration) {
					((InstallConfiguration) newInstallConfiguration).setTimeline(oldInstallConfiguration.getTimeline());
				}
			}
		}

		// add the configuration as the currentConfig
		siteLocal.addConfiguration(newInstallConfiguration);
		siteLocal.save();

		return saveNewFeatures(newInstallConfiguration);
	}


	/**
	* 
	*/
	/*package */
	URL resolveSiteEntry(IPlatformConfiguration.ISiteEntry newSiteEntry) throws CoreException {
		URL resolvedURL = null;
		try {
			resolvedURL = Platform.resolve(newSiteEntry.getURL());
		} catch (IOException e) {
			throw Utilities.newCoreException(Policy.bind("SiteLocal.UnableToResolve", newSiteEntry.getURL().toExternalForm()), e);
			//$NON-NLS-1$
		}
		return resolvedURL;
	}

	/**
	 * Compare the old state of ConfiguredSite with
	 * the 'real' features we found in Site
	 * 
	 * getSite of ConfiguredSite contains the real features found
	 * 
	 * So if ConfiguredSite.getPolicy has feature A and D as configured and C as unconfigured
	 * And if the Site contains features A,B and C
	 * We have to remove D and Configure B
	 * 
	 * We copy the oldConfig without the Features
	 * Then we loop through the features we found on the real site
	 * If they didn't exist before we add them as configured
	 * Otherwise we use the old policy and add them to the new configuration site
	 */
	private ConfiguredSite reconcile(IConfiguredSite oldConfiguredSite, boolean isOptimistic) throws CoreException {

		// TRACE
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("Configured Site to reconfigure:" + oldConfiguredSite.getSite().getURL() + (isOptimistic ? " OPTIMISTIC" : " PESSIMISTIC"));
		}

		ConfiguredSite newConfiguredSite = createNewConfigSite(oldConfiguredSite);
		ConfigurationPolicy newSitePolicy = newConfiguredSite.getConfigurationPolicy();
		ConfigurationPolicy oldSitePolicy = ((ConfiguredSite) oldConfiguredSite).getConfigurationPolicy();

		// check the Features that are still on the new version of the Config Site
		// and the new one. Add the new Features as Configured
		List toCheck = new ArrayList();
		ISite site = oldConfiguredSite.getSite();
		ISiteFeatureReference[] foundFeatures = site.getFeatureReferences();
		IFeatureReference[] oldConfiguredFeaturesRef = oldConfiguredSite.getFeatureReferences();

		// TRACE
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			for (int i = 0; i < oldConfiguredFeaturesRef.length; i++) {
				UpdateCore.debug("Old feature :" + oldConfiguredFeaturesRef[i].getURL());
			}
		}

		for (int i = 0; i < foundFeatures.length; i++) {
			boolean newFeatureFound = true;

			// TRACE
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("Is this feature new? :" + foundFeatures[i].getURL());
			}

			// if it is an old feature, add it to the list of features to check	
			for (int j = 0; j < oldConfiguredFeaturesRef.length; j++) {
				IFeatureReference oldFeatureRef = oldConfiguredFeaturesRef[j];
				if (oldFeatureRef != null && oldFeatureRef.equals(foundFeatures[i])) {
					toCheck.add(oldFeatureRef);
					newFeatureFound = false;
				}
			}

			// new feature found: add as configured if the policy is optimistic
			// or [2.0.1] if the feature is optional by all the parents AND one exact parent 
			// (pointing to same version) is enable
			if (newFeatureFound) {
				configureNewFoundFeature(isOptimistic, newSitePolicy, oldSitePolicy, foundFeatures[i], oldConfiguredFeaturesRef);
			}
		}

		// if a feature has been found in new and old state 
		// use old state (configured/unconfigured)
		// pessimistic or optimistic
		// do not call install handler as the configure/unconfigure already happened
		Iterator featureIter = toCheck.iterator();
		while (featureIter.hasNext()) {
			IFeatureReference oldFeatureRef = (IFeatureReference) featureIter.next();
			if (oldSitePolicy.isConfigured(oldFeatureRef)) {
				newSitePolicy.configure(oldFeatureRef, false, false);
			} else {
				newSitePolicy.unconfigure(oldFeatureRef, false, false);
			}
		}

		return newConfiguredSite;
	}

	/*
	 * Enable feature if:
	 * This is an optimistic reconciliation OR
	 * The feature is considered optional by ALL its parents AND at least one of them is enable
	 * Otherwise disable the feature.
	 * 
	 * If all its parent consider the feature as optional but none are enable, 
	 * do not add in the list of new found features. Just disable it.
	 */
	private void configureNewFoundFeature(boolean isOptimistic, ConfigurationPolicy newSitePolicy, ConfigurationPolicy oldSitePolicy, IFeatureReference foundFeature, IFeatureReference[] possibleParents) throws CoreException {

		// TRACE
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			String reconciliationType = isOptimistic ? "enable (optimistic)" : "disable (pessimistic)";
			UpdateCore.debug("This feature is new: " + foundFeature.getURL() + " reconciled as " + reconciliationType);
		}

		if (isOptimistic) {
			newSitePolicy.configure(foundFeature, true, false);
			return;
		}

		IFeatureReference[] allOptionalParents = UpdateManagerUtils.getParentFeatures(foundFeature, possibleParents, true);
		IFeatureReference[] allParents = UpdateManagerUtils.getParentFeatures(foundFeature, possibleParents, false);

		// none of my parents consider me as optional OR I have no parents,
		// consider as root feature
		if (allOptionalParents.length == 0) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("There are no features who consider the feature as optional. Treat as root feature.");
			}
			newSitePolicy.unconfigure(foundFeature, true, false);
			newFoundFeatures.add(foundFeature);
			return;

		}

		//At least one of my parent considers me non optional
		// consider root feature
		if (allParents.length > allOptionalParents.length) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("At least one parent considers the feature as NON optional. Treat as root feature.");
			}
			newSitePolicy.unconfigure(foundFeature, true, false);
			newFoundFeatures.add(foundFeature);
			return;
		}

		for (int i = 0; i < allOptionalParents.length; i++) {
			// one parent that consider me optional is enable, enable feature
			if (oldSitePolicy.isConfigured(allOptionalParents[i])) {
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
					UpdateCore.debug("Found parent feature:" + allOptionalParents[i] + " as enable: Enable optional child feature:" + foundFeature);
				}
				newSitePolicy.configure(foundFeature, true, false);
				return;
			}
		}

		// found parent that consider me optional but they are all disable
		// unconfigure feature without adding it to the list fo new found features
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("No parents are enable. Disable feature.");
		}
		newSitePolicy.unconfigure(foundFeature, true, false);

	}

	/**
	* Validate we have only one configured feature per site
	* even if we found multiples
	* 
	* If we find 2 features, the one with a higher version is configured
	* If they have the same version, the first feature is configured
	* 
	* DO NOT check across sites [17980]
	* If Feature1 is installed natively on Site A
	* If Feature1 is installed on Site B
	* If Feature1 from SiteA is removed... 
	*/
	private void checkConfiguredFeatures(IInstallConfiguration newDefaultConfiguration) throws CoreException {

		IConfiguredSite[] configuredSites = newDefaultConfiguration.getConfiguredSites();

		// each configured site
		for (int indexConfiguredSites = 0; indexConfiguredSites < configuredSites.length; indexConfiguredSites++) {
			checkConfiguredFeatures(configuredSites[indexConfiguredSites]);
		}
	}

	/**
	 * Validate we have only one configured feature of a specific id
	 * per configured site
	 */
	public static void checkConfiguredFeaturesOld(IConfiguredSite configuredSite) throws CoreException {

		// NOT USED

		ConfiguredSite cSite = (ConfiguredSite) configuredSite;
		IFeatureReference[] configuredFeatures = cSite.getConfiguredFeatures();
		ConfigurationPolicy cPolicy = cSite.getConfigurationPolicy();

		// TRACE
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("Compare features within :" + configuredSite.getSite().getURL());
		}

		for (int indexConfiguredFeatures = 0; indexConfiguredFeatures < configuredFeatures.length - 1; indexConfiguredFeatures++) {

			IFeatureReference featureToCompare = configuredFeatures[indexConfiguredFeatures];

			// within the configured site
			// compare with the other configured features of this site
			for (int restOfConfiguredFeatures = indexConfiguredFeatures + 1; restOfConfiguredFeatures < configuredFeatures.length; restOfConfiguredFeatures++) {
				int result = compare(featureToCompare, configuredFeatures[restOfConfiguredFeatures]);
				if (result != 0) {
					if (result == 1) {
						cPolicy.unconfigure(configuredFeatures[restOfConfiguredFeatures], true, false);
					}
					if (result == 2) {
						cPolicy.unconfigure(featureToCompare, true, false);
					}
				}
			}
		}
	}

	/**
	 * compare two feature references
	 * returns 0 if the feature are different
	 * returns 1 if the version of feature 1 is greater than the version of feature 2
	 * returns 2 if opposite
	 */
	private static int compare(IFeatureReference featureRef1, IFeatureReference featureRef2) throws CoreException {

		// TRACE
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("Compare: " + featureRef1 + " && " + featureRef2);
		}

		if (featureRef1 == null)
			return 0;

		IFeature feature1 = null;
		IFeature feature2 = null;
		try {
			feature1 = featureRef1.getFeature(null);
			feature2 = featureRef2.getFeature(null);
		} catch (CoreException e) {
			UpdateCore.warn(null, e);
			return 0;
		}

		if (feature1 == null || feature2 == null) {
			return 0;
		}

		VersionedIdentifier id1 = feature1.getVersionedIdentifier();
		VersionedIdentifier id2 = feature2.getVersionedIdentifier();

		if (id1 == null || id2 == null) {
			return 0;
		}

		if (id1.getIdentifier() != null && id1.getIdentifier().equals(id2.getIdentifier())) {
			PluginVersionIdentifier version1 = id1.getVersion();
			PluginVersionIdentifier version2 = id2.getVersion();
			if (version1 != null) {
				if (version1.isGreaterThan(version2)) {
					return 1;
				} else {
					return 2;
				}
			} else {
				return 2;
			}
		}
		return 0;
	}

	/*
	 * 
	 */
	private ConfiguredSite createNewConfigSite(IConfiguredSite oldConfiguredSiteToReconcile) throws CoreException {
		// create a copy of the ConfigSite based on old ConfigSite
		// this is not a clone, do not copy any features
		ConfiguredSite cSiteToReconcile = (ConfiguredSite) oldConfiguredSiteToReconcile;
		Site siteModel = cSiteToReconcile.getSiteModel();
		int policy = cSiteToReconcile.getConfigurationPolicy().getPolicy();

		// copy values of the old ConfigSite that should be preserved except Features
		ConfiguredSite newConfigurationSite = (ConfiguredSite) new BaseSiteLocalFactory().createConfigurationSiteModel(siteModel, policy);
		newConfigurationSite.setUpdatable(cSiteToReconcile.isUpdatable());
		newConfigurationSite.setEnabled(cSiteToReconcile.isEnabled());
		newConfigurationSite.setPlatformURLString(cSiteToReconcile.getPlatformURLString());

		return newConfigurationSite;
	}

	/*
	 * 
	 */
	private IFeatureReference[] getFeatureReferences() {
		if (newFoundFeatures == null || newFoundFeatures.size() == 0)
			return new IFeatureReference[0];

		return (IFeatureReference[]) newFoundFeatures.toArray(arrayTypeFor(newFoundFeatures));
	}

	/*
	 * 
	 */
	private boolean saveNewFeatures(IInstallConfiguration installConfig) throws CoreException {

		if (getFeatureReferences().length == 0) {
			UpdateCore.warn("No new features found");
			return false;
		}

		// recompute list of new features to only keep root features [16496]
		IFeatureReference[] refs = getFeatureReferences();
		newFoundFeatures = new ArrayList();
		for (int i = 0; i < refs.length; i++) {
			IFeatureReference[] parents = UpdateManagerUtils.getParentFeatures(refs[i], refs, false);
			if (parents.length == 0)
				newFoundFeatures.add(refs[i]);
		}


		if (getFeatureReferences().length == 0) {
			UpdateCore.warn("No root feature found when saving new features");
			return false;
		}

		// remove efixes from the delta that patch disabled feature from the installConfig
		// bug 71730
		removeInvalidEfixes(installConfig);

		if (getFeatureReferences().length == 0) {
			UpdateCore.warn("No new features found after removing invalid efixes");
			return false;
		}


		date = new Date();
		String fileName = UpdateManagerUtils.getLocalRandomIdentifier(DEFAULT_INSTALL_CHANGE_NAME, date);
		IPath path = UpdateCore.getPlugin().getStateLocation();
		IPath filePath = path.append(fileName);
		File file = filePath.toFile();
		// persist list of new features 
		try {
			UpdateManagerUtils.Writer writer = UpdateManagerUtils.getWriter(file, "UTF-8");
			writer.write(this);
			return true;
		} catch (UnsupportedEncodingException e) {
			throw Utilities.newCoreException(Policy.bind("SiteReconciler.UnableToEncodeConfiguration", file.getAbsolutePath()), e);
			//$NON-NLS-1$
		} catch (FileNotFoundException e) {
			throw Utilities.newCoreException(Policy.bind("SiteReconciler.UnableToSaveStateIn", file.getAbsolutePath()), e);
			//$NON-NLS-1$
		}
	}

	/*
	 * @see IWritable#write(int, PrintWriter)
	 */
	public void write(int indent, PrintWriter w) {
		String gap = ""; //$NON-NLS-1$
		for (int i = 0; i < indent; i++)
			gap += " "; //$NON-NLS-1$
		String increment = ""; //$NON-NLS-1$
		for (int i = 0; i < IWritable.INDENT; i++)
			increment += " "; //$NON-NLS-1$		

		// CHANGE tag
		w.print(gap + "<" + InstallChangeParser.CHANGE + " ");
		//$NON-NLS-1$ //$NON-NLS-2$
		long time = (date != null) ? date.getTime() : 0L;
		w.println("date=\"" + time + "\" >"); //$NON-NLS-1$ //$NON-NLS-2$

		// NEW FEATURE
		w.println(gap + increment + "<" + InstallChangeParser.NEW_FEATURE + " >");

		// FEATURE REF
		IFeatureReference[] references = getFeatureReferences();
		String URLFeatureString = null;
		if (references != null) {
			for (int index = 0; index < references.length; index++) {
				IFeatureReference ref = references[index];
				if (ref.getURL() != null) {
					ISite featureSite = ref.getSite();
					URLFeatureString = UpdateManagerUtils.getURLAsString(featureSite.getURL(), ref.getURL());

					w.print(gap + increment + increment + "<" + InstallChangeParser.REFERENCE + " ");
					//$NON-NLS-1$
					w.println("siteURL = \"" + UpdateManagerUtils.Writer.xmlSafe(getURLSiteString(featureSite)) + "\" ");
					//$NON-NLS-1$ //$NON-NLS-2$
					w.println(gap + increment + increment + increment + "featureURL=\"" + UpdateManagerUtils.Writer.xmlSafe(URLFeatureString) + "\" />");
					//$NON-NLS-1$ //$NON-NLS-2$
				}
				w.println(""); //$NON-NLS-1$
			}
		}

		// END NEW FEATURE
		w.println(gap + increment + "</" + InstallChangeParser.NEW_FEATURE + " >");

		// end
		w.println(gap + "</" + InstallChangeParser.CHANGE + ">");
		//$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Returns the Site URL, attempting to replace it by platform: URL if needed
	 */
	private String getURLSiteString(ISite site) {
		// since 2.0.2 ISite.getConfiguredSite();
		ConfiguredSite cSite = (ConfiguredSite) site.getCurrentConfiguredSite();
		if (cSite != null)
			return cSite.getPlatformURLString();
		return site.getURL().toExternalForm();
	}

	/*
	 * return true if the platformBase URL is not the same
	 * we thought it is. In this case we should reconcile in an optimistic way 
	 */
	private boolean platformBaseChanged(IConfiguredSite[] oldConfiguredSites) {

		if (oldConfiguredSites == null) {
			UpdateCore.warn("No previous configured sites. Optimistic reconciliation.");
			return true;
		}

		String platformString = "platform:/base/";
		URL platformURL = null;
		try {
			platformURL = new URL(platformString);
		} catch (MalformedURLException e) {
			UpdateCore.warn("Unable to resolve platform:/base/. Check you are running a Platform", e);
			return true;
		}
		URL resolvedCurrentBaseURL = null;
		try {
			resolvedCurrentBaseURL = Platform.resolve(platformURL);
		} catch (IOException e) {
			UpdateCore.warn("Error while resolving platform:/base/. Check you are running a Platform", e);
			return true;
		}

		// find the 'platform:/base/' configuredSite
		int index = 0;
		boolean found = false;
		ConfiguredSite cSite = null;
		while (!found && index < oldConfiguredSites.length) {
			if (oldConfiguredSites[index] instanceof ConfiguredSite) {
				cSite = (ConfiguredSite) oldConfiguredSites[index];
				if (platformString.equalsIgnoreCase(cSite.getPlatformURLString())) {
					found = true;
				}
			}
			index++;
		}

		if (!found) {
			UpdateCore.warn("Unable to find an old configured site with platform:/base/ as a platform URL");
			return true;
		}

		if (cSite == null) {
			UpdateCore.warn("The configuredSite that contains the platform is null");
			return true;
		}

		if (UpdateManagerUtils.sameURL(resolvedCurrentBaseURL, cSite.getSite().getURL())) {
			UpdateCore.warn("Platform URL found are the same:" + resolvedCurrentBaseURL + " : " + cSite.getSite().getURL());
			return false;
		}

		UpdateCore.warn("Platform URL found is different than the one previously saved. Reconcile optimistically:" + resolvedCurrentBaseURL + " : " + cSite.getSite().getURL());
		return true;
	}

	/**
	 * Validate the list of configured features eliminating extra
	 * entries (if possible). Make sure we do not leave configured
	 * nested features with "holes" (ie. unconfigured children)
	 */
	public static void checkConfiguredFeatures(IConfiguredSite configuredSite) {

		// Note: if we hit errors in the various computation
		// methods and throw a CoreException, we will not catch it
		// in this method. Consequently we will not attempt to
		// unconfigure any "extra" features because we would 
		// likely get it wrong. The platform will run with extra features
		// configured. The runtime will eliminate extra plugins based
		// on runtime binding rules.

		// determine "proposed" list of configured features
		ConfiguredSite cSite = (ConfiguredSite) configuredSite;
		// debug
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("Validate configuration of site " + cSite.getSite().getURL());
		}
		IFeatureReference[] configuredRefs = cSite.getConfiguredFeatures();
		ArrayList allPossibleConfiguredFeatures = new ArrayList();
		for (int i = 0; i < configuredRefs.length; i++) {
			try {
				IFeature feature = configuredRefs[i].getFeature(null);
				allPossibleConfiguredFeatures.add(feature);
				// debug
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
					UpdateCore.debug("   configured feature " + feature.getVersionedIdentifier().toString());
				}
			} catch (CoreException e) {
				UpdateCore.warn("", e);
			}
		}

		// find "unique" top level features (latest version)
		ArrayList topFeatures = computeTopFeatures(allPossibleConfiguredFeatures, configuredSite);

		// find non efix top level features
		ArrayList topNonEfixFeatures = getNonEfixFeatures(topFeatures);

		// expand non efix top level features (compute full nesting structures).
		ArrayList configuredFeatures = expandFeatures(topNonEfixFeatures, configuredSite);

		// retrieve efixes that patch enable feature
		// they must be kept enabled
		if (topFeatures.size() != topNonEfixFeatures.size()) {
			Map patches = getPatchesAsFeature(allPossibleConfiguredFeatures);
			if (!patches.isEmpty()) {
				// calculate efixes to enable
				List efixesToEnable = getPatchesToEnable(patches, configuredFeatures);
				// add efies to keep enable
				//add them to the enable list
				for (Iterator iter = efixesToEnable.iterator(); iter.hasNext();) {
					IFeature element = (IFeature) iter.next();
					ArrayList expandedEfix = new ArrayList();
					expandEfixFeature(element, expandedEfix, configuredSite);
					configuredFeatures.addAll(expandedEfix);
				}
			}
		}

		// compute extra features
		ArrayList extras = diff(allPossibleConfiguredFeatures, configuredFeatures);

		// unconfigure extra features
		ConfigurationPolicy cPolicy = cSite.getConfigurationPolicy();
		for (int i = 0; i < extras.size(); i++) {
			IFeature feature = (IFeature) extras.get(i);
			IFeatureReference ref = cSite.getSite().getFeatureReference(feature);
			try {
				cPolicy.unconfigure(ref, true, false);
				// debug
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
					UpdateCore.debug("Unconfiguring \"extra\" feature " + feature.getVersionedIdentifier().toString());
				}
			} catch (CoreException e) {
				UpdateCore.warn("", e);
			}
		}
	}

	/*
	 * 
	 */
	private static ArrayList computeTopFeatures(ArrayList features, IConfiguredSite configuredSite) {

		// start with the features passed in
		ArrayList result = new ArrayList();
		result.addAll(features);
		IFeature[] list = (IFeature[]) result.toArray(new IFeature[0]);

		// remove all features that nest in some other feature
		for (int i = 0; i < list.length; i++) {
			IIncludedFeatureReference[] children = null;
			try {
				children = list[i].getIncludedFeatures();
			} catch (CoreException e) {
				UpdateCore.warn("", e);
			}

			if (children != null) {
				for (int j = 0; j < children.length; j++) {
					// fix 71730: remove all possible matching children
					removeMatchingFeatures(children[j], result);
				}
			}
		}

		// debug
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("Computed top-level features");
			for (int i = 0; i < result.size(); i++) {
				UpdateCore.debug("   " + ((IFeature) result.get(i)).getVersionedIdentifier().toString());
			}
		}

		// eliminate duplicate versions (keep latest)
		list = (IFeature[]) result.toArray(new IFeature[0]);
		for (int i = 0; i < list.length - 1; i++) {
			IFeature left = list[i];
			VersionedIdentifier leftVid = left.getVersionedIdentifier();
			for (int j = i + 1; j < list.length; j++) {
				IFeature right = list[j];
				VersionedIdentifier rightVid = right.getVersionedIdentifier();
				if (leftVid.getIdentifier().equals(rightVid.getIdentifier())) {
					// duplicate versions ... keep latest
					IFeature oldest = null;
					// bug 31940. If right>left remove left ELSE REMOVE RIGHT
					if (rightVid.getVersion().isGreaterOrEqualTo(leftVid.getVersion()))
						oldest = left;
					else
						oldest = right;
					result.remove(oldest);
					// debug
					if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
						UpdateCore.debug("Removing \"duplicate\" " + oldest.getVersionedIdentifier().toString());
					}
				}
			}
		}

		// return resulting top level features
		return result;
	}

	/*
	 * 
	 */
	private static ArrayList expandFeatures(ArrayList features, IConfiguredSite configuredSite) {
		ArrayList result = new ArrayList();

		// expand all top level features
		for (int i = 0; i < features.size(); i++) {
			expandFeature((IFeature) features.get(i), result, configuredSite);
		}

		return result;
	}

	/*
	 * 
	 */
	private static void expandFeature(IFeature feature, ArrayList features, IConfiguredSite configuredSite) {

		// add feature
		if (!features.contains(feature)) {
			features.add(feature);
			// debug
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("Retaining configured feature " + feature.getVersionedIdentifier().toString());
			}
		}

		// add nested children to the list
		IIncludedFeatureReference[] children = null;
		try {
			children = feature.getIncludedFeatures();
		} catch (CoreException e) {
			UpdateCore.warn("", e);
			return;
		}

		for (int j = 0; j < children.length; j++) {
			IFeature child = null;
			try {
				// 71730 expand with best match (i.e not perfect match)
				child = children[j].getFeature(false, configuredSite, null);
			} catch (CoreException e) {
				if (!UpdateManagerUtils.isOptional(children[j]))
					UpdateCore.warn("", e);
				// 25202 do not return right now, the peer children may be ok
			}
			if (child != null)
				expandFeature(child, features, configuredSite);
		}
	}

	/*
	 * 
	 */
	private static ArrayList diff(ArrayList left, ArrayList right) {
		ArrayList result = new ArrayList();

		// determine difference (left "minus" right)
		for (int i = 0; i < left.size(); i++) {
			IFeature feature = (IFeature) left.get(i);
			if (!right.contains(feature))
				result.add(feature);
		}
		return result;
	}

	/*
	 * remove all possible matching childrens from the list of possible root
	 */
	private static void removeMatchingFeatures(IIncludedFeatureReference featureReference, ArrayList possibleRootFeaturesArray) {

		if (possibleRootFeaturesArray == null || possibleRootFeaturesArray.isEmpty())
			return;

		IFeature[] possibleRootFeatures = new IFeature[possibleRootFeaturesArray.size()];
		possibleRootFeaturesArray.toArray(possibleRootFeatures);

		// find the all the possible matching features in the list of possible root features 
		// remove the exact feature of each found feature reference from the list of possible root
		//boolean isIncludedFeatureReference;
		for (int ref = 0; ref < possibleRootFeatures.length; ref++) {
			try {
				if (possibleRootFeatures[ref] != null) {
					VersionedIdentifier possibleRootFeatureRefID = possibleRootFeatures[ref].getVersionedIdentifier();
					if (matches(featureReference, possibleRootFeatureRefID)) {
					//	isIncludedFeatureReference = possibleRootFeatures[ref] instanceof IIncludedFeatureReference;
					//	if (isIncludedFeatureReference) {
					//		possibleRootFeaturesArray.remove(((IIncludedFeatureReference) possibleRootFeatures[ref]).getFeature(true, null, null));
					//	} else {
					//		possibleRootFeaturesArray.remove(possibleRootFeatures[ref].getFeature(null));
					//	}
					possibleRootFeaturesArray.remove(possibleRootFeatures[ref]);
					}
				}
			} catch (CoreException e) {
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_WARNINGS)
					UpdateCore.warn("", e);
			}
		}

	}

	/*
	 * returns true if the VersionedIdentifier can be a best match for the
	 * feature Reference.
	 */
	private static boolean matches(IIncludedFeatureReference featureReference, VersionedIdentifier id) throws CoreException {
		VersionedIdentifier baseIdentifier = featureReference.getVersionedIdentifier();
		if (baseIdentifier == null || id == null)
			return false;
		if (!id.getIdentifier().equals(baseIdentifier.getIdentifier()))
			return false;

		switch (featureReference.getMatch()) {
			case IImport.RULE_PERFECT :
				return id.getVersion().isPerfect(baseIdentifier.getVersion());
			case IImport.RULE_COMPATIBLE :
				return id.getVersion().isCompatibleWith(baseIdentifier.getVersion());
			case IImport.RULE_EQUIVALENT :
				return id.getVersion().isEquivalentTo(baseIdentifier.getVersion());
			case IImport.RULE_GREATER_OR_EQUAL :
				return id.getVersion().isGreaterOrEqualTo(baseIdentifier.getVersion());
		}
		UpdateCore.warn("Unknown matching rule:" + featureReference.getMatch());
		return false;
	}
	
	/*
	 * get the list of enabled patches
	 */
	private static Map getPatchesAsFeature(ArrayList allConfiguredFeatures) {
		// get all efixes and the associated patched features
		Map patches = new HashMap();
		if (allConfiguredFeatures != null) {
			Iterator iter = allConfiguredFeatures.iterator();
			while (iter.hasNext()) {
				List patchedFeaturesID = new ArrayList();
				IFeature element = (IFeature) iter.next();
				// add the patched feature identifiers
				for (int i = 0; i < element.getImports().length; i++) {
					if (element.getImports()[i].isPatch()) {
						VersionedIdentifier id = element.getImports()[i].getVersionedIdentifier();
						if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
							UpdateCore.debug("Found patch " + element + " for feature identifier " + id);
						patchedFeaturesID.add(id);
					}
				}

				if (!patchedFeaturesID.isEmpty()) {
					patches.put(element, patchedFeaturesID);
				}
			}
		}

		return patches;
	}

	/*
	 * retruns the list of pathes-feature who patch enabled features
	 */
	private static List getPatchesToEnable(Map efixes, ArrayList configuredFeatures) {

		ArrayList enabledVersionedIdentifier = new ArrayList();
		Iterator iter = configuredFeatures.iterator();
		while (iter.hasNext()) {
			IFeature element = (IFeature) iter.next();
			enabledVersionedIdentifier.add(element.getVersionedIdentifier());
		}

		// loop through the patches
		List result = new ArrayList();
		iter = efixes.keySet().iterator();
		while (iter.hasNext()) {
			boolean toEnable = false;
			IFeature efixFeature = (IFeature) iter.next();
			List patchedFeatures = (List) efixes.get(efixFeature);
			// loop through the 'patched features identifier' the for this patch
			// see if it the patch patches at least one enable feature
			Iterator patchedFeaturesIter = patchedFeatures.iterator();
			while (patchedFeaturesIter.hasNext() && !toEnable) {
				VersionedIdentifier patchedFeatureID = (VersionedIdentifier) patchedFeaturesIter.next();
				if (enabledVersionedIdentifier.contains(patchedFeatureID)) {
					toEnable = true;
				}
			}

			if (!toEnable) {
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
				UpdateCore.debug("The Patch " + efixFeature + " does not patch any enabled features: it will be disabled");
			} else {
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
					UpdateCore.debug("The patch " + efixFeature + " will be enabled.");
				result.add(efixFeature);
			}
		}
		return result;
	}

	/*
	 * returns the feature that are not patches
	 */
	private static ArrayList getNonEfixFeatures(ArrayList topFeatures) {
		Map efixFeatures = getPatchesAsFeature(topFeatures);
		Set keySet = efixFeatures.keySet();
		if (keySet == null || keySet.isEmpty())
			return topFeatures;

		Iterator iter = topFeatures.iterator();
		ArrayList result = new ArrayList();
		while (iter.hasNext()) {
			IFeature element = (IFeature) iter.next();
			if (!keySet.contains(element)) {
				result.add(element);
			}
		}
		return result;
	}


	/*
	 * get the map of enabled patches (as feature reference)  or an empty map
	 */
	private Map getPatchesAsFeatureReference(List listOfFeatureReferences) {
		// get all efixes and the associated patched features
		Map patches = new HashMap();
		if (listOfFeatureReferences != null) {
			Iterator iter = listOfFeatureReferences.iterator();
			while (iter.hasNext()) {
				List patchedFeaturesID = new ArrayList();
				IFeatureReference element = (IFeatureReference) iter.next();
				// add the patched feature identifiers
				try {
					IFeature feature = element.getFeature(null);
					if (feature != null) {
						IImport[] imports = feature.getImports();
						for (int i = 0; i < imports.length; i++) {
							if (imports[i].isPatch()) {
								VersionedIdentifier id = imports[i].getVersionedIdentifier();
								if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
								UpdateCore.debug("Found patch " + element + " for feature identifier " + id);
								patchedFeaturesID.add(id);
							}
						}
					}

					if (!patchedFeaturesID.isEmpty()) {
						patches.put(element, patchedFeaturesID);
					}
				} catch (CoreException e) {
				}
			}
		}

		return patches;
	}


	/*
	 * Removes efixes from new found features if they do not patch an enable feature
	 * either from the found delta or from the file system
	 */
	private void removeInvalidEfixes(IInstallConfiguration installConfig) {

		// disable new found efixes if the feature is not in the list nor enabled features
		// on the file system
		Map newFoundEfixesAsReference = getPatchesAsFeatureReference(newFoundFeatures);

		if (newFoundEfixesAsReference.size() > 0) {
			// retrieve all enabled features on all the sites
			ArrayList allEnabledFeatures = new ArrayList();
			IConfiguredSite[] configSites = installConfig.getConfiguredSites();
			for (int i = 0; i < configSites.length; i++) {
				IFeatureReference[] references = configSites[i].getConfiguredFeatures();
				for (int j = 0; j < references.length; j++) {
					try {
						allEnabledFeatures.add(references[j].getFeature(null));
					} catch (CoreException e) {
					}
				}
			}

			// create a List of eFixes Features
			List arrayOfNewFoundFeatures = new ArrayList();
			for (Iterator iter = newFoundFeatures.iterator(); iter.hasNext();) {
				IFeatureReference element = (IFeatureReference) iter.next();
				try {
					arrayOfNewFoundFeatures.add(element.getFeature(null));
				} catch (CoreException e) {
				}
			}

			// retrieve the efixes that patch enable features in delta and enabled features on the 
			// file system
			List patchesForNewFoundFeatures = getFeatureReferencePatchesToEnable(newFoundEfixesAsReference, allEnabledFeatures);
			List patchesForEnabledFeatures = getFeatureReferencePatchesToEnable(newFoundEfixesAsReference, arrayOfNewFoundFeatures);

			// IMPORTANT: add efixes first so they will be processed first
			newFoundFeatures.removeAll(newFoundEfixesAsReference.keySet());
			newFoundFeatures.addAll(0,patchesForEnabledFeatures);
			newFoundFeatures.addAll(0,patchesForNewFoundFeatures);
		}
	}

	/*
	 * retruns the list of pathes-featureReference who patch enabled features
	 */
	private List getFeatureReferencePatchesToEnable(Map efixes, List configuredFeatures) {

		ArrayList enabledVersionedIdentifier = new ArrayList();
		Iterator iter = configuredFeatures.iterator();
		while (iter.hasNext()) {
			IFeature element = (IFeature) iter.next();
			enabledVersionedIdentifier.add(element.getVersionedIdentifier());
		}

		// loop through the patches
		List result = new ArrayList();
		iter = efixes.keySet().iterator();
		while (iter.hasNext()) {
			boolean toEnable = false;
			IFeatureReference efixFeatureReference = (IFeatureReference) iter.next();
			List patchedFeatures = (List) efixes.get(efixFeatureReference);
			// loop through the 'patched features identifier' the for this patch
			// see if it the patch patches at least one enable feature
			Iterator patchedFeaturesIter = patchedFeatures.iterator();
			while (patchedFeaturesIter.hasNext() && !toEnable) {
				VersionedIdentifier patchedFeatureID = (VersionedIdentifier) patchedFeaturesIter.next();
				if (enabledVersionedIdentifier.contains(patchedFeatureID)) {
					toEnable = true;
				}
			}

			if (!toEnable) {
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
				UpdateCore.debug("The Patch " + efixFeatureReference + " does not patch any enabled features: it will be disabled");
			} else {
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
				UpdateCore.debug("The patch " + efixFeatureReference + " will be enabled.");

				result.add(efixFeatureReference);
			}
		}
		return result;
	}

	/*
	 * only enable non-efix children recursively
	 */
	private static void expandEfixFeature(IFeature feature, ArrayList features, IConfiguredSite configuredSite) {

		// add feature
		if (!features.contains(feature)) {
			features.add(feature);
			// debug
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("Retaining configured feature " + feature.getVersionedIdentifier().toString());
			}
		}

		// add nested children to the list
		IIncludedFeatureReference[] children = null;
		try {
			children = feature.getIncludedFeatures();
		} catch (CoreException e) {
			UpdateCore.warn("", e);
			return;
		}

		for (int j = 0; j < children.length; j++) {
			IFeature child = null;
			try { // fix 71730, expand with the best match in the configured site
				child = children[j].getFeature(false, configuredSite,null);
			} catch (CoreException e) {
				if (!children[j].isOptional())
				UpdateCore.warn("", e);
				// 25202 do not return right now, the peer children may be ok
			}
			if (child != null){
				if (!UpdateCore.isPatch(child))
					expandEfixFeature(child, features, configuredSite);
			}
		}
	}	
	
}
