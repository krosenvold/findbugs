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

package edu.umd.cs.findbugs;

/**
 * A BugCode is an abbreviation that is shared among some number
 * of BugPatterns.  For example, the code "HE" is shared by
 * all of the BugPatterns that represent hashcode/equals
 * violations.
 *
 * @see BugPattern
 * @author David Hovemeyer
 */
public class BugCode {
	private String abbrev;
	private String description;

	/**
	 * Constructor.
	 * @param abbrev the abbreviation for the bug code
	 * @param description a short textual description of the class of bug pattern
	 *   represented by this bug code
	 */
	public BugCode(String abbrev, String description) {
		this.abbrev = abbrev;
		this.description = description;
	}

	/** Get the abbreviation for this bug code. */
	public String getAbbrev() { return abbrev; }

	/** Get the short textual description of the bug code. */
	public String getDescription() { return description; }
}

// vim:ts=4
