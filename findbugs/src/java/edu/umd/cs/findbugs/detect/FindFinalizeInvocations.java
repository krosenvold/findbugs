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

package edu.umd.cs.findbugs.detect;
import edu.umd.cs.findbugs.*;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;

import edu.umd.cs.pugh.visitclass.DismantleBytecode;
import edu.umd.cs.pugh.visitclass.Constants2;

public class FindFinalizeInvocations extends BytecodeScanningDetector implements   Constants2 {
    private static final boolean DEBUG = Boolean.getBoolean("ffi.debug");

   private BugReporter bugReporter;

   public FindFinalizeInvocations(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
   }

   boolean sawSuperFinalize;
   public void visit(Method obj) {
		if (DEBUG) System.out.println("FFI: visiting " + betterMethodName);
		if (methodName.equals("finalize") 
			&& methodSig.equals("()V")
			&& (obj.getAccessFlags() & (ACC_PUBLIC )) != 0
			) 
			bugReporter.reportBug(new BugInstance("FI_PUBLIC_SHOULD_BE_PROTECTED", NORMAL_PRIORITY).addClassAndMethod(this));
		}
   public void visit(Code obj) {
		sawSuperFinalize = false;
		super.visit(obj);
		if (!methodName.equals("finalize") 
			|| !methodSig.equals("()V")) return;
		String overridesFinalizeIn 
			= Lookup.findSuperImplementor(betterClassName, 
						"finalize",
						"()V",
						bugReporter);
		boolean superHasNoFinalizer = overridesFinalizeIn.equals("java.lang.Object");
		// System.out.println("superclass: " + superclassName);
		    if (obj.getCode().length == 1)	 {
			if (superHasNoFinalizer)
				bugReporter.reportBug(new BugInstance("FI_EMPTY", NORMAL_PRIORITY).addClassAndMethod(this));
			else
				bugReporter.reportBug(new BugInstance("FI_NULLIFY_SUPER", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addClass(overridesFinalizeIn));
			}
		    else if (obj.getCode().length == 5 && sawSuperFinalize) 
			bugReporter.reportBug(new BugInstance("FI_USELESS", NORMAL_PRIORITY).addClassAndMethod(this));
		    else if (!sawSuperFinalize && !superHasNoFinalizer)
			bugReporter.reportBug(new BugInstance("FI_MISSING_SUPER_CALL", NORMAL_PRIORITY).addClassAndMethod(this)
					.addClass(overridesFinalizeIn)
			);
		}
   public void sawOpcode(int seen) {
	if (seen == INVOKEVIRTUAL && nameConstant.equals("finalize"))
		bugReporter.reportBug(new BugInstance("FI_EXPLICIT_INVOCATION", NORMAL_PRIORITY)
			.addClassAndMethod(this)
			.addCalledMethod(this).describe("METHOD_CALLED")
			.addSourceLine(this, PC));
	if (seen == INVOKESPECIAL && nameConstant.equals("finalize")) 
		sawSuperFinalize = true;
	}
}
