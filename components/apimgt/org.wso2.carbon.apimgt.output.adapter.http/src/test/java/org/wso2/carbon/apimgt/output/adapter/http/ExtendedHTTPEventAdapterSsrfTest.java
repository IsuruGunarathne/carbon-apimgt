/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.output.adapter.http;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.event.output.adapter.core.OutputEventAdapterConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;

/**
 * SSRF gate (G4) tests for {@link ExtendedHTTPEventAdapter#publish(Object, Map)}.
 *
 * <p>publish() calls {@link APIUtil#validateRemoteURL(String, String)} on the per-event {@code http.url}
 * before submitting an {@code HTTPSender} job. A blocked URL must drop the event (no executor submission);
 * an allowed URL must proceed past the gate and submit the send job.</p>
 *
 * <p>Only {@link APIUtil} is mocked statically; the real {@link PrivilegedCarbonContext} runs (as in the
 * existing adaptor test case) and is delegated to the real classloader via {@link PowerMockIgnore} so the
 * Carbon/XML machinery is not rewritten by PowerMock.</p>
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({APIUtil.class})
@SuppressStaticInitializationFor("org.wso2.carbon.apimgt.impl.utils.APIUtil")
@PowerMockIgnore({"javax.xml.*", "org.xml.*", "org.w3c.*", "javax.naming.*", "jdk.xml.*",
        "org.apache.xerces.*", "org.apache.xalan.*", "org.apache.xml.*",
        "org.wso2.carbon.context.*", "org.wso2.carbon.utils.*", "org.wso2.carbon.base.*"})
public class ExtendedHTTPEventAdapterSsrfTest {

    private static final String BLOCKED_URL = "http://169.254.169.254/x";
    private static final String ALLOWED_URL = "http://localhost:8080/service";

    @Before
    public void setupCarbonContext() {
        System.setProperty("carbon.home",
                "src" + File.separator + "test" + File.separator + "resources" + File.separator + "carbon-context");
        System.setProperty("tenant.name", "tenant.name");
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(-1234);
        // Reset the shared static state so each test's init() fully re-runs (builds the connection manager
        // and the executor) rather than short-circuiting on a value left over from a previous test.
        Whitebox.setInternalState(ExtendedHTTPEventAdapter.class, "executorService", (ExecutorService) null);
    }

    private ExtendedHTTPEventAdapter getAdapter() {
        OutputEventAdapterConfiguration eventAdapterConfiguration = new OutputEventAdapterConfiguration();
        eventAdapterConfiguration.setName("TestHttpAdaptor");
        eventAdapterConfiguration.setType("http-extended");
        eventAdapterConfiguration.setMessageFormat("text");
        Map<String, String> staticPropertes = new HashMap<>();
        staticPropertes.put("http.client.method", "HttpPost");
        eventAdapterConfiguration.setStaticProperties(staticPropertes);
        Map<String, String> globalProperties = new HashMap<>();
        globalProperties.put("jobQueueSize", "10000");
        globalProperties.put("keepAliveTimeInMillis", "20000");
        globalProperties.put("maxThread", "100");
        globalProperties.put("minThread", "8");
        globalProperties.put("defaultMaxConnectionsPerHost", "50");
        globalProperties.put("maxTotalConnections", "1000");
        return new ExtendedHTTPEventAdapter(eventAdapterConfiguration, globalProperties);
    }

    /**
     * Swaps in a mock {@code executorService} (after init() has built the rest of the static state) so we
     * can assert whether a send job was submitted (egress) or not (dropped) without performing real I/O.
     */
    private ExecutorService injectMockExecutor() {
        ExecutorService mockExecutor = Mockito.mock(ExecutorService.class);
        Whitebox.setInternalState(ExtendedHTTPEventAdapter.class, "executorService", mockExecutor);
        return mockExecutor;
    }

    @Test
    public void publishBlocksOnSsrf() throws Exception {
        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.doThrow(new APIManagementException("blocked by network security access control policy",
                        ExceptionCodes.UNTRUSTED_URL))
                .when(APIUtil.class);
        APIUtil.validateRemoteURL(eq(BLOCKED_URL), nullable(String.class));

        ExtendedHTTPEventAdapter adapter = getAdapter();
        adapter.init();
        adapter.connect();
        ExecutorService mockExecutor = injectMockExecutor();

        Map<String, String> dynamicProperties = new HashMap<>();
        dynamicProperties.put("http.url", BLOCKED_URL);
        dynamicProperties.put("http.username", "user123");
        dynamicProperties.put("http.password", "pwd123");
        dynamicProperties.put("http.headers", null);

        // Must return normally (log-and-drop), not throw.
        adapter.publish("msg", dynamicProperties);

        // Gate fired on the blocked URL ...
        PowerMockito.verifyStatic(APIUtil.class);
        APIUtil.validateRemoteURL(eq(BLOCKED_URL), nullable(String.class));
        // ... and NO send job was submitted (event dropped before egress).
        Mockito.verify(mockExecutor, Mockito.never()).execute(any(Runnable.class));

        adapter.disconnect();
        adapter.destroy();
    }

    @Test
    public void publishProceedsWhenAllowed() throws Exception {
        PowerMockito.mockStatic(APIUtil.class);
        // validateRemoteURL does not throw -> publish proceeds past the gate.

        ExtendedHTTPEventAdapter adapter = getAdapter();
        adapter.init();
        adapter.connect();
        ExecutorService mockExecutor = injectMockExecutor();

        Map<String, String> dynamicProperties = new HashMap<>();
        dynamicProperties.put("http.url", ALLOWED_URL);
        dynamicProperties.put("http.username", "user123");
        dynamicProperties.put("http.password", "pwd123");
        dynamicProperties.put("http.headers", null);

        adapter.publish("msg", dynamicProperties);

        // Gate was consulted ...
        PowerMockito.verifyStatic(APIUtil.class);
        APIUtil.validateRemoteURL(eq(ALLOWED_URL), nullable(String.class));
        // ... and publish proceeded to submit the send job (egress).
        Mockito.verify(mockExecutor).execute(any(Runnable.class));

        adapter.disconnect();
        adapter.destroy();
    }
}
