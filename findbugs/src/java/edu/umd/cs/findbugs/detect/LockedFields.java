/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.detect;
import edu.umd.cs.findbugs.*;
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.pugh.visitclass.Constants2;

public class LockedFields extends BytecodeScanningDetector implements   Constants2 {
    private static final boolean DEBUG = Boolean.getBoolean("lockedfields.debug");

    HashSet<FieldAnnotation> volatileOrFinalFields = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> fieldsWritten = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> fieldsRead = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> localLocks = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> publicFields = new HashSet<FieldAnnotation>();
    HashSet<FieldAnnotation> writtenOutsideOfConstructor = new HashSet<FieldAnnotation>();
    boolean synchronizedMethod;
    boolean publicMethod;
    boolean protectedMethod;
    // boolean privateMethod;
    boolean inConstructor;
    Map<FieldAnnotation, int[]> stats = new TreeMap<FieldAnnotation, int[]>();
    int state;
    boolean thisOnTopOfStack;
    boolean doubleThisOnTopOfStack;
    boolean thisLocked;
    boolean thisLockingOnly = true;

    private BugReporter bugReporter;

    static final int READ_LOCKED = 0;
    static final int WRITTEN_LOCKED = 1;
    static final int READ_UNLOCKED = 2;
    static final int WRITTEN_UNLOCKED = 3;

    static final String[] names = {
				"R/L",
				"W/L",
				"R/U",
				"W/U"};

  public LockedFields(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
  }

  private void updateStats(Set<FieldAnnotation> fields, int mode) {
	// if (privateMethod) return;
	if (!publicMethod && !protectedMethod) {
		if (mode == READ_UNLOCKED || mode == WRITTEN_UNLOCKED) return;
		}
	/*
	if (!publicMethod) {
		if (mode == READ_UNLOCKED || mode == WRITTEN_UNLOCKED) return;
		}
	*/
	for(Iterator<FieldAnnotation> i = fields.iterator(); i.hasNext(); ) {
		FieldAnnotation f = i.next();
		if (f.getClassName().equals(betterClassName) && mode <= WRITTEN_LOCKED) 
			localLocks.add(f);
		int  [] theseStats = (int []) stats.get(f); 
		if (theseStats == null) {
			theseStats = new int[4];
			stats.put(f, theseStats);
			}
		if (DEBUG) System.out.println(names[mode]
				+ "	" 
				+ betterMethodName
				+ "	" 
				+ f.toString());
		
		theseStats[mode]++;
		}
	}
  public void visit(JavaClass obj)     {
	super.visit(obj);
	}



    public void visit(Field obj) {
        super.visit(obj);

	FieldAnnotation f = FieldAnnotation.fromVisitedField(this);

	int flags = obj.getAccessFlags();
        boolean publicField = (flags & ACC_PUBLIC) != 0;
        boolean volatileField = (flags & ACC_VOLATILE) != 0;
        boolean finalField = (flags & ACC_FINAL) != 0;
	if (publicField) 
			publicFields.add(f);
	if (volatileField || finalField) 
			volatileOrFinalFields.add(f);
	}
    public void visit(Method obj) {
        super.visit(obj);
	int flags = obj.getAccessFlags();
        publicMethod = (flags & ACC_PUBLIC) != 0;
        protectedMethod = (flags & ACC_PROTECTED) != 0;
        synchronizedMethod = (flags & ACC_SYNCHRONIZED) != 0;
	if (synchronizedMethod) state = 1;
	else state = 0;
	fieldsWritten.clear();
	fieldsRead.clear();
        inConstructor = methodName.equals("<init>")
        		||  methodName.equals("<clinit>")
        		||  methodName.equals("readObject")
        		||  methodName.equals("clone")
        		||  methodName.equals("close")
        		||  methodName.equals("finalize");
	/*
        privateMethod = (flags & ACC_PRIVATE) != 0
        		|| methodName.startsWith("access$");
	*/

	}


