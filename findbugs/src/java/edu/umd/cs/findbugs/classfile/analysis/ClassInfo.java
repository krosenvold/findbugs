/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile.analysis;

import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.Util;

/**
 * ClassInfo represents important metadata about a loaded class, such as its
 * superclass, access flags, codebase entry, etc.
 * 
 * @author David Hovemeyer
 */
public class ClassInfo extends ClassNameAndSuperclassInfo implements XClass, AnnotatedObject {
	private final FieldInfo[] xFields;

	private final MethodInfo[] xMethods;

	private final ClassDescriptor immediateEnclosingClass;

	/*final*/ Map<ClassDescriptor, AnnotationValue> classAnnotations;
	final private String classSourceSignature;
	final private String source;


	public static class Builder extends ClassNameAndSuperclassInfo.Builder {
		private List<FieldInfo>fieldDescriptorList = new LinkedList<FieldInfo>();

		private List<MethodInfo> methodDescriptorList  = new LinkedList<MethodInfo>();


		private ClassDescriptor immediateEnclosingClass;
		final Map<ClassDescriptor, AnnotationValue> classAnnotations = new HashMap<ClassDescriptor, AnnotationValue>(3);
		private String classSourceSignature;
		private String source;

		@Override
        public ClassInfo build() {
			FieldInfo fields [];
			MethodInfo methods[];
			
			if (fieldDescriptorList.size() == 0)
				fields = FieldInfo.EMPTY_ARRAY;
			else fields = fieldDescriptorList.toArray(new FieldInfo[fieldDescriptorList.size()]);
			if (methodDescriptorList.size() == 0)
				methods = MethodInfo.EMPTY_ARRAY;
			else methods = methodDescriptorList.toArray(new MethodInfo[methodDescriptorList.size()]);
			
			return new ClassInfo(classDescriptor,classSourceSignature, superclassDescriptor, interfaceDescriptorList, codeBaseEntry, accessFlags, source, majorVersion, minorVersion, 
					referencedClassDescriptorList,calledClassDescriptorList,
					classAnnotations, fields, 
					methods, immediateEnclosingClass );
		}

		
		public void setSource(String source) {
			this.source = source;
		}
		/**
		 * @return Returns the classDescriptor.
		 */
		public ClassDescriptor getClassDescriptor() {
			return classDescriptor;
		}

		public void setSourceSignature(String classSourceSignature) {
			this.classSourceSignature = classSourceSignature;
		}
		public void addAnnotation(String name, AnnotationValue value) {
			ClassDescriptor annotationClass = DescriptorFactory.createClassDescriptorFromSignature(name);
			classAnnotations.put(annotationClass, value);
		}
		/**
		 * @param fieldDescriptorList
		 *            The fieldDescriptorList to set.
		 */
		public void setFieldDescriptorList(FieldInfo [] fieldDescriptorList) {
			this.fieldDescriptorList = Arrays.asList(fieldDescriptorList);
		}
		public void addFieldDescriptor(FieldInfo field) {
			fieldDescriptorList.add(field);
		}

		/**
		 * @param methodDescriptorList
		 *            The methodDescriptorList to set.
		 */
		public void setMethodDescriptorList(MethodInfo[] methodDescriptorList) {
			this.methodDescriptorList = Arrays.asList(methodDescriptorList);
		}
		public void addMethodDescriptor(MethodInfo method) {
			methodDescriptorList.add(method);
		}

		/**
		 * @param immediateEnclosingClass
		 *            The immediateEnclosingClass to set.
		 */
		public void setImmediateEnclosingClass(ClassDescriptor immediateEnclosingClass) {
			this.immediateEnclosingClass = immediateEnclosingClass;
		}

	}

