/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Project.java
 *
 * Created on March 30, 2003, 2:22 PM
 */

package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutputUtil;
import edu.umd.cs.findbugs.xml.XMLWriteable;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.*;

import org.dom4j.DocumentException;
import org.dom4j.Element;

/**
 * A project in the GUI.
 * This consists of some number of Jar files to analyze for bugs, and optionally
 * <p/>
 * <ul>
 * <li> some number of source directories, for locating the program's
 * source code
 * <li> some number of auxiliary classpath entries, for locating classes
 * referenced by the program which the user doesn't want to analyze
 * <li> some number of boolean options
 * </ul>
 *
 * @author David Hovemeyer
 */
public class Project implements XMLWriteable {
	private static final boolean DEBUG = Boolean.getBoolean("findbugs.project.debug");

	/**
	 * Project filename.
	 */
	private String projectFileName;

	/**
	 * Options.
	 */
	private Map<String, Boolean> optionsMap;

	/**
	 * The list of project files.
	 */
	private LinkedList<String> fileList;

	/**
	 * The list of source directories.
	 */
	private LinkedList<String> srcDirList;

	/**
	 * The list of auxiliary classpath entries.
	 */
	private LinkedList<String> auxClasspathEntryList;

	/**
	 * Flag to indicate that this Project has been modified.
	 */
	private boolean isModified;

	/**
	 * Constant used to name anonymous projects.
	 */
	public static final String UNNAMED_PROJECT = "<<unnamed project>>";

	/**
	 * Create an anonymous project.
	 */
	public Project() {
		this.projectFileName = UNNAMED_PROJECT;
		optionsMap = new HashMap<String, Boolean>();
		optionsMap.put(RELATIVE_PATHS, Boolean.FALSE);
		fileList = new LinkedList<String>();
		srcDirList = new LinkedList<String>();
		auxClasspathEntryList = new LinkedList<String>();
		isModified = false;
	}

	/**
	 * Return an exact copy of this Project.
	 */
	public Project duplicate() {
		Project dup = new Project();
		dup.projectFileName = this.projectFileName;
		dup.optionsMap.putAll(this.optionsMap);
		dup.fileList.addAll(this.fileList);
		dup.srcDirList.addAll(this.srcDirList);
		dup.auxClasspathEntryList.addAll(this.auxClasspathEntryList);

		return dup;
	}

	/**
	 * Return whether or not this Project has unsaved modifications.
	 */
	public boolean isModified() {
		return isModified;
	}

	/**
	 * Set whether or not this Project has unsaved modifications.
	 */
	public void setModified(boolean isModified) {
		this.isModified = isModified;
	}

	/**
	 * Get the project filename.
	 */
	public String getProjectFileName() {
		return projectFileName;
	}

	/**
	 * Set the project filename.
	 *
	 * @param projectFileName the new filename
	 */
	public void setProjectFileName(String projectFileName) {
		this.projectFileName = projectFileName;
	}

	/**
	 * Add a file to the project.
	 *
	 * @param fileName the file to add
	 * @return true if the file was added, or false if the
	 *         file was already present
	 */
	public boolean addFile(String fileName) {
		return addToListInternal(fileList, makeAbsoluteCWD(fileName));
	}

	/**
	 * Add a source directory to the project.
	 * @param dirName the directory to add
	 * @return true if the source directory was added, or false if the
	 *   source directory was already present
	 */
	public boolean addSourceDir(String dirName) {
		return addToListInternal(srcDirList, makeAbsoluteCWD(dirName));
	}

	/**
	 * Retrieve the Options value.
	 *
	 * @param option the name of option to get
	 * @return the value of the option
	 */
	public boolean getOption(String option) {
		Boolean value = optionsMap.get(option);
		return value != null && value.booleanValue();
	}

	/**
	 * Get the number of files in the project.
	 *
	 * @return the number of files in the project
	 */
	public int getFileCount() {
		return fileList.size();
	}

	/**
	 * Get the given file in the list of project files.
	 *
	 * @param num the number of the file in the list of project files
	 * @return the name of the file
	 */
	public String getFile(int num) {
		return fileList.get(num);
	}

	/**
	 * Remove file at the given index in the list of project files
	 *
	 * @param num index of the file to remove in the list of project files
	 */
	public void removeFile(int num) {
		fileList.remove(num);
		isModified = true;
	}

	/**
	 * Get the list of files, directories, and zip files in the project.
	 */
	public List<String> getFileList() {
		return fileList;
	}

