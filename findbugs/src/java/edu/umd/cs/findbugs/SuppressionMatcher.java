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

import java.util.*;

public class SuppressionMatcher implements Matcher {
	private HashMap <ClassAnnotation, Collection<WarningSuppressor>> suppressedWarnings
			= new HashMap();
	private HashMap <String, Collection<WarningSuppressor>> 
		suppressedPackageWarnings
			= new HashMap();
	int count = 0;

	public void addPackageSuppressor(PackageWarningSuppressor suppressor) {
		String packageName = suppressor.packageName;

		Collection<WarningSuppressor> c =  suppressedPackageWarnings.get(packageName);
		if (c == null) {
			c = new LinkedList<WarningSuppressor>();
			suppressedPackageWarnings.put(packageName,c);
			}
			c.add(suppressor);
		}
		
	public void addSuppressor(ClassWarningSuppressor suppressor) {
		ClassAnnotation clazz = suppressor.clazz;
		Collection<WarningSuppressor> c =  suppressedWarnings.get(clazz);
		if (c == null) {
			c = new LinkedList<WarningSuppressor>();
			suppressedWarnings.put(clazz,c);
			}
			c.add(suppressor);
	}
	public int count() {
		return count;
		}
	public boolean match(BugInstance b) {
		ClassAnnotation clazz = b.getPrimaryClass();
		Collection<WarningSuppressor> c = suppressedWarnings.get(clazz);
		if (c != null) 
		for(WarningSuppressor w : c) 
			if (w.match(b)) {
				count++;
				return true;
				}
		for(Collection<WarningSuppressor> c2 : suppressedPackageWarnings.values())
		  for(WarningSuppressor w : c2) {
			if (w.match(b)) {
				count++;
				return true;
				}
			}
		return false;
		}
		

}

// vim:ts=4
