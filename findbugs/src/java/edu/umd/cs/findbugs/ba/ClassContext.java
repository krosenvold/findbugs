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

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A ClassContext caches all of the auxiliary objects used to analyze
 * the methods of a class.  That way, these objects don't need to
 * be created over and over again.
 *
 * @author David Hovemeyer
 */
public class ClassContext implements AnalysisFeatures {
	/**
	 * We only do pruning of infeasible exception edges
	 * if the <code>WORK_HARD</code> analysis feature
	 * is enabled.
	 */
	public static final boolean PRUNE_INFEASIBLE_EXCEPTION_EDGES = WORK_HARD;

	/**
	 * Only try to determine unconditional exception throwers
	 * if we're not trying to conserve space.
	 */
	public static final boolean PRUNE_UNCONDITIONAL_EXCEPTION_THROWER_EDGES =
		!CONSERVE_SPACE;

	public static final boolean DEBUG = Boolean.getBoolean("classContext.debug");

	private static final int PRUNED_INFEASIBLE_EXCEPTIONS = 1;
	private static final int PRUNED_UNCONDITIONAL_THROWERS = 2;

	private static final boolean TIME_ANALYSES = Boolean.getBoolean("classContext.timeAnalyses");

	private static final boolean DEBUG_CFG = Boolean.getBoolean("classContext.debugCFG");

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static int depth;
	private static void indent() {
		for (int i = 0; i < depth; ++i) System.out.print("  ");
	}

	private abstract class AnalysisFactory<AnalysisResult> {
		private String analysisName;
		private IdentityHashMap<Method, AnalysisResult> map = new IdentityHashMap<Method, AnalysisResult>();

		public AnalysisFactory(String analysisName) {
			this.analysisName = analysisName;
		}

		public AnalysisResult getAnalysis(Method method) throws CFGBuilderException, DataflowAnalysisException {
			AnalysisResult result = map.get(method);
			if (result == null) {
				if (TIME_ANALYSES) {
					++depth;
					indent();
					System.out.println("CC: Starting " + analysisName + " for " +
						SignatureConverter.convertMethodSignature(jclass, method) + ":");
				}

				long begin = System.currentTimeMillis();

				try {
					result = analyze(method);
				} catch (DataflowAnalysisException e) {
					if (TIME_ANALYSES) {
						long end = System.currentTimeMillis();
						indent();
						System.out.println("CC: " + analysisName + " killed by exception after " +
							(end-begin) + " millis");
						e.printStackTrace();
						--depth;
					}
					throw e;
				}

				if (TIME_ANALYSES) {
					long end = System.currentTimeMillis();
					indent();
					System.out.println("CC: finished " + analysisName + " in " + (end - begin) + " millis");
					--depth;
				}

				map.put(method, result);
			}
			return result;
		}

		protected abstract AnalysisResult analyze(Method method)
			throws CFGBuilderException, DataflowAnalysisException;
	}

	private abstract class NoExceptionAnalysisFactory<AnalysisResult> extends AnalysisFactory<AnalysisResult> {
		public NoExceptionAnalysisFactory(String analysisName) { super(analysisName); }

		public AnalysisResult getAnalysis(Method method) {
			try {
				return super.getAnalysis(method);
			} catch (DataflowAnalysisException e) {
				throw new IllegalStateException("Should not happen");
			} catch (CFGBuilderException e) {
				throw new IllegalStateException("Should not happen");
			}
		}
	}

	private abstract class NoDataflowAnalysisFactory<AnalysisResult> extends AnalysisFactory<AnalysisResult> {
		public NoDataflowAnalysisFactory(String analysisName) { super(analysisName); }

		public AnalysisResult getAnalysis(Method method) throws CFGBuilderException {
			try {
				return super.getAnalysis(method);
			} catch (DataflowAnalysisException e) {
				throw new IllegalStateException("Should not happen");
			}
		}
	}

	private static final Set<String> busyCFGSet = new HashSet<String>();

	private class CFGFactory extends AnalysisFactory<CFG> {

		public CFGFactory() { super("CFG construction"); }