	/**
	 * Get the number of source directories in the project.
	 *
	 * @return the number of source directories in the project
	 */
	public int getNumSourceDirs() {
		return srcDirList.size();
	}

	/**
	 * Get the given source directory.
	 *
	 * @param num the number of the source directory
	 * @return the source directory
	 */
	public String getSourceDir(int num) {
		return srcDirList.get(num);
	}

	/**
	 * Remove source directory at given index.
	 *
	 * @param num index of the source directory to remove
	 */
	public void removeSourceDir(int num) {
		srcDirList.remove(num);
		isModified = true;
	}

	/**
	 * Get project files as an array of Strings.
	 */
	public String[] getFileArray() {
		return (String[]) fileList.toArray(new String[fileList.size()]);
	}

	/**
	 * Get source dirs as an array of Strings.
	 */
	public String[] getSourceDirArray() {
		return (String[]) srcDirList.toArray(new String[srcDirList.size()]);
	}

	/**
	 * Get the source dir list.
	 */
	public List<String> getSourceDirList() {
		return srcDirList;
	}

	/**
	 * Add an auxiliary classpath entry
	 *
	 * @param auxClasspathEntry the entry
	 * @return true if the entry was added successfully, or false
	 *         if the given entry is already in the list
	 */
	public boolean addAuxClasspathEntry(String auxClasspathEntry) {
		return addToListInternal(auxClasspathEntryList, makeAbsoluteCWD(auxClasspathEntry));
	}

	/**
	 * Get the number of auxiliary classpath entries.
	 */
	public int getNumAuxClasspathEntries() {
		return auxClasspathEntryList.size();
	}

	/**
	 * Get the n'th auxiliary classpath entry.
	 */
	public String getAuxClasspathEntry(int n) {
		return auxClasspathEntryList.get(n);
	}

	/**
	 * Remove the n'th auxiliary classpath entry.
	 */
	public void removeAuxClasspathEntry(int n) {
		auxClasspathEntryList.remove(n);
		isModified = true;
	}

	/**
	 * Return the list of aux classpath entries.
	 */
	public List<String> getAuxClasspathEntryList() {
		return auxClasspathEntryList;
	}

	/**
	 * Worklist item for finding implicit classpath entries.
	 */
	private static class WorkListItem {
		private URL url;

		/**
		 * Constructor.
		 *
		 * @param url the URL of the Jar or Zip file
		 */
		public WorkListItem(URL url) {
			this.url = url;
		}

		/**
		 * Get URL of Jar/Zip file.
		 */
		public URL getURL() {
			return this.url;
		}
	}

	/**
	 * Worklist for finding implicit classpath entries.
	 */
	private static class WorkList {
		private LinkedList<WorkListItem> itemList;
		private HashSet<String> addedSet;

		/**
		 * Constructor.
		 * Creates an empty worklist.
		 */
		public WorkList() {
			this.itemList = new LinkedList<WorkListItem>();
			this.addedSet = new HashSet<String>();
		}

		/**
		 * Create a URL from a filename specified in the project file.
		 */
		public URL createURL(String fileName) throws MalformedURLException {
			String protocol = FindBugs.getURLProtocol(fileName);
			if (protocol == null) {
				fileName = "file:" + fileName;
			}
			return new URL(fileName);
		}

		/**
		 * Create a URL of a file relative to another URL.
		 */
		public URL createRelativeURL(URL base, String fileName) throws MalformedURLException {
			return new URL(base, fileName);
		}

		/**
		 * Add a worklist item.
		 *
		 * @param item the WorkListItem representing a zip/jar file to be examined
		 * @return true if the item was added, false if not (because it was
		 *         examined already)
		 */
		public boolean add(WorkListItem item) {
			if (DEBUG) System.out.println("Adding " + item.getURL().toString());
			if (!addedSet.add(item.getURL().toString())) {
				if (DEBUG) System.out.println("\t==> Already processed");
				return false;
			}

			itemList.add(item);
			return true;
		}

		/**
		 * Return whether or not the worklist is empty.
		 */
		public boolean isEmpty() {
			return itemList.isEmpty();
		}

		/**
		 * Get the next item in the worklist.
		 */
		public WorkListItem getNextItem() {
			return itemList.removeFirst();
		}
	}

