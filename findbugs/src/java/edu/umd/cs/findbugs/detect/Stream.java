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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ResourceCreationPoint;

import edu.umd.cs.findbugs.ba.Location;

import org.apache.bcel.generic.InstructionHandle;

/**
 * A Stream object marks the location in the code where a
 * stream is created.
 */
public class Stream extends ResourceCreationPoint {
	private String streamBase;
	private boolean isUninteresting;
	private boolean isOpenOnCreation;
	private InstructionHandle ctorHandle;

	public Stream(Location location, String streamClass, String streamBase,
		boolean isUninteresting) {
		this(location, streamClass, streamBase, isUninteresting, false);
	}

	public Stream(Location location, String streamClass, String streamBase,
		boolean isUninteresting, boolean isOpenOnCreation) {
		super(location, streamClass);
		this.streamBase = streamBase;
		this.isUninteresting = isUninteresting;
		this.isOpenOnCreation = isOpenOnCreation;
	}

	public String getStreamBase() { return streamBase; }

	public boolean isUninteresting() { return isUninteresting; }

	public boolean isOpenOnCreation() { return isOpenOnCreation; }

	public void setConstructorHandle(InstructionHandle handle) { this.ctorHandle = handle; }

	public InstructionHandle getConstructorHandle() { return ctorHandle; }
}

// vim:ts=3
