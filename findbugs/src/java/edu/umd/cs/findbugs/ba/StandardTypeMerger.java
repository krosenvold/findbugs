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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

/**
 * A TypeMerger which applies standard Java semantics
 * when merging Types.  Subclasses may override mergeReferenceTypes()
 * in order to implement special typing rules for reference types.
 *
 * @see TypeMerger
 * @author David Hovemeyer
 */
public class StandardTypeMerger implements TypeMerger, Constants, ExtendedTypes {
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private ExceptionSetFactory exceptionSetFactory;

	/**
	 * Constructor.
	 * @param lookupFailureCallback object used to report Repository lookup failures
	 * @param exceptionSetFactory factory for creating ExceptionSet objects
	 */
	public StandardTypeMerger(RepositoryLookupFailureCallback lookupFailureCallback,
		ExceptionSetFactory exceptionSetFactory) {
		this.lookupFailureCallback = lookupFailureCallback;
		this.exceptionSetFactory = exceptionSetFactory;
	}

	public Type mergeTypes(Type a, Type b) throws DataflowAnalysisException {
		byte aType = a.getType(), bType = b.getType();

		if (aType == T_TOP)			// Top is the identity element
			return b;
		else if (bType == T_TOP)	// Top is the identity element
			return a;
		else if (aType == T_BOTTOM || bType == T_BOTTOM)	// Bottom meet anything is bottom
			return BottomType.instance();
		else if (isReferenceType(aType) && isReferenceType(bType)) {	// Two object types!
			// Handle the Null type, which serves as a special "top"
			// value for reference types.
			if (aType == T_NULL)
				return b;
			else if (bType == T_NULL)
				return a;

			ReferenceType aRef = (ReferenceType) a;
			ReferenceType bRef = (ReferenceType) b;
			return mergeReferenceTypes(aRef, bRef);
		} else if (isReferenceType(aType) || isReferenceType(bType))	// Object meet non-object is bottom
			return BottomType.instance();
		else if (aType == bType)	// Same non-object type?
			return a;
		else if (isIntegerType(aType) && isIntegerType(bType)) // Two different integer types - use T_INT
			return Type.INT;
		else						// Default - types are incompatible
			return BottomType.instance();
	}

	/**
	 * Determine if the given typecode refers to a reference type.
	 * This implementation just checks that the type code is T_OBJECT,
	 * T_ARRAY, T_NULL, or T_EXCEPTION.  Subclasses should override this
	 * if they have defined new object types with different type codes.
	 */
	protected boolean isReferenceType(byte type) {
		return type == T_OBJECT || type == T_ARRAY || type == T_NULL || type == T_EXCEPTION;
	}

	/**
	 * Determine if the given typecode refers to an Object type.
	 * Subclasses should override with any new object types.
	 */
	protected boolean isObjectType(byte type) {
		return type == T_OBJECT || type == T_EXCEPTION;
	}

	/**
	 * Determine if the given typecode refers to an Integer type (other than long).
	 * This implementation checks that the type code is T_INT, T_BYTE, T_BOOLEAN,
	 * T_CHAR, or T_SHORT.  Subclasses should override this if they have
	 * defined new integer types with different type codes.
	 */
	protected boolean isIntegerType(byte type) {
		return type == T_INT || type == T_BYTE || type == T_BOOLEAN || type == T_CHAR || type == T_SHORT;
	}

	private static void updateExceptionSet(ExceptionSet exceptionSet, ObjectType type) {
		if (type instanceof ExceptionObjectType)
			exceptionSet.addAll(((ExceptionObjectType) type).getExceptionSet());
		else
			exceptionSet.addExplicit(type);
	}

	/**
	 * Default implementation of merging reference types.
	 * This just returns the first common superclass, which is compliant
	 * with the JVM Spec.  Subclasses may override this method
	 * in order to implement extended type rules.
	 *
	 * @param aRef a ReferenceType
	 * @param bRef a ReferenceType
	 * @return the merged Type
	 */
	protected Type mergeReferenceTypes(ReferenceType aRef, ReferenceType bRef) throws DataflowAnalysisException {
		// Two concrete object types.
		// According to the JVM spec, 2nd edition, 4.9.2,
		// the result of merging types is the "first common superclass".
		// Interfaces are NOT considered!
		// This will use the Repository to look up classes.
		try {
			// Special case: ExceptionObjectTypes.
			// We want to preserve the ExceptionSets associated,
			// in order to track the exact set of exceptions
			if (isObjectType(aRef.getType()) && isObjectType(bRef.getType()) &&
				(aRef.getType() == T_EXCEPTION || bRef.getType() == T_EXCEPTION)) {
				ExceptionSet union = exceptionSetFactory.createExceptionSet();

				updateExceptionSet(union, (ObjectType) aRef);
				updateExceptionSet(union, (ObjectType) bRef);

				return ExceptionObjectType.fromExceptionSet(union);
			}

			return aRef.getFirstCommonSuperclass(bRef);
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
			throw new DataflowAnalysisException("Repository lookup failure: " + e.toString(), e);
		}
	}

}

// vim:ts=4
