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

package edu.umd.cs.findbugs.detect;
import edu.umd.cs.findbugs.*;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;

import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class EqStringTest extends BytecodeScanningDetector implements   Constants2 {
    boolean constantOnTOS = false;
    boolean callToInternSeen = false;
    private BugReporter bugReporter;
    // String stringOnTop;

  public EqStringTest(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
  }

  public void visit(JavaClass obj)     {
	super.visit(obj);
	}

    public void visit(Method obj) {
        super.visit(obj);
        constantOnTOS = false;
        callToInternSeen = false;
	}


    public void sawOpcode(int seen) {

	switch (seen) {
	case LDC:
		constantOnTOS = true;
		// stringOnTop = stringConstant;
		return;
 	case INVOKEVIRTUAL:
		if (getRefConstantOperand().equals("java.lang.String.intern : ()Ljava.lang.String;")
		 || getRefConstantOperand().equals("java.lang.String.equals : (Ljava.lang.Object;)Z"))
			callToInternSeen = true;
		break;
 	case IF_ACMPEQ:
	case IF_ACMPNE:
		if (constantOnTOS && !callToInternSeen) 
                  bugReporter.reportBug(new BugInstance("ES_COMPARING_STRINGS_WITH_EQ", NORMAL_PRIORITY)
                        .addClassAndMethod(this)
                        .addSourceLine(this, getPC()));
		break;
	default: 
		break;
	}
	constantOnTOS = false;
	}	
}
