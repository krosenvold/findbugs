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
 * A class representing a location in the CFG for a method.
 * Essentially, it represents a static instruction, <em>with the important caveat</em>
 * that CFGs have inlined JSR subroutines, meaning that a single InstructionHandle
 * in a CFG may represent several static locations.  To this end, a Location
 * is comprised of both an InstructionHandle and the BasicBlock that
 * contains it.
 *
 * <p> Location objects may be compared with each other using the equals() method,
 * and may be used as keys in tree and hash maps and sets.
 * Note that <em>it is only valid to compare Locations produced from the same CFG</em>.
 *
 * @see CFG
 * @author David Hovemeyer
 */
public class Location implements Comparable<Location> {
	private final InstructionHandle handle;
	private final BasicBlock basicBlock;

	/**
	 * Constructor.
	 * @param handle the instruction
	 * @param basicBlock the basic block containing the instruction
	 */
	public Location(InstructionHandle handle, BasicBlock basicBlock) {
		this.handle = handle;
		this.basicBlock = basicBlock;
	}

	/** Get the instruction handle. */
	public InstructionHandle getHandle() { return handle; }

	/** Get the basic block. */
	public BasicBlock getBasicBlock() { return basicBlock; }

	public int compareTo(Location other) {
		int cmp = basicBlock.getId() - other.basicBlock.getId();
		if (cmp != 0)
			return cmp;

		return handle.getPosition() - other.handle.getPosition();
	}

	public int hashCode() {
		return System.identityHashCode(basicBlock) + handle.getPosition();
	}

	public boolean equals(Object o) {
		if (!(o instanceof Location))
			return false;
		Location other = (Location) o;
		return basicBlock == other.basicBlock && handle == other.handle;
	}

	public String toString() {
		return handle.toString() + " in basic block " + basicBlock.getId();
	}
}

// vim:ts=4
