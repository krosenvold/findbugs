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
import org.apache.bcel.generic.Instruction;

/**
 * A cache mapping instructions and input values to the output values they
 * produce.  We must always produce the same output given identical
 * input, or else value number analysis will not terminate.
 *
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class ValueNumberCache {
	/**
	 * An entry in the cache.
	 * It represents an instruction with specific input values.
	 */
	public static class Entry {
		public final Instruction instruction;
		public final ValueNumber[] inputValueList;
		private int cachedHashCode;

		public Entry(Instruction instruction, ValueNumber[] inputValueList) {
			this.instruction = instruction;
			this.inputValueList = inputValueList;
			this.cachedHashCode = 0;
		}

		public boolean equals(Object o) {
			if (!(o instanceof Entry))
				return false;
			Entry other = (Entry) o;
			if (!instruction.equals(other.instruction))
				return false;
			ValueNumber[] myList = inputValueList;
			ValueNumber[] otherList = other.inputValueList;
			if (myList.length != otherList.length)
				return false;
			for (int i = 0; i < myList.length; ++i)
				if (!myList[i].equals(otherList[i]))
					return false;
			return true;
		}

		public int hashCode() {
			if (cachedHashCode == 0) {
				int code = instruction.getOpcode();
				for (int i = 0; i < inputValueList.length; ++i) {
					ValueNumber valueNumber = inputValueList[i];
					code += valueNumber.hashCode();
				}
				cachedHashCode = code;
			}
			return cachedHashCode;
		}
	}

	/** Map of entries to output values. */
	private HashMap<Entry, ValueNumber[]> entryToOutputMap = new HashMap<Entry, ValueNumber[]>();

	/**
	 * Look up cached output values for given entry.
	 * @param entry the entry
	 * @return the list of output values, or null if there is no matching entry
	 *   in the cache
	 */
	public ValueNumber[] lookupOutputValues(Entry entry) {
		return entryToOutputMap.get(entry);
	}

	/**
	 * Add output values for given entry.
	 * Assumes that lookupOutputValues() has determined that the entry
	 * is not in the cache.
	 * @param entry the entry
	 * @param outputValueList the list of output values produced
	 *  by the entry's instruction and input values
	 */
	public void addOutputValues(Entry entry, ValueNumber[] outputValueList) {
		ValueNumber[] old = entryToOutputMap.put(entry, outputValueList);
		if (old != null)
			throw new IllegalStateException("overwriting output values for entry!");
	}

}

// vim:ts=4
