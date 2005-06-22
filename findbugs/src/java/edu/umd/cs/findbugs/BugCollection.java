/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

package edu.umd.cs.findbugs;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import edu.umd.cs.findbugs.ba.ClassHash;
import edu.umd.cs.findbugs.xml.Dom4JXMLOutput;
import edu.umd.cs.findbugs.xml.OutputStreamXMLOutput;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLOutputUtil;

/**
 * Abstract base class for collections of BugInstance objects
 * and error messages associated with analysis.
 * Supports reading and writing XML files.
 *
 * @author David Hovemeyer
 * @see BugInstance
 */
public abstract class BugCollection {

	/**
	 * Add a Collection of BugInstances to this BugCollection object.
	 * This just calls add(BugInstance) for each instance in the input collection.
	 *
	 * @param collection the Collection of BugInstances to add
	 */
	public void addAll(Collection<BugInstance> collection) {
		Iterator<BugInstance> i = collection.iterator();
		while (i.hasNext()) {
			add(i.next());
		}
	}
	
	/**
	 * Add a Collection of BugInstances to this BugCollection object.
	 *
	 * @param collection       the Collection of BugInstances to add
	 * @param updateActiveTime true if active time of added BugInstances should
	 *                         be updated to match collection: false if not
	 */
	public void addAll(Collection<BugInstance> collection, boolean updateActiveTime) {
		for (BugInstance warning : collection) {
			add(warning, updateActiveTime);
		}
	}

	/**
	 * Add a BugInstance to this BugCollection.
	 * This just calls add(bugInstance, true).
	 *
	 * @param bugInstance the BugInstance
	 * @return true if the BugInstance was added, or false if a matching
	 *         BugInstance was already in the BugCollection
	 */
	public boolean add(BugInstance bugInstance) {
		return add(bugInstance, true);
	}

	/**
	 * Add a BugInstance to this BugCollection.
	 *
	 * @param bugInstance      the BugInstance
	 * @param updateActiveTime true if the warning's active time should be updated
	 *                         to include the collection's current time
	 * @return true if the BugInstance was added, or false if a matching
	 *         BugInstance was already in the BugCollection
	 */
	public abstract boolean add(BugInstance bugInstance, boolean updateActiveTime);

	/**
	 * Remove a BugInstance from this BugCollection.
	 *
	 * @param bugInstance the BugInstance
	 * @return true if the BugInstance was removed, or false if
	 *         it (or an equivalent BugInstance) was not present originally
	 */
	public abstract boolean remove(BugInstance bugInstance);

	/**
	 * Return an Iterator over all the BugInstance objects in
	 * the BugCollection.
	 */
	public abstract Iterator<BugInstance> iterator();

	/**
	 * Return the Collection storing the BugInstance objects.
	 */
	public abstract Collection<BugInstance> getCollection();

	/**
	 * Add an analysis error.
	 *
	 * @param message the error message
	 */
	public void addError(String message) {
		addError(message, null);
	}

	/**
	 * Add an analysis error.
	 * 
	 * @param message   the error message
	 * @param exception the cause of the error
	 */
	public abstract void addError(String message, Throwable exception);
	
	/**
	 * Add an analysis error.
	 * 
	 * @param the AnalysisError object to add
	 */
	public abstract void addError(AnalysisError error);
	
	/**
	 * Add a missing class message.
	 *
	 * @param message the missing class message
	 */
	public abstract void addMissingClass(String message);

	/**
	 * Return an Iterator over analysis errors.
	 */
	public abstract Iterator<AnalysisError> errorIterator();
	
	/**
	 * Return an Iterator over missing class messages.
	 */
	public abstract Iterator<String> missingClassIterator();

	/**
	 * Get the summary HTML text.
	 */
	public abstract String getSummaryHTML() throws IOException;
	
	/**
	 * Get the project stats.
	 */
	public abstract ProjectStats getProjectStats();

	/**
	 * Look up a BugInstance by its unique id.
	 * 
	 * @param uniqueId the BugInstance's unique id.
	 * @return the BugInstance with the given unique id,
	 *         or null if there is no such BugInstance
	 */
	public abstract BugInstance lookupFromUniqueId(String uniqueId);
	
	/**
	 * Set the timestamp of the BugCollection.
	 * This is the wall clock time of the most recent update
	 * of the collection.
	 *  
	 * @return the timestamp
	 */
	public abstract long getTimestamp();

	/**
	 * Get the timestamp of the BugCollection.
	 * This is the wall clock time of the most recent update
	 * of the collection.
	 * 
	 * @param timestamp the timestamp
	 */
	public abstract void setTimestamp(long timestamp);
	
	public abstract ClassHash getClassHash(String className);
	
	public abstract void setClassHash(String className, ClassHash classHash);
	
	public abstract Iterator<ClassHash> classHashIterator();
	
	/**
	 * Make an exact deep copy of this BugCollection.
	 * 
	 * @return an exact copy of this BugCollection
	 */
	public abstract BugCollection duplicate();
	
