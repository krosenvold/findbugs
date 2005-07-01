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

package edu.umd.cs.findbugs;

import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import edu.umd.cs.findbugs.ba.ClassHash;
import edu.umd.cs.findbugs.ba.MethodHash;

/**
 * A slightly more intellegent way of comparing BugInstances from two versions
 * to see if they are the "same".  Uses class and method hashes to try to
 * handle renamings, at least for simple cases.  (<em>Hashes disabled for the
 * time being.</em>)  Uses opcode context to try to identify code that is the
 * same, even if it moves within the method.  Also compares by bug abbreviation
 * rather than bug type, since the "same" bug can change type if the context
 * changes (e.g., "definitely null" to "null on simple path" for a null pointer
 * dereference).  Also, we often change bug types between different versions
 * of FindBugs.
 * 
 * @see edu.umd.cs.findbugs.BugInstance
 * @see edu.umd.cs.findbugs.VersionInsensitiveBugComparator
 * @author David Hovemeyer
 */
public class FuzzyBugComparator implements Comparator<BugInstance> {
	private static final boolean DEBUG = false;

	// Don't use hashes for now.  Still ironing out issues there.
	private static final boolean USE_HASHES = false;
	
	/**
	 * Filter ignored BugAnnotations from given Iterator.
	 */
	private static class FilteringBugAnnotationIterator implements Iterator<BugAnnotation> {
		
		Iterator<BugAnnotation> iter;
		BugAnnotation next;
		
		public FilteringBugAnnotationIterator(Iterator<BugAnnotation> iter) {
			this.iter = iter;
		}
		
