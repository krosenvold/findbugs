/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.util.Util;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;

/**
 * Helper class to parse the command line and configure
 * the IFindBugsEngine object.
 * As a side-effect it also configures a DetectorFactoryCollection
 * (to enable and disable detectors as requested).
 */
public class TextUICommandLine extends FindBugsCommandLine {
	/**
	 * Handling callback for choose() method,
	 * used to implement the -chooseVisitors and -choosePlugins options.
	 */
	private interface Chooser {
		/**
		 * Choose a detector, plugin, etc.
		 *
		 * @param enable whether or not the item should be enabled
		 * @param what   the item
		 */
		public void choose(boolean enable, String what);
	}

	private static final int PRINTING_REPORTER = 0;
	private static final int SORTING_REPORTER = 1;
	private static final int XML_REPORTER = 2;
	private static final int EMACS_REPORTER = 3;
	private static final int HTML_REPORTER = 4;
	private static final int XDOCS_REPORTER = 5;

	private int bugReporterType = PRINTING_REPORTER;
	private boolean relaxedReportingMode = false;
	private boolean useLongBugCodes = false;
	private boolean showProgress = false;
	private boolean xmlWithMessages = false;
	private boolean xmlWithAbridgedMessages = false;
	private String stylesheet = null;
	private boolean quiet = false;
	private ClassScreener classScreener = new ClassScreener();
	private List<String> includeFilterFile = new LinkedList<String>();
	private List<String> excludeFilterFile = new LinkedList<String>();
	private List<String> excludeBugFile = new LinkedList<String>();
	private boolean setExitCode = false;
	private boolean noClassOk = false;
	private int priorityThreshold = Detector.NORMAL_PRIORITY;
	private PrintStream outputStream = null;
	private Set<String> bugCategorySet = null;
	private UserPreferences userPreferences;
	private String trainingOutputDir;
	private String trainingInputDir;
	private String releaseName = "";
	private String projectName="";
	private String sourceInfoFile = null;
	private boolean xargs = false;
	private boolean scanNestedArchives = false;
	private String userAnnotationPlugin;
	private Map<String,String> userAnnotationPluginProperties = new HashMap<String, String>();
	private boolean userAnnotationSync;

	/**
	 * Constructor.
	 */
	public TextUICommandLine() {

		addSwitch("-showPlugins", "show list of available detector plugins");

		startOptionGroup("Output options:");
		addSwitch("-timestampNow", "set timestamp of results to be current time");
		addSwitch("-quiet", "suppress error messages");
		addSwitch("-longBugCodes", "report long bug codes");
		addSwitch("-progress", "display progress in terminal window");
		addOption("-release", "release name", "set the release name of the analyzed application");
		addSwitch("-experimental", "report all warnings including experimental bug patterns");
		addSwitch("-low", "report all warnings");
		addSwitch("-medium", "report only medium and high priority warnings [default]");
		addSwitch("-high", "report only high priority warnings");
		addSwitch("-sortByClass", "sort warnings by class");
		addSwitchWithOptionalExtraPart("-xml", "withMessages",
				"XML output (optionally with messages)");
		addSwitch("-xdocs", "xdoc XML output to use with Apache Maven");
		addSwitchWithOptionalExtraPart("-html", "stylesheet",
				"Generate HTML output (default stylesheet is default.xsl)");
		addSwitch("-emacs", "Use emacs reporting format");
		addSwitch("-relaxed", "Relaxed reporting mode (more false positives!)");
		addSwitchWithOptionalExtraPart("-train", "outputDir",
				"Save training data (experimental); output dir defaults to '.'");
		addSwitchWithOptionalExtraPart("-useTraining", "inputDir",
				"Use training data (experimental); input dir defaults to '.'");
		addOption("-sourceInfo", "filename",
				"Specify source info file (line numbers for fields/classes)");
		addOption("-projectName", "project name", "Descriptive name of project");

		addOption("-outputFile", "filename", "Save output in named file");
		addOption("-output", "filename", "Save output in named file");
		makeOptionUnlisted("-outputFile");

		startOptionGroup("Output filtering options:");
		addOption("-bugCategories", "cat1[,cat2...]", "only report bugs in given categories");
		addOption("-onlyAnalyze", "classes/packages", "only analyze given classes and packages; end with .* to indicate classes in a package, .- to indicate a package prefix");
		addOption("-excludeBugs", "baseline bugs", "exclude bugs that are also reported in the baseline xml output");
		addOption("-exclude", "filter file", "exclude bugs matching given filter");
		addOption("-include", "filter file", "include only bugs matching given filter");
		addSwitchWithOptionalExtraPart("-nested", "true|false",
				"analyze nested jar/zip archives (default=true)");
		
		startOptionGroup("Detector (visitor) configuration options:");
		addOption("-visitors", "v1[,v2...]", "run only named visitors");
		addOption("-omitVisitors", "v1[,v2...]", "omit named visitors");
		addOption("-chooseVisitors", "+v1,-v2,...", "selectively enable/disable detectors");
		addOption("-choosePlugins", "+p1,-p2,...", "selectively enable/disable plugins");
		addOption("-adjustPriority", "v1=(raise|lower)[,...]",
				"raise/lower priority of warnings for given visitor(s)");

		startOptionGroup("Project configuration options:");
		addOption("-auxclasspath", "classpath", "set aux classpath for analysis");
		addOption("-sourcepath", "source path", "set source path for analyzed classes");
		addSwitch("-exitcode", "set exit code of process");
		addSwitch("-noClassOk", "output empty warning file if no classes are specified");
		addSwitch("-xargs", "get list of classfiles/jarfiles from standard input rather than command line");
		
		startOptionGroup("User annotation persistence options:");
		addOption("-uaPlugin", "class name", "class name of user annotation plugin");
		addOption("-uaPluginProps", "p1=val[,p2=val,...]", "specify configuration properties for user annotation plugin");
		addSwitch("-uaSync", "fetch user annotations and apply them to new analysis results");
	}

