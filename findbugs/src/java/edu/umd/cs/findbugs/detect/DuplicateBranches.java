/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LOOKUPSWITCH;
import org.apache.bcel.generic.TABLESWITCH;
import org.apache.bcel.util.ByteSequence;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class DuplicateBranches extends PreorderVisitor implements Detector, StatelessDetector, Constants2
{
	private ClassContext classContext;
	private BugReporter bugReporter;
	
	public DuplicateBranches(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;
		classContext.getJavaClass().accept(this);
	}

	public void visitMethod(Method method) {
		try {
			if (method.getCode() == null)
				return;
			
			CFG cfg = classContext.getCFG(method);
	
			Iterator<BasicBlock> bbi = cfg.blockIterator();
			while (bbi.hasNext()) {
				BasicBlock bb = bbi.next();
				
				int numOutgoing = cfg.getNumOutgoingEdges(bb);
				if (numOutgoing == 2)
					findIfElseDuplicates(cfg, method, bb);
				else if (numOutgoing > 2)
					findSwitchDuplicates(cfg, method, bb);
			}
		} catch (Exception e) {
			bugReporter.logError("Failure examining basic blocks in Duplicate Branches detector", e);
		}
	}
	
	private void findIfElseDuplicates(CFG cfg, Method method, BasicBlock bb) {
		BasicBlock thenBB = null, elseBB = null;

		Iterator<Edge> iei = cfg.outgoingEdgeIterator(bb);
		while (iei.hasNext()) {
			Edge e = iei.next();
			if (e.getType() == EdgeTypes.IFCMP_EDGE) {
				elseBB = e.getTarget();
			}
			else if (e.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
				thenBB = e.getTarget();
			}
		}
		
		if ((thenBB == null) || (elseBB == null) 
		||  (thenBB.getFirstInstruction() == null) || (elseBB.getFirstInstruction() == null))
			return;
		
		int thenStartPos = thenBB.getFirstInstruction().getPosition();
		int elseStartPos = elseBB.getFirstInstruction().getPosition();
		
		BasicBlock thenFinishBlock = findThenFinish(cfg, thenBB, elseStartPos);
		
		if (thenFinishBlock == null)
			return;
		
		Instruction lastFinishIns = thenFinishBlock.getLastInstruction().getInstruction();
		if (!(lastFinishIns instanceof GotoInstruction))
			return;
		
		int thenFinishPos = thenFinishBlock.getLastInstruction().getPosition();
		int elseFinishPos = ((GotoInstruction) lastFinishIns).getTarget().getPosition();
		
		if (thenFinishPos >= elseStartPos)
			return;
		
		if ((thenFinishPos - thenStartPos) != (elseFinishPos - elseStartPos))
			return;
		
		byte[] thenBytes = getCodeBytes(method, thenStartPos, thenFinishPos);
		byte[] elseBytes = getCodeBytes(method, elseStartPos, elseFinishPos);
		
		if (!Arrays.equals(thenBytes, elseBytes))
			return;
		
		bugReporter.reportBug(new BugInstance(this, "DB_DUPLICATE_BRANCHES", LOW_PRIORITY)
				.addClass(classContext.getJavaClass())
				.addMethod(classContext.getJavaClass(), method)
				.addSourceLineRange(this.classContext, this, 
						thenBB.getFirstInstruction().getPosition(),
						thenBB.getLastInstruction().getPosition())
				.addSourceLineRange(this.classContext, this, 
						elseBB.getFirstInstruction().getPosition(),
						elseBB.getLastInstruction().getPosition()));		
	}
	
	private void findSwitchDuplicates(CFG cfg, Method method, BasicBlock bb) {		
		Iterator<Edge> iei = cfg.outgoingEdgeIterator(bb);
				
		int[] switchPos = new int[cfg.getNumOutgoingEdges(bb)+1];
		int idx = 0;
		
		while (iei.hasNext()) {
			Edge e = iei.next();
			if (EdgeTypes.SWITCH_EDGE == e.getType()) {
				BasicBlock target = e.getTarget();
				InstructionHandle firstIns = target.getFirstInstruction();
				if (firstIns == null)
					return;
				switchPos[idx++] = firstIns.getPosition();
			} else if (EdgeTypes.SWITCH_DEFAULT_EDGE == e.getType()) {
				BasicBlock target = e.getTarget();
				InstructionHandle firstIns = target.getFirstInstruction();
				if (firstIns == null)
					return;
				switchPos[idx++] = firstIns.getPosition();
			} else
				return;
		}
		
		Arrays.sort(switchPos);
		
		if (switchPos.length < 2)
			return;
						
		for (int i = 0; i < switchPos.length-2; i++) {
			int s1Length = switchPos[i+1] - switchPos[i];
			if (s1Length == 0)
				continue;
			
			byte[] s1Bytes = null;
			
			for (int j = i+1; j < switchPos.length-1; j++) {
				int s2Length = switchPos[j+1] - switchPos[j];
				if (s2Length == 0)
					continue;
				
				if (s1Length != s2Length)
					continue;
				
				if (s1Bytes == null)
					s1Bytes = getCodeBytes(method, switchPos[i], switchPos[i+1]);
								
				byte[] s2Bytes = getCodeBytes(method, switchPos[j], switchPos[j+1]);
				
				if (!Arrays.equals(s1Bytes, s2Bytes))
					continue;
				
				bugReporter.reportBug(new BugInstance(this, "DB_DUPLICATE_BRANCHES", LOW_PRIORITY)
						.addClass(classContext.getJavaClass())
						.addMethod(classContext.getJavaClass(), method)
						.addSourceLineRange(this.classContext, this, 
								switchPos[i],
								switchPos[i+1]-1)
						.addSourceLineRange(this.classContext, this, 
								switchPos[j],
								switchPos[j+1]-1));
				j = switchPos.length;
			}
		}
	}
	
	private byte[] getCodeBytes(Method m, int start, int end) {
		byte[] code = m.getCode().getCode();
		byte[] bytes = new byte[end-start];
		System.arraycopy( code, start, bytes, 0, end - start);
		
		try {
			ByteSequence sequence = new ByteSequence(code);
			while ((sequence.available() > 0) && (sequence.getIndex() < start)) {
				Instruction.readInstruction(sequence);
			}
			
			int pos;
			while (sequence.available() > 0 && ((pos = sequence.getIndex()) < end)) {
				Instruction ins = Instruction.readInstruction(sequence);
				if ((ins instanceof BranchInstruction)
				&&  !(ins instanceof TABLESWITCH)
				&&  !(ins instanceof LOOKUPSWITCH)) {
					BranchInstruction bi = (BranchInstruction)ins;
					int offset = bi.getIndex();
					if ((offset + pos) >= end) {
						bytes[pos+bi.getLength()-1 - start] = 0;
					}
				}
			}
		} catch (IOException ioe) {
		}
		
		return bytes;
	}
	
	private BasicBlock findThenFinish(CFG cfg, BasicBlock thenBB, int elsePos) {
		//Follow fall thru links until we find a goto link past the else
		
		Iterator<Edge> ie = cfg.outgoingEdgeIterator(thenBB);
		while (ie.hasNext()) {
			Edge e = ie.next();
			if (e.getType() == EdgeTypes.GOTO_EDGE) {
				InstructionHandle firstInsH = e.getTarget().getFirstInstruction();
				if (firstInsH != null) {
					int targetPos = firstInsH.getPosition();
					if (targetPos > elsePos)
						return e.getSource();
				}
			}
		}
		
		ie = cfg.outgoingEdgeIterator(thenBB);
		while (ie.hasNext()) {
			Edge e = ie.next();
			if (e.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
				BasicBlock target = e.getTarget();
				if (target.getFirstInstruction() == null)
					return findThenFinish(cfg, target, elsePos);
				int targetPos = target.getFirstInstruction().getPosition();
				if (targetPos < elsePos)
					return findThenFinish(cfg, target, elsePos);
			}
		}
		return null;
	}
	
	public void report() {
	}
}
