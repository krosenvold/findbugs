/*
 * Bytecode analysis framework
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba.npe;

import java.util.Map;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.BackwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowTestDriver;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.TypeDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Dataflow analysis to look for parameters dereferenced unconditionally.
 * Flow values are sets of parameters (indexed starting from 0) which are
 * dereferenced on every path past the current location.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalDerefAnalysis extends BackwardDataflowAnalysis<UnconditionalDerefSet> {
	private static final boolean DEBUG = Boolean.getBoolean("npe.deref.debug");
	
	private final CFG cfg;
	private final MethodGen methodGen;
	private final TypeDataflow typeDataflow;
	private final ValueNumberDataflow vnaDataflow;
	private final Map<ValueNumber, Integer> valueNumberToParamMap;
	private final int numParams;
	private final int topBit;
	private final int bottomBit;
	
	public UnconditionalDerefAnalysis(
			ReverseDepthFirstSearch rdfs,
			CFG cfg,
			MethodGen methodGen,
			ValueNumberDataflow vnaDataflow,
			TypeDataflow typeDataflow) {
		super(rdfs);
		this.cfg = cfg;
		this.methodGen = methodGen;
		this.typeDataflow = typeDataflow;
		this.vnaDataflow = vnaDataflow;
		this.valueNumberToParamMap = vnaDataflow.getValueNumberToParamMap(methodGen.getSignature(), methodGen.isStatic());
		this.numParams = new SignatureParser(methodGen.getSignature()).getNumParameters();
		this.topBit = numParams;
		this.bottomBit = numParams + 1;
	}

	public void copy(UnconditionalDerefSet source, UnconditionalDerefSet dest) {
		dest.clear();
		dest.or(source);
	}
	
	public UnconditionalDerefSet createFact() {
		return new UnconditionalDerefSet(numParams);
	}
	
	public void initEntryFact(UnconditionalDerefSet result) throws DataflowAnalysisException {
		// At entry (really the CFG exit, since this is a backwards analysis)
		// no dereferences have been seen
		result.clear();
	}
	
	public void initResultFact(UnconditionalDerefSet result) {
		makeFactTop(result);
	}
	
	public void makeFactTop(UnconditionalDerefSet fact) {
		fact.setTop();
	}
	
	public void meetInto(UnconditionalDerefSet fact, Edge edge, UnconditionalDerefSet result) throws DataflowAnalysisException {
		// Ignore implicit exceptions
		if (ClassContext.PRUNE_INFEASIBLE_EXCEPTION_EDGES
				&& edge.isExceptionEdge()
				&& !edge.isFlagSet(EdgeTypes.EXPLICIT_EXCEPTIONS_FLAG)) {
			return;
		}
		
		if (result.isTop() || fact.isBottom()) {
			copy(fact, result);
		} else if (result.isBottom() || fact.isTop()) {
			// Nothing to do
		} else {
			// Meet is intersection
			result.and(fact);
		}
	}
	
	public boolean same(UnconditionalDerefSet fact1, UnconditionalDerefSet fact2) {
		return fact1.equals(fact2);
	}
	
	//@Override
	public boolean isFactValid(UnconditionalDerefSet fact) {
		return fact.isValid();
	}
	
	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, UnconditionalDerefSet fact)
		throws DataflowAnalysisException {
		
		if (!fact.isValid())
			throw new IllegalStateException();

		// See if this instruction has a null check.
		if (handle != basicBlock.getFirstInstruction())
			return;
		BasicBlock fallThroughPredecessor = cfg.getPredecessorWithEdgeType(basicBlock, EdgeTypes.FALL_THROUGH_EDGE);
		if (fallThroughPredecessor == null || !fallThroughPredecessor.isNullCheck())
			return;

		// Get value number of the checked value
		ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));
		if (!vnaFrame.isValid()) {
			// Probably dead code.
			// Assume this location can't be reached.
			makeFactTop(fact);
			return;
		}
		ValueNumber instance = vnaFrame.getInstance(handle.getInstruction(), methodGen.getConstantPool());
		if (DEBUG) {
			System.out.println("[Null check of value " + instance.getNumber() + "]");
		}

		Integer param = valueNumberToParamMap.get(instance);
		if (param == null)
			return;
		
		if (DEBUG) {
			System.out.println("[Value is a parameter!]");
		}
		fact.set(param.intValue());
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + UnconditionalDerefAnalysis.class.getName() + " <class file>");
			System.exit(1);
		}
		DataflowTestDriver<UnconditionalDerefSet, UnconditionalDerefAnalysis> driver =
			new DataflowTestDriver<UnconditionalDerefSet, UnconditionalDerefAnalysis>() {
				public UnconditionalDerefDataflow createDataflow(ClassContext classContext, Method method)
						throws CFGBuilderException, DataflowAnalysisException {
					return classContext.getUnconditionalDerefDataflow(method);
				}
		};
		driver.execute(argv[0]);
	}
}