	@Override
	public Project getProject() {
		return project;
	}
	public boolean getXargs() {
		return xargs;
	}
	public boolean setExitCode() {
		return setExitCode;
	}
	public boolean noClassOk() {
		return noClassOk;
	}

	public boolean quiet() {
		return quiet;
	}

	/**
	 * Get class name of user annotation plugin.
	 * 
	 * @return class name of user annotation plugin, or null if not specified
	 */
	public @CheckForNull String getUserAnnotationPlugin() {
		return userAnnotationPlugin;
	}

	/**
	 * Get map of configuration properties for user annotation plugin.
	 * 
	 * @return map of configuration properties for user annotation plugin
	 */
	public Map<String, String> getUserAnnotationPluginProperties() {
		return Collections.unmodifiableMap(userAnnotationPluginProperties);
	}
	
	/**
	 * Return whether or not the user annotation plugin should be used to
	 * fetch user annotations and apply them to the generated
	 * analysis results.
	 * 
	 * @return true if user annotations should be fetched/applied, false if not
	 */
	public boolean getUserAnnotationSync() {
		return userAnnotationSync;
	}

	@SuppressWarnings("DM_EXIT")
	@Override
	protected void handleOption(String option, String optionExtraPart) {
		if (option.equals("-showPlugins")) {
			System.out.println("Available plugins:");
			int count = 0;
			for (Iterator<Plugin> i = DetectorFactoryCollection.instance().pluginIterator(); i.hasNext(); ) {
				Plugin plugin = i.next();
				System.out.println("  " + plugin.getPluginId() + " (default: " +
						(plugin.isEnabled() ? "enabled" : "disabled") + ")");
				if (plugin.getShortDescription() != null)
					System.out.println("    Description: " + plugin.getShortDescription());
				if (plugin.getProvider() != null)
					System.out.println("    Provider: " + plugin.getProvider());
				if (plugin.getWebsite() != null)
					System.out.println("    Website: " + plugin.getWebsite());
				++count;
			}
			if (count == 0) {
				System.out.println("  No plugins are available (FindBugs installed incorrectly?)");
			}
			System.exit(0);
		} else if (option.equals("-experimental"))
			priorityThreshold = Detector.EXP_PRIORITY;
		else if (option.equals("-longBugCodes"))
			useLongBugCodes = true;
		else if (option.equals("-progress")) {
			showProgress = true;
		}
		else if (option.equals("-timestampNow"))
			project.setTimestamp(System.currentTimeMillis());
		else if (option.equals("-low"))
			priorityThreshold = Detector.LOW_PRIORITY;
		else if (option.equals("-medium"))
			priorityThreshold = Detector.NORMAL_PRIORITY;
		else if (option.equals("-high"))
			priorityThreshold = Detector.HIGH_PRIORITY;
		else if (option.equals("-sortByClass"))
			bugReporterType = SORTING_REPORTER;
		else if (option.equals("-xml")) {
			bugReporterType = XML_REPORTER;
			if (!optionExtraPart.equals("")) {
				if (optionExtraPart.equals("withMessages"))
					xmlWithMessages = true;
				else if (optionExtraPart.equals("withAbridgedMessages")) {
					xmlWithMessages = true;
					xmlWithAbridgedMessages = true;
				} else
					throw new IllegalArgumentException("Unknown option: -xml:" + optionExtraPart);
			}
		} else if (option.equals("-emacs")) {
			bugReporterType = EMACS_REPORTER;
		} else if (option.equals("-relaxed")) {
			relaxedReportingMode = true;
		} else if (option.equals("-train")) {
			trainingOutputDir = !optionExtraPart.equals("") ? optionExtraPart : ".";
		} else if (option.equals("-useTraining")) {
			trainingInputDir = !optionExtraPart.equals("") ? optionExtraPart : ".";
		} else if (option.equals("-html")) {
			bugReporterType = HTML_REPORTER;
			if (!optionExtraPart.equals("")) {
				stylesheet = optionExtraPart;
			} else {
				stylesheet = "default.xsl";
			}
		} else if (option.equals("-xdocs")) {
			bugReporterType = XDOCS_REPORTER;
		} else if (option.equals("-quiet")) {
			quiet = true;
		} else if (option.equals("-nested")) {
			scanNestedArchives =
				optionExtraPart.equals("") || Boolean.valueOf(optionExtraPart).booleanValue();
		} else if (option.equals("-exitcode")) {
			setExitCode = true;
		} else if (option.equals("-noClassOk")) {
			noClassOk = true;
		} else if (option.equals("-xargs")) {
			xargs = true;
		} else if (option.equals("-uaSync")) {
			userAnnotationSync = true;
		} else {
			super.handleOption(option, optionExtraPart);
		}
	}

