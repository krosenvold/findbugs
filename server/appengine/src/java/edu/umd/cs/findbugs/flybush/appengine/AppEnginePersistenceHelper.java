package edu.umd.cs.findbugs.flybush.appengine;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import edu.umd.cs.findbugs.flybush.DbClientVersionStats;
import edu.umd.cs.findbugs.flybush.DbEvaluation;
import edu.umd.cs.findbugs.flybush.DbInvocation;
import edu.umd.cs.findbugs.flybush.DbIssue;
import edu.umd.cs.findbugs.flybush.DbPluginUpdateXml;
import edu.umd.cs.findbugs.flybush.DbUsageEntry;
import edu.umd.cs.findbugs.flybush.DbUsageSummary;
import edu.umd.cs.findbugs.flybush.DbUser;
import edu.umd.cs.findbugs.flybush.PersistenceHelper;

public class AppEnginePersistenceHelper extends PersistenceHelper {
    private static final Logger LOGGER = Logger.getLogger(AppEnginePersistenceHelper.class.getName());
    private Random random = new Random();

    @Override
    public PersistenceManagerFactory getPersistenceManagerFactory() {
        return PMF.get();
    }

    @Override
    public PersistenceManager getPersistenceManager() {
        return getPersistenceManagerFactory().getPersistenceManager();
    }

    @Override
    public AppEngineDbUser createDbUser(String openidUrl, String email) {
        return new AppEngineDbUser(openidUrl, email);
    }

    @Override
    public AppEngineSqlCloudSession createSqlCloudSession(long id, Date date, Object userKey, String email) {
        return new AppEngineSqlCloudSession((Key) userKey, id, email, date);
    }

    @Override
    public Class<? extends DbUser> getDbUserClass() {
        return AppEngineDbUser.class;
    }

    @Override
    public DbInvocation createDbInvocation() {
        return new AppEngineDbInvocation();
    }

    @Override
    public Class<AppEngineSqlCloudSession> getSqlCloudSessionClass() {
        return AppEngineSqlCloudSession.class;
    }

    @Override
    public DbUsageEntry createDbUsageEntry() {
        return new AppEngineDbUsageEntry();
    }

    @Override
    public DbPluginUpdateXml createPluginUpdateXml(String value) {
        return new AppEngineDbPluginUpdateXml(value);
    }

    @Override
    public DbPluginUpdateXml createPluginUpdateXml() {
        return new AppEngineDbPluginUpdateXml();
    }

    @Override
    public DbUsageSummary createDbUsageSummary() {
        return new AppEngineDbUsageSummary();
    }

    @Override
    public int clearAllData() {
        int deleted = 0;
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pquery = ds.prepare(new Query().setKeysOnly());
        for (Entity entity : pquery.asIterable()) {
            ds.delete(entity.getKey());
            deleted++;
        }
        return deleted;
    }

    @Override
    public AppEngineDbIssue createDbIssue() {
        return new AppEngineDbIssue();
    }

    @Override
    public Class<AppEngineDbInvocation> getDbInvocationClass() {
        return AppEngineDbInvocation.class;
    }

    @Override
    public AppEngineDbEvaluation createDbEvaluation() {
        return new AppEngineDbEvaluation();
    }

    @Override
    public DbClientVersionStats createDbClientVersionStats(String application, String version, long dayStart) {
        return new AppEngineDbClientVersionStats(application, version, dayStart);
    }

    @Override
    public Class<AppEngineDbIssue> getDbIssueClass() {
        return AppEngineDbIssue.class;
    }

    @Override
    public Class<AppEngineDbEvaluation> getDbEvaluationClass() {
        return AppEngineDbEvaluation.class;
    }

    @Override
    public Class<? extends DbClientVersionStats> getDbClientVersionStatsClass() {
        return AppEngineDbClientVersionStats.class;
    }

