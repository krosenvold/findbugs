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
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;
import edu.umd.cs.findbugs.ba.ClassContext;

public class Naming extends PreorderVisitor implements Detector, Constants2 {
  String baseClassName;
  Map<String, String> superClass = new HashMap<String, String>();

  static class MyMethod {
	final JavaClass clazz;
	final String methodName;
	final String methodSig;
	MyMethod(JavaClass c, String n, String s) {
		clazz = c;
		methodName = n;
		methodSig = s;
		}
	public String getClassName() {
		return clazz.getClassName();
		}
	public boolean equals(Object o) {
		if (!(o instanceof MyMethod)) return false;
		MyMethod m2 = (MyMethod) o;
		return 
			clazz.equals(m2.clazz)
			&& methodName.equals(m2.methodName)
			&& methodSig.equals(m2.methodSig);
			}

	public int hashCode() {
		return clazz.hashCode()
			+methodName.hashCode()
			+methodSig.hashCode();
		}
	public boolean confusingMethodNames(MyMethod m) {
		return methodName.equalsIgnoreCase(m.methodName)
			& !methodName.equals(m.methodName);
		}
	public String toString() {
		return getClassName() 
			+ "." + methodName
				+ ":" + methodSig;
		}
	}

 
  // map of canonicalName -> trueMethodName 
  HashMap<String, HashSet<String>> canonicalToTrueMapping
	= new HashMap<String, HashSet<String>>();
  // map of canonicalName -> Set<MyMethod>
  HashMap<String, HashSet<MyMethod>> canonicalToMyMethod
	= new HashMap<String, HashSet<MyMethod>>();

  HashSet<String> reported = new HashSet<String>();
  HashSet<String> visited = new HashSet<String>();

  private BugReporter bugReporter;

  public Naming(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

  public void visitClassContext(ClassContext classContext) {
	classContext.getJavaClass().accept(this);
	}

  private boolean checkSuper(MyMethod m, HashSet<MyMethod> others) {
	for(Iterator<MyMethod> i = others.iterator(); i.hasNext() ; ) {
		MyMethod m2 =  i.next();
		try {
		if (m.confusingMethodNames(m2)
				&& Repository.instanceOf(m.clazz, m2.clazz)) {
		  MyMethod m3 = new MyMethod(
					m.clazz, m2.methodName, m.methodSig);
		  boolean r = others.contains(m3);
		  if (r) continue;
		  bugReporter.reportBug(new BugInstance("NM_VERY_CONFUSING", HIGH_PRIORITY)
			.addClass(m.getClassName())
			.addMethod(m.getClassName(), m.methodName, m.methodSig)
			.addClass(m2.getClassName())
			.addMethod(m2.getClassName(), m2.methodName, m2.methodSig));
			return true;
		}
		} catch (ClassNotFoundException e) {}
	}
	return false;
	}

  private boolean checkNonSuper(MyMethod m, HashSet<MyMethod> others) {
	for(Iterator<MyMethod> i = others.iterator(); i.hasNext() ; ) {
		MyMethod m2 =  i.next();
		if (m.confusingMethodNames(m2)) {
		  bugReporter.reportBug(new BugInstance("NM_CONFUSING", LOW_PRIORITY)
			.addClass(m.getClassName())
			.addMethod(m.getClassName(), m.methodName, m.methodSig)
			.addClass(m2.getClassName())
			.addMethod(m2.getClassName(), m2.methodName, m2.methodSig));
			return true;
		}
	}
	return false;
	}





  public void report() {
	
	canonicalNameIterator: for(Iterator<Map.Entry<String, HashSet<String>>> i 
		= canonicalToTrueMapping.entrySet().iterator();
	    i.hasNext(); ) {
	   Map.Entry<String, HashSet<String>> e = i.next();
	   HashSet<String> s = e.getValue();
	   if (s.size() <= 1) continue;
	   String allSmall = e.getKey();
	   HashSet<MyMethod> conflictingMethods = canonicalToMyMethod.get(allSmall);
	   for(Iterator<MyMethod> j = conflictingMethods.iterator(); j.hasNext(); ) {
		if (checkSuper(j.next(), conflictingMethods)) 
			j.remove();
		}
	   for(Iterator<MyMethod> j = conflictingMethods.iterator(); j.hasNext(); ) {
		if (checkNonSuper(j.next(), conflictingMethods)) 
			continue canonicalNameIterator;
		}
	}
 }

  public void visitJavaClass(JavaClass obj)     {
	if (obj.isInterface()) return;
	String name = obj.getClassName();
	if (!visited.add(name)) return;
	try {
	JavaClass supers[] = Repository.getSuperClasses(obj);
		for(int i = 0; i < supers.length; i++) {
			JavaClass sup = supers[i];
			visitJavaClass(supers[i]);
			}
	} catch (ClassNotFoundException e) {
		// ignore it
		}
	super.visitJavaClass(obj);
		}
  public void visit(JavaClass obj)     {
	String name = obj.getClassName();
	String[] parts = name.split("[$+.]");
        baseClassName = parts[parts.length-1];
	super.visit(obj);
	}

    public void visit(Method obj) {
	if (obj.isAbstract()) return;
	if (methodName.length() == 1) return;

	if (methodName.equals(baseClassName)) 
		bugReporter.reportBug(new BugInstance("NM_CONFUSING_METHOD_NAME", NORMAL_PRIORITY)
			.addClassAndMethod(this));

	if (obj.isPrivate()
			|| obj.isStatic()
			) return;

	String trueName = methodName + methodSig;
	String allSmall = methodName.toLowerCase() + methodSig;
	
	MyMethod mm = new MyMethod( thisClass, methodName, methodSig);
	{
	HashSet<String> s = canonicalToTrueMapping.get(allSmall);
	if (s == null) {
		s = new HashSet<String>();
		canonicalToTrueMapping.put(allSmall,s);
		}
	s.add(trueName);
	}
	{
	HashSet<MyMethod> s = canonicalToMyMethod.get(allSmall);
	if (s == null) {
		s = new HashSet<MyMethod>();
		canonicalToMyMethod.put(allSmall,s);
		}
	s.add(mm);
	}

	if (methodName.equals("equal") && methodSig.equals("(Ljava/lang/Object;)Z"))  
		System.out.println("Found bad definition of equals: " 
			+ betterMethodName);
	if (methodName.equals("hashcode") && methodSig.equals("()I")) 
		bugReporter.reportBug(new BugInstance("NM_LCASE_HASHCODE", HIGH_PRIORITY)
			.addClassAndMethod(this));
	if (methodName.equals("tostring") && methodSig.equals("()Ljava/lang/String;")) 
		bugReporter.reportBug(new BugInstance("NM_LCASE_TOSTRING", HIGH_PRIORITY)
			.addClassAndMethod(this));
	}



}
