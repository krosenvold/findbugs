/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005, Tom Truscott <trt@unx.sas.com>
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

import java.text.NumberFormat;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class FindPuzzlers extends BytecodeScanningDetector implements Constants2 {


	BugReporter bugReporter;
	public FindPuzzlers(BugReporter bugReporter) {
		this.bugReporter =  bugReporter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visit(Code obj) {
		prevOpcodeIncrementedRegister = -1;
		stack.resetForMethodEntry(this);
		super.visit(obj);
	}

	int prevOpcodeIncrementedRegister;
	int valueOfConstantArgumentToShift;
	boolean constantArgumentToShift;
	OpcodeStack stack = new OpcodeStack();
	public void sawOpcode(int seen) {

         if ((seen == INVOKEVIRTUAL)
                &&   getNameConstantOperand().equals("equals")
                &&   getSigConstantOperand().equals("(Ljava/lang/Object;)Z")
		&& stack.getStackDepth() > 1) {
			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);

			if (item0.isArray() || item1.isArray()) {
				bugReporter.reportBug(new BugInstance("EC_BAD_ARRAY_COMPARE", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this));
		}
		}
 


		if ((seen == IFEQ || seen == IFNE) && getPrevOpcode(1) == IMUL
			&& ( getPrevOpcode(2) == SIPUSH
				|| getPrevOpcode(2) == BIPUSH
				)
			&& getPrevOpcode(3) == IREM
				)
			 bugReporter.reportBug(new BugInstance(this, "IM_MULTIPLYING_RESULT_OF_IREM", LOW_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));
		if (seen == I2S && getPrevOpcode(1) == IUSHR
				&& (!constantArgumentToShift || valueOfConstantArgumentToShift % 16 != 0)
			||
		    seen == I2B && getPrevOpcode(1) == IUSHR
				&& (!constantArgumentToShift || valueOfConstantArgumentToShift % 8 != 0)
			 )
			 bugReporter.reportBug(new BugInstance(this, "ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT", LOW_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));

		constantArgumentToShift = false;
		if ( (seen == IUSHR 
				|| seen == ISHR 
				|| seen == ISHL )) {
			if (stack.getStackDepth() <= 1) {
				// don't understand; lie so other detectors won't get concerned
				constantArgumentToShift = true;
				valueOfConstantArgumentToShift = 8;
				}
			else {
			Object rightHandSide
				 = stack.getStackItem(0).getConstant();
			Object leftHandSide 
				=  stack.getStackItem(1).getConstant();
			if (rightHandSide != null && rightHandSide instanceof Integer) {
				constantArgumentToShift = true;
				valueOfConstantArgumentToShift = ((Integer)rightHandSide).intValue();
				if (valueOfConstantArgumentToShift < 0 || valueOfConstantArgumentToShift >= 32)
				 bugReporter.reportBug(new BugInstance(this, "ICAST_BAD_SHIFT_AMOUNT", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addInt(valueOfConstantArgumentToShift)
						.addSourceLine(this)
						);
				}
				if (leftHandSide != null 
					&& leftHandSide instanceof Integer
					&&  ((Integer)leftHandSide).intValue()
						> 0) {
				// boring; lie so other detectors won't get concerned
				constantArgumentToShift = true;
				valueOfConstantArgumentToShift = 8;
					}
			}
			}



	   if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 0
                        && getClassConstantOperand().equals("java/util/Date")
                        && getNameConstantOperand().equals("setDate")
                        && getSigConstantOperand().equals("(I)V")) {
			OpcodeStack.Item item = stack.getStackItem(0);
			Object o = item.getConstant();
			if (o != null && o instanceof Integer) {
				int v =  ((Integer)o).intValue();
				if (v < 0 || v > 11)
				 bugReporter.reportBug(new BugInstance(this, "PZ_BAD_MONTH", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addInt(v)
						.addCalledMethod(this)
						.addSourceLine(this)
					);
				}
		}
				
	   if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 1
                        && getClassConstantOperand().equals("java/util/Calendar")
                        && getNameConstantOperand().equals("set")
                        && getSigConstantOperand().equals("(III)V")
		||
	   	seen == INVOKESPECIAL && stack.getStackDepth() > 1
                        && getClassConstantOperand().equals("java/util/GregorianCalendar")
                        && getNameConstantOperand().equals("<init>")
                        && getSigConstantOperand().equals("(III)V")
		) {
			OpcodeStack.Item item = stack.getStackItem(1);
			Object o = item.getConstant();
			if (o != null && o instanceof Integer) {
				int v =  ((Integer)o).intValue();
				if (v < 0 || v > 11)
				 bugReporter.reportBug(new BugInstance(this, "PZ_BAD_MONTH", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addInt(v)
						.addCalledMethod(this)
						.addSourceLine(this)
						);
				}
		}
				


		if (isRegisterStore() && (seen == ISTORE 
			|| seen == ISTORE_0
			|| seen == ISTORE_1
			|| seen == ISTORE_2
			|| seen == ISTORE_3)
			&& getRegisterOperand() == prevOpcodeIncrementedRegister) {
			 bugReporter.reportBug(new BugInstance(this, "DLS_OVERWRITTEN_INCREMENT", HIGH_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));

			}
		if (seen == IINC) {
			prevOpcodeIncrementedRegister = getRegisterOperand();	
			}
		else
			prevOpcodeIncrementedRegister = -1;
		stack.sawOpcode(this,seen);
	}

}