		private void findNext() {
			if (next == null) {
				while (iter.hasNext()) {
					BugAnnotation candidate = iter.next();
					if (!ignore(candidate)) {
						next = candidate;
						break;
					}
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			findNext();
			return next != null;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public BugAnnotation next() {
			findNext();
			if (next == null)
				throw new NoSuchElementException();
			BugAnnotation result = next;
			next = null;
			return result;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	/** Keep track of which BugCollections the various BugInstances have come from. */
	private IdentityHashMap<BugInstance, BugCollection> bugCollectionMap;
	
	/**
	 * Map of class hashes to canonicate class names used for comparison purposes.
	 */
	private Map<ClassHash, String> classHashToCanonicalClassNameMap;
	
	public FuzzyBugComparator() {
		if (DEBUG) System.out.println("Created fuzzy comparator");
		this.bugCollectionMap = new IdentityHashMap<BugInstance, BugCollection>();
		this.classHashToCanonicalClassNameMap = new TreeMap<ClassHash, String>();
	}
	
	/**
	 * Register a BugCollection.  This allows us to find the class and method
	 * hashes for BugInstances to be compared.
	 * 
	 * @param bugCollection a BugCollection
	 */
	public void registerBugCollection(BugCollection bugCollection) {
		for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext(); ) {
			bugCollectionMap.put(i.next(), bugCollection);
		}
		
		// For each distinct ClassHash, keep track of the lexicographically
		// least class name.  This serves as the "representative" for all (equivalent)
		// classes sharing that hash value.  This allows us to ensure that the
		// class ordering induced by this comparator is transitive.
		for (Iterator<ClassHash> i = bugCollection.classHashIterator(); i.hasNext();) {
			ClassHash classHash = i.next();
			String canonicalClassName = classHashToCanonicalClassNameMap.get(classHash);
			if (canonicalClassName == null || classHash.getClassName().compareTo(canonicalClassName) < 0) {
				classHashToCanonicalClassNameMap.put(classHash, classHash.getClassName());
			}
		}
	}
	
	public int compare(BugInstance a, BugInstance b) {
		int cmp;
		
		if (DEBUG) System.out.println("Fuzzy comparison");
		
		// Bug abbreviations must match.
		BugPattern lhsPattern = a.getBugPattern();
		BugPattern rhsPattern = b.getBugPattern();
		if ((cmp = compareNullElements(lhsPattern, rhsPattern)) != 0)
			return cmp;
		if ((cmp = lhsPattern.getAbbrev().compareTo(rhsPattern.getAbbrev())) != 0)
			return cmp;
		
		BugCollection lhsCollection = bugCollectionMap.get(a);
		BugCollection rhsCollection = bugCollectionMap.get(b);
		
		// Scan through bug annotations, comparing fuzzily if possible
		
		Iterator<BugAnnotation> lhsIter = new FilteringBugAnnotationIterator(a.annotationIterator());
		Iterator<BugAnnotation> rhsIter = new FilteringBugAnnotationIterator(b.annotationIterator());
		
		while (lhsIter.hasNext() && rhsIter.hasNext()) {
			BugAnnotation lhs = lhsIter.next();
			BugAnnotation rhs = rhsIter.next();
			
			if (DEBUG) System.out.println("Compare annotations: " + lhs + "," + rhs);

			// Annotation classes must match exactly
			cmp = lhs.getClass().getName().compareTo(rhs.getClass().getName());
			if (cmp != 0) {
				if (DEBUG) System.out.println("annotation class mismatch: " + lhs.getClass().getName() +
						"," + rhs.getClass().getName());
				return cmp;
			}
			
			if (lhs.getClass() == ClassAnnotation.class)
				cmp = compareClasses(lhsCollection, rhsCollection, (ClassAnnotation) lhs, (ClassAnnotation) rhs);
			else if (lhs.getClass() == MethodAnnotation.class)
				cmp = compareMethods(lhsCollection, rhsCollection, (MethodAnnotation) lhs, (MethodAnnotation) rhs);
			else if (lhs.getClass() == SourceLineAnnotation.class)
				cmp = compareSourceLines(lhsCollection, rhsCollection, (SourceLineAnnotation) lhs, (SourceLineAnnotation) rhs);
			else
				// everything else just compare directly
				cmp = lhs.compareTo(rhs);
			
			if (cmp != 0)
				return cmp;
		}
		
		// Number of bug annotations must match
		if (!lhsIter.hasNext() && !rhsIter.hasNext()) {
			if (DEBUG) System.out.println("Match!");
			return 0;
		} else
			return (lhsIter.hasNext() ? 1 : -1);
	}

	private static int compareNullElements(Object a, Object b) {
		if (a != null)
			return 1;
		else if (b != null)
			return -1;
		else
			return 0;
	}
	
	private static ClassHash getClassHash(BugCollection bugCollection, String className) {
		if (bugCollection == null)
			return null;
		else
			return bugCollection.getClassHash(className);
	}
	
	public int compareClasses(BugCollection lhsCollection, BugCollection rhsCollection, ClassAnnotation lhsClass, ClassAnnotation rhsClass) {
		if (lhsClass == null || rhsClass == null) {
			return compareNullElements(lhsClass, rhsClass);
		} else {
			return compareClassesByName(lhsCollection, rhsCollection, lhsClass.getClassName(), rhsClass.getClassName());
		}
	}
	
	// Compare classes: either exact fully qualified name must match, or class hash must match
	public int compareClassesByName(BugCollection lhsCollection, BugCollection rhsCollection, String lhsClassName, String rhsClassName) {
		int cmp;

		if (USE_HASHES) {
			// Get class hashes
			ClassHash lhsHash = getClassHash(lhsCollection, lhsClassName);
			ClassHash rhsHash = getClassHash(rhsCollection, rhsClassName);
			
			// Convert to canonical class names based on the class hashes.
			// This has the effect that classes with the same hash compare as equal,
			// while ensuring that all class names have a consistent ordering.
			if (lhsHash != null)
				lhsClassName = classHashToCanonicalClassNameMap.get(lhsHash);
			if (rhsHash != null)
				rhsClassName = classHashToCanonicalClassNameMap.get(rhsHash);
		}

		return lhsClassName.compareTo(rhsClassName);
	}
	
	// Compare methods: either exact name and signature must match, or method hash must match
	public int compareMethods(BugCollection lhsCollection, BugCollection rhsCollection, MethodAnnotation lhsMethod, MethodAnnotation rhsMethod) {
		if (lhsMethod == null || rhsMethod == null) {
			return compareNullElements(lhsMethod, rhsMethod);
		}

		// Compare for exact match
		int cmp = lhsMethod.compareTo(rhsMethod);
		
		if (USE_HASHES) {
			if (cmp == 0)
				return 0;

			// Get class hashes for primary classes
			ClassHash lhsClassHash = getClassHash(lhsCollection, lhsMethod.getClassName());
			ClassHash rhsClassHash = getClassHash(rhsCollection, rhsMethod.getClassName());
			if (lhsClassHash == null || rhsClassHash == null)
				return cmp;
			
			// Look up method hashes
			MethodHash lhsHash = lhsClassHash.getMethodHash(lhsMethod.toXMethod());
			MethodHash rhsHash = rhsClassHash.getMethodHash(rhsMethod.toXMethod());
			if (lhsHash == null || rhsHash == null)
				return cmp;
			
			if (lhsHash.isSameHash(rhsHash))
				return 0;
		}
		
		return cmp;
	}
	
	/**
	 * For now, just look at the 2 preceeding and succeeding opcodes
	 * for fuzzy source line matching.
	 */
	private static final int NUM_CONTEXT_OPCODES = 2;
	
	/**
	 * Compare source line annotations.
	 * 
	 * @param rhsCollection lhs BugCollection
	 * @param lhsCollection rhs BugCollection
	 * @param lhs           a SourceLineAnnotation
	 * @param rhs           another SourceLineAnnotation
	 * @return comparison of lhs and rhs
	 */
	public int compareSourceLines(BugCollection lhsCollection, BugCollection rhsCollection, SourceLineAnnotation lhs, SourceLineAnnotation rhs) {
		if (lhs == null || rhs == null) {
			return compareNullElements(lhs, rhs);
		}
		
		// Classes must match fuzzily.
		int cmp = compareClassesByName(lhsCollection, rhsCollection, lhs.getClassName(), rhs.getClassName());
		if (cmp != 0)
			return cmp;
		
		// If both annotations refer to entire methods, as opposed to
		// a specific instruction or range of instructions, consider them
		// equal.  This handles the case where a warning may refer to
		// another method (i.e., it was called).
		// Even if the referred-to method changes between versions, we want
		// the warning to be considered equivalent in both versions.
		if (!lhs.hasSpecificInstructions() && !rhs.hasSpecificInstructions())
			return 0;
		
		// See if the opcode contexts match.
		if (   lhs.getEarlierOpcodesAsString(NUM_CONTEXT_OPCODES).equals(rhs.getEarlierOpcodesAsString(NUM_CONTEXT_OPCODES))
			&& lhs.getSelectedOpcodesAsString().equals(rhs.getSelectedOpcodesAsString())
			&& lhs.getLaterOpcodesAsString(NUM_CONTEXT_OPCODES).equals(rhs.getLaterOpcodesAsString(NUM_CONTEXT_OPCODES)))
				return 0;
		
		// Give up and use exact matching algorithm to order the annotations
		return lhs.compareTo(rhs);
	}
	
	// See "FindBugsAnnotationDescriptions.properties"
	private static final HashSet<String> significantDescriptionSet = new HashSet<String>();
	static {
		// Classes, methods, and fields are significant.
		significantDescriptionSet.add("CLASS_DEFAULT");
		significantDescriptionSet.add("CLASS_EXCEPTION");
		significantDescriptionSet.add("CLASS_REFTYPE");
		significantDescriptionSet.add("INTERFACE_TYPE");
		significantDescriptionSet.add("METHOD_DEFAULT");
		significantDescriptionSet.add("METHOD_CALLED");
		significantDescriptionSet.add("METHOD_DANGEROUS_TARGET"); // but do NOT use safe targets
		significantDescriptionSet.add("METHOD_DECLARED_NONNULL");
		significantDescriptionSet.add("FIELD_DEFAULT");
		significantDescriptionSet.add("FIELD_ON");
		significantDescriptionSet.add("FIELD_SUPER");
		significantDescriptionSet.add("FIELD_MASKED");
		significantDescriptionSet.add("FIELD_MASKING");
		// Many int annotations are NOT significant: e.g., sync %, biased locked %, bytecode offset, etc.
		// The null parameter annotations, however, are definitely significant.
		significantDescriptionSet.add("INT_NULL_ARG");
		significantDescriptionSet.add("INT_MAYBE_NULL_ARG");
		significantDescriptionSet.add("INT_NONNULL_PARAM");
		// Only DEFAULT source line annotations are significant.
		significantDescriptionSet.add("SOURCE_LINE_DEFAULT");
	}
	
	public static boolean ignore(BugAnnotation annotation) {
		return !significantDescriptionSet.contains(annotation.getDescription());
	}
}