	@SuppressWarnings("DM_EXIT")
	@Override
	protected void handleOptionWithArgument(String option, String argument) throws IOException {
		if (option.equals("-outputFile") || option.equals("-output")) {
			File outputFile = new File(argument);
			String extension = Util.getFileExtension(outputFile);
			if (bugReporterType == PRINTING_REPORTER && (extension.equals("xml") || extension.equals("fba")))
					bugReporterType = XML_REPORTER;

			try {
				outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
			} catch (IOException e) {
				System.err.println("Couldn't open " + outputFile + " for output: " + e.toString());
				System.exit(1);
			}
		} else if (option.equals("-projectName")) {
			this.projectName = argument;
		} else if (option.equals("-release")) {
			this.releaseName = argument;
		} else if (option.equals("-sourceInfo")) {
			sourceInfoFile = argument;
		} else if (option.equals("-visitors") || option.equals("-omitVisitors")) {
			boolean omit = option.equals("-omitVisitors");

			if (!omit) {
				// Selecting detectors explicitly, so start out by
				// disabling all of them.  The selected ones will
				// be re-enabled.
				getUserPreferences().enableAllDetectors(false);
			}

			// Explicitly enable or disable the selected detectors.
			StringTokenizer tok = new StringTokenizer(argument, ",");
			while (tok.hasMoreTokens()) {
				String visitorName = tok.nextToken().trim();
				DetectorFactory factory = DetectorFactoryCollection.instance().getFactory(visitorName);
				if (factory == null)
					throw new IllegalArgumentException("Unknown detector: " + visitorName);
				getUserPreferences().enableDetector(factory, !omit);
			}
		} else if (option.equals("-chooseVisitors")) {
			// This is like -visitors and -omitVisitors, but
			// you can selectively enable and disable detectors,
			// starting from the default set (or whatever set
			// happens to be in effect).
			choose(argument, "Detector choices", new Chooser() {
				public void choose(boolean enabled, String what) {
					DetectorFactory factory = DetectorFactoryCollection.instance().getFactory(what);
					if (factory == null)
						throw new IllegalArgumentException("Unknown detector: " + what);
					if (FindBugs.DEBUG) {
						System.err.println("Detector " + factory.getShortName() + " " +
								(enabled ? "enabled" : "disabled") +
								", userPreferences="+System.identityHashCode(getUserPreferences()));
					}
					getUserPreferences().enableDetector(factory, enabled);
				}
			});
		} else if (option.equals("-choosePlugins")) {
			// Selectively enable/disable plugins
			choose(argument, "Plugin choices", new Chooser() {
				public void choose(boolean enabled, String what) {
					Plugin plugin = DetectorFactoryCollection.instance().getPluginById(what);
					if (plugin == null)
						throw new IllegalArgumentException("Unknown plugin: " + what);
					plugin.setEnabled(enabled);
				}
			});
		} else if (option.equals("-adjustPriority")) {
			// Selectively raise or lower the priority of warnings
			// produced by specified detectors.

			StringTokenizer tok = new StringTokenizer(argument, ",");
			while (tok.hasMoreTokens()) {
				String token = tok.nextToken();
				int eq = token.indexOf('=');
				if (eq < 0)
					throw new IllegalArgumentException("Illegal priority adjustment: " + token);

				String adjustmentTarget = token.substring(0, eq);
				String adjustment = token.substring(eq + 1);
				
				int adjustmentAmount;
				if (adjustment.equals("raise"))
					adjustmentAmount = -1;
				else if (adjustment.equals("lower"))
					adjustmentAmount = +1;
				else if (adjustment.equals("suppress"))
					adjustmentAmount = +100;
				else 
					throw new IllegalArgumentException("Illegal priority adjustment value: " +
							adjustment);

				
				DetectorFactory factory = DetectorFactoryCollection.instance().getFactory(adjustmentTarget);
				if (factory != null) 
					factory.setPriorityAdjustment(adjustmentAmount);
				else {
					// 
					I18N i18n = I18N.instance();
					BugPattern pattern = i18n.lookupBugPattern(adjustmentTarget);
					if (pattern == null)
						throw new IllegalArgumentException("Unknown detector: " + adjustmentTarget);
					pattern.adjustPriority(adjustmentAmount);
				}
			
		 
			}
		} else if (option.equals("-bugCategories")) {
			this.bugCategorySet = FindBugs.handleBugCategories(getUserPreferences(), argument);
		} else if (option.equals("-onlyAnalyze")) {
			// The argument is a comma-separated list of classes and packages
			// to select to analyze.  (If a list item ends with ".*",
			// it specifies a package, otherwise it's a class.)
			StringTokenizer tok = new StringTokenizer(argument, ",");
			while (tok.hasMoreTokens()) {
				String item = tok.nextToken();
				if (item.endsWith(".-"))
					classScreener.addAllowedPrefix(item.substring(0, item.length() - 1));
				else if (item.endsWith(".*"))
					classScreener.addAllowedPackage(item.substring(0, item.length() - 1));
				else
					classScreener.addAllowedClass(item);
			}
		} else if (option.equals("-exclude")) {
			excludeFilterFile.add(argument);
		} else if (option.equals("-excludeBugs")) {
			excludeBugFile.add(argument);
		} else if (option.equals("-include")) {
			includeFilterFile.add(argument);
		} else if (option.equals("-auxclasspath")) {
			StringTokenizer tok = new StringTokenizer(argument, File.pathSeparator);
			while (tok.hasMoreTokens())
				project.addAuxClasspathEntry(tok.nextToken());
		} else if (option.equals("-sourcepath")) {
			StringTokenizer tok = new StringTokenizer(argument, File.pathSeparator);
			while (tok.hasMoreTokens())
				project.addSourceDir(new File(tok.nextToken()).getAbsolutePath());
		} else if (option.equals("-uaPlugin")) {
			userAnnotationPlugin = argument;
		} else if (option.equals("-uaPluginProps")) {
			StringTokenizer t = new StringTokenizer(argument, ",");
			while (t.hasMoreTokens()) {
				String pair = t.nextToken();
				int eq = pair.indexOf('=');
				if (eq < 0) {
					throw new IllegalArgumentException("Bad user annotatin plugin configuration property: " + pair);
				}
				userAnnotationPluginProperties.put(pair.substring(0, eq), pair.substring(eq+1));
			}
		} else {
			super.handleOptionWithArgument(option, argument);
		}
	}