		public CFG getAnalysis(Method method) throws CFGBuilderException {
			try {
				return super.getAnalysis(method);
			} catch (DataflowAnalysisException e) {
				throw new IllegalStateException("Should not happen");
			}
		}

		public CFG getRawCFG(Method method) throws CFGBuilderException {
			return getAnalysis(method);
		}

		public CFG getRefinedCFG(Method method) throws CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);

			CFG cfg = getRawCFG(method);

			// HACK:
			// Due to recursive method invocations, we may get a recursive
			// request for the pruned CFG of a method.  In this case,
			// we just return the raw CFG.
			String methodId = methodGen.getClassName()+"."+methodGen.getName()+":"+methodGen.getSignature();
			if (DEBUG_CFG) {
				indent();
				System.out.println("CC: getting refined CFG for " + methodId);
			}
			if (DEBUG) System.out.println("ClassContext: request to prune " + methodId);
			if (!busyCFGSet.add(methodId))
				return cfg;

			if (PRUNE_INFEASIBLE_EXCEPTION_EDGES && !cfg.isFlagSet(PRUNED_INFEASIBLE_EXCEPTIONS)) {
				try {
					TypeDataflow typeDataflow = getTypeDataflow(method);
					new PruneInfeasibleExceptionEdges(cfg, getMethodGen(method), typeDataflow).execute();
				} catch (DataflowAnalysisException e) {
					// FIXME: should report the error
				} catch (ClassNotFoundException e) {
					lookupFailureCallback.reportMissingClass(e);
				}
			}
			cfg.setFlags(cfg.getFlags() | PRUNED_INFEASIBLE_EXCEPTIONS);

			if (PRUNE_UNCONDITIONAL_EXCEPTION_THROWER_EDGES && !cfg.isFlagSet(PRUNED_UNCONDITIONAL_THROWERS)) {
				try {
					new PruneUnconditionalExceptionThrowerEdges(
						methodGen, cfg, getConstantPoolGen(), lookupFailureCallback).execute();
				} catch (DataflowAnalysisException e) {
					// FIXME: should report the error
				}
			}
			cfg.setFlags(cfg.getFlags() | PRUNED_UNCONDITIONAL_THROWERS);

			busyCFGSet.remove(methodId);