    @Override
    public Class<? extends DbUsageEntry> getDbUsageEntryClass() {
        return AppEngineDbUsageEntry.class;
    }

    @Override
    public Class<? extends DbPluginUpdateXml> getDbPluginUpdateXmlClass() {
        return AppEngineDbPluginUpdateXml.class;
    }

    @Override
    public Class<? extends DbUsageSummary> getDbUsageSummaryClass() {
        return AppEngineDbUsageSummary.class;
    }

    @Override
    public <E> E getObjectById(PersistenceManager pm, Class<? extends E> cls, Object key) {
        return pm.getObjectById(cls, key);
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public Map<String, DbIssue> findIssues(PersistenceManager pm, Iterable<String> hashes) {
        javax.jdo.Query query = pm
                .newQuery("select hash, firstSeen, lastSeen, bugLink, bugLinkType, hasEvaluations, evaluations from "
                        + getDbIssueClassname() + " where :hashes.contains(hash)");
        List<Object[]> results = (List<Object[]>) query.execute(hashes);
        Map<String, DbIssue> map = new HashMap<String, DbIssue>();
        for (Object[] result : results) {
            DbIssue issue = createDbIssue();
            issue.setHash((String) result[0]);
            issue.setFirstSeen((Long) result[1]);
            issue.setLastSeen((Long) result[2]);
            issue.setBugLink((String) result[3]);
            try {
                String linkType = (String) result[4];
                if (linkType != null) {
                    issue.setBugLinkType(linkType);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "Error parsing issue " + issue.getHash(), e);
            }
            issue.setHasEvaluations((Boolean) result[5]);
            issue.setEvaluationsDontLook((Set<DbEvaluation>) result[6]);
            map.put(issue.getHash(), issue);
        }
        return map;
    }

    @Override
    public void convertToOldCommentStyleForTesting(DbEvaluation eval) {
        AppEngineDbEvaluation aede = (AppEngineDbEvaluation) eval;
        aede.setShortComment(aede.getLongComment().getValue());
        aede.setLongComment(null);
    }

    @Override
    public boolean convertToNewCommentStyle(DbEvaluation eval) {
        return ((AppEngineDbEvaluation) eval).convertToNewCommentStyle();
    }

    /**
     * Uses App Engine's memcache implementation to determine whether a
     * given user's client version data has been logged today.
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public boolean shouldRecordClientStats(String ip, String appName, String appVer, long midnightToday) {
        Cache cache;

        try {
            cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
        } catch (CacheException e) {
            LOGGER.log(Level.WARNING, "memcache could not be initialized", e);
            return true; // to be safe
        }

        String id = "already-recorded-" + ip + "-" + appName + "-" + appVer + "-" + midnightToday;
        Boolean val = (Boolean) cache.get(id);
        if (val != null && val.equals(Boolean.TRUE)) // has already been recorded today
            return false;
        cache.put(id, Boolean.TRUE);
        return true;
    }

    @Override
    public void addToQueue(String url, Map<String, String> params) {
        Queue queue = QueueFactory.getDefaultQueue();
        TaskOptions taskOptions = withUrl(url);
        //[a-zA-Z\d_-]
        taskOptions.taskName((url + "--" + new TreeMap<String, String>(params).toString()).replaceAll("[^a-zA-Z\\d_-]", "_") + "__" + random.nextInt());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            taskOptions.param(entry.getKey(), entry.getValue());
        }
        queue.add(taskOptions);
    }

    @Override
    public String getEmail(PersistenceManager pm, Comparable<?> who) {
        // noinspection RedundantCast
        return pm.getObjectById(getDbUserClass(), (Key) who).getEmail();
    }

    @Override
    public String getEmailOfCurrentAppengineUser() {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user == null)
            return null;
        return user.getEmail();
    }

    @Override
    public boolean isOldCommentStyle(DbEvaluation eval) {
        return ((AppEngineDbEvaluation) eval).getShortComment() != null;
    }
}
