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
package org.eclipse.ant.ui.internal.launchConfigurations;


import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.ui.internal.model.AntUIPlugin;
import org.eclipse.ant.ui.internal.model.AntUtil;
import org.eclipse.ant.ui.internal.model.IAntUIConstants;
import org.eclipse.ant.ui.internal.model.IAntUIPreferenceConstants;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.debug.ui.variables.ExpandVariableContext;
import org.eclipse.debug.ui.variables.IVariableConstants;
import org.eclipse.debug.ui.variables.VariableUtil;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

/**
 *
 */
public class AntLaunchShortcut implements ILaunchShortcut {

	private boolean fShowDialog= false;

	/**
	 * Constructor for AntLaunchShortcut.
	 */
	public AntLaunchShortcut() {
		super();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection)selection;
			Object object = structuredSelection.getFirstElement();
			if (object instanceof IAdaptable) {
				IResource resource = (IResource)((IAdaptable)object).getAdapter(IResource.class);
				if (resource != null) {
					launch(resource, mode);
					return;
				}
			}
		}
		antFileNotFound();
	}
	
	/**
	 * Inform the user that an ant file was not found to run.
	 */
	private void antFileNotFound() {
		reportError(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Unable"), null); //$NON-NLS-1$	
	}
	
	/**
	 * Launch the given file in the specified mode.
	 * 
	 * @param resource either a build file (*.xml file) to execute or a resource
	 * from whose location a build file should be searched for. If the given
	 * resource is a file that does not end in ".xml", a search will begin at
	 * the resource's enclosing folder. The given resource must be of type IFile
	 * or IContainer.
	 * @param mode the mode in which the build file should be executed
	 */
	protected void launch(IResource resource, String mode) {
		if (!("xml".equalsIgnoreCase(resource.getFileExtension()))) { //$NON-NLS-1$
			if (resource.getType() == IResource.FILE) {
				resource= resource.getParent();
			}
			resource= findBuildFile((IContainer)resource);
		} 
		if (resource != null) {
			launch((IFile)resource, mode, null);
		} else {
		 	antFileNotFound();
		 }
	}
	
	/**
	 * Launch the given targets in the given build file. The targets are
	 * launched in the given mode.
	 * 
	 * @param file the build file to launch
	 * @param mode the mode in which the build file should be executed
	 * @param targetAttribute the targets to launch, in the form of the launch
	 * configuration targets attribute.
	 */
	public void launch(IFile file, String mode, String targetAttribute) {
		ILaunchConfiguration configuration= null;
		if (verifyMode(mode)) {
			List configurations = findExistingLaunchConfigurations(file);
			if (configurations.isEmpty()) {
				configuration = createDefaultLaunchConfiguration(file);
			} else {
				if (configurations.size() == 1) {
					configuration= (ILaunchConfiguration)configurations.get(0);
				} else {
					configuration= chooseConfig(configurations);
					if (configuration == null) {
						// User cancelled selection
						return;
					}
				}
			}
		}

		if (configuration != null) {
			if (fShowDialog) {
				IStatus status = new Status(IStatus.INFO, IAntUIConstants.PLUGIN_ID, IAntUIConstants.STATUS_INIT_RUN_ANT, "", null); //$NON-NLS-1$
				DebugUITools.openLaunchConfigurationDialog(AntUIPlugin.getActiveWorkbenchWindow().getShell(), configuration, IExternalToolConstants.ID_EXTERNAL_TOOLS_LAUNCH_GROUP, status);
			} else {
				if (targetAttribute != null) {
					String newName= DebugPlugin.getDefault().getLaunchManager().generateUniqueLaunchConfigurationNameFrom(configuration.getName());
					try {
						configuration= configuration.copy(newName);
						((ILaunchConfigurationWorkingCopy) configuration).setAttribute(IAntUIConstants.ATTR_ANT_TARGETS, targetAttribute);
					} catch (CoreException exception) {
						reportError(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Exception_launching"), new String[] {file.getName()}), exception); //$NON-NLS-1$
						return;
					}
				}
				DebugUITools.launch(configuration, mode);
			}
			return;
		}
		
		antFileNotFound();
	}
	
	/**
	 * Walks the file hierarchy looking for a build file.
	 * Returns the first build file found that matches the 
	 * search criteria.
	 */
	private IFile findBuildFile(IContainer parent) {
		String[] names= getBuildFileNames();
		if (names == null) {
			return null;
		}
		IResource file= null;
		while (file == null || file.getType() != IFile.FILE) {		
			for (int i = 0; i < names.length; i++) {
				String string = names[i];
				file= parent.findMember(string);
				if (file != null && file.getType() == IFile.FILE) {
					break;
				}
			}
			parent = parent.getParent();
			if (parent == null) {
				return null;
			}
		}
		return (IFile)file;
	}
	
	private String[] getBuildFileNames() {
		IPreferenceStore prefs= AntUIPlugin.getDefault().getPreferenceStore();
		String buildFileNames= prefs.getString(IAntUIPreferenceConstants.ANT_FIND_BUILD_FILE_NAMES);
		if (buildFileNames.length() == 0) {
			//the user has not specified any names to look for
			return null;
		}
		return AntUtil.parseString(buildFileNames, ","); //$NON-NLS-1$
	}
	
	/**
	 * Creates and returns a default launch configuration for the given file.
	 * 
	 * @param file
	 * @return default launch configuration
	 */
	public static ILaunchConfiguration createDefaultLaunchConfiguration(IFile file) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IAntUIConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		StringBuffer buffer = new StringBuffer(file.getProject().getName());
		buffer.append(' ');
		buffer.append(file.getName());
		String name = buffer.toString().trim();
		name= manager.generateUniqueLaunchConfigurationNameFrom(name);
		try {
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
			StringBuffer buf = new StringBuffer();
			VariableUtil.buildVariableTag(IVariableConstants.VAR_WORKSPACE_LOC, file.getFullPath().toString(), buf);
			workingCopy.setAttribute(IExternalToolConstants.ATTR_LOCATION, buf.toString());
			
			// set default for common settings
			CommonTab tab = new CommonTab();
			tab.setDefaults(workingCopy);
			tab.dispose();
			
			return workingCopy.doSave();
		} catch (CoreException e) {
			reportError(MessageFormat.format(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.An_exception_occurred_while_creating_a_default_Ant_launch_configuration_for_{0}_2"), new String[]{file.toString()}), e); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * Returns a list of existing launch configuration for the given file.
	 * 
	 * @param file
	 * @return list of launch configurations
	 */
	public static List findExistingLaunchConfigurations(IFile file) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IAntUIConstants.ID_ANT_LAUNCH_CONFIGURATION_TYPE);
		List validConfigs= new ArrayList();
		if (type != null) {
				ILaunchConfiguration[] configs = null;
				try {
					configs = manager.getLaunchConfigurations(type);
				} catch (CoreException e) {
					reportError(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.An_exception_occurred_while_retrieving_Ant_launch_configurations._3"), e); //$NON-NLS-1$
				}
				if (configs != null && configs.length > 0) {
					IPath filePath = file.getLocation();
					ExpandVariableContext context = ExternalToolsUtil.getVariableContext();
					for (int i = 0; i < configs.length; i++) {
						ILaunchConfiguration configuration = configs[i];
						IPath location;
						try {
							location = ExternalToolsUtil.getLocation(configuration, context);
							if (filePath.equals(location)) {
								validConfigs.add(configuration);
							}
						} catch (CoreException e) {
							// error occurred in variable expand - ignore
						}
					}
				}
		}
		return validConfigs;
	}
	
	/**
	 * Prompts the user to choose from the list of given launch configurations
	 * and returns the config the user choose of <code>null</code> if the user
	 * pressed Cancel or if the given list is empty.
	 */
	public static ILaunchConfiguration chooseConfig(List configs) {
		if (configs.isEmpty()) {
			return null;
		}
		ILabelProvider labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(Display.getDefault().getActiveShell(), labelProvider);
		dialog.setElements((ILaunchConfiguration[]) configs.toArray(new ILaunchConfiguration[configs.size()]));
		dialog.setTitle(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Ant_Configuration_Selection_4")); //$NON-NLS-1$
		dialog.setMessage(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Choose_an_ant_configuration_to_run_5")); //$NON-NLS-1$
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == ElementListSelectionDialog.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;
	}
	
	/**
	 * Verifies the mode is supported
	 * 
	 * @param mode
	 * @return boolean
	 */
	protected boolean verifyMode(String mode) {
		if (!mode.equals(ILaunchManager.RUN_MODE)) {
			reportError(AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Ant_builds_only_support___run___mode._6"), null); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
	 */
	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IFile file = (IFile)input.getAdapter(IFile.class);
		if (file != null) {
			launch(file, mode);
			return;
		}
		antFileNotFound();
	}
	
	protected static void reportError(String message, Throwable throwable) {
		IStatus status = null;
		if (throwable instanceof CoreException) {
			status = ((CoreException)throwable).getStatus();
		} else {
			status = new Status(IStatus.ERROR, IAntUIConstants.PLUGIN_ID, 0, message, throwable);
		}
		ErrorDialog.openError(AntUIPlugin.getActiveWorkbenchWindow().getShell(), AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Error_7"), AntLaunchConfigurationMessages.getString("AntLaunchShortcut.Build_Failed_2"), status); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Sets whether to show the external tools launch configuration dialog
	 * 
	 * @param showDialog If true the launch configuration dialog will always be
	 * 			shown
	 */
	public void setShowDialog(boolean showDialog) {
		fShowDialog = showDialog;
	}
}
