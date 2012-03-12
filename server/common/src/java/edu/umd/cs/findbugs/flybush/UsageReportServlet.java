package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.googlecode.charts4j.AxisLabelsFactory;
import com.googlecode.charts4j.BarChart;
import com.googlecode.charts4j.BarChartPlot;
import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Data;
import com.googlecode.charts4j.DataEncoding;
import com.googlecode.charts4j.GChart;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.LegendPosition;
import com.googlecode.charts4j.Line;
import com.googlecode.charts4j.LineChart;
import com.googlecode.charts4j.Plots;
import org.apache.commons.lang.StringEscapeUtils;

public class UsageReportServlet extends AbstractFlybushServlet {
    public static final Pattern DEV_VERSION_REGEX = Pattern.compile("(.*-dev-(\\d{6})).*");
    private int form_id = 0;

    private static  SimpleDateFormat DF_M_D_Y() {
        return new SimpleDateFormat("M/d/yy", Locale.ENGLISH);
    }

    private static  SimpleDateFormat DF_M_D() {
        return new SimpleDateFormat("M/d", Locale.ENGLISH);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String uri = req.getRequestURI();
        PersistenceManager pm = getPersistenceManager();
        try {
            if (uri.equals("/usage")) {
                showStats(resp, pm);

            } else {
                show404(resp);
            }
        } finally {
            pm.close();
        }
    }
    
    /*
    Important stats:
    - per day:
      - users by version, stacked
    - past month:
      - users by country
      - users by os
      - users by java version
      - users by language
    - plugin usage
      - drill-down: plugin versions
     */

    private void showStats(HttpServletResponse resp, PersistenceManager pm) throws IOException {
        SortedSet<DbUsageSummary> summaries = Sets.newTreeSet();

        getTodayUsage(pm, summaries);

        getPastUsage(pm, summaries);
        
        // build charts

        Map<Long, Integer> usersByDay = Maps.newHashMap();
        Map<Long, Integer> ipsByDay = Maps.newHashMap();
        Map<String, Integer> uuidsByPlugin = Maps.newHashMap();
        Map<String, Integer> uuidsByAppName = Maps.newHashMap();
        Map<String, Integer> uuidsByEntryPoint = Maps.newHashMap();
        Map<String, Integer> uuidsByVersion = Maps.newHashMap();
        Map<String, Integer> uuidsByCountry = Maps.newHashMap();
        Map<String, Integer> uuidsByLanguage = Maps.newHashMap();
        Map<String, Integer> uuidsByJavaVersion = Maps.newHashMap();
        Map<String, Integer> uuidsByOs = Maps.newHashMap();
        for (DbUsageSummary summary : summaries) {
            long time = summary.getDate().getTime();
            int value = summary.getValue();
            if (summary.getCategory().equals("users"))
                usersByDay.put(time, value);

            if (summary.getCategory().equals("ips"))
                ipsByDay.put(time, value);

            if (summary.getCategory().equals("version")) {
                String version = summary.getCategoryKey();
                Matcher m = DEV_VERSION_REGEX.matcher(version);
                if (m.matches())
                    version = m.group(1) + "xx";
                increment(uuidsByVersion, version, value);
            }
            if (summary.getCategory().equals("plugin")) {
                String pkg = summary.getCategoryKey();
                if (pkg.startsWith("edu.umd.cs.findbugs.plugins."))
                    pkg = pkg.substring(0,pkg.length()-"edu.umd.cs.findbugs.plugins.".length());
                increment(uuidsByPlugin, pkg, value);
            }
            if (summary.getCategory().equals("country"))
                increment(uuidsByCountry, summary.getCategoryKey(), value);
            if (summary.getCategory().equals("language"))
                increment(uuidsByLanguage, summary.getCategoryKey(), value);
            if (summary.getCategory().equals("javaVersion"))
                increment(uuidsByJavaVersion, summary.getCategoryKey(), value);
            if (summary.getCategory().equals("os"))
                increment(uuidsByOs, summary.getCategoryKey(), value);
            if (summary.getCategory().equals("appName"))
                increment(uuidsByAppName, summary.getCategoryKey(), value);
            if (summary.getCategory().equals("entryPoint"))
                increment(uuidsByEntryPoint, summary.getCategoryKey(), value);

        }
        
        LineChart usersByVersionPerDay = createTimelineChart2(usersByDay, ipsByDay, "Unique Users");
//        LineChart ipsByVersionPerDay = createTimelineChart2(ipsByDay, "Unique IP Addresses");
        BarChart versionsChart = makeBarChart(uuidsByVersion, "FindBugs Versions", 400, 300, false);
        BarChart pluginsChart = makeBarChart(uuidsByPlugin, "Unique Plugin 'Visits'", 400, 300, true);

        BarChart appNameChart = makeBarChart(uuidsByAppName, "Applications", 600, 400, false);
        BarChart entryPointChart = makeBarChart(uuidsByEntryPoint, "Entry Points", 600, 400, false);

        BarChart languageChart = makeBarChart(uuidsByLanguage, "Languages", 400, 300, false);
        BarChart countryChart = makeBarChart(uuidsByCountry, "Countries", 400, 300, false);
        BarChart osChart = makeBarChart(uuidsByOs, "Operating Systems", 400, 300, false);
        BarChart javaVersionChart = makeBarChart(uuidsByJavaVersion, "Java Versions", 400, 300, false);

        // print results
        resp.setStatus(200);

        ServletOutputStream page = printHtmlHeader(resp, getCloudName() + " - Statistics");
        resp.getOutputStream().print("<div align=center style='font-size:large; font-weight:bold'>" +
                "<a href='/stats'>Evaluations</a> - Usage Stats</div>");
        showChartImg(resp, usersByVersionPerDay, true);
//        page.println("<br><br>");
//        showChartImg(resp, ipsByVersionPerDay, true);

        page.println("<br><br>");
        page.println("<table border=0 cellspacing=20><tr valign=top><td>");
        showChartImg(resp, versionsChart, false);
        page.println("</td><td>");
        showChartImg(resp, pluginsChart, false);
        page.println("<em style='width:500px;display:block'>This plugin chart counts 'visits' from users running each " +
                "plugin. If a user ran FindBugs 100 times per day for 30 days, it " +
                "would only show up as 30 visits.</em>");
        page.println("</td></tr></table>");

        page.println("<br><br>");

        page.println("<table border=0 cellspacing=20><tr><td>");
        showChartImg(resp, appNameChart, false);
        page.println("</td><td>");
        showChartImg(resp, entryPointChart, false);
        page.println("</td></tr></table>");

        page.println("<br><br>");

        page.println("<table border=0 cellspacing=20><tr><td>");
        showChartImg(resp, languageChart, false);
        page.println("</td><td>");
        showChartImg(resp, countryChart, false);
        page.println("</td></tr><tr><td>");
        showChartImg(resp, osChart, false);
        page.println("</td><td>");
        showChartImg(resp, javaVersionChart, false);
        page.println("</td></tr></table>");
    }

