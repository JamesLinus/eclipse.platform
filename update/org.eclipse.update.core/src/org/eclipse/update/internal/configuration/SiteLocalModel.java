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
package org.eclipse.update.internal.configuration;
import java.net.*;
import java.util.*;

import org.eclipse.update.core.model.*;

/**
 * This class manages the configurations.
 */

public class SiteLocalModel extends ModelObject {


	public static final String SITE_LOCAL_PREFIX = "v2LocalSite"; //$NON-NLS-1$
	public static final String DEFAULT_CONFIG_PREFIX = "v2Config"; //$NON-NLS-1$
	public static final String DEFAULT_PRESERVED_CONFIG_PREFIX = "v2PreservedConfig"; //$NON-NLS-1$

	public static final String SITE_LOCAL_FILE = SITE_LOCAL_PREFIX +".xml"; //$NON-NLS-1$
	public static final String DEFAULT_CONFIG_FILE = DEFAULT_CONFIG_PREFIX+".xml"; //$NON-NLS-1$
	public static final String DEFAULT_PRESERVED_CONFIG_FILE = DEFAULT_PRESERVED_CONFIG_PREFIX+".xml"; //$NON-NLS-1$
	public static int DEFAULT_HISTORY = Integer.MAX_VALUE;	


	private long stamp;
	private String label;
	private URL location;
	private String locationURLString;
	private int history = DEFAULT_HISTORY;
	private List /* of InstallConfigurationModel */configurations;
	private List /* of InstallConfigurationModel */preservedConfigurations;
	private InstallConfigurationModel currentConfiguration;

	/**
	 * Constructor for LocalSite
	 */
	public SiteLocalModel(){
		super();
	}

	/**
	 * @since 2.0
	 */
	public InstallConfigurationModel getCurrentConfigurationModel() {
		return currentConfiguration;
	}

	/**
	 * @since 2.0
	 */
	public InstallConfigurationModel[] getConfigurationHistoryModel() {
		if (configurations==null) return new InstallConfigurationModel[0];
		return (InstallConfigurationModel[])configurations.toArray(arrayTypeFor(configurations));
	}

	/**
	 * adds a new configuration to the LocalSite
	 *  the newly added configuration is teh current one
	 */
	public void addConfigurationModel(InstallConfigurationModel config) {
		if (config != null) {
			if (configurations == null)
				configurations = new ArrayList();
			if (!configurations.contains(config))
				configurations.add(config);
		}
	}

	/**
	 * adds a new configuration to the LocalSite
	 *  the newly added configuration is teh current one
	 */
	public boolean removeConfigurationModel(InstallConfigurationModel config) {
		if (config != null) {
			return configurations.remove(config);
		}
		return false;
	}
	/**
	 * Gets the location of the local site.
	 * @return Returns a URL
	 */
	public URL getLocationURL() {
		return location;
	}

	/**
	 * Gets the locationURLString.
	 * @return Returns a String
	 */
	public String getLocationURLString() {
		return locationURLString;
	}


	/**
	 * Sets the locationURLString.
	 * @param locationURLString The locationURLString to set
	 */
	public void setLocationURLString(String locationURLString) {
		assertIsWriteable();
		this.locationURLString = locationURLString;
		this.location=null;
	}


	/**
	 * @since 2.0
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the label.
	 * @param label The label to set
	 */
	public void setLabel(String label) {
		assertIsWriteable();
		this.label = label;
	}

	
	/**
	 * @since 2.0
	 */
	public int getMaximumHistoryCount() {
		return history;
	}

	/**
	 * @since 2.0
	 */
	public void setMaximumHistoryCount(int history) {
		assertIsWriteable();
		this.history = history;
	}

	
	/**
	 * Adds a preserved configuration into teh collection
	 * do not save the configuration
	 * @since 2.0
	 */
	public void addPreservedInstallConfigurationModel(InstallConfigurationModel configuration) {
		if (preservedConfigurations == null)
			preservedConfigurations = new ArrayList();

		preservedConfigurations.add(configuration);
	}

	/**
	 * @since 2.0
	 */
	public boolean removePreservedConfigurationModel(InstallConfigurationModel configuration) {
		if (preservedConfigurations != null) {
			return preservedConfigurations.remove(configuration);
		}
		return false;
	}

	/**
	 * @since 2.0
	 */
	public InstallConfigurationModel[] getPreservedConfigurationsModel() {
		if (preservedConfigurations==null || preservedConfigurations.isEmpty())
			return new InstallConfigurationModel[0];
		return (InstallConfigurationModel[])preservedConfigurations.toArray(arrayTypeFor(preservedConfigurations));
	}


	/**
	 * Sets the currentConfiguration.
	 * @param currentConfiguration The currentConfiguration to set
	 */
	public void setCurrentConfigurationModel(InstallConfigurationModel currentConfiguration) {
		assertIsWriteable();
		this.currentConfiguration = currentConfiguration;
		
		//2.0.2 set the configuredSite of sites
		ConfiguredSite[] confSites = currentConfiguration.getConfigurationSitesModel();
		for (int i = 0; i < confSites.length; i++) {
			confSites[i].getSiteModel().setConfiguredSiteModel(confSites[i]);
		}
	}

	/*
	 * @see ModelObject#resolve(URL)
	 */
	public void resolve(URL base,URL bundleURL) throws MalformedURLException {
		// local
		location = resolveURL(base,bundleURL,getLocationURLString());
		
		// delegate
		resolveListReference(getConfigurationHistoryModel(),base,bundleURL);
		resolveListReference(getPreservedConfigurationsModel(),base,bundleURL);
		resolveReference(getCurrentConfigurationModel(),base,bundleURL);
	}
	

	/**
	 * Gets the stamp.
	 * @return Returns a long
	 */
	public long getStamp() {
		return stamp;
	}

	/**
	 * Sets the stamp.
	 * @param stamp The stamp to set
	 */
	public void setStamp(long stamp) {
		this.stamp = stamp;
	}

	/**
	 * @see org.eclipse.update.core.model.ModelObject#getPropertyName()
	 */
	protected String getPropertyName() {
		return SiteLocalModel.SITE_LOCAL_FILE;
	}

}
