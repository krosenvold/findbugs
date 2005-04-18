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

import edu.umd.cs.findbugs.graph.AbstractGraph;
import org.apache.bcel.generic.InstructionHandle;

/**
 * Simple control flow graph abstraction for BCEL.
 *
 * @see BasicBlock
 * @see Edge
 */
public class CFG extends AbstractGraph<Edge, BasicBlock> implements Debug {

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * An Iterator over the Locations in the CFG.
	 * Because of JSR subroutines, the same instruction may actually
	 * be part of multiple basic blocks (with different facts
	 * true in each, due to calling context).  Locations specify
	 * both the instruction and the basic block.
	 */
	private class LocationIterator implements Iterator<Location> {
		private Iterator<BasicBlock> blockIter;
		private BasicBlock curBlock;
		private Iterator<InstructionHandle> instructionIter;
		private Location next;

		private LocationIterator() {
			this.blockIter = blockIterator();
			findNext();
		}

		public boolean hasNext() {
			findNext();
			return next != null;
		}

		public Location next() {
			findNext();
			if (next == null) throw new NoSuchElementException();
			Location result = next;
			next = null;
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void findNext() {
			while (next == null) {
				// Make sure we have an instruction iterator
				if (instructionIter == null) {
					if (!blockIter.hasNext())
						return; // At end
					curBlock = blockIter.next();
					instructionIter = curBlock.instructionIterator();
				}

				if (instructionIter.hasNext())
					next = new Location(instructionIter.next(), curBlock);
				else
					instructionIter = null; // Go to next block
			}
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BasicBlock entry, exit;
	private int flags;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * Creates empty control flow graph (with just entry and exit nodes).
	 */
	public CFG() {
	}

	void setFlags(int flags) {
		this.flags = flags;
	}

	int getFlags() {
		return flags;
	}

	boolean isFlagSet(int flag) {
		return (flags & flag) != 0;
	}

	/**
	 * Get the entry node.
	 */
	public BasicBlock getEntry() {
		if (entry == null) {
			entry = allocate();
		}
		return entry;
	}

	/**
	 * Get the exit node.
	 */
	public BasicBlock getExit() {
		if (exit == null) {
			exit = allocate();
		}
		return exit;
	}

	/**
	 * Add a unique edge to the graph.
	 * There must be no other edge already in the CFG with
	 * the same source and destination blocks.
	 *
	 * @param source the source basic block
	 * @param dest   the destination basic block
	 * @param type   the type of edge; see constants in EdgeTypes interface
	 * @return the newly created Edge
	 * @throws IllegalStateException if there is already an edge in the CFG
	 *                               with the same source and destination block
	 */
	public Edge createEdge(BasicBlock source, BasicBlock dest, int type) {
		Edge edge = createEdge(source, dest);
		edge.setType(type);
		return edge;
	}

	/**
	 * Look up an Edge by its id.
	 *
	 * @param id the id of the edge to look up
	 * @return the Edge, or null if no matching Edge was found
	 */
	public Edge lookupEdgeById(int id) {
		Iterator<Edge> i = edgeIterator();
		while (i.hasNext()) {
			Edge edge = i.next();
			if (edge.getId() == id)
				return edge;
		}
		return null;
	}

	/**
	 * Get an Iterator over the nodes (BasicBlocks) of the control flow graph.
	 */
	public Iterator<BasicBlock> blockIterator() {
		return vertexIterator();
	}

	/**
	 * Get an Iterator over the Locations in the control flow graph.
	 */
	public Iterator<Location> locationIterator() {
		return new LocationIterator();
	}

	/**
	 * Get Collection of basic blocks whose IDs are specified by
	 * given BitSet.
	 *
	 * @param idSet BitSet of block IDs
	 * @return a Collection containing the blocks whose IDs are given
	 */
	public Collection<BasicBlock> getBlocks(BitSet idSet) {
		LinkedList<BasicBlock> result = new LinkedList<BasicBlock>();
		for (Iterator<BasicBlock> i = blockIterator(); i.hasNext();) {
			BasicBlock block = i.next();
			if (idSet.get(block.getId()))
				result.add(block);
		}
		return result;
	}

	/**
	 * Get a Collection of basic blocks which contain the bytecode
	 * instruction with given offset.
	 *
	 * @param offset the bytecode offset of an instruction
	 * @return Collection of BasicBlock objects which contain the instruction
	 *         with that offset
	 */
	public Collection<BasicBlock> getBlocksContainingInstructionWithOffset(int offset) {
		LinkedList<BasicBlock> result = new LinkedList<BasicBlock>();
		for (Iterator<BasicBlock> i = blockIterator(); i.hasNext(); ) {
			BasicBlock block = i.next();
			if (block.containsInstructionWithOffset(offset))
				result.add(block);
		}
		return result;
	}
	
	/**
	 * Get a Collection of Locations which specify the instruction
	 * at given bytecode offset.
	 * 
	 * @param offset the bytecode offset
	 * @return all Locations referring to the instruction at that offset
	 */
	public Collection<Location> getLocationsContainingInstructionWithOffset(int offset) {
		LinkedList<Location> result = new LinkedList<Location>();
		for (Iterator<Location> i = locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			if (location.getHandle().getPosition() == offset) {
				result.add(location);
			}
		}
		return result;
	}

	/**
	 * Get the first successor reachable from given edge type.
	 *
	 * @param source   the source block
	 * @param edgeType the edge type leading to the successor
	 * @return the successor, or null if there is no outgoing edge with
	 *         the specified edge type
	 */
	public BasicBlock getSuccessorWithEdgeType(BasicBlock source, int edgeType) {
		Edge edge = getOutgoingEdgeWithType(source, edgeType);
		return edge != null ? edge.getTarget() : null;
	}

	/**
	 * Get the first outgoing edge in basic block with given type.
	 *
	 * @param basicBlock the basic block
	 * @param edgeType   the edge type
	 * @return the Edge, or null if there is no edge with that edge type
	 */
	public Edge getOutgoingEdgeWithType(BasicBlock basicBlock, int edgeType) {
		Iterator<Edge> i = outgoingEdgeIterator(basicBlock);
		while (i.hasNext()) {
			Edge edge = i.next();
			if (edge.getType() == edgeType)
				return edge;
		}
		return null;
	}

	/**
	 * Allocate a new BasicBlock.  The block won't be connected to
	 * any node in the graph.
	 */
	public BasicBlock allocate() {
		BasicBlock b = new BasicBlock();
		addVertex(b);
		return b;
	}

	/**
	 * Get number of basic blocks.
	 * This is just here for compatibility with the old CFG
	 * method names.
	 */
	public int getNumBasicBlocks() {
		return getNumVertices();
	}

	/**
	 * Get the number of edge labels allocated.
	 * This is just here for compatibility with the old CFG
	 * method names.
	 */
	public int getMaxEdgeId() {
		return getNumEdgeLabels();
	}

	public void checkIntegrity() {
		// Ensure that basic blocks have only consecutive instructions
		for (Iterator<BasicBlock> i = blockIterator(); i.hasNext();) {
			BasicBlock basicBlock = i.next();
			InstructionHandle prev = null;
			for (Iterator<InstructionHandle> j = basicBlock.instructionIterator(); j.hasNext();) {
				InstructionHandle handle = j.next();
				if (prev != null && prev.getNext() != handle)
					throw new IllegalStateException("Non-consecutive instructions in block " + basicBlock.getId() +
					        ": prev=" + prev + ", handle=" + handle);
				prev = handle;
			}
		}
	}

	protected Edge allocateEdge(BasicBlock source, BasicBlock target) {
		return new Edge(source, target);
	}
}

// vim:ts=4
