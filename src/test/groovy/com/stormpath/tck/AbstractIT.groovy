/*
 * Copyright 2016 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stormpath.tck

import com.jayway.restassured.RestAssured
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.response.Response
import com.stormpath.tck.util.RestUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeTest

import static com.jayway.restassured.RestAssured.*
import static com.stormpath.tck.util.EnvUtils.getVal

abstract class AbstractIT {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    static final String webappUrlScheme = getVal("STORMPATH_TCK_WEBAPP_SCHEME", "http")
    static final String webappUrlHost = getVal("STORMPATH_TCK_WEBAPP_HOST", "localhost")
    static final int webappUrlPort = getVal("STORMPATH_TCK_WEBAPP_PORT", "8080") as int
    static final private String webappUrlPortSuffix = toPortSuffix(webappUrlScheme, webappUrlPort)
    static final private String defaultWebappBaseUrl = "$webappUrlScheme://$webappUrlHost$webappUrlPortSuffix"
    static final String webappBaseUrl = getVal("STORMPATH_TCK_WEBAPP_URL", defaultWebappBaseUrl)
    static {
        setupRestAssured()
    }

    private final List<String> classResourcesToDelete = []
    private final List<String> methodResourcesToDelete = []

    private static String toPortSuffix(String scheme, int port) {
        if (("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443)) {
            return ""
        }
        return ":$port"
    }

    @BeforeClass
    public void setUpClass() {
        setupRestAssured()
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    private static void setupRestAssured() {
        if (webappUrlPort != RestAssured.DEFAULT_PORT) {
            RestAssured.port = webappUrlPort
        }
        RestAssured.baseURI = webappBaseUrl
    }

    @BeforeTest
    public void setUp() {
        methodResourcesToDelete.clear() //fresh list per test
    }

    @AfterTest
    public void tearDown() {
        deleteResources(methodResourcesToDelete)
    }

    @AfterClass
    public void tearDownClass() {
        deleteResources(classResourcesToDelete)
    }

    protected static String uniqify(String s) {
        return s + UUID.randomUUID()
    }

    protected static String qualify(String uri) {
        return webappBaseUrl + uri
    }

    protected static XmlPath getHtmlDoc(Response response) {
        return new XmlPath(XmlPath.CompatibilityMode.HTML, response.getBody().asString());
    }

    private void deleteResources(List<String> hrefs) {
        //delete in opposite order - it's cleaner; children are deleted before parents
        hrefs.reverse().each { href ->
            try {
                given()
                    .log().all()
                    .header("Authorization", RestUtils.getBasicAuthorizationHeaderValue())
                    .header("User-Agent", "stormpath-framework-tck")
                .expect()
                    .statusCode(204)
                .when()
                     .delete(href)
            } catch (Throwable t) {
                log.error("Unable to delete specified resource: $href", t)
            }
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected void deleteOnTeardown(String resourceHref) {
        if (resourceHref) {
            this.methodResourcesToDelete.add(resourceHref)
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected void deleteOnClassTeardown(String resourceHref) {
        if (resourceHref) {
            this.classResourcesToDelete.add(resourceHref)
        }
    }
}
