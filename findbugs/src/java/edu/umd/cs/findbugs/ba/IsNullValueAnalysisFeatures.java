/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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

/*
 * Created on Feb 22, 2005
 */
package edu.umd.cs.findbugs.ba;

/**
 * Knobs for null value analysis.
 * 
 * @author David Hovemeyer
 */
public interface IsNullValueAnalysisFeatures {

	/**
	 * Do not downgrade "null on simple path" values to "null on complex path"
	 * on control splits.
	 */
	public static final boolean NO_SPLIT_DOWNGRADE_NSP =
		Boolean.getBoolean("inva.noSplitDowngradeNSP");
	
	/**
	 * Don't consider switch default cases as exception paths.
	 */
	public static final boolean NO_SWITCH_DEFAULT_AS_EXCEPTION =
		Boolean.getBoolean("inva.noSwitchDefaultAsException");

	/**
	 * Keep track of an extra branch, so we can distinguish
	 * conditionally-null values with two branches from
	 * conditionally-null values with three or more branches.  
	 */
	public static final boolean NCP_EXTRA_BRANCH =
		Boolean.getBoolean("inva.ncpExtraBranch");
	
	/**
	 * If this property is true, then we assume parameters
	 * and return values can be null (but aren't definitely null).
	 */
	public static final boolean UNKNOWN_VALUES_ARE_NSP =
		Boolean.getBoolean("findbugs.nullderef.assumensp");

}
