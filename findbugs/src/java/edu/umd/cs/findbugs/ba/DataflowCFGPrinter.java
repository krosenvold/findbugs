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

import org.apache.bcel.generic.InstructionHandle;

/**
 * CFGPrinter class which prints dataflow values at
 * each basic block and instruction.
 */
public class DataflowCFGPrinter<Fact, AnalysisType extends AbstractDataflowAnalysis<Fact>> extends CFGPrinter {
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

// vim:ts=4
