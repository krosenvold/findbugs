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

package edu.umd.cs.findbugs.ba.jsr305;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.util.DualKeyHashMap;
import edu.umd.cs.findbugs.util.Util;

/**
 * A TypeQualifierValue is a pair specifying a type qualifier annotation
 * and a value.  Each TypeQualifierValue is effectively a different
 * type qualifier.  For example, if Foo is a type qualifier annotation
 * having an int value, then Foo(0), Foo(1), etc. are all
 * different type qualifiers which must be checked separately.
 * 
 * @author William Pugh
 */
public class TypeQualifierValue {
	private static final boolean DEBUG = SystemProperties.getBoolean("tqv.debug");
	
	public final ClassDescriptor typeQualifier;
	public final @CheckForNull Object value;
	private boolean isStrict;

	private TypeQualifierValue(ClassDescriptor typeQualifier, @CheckForNull Object value) {
		this.typeQualifier =  typeQualifier;
		this.value = value;
		this.isStrict = false; // will be set to true if this is a strict type qualifier value
	}

	private static DualKeyHashMap <ClassDescriptor, Object, TypeQualifierValue> map = new DualKeyHashMap <ClassDescriptor, Object, TypeQualifierValue> ();

	/**
	 * Given a ClassDescriptor/value pair, return the
	 * interned TypeQualifierValue representing that pair.
	 * 
	 * @param desc  a ClassDescriptor denoting a type qualifier annotation
	 * @param value a value
	 * @return an interned TypeQualifierValue object 
	 */
	public static synchronized  @NonNull TypeQualifierValue getValue(ClassDescriptor desc, Object value) {
		TypeQualifierValue result = map.get(desc, value);
		if (result != null) return result;
		result = new TypeQualifierValue(desc, value);
		determineIfQualifierIsStrict(desc, result);
		map.put(desc, value, result);
		return result;
	}

	private static void determineIfQualifierIsStrict(ClassDescriptor desc, TypeQualifierValue result) {
		if (DEBUG) {
			System.out.print("Checking to see if " + desc + " requires strict checking...");
		}
		// Check to see if the type qualifier should be checked strictly
		try {
			XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, desc);

			// Annotation elements appear as abstract methods in the annotation class (interface).
			// So, if the type qualifier annotation has specified a default When value,
			// it will appear as an abstract method called "when".
			XMethod whenMethod = xclass.findMethod("when", "()Ljavax/annotation/meta/When;", false);
			if (whenMethod == null) {
				result.setIsStrict();
			}
		} catch (MissingClassException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassNotFoundException());
		} catch (CheckedAnalysisException e) {
			AnalysisContext.logError("Error looking up annotation class " + desc.toDottedClassName(), e);
		}
		if (DEBUG) {
			System.out.println(result.isStrictQualifier() ? "yes" : "no"); 
		}
	}

	/**
	 * Get the ClassDescriptor which specifies the type qualifier annotation.
	 * 
	 * @return ClassDescriptor which specifies the type qualifier annotation
	 */
	public ClassDescriptor getTypeQualifierClassDescriptor() {
		return typeQualifier;
	}
	
	/**
	 * Mark this as a type qualifier value that should
	 * be checked strictly.
	 */
	private void setIsStrict() {
		this.isStrict = true;
	}

	/**
	 * Return whether or not this TypeQualifierValue denotes
	 * a strict qualifier.
	 * 
	 * @return true if type qualifier is strict, false otherwise
	 */
	public boolean isStrictQualifier() {
		return isStrict;
	}

	@Override
    public int hashCode() {
		int result = typeQualifier.hashCode();
		if (value != null) result += 37*value.hashCode();
		return result;
	}

	@Override
    public boolean equals(Object o) {
		if (!(o instanceof TypeQualifierValue)) return false;
		TypeQualifierValue other = (TypeQualifierValue) o;
		return typeQualifier.equals(other.typeQualifier) && Util.nullSafeEquals(value, other.value);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(typeQualifier.toString());
		if (value != null) {
			buf.append(':');
			buf.append(value.toString());
		}
		return buf.toString();
	}


}
