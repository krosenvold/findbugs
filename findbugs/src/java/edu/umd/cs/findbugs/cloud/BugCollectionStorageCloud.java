/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.cloud;

import java.util.Collection;
import java.util.Collections;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.username.NoNameLookup;


/**
 * @author pwilliam
 */
 class BugCollectionStorageCloud extends AbstractCloud {
	
	BugCollectionStorageCloud(CloudPlugin plugin, BugCollection bc) {
			super(plugin, bc);
	}
	
	BugCollectionStorageCloud(BugCollection bc) {
		this(getFallbackPlugin(), bc);
	}

	private  static CloudPlugin getFallbackPlugin() {
		 return new CloudPlugin("fallback local cloud", BugCollectionStorageCloud.class.getClassLoader(),
				 BugCollectionStorageCloud.class, NoNameLookup.class, new PropertyBundle(), "no description", "no details");
	 }
	public Mode getMode() {
	    return Mode.COMMUNAL;
    }

	public String getUser() {
	    // TODO Auto-generated method stub
	    return null;
    }

	public UserDesignation getUserDesignation(BugInstance b) {
	    BugDesignation bd = b.getUserDesignation();
	    if (bd == null) return UserDesignation.UNCLASSIFIED;
	    return UserDesignation.valueOf(bd.getDesignationKey());
    }

	public String getUserEvaluation(BugInstance b) {
    	BugDesignation bd = b.getUserDesignation();
  	    if (bd == null) return "";
  	    return bd.getAnnotationText();
    }

	public long getUserTimestamp(BugInstance b) {
    	BugDesignation bd = b.getUserDesignation();
  	    if (bd == null) return Long.MAX_VALUE;
  	    return bd.getTimestamp();
    }

	public void setMode(Mode m) {
	    // TODO Auto-generated method stub
    }

    public void bugsPopulated() {
	    assert true;
	    
    }

	public boolean availableForInitialization() {
		return true;
	}
    public boolean initialize() {
	    return true;
	    
    }

    public void storeUserAnnotation(BugInstance bugInstance) {
	    // TODO Auto-generated method stub
	    
    }

    public void bugFiled(BugInstance b, Object bugLink) {
    	 throw new UnsupportedOperationException();
	    
    }

    public BugDesignation getPrimaryDesignation(BugInstance b) {
    	return  b.getUserDesignation();
    }

    @Override
    protected Iterable<BugDesignation> getAllUserDesignations(BugInstance bd) {
	    return Collections.emptyList();
    }

    public Collection<String> getProjects(String className) {
	    return Collections.emptyList();
    }
}
