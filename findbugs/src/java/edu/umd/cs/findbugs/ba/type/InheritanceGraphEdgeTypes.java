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
 * Constants defining types of inheritance graph edges.
 * @see InheritanceGraph
 * @author David Hovemeyer
 */
public interface InheritanceGraphEdgeTypes {
	/**
	 * Class edge. One class directly extends another class.
	 */
	public static final int CLASS_EDGE = 1;

	/**
	 * Interface edge. A class directly implements an interface,
	 * or one interface directly extends another.
	 */
	public static final int INTERFACE_EDGE = 2;
}

// vim:ts=4
