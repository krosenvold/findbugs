/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import java.util.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.bcp.*;

/**
 * A base class for bug detectors that are based on a ByteCodePattern.
 * ByteCodePatterns provide an easy way to detect patterns of
 * bytecode instructions, taking into account control flow and
 * uses of fields and values.
 *
 * @see ByteCodePattern
 */
public abstract class ByteCodePatternDetector implements Detector {
	private static final boolean DEBUG = Boolean.getBoolean("bcpd.debug");

	public void visitClassContext(ClassContext classContext) {
		try {
			ByteCodePattern pattern = getPattern();
			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();

			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (method.isAbstract() || method.isNative())
					continue;

				if (DEBUG) {
					System.out.print(
						"=====================================================================\n"+
						"Method " + jclass.getClassName() + "." + method.getName() + "\n" +
						"=====================================================================\n");
				}

				if (!prescreen(method, classContext))
					continue;
	
				MethodGen methodGen = classContext.getMethodGen(method);
				if (methodGen == null)
					continue;
				ConstantPoolGen cpg = methodGen.getConstantPool();
				CFG cfg = classContext.getCFG(method);
				DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
				ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);

				PatternMatcher matcher = new PatternMatcher(pattern, cfg, cpg, dfs, vnaDataflow);
				matcher.execute();
	
				Iterator<ByteCodePatternMatch> j = matcher.byteCodePatternMatchIterator();
				while (j.hasNext()) {
					ByteCodePatternMatch match = j.next();

					if (DEBUG) {
						System.out.println("Pattern match:");
						Iterator<PatternElementMatch> pemIter = match.patternElementMatchIterator();
						while (pemIter.hasNext()) {
							PatternElementMatch pem = pemIter.next();
							System.out.println("\t" + pem.toString());
						}
					}

					reportMatch(jclass, methodGen, match);
				}
			}
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("BCPDoubleCheck caught exception", e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException(e.getMessage());
		}
	}

	public void report() {
	}

	/**
	 * Get the ByteCodePattern for this detector.
	 */
	public abstract ByteCodePattern getPattern();

	/**
	 * Prescreen a method.
	 * It is a valid, but dumb, implementation simply to return true unconditionally.
	 * A better implementation is to call ClassContext.getBytecodeSet() to check
	 * whether the method actually contains the bytecode instructions that
	 * the pattern will look for.  The theory is that checking the bytecode
	 * set is very fast, while building the MethodGen, CFG, ValueNumberAnalysis,
	 * etc. objects required to match ByteCodePatterns is slow, and the bytecode
	 * pattern matching algorithm is also not particularly fast.
	 *
	 * <p> As a datapoint, prescreening speeds up the BCPDoubleCheck detector
	 * <b>by a factor of 5</b> with no loss of generality and only a dozen
	 * or so extra lines of code.
	 *
	 * @param method the method
	 * @param classContext the ClassContext for the method
	 * @return true if the method should be analyzed for instances of the 
	 *   ByteCodePattern
	 */
	public abstract boolean prescreen(Method method, ClassContext classContext);

	/**
	 * Called to report an instance of the ByteCodePattern.
	 * @param javaClass the class in which the pattern was detected
	 * @param methodGen the method in which the pattern was detected
	 * @param match the ByteCodePatternMatch object representing the match
	 *   of the ByteCodePattern against actual instructions in the method
	 */
	public abstract void reportMatch(JavaClass javaClass, MethodGen methodGen, ByteCodePatternMatch match);
}

// vim:ts=4
