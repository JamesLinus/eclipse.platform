package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * Local .jar file content reference
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.ContentReference
 * @see org.eclipse.update.core.JarEntryContentReference
 * @since 2.0
 */

public class JarContentReference extends ContentReference {

	private JarFile jarFile;

	/**
	 * Content selector used in .jar operations.
	 * Default implementation causes all file entries to be selected with
	 * generated identifiers being the same as the original .jar entry name.
	 * 
	 * @since 2.0
	 */
	public static class ContentSelector {

		/**
		 * Indicates whether the .jar entry should be selected.
		 * Default behavior is to select all non-directory entries.
		 * 
		 * @param entry .jar entry
		 * @return <code>true</code> if entry is to be selected, 
		 * <code>false</code> otherwise
		 * @since 2.0
		 */
		public boolean include(JarEntry entry) {
			return entry == null ? false : !entry.isDirectory();
		}

		/**
		 * Defines the "symbolic" path identifier for the 
		 * entry. Default identifier is the same as the jar entry name.
		 * 
		 * @param entry .jar entry
		 * @return "symbolic" path identifier
		 * @since 2.0
		 */
		public String defineIdentifier(JarEntry entry) {
			return entry == null ? null : entry.getName();
		}
	}

	/**
	 * Create jar content reference from URL.
	 * 
	 * @param id "symbolic" path identifier
	 * @param url actual referenced URL
	 * @since 2.0
	 */
	public JarContentReference(String id, URL url) {
		super(id, url);
		this.jarFile = null;
	}

	/**
	 * Create jar content reference from file.
	 * 
	 * @param id "symbolic" path identifier
	 * @param file actual referenced file
	 * @since 2.0
	 */
	public JarContentReference(String id, File file) {
		super(id, file);
		this.jarFile = null;
	}
	
	/**
	 * A factory method to create a jar content reference.
	 * 
	 * @param id "symbolic" path identifier
	 * @param file actual referenced file
	 * @return jar content reference
	 * @since 2.0
	 */
	public ContentReference createContentReference(String id, File file) {
		return new JarContentReference(id, file);
	}	
		
