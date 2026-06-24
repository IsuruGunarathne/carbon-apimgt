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

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.event.output.adapter.core.security.SsrfPolicyProvider;
import org.wso2.carbon.event.output.adapter.core.security.SsrfPolicyViolationException;

/**
 * Bridges the analytics event-output-adapter {@link SsrfPolicyProvider} SPI to the APIM SSRF gate.
 * Registered over OSGi by {@code APIManagerComponent}; consumed by carbon-analytics-common.
 */
public class EventAdapterSsrfPolicyProviderImpl implements SsrfPolicyProvider {

    @Override
    public void validate(String url, String tenantDomain) throws SsrfPolicyViolationException {
        try {
            APIUtil.validateRemoteURL(url, tenantDomain);
        } catch (APIManagementException e) {
            throw new SsrfPolicyViolationException(e.getMessage(), e);
        }
    }
}
