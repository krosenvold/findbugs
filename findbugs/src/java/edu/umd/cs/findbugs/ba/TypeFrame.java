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
 * A specialization of {@link Frame} for determining the types
 * of values in the Java stack frame (locals and operand stack).
 *
 * @see Frame
 * @see TypeAnalysis
 * @author David Hovemeyer
 */
public class TypeFrame extends Frame<Type> implements Constants, ExtendedTypes {

	////////////////////////////////////////////////////////////////////////////////////
	// Fields
	////////////////////////////////////////////////////////////////////////////////////

	private TypeMerger typeMerger;

	////////////////////////////////////////////////////////////////////////////////////
	// Methods
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor.
	 */
	public TypeFrame(int numLocals, TypeMerger typeMerger) {
		super(numLocals);
		this.typeMerger = typeMerger;
	}

	/**
	 * Get default value to put in unintialized slots.
	 */
	public Type getDefaultValue() {
		return getBottomType();
	}

	/**
	 * Merge two types together.  This is somewhat complicated!
	 * @param slot the slot number
	 * @param a a type to be merged
	 * @param b a type to be merged
	 * @return the merged type
	 */
	public Type mergeValues(int slot, Type a, Type b) throws DataflowAnalysisException {
		return typeMerger.mergeTypes(a, b);
	}

	protected String valueToString(Type value) {
		return value.toString() + ",";
	}

	/** Get the single instance of the "Top" type. */
	public static Type getTopType() { return TopType.instance(); }

	/** Get the single instance of the "Bottom" type. */
	public static Type getBottomType() { return BottomType.instance(); }

	/** Get the single instance of the "LongExtra" type. */
	public static Type getLongExtraType() { return LongExtraType.instance(); }

	/** Get the single instance of the "DoubleExtra" type. */
	public static Type getDoubleExtraType() { return DoubleExtraType.instance(); }

	/** Get the single instance of the "Null" type. */
	public static Type getNullType() { return NullType.instance(); }

}

// vim:ts=4
