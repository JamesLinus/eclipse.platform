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
package org.eclipse.update.internal.ui.views;
import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.part.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.operations.IUpdateModelChangedListener;
import org.eclipse.update.internal.ui.UpdateUI;

/**
 * Insert the type's description here.
 * @see ViewPart
 */
public class ConfigurationView
	extends ViewPart
	implements
		IInstallConfigurationChangedListener,
		IConfiguredSiteChangedListener,
		ILocalSiteChangedListener {
	private TreeViewer treeViewer;
	private DrillDownAdapter drillDownAdapter;
	private Action collapseAllAction;
	private static final String STATE_SHOW_UNCONF = "ConfigurationView.showUnconf"; //$NON-NLS-1$
	private static final String STATE_SHOW_SITES = "ConfigurationView.showSites"; //$NON-NLS-1$
	private static final String STATE_SHOW_NESTED_FEATURES =
		"ConfigurationView.showNestedFeatures"; //$NON-NLS-1$

	private Action showSitesAction;
	private Action showNestedFeaturesAction;
	private ReplaceVersionAction swapVersionAction;
	private FeatureStateAction featureStateAction;
	private UninstallFeatureAction uninstallFeatureAction;
	private InstallOptionalFeatureAction installOptFeatureAction;
	private Action showUnconfFeaturesAction;
	private RevertConfigurationAction revertAction;
	private Action propertiesAction;
	private SiteStateAction siteStateAction;
	private Action installationHistoryAction;
	private Action newExtensionLocationAction;
	private Action detectedChangesAction;
	private FindUpdatesAction findUpdatesAction;
	private SashForm splitter;
	private ConfigurationPreview preview;
	private Hashtable previewTasks;
	private String viewName;

	private IUpdateModelChangedListener modelListener;
	private boolean refreshLock = false;
	private Image eclipseImage;
	private boolean initialized;

	class ConfigurationSorter extends ViewerSorter {
		public int category(Object obj) {
			// sites
			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfiguredSite();
				if (csite.isProductSite())
					return 1;
				if (csite.isExtensionSite())
					return 2;
				return 3;
			}
			return super.category(obj);
		}
	}

	class LocalSiteProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public void inputChanged(
			Viewer viewer,
			Object oldInput,
			Object newInput) {
			if (newInput == null)
				return;
			updateTitle(newInput);
		}

		/**
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		public Object[] getChildren(Object parent) {
			if (parent instanceof UpdateModel) {
				ILocalSite localSite = getLocalSite();
				return (localSite != null) ? new Object[] { localSite }
				: new Object[0];
			}

			if (parent instanceof ILocalSite) {
				Object[] csites = openLocalSite();
				if (showSitesAction.isChecked())
					return csites;
				ArrayList result = new ArrayList();
				boolean showUnconf = showUnconfFeaturesAction.isChecked();
				for (int i = 0; i < csites.length; i++) {
					IConfiguredSiteAdapter adapter =
						(IConfiguredSiteAdapter) csites[i];
					Object[] roots = getFeatures(adapter, !showUnconf);
					for (int j = 0; j < roots.length; j++) {
						result.add(roots[j]);
					}
				}
				return result.toArray();
			}

			if (parent instanceof IConfiguredSiteAdapter) {
				return getFeatures(
					(IConfiguredSiteAdapter) parent,
					!showUnconfFeaturesAction.isChecked());
			}
			if (parent instanceof ConfiguredFeatureAdapter
				&& showNestedFeaturesAction.isChecked()) {
				IFeatureAdapter[] nested =
					((ConfiguredFeatureAdapter) parent).getIncludedFeatures(
						null);
				if (showUnconfFeaturesAction.isChecked())
					return nested;
				ArrayList result = new ArrayList();
				for (int i = 0; i < nested.length; i++) {
					if (((ConfiguredFeatureAdapter) nested[i]).isConfigured())
						result.add(nested[i]);
				}
				return (IFeatureAdapter[]) result.toArray(
					new IFeatureAdapter[result.size()]);
			}
			return new Object[0];
		}

		public Object getParent(Object child) {
			return null;
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof ConfiguredFeatureAdapter) {
				if (!showNestedFeaturesAction.isChecked())
					return false;
				IFeatureAdapter[] features =
					((ConfiguredFeatureAdapter) parent).getIncludedFeatures(
						null);

				if (showUnconfFeaturesAction.isChecked())
					return features.length > 0;

				for (int i = 0; i < features.length; i++) {
					if (((ConfiguredFeatureAdapter) features[i])
						.isConfigured())
						return true;
				}
				return false;
			}
			if (parent instanceof ConfiguredSiteAdapter) {
				IConfiguredSite site =
					((ConfiguredSiteAdapter) parent).getConfiguredSite();
				if (site.isEnabled()) {
					if (!showUnconfFeaturesAction.isChecked())
						return site.getConfiguredFeatures().length > 0;
					return site.getFeatureReferences().length > 0;
				}
				return (showUnconfFeaturesAction.isChecked());
			}
			return true;
		}

		public Object[] getElements(Object input) {
			return getChildren(input);
		}
	}

	class LocalSiteLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof ILocalSite) {
				AboutInfo info = UpdateUI.getDefault().getAboutInfo();
				String productName = info.getProductName();
				if (productName != null)
					return productName;
				return UpdateUI.getString("ConfigurationView.current"); //$NON-NLS-1$
			}

			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfiguredSite();
				ISite site = csite.getSite();
				return site.getURL().toString();
			}
			if (obj instanceof IFeatureAdapter) {
				try {
					IFeature feature = ((IFeatureAdapter) obj).getFeature(null);
					if (feature instanceof MissingFeature) {
						return UpdateUI.getFormattedMessage(
							"ConfigurationView.missingFeature", //$NON-NLS-1$
							feature.getName());
					}
					String version =
						feature
							.getVersionedIdentifier()
							.getVersion()
							.toString();
					String pending = ""; //$NON-NLS-1$
					if (OperationsManager.findPendingOperation(feature)
						!= null)
						pending = UpdateUI.getString("ConfigurationView.pending"); //$NON-NLS-1$
					return feature.getName() + " " + version + pending; //$NON-NLS-1$
				} catch (CoreException e) {
					return UpdateUI.getString("ConfigurationView.error"); //$NON-NLS-1$
				}
			}
			return super.getText(obj);
		}

		public Image getImage(Object obj) {
			UpdateLabelProvider provider =
				UpdateUI.getDefault().getLabelProvider();
			if (obj instanceof ILocalSite)
				return eclipseImage;

			if (obj instanceof ConfiguredFeatureAdapter)
				return getFeatureImage(
					provider,
					(ConfiguredFeatureAdapter) obj);

			if (obj instanceof IConfiguredSiteAdapter) {
				IConfiguredSite csite =
					((IConfiguredSiteAdapter) obj).getConfiguredSite();
				int flags =
					csite.isUpdatable() ? 0 : UpdateLabelProvider.F_LINKED;
				if (!csite.isEnabled())
					flags |= UpdateLabelProvider.F_UNCONFIGURED;
				return provider.get(
					provider.getLocalSiteDescriptor(csite),
					flags);
			}
			return null;
		}

		private Image getFeatureImage(
			UpdateLabelProvider provider,
			ConfiguredFeatureAdapter adapter) {
			try {
				IFeature feature = adapter.getFeature(null);
				if (feature instanceof MissingFeature) {
					if (((MissingFeature) feature).isOptional())
						return provider.get(
							UpdateUIImages.DESC_NOTINST_FEATURE_OBJ);
					return provider.get(
						UpdateUIImages.DESC_FEATURE_OBJ,
						UpdateLabelProvider.F_ERROR);
				}

				boolean efix = feature.isPatch();
				ImageDescriptor baseDesc =
					efix
						? UpdateUIImages.DESC_EFIX_OBJ
						: (adapter.isConfigured()
							? UpdateUIImages.DESC_FEATURE_OBJ
							: UpdateUIImages.DESC_UNCONF_FEATURE_OBJ);

				int flags = 0;
				if (efix && !adapter.isConfigured())
					flags |= UpdateLabelProvider.F_UNCONFIGURED;
				if (OperationsManager.findPendingOperation(feature) == null) {
					ILocalSite localSite = getLocalSite();
					if (localSite != null) {
						int code =
							getStatusCode(
								feature,
								localSite.getFeatureStatus(feature));
						switch (code) {
							case IFeature.STATUS_UNHAPPY :
								flags |= UpdateLabelProvider.F_ERROR;
								break;
							case IFeature.STATUS_AMBIGUOUS :
								flags |= UpdateLabelProvider.F_WARNING;
								break;
							default :
								if (adapter.isConfigured()
									&& adapter.isUpdated())
									flags |= UpdateLabelProvider.F_UPDATED;
								break;
						}
					}
				}
				return provider.get(baseDesc, flags);
			} catch (CoreException e) {
				return provider.get(
					UpdateUIImages.DESC_FEATURE_OBJ,
					UpdateLabelProvider.F_ERROR);
			}
		}
	}

	class PreviewTask implements IPreviewTask {
		private String name;
		private String desc;
		private IAction action;
		public PreviewTask(String name, String desc, IAction action) {
			this.name = name;
			this.desc = desc;
			this.action = action;
		}

		public String getName() {
			if (name != null)
				return name;
			return action.getText();
		}
		public String getDescription() {
			return desc;
		}
		public void run() {
			action.run();
		}
		public boolean isEnabled() {
			return action.isEnabled();
		}
	}

	public ConfigurationView() {
		UpdateUI.getDefault().getLabelProvider().connect(this);
		initializeImages();
	}

	private void initializeImages() {
		ImageDescriptor edesc = UpdateUIImages.DESC_APP_OBJ;
		AboutInfo info = UpdateUI.getDefault().getAboutInfo();
		if (info.getWindowImage() != null)
			edesc = info.getWindowImage();
		eclipseImage = UpdateUI.getDefault().getLabelProvider().get(edesc);
	}

	public void initProviders() {
		treeViewer.setContentProvider(new LocalSiteProvider());
		treeViewer.setLabelProvider(new LocalSiteLabelProvider());
		treeViewer.setInput(UpdateUI.getDefault().getUpdateModel());
		treeViewer.setSorter(new ConfigurationSorter());
		ILocalSite localSite = getLocalSite();
		if (localSite != null)
			localSite.addLocalSiteChangedListener(this);

		modelListener = new IUpdateModelChangedListener() {
			public void objectsAdded(Object parent, Object[] children) {
			}
			public void objectsRemoved(Object parent, Object[] children) {
			}
			public void objectChanged(final Object obj, String property) {
				if (refreshLock)
					return;
				Control control = getControl();
				if (!control.isDisposed()) {
					control.getDisplay().asyncExec(new Runnable() {
						public void run() {
							treeViewer.refresh();
							handleSelectionChanged(
								(IStructuredSelection) treeViewer
									.getSelection());
						}
					});
				}
			}
		};
		OperationsManager.addUpdateModelChangedListener(modelListener);
		WorkbenchHelp.setHelp(
			getControl(),
			"org.eclipse.update.ui.ConfigurationView"); //$NON-NLS-1$
	}

	private ILocalSite getLocalSite() {
		try {
			return SiteManager.getLocalSite();
		} catch (CoreException e) {
			UpdateUI.logException(e);
			return null;
		}
	}

	private Object[] openLocalSite() {
		final Object[][] bag = new Object[1][];
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				ILocalSite localSite = getLocalSite();
				if (localSite == null)
					return;
				IInstallConfiguration config =
					getLocalSite().getCurrentConfiguration();
				IConfiguredSite[] sites = config.getConfiguredSites();
				Object[] result = new Object[sites.length];
				for (int i = 0; i < sites.length; i++) {
					result[i] = new ConfiguredSiteAdapter(config, sites[i]);
				}
				if (!initialized) {
					config.addInstallConfigurationChangedListener(
						ConfigurationView.this);
					initialized = true;
				}
				bag[0] = result;
			}
		});
		return bag[0];
	}

	public void dispose() {
		UpdateUI.getDefault().getLabelProvider().disconnect(this);
		if (initialized) {
			ILocalSite localSite = getLocalSite();
			if (localSite != null) {
				localSite.removeLocalSiteChangedListener(this);
				IInstallConfiguration config =
					localSite.getCurrentConfiguration();
				config.removeInstallConfigurationChangedListener(this);
			}
			initialized = false;
		}
		OperationsManager.removeUpdateModelChangedListener(modelListener);
		if (preview != null)
			preview.dispose();
		super.dispose();
	}

	protected void makeActions() {
		collapseAllAction = new Action() {
			public void run() {
				treeViewer.getControl().setRedraw(false);
				treeViewer.collapseToLevel(
					treeViewer.getInput(),
					TreeViewer.ALL_LEVELS);
				treeViewer.getControl().setRedraw(true);
			}
		};
		collapseAllAction.setText(UpdateUI.getString("ConfigurationView.collapseLabel")); //$NON-NLS-1$
		collapseAllAction.setToolTipText(UpdateUI.getString("ConfigurationView.collapseTooltip")); //$NON-NLS-1$
		collapseAllAction.setImageDescriptor(UpdateUIImages.DESC_COLLAPSE_ALL);

		drillDownAdapter = new DrillDownAdapter(treeViewer);

		featureStateAction = new FeatureStateAction();

		siteStateAction = new SiteStateAction();

		revertAction = new RevertConfigurationAction(UpdateUI.getString("ConfigurationView.revertLabel")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(
			revertAction,
			"org.eclipse.update.ui.CofigurationView_revertAction"); //$NON-NLS-1$

		installationHistoryAction =
			new InstallationHistoryAction(
				UpdateUI.getString("ConfigurationView.installHistory"), //$NON-NLS-1$
				UpdateUIImages.DESC_HISTORY_OBJ);
		installationHistoryAction.setToolTipText(installationHistoryAction.getText());
		
		newExtensionLocationAction =
			new NewExtensionLocationAction(
				UpdateUI.getString("ConfigurationView.extLocation"), //$NON-NLS-1$
				UpdateUIImages.DESC_ESITE_OBJ);

		detectedChangesAction = new DetectedChangesAction(UpdateUI.getString("ConfigurationView.detectedChanges")); //$NON-NLS-1$
		
		propertiesAction =
			new PropertyDialogAction(
				UpdateUI.getActiveWorkbenchShell(),
				treeViewer);
		WorkbenchHelp.setHelp(
			propertiesAction,
			"org.eclipse.update.ui.CofigurationView_propertiesAction"); //$NON-NLS-1$

		uninstallFeatureAction = new UninstallFeatureAction(UpdateUI.getString("ConfigurationView.uninstall")); //$NON-NLS-1$

		installOptFeatureAction =
			new InstallOptionalFeatureAction(
				getControl().getShell(),
				UpdateUI.getString("ConfigurationView.install")); //$NON-NLS-1$

		swapVersionAction = new ReplaceVersionAction(UpdateUI.getString("ConfigurationView.anotherVersion")); //$NON-NLS-1$

		findUpdatesAction =
			new FindUpdatesAction(getControl().getShell(), UpdateUI.getString("ConfigurationView.findUpdates")); //$NON-NLS-1$

		makeShowUnconfiguredFeaturesAction();
		makeShowSitesAction();
		makeShowNestedFeaturesAction();
		makePreviewTasks();
		getViewSite().getActionBars().setGlobalActionHandler(
			IWorkbenchActionConstants.PROPERTIES,
			propertiesAction);
	}

	private void makeShowNestedFeaturesAction() {
		final Preferences pref = UpdateUI.getDefault().getPluginPreferences();
		pref.setDefault(STATE_SHOW_NESTED_FEATURES, true);
		showNestedFeaturesAction = new Action() {
			public void run() {
				treeViewer.refresh();
				pref.setValue(
					STATE_SHOW_NESTED_FEATURES,
					showNestedFeaturesAction.isChecked());
			}
		};
		showNestedFeaturesAction.setText(UpdateUI.getString("ConfigurationView.showNestedFeatures")); //$NON-NLS-1$
		showNestedFeaturesAction.setImageDescriptor(
			UpdateUIImages.DESC_SHOW_HIERARCHY);
		showNestedFeaturesAction.setHoverImageDescriptor(
			UpdateUIImages.DESC_SHOW_HIERARCHY_H);
		showNestedFeaturesAction.setDisabledImageDescriptor(
			UpdateUIImages.DESC_SHOW_HIERARCHY_D);

		showNestedFeaturesAction.setChecked(
			pref.getBoolean(STATE_SHOW_NESTED_FEATURES));
		showNestedFeaturesAction.setToolTipText(UpdateUI.getString("ConfigurationView.showNestedTooltip")); //$NON-NLS-1$
	}

	private void makeShowSitesAction() {
		final Preferences pref = UpdateUI.getDefault().getPluginPreferences();
		pref.setDefault(STATE_SHOW_SITES, true);
		showSitesAction = new Action() {
			public void run() {
				treeViewer.refresh();
				pref.setValue(STATE_SHOW_SITES, showSitesAction.isChecked());
				UpdateUI.getDefault().savePluginPreferences();
			}
		};
		showSitesAction.setText(UpdateUI.getString("ConfigurationView.showInstall")); //$NON-NLS-1$
		showSitesAction.setImageDescriptor(UpdateUIImages.DESC_LSITE_OBJ);
		showSitesAction.setChecked(pref.getBoolean(STATE_SHOW_SITES));
		showSitesAction.setToolTipText(UpdateUI.getString("ConfigurationView.showInstallTooltip")); //$NON-NLS-1$
	}

	private void makeShowUnconfiguredFeaturesAction() {
		final Preferences pref = UpdateUI.getDefault().getPluginPreferences();
		pref.setDefault(STATE_SHOW_UNCONF, false);
		showUnconfFeaturesAction = new Action() {
			public void run() {
				pref.setValue(
					STATE_SHOW_UNCONF,
					showUnconfFeaturesAction.isChecked());
				UpdateUI.getDefault().savePluginPreferences();
				treeViewer.refresh();
			}
		};
		WorkbenchHelp.setHelp(
			showUnconfFeaturesAction,
			"org.eclipse.update.ui.CofigurationView_showUnconfFeaturesAction"); //$NON-NLS-1$
		showUnconfFeaturesAction.setText(UpdateUI.getString("ConfigurationView.showDisabled")); //$NON-NLS-1$
		showUnconfFeaturesAction.setImageDescriptor(
			UpdateUIImages.DESC_UNCONF_FEATURE_OBJ);
		showUnconfFeaturesAction.setChecked(pref.getBoolean(STATE_SHOW_UNCONF));
		showUnconfFeaturesAction.setToolTipText(UpdateUI.getString("ConfigurationView.showDisabledTooltip")); //$NON-NLS-1$
	}

	protected void fillActionBars(IActionBars bars) {
		IToolBarManager tbm = bars.getToolBarManager();
		tbm.add(showSitesAction);
		tbm.add(showNestedFeaturesAction);
		tbm.add(showUnconfFeaturesAction);
		tbm.add(new Separator());
		drillDownAdapter.addNavigationActions(bars.getToolBarManager());
		tbm.add(new Separator());
		tbm.add(collapseAllAction);
		tbm.add(new Separator());
		tbm.add(installationHistoryAction);
	}

	protected Object getSelectedObject() {
		ISelection selection = treeViewer.getSelection();
		if (selection instanceof IStructuredSelection
			&& !selection.isEmpty()) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() == 1)
				return ssel.getFirstElement();
		}
		return null;
	}

	protected void fillContextMenu(IMenuManager manager) {
		Object obj = getSelectedObject();

		if (obj instanceof ILocalSite) {
			manager.add(revertAction);
			manager.add(findUpdatesAction);
			manager.add(detectedChangesAction);
		} else if (obj instanceof IConfiguredSiteAdapter) {
			manager.add(siteStateAction);
		}

		if (obj instanceof ILocalSite
			|| obj instanceof IConfiguredSiteAdapter) {
			manager.add(new Separator());
			MenuManager mgr = new MenuManager(UpdateUI.getString("ConfigurationView.new")); //$NON-NLS-1$
			mgr.add(newExtensionLocationAction);
			manager.add(mgr);
			manager.add(new Separator());
		} else if (obj instanceof ConfiguredFeatureAdapter) {
			try {
				MenuManager mgr = new MenuManager(UpdateUI.getString("ConfigurationView.replaceWith")); //$NON-NLS-1$
				mgr.add(swapVersionAction);
				manager.add(mgr);

				manager.add(featureStateAction);

				IFeature feature =
					((ConfiguredFeatureAdapter) obj).getFeature(null);
				if (feature instanceof MissingFeature) {
					manager.add(installOptFeatureAction);
				} else {
					manager.add(uninstallFeatureAction);
				}
				manager.add(new Separator());
				manager.add(findUpdatesAction);
				manager.add(new Separator());
			} catch (CoreException e) {
			}
		}

		drillDownAdapter.addNavigationActions(manager);

		if (obj instanceof ILocalSite) {
			manager.add(new Separator());
			manager.add(installationHistoryAction);
		}

		if (obj instanceof IFeatureAdapter
			|| obj instanceof ILocalSite
			|| obj instanceof IConfiguredSiteAdapter) {
			manager.add(new Separator());
			manager.add(propertiesAction);
		}
	}

	public void installSiteAdded(IConfiguredSite csite) {
		asyncRefresh();
	}
	public void installSiteRemoved(IConfiguredSite site) {
		asyncRefresh();
	}
	public void featureInstalled(IFeature feature) {
		asyncRefresh();
	}
	public void featureRemoved(IFeature feature) {
		asyncRefresh();
	}
	public void featureConfigured(IFeature feature) {
	}

	public void featureUnconfigured(IFeature feature) {
	}

	public void currentInstallConfigurationChanged(IInstallConfiguration configuration) {
		asyncRefresh();
	}

	public void installConfigurationRemoved(IInstallConfiguration configuration) {
		asyncRefresh();
	}

	private void asyncRefresh() {
		Display display = SWTUtil.getStandardDisplay();
		if (display == null)
			return;
		if (getControl().isDisposed())
			return;
		display.asyncExec(new Runnable() {
			public void run() {
				if (!getControl().isDisposed())
					treeViewer.refresh();
			}
		});
	}

	private Object[] getFeatures(
		final IConfiguredSiteAdapter siteAdapter,
		final boolean configuredOnly) {
		final IConfiguredSite csite = siteAdapter.getConfiguredSite();
		final Object[][] bag = new Object[1][];
		refreshLock = true;

		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) {
				ArrayList result = new ArrayList();
				IFeatureReference[] refs;

				if (configuredOnly)
					refs = csite.getConfiguredFeatures();
				else {
					ISite site = csite.getSite();
					refs = site.getFeatureReferences();
				}
				monitor.beginTask(
					UpdateUI.getString("ConfigurationView.loading"), //$NON-NLS-1$
					refs.length);

				for (int i = 0; i < refs.length; i++) {
					IFeatureReference ref = refs[i];
					IFeature feature;
					try {
						monitor.subTask(ref.getURL().toString());
						feature = ref.getFeature(null);
					} catch (CoreException e) {
						feature =
							new MissingFeature(ref.getSite(), ref.getURL());
					}
					monitor.worked(1);
					result.add(
						new ConfiguredFeatureAdapter(
							siteAdapter,
							feature,
							csite.isConfigured(feature),
							false,
							false));
				}
				monitor.done();
				bag[0] = getRootFeatures(result);
			}
		};
		try {
			if (getViewSite().getWorkbenchWindow().getShell().isVisible())
				getViewSite().getWorkbenchWindow().run(true, false, op);
			else
				op.run(new NullProgressMonitor());
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
		} finally {
			refreshLock = false;
		}
		return bag[0];
	}

	private Object[] getRootFeatures(ArrayList list) {
		ArrayList children = new ArrayList();
		ArrayList result = new ArrayList();
		try {
			for (int i = 0; i < list.size(); i++) {
				ConfiguredFeatureAdapter cf =
					(ConfiguredFeatureAdapter) list.get(i);
				IFeature feature = cf.getFeature(null);
				if (feature != null)
					addChildFeatures(
						feature,
						cf.getConfiguredSite(),
						children,
						cf.isConfigured());
			}
			for (int i = 0; i < list.size(); i++) {
				ConfiguredFeatureAdapter cf =
					(ConfiguredFeatureAdapter) list.get(i);
				IFeature feature = cf.getFeature(null);
				if (feature != null
					&& isChildFeature(feature, children) == false)
					result.add(cf);
			}
		} catch (CoreException e) {
			return list.toArray();
		}
		return result.toArray();
	}

	private void addChildFeatures(
		IFeature feature,
		IConfiguredSite csite,
		ArrayList children,
		boolean configured) {
		try {
			IIncludedFeatureReference[] included =
				feature.getIncludedFeatures();
			for (int i = 0; i < included.length; i++) {
				IFeature childFeature;
				try {
					childFeature =
						included[i].getFeature(!configured, csite, null);
				} catch (CoreException e) {
					childFeature = new MissingFeature(included[i]);
				}
				children.add(childFeature);
			}
		} catch (CoreException e) {
			UpdateUI.logException(e);
		}
	}

	private boolean isChildFeature(IFeature feature, ArrayList children) {
		for (int i = 0; i < children.size(); i++) {
			IFeature child = (IFeature) children.get(i);
			if (feature
				.getVersionedIdentifier()
				.equals(child.getVersionedIdentifier()))
				return true;
		}
		return false;
	}

	protected void handleDoubleClick(DoubleClickEvent e) {
		if (e.getSelection() instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) e.getSelection();
			Object obj = ssel.getFirstElement();
			if (obj!=null)
				propertiesAction.run();
		}
	}

	public void createPartControl(Composite parent) {
		splitter = new SashForm(parent, SWT.HORIZONTAL);
		splitter.setLayoutData(new GridData(GridData.FILL_BOTH));
		Composite leftContainer = createLineContainer(splitter);
		Composite rightContainer = createLineContainer(splitter);
		createTreeViewer(leftContainer);
		makeActions();
		createVerticalLine(leftContainer);
		createVerticalLine(rightContainer);
		preview = new ConfigurationPreview(this);
		preview.createControl(rightContainer);
		preview.getScrollingControl().setLayoutData(
			new GridData(GridData.FILL_BOTH));
		splitter.setWeights(new int[] { 2, 3 });
		fillActionBars(getViewSite().getActionBars());

		treeViewer.expandToLevel(2);
	}

	private void createTreeViewer(Composite parent) {
		treeViewer =
			new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.setUseHashlookup(true);
		initProviders();

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				manager.add(new GroupMarker("additions")); //$NON-NLS-1$
				fillContextMenu(manager);
			}
		});

		treeViewer.getControl().setMenu(
			menuMgr.createContextMenu(treeViewer.getControl()));
		getSite().registerContextMenu(menuMgr, treeViewer);

		treeViewer
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handleSelectionChanged(event);
			}
		});

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick(event);
			}
		});

		getSite().setSelectionProvider(treeViewer);

	}

	public TreeViewer getTreeViewer() {
		return treeViewer;
	}

	private Composite createLineContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		container.setLayout(layout);
		return container;
	}

	private void createVerticalLine(Composite parent) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_FILL);
		gd.widthHint = 1;
		line.setLayoutData(gd);
	}

	public Control getControl() {
		return splitter;
	}

	private int getStatusCode(IFeature feature, IStatus status) {
		int code = status.getCode();
		if (code == IFeature.STATUS_UNHAPPY) {
			if (status.isMultiStatus()) {
				IStatus[] children = status.getChildren();
				for (int i = 0; i < children.length; i++) {
					IStatus child = children[i];
					if (child.isMultiStatus()
						|| child.getCode() != IFeature.STATUS_DISABLED)
						return code;
				}
				// If we are here, global status is unhappy
				// because one or more included features
				// is disabled.
				if (UpdateUtils.hasObsoletePatches(feature)) {
					// The disabled included features
					// are old patches that are now
					// subsumed by better versions of
					// the features they were designed to
					// patch.
					return IFeature.STATUS_HAPPY;
				}
			}
		}
		return code;
	}

	protected void handleSelectionChanged(IStructuredSelection ssel) {
		Object obj = ssel.getFirstElement();
		if (obj instanceof IFeatureAdapter) {
			try {
				ConfiguredFeatureAdapter adapter =
					(ConfiguredFeatureAdapter) obj;
				IFeature feature = adapter.getFeature(null);
				boolean enable =
					(adapter.isOptional() || !adapter.isIncluded());
				boolean missing = feature instanceof MissingFeature;

				featureStateAction.setFeature(adapter);
				featureStateAction.setEnabled(enable && !missing);

				if (enable && !missing && adapter.isConfigured()) {
					IFeature[] features = UpdateUtils.getInstalledFeatures(feature, false);
					swapVersionAction.setEnabled(features.length > 1);
					if (features.length > 1) {
						swapVersionAction.setCurrentFeature(feature);
						swapVersionAction.setFeatures(features);
					}
					findUpdatesAction.setEnabled(true);
					findUpdatesAction.setFeature(feature);
				} else {
					swapVersionAction.setEnabled(false);
					findUpdatesAction.setEnabled(false);
				}

				if (missing) {
					MissingFeature mf = (MissingFeature) feature;
					installOptFeatureAction.setEnabled(
						mf.isOptional() && mf.getOriginatingSiteURL() != null);
					installOptFeatureAction.setFeature(mf);
					uninstallFeatureAction.setEnabled(false);
				} else {
					installOptFeatureAction.setEnabled(false);
					uninstallFeatureAction.setFeature(adapter);
					uninstallFeatureAction.setEnabled(
						enable && uninstallFeatureAction.canUninstall());
				}
			} catch (CoreException ex) {
				UpdateUI.logException(ex);
			}
		}
		if (obj instanceof ILocalSite) {
			propertiesAction.setEnabled(true);
			findUpdatesAction.setEnabled(true);
			findUpdatesAction.setFeature(null);
			detectedChangesAction.setEnabled(UpdateUtils.getSessionDeltas().length > 0);
			ILocalSite site = getLocalSite();
			revertAction.setEnabled(site != null && site.getConfigurationHistory().length > 1);
		} else if (obj instanceof IConfiguredSiteAdapter) {
			siteStateAction.setSite(
				((IConfiguredSiteAdapter) obj).getConfiguredSite());
			siteStateAction.setEnabled(true);
		}
		preview.setSelection(ssel);
	}

	protected void handleSelectionChanged(SelectionChangedEvent e) {
		handleSelectionChanged(((IStructuredSelection) e.getSelection()));
	}

	private void makePreviewTasks() {
		previewTasks = new Hashtable();
		Class key;
		ArrayList array = new ArrayList();
		// local site tasks
		key = ILocalSite.class;
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.revertPreviousLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.revertPreviousDesc"), //$NON-NLS-1$
				revertAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.updateLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.updateDesc"), //$NON-NLS-1$
				findUpdatesAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.detectedLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.detectedDesc"), //$NON-NLS-1$
				detectedChangesAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.linkLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.linkDesc"), //$NON-NLS-1$
				newExtensionLocationAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.installHistLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.installHistDesc"), //$NON-NLS-1$
				installationHistoryAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.activitiesLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.activitiesDesc"), //$NON-NLS-1$
				propertiesAction));

		previewTasks.put(key, array.toArray(new IPreviewTask[array.size()]));

		// configured site tasks
		array.clear();
		key = IConfiguredSiteAdapter.class;
		array.add(
			new PreviewTask(
				null,
				UpdateUI.getString("ConfigurationView.enableLocDesc"), //$NON-NLS-1$
				siteStateAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.extLocLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.extLocDesc"), //$NON-NLS-1$
				newExtensionLocationAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.propertiesLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.installPropDesc"), //$NON-NLS-1$
				propertiesAction));
		previewTasks.put(key, array.toArray(new IPreviewTask[array.size()]));

		// feature adapter tasks
		array.clear();
		key = IFeatureAdapter.class;
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.replaceVersionLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.replaceVersionDesc"), //$NON-NLS-1$
				swapVersionAction));
		array.add(
			new PreviewTask(
				null,
				UpdateUI.getString("ConfigurationView.enableFeatureDesc"), //$NON-NLS-1$
				featureStateAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.installOptionalLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.installOptionalDesc"), //$NON-NLS-1$
				installOptFeatureAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.uninstallLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.uninstallDesc"), //$NON-NLS-1$
				uninstallFeatureAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.scanLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.scanDesc"), //$NON-NLS-1$
				findUpdatesAction));
		array.add(
			new PreviewTask(
				UpdateUI.getString("ConfigurationView.featurePropLabel"), //$NON-NLS-1$
				UpdateUI.getString("ConfigurationView.featurePropDesc"), //$NON-NLS-1$
				propertiesAction));
		previewTasks.put(key, array.toArray(new IPreviewTask[array.size()]));
	}

	public IPreviewTask[] getPreviewTasks(Object object) {
		IPreviewTask[] tasks = null;

		if (object instanceof IFeatureAdapter)
			tasks = (IPreviewTask[]) previewTasks.get(IFeatureAdapter.class);
		if (object instanceof ILocalSite)
			tasks = (IPreviewTask[]) previewTasks.get(ILocalSite.class);
		if (object instanceof IConfiguredSiteAdapter)
			tasks =
				(IPreviewTask[]) previewTasks.get(IConfiguredSiteAdapter.class);
		return (tasks != null) ? tasks : new IPreviewTask[0];
	}

	void updateTitle(Object newInput) {
		if (newInput == null
			|| newInput.equals(UpdateUI.getDefault().getUpdateModel())) {
			// restore old
			setTitle(getViewName());
			setTitleToolTip(getTitle());
		} else {
			String name =
				((LabelProvider) treeViewer.getLabelProvider()).getText(
					newInput);
			setTitle(getViewName() + ": " + name); //$NON-NLS-1$
			setTitleToolTip(getTitle());
		}
	}
	public String getViewName() {
		return viewName;
	}

	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	public void setFocus() {
	}

}
