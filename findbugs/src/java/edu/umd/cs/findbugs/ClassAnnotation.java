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

import org.dom4j.Element;
import org.dom4j.Branch;
import org.dom4j.DocumentException;

/**
 * A BugAnnotation object specifying a Java class involved in the bug.
 *
 * @see BugAnnotation
 * @see BugInstance
 * @author David Hovemeyer
 */
public class ClassAnnotation extends PackageMemberAnnotation {
	private static final String DEFAULT_ROLE = "CLASS_DEFAULT";

	/**
	 * Constructor.
	 * @param className the name of the class
	 * @param sourceFile the name of the source file where the class is defined
	 */
	public ClassAnnotation(String className) {
		super(className, DEFAULT_ROLE);
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitClassAnnotation(this);
	}

	protected String formatPackageMember(String key) {
		if (key.equals(""))
			return className;
		else
			throw new IllegalArgumentException("unknown key " + key);
	}

	public int hashCode() {
		return className.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof ClassAnnotation))
			return false;
		ClassAnnotation other = (ClassAnnotation) o;
		return className.equals(other.className);
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof ClassAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		ClassAnnotation other = (ClassAnnotation) o;
		return className.compareTo(other.className);
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	private static final String ELEMENT_NAME = "Class";

	private static class ClassAnnotationXMLTranslator implements XMLTranslator {
		public String getElementName() {
			return ELEMENT_NAME;
		}

		public XMLConvertible fromElement(Element element) throws DocumentException {
			String className = element.attributeValue("classname");
			ClassAnnotation annotation = new ClassAnnotation(className);
			String role = element.attributeValue("role");
			if (role != null)
				annotation.setDescription(role);

			return annotation;
		}
	}

	static {
		XMLTranslatorRegistry.instance().registerTranslator(new ClassAnnotationXMLTranslator());
	}

	public Element toElement(Branch parent) {
		Element element = parent.addElement(ELEMENT_NAME)
			.addAttribute("classname", getClassName());
		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			element.addAttribute("role", role);
		return element;
	}
}

// vim:ts=4
