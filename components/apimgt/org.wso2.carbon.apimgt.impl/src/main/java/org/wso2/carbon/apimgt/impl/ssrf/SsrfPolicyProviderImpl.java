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
import org.wso2.carbon.identity.core.security.SsrfPolicyProvider;
import org.wso2.carbon.identity.core.security.SsrfPolicyViolationException;

/**
 * Bridges carbon-apimgt's network access-control (SSRF) policy to upstream platform bundles via
 * the {@link SsrfPolicyProvider} SPI. Delegates to {@link APIUtil#validateRemoteURL}, which applies
 * both the platform policy (deployment.toml) and the per-tenant policy (tenant-conf.json). This
 * impl is registered as an OSGi service by {@code APIManagerComponent}.
 */
public class SsrfPolicyProviderImpl implements SsrfPolicyProvider {

    @Override
    public void validate(String url, String tenantDomain) throws SsrfPolicyViolationException {
        try {
            APIUtil.validateRemoteURL(url, tenantDomain);
        } catch (APIManagementException e) {
            throw new SsrfPolicyViolationException(e.getMessage(), e);
        }
    }
}