			return cfg;
		}

		protected CFG analyze(Method method) throws CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);
			CFGBuilder cfgBuilder = CFGBuilderFactory.create(methodGen);
			cfgBuilder.build();
			return cfgBuilder.getCFG();
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private JavaClass jclass;
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private NoExceptionAnalysisFactory<MethodGen> methodGenFactory =
	new NoExceptionAnalysisFactory<MethodGen>("MethodGen construction") {
		protected MethodGen analyze(Method method) {
			if (method.getCode() == null)
				return null;
			return new MethodGen(method, jclass.getClassName(), getConstantPoolGen());
		}
	};

	private CFGFactory cfgFactory = new CFGFactory();

	private AnalysisFactory<ValueNumberDataflow> vnaDataflowFactory =
	new AnalysisFactory<ValueNumberDataflow>("value number analysis") {
		protected ValueNumberDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			ValueNumberAnalysis analysis = new ValueNumberAnalysis(methodGen, dfs, lookupFailureCallback);
			CFG cfg = getCFG(method);
			ValueNumberDataflow vnaDataflow = new ValueNumberDataflow(cfg, analysis);
			vnaDataflow.execute();
			return vnaDataflow;
		}
	};

	private AnalysisFactory<IsNullValueDataflow> invDataflowFactory =
	new AnalysisFactory<IsNullValueDataflow>("null value analysis") {
		protected IsNullValueDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);
			CFG cfg = getCFG(method);
			ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			AssertionMethods assertionMethods = getAssertionMethods();

			IsNullValueAnalysis invAnalysis = new IsNullValueAnalysis(methodGen, cfg, vnaDataflow, dfs, assertionMethods);
			IsNullValueDataflow invDataflow = new IsNullValueDataflow(cfg, invAnalysis);
			invDataflow.execute();
			return invDataflow;
		}
	};

	private AnalysisFactory<TypeDataflow> typeDataflowFactory =
	new AnalysisFactory<TypeDataflow>("type analysis") {
		protected TypeDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);
			CFG cfg = getRawCFG(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			ExceptionSetFactory exceptionSetFactory = getExceptionSetFactory(method);

			TypeAnalysis typeAnalysis =
				new TypeAnalysis(methodGen, cfg, dfs, lookupFailureCallback, exceptionSetFactory);
			TypeDataflow typeDataflow = new TypeDataflow(cfg, typeAnalysis);
			typeDataflow.execute();

			return typeDataflow;
		}
	};

	private NoDataflowAnalysisFactory<DepthFirstSearch> dfsFactory =
	new NoDataflowAnalysisFactory<DepthFirstSearch>("depth first search") {
		protected DepthFirstSearch analyze(Method method) throws CFGBuilderException {
			CFG cfg = getRawCFG(method);
			DepthFirstSearch dfs = new DepthFirstSearch(cfg);
			dfs.search();
			return dfs;
		}
	};

	private NoDataflowAnalysisFactory<ReverseDepthFirstSearch> rdfsFactory =
	new NoDataflowAnalysisFactory<ReverseDepthFirstSearch>("reverse depth first search") {
		protected ReverseDepthFirstSearch analyze(Method method) throws CFGBuilderException {
			CFG cfg = getRawCFG(method);
			ReverseDepthFirstSearch rdfs = new ReverseDepthFirstSearch(cfg);
			rdfs.search();
			return rdfs;
		}
	};

	private NoExceptionAnalysisFactory<BitSet> bytecodeSetFactory =
	new NoExceptionAnalysisFactory<BitSet>("bytecode set construction") {
		protected BitSet analyze(Method method) {
			final BitSet result = new BitSet();

			Code code = method.getCode();
			if (code != null) {
				byte[] instructionList = code.getCode();
	
				// Create a callback to put the opcodes of the method's
				// bytecode instructions into the BitSet.
				BytecodeScanner.Callback callback = new BytecodeScanner.Callback() {
					public void handleInstruction(int opcode, int index) {
						result.set(opcode, true);
					}
				};
	
				// Scan the method.
				BytecodeScanner scanner = new BytecodeScanner();
				scanner.scan(instructionList, callback);
			}

			return result;
		}
	};

