/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
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

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.ba.ClassContext;
import org.apache.bcel.classfile.*;

public class FindMaskedFields extends BytecodeScanningDetector implements Constants2 {
	private BugReporter bugReporter;
	private int numParms;
	private Set<Field> maskedFields = new HashSet<Field>();
	private Map<String, Field> classFields = new HashMap<String, Field>();
	private boolean staticMethod;

	public FindMaskedFields(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass obj = classContext.getJavaClass();
		if (!obj.isInterface())
			classContext.getJavaClass().accept(this);
	}

	public void visit(JavaClass obj) {
		classFields.clear();

		Field[] fields = obj.getFields();
		String fieldName;
		for (int f = 0; f < fields.length; f++) {
			fieldName = fields[f].getName();
			classFields.put(fieldName, fields[f]);
		}
		
		// Walk up the super class chain, looking for name collisions
		try {
			JavaClass[] superClasses = org.apache.bcel.Repository.getSuperClasses(obj);
			for (int c = 0; c < superClasses.length; c++) {
				fields = superClasses[c].getFields();
				for (int f = 0; f < fields.length; f++) {
					Field fld = fields[f];
					if (!fld.isStatic()
					        && !maskedFields.contains(fld)
					        && (fld.isPublic() || fld.isProtected())) {
						fieldName = fld.getName();
						if (fieldName.length() == 1)
							continue;
						if (fieldName.equals("serialVersionUID"))
							continue;
						if (classFields.containsKey(fieldName)) {
							maskedFields.add(fld);
							Field maskingField = classFields.get(fieldName);
							FieldAnnotation fa = new FieldAnnotation(getDottedClassName(),
							        maskingField.getName(),
							        maskingField.getSignature(),
							        maskingField.isStatic());
							int priority = NORMAL_PRIORITY;
							if (maskingField.isStatic()
							        || maskingField.isFinal())
								priority++;
							else if (fld.getSignature().charAt(0) == 'L'
							        && !fld.getSignature().startsWith("Ljava/lang/")
							        || fld.getSignature().charAt(0) == '[')
								priority--;
							if (fld.getAccessFlags()
							        != maskingField.getAccessFlags())
								priority++;
							if (!fld.getSignature().equals(maskingField.getSignature()))
								priority++;

							FieldAnnotation maskedFieldAnnotation
							        = FieldAnnotation.fromBCELField(superClasses[c].getClassName(), fld);
							bugReporter.reportBug(new BugInstance("MF_CLASS_MASKS_FIELD",
							        priority)
							        .addClass(this)
							        .addField(maskedFieldAnnotation)
							        .describe("FIELD_MASKED")
							        .addField(fa)
							        .describe("FIELD_MASKING"));
						}
					}
				}
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
		}

		super.visit(obj);
	}

	public void visit(Method obj) {
		super.visit(obj);
		numParms = obj.getArgumentTypes().length;
		if (!obj.isStatic()) numParms++;
		// System.out.println(obj);
		// System.out.println(numParms);
		staticMethod = obj.isStatic();
	}

	/**
	 * This property enables production of warnings for
	 * locals which obscure fields.
	 */
	private static final boolean ENABLE_LOCALS =
		Boolean.getBoolean("findbugs.maskedfields.locals");

	public void visit(LocalVariableTable obj) {
		if (ENABLE_LOCALS) {
			if (staticMethod)
				return;

			LocalVariable[] vars = obj.getLocalVariableTable();
			// System.out.println("Num params = " + numParms);
			for (int v = 0; v < vars.length; v++) {
				LocalVariable var = vars[v];
				if (var.getIndex() < numParms)
					continue;
				String varName = var.getName();
				if (varName.equals("serialVersionUID"))
					continue;
				Field f = classFields.get(varName);
				// System.out.println("Checking " + varName);
				// System.out.println(" got " + f);
				// TODO: we could distinguish between obscuring a field in the same class
				// vs. obscuring a field in a superclass.  Not sure how important that is.
				if (f != null) {
					FieldAnnotation fa
					        = FieldAnnotation.fromBCELField(getClassName(), f);
					if (true || var.getStartPC() > 0)
						bugReporter.reportBug(new BugInstance("MF_METHOD_MASKS_FIELD", LOW_PRIORITY)
						        .addClassAndMethod(this)
						        .addField(fa)
						        .addSourceLine(this, var.getStartPC() - 1));
				}
			}
		}
		super.visit(obj);
	}
}

// vim:ts=4
