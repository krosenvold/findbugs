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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.bcel.classfile.Method;


/**
 * Compute a hash of the bytecode for given method. 
 * This can find methods which have not been changed other
 * than accessing different constant pool entries.
 * 
 * @author David Hovemeyer
 */
public class MethodHash implements Comparable<MethodHash> {
	public static final String METHOD_HASH_ELEMENT_NAME = "MethodHash";
	
	private byte[] hash;
	private String methodName;
	private String methodSig;
	private boolean isStatic;

	/**
	 * Constructor.
	 * computeHash(Method) must be used to initialize the contents.
	 */
	public MethodHash() {
	}
	
	/**
	 * Constructor.
	 * 
	 * @param methodName method name
	 * @param methodSig  method signature
	 * @param isStatic   true if the method is static, false if not
	 * @param hash       the pre-computed hash
	 */
	public MethodHash(String methodName, String methodSig, boolean isStatic, byte[] hash) {
		this.methodName = methodName;
		this.methodSig = methodSig;
		this.isStatic = isStatic;
		this.hash = new byte[hash.length];
		System.arraycopy(hash, 0, this.hash, 0, hash.length);
	}
	
	/**
	 * @return Returns the method name.
	 */
	public String getMethodName() {
		return methodName;
	}
	
	/**
	 * @return Returns the method signature.
	 */
	public String getMethodSig() {
		return methodSig;
	}
	
	/**
	 * @return Returns whether the method is static.
	 */
	public boolean isStatic() {
		return isStatic;
	}
	
	/**
	 * Get the computed method hash.
	 * 
	 * @return the method hash
	 */
	public byte[] getMethodHash() {
		return hash;
	}
	
	/**
	 * Compute hash on given method.
	 * 
	 * @param method the method
	 * @return this object
	 */
	public MethodHash computeHash(Method method) {
		MessageDigest digest_;
		try {
			digest_ = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("No algorithm for computing method hash", e);
		}
		final MessageDigest digest = digest_;

		byte[] code;
		if (method.getCode() == null || method.getCode().getCode() == null) {
			code = new byte[0];
		} else {
			code = method.getCode().getCode();
		}
		
		BytecodeScanner.Callback callback = new BytecodeScanner.Callback() {
			public void handleInstruction(int opcode, int index) {
				digest.update((byte) opcode);
			}
		};
		
		BytecodeScanner bytecodeScanner = new BytecodeScanner();
		bytecodeScanner.scan(code, callback);
		
		hash = digest.digest();
		
		return this;
	}
	
	/**
	 * Return whether or not this method hash has the same value as the one given.
	 * 
	 * @param other another MethodHash
	 * @return true if the hash values are the same, false if not
	 */
	public boolean isSameHash(MethodHash other) {
		return Arrays.equals(this.hash, other.hash);
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(T)
	 */
	public int compareTo(MethodHash other) {
		return ClassHash.compareHashes(this.hash, other.hash);
	}
}
