package org.eclipse.update.core.model;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */ 
 
 import java.net.URL;

/**
 * An object which represents the definition of a custom
 * install handler in the packaging manifest.
 * <p>
 * This class may be instantiated, or further subclassed.
 * </p>
 * @since 2.0
 */

public class InstallHandlerModel extends ModelObject {
	
	private String url;
	private String library;
	private String clazz;
	
	/**
	 * Creates a uninitialized model object.
	 * 
	 * @since 2.0
	 */
	public InstallHandlerModel() {
		super();
	}
	
	/**
	 * Returns the URL of the custom installation trigger page.
	 *
	 * @return url or <code>null</code>
	 * @since 2.0
	 */
	public String getURLString() {
		return url;
	}
	
	/**
	 * Returns the resolved URL for the install trigger page.
	 * 
	 * @return url, or <code>null</code>
	 * @since 2.0
	 */
	public URL getURL() {
		return null;
	}
	
	/**
	 * Returns the name of the custom installer jar, relative to the
	 * containing feature.
	 *
	 * @return jar relative path or <code>null</code>
	 * @since 2.0
	 */
	public String getLibrary() {
		return library;
	}
	
	/**
	 * Returns the name of the custom installer class. The class must
	 * implement org.eclipse.update.core.IInstallHandler.
	 *
	 * @see org.eclipse.update.core.IInstallHandler
	 * @return url or <code>null</code>
	 * @since 2.0
	 */
	public String getClassName() {
		return clazz;
	}
	
	/**
	 * Sets URL of the custom installation trigger page.
	 * This object must not be read-only.
	 *
	 * @param url Trigger page URL. May be <code>null</code>.
	 * @since 2.0
	 */	
	public void setURLString(String url) {
		assertIsWriteable();
		this.url = url;
	}
	
	/**
	 * Sets the relative path of the custom installer jar.
	 * This object must not be read-only.
	 *
	 * @param library jar path. May be <code>null</code>.
	 * @since 2.0
	 */	
	public void setLibrary(String library) {
		assertIsWriteable();
		this.library = library;
	}
	
	/**
	 * Sets the name of the custom installer class.
	 * This object must not be read-only.
	 *
	 * @see org.eclipse.update.core.IInstallHandler
	 * @param clazz name of class implementing IInstallHandler.
	 * 		May be <code>null</code>.
	 * @since 2.0
	 */	
	public void setClassName(String clazz) {
		assertIsWriteable();
		this.clazz = clazz;
	}
}
