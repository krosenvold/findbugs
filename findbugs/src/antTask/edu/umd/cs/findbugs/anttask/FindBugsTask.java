/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package edu.umd.cs.findbugs.anttask;

import edu.umd.cs.findbugs.ExitCodes;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * FindBugs in Java class files. This task can take the following
 * arguments:
 * <ul>
 * <li>auxClasspath       (classpath or classpathRef)
 * <li>home               (findbugs install dir)
 * <li>quietErrors        (boolean - default false)
 * <li>failOnError        (boolean - default false)
 * <li>reportLevel        (enum experimental|low|medium|high)
 * <li>sort               (boolean default true)
 * <li>debug              (boolean default false) 
 * <li>output             (enum text|xml|xml:withMessages|html - default xml)
 * <li>outputFile         (name of output file to create)
 * <li>stylesheet         (name of stylesheet to generate HTML: default is "default.xsl")
 * <li>visitors           (collection - comma seperated)
 * <li>omitVisitors       (collection - comma seperated)
 * <li>excludeFilter      (filter filename)
 * <li>includeFilter      (filter filename)
 * <li>projectFile        (project filename)
 * <li>jvmargs            (any additional jvm arguments)
 * <li>classpath          (classpath for running FindBugs)
 * <li>pluginList         (list of plugin Jar files to load)
 * <li>systemProperty     (a system property to set)
 * <li>workHard           (boolean default false)
 * <li>adjustExperimental (boolean default false)
 * </ul>
 * Of these arguments, the <b>home</b> is required.
 * <b>projectFile</b> is required if nested &lt;class&gt; are not
 * specified. the &lt;class&gt; tag defines the location of either a 
 *  class, jar file, zip file, or directory containing classes
 * <p>
 *
 * @author Mike Fagan <a href="mailto:mfagan@tde.com">mfagan@tde.com</a>
 * @author Michael Tamm <a href="mailto:mail@michaeltamm.de">mail@michaeltamm.de</a>
 *
 * @version $Revision: 1.31 $
 *
 * @since Ant 1.5
 *
 * @ant.task category="utility"
 */

public class FindBugsTask extends Task {

    private static final String FINDBUGS_JAR = "findbugs.jar";
    private static final long DEFAULT_TIMEOUT = 600000; // ten minutes

	private boolean debug = false;
	private boolean conserveSpace = false;
	private boolean sorted = true;
	private boolean quietErrors = false;
	private boolean failOnError = false;
	private String errorProperty = null;
	private boolean workHard = false;
	private boolean adjustExperimental = false;
	private File homeDir = null;
	private File projectFile = null;
	private File excludeFile = null;
	private File includeFile = null;
	private Path auxClasspath = null;
	private Path sourcePath = null;
	private String outputFormat = "xml";
	private String reportLevel = null;
	private String jvmargs = "";
	private String visitors = null;
	private String omitVisitors = null;
	private String outputFileName = null;
	private String stylesheet = null;
    private List<ClassLocation> classLocations = new ArrayList<ClassLocation>();
	private long timeout = DEFAULT_TIMEOUT;
	private Path classpath = null;
	private Path pluginList = null;
	private List<SystemProperty> systemPropertyList = new ArrayList<SystemProperty>();

	private Java findbugsEngine = null;

    //define the inner class to store class locations
	public static class ClassLocation {
		File classLocation = null;

		public void setLocation( File location ) {
			classLocation = location;
		}

		public File getLocation( ) {
			return classLocation;
		}
      
        public String toString( ) {
           return classLocation!=null?classLocation.toString():"";
        }
 
	}

	// A System property to set when FindBugs is run
	public static class SystemProperty {
		private String name;
		private String value;

		public SystemProperty() {
		}

		public void setName(String name) { this.name = name; }
		public void setValue(String value) { this.value = value; }

		public String getName() { return name; }
		public String getValue() { return value; }
	}

	/**
	 * Set any specific jvm args
	 */
	public void setJvmargs(String args) {
		this.jvmargs = args;
	}

	/**
	 * Set the workHard flag.
	 * 
	 * @param workHard true if we want findbugs to run with workHard option enabled
	 */
	public void setWorkHard(boolean workHard){
		this.workHard = workHard;   
	}
	
