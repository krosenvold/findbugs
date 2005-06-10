/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

/**
 * Boolean analysis properties for use in the AnalysisContext.
 * These can be used to enable or disable various analysis features
 * in the bytecode analysis framework.
 * 
 * @author David Hovemeyer
 */
public interface AnalysisFeatures {
	/**
	 * Determine (1) what exceptions can be thrown on exception edges,
	 * (2) which catch blocks are reachable, and (3) which exception
	 * edges carry only "implicit" runtime exceptions.
	 */
	public static final int ACCURATE_EXCEPTIONS = 0;

	/**
	 * A boolean flag which if set means that analyses should try to
	 * conserve space at the expense of precision.
	 */
	public static final int CONSERVE_SPACE = 1;
	
	/**
	 * If true, model the effect of instanceof checks in type analysis.
	 */
	public static final int MODEL_INSTANCEOF = 2;

	/**
	 * Number of boolean analysis properties reserved for the bytecode analysis framework.
	 * Clients of the framework may use property values &gt;= this value.
	 */
	public static final int NUM_BOOLEAN_ANALYSIS_PROPERTIES = 128; 

}

// vim:ts=4
