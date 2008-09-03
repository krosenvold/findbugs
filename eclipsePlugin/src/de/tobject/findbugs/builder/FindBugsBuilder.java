/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
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

package de.tobject.findbugs.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import de.tobject.findbugs.FindbugsPlugin;

/**
 * The <code>FindBugsBuilder</code> performs a FindBugs run on a subset of the
 * current project. It will either check all classes in a project or just the
 * ones just having been modified.
 *
 * @author Peter Friese
 * @version 1.0
 * @since 25.9.2003
 * @see IncrementalProjectBuilder
 */
public class FindBugsBuilder extends IncrementalProjectBuilder {

	/** Controls debugging. */
	public static boolean DEBUG;

	/**
	 * Run the builder.
	 *
	 * @see IncrementalProjectBuilder#build
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		monitor.subTask("Running FindBugs...");
		switch (kind) {
		case IncrementalProjectBuilder.FULL_BUILD: {
			if (FindbugsPlugin.getUserPreferences(getProject()).isRunAtFullBuild()){
				if (DEBUG) {
					System.out.println("FULL BUILD");
				}
				doBuild(args, monitor, kind);
			} else {
				// TODO probably worth to cleanup? MarkerUtil.removeMarkers(getProject());
			}
			break;
		}
		case IncrementalProjectBuilder.INCREMENTAL_BUILD: {
			if (DEBUG) {
				System.out.println("INCREMENTAL BUILD");
			}
			doBuild(args, monitor, kind);
			break;
		}
		case IncrementalProjectBuilder.AUTO_BUILD: {
			if (DEBUG) {
				System.out.println("AUTO BUILD");
			}
			doBuild(args, monitor, kind);
			break;
		}
		}
		return null;
	}

	/**
	 * Performs the build process. This method gets all files in the current project and
	 * has a <code>FindBugsVisitor</code> run on them.
	 *
	 * @param args
	 *            A <code>Map</code> containing additional build parameters.
	 * @param monitor
	 *            The <code>IProgressMonitor</code> displaying the build progress.
	 * @param kind
	 *            kind the kind of build being requested, see IncrementalProjectBuilder
	 * @throws CoreException
	 */
	private void doBuild(final Map<?,?> args, final IProgressMonitor monitor, int kind) throws CoreException {
		boolean incremental = (kind != IncrementalProjectBuilder.FULL_BUILD);
		IProject project = getProject();
		FindBugsWorker worker = new FindBugsWorker(project, monitor);
		List<IResource> files;
		if(incremental) {
			IResourceDelta resourceDelta = getDelta(project);
			if (shouldRunIncremental(resourceDelta)) {
				files = ResourceUtils.collectIncremental(resourceDelta);
			} else {
				files = new ArrayList<IResource>();
				files.add(project);
			}
		} else {
			files = new ArrayList<IResource>();
			files.add(project);
		}
		worker.work(files);
	}

	/**
	 * @return true if we should run incremental build (no config change OR full build is
	 *         disabled)
	 */
	private boolean shouldRunIncremental(IResourceDelta resourceDelta)
			throws CoreException {
		boolean configUnchanged = resourceDelta != null
				&& resourceDelta.findMember(new Path(".project")) == null
				&& resourceDelta.findMember(new Path(".classpath")) == null
				&& resourceDelta.findMember(new Path(".fbprefs")) == null;
		return configUnchanged
				|| !FindbugsPlugin.getUserPreferences(getProject()).isRunAtFullBuild();
	}
}
