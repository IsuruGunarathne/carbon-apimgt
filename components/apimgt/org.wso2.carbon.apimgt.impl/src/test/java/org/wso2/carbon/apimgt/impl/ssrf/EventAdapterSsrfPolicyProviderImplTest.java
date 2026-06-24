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

package org.wso2.carbon.apimgt.impl.ssrf;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.ExceptionCodes;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.event.output.adapter.core.security.SsrfPolicyViolationException;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Unit tests for {@link EventAdapterSsrfPolicyProviderImpl}, verifying it delegates to
 * {@link APIUtil#validateRemoteURL} and wraps a block as {@link SsrfPolicyViolationException}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({APIUtil.class})
public class EventAdapterSsrfPolicyProviderImplTest {

    @Test
    public void delegatesToValidateRemoteURL() throws Exception {
        PowerMockito.mockStatic(APIUtil.class);
        new EventAdapterSsrfPolicyProviderImpl().validate("https://ok.example/x", "carbon.super");
        PowerMockito.verifyStatic(APIUtil.class);
        APIUtil.validateRemoteURL("https://ok.example/x", "carbon.super");
    }

    @Test(expected = SsrfPolicyViolationException.class)
    public void wrapsApiManagementException() throws Exception {
        PowerMockito.mockStatic(APIUtil.class);
        PowerMockito.doThrow(new APIManagementException("blocked by network security access control policy",
                        ExceptionCodes.UNTRUSTED_URL))
                .when(APIUtil.class);
        APIUtil.validateRemoteURL(anyString(), anyString());
        new EventAdapterSsrfPolicyProviderImpl().validate("https://169.254.169.254/x", "carbon.super");
    }
}