	/**
	 * Return the list of implicit classpath entries.  The implicit
	 * classpath is computed from the closure of the set of jar files
	 * that are referenced by the <code>"Class-Path"</code> attribute
	 * of the manifest of the any jar file that is part of this project
	 * or by the <code>"Class-Path"</code> attribute of any directly or
	 * indirectly referenced jar.  The referenced jar files that exist
	 * are the list of implicit classpath entries.
	 */
	public List<String> getImplicitClasspathEntryList() {
		final LinkedList<String> implicitClasspath = new LinkedList<String>();
		WorkList workList = new WorkList();

		// Prime the worklist by adding the zip/jar files
		// in the project.
		for (Iterator<String> i = fileList.iterator(); i.hasNext();) {
			String fileName = i.next();

			try {
				URL url = workList.createURL(fileName);
				WorkListItem item = new WorkListItem(url);
				workList.add(item);
			} catch (MalformedURLException ignore) {
				// Ignore
			}
		}

		// Scan recursively.
		while (!workList.isEmpty()) {
			WorkListItem item = workList.getNextItem();
			processComponentJar(item.getURL(), workList, implicitClasspath);
		}

		return implicitClasspath;
	}

	/**
	 * Examine the manifest of a single zip/jar file for implicit
	 * classapth entries.
	 *
	 * @param jarFileURL        URL of the zip/jar file
	 * @param workList          worklist of zip/jar files to examine
	 * @param implicitClasspath list of implicit classpath entries found
	 */
	private void processComponentJar(URL jarFileURL, WorkList workList,
		List<String> implicitClasspath) {

		if (DEBUG) System.out.println("Processing " + jarFileURL.toString());

		if (!jarFileURL.toString().endsWith(".zip") && !jarFileURL.toString().endsWith(".jar"))
			return;

		try {
			URL manifestURL = new URL("jar:" + jarFileURL.toString() + "!/META-INF/MANIFEST.MF");

			InputStream in = null;
			try {
				in = manifestURL.openStream();
				Manifest manifest = new Manifest(in);

				Attributes mainAttrs = manifest.getMainAttributes();
				String classPath = mainAttrs.getValue("Class-Path");
				if (classPath != null) {
					String[] fileList = classPath.split("\\s+");

					for (int i = 0; i < fileList.length; ++i) {
						String jarFile = fileList[i];
						URL referencedURL = workList.createRelativeURL(jarFileURL, jarFile);
						if (workList.add(new WorkListItem(referencedURL))) {
							implicitClasspath.add(referencedURL.toString());
							if (DEBUG) System.out.println("Implicit jar: " + referencedURL.toString());
						}
					}
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
		} catch (IOException ignore) {
			// Ignore
		}
	}

	private static final String OPTIONS_KEY = "[Options]";
	private static final String JAR_FILES_KEY = "[Jar files]";
	private static final String SRC_DIRS_KEY = "[Source dirs]";
	private static final String AUX_CLASSPATH_ENTRIES_KEY = "[Aux classpath entries]";

	// Option keys
	public static final String RELATIVE_PATHS = "relative_paths";

	/**
	 * Save the project to an output file.
	 *
	 * @param outputFile       name of output file
	 * @param useRelativePaths true if the project should be written
	 *                         using only relative paths
	 * @param relativeBase     if useRelativePaths is true,
	 *                         this file is taken as the base directory in terms of which
	 *                         all files should be made relative
	 * @throws IOException if an error occurs while writing
	 */
	public void write(String outputFile, boolean useRelativePaths, String relativeBase)
	        throws IOException {
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
		try {
			writer.println(JAR_FILES_KEY);
			for (Iterator<String> i = fileList.iterator(); i.hasNext();) {
				String jarFile = i.next();
				if (useRelativePaths)
					jarFile = convertToRelative(jarFile, relativeBase);
				writer.println(jarFile);
			}

			writer.println(SRC_DIRS_KEY);
			for (Iterator<String> i = srcDirList.iterator(); i.hasNext();) {
				String srcDir = i.next();
				if (useRelativePaths)
					srcDir = convertToRelative(srcDir, relativeBase);
				writer.println(srcDir);
			}

			writer.println(AUX_CLASSPATH_ENTRIES_KEY);
			for (Iterator<String> i = auxClasspathEntryList.iterator(); i.hasNext();) {
				String auxClasspathEntry = i.next();
				if (useRelativePaths)
					auxClasspathEntry = convertToRelative(auxClasspathEntry, relativeBase);
				writer.println(auxClasspathEntry);
			}

			if (useRelativePaths) {
				writer.println(OPTIONS_KEY);
				writer.println(RELATIVE_PATHS + "=true");
			}
		} finally {
			writer.close();
		}
		
		// Project successfully saved
		isModified = false;
	}

	/**
	 * Read the project from an input file.
	 * This method should only be used on an empty Project
	 * (created with the default constructor).
	 *
	 * @param inputFile name of the input file to read the project from
	 * @throws IOException if an error occurs while reading
	 */
	public void read(String inputFile) throws IOException {
		if (isModified)
			throw new IllegalStateException("Reading into a modified Project!");

		// Make the input file absolute, if necessary
		File file = new File(inputFile);
		if (!file.isAbsolute())
			inputFile = file.getAbsolutePath();

		// Store the project filename
		setProjectFileName(inputFile);

		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new FileReader(inputFile));
			String line;
			line = getLine(reader);

			if (line == null || !line.equals(JAR_FILES_KEY))
				throw new IOException("Bad format: missing jar files key");
			while ((line = getLine(reader)) != null && !line.equals(SRC_DIRS_KEY)) {
				addToListInternal(fileList, line);
			}

			if (line == null)
				throw new IOException("Bad format: missing source dirs key");
			while ((line = getLine(reader)) != null && !line.equals(AUX_CLASSPATH_ENTRIES_KEY)) {
				addToListInternal(srcDirList, line);
			}

			// The list of aux classpath entries is optional
			if (line != null) {
				while ((line = getLine(reader)) != null) {
					if (line.equals(OPTIONS_KEY))
						break;
					addToListInternal(auxClasspathEntryList, line);
				}
			}

			// The Options section is also optional
			if (line != null && line.equals(OPTIONS_KEY)) {
				while ((line = getLine(reader)) != null && !line.equals(JAR_FILES_KEY))
					parseOption(line);
			}

			// If this project has the relative paths option set,
			// resolve all internal relative paths into absolute
			// paths, using the absolute path of the project
			// file as a base directory.
			if (getOption(RELATIVE_PATHS)) {
				makeListAbsoluteProject(fileList);
				makeListAbsoluteProject(srcDirList);
				makeListAbsoluteProject(auxClasspathEntryList);
			}
	
			// Clear the modification flag set by the various "add" methods.
			isModified = false;
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	/**
	 * Read a line from a BufferedReader, ignoring blank lines
	 * and comments.
	 */
	private static String getLine(BufferedReader reader) throws IOException {
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!line.equals("") && !line.startsWith("#"))
				break;
		}
		return line;
	}

