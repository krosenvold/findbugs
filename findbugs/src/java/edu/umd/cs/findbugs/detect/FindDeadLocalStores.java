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

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.StoreInstruction;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisException;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LiveLocalStoreAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ml.DeadLocalStoreHeuristics;
import edu.umd.cs.findbugs.ml.HeuristicPropertySet;
import edu.umd.cs.findbugs.ml.HeuristicPropertySetSchema;

/**
 * Find dead stores to local variables.
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public class FindDeadLocalStores implements Detector {
	
	private static final boolean DEBUG = Boolean.getBoolean("fdls.debug");
	
	// Define the name of the property that is used to exclude named local variables
	// from Dead Local Storage detection...
	private static final String FINDBUGS_EXCLUDED_LOCALS_PROP_NAME = "findbugs.dls.exclusions";
	
	// Define a collection of excluded local variables...
	private static final Set<String> EXCLUDED_LOCALS = new HashSet<String>();
	
	private static final boolean DO_EXCLUDE_LOCALS =
		System.getProperty(FINDBUGS_EXCLUDED_LOCALS_PROP_NAME) != null;
	static {
		// Get the value of the property...
		String exclLocalsProperty = System.getProperty(FINDBUGS_EXCLUDED_LOCALS_PROP_NAME);
		
		// If we have one, then split its contents into a table...
		if (exclLocalsProperty != null) {        	
			EXCLUDED_LOCALS.addAll( (List<String>)Arrays.asList(exclLocalsProperty.split(",")));
			EXCLUDED_LOCALS.remove("");
		}
	}   
	
	//private static final Set<String> classesAlreadyReportedOn = new HashSet<String>();
	/**
	 * Opcodes of instructions that load constant values that
	 * often indicate defensive programming.
	 */
	private static final BitSet defensiveConstantValueOpcodes = new BitSet();
	static {
		defensiveConstantValueOpcodes.set(Constants.DCONST_0);
		defensiveConstantValueOpcodes.set(Constants.DCONST_1);
		defensiveConstantValueOpcodes.set(Constants.FCONST_0);
		defensiveConstantValueOpcodes.set(Constants.FCONST_1);
		defensiveConstantValueOpcodes.set(Constants.ACONST_NULL);
		defensiveConstantValueOpcodes.set(Constants.ICONST_0);
		defensiveConstantValueOpcodes.set(Constants.ICONST_1);
	}
	
	private BugReporter bugReporter;
	private HeuristicPropertySet propertySet;
	
	public FindDeadLocalStores(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.propertySet = new HeuristicPropertySet();
		if (DEBUG) System.out.println("Debugging FindDeadLocalStores detector");
	}
	
	public void setAnalysisContext(AnalysisContext analysisContext) {
	}
	
	private boolean prescreen(ClassContext classContext, Method method) {
		return true;
	}
	
	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();
		
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;
			
			if (!prescreen(classContext, method))
				continue;
			
			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				throw new AnalysisException("FindDeadLocalStores caught exception", methodGen, e);
			} catch (CFGBuilderException e) {
				throw new AnalysisException("FindDeadLocalStores caught exception", methodGen, e);
			}
		}
	}
	
	private static final int KILLED_BY_SUBSEQUENT_STORE = 0;
	private static final int DEFENSIVE_CONSTANT_OPCODE = 1;
	private static final int EXCEPTION_HANDLER = 2;
	private static final int DEAD_INCREMENT = 3;
	private static final int SINGLE_DEAD_INCREMENT = 4;
	private static final int DEAD_OBJECT_STORE = 5;
	private static final int TWO_STORES_MULTIPLE_LOADS = 6;
	private static final int SINGLE_STORE = 7;
	private static final int NO_LOADS = 8;
	private static final int PARAM_DEAD_ON_ENTRY = 9;
	private static final String[] boolPropertyNameList = {
		DeadLocalStoreHeuristics.KILLED_BY_SUBSEQUENT_STORE,
		DeadLocalStoreHeuristics.DEFENSIVE_CONSTANT_OPCODE,
		DeadLocalStoreHeuristics.EXCEPTION_HANDLER,
		DeadLocalStoreHeuristics.DEAD_INCREMENT,
		DeadLocalStoreHeuristics.SINGLE_DEAD_INCREMENT,
		DeadLocalStoreHeuristics.DEAD_OBJECT_STORE,
		DeadLocalStoreHeuristics.TWO_STORES_MULTIPLE_LOADS,
		DeadLocalStoreHeuristics.SINGLE_STORE,
		DeadLocalStoreHeuristics.NO_LOADS,
		DeadLocalStoreHeuristics.PARAM_DEAD_ON_ENTRY,
	};
	private static final HeuristicPropertySetSchema schema =
		new HeuristicPropertySetSchema(boolPropertyNameList);
	
	private void analyzeMethod(ClassContext classContext, Method method)
			throws DataflowAnalysisException, CFGBuilderException {
		
		JavaClass javaClass = classContext.getJavaClass();
		
		Dataflow<BitSet, LiveLocalStoreAnalysis> llsaDataflow =
			classContext.getLiveLocalStoreDataflow(method);
		
		int numLocals = method.getCode().getMaxLocals();
		int [] localStoreCount = new int[numLocals];
		int [] localLoadCount = new int[numLocals];
		int [] localIncrementCount = new int[numLocals];
		MethodGen methodGen = classContext.getMethodGen(method);
		CFG cfg = classContext.getCFG(method);
		BitSet liveStoreSetAtEntry = llsaDataflow.getAnalysis().getResultFact(cfg.getEntry());
		BitSet complainedAbout = new BitSet();
		
		// Get number of locals that are parameters.
		int localsThatAreParameters = method.getArgumentTypes().length;
		if (!method.isStatic()) localsThatAreParameters++;
		
		// Scan method to determine number of loads, stores, and increments
		// of local variables.
		countLocalStoresLoadsAndIncrements(
				localStoreCount, localLoadCount, localIncrementCount, cfg);
		
		// Scan method for
		// - dead stores
		// - stores to parameters that are dead upon entry to the method
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			
			// Skip any instruction which is not a store
			if (!isStore(location))
				continue;
			
			// Heuristic: exception handler blocks often contain
			// dead stores generated by the compiler.
			propertySet.setBoolProperty(
					EXCEPTION_HANDLER, location.getBasicBlock().isExceptionHandler());
			
			IndexedInstruction ins = (IndexedInstruction) location.getHandle().getInstruction();
			int local = ins.getIndex();
			
			// Heuristic: name of local variable.
			checkLocalVariableName(
					method.getLocalVariableTable(),
					local,
					location.getHandle().getPosition());
			
			// Is this a store to a parameter which was dead on entry to the method?
			boolean parameterThatIsDeadAtEntry = local < localsThatAreParameters
			&& !llsaDataflow.getAnalysis().isStoreAlive(liveStoreSetAtEntry, local);
			if (parameterThatIsDeadAtEntry && !complainedAbout.get(local)) {
				BugInstance bugInstance = new BugInstance(this, "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN", NORMAL_PRIORITY)
					.addClassAndMethod(methodGen, javaClass.getSourceFileName())
					.addSourceLine(methodGen, javaClass.getSourceFileName(), location.getHandle());
				bugReporter.reportBug(bugInstance);
				complainedAbout.set(local);
			}
			
			// Get live stores at this instruction.
			// Note that the analysis also computes which stores were
			// killed by a subsequent unconditional store.
			BitSet liveStoreSet = llsaDataflow.getAnalysis().getFactAtLocation(location);
			
			// Is store alive?
			if (llsaDataflow.getAnalysis().isStoreAlive(liveStoreSet, local))
				continue;
			// Store is dead
			
			// Ignore assignments that were killed by a subsequent assignment.
			propertySet.setBoolProperty(
					KILLED_BY_SUBSEQUENT_STORE,
					llsaDataflow.getAnalysis().killedByStore(liveStoreSet, local));
			
			// Ignore dead assignments of null and 0.
			// These often indicate defensive programming.
			InstructionHandle prev = location.getBasicBlock().getPredecessorOf(location.getHandle());
			propertySet.setBoolProperty(
					DEFENSIVE_CONSTANT_OPCODE,
					prev != null && defensiveConstantValueOpcodes.get(prev.getInstruction().getOpcode())
			);
			
			if (ins instanceof IINC) {
				// special handling of IINC
				
				propertySet.setBoolProperty(DEAD_INCREMENT, true);
				if (localIncrementCount[local] == 1)
					propertySet.setBoolProperty(SINGLE_DEAD_INCREMENT, true);
				
			} else if (ins instanceof ASTORE && prev != null) { 
				// Look for objects created but never used
				
				Instruction prevIns = prev.getInstruction();
				if ((prevIns instanceof INVOKESPECIAL &&
						((INVOKESPECIAL)prevIns).getMethodName(methodGen.getConstantPool()).equals("<init>"))
						|| prevIns instanceof ANEWARRAY
						|| prevIns instanceof NEWARRAY
						|| prevIns instanceof MULTIANEWARRAY) {
					propertySet.setBoolProperty(DEAD_OBJECT_STORE, true);
				}
				
			} else if (localStoreCount[local] == 2 && localLoadCount[local] > 0) {
				// TODO: why is this significant?
				
				propertySet.setBoolProperty(TWO_STORES_MULTIPLE_LOADS, true);
				
			} else if (localStoreCount[local] == 1) {
				// TODO: why is this significant?
				
				propertySet.setBoolProperty(SINGLE_STORE, true);
				
			} else if (localLoadCount[local] == 0) {
				// TODO: why is this significant?
				
				propertySet.setBoolProperty(NO_LOADS, true);
				
			}
			
			if (parameterThatIsDeadAtEntry) {
				if (DEBUG) System.out.println("Raising priority");
				propertySet.setBoolProperty(PARAM_DEAD_ON_ENTRY, true);
			}

			int priority = determineWarningPriority(); 
			if (priority >= 0) {	
				// Report the warning
				BugInstance bugInstance = new BugInstance(this, "DLS_DEAD_LOCAL_STORE", priority)
					.addClassAndMethod(methodGen, javaClass.getSourceFileName())
					.addSourceLine(methodGen, javaClass.getSourceFileName(), location.getHandle());
				if (DEBUG) System.out.println("Reporting " + bugInstance);
				
				// Encode heuristic information.
				propertySet.decorateBugInstance(bugInstance, schema);
				
				bugReporter.reportBug(bugInstance);
			}
		}
	}
	
	/**
	 * Count stores, loads, and increments of local variables
	 * in method whose CFG is given.
	 * 
	 * @param localStoreCount     counts of local stores (indexed by local)
	 * @param localLoadCount      counts of local loads (indexed by local)
	 * @param localIncrementCount counts of local increments (indexed by local)
	 * @param cfg                 control flow graph (CFG) of method
	 */
	private void countLocalStoresLoadsAndIncrements(int[] localStoreCount, int[] localLoadCount, int[] localIncrementCount, CFG cfg) {
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			
			if (location.getBasicBlock().isExceptionHandler())
				continue;
			
			boolean isStore = isStore(location);
			boolean isLoad = isLoad(location);
			if (!isStore && !isLoad)
				continue;
			
			IndexedInstruction ins = (IndexedInstruction) location.getHandle().getInstruction();
			int local = ins.getIndex();
			if (ins instanceof IINC) {
				localStoreCount[local]++;
				localLoadCount[local]++;
				localIncrementCount[local]++;
			} else if (isStore) 
				localStoreCount[local]++;
			else 
				localLoadCount[local]++;
		}
	}
	
	/**
	 * Get the name of given local variable (if possible) and store it in
	 * the HeuristicPropertySet.
	 * 
	 * @param lvt   the LocalVariableTable
	 * @param local index of the local
	 * @param pc    program counter value of the instruction
	 */
	private void checkLocalVariableName(LocalVariableTable lvt, int local, int pc) {
		if (lvt != null) {
			LocalVariable lv = lvt.getLocalVariable(local, pc);
			if (lv != null) {
				String localName = lv.getName();
				propertySet.setStringProperty(DeadLocalStoreHeuristics.LOCAL_NAME, localName);
			}
		}
		
	}
	
	/**
	 * Based on current HeuristicPropertySet contents, decide whether
	 * or not to report a warning, and at what priority.
	 * 
	 * @return a valid warning priority if the warning should be
	 *         reported, or &lt;0 if the warning should not be
	 *         reported
	 */
	private int determineWarningPriority() {
		// Allow "relaxed" reporting mode where we just report
		// everything and let the machine learning ranking find the
		// real problems.
		if (AnalysisContext.currentAnalysisContext().getBoolProperty(
				FindBugsAnalysisProperties.RELAXED_REPORTING_MODE)) {
			int priority = computeWarningPriority();
			return (priority <= LOW_PRIORITY) ? priority : LOW_PRIORITY;
		}
		
		if (DO_EXCLUDE_LOCALS) {
			String localName = propertySet.getStringProperty(DeadLocalStoreHeuristics.LOCAL_NAME);
			if (localName != null && EXCLUDED_LOCALS.contains(localName))
				return -1;
		}
		
		if (propertySet.getBoolProperty(EXCEPTION_HANDLER))
			return -1;
		
		if (propertySet.getBoolProperty(KILLED_BY_SUBSEQUENT_STORE))
			return -1;
		
		if (propertySet.getBoolProperty(DEFENSIVE_CONSTANT_OPCODE))
			return -1;
		
		if (propertySet.getBoolProperty(DEAD_INCREMENT) 
				&& !propertySet.getBoolProperty(SINGLE_DEAD_INCREMENT))
			return -1;
		
		return computeWarningPriority();
	}
	
	private int computeWarningPriority() {
		int priority = LOW_PRIORITY;
		
		if (propertySet.getBoolProperty(SINGLE_DEAD_INCREMENT)
				||propertySet.getBoolProperty(DEAD_OBJECT_STORE)
				|| propertySet.getBoolProperty(TWO_STORES_MULTIPLE_LOADS))
			--priority; // raise
		else if (propertySet.getBoolProperty(SINGLE_STORE)
				|| propertySet.getBoolProperty(NO_LOADS))
			++priority; // lower
		
		if (propertySet.getBoolProperty(PARAM_DEAD_ON_ENTRY))
			--priority; // raise
		
		if (priority < HIGH_PRIORITY)
			priority = HIGH_PRIORITY;
		
		return priority;
	}
	
	/**
	 * Is instruction at given location a store?
	 * 
	 * @param location the location
	 * @return true if instruction at given location is a store, false if not
	 */
	private boolean isStore(Location location) {
		Instruction ins = location.getHandle().getInstruction();
		return (ins instanceof StoreInstruction) || (ins instanceof IINC);
	}

	/**
	 * Is instruction at given location a load?
	 * 
	 * @param location the location
	 * @return true if instruction at given location is a load, false if not
	 */
	private boolean isLoad(Location location) {
		Instruction ins = location.getHandle().getInstruction();
		return (ins instanceof LoadInstruction) || (ins instanceof IINC);
	}
	
	public void report() {
	}
}

//vim:ts=4