    private void getPastUsage(PersistenceManager pm, SortedSet<DbUsageSummary> summaries) {
        LOGGER.info("Getting past month's usage");
        Query squery = pm.newQuery("select from " + persistenceHelper.getDbUsageSummaryClassname()
                + " where date >= :date && endDate == null");
        squery.addExtension("javax.persistence.query.chunkSize", 200);
        @SuppressWarnings("unchecked")
        List<DbUsageSummary> oldSummaries = (List<DbUsageSummary>) squery.execute(fourWeeksAgo());
        summaries.addAll(oldSummaries);
        squery.closeAll();
    }

    private void getTodayUsage(PersistenceManager pm, SortedSet<DbUsageSummary> summaries) {
        LOGGER.info("Getting today's usage");
        //TODO: re-enable!
//        Query query = pm.newQuery("select from " + persistenceHelper.getDbUsageEntryClassname()
//                + " where date >= :date order by date ascending");
//        query.addExtension("javax.persistence.query.chunkSize", 200);
//        Date date = todayMidnight();
//        @SuppressWarnings("unchecked")
//        List<DbUsageEntry> entries = (List<DbUsageEntry>) query.execute(date);
//        UsageDataConsolidator consolidator = new UsageDataConsolidator(persistenceHelper);
//        consolidator.process(query, entries);
//
//        summaries.addAll(consolidator.createSummaryEntries(date, null));
    }

    private void increment(Map<String, Integer> map, String key, int value) {
        Integer storedValue = map.get(key);
        if (storedValue == null) storedValue = 0;
        storedValue += value;
        map.put(key, storedValue);
    }

    private Date todayMidnight() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private BarChart makeBarChart(Map<String, Integer> uuidsByPlugin, String title, int width, int height, boolean showActualNumbers) {
        double max = Collections.max(uuidsByPlugin.values());
        List<Double> data = Lists.newArrayList();
        List<String> labels = Lists.newArrayList();
        List<Entry<String, Integer>> sorted = sortEntries(uuidsByPlugin.entrySet());
        for (Entry<String, Integer> entry : sorted.subList(0,Math.min(10, sorted.size()))) {
            int val = entry.getValue();
            data.add(val/max*100);
            labels.add(entry.getKey());
        }
        if (sorted.size() > 10) {
            int total = 0;
            for (Entry<String, Integer> entry : sorted.subList(10, sorted.size())) {
                total += entry.getValue();
            }
            data.add(total/max*100);
            labels.add("(other)");
        }
        Collections.reverse(labels);
        BarChartPlot plot = Plots.newBarChartPlot(Data.newData(data));
        BarChart chart = GCharts.newBarChart(plot);
        chart.setHorizontal(true);
        chart.addYAxisLabels(AxisLabelsFactory.newAxisLabels(labels));
        if (showActualNumbers)
            chart.addXAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, max, Math.floor(max/30)*10));

