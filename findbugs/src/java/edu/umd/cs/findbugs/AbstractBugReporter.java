/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * An abstract class which provides much of the functionality
 * required of all BugReporter objects.
 */
public abstract class AbstractBugReporter implements BugReporter {

	private int verbosityLevel = NORMAL;
	private int priorityThreshold;
	private HashSet<String> missingClassMessageSet = new HashSet<String>();
	private LinkedList<String> missingClassMessageList = new LinkedList<String>();
	private LinkedList<String> errorMessageList = new LinkedList<String>();
	private List<BugReporterObserver> observerList = new LinkedList<BugReporterObserver>();
	private ProjectStats projectStats = new ProjectStats();

	private static final Pattern missingClassPattern = Pattern.compile("^.*while looking for class ([^:]*):.*$");

	public void setErrorVerbosity(int level) {
		this.verbosityLevel = level;
	}

	public void setPriorityThreshold(int threshold) {
		this.priorityThreshold = threshold;
	}

	// Subclasses must override doReportBug(), not this method.
	public final void reportBug(BugInstance bugInstance) {
		if (bugInstance.getPriority() <= priorityThreshold)
			doReportBug(bugInstance);
	}

	public static String getMissingClassName(ClassNotFoundException ex) {
		String message = ex.getMessage();

		// Try to decode the error message by extracting the class name.
		// BCEL seems to report missing classes in a fairly consistent way.
		Matcher matcher = missingClassPattern.matcher(message);
		if (matcher.matches())
			message = matcher.group(1);

		return message;
	}

	public void reportMissingClass(ClassNotFoundException ex) {
		if (verbosityLevel == SILENT)
			return;

		String message = getMissingClassName(ex);

		if (message.startsWith("["))
			throw new IllegalStateException("Missing array class: " + message);

		if (!missingClassMessageSet.contains(message)) {
			missingClassMessageSet.add(message);
			missingClassMessageList.add(message);
		}
	}

	public void logError(String message) {
		if (verbosityLevel == SILENT)
			return;

		errorMessageList.add(message);
	}

	public void reportQueuedErrors() {
		if (errorMessageList.isEmpty() && missingClassMessageList.isEmpty())
			return;

		beginReport();
		if (!errorMessageList.isEmpty()) {
			reportLine("The following errors occured during analysis:");
			for (Iterator<String> i = errorMessageList.iterator(); i.hasNext(); )
				reportLine("\t" + i.next());
		}
		if (!missingClassMessageList.isEmpty()) {
			reportLine("The following classes needed for analysis were missing:");
			for (Iterator<String> i = missingClassMessageList.iterator(); i.hasNext(); )
				reportLine("\t" + i.next());
		}
		endReport();
	}

	public void addObserver(BugReporterObserver observer) {
		observerList.add(observer);
	}

	public ProjectStats getProjectStats() {
		return projectStats;
	}

	/**
	 * This should be called when a bug is reported by a subclass.
	 */
	protected void notifyObservers(BugInstance bugInstance) {
		Iterator<BugReporterObserver> i = observerList.iterator();
		while (i.hasNext())
			i.next().reportBug(bugInstance);
	}

	/**
	 * Subclasses must override this.
	 * It will be called only for bugs which meet the priority threshold.
	 */
	protected abstract void doReportBug(BugInstance bugInstance);

	public abstract void beginReport();
	public abstract void reportLine(String msg);
	public abstract void endReport();
}

// vim:ts=4
