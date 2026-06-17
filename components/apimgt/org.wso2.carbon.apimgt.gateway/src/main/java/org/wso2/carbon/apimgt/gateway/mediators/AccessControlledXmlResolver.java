/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.apimgt.gateway.mediators;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.wso2.carbon.apimgt.api.APIManagementException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * An {@link LSResourceResolver} that enforces the network access-control policy on
 * every external reference resolved while an XSD is compiled — nested
 * xsd:import/include/redefine and external DTDs. Only http/https are permitted; each
 * reference is checked via the injected {@link RemoteUrlValidator}. On any violation it
 * throws {@link XsdRefBlockedException} to abort compilation (fail closed). On success it
 * returns {@code null} so the default resolver fetches the now-authorized URL.
 */
public class AccessControlledXmlResolver implements LSResourceResolver {

    private final RemoteUrlValidator validator;

    public AccessControlledXmlResolver(RemoteUrlValidator validator) {
        this.validator = validator;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                   String systemId, String baseURI) {
        String absoluteUrl = toAbsoluteUrl(systemId, baseURI);
        if (absoluteUrl == null) {
            throw new XsdRefBlockedException(
                    "Blocked XSD reference with an unresolvable system id: " + systemId);
        }
        if (!isHttpOrHttps(absoluteUrl)) {
            throw new XsdRefBlockedException(
                    "Blocked XSD reference with a non-HTTP(S) scheme: " + absoluteUrl);
        }
        try {
            validator.validate(absoluteUrl);
        } catch (APIManagementException e) {
            throw new XsdRefBlockedException(
                    "Blocked XSD reference not permitted by the network access-control policy: "
                            + absoluteUrl, e);
        }
        // Permitted: let the default resolver fetch the validated URL.
        return null;
    }

    /**
     * Resolves a possibly-relative systemId against its baseURI into an absolute URL
     * string, or returns {@code null} if it cannot be resolved to an absolute URL.
     */
    static String toAbsoluteUrl(String systemId, String baseURI) {
        if (systemId == null) {
            return null;
        }
        try {
            URI ref = new URI(systemId.trim());
            if (ref.isAbsolute()) {
                return ref.toString();
            }
            if (baseURI != null) {
                URI resolved = new URI(baseURI.trim()).resolve(ref);
                return resolved.isAbsolute() ? resolved.toString() : null;
            }
            return null;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * @return {@code true} only if the URL parses and has an http or https scheme.
     */
    static boolean isHttpOrHttps(String url) {
        try {
            String scheme = new URI(url).getScheme();
            if (scheme == null) {
                return false;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
