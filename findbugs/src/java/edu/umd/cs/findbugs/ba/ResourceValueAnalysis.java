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

import java.util.BitSet;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class ResourceValueAnalysis<Resource> extends FrameDataflowAnalysis<ResourceValue, ResourceValueFrame>
	implements EdgeTypes {

	private static final boolean DEBUG = Boolean.getBoolean("dataflow.debug");

	private MethodGen methodGen;
	private CFG cfg;
	private ResourceTracker<Resource> resourceTracker;
	private Resource resource;
	private ResourceValueFrameModelingVisitor visitor;
	private RepositoryLookupFailureCallback lookupFailureCallback;

	public ResourceValueAnalysis(MethodGen methodGen, CFG cfg, DepthFirstSearch dfs,
		ResourceTracker<Resource> resourceTracker, Resource resource,
		RepositoryLookupFailureCallback lookupFailureCallback) {

		super(dfs);
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.resourceTracker = resourceTracker;
		this.resource = resource;
		this.visitor = resourceTracker.createVisitor(resource, methodGen.getConstantPool());
		this.lookupFailureCallback = lookupFailureCallback;
	}

	public ResourceValueFrame createFact() {
		ResourceValueFrame fact = new ResourceValueFrame(methodGen.getMaxLocals());
		fact.setTop();
		return fact;
	}

	public void initEntryFact(ResourceValueFrame result) {
		result.setValid();
		result.clearStack();
		final int numSlots = result.getNumSlots();
		for (int i = 0; i < numSlots; ++i)
			result.setValue(i, ResourceValue.notInstance());
	}

	public void meetInto(ResourceValueFrame fact, Edge edge, ResourceValueFrame result) throws DataflowAnalysisException {
		BasicBlock source = edge.getSource();
		BasicBlock dest = edge.getTarget();

		ResourceValueFrame tmpFact = null;

		if (edge.isExceptionEdge()) {
			InstructionHandle handle = source.getExceptionThrower();
			if (isFinalFieldLoad(handle)) {
				// Ignore exceptions thrown by load of final instance fields.
				// We'll just assume (in the name of reducing false positives)
				// that they got initialized to something.
				if (DEBUG) System.out.println("Making resource nonexistent on load of final field from block " + source.getId());
				tmpFact = modifyFrame(fact, tmpFact);
				tmpFact.setStatus(ResourceValueFrame.NONEXISTENT);
			} else if (fact.getStatus() == ResourceValueFrame.OPEN) {
				// If status is OPEN, downgrade to OPEN_ON_EXCEPTION_PATH
				tmpFact = modifyFrame(fact, tmpFact);
				tmpFact.setStatus(ResourceValueFrame.OPEN_ON_EXCEPTION_PATH);
			}

			// Special case: if the instruction that closes the resource
			// throws an exception, we consider the resource to be successfully
			// closed anyway.
			InstructionHandle exceptionThrower = source.getExceptionThrower();
			BasicBlock fallThroughSuccessor = cfg.getSuccessorWithEdgeType(source, FALL_THROUGH_EDGE);
			if (DEBUG && fallThroughSuccessor == null) System.out.println("Null fall through successor!");
			if (fallThroughSuccessor != null &&
				resourceTracker.isResourceClose(fallThroughSuccessor, exceptionThrower, methodGen.getConstantPool(), resource, fact)) {
				tmpFact = modifyFrame(fact, tmpFact);
				tmpFact.setStatus(ResourceValueFrame.CLOSED);
				if (DEBUG) System.out.print("(failed attempt to close)");
			}

			if (dest.isExceptionHandler()) {
				// Clear stack, push value for exception
				if (fact.isValid()) {
					tmpFact = modifyFrame(fact, tmpFact);
					tmpFact.clearStack();
					tmpFact.pushValue(ResourceValue.notInstance());
				}
			}
		}

		// Make the resource nonexistent if it is compared against null
		int edgeType = edge.getType();
		if (edgeType == IFCMP_EDGE || edgeType == FALL_THROUGH_EDGE) {
			InstructionHandle lastInSourceHandle = source.getLastInstruction();
			if (lastInSourceHandle != null) {
				Instruction lastInSource = lastInSourceHandle.getInstruction();
				if (lastInSource instanceof IFNULL || lastInSource instanceof IFNONNULL) {
					// Get the frame at the if statement
					ResourceValueFrame startFrame = getStartFact(source);
					if (startFrame.isValid()) {
						// The source block has a valid start fact.
						// That means it is safe to inspect the frame at the If instruction.
						ResourceValueFrame frameAtIf = getFactAtLocation(new Location(lastInSourceHandle, source));
						ResourceValue topValue = frameAtIf.getValue(frameAtIf.getNumSlots() - 1);

						if (topValue.isInstance()) {
							if ((lastInSource instanceof IFNULL && edgeType == IFCMP_EDGE) ||
								(lastInSource instanceof IFNONNULL && edgeType == FALL_THROUGH_EDGE)) {
								//System.out.println("**** making resource nonexistent on edge "+edge.getId());
								tmpFact = modifyFrame(fact, tmpFact);
								tmpFact.setStatus(ResourceValueFrame.NONEXISTENT);
							}
						}
					}
				}
			}
		}

		if (tmpFact != null)
			fact = tmpFact;

		result.mergeWith(fact);
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ResourceValueFrame fact)
		throws DataflowAnalysisException {

		visitor.setFrame(fact);
		visitor.transferInstruction(handle, basicBlock);

	}

	private final BitSet checked = new BitSet();
	private final BitSet finalFieldLoadSet = new BitSet();

	private boolean isFinalFieldLoad(InstructionHandle handle) {
		if (DEBUG) System.out.print("[Checking " + handle + " for load of final field ");
		int offset = handle.getPosition();
		if (!checked.get(offset)) {
			checked.set(offset);

			Instruction ins = handle.getInstruction();
			if (ins instanceof GETFIELD || ins instanceof GETSTATIC) {
				FieldInstruction fins = (FieldInstruction) ins;

				ConstantPoolGen cpg = methodGen.getConstantPool();
				String className = fins.getClassName(cpg);
				String fieldName = fins.getName(cpg);

				if (DEBUG) System.out.print("field=" + className + "." + fieldName);

				try {
					Field field = Lookup.findField(className, fieldName);
					if (field != null)
						finalFieldLoadSet.set(offset, field.isFinal());
				} catch (ClassNotFoundException e) {
					lookupFailureCallback.reportMissingClass(e);
				}
			}
		}
		boolean result = finalFieldLoadSet.get(offset);
		if (DEBUG) System.out.print(" ==> " + result + "]");
		return result;
	}

}

// vim:ts=4
