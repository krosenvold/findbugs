/*
 * XML input/output support for FindBugs
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

package edu.umd.cs.findbugs.xml;

import java.io.IOException;

/**
 * Interface to generate an XML document in some form.
 * E.g., writing it to a stream, generating SAX events,
 * etc.
 *
 * @author David Hovemeyer
 */
public interface XMLOutput {
	/**
	 * Begin the XML document.
	 */
	public void beginDocument() throws IOException;

	/**
	 * Open a tag with given name.
	 *
	 * @param tagName the tag name
	 */
	public void openTag(String tagName) throws IOException;

	/**
	 * Open a tag with given name and given attributes.
	 *
	 * @param tagName       the tag name
	 * @param attributeList the attributes
	 */
	public void openTag(String tagName, XMLAttributeList attributeList) throws IOException;

	/**
	 * Open and close tag with given name.
	 *
	 * @param tagName the tag name
	 */
	public void openCloseTag(String tagName) throws IOException;

	/**
	 * Open and close tag with given name and given attributes.
	 *
	 * @param tagName       the tag name
	 * @param attributeList the attributes
	 */
	public void openCloseTag(String tagName, XMLAttributeList attributeList) throws IOException;

	/**
	 * Close tag with given name.
	 *
	 * @param tagName the tag name
	 */
	public void closeTag(String tagName) throws IOException;

	/**
	 * Write text to the XML document.
	 * XML metacharacters are automatically escaped.
	 *
	 * @param text the text to write
	 */
	public void writeText(String text) throws IOException;

	/**
	 * Write a CDATA section to the XML document.
	 * The characters are not escaped in any way.
	 *
	 * @param cdata the character data to write
	 */
	public void writeCDATA(String cdata) throws IOException;

	/**
	 * End the XML document.
	 * The underlying output stream will be closed.
	 */
	public void endDocument() throws IOException;
}

// vim:ts=4