	/**
	 * Returns the content reference as a jar file. Note, that this method
	 * <b>does not</b> cause the file to be downloaded if it
	 * is not already local.
	 * 
	 * @return reference as jar file
	 * @exception IOException reference cannot be returned as jar file
	 * @since 2.0
	 */
	protected JarFile asJarFile() throws IOException {
		if (this.jarFile == null){
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_INSTALL)
				UpdateManagerPlugin.getPlugin().debug("asJarFile :"+asFile());			
			this.jarFile = new JarFile(asFile());
		}
		return jarFile;
	}

	/**
	 * Unpacks the referenced jar archive into the specified location.
	 * Returns content references to the unpacked files.
	 * 
	 * @param dir location to unpack the jar into
	 * @param selector selector, used to select entries to unpack, and to define
	 * "symbolic" path identifiers for the entries.
	 * @param monitor progress monitor 
	 * @exception IOException
	 * @since 2.0
	 */
	public ContentReference[] unpack(
		File dir,
		ContentSelector selector,
		InstallMonitor monitor)
		throws IOException {

		// make sure we have a selector
		if (selector == null)
			selector = new ContentSelector();

		// get archive content
		JarFile jarArchive = this.asJarFile();
		List content = new ArrayList();
		Enumeration entries = jarArchive.entries();

		// run through the entries and unjar
		String entryId;
		JarEntry entry;
		InputStream is;
		OutputStream os;
		File localFile;
		try {
			if (monitor != null) {
				monitor.saveState();
				monitor.setTaskName(Policy.bind("JarContentReference.Unpacking"));
				//$NON-NLS-1$
				monitor.subTask(this.getIdentifier());
				monitor.showCopyDetails(false);
			}
			while (entries.hasMoreElements()) {
				entry = (JarEntry) entries.nextElement();
				if (entry != null && selector.include(entry)) {
					is = null;
					os = null;
					entryId = selector.defineIdentifier(entry);
					localFile = Utilities.createLocalFile(dir, null /*key*/
					, entryId); // create temp file w/o a key map
					if (!entry.isDirectory()) {
						try {
							is = jarArchive.getInputStream(entry);
							os = new FileOutputStream(localFile);
							Utilities.copy(is, os, monitor);
						} finally {
							if (is != null)
								try {
									is.close();
								} catch (IOException e) {
								}
							if (os != null)
								try {
									os.close();
								} catch (IOException e) {
								}
						}
						content.add(new ContentReference(entryId, localFile));
					}
				}
			}
		} finally {
			if (monitor != null)
				monitor.restoreState();
		}
		return (ContentReference[]) content.toArray(new ContentReference[0]);
	}

	/**
	 * Unpacks the named jar entry into the specified location.
	 * Returns content reference to the unpacked file.
	 * 
	 * @param dir location to unpack the jar into
	 * @param entryName name of the jar entry
	 * @param selector selector, used to define "symbolic" path identifier
	 * for the entry
	 * @param monitor progress monitor 
	 * @exception IOException
	 * @since 2.0
	 */
	public ContentReference unpack(
		File dir,
		String entryName,
		ContentSelector selector,
		InstallMonitor monitor)
		throws IOException {

		// make sure we have a selector
		if (selector == null)
			selector = new ContentSelector();

		// unjar the entry
		JarFile jarArchive = this.asJarFile();
		entryName = entryName.replace(File.separatorChar, '/');
		JarEntry entry = jarArchive.getJarEntry(entryName);
		String entryId;
		if (entry != null) {
			InputStream is = null;
			OutputStream os = null;
			entryId = selector.defineIdentifier(entry);
			File localFile = Utilities.createLocalFile(dir, null /*key*/
			, entryId); // create temp file w/o a key map
			if (!entry.isDirectory()) {
				try {
					is = jarArchive.getInputStream(entry);
					os = new FileOutputStream(localFile);
					Utilities.copy(is, os, monitor);
				} finally {
					if (is != null)
						try {
							is.close();
						} catch (IOException e) {
						}
					if (os != null)
						try {
							os.close();
						} catch (IOException e) {
						}
				}
				return new ContentReference(entryId, localFile);
			} else
				return null; // entry was a directory
		} else
			throw new FileNotFoundException(
				this.asFile().getAbsolutePath() + " " + entryName);
		//$NON-NLS-1$
	}

	/**
	 * Peeks into the referenced jar archive.
	 * Returns content references to the jar entries within the jar file.
	 * 
	 * @param selector selector, used to select entries to return, and to define
	 * "symbolic" path identifiers for the entries.
	 * @param monitor progress monitor 
	 * @exception IOException
	 * @since 2.0
	 */
	public ContentReference[] peek(
		ContentSelector selector,
		InstallMonitor monitor)
		throws IOException {

		// make sure we have a selector
		if (selector == null)
			selector = new ContentSelector();

		// get archive content
		JarFile jarArchive = this.asJarFile();
		List content = new ArrayList();
		Enumeration entries = jarArchive.entries();

		// run through the entries and create content references
		JarEntry entry;
		String entryId;
		while (entries.hasMoreElements()) {
			entry = (JarEntry) entries.nextElement();
			if (selector.include(entry)) {
				entryId = selector.defineIdentifier(entry);
				content.add(new JarEntryContentReference(entryId, this, entry));
			}
		}
		return (ContentReference[]) content.toArray(new ContentReference[0]);
	}
	/**
	 * Peeks into the referenced jar archive looking for the named entry.
	 * Returns content reference to the jar entry within the jar file.
	 * 
	 * @param entryName name of the jar entry
	 * @param selector selector, used to define "symbolic" path identifier
	 * for the entry
	 * @param monitor progress monitor 
	 * @exception IOException
	 * @since 2.0
	 */
	public ContentReference peek(
		String entryName,
		ContentSelector selector,
		InstallMonitor monitor)
		throws IOException {

		// make sure we have a selector
		if (selector == null)
			selector = new ContentSelector();

		// assume we have a reference that represents a jar archive.
		JarFile jarArchive = this.asJarFile();
		entryName = entryName.replace(File.separatorChar, '/');
		JarEntry entry = jarArchive.getJarEntry(entryName);
		String entryId = selector.defineIdentifier(entry);
		return new JarEntryContentReference(entryId, this, entry);
	}

	
}