package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Dataflow analysis to find the propagation of the "this" value
 * in Java stack frames.  This is a nice, simple example of a dataflow analysis
 * which determines properties of values in the Java stack frame.
 *
 * @see ThisValue
 * @see ThisValueFrame
 * @see ThisValueFrameModelingVisitor
 * @author David Hovemeyer
 */
public class ThisValueAnalysis extends ForwardDataflowAnalysis<ThisValueFrame> {
	private MethodGen methodGen;

	/**
	 * Constructor.
	 * @param methodGen the method to be analyzed - must not be static
	 */
	public ThisValueAnalysis(MethodGen methodGen) {
		if (methodGen.isStatic()) throw new IllegalArgumentException("Useless for static methods");
		this.methodGen = methodGen;
	}

	public ThisValueFrame createFact() {
		return new ThisValueFrame(methodGen.getMaxLocals());
	}

	public void copy(ThisValueFrame source, ThisValueFrame dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(ThisValueFrame result) {
		// Upon entry to the method, the "this" pointer is in local 0,
		// and all of the locals as "not this".
		result.setValid();
		result.setValue(0, ThisValue.thisValue());
		for (int i = 1; i < result.getNumLocals(); ++i)
			result.setValue(i, ThisValue.notThisValue());
	}

	public void initResultFact(ThisValueFrame result) {
		result.setTop();
	}

	public void makeFactTop(ThisValueFrame fact) {
		fact.setTop();
	}

	public boolean same(ThisValueFrame fact1, ThisValueFrame fact2) {
		return fact1.sameAs(fact2);
	}

	public void transfer(BasicBlock basicBlock, InstructionHandle end, ThisValueFrame start, ThisValueFrame result) throws DataflowAnalysisException {
		result.copyFrom(start);

		if (!start.isTop() && !start.isBottom()) {
			ThisValueFrameModelingVisitor visitor = new ThisValueFrameModelingVisitor(result, methodGen.getConstantPool());
			Iterator<InstructionHandle> i = basicBlock.instructionIterator();
			while (i.hasNext()) {
				InstructionHandle handle = i.next();
				if (handle == end)
					break;
				Instruction ins = handle.getInstruction();

				// Make sure that stack change was what we expected!!!
				// There is a bug in BCEL 5.0 which prevents the visitor from
				// correctly modeling the stack.  (Specifically, the PUTFIELD
				// instruction in that version does not implement the
				// StackConsumer interface).
				int oldStack = result.getStackDepth();
				ins.accept(visitor);
				int newStack = result.getStackDepth();
				int delta = (newStack - oldStack);
				int predictedDelta = predictStackDelta(ins);
				if (delta != predictedDelta)
					throw new IllegalStateException("Failure modeling stack for instruction " + ins +
						" (predicted=" + predictedDelta + ", actual="+delta + ")");
			}
		}
	}

	private int predictStackDelta(Instruction ins) {
		ConstantPoolGen cpg = methodGen.getConstantPool();
		int consumed = ins.consumeStack(cpg);
		int produced = ins.produceStack(cpg);
		if (consumed == Constants.UNPREDICTABLE || produced == Constants.UNPREDICTABLE)
			throw new IllegalStateException("unpredictable stack delta for instruction " + ins);
		return produced - consumed;
	}

	public void meetInto(ThisValueFrame fact, Edge edge, ThisValueFrame result) throws DataflowAnalysisException {
		if (edge.getDest().isExceptionHandler() && fact.isValid()) {
			// Special case: when merging predecessor facts for entry to
			// an exception handler, we clear the stack and push a
			// single entry for the exception object.  That way, the locals
			// can still be merged.
			ThisValueFrame tmpFact = createFact();
			tmpFact.copyFrom(fact);
			tmpFact.clearStack();
			tmpFact.pushValue(ThisValue.notThisValue());
			fact = tmpFact;
		}
		result.mergeWith(fact);
	}

	/**
	 * Test driver.
	 */
	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.out.println("Usage: edu.umd.cs.daveho.ba.ThisValueAnalysis <class file>");
				System.exit(1);
			}

			DataflowTestDriver<ThisValueFrame> driver = new DataflowTestDriver<ThisValueFrame>() {
				public DataflowAnalysis<ThisValueFrame> createAnalysis(MethodGen methodGen, CFG cfg) {
					return new ThisValueAnalysis(methodGen);
				}

				public void examineResults(CFG cfg, Dataflow<ThisValueFrame> dataflow) {
					Iterator<BasicBlock> i = cfg.blockIterator();
					while (i.hasNext()) {
						BasicBlock block = i.next();
						ThisValueFrame start = dataflow.getStartFact(block);
						ThisValueFrame result = dataflow.getResultFact(block);
						int numFound = 0;
						numFound += count(start);
						numFound += count(result);
						System.out.println("In block " + block.getId() + " found " + numFound + " occurrences of \"this\" value");
					}
				}
			
				private int count(ThisValueFrame frame) {
					int count = 0;
					if (frame.isTop()) {
						System.out.println("TOP frame!");
						return 0;
					}
					if (frame.isBottom()) {
						System.out.println("BOTTOM frame!");
						return 0;
					}
					int numSlots = frame.getNumSlots();
					for (int i = 0; i < numSlots; ++i) {
						if (frame.getValue(i).isThis())
							++count;
					}
					return count;
				}
			};

			driver.execute(argv[0]);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}

// vim:ts=4
