/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.daveho.ba;

/**
 * An AvailableLoad indicates a field and (optionally) object reference
 * for which a value is available.  It is used to implement
 * redundant load elimination and forward substitution in ValueNumberAnalysis.
 * The idea is that programmers often reload fields unnecessarily when they
 * "know" that the value will not change.  In order to deduce the intended
 * meaning of such code, our analyses need to figure out that such
 * loads return the same value.
 *
 * <p> AvailableLoad objects may be used as keys in both hash and tree
 * sets and maps.
 *
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class AvailableLoad implements Comparable<AvailableLoad> {
	private final ValueNumber reference;
	private final XField field;

	/**
	 * Constructor from static field.
	 * @param staticField the StaticField
	 */
	public AvailableLoad(StaticField staticField) {
		this.reference = null;
		this.field = staticField;
	}

	/**
	 * Constructor from object reference and instance field.
	 * @param reference the ValueNumber of the object reference
	 * @param field the InstanceField
	 */
	public AvailableLoad(ValueNumber reference, InstanceField field) {
		if (reference == null) throw new IllegalArgumentException();
		this.reference = reference;
		this.field = field;
	}

	/**
	 * Get the ValueNumber of the object reference.
	 * @return the ValueNumber, or null if this is a an available
	 *   static field load
	 */
	public ValueNumber getReference() {
		return reference;
	}

	/**
	 * Get the field for which a load is available.
	 * @return the XField
	 */
	public XField getField() {
		return field;
	}

	public int compareTo(AvailableLoad other) {
		int cmp = reference.compareTo(other.reference);
		if (cmp != 0)
			return cmp;
		return field.compareTo(other.field);
	}

	public int hashCode() {
		return reference.hashCode() ^ field.hashCode();
	}

	public boolean equals(Object o) {
		if (this.getClass() != o.getClass())
			return false;
		AvailableLoad other = (AvailableLoad) o;
		return reference.equals(other.reference) && field.equals(other.field);
	}

	public String toString() {
		return reference.getNumber() + "." + field;
	}
}

// vim:ts=4
