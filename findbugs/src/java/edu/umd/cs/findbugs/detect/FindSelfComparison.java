/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LineNumberTable;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindSelfComparison extends OpcodeStackDetector {

	final BugAccumulator bugAccumulator;

	public FindSelfComparison(BugReporter bugReporter) {
		this.bugAccumulator = new BugAccumulator(bugReporter);
	}
	String f;
	String className;
	int state;
	
	int putFieldRegister;
	
	int putFieldPC = Integer.MIN_VALUE;
	OpcodeStack.Item putFieldObj;
	OpcodeStack.Item putFieldValue;
	XField putFieldXField;
	

	@Override
	public void visit(Code obj) {
		// System.out.println(getFullyQualifiedMethodName());
		whichRegister = -1;
		registerLoadCount = 0;
		state = 0;
		resetDoubleAssignmentState();
		super.visit(obj);
		resetDoubleAssignmentState();
		bugAccumulator.reportAccumulatedBugs();
	}

	/**
     * 
     */
    private void resetDoubleAssignmentState() {
	    putFieldPC = Integer.MIN_VALUE;
		putFieldXField = null;
		putFieldValue = null;
		putFieldObj = null;
    }

	@Override
    public void sawBranchTo(int target) {
		resetDoubleAssignmentState();
	}
	@Override
	public void sawOpcode(int seen) {
		// System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + whichRegister + " " + registerLoadCount);
		
	    checkPUTFIELD: if (seen == PUTFIELD) {
	    	OpcodeStack.Item obj = stack.getStackItem(1);
	    	OpcodeStack.Item value = stack.getStackItem(0);
	    	XField f = getXFieldOperand();
	    	if (putFieldPC + 10 > getPC() 
	    			&& f.equals(putFieldXField)
	    			&& obj.equals(putFieldObj)) {
	    		LineNumberTable table = getCode().getLineNumberTable();
	    		if (table != null) {
	    			int first = table.getSourceLine(putFieldPC);
	    			int second = table.getSourceLine(getPC());
	    			if (first+1 < second) 
	    				break checkPUTFIELD;
	    		} else if (putFieldPC + 4 < getPC()) 
	    			break checkPUTFIELD;
	    		int priority = value.equals(putFieldValue) ? NORMAL_PRIORITY : HIGH_PRIORITY;
	    		bugAccumulator.accumulateBug(new BugInstance(this, "SA_FIELD_DOUBLE_ASSIGNMENT", priority)
				.addClassAndMethod(this)
				.addReferencedField(this), this);
	    	}
	    	putFieldPC = getPC();
	    	putFieldXField = f;
	    	putFieldObj = obj;
	    	putFieldValue = value;
	    	
	    } else if (isReturn(seen))
	    	resetDoubleAssignmentState();
	    
	    
		if (false) switch (state) {
		case 0:
			if (seen == DUP_X1) state = 4;
			break;

		case 4:
			if (seen == PUTFIELD) {

				f = getRefConstantOperand();
				className = getClassConstantOperand();
				OpcodeStack.Item item1 = stack.getStackItem(1);
				putFieldRegister = item1.getRegisterNumber();
				if (putFieldRegister >= 0)
					state = 5;
				else state = 0;
			} else
				state = 0;
			break;
		case 5:
			if (seen == PUTFIELD && getRefConstantOperand().equals(f) && getClassConstantOperand().equals(className)) {
				OpcodeStack.Item item1 = stack.getStackItem(1);
				if (putFieldRegister == item1.getRegisterNumber())
				bugAccumulator.accumulateBug(new BugInstance(this, "SA_FIELD_DOUBLE_ASSIGNMENT", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addReferencedField(this), this);
			}
			state = 0;
			break;
		}
		switch (seen) {
		case INVOKEVIRTUAL:
		case INVOKEINTERFACE:
			if (getClassName().toLowerCase().indexOf("test") >= 0)
				break;
			if (getMethodName().toLowerCase().indexOf("test") >= 0)
				break;
			if (getSuperclassName().toLowerCase().indexOf("test") >= 0)
				break;
			if (getNextOpcode() == POP)
				break;
			String name = getNameConstantOperand();
			
			if (name.equals("equals") || name.equals("compareTo")) {
				String sig = getSigConstantOperand();
				SignatureParser parser = new SignatureParser(sig);
				if (parser.getNumParameters() == 1
				        && (name.equals("equals") && sig.endsWith(";)Z") || name.equals("compareTo") && sig.endsWith(";)I")))
					checkForSelfOperation(seen, "COMPARISON");
			}
			break;

		case LOR:
		case LAND:
		case LXOR:
		case LSUB:
		case IOR:
		case IAND:
		case IXOR:
		case ISUB:
			checkForSelfOperation(seen, "COMPUTATION");
			break;
		case FCMPG:
		case DCMPG:
		case DCMPL:
		case FCMPL:
			break;
		case LCMP:
		case IF_ACMPEQ:
		case IF_ACMPNE:
		case IF_ICMPNE:
		case IF_ICMPEQ:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ICMPLT:
		case IF_ICMPGE: 
			checkForSelfOperation(seen, "COMPARISON");
		}
		if (isRegisterLoad() && seen != IINC) {
			if (getRegisterOperand() == whichRegister) registerLoadCount++;
			else {
				whichRegister = getRegisterOperand();
				registerLoadCount = 1;
			}
		} else {
			whichRegister = -1;
			registerLoadCount = 0;
		}
	}
	int whichRegister;
	int registerLoadCount;



private void checkForSelfOperation(int opCode, String op) {
	{

		OpcodeStack.Item item0 = stack.getStackItem(0);
		OpcodeStack.Item item1 = stack.getStackItem(1);

		if (item0.getSignature().equals("D")
				|| item0.getSignature().equals("F"))
			return;
		if (item1.getSignature().equals("D")
				|| item1.getSignature().equals("F"))
			return;

		XField field0 = item0.getXField();
		XField field1 = item1.getXField();
		int fr0 = item0.getFieldLoadedFromRegister();
		int fr1 = item1.getFieldLoadedFromRegister();
		if (field0 != null && field0.equals(field1) && fr0 != -1 && fr0 == fr1)
			bugAccumulator.accumulateBug(new BugInstance(this,
					"SA_FIELD_SELF_" + op, NORMAL_PRIORITY)
			.addClassAndMethod(this).addField(field0), this);

		else if (opCode == IXOR && item0.equals(item1)) {
			LocalVariableAnnotation localVariableAnnotation = LocalVariableAnnotation
            .getLocalVariableAnnotation(this, item0);
			if (localVariableAnnotation != null) 
				bugAccumulator.accumulateBug(new BugInstance(this,
					"SA_LOCAL_SELF_" + op,  HIGH_PRIORITY)
			.addClassAndMethod(this).add(
					localVariableAnnotation),this);
		} else if (opCode == ISUB  && registerLoadCount >= 2) { // let FindSelfComparison2 report this; more accurate
			bugAccumulator.accumulateBug(new BugInstance(this,
					"SA_LOCAL_SELF_" + op, (opCode == ISUB || opCode == LSUB  || opCode == INVOKEINTERFACE || opCode == INVOKEVIRTUAL) ? NORMAL_PRIORITY : HIGH_PRIORITY)
			.addClassAndMethod(this).add(
					LocalVariableAnnotation
					.getLocalVariableAnnotation(
							getMethod(), whichRegister, getPC(),
							getPC() - 1)),this);
		}
	}
}
}
