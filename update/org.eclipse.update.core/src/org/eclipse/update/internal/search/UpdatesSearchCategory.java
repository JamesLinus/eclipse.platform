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
package org.eclipse.update.internal.search;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

public class UpdatesSearchCategory extends BaseSearchCategory {
	private static final String CATEGORY_ID =
		"org.eclipse.update.core.new-updates";
	private IFeature [] features;

	class Candidate {
		ArrayList children;
		Candidate parent;
		IFeatureReference ref;
		public Candidate(IFeatureReference ref) {
			this.ref = ref;
		}
		public Candidate(IFeatureReference ref, Candidate parent) {
			this(ref);
			this.parent = parent;
		}
		public void add(Candidate child) {
			if (children == null)
				children = new ArrayList();
			child.setParent(this);
			children.add(child);
		}
		void setParent(Candidate parent) {
			this.parent = parent;
		}
		public IFeatureReference getReference() {
			return ref;
		}
		void setReference(IFeatureReference ref) {
			this.ref = ref;
		}
		public IFeature getFeature(IProgressMonitor monitor) {
			try {
				return ref.getFeature(monitor);
			} catch (CoreException e) {
				return null;
			}
		}
		public Candidate getParent() {
			return parent;
		}
		public Candidate getRoot() {
			Candidate root = this;

			while (root.getParent() != null) {
				root = root.getParent();
			}
			return root;
		}
		public IURLEntry getUpdateEntry() {
			int location = IUpdateConstants.SEARCH_ROOT;

			if (ref instanceof IIncludedFeatureReference)
				location =
					((IIncludedFeatureReference) ref).getSearchLocation();
			if (parent == null || location == IUpdateConstants.SEARCH_SELF) {
				return getFeature(null).getUpdateSiteEntry();
			}
			return getRoot().getUpdateEntry();
		}
		public String toString() {
			return ref.toString();
		}
		public boolean equals(Object source) {
			if (source instanceof Candidate) {
				return this.ref.equals(((Candidate) source).getReference());
			}
			if (source instanceof IFeatureReference) {
				return this.ref.equals(source);
			}
			return false;
		}
		public void addToFlatList(ArrayList list, boolean updatableOnly) {
			// add itself
			if (!updatableOnly || isUpdatable())
				list.add(this);
			// add children
			if (children != null) {
				for (int i = 0; i < children.size(); i++) {
					Candidate child = (Candidate) children.get(i);
					child.addToFlatList(list, updatableOnly);
				}
			}
		}
		public boolean isUpdatable() {
			if (parent == null)
				return true;
			if (ref instanceof IIncludedFeatureReference) {
				return ((IIncludedFeatureReference) ref).getMatch()
					!= IUpdateConstants.RULE_PERFECT;
			}
			return false;
		}

		public int getMatch() {
			if (ref instanceof IIncludedFeatureReference)
				return ((IIncludedFeatureReference) ref).getMatch();
			return IUpdateConstants.RULE_PERFECT;
		}
	}

	private static class Hit {
		IFeature candidate;
		IFeatureReference ref;
		boolean patch;
		public Hit(IFeature candidate, IFeatureReference ref) {
			this.candidate = candidate;
			this.ref = ref;
		}
		public Hit(IFeature candidate, IFeatureReference ref, boolean patch) {
			this(candidate, ref);
			this.patch = patch;
		}

		public IInstallFeatureOperation getJob() {
			try {
				IFeature feature = ref.getFeature(null);
				return OperationsManager.getOperationFactory().createInstallOperation(null, null, feature, null, null, null);
			} catch (CoreException e) {
				return null;
			}
		}

		public boolean isPatch() {
			return patch;
		}
	}

	private class HitSorter extends Sorter {
		public boolean compare(Object left, Object right) {
			Hit hit1 = (Hit) left;
			Hit hit2 = (Hit) right;
			try {
				VersionedIdentifier hv1 = hit1.ref.getVersionedIdentifier();
				VersionedIdentifier hv2 = hit2.ref.getVersionedIdentifier();
				return isNewerVersion(hv2, hv1);
			} catch (CoreException e) {
				return false;
			}
		}
	}

