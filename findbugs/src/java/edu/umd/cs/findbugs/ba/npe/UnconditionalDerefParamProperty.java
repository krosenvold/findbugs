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
package edu.umd.cs.findbugs.ba.npe;

import edu.umd.cs.findbugs.ba.interproc.MethodProperty;

/**
 * Method property recording which parameters might be 
 * dereferenced unconditionally.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalDerefParamProperty implements MethodProperty<UnconditionalDerefParamProperty> {
	private int unconditionalDerefParamSet;
	
	/**
	 * Constructor.
	 * Parameters are all assumed not to be unconditionally dereferenced.
	 */
	public UnconditionalDerefParamProperty() {
		this.unconditionalDerefParamSet = 0;
	}
	
	/**
	 * Get the unconditional deref bitset.
	 * 
	 * @return the unconditional deref bitset
	 */
	int getUnconditionalDerefParamSet() {
		return unconditionalDerefParamSet;
	}
	
	/**
	 * Set the unconditional deref bitset.
	 * 
	 * @param unconditionalDerefParamSet the unconditional deref bitset
	 */
	void setUnconditionalDerefParamSet(int unconditionalDerefParamSet) {
		this.unconditionalDerefParamSet = unconditionalDerefParamSet;
	}
	
	/**
	 * Set whether or not a parameter might be unconditionally dereferenced.
	 * 
	 * @param param              the parameter index
	 * @param unconditionalDeref true if the parameter might be unconditionally dereferenced, false otherwise
	 */
	public void setParamUnconditionalDeref(int param, boolean unconditionalDeref) {
		if (param < 0 || param > 31)
			return;
		if (unconditionalDeref) {
			unconditionalDerefParamSet |= (1 << param);
		} else {
			unconditionalDerefParamSet &= ~(1 << param);
		}
	}
	
	/**
	 * Return whether or not a parameter might be unconditionally dereferenced.
	 * 
	 * @param param the parameter index
	 * @return true if the parameter might be unconditionally dereferenced, false otherwise
	 */
	public boolean paramUnconditionalDeref(int param) {
		if (param < 0 || param > 31)
			return false;
		else
			return (unconditionalDerefParamSet & (1 << param)) != 0;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.interproc.MethodProperty#duplicate()
	 */
	public UnconditionalDerefParamProperty duplicate() {
		UnconditionalDerefParamProperty dup = new UnconditionalDerefParamProperty();
		dup.unconditionalDerefParamSet = this.unconditionalDerefParamSet;
		return dup;
	}

}
