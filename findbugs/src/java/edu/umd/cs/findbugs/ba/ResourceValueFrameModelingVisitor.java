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

public abstract class ResourceValueFrameModelingVisitor extends AbstractFrameModelingVisitor<ResourceValue, ResourceValueFrame> {
	public ResourceValueFrameModelingVisitor(ConstantPoolGen cpg) {
		super(cpg);
	}

	public ResourceValue getDefaultValue() {
		return ResourceValue.notInstance();
	}

	/**
	 * Subclasses must override this to model the effect of the
	 * given instruction on the current frame.
	 */
	public abstract void transferInstruction(InstructionHandle handle, BasicBlock basicBlock) throws DataflowAnalysisException;

	// Things to do:
	// Automatically detect when resource instances escape:
	//   - putfield, putstatic
	//   - parameters to invoke, but subclasses may override
	//   - return (areturn)

	private void handleFieldStore(FieldInstruction ins) {
		try {
			// If the resource instance is stored in a field, then it escapes
			ResourceValueFrame frame = getFrame();
			ResourceValue topValue = frame.getTopValue();
			if (topValue.equals(ResourceValue.instance()))
				frame.setEscaped(true);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.toString());
		}

		handleNormalInstruction(ins);
	}

	public void visitPUTFIELD(PUTFIELD putfield) {
		handleFieldStore(putfield);
	}

	public void visitPUTSTATIC(PUTSTATIC putstatic) {
		handleFieldStore(putstatic);
	}

	/**
	 * Override this to check for methods that it is legal to
	 * pass the instance to without the instance escaping.
	 * By default, we consider all methods to be possible escape routes.
	 * @param inv the InvokeInstruction to which the resource instance
	 *   is passed as an argument
	 * @param instanceArgNum the first argument the instance is passed in
	 */
	protected boolean instanceEscapes(InvokeInstruction inv, int instanceArgNum) {
		return true;
	}

	private void handleInvoke(InvokeInstruction inv) {
		ResourceValueFrame frame = getFrame();
		int numSlots = frame.getNumSlots();
		int numConsumed = getNumWordsConsumed(inv);

		// See if the resource instance is passed as an argument
		int instanceArgNum = -1;
		for (int i = numSlots - numConsumed, argCount = 0; i < numSlots; ++i, ++argCount) {
			ResourceValue value = frame.getValue(i);
			if (value.equals(ResourceValue.instance())) {
				instanceArgNum = argCount;
				break;
			}
		}

		if (instanceArgNum >= 0 && instanceEscapes(inv, instanceArgNum))
			frame.setEscaped(true);

		handleNormalInstruction(inv);
	}

	public void visitINVOKEVIRTUAL(INVOKEVIRTUAL inv) {
		handleInvoke(inv);
	}

	public void visitINVOKEINTERFACE(INVOKEINTERFACE inv) {
		handleInvoke(inv);
	}

	public void visitINVOKESPECIAL(INVOKESPECIAL inv) {
		handleInvoke(inv);
	}

	public void visitINVOKESTATIC(INVOKESTATIC inv) {
		handleInvoke(inv);
	}

	public void visitARETURN(ARETURN ins) {
		try {
			ResourceValueFrame frame = getFrame();
			ResourceValue topValue = frame.getTopValue();
			if (topValue.equals(ResourceValue.instance()))
				frame.setEscaped(true);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException(e.toString());
		}

		handleNormalInstruction(ins);
	}

}

// vim:ts=4
