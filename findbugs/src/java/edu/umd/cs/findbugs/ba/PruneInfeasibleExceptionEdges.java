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
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Prune a CFG to remove infeasible exception edges.
 * In order to determine what kinds of exceptions can be thrown by
 * explicit ATHROW instructions, type analysis must first be
 * performed on the unpruned CFG.
 *
 * @see CFG
 * @see TypeAnalysis
 * @author David Hovemeyer
 */
public class PruneInfeasibleExceptionEdges implements EdgeTypes {
	private static final boolean DEBUG = Boolean.getBoolean("cfg.prune.debug");
	private static final boolean STATS = Boolean.getBoolean("cfg.prune.stats");
	private static int numEdgesPruned = 0;

	static {
		if (STATS) {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					System.err.println("Exception edges pruned: " + numEdgesPruned);
				}
			});
		}
	}

	/**
	 * A momento to remind us of how we classified a particular
	 * exception edge.  If pruning and classifying succeeds,
	 * then these momentos can be applied to actually change
	 * the state of the edges.  The issue is that the entire
	 * pruning/classifying operation must either fail or succeed
	 * as a whole.  Thus, we don't commit any CFG changes until
	 * we know everything was successful.
	 */
	private static class MarkedEdge {
		private Edge edge;
		private int flag;

		public MarkedEdge(Edge edge, int flag) {
			this.edge = edge;
			this.flag = flag;
		}

		public void apply() {
			int flags = edge.getFlags();
			flags |= this.flag;
			edge.setFlags(flags);
		}
	}

	private CFG cfg;
	private MethodGen methodGen;
	private TypeDataflow typeDataflow;

	/**
	 * Constructor.
	 * @param cfg the CFG to prune
	 * @param methodGen the method
	 * @param typeDataflow initialized TypeDataflow object for the CFG,
	 *   indicating the types of all stack locations
	 */
	public PruneInfeasibleExceptionEdges(CFG cfg, MethodGen methodGen, TypeDataflow typeDataflow) {
		this.cfg = cfg;
		this.methodGen = methodGen;
		this.typeDataflow = typeDataflow;
	}

	/**
	 * Prune infeasible exception edges from the CFG.
	 * If the method returns normally, then the operation
	 * was successful, and the CFG should no longer contain infeasible
	 * exception edges.  If ClassNotFoundException or DataflowAnalysisException
	 * are thrown, then the operation was unsuccessful,
	 * <em>but the CFG is still valid because it was not modified</em>.
	 * If a runtime exception is thrown, then the CFG may be
	 * partially modified and should be considered invalid.
	 */
	public void execute() throws ClassNotFoundException {
		HashSet<Edge> deletedEdgeSet = new HashSet<Edge>();
		List<MarkedEdge> markedEdgeList = new LinkedList<MarkedEdge>();

		// Mark edges to delete,
		// mark edges to set properties of
		for (Iterator<Edge> i = cfg.edgeIterator(); i.hasNext(); ) {
			Edge edge = i.next();
			if (!edge.isExceptionEdge())
				continue;

			ExceptionSet exceptionSet = typeDataflow.getEdgeExceptionSet(edge);
			if (exceptionSet.isEmpty()) {
				// No exceptions are actually thrown on this edge,
				// so we can delete the edge.
				deletedEdgeSet.add(edge);
			} else {
				// Some exceptions appear to be thrown on the edge.
				// Mark to indicate if any of the exceptions are checked,
				// and if any are explicit (checked or explicitly declared
				// or thrown unchecked).
				boolean someChecked = exceptionSet.containsCheckedExceptions();
				boolean someExplicit = exceptionSet.containsExplicitExceptions();

				int flags = 0;
				if (someChecked) flags |= CHECKED_EXCEPTIONS_FLAG;
				if (someExplicit) flags |= EXPLICIT_EXCEPTIONS_FLAG;

				markedEdgeList.add(new MarkedEdge(edge, flags));
			}
		}

		// Remove deleted edges
		for (Iterator<Edge> j = deletedEdgeSet.iterator(); j.hasNext(); ) {
			Edge edge = j.next();
			cfg.removeEdge(edge);
			if (STATS) ++numEdgesPruned;
		}

		// Mark edges
		for (Iterator<MarkedEdge> j = markedEdgeList.iterator(); j.hasNext(); ) {
			j.next().apply();
		}
	}
}

// vim:ts=4