/*
	private AnalysisFactory<LockCountDataflow> anyLockCountDataflowFactory =
	new AnalysisFactory<LockCountDataflow>("lock count analysis (any lock)") {
		protected LockCountDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);
			ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			CFG cfg = getCFG(method);

			AnyLockCountAnalysis analysis = new AnyLockCountAnalysis(methodGen, vnaDataflow, dfs);
			LockCountDataflow dataflow = new LockCountDataflow(cfg, analysis);
			dataflow.execute();
			return dataflow;
		}
	};
*/

	private AnalysisFactory<LockDataflow> lockDataflowFactory =
	new AnalysisFactory<LockDataflow>("lock set analysis") {
		protected LockDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);
			ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			CFG cfg = getCFG(method);

			LockAnalysis analysis = new LockAnalysis(methodGen, vnaDataflow, dfs);
			LockDataflow dataflow = new LockDataflow(cfg, analysis);
			dataflow.execute();
			return dataflow;
		}
	};

	private AnalysisFactory<ReturnPathDataflow> returnPathDataflowFactory =
	new AnalysisFactory<ReturnPathDataflow>("return path analysis") {
		protected ReturnPathDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			CFG cfg = getCFG(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			ReturnPathAnalysis analysis = new ReturnPathAnalysis(dfs);
			ReturnPathDataflow dataflow = new ReturnPathDataflow(cfg, analysis);
			dataflow.execute();
			return dataflow;
		}
	};

	private AnalysisFactory<DominatorsAnalysis> nonExceptionDominatorsAnalysisFactory =
	new AnalysisFactory<DominatorsAnalysis>("non-exception dominators analysis") {
		protected DominatorsAnalysis analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			CFG cfg = getCFG(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			DominatorsAnalysis analysis = new DominatorsAnalysis(cfg, dfs, true);
			Dataflow<java.util.BitSet, DominatorsAnalysis> dataflow =
				new Dataflow<java.util.BitSet, DominatorsAnalysis>(cfg, analysis);
			dataflow.execute();
			return analysis;
		}
	};

	private AnalysisFactory<PostDominatorsAnalysis> nonExceptionPostDominatorsAnalysisFactory =
	new AnalysisFactory<PostDominatorsAnalysis>("non-exception postdominators analysis") {
		protected PostDominatorsAnalysis analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			CFG cfg = getCFG(method);
			ReverseDepthFirstSearch rdfs = getReverseDepthFirstSearch(method);
			PostDominatorsAnalysis analysis = new PostDominatorsAnalysis(cfg, rdfs, true);
			Dataflow<java.util.BitSet, PostDominatorsAnalysis> dataflow =
				new Dataflow<java.util.BitSet, PostDominatorsAnalysis>(cfg, analysis);
			dataflow.execute();
			return analysis;
		}
	};

	private NoExceptionAnalysisFactory<ExceptionSetFactory> exceptionSetFactoryFactory =
	new NoExceptionAnalysisFactory<ExceptionSetFactory>("exception set factory") {
		protected ExceptionSetFactory analyze(Method method) {
			return new ExceptionSetFactory();
		}
	};

	private ClassGen classGen;
	private AssignedFieldMap assignedFieldMap;
	private AssertionMethods assertionMethods;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * @param jclass the JavaClass
	 */
	public ClassContext(JavaClass jclass, RepositoryLookupFailureCallback lookupFailureCallback) {
		if (lookupFailureCallback == null) throw new IllegalArgumentException();
		this.jclass = jclass;
		this.lookupFailureCallback = lookupFailureCallback;
		this.classGen = null;
		this.assignedFieldMap = null;
		this.assertionMethods = null;
	}

	/**
	 * Get the JavaClass.
	 */
	public JavaClass getJavaClass() { return jclass; }

	/**
	 * Get the RepositoryLookupFailureCallback.
	 * @return the RepositoryLookupFailureCallback
	 */
	public RepositoryLookupFailureCallback getLookupFailureCallback() {
		return lookupFailureCallback;
	}

	/**
	 * Get a MethodGen object for given method.
	 * @param method the method
	 * @return the MethodGen object for the method, or null
	 *   if the method has no Code attribute (and thus cannot be analyzed)
	 */
	public MethodGen getMethodGen(Method method) {
		return methodGenFactory.getAnalysis(method);
	}

	/**
	 * Get a "raw" CFG for given method.
	 * No pruning is done, although the CFG may already be pruned.
	 * @param method the method
	 * @return the raw CFG
	 */
	public CFG getRawCFG(Method method) throws CFGBuilderException {
		return cfgFactory.getRawCFG(method);
	}

	/**
	 * Get a CFG for given method.
	 * If pruning options are in effect, pruning will be done.
	 * Because the CFG pruning can involve interprocedural analysis,
	 * it is done on a best-effort basis, so the CFG returned might
	 * not actually be pruned.
	 *
	 * @param method the method
	 * @return the CFG
	 * @throws CFGBuilderException if a CFG cannot be constructed for the method
	 */
	public CFG getCFG(Method method) throws CFGBuilderException {
		return cfgFactory.getRefinedCFG(method);
	}

	/**
	 * Get the ConstantPoolGen used to create the MethodGens
	 * for this class.
	 * @return the ConstantPoolGen
	 */
	public ConstantPoolGen getConstantPoolGen() {
		if (classGen == null)
			classGen = new ClassGen(jclass);
		return classGen.getConstantPool();
	}

	/**
	 * Get a ValueNumberDataflow for given method.
	 * @param method the method
	 * @return the ValueNumberDataflow
	 */
	public ValueNumberDataflow getValueNumberDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		return vnaDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get an IsNullValueDataflow for given method.
	 * @param method the method
	 * @return the IsNullValueDataflow
	 */
	public IsNullValueDataflow getIsNullValueDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		return invDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get a TypeDataflow for given method.
	 * @param method the method
	 * @return the TypeDataflow
	 */
	public TypeDataflow getTypeDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		return typeDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get a DepthFirstSearch for given method.
	 * @param method the method
	 * @return the DepthFirstSearch
	 */
	public DepthFirstSearch getDepthFirstSearch(Method method) throws CFGBuilderException {
		return dfsFactory.getAnalysis(method);
	}

	/**
	 * Get a ReverseDepthFirstSearch for given method.
	 * @param method the method
	 * @param the ReverseDepthFirstSearch
	 */
	public ReverseDepthFirstSearch getReverseDepthFirstSearch(Method method)
		throws CFGBuilderException {
		return rdfsFactory.getAnalysis(method);
	}

	/**
	 * Get a BitSet representing the bytecodes that are used in the given method.
	 * This is useful for prescreening a method for the existence of particular instructions.
	 * Because this step doesn't require building a MethodGen, it is very
	 * fast and memory-efficient.  It may allow a Detector to avoid some
	 * very expensive analysis, which is a Big Win for the user.
	 *
	 * @param method the method
	 * @return the BitSet containing the opcodes which appear in the method
	 */
	public BitSet getBytecodeSet(Method method) {
		return bytecodeSetFactory.getAnalysis(method);
	}

