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

import edu.umd.cs.pugh.visitclass.BetterVisitor;
import edu.umd.cs.pugh.visitclass.DismantleBytecode;
import edu.umd.cs.daveho.ba.SignatureConverter;

/**
 * A BugAnnotation specifying a particular method in a particular class.
 * A MethodAnnotation may (optionally) have a SourceLineAnnotation directly
 * embedded inside it to indicate the range of source lines where the
 * method is defined.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class MethodAnnotation extends PackageMemberAnnotation {
	private static final boolean UGLY_METHODS = Boolean.getBoolean("ma.ugly");

	private String methodName;
	private String methodSig;
	private String fullMethod;
	private SourceLineAnnotation sourceLines;

	/**
	 * Constructor.
	 * @param className the name of the class containing the method
	 * @param methodName the name of the method
	 * @param methodSig the Java type signature of the method
	 */
	public MethodAnnotation(String className, String methodName, String methodSig) {
		super(className, "METHOD_DEFAULT");
		this.methodName = methodName;
		this.methodSig = methodSig;
		fullMethod = null;
		sourceLines = null;
	}

	/**
	 * Factory method to create a MethodAnnotation from the method the
	 * given visitor is currently visiting.
	 * @param visitor the BetterVisitor currently visiting the method
	 */
	public static MethodAnnotation fromVisitedMethod(BetterVisitor visitor) {
		return new MethodAnnotation(visitor.getBetterClassName(), visitor.getMethodName(), visitor.getMethodSig());
	}

	/** Get the method name. */
	public String getMethodName() { return methodName; }

	/** Get the method type signature. */
	public String getMethodSignature() { return methodSig; }

	/**
	 * Set a SourceLineAnnotation describing the source lines
	 * where the method is defined.
	 */
	public void setSourceLines(SourceLineAnnotation sourceLines) {
		this.sourceLines = sourceLines;
	}

	/**
	 * Get the SourceLineAnnotation describing the source lines
	 * where the method is defined.
	 * @return the SourceLineAnnotation, or null if there is no source information
	 *   for this method
	 */
	public SourceLineAnnotation getSourceLines() {
		return sourceLines;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitMethodAnnotation(this);
	}

	protected String formatPackageMember(String key) {
		if (key.equals(""))
			return UGLY_METHODS ? getUglyMethod() : getFullMethod();
		else if (key.equals("shortMethod"))
			return className + "." + methodName + "()";
		else
			throw new IllegalArgumentException("unknown key " + key);
	}

	/**
	 * Get the "full" method name.
	 * This is a format which looks sort of like a method signature
	 * that would appear in Java source code.
	 */
	public String getFullMethod() {
		if (fullMethod == null) {
			// Convert to "nice" representation
			SignatureConverter converter = new SignatureConverter(methodSig);
			String pkgName = getPackageName();

			StringBuffer args = new StringBuffer();

			if (converter.getFirst() != '(')
				throw new IllegalStateException("bad method signature " + methodSig);
			converter.skip();

			while (converter.getFirst() != ')') {
				if (args.length() > 0)
					args.append(',');
				args.append(shorten(pkgName, converter.parseNext()));
			}
			converter.skip();

			// NOTE: we omit the return type.
			// It is not needed to disambiguate the method,
			// and would just clutter the output.

			// Actually, GJ implements covariant return types at the source level,
			// so perhaps it really is necessary.

			StringBuffer result = new StringBuffer();
			result.append(className);
			result.append('.');
			result.append(methodName);
			result.append('(');
			result.append(args);
			result.append(')');

			fullMethod = result.toString();
		}

		return fullMethod;
	}

	private String getUglyMethod() {
		return className + "." + methodName + " : " + methodSig.replace('/', '.');
	}

	public int hashCode() {
		return className.hashCode() + methodName.hashCode() + methodSig.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof MethodAnnotation))
			return false;
		MethodAnnotation other = (MethodAnnotation) o;
		return className.equals(other.className)
			&& methodName.equals(other.methodName)
			&& methodSig.equals(other.methodSig);
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof MethodAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		MethodAnnotation other = (MethodAnnotation) o;
		int cmp;
		cmp = className.compareTo(other.className);
		if (cmp != 0)
			return cmp;
		cmp = methodName.compareTo(other.methodName);
		if (cmp != 0)
			return cmp;
		return methodSig.compareTo(other.methodSig);
	}

/*
	public static void main(String[] argv) {
		MethodAnnotation m = new MethodAnnotation("edu.umd.cs.daveho.ba.CFG", "fooIterator",
			"(I[[BLjava/util/Iterator;Ljava/lang/String;)Ledu/umd/cs/daveho/ba/BasicBlock;");
		System.out.println(m.toString());
	}
*/
}

// vim:ts=4
