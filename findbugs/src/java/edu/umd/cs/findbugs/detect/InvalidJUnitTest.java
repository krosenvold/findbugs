/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004 University of Maryland
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class InvalidJUnitTest extends BytecodeScanningDetector implements Constants2 {

	private static final int SEEN_NOTHING = 0;
	private static final int SEEN_ALOAD_0 = 1;

	private BugReporter bugReporter;

	private MethodAnnotation setUpAnnotation;
	private MethodAnnotation tearDownAnnotation;
	private String methodName;
	private boolean validClass;
	private boolean validMethod;
	private boolean sawSetUp;
	private boolean sawTearDown;
	private int state;

	public InvalidJUnitTest(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visit(JavaClass obj) {
		try {
			setUpAnnotation = null;
			tearDownAnnotation = null;
			validClass = false;
			validMethod = false;
			sawSetUp = false;
			sawTearDown = false;
			JavaClass[] superClasses = obj.getSuperClasses();
			for (int i = 0; i < superClasses.length; i++) {
				JavaClass sc = superClasses[i];
				if (sc.getClassName().equals("junit.framework.TestCase")) {
					validClass = true;
					super.visit(obj);
					break;
				}
			}
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
	}

	public void visitAfter(JavaClass obj) {
		if ((setUpAnnotation != null) && !sawSetUp) {
			bugReporter.reportBug(new BugInstance("IJU_SETUP_NO_SUPER", NORMAL_PRIORITY)
			        .addClass(this)
			        .addMethod(setUpAnnotation));

		}
		if ((tearDownAnnotation != null) && !sawTearDown) {
			bugReporter.reportBug(new BugInstance("IJU_TEARDOWN_NO_SUPER", NORMAL_PRIORITY)
			        .addClass(this)
			        .addMethod(tearDownAnnotation));

		}
	}

	public void visit(Method obj) {
		if (!validClass)
			return;
		validMethod = false;
		methodName = obj.getName();
		if (methodName.equals("setUp") || methodName.equals("tearDown")) {
			if (methodName.equals("setUp"))
				setUpAnnotation = MethodAnnotation.fromVisitedMethod(this);
			else if (methodName.equals("tearDown"))
				tearDownAnnotation = MethodAnnotation.fromVisitedMethod(this);
			validMethod = true;
			state = SEEN_NOTHING;
			super.visit(obj);
		} else if (methodName.equals("suite") && !obj.isStatic())
			bugReporter.reportBug(new BugInstance("IJU_SUITE_NOT_STATIC", NORMAL_PRIORITY)
			        .addClass(this)
			        .addMethod(MethodAnnotation.fromVisitedMethod(this)));
	}

	public void sawOpcode(int seen) {
		if (!validClass || !validMethod)
			return;

		// System.out.println( OPCODE_NAMES[seen] );

		switch (state) {
		case SEEN_NOTHING:
			if (seen == ALOAD_0)
				state = SEEN_ALOAD_0;
			break;

		case SEEN_ALOAD_0:
			if (seen == INVOKESPECIAL)
				System.out.println(getNameConstantOperand());
			if ((seen == INVOKESPECIAL)
			        && (getNameConstantOperand().equals(methodName))
			        && (getMethodSig().equals("()V"))) {
				if (methodName.equals("setUp"))
					sawSetUp = true;
				else if (methodName.equals("tearDown"))
					sawTearDown = true;
			}
			state = SEEN_NOTHING;
			break;
		}
	}
}

// vim:ts=4
