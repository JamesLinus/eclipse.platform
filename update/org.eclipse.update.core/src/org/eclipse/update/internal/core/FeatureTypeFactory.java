package org.eclipse.update.internal.core;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.IPluginRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.*;
import org.eclipse.update.core.IFeatureFactory;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * 
 */
public final class FeatureTypeFactory {
	// VK: FeatureFactoryManager ???
	// VK: change to all statics ... is singleton

	private static FeatureTypeFactory inst;
	
	private Map factories;

	/**
	 * hide ctr 
	 */
	private FeatureTypeFactory() {
	}

	public static FeatureTypeFactory getInstance() {
		if (inst == null)
			inst = new FeatureTypeFactory();
		return inst;
	}


	/**
	 * return the factory for the type
	 */
	public IFeatureFactory getFactory(String type) throws CoreException {
			//
			Object instance = getFactories().get(type);
			if (instance==null) {
				instance = createFactoryFor(type);
				getFactories().put(type,instance);
			}
			return (IFeatureFactory) instance;
	}

	/**
	 * 
	 */
	private IFeatureFactory createFactoryFor(String type) throws CoreException {
		IFeatureFactory result = null;
		
		String pluginID = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
		IPluginRegistry pluginRegistry = Platform.getPluginRegistry();
		IConfigurationElement[] elements = pluginRegistry.getConfigurationElementsFor(pluginID,IFeatureFactory.SIMPLE_EXTENSION_ID,type);
		if (elements==null || elements.length==0){
			IStatus status = new Status(IStatus.ERROR,pluginID,IStatus.OK,"Cannot find feature factory for id: " +type,null);
			throw new CoreException(status);
		} else {
			IConfigurationElement element = elements[0];
			result = (IFeatureFactory)element.createExecutableExtension("class");
		}
		return result;
	}

	/**
	 * Gets the actories.
	 * @return Returns a Map
	 */
	private Map getFactories() {
		if (factories==null) factories = new HashMap();
			return factories;
	}

	/**
	 * Sets the actories.
	 * @param actories The actories to set
	 */
	private void setFactories(Map factories) {
		factories = factories;
	}

}