    public void visit(Code obj)  {
	if (inConstructor) return;
        thisOnTopOfStack = false;
	thisLocked = false;
	super.visit(obj);
	// System.out.println("End of method, state = " + state);
	if (state == 1) {
		updateStats(fieldsWritten, WRITTEN_LOCKED);
		updateStats(fieldsRead, READ_LOCKED);
		}
	else if (obj.getCode().length > 6) {
		updateStats(fieldsWritten, WRITTEN_UNLOCKED);
		updateStats(fieldsRead, READ_UNLOCKED);
		}
	}
    public void sawOpcode(int seen) {
	// state: 0 - unlocked
	// state: 1 - locked
	// state: 2 - saw unlocked, but might still be locked

	switch (seen) {
	case ASTORE_1:
	case ASTORE_2:
	case ASTORE_3:
	case ASTORE:
		thisOnTopOfStack = doubleThisOnTopOfStack;
		return;
	case ALOAD_0:
		thisOnTopOfStack = true;
		return;
	case DUP:
		doubleThisOnTopOfStack = thisOnTopOfStack;
		return;
	case MONITOREXIT:
		if (thisLockingOnly && !thisLocked) break;
		updateStats(fieldsWritten, WRITTEN_LOCKED);
		updateStats(fieldsRead, READ_LOCKED);
		state = 2;
		// System.out.println("monitorexit	" + thisLocked);
		fieldsWritten.clear();
		fieldsRead.clear();
		break;
	case MONITORENTER:
		thisLocked = thisOnTopOfStack;
		if (thisLockingOnly && !thisLocked) break;
		updateStats(fieldsWritten, WRITTEN_UNLOCKED);
		updateStats(fieldsRead, READ_UNLOCKED);
		// System.out.println("monitorenter	" + thisLocked);
		state = 1;
		fieldsWritten.clear();
		fieldsRead.clear();
		break;
	case PUTFIELD:
		{
		 FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		 writtenOutsideOfConstructor.add(f);
		 if (!className.equals(classConstant)) break;
		 // System.out.println("putfield	" + f + ", state = " + state);
		 fieldsWritten.add(f);
		}
		break;
	case GETFIELD:
		int next = codeBytes[PC+3] & 0xff;
		if (!thisOnTopOfStack) break;
		if (next != IFNULL &&  next != IFNONNULL) {
		   FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
		   // System.out.println("getfield	" + f);
		   fieldsRead.add(f);
		   /*
		   System.out.println("After read of " 
			   + classConstant + "." + nameConstant
			   + ", next PC is " + (PC+3)
			   );
		   System.out.println("After read of " 
			   + classConstant + "." + nameConstant
			   + ", next opcode is " + OPCODE_NAMES[next]
			   + " (" + next + ")"
			   );
		   */
		   }
		// OPCODE_NAMES
		break;
	}
	thisOnTopOfStack = false;
	doubleThisOnTopOfStack = false;
	}

public void report() {

	int noLocked = 0;
	int noUnlocked = 0;
	int isPublic = 0;
	int couldBeFinal = 0;
	int noLocalLocks = 0;
	int volatileOrFinalCount = 0;
	int mostlyUnlocked = 0;

	for(Iterator<Map.Entry<FieldAnnotation, int[]>> i = stats.entrySet().iterator(); i.hasNext(); ) {
		Map.Entry<FieldAnnotation, int[]> e = i.next();
		FieldAnnotation f = e.getKey();
		int [] theseStats  =  e.getValue();
		int locked =  theseStats[READ_LOCKED] + theseStats[WRITTEN_LOCKED];
		int biasedLocked =  theseStats[READ_LOCKED] + 2*theseStats[WRITTEN_LOCKED];
		int unlocked =  theseStats[READ_UNLOCKED] + theseStats[WRITTEN_UNLOCKED];
		int biasedUnlocked =  theseStats[READ_UNLOCKED] + 2*theseStats[WRITTEN_UNLOCKED];
		int writes =  theseStats[WRITTEN_LOCKED] + theseStats[WRITTEN_UNLOCKED];
		if (locked == 0) {
			noLocked++;
			continue;
			}
		if (unlocked == 0)  {
			noUnlocked++;
			continue;
			}
		if (theseStats[READ_UNLOCKED] > 0 && 2*biasedUnlocked > biasedLocked) {
			if (DEBUG) System.out.println("Mostly unlocked for " + f + ":");
		int freq = (100 * locked) / (locked + unlocked);
		if (DEBUG) {
			System.out.print(freq
				+ "	");
		for(int j = 0; j < 4; j++)
			System.out.print(theseStats[j] + "	");
		System.out.println(f);
			}
			mostlyUnlocked++;
			continue;
			}
		if (publicFields.contains(f)) {
			isPublic++;
			continue;
			}
		if (volatileOrFinalFields.contains(f)) {
			volatileOrFinalCount++;
			continue;
			}
		if (!writtenOutsideOfConstructor.contains(f)) {
			couldBeFinal++;
			continue;
			}
		if (!localLocks.contains(f)) {
			if (DEBUG) System.out.println("No local locks of " + f);
			noLocalLocks++;
			continue;
			}
		int freq = (100 * locked) / (locked + unlocked);
		bugReporter.reportBug(new BugInstance("IS_INCONSISTENT_SYNC", NORMAL_PRIORITY)
			.addClass(f.getClassName())
			.addField(f)
			.addInt(freq).describe("INT_SYNC_PERCENT"));
		if (DEBUG) {
			System.out.print(freq
				+ "	");
		for(int j = 0; j < 4; j++)
			System.out.print(theseStats[j] + "	");
		System.out.println(f);
			}
		}
		if (DEBUG) {
		int total = stats.size();
		System.out.println("        Total fields: " + total);
		System.out.println("  No locked accesses: " + noLocked);
		System.out.println("No unlocked accesses: " + noUnlocked);
		System.out.println("     Mostly unlocked: " + mostlyUnlocked);
		System.out.println("       public fields: " + isPublic);
		if (couldBeFinal > 0) 
			System.out.println("      could be Final: " + couldBeFinal);
		System.out.println("   volatile or final: " + volatileOrFinalCount);
		System.out.println("      no local locks: " + noLocalLocks);
		System.out.println(" questionable fields: " + (total - noLocked - noUnlocked - isPublic - volatileOrFinalCount - couldBeFinal - noLocalLocks - mostlyUnlocked));
		}
	}
	

	}	
