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

public class XMLBugReporter extends TextUIBugReporter {
	private SortedBugCollection bugCollection = new SortedBugCollection();
	private Project project;

	public XMLBugReporter(Project project) {
		this.project = project;
	}

	public void logError(String message) {
		bugCollection.addError(message);
		super.logError(message);
	}

	public void reportMissingClass(ClassNotFoundException ex) {
		bugCollection.addMissingClass(getMissingClassName(ex));
		super.reportMissingClass(ex);
	}

	public void doReportBug(BugInstance bugInstance) {
		if (bugCollection.add(bugInstance))
			notifyObservers(bugInstance);
	}

	public void finish() {
		try {
			bugCollection.writeXML(outputStream, project);
		} catch (Exception e) {
			logError("Couldn't write XML output: " + e.toString());
		}
	}
}

// vim:ts=4
