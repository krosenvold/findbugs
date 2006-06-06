package edu.umd.cs.findbugs.bluej.test;

import java.io.IOException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;


public class RunFindbugs {
	public static void main(String args[]) throws IOException, InterruptedException {
		Project findBugsProject = new Project();
		for(String f : args)
			findBugsProject.addFile(f);
		final SortedBugCollection bugs = new SortedBugCollection();
		BugReporter reporter = new MyReporter(bugs);
		
		FindBugs findBugs = new FindBugs(reporter, findBugsProject);
		reporter.setPriorityThreshold(Detector.NORMAL_PRIORITY);
	    
		
		System.setProperty("findbugs.home", "/Users/pugh/Documents/eclipse-3.2/workspace/findbugs");
		System.setProperty("findbugs.jaws", "true");
		
		findBugs.execute();
		for(BugInstance bug : bugs.getCollection()) {
			SourceLineAnnotation source = bug.getPrimarySourceLineAnnotation();

			System.out.println(bug.getPrimarySourceLineAnnotation());
			System.out.println(bug.getMessageWithoutPrefix());
			System.out.println(bug.getBugPattern().getDetailHTML());
			
		}

			
	}

}