//        chart.setLegendMargins(100, 100);
//        chart.setMargins(100, 0, 100, 0);
        chart.setSize(width, height);

        chart.setBarWidth(BarChart.AUTO_RESIZE);
        chart.setTitle(title);
        return chart;
    }

    private List<Entry<String, Integer>> sortEntries(Iterable<Entry<String, Integer>> entries) {
        List<Entry<String, Integer>> list = Lists.newArrayList(entries);
        Collections.sort(list, new Comparator<Entry<String, Integer>>() {
            public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                int a = o1.getValue();
                int b = o2.getValue();
                return -(a < b ? -1 : (a > b ? 1 : 0));
            }
        });
        
        return list; 
    }

    private static Date fourWeeksAgo() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -7*4);

        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    private ServletOutputStream printHtmlHeader(HttpServletResponse resp, String title) throws IOException {
        ServletOutputStream page = resp.getOutputStream();
        page.println("<html>" +
                "<head>" +
                "<title>" + title + "</title>" +
                "<script type='application/javascript'>\n" +
                "    // Send the POST when the page is loaded,\n" +
                "    // which will replace this whole page with the retrieved chart.\n" +
                "    function loadGraph() {\n" +
                "      for (var i = 1; i < 40; i++) {" +
                "        var frm = document.getElementById('post_form_' + i);\n" +
                "        if (frm) \n" +
                "          frm.submit();\n" +
                "      }\n" +
                "    }\n" +
                "  </script>" +
                "</head>"
                + "<body onload='loadGraph()'>");
        return page;
    }

    private LineChart createTimelineChart2(Map<Long, Integer> firstByDay, Map<Long, Integer> secondByDay, String title) {
        int maxUsersPerDay = Collections.max(firstByDay.values());
        int maxIpsPerDay = Collections.max(firstByDay.values());
        int maxPerDay = Math.max(maxUsersPerDay, maxIpsPerDay);

        List<Double> userCountData = new ArrayList<Double>();
        List<Double> ipCountData = new ArrayList<Double>();
        List<String> timelineLabels = Lists.newArrayList();
        boolean first = true;
        for (Calendar cal : iterateByDay(firstByDay.keySet())) {
            Integer usersThisWeek = firstByDay.get(cal.getTimeInMillis());
            if (usersThisWeek == null) 
                usersThisWeek = 0;
            Integer ipsThisWeek = secondByDay.get(cal.getTimeInMillis());
            if (ipsThisWeek == null) 
                ipsThisWeek = 0;

            userCountData.add(usersThisWeek * 100.0 / maxPerDay);
            ipCountData.add(ipsThisWeek * 100.0 / maxPerDay);
            timelineLabels.add(formatDate(cal, first));
            first = false;
        }
        
        pruneLabels(timelineLabels);

        Line userCountLine = Plots.newLine(Data.newData(userCountData), Color.PINK, "Users");
        userCountLine.setFillAreaColor(Color.LIGHTPINK);
        Line ipCountLine = Plots.newLine(Data.newData(ipCountData), Color.MAGENTA, "IP Addresses");
//        userCountLine.setFillAreaColor(new Color("ADD8E6"));

        LineChart chart = GCharts.newLineChart(userCountLine, ipCountLine);
        chart.setLegendPosition(LegendPosition.RIGHT);
        chart.setTitle(title);
        chart.setDataEncoding(DataEncoding.TEXT);
        chart.setSize(850, 350);

        chart.setGrid(100, (maxPerDay / 100.0), 4, 1);

        chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(timelineLabels));
        chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxPerDay));
        return chart;
    }

    private void pruneLabels(List<String> timelineLabels) {
        int showLabelEvery = Math.max(1, timelineLabels.size()/10);
        for (int i = 0, timelineLabelsSize = timelineLabels.size(); i < timelineLabelsSize; i++) {
            if (i % showLabelEvery != 0)
                timelineLabels.set(i, "");
        }
    }

    private LineChart createTimelineChart2(Map<Long, Integer> byDayCounts, String title) {
        int maxPerDay = Collections.max(byDayCounts.values());

        List<Double> userCountData = new ArrayList<Double>();
        List<String> timelineLabels = Lists.newArrayList();
        boolean first = true;
        for (Calendar cal : iterateByDay(byDayCounts.keySet())) {
            Integer usersThisWeek = byDayCounts.get(cal.getTimeInMillis());
            if (usersThisWeek == null)
                usersThisWeek = 0;

            userCountData.add(usersThisWeek * 100.0 / maxPerDay);
            timelineLabels.add(formatDate(cal, first));
            first = false;
        }

        Line userCountLine = Plots.newLine(Data.newData(userCountData), Color.LIGHTPINK);
        userCountLine.setFillAreaColor(Color.LIGHTPINK);

        LineChart chart = GCharts.newLineChart(userCountLine);
        chart.setTitle(title);
        chart.setDataEncoding(DataEncoding.TEXT);
        chart.setSize(850, 350);

        chart.setGrid(100, 10 / (maxPerDay / 500.0), 4, 1);

        chart.addXAxisLabels(AxisLabelsFactory.newAxisLabels(timelineLabels));
        chart.addYAxisLabels(AxisLabelsFactory.newNumericRangeAxisLabels(0, maxPerDay));
        return chart;
    }

    private String formatDate(Calendar cal, boolean firstDateSoFar) {
        if (firstDateSoFar || cal.get(Calendar.MONTH) == Calendar.JANUARY) {
            return DF_M_D_Y().format(new Date(cal.getTimeInMillis()));
        } else {
            return DF_M_D().format(new Date(cal.getTimeInMillis()));
        }
    }

    private Iterable<Calendar> iterateByDay(Set<Long> unixtimes) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(Collections.min(unixtimes));
