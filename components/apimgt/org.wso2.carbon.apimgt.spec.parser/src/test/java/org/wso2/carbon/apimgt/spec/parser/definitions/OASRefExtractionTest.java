/*
 *   Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.spec.parser.definitions;

import org.junit.Assert;
import org.junit.Test;
import java.util.Set;

public class OASRefExtractionTest {
    private static final String YAML =
        "openapi: 3.0.0\n" +
        "components:\n" +
        "  schemas:\n" +
        "    A: { $ref: 'http://169.254.169.254/x.yaml#/C' }\n" +
        "    Local: { $ref: '#/components/schemas/A' }\n" +
        "    Rel:   { $ref: './rel.yaml' }\n" +
        "    Ext:   { $ref: 'https://good.example.com/s.json' }\n";

    @Test
    public void testExtractsOnlyAbsoluteHttpRefs() {
        Set<String> refs = OASParserUtil.extractExternalRefUrls(YAML);
        Assert.assertTrue(refs.contains("http://169.254.169.254/x.yaml#/C"));
        Assert.assertTrue(refs.contains("https://good.example.com/s.json"));
        Assert.assertEquals(2, refs.size());
    }

    @Test
    public void testJsonInputAndMalformed() {
        Assert.assertEquals(1,
            OASParserUtil.extractExternalRefUrls("{\"$ref\":\"http://10.0.0.1/a.json\",\"x\":{\"$ref\":\"#/l\"}}").size());
        Assert.assertTrue(OASParserUtil.extractExternalRefUrls(":not yaml or json::").isEmpty());
        Assert.assertTrue(OASParserUtil.extractExternalRefUrls(null).isEmpty());
    }
}
