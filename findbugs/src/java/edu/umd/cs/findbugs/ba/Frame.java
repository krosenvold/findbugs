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

import java.util.*;

/**
 * Generic class for representing a Java stack frame as a dataflow value.
 * A frame consists of "slots", which represent the local variables
 * and values on the Java operand stack.
 * Slots 0 .. <code>getNumLocals() - 1</code> represent the local variables.
 * Slots <code>getNumLocals()</code> .. <code>getNumSlots() - 1</code>
 * represent the Java operand stack.
 *
 * <p> Frame is parametized by "ValueType", which is the type of value
 * to be stored in the Frame's slots.  This type must form a lattice,
 * according to the abstract mergeValues() operation.
 *
 * <p> A Frame may have the special "TOP" value. Such frames serve as
 * the identity element for the mergeWith() operation.
 *
 * <p> A Frame may have the special "BOTTOM" value. The result of merging
 * any frame with BOTTOM is BOTTOM.
 *
 * @see FrameAnalysis
 * @author David Hovemeyer
 */
public abstract class Frame<ValueType> {

	////////////////////////////////////////////////////////////////////////////////////
	// Instance variables
	////////////////////////////////////////////////////////////////////////////////////

	/** Number of local variables in the method. */
	private int numLocals;

	/** Array storing the values of local variables and operand stack slots.  */
	private ArrayList<ValueType> slotList;

	/**
	 * Flag marking this frame as a special "TOP" value.
	 * Such Frames serve as the identity element when merging.
	 */
	private boolean isTop;

	/**
	 * Flag marking this frame as a special "BOTTOM" value.
	 * Such Frames arise when merging two frames of different size.
	 */
	private boolean isBottom;

	////////////////////////////////////////////////////////////////////////////////////
	// Methods
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor.
	 * @param numLocals number of local variable slots in the method
	 */
	public Frame(int numLocals) {
		this.numLocals = numLocals;
		slotList = new ArrayList<ValueType>();
		for (int i = 0; i < numLocals; ++i)
			slotList.add(getDefaultValue());
		isTop = false;
		isBottom = false;
	}

	/**
	 * Return whether or not this object the special "TOP" value for Frames.
	 * Such Frames are the identity element of the mergeWith() operation.
	 */
	public boolean isTop() {
		return isTop;
	}

	/**
	 * Make this frame the special "TOP" value.
	 * Such Frames are the identity element of the mergeWith() operation.
	 */
	public void setTop() {
		isTop = true;
		isBottom = false;
	}

	/**
	 * Return whether or not this object is the special "BOTTOM" value for Frames.
	 * Such Frames arise when merging two frames of different size.
	 */
	public boolean isBottom() {
		return isBottom;
	}

	/**
	 * Make this Frame the special "BOTTOM" value.
	 * Such Frames arise when merging two frames of different size.
	 */
	public void setBottom() {
		isBottom = true;
		isTop = false;
	}

	/**
	 * Set the Frame to be valid (neither TOP nor BOTTOM).
	 */
	public void setValid() {
		isTop = isBottom = false;
	}

	/**
	 * Is the frame valid (meaning it is not TOP or BOTTOM)?
	 */
	public boolean isValid() { return !isTop() && !isBottom(); }

	/**
	 * Push a value onto the Java operand stack.
	 * @param value the ValueType to push
	 */
	public void pushValue(ValueType value) {
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		slotList.add(value);
	}

	/**
	 * Pop a value off of the Java operand stack.
	 * @return the value that was popped
	 * @throws DataflowAnalysisException if the Java operand stack is empty
	 */
	public ValueType popValue() throws DataflowAnalysisException {
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		if (slotList.size() == numLocals) throw new DataflowAnalysisException("operand stack empty");
		return slotList.remove(slotList.size() - 1);
	}

