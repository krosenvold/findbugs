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
import java.io.*;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A test driver for dataflow analysis classes.
 * It runs the dataflow analysis on the methods of a single class,
 * and has options (properties) to restrict the analysis to a single
 * method, and to print out a CFG annotated with dataflow values.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 * @author David Hovemeyer
 */
public abstract class DataflowTestDriver<Fact, AnalysisType extends AbstractDataflowAnalysis<Fact>> {

	private static class DataflowCFGPrinter<Fact, AnalysisType extends AbstractDataflowAnalysis<Fact>> extends CFGPrinter {
		private Dataflow<Fact, AnalysisType> dataflow;
		private AnalysisType analysis;

		public DataflowCFGPrinter(CFG cfg, Dataflow<Fact, AnalysisType> dataflow, AnalysisType analysis) {
			super(cfg);
			this.dataflow = dataflow;
			this.analysis = analysis;
		}

		public String blockStartAnnotate(BasicBlock bb) {
			return " " + analysis.factToString(dataflow.getStartFact(bb));
		}

		public String blockAnnotate(BasicBlock bb) {
			return " " + analysis.factToString(dataflow.getResultFact(bb));
		}

		public String instructionAnnotate(InstructionHandle handle, BasicBlock bb) {
			try {
				Fact result = analysis.getFactAtLocation(new Location(handle, bb));
				return " " + analysis.factToString(result);
			} catch (DataflowAnalysisException e) {
				throw new IllegalStateException("Caught exception: " + e.toString());
			}
		}
	}

	/**
	 * Execute the analysis on a single class.
	 * @param filename the name of the class file
	 */
	public void execute(String filename) throws DataflowAnalysisException, CFGBuilderException, IOException {
		JavaClass jclass = new RepositoryClassParser(filename).parse();

		final RepositoryLookupFailureCallback lookupFailureCallback = new RepositoryLookupFailureCallback() {
			public void reportMissingClass(ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			}
		};
		AnalysisContext.instance().setLookupFailureCallback(lookupFailureCallback);

		ClassContext classContext = AnalysisContext.instance().getClassContext(jclass);
		String methodName = System.getProperty("dataflow.method");

		Method[] methods = jclass.getMethods();
		for (int i = 0; i < methods.length; ++i) {
			Method method = methods[i];
			if (methodName != null && !method.getName().equals(methodName))
				continue;

			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			System.out.println("-----------------------------------------------------------------");
			System.out.println("Method: " + SignatureConverter.convertMethodSignature(methodGen));
			System.out.println("-----------------------------------------------------------------");

			execute(classContext, method);
		}
	}

	/**
	 * Execute the analysis on a single method of a class.
	 */
	public void execute(ClassContext classContext, Method method) throws DataflowAnalysisException, CFGBuilderException {

		Dataflow<Fact, AnalysisType> dataflow = createDataflow(classContext, method);
		System.out.println("Finished in " + dataflow.getNumIterations() + " iterations");

		CFG cfg = classContext.getCFG(method);
		examineResults(cfg, dataflow);

		if (Boolean.getBoolean("dataflow.printcfg")) {
			CFGPrinter p = new DataflowCFGPrinter<Fact, AnalysisType>(cfg, dataflow, dataflow.getAnalysis());
			p.print(System.out);
		}
	}

	/**
	 * Downcall method to create the dataflow driver object
	 * and execute the analysis.
	 * @param classContext ClassContext for the class
	 * @param method the Method
	 * @return the Dataflow driver
	 */
	public abstract Dataflow<Fact, AnalysisType> createDataflow(ClassContext classContext, Method method)
		throws CFGBuilderException, DataflowAnalysisException;

	/**
	 * Downcall method to inspect the analysis results.
	 * Need not be implemented by subclasses.
	 * @param cfg the control flow graph
	 * @param dataflow the analysis results
	 */
	public void examineResults(CFG cfg, Dataflow<Fact, AnalysisType> dataflow) {
	}
}

// vim:ts=4
