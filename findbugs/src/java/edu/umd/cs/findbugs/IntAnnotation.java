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

import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

import java.io.IOException;

import org.dom4j.Branch;
import org.dom4j.DocumentException;
import org.dom4j.Element;

/**
 * Bug annotation class for integer values.
 *
 * @author David Hovemeyer
 * @see BugAnnotation
 */
public class IntAnnotation implements BugAnnotation {
	private static final String DEFAULT_ROLE = "INT_DEFAULT";

	private int value;
	private String description;

	/**
	 * Constructor.
	 *
	 * @param value the integer value
	 */
	public IntAnnotation(int value) {
		this.value = value;
		this.description = DEFAULT_ROLE;
	}

	/**
	 * Get the integer value.
	 *
	 * @return the integer value
	 */
	public int getValue() {
		return value;
	}

	public void accept(BugAnnotationVisitor visitor) {
		visitor.visitIntAnnotation(this);
	}

	public String format(String key) {
		return String.valueOf(value);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public int hashCode() {
		return value;
	}

	public boolean equals(Object o) {
		if (!(o instanceof IntAnnotation))
			return false;
		return value == ((IntAnnotation) o).value;
	}

	public int compareTo(BugAnnotation o) {
		if (!(o instanceof IntAnnotation)) // BugAnnotations must be Comparable with any type of BugAnnotation
			return this.getClass().getName().compareTo(o.getClass().getName());
		return value - ((IntAnnotation) o).value;
	}

	public String toString() {
		String pattern = I18N.instance().getAnnotationDescription(description);
		FindBugsMessageFormat format = new FindBugsMessageFormat(pattern);
		return format.format(new BugAnnotation[]{this});
	}

	/* ----------------------------------------------------------------------
	 * XML Conversion support
	 * ---------------------------------------------------------------------- */

	private static final String ELEMENT_NAME = "Int";
/*
	private static class IntAnnotationXMLTranslator implements XMLTranslator {
		public String getElementName() {
			return ELEMENT_NAME;
		}

		public XMLConvertible fromElement(Element element) throws DocumentException {
			try {
				int value = Integer.parseInt(element.attributeValue("value"));
				IntAnnotation annotation = new IntAnnotation(value);

				String role = element.attributeValue("role");
				if (role != null)
					annotation.setDescription(role);

				return annotation;
			} catch (NumberFormatException e) {
				throw new DocumentException("Bad attribute value: " + e.toString());
			}
		}
	}

	static int dummy; // XXX: needed to allow BugCollection to force static init in JDK 1.5

	static {
		XMLTranslatorRegistry.instance().registerTranslator(new IntAnnotationXMLTranslator());
	}

	public Element toElement(Branch parent) {
		Element element = parent.addElement(ELEMENT_NAME)
		        .addAttribute("value", String.valueOf(value));

		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			element.addAttribute("role", role);

		return element;
	}
*/

	public void writeXML(XMLOutput xmlOutput) throws IOException {
		XMLAttributeList attributeList = new XMLAttributeList()
			.addAttribute("value", String.valueOf(value));

		String role = getDescription();
		if (!role.equals(DEFAULT_ROLE))
			attributeList.addAttribute("role", role);

		xmlOutput.openCloseTag(ELEMENT_NAME, attributeList);
	}
}

// vim:ts=4