	class UpdateQuery implements IUpdateSearchQuery {
		IFeature candidate;
		IQueryUpdateSiteAdapter adapter;
		int match = IImport.RULE_PERFECT;

		public UpdateQuery(
			IFeature candidate,
			int match,
			IURLEntry updateEntry) {
			this.candidate = candidate;
			this.match = match;
			if (updateEntry != null && updateEntry.getURL() != null)
				adapter =
					new QueryUpdateSiteAdapter(
						getLabelForEntry(updateEntry),
						updateEntry.getURL(),
						candidate.getVersionedIdentifier().getIdentifier());
		}
		private String getLabelForEntry(IURLEntry entry) {
			String label = entry.getAnnotation();
			if (label == null || label.length() == 0)
				label = entry.getURL().toString();
			return label;
		}

		public IQueryUpdateSiteAdapter getQuerySearchSite() {
			return adapter;
		}
		private boolean isBroken() {
			try {
				IStatus status =
					SiteManager.getLocalSite().getFeatureStatus(candidate);
				return status.getSeverity() == IStatus.ERROR;
			} catch (CoreException e) {
				return false;
			}
		}
		private boolean isMissingOptionalChildren(IFeature feature) {
			try {
				IIncludedFeatureReference[] children =
					feature.getIncludedFeatures();
				for (int i = 0; i < children.length; i++) {
					IIncludedFeatureReference ref = children[i];
					try {
						IFeature child = ref.getFeature(null);
						// If we are here, the child is not missing.
						// Check it's children recursively.
						if (isMissingOptionalChildren(child))
							return true;
					} catch (CoreException e) {
						// Missing child. Return true if optional,
						// otherwise it is a broken feature that we 
						// do not care about.
						if (ref.isOptional()) {
							return FeatureHierarchyElement.hasOlderVersion(ref);
						}
					}
				}
			} catch (CoreException e) {
			}
			return false;
		}
		public void run(
			ISite site,
			String[] categoriesToSkip,
			IUpdateSearchFilter filter,
			IUpdateSearchResultCollector collector,
			IProgressMonitor monitor) {
			ArrayList hits = new ArrayList();
			boolean broken = isBroken();
			boolean missingOptionalChildren = false;

			// Don't bother to compute missing optional children
			// if the feature is broken - all we want is to 
			// see if we should allow same-version re-install.
			if (!broken)
				missingOptionalChildren = isMissingOptionalChildren(candidate);
			ISiteFeatureReference[] refs = site.getFeatureReferences();
			monitor.beginTask("", refs.length + 1);
			for (int i = 0; i < refs.length; i++) {
				ISiteFeatureReference ref = refs[i];
				try {
					if (isNewerVersion(candidate.getVersionedIdentifier(),
						ref.getVersionedIdentifier(),
						match)) {
						hits.add(new Hit(candidate, ref));
					} else {
						// accept the same feature if the installed
						// feature is broken
						if ((broken || missingOptionalChildren)
							&& candidate.getVersionedIdentifier().equals(
								ref.getVersionedIdentifier()))
							hits.add(new Hit(candidate, ref));
						else {
							// check for patches
							if (isPatch(candidate, ref))
								hits.add(new Hit(candidate, ref, true));
						}
					}
				} catch (CoreException e) {
				}
				monitor.worked(1);
				if (monitor.isCanceled())
					return;
			}
			if (hits.size() > 0) {
				collectValidHits(hits, filter, collector);
			}
			monitor.worked(1);
			monitor.done();
		}
	}

	private ArrayList candidates;

	public UpdatesSearchCategory() {
		super(CATEGORY_ID);
	}

