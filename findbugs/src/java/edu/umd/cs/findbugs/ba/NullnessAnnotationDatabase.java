/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author pugh
 */
public class NullnessAnnotationDatabase extends AnnotationDatabase<NullnessAnnotation> {
	
	public NullnessAnnotationDatabase() {
		addDefaultAnnotation(AnnotationDatabase.METHOD, "java.lang.String", NullnessAnnotation.NONNULL);
		
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.package-info", NullnessAnnotation.NONNULL);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.CopyOnWriteArrayList", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.CopyOnWriteArraySet", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ConcurrentLinkedQueue$Node", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.Exchanger", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.FutureTask", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.LinkedBlockingQueue$Node", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.SynchronousQueue$WaitQueue", NullnessAnnotation.UNKNOWN_NULLNESS);
		addDefaultAnnotation(AnnotationDatabase.PARAMETER, "java.util.concurrent.ThreadPoolExecutor$Worker", NullnessAnnotation.UNKNOWN_NULLNESS);
		
		addMethodParameterAnnotation("java.util.concurrent.ConcurrentHashMap", "remove", "(Ljava/lang/Object;ILjava/lang/Object;)Ljava/lang/Object;", false, 2, NullnessAnnotation.CHECK_FOR_NULL);
		addMethodParameterAnnotation("java.util.concurrent.CyclicBarrier", "<init>", "(ILjava/lang/Runnable;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		addMethodParameterAnnotation("java.util.concurrent.Executors$RunnableAdapter", "<init>", "(Ljava/lang/Runnable;Ljava/lang/Object;)V", false, 1, NullnessAnnotation.CHECK_FOR_NULL);
		
		addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "addIfUnderCorePoolSize", "(Ljava/lang/Runnable;)Z", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
		addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "addThread", "(Ljava/lang/Runnable;)Ljava/lang/Thread;", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
		addMethodParameterAnnotation("java.util.concurrent.ThreadPoolExecutor", "afterExecute", "(Ljava/lang/Runnable;Ljava/lang/Throwable;)V", false, 0, NullnessAnnotation.CHECK_FOR_NULL);
		
		
	
		addMethodParameterAnnotation("java.util.EnumMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		addMethodParameterAnnotation("java.util.EnumMap", "containsKey", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		addMethodParameterAnnotation("java.util.EnumMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		addMethodParameterAnnotation("java.util.EnumMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		
		addMethodParameterAnnotation("java.util.SortedMap", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		addMethodParameterAnnotation("java.util.SortedMap", "containsKey", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		addMethodParameterAnnotation("java.util.SortedMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		addMethodParameterAnnotation("java.util.SortedMap", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", false, 0, NullnessAnnotation.NONNULL);
		
		addMethodParameterAnnotation("java.util.SortedSet", "add", "(Ljava/lang/Object;)Z", false, 0, NullnessAnnotation.NONNULL);
		addMethodParameterAnnotation("java.util.SortedSet", "remove", "(Ljava/lang/Object;)Z", false, 0, NullnessAnnotation.NONNULL);
		addMethodParameterAnnotation("java.util.SortedSet", "cotains", "(Ljava/lang/Object;)Z", false, 0, NullnessAnnotation.NONNULL);
	
		// addMethodAnnotation("java.util.Queue", "poll", "()Ljava/lang/Object;", false, NullnessAnnotation.CHECK_FOR_NULL);
		addMethodAnnotation("java.io.BufferedReader", "readLine", "()Ljava/lang/String;", false, NullnessAnnotation.CHECK_FOR_NULL);
	}
	public boolean parameterMustBeNonNull(XMethod m, int param) {
		if (!anyAnnotations(NullnessAnnotation.NONNULL)) return false;
		XMethodParameter xmp = new XMethodParameter(m,param);
		NullnessAnnotation resolvedAnnotation = getResolvedAnnotation(xmp, true);
		// System.out.println("QQQ parameter " + param + " of " + m + " is " + resolvedAnnotation);
		return resolvedAnnotation == NullnessAnnotation.NONNULL;
	}
	
	@CheckForNull @Override
	public NullnessAnnotation getResolvedAnnotation(final Object o, boolean getMinimal) {
		
		if (o instanceof XMethodParameter) {
			XMethodParameter mp = (XMethodParameter) o;
			XMethod m = mp.getMethod();
			if (m.getName().startsWith("access$")) return null;
			if (mp.getParameterNumber() == 0 && m.getName().equals("equals") 
					&& m.getSignature().equals("(Ljava/lang/Object;)Z") && !m.isStatic())
					return NullnessAnnotation.CHECK_FOR_NULL;
			else if (mp.getParameterNumber() == 0 && m.getName().equals("compareTo") 
					&& m.getSignature().endsWith(";)Z") && !m.isStatic())
					return NullnessAnnotation.NONNULL;
		}
		else if (o instanceof XMethod) {
			XMethod m = (XMethod) o;
			if (m.getName().startsWith("access$")) return null;

		}
		return super.getResolvedAnnotation(o, getMinimal);
	}
}
