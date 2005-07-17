/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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
package edu.umd.cs.findbugs.detect;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class QuestionableBooleanAssignment extends BytecodeScanningDetector implements StatelessDetector, Constants2
{
	public static final int SEEN_NOTHING = 0;
	public static final int SEEN_ICONST_0_OR_1 = 1;
	public static final int SEEN_DUP = 2;
	public static final int SEEN_ISTORE = 3;
	
	private BugReporter bugReporter;
	private int state;
	
	public QuestionableBooleanAssignment(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public void visitCode(Code obj) {
		state = SEEN_NOTHING;
		super.visitCode(obj);
	}
	
	public void sawOpcode(int seen) {
		switch (state) {
			case SEEN_NOTHING:
				if ((seen == ICONST_1) || (seen == ICONST_0))
					state = SEEN_ICONST_0_OR_1;
			break;
			
			case SEEN_ICONST_0_OR_1:
				if (seen == DUP)
					state = SEEN_DUP;
				else
					state = SEEN_NOTHING;
			break;
			
			case SEEN_DUP:
				if (((seen >= ISTORE_0) && (seen < ISTORE_3)) || (seen == ISTORE))
					state = SEEN_ISTORE;
				else
					state = SEEN_NOTHING;
			break;
			
			case SEEN_ISTORE:
				if (seen == IFEQ)
				{
					bugReporter.reportBug( new BugInstance( this, "QBA_QUESTIONABLE_BOOLEAN_ASSIGNMENT", LOW_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this));
				}
				state = SEEN_NOTHING;
			break;
		}
	}
}
