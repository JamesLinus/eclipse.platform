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
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;

/**
 * 
 */
public class UpdateManagerUtils {

	private static boolean OS_UNIX = BootLoader.OS_HPUX.equals(BootLoader.getOS()) || BootLoader.OS_AIX.equals(BootLoader.getOS()) || BootLoader.OS_LINUX.equals(BootLoader.getOS()) || BootLoader.OS_SOLARIS.equals(BootLoader.getOS());

	private static Map table;

	static {
		table = new HashMap();
		table.put("compatible", new Integer(IImport.RULE_COMPATIBLE)); //$NON-NLS-1$
		table.put("perfect", new Integer(IImport.RULE_PERFECT)); //$NON-NLS-1$
		table.put("equivalent", new Integer(IImport.RULE_EQUIVALENT)); //$NON-NLS-1$
		table.put("greaterOrEqual", new Integer(IImport.RULE_GREATER_OR_EQUAL)); //$NON-NLS-1$
	}

	private static Writer writer;
	// manage URL to File
	private static Map urlFileMap;
	
	private static String os;
	private static String ws;
	private static String arch;
	private static String nl;

	
	/**
	 * return the urlString if it is a absolute URL
	 * otherwise, return the default URL if the urlString is null
	 * The defaultURL may point ot a file, create a file URL then
	 * if the urlString or the default URL are relatives, prepend the rootURL to it
	 */
	public static URL getURL(URL rootURL, String urlString, String defaultURL) throws MalformedURLException {
		URL url = null;

		// if no URL , provide Default
		if (urlString == null || urlString.trim().equals("")) { //$NON-NLS-1$

			// no URL, no default, return right now...
			if (defaultURL == null || defaultURL.trim().equals("")) //$NON-NLS-1$
				return null;
			else
				urlString = defaultURL;
		}

		// URL can be relative or absolute	
		if (urlString.startsWith("/") && urlString.length() > 1) //$NON-NLS-1$
			urlString = urlString.substring(1);
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			// the url is not an absolute URL
			// try relative
			url = new URL(rootURL, urlString);
		}

