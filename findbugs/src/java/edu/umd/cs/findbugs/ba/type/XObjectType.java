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

/**
 * Lightweight data structure representing an
 * <i>object type</i>: a node in the
 * class hierarchy (i.e., a class or interface).
 * Note that not all object types represent Java classes
 * or interfaces: e.g., array types.
 *
 * <p> Instances of XType participate in the flyweight pattern,
 * meaning there is at most one instance per type.
 * Instances should be created and accessed using the XTypeFactory
 * class.
 *
 * @author David Hovemeyer
 */
public abstract class XObjectType extends XReferenceType {
	private String typeSignature;
	private XType superclass;
	private XType[] implementedInterfaceList;

	protected XObjectType(String typeSignature) {
		this.typeSignature = typeSignature;
	}

	void setSuperclass(XType superclass) {
		this.superclass = superclass;
	}

	void setImplementedInterfaceList(XType[] implementedInterfaceList) {
		this.implementedInterfaceList = implementedInterfaceList;
	}

	public String getSignature() {
		return typeSignature;
	}

	// All object types are valid as array element types.
	public boolean isValidArrayElementType() {
		return true;
	}
}

// vim:ts=4
