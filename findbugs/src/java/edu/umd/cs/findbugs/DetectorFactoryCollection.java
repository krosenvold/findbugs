/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import edu.umd.cs.findbugs.util.ClassPathUtil;

/**
 * The DetectorFactoryCollection stores all of the DetectorFactory objects
 * used to create the Detectors which implement the various analyses.
 * It is a singleton class.
 *
 * @author David Hovemeyer
 * @see DetectorFactory
 */
public class DetectorFactoryCollection {
	private HashMap<String, Plugin> pluginByIdMap = new HashMap<String, Plugin>();
	private ArrayList<DetectorFactory> factoryList = new ArrayList<DetectorFactory>();
	private HashMap<String, DetectorFactory> factoriesByName = new HashMap<String, DetectorFactory>();
	private HashMap<String, DetectorFactory> factoriesByDetectorClassName =
		new HashMap<String, DetectorFactory>();

	private static DetectorFactoryCollection theInstance;
	private static final Object lock = new Object();
	private boolean loaded = false;

	private URL[] pluginList;

	/**
	 * Constructor.
	 * loadPlugins() method must be called before
	 * any detector factories can be accessed.
	 */
	DetectorFactoryCollection() {
	}

	/**
	 * Set the list of plugins to load explicitly.
	 * This must be done before the instance of DetectorFactoryCollection
	 * is created.
	 *
	 * @param pluginList list of plugin Jar files to load
	 */
	public void setPluginList(URL[] pluginList) {
		if (loaded) throw new IllegalStateException();
		this.pluginList = new URL[pluginList.length];
		System.arraycopy(pluginList, 0, this.pluginList, 0, pluginList.length);
	}

	/**
	 * Set the instance that should be retured as the singleton instance.
	 * 
	 * @param instance the singleton instance to be set
	 */
	static void setInstance(DetectorFactoryCollection instance) {
		synchronized (lock) {
			if (theInstance != null) {
				throw new IllegalStateException();
			}
			theInstance = instance;
		}
	}
	
	static void resetInstance(DetectorFactoryCollection instance) {
		synchronized (lock) {
			theInstance = instance;
		}
	}

	/**
	 * Get the single instance of DetectorFactoryCollection.
	 */
	public static DetectorFactoryCollection instance() {
		synchronized (lock) {
			if (theInstance == null) {
				theInstance = new DetectorFactoryCollection();
			}
			theInstance.ensureLoaded();
			return theInstance;
		}
	}
	/**
	 * Get the single instance of DetectorFactoryCollection.
	 */
	public static DetectorFactoryCollection rawInstance() {
		synchronized (lock) {
			if (theInstance == null) {
				theInstance = new DetectorFactoryCollection();
			}
			return theInstance;
		}
	}
	/**
	 * Return an Iterator over all available Plugin objects.
	 */
	public Iterator<Plugin> pluginIterator() {
		ensureLoaded();
		return pluginByIdMap.values().iterator();
	}
	/**
	 * Return an Iterable of all available Plugin objects.
	 */
	public Iterable<Plugin> plugins() {
		ensureLoaded();
		return pluginByIdMap.values();
	}

	/**
	 * Get a Plugin by its unique id.
	 *
	 * @param pluginId the unique id
	 * @return the Plugin with that id, or null if no such Plugin is found
	 */
	public Plugin getPluginById(String pluginId) {
		ensureLoaded();
		return pluginByIdMap.get(pluginId);
	}

	/**
	 * Return an Iterator over the DetectorFactory objects for all
	 * registered Detectors.
	 */
	public Iterator<DetectorFactory> factoryIterator() {
		ensureLoaded();
		return factoryList.iterator();
	}

	/**
	 * Look up a DetectorFactory by its short name.
	 *
	 * @param name the short name
	 * @return the DetectorFactory, or null if there is no factory with that short name
	 */
	public DetectorFactory getFactory(String name) {
		ensureLoaded();
		return factoriesByName.get(name);
	}