	private void collectValidHits(
		ArrayList hits,
		IUpdateSearchFilter filter,
		IUpdateSearchResultCollector collector) {
		Object[] array = hits.toArray();
		HitSorter sorter = new HitSorter();
		sorter.sortInPlace(array);
		IFeature topHit = null;
		for (int i = 0; i < array.length; i++) {
			Hit hit = (Hit) array[i];
			IInstallFeatureOperation job = hit.getJob();
			if (job == null)
				continue;
			// do not accept updates without a license
			if (!UpdateUtils.hasLicense(job.getFeature()))
				continue;
			IStatus status = OperationsManager.getValidator().validatePendingInstall(job.getOldFeature(), job.getFeature());
			if (status == null || status.getCode() == IStatus.WARNING) {
				if (hit.isPatch()) {
					IFeature patch = job.getFeature();
					// Do not add the patch if already installed
					IFeature[] sameId = UpdateUtils.getInstalledFeatures(patch, false);
					if (sameId.length==0) {
						if (filter.accept(patch))
							collector.accept(patch);
					}
				}
				else if (topHit == null) {
					topHit = job.getFeature();
					if (filter.accept(topHit))
						collector.accept(topHit);
				}
			}
		}
	}

	private void initialize() {
		candidates = new ArrayList();
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfiguredSite[] isites = config.getConfiguredSites();
			for (int i = 0; i < isites.length; i++) {
				contributeCandidates(isites[i]);
			}
		} catch (CoreException e) {
			UpdateCore.log(
				"Error while initializing the search for new updates",
				e);
		}
	}

	private void contributeCandidates(IConfiguredSite isite)
		throws CoreException {
		IFeatureReference[] refs = isite.getConfiguredFeatures();
		ArrayList candidatesPerSite = new ArrayList();
		for (int i = 0; i < refs.length; i++) {
			IFeatureReference ref = refs[i];
			// Don't waste time searching for updates to 
			// patches.
			try {
				if (UpdateUtils.isPatch(ref.getFeature(null)))
					continue;
			}
			catch (CoreException e) {
				continue;
			}
			Candidate c = new Candidate(ref);
			candidatesPerSite.add(c);
		}
		// Create a tree from a flat list
		buildHierarchy(candidatesPerSite);
		// Add the remaining root candidates to 
		// the global list of candidates.
		candidates.addAll(candidatesPerSite);
	}

	private void buildHierarchy(ArrayList candidates) throws CoreException {
		Candidate[] array =
			(Candidate[]) candidates.toArray(new Candidate[candidates.size()]);
		// filter out included features so that only top-level features remain on the list
		for (int i = 0; i < array.length; i++) {
			Candidate parent = array[i];
			IFeature feature = parent.getFeature(null);
			IFeatureReference[] included =
				feature.getIncludedFeatures();
			for (int j = 0; j < included.length; j++) {
				IFeatureReference fref = included[j];
				Candidate child = findCandidate(candidates, fref);
				if (child != null) {
					parent.add(child);
					child.setReference(fref);
					candidates.remove(child);
				}
			}
		}
	}
	private Candidate findCandidate(ArrayList list, IFeatureReference ref) {
		for (int i = 0; i < list.size(); i++) {
			Candidate c = (Candidate) list.get(i);
			if (c.ref.equals(ref))
				return c;
		}
		return null;
	}

	public IUpdateSearchQuery[] getQueries() {
		initialize();
		ArrayList allCandidates = getAllCandidates();

		IUpdateSearchQuery[] queries =
			new IUpdateSearchQuery[allCandidates.size()];
		for (int i = 0; i < queries.length; i++) {
			Candidate candidate = (Candidate) allCandidates.get(i);
			IFeature feature = candidate.getFeature(null);
			int match = candidate.getMatch();
			IURLEntry updateEntry = candidate.getUpdateEntry();
			if (feature == null) {
				queries[i] = null;
			} else {
				queries[i] = new UpdateQuery(feature, match, updateEntry);
			}
		}
		return queries;
	}
	
/**
 * Sets the features for which new updates need to be found. If
 * not set, updates will be searched for all the installed
 * and configured features.
 * @param features the features to search updates for
 */	
	public void setFeatures(IFeature [] features) {
		this.features = features;
	}
	