	/**
	 * Common handling code for -chooseVisitors and -choosePlugins options.
	 *
	 * @param argument the list of visitors or plugins to be chosen
	 * @param desc     String describing what is being chosen
	 * @param chooser  callback object to selectively choose list members
	 */
	private void choose(String argument, String desc, Chooser chooser) {
		StringTokenizer tok = new StringTokenizer(argument, ",");
		while (tok.hasMoreTokens()) {
			String what = tok.nextToken().trim();
			if (!what.startsWith("+") && !what.startsWith("-"))
				throw new IllegalArgumentException(desc + " must start with " +
						"\"+\" or \"-\" (saw " + what + ")");
			boolean enabled = what.startsWith("+");
			chooser.choose(enabled, what.substring(1));
		}
	}

	public void configureEngine(IFindBugsEngine findBugs) throws IOException, FilterException {
		// Load plugins
		DetectorFactoryCollection.instance().ensureLoaded();

		// Set the DetectorFactoryCollection (that has been configured
		// by command line parsing)
		findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());

		TextUIBugReporter textuiBugReporter;
		switch (bugReporterType) {
		case PRINTING_REPORTER:
			textuiBugReporter = new PrintingBugReporter();
			break;
		case SORTING_REPORTER:
			textuiBugReporter = new SortingBugReporter();
			break;
		case XML_REPORTER:
			{
				XMLBugReporter xmlBugReporter = new XMLBugReporter(project);
				xmlBugReporter.setAddMessages(xmlWithMessages);
				textuiBugReporter = xmlBugReporter;
			}
			break;
		case EMACS_REPORTER:
			textuiBugReporter = new EmacsBugReporter();
			break;
		case HTML_REPORTER:
			textuiBugReporter = new HTMLBugReporter(project, stylesheet);
			break;
		case XDOCS_REPORTER:
			textuiBugReporter = new XDocsBugReporter(project);
			break;
		default:
			throw new IllegalStateException();
		}

