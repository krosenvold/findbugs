/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

import edu.umd.cs.findbugs.ba.type.Type;
import edu.umd.cs.findbugs.ba.type.TypeMerger;
import edu.umd.cs.findbugs.ba.type.TypeRepository;

import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

public class BetterTypeAnalysis extends FrameDataflowAnalysis<Type, BetterTypeFrame> {
	private static final String JAVA_LANG_THROWABLE_SIGNATURE = "Ljava/lang/Throwable;";

	private MethodGen methodGen;
	private String[] parameterSignatureList;
	private CFG cfg;
	private TypeRepository typeRepository;
	private TypeMerger typeMerger;
	private RepositoryLookupFailureCallback lookupFailureCallback;

	public BetterTypeAnalysis(MethodGen methodGen, String[] parameterSignatureList,
			CFG cfg, DepthFirstSearch dfs,
			TypeRepository typeRepository, TypeMerger typeMerger,
			RepositoryLookupFailureCallback lookupFailureCallback) {
		super(dfs);
		this.methodGen = methodGen;
		this.parameterSignatureList = parameterSignatureList;
		this.cfg = cfg;
		this.typeRepository = typeRepository;
		this.typeMerger = typeMerger;
		this.lookupFailureCallback = lookupFailureCallback;
	}

	public BetterTypeFrame createFact() {
		return new BetterTypeFrame(methodGen.getMaxLocals());
	}

	public void initEntryFact(BetterTypeFrame result) {
		// TODO: implement
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, BetterTypeFrame fact)
		throws DataflowAnalysisException {
		// TODO: implement
	}

	public void meetInto(BetterTypeFrame fact, Edge edge, BetterTypeFrame result)
		throws DataflowAnalysisException {

		// TODO: implement ACCURATE_EXCEPTIONS

		if (fact.isValid() && edge.getTarget().isExceptionHandler()) {
			BetterTypeFrame tmpFact = null;

			// Exception handler.
			// Clear stack and push exception handler catch type.

			tmpFact = modifyFrame(fact, tmpFact);
			tmpFact.clearStack();

			CodeExceptionGen exceptionGen = edge.getTarget().getExceptionGen();
			org.apache.bcel.generic.ObjectType catchType = exceptionGen.getCatchType();
			if (catchType == null) {
				tmpFact.pushValue(typeRepository.classTypeFromSignature(JAVA_LANG_THROWABLE_SIGNATURE));
			} else {
				tmpFact.pushValue(typeRepository.classTypeFromDottedClassName(catchType.getClassName()));
			}

			if (tmpFact != null)
				fact = tmpFact;
		}

		mergeInto(fact, result);

	}

	protected Type mergeValues(BetterTypeFrame frame, int slot, Type a, Type b)
		throws DataflowAnalysisException {
		try {
			return typeMerger.mergeTypes(a, b);
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
			throw new DataflowAnalysisException("Missing class for type analysis", e);
		}
	}
}

// vim:ts=4
