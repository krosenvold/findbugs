/*
 * Run JUnit tests contained in a Jar file
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

package edu.umd.cs.findbugs.tools.junit;

import java.io.File;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringTokenizer;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import junit.framework.TestSuite;

/**
 * Run all of the JUnit tests in a jar file
 * using the JUnit textui.
 * There might be a simple way of doing this directly
 * with JUnit.  However, I'm lazy and impatient, and writing
 * some code to do this was very simple.
 *
 * @author David Hovemeyer
 */
public class JUnitJarRunner {
	private String jarFileName;
	private String classpath;

	/**
	 * Constructor.
	 * @param jarFileName name of jar file to load tests from
	 */
	public JUnitJarRunner(String jarFileName) {
		this.jarFileName = jarFileName;
	}

	/**
	 * Set the classpath containing the code to be tested
	 * (if it is not already on the system classpath).
	 * @param classpath the classpath
	 */
	public void setClassPath(String classpath) {
		this.classpath = classpath;
	}

	/**
	 * Build a TestSuite of all the tests contained in the
	 * jar file.
	 * @return TestSuite for running all of the tests in the jar file
	 */
	public TestSuite buildTestSuite() throws Exception {
		TestSuite suite = new TestSuite();

		ArrayList<URL> urlList = new ArrayList<URL>();
		urlList.add(new URL("file:" + jarFileName));
		if (classpath != null) {
			StringTokenizer tok = new StringTokenizer(classpath, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				urlList.add(new URL("file:" + tok.nextToken()));
			}
		}

		ClassLoader cl = new URLClassLoader(urlList.toArray(new URL[0]));

		Class<?> testCaseClass = cl.loadClass("junit.framework.TestCase");

		JarFile jarFile = new JarFile(jarFileName);
		Enumeration e = jarFile.entries();
		while (e.hasMoreElements()) {
			JarEntry entry = (JarEntry) e.nextElement();
			String entryName = entry.getName();
			if (entryName.endsWith(".class")) {
				String className =
					entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
				if (!className.endsWith("Test"))
					continue;
				System.out.println("Loading test class: " + className);
				System.out.flush();
				Class<?> jarClass = cl.loadClass(className);
				if (testCaseClass.isAssignableFrom(jarClass))
					suite.addTestSuite(jarClass);
			}
		}

		return suite;
	}

	public void run(TestSuite suite, String how) {
		if (how.equals("-textui")) {
			junit.textui.TestRunner.run(suite);
		} else if (how.equals("-swingui")) {
			//junit.swingui.TestRunner.run(suite);
			throw new UnsupportedOperationException("I don't know how to run the Swing UI on a test suite yet");
		} else
			throw new IllegalArgumentException("Unknown option: " + how);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length < 1) {
			System.err.println("Usage: " + JUnitJarRunner.class.getName() +
				" [-textui|-swingui]" +
				" <test suite jar file> [<classpath with code to test>]");
			System.exit(1);
		}
		String how = "-textui";
		int arg = 0;
		if (argv[arg].startsWith("-")) {
			how = argv[arg++];
		}
		String jarFileName = argv[arg++];
		JUnitJarRunner runner = new JUnitJarRunner(jarFileName);
		if (arg < argv.length)
			runner.setClassPath(argv[arg++]);
		TestSuite suite = runner.buildTestSuite();
		runner.run(suite, how);
	}
}

// vim:ts=4
