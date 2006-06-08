package edu.umd.cs.findbugs.bluej;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.TextUIBugReporter;


public class MyReporter extends TextUIBugReporter {
	BugCollection bugs;
	
	
	MyReporter(BugCollection bugs) {
		this.bugs = bugs;
	}
	@Override
	protected void doReportBug(BugInstance bugInstance) {
		if (bugInstance.getBugPattern().getCategory().equals("CORRECTNESS"))
			bugs.add(bugInstance);
		
	}

	public void finish() {
		// TODO Auto-generated method stub
		
	}

	public void observeClass(JavaClass javaClass) {
		// TODO Auto-generated method stub
		
	}


}
