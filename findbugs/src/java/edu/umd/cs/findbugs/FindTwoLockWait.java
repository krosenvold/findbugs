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
import java.io.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

public class FindTwoLockWait extends CFGBuildingDetector implements Detector {

	private BugReporter bugReporter;
	private AnyLockCountAnalysis analysis;
	private Dataflow<LockCount> dataflow;

	public FindTwoLockWait(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public boolean preScreen(MethodGen mg) {
		ConstantPoolGen cpg = mg.getConstantPool();

		int lockCount = mg.isSynchronized() ? 1 : 0;
		boolean sawWait = false;

		InstructionHandle handle = mg.getInstructionList().getStart();
		while (handle != null && !(lockCount >= 2 && sawWait)) {
			Instruction ins = handle.getInstruction();
			if (ins instanceof MONITORENTER)
				++lockCount;
			else if (ins instanceof INVOKEVIRTUAL) {
				INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;
				if (inv.getMethodName(cpg).equals("wait"))
					sawWait = true;
			}

			handle = handle.getNext();
		}

		return lockCount >= 2 && sawWait;
	}

	public void visitCFG(CFG cfg, MethodGen methodGen) {
		try {
			analysis = new AnyLockCountAnalysis(methodGen, null);
			dataflow = new Dataflow<LockCount>(cfg, analysis);
			dataflow.execute();

			visitCFGInstructions(cfg, methodGen);
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException(e.getMessage());
		}
	}

	public void visitInstruction(InstructionHandle handle, BasicBlock bb, MethodGen methodGen) {
		try {
			ConstantPoolGen cpg = methodGen.getConstantPool();
	
			if (isWait(handle, cpg)) {
				LockCount count = analysis.createFact();
				analysis.transfer(bb, handle, dataflow.getStartFact(bb), count);
				if (count.getCount() > 1) {
					// A wait with multiple locks held?
					bugReporter.reportBug(new BugInstance("2LW_TWO_LOCK_WAIT", NORMAL_PRIORITY)
						.addClass(getJavaClass())
						.addMethod(methodGen)
						.addSourceLine(methodGen, handle));
				}
			}
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException(e.getMessage());
		}
	}

	private boolean isWait(InstructionHandle handle, ConstantPoolGen cpg) {
		Instruction ins = handle.getInstruction();
		if (!(ins instanceof INVOKEVIRTUAL))
			return false;
		INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

		String methodName = inv.getMethodName(cpg);
		String methodSig = inv.getSignature(cpg);

		return methodName.equals("wait") &&
			(methodSig.equals("()V") || methodSig.equals("(J)V") || methodSig.equals("(JI)V"));
	}

	public void report(java.io.PrintStream out) {
	}
}

// vim:ts=4
