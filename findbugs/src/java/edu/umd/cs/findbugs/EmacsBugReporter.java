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
import org.apache.bcel.classfile.JavaClass;

/**
 * A simple BugReporter which simply prints the formatted message
 * to the output stream.
 *
 * @author David Li
 */
public class EmacsBugReporter extends TextUIBugReporter {

	private HashSet<BugInstance> seenAlready = new HashSet<BugInstance>();

        private Project project;

        EmacsBugReporter (Project project) {
                super();
                this.project = project;
        }

	public void observeClass(JavaClass javaClass) {
		// Don't need to do anything special, since we won't be
		// reporting statistics.
	}

	protected void printBug(BugInstance bugInstance) {
                SourceLineAnnotation line = 
                        bugInstance.getPrimarySourceLineAnnotation();
                if (line == null) {
                        outputStream.print(bugInstance.getMessage());
                } else {
			String fullPath = project.getSourceFilePath(line.getSourceFile());
			if (fullPath == null) {
				// Source path lookup failed; just print out
				// our guess at the relative path.
				String pkgName = line.getPackageName();
				if (pkgName.equals("")) {
					fullPath = line.getSourceFile();
				} else {
					fullPath = pkgName.replace('.', '/') + "/" + line.getSourceFile();
				}
			}
                        outputStream.print(fullPath + ":" 
                                           + line.getStartLine() + ":" 
                                           + line.getEndLine() + " "
                                           + bugInstance.getMessage());
                }
            
                switch(bugInstance.getPriority()) {
		case Detector.LOW_PRIORITY:
			outputStream.print(" (L) ");
			break;
		case Detector.NORMAL_PRIORITY:
			outputStream.print(" (M) ");
			break;
		case Detector.HIGH_PRIORITY:
			outputStream.print(" (H) ");
			break;
		}

                outputStream.println();
	}

	protected void doReportBug(BugInstance bugInstance) {
		if (seenAlready.add(bugInstance)) {
			printBug(bugInstance);
			notifyObservers(bugInstance);
		}
	}

	public void finish() {
		outputStream.close();
	}
}

/*
 * Local Variables:
 * eval: (c-set-style "bsd")
 * End:
 */ 
