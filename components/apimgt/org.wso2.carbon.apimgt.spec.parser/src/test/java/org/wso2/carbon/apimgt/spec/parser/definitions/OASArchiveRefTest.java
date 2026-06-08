/*
 *   Copyright (c) 2026, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.carbon.apimgt.api.APIDefinitionValidationResponse;
import org.wso2.carbon.apimgt.api.model.OASParserOptions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class OASArchiveRefTest {
    @Test
    public void testArchiveBlockedRefNotFetched() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal.yaml", ex -> { hits.incrementAndGet();
            byte[] b = "type: object".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, b.length); ex.getResponseBody().write(b); ex.close(); });
        server.start();
        int port = server.getAddress().getPort();
        File zip = File.createTempFile("ssrf-arch", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("api/swagger.yaml"));
            String master = "openapi: 3.0.0\ninfo: {title: t, version: '1.0'}\npaths:\n  /a:\n    get:\n" +
                "      responses:\n        '200':\n          description: ok\n          content:\n" +
                "            application/json:\n              schema:\n" +
                "                $ref: 'http://127.0.0.1:" + port + "/internal.yaml'\n";
            zos.write(master.getBytes(StandardCharsets.UTF_8)); zos.closeEntry();
        }
        try {
            OASParserOptions opts = new OASParserOptions();
            opts.setSafeRefResolution(true);
            opts.setRemoteRefBlockList(java.util.Collections.singletonList("*"));
            APIDefinitionValidationResponse resp;
            try (FileInputStream fis = new FileInputStream(zip)) {
                resp = OASParserUtil.extractAndValidateOpenAPIArchive(fis, false, opts);
            }
            Assert.assertEquals(0, hits.get());
            Assert.assertFalse(resp.isValid());
        } finally {
            server.stop(0);
            zip.delete();
        }
    }
}
