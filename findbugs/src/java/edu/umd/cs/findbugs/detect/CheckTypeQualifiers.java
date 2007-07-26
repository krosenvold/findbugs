/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowCFGPrinter;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.jsr305.Analysis;
import edu.umd.cs.findbugs.ba.jsr305.BackwardTypeQualifierDataflow;
import edu.umd.cs.findbugs.ba.jsr305.BackwardTypeQualifierDataflowAnalysis;
import edu.umd.cs.findbugs.ba.jsr305.BackwardTypeQualifierDataflowFactory;
import edu.umd.cs.findbugs.ba.jsr305.FlowValue;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflow;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflowAnalysis;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflowFactory;
import edu.umd.cs.findbugs.ba.jsr305.SourceSinkInfo;
import edu.umd.cs.findbugs.ba.jsr305.SourceSinkType;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValueSet;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.bcel.CFGDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.MissingClassException;

/**
 * Check JSR-305 type qualifiers.
 * 
 * @author David Hovemeyer
 */
public class CheckTypeQualifiers extends CFGDetector {
	private static final boolean DEBUG = SystemProperties.getBoolean("ctq.debug");
	private static final boolean DEBUG_DATAFLOW = SystemProperties.getBoolean("ctq.dataflow.debug");
	private static final String DEBUG_DATAFLOW_MODE = SystemProperties.getProperty("ctq.dataflow.debug.mode", "both");

	private BugReporter bugReporter;

