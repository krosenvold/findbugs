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
import edu.umd.cs.findbugs.*;
import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import edu.umd.cs.findbugs.visitclass.Constants2;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.ba.ClassContext;

public class CloneIdiom extends DismantleBytecode implements Detector, Constants2 {

  boolean /*isCloneable,*/ hasCloneMethod;
  MethodAnnotation cloneMethodAnnotation;
  boolean referencesCloneMethod;
  boolean invokesSuperClone;
  boolean isFinal;
 
  boolean check;
  //boolean throwsExceptions;
  boolean implementsCloneableDirectly;
  private BugReporter bugReporter;

  public CloneIdiom(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

  public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
	}


  public void report() {
	}

    public void visit(Code obj)  {
	if (getMethodName().equals("clone") &&
		getMethodSig().startsWith("()"))
		super.visit(obj);
	}

 public void sawOpcode(int seen) {
	if (seen == INVOKESPECIAL
		&& getNameConstantOperand().equals("clone")
		&& getSigConstantOperand().startsWith("()")) {
		/*
		System.out.println("Saw call to " + nameConstant
					+ ":" + sigConstant 
					+ " in " + betterMethodName);
		*/
		  invokesSuperClone = true;
		}
	}

  public void visit(JavaClass obj)     {
	implementsCloneableDirectly = false;
        invokesSuperClone = false;
	//isCloneable = false;
	check = false;
	isFinal = obj.isFinal();
	if (obj.isInterface()) return;
	if (obj.isAbstract()) return;
        // Does this class directly implement Cloneable?
        String [] interface_names = obj.getInterfaceNames();
        for(int i=0; i < interface_names.length; i++) {
            if (interface_names[i].equals("java.lang.Cloneable")) {
                implementsCloneableDirectly = true;
                //isCloneable = true;
                break;
                }
            }

	try {
	//isCloneable = Repository.implementationOf(obj, "java.lang.Cloneable");
	JavaClass superClass = obj.getSuperClass();
	if (superClass != null && Repository.implementationOf(superClass, "java.lang.Cloneable"))
		implementsCloneableDirectly = false;
	} catch (ClassNotFoundException e) {
		// ignore
		}
	hasCloneMethod = false;
	referencesCloneMethod = false;
	check = true;
	super.visit(obj);
	}
  public void visitAfter(JavaClass obj)     {
	if (!check) return;
	if (implementsCloneableDirectly && !hasCloneMethod) {
		if (!referencesCloneMethod)
                        bugReporter.reportBug(
				new BugInstance("CN_IDIOM", NORMAL_PRIORITY)
                                .addClass(this));
		}

	if (hasCloneMethod && !invokesSuperClone && !isFinal) {
		bugReporter.reportBug(new BugInstance("CN_IDIOM_NO_SUPER_CALL", NORMAL_PRIORITY)
			.addClass(this)
			.addMethod(cloneMethodAnnotation)
			);
	}

	/*
	if (!isCloneable && hasCloneMethod) {
		if (throwsExceptions) 
		System.out.println("has public clone method that throws exceptions and class is not Cloneable: " + betterClassName) ;
		else System.out.println("has public clone method but is not Cloneable: " + betterClassName) ;
		}
	*/
	}

     public void visit(ConstantNameAndType obj) { 
		String methodName = obj.getName(getConstantPool());
		String methodSig = obj.getSignature(getConstantPool());
		if (!methodName.equals("clone")) return;
		if (!methodSig.startsWith("()")) return;
		referencesCloneMethod = true;
		}

    public void visit(Method obj) {
	if (obj.isAbstract()) return;
	if (!obj.isPublic()) return;
	if (!getMethodName().equals("clone")) return;
	if (!getMethodSig().startsWith("()")) return;
	hasCloneMethod = true;
	cloneMethodAnnotation = MethodAnnotation.fromVisitedMethod(this);
	ExceptionTable tbl = obj.getExceptionTable();
	//throwsExceptions = tbl != null && tbl.getNumberOfExceptions() > 0;
	}
}
