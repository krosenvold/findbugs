/*
	* FindBugs Eclipse Plug-in.
	* Copyright (C) 2003 - 2004, Peter Friese
	* Copyright (C) 2005, University of Maryland
	*
	* This library is free software; you can redistribute it and/or
	* modify it under the terms of the GNU Lesser General Public
	* License as published by the Free Software Foundation; either
	* version 2.1 of the License, or (at your option) any later version.
	*
	* This library is distributed in the hope that it will be useful,
	* but WITHOUT ANY WARRANTY; without even the implied warranty of
	* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
	* Lesser General Public License for more details.
	*
	* You should have received a copy of the GNU Lesser General Public
	* License along with this library; if not, write to the Free Software
	* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
	*/

package de.tobject.findbugs.reporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;

/**
	* Creates a FindBugs marker in a runnable window.
	*/
public class MarkerReporter implements IWorkspaceRunnable {
	private final BugInstance bug;
	private final IResource resource;
	private final Set<Integer> lines;
	private final BugCollection collection;
	private static final boolean EXPERIMENTAL_BUGS = false;
	private final Integer primaryLine;

	public MarkerReporter(BugInstance bug, IResource resource, Integer primaryLine,
			Set<Integer> lines,
			BugCollection theCollection, IProject project) {
		this.primaryLine = primaryLine;
		this.lines = lines;
		this.bug = bug;
		this.resource = resource;
		this.collection = theCollection;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		String markerType = getMarkerType();
		if(markerType == null) {
			return;
		}
		addmarker(markerType, primaryLine);
		for (Integer line : lines) {
			// This triggers resource update on IResourceChangeListener's (BugTreeView)
			addmarker(markerType, line);
		}
	}

	private void addmarker(String markerType, Integer line) throws CoreException {
		IMarker marker = resource.createMarker(markerType);

		Map<String, Object> attributes = createMarkerAttributes(marker, line);
		setAttributes(marker, attributes);
	}

	/**
	 * @return null if marker shouldn't be generated
	 */
	private String getMarkerType() {
		String markerType;
		switch (bug.getPriority()) {
		case Priorities.HIGH_PRIORITY:
			markerType = FindBugsMarker.NAME_HIGH;
			break;
		case Priorities.NORMAL_PRIORITY:
			markerType = FindBugsMarker.NAME_NORMAL;
			break;
		case Priorities.LOW_PRIORITY:
			markerType = FindBugsMarker.NAME_LOW;
			break;
		case Priorities.EXP_PRIORITY:
			if (!EXPERIMENTAL_BUGS) {
				return null;
			}
			markerType = FindBugsMarker.NAME_EXPERIMENTAL;
			break;
		case Priorities.IGNORE_PRIORITY:
			FindbugsPlugin.getDefault().logError("Bug with ignore priority ");
			return null;
		default:
			FindbugsPlugin.getDefault().logError(
					"Bug with unknown priority " + bug.getPriority());
			return null;
		}
		return markerType;
	}

	/**
	 * @param marker non null
	 * @param startLine
	 * @return attributes map which should be assigned to the given marker
	 */
	private Map<String, Object> createMarkerAttributes(IMarker marker, Integer startLine) {
		Map<String, Object> attributes = new HashMap<String, Object>(23);
		attributes.put(IMarker.LINE_NUMBER, startLine);
		attributes.put(FindBugsMarker.PRIMARY_LINE, primaryLine);
		attributes.put(FindBugsMarker.BUG_TYPE, bug.getType());
		long seqNum = bug.getFirstVersion();
		if(seqNum == 0) {
			attributes.put(FindBugsMarker.FIRST_VERSION, "-1");
		} else {
			AppVersion theVersion = collection.getAppVersionFromSequenceNumber(seqNum);
			if (theVersion == null) {
				attributes.put(FindBugsMarker.FIRST_VERSION,
						"Cannot find AppVersion: seqnum=" + seqNum
								+ "; collection seqnum="
								+ collection.getSequenceNumber());
			} else {
				attributes.put(FindBugsMarker.FIRST_VERSION, Long
						.toString(theVersion.getTimestamp()));
			}
		}
		try {
			attributes.put(IMarker.MESSAGE, bug
					.getMessageWithPriorityTypeAbbreviation());
		} catch (RuntimeException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Error generating msg for " + bug.getType());
			attributes.put(IMarker.MESSAGE, "??? " + bug.getType());
		}
		attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_WARNING));
		attributes.put(FindBugsMarker.PRIORITY_TYPE, bug.getPriorityTypeString());

		switch (bug.getPriority()) {
		case Priorities.HIGH_PRIORITY:
			attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_HIGH));
			break;
		case Priorities.NORMAL_PRIORITY:
			attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_NORMAL));
			break;
		default:
			attributes.put(IMarker.PRIORITY, Integer.valueOf(IMarker.PRIORITY_LOW));
			break;
		}

		attributes.put(FindBugsMarker.PATTERN_DESCR_SHORT, bug.getBugPattern().getShortDescription());

		// Set unique id of warning, so we can easily refer back
		// to it later: for example, when the user classifies the warning.
		String uniqueId = bug.getInstanceHash();
		if (uniqueId != null) {
			attributes.put(FindBugsMarker.UNIQUE_ID, uniqueId);
		}
		return attributes;
	}

	/**
	 * Set all the attributes to marker in one 'workspace transaction'
	 * @param marker non null
	 * @throws CoreException
	 */
	private void setAttributes(IMarker marker, Map<String, Object> attributes) throws CoreException {
		marker.setAttributes(attributes);
	}

}
