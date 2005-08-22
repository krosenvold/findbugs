/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005, Tom Truscott <trt@unx.sas.com>
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
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;

public class TestingGround extends BytecodeScanningDetector  {

	private static final boolean active 
		 = Boolean.getBoolean("findbugs.tg.active");
	

	BugReporter bugReporter;

	OpcodeStack stack = new OpcodeStack();
	public TestingGround(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visit(JavaClass obj) {
	}

	public void visit(Method obj) {
	}

	public void visit(Code obj) {
		// unless active, don't bother dismantling bytecode
		if (active) {
			// System.out.println("TestingGround: " + getFullyQualifiedMethodName());
                	stack.resetForMethodEntry(this);
			super.visit(obj);
		}
	}


	public void sawOpcode(int seen) {
		if (seen == INVOKESTATIC
			&& getNameConstantOperand().equals("forName")
			&& getClassConstantOperand().equals("java/lang/Class")
			&& getSigConstantOperand().equals("(Ljava/lang/String;)Ljava/lang/Class;"))
			if (stack.getStackDepth() == 0) 
				System.out.println("empty stack");
			else {

			OpcodeStack.Item item = stack.getStackItem(0);
			Object constantValue = item.getConstant();
			if (constantValue != null
				&& constantValue instanceof String)
				System.out.println("XXYYZ: " + getFullyQualifiedMethodName() + " Class.forName("+constantValue+")");
			else
				System.out.println("XXYYZ: " + getFullyQualifiedMethodName() + " Class.forName(???)");

			}

		stack.sawOpcode(this,seen);
	}
}