	/**
	 * Clear out all BugInstances, but preserve other metadata.
	 */
	public abstract void clearBugInstances();
	
	/**
	 * Get the current release name.
	 * Returns an empty String if the current release name is unknown.
	 */
	public abstract String getReleaseName();

	private static final boolean REPORT_SUMMARY_HTML = 
		Boolean.getBoolean("findbugs.report.SummaryHTML");

	static final String ROOT_ELEMENT_NAME = "BugCollection";
	static final String SRCMAP_ELEMENT_NAME = "SrcMap";
	static final String PROJECT_ELEMENT_NAME = "Project";
	static final String ERRORS_ELEMENT_NAME = "Errors";
	static final String ANALYSIS_ERROR_ELEMENT_NAME = "AnalysisError"; // 0.8.6 and earlier
	static final String ERROR_ELEMENT_NAME = "Error";                  // 0.8.7 and later
	static final String ERROR_MESSAGE_ELEMENT_NAME = "ErrorMessage";   // 0.8.7 and later
	static final String ERROR_EXCEPTION_ELEMENT_NAME = "Exception";    // 0.8.7 and later
	static final String ERROR_STACK_TRACE_ELEMENT_NAME = "StackTrace"; // 0.8.7 and later
	static final String MISSING_CLASS_ELEMENT_NAME = "MissingClass";
	static final String SUMMARY_HTML_ELEMENT_NAME = "SummaryHTML";
	static final String APP_CLASS_ELEMENT_NAME = "AppClass";
	static final String CLASS_HASHES_ELEMENT_NAME = "ClassHashes"; // 0.9.2 and later