	/**
	 * Look up a DetectorFactory by its class name.
	 * 
	 * @param className the class name
	 * @return the DetectoryFactory, or null if there is no factory with
	 *         that class name
	 */
	public DetectorFactory getFactoryByClassName(String className) {
		ensureLoaded();
		return factoriesByDetectorClassName.get(className);
	}

	/**
	 * Register a DetectorFactory.
	 */
	void registerDetector(DetectorFactory factory) {
		if (FindBugs.DEBUG) System.out.println("Registering detector: " + factory.getFullName());
		String detectorName = factory.getShortName();
		factoryList.add(factory);
		factoriesByName.put(detectorName, factory);
		factoriesByDetectorClassName.put(factory.getFullName(), factory);
	}

	private void determinePlugins() {
		if (pluginList != null)
			return;
			
		String homeDir = getFindBugsHome();
		
		if (homeDir == null) {
			
			// Since findbugs.home isn't set, we won't attempt
			// to look for third-party plugins.
			pluginList = new URL[0];
			return;
		}

		//
		// See what plugins are available in the ${findbugs.home}/plugin directory
		//
		
		File pluginDir = new File(homeDir + File.separator + "plugin");
		File[] contentList = pluginDir.listFiles();
		if (contentList == null) {
			pluginList = new URL[0];
			return;
		}

		ArrayList<URL> arr = new ArrayList<URL>();
		for (File aContentList : contentList) {
			if (aContentList.getName().endsWith(".jar")) {

				try {
					arr.add(aContentList.toURL());
					if (FindBugs.DEBUG)
						System.out.println("Found plugin: " + aContentList.toString());
				} catch (MalformedURLException e) {

				}

			}
		}
		pluginList = arr.toArray(new URL[arr.size()]);

	}
	
	private static final Pattern[] findbugsJarNames = {
		Pattern.compile("findbugs\\.jar$"),
	};

	/**
	 * See if the location of ${findbugs.home} can be
	 * inferred from the location of findbugs.jar in the classpath.
	 * 
	 * @return inferred ${findbugs.home}, or null if 
	 *         we can't figure it out
	 */
	private static String inferFindBugsHome() {
		for (Pattern jarNamePattern : findbugsJarNames) {
			String findbugsJarCodeBase =
				ClassPathUtil.findCodeBaseInClassPath(jarNamePattern, SystemProperties.getProperty("java.class.path"));
			if (findbugsJarCodeBase != null) {
				File findbugsJar = new File(findbugsJarCodeBase);
				File libDir = findbugsJar.getParentFile();
				if (libDir.getName().equals("lib")) {
					String fbHome = libDir.getParent();
					FindBugs.setHome(fbHome);
					return fbHome;
				}
			}
		}
		return null;
		
	}

	public static String getFindBugsHome() {
		
		String homeDir = FindBugs.getHome();
		
		if (homeDir == null) {
			// Attempt to infer findbugs.home from the observed
			// location of findbugs.jar.
			homeDir = inferFindBugsHome();
		}
		return homeDir;

	}
	public void ensureLoaded() {
		if (loaded) return;
		loadPlugins();
	}
	
	/**
	 * Directly set the collection of Plugins from which to load DetectorFactories.
	 * May be called instead of loadPlugins().
	 * 
	 * @param plugins array of Plugins to register
	 */
	void setPlugins(Plugin[] plugins) {
		if (loaded) {
			throw new IllegalStateException();
		}
		for (Plugin plugin : plugins) {
			pluginByIdMap.put(plugin.getPluginId(), plugin);
		}
		loaded = true;
	}
	
	/**
	 * Load all plugins. If a setPluginList() has been called, then those
	 * plugins are loaded. Otherwise, the "findbugs.home" property is checked to
	 * determine where FindBugs is installed, and the plugin files are
	 * dynamically loaded from the plugin directory.
	 */

	
	
