/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.type;

import org.apache.bcel.Constants;

public class XBasicType implements XType {
	private int typeCode;

	XBasicType(int typeCode) {
		this.typeCode = typeCode;
	}

	public String getSignature() {
		return Constants.SHORT_TYPE_NAMES[typeCode];
	}

	public int getTypeCode() {
		return typeCode;
	}

	public boolean isBasicType() {
		return true;
	}

	public boolean isReferenceType() {
		return false;
	}

	public boolean isValidArrayElementType() {
		return typeCode != Constants.T_VOID;
	}

	public void accept(XTypeVisitor visitor) {
		visitor.visitXBasicType(this);
	}

	public boolean equals(Object o) {
		if (o.getClass() != this.getClass())
			return false;
		XBasicType other = (XBasicType) o;
		return this.typeCode == other.typeCode;
	}

	public int hashCode() {
		return 1003 * typeCode;
	}
}

// vim:ts=4
