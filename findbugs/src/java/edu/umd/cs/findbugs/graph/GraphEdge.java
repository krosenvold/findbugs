/*
 * Generic graph library
 * Copyright (C) 2000,2003,2004 University of Maryland
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

// $Revision: 1.8 $

package edu.umd.cs.findbugs.graph;



/**
 * GraphEdge interface; represents an edge in a graph.
 */
public interface GraphEdge <ActualEdgeType, VertexType extends GraphVertex<VertexType>>
        extends Comparable<ActualEdgeType> {

	/**
	 * Get the source vertex.
	 */
	public VertexType getSource();

	/**
	 * Get the target vertex.
	 */
	public VertexType getTarget();

	/**
	 * Get the integer label.
	 */
	public int getLabel();

	/**
	 * Set the integer label.
	 */
	public void setLabel(int label);

}

// vim:ts=4
