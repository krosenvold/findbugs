/* 
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003, Peter Friese
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
 
package de.tobject.findbugs;

import java.io.IOException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.tobject.findbugs.builder.AbstractFilesCollector;
import de.tobject.findbugs.builder.FindBugsBuilder;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.nature.FindBugsNature;
import de.tobject.findbugs.reporter.Reporter;

/**
 * The main plugin class to be used in the desktop.
 */
public class FindbugsPlugin extends AbstractUIPlugin {
	
	/** Controls debugging of the plugin */
	public static boolean DEBUG;
	
	/**
	 * The plug-in identifier of the FindBugs Plug-in
	 * (value <code>"de.tobject.findbugs"</code>).
	 */
	public static final String PLUGIN_ID = "de.tobject.findbugs" ;

	/**
	 * The identifier for the FindBugs builder
	 * (value <code>"de.tobject.findbugs.findbugsbuilder"</code>).
	 */
	public static final String BUILDER_ID = PLUGIN_ID + ".findbugsBuilder" ;
	
	/**
	 * The identifier for the FindBugs nature
	 * (value <code>"de.tobject.findbugs.findbugsnature"</code>).
	 *
	 * @see org.eclipse.core.resources.IProject#hasNature(java.lang.String)
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".findbugsNature" ;
	
	// Debugging options
	private static final String PLUGIN_DEBUG = PLUGIN_ID + "/debug/plugin";
	private static final String WORKER_DEBUG = PLUGIN_ID + "/debug/worker";
	private static final String BUILDER_DEBUG = PLUGIN_ID + "/debug/builder";
	private static final String MARKER_DEBUG = PLUGIN_ID + "/debug/marker" ;	
	private static final String NATURE_DEBUG = PLUGIN_ID + "/debug/nature";
	private static final String PROPERTIES_DEBUG = PLUGIN_ID + "/debug/properties";
	private static final String REPORTER_DEBUG = PLUGIN_ID + "/debug/reporter";
	private static final String UTIL_DEBUG = PLUGIN_ID + "/debug/util";
	private static final String VISITOR_DEBUG = PLUGIN_ID + "/debug/visitor" ;
	
	// The shared instance.
	private static FindbugsPlugin plugin;
	
	// Resource bundle.
	private ResourceBundle resourceBundle;
	
	/**
	 * The constructor.
	 */
	public FindbugsPlugin(IPluginDescriptor descriptor) {
		// basic initialization
		super(descriptor);
		plugin = this;
		
		// configure debugging
		configurePluginDebugOptions();
		
		// initialize resource strings
		try {
			resourceBundle= ResourceBundle.getBundle("de.tobject.findbugs.FindbugsPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * Returns the shared instance.
	 */
	public static FindbugsPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	public static IWorkbench getActiveWorkbench() {
		FindbugsPlugin plugin= getDefault();
		if (plugin == null) {
			return null;
		}
		return plugin.getWorkbench();
	}
	
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		IWorkbench workbench= getActiveWorkbench();
		if (workbench == null) {
			return null;	
		}
		return workbench.getActiveWorkbenchWindow();
	}
	
	/**
	 * Returns the SWT Shell of the active workbench window or <code>null</code> if
	 * no workbench window is active.
	 *
	 * @return the SWT Shell of the active workbench window, or <code>null</code> if
	 * 	no workbench window is active
	 */
	public static Shell getShell() {
		IWorkbenchWindow window= getActiveWorkbenchWindow();
		if (window == null) {
			return null;
		}
		return window.getShell();
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle= FindbugsPlugin.getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	public void configurePluginDebugOptions() {
		if (isDebugging()) {
			
			// debugging for the plugin itself
			String option = Platform.getDebugOption(PLUGIN_DEBUG);
			if (option != null) {
				FindbugsPlugin.DEBUG = option.equalsIgnoreCase("true");
			}
			
			// debugging for the builder and friends
			option = Platform.getDebugOption(BUILDER_DEBUG);
			if (option != null) {
				FindBugsBuilder.DEBUG = option.equalsIgnoreCase("true");
				AbstractFilesCollector.DEBUG = FindBugsBuilder.DEBUG;
				FindBugsWorker.DEBUG = FindBugsBuilder.DEBUG;
			}

			// debugging for the nature
			option = Platform.getDebugOption(NATURE_DEBUG);
			if (option != null) {
				FindBugsNature.DEBUG = option.equalsIgnoreCase("true");
			}

			// debugging for the reporter
			option = Platform.getDebugOption(REPORTER_DEBUG);
			if (option != null) {
				Reporter.DEBUG = option.equalsIgnoreCase("true");
			}
			
		}
	}

	public static String getFindBugsEnginePluginLocation() {
		//		URL u = FindbugsPlugin.getDefault().getDescriptor().getInstallURL();
		Plugin plugin = Platform.getPlugin("edu.umd.cs.findbugs");
		if (plugin != null) {
			if (FindbugsPlugin.DEBUG) {
				System.out.println("Found the findbugs binaries.");
			}
			URL u = plugin.getDescriptor().getInstallURL();
			try {
				// this gets a file://... url for the plugin
				URL u2 = Platform.resolve(u);
				// convert to real path
				String pluginPath = u2.getPath();
				if (FindBugsBuilder.DEBUG) {
					System.out.println("Pluginpath: " + pluginPath);
				}
				return pluginPath;
			}
			catch (RuntimeException e) {
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (FindBugsBuilder.DEBUG) {
			System.out.println("Could not find findbugs binaries.");
		}
		return null;
	}
}
