package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractFlybushServlet extends HttpServlet {
    protected static final Logger LOGGER = Logger.getLogger(AbstractFlybushServlet.class.getName());

    protected PersistenceHelper persistenceHelper;
    private JspHelper jspHelper = new JspHelper();

    /** for testing */
    void setPersistenceHelper(PersistenceHelper persistenceHelper) {
        this.persistenceHelper = persistenceHelper;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String helperCls = config.getServletContext().getInitParameter("edu.umd.cs.findbugs.flybush.persistenceHelper");
        try {
            persistenceHelper = (PersistenceHelper) Class.forName(helperCls).newInstance();
        } catch (Exception e) {
            throw new ServletException("Couldn't load persistence helper " + helperCls, e);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        PersistenceManager pm = getPersistenceManager();

        try {
            handlePost(pm, req, resp, uri);
        } finally {
            pm.close();
        }
    }

    protected abstract void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException;

    protected void show404(HttpServletResponse resp) throws IOException {
        setResponse(resp, 404, "Not Found");
    }

    protected void setResponse(HttpServletResponse resp, int statusCode, String textResponse) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain");
        resp.getWriter().println(textResponse);
    }

    protected String getCloudName() {
        return jspHelper.getCloudName();
    }

    @SuppressWarnings("unchecked")
    protected SqlCloudSession lookupCloudSessionById(long id, PersistenceManager pm) {
        Query query = pm.newQuery("select from " + persistenceHelper.getSqlCloudSessionClassname()
                + " where randomID == :randomIDquery");
        List<SqlCloudSession> sessions = (List<SqlCloudSession>) query.execute(Long.toString(id));
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    protected PersistenceManager getPersistenceManager() throws IOException {
        return persistenceHelper.getPersistenceManager();
    }

    protected static LinkedList<DbEvaluation> sortAndFilterEvaluations(Set<? extends DbEvaluation> origEvaluations) {
        Set<String> seenUsernames = new HashSet<String>();
        List<DbEvaluation> evaluationsList = new ArrayList<DbEvaluation>(origEvaluations);
        Collections.sort(evaluationsList);
        int numEvaluations = evaluationsList.size();
        LinkedList<DbEvaluation> result = new LinkedList<DbEvaluation>();
        for (ListIterator<DbEvaluation> it = evaluationsList.listIterator(numEvaluations); it.hasPrevious();) {
            DbEvaluation dbEvaluation = it.previous();
            boolean userIsNew = seenUsernames.add(dbEvaluation.getWhoId());
            if (userIsNew) {
                result.add(0, dbEvaluation);
            }
        }
        return result;
    }

    /** for testing */
    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }
}
