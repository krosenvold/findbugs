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

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

/**
 * @author pugh
 */
public class CheckReturnAnnotationDatabase extends AnnotationDatabase<CheckReturnValueAnnotation> {
	
	private JavaClass throwableClass, threadClass;
	CheckReturnAnnotationDatabase() {
		addMethodAnnotation("java.util.Iterator","hasNext", "()Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
		addMethodAnnotation("java.io.File","createNewFile", "()Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
		addMethodAnnotation("java.util.Enumeration","hasMoreElements", "()Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
		addMethodAnnotation("java.security.MessageDigest","digest", "([B)[B", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
		
		addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock","readLock",   "()Ljava/util/concurrent/locks/Lock;", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock","writeLock",  "()Ljava/util/concurrent/locks/Lock;", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.util.concurrent.locks.Condition",    "await",      "(JLjava/util/concurrent/TimeUnit;)Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
		addMethodAnnotation("java.util.concurrent.locks.Condition",    "awaitUtil",  "(Ljava/util/Date;)Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
		addMethodAnnotation("java.util.concurrent.locks.Condition",    "awaitNanos", "(J)Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
		addMethodAnnotation("java.util.concurrent.Semaphore",          "tryAcquire", "(JLjava/util/concurrent/TimeUnit;)Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.util.concurrent.Semaphore",          "tryAcquire", "()Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.util.concurrent.locks.Lock",         "tryLock",    "(JLjava/util/concurrent/TimeUnit;)Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.util.concurrent.locks.Lock",         "newCondition","()Ljava/util/concurrent/locks/Condition;", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.util.concurrent.locks.Lock",         "tryLock",     "()Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.util.Queue",                         "offer",       "(Ljava/lang/Object;)Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
		addMethodAnnotation("java.util.concurrent.BlockingQueue",      "offer",       "(Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
		addMethodAnnotation("java.util.concurrent.BlockingQueue",      "poll",        "(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
		addMethodAnnotation("java.util.Queue",                         "poll",        "()Ljava/lang/Object;", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
		
		
		addDefaultMethodAnnotation("java.lang.String", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addDefaultMethodAnnotation("java.math.BigDecimal", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.math.BigDecimal", "inflate", "()V", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
		addDefaultMethodAnnotation("java.math.BigInteger", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH);
		addMethodAnnotation("java.math.BigInteger", "addOne", "([IIII)I", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
		addMethodAnnotation("java.math.BigInteger", "subN", "([I[II)I", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE);
		addDefaultMethodAnnotation("java.sql.Connection", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
		addDefaultMethodAnnotation("java.net.InetAddress", CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM);
		try {
			throwableClass = Repository.lookupClass("java.lang.Throwable");
		} catch (ClassNotFoundException e) {
			// ignore it
		}
		try {
			threadClass = Repository.lookupClass("java.lang.Thread");
		} catch (ClassNotFoundException e) {
			// ignore it
		}
	}
	
	 @Override
	public CheckReturnValueAnnotation getResolvedAnnotation(Object o, boolean getMinimal) {
		if (!(o instanceof XMethod))
			return null;
		XMethod m = (XMethod) o;
		if (m.getName().equals("<init>")) {
			try {
				if (Repository.instanceOf(m.getClassName(), throwableClass))
					return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_HIGH;
			} catch (ClassNotFoundException e) {
				// ignore it
			}
			try {
				if (Repository.instanceOf(m.getClassName(), threadClass))
					return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW;
			} catch (ClassNotFoundException e) {
				// ignore it
			}
		} else if (m.getName().equals("equals") && m.getSignature().equals("(Ljava/lang/Object;)Z")
				&& !m.isStatic())
			return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM;
		else if (m.getSignature().endsWith(")Ljava/lang/String;")
				&& (m.getClassName().equals("java.lang.StringBuffer") || m.getClassName().equals(
						"java.lang.StringBuilder")))
			return CheckReturnValueAnnotation.CHECK_RETURN_VALUE_MEDIUM;
		return super.getResolvedAnnotation(o, getMinimal);
	}

}
