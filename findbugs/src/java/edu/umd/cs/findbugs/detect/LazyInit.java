/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
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

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ByteCodePatternDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DominatorsAnalysis;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.LockSet;
import edu.umd.cs.findbugs.ba.PostDominatorsAnalysis;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.bcp.Binding;
import edu.umd.cs.findbugs.ba.bcp.BindingSet;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePattern;
import edu.umd.cs.findbugs.ba.bcp.ByteCodePatternMatch;
import edu.umd.cs.findbugs.ba.bcp.FieldVariable;
import edu.umd.cs.findbugs.ba.bcp.IfNull;
import edu.umd.cs.findbugs.ba.bcp.Load;
import edu.umd.cs.findbugs.ba.bcp.PatternElementMatch;
import edu.umd.cs.findbugs.ba.bcp.Store;
import edu.umd.cs.findbugs.ba.bcp.Wild;

/*
 * Look for lazy initialization of fields which
 * are not volatile.  This is quite similar to checking for
 * double checked locking, except that there is no lock.
 *
 * @author David Hovemeyer
 */

public class LazyInit extends ByteCodePatternDetector implements StatelessDetector {
	private BugReporter bugReporter;

	private static final boolean DEBUG = Boolean.getBoolean("lazyinit.debug");

	/**
	 * The pattern to look for.
	 */
	private static ByteCodePattern pattern = new ByteCodePattern();

	static {
		pattern
		        .add(new Load("f", "val").label("start"))
		        .add(new IfNull("val"))
		        .add(new Wild(1, 1).label("createObject"))
		        .add(new Store("f", pattern.dummyVariable()).label("end").dominatedBy("createObject"));
	}

	public LazyInit(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public BugReporter getBugReporter() {
		return bugReporter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public ByteCodePattern getPattern() {
		return pattern;
	}

	public boolean prescreen(Method method, ClassContext classContext) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);

		// The pattern requires a get/put pair accessing the same field.
		if (!(bytecodeSet.get(Constants.GETSTATIC) && bytecodeSet.get(Constants.PUTSTATIC)) &&
		        !(bytecodeSet.get(Constants.GETFIELD) && bytecodeSet.get(Constants.PUTFIELD)))
			return false;

		// If the method is synchronized, then we'll assume that
		// things are properly synchronized
		if (method.isSynchronized())
			return false;

		return true;
	}

	public void reportMatch(ClassContext classContext, Method method, ByteCodePatternMatch match)
	        throws CFGBuilderException, DataflowAnalysisException {
		JavaClass javaClass = classContext.getJavaClass();
		MethodGen methodGen = classContext.getMethodGen(method);
		CFG cfg = classContext.getCFG(method);

		try {
			// Get the variable referenced in the pattern instance.
			BindingSet bindingSet = match.getBindingSet();
			Binding binding = bindingSet.lookup("f");

			// Look up the field as an XField.
			// If it is volatile, then the instance is not a bug.
			FieldVariable field = (FieldVariable) binding.getVariable();
			XField xfield =
			        Hierarchy.findXField(field.getClassName(), field.getFieldName(), field.getFieldSig());
			if (xfield == null || (xfield.getAccessFlags() & Constants.ACC_VOLATILE) != 0)
				return;

			// XXX: for now, ignore lazy initialization of instance fields
			if (!xfield.isStatic())
				return;

			// Definitely ignore synthetic class$ fields
			if (xfield.getFieldName().startsWith("class$") || xfield.getFieldName().startsWith("array$")) {
				if (DEBUG) System.out.println("Ignoring field " + xfield.getFieldName());
				return;
			}

			// Ignore non-reference fields
			if (!xfield.getSignature().startsWith("[")) {
				if (DEBUG) System.out.println("Ignoring non-reference field " + xfield.getFieldName());
				return;
			}

			// TODO:
			// - Strings are safe to pass by data race in 1.5

			// Get locations matching the beginning of the object creation,
			// and the final field store.
			PatternElementMatch createBegin = match.getFirstLabeledMatch("createObject");
			PatternElementMatch store = match.getFirstLabeledMatch("end");

			// Get all blocks
			//
			//   (1) dominated by the wildcard instruction matching
			//       the beginning of the instructions creating the object, and
			//   (2) postdominated by the field store
			//
			// Exception edges are not considered in computing dominators/postdominators.
			// We will consider this to be all of the code that creates
			// the object.
			DominatorsAnalysis domAnalysis =
			        classContext.getNonExceptionDominatorsAnalysis(method);
			PostDominatorsAnalysis postDomAnalysis =
			        classContext.getNonExceptionPostDominatorsAnalysis(method);
			BitSet extent = domAnalysis.getAllDominatedBy(createBegin.getBasicBlock());
			extent.and(postDomAnalysis.getAllDominatedBy(store.getBasicBlock()));
			//System.out.println("Extent: " + extent);
			if (DEBUG) System.out.println("Object creation extent: " + extent);

			// Check all instructions in the object creation extent
			//
			//   (1) to determine the common lock set, and
			//   (2) to check for NEW and Invoke instructions that might create an object
			//
			// We ignore matches where a lock is held consistently,
			// or if the extent does not appear to create a new object.
			LockDataflow lockDataflow = classContext.getLockDataflow(method);
			LockSet lockSet = null;
			boolean sawNEW = false, sawINVOKE = false;
			for (Iterator<BasicBlock> i = cfg.getBlocks(extent).iterator(); i.hasNext();) {
				BasicBlock block = i.next();
				for (Iterator<InstructionHandle> j = block.instructionIterator(); j.hasNext();) {
					InstructionHandle handle = j.next();

					Location location = new Location(handle, block);

					// Keep track of whether we saw any instructions
					// that might actually have created a new object.
					Instruction ins = handle.getInstruction();
					if (ins instanceof NEW)
						sawNEW = true;
					else if (ins instanceof InvokeInstruction)
						sawINVOKE = true;

					// Compute lock set intersection for all matched instructions.
					LockSet insLockSet = lockDataflow.getFactAtLocation(location);
					if (lockSet == null) {
						lockSet = new LockSet();
						lockSet.copyFrom(insLockSet);
					} else
						lockSet.intersectWith(insLockSet);
				}
			}
			if (!(sawNEW || sawINVOKE))
				return;
			if (lockSet == null) throw new IllegalStateException();
			if (!lockSet.isEmpty())
				return;

			// Compute the priority:
			//  - ignore lazy initialization of instance fields
			//  - when it's done in a public method, emit a high priority warning
			//  - protected or default access method, emit a medium priority warning
			//  - otherwise, low priority
			int priority = LOW_PRIORITY;
			boolean isDefaultAccess =
			        (method.getAccessFlags() & (Constants.ACC_PUBLIC | Constants.ACC_PRIVATE | Constants.ACC_PROTECTED)) == 0;
			if (method.isPublic())
				priority = NORMAL_PRIORITY;
			else if (method.isProtected() || isDefaultAccess)
				priority = NORMAL_PRIORITY;

			// Report the bug.
			InstructionHandle start = match.getLabeledInstruction("start");
			InstructionHandle end = match.getLabeledInstruction("end");
			String sourceFile = javaClass.getSourceFileName();
			bugReporter.reportBug(new BugInstance(this, "LI_LAZY_INIT_STATIC", priority)
			        .addClassAndMethod(methodGen, sourceFile)
			        .addField(xfield).describe("FIELD_ON")
			        .addSourceLine(methodGen, sourceFile, start, end));
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return;
		}
	}

}

// vim:ts=4
