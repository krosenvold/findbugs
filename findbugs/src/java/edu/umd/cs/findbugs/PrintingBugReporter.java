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

package edu.umd.cs.findbugs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.config.CommandLine;


/**
 * A simple BugReporter which simply prints the formatted message
 * to the output stream.
 */
public class PrintingBugReporter extends TextUIBugReporter {
	private HashSet<BugInstance> seenAlready = new HashSet<BugInstance>();

	public void observeClass(JavaClass javaClass) {
		// Don't need to do anything special, since we won't be
		// reporting statistics.
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
	
	class PrintingCommandLine extends CommandLine {

		public PrintingCommandLine() {
			addSwitch("-longBugCodes", "use long bug codes when generating text");
		}
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			if (option.equals("-longBugCodes"))
				setUseLongBugCodes(true);
			
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String, java.lang.String)
		 */
		@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			// TODO Auto-generated method stub
			
		}
		
	}
	public static void main(String[] args) throws Exception {

		PrintingBugReporter reporter = new PrintingBugReporter();
		PrintingCommandLine commandLine = reporter.new PrintingCommandLine();

		int argCount = commandLine.parse(args, 0, 2, "Usage: " + PrintingCommandLine.class.getName()
				+ " [options] [<xml results> [<test results]] ");

		// Load plugins, in order to get message files
		DetectorFactoryCollection.instance();
		
		
		SortedBugCollection bugCollection = new SortedBugCollection();
		if (argCount < args.length)
			bugCollection.readXML(args[argCount++], new Project());
		else
			bugCollection.readXML(System.in, new Project());
		
		if (argCount < args.length)
			reporter.setOutputStream(new PrintStream(args[argCount++]));
		
		for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
			BugInstance warning = i.next();
			reporter.printBug(warning);
		}
		
	}
}

// vim:ts=4
