/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 * Data source for source files which are stored in
 * the filesystem.
 */
public class FileSourceFileDataSource implements SourceFileDataSource {
	private String fileName;

	public FileSourceFileDataSource(String fileName) {
		this.fileName = fileName;
	}

	public InputStream open() throws IOException {
		return new BufferedInputStream(new FileInputStream(fileName));
	}

	public String getFullFileName() {
		return fileName;
	}
}

// vim:ts=4