	/**
	 * 
	 * @param classDescriptor
	 *            ClassDescriptor representing the class name
	 * @param superclassDescriptor
	 *            ClassDescriptor representing the superclass name
	 * @param interfaceDescriptorList
	 *            ClassDescriptors representing implemented interface names
	 * @param codeBaseEntry
	 *            codebase entry class was loaded from
	 * @param accessFlags
	 *            class's access flags
	 * @param referencedClassDescriptorList
	 *            ClassDescriptors of all classes/interfaces referenced by the
	 *            class
	 * @param calledClassDescriptors TODO
	 * @param fieldDescriptorList
	 *            FieldDescriptors of fields defined in the class
	 * @param methodDescriptorList
	 *            MethodDescriptors of methods defined in the class
	 */
	private ClassInfo(ClassDescriptor classDescriptor, String classSourceSignature, ClassDescriptor superclassDescriptor,
			ClassDescriptor[] interfaceDescriptorList, ICodeBaseEntry codeBaseEntry, int accessFlags, String source, int majorVersion,  int minorVersion,
			Collection<ClassDescriptor> referencedClassDescriptorList,
			Collection<ClassDescriptor> calledClassDescriptors,
			Map<ClassDescriptor, AnnotationValue> classAnnotations, FieldInfo[] fieldDescriptorList,
			MethodInfo[] methodDescriptorList, ClassDescriptor immediateEnclosingClass) {
		super(classDescriptor, superclassDescriptor, interfaceDescriptorList, codeBaseEntry, accessFlags, referencedClassDescriptorList, calledClassDescriptors,   majorVersion,  minorVersion);
		this.source = source;
		this.classSourceSignature = classSourceSignature;
		if (fieldDescriptorList.length == 0) 
			fieldDescriptorList = FieldInfo.EMPTY_ARRAY;
		this.xFields = fieldDescriptorList;
		this.xMethods = methodDescriptorList;
		this.immediateEnclosingClass = immediateEnclosingClass;
		this.classAnnotations = Util.immutableMap(classAnnotations);
	}

	/**
	 * @return Returns the fieldDescriptorList.
	 */
	public List<? extends XField> getXFields() {
		return Arrays.asList(xFields);
	}

