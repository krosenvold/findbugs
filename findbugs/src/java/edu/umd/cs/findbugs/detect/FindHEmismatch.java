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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.Lookup;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class FindHEmismatch extends BytecodeScanningDetector implements Constants2 {
	boolean hasFields = false;
	boolean visibleOutsidePackage = false;
	boolean hasHashCode = false;
	boolean hasEqualsObject = false;
	boolean hashCodeIsAbstract = false;
	boolean equalsObjectIsAbstract = false;
	boolean equalsMethodIsInstanceOfEquals = false;
	boolean hasCompareToObject = false;
	boolean hasEqualsSelf = false;
	boolean hasCompareToSelf = false;
	boolean extendsObject = false;
	private BugReporter bugReporter;

	public FindHEmismatch(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitAfter(JavaClass obj) {
		if (!obj.isClass()) return;
		if (getDottedClassName().equals("java.lang.Object")) return;
		int accessFlags = obj.getAccessFlags();
		if ((accessFlags & ACC_INTERFACE) != 0) return;
		visibleOutsidePackage = obj.isPublic() || obj.isProtected();
		String whereEqual = getDottedClassName();
		boolean classThatDefinesEqualsIsAbstract = false;
		boolean inheritedHashCodeIsFinal = false;
		boolean inheritedEqualsIsFinal = false;
		boolean inheritedHashCodeIsAbstract = false;
		boolean inheritedEqualsIsAbstract = false;
		if (!hasEqualsObject) {
			JavaClass we = Lookup.findSuperImplementor(obj, "equals",
			        "(Ljava/lang/Object;)Z", bugReporter);
			if (we == null) {
				whereEqual = "java.lang.Object";
			} else {
				whereEqual = we.getClassName();
				classThatDefinesEqualsIsAbstract = we.isAbstract();
				;
				Method m = findMethod(we, "equals", "(Ljava/lang/Object;)Z");
				if (m != null && m.isFinal()) inheritedEqualsIsFinal = true;
				if (m != null && m.isAbstract()) inheritedEqualsIsAbstract = true;
			}
		}
		boolean usesDefaultEquals = whereEqual.equals("java.lang.Object");
		String whereHashCode = getDottedClassName();
		if (!hasHashCode) {
			JavaClass wh = Lookup.findSuperImplementor(obj, "hashCode",
			        "()I", bugReporter);
			if (wh == null) {
				whereHashCode = "java.lang.Object";
			} else {
				whereHashCode = wh.getClassName();
				Method m = findMethod(wh, "hashCode", "()I");
				if (m != null && m.isFinal()) inheritedHashCodeIsFinal = true;
				if (m != null && m.isAbstract()) inheritedHashCodeIsAbstract = true;
			}
		}
		boolean usesDefaultHashCode = whereHashCode.equals("java.lang.Object");
		if (false && (usesDefaultEquals || usesDefaultHashCode)) {
			try {
				if (Repository.implementationOf(obj, "java/util/Set")
				        || Repository.implementationOf(obj, "java/util/List")
				        || Repository.implementationOf(obj, "java/util/Map")) {
					// System.out.println(getDottedClassName() + " uses default hashCode or equals");
				}
			} catch (ClassNotFoundException e) {
				// e.printStackTrace();
			}
		}

		if (!hasEqualsObject && hasEqualsSelf) {

			if (usesDefaultEquals) {
				int priority = HIGH_PRIORITY;
				if (usesDefaultHashCode || obj.isAbstract())
					priority++;
				if (!visibleOutsidePackage)
					priority++;
				bugReporter.reportBug(new BugInstance(this, "EQ_SELF_USE_OBJECT", priority).addClass(getDottedClassName()));
			} else {
				int priority = NORMAL_PRIORITY;
				if (hasFields)
					priority--;
				if (obj.isAbstract()) priority++;
				bugReporter.reportBug(new BugInstance(this, "EQ_SELF_NO_OBJECT", priority).addClass(getDottedClassName()));
			}
		}
		/*
		System.out.println("Class " + betterClassName);
		System.out.println("usesDefaultEquals: " + usesDefaultEquals);
		System.out.println("hasHashCode: : " + hasHashCode);
		System.out.println("usesDefaultHashCode: " + usesDefaultHashCode);
		System.out.println("hasEquals: : " + hasEqualsObject);
		*/

		if (!hasCompareToObject && hasCompareToSelf) {
			if (!extendsObject)
				bugReporter.reportBug(new BugInstance(this, "CO_SELF_NO_OBJECT", NORMAL_PRIORITY).addClass(getDottedClassName()));
		}

		// if (!hasFields) return;
		if (hasHashCode && !hashCodeIsAbstract && !(hasEqualsObject || hasEqualsSelf)) {
			int priority = LOW_PRIORITY;
			if (usesDefaultEquals)
				bugReporter.reportBug(new BugInstance(this, "HE_HASHCODE_USE_OBJECT_EQUALS", priority).addClass(getDottedClassName()));
			else if (!inheritedEqualsIsFinal)
				bugReporter.reportBug(new BugInstance(this, "HE_HASHCODE_NO_EQUALS", priority).addClass(getDottedClassName()));
		}
		if (!hasHashCode
		        && (hasEqualsObject && !equalsObjectIsAbstract || hasEqualsSelf)) {
			if (usesDefaultHashCode) {
				int priority = HIGH_PRIORITY;
				if (equalsMethodIsInstanceOfEquals)
					priority += 2;
				else if (obj.isAbstract() || !hasEqualsObject) priority++;
				if (!visibleOutsidePackage) {
					priority++;
				}
				bugReporter.reportBug(new BugInstance(this, "HE_EQUALS_USE_HASHCODE",
				        priority).addClass(getDottedClassName()));
			} else if (!inheritedHashCodeIsFinal) {
				int priority = LOW_PRIORITY;
				if (hasEqualsObject && inheritedEqualsIsAbstract)
					priority++;
				if (hasFields) priority--;
				if (equalsMethodIsInstanceOfEquals || !hasEqualsObject)
					priority += 2;
				else if (obj.isAbstract()) priority++;
				bugReporter.reportBug(new BugInstance(this, "HE_EQUALS_NO_HASHCODE",
				        priority)
				        .addClass(getDottedClassName()));
			}
		}
		if (!hasHashCode && !hasEqualsObject && !hasEqualsSelf
		        && !usesDefaultEquals && usesDefaultHashCode
		        && !obj.isAbstract() && classThatDefinesEqualsIsAbstract) {
			bugReporter.reportBug(new BugInstance(this, "HE_INHERITS_EQUALS_USE_HASHCODE",
			        NORMAL_PRIORITY).addClass(getDottedClassName()));
		}
	}

	public void visit(JavaClass obj) {
		extendsObject = getDottedSuperclassName().equals("java.lang.Object");
		hasFields = false;
		hasHashCode = false;
		hasCompareToObject = false;
		hasCompareToSelf = false;
		hasEqualsObject = false;
		hasEqualsSelf = false;
		hashCodeIsAbstract = false;
		equalsObjectIsAbstract = false;
		equalsMethodIsInstanceOfEquals = false;
	}

	public void visit(Field obj) {
		int accessFlags = obj.getAccessFlags();
		if ((accessFlags & ACC_STATIC) != 0) return;
		if (!obj.getName().startsWith("this$"))
			hasFields = true;
	}

	public void visit(Method obj) {
		int accessFlags = obj.getAccessFlags();
		if ((accessFlags & ACC_STATIC) != 0) return;
		String name = obj.getName();
		String sig = obj.getSignature();
		if ((accessFlags & ACC_ABSTRACT) != 0) {
			if (name.equals("equals")
			        && sig.equals("(L" + getClassName() + ";)Z")) {
				bugReporter.reportBug(new BugInstance(this, "EQ_ABSTRACT_SELF", LOW_PRIORITY).addClass(getDottedClassName()));
				return;
			} else if (name.equals("compareTo")
			        && sig.equals("(L" + getClassName() + ";)I")) {
				bugReporter.reportBug(new BugInstance(this, "CO_ABSTRACT_SELF", LOW_PRIORITY).addClass(getDottedClassName()));
				return;
			}
		}
		boolean sigIsObject = sig.equals("(Ljava/lang/Object;)Z");
		if (name.equals("hashCode")
		        && sig.equals("()I")) {
			hasHashCode = true;
			if (obj.isAbstract()) hashCodeIsAbstract = true;
			// System.out.println("Found hashCode for " + betterClassName);
		} else if (name.equals("equals")) {
			if (sigIsObject) {
				hasEqualsObject = true;
				if (obj.isAbstract())
					equalsObjectIsAbstract = true;
				else {
					Code code = obj.getCode();
					byte[] codeBytes = code.getCode();

					if ((codeBytes.length == 5 &&
					        (codeBytes[1] & 0xff) == INSTANCEOF)
					        || (codeBytes.length == 15 &&
					        (codeBytes[1] & 0xff) == INSTANCEOF &&
					        (codeBytes[11] & 0xff) == INVOKESPECIAL)) {
						equalsMethodIsInstanceOfEquals = true;
					}
				}
			} else if (sig.equals("(L" + getClassName() + ";)Z"))
				hasEqualsSelf = true;
		} else if (name.equals("compareTo")) {
			if (sig.equals("(Ljava/lang/Object;)I"))
				hasCompareToObject = true;
			else if (sig.equals("(L" + getClassName() + ";)I"))
				hasCompareToSelf = true;
		}
	}

	Method findMethod(JavaClass clazz, String name, String sig) {
		Method[] m = clazz.getMethods();
		for (int i = 0; i < m.length; i++)
			if (m[i].getName().equals(name)
			        && m[i].getSignature().equals(sig))
				return m[i];
		return null;
	}
}