/**
 * Returns an array of features for which updates need to
 * be found. 
 * @return an array of features or <samp>null</samp> if not
 * set.
 */	
	public IFeature [] getFeatures() {
		return features;
	}
	
	private boolean isNewerVersion(
		VersionedIdentifier fvi,
		VersionedIdentifier cvi) {
		if (!fvi.getIdentifier().equals(cvi.getIdentifier()))
			return false;
		PluginVersionIdentifier fv = fvi.getVersion();
		PluginVersionIdentifier cv = cvi.getVersion();
		String mode = getUpdateVersionsMode();
		boolean greater = cv.isGreaterThan(fv);
		if (!greater)
			return false;
		if (mode.equals(UpdateCore.EQUIVALENT_VALUE))
			return cv.isEquivalentTo(fv);
		else if (mode.equals(UpdateCore.COMPATIBLE_VALUE))
			return cv.isCompatibleWith(fv);
		else
			return false;
	}

	private boolean isNewerVersion(
		VersionedIdentifier fvi,
		VersionedIdentifier cvi,
		int match) {
		if (!fvi.getIdentifier().equals(cvi.getIdentifier()))
			return false;
		PluginVersionIdentifier fv = fvi.getVersion();
		PluginVersionIdentifier cv = cvi.getVersion();
		String mode = getUpdateVersionsMode();
		boolean greater = cv.isGreaterThan(fv);
		if (!greater)
			return false;
		int userMatch = IImport.RULE_GREATER_OR_EQUAL;
		if (mode.equals(UpdateCore.EQUIVALENT_VALUE))
			userMatch = IImport.RULE_EQUIVALENT;
		else if (mode.equals(UpdateCore.COMPATIBLE_VALUE))
			userMatch = IImport.RULE_COMPATIBLE;
		// By default, use match rule defined in the preferences
		int resultingMatch = userMatch;
		//If match has been encoded in the feature reference,
		// pick the most conservative of the two values.
		if (match != IImport.RULE_PERFECT) {
			if (match == IImport.RULE_EQUIVALENT
				|| userMatch == IImport.RULE_EQUIVALENT)
				resultingMatch = IImport.RULE_EQUIVALENT;
			else
				resultingMatch = IImport.RULE_COMPATIBLE;
		}

		if (resultingMatch == IImport.RULE_EQUIVALENT)
			return cv.isEquivalentTo(fv);
		else if (resultingMatch == IImport.RULE_COMPATIBLE)
			return cv.isCompatibleWith(fv);
		else
			return false;
	}

	private boolean isPatch(IFeature candidate, ISiteFeatureReference ref) {
		if (ref.isPatch() == false)
			return false;
		try {
			IFeature feature = ref.getFeature(null);
			return UpdateUtils.isPatch(candidate, feature);
		} catch (CoreException e) {
			return false;
		}
	}
	
	private String getUpdateVersionsMode() {
		Preferences store = UpdateCore.getPlugin().getPluginPreferences();
		return store.getString(UpdateCore.P_UPDATE_VERSIONS);
	}
/*
 * This method recursively walks the list of candidates
 * building the flat that starts with the roots but
 * also includes all the children that are updatable
 * (use 'include' clause with a match that is not 'perfect').
 */
	private ArrayList getAllCandidates() {
		ArrayList selected = new ArrayList();
		for (int i=0; i<candidates.size(); i++) {
			Candidate c = (Candidate)candidates.get(i);
			if (isOnTheList(c))
				c.addToFlatList(selected, true);
		}
		return selected;
	}
	
	private boolean isOnTheList(Candidate c) {
		if (features==null) return true;
		VersionedIdentifier vid;
		try {
			vid = c.getReference().getVersionedIdentifier();
		}
		catch (CoreException e) {
			return false;
		}
		for (int i=0; i<features.length; i++) {
			IFeature feature = features[i];
			VersionedIdentifier fvid = feature.getVersionedIdentifier();
			if (fvid.equals(vid))
				return true;
		}
		return false;
	}
}