	/**
	 * Read XML data from given file into this object,
	 * populating given Project as a side effect.
	 *
	 * @param fileName name of the file to read
	 * @param project  the Project
	 */
	public void readXML(String fileName, Project project)
	        throws IOException, DocumentException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(fileName));
		readXML(in, project);
	}

	/**
	 * Read XML data from given file into this object,
	 * populating given Project as a side effect.
	 *
	 * @param file    the file
	 * @param project the Project
	 */
	public void readXML(File file, Project project)
	        throws IOException, DocumentException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
		readXML(in, project);
	}

	/**
	 * Read XML data from given input stream into this
	 * object, populating the Project as a side effect.
	 * An attempt will be made to close the input stream
	 * (even if an exception is thrown).
	 *
	 * @param in      the InputStream
	 * @param project the Project
	 */
	public void readXML(InputStream in, Project project)
	        throws IOException, DocumentException {
		if (in == null) throw new IllegalArgumentException();
		if (project == null) throw new IllegalArgumentException();

		try {
			doReadXML(in, project);
		} finally {
			in.close();
		}
	}

	private void doReadXML(InputStream in, Project project) throws IOException, DocumentException {

		checkInputStream(in);

		try {
			SAXBugCollectionHandler handler = new SAXBugCollectionHandler(this, project);

			// FIXME: for now, use dom4j's XML parser
			XMLReader xr = new org.dom4j.io.aelfred.SAXDriver();

			xr.setContentHandler(handler);
			xr.setErrorHandler(handler);

			Reader reader = new InputStreamReader(in);

			xr.parse(new InputSource(reader));
		} catch (SAXException e) {
			// FIXME: throw SAXException from method?
			throw new DocumentException("Parse error", e);
		}

		// Presumably, project is now up-to-date
		project.setModified(false);
	}

	/**
	 * Write this BugCollection to a file as XML.
	 *
	 * @param fileName the file to write to
	 * @param project  the Project from which the BugCollection was generated
	 */
	public void writeXML(String fileName, Project project) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
		writeXML(out, project);
	}

	/**
	 * Write this BugCollection to a file as XML.
	 *
	 * @param file    the file to write to
	 * @param project the Project from which the BugCollection was generated
	 */
	public void writeXML(File file, Project project) throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		writeXML(out, project);
	}

	/**
	 * Convert the BugCollection into a dom4j Document object.
	 *
	 * @param project the Project from which the BugCollection was generated
	 * @return the Document representing the BugCollection as a dom4j tree
	 */
	public Document toDocument(Project project) {
		DocumentFactory docFactory = new DocumentFactory();
		Document document = docFactory.createDocument();
		Dom4JXMLOutput treeBuilder = new Dom4JXMLOutput(document);

		try {
			writeXML(treeBuilder, project);
		} catch (IOException e) {
			// Can't happen
		}

		return document;
	}

	/**
	 * Write the BugCollection to given output stream as XML.
	 * The output stream will be closed, even if an exception is thrown.
	 *
	 * @param out     the OutputStream to write to
	 * @param project the Project from which the BugCollection was generated
	 */
	public void writeXML(OutputStream out, Project project) throws IOException {
		XMLOutput xmlOutput = new OutputStreamXMLOutput(out);

		writeXML(xmlOutput, project);
	}

	public void writePrologue(XMLOutput xmlOutput, Project project) throws IOException {
		xmlOutput.beginDocument();
		xmlOutput.openTag(ROOT_ELEMENT_NAME,
			new XMLAttributeList()
				.addAttribute("version",Version.RELEASE)
				.addAttribute("timestamp",String.valueOf(getTimestamp()))
		);
		project.writeXML(xmlOutput);
	}

	/**
	 * Write the BugCollection to an XMLOutput object.
	 * The finish() method of the XMLOutput object is guaranteed
	 * to be called.
	 *
	 * <p>
	 * To write the SummaryHTML element, set property
	 * findbugs.report.SummaryHTML to "true".
	 * </p>
	 *
	 * @param xmlOutput the XMLOutput object
	 * @param project   the Project from which the BugCollection was generated
	 */
	public void writeXML(XMLOutput xmlOutput, Project project) throws IOException {
		try {
			writePrologue(xmlOutput, project);

			// Write BugInstances
			XMLOutputUtil.writeCollection(xmlOutput, getCollection());

			writeEpilogue(xmlOutput);
		} finally {
			xmlOutput.finish();
		}
	}

	public void writeEpilogue(XMLOutput xmlOutput) throws IOException {
		// Errors, missing classes
		emitErrors(xmlOutput);

		// Statistics
		getProjectStats().writeXML(xmlOutput);
		
		// Class and method hashes
		xmlOutput.openTag(CLASS_HASHES_ELEMENT_NAME);
		for (Iterator<ClassHash> i = classHashIterator(); i.hasNext();) {
			ClassHash classHash = i.next();
			classHash.writeXML(xmlOutput);
		}
		xmlOutput.closeTag(CLASS_HASHES_ELEMENT_NAME);
		
		// Summary HTML
		if ( REPORT_SUMMARY_HTML ) {
			String html = getSummaryHTML();
			if (html != null && !html.equals("")) {
				xmlOutput.openTag(SUMMARY_HTML_ELEMENT_NAME);
				xmlOutput.writeCDATA(html);
				xmlOutput.closeTag(SUMMARY_HTML_ELEMENT_NAME);
			}
		}

		xmlOutput.closeTag(ROOT_ELEMENT_NAME);
	}

	private void emitErrors(XMLOutput xmlOutput) throws IOException {
		//System.err.println("Writing errors to XML output");
		
		xmlOutput.openTag(ERRORS_ELEMENT_NAME);
		
		// Emit Error elements describing analysis errors
		for (Iterator<AnalysisError> i = errorIterator(); i.hasNext(); ) {
			AnalysisError error = i.next();
			xmlOutput.openTag(BugCollection.ERROR_ELEMENT_NAME);
			
			xmlOutput.openTag(BugCollection.ERROR_MESSAGE_ELEMENT_NAME);
			xmlOutput.writeText(error.getMessage());
			xmlOutput.closeTag(BugCollection.ERROR_MESSAGE_ELEMENT_NAME);
			
			if (error.getExceptionMessage() != null) {
				xmlOutput.openTag(BugCollection.ERROR_EXCEPTION_ELEMENT_NAME);
				xmlOutput.writeText(error.getExceptionMessage());
				xmlOutput.closeTag(BugCollection.ERROR_EXCEPTION_ELEMENT_NAME);
			}
			
			String stackTrace[] = error.getStackTrace();
			if (stackTrace != null) {
				for (int j = 0; j < stackTrace.length; ++j) {
					xmlOutput.openTag(BugCollection.ERROR_STACK_TRACE_ELEMENT_NAME);
					xmlOutput.writeText(stackTrace[j]);
					xmlOutput.closeTag(BugCollection.ERROR_STACK_TRACE_ELEMENT_NAME);
				}
			}
			
			xmlOutput.closeTag(BugCollection.ERROR_ELEMENT_NAME);
		}
		
		// Emit missing classes
		XMLOutputUtil.writeElementList(xmlOutput, MISSING_CLASS_ELEMENT_NAME,
			missingClassIterator());
		
		xmlOutput.closeTag(ERRORS_ELEMENT_NAME);
	}

	private void checkInputStream(InputStream in) throws IOException {
		if (in.markSupported()) {
			byte[] buf = new byte[60];
			in.mark(buf.length);

			int numRead = 0;
			while (numRead < buf.length) {
				int n = in.read(buf, numRead, buf.length - numRead);
				if (n < 0)
					throw new IOException("XML does not contain saved bug data");
				numRead += n;
			}

			in.reset();

			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf)));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("<BugCollection"))
					return;
			}

			throw new IOException("XML does not contain saved bug data");
		}
	}

	/**
	 * Clone all of the BugInstance objects in the source Collection
	 * and add them to the destination Collection.
	 * 
	 * @param dest   the destination Collection
	 * @param source the source Collection
	 */
	public static void cloneAll(Collection<BugInstance> dest, Collection<BugInstance> source) {
		for (Iterator<BugInstance> i = source.iterator(); i.hasNext(); ) {
			BugInstance obj = i.next();
			dest.add((BugInstance) obj.clone());
		}
	}

}

// vim:ts=4