	/**
	 * Set the adjustExperimental flag
	 * 
	 * @param adjustExperimental true if we want experimental bug patterns to have lower priority
	 */
	public void setAdjustExperimental(boolean adjustExperimental){
		this.adjustExperimental = adjustExperimental;
	}
    
	/**
	 * Set the specific visitors to use 
	 */
	public void setVisitors(String commaSeperatedString) {
		this.visitors = commaSeperatedString;
	}

	/**
	 * Set the specific visitors to use 
	 */
	public void setOmitVisitors(String commaSeperatedString) {
		this.omitVisitors = commaSeperatedString;
	}

	/**
	 * Set the home directory into which findbugs was installed
	 */
	public void setHome(File homeDir) {
		this.homeDir = homeDir;
	}

	/**
	 * Set the output format
	 */
	public void setOutput(String format) {
		this.outputFormat = format;
	}

	/**
	 * Set the stylesheet filename for HTML generation.
	 */
	public void setStylesheet(String stylesheet) {
		this.stylesheet = stylesheet;
	}

	/**
	 * Set the report level 
	 */
	public void setReportLevel(String level) {
		this.reportLevel = level;
	}

	/**
	 * Set the sorted flag
	 */
	public void setSort(boolean flag) {
		this.sorted = flag;
	}

	/**
	 * Set the quietErrors flag
	 */
	public void setQuietErrors(boolean flag) {
		this.quietErrors = flag;
	}

	/**
	 * Set the failOnError flag
	 */
	public void setFailOnError(boolean flag) {
		this.failOnError = flag;
	}

	/**
	 * Tells this task to set the property with the
	 * given name to "true" when there were errors.
	 */
	public void setErrorProperty(String name) {
		this.errorProperty = name;
	}

	/**
	 * Set the debug flag
	 */
	public void setDebug(boolean flag) {
		this.debug = flag;
	}

	/**
	 * Set the conserveSpace flag.
	 */
	public void setConserveSpace(boolean flag) {
		this.conserveSpace = flag;
	}

	/**
	 * Set the exclude filter file 
	 */
	public void setExcludeFilter(File filterFile) {
		this.excludeFile = filterFile;
	}

	/**
	 * Set the exclude filter file 
	 */
	public void setIncludeFilter(File filterFile) {
		this.includeFile = filterFile;
	}

	/**
	 * Set the project file 
	 */
	public void setProjectFile(File projectFile) {
		this.projectFile = projectFile;
	}

	/**
	 * the auxclasspath to use.
	 */
	public void setAuxClasspath(Path src) {
		if (auxClasspath == null) {
			auxClasspath = src;
		} else {
			auxClasspath.append(src);
		}
	}

	/**
	 * Path to use for auxclasspath.
	 */
	public Path createAuxClasspath() {
		if (auxClasspath == null) {
			auxClasspath = new Path(getProject());
		}
		return auxClasspath.createPath();
	}

	/**
	 * Adds a reference to a sourcepath defined elsewhere.
	 */
	public void setAuxClasspathRef(Reference r) {
		createAuxClasspath().setRefid(r);
	}

	/**
	 * the sourcepath to use.
	 */
	public void setSourcePath(Path src) {
		if (sourcePath == null) {
			sourcePath = src;
		} else {
			sourcePath.append(src);
		}
	}

	/**
	 * Path to use for sourcepath.
	 */
	public Path createSourcePath() {
		if (sourcePath == null) {
			sourcePath = new Path(getProject());
		}
		return sourcePath.createPath();
	}

	/**
	 * Adds a reference to a source path defined elsewhere.
	 */
	public void setSourcePathRef(Reference r) {
		createSourcePath().setRefid(r);
	}

	/**
     * Add a class location
     */
	public ClassLocation createClass() {
		ClassLocation cl = new ClassLocation();
		classLocations.add( cl );
		return cl;
	}

