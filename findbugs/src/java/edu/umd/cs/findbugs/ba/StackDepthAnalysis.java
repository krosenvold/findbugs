/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A really simple forward dataflow analysis to find the depth of
 * the Java operand stack.  This is more of a proof of concept for
 * the dataflow analysis framework than anything useful.
 * @see Dataflow
 * @see DataflowAnalysis
 */
public class StackDepthAnalysis extends ForwardDataflowAnalysis<StackDepth> {
	public static final int TOP = -1;
	public static final int BOTTOM = -2;

	private ConstantPoolGen cpg;

	/**
	 * Constructor.
	 * @param cpg the ConstantPoolGen of the method whose CFG we're performing the analysis on
	 */
	public StackDepthAnalysis(ConstantPoolGen cpg) {
		this.cpg = cpg;
	}

	public StackDepth createFact() {
		return new StackDepth(TOP);
	}

	public void makeFactTop(StackDepth fact) {
		fact.setDepth(TOP);
	}

	public boolean isFactValid(StackDepth fact) {
		int depth = fact.getDepth();
		return depth != TOP && depth != BOTTOM;
	}

	public void copy(StackDepth source, StackDepth dest) {
		dest.setDepth(source.getDepth());
	}

	public void initEntryFact(StackDepth entryFact) {
		entryFact.setDepth(0); // stack depth == 0 at entry to CFG
	}

	public void initResultFact(StackDepth result) {
		makeFactTop(result);
	}

	public boolean same(StackDepth fact1, StackDepth fact2) {
		return fact1.getDepth() == fact2.getDepth();
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, StackDepth fact) throws DataflowAnalysisException {
		Instruction ins = handle.getInstruction();
		int produced = ins.produceStack(cpg);
		int consumed = ins.consumeStack(cpg);
		if (produced == Constants.UNPREDICTABLE || consumed == Constants.UNPREDICTABLE)
			throw new IllegalStateException("Unpredictable stack delta for instruction: " + handle);
		int depth = fact.getDepth();
		depth += (produced - consumed);
		fact.setDepth(depth);
	}

	public void meetInto(StackDepth fact, Edge edge, StackDepth result) {
		int a = fact.getDepth();
		int b = result.getDepth();
		int combined;

		if (a == TOP)
			combined = b;
		else if (b == TOP)
			combined = a;
		else if (a == BOTTOM || b == BOTTOM || a != b)
			combined = BOTTOM;
		else
			combined = a;

		result.setDepth(combined);
	}

	private static class StackDepthCFGPrinter extends CFGPrinter {
		private Dataflow<StackDepth> dataflow;
		private ConstantPoolGen cpg;

		public StackDepthCFGPrinter(CFG cfg, Dataflow<StackDepth> dataflow, ConstantPoolGen cpg) {
			super(cfg);
			this.dataflow = dataflow;
			this.cpg = cpg;
		}

		public String blockStartAnnotate(BasicBlock block) {
			StackDepth in = dataflow.getStartFact(block);
			return " start stack depth = " + in;
		}

		public String blockAnnotate(BasicBlock block) {
			StackDepth out = dataflow.getResultFact(block);
			return " end stack depth = " + out;
		}

		public String instructionAnnotate(InstructionHandle handle, BasicBlock bb){
			Instruction ins = handle.getInstruction();
			int produced = ins.produceStack(cpg);
			int consumed = ins.consumeStack(cpg);
			if (produced == Constants.UNPREDICTABLE || consumed == Constants.UNPREDICTABLE)
				return " stack delta = unpredictable";
			else
				return " stack delta = " + (produced - consumed);
		}
	}

	/**
	 * Command line driver, for testing.
	 */
	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.out.println("Usage: edu.umd.cs.daveho.ba.StackDepthAnalysis <class file>");
				System.exit(1);
			}

			String className = argv[0];
			JavaClass jclass = new RepositoryClassParser(className).parse();
			ClassGen cg = new ClassGen(jclass);
			ConstantPoolGen cpg = cg.getConstantPool();
			String methodName = System.getProperty("sda.method");

			Method[] methods = cg.getMethods();
			for (int i = 0; i < methods.length; ++i) {
				Method method = methods[i];
				if (method.isAbstract() || method.isNative())
					continue;
				if (methodName != null && !method.getName().equals(methodName))
					continue;

				MethodGen methodGen = new MethodGen(method, jclass.getClassName(), cpg);

				CFGBuilder cfgBuilder = CFGBuilderFactory.create(methodGen);
				cfgBuilder.build();

				CFG cfg = cfgBuilder.getCFG();
				cfg.assignEdgeIds(0);

				StackDepthAnalysis analysis = new StackDepthAnalysis(cpg);
				Dataflow<StackDepth> dataflow = new Dataflow<StackDepth>(cfg, analysis);

				dataflow.execute();

				System.out.println("Finished in " + dataflow.getNumIterations() + " iterations");

				StackDepthCFGPrinter printer = new StackDepthCFGPrinter(cfg, dataflow, cpg);
				printer.print(System.out);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

// vim:ts=4