	/**
	 * Convert to a string in a nice (displayable) format.
	 */
	public String toString() {
		String name = projectFileName;
		int lastSep = name.lastIndexOf(File.separatorChar);
		if (lastSep >= 0)
			name = name.substring(lastSep + 1);
		int dot = name.lastIndexOf('.');
		if (dot >= 0)
			name = name.substring(0, dot);
		return name;
	}

	/**
	 * Transform a user-entered filename into a proper filename,
	 * by adding the ".fb" file extension if it isn't already present.
	 */
	public static String transformFilename(String fileName) {
		if (!fileName.endsWith(".fb"))
			fileName = fileName + ".fb";
		return fileName;
	}

	private static final String JAR_ELEMENT_NAME = "Jar";
	private static final String AUX_CLASSPATH_ENTRY_ELEMENT_NAME = "AuxClasspathEntry";
	private static final String SRC_DIR_ELEMENT_NAME = "SrcDir";
	private static final String FILENAME_ATTRIBUTE_NAME = "filename";

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		xmlOutput.openTag(BugCollection.PROJECT_ELEMENT_NAME);

		XMLOutputUtil.writeElementList(xmlOutput, JAR_ELEMENT_NAME, fileList);
		XMLOutputUtil.writeElementList(xmlOutput, AUX_CLASSPATH_ENTRY_ELEMENT_NAME, auxClasspathEntryList);
		XMLOutputUtil.writeElementList(xmlOutput, SRC_DIR_ELEMENT_NAME, srcDirList);

