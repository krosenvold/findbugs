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
import org.apache.bcel.generic.*;
import org.dom4j.Element;
import org.dom4j.Branch;
import org.dom4j.DocumentException;

/**
 * A BugAnnotation specifying a particular field in particular class.
 *
 * @see BugAnnotation
 * @author David Hovemeyer
 */
public class FieldAnnotation extends PackageMemberAnnotation {
	private static final String DEFAULT_ROLE = "FIELD_DEFAULT";

	private String fieldName;
	private String fieldSig;
	private boolean isStatic;

	/**
	 * Constructor.
	 * @param className the name of the class containing the field
	 * @param fieldName the name of the field
	 * @param fieldSig the type signature of the field
	 */
	public FieldAnnotation(String className, String fieldName, String fieldSig, boolean isStatic) {
		super(className, DEFAULT_ROLE);
		this.fieldName = fieldName;
		this.fieldSig = fieldSig;
		this.isStatic = isStatic;
	}

	/**
	 * Factory method. Class name, field name, and field signatures are taken from
	 * the given visitor, which is visiting the field.
	 * @param visitor the visitor which is visiting the field
	 * @return the FieldAnnotation object
	 */
	public static FieldAnnotation fromVisitedField(BetterVisitor visitor) {
		return new FieldAnnotation(visitor.getBetterClassName(), visitor.getFieldName(), visitor.getFieldSig(),
			visitor.getFieldIsStatic());
	}

	/**
	 * Factory method. Class name, field name, and field signatures are taken from
	 * the given visitor, which is visiting a reference to the field
	 * (i.e., a getfield or getstatic instruction).
	 * @param visitor the visitor which is visiting the field reference
	 * @return the FieldAnnotation object
	 */
	public static FieldAnnotation fromReferencedField(DismantleBytecode visitor) {
		return new FieldAnnotation(visitor.getBetterClassConstant(), visitor.getNameConstant(), visitor.getSigConstant(),
			visitor.getRefFieldIsStatic());
	}

	/**
	 * Get the field name.
	 */
	public String getFieldName() {
		return fieldName;
	}

	/**
	 * Get the type signature of the field.
	 */
	public String getFieldSignature() {
		return fieldSig;
	}

	/**
	 * Return whether or not the field is static.
	 */
	public boolean isStatic() {
		return isStatic;
	}

	/**
	 * Is the given instruction a read of a field?
	 * @param ins the Instruction to check
	 * @param cpg ConstantPoolGen of the method containing the instruction
	 * @return the Field if the instruction is a read of a field, null otherwise
	 */
	public static FieldAnnotation isRead(Instruction ins, ConstantPoolGen cpg) {
		if (ins instanceof GETFIELD || ins instanceof GETSTATIC) {
			FieldInstruction fins = (FieldInstruction) ins;
			return new FieldAnnotation(fins.getClassName(cpg), fins.getName(cpg), fins.getSignature(cpg), fins instanceof GETSTATIC);
		} else
			return null;
	}

	/**
	 * Is the instruction a write of a field?
	 * @param the Instruction to check
	 * @param cpg ConstantPoolGen of the method containing the instruction
	 * @return the Field if instruction is a write of a field, null otherwise
	 */
	public static FieldAnnotation isWrite(Instruction ins, ConstantPoolGen cpg) {
		if (ins instanceof PUTFIELD || ins instanceof PUTSTATIC) {
			FieldInstruction fins = (FieldInstruction) ins;
			return new FieldAnnotation(fins.getClassName(cpg), fins.getName(cpg), fins.getSignature(cpg), fins instanceof PUTSTATIC);
		} else
			return null;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitFieldAnnotation(this);
	}

	protected String formatPackageMember(String key) {
		if (key.equals(""))
			return className + "." + fieldName;
		else if (key.equals("fullField")) {
			String pkgName = getPackageName();
			SignatureConverter converter = new SignatureConverter(fieldSig);
			StringBuffer result = new StringBuffer();
			if (isStatic)
				result.append("static ");
			result.append(converter.parseNext());
			result.append(' ');
			result.append(className);
			result.append('.');
			result.append(fieldName);
			return result.toString();
		} else
			throw new IllegalArgumentException("unknown key " + key);
	}

	public int hashCode() {
		return className.hashCode() + fieldName.hashCode() + fieldSig.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof FieldAnnotation))
			return false;
		FieldAnnotation other = (FieldAnnotation) o;
		return className.equals(other.className)
			&& fieldName.equals(other.fieldName)
			&& fieldSig.equals(other.fieldSig)
			&& isStatic == other.isStatic;
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof FieldAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		FieldAnnotation other = (FieldAnnotation) o;
		int cmp;
		cmp = className.compareTo(other.className);
		if (cmp != 0)
			return cmp;
		cmp = fieldName.compareTo(other.fieldName);
		if (cmp != 0)
			return cmp;
		return fieldSig.compareTo(other.fieldSig);
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	private static final String ELEMENT_NAME = "Field";

	private static class FieldAnnotationXMLTranslator implements XMLTranslator {
		public String getElementName() {
			return ELEMENT_NAME;
		}

		public XMLConvertible fromElement(Element element) throws DocumentException {
			String className = element.attributeValue("classname");
			String fieldName = element.attributeValue("name");
			String fieldSig  = element.attributeValue("signature");
			boolean isStatic = Boolean.valueOf(element.attributeValue("isStatic")).booleanValue();
			FieldAnnotation annotation = new FieldAnnotation(className, fieldName, fieldSig, isStatic);

			String role = element.attributeValue("role");
			if (role != null)
				annotation.setDescription(role);

			return annotation;
		}
	}

	static {
		XMLTranslatorRegistry.instance().registerTranslator(new FieldAnnotationXMLTranslator());
	}

	public Element toElement(Branch parent) {
		Element element = parent.addElement(ELEMENT_NAME)
			.addAttribute("classname", getClassName())
			.addAttribute("name", getFieldName())
			.addAttribute("signature", getFieldSignature())
			.addAttribute("isStatic", String.valueOf(isStatic()));

		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			element.addAttribute("role", role);

		return element;
	}
}

// vim:ts=4
