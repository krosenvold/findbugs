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

/**
 * Types of control-flow edges
 * @see Edge
 */
public interface EdgeTypes {
    /** Unknown edge type. */
    public static final int UNKNOWN_EDGE = -1;
    /** Edge type for fall-through to next instruction. */
    public static final int FALL_THROUGH_EDGE = 0;
    /** Edge type for IFCMP instructions when condition is true. */
    public static final int IFCMP_EDGE = 1;
    /** Edge type for switch instructions (explicit case). */
    public static final int SWITCH_EDGE = 2;
    /** Edge type for switch instructions (default case). */
    public static final int SWITCH_DEFAULT_EDGE = 3;
    /** Edge type for JSR instructions. */
    public static final int JSR_EDGE = 4;
    /** Edge type for RET instructions. */
    public static final int RET_EDGE = 5;
    /** Edge type for GOTO instructions. */
    public static final int GOTO_EDGE = 6;
    /** Edge type for RETURN instructions.  (These must go to the exit node of the CFG). */
    public static final int RETURN_EDGE = 7;
    /** Edge representing the possibility that an exception might propagate
        out of the current method.  Such edges always go to the exit node 
        in the CFG. */
    public static final int UNHANDLED_EXCEPTION_EDGE = 8;
    /** Edge representing control flow from an exception-raising basic block
        to an explicit handler for the exception. */
    public static final int HANDLED_EXCEPTION_EDGE = 9;
    /** Edge from entry node to real start node. */
    public static final int START_EDGE = 10;
    /** Special (synthetic) edge for path profiling; CFG entry to backedge target. */
    public static final int BACKEDGE_TARGET_EDGE = 11;
    /** Special (synthetic) edge for path profiling; backedge source to CFG exit. */
    public static final int BACKEDGE_SOURCE_EDGE = 12;
    /** System.exit() edge. */
    public static final int EXIT_EDGE = 13;
}
