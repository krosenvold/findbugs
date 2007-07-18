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

import java.util.Collection;

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierResolver;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class TestingGround  implements Detector  {

	BugReporter bugReporter;

	public TestingGround(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void visitClassContext(ClassContext classContext) {
		try {
	        ClassInfo xclass = (ClassInfo) classContext.getXClass();
	        Collection<ClassDescriptor> annotations = xclass.getAnnotationDescriptors();
	        for(ClassDescriptor c : annotations) {
	        	System.out.println(xclass.getDottedClassName()  + " : " + c + " -> " + xclass.getAnnotation(c));
	        }
	        for(MethodDescriptor m : xclass.getMethodDescriptorList()) {
	        	System.out.println(m);
	        	if (m instanceof MethodInfo) {
	        		System.out.println("Method info: " + TypeQualifierApplications.getAnnotation((MethodInfo)m));
	        		
	        		Collection<ClassDescriptor> mAnnotations = ((MethodInfo)m).getAnnotationDescriptors();
			        for(ClassDescriptor c : mAnnotations) {
			        	System.out.println(m.getName()  + " : " + c + " -> " + ((MethodInfo)m).getAnnotation(c));
			        }
	        	for(int i = 0; i < 5; i++) {
	        		Collection<ClassDescriptor> pAnnotations = ((MethodInfo)m).getParameterAnnotationDescriptors(i);
			        if (pAnnotations != null) {
			        	Collection<AnnotationValue> annotation = TypeQualifierApplications.getAnnotation((MethodInfo)m, i);
						System.out.println("#" + i + " : " + annotation);
			        	for(ClassDescriptor c : pAnnotations) {
			        
			        	System.out.println(m.getName()  + "(" + i + ")  : " + c + " -> " + ((MethodInfo)m).getParameterAnnotation(i, c));
			        }}
	        	}
	        	
	        	}
	        }
	   
	        
        } catch (CheckedAnalysisException e) {
	        AnalysisContext.logError("Error getting xclass for " + classContext.getClass(), e);
        }
	}

    public void report() {
	    // TODO Auto-generated method stub
	    
    }

}