		xmlOutput.closeTag(BugCollection.PROJECT_ELEMENT_NAME);
	}

	/**
	 * Parse one line in the [Options] section.
	 *
	 * @param option one line in the [Options] section
	 */
	private void parseOption(String option) throws IOException {
		int equalPos = option.indexOf("=");
		if (equalPos < 0)
			throw new IOException("Bad format: invalid option format");
		String name = option.substring(0, equalPos);
		String value = option.substring(equalPos + 1);
		optionsMap.put(name, Boolean.valueOf(value));
	}

	/**
	 * Hack for whether files are case insensitive.
	 * For now, we'll assume that Windows is the only
	 * case insensitive OS.  (OpenVMS users,
	 * feel free to submit a patch :-)
	 */
	private static final boolean FILE_IGNORE_CASE =
	        System.getProperty("os.name", "unknown").startsWith("Windows");

	/**
	 * Converts a full path to a relative path if possible
	 *
	 * @param srcFile path to convert
	 * @return the converted filename
	 */
	private String convertToRelative(String srcFile, String base) {
		String slash = System.getProperty("file.separator");

		if (FILE_IGNORE_CASE) {
			srcFile = srcFile.toLowerCase();
			base = base.toLowerCase();
		}

		if (base.equals(srcFile))
			return ".";

		if (!base.endsWith(slash))
			base = base + slash;

		if (base.length() <= srcFile.length()) {
			String root = srcFile.substring(0, base.length());
			if (root.equals(base)) {
				// Strip off the base directory, make relative
				return "." + System.getProperty("file.separator") + srcFile.substring(base.length());
			}
		}
		
		//See if we can build a relative path above the base using .. notation
		int slashPos = srcFile.indexOf(slash);
		int branchPoint;
		if (slashPos >= 0) {
			String subPath = srcFile.substring(0, slashPos);
			if ((subPath.length() == 0) || base.startsWith(subPath)) {
				branchPoint = slashPos + 1;
				slashPos = srcFile.indexOf(slash, branchPoint);
				while (slashPos >= 0) {
					subPath = srcFile.substring(0, slashPos);
					if (base.startsWith(subPath))
						branchPoint = slashPos + 1;
					else
						break;
					slashPos = srcFile.indexOf(slash, branchPoint);
				}

				int slashCount = 0;
				slashPos = base.indexOf(slash, branchPoint);
				while (slashPos >= 0) {
					slashCount++;
					slashPos = base.indexOf(slash, slashPos + 1);
				}

				StringBuffer path = new StringBuffer();
				String upDir = ".." + slash;
				for (int i = 0; i < slashCount; i++)
					path.append(upDir);
				path.append(srcFile.substring(branchPoint));
				return path.toString();
			}
		}


		return srcFile;

	}

	/**
	 * Converts a relative path to an absolute path if possible.
	 *
	 * @param fileName path to convert
	 * @return the converted filename
	 */
	private String convertToAbsolute(String fileName) throws IOException {
		// At present relative paths are only calculated if the fileName is
		// below the project file. This need not be the case, and we could use ..
		// syntax to move up the tree. (To Be Added)

		File file = new File(fileName);

		if (!file.isAbsolute()) {
			// Only try to make the relative path absolute
			// if the project file is absolute.
			File projectFile = new File(projectFileName);
			if (projectFile.isAbsolute()) {
				// Get base directory (parent of the project file)
				String base = new File(projectFileName).getParent();

				// Make the file absolute in terms of the parent directory
				fileName = new File(base, fileName).getCanonicalPath();
			}
		}
		return fileName;
	}

	/**
	 * Make the given filename absolute relative to the
	 * current working directory.
	 */
	private static String makeAbsoluteCWD(String fileName) {
		File file = new File(fileName);
		boolean hasProtocol = (FindBugs.getURLProtocol(fileName) != null);
		if (!hasProtocol && !file.isAbsolute())
			fileName = file.getAbsolutePath();
		return fileName;
	}

	/**
	 * Add a value to given list, making the Project modified
	 * if the value is not already present in the list.
	 *
	 * @param list  the list
	 * @param value the value to be added
	 * @return true if the value was not already present in the list,
	 *         false otherwise
	 */
	private boolean addToListInternal(List<String> list, String value) {
		if (!list.contains(value)) {
			list.add(value);
			isModified = true;
			return true;
		} else
			return false;
	}

	/**
	 * Make the given list of pathnames absolute relative
	 * to the absolute path of the project file.
	 */
	private void makeListAbsoluteProject(List<String> list) throws IOException {
		List<String> replace = new LinkedList<String>();
		for (Iterator<String> i = list.iterator(); i.hasNext();) {
			String fileName = i.next();
			fileName = convertToAbsolute(fileName);
			replace.add(fileName);
		}

		list.clear();
		list.addAll(replace);
	}
}

// vim:ts=4