	/**
	 * Get the value on the top of the Java operand stack.
	 * @throws DataflowAnalysisException if the Java operand stack is empty
	 */
	public ValueType getTopValue() throws DataflowAnalysisException {
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		assert slotList.size() >= numLocals;
		if (slotList.size() == numLocals)
			throw new DataflowAnalysisException("operand stack is empty");
		return slotList.get(slotList.size() - 1);
	}

	/**
	 * Clear the Java operand stack.
	 * Only local variable slots will remain in the frame.
	 */
	public void clearStack() {
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		assert slotList.size() >= numLocals;
		if (slotList.size() > numLocals)
			slotList.subList(numLocals, slotList.size()).clear();
	}

	/**
	 * Get the depth of the Java operand stack.
	 */
	public int getStackDepth() {
		return slotList.size() - numLocals;
	}

	/** Get the number of locals. */
	public int getNumLocals() { return numLocals; }

	/** Get the number of slots (locals plus stack values). */
	public int getNumSlots() { return slotList.size(); }

	/**
	 * Get the value at the <i>n</i>th slot.
	 * @param n the slot to get the value of
	 * @return the value in the slot
	 */
	public ValueType getValue(int n) {
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		return slotList.get(n);
	}

	/**
	 * Set the value at the <i>n</i>th slot.
	 * @param n the slot in which to set a new value
	 * @param value the value to set
	 */
	public void setValue(int n, ValueType value) {
		if (!isValid()) throw new IllegalStateException("accessing top or bottom frame");
		slotList.set(n, value);
	}

	/**
	 * Return true if this stack frame is the same as the one given
	 * as a parameter.
	 * @param other the other Frame
	 * @return true if the frames are the same, false otherwise
	 */
	public boolean sameAs(Frame<ValueType> other) {
		if (isTop != other.isTop)
			return false;

		if (isTop && other.isTop)
			return true;

		if (isBottom != other.isBottom)
			return false;

		if (isBottom && other.isBottom)
			return true;

		if (getNumSlots() != other.getNumSlots())
			return false;

		for (int i = 0; i < getNumSlots(); ++i)
			if (!getValue(i).equals(other.getValue(i)))
				return false;

		return true;
	}

	/**
	 * Make this Frame exactly the same as the one given as a parameter.
	 * @param other the Frame to make this object the same as
	 */
	public void copyFrom(Frame<ValueType> other) {
		slotList.clear();
		slotList.addAll(other.slotList);
		isTop = other.isTop;
		isBottom = other.isBottom;
	}

	/**
	 * Merge this object with the one given as a parameter.
	 * @param other the other Frame object
	 * @throws IllegalStateException if the two objects cannot be merged
	 */
	public void mergeWith(Frame<ValueType> other) throws DataflowAnalysisException {
		// Handle if this Frame or the other Frame is the special "TOP" value.
		if (isTop) {
			copyFrom(other); // I'm the identity element, so copy the other Frame
			return;
		} else if (other.isTop)
			return;			// Other Frame is the identity element, so I stay the same

		// Handle if this Frame or the other Frame is the special "BOTTOM" value.
		if (isBottom)
			return;			// I'm the bottom element, so I stay that way
		else if (other.isBottom) {
			setBottom();	// Other Frame is the bottom element, so I become the bottom element too
			return;
		}

		// If the number of slots in the Frames differs,
		// then the result is the special "BOTTOM" value.
		if (getNumSlots() != other.getNumSlots()) {
			setBottom();
			return;
		}

		// Usual case: ordinary Frames consisting of the same number of values.
		// Merge each value in the two slot lists element-wise.
		for (int i = 0; i < slotList.size(); ++i)
			slotList.set(i, mergeValues(slotList.get(i), other.slotList.get(i)));
	}

	/**
	 * Merge two values.
	 * @param a first value to merge
	 * @param b second value to merge
	 * @return the merged value
	 */
	public abstract ValueType mergeValues(ValueType a, ValueType b);

	/**
	 * Get the default value (to be put in slots of newly created frames).
	 */
	public abstract ValueType getDefaultValue();

}

// vim:ts=4
