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

import org.apache.bcel.generic.*;

public class IsNullValueFrameModelingVisitor extends AbstractFrameModelingVisitor<IsNullValue, IsNullValueFrame> {

	public IsNullValueFrameModelingVisitor(ConstantPoolGen cpg) {
		super(cpg);
	}

	public IsNullValue getDefaultValue() {
		return IsNullValue.doNotReportValue();
	}

	// Overrides of specific instruction visitor methods.
	// ACONST_NULL obviously produces a value that is DEFINITELY NULL.
	// LDC produces values that are NOT NULL.
	// NEW produces values that are NOT NULL.

	// Note that all instructions that have an implicit null
	// check (field access, invoke, etc.) are handled in IsNullValueAnalysis,
	// because handling them relies on control flow (the existence of
	// an ETB and exception edge prior to the block containing the
	// instruction with the null check.)

	// Note that we don't override IFNULL and IFNONNULL.
	// Those are handled in the analysis itself, because we need
	// to produce different values in each of the control successors.

	private void produce(IsNullValue value) {
		IsNullValueFrame frame = getFrame();
		frame.pushValue(value);
	}

	private void produce2(IsNullValue value) {
		IsNullValueFrame frame = getFrame();
		frame.pushValue(value);
		frame.pushValue(value);
	}

	public void visitACONST_NULL(ACONST_NULL obj) {
		produce(IsNullValue.nullValue());
	}

	public void visitNEW(NEW obj) {
		produce(IsNullValue.nonNullValue());
	}

	public void visitLDC(LDC obj) {
		produce(IsNullValue.nonNullValue());
	}

	public void visitLDC_W(LDC_W obj) {
		produce(IsNullValue.nonNullValue());
	}

	public void visitLDC2_W(LDC2_W obj) {
		produce2(IsNullValue.nonNullValue());
	}

}

// vim:ts=4