//        cal.add(Calendar.DAY_OF_MONTH, -1);

        final long first = cal.getTimeInMillis();
        final long last = Collections.max(unixtimes);

        return new Iterable<Calendar>() {
            public Iterator<Calendar> iterator() {
                return new Iterator<Calendar>() {
                    private Calendar cal = Calendar.getInstance();
                    {
                        cal.setTimeInMillis(first);
                    }

                    public boolean hasNext() {
                        return cal.getTimeInMillis() <= last;
                    }

                    public Calendar next() {
                        if (!hasNext())
                            throw new NoSuchElementException();
                        Calendar toReturn = (Calendar) cal.clone();
                        cal.add(Calendar.DAY_OF_MONTH, 1);
                        return toReturn;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /** protected for testing */
    protected void showChartImg(HttpServletResponse resp, GChart chart, boolean iframe) throws IOException {
        Matcher m = Pattern.compile("(\\d+)x(\\d+)").matcher(chart.getParameters().get("chs"));
        String margins = chart.getParameters().get("chma");
        if (margins == null) margins = "";
        Matcher m2 = Pattern.compile("(\\d+),(\\d+),(\\d+),(\\d+)").matcher(margins);
        int actualwidth, actualheight;
        int width, height;
        if (m.find()) {
            width = Integer.parseInt(m.group(1));
            height = Integer.parseInt(m.group(2));
            actualwidth = width;
            actualheight = height;
            if (m2.find()) {
                actualwidth += Integer.parseInt(m2.group(1));
                actualwidth += Integer.parseInt(m2.group(3));
            }
        } else {
            width = 300;
            height = 200;
            actualwidth = width;
            actualheight = height;
        }
        
        if(iframe) {

            form_id++;
            Map<String, String> parameters = chart.getParameters();
            resp.getOutputStream().print(
                    "<form action='https://chart.googleapis.com/chart' method='POST' " +
                            "id='post_form_" + form_id + "'\n" +
                            "target='post_frame_" + form_id + "' " +
                            "onsubmit=\"this.action = 'https://chart.googleapis.com/chart?chid=' " +
                            "+ (new Date()).getMilliseconds(); return true;\">\n");
            for (Entry<String, String> entry : parameters.entrySet()) {
                resp.getOutputStream().println("<input type='hidden' name='" + entry.getKey()
                        + "' value='" + StringEscapeUtils.escapeHtml(URLDecoder.decode(entry.getValue(), "UTF-8")) + "'/>");
            }
            resp.getOutputStream().print("    </form>");
            resp.getOutputStream().print("<iframe name='post_frame_" + form_id + "' src=\"/empty.html\" " +
                    "width=\"" + actualwidth + "\" height=\"" + actualheight + "\"></iframe>");
        } else {
            resp.getOutputStream().print("<img src='" + chart.toURLForHTML() + "' style='width:" + width
                    + "px;height:" + height + "px;border:1px solid gray'>");
        }
    }

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
    }
}
