/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import java.util.*;

import org.apache.bcel.generic.InstructionHandle;

/**
 * Abstract base class providing functionality that will be useful
 * for most dataflow analysis implementations.  In particular, it implements
 * the transfer() function by calling down to the transferInstruction() function.
 * It also maintains a map of the dataflow fact for every location in the CFG,
 * which is useful when using the results of the analysis.
 *
 * @author David Hovemeyer
 * @see Dataflow
 * @see DataflowAnalysis
 */
public abstract class AbstractDataflowAnalysis <Fact> implements DataflowAnalysis<Fact> {
	private static final boolean DEBUG = Boolean.getBoolean("dataflow.transfer");

	private IdentityHashMap<BasicBlock, Fact> startFactMap = new IdentityHashMap<BasicBlock, Fact>();
	private IdentityHashMap<BasicBlock, Fact> resultFactMap = new IdentityHashMap<BasicBlock, Fact>();

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Transfer function for a single instruction.
	 *
	 * @param handle     the instruction
	 * @param basicBlock the BasicBlock containing the instruction; needed to disambiguate
	 *                   instructions in inlined JSR subroutines
	 * @param fact       which should be modified based on the instruction
	 */
	public abstract void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, Fact fact) throws DataflowAnalysisException;

	/**
	 * Determine whether the given fact is <em>valid</em>
	 * (neither top nor bottom).
	 */
	public abstract boolean isFactValid(Fact fact);

	/**
	 * Subclasses may override this.
	 * Due to a bug in the 2.2 version of the generics-enabled javac,
	 * it is not possible to directly override the transfer() method.
	 * This method will be called immediately upon entry to transfer().
	 *
	 * @param basicBlock the basic block
	 * @param fact       the start fact for the block
	 */
	public void startTransfer(BasicBlock basicBlock, Object fact) throws DataflowAnalysisException {
	}

	/**
	 * Subclasses may override this.
	 * Due to a bug in the 2.2 version of the generics-enabled javac,
	 * it is not possible to directly override the transfer() method.
	 * This method will be called just before exiting transfer().
	 *
	 * @param basicBlock the basic block
	 * @param end        last instruction analyzed (null if entire block was analyzed)
	 * @param result     the result fact for the block
	 */
	public void endTransfer(BasicBlock basicBlock, InstructionHandle end, Object result) throws DataflowAnalysisException {
	}

	/**
	 * Get the dataflow fact representing the point just before given Location.
	 * Note "before" is meant in the logical sense, so for backward analyses,
	 * before means after the location in the control flow sense.
	 *
	 * @param location the location
	 * @return the fact at the point just before the location
	 */
	public Fact getFactAtLocation(Location location) throws DataflowAnalysisException {
		Fact start = getStartFact(location.getBasicBlock());
		Fact result = createFact();
		makeFactTop(result);
		transfer(location.getBasicBlock(), location.getHandle(), start, result);
		return result;
	}

	/**
	 * Get the dataflow fact representing the point just after given Location.
	 * Note "after" is meant in the logical sense, so for backward analyses,
	 * after means before the location in the control flow sense.
	 */
	public Fact getFactAfterLocation(Location location) throws DataflowAnalysisException {
		BasicBlock basicBlock = location.getBasicBlock();
		InstructionHandle handle = location.getHandle();

		if (handle == basicBlock.getLastInstruction())
			return getResultFact(basicBlock);
		else
			return getFactAtLocation(new Location(isForwards() ? handle.getNext() : handle.getPrev(), basicBlock));
	}

	/**
	 * Get an iterator over the result facts.
	 */
	public Iterator<Fact> resultFactIterator() {
		return resultFactMap.values().iterator();
	}

	/**
	 * Call this to get a dataflow value as a String.
	 * By default, we just call toString().
	 * Subclasses may override to get different behavior.
	 */
	public String factToString(Fact fact) {
		return fact.toString();
	}

	/* ----------------------------------------------------------------------
	 * Implementations of interface methods
	 * ---------------------------------------------------------------------- */

	public Fact getStartFact(BasicBlock block) {
		return lookupOrCreateFact(startFactMap, block);
	}

	public Fact getResultFact(BasicBlock block) {
		return lookupOrCreateFact(resultFactMap, block);
	}

	private Fact lookupOrCreateFact(Map<BasicBlock, Fact> map, BasicBlock block) {
		Fact fact = map.get(block);
		if (fact == null) {
			fact = createFact();
			map.put(block, fact);
		}
		return fact;
	}

	public void transfer(BasicBlock basicBlock, InstructionHandle end, Fact start, Fact result) throws DataflowAnalysisException {
		startTransfer(basicBlock, start);

		copy(start, result);

		if (isFactValid(result)) {
			Iterator<InstructionHandle> i = isForwards() ? basicBlock.instructionIterator() : basicBlock.instructionReverseIterator();

			while (i.hasNext()) {
				InstructionHandle handle = i.next();
				if (handle == end)
					break;

				if (DEBUG && end == null) System.out.print("Transfer " + handle);
	
				// Transfer the dataflow value
				transferInstruction(handle, basicBlock, result);

				if (DEBUG && end == null) System.out.println(" ==> " + result.toString());
			}
		}

		endTransfer(basicBlock, end, result);
	}

}

// vim:ts=4