		return url;
	}

	/**
	 * return a relative String to rootURL 
	 * if url contains rootURL so
	 * new URL(rootURL, resultString) == url
	 * 
	 */
	public static String getURLAsString(URL rootURL, URL url) {
		String result = null;

		if (rootURL == null) {
			return (url == null) ? null : url.toString();
		}

		// if no URL , return null
		if (url != null) {

			result = url.toExternalForm();

			if (rootURL.getHost() != null && !rootURL.getHost().equals(url.getHost()))
				return result;

			if (rootURL.getProtocol() != null && !rootURL.getProtocol().equals(url.getProtocol()))
				return result;

			if (rootURL.getPort() != url.getPort())
				return result;

			String rootURLFileString = rootURL.getFile();
			rootURLFileString.replace(File.separatorChar, '/');
			if (!rootURLFileString.endsWith("/")) {
				int index = rootURLFileString.lastIndexOf('/');
				if (index != -1) {
					rootURLFileString = rootURLFileString.substring(0, index);
				}
			}
			String urlFileString = url.getFile();

			if (urlFileString.startsWith(rootURLFileString)) {
				result = urlFileString.substring(rootURLFileString.length());
				result.replace(File.separatorChar, '/');
			} else {
				// we need to check the following
				// file:/C:/ and file:C:/
				if ("file".equalsIgnoreCase(url.getProtocol())) {
					File rootFile = new File(rootURLFileString);
					File urlFile = new File(urlFileString);

					File relativePath = urlFile;
					while (relativePath != null && !rootFile.equals(relativePath.getParentFile())) {
						relativePath = relativePath.getParentFile();
					}

					if (relativePath == null) {
						UpdateCore.warn("Cannot calculate relative path");
						return url.toString();
					} else {
						String relativeRootString = relativePath.getParentFile().getAbsolutePath();
						String fullString = urlFile.getAbsolutePath();
						if (!fullString.startsWith(relativeRootString)) {
							UpdateCore.warn("Full path:" + fullString + " does not start with " + relativeRootString);
							return url.toString();
						} else {
							String returnString = fullString.substring(relativeRootString.length() + 1);
							if (urlFile.isDirectory())
								returnString += File.separator;
							// we lost the last slash when tranforming in File
							returnString = returnString.replace(File.separatorChar, '/');
							return returnString;
						}

					}

				} else {
					result = url.toString();
				}
			}
		}

		return result;
	}

	/**
	 * returns a translated String
	 */
	public static String getResourceString(String infoURL, ResourceBundle bundle) {
		String result = null;
		if (infoURL != null) {
			result = UpdateCore.getPlugin().getDescriptor().getResourceString(infoURL, bundle);
		}
		return result;
	}

	/**
	 * 
	 */
	public static URL copyToLocal(InputStream sourceContentReferenceStream, String localName, InstallMonitor monitor) throws MalformedURLException, IOException, InstallAbortedException {
		URL result = null;
		// create the Dir if they do not exist
		// get the path from the File to resolve File.separator..
		// do not use the String as it may contain URL like separator
		File localFile = new File(localName);
		int index = localFile.getPath().lastIndexOf(File.separator);
		if (index != -1) {
			File dir = new File(localFile.getPath().substring(0, index));
			if (!dir.exists())
				dir.mkdirs();
		}

		// transfer the content of the File
		if (!localFile.isDirectory()) {
			OutputStream localContentReferenceStream = new FileOutputStream(localFile);
			try {
				Utilities.copy(sourceContentReferenceStream, localContentReferenceStream, monitor);
			} finally {
				try {
					localContentReferenceStream.close();
				} catch (IOException e){}
			}
		}
		result = localFile.toURL();
		return result;
	}

	/*
	 * [20305] need to slam permissions for executable libs on some
	 * platforms. This is a temporary fix
	 */
	public static void checkPermissions(ContentReference ref, String filePath) {

		if (ref.getPermission() != 0) {
			UpdateCore.warn("Change permission for " + filePath + " to " + ref.getPermission());
			// FIXME: change the code to use JNI
		}

		if (filePath != null && OS_UNIX && ref.getPermission() != 0) {
			// add execute permission on shared libraries 20305
			// do not remove write permission 20896
			// chmod a+x *.sl
			try {
				Process pr = Runtime.getRuntime().exec(new String[] { "chmod", "a+x", filePath });
				Thread chmodOutput = new StreamConsumer(pr.getInputStream());
				chmodOutput.setName("chmod output reader");
				chmodOutput.start();
				Thread chmodError = new StreamConsumer(pr.getErrorStream());
				chmodError.setName("chmod error reader");
				chmodError.start();
			} catch (IOException ioe) {
			}

		}
	}

	/**
	 * Returns a random file name for the local system
	 * attempt to conserve the extension if there is a '.' in the path
	 * and no File.Seperator after the '.'
	 * 
	 * \a\b\c.txt -> c987659385.txt
	 * c.txt -> c3854763.txt
	 * c	-> c953867549
	 */
	public static String getLocalRandomIdentifier(String remotePath, Date date) {
		int dotIndex = remotePath.lastIndexOf(".");
		//$NON-NLS-1$
		int fileIndex = remotePath.lastIndexOf(File.separator);
		// if there is a separator after the dot
		// do not consider it as an extension
		String ext = (dotIndex != -1 && fileIndex < dotIndex) ? remotePath.substring(dotIndex) : "";
		//$NON-NLS-1$
		// the name is the string between the separator and the dot
		// if there is no separator, it is the string up to the dot		
		// if there is no dot, go to the end of the string 
		if (fileIndex == -1)
			fileIndex = 0;
		if (dotIndex == -1)
			dotIndex = remotePath.length();
		// if I have a separator and no dot: /a/b/c -> c
		// if my separator is the last /a/b/c/, fileIndex and dotIndex are the same, so it will return the default temp name
		String name = (fileIndex < dotIndex) ? remotePath.substring(fileIndex, dotIndex) : "Eclipse_Update_TMP_";
		//$NON-NLS-1$
		String result = name + date.getTime() + ext;
		return result;
	}

	/**
	 * remove a file or directory from the file system.
	 * used to clean up install
	 */
	public static void removeFromFileSystem(File file) {
		if (!file.exists())
			return;

		if (file.isDirectory()) {
			String[] files = file.list();
			if (files != null) // be careful since file.list() can return null
				for (int i = 0; i < files.length; ++i)
					removeFromFileSystem(new File(file, files[i]));
		}

		if (!file.delete()) {
			String msg = Policy.bind("UpdateManagerUtils.UnableToRemoveFile", file.getAbsolutePath());
			//$NON-NLS-1$ //$NON-NLS-2$
			UpdateCore.log(msg, new Exception());
		}
	}

	/**
	 * remove all the empty directories recursively
	 * used to clean up install
	 */
	public static void removeEmptyDirectoriesFromFileSystem(File file) {
		if (!file.isDirectory())
			return;

		String[] files = file.list();
		if (files != null) { // be careful since file.list() can return null
			for (int i = 0; i < files.length; ++i) {
				removeEmptyDirectoriesFromFileSystem(new File(file, files[i]));
			}
		}
		if (!file.delete()) {
			String msg = Policy.bind("UpdateManagerUtils.UnableToRemoveFile", file.getAbsolutePath());
			//$NON-NLS-1$ //$NON-NLS-2$
			UpdateCore.log(msg, new Exception());
		}
	}

	/**
	 * Returns the plugin entries that are in source array and
	 * not in target array
	 */
	public static IPluginEntry[] diff(IPluginEntry[] sourceArray, IPluginEntry[] targetArray) { // No pluginEntry to Install, return Nothing to instal
		if (sourceArray == null || sourceArray.length == 0) {
			return new IPluginEntry[0];
		} // No pluginEntry installed, Install them all
		if (targetArray == null || targetArray.length == 0) {
			return sourceArray;
		} // if a IPluginEntry from sourceArray is NOT in
		// targetArray, add it to the list
		List list1 = Arrays.asList(targetArray);
		List result = new ArrayList(0);
		for (int i = 0; i < sourceArray.length; i++) {
			if (!list1.contains(sourceArray[i]))
				result.add(sourceArray[i]);
		}

		IPluginEntry[] resultEntry = new IPluginEntry[result.size()];
		if (result.size() > 0)
			result.toArray(resultEntry);
		return resultEntry;
	}

	/**
	 * Returns the parent URL of the given URL, or <code>null</code> if the
	 * given URL is the root.
	 * <table>
	 * <caption>Example</caption>
	 * <tr>
	 *   <th>Given URL</th>
	 *   <th>Parent URL</th>
	 * <tr>
	 *   <td>"http://hostname/"</td>
	 *   <td>null</td>
	 * <tr>
	 *   <td>"http://hostname/folder/file</td>
	 *   <td>"http://hostname/folder/</td>
	 * </table>
	 *
	 * @param url a URL
	 * @return    the parent of the given URL
	 */
	public static URL getParent(URL url) {
		String file = url.getFile();
		int len = file.length();
		if (len == 0 || len == 1 && file.charAt(0) == '/')
			return null;
		int lastSlashIndex = -1;
		for (int i = len - 2; lastSlashIndex == -1 && i >= 0; --i) {
			if (file.charAt(i) == '/')
				lastSlashIndex = i;
		}
		if (lastSlashIndex == -1)
			file = "";
		else
			file = file.substring(0, lastSlashIndex + 1);
		try {
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
		} catch (MalformedURLException e) {
			Assert.isTrue(false, e.getMessage());
		}
		return url;
	}

	/**
	 * 
	 */
	public static URL asDirectoryURL(URL url) throws MalformedURLException {
		//url = URLEncoder.encode(url);
		String path = url.getFile();
		if (!path.endsWith("/")) {
			int index = path.lastIndexOf('/');
			if (index != -1)
				path = path.substring(0, index + 1);
			// ignore any ref in original URL
			url = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
		}
		return url;
	}

	/*
	 * Compares two URL for equality
	 * Return false if one of them is null
	 */
	public static boolean sameURL(URL url1, URL url2) {

		if (url1 == null || url2 == null)
			return false;
		if (url1.equals(url2))
			return true;

		// check if URL are file: URL as we may
		// have 2 URL pointing to the same featureReference
		// but with different representation
		// (i.e. file:/C;/ and file:C:/)
		if (!"file".equalsIgnoreCase(url1.getProtocol()))
			return false;
		if (!"file".equalsIgnoreCase(url2.getProtocol()))
			return false;

		File file1 = getFileFor(url1);//new File(url1.getFile());
		File file2 = getFileFor(url2);

		if (file1 == null)
			return false;

		return (file1.equals(file2));
	}
	
	/*
	 * Method getFileFor.
	 * @param url1
	 * @return File
	 */
	private static File getFileFor(URL url1) {
		if (urlFileMap == null) urlFileMap = new HashMap();
		if (urlFileMap.get(url1)!=null) return (File)urlFileMap.get(url1);
		File newFile = new File(url1.getFile());
		urlFileMap.put(url1,newFile);
		return newFile; 
	}

	/*
	 * returns the list of FeatureReference that are parent of 
	 * the Feature or an empty array if no parent found.
	 * @param onlyOptional if set to <code>true</code> only return parents that consider the feature optional
	 * @param child
	 * @param possiblesParent
	 */
	public static IFeatureReference[] getParentFeatures(IFeature childFeature, IFeatureReference[] possiblesParent, boolean onlyOptional) throws CoreException {

		if (childFeature == null)
			return new IFeatureReference[0];

		List parentList = new ArrayList();
		IIncludedFeatureReference[] children = null;
		IFeature compareFeature = null;
		for (int i = 0; i < possiblesParent.length; i++) {
			try {
				IFeature possibleParentFeature = possiblesParent[i].getFeature(null);
				if (possibleParentFeature != null) {
					children = possibleParentFeature.getIncludedFeatures();
					for (int j = 0; j < children.length; j++) {
						try {
							compareFeature = children[j].getFeature(true,null,null); // compare with the 'real' feature, not the best match
						} catch (CoreException e) {
							UpdateCore.warn("", e);
						}
						if (childFeature.equals(compareFeature)) {
							if (onlyOptional) {
								if (UpdateManagerUtils.isOptional(children[j])) {
									parentList.add(possiblesParent[i]);
								} else {
									UpdateCore.warn("Feature :" + children[j] + " not optional. Not included in parents list.");
								}
							} else {
								parentList.add(possiblesParent[i]);
							}
						}
					}
				}
			} catch (CoreException e) {
				UpdateCore.warn("", e);
			}
		}

		IFeatureReference[] parents = new IFeatureReference[0];
		if (parentList.size() > 0) {
			parents = new IFeatureReference[parentList.size()];
			parentList.toArray(parents);
		}
		return parents;
	}

	/*
	 * returns the list of Features that are parent of 
	 * the Feature or an empty array if no parent found
	 * @param onlyOptional if set to <code>true</code> only return parents that consider the feature optional
	 * @param child
	 * @param possiblesParent
	 */
	public static IFeatureReference[] getParentFeatures(IFeatureReference child, IFeatureReference[] possiblesParent, boolean onlyOptional) throws CoreException {

		if (child == null)
			return new IFeatureReference[0];

		IFeature childFeature = null;
		try {
			childFeature = child.getFeature(null);
		} catch (CoreException e) {
			UpdateCore.warn(null, e);
		}

		if (childFeature == null)
			return new IFeatureReference[0];

		return getParentFeatures(childFeature, possiblesParent, onlyOptional);
	}

	/*
	 * If the return code of the HTTP connection is not 200 (OK)
	 * thow an IO exception
	 * 
	 */
	public static void checkConnectionResult(Response response,URL url) throws IOException {
		// did the server return an error code ?
			int result = response.getStatusCode();

			if (result != IStatusCodes.HTTP_OK) {
				String serverMsg = response.getStatusMessage();
				throw new IOException(Policy.bind("ContentReference.HttpNok", new Object[] { new Integer(result), serverMsg, url })); //$NON-NLS-1$						
			}
	}

	public static class StreamConsumer extends Thread {
		InputStream is;
		byte[] buf;
		public StreamConsumer(InputStream inputStream) {
			super();
			this.setDaemon(true);
			this.is = inputStream;
			buf = new byte[512];
		}
		public void run() {
			try {
				int n = 0;
				while (n >= 0)
					n = is.read(buf);
			} catch (IOException ioe) {
			}
		}
	}

	/**
	 * Return the optional children to install
	 * The optional features to install may not all be direct children 
	 * of the feature.
	 * Also include non-optional features
	 * 
	 * @param children all the nested features
	 * @param optionalfeatures optional features to install
	 * @return IFeatureReference[]
	 */
	public static IFeature[] optionalChildrenToInstall(IFeature[] children, IFeature[] optionalfeatures) {

		List optionalChildrenToInstall = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IFeature optionalFeature = children[i];
			if (!UpdateManagerUtils.isOptional(optionalFeature)) {
				optionalChildrenToInstall.add(optionalFeature);
			} else {
				for (int j = 0; j < optionalfeatures.length; j++) {
					if (optionalFeature.equals(optionalfeatures[j])) {
						optionalChildrenToInstall.add(optionalFeature);
						break;
					}
				}
			}
		}

		IFeature[] result = new IFeature[optionalChildrenToInstall.size()];
		if (optionalChildrenToInstall.size() > 0) {
			optionalChildrenToInstall.toArray(result);
		}

		return result;
	}

	/**
	 * returns the mapping of matching rules
	 * the default returns perfect
	 * 
	 * @since 2.0.2
	 */
	public static int getMatchingRule(String rule) {
		if (rule == null)
			return IImport.RULE_PERFECT;
		int ruleInt = ((Integer) table.get(rule)).intValue();
		if (ruleInt == IImport.RULE_NONE)
			return IImport.RULE_PERFECT;
		return ruleInt;
	}
	
	/**
	 * returns the mapping of matching id rules
	 * the default returns perfect
	 * 
	 * @since 2.0.2
	 */
	public static int getMatchingIdRule(String rule) {
		if (rule!=null && rule.equalsIgnoreCase("prefix"))
			return IImport.RULE_PREFIX;
		return IImport.RULE_PERFECT;
	}
	
	/**
	 * Method isOptional.
	 * @param featureReference
	 * @return boolean
	 */
	public static boolean isOptional(IFeatureReference featureReference) {
		if (featureReference==null) return false;
		if (featureReference instanceof IIncludedFeatureReference){
			return ((IIncludedFeatureReference)featureReference).isOptional();
		}
		return false;
	}

	/**
	 * 
	 */
	public static boolean isValidEnvironment(IPlatformEnvironment candidate) {
		if (candidate==null) return false;
		String os = candidate.getOS();
		String ws = candidate.getWS();
		String arch = candidate.getOSArch();
		if (os!=null && isMatching(os, UpdateManagerUtils.getOS())==false) return false;
		if (ws!=null && isMatching(ws, UpdateManagerUtils.getWS())==false) return false;
		if (arch!=null && isMatching(arch, UpdateManagerUtils.getOSArch())==false) return false;
		return true;
	}

	/**
	 * 
	 */	
	private static boolean isMatching(String candidateValues, String siteValues) {
		if (siteValues==null) return false;
		if ("*".equalsIgnoreCase(candidateValues)) return true;
		siteValues = siteValues.toUpperCase();		
		StringTokenizer stok = new StringTokenizer(candidateValues, ",");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().toUpperCase();
			if (siteValues.indexOf(token)!=-1) return true;
		}
		return false;
	}
	