	void loadPlugins() {
		if (loaded) throw new IllegalStateException();
		//If we are running under jaws, just use the loaded plugin
		if (SystemProperties.getBoolean("findbugs.jaws")) {
			URL u = DetectorFactoryCollection.class.getResource("/findbugs.xml");
			String message = "Loading plugin from " + u;
			jawsDebugMessage(message);
			String findbugsJarCodeBase =
				ClassPathUtil.findCodeBaseInClassPath(findbugsJarNames[0], SystemProperties.getProperty("java.class.path"));
			jawsDebugMessage( "findBugs jar code base = " + findbugsJarCodeBase);
			List<URL> plugins = new ArrayList<URL>();
			int i = 1;
			while (true) {
				String plugin = SystemProperties.getProperty("findbugs.jaws.plugin"+i);
				if (plugin == null) 
					break;
				try {
				URL url = new URL(plugin);
				URLConnection urlConnection = url.openConnection();
				String type = urlConnection.getContentType();
				jawsDebugMessage( "plugin " + plugin + " has type " + type);
				plugins.add(url);
				} catch (Exception e) {
					jawsDebugMessage( "Unable to load plugin " + plugin);
				}
				i++;
			}
			setPluginList(plugins.toArray(new URL[plugins.size()]));
		}

		// Load all detector plugins.
		loaded = true;
		determinePlugins();

		//
		// Load the core plugin.
		//
		PluginLoader corePluginLoader = new PluginLoader();
		try {
			loadPlugin(corePluginLoader);
		} catch (PluginException e) {
			throw new IllegalStateException("Warning: could not load FindBugs core plugin: " + e.toString(), e);
		}
		
		//
		// Load any discovered third-party plugins.
		//
		for (final URL url : pluginList) {
			try {
				if (FindBugs.DEBUG) System.out.println("Loading plugin: " + url.toString());
				PluginLoader pluginLoader =
					AccessController.doPrivileged(new PrivilegedExceptionAction<PluginLoader>() {

						public PluginLoader run() throws PluginException {
							return	new PluginLoader(url, this.getClass().getClassLoader());
						}

					});
				loadPlugin(pluginLoader);
			} catch (PluginException e) {
				System.err.println("Warning: could not load plugin " + url + ": " + e.toString());
				if (FindBugs.DEBUG)
					e.printStackTrace();
			} catch (PrivilegedActionException e) {
				System.err.println("Warning: could not load plugin " + url + ": " + e.toString());
				if (FindBugs.DEBUG)
					e.printStackTrace();
			}
		}


		//System.out.println("Loaded " + numLoaded + " plugins");
	}

	final static boolean DEBUG_JAWS = false;
	/**
     * @param message
     */
	
	
    private void jawsDebugMessage(String message) {
	    if (DEBUG_JAWS)
	    	JOptionPane.showMessageDialog(null, message);
    }

	private void loadPlugin(PluginLoader pluginLoader) throws PluginException {


		Plugin plugin = pluginLoader.getPlugin();
		pluginByIdMap.put(plugin.getPluginId(), plugin);

		// Register all of the detectors that this plugin contains
		for (Iterator<DetectorFactory> j = plugin.detectorFactoryIterator(); j.hasNext();) {
			DetectorFactory factory = j.next();
			registerDetector(factory);
		}

		I18N i18n = I18N.instance();

		// Register the BugPatterns
		for (Iterator<BugPattern> j = plugin.bugPatternIterator(); j.hasNext();) {
			BugPattern bugPattern = j.next();
			i18n.registerBugPattern(bugPattern);
		}

		// Register the BugCodes
		for (Iterator<BugCode> j = plugin.bugCodeIterator(); j.hasNext();) {
			BugCode bugCode = j.next();
			i18n.registerBugCode(bugCode);
		}
	}
}

// vim:ts=4