//	/**
//	 * Get dataflow for AnyLockCountAnalysis for given method.
//	 * @param method the method
//	 * @return the Dataflow
//	 */
//	public LockCountDataflow getAnyLockCountDataflow(Method method)
//		throws CFGBuilderException, DataflowAnalysisException {
//		return anyLockCountDataflowFactory.getAnalysis(method);
//	}

	/**
	 * Get dataflow for LockAnalysis for given method.
	 * @param method the method
	 * @return the LockDataflow
	 */
	public LockDataflow getLockDataflow(Method method)
		throws CFGBuilderException, DataflowAnalysisException {
		return lockDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get ReturnPathDataflow for method.
	 * @param method the method
	 * @return the ReturnPathDataflow
	 */
	public ReturnPathDataflow getReturnPathDataflow(Method method)
		throws CFGBuilderException, DataflowAnalysisException {
		return returnPathDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get DominatorsAnalysis for given method,
	 * where exception edges are ignored.
	 * @param method the method
	 * @return the DominatorsAnalysis
	 */
	public DominatorsAnalysis getNonExceptionDominatorsAnalysis(Method method)
		throws CFGBuilderException, DataflowAnalysisException {
		return nonExceptionDominatorsAnalysisFactory.getAnalysis(method);
	}

	/**
	 * Get PostDominatorsAnalysis for given method,
	 * where exception edges are ignored.
	 * @param method the method
	 * @return the PostDominatorsAnalysis
	 */
	public PostDominatorsAnalysis getNonExceptionPostDominatorsAnalysis(Method method)
		throws CFGBuilderException, DataflowAnalysisException {
		return nonExceptionPostDominatorsAnalysisFactory.getAnalysis(method);
	}

	/**
	 * Get ExceptionSetFactory for given method.
	 * @param method the method
	 * @return the ExceptionSetFactory
	 */
	public ExceptionSetFactory getExceptionSetFactory(Method method) {
		return exceptionSetFactoryFactory.getAnalysis(method);
	}

	/**
	 * Get the assigned field map for the class.
	 * @return the AssignedFieldMap
	 * @throws ClassNotFoundException if a class lookup prevents
	 *   the class's superclasses from being searched for
	 *   assignable fields
	 */
	public AssignedFieldMap getAssignedFieldMap() throws ClassNotFoundException {
		if (assignedFieldMap == null) {
			assignedFieldMap = new AssignedFieldMap(this);
		}
		return assignedFieldMap;
	}

	/**
	 * Get AssertionMethods for class.
	 * @return the AssertionMethods
	 */
	public AssertionMethods getAssertionMethods() {
		if (assertionMethods == null) {
			assertionMethods = new AssertionMethods(jclass);
		}
		return assertionMethods;
	}
}

// vim:ts=3