/**
 * write XML file
 */
public static class Writer {

	private PrintWriter w;
	private OutputStream out;
	private OutputStreamWriter outWriter;
	private BufferedWriter buffWriter;
	private String encoding;

	/*
	 * 
	 */
	private Writer() {
		super();
	}
	
	public void init(File file, String encoding) throws FileNotFoundException, UnsupportedEncodingException{
		this.encoding = encoding;
		out = new FileOutputStream(file);
		outWriter = new OutputStreamWriter(out, encoding); //$NON-NLS-1$
		buffWriter = new BufferedWriter(outWriter);
		w = new PrintWriter(buffWriter);
	}

	
	/*
	 * 
	 */
	public void write(IWritable element) {
		w.println("<?xml version=\"1.0\" encoding=\""+encoding+"\"?>"); //$NON-NLS-1$
		w.println(""); //$NON-NLS-1$
		w.println("<!-- File written by Update manager 2.0 -->"); //$NON-NLS-1$
		w.println("<!-- comments in this file are not preserved -->"); //$NON-NLS-1$
		w.println(""); //$NON-NLS-1$
		 ((IWritable) element).write(0, w);
		close();
	}
	
	/*
	 * 
	 */
	 public void close(){
	 		w.close();
	 }

	/*
	 * 
	 */
	private static void appendEscapedChar(StringBuffer buffer, char c) {
		String replacement = getReplacement(c);
		if (replacement != null) {
			buffer.append('&');
			buffer.append(replacement);
			buffer.append(';');
		} else {
			if ((c >= ' ' && c <= 0x7E) || c == '\n' || c == '\r' || c == '\t') {
				buffer.append(c);
			} else {
				buffer.append("&#"); //$NON-NLS-1$
				buffer.append(Integer.toString(c));
				buffer.append(';');
			}
		}
	}

