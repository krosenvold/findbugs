package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.util.ServiceException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;

public class BugFilingHelper {
    private static final Logger LOGGER = Logger.getLogger(BugFilingHelper.class.getName());

    private final AppEngineCloudClient appEngineCloudClient;
    private final String trackerUrl;
    
    private final BugFiler bugFiler;
    
    public BugFilingHelper(AppEngineCloudClient appEngineCloudClient, PropertyBundle properties) {
        this.appEngineCloudClient = appEngineCloudClient;
        this.trackerUrl = properties.getProperty("cloud.bugTrackerUrl");
        String bugTrackerType = properties.getProperty("cloud.bugTrackerType");
        BugFiler filer;
        if ("GOOGLE_CODE".equals(bugTrackerType)) 
        	filer = new GoogleCodeBugFiler(appEngineCloudClient, trackerUrl);
        else if ("JIRA".equals(bugTrackerType))
            filer = new JiraBugFiler(appEngineCloudClient, trackerUrl);
        else
            filer = null;
        this.bugFiler = filer;
        
    }

    public String lookupBugStatus(final BugInstance b) {

        if (appEngineCloudClient.getBugLinkStatus(b) == Cloud.BugFilingStatus.FILE_BUG)
            return null;

        String status;

        final String bugLink = appEngineCloudClient.getBugLink(b).toExternalForm();
        appEngineCloudClient.getBackgroundExecutor().execute(new Runnable() {
            public void run() {
                String status = null;
                try {
                    status = bugFiler.getBugStatus(bugLink);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while connecting to bug tracker", e);
                }
                if (status == null)
                    status = "<unknown>";
                appEngineCloudClient.updateBugStatusCache(b, status);
            }
        });
        status = "<loading...>";
        appEngineCloudClient.updateBugStatusCache(b, status);
        return status;
    }

	public URL fileBug(BugInstance b)
            throws javax.xml.rpc.ServiceException, IOException, SignInCancelledException, OAuthException,
                   InterruptedException, ServiceException {
		
		return bugFiler.file(b);

    }

    public boolean bugFilingAvailable() {
		return bugFiler != null && trackerUrl != null;
	}

    // ============================== end of public methods ==============================

}