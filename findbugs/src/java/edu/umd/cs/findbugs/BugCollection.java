/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2005 University of Maryland
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.model.ClassFeatureSet;
import edu.umd.cs.findbugs.xml.XMLOutput;

public interface BugCollection
{
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
	static final String HISTORY_ELEMENT_NAME = "History"; // 0.9.2 and later

	/**
	 * Set the current release name.
	 * 
	 * @param releaseName the current release name
	 */
	public void setReleaseName(String releaseName);
	
	/**
	 * Get the project stats.
	 */
	public ProjectStats getProjectStats();

	/**
	 * Set the analysis timestamp.
	 * 
	 * @param timestamp the analysis timestamp.
	 */
	public void setTimestamp(long timestamp);

	/**
	 * Set the sequence number of the BugCollection.
	 * 
	 * @param sequence the sequence number
	 * @see BugCollection#getSequenceNumber()
	 */
	public void setSequenceNumber(long sequence);

	/**
	 * Get the sequence number of the BugCollection.
	 * This value represents the number of times the user has
	 * analyzed a different version of the application and
	 * updated the historical bug collection using the
	 * UpdateBugCollection class.
	 *  
	 * @return the sequence number
	 */
	public long getSequenceNumber();

	/**
	 * Add an AppVersion representing a version of the analyzed application.
	 * 
	 * @param appVersion the AppVersion
	 */
	public void addAppVersion(AppVersion appVersion);

	/**
	 * Get the current AppVersion.
	 */
	public AppVersion getCurrentAppVersion();

	/**
	 * Get an Iterator over AppVersions defined in the collection.
	 */
	public Iterator<AppVersion> appVersionIterator();
	
	/**
	 * Add a BugInstance to this BugCollection.
	 * This just calls add(bugInstance, true).
	 *
	 * @param bugInstance the BugInstance
	 * @return true if the BugInstance was added, or false if a matching
	 *         BugInstance was already in the BugCollection
	 */
	public boolean add(BugInstance bugInstance);
	
	/**
	 * Add a BugInstance to this BugCollection.
	 *
	 * @param bugInstance      the BugInstance
	 * @param updateActiveTime true if the warning's active time should be updated
	 *                         to include the collection's current time
	 * @return true if the BugInstance was added, or false if a matching
	 *         BugInstance was already in the BugCollection
	 */
	public boolean add(BugInstance bugInstance, boolean updateActiveTime);

	/**
	 * Add an analysis error.
	 *
	 * @param message the error message
	 */
	public void addError(String message);

	/**
	 * Add an analysis error.
	 * 
	 * @param the AnalysisError object to add
	 */
	public void addError(AnalysisError error);

	/**
	 * Add a missing class message.
	 *
	 * @param message the missing class message
	 */
	public void addMissingClass(String message);

	public void setClassFeatureSet(ClassFeatureSet classFeatureSet);

	public void writePrologue(XMLOutput xmlOutput, Project project) throws IOException;
	
	public void writeEpilogue(XMLOutput xmlOutput) throws IOException;
	
	public void clearClassFeatures();

	/**
	 * Read XML data from given file into this object,
	 * populating given Project as a side effect.
	 *
	 * @param fileName name of the file to read
	 * @param project  the Project
	 */
	public void readXML(String fileName, Project project)
    	throws IOException, DocumentException;
	
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
	        throws IOException, DocumentException;

	/**
	 * Write this BugCollection to a file as XML.
	 *
	 * @param fileName the file to write to
	 * @param project  the Project from which the BugCollection was generated
	 */
	public void writeXML(String fileName, Project project) 
		throws IOException;
	
	/**
	 * Write the BugCollection to given output stream as XML.
	 * The output stream will be closed, even if an exception is thrown.
	 *
	 * @param out     the OutputStream to write to
	 * @param project the Project from which the BugCollection was generated
	 */
	public void writeXML(OutputStream out, Project project) throws IOException;

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
	public void writeXML(XMLOutput xmlOutput, Project project) throws IOException;
	
	/**
	 * Return an Iterator over all the BugInstance objects in
	 * the BugCollection.
	 */
	public Iterator<BugInstance> iterator();
	
	/**
	 * Return the Collection storing the BugInstance objects.
	 */
	public Collection<BugInstance> getCollection();
	
	/**
	 * Convert the BugCollection into a dom4j Document object.
	 *
	 * @param project the Project from which the BugCollection was generated
	 * @return the Document representing the BugCollection as a dom4j tree
	 */
	public Document toDocument(Project project);
	
	/**
	 * Create a new empty BugCollection with the same metadata as this one.
	 * 
	 * @return a new empty BugCollection with the same metadata as this one
	 */
	public BugCollection createEmptyCollectionWithMetadata();

}