	/**
	 * Set name of output file.
	 */
	public void setOutputFile(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	/**
	 * Set timeout in milliseconds.
	 * @param timeout the timeout
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void execute() throws BuildException {
		checkParameters();
		try {
			execFindbugs();
		} catch (BuildException e) {
			if (errorProperty != null) {
				getProject().setProperty(errorProperty, "true");
			}
			if (failOnError) {
				throw e;
			}
		}
	}

	/**
	 * the classpath to use.
	 */
	public void setClasspath(Path src) {
		if (classpath == null) {
			classpath = src;
		} else {
			classpath.append(src);
		}
	}

	/**
	 * Path to use for classpath.
	 */
	public Path createClasspath() {
		if (classpath == null) {
			classpath = new Path(getProject());
		}
		return classpath.createPath();
	}

	/**
	 * Adds a reference to a classpath defined elsewhere.
	 */
	public void setClasspathRef(Reference r) {
		createClasspath().setRefid(r);
	}

	/**
	 * the plugin list to use.
	 */
	public void setPluginList(Path src) {
		if (pluginList == null) {
			pluginList = src;
		} else {
			pluginList.append(src);
		}
	}

	/**
	 * Path to use for plugin list.
	 */
	public Path createPluginList() {
		if (pluginList == null) {
			pluginList = new Path(getProject());
		}
		return pluginList.createPath();
	}

	/**
	 * Adds a reference to a plugin list defined elsewhere.
	 */
	public void setPluginListRef(Reference r) {
		createPluginList().setRefid(r);
	}

	/**
	 * Create a SystemProperty (to handle &lt;systemProperty&gt; elements).
	 */
	public SystemProperty createSystemProperty() {
		SystemProperty systemProperty = new SystemProperty();
		systemPropertyList.add(systemProperty);
		return systemProperty;
	}

    /**
     * Check that all required attributes have been set
     *
     * @since Ant 1.5
     */
	private void checkParameters() {
		if ( homeDir == null && (classpath == null || pluginList == null) ) {
			throw new BuildException( "either home attribute or " +
									  "classpath and pluginList attributes " +
									  " must be defined for task <"
										+ getTaskName() + "/>",
									  getLocation() );
		}

		if (pluginList != null) {
			// Make sure that all plugins are actually Jar files.
			String[] pluginFileList = pluginList.list();
			for (int i = 0; i < pluginFileList.length; ++i) {
				String pluginFile = pluginFileList[i];
				if (!pluginFile.endsWith(".jar")) {
					throw new BuildException("plugin file " + pluginFile + " is not a Jar file " +
											"in task <" + getTaskName() + "/>",
											getLocation());
				}
			}
		}

		if ( projectFile == null && classLocations.size() == 0 ) {
			throw new BuildException( "either projectfile or <class/> child " +
									  "elements must be defined for task <"
										+ getTaskName() + "/>",
									  getLocation() );
		}
 
		if ( outputFormat != null  && 
			!( outputFormat.trim().equalsIgnoreCase("xml" ) || 
			   outputFormat.trim().equalsIgnoreCase("xml:withMessages" ) || 
			   outputFormat.trim().equalsIgnoreCase("html" ) || 
			   outputFormat.trim().equalsIgnoreCase("text" ) ||
			   outputFormat.trim().equalsIgnoreCase("xdocs" ) ||
			   outputFormat.trim().equalsIgnoreCase("emacs") ) ) { 
			throw new BuildException( "output attribute must be either " +
  									  "'text', 'xml', 'html', 'xdocs' or 'emacs' for task <"
										+ getTaskName() + "/>",
									  getLocation() );
		}
	
		if ( reportLevel != null  && 
			!( reportLevel.trim().equalsIgnoreCase("experimental" ) || 
			   reportLevel.trim().equalsIgnoreCase("low" ) || 
			   reportLevel.trim().equalsIgnoreCase("medium" ) ||
			   reportLevel.trim().equalsIgnoreCase("high" ) ) ) { 
			throw new BuildException( "reportlevel attribute must be either " +
  									  "'experimental' or 'low' or 'medium' or 'high' for task <" + 
										getTaskName() + "/>",
									  getLocation() );
		}

		if ( excludeFile != null && includeFile != null ) {
			throw new BuildException("only one of excludeFile and includeFile " +
				" attributes may be used in task <" + getTaskName() + "/>",
				getLocation());
		}

		for (Iterator i = systemPropertyList.iterator(); i.hasNext(); ) {
			SystemProperty systemProperty = (SystemProperty) i.next();
			if (systemProperty.getName() == null || systemProperty.getValue() == null)
				throw new BuildException("systemProperty elements must have name and value attributes");
		}
    } 

	/**
	 * Add an argument to the JVM used to execute FindBugs.
	 * @param arg the argument
	 */
	private void addArg(String arg) {
		findbugsEngine.createArg().setValue(arg);
	}

    /**
     * Create a new JVM to do the work.
     *
     * @since Ant 1.5
     */
	private void execFindbugs() throws BuildException {
		findbugsEngine = (Java) getProject().createTask("java");

		findbugsEngine.setTaskName( getTaskName() );
		findbugsEngine.setFork( true );
		findbugsEngine.setTimeout( new Long( timeout ) );

		if ( workHard ){
			jvmargs = jvmargs + " -Dfindbugs.workHard=true";
		}
		if ( debug )
			jvmargs = jvmargs + " -Dfindbugs.debug=true";
		if ( conserveSpace )
			jvmargs = jvmargs + " -Dfindbugs.conserveSpace=true";
		findbugsEngine.createJvmarg().setLine( jvmargs ); 

		// Add JVM arguments for system properties
		for (Iterator i = systemPropertyList.iterator(); i.hasNext(); ) {
			SystemProperty systemProperty = (SystemProperty) i.next();
			String jvmArg = "-D" + systemProperty.getName() + "=" + systemProperty.getValue();
			findbugsEngine.createJvmarg().setValue(jvmArg);
		}

		if (homeDir != null) {
			// Use findbugs.home to locate findbugs.jar and the standard
			// plugins.  This is the usual means of initialization.

			findbugsEngine.setJar( new File( homeDir + File.separator + "lib" + 
                                         File.separator + FINDBUGS_JAR ) );

			addArg("-home");
			addArg(homeDir.getPath());
		} else {
			// Use an explicitly specified classpath and list of plugin Jars
			// to initialize.  This is useful for other tools which may have
			// FindBugs installed using a non-standard directory layout.

			findbugsEngine.setClasspath(classpath);
			findbugsEngine.setClassname("edu.umd.cs.findbugs.FindBugs");

			addArg("-pluginList");
			addArg(pluginList.toString());
		}
		
		if (adjustExperimental) {
			addArg("-adjustExperimental");
		}

		if ( sorted ) addArg("-sortByClass");
		if ( outputFormat != null && !outputFormat.trim().equalsIgnoreCase("text") ) {
			outputFormat = outputFormat.trim();
			String outputArg = "-";
			int colon = outputFormat.indexOf(':');
			if (colon >= 0) {
				outputArg += outputFormat.substring(0, colon).toLowerCase();
				outputArg += ":";
				outputArg += outputFormat.substring(colon + 1);
			} else {
				outputArg += outputFormat.toLowerCase();
				if (stylesheet != null) {
					outputArg += ":";
					outputArg += stylesheet.trim();
				}
			}
			addArg(outputArg);
		}
		if ( quietErrors ) addArg("-quiet");
		if ( reportLevel != null ) addArg("-" + reportLevel.trim().toLowerCase());
		if ( projectFile != null ) {
			addArg("-project");
			addArg(projectFile.getPath());
		}
		if ( excludeFile != null ) {
			addArg("-exclude");
			addArg(excludeFile.getPath());
		}
		if ( includeFile != null) {
			addArg("-include");
			addArg(includeFile.getPath());
		}
		if ( visitors != null) {
			addArg("-visitors");
			addArg(visitors);
		}
		if ( omitVisitors != null ) {
			addArg("-omitVisitors");
			addArg(omitVisitors);
		}
		if ( auxClasspath != null ) {
			addArg("-auxclasspath");
			addArg(auxClasspath.toString());
		}
		if ( sourcePath != null) {
			addArg("-sourcepath");
			addArg(sourcePath.toString());
		}
		if ( outputFileName != null ) {
			addArg("-outputFile");
			addArg(outputFileName);
		}
        
		addArg("-exitcode");
        Iterator itr = classLocations.iterator();
        while ( itr.hasNext() ) {
			addArg(itr.next().toString());
      	} 

		log("Running FindBugs...");

		int rc = findbugsEngine.executeJava();

		if ((rc & ExitCodes.ERROR_FLAG) != 0) {
			throw new BuildException("Execution of findbugs failed.");
		}
		if ((rc & ExitCodes.MISSING_CLASS_FLAG) != 0) {
			log("Classes needed for analysis were missing");
		}
		if (outputFileName != null) {
			log("Output saved to " + outputFileName);
		}
    } 

}

// vim:ts=4
