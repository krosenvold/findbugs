/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.type;

import edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser;
import edu.umd.cs.findbugs.ba.Debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.bcel.Constants;

/**
 * Factory/repository class to ensure that all abstract Java types
 * are represented by a unique Type object.
 * Queries on the type hierarchy can be performed
 * on the types instantiated by the repository.
 *
 * <p> Typically, this class is used by specifying a
 * {@link ClassResolver ClassResolver} that does the work of
 * finding class representations, which will determine
 * whether particular types are classes or interfaces,
 * and what the superclasses and superinterfaces of
 * class and interface types are.  The {@link #isSubtype}
 * method will automatically construct the class hierarchy
 * using the ClassResolver to determine the hierarchy.
 *
 * <p> Another way to use TypeRepository is to explicitly mark
 * ClassType objects as interfaces or classes, and add the
 * subtype relationships using {@link #addSuperclassLink}
 * and {@link @addInterfaceLink} for ClassTypes.  Subtype
 * relationships for array types are always added automatically
 * based on the class hierarchy.  Note that if you use this
 * approach, you must explicitly add <code>java.lang.Object</code>,
 * <code>java.io.Serializable</code>, and <code>java.lang.Cloneable</code>
 * to the repository.
 *
 * @see Type
 * @see ClassResolver
 * @author David Hovemeyer
 */
public class TypeRepository {
	// FIXME:
	// - signatures should probably be interned
	//   (do experiment, see if it makes any difference in memory use)

	/* ----------------------------------------------------------------------
	 * Static data
	 * ---------------------------------------------------------------------- */

	/**
	 * Basic type signatures to type codes.
	 */
	private static final HashMap<String, Byte> signatureToBasicTypeCodeMap = new HashMap<String, Byte>();
	static {
		signatureToBasicTypeCodeMap.put("Z", new Byte(Constants.T_BOOLEAN));
		signatureToBasicTypeCodeMap.put("C", new Byte(Constants.T_CHAR));
		signatureToBasicTypeCodeMap.put("F", new Byte(Constants.T_FLOAT));
		signatureToBasicTypeCodeMap.put("D", new Byte(Constants.T_DOUBLE));
		signatureToBasicTypeCodeMap.put("B", new Byte(Constants.T_BYTE));
		signatureToBasicTypeCodeMap.put("S", new Byte(Constants.T_SHORT));
		signatureToBasicTypeCodeMap.put("I", new Byte(Constants.T_INT));
		signatureToBasicTypeCodeMap.put("J", new Byte(Constants.T_LONG));
		signatureToBasicTypeCodeMap.put("V", new Byte(Constants.T_VOID));
	}

