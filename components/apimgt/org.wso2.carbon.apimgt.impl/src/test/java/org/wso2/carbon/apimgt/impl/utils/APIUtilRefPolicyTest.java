/*
 *  Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.impl.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.wso2.carbon.apimgt.api.model.OASParserOptions;

import java.util.Arrays;
import java.util.Collections;

@RunWith(PowerMockRunner.class)
@PrepareForTest({APIUtil.class})
public class APIUtilRefPolicyTest {

    private OASParserOptions run(String mode, java.util.List<String> hosts, boolean bpna) throws Exception {
        PowerMockito.spy(APIUtil.class);
        PowerMockito.doReturn(null).when(APIUtil.class, "getTenantConfig", org.mockito.ArgumentMatchers.anyString());
        Whitebox.setInternalState(APIUtil.class, "networkSecurityEnabled", mode != null);
        Whitebox.setInternalState(APIUtil.class, "networkSecurityMode", mode);
        Whitebox.setInternalState(APIUtil.class, "networkSecurityHosts", hosts);
        Whitebox.setInternalState(APIUtil.class, "networkSecurityBlockPrivateAccess", bpna);
        OASParserOptions o = new OASParserOptions();
        APIUtil.populateRefResolutionPolicy(o, "carbon.super");
        return o;
    }

    @Test public void testAllow() throws Exception {
        OASParserOptions o = run("allow", Arrays.asList("good.com"), true);
        Assert.assertTrue(o.isSafeRefResolution());
        Assert.assertEquals(Arrays.asList("good.com"), o.getRemoteRefAllowList());
        Assert.assertEquals(Collections.singletonList("*"), o.getRemoteRefBlockList());
    }
    @Test public void testDeny() throws Exception {
        OASParserOptions o = run("deny", Arrays.asList("bad.com"), true);
        Assert.assertTrue(o.getRemoteRefAllowList() == null || o.getRemoteRefAllowList().isEmpty());
        Assert.assertEquals(Arrays.asList("bad.com"), o.getRemoteRefBlockList());
    }
    @Test public void testAllowEmptyFailsClosed() throws Exception {
        OASParserOptions o = run("allow", Collections.emptyList(), true);
        Assert.assertEquals(Collections.singletonList("*"), o.getRemoteRefBlockList());
        Assert.assertTrue(o.getRemoteRefAllowList() == null || o.getRemoteRefAllowList().isEmpty());
    }
    @Test public void testInactive() throws Exception {
        Assert.assertFalse(run(null, null, false).isSafeRefResolution());
    }
    @Test(expected = org.wso2.carbon.apimgt.api.APIManagementException.class)
    public void testInvalidMode() throws Exception { run("bogus", Arrays.asList("x"), true); }

    @Test public void testBuildSetsHook() throws Exception {
        PowerMockito.spy(APIUtil.class);
        PowerMockito.doReturn(null).when(APIUtil.class, "getTenantConfig", org.mockito.ArgumentMatchers.anyString());
        Whitebox.setInternalState(APIUtil.class, "networkSecurityEnabled", true);
        Whitebox.setInternalState(APIUtil.class, "networkSecurityMode", "deny");
        Whitebox.setInternalState(APIUtil.class, "networkSecurityHosts", Arrays.asList("bad.com"));
        Whitebox.setInternalState(APIUtil.class, "networkSecurityBlockPrivateAccess", true);
        OASParserOptions o = APIUtil.buildRefAwareOASParserOptions(new OASParserOptions(), "carbon.super");
        Assert.assertNotNull(o.getRefValidator());
        Assert.assertEquals("carbon.super", o.getRefValidationTenantDomain());
    }

    @Test public void testDenyThenAllowFailsClosed() throws Exception {
        PowerMockito.spy(APIUtil.class);
        Whitebox.setInternalState(APIUtil.class, "networkSecurityEnabled", true);
        Whitebox.setInternalState(APIUtil.class, "networkSecurityMode", "deny");
        Whitebox.setInternalState(APIUtil.class, "networkSecurityHosts", Arrays.asList("x.com"));
        Whitebox.setInternalState(APIUtil.class, "networkSecurityBlockPrivateAccess", true);
        org.json.simple.JSONObject tenant = new org.json.simple.JSONObject();
        org.json.simple.JSONObject ac = new org.json.simple.JSONObject();
        ac.put("Mode", "allow");
        org.json.simple.JSONArray hosts = new org.json.simple.JSONArray();
        hosts.add("x.com"); hosts.add("y.com");
        ac.put("Hosts", hosts);
        tenant.put("NetworkSecurityAccessControl", ac);
        PowerMockito.doReturn(tenant).when(APIUtil.class, "getTenantConfig", org.mockito.ArgumentMatchers.anyString());
        OASParserOptions o = new OASParserOptions();
        APIUtil.populateRefResolutionPolicy(o, "carbon.super");
        Assert.assertFalse(o.getRemoteRefAllowList().contains("x.com"));
        Assert.assertTrue(o.getRemoteRefAllowList().contains("y.com"));
        Assert.assertTrue(o.getRemoteRefBlockList().contains("*"));
    }
}
