/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Recursively search a directory, its subdirectories, etc.
 * Note that the search algorithm uses a worklist, so its implementation does
 * not use recursive method calls.
 *
 * @author David Hovemeyer
 */
public class RecursiveFileSearch {
	private String baseDir;
	private FileFilter fileFilter;
	private LinkedList<File> directoryWorkList;
	private ArrayList<String> resultList;

	/**
	 * Constructor.
	 * @param baseDir the base directory for the search
	 * @param fileFilter chooses files to add to the results, and subdirectories
	 *   to continue the search in
	 */
	public RecursiveFileSearch(String baseDir, FileFilter fileFilter) {
		this.baseDir = baseDir;
		this.fileFilter = fileFilter;
		this.directoryWorkList = new LinkedList<File>();
		this.resultList = new ArrayList<String>();
	}

	/**
	 * Perform the search.
	 * @return this object
	 * @throws InterruptedException if the thread is interrupted before the
	 *   search completes
	 */
	public RecursiveFileSearch search() throws InterruptedException {
		directoryWorkList.add(new File(baseDir));

		while (!directoryWorkList.isEmpty()) {
			File dir = directoryWorkList.removeFirst();
			if (!dir.isDirectory())
				continue;

			File[] contentList = dir.listFiles();
			for (int i = 0; i < contentList.length; ++i) {
				if (Thread.interrupted())
					throw new InterruptedException();

				File file = contentList[i];

				if (!fileFilter.accept(file))
					continue;

				if (file.isDirectory())
					directoryWorkList.add(file);
				else
					resultList.add(file.getPath());
			}
		}

		return this;
	}

	/**
	 * Get an iterator over the files found by the search.
	 * The full path names of the files are returned.
	 */
	public Iterator<String> fileNameIterator() {
		return resultList.iterator();
	}

}

// vim:ts=4
