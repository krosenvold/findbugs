/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.ch.Subtypes;

public class PruneUnconditionalExceptionThrowerEdges implements EdgeTypes {
	private static final boolean DEBUG = Boolean.getBoolean("cfg.prune.throwers.debug");

	private MethodGen methodGen;
	private CFG cfg;
	private ConstantPoolGen cpg;
	private AnalysisContext analysisContext;
	private  static final BitSet RETURN_OPCODE_SET = new BitSet();
	static {
		RETURN_OPCODE_SET.set(Constants.ARETURN);
		RETURN_OPCODE_SET.set(Constants.IRETURN);
		RETURN_OPCODE_SET.set(Constants.LRETURN);
		RETURN_OPCODE_SET.set(Constants.DRETURN);
		RETURN_OPCODE_SET.set(Constants.FRETURN);
		RETURN_OPCODE_SET.set(Constants.RETURN);
	}

	public PruneUnconditionalExceptionThrowerEdges(MethodGen methodGen, CFG cfg, ConstantPoolGen cpg,
	                                               AnalysisContext analysisContext) {
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.cpg = cpg;
		this.analysisContext = analysisContext;
	}

	static Map<XMethod,Boolean> cachedResults = new HashMap<XMethod,Boolean>();
	public void execute() throws CFGBuilderException, DataflowAnalysisException {
		if (AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.CONSERVE_SPACE))
			throw new IllegalStateException("This should not happen");

		Set<Edge> deletedEdgeSet = new HashSet<Edge>();

		if (DEBUG)
			System.out.println("PruneUnconditionalExceptionThrowerEdges: examining " +
			        SignatureConverter.convertMethodSignature(methodGen));
		 Subtypes subtypes = AnalysisContext.currentAnalysisContext()
			.getSubtypes();
		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
			BasicBlock basicBlock = i.next();
			if (!basicBlock.isExceptionThrower())
				continue;

			Instruction exceptionThrower = basicBlock.getExceptionThrower().getInstruction();
			if (!(exceptionThrower instanceof InvokeInstruction))
				continue;

			InvokeInstruction inv = (InvokeInstruction) exceptionThrower;
			try {
				String className = inv.getClassName(cpg);
				if (className.startsWith("["))
					continue;
				String methodSig = inv.getSignature(cpg);
				if (!methodSig.endsWith("V")) 
					continue;
				JavaClass javaClass = Repository.lookupClass(className);
				ClassContext classContext = analysisContext.getClassContext(javaClass);

				if (DEBUG) System.out.println("\tlooking up method for " + basicBlock.getExceptionThrower());
				JavaClassAndMethod classAndMethod = Hierarchy.findExactMethod(inv, cpg);
				if (classAndMethod == null) {
					if (DEBUG) System.out.println("\tNOT FOUND");
					continue;
				}
				Method method = classAndMethod.getMethod();
				XMethod xMethod = XFactory.createXMethod(javaClass, method);
				if (DEBUG) System.out.println("\tFound " + method);

				// FIXME: for now, only allow static and private methods.
				// Could also allow final methods (but would require class hierarchy
				// search).
				if (!(method.isStatic() || method.isPrivate() || method.isFinal() || javaClass.isFinal() || !subtypes.hasSubtypes(javaClass)))
					continue;
				
				// Ignore abstract and native methods
				if (method.getCode() == null) continue;
				
				BitSet bytecodeSet = classContext.getBytecodeSet(method);
				
				Boolean result = cachedResults.get(xMethod);
				if (result == null) {
					result = bytecodeSet.intersects(RETURN_OPCODE_SET);
					cachedResults.put(xMethod, result);
				}
				if (false && result.booleanValue()) {
				    MethodGen calledMethodGen = classContext.getMethodGen(method);
					// Ignore abstract and native methods

					if (calledMethodGen == null)
						continue;

					// Analyze exception paths of called method
					// to see if it always throws an unhandled exception.
					CFG calledCFG = classContext.getCFG(method);
					ReturnPathDataflow pathDataflow = classContext
							.getReturnPathDataflow(method);
					ReturnPath pathValue = pathDataflow.getStartFact(calledCFG
							.getExit());

					result = pathValue.getKind() != ReturnPath.RETURNS;
					// System.out.println("isThrower: " + result + " " + method.getCode().getLength() + " " + method);
					if (true) cachedResults.put(xMethod, result);
				}

				if (result.booleanValue()) {
					// Method always throws an unhandled exception
					// Remove the normal control flow edge from the CFG.
					Edge fallThrough = cfg.getOutgoingEdgeWithType(basicBlock,
							FALL_THROUGH_EDGE);
					if (fallThrough != null) {
						if (DEBUG) {
							System.out.println("\tREMOVING normal return for: " + xMethod);
						}
						deletedEdgeSet.add(fallThrough);
					}
				}
			} catch (ClassNotFoundException e) {
				analysisContext.getLookupFailureCallback().reportMissingClass(e);
			}
		}

		// Remove all edges marked for deletion
		for (Edge edge : deletedEdgeSet) {
			cfg.removeEdge(edge);
		}
	}
}

// vim:ts=4