	/**
	 * Basic type codes to signatures.
	 */
	private static final HashMap<Byte, String> basicTypeCodeToSignatureMap = new HashMap<Byte,String>();
	static {
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_BOOLEAN), "Z");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_CHAR), "C");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_FLOAT), "F");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_DOUBLE), "D");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_BYTE), "B");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_SHORT), "S");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_INT), "I");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_LONG), "J");
		basicTypeCodeToSignatureMap.put(new Byte(Constants.T_VOID), "V");
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private HashMap<String, Type> signatureToTypeMap;
	private InheritanceGraph inheritanceGraph;
	private ClassResolver resolver;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * Creates an empty type repository.
	 * @param resolver the ClassResolver that will be used to
	 *   find inheritance hierarchy information for classes
	 */
	public TypeRepository(ClassResolver resolver) {
		this.signatureToTypeMap = new HashMap<String, Type>();
		this.inheritanceGraph = new InheritanceGraph();
		this.resolver = resolver;
	}

	/**
	 * Get an ClassType from a class or interface name
	 * using slashes to separate package components,
	 * creating it if it doesn't exist.
	 * (A name with components separated by slashes
	 * is the native format for bare class names
	 * in class files.)
	 * @param slashedClassName class name in slashed format
	 * @return the ClassType representing the class
	 */
	public ClassType classTypeFromSlashedClassName(String slashedClassName) {
		if (Debug.CHECK_ASSERTIONS && slashedClassName.endsWith(";"))
			throw new IllegalArgumentException("Illegal slashed class name: " + slashedClassName);
		String signature = "L" + slashedClassName + ";";
		return createClassType(signature);
	}

	/**
	 * Get an ClassType from a class or interface name
	 * using dots to separate package components,
	 * creating it if it doesn't exist.
	 * @param param dottedClassName the class name in dotted format
	 * @return the ClassType representing the class
	 */
	public ClassType classTypeFromDottedClassName(String dottedClassName) {
		StringBuffer buf = new StringBuffer();
		buf.append('L');
		buf.append(dottedClassName.replace('.', '/'));
		buf.append(';');
		return createClassType(buf.toString());
	}

	/**
	 * Get an ArrayType from an array signature,
	 * creating it if it doesn't exist.
	 * @param signature the array signature
	 * @return the ArrayType representing the array type
	 */
	public ArrayType arrayTypeFromSignature(String signature) throws InvalidSignatureException {
		return createArrayType(signature);
	}

	/**
	 * Get an ArrayType from number of dimensions and element type. 
	 * @param numDimensions the number of dimensions
	 * @param elementType the element type: must be created from this type repository
	 * @return the array type
	 */
	public ArrayType arrayTypeFromDimensionsAndElementType(int numDimensions, Type elementType) {
		return createArrayType(numDimensions, elementType);
	}

	/**
	 * Create an BasicType from a type code.
	 * @param typeCode the basic type code (T_BOOLEAN, etc.)
	 * @return the BasicType representing the basic type
	 */
	public BasicType basicTypeFromTypeCode(byte typeCode) {
		String signature = basicTypeCodeToSignatureMap.get(new Byte(typeCode));
		if (signature == null)
			throw new IllegalArgumentException("Invalid basic type code: " + typeCode);
		BasicType type = (BasicType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new BasicType(typeCode);
			signatureToTypeMap.put(signature, type);
		}
		return type;
	}

	/**
	 * Create an BasicType from a basic type signature.
	 * @param signature the signature
	 * @return the BasicType representing the basic type
	 */
	public BasicType basicTypeFromSignature(String signature) throws InvalidSignatureException {
		Byte typeCode = signatureToBasicTypeCodeMap.get(signature);
		if (typeCode == null)
			throw new InvalidSignatureException("Bad type signature: " + signature);
		BasicType type = (BasicType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new BasicType(typeCode.byteValue());
			signatureToTypeMap.put(signature, type);
		}
		return type;
	}

	/**
	 * Get an Type object representing the type whose JVM signature
	 * is given, creating it if it doesn't exist.
	 *
	 * @param signature the JVM signature of the type: something
	 * like "B" (the byte basic type), "Ljava/lang/String;"
	 * (the type of a reference to java.lang.String), or
	 * "[Ljava/lang/Object;" (the type of a reference to an array of
	 * java.lang.Object references).
	 * @return the Type object representing the type
	 */
	public Type typeFromSignature(String signature) throws InvalidSignatureException {
		Type type = signatureToTypeMap.get(signature);
		if (type != null)
			return type;
		else if (signature.startsWith("L"))
			return createClassType(signature);
		else if (signature.startsWith("["))
			return createArrayType(signature);
		else
			return basicTypeFromSignature(signature);
	}

	/**
	 * Add a direct superclass relationship to types in the repository.
	 * @param subclass the subclass
	 * @param superclass the superclass
	 */
	public void addSuperclassLink(ObjectType subclass, ObjectType superclass) {
		inheritanceGraph.createEdge(subclass, superclass, InheritanceGraphEdgeTypes.CLASS_EDGE);
	}

	/**
	 * Add a direct implemented interface relationship to types in the repository.
	 * @param implementor the class or interface directly implementing the interface (i.e., the subtype)
	 * @param iface the implemented interface (i.e., the supertype)
	 */
	public void addInterfaceLink(ObjectType implementor, ClassType iface) {
		inheritanceGraph.createEdge(implementor, iface, InheritanceGraphEdgeTypes.INTERFACE_EDGE);
	}

	/**
	 * Determine if one object type is a subtype of another.
	 * @param subtype the potential subtype
	 * @param supertype the potential supertype
	 * @return true if subtype is really a subtype of supertype, false otherwise
	 */
	public boolean isSubtype(ObjectType subtype, ObjectType supertype) throws ClassNotFoundException {
		if (Debug.VERIFY_INTEGRITY) {
			if (!inheritanceGraph.containsVertex(subtype))
				throw new IllegalStateException("Inheritance graph does not contain node " +
					subtype.getSignature());
			if (!inheritanceGraph.containsVertex(supertype))
				throw new IllegalStateException("Inheritance graph does not contain node " +
					supertype.getSignature());
		}

		SubtypeQueryResult cachedResult = findSupertypes(subtype);
		return cachedResult.isSupertype(supertype);
	}

	/**
	 * Get the superclass of a class type.
	 * @param type the class type
	 * @return the ClassType representing the class's superclass,
	 *    or null if the type has no superclass (i.e., is java.lang.Object)
	 */
	public ClassType getSuperclass(ClassType type) throws ClassNotFoundException {
		resolveObjectType(type);

		for (Iterator<InheritanceGraphEdge> i = inheritanceGraph.outgoingEdgeIterator(type); i.hasNext(); ) {
			InheritanceGraphEdge edge = i.next();
			if (edge.getType() == InheritanceGraphEdgeTypes.CLASS_EDGE) {
				ObjectType supertype = edge.getTarget();
				if (!(supertype instanceof ClassType))
					throw new IllegalStateException("Class type " + type.getClassName() +
						" has non-class type " + supertype.getSignature() + " as its superclass");
				// TODO: cache result
				return (ClassType) supertype;
			}
		}
		return null;
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	private ClassType createClassType(String signature) {
		ClassType type = (ClassType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new ClassType(signature);
			signatureToTypeMap.put(signature, type);
			inheritanceGraph.addVertex(type);
		}
		return type;
	}

	private ArrayType createArrayType(String signature) throws InvalidSignatureException {
		ArrayType type = (ArrayType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = ArrayType.typeFromSignature(this, signature);
			signatureToTypeMap.put(signature, type);
			inheritanceGraph.addVertex(type);
		}
		return type;
	}

	private ArrayType createArrayType(int numDimensions, Type elementType) {
		String signature = ArrayType.makeArraySignature(numDimensions, elementType);
		ArrayType type = (ArrayType) signatureToTypeMap.get(signature);
		if (type == null) {
			type = new ArrayType(signature, numDimensions, elementType);
			signatureToTypeMap.put(signature, type);
			inheritanceGraph.addVertex(type);
		}
		return type;
	}

	private SubtypeQueryResult findSupertypes(ObjectType subtype) throws ClassNotFoundException {
		// See if there is a cached query result.
		SubtypeQueryResult cachedResult = subtype.getSubtypeQueryResult();

		if (cachedResult == null) {
			// Breadth first traversal of supertypes
	
			// Work queue
			LinkedList<ObjectType> work = new LinkedList<ObjectType>();
			work.add(subtype);
	
			// Keep track of where we've been
			HashSet<ObjectType> visited = new HashSet<ObjectType>();
	
			// Keep track of missing classes
			LinkedList<String> missingClassList = new LinkedList<String>();
	
			// Cached result for future queries
			cachedResult = new SubtypeQueryResult();
	
			// Keep going until we have examined all supertypes.
			while (!work.isEmpty()) {
				ObjectType type = work.removeFirst();
				if (!visited.add(type))
					continue;
	
				cachedResult.addSupertype(subtype, type);
	
				try {
					// Resolve the type so we know its supertypes.
					resolveObjectType(type);
	
					// Add all supertypes to work queue.
					for (Iterator<ObjectType> i = inheritanceGraph.successorIterator(type); i.hasNext(); )
						work.add(i.next());
				} catch (ClassNotFoundException e) {
					String missingClassName = ClassNotFoundExceptionParser.getMissingClassName(e);
					if (missingClassName == null)
						missingClassName = "<unknown class>";
					missingClassList.add(missingClassName);
				}
			}
	
			cachedResult.finish(missingClassList.toArray(new String[0]));
	
			// Cache result for future queries
			subtype.setSubtypeQueryResult(cachedResult);
		}

		return cachedResult;
	}

	/**
	 * Fill in supertype information for an object type.
	 * If it's a class type, also check whether it is
	 * a class or interface.
	 */
	private void resolveObjectType(ObjectType type) throws ClassNotFoundException {
		if (type.getState() != ObjectType.KNOWN) {
			if (type instanceof ArrayType) {
				resolveArrayClass((ArrayType) type);
			} else {
				resolveClass((ClassType) type);
			}
		}
	}

	private void resolveArrayClass(ArrayType type) throws ClassNotFoundException {
		// FIXME: avoid repeated failed attempts to resolve

		Type baseType = type.getBaseType();

		if (baseType.getSignature().equals("Ljava/lang/Object;")) {
			// Special case: an array whose base type is java.lang.Object
			// is a direct subtype of an array of Object with one less dimension.
			// Except, a single dimensional array of Object is a
			// a direct subtype of java.lang.Object.
			ObjectType directBaseType;
			if (type.getNumDimensions() == 1) {
				directBaseType = (ObjectType) baseType;
			} else {
				directBaseType =
					arrayTypeFromDimensionsAndElementType(type.getNumDimensions() - 1, baseType);
			}

			addSuperclassLink(type, directBaseType);

		} else {
			Type elementType = type.getElementType(this);
			if (elementType.isBasicType()) {
				// All arrays of basic types are subtypes of java.lang.Object
				ClassType javaLangObjectType = classTypeFromSlashedClassName("java/lang/Object");
				addSuperclassLink(type, javaLangObjectType);
			} else {
				// Array is a direct subtype of all arrays (same dimensionality)
				// of supertypes of the element type.
				ObjectType elementObjectType = (ObjectType) elementType;
				resolveObjectType(elementObjectType);
				for (Iterator<ObjectType> i = inheritanceGraph.successorIterator(elementObjectType); i.hasNext(); ) {
					ObjectType elementSupertype = i.next();
					ObjectType arraySupertype =
						arrayTypeFromDimensionsAndElementType(type.getNumDimensions(), elementSupertype);
					addSuperclassLink(type, arraySupertype);
				}
			}

		}

		// All arrays implement Serializable and Cloneable
		addInterfaceLink(type, classTypeFromSlashedClassName("java/io/Serializable"));
		addInterfaceLink(type, classTypeFromSlashedClassName("java/lang/Cloneable"));

		type.setState(ObjectType.KNOWN);
	}

	private void resolveClass(ClassType type) throws ClassNotFoundException {
		// If previous attempt at resolution failed,
		// just throw an exception
		if (type.getState() == ObjectType.UNKNOWN)
			throw new ClassNotFoundException("Class " + type.getClassName() +
				" cannot be resolved", type.getResolverFailure());

		// Delegate to the ClassResolver
		try {
			resolver.resolveClass(type, this);
			type.setState(ObjectType.KNOWN);

			// If the class is an interface, make it a direct
			// subclass of java.lang.Object.  This is a convenient
			// fiction that makes things a bit simpler.
			if (type.isInterface())
				addInterfaceLink(type, classTypeFromSlashedClassName("java/lang/Object"));
		} catch (ClassNotFoundException e) {
			type.setState(ObjectType.UNKNOWN);
			type.setResolverFailure(e);
			throw new ClassNotFoundException("Class " + type.getClassName() +
				" cannot be resolved", e);
		}
	}

	/**
	 * Get an ObjectType by its vertex label in the inheritance graph.
	 * @param label the vertex label
	 * @return the ObjectType with that label
	 */
	ObjectType getObjectTypeByVertexLabel(int label) {
		return inheritanceGraph.getVertexByLabel(label);
	}
}

// vim:ts=4