		if (quiet)
			textuiBugReporter.setErrorVerbosity(BugReporter.SILENT);

		textuiBugReporter.setPriorityThreshold(priorityThreshold);
		textuiBugReporter.setUseLongBugCodes(useLongBugCodes);

		if (outputStream != null)
			textuiBugReporter.setOutputStream(outputStream);

		BugReporter bugReporter = textuiBugReporter;

		if (bugCategorySet != null) {
			bugReporter = new CategoryFilteringBugReporter(bugReporter, bugCategorySet);
		}

		findBugs.setBugReporter(bugReporter);
		findBugs.setProject(project);
		
		if (showProgress) {
			findBugs.setProgressCallback(new TextUIProgressCallback(System.out));
		}

		findBugs.setUserPreferences(getUserPreferences());
		for(String s : excludeBugFile) 
	        try {
	            findBugs.excludeBaselineBugs(s);
            } catch (DocumentException e) {
	           throw new IOException("Unable to read " + excludeBugFile + ":" + e.getMessage());
            }
        for(String s : includeFilterFile) 
			findBugs.addFilter(s, true);
        for(String s : excludeFilterFile) 
			findBugs.addFilter(s, false);

		
		
		findBugs.setClassScreener(classScreener);

		findBugs.setRelaxedReportingMode(relaxedReportingMode);
		findBugs.setAbridgedMessages(xmlWithAbridgedMessages);

		if (trainingOutputDir != null) {
			findBugs.enableTrainingOutput(trainingOutputDir);
		}
		if (trainingInputDir != null) {
			findBugs.enableTrainingInput(trainingInputDir);
		}

		if (sourceInfoFile != null) {
			findBugs.setSourceInfoFile(sourceInfoFile);
		}

		findBugs.setAnalysisFeatureSettings(settingList);

		findBugs.setReleaseName(releaseName);
		findBugs.setProjectName(projectName);

		findBugs.setScanNestedArchives(scanNestedArchives);
		findBugs.setNoClassOk(noClassOk);

		if (userAnnotationPlugin != null) {
			if (!(findBugs instanceof IFindBugsEngine2)) {
				throw new IllegalStateException();
			}
			((IFindBugsEngine2)findBugs).loadUserAnnotationPlugin(userAnnotationPlugin, userAnnotationPluginProperties);
			((IFindBugsEngine2)findBugs).setUserAnnotationSync(userAnnotationSync);
		}
	}

	/**
	 * Handle -xargs command line option by reading
	 * jar file names from standard input and adding them
	 * to the project.
	 * 
	 * @throws IOException
	 */
	public void handleXArgs() throws IOException {
		if (getXargs()) {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				String s = in.readLine();
				if (s == null) break;
				project.addFile(s);
			}
		}
	}

	/**
	 * @param userPreferences The userPreferences to set.
	 */
	private void setUserPreferences(UserPreferences userPreferences) {
		this.userPreferences = userPreferences;
	}

	/**
	 * @return Returns the userPreferences.
	 */
	private UserPreferences getUserPreferences() {
		if (userPreferences == null) 
			userPreferences = UserPreferences.createDefaultUserPreferences();
		return userPreferences;
	}
}