	/**
	 * @return Returns the methodDescriptorList.
	 */
	public List<? extends XMethod> getXMethods() {
		return Arrays.asList(xMethods);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XClass#findMethod(java.lang.String, java.lang.String, boolean)
	 */
	public XMethod findMethod(String methodName, String methodSig, boolean isStatic) {
		for (MethodInfo mInfo : xMethods) 
				if (mInfo.getName().equals(methodName)
						&& mInfo.getSignature().equals(methodSig)
						&& mInfo.isStatic() == isStatic) 
					return mInfo;
		if (true) return null;
		try {
			if (getSuperclassDescriptor() == null) return null;
			XClass superClass = Global.getAnalysisCache().getClassAnalysis(XClass.class,  getSuperclassDescriptor());
			return superClass.findMethod(methodName, methodSig, isStatic);
        } catch (CheckedAnalysisException e) {
        	return null;
        }
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XClass#findMethod(edu.umd.cs.findbugs.classfile.MethodDescriptor)
	 */
	public XMethod findMethod(MethodDescriptor descriptor) {
		if (!descriptor.getClassDescriptor().equals(this)) {
			throw new IllegalArgumentException();
		}
		return findMatchingMethod(descriptor);
	}

	
	public XMethod findMatchingMethod(MethodDescriptor descriptor) {
		return findMethod(descriptor.getName(), descriptor.getSignature(), descriptor.isStatic());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.XClass#findField(java.lang.String, java.lang.String, boolean)
	 */
	public XField findField(String name, String signature, boolean isStatic) {
		for (FieldInfo fInfo : xFields) 
				if (fInfo.getName().equals(name)
						&& fInfo.getSignature().equals(signature)
						&& fInfo.isStatic() == isStatic) 
					return fInfo;
		try {
			if (getSuperclassDescriptor() == null) return null;
			XClass superClass = Global.getAnalysisCache().getClassAnalysis(XClass.class,  getSuperclassDescriptor());
			XField result = superClass.findField(name, signature, isStatic);
			if (result != null) return result;
			if (!isStatic) return null;
			ClassDescriptor[] interfaces = getInterfaceDescriptorList();
			for(ClassDescriptor implementedInterface : interfaces) {
				superClass = Global.getAnalysisCache().getClassAnalysis(XClass.class,  implementedInterface);
				result = superClass.findField(name, signature, isStatic);
				if (result != null) return result;
			}
			return null;
        } catch (CheckedAnalysisException e) {
        	return null;
        }
	}

	/**
	 * @return Returns the immediateEnclosingClass.
	 */
	public ClassDescriptor getImmediateEnclosingClass() {
		return immediateEnclosingClass;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#getPackageName()
	 */
	@Override
    public String getPackageName() {
		String dottedClassName = getClassDescriptor().toDottedClassName();
		int lastDot = dottedClassName.lastIndexOf('.');
		if (lastDot < 0) {
			return "";
		} else {
			return dottedClassName.substring(0, lastDot);
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.ba.AccessibleEntity#getPackageName()
	 */
	
    public String getSlashedPackageName() {
		String slashedClassName = getClassDescriptor().getClassName();
		int lastSlash = slashedClassName.lastIndexOf('/');
		if (lastSlash < 0) {
			return "";
		} else {
			return slashedClassName.substring(0, lastSlash);
		}
	}
	public Collection<ClassDescriptor> getAnnotationDescriptors() {
		return classAnnotations.keySet();
	}
	public Collection<AnnotationValue> getAnnotations() {
		return classAnnotations.values();
	}
	public AnnotationValue getAnnotation(ClassDescriptor desc) {
		return classAnnotations.get(desc);
	}
	
	/**
	 * Destructively add an annotation to the object.
	 * In general, this is not a great idea, since it could cause
	 * the same class to appear to have different annotations
	 * at different times.  However, this method is necessary
	 * for "built-in" annotations that FindBugs adds to
	 * system classes.  As long as we add such annotations early
	 * enough that nobody will notice, we should be ok. 
	 * 
	 * @param annotationValue an AnnotationValue to add to the class
	 */
	public void addAnnotation(AnnotationValue annotationValue) {
		HashMap<ClassDescriptor, AnnotationValue> updatedMap = new HashMap<ClassDescriptor, AnnotationValue>(classAnnotations);
		updatedMap.put(annotationValue.getAnnotationClass(), annotationValue);
		classAnnotations = Util.immutableMap(updatedMap);
	}

	public ElementType getElementType() {
		if (getClassName().endsWith("package-info")) return ElementType.PACKAGE;
		else if (isAnnotation()) return ElementType.ANNOTATION_TYPE;
		return ElementType.TYPE;
		
	}
	
	public @CheckForNull String getSource() {
		return source;
	}
	
	static final private AnnotatedObject NOT_CACHED = new AnnotatedObject() {

		public AnnotationValue getAnnotation(ClassDescriptor desc) {
	        throw new UnsupportedOperationException();
        }

		public Collection<ClassDescriptor> getAnnotationDescriptors() {
			throw new UnsupportedOperationException();
        }

		public Collection<AnnotationValue> getAnnotations() {
			throw new UnsupportedOperationException();
        }

		public ClassDescriptor getClassDescriptor() {
			throw new UnsupportedOperationException();
        }

		public AnnotatedObject getContainingScope() {
			throw new UnsupportedOperationException();
        }

		public ElementType getElementType() {
			throw new UnsupportedOperationException();
        }};
	@CheckForNull AnnotatedObject containingScope = NOT_CACHED;
	public @CheckForNull AnnotatedObject getContainingScope() {
		if (containingScope == NOT_CACHED) {
			containingScope = getContainingScope0();
		}
		return containingScope;
	}
	public @CheckForNull AnnotatedObject getContainingScope0() {
		try {
			if (immediateEnclosingClass != null) {
				return Global.getAnalysisCache().getClassAnalysis(XClass.class, getImmediateEnclosingClass());
			}
			if (getClassName().endsWith("package-info") ) {
				return null;
			}
			ClassDescriptor p = DescriptorFactory.createClassDescriptor(getSlashedPackageName() +"/"+"package-info");
			return Global.getAnalysisCache().getClassAnalysis(XClass.class, p);
		} catch (CheckedAnalysisException e) {
			return null;
		}
	}

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.ba.XClass#getSourceSignature()
     */
    public String getSourceSignature() {
	    return classSourceSignature;
    }

}
