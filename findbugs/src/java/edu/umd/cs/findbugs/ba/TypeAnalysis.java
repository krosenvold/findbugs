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

import java.util.*;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

/**
 * A forward dataflow analysis to determine the types of all values
 * in the Java stack frame at all points in a Java method.
 * The values include local variables and values on the Java operand stack.
 * <p/>
 * <p> As a side effect, the analysis computes the exception
 * set throwable on each exception edge in the CFG.
 * This information can be used to prune infeasible exception
 * edges, and mark exception edges which propagate only
 * implicit exceptions.
 *
 * @author David Hovemeyer
 * @see Dataflow
 * @see DataflowAnalysis
 * @see TypeFrame
 */
public class TypeAnalysis extends FrameDataflowAnalysis<Type, TypeFrame>
        implements EdgeTypes {

	private static final boolean DEBUG = Boolean.getBoolean("ta.debug");

	/**
	 * Compute what kinds of exceptions can propagate
	 * on each exception edge.
	 */
	private static final boolean ACCURATE_EXCEPTIONS =
	        Boolean.getBoolean("ta.accurateExceptions") || ClassContext.PRUNE_INFEASIBLE_EXCEPTION_EDGES;

	/**
	 * Repository of information about thrown exceptions computed for
	 * a basic block and its outgoing exception edges.
	 * It contains a result TypeFrame, which is used to detect
	 * when the exception information needs to be recomputed
	 * for the block.
	 */
	private class CachedExceptionSet {
		private TypeFrame result;
		private ExceptionSet exceptionSet;
		private Map<Edge, ExceptionSet> edgeExceptionMap;

		public CachedExceptionSet(TypeFrame result, ExceptionSet exceptionSet) {
			this.result = result;
			this.exceptionSet = exceptionSet;
			this.edgeExceptionMap = new HashMap<Edge, ExceptionSet>();
		}

		public boolean isUpToDate(TypeFrame result) {
			return this.result.equals(result);
		}

		public ExceptionSet getExceptionSet() {
			return exceptionSet;
		}

		public void setEdgeExceptionSet(Edge edge, ExceptionSet exceptionSet) {
			edgeExceptionMap.put(edge, exceptionSet);
		}

		public ExceptionSet getEdgeExceptionSet(Edge edge) {
			ExceptionSet edgeExceptionSet = edgeExceptionMap.get(edge);
			if (edgeExceptionSet == null) {
				edgeExceptionSet = exceptionSetFactory.createExceptionSet();
				edgeExceptionMap.put(edge, edgeExceptionSet);
			}
			return edgeExceptionSet;
		}
	}

	private MethodGen methodGen;
	private CFG cfg;
	private TypeMerger typeMerger;
	private TypeFrameModelingVisitor visitor;
	private Map<BasicBlock, CachedExceptionSet> thrownExceptionSetMap;
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private ExceptionSetFactory exceptionSetFactory;

	/**
	 * Constructor.
	 *
	 * @param methodGen             the MethodGen whose CFG we'll be analyzing
	 * @param cfg                   the control flow graph
	 * @param dfs                   DepthFirstSearch of the method
	 * @param typeMerger            object to merge types
	 * @param visitor               a TypeFrameModelingVisitor to use to model the effect
	 *                              of instructions
	 * @param lookupFailureCallback lookup failure callback
	 * @param exceptionSetFactory   factory for creating ExceptionSet objects
	 */
	public TypeAnalysis(MethodGen methodGen, CFG cfg, DepthFirstSearch dfs,
	                    TypeMerger typeMerger, TypeFrameModelingVisitor visitor,
	                    RepositoryLookupFailureCallback lookupFailureCallback,
	                    ExceptionSetFactory exceptionSetFactory) {
		super(dfs);
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.typeMerger = typeMerger;
		this.visitor = visitor;
		this.thrownExceptionSetMap = new HashMap<BasicBlock, CachedExceptionSet>();
		this.lookupFailureCallback = lookupFailureCallback;
		this.exceptionSetFactory = exceptionSetFactory;
	}

	/**
	 * Constructor.
	 *
	 * @param methodGen             the MethodGen whose CFG we'll be analyzing
	 * @param cfg                   the control flow graph
	 * @param dfs                   DepthFirstSearch of the method
	 * @param typeMerger            object to merge types
	 * @param lookupFailureCallback lookup failure callback
	 * @param exceptionSetFactory   factory for creating ExceptionSet objects
	 */
	public TypeAnalysis(MethodGen methodGen, CFG cfg, DepthFirstSearch dfs,
	                    TypeMerger typeMerger, RepositoryLookupFailureCallback lookupFailureCallback,
	                    ExceptionSetFactory exceptionSetFactory) {
		this(methodGen, cfg, dfs, typeMerger,
		        new TypeFrameModelingVisitor(methodGen.getConstantPool()), lookupFailureCallback,
		        exceptionSetFactory);
	}

	/**
	 * Constructor which uses StandardTypeMerger.
	 *
	 * @param methodGen             the MethodGen whose CFG we'll be analyzing
	 * @param cfg                   the control flow graph
	 * @param dfs                   DepthFirstSearch of the method
	 * @param lookupFailureCallback callback for Repository lookup failures
	 * @param exceptionSetFactory   factory for creating ExceptionSet objects
	 */
	public TypeAnalysis(MethodGen methodGen, CFG cfg, DepthFirstSearch dfs,
	                    RepositoryLookupFailureCallback lookupFailureCallback,
	                    ExceptionSetFactory exceptionSetFactory) {
		this(methodGen, cfg, dfs,
		        new StandardTypeMerger(lookupFailureCallback, exceptionSetFactory),
		        lookupFailureCallback, exceptionSetFactory);
	}

	/**
	 * Get the set of exceptions that can be thrown on given edge.
	 * This should only be called after the analysis completes.
	 *
	 * @param edge the Edge
	 * @return the ExceptionSet
	 */
	public ExceptionSet getEdgeExceptionSet(Edge edge) {
		CachedExceptionSet cachedExceptionSet = thrownExceptionSetMap.get(edge.getSource());
		return cachedExceptionSet.getEdgeExceptionSet(edge);
	}

	public TypeFrame createFact() {
		return new TypeFrame(methodGen.getMaxLocals());
	}

	public void initEntryFact(TypeFrame result) {
		// Make the frame valid
		result.setValid();

		int slot = 0;

		// Clear the stack slots in the frame
		result.clearStack();

		// Add local for "this" pointer, if present
		if (!methodGen.isStatic())
			result.setValue(slot++, new ObjectType(methodGen.getClassName()));

		// Add locals for parameters.
		// Note that long and double parameters need to be handled
		// specially because they occupy two locals.
		Type[] argumentTypes = methodGen.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; ++i) {
			Type argType = argumentTypes[i];

			// Add special "extra" type for long or double params.
			// These occupy the slot before the "plain" type.
			if (argType.getType() == Constants.T_LONG) {
				result.setValue(slot++, TypeFrame.getLongExtraType());
			} else if (argType.getType() == Constants.T_DOUBLE) {
				result.setValue(slot++, TypeFrame.getDoubleExtraType());
			}

			// Add the plain parameter type.
			result.setValue(slot++, argType);
		}

		// Set remaining locals to BOTTOM; this will cause any
		// uses of them to be flagged
		while (slot < methodGen.getMaxLocals())
			result.setValue(slot++, TypeFrame.getBottomType());
	}

	public void copy(TypeFrame source, TypeFrame dest) {
		dest.copyFrom(source);
	}

	public void initResultFact(TypeFrame result) {
		// This is important.  Sometimes we need to use a result value
		// before having a chance to initialize it.  We don't want such
		// values to corrupt other TypeFrame values that we merge them with.
		// So, all result values must be TOP.
		result.setTop();
	}

	public void makeFactTop(TypeFrame fact) {
		fact.setTop();
	}

	public boolean isFactValid(TypeFrame fact) {
		return fact.isValid();
	}

	public boolean same(TypeFrame fact1, TypeFrame fact2) {
		return fact1.sameAs(fact2);
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, TypeFrame fact)
	        throws DataflowAnalysisException {
		visitor.setFrame(fact);
		visitor.analyzeInstruction(handle.getInstruction());
	}

	public void endTransfer(BasicBlock basicBlock, InstructionHandle end, Object result)
	        throws DataflowAnalysisException {

		// Do nothing if we're not computing propagated exceptions
		if (!ACCURATE_EXCEPTIONS)
			return;

		// Also, nothing to do if the block is not an exception thrower
		if (!basicBlock.isExceptionThrower())
			return;

		// If cached results are up to date, don't recompute.
		CachedExceptionSet cachedExceptionSet = getCachedExceptionSet(basicBlock);
		if (cachedExceptionSet.isUpToDate((TypeFrame) result))
			return;

		// Figure out what exceptions can be thrown out
		// of the basic block, and mark each exception edge
		// with the set of exceptions which can be propagated
		// along the edge.

		// Compute exceptions that can be thrown by the
		// basic block.
		cachedExceptionSet = computeBlockExceptionSet(basicBlock, (TypeFrame) result);

		// For each outgoing exception edge, compute exceptions
		// that can be thrown.  This assumes that the exception
		// edges are enumerated in decreasing order of priority.
		// In the process, this will remove exceptions from
		// the thrown exception set.
		ExceptionSet thrownExceptionSet = cachedExceptionSet.getExceptionSet().duplicate();
		for (Iterator<Edge> i = cfg.outgoingEdgeIterator(basicBlock); i.hasNext();) {
			Edge edge = i.next();
			if (!edge.isExceptionEdge())
				continue;

			cachedExceptionSet.setEdgeExceptionSet(edge, computeEdgeExceptionSet(edge, thrownExceptionSet));
		}
	}

	public void meetInto(TypeFrame fact, Edge edge, TypeFrame result) throws DataflowAnalysisException {
		BasicBlock basicBlock = edge.getTarget();

		if (basicBlock.isExceptionHandler() && fact.isValid()) {
			// Special case: when merging predecessor facts for entry to
			// an exception handler, we clear the stack and push a
			// single entry for the exception object.  That way, the locals
			// can still be merged.
			CodeExceptionGen exceptionGen = basicBlock.getExceptionGen();
			TypeFrame tmpFact = createFact();
			tmpFact.copyFrom(fact);
			tmpFact.clearStack();

			// Determine the type of exception(s) caught.
			Type catchType = null;

			if (ACCURATE_EXCEPTIONS) {
				try {
					// Ideally, the exceptions that can be propagated
					// on this edge has already been computed.
					CachedExceptionSet cachedExceptionSet = getCachedExceptionSet(edge.getSource());
					ExceptionSet edgeExceptionSet = cachedExceptionSet.getEdgeExceptionSet(edge);
					if (!edgeExceptionSet.isEmpty()) {
						//System.out.println("Using computed edge exception set!");
						catchType = ExceptionObjectType.fromExceptionSet(edgeExceptionSet);
					}
				} catch (ClassNotFoundException e) {
					lookupFailureCallback.reportMissingClass(e);
				}
			}

			if (catchType == null) {
				// No information about propagated exceptions, so
				// pick a type conservatively using the handler catch type.
				catchType = exceptionGen.getCatchType();
				if (catchType == null)
					catchType = Type.THROWABLE; // handle catches anything throwable
			}

			tmpFact.pushValue(catchType);
			fact = tmpFact;
		}

		mergeInto(fact, result);
	}

	protected Type mergeValues(TypeFrame frame, int slot, Type a, Type b)
	        throws DataflowAnalysisException {
		return typeMerger.mergeTypes(a, b);
	}

	/**
	 * Get the cached set of exceptions that can be thrown
	 * from given basic block.  If this information hasn't
	 * been computed yet, then an empty exception set is
	 * returned.
	 *
	 * @param basicBlock the block to get the cached exception set for
	 * @return the CachedExceptionSet for the block
	 */
	private CachedExceptionSet getCachedExceptionSet(BasicBlock basicBlock) {
		CachedExceptionSet cachedExceptionSet = thrownExceptionSetMap.get(basicBlock);
		if (cachedExceptionSet == null) {
			// When creating the cached exception type set for the first time:
			// - the block result is set to TOP, so it won't match
			//   any block result that has actually been computed
			//   using the analysis transfer function
			// - the exception set is created as empty (which makes it
			//   return TOP as its common superclass)

			TypeFrame top = createFact();
			makeFactTop(top);
			cachedExceptionSet = new CachedExceptionSet(top, exceptionSetFactory.createExceptionSet());

			thrownExceptionSetMap.put(basicBlock, cachedExceptionSet);
		}

		return cachedExceptionSet;
	}

	/**
	 * Compute the set of exceptions that can be
	 * thrown from the given basic block.
	 * This should only be called if the existing cached
	 * exception set is out of date.
	 *
	 * @param basicBlock the basic block
	 * @param result     the result fact for the block; this is used
	 *                   to determine whether or not the cached exception
	 *                   set is up to date
	 * @return the cached exception set for the block
	 */
	private CachedExceptionSet computeBlockExceptionSet(BasicBlock basicBlock, TypeFrame result)
	        throws DataflowAnalysisException {

		ExceptionSet exceptionSet = null;
		try {
			exceptionSet = computeThrownExceptionTypes(basicBlock);
		} catch (ClassNotFoundException e) {
			// Special case: be as conservative as possible
			// if a class hierarchy lookup fails.
			lookupFailureCallback.reportMissingClass(e);
			exceptionSet = exceptionSetFactory.createExceptionSet();
			exceptionSet.addExplicit(Type.THROWABLE);
		}

		TypeFrame copyOfResult = createFact();
		copy(result, copyOfResult);

		CachedExceptionSet cachedExceptionSet = new CachedExceptionSet(copyOfResult, exceptionSet);
		thrownExceptionSetMap.put(basicBlock, cachedExceptionSet);

		return cachedExceptionSet;
	}

	/**
	 * Based on the set of exceptions that can be thrown
	 * from the source basic block,
	 * compute the set of exceptions that can propagate
	 * along given exception edge.  This method should be
	 * called for each outgoing exception edge in sequence,
	 * so the caught exceptions can be removed from the
	 * thrown exception set as needed.
	 *
	 * @param edge               the exception edge
	 * @param thrownExceptionSet current set of exceptions that
	 *                           can be thrown, taking earlier (higher priority)
	 *                           exception edges into account
	 * @return the set of exceptions that can propagate
	 *         along this edge
	 */
	private ExceptionSet computeEdgeExceptionSet(Edge edge, ExceptionSet thrownExceptionSet) {

		ExceptionSet result = exceptionSetFactory.createExceptionSet();

		if (edge.getType() == UNHANDLED_EXCEPTION_EDGE) {
			// The unhandled exception edge always comes
			// after all of the handled exception edges.
			result.addAll(thrownExceptionSet);
			thrownExceptionSet.clear();
			return result;
		}

		BasicBlock handlerBlock = edge.getTarget();
		CodeExceptionGen handler = handlerBlock.getExceptionGen();
		ObjectType catchType = handler.getCatchType();

		if (Hierarchy.isUniversalExceptionHandler(catchType)) {
			result.addAll(thrownExceptionSet);
			thrownExceptionSet.clear();
		} else {
			// Go through the set of thrown exceptions.
			// Any that will DEFINITELY be caught be this handler, remove.
			// Any that MIGHT be caught, but won't definitely be caught,
			// remain.

			for (ExceptionSet.ThrownExceptionIterator i = thrownExceptionSet.iterator(); i.hasNext();) {
				//ThrownException thrownException = i.next();
				ObjectType thrownType = i.next();
				boolean explicit = i.isExplicit();

				if (DEBUG)
					System.out.println("\texception type " + thrownType +
					        ", catch type " + catchType);

				try {
					if (Hierarchy.isSubtype(thrownType, catchType)) {
						// Exception can be thrown along this edge
						result.add(thrownType, explicit);
	
						// And it will definitely be caught
						i.remove();

						if (DEBUG)
							System.out.println("\tException is subtype of catch type: " +
							        "will definitely catch");
					} else if (Hierarchy.isSubtype(catchType, thrownType)) {
						// Exception possibly thrown along this edge
						result.add(thrownType, explicit);

						if (DEBUG)
							System.out.println("\tException is supertype of catch type: " +
							        "might catch");
					}
				} catch (ClassNotFoundException e) {
					// As a special case, if a class hierarchy lookup
					// fails, then we will conservatively assume that the
					// exception in question CAN, but WON'T NECESSARILY
					// be caught by the handler.
					result.add(thrownType, explicit);
				}
			}
		}

		return result;
	}

	/**
	 * Compute the set of exception types that can
	 * be thrown by given basic block.
	 *
	 * @param basicBlock the basic block
	 * @return the set of exceptions that can be thrown by the block
	 */
	private ExceptionSet computeThrownExceptionTypes(BasicBlock basicBlock)
	        throws ClassNotFoundException, DataflowAnalysisException {

		ExceptionSet exceptionTypeSet = exceptionSetFactory.createExceptionSet();
		InstructionHandle pei = basicBlock.getExceptionThrower();
		Instruction ins = pei.getInstruction();

		// Get the exceptions that BCEL knows about.
		// Note that all of these are unchecked.
		ExceptionThrower exceptionThrower = (ExceptionThrower) ins;
		Class[] exceptionList = exceptionThrower.getExceptions();
		for (int i = 0; i < exceptionList.length; ++i) {
			exceptionTypeSet.addImplicit(new ObjectType(exceptionList[i].getName()));
		}

		// Assume that an Error may be thrown by any instruction.
		exceptionTypeSet.addImplicit(Hierarchy.ERROR_TYPE);

		if (ins instanceof ATHROW) {
			// For ATHROW instructions, we generate *two* blocks
			// for which the ATHROW is an exception thrower.
			//
			// - The first, empty basic block, does the null check
			// - The second block, which actually contains the ATHROW,
			//   throws the object on the top of the operand stack
			//
			// We make a special case of the block containing the ATHROW,
			// by removing all of the implicit exceptions,
			// and using type information to figure out what is thrown.

			if (basicBlock.containsInstruction(pei)) {
				// This is the actual ATHROW, not the null check
				// and implicit exceptions.
				exceptionTypeSet.clear();

				// The frame containing the thrown value is the start fact
				// for the block, because ATHROW is guaranteed to be
				// the only instruction in the block.
				TypeFrame frame = getStartFact(basicBlock);
	
				// Check whether or not the frame is valid.
				// Sun's javac sometimes emits unreachable code.
				// For example, it will emit code that follows a JSR
				// subroutine call that never returns.
				// If the frame is invalid, then we can just make
				// a conservative assumption that anything could be
				// thrown at this ATHROW.
				if (!frame.isValid()) {
					exceptionTypeSet.addExplicit(Type.THROWABLE);
				} else {
					Type throwType = frame.getTopValue();
					if (throwType instanceof ObjectType) {
						exceptionTypeSet.addExplicit((ObjectType) throwType);
					} else if (throwType instanceof ExceptionObjectType) {
						exceptionTypeSet.addAll(((ExceptionObjectType) throwType).getExceptionSet());
					} else {
						// Not sure what is being thrown here.
						// Be conservative.
						if (DEBUG) {
							System.out.println("Non object type " + throwType +
							        " thrown by " + pei + " in " +
							        SignatureConverter.convertMethodSignature(methodGen));
						}
						exceptionTypeSet.addExplicit(Type.THROWABLE);
					}
				}
			}
		}

		// If it's an InvokeInstruction, add declared exceptions and RuntimeException
		if (ins instanceof InvokeInstruction) {
			ConstantPoolGen cpg = methodGen.getConstantPool();

			InvokeInstruction inv = (InvokeInstruction) ins;
			ObjectType[] declaredExceptionList = Hierarchy.findDeclaredExceptions(inv, cpg);
			if (declaredExceptionList == null) {
				// Couldn't find declared exceptions,
				// so conservatively assume it could thrown any checked exception.
				if (DEBUG)
					System.out.println("Couldn't find declared exceptions for " +
					        SignatureConverter.convertMethodSignature(inv, cpg));
				exceptionTypeSet.addExplicit(Hierarchy.EXCEPTION_TYPE);
			} else {
				for (int i = 0; i < declaredExceptionList.length; ++i) {
					exceptionTypeSet.addExplicit(declaredExceptionList[i]);
				}
			}

			exceptionTypeSet.addImplicit(Hierarchy.RUNTIME_EXCEPTION_TYPE);
		}

		if (DEBUG) System.out.println(pei + " can throw " + exceptionTypeSet);

		return exceptionTypeSet;
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + TypeAnalysis.class.getName() + " <class file>");
			System.exit(1);
		}

		DataflowTestDriver<TypeFrame, TypeAnalysis> driver = new DataflowTestDriver<TypeFrame, TypeAnalysis>() {
			public Dataflow<TypeFrame, TypeAnalysis> createDataflow(ClassContext classContext, Method method)
			        throws CFGBuilderException, DataflowAnalysisException {
				return classContext.getTypeDataflow(method);
			}
		};

		driver.execute(argv[0]);
	}
}

// vim:ts=3
