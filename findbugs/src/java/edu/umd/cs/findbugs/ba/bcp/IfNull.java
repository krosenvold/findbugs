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

package edu.umd.cs.findbugs.ba.bcp;

import org.apache.bcel.generic.*;
import edu.umd.cs.findbugs.ba.*;

public class IfNull extends OneVariableInstruction implements EdgeTypes {

	public IfNull(String varName) {
		super(varName);
	}

	public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
		ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		// Instruction must be IFNULL or IFNONNULL.
		Instruction ins = handle.getInstruction();
		if (!(ins instanceof IFNULL || ins instanceof IFNONNULL))
			return null;

		// Ensure reference used is consistent with previous uses of
		// same variable.
		LocalVariable ref = new LocalVariable(before.getTopValue());
		return addOrCheckDefinition(ref, bindingSet);
	}

	public boolean acceptBranch(Edge edge, InstructionHandle source) {
		boolean isIfNull = (source.getInstruction() instanceof IFNULL);
		return edge.getType() == (isIfNull ? IFCMP_EDGE : FALL_THROUGH_EDGE);
	}
}

// vim:ts=4
