/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import edu.umd.cs.daveho.ba.SignatureConverter;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

/**
 * A kind of runtime exception that can be thrown to indicate
 * a fatal error in an analysis.  It would be nice to make this a
 * checked exception, but we can't throw those from BCEL visitors.
 */
public class AnalysisException extends RuntimeException {
	/**
	 * Constructor.
	 * @param message reason for the error
	 */
	public AnalysisException(String message) {
		super(message);
	}

	/**
	 * Constructor from another Throwable object.
	 * This is useful for chaining exceptions.
	 * @param message reason for the error
	 * @param throwable cause of the error
	 */
	public AnalysisException(String message, Throwable throwable) {
		super(message, throwable);
	}

	/**
	 * Constructor from method and instruction.
	 * @param message reason for the error
	 * @param methodGen the method
	 * @param handle the instruction
	 */
	public AnalysisException(String message, MethodGen methodGen, InstructionHandle handle) {
		super(message + " in " + SignatureConverter.convertMethodSignature(methodGen) + " at " + handle);
	}
}

// vim:ts=4
