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

public class IsNullValueFrame extends Frame<IsNullValue> {
	private IsNullConditionDecision decision;

	public IsNullValueFrame(int numLocals) {
		super(numLocals);
	}

	public IsNullValue mergeValues(int slot, IsNullValue a, IsNullValue b) throws DataflowAnalysisException {
		return IsNullValue.merge(a, b);
	}

	public IsNullValue getDefaultValue() {
		return IsNullValue.doNotReportValue();
	}

	public void toExceptionValues() {
		for (int i = 0; i < getNumSlots(); ++i)
			setValue(i, getValue(i).toExceptionValue());
	}

	public void setDecision(IsNullConditionDecision decision) {
		this.decision = decision;
	}

	public IsNullConditionDecision getDecision() {
		return decision;
	}

	public String toString() {
		String result = super.toString();
		if (decision != null) {
			result = result + ", [decision=" + decision.toString() + "]";
		}
		return result;
	}
}

// vim:ts=4
