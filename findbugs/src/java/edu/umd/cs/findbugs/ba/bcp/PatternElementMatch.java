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

package edu.umd.cs.daveho.ba.bcp;

import java.util.*;
import org.apache.bcel.generic.InstructionHandle;

/**
 * PatternElementMatch represents matching a PatternElement against
 * a single instruction.  The "prev" field points to the previous
 * PatternElementMatch.  By building up sequences of PatternElementMatch objects
 * in this way, we can implement nondeterministic matching without
 * having to copy anything.
 */
public class PatternElementMatch {
	private final PatternElement patternElement;
	private final InstructionHandle matchedInstruction;
	private final int matchCount;
	private final PatternElementMatch prev;

	/**
	 * Constructor.
	 * @param patternElement the PatternElement being matched
	 * @param matchedInstruction the instruction which matched the PatternElement
	 * @param matchCount the index (starting at zero) of the instructions
	 *   matching the PatternElement; multiple instructions can match the
	 *   same PatternElement
	 * @param prev the previous PatternElementMatch
	 */
	public PatternElementMatch(PatternElement patternElement, InstructionHandle matchedInstruction,
		int matchCount, PatternElementMatch prev) {
		this.patternElement = patternElement;
		this.matchedInstruction = matchedInstruction;
		this.matchCount = matchCount;
		this.prev = prev;
	}

	/** Get the PatternElement. */
	public PatternElement getPatternElement() {
		return patternElement;
	}

	/** Get the matched instruction. */
	public InstructionHandle getMatchedInstructionInstructionHandle() {
		return matchedInstruction;
	}

	/*
	 * Get the index of this instruction in terms of how many instructions
	 * have matched this PatternElement.  (0 for the first instruction to
	 * match the PatternElement, etc.)
	 */
	public int getMatchCount() {
		return matchCount;
	}

	/**
	 * Get the previous PatternMatchElement.
	 */
	public PatternElementMatch getPrev() {
		return prev;
	}
}

// vim:ts=4
