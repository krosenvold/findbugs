package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.protobuf.GeneratedMessage;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;
import junit.framework.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockAppEngineCloudClient extends AppEngineCloudClient {
    private List<ExpectedConnection> expectedConnections = new ArrayList<ExpectedConnection>();

    private int nextConnection = 0;

    private AppEngineNameLookup mockNameLookup;

    private Long mockSessionId = null;

    public List<String> urlsRequested;

    public IGuiCallback mockGuiCallback;

    public List<String> statusMsgHistory = new CopyOnWriteArrayList<String>();

    private final Object statusMsgLock = new Object();

    public MockAppEngineCloudClient(CloudPlugin plugin, SortedBugCollection bugs, List<HttpURLConnection> mockConnections)
            throws IOException {
        super(plugin, bugs, new Properties());

        setNetworkClient(new MockAppEngineCloudNetworkClient());

        urlsRequested = Lists.newArrayList();
        for (HttpURLConnection mockConnection : mockConnections) {
            expectedConnections.add(new ExpectedConnection().withLegacyMock(mockConnection));
        }
        mockNameLookup = createMockNameLookup();
        mockGuiCallback = mock(IGuiCallback.class);
        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Runnable r = (Runnable) args[0];
                r.run();
                return null;
            }
        }).when(mockGuiCallback).invokeInGUIThread(Mockito.isA(Runnable.class));

        initStatusBarHistory();
    }

    @Override
    protected ExecutorService getBugUpdateExecutor() {
        return backgroundExecutorService;
    }

    @Override
    protected IGuiCallback getGuiCallback() {
        return mockGuiCallback;
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public AppEngineCloudNetworkClient createSpyNetworkClient() throws IOException {
        AppEngineCloudNetworkClient spyNetworkClient = Mockito.spy(getNetworkClient());
        Mockito.doThrow(new IOException()).when(spyNetworkClient).signIn(true);
        setNetworkClient(spyNetworkClient);
        return spyNetworkClient;
    }

    @Override
    public void setSigninState(SigninState state) {
        super.setSigninState(state);
    }

    public ExpectedConnection expectConnection(String url) {
        ExpectedConnection connection = new ExpectedConnection();
        expectedConnections.add(connection.withUrl(url));
        return connection;
    }

    /**
     * Returns POST data submitted for the given URL. If the URL was expected &
     * requested more than once, this will return only the data from the LATEST
     * one.
     */
    public byte[] postedData(String url) {
        return getLatestExpectedConnection(url).getPostData();
    }

    public void verifyConnections() {
        if (expectedConnections.size() != nextConnection) {
            Assert.fail("some connections were not opened\n" + "opened: " + expectedConnections.subList(0, nextConnection) + "\n"
                    + "missed: " + expectedConnections.subList(nextConnection, expectedConnections.size()));
        }
    }

    public void waitUntilIssuesUploaded(long timeout, TimeUnit unit) throws InterruptedException {
        if (!newIssuesUploaded.await(timeout, unit)) {
            Assert.fail("issues uploaded event never fired after " + timeout + " " + unit.toString());
        }
    }

    /**
     * Returns a {@link CountDownLatch} that waits for a IGuiCallback
     * showMessageDialog call with a message matching the given regex.
     */
    public CountDownLatch getDialogLatch(final String dialogRegex) {
        final CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                String message = (String) invocationOnMock.getArguments()[0];
                boolean match = Pattern.compile(dialogRegex).matcher(message).find();
                System.out.println("QQQ: " + " " + match + "\n\"" + dialogRegex + "\"\n\"" + message + "\"");
                if (match)
                    latch.countDown();
                return null;
            }
        }).when(mockGuiCallback).showMessageDialog(Mockito.anyString());
        return latch;
    }

    public void clickYes(String regex) {
        when(mockGuiCallback.showConfirmDialog(matches(regex), anyString(), anyString(), anyString())).thenReturn(
                IGuiCallback.YES_OPTION);
    }

    // ========================== end of public methods
    // =========================

    private void initStatusBarHistory() {
        addListener(new CloudListener() {
            public void issueUpdated(BugInstance bug) {
            }

            public void statusUpdated() {
                String statusMsg = getStatusMsg();

                if (!statusMsgHistory.isEmpty()) {
                    String last = statusMsgHistory.get(statusMsgHistory.size() - 1);
                    if (statusMsg.equals(last))
                        return;
                }
                statusMsgHistory.add(statusMsg);
                synchronized (statusMsgLock) {
                    statusMsgLock.notifyAll();
                }
            }

            public void taskStarted(CloudTask task) {
            }
        });
    }

    private AppEngineNameLookup createMockNameLookup() throws IOException {
        AppEngineNameLookup mockNameLookup = mock(AppEngineNameLookup.class);
        when(mockNameLookup.getHost()).thenReturn("host");
        when(mockNameLookup.getUsername()).thenReturn("test@example.com");
        when(mockNameLookup.getSessionId()).thenAnswer(new Answer<Long>() {
            public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                return mockSessionId;
            }
        });
        when(mockNameLookup.signIn(Mockito.<CloudPlugin> any(), Mockito.<BugCollection> any())).thenAnswer(new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                mockSessionId = 555L;
                return true;
            }
        });
        return mockNameLookup;
    }

    private ExpectedConnection getLatestExpectedConnection(String url) {
        for (int i = expectedConnections.size() - 1; i >= 0; i--) {
            ExpectedConnection expectedConnection = expectedConnections.get(i);
            if (url.equals(expectedConnection.url()))
                return expectedConnection;
        }
        return null;
    }

    public void checkStatusBarHistory(String... expectedStatusLines) {
        Assert.assertEquals(Arrays.asList(expectedStatusLines), statusMsgHistory);
    }

    public void waitForStatusMsg(String regex) throws InterruptedException {
        Pattern pattern = Pattern.compile(regex);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15 * 1000) {
            synchronized (statusMsgLock) {
                statusMsgLock.wait(1000);
                for (String status : statusMsgHistory) {
                    if (pattern.matcher(status).matches())
                        return;
                }
            }
        }
        Assert.fail("Did not see status message " + regex + " in:\n" + statusMsgHistory);
    }

    private class MockAppEngineCloudNetworkClient extends AppEngineCloudNetworkClient {
        @Override
        HttpURLConnection openConnection(String url) {
            if (nextConnection >= expectedConnections.size()) {
                Assert.fail("Cannot open " + url + " - already requested all " + expectedConnections.size() + " url's: "
                        + expectedConnections);
            }
            urlsRequested.add(url);
            ExpectedConnection connection = expectedConnections.get(nextConnection);
            nextConnection++;
            String expectedUrl = connection.url();
            if (expectedUrl != null) {
                expectedUrl = "/" + expectedUrl;
                if (!expectedUrl.equals(url)) {
                    Assert.fail("Expected '" + expectedUrl + "' but '" + url + "' was requested");
                }
            }
            System.err.println("opening " + url + " at " + Thread.currentThread().getStackTrace()[2]);
            return connection.mockConnection;
        }

        @Override
        protected AppEngineNameLookup createNameLookup() {
            return mockNameLookup;
        }
    }

    public class ExpectedConnection {
        private HttpURLConnection mockConnection;

        private String url = null;

        private int responseCode = 200;

        private InputStream responseStream;

        private IOException networkError = null;

        private ByteArrayOutputStream postDataStream;

        private CountDownLatch latch = new CountDownLatch(1);

        public ExpectedConnection() {
            mockConnection = mock(HttpURLConnection.class);
            postDataStream = new ByteArrayOutputStream();
            try {
                when(mockConnection.getOutputStream()).thenAnswer(new Answer<Object>() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        latch.countDown();
                        if (networkError != null)
                            throw networkError;
                        return postDataStream;
                    }
                });
                when(mockConnection.getInputStream()).thenAnswer(new Answer<InputStream>() {
                    public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                        latch.countDown();
                        return responseStream;
                    }
                });
                when(mockConnection.getResponseCode()).thenAnswer(new Answer<Integer>() {
                    public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                        latch.countDown();
                        return responseCode;
                    }
                });
            } catch (IOException e) {
            }
        }

        public ExpectedConnection withLegacyMock(HttpURLConnection mockConnection) {
            this.mockConnection = mockConnection;
            return this;
        }

        public @CheckForNull
        String url() {
            return url;
        }

        public ExpectedConnection withUrl(String url) {
            this.url = url;
            return this;
        }

        public ExpectedConnection withResponse(GeneratedMessage response) {
            if (responseStream != null)
                throw new IllegalStateException("Already have response stream");
            responseStream = new ByteArrayInputStream(response.toByteArray());
            return this;
        }

        public byte[] getPostData() {
            return getOutputStream().toByteArray();
        }

        public void withErrorCode(int code) {
            this.responseCode = code;
        }

        public void throwsNetworkError(IOException e) {
            networkError = e;
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        @Override
        public String toString() {
            ByteArrayOutputStream postStream = getOutputStream();
            return "/" + url() + (postStream.size() > 0 ? " <" + postStream.size() + ">" : "");
        }

        // ====================== end of public methods =======================

        private ByteArrayOutputStream getOutputStream() {
            try {
                return (ByteArrayOutputStream) mockConnection.getOutputStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