	public CheckTypeQualifiers(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.bcel.CFGDetector#visitMethodCFG(edu.umd.cs.findbugs.classfile.MethodDescriptor, edu.umd.cs.findbugs.ba.CFG)
	 */
	@Override
	protected void visitMethodCFG(MethodDescriptor methodDescriptor, CFG cfg) throws CheckedAnalysisException {
		if (DEBUG) {
			System.out.println("CheckTypeQualifiers: checking " + methodDescriptor.toString());
		}

		IAnalysisCache analysisCache = Global.getAnalysisCache();
		ForwardTypeQualifierDataflowFactory forwardDataflowFactory =
			analysisCache.getMethodAnalysis(ForwardTypeQualifierDataflowFactory.class, methodDescriptor);
		BackwardTypeQualifierDataflowFactory backwardDataflowFactory =
			analysisCache.getMethodAnalysis(BackwardTypeQualifierDataflowFactory.class, methodDescriptor);

		Collection<TypeQualifierValue> relevantQualifiers = Analysis.getRelevantTypeQualifiers(methodDescriptor);
		if (DEBUG) {
			System.out.println("  Relevant type qualifiers are " + relevantQualifiers);
		}

		for (TypeQualifierValue typeQualifierValue : relevantQualifiers) {
			try {
				checkQualifier(methodDescriptor, cfg, typeQualifierValue, forwardDataflowFactory, backwardDataflowFactory);
			} catch (MissingClassException e) {
				bugReporter.reportMissingClass(e.getClassDescriptor());
			} catch (CheckedAnalysisException e) {
				bugReporter.logError(
						"Exception checking type qualifier " + typeQualifierValue.toString() +
						" on method " + methodDescriptor.toString(),
						e);
			}
		}
	}

	/**
	 * Check a specific TypeQualifierValue on a method.
	 * 
	 * @param methodDescriptor       MethodDescriptor of method
	 * @param cfg                    CFG of method
	 * @param typeQualifierValue     TypeQualifierValue to check
	 * @param forwardDataflowFactory ForwardTypeQualifierDataflowFactory used to create forward dataflow analysis objects
	 * @param backwardDataflowFactory BackwardTypeQualifierDataflowFactory used to create backward dataflow analysis objects
	 */
	private void checkQualifier(
			MethodDescriptor methodDescriptor,
			CFG cfg,
			TypeQualifierValue typeQualifierValue,
			ForwardTypeQualifierDataflowFactory forwardDataflowFactory,
			BackwardTypeQualifierDataflowFactory backwardDataflowFactory) throws CheckedAnalysisException {

		if (DEBUG) {
			System.out.println("----------------------------------------------------------------------");
			System.out.println("Checking type qualifier " + typeQualifierValue.toString() + " on method " + methodDescriptor.toString());
			System.out.println("----------------------------------------------------------------------");
		}

		ForwardTypeQualifierDataflow forwardDataflow = forwardDataflowFactory.getDataflow(typeQualifierValue);

		if (DEBUG_DATAFLOW && (DEBUG_DATAFLOW_MODE.startsWith("forward") || DEBUG_DATAFLOW_MODE.equals("both"))) {
			System.out.println("********* Forwards analysis *********");
			DataflowCFGPrinter<TypeQualifierValueSet, ForwardTypeQualifierDataflowAnalysis> p =
				new DataflowCFGPrinter<TypeQualifierValueSet, ForwardTypeQualifierDataflowAnalysis>(forwardDataflow);
			p.print(System.out);
		}

		BackwardTypeQualifierDataflow backwardDataflow = backwardDataflowFactory.getDataflow(typeQualifierValue);

		if (DEBUG_DATAFLOW && (DEBUG_DATAFLOW_MODE.startsWith("backward") || DEBUG_DATAFLOW_MODE.equals("both"))) {
			System.out.println("********* Backwards analysis *********");
			DataflowCFGPrinter<TypeQualifierValueSet, BackwardTypeQualifierDataflowAnalysis> p =
				new DataflowCFGPrinter<TypeQualifierValueSet, BackwardTypeQualifierDataflowAnalysis>(backwardDataflow);
			p.print(System.out);
		}

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location loc = i.next();

			TypeQualifierValueSet forwardsFact = forwardDataflow.getFactAtLocation(loc);
			TypeQualifierValueSet backwardsFact = backwardDataflow.getFactAfterLocation(loc);

			if (!forwardsFact.isValid() || !backwardsFact.isValid()) {
				continue;
			}

			check(methodDescriptor, typeQualifierValue, forwardsFact, backwardsFact);
		}

		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext(); ) {
			BasicBlock block = i.next();
			check(methodDescriptor, typeQualifierValue, forwardDataflow.getResultFact(block), backwardDataflow.getStartFact(block));
		}
	}

	private void check(MethodDescriptor methodDescriptor, TypeQualifierValue typeQualifierValue,
			TypeQualifierValueSet forwardsFact, TypeQualifierValueSet backwardsFact) {
		Set<ValueNumber> valueNumberSet = new HashSet<ValueNumber>();
		valueNumberSet.addAll(forwardsFact.getValueNumbers());
		valueNumberSet.addAll(backwardsFact.getValueNumbers());

		for (ValueNumber vn : valueNumberSet) {
			FlowValue forward = forwardsFact.getValue(vn);
			FlowValue backward = backwardsFact.getValue(vn);

			if (DEBUG) {
				System.out.println("Check " + vn + ": forward=" + forward + ", backward=" + backward);
			}

			if (FlowValue.valuesConflict(forward, backward)) {
				emitWarning(methodDescriptor, typeQualifierValue, forwardsFact, backwardsFact, vn, forward, backward);
			}
		}
	}

	private void emitWarning(MethodDescriptor methodDescriptor, TypeQualifierValue typeQualifierValue,
			TypeQualifierValueSet forwardsFact, TypeQualifierValueSet backwardsFact, ValueNumber vn, FlowValue forward,
			FlowValue backward) {
		// Issue warning
		BugInstance warning = new BugInstance(this, "CTQ_INCONSISTENT_USE", Priorities.NORMAL_PRIORITY)
			.addClassAndMethod(methodDescriptor)
			.addClass(typeQualifierValue.getTypeQualifierClassDescriptor()).describe("TYPE_ANNOTATION");

		Set<SourceSinkInfo> sourceSet = (forward == FlowValue.ALWAYS)
				? forwardsFact.getWhereAlways(vn)
				: forwardsFact.getWhereNever(vn);

		// TODO
//		LocalVariableAnnotation local = ValueNumberSourceInfo.findLocalAnnotationFromValueNumber(method, location, valueNumber, vnaFrame)

		for (SourceSinkInfo source : sourceSet) {
			annotateWarningWithSourceSinkInfo(warning, methodDescriptor, vn, source);
		}

		Set<SourceSinkInfo> sinkSet = (backward == FlowValue.ALWAYS)
				? backwardsFact.getWhereAlways(vn)
				: backwardsFact.getWhereNever(vn);

		for (SourceSinkInfo sink : sinkSet) {
			annotateWarningWithSourceSinkInfo(warning, methodDescriptor, vn, sink);
		}

		bugReporter.reportBug(warning);
	}

	private void annotateWarningWithSourceSinkInfo(BugInstance warning, MethodDescriptor methodDescriptor, ValueNumber vn, SourceSinkInfo sourceSinkInfo) {
		switch (sourceSinkInfo.getType()) {
		case PARAMETER:
		case RETURN_VALUE_OF_CALLED_METHOD:
		case FIELD_LOAD:
			warning.addSourceLine(methodDescriptor, sourceSinkInfo.getLocation()).describe("SOURCE_LINE_VALUE_SOURCE");
			break;
			
		case ARGUMENT_TO_CALLED_METHOD:
		case RETURN_VALUE:
		case FIELD_STORE:
			warning.addSourceLine(methodDescriptor, sourceSinkInfo.getLocation()).describe("SOURCE_LINE_VALUE_SINK");
			return;
			
		default:
			throw new IllegalStateException();
		}
	}

}
