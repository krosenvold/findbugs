/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

/**
 * Version number and release date information.
 */
public class Version {
	/** Major version number. */
	public static final int MAJOR = 0;

	/** Minor version number. */
	public static final int MINOR = 8;

	/** Patch level. */
	public static final int PATCHLEVEL = 0;

	/** Development version? */
	public static final boolean IS_DEVELOPMENT = false;

	/** Release version string. */
	public static final String RELEASE = MAJOR + "." + MINOR + "." + PATCHLEVEL + (IS_DEVELOPMENT ? "-dev" : "");

	/** Release date. */
	public static final String DATE = "June 24, 2004";

	/**
	 * Version of Eclipse "core" plugin, which is just a repackaged
	 * form of the main FindBugs application.  This is slightly
	 * different than the RELEASE constant because Eclipse doesn't
	 * like the "-" character in plugin versions.
	 */
	public static final String ECLIPSE_CORE_VERSION = MAJOR + "." + MINOR + "." +
		PATCHLEVEL + (IS_DEVELOPMENT ? ".dev" : "");

	/** Version of Eclipse UI plugin. */
	public static final String ECLIPSE_UI_VERSION = "0.0.7" + (IS_DEVELOPMENT ? ".dev" : "");

	/** FindBugs website. */
	public static final String WEBSITE = "http://findbugs.sourceforge.net";

	/** Downloads website. */
	public static final String DOWNLOADS_WEBSITE = "http://prdownloads.sourceforge.net/findbugs";

	/** Support email. */
	public static final String SUPPORT_EMAIL = "daveho@cs.umd.edu";

	public static void main(String[] argv) {
		if (argv.length != 1)
			usage();

		String arg = argv[0];

		if (arg.equals("-release"))
			System.out.println(RELEASE);
		else if (arg.equals("-date"))
			System.out.println(DATE);
		else if (arg.equals("-props")) {
			System.out.println("release.number="+RELEASE);
			System.out.println("release.date="+DATE);
			System.out.println("eclipse.core.version="+ECLIPSE_CORE_VERSION);
			System.out.println("eclipse.ui.version="+ECLIPSE_UI_VERSION);
			System.out.println("findbugs.website="+WEBSITE);
			System.out.println("findbugs.downloads.website="+DOWNLOADS_WEBSITE);
		} else
			usage();
	}

	private static void usage() {
		System.err.println("Usage: " + Version.class.getName() + 
                       "  (-release|-date|-props)");
		System.exit(1);
	}
}

// vim:ts=4