	/*
	 * 
	 */
	public static String xmlSafe(String s) {
		StringBuffer result = new StringBuffer(s.length() + 10);
		for (int i = 0; i < s.length(); ++i)
			appendEscapedChar(result, s.charAt(i));
		return result.toString();
	}
	
	/*
	 * 
	 */
	private static String getReplacement(char c) {
		// Encode special XML characters into the equivalent character references.
		// These five are defined by default for all XML documents.
		switch (c) {
			case '<' :
				return "lt"; //$NON-NLS-1$
			case '>' :
				return "gt"; //$NON-NLS-1$
			case '"' :
				return "quot"; //$NON-NLS-1$
			case '\'' :
				return "apos"; //$NON-NLS-1$
			case '&' :
				return "amp"; //$NON-NLS-1$
		}
		return null;
	}
}
	public static Writer getWriter(File file,String encoding) throws FileNotFoundException, UnsupportedEncodingException {
		if (writer==null) writer = new Writer();
		writer.init(file,encoding);
		return writer;
	}
	
	public static boolean isSameTimestamp(URL url, long timestamp) {	
		try {
			URL resolvedURL = URLEncoder.encode(url);
			Response response = UpdateCore.getPlugin().get(resolvedURL);
			long remoteLastModified = response.getLastModified();
			// 2 seconds tolerance, as some OS's may round up the time stamp
			// to the closest second. For safety, we make it 2 seconds.
			return Math.abs(remoteLastModified - timestamp)/1000 <= 2;
		} catch (MalformedURLException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Returns system architecture specification. A comma-separated list of arch
	 * designators defined by the platform. 
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
	 * @see BootLoader#ARCH_PA_RISC
	 * @see BootLoader#ARCH_PPC
	 * @see BootLoader#ARCH_SPARC
	 * @see BootLoader#ARCH_X86
	 * @return system architecture specification
	 * @since 2.1
	 */
	public static String getOSArch() {
		if (arch == null)
			arch = BootLoader.getOSArch();
		return arch;
	}

	/**
	 * Returns operating system specification. A comma-separated list of os
	 * designators defined by the platform.
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 *
	 * @see BootLoader#OS_AIX
	 * @see BootLoader#OS_HPUX
	 * @see BootLoader#OS_LINUX
	 * @see BootLoader#OS_MACOSX
	 * @see BootLoader#OS_QNX
	 * @see BootLoader#OS_SOLARIS
	 * @see BootLoader#OS_WIN32 
	 * @return the operating system specification.
	 * @since 2.1
	 */
	public static String getOS() {
		if (os == null)
			os = BootLoader.getOS();
		return os;
	}

	/**
	 * Returns system architecture specification. A comma-separated list of arch
	 * designators defined by the platform. 
	 * 
	 * This information is used as a hint by the installation and update
	 * support.
	 * 
	 * @see BootLoader#WS_CARBON
	 * @see BootLoader#WS_GTK
	 * @see BootLoader#WS_MOTIF
	 * @see BootLoader#WS_PHOTON
	 * @see BootLoader#WS_WIN32 
	 * @return system architecture specification.
	 * @since 2.1
	 */
	public static String getWS() {
		if (ws == null)
			ws = BootLoader.getWS();
		return ws;
	}

	/**
	 * Returns current locale
	 * 
	 * @see BootLoader#getNL()
	 * @return the string name of the current locale or <code>null</code>
	 * @since 2.1
	 */
	public static String getNL() {
		if (nl == null)
			nl = BootLoader.getNL();
		return nl;
	}

	/**
	 * Sets the arch.
	 * @param arch The arch to set
	 */
	public static void setOSArch(String arch) {
		UpdateManagerUtils.arch = arch;
	}

	/**
	 * Sets the os.
	 * @param os The os to set
	 */
	public static void setOS(String os) {
		UpdateManagerUtils.os = os;
	}

	/**
	 * Sets the ws.
	 * @param ws The ws to set
	 */
	public static void setWS(String ws) {
		UpdateManagerUtils.ws = ws;
	}

	/**
	 * Sets the nl.
	 * @param nl The nl to set
	 */
	public static void setNL(String nl) {
		UpdateManagerUtils.nl = nl;
	}
}
