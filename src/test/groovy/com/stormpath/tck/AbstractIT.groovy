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
import com.jayway.restassured.config.RedirectConfig
import com.jayway.restassured.http.ContentType
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.jayway.restassured.path.xml.element.NodeChildren
import com.jayway.restassured.response.Response
import com.stormpath.tck.util.RestUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.*

import static com.jayway.restassured.RestAssured.*
import static com.stormpath.tck.util.EnvUtils.getVal
import static com.stormpath.tck.util.FrameworkConstants.RegisterRoute

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
    private final List<String> methodAccountsToDelete = []

    private static String toPortSuffix(String scheme, int port) {
        if (("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443)) {
            return ""
        }
        return ":$port"
    }

    @BeforeSuite
    public void setUpClass() {
        setupRestAssured()
    }

    @SuppressWarnings("UnnecessaryQualifiedReference")
    private static void setupRestAssured() {
        if (webappUrlPort != RestAssured.DEFAULT_PORT) {
            RestAssured.port = webappUrlPort
        }
        RestAssured.baseURI = webappBaseUrl
        RestAssured.config = config().redirect(RedirectConfig.redirectConfig().followRedirects(false))
    }

    @BeforeTest
    public void setUp() {
        methodResourcesToDelete.clear() //fresh list per test
    }

    @AfterTest
    public void tearDown() {
        deleteResources(methodResourcesToDelete)
        deleteAccounts(methodAccountsToDelete)
    }

    @AfterSuite
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

    protected String getCsrfToken() {
        //We can get the CSRF token from any endpoint if not found then don't send it
        Response response =
                given()
                        .accept(ContentType.HTML)
                        .when()
                        .get(RegisterRoute)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.HTML)
                        .extract()
                        .response()

        XmlPath doc = getHtmlDoc(response)

        Node csrfInput = findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "csrfToken")

        return csrfInput == null ? null : csrfInput.getAttribute("value")
    }

    protected Node findTagWithAttribute(NodeChildren children, String tag, String attributeKey, String attributeValue) {
        for (Node node : children.list()) {
            def actualTag = node.name()
            def actualAttribute = node.attributes().get(attributeKey)

            if (actualTag == tag && actualAttribute.contains(attributeValue)) {
                return node
            }
            else {
                Node foundNode = findTagWithAttribute(node.children(), tag, attributeKey, attributeValue)
                if (foundNode != null) {
                    return foundNode
                }
            }
        }
    }

    protected List<Node> findTags(NodeChildren children, String tag) {
        def results = new ArrayList<Node>()

        for (Node node in children.list()) {
            if (node.name() == tag) {
                results.add(node)
            }
            else {
                Collection<Node> innerResults = findTags(node.children(), tag)
                results.addAll(innerResults)
            }
        }

        return results
    }

    private void deleteResources(List<String> hrefs) {
        //delete in opposite order - it's cleaner; children are deleted before parents
        hrefs.reverse().each { href ->
            try {
                given()
                    .header("Authorization", RestUtils.getBasicAuthorizationHeaderValue())
                    .header("User-Agent", "stormpath-framework-tck")
                    .port(443)
                .expect()
                    .statusCode(204)
                .when()
                     .delete(href)
            } catch (Throwable t) {
                log.error("Unable to delete specified resource: $href", t)
            }
        }
    }

    private void deleteAccounts(List<String> emails) {
        // todo
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected void deleteOnTeardown(String resourceHref) {
        if (resourceHref) {
            this.methodResourcesToDelete.add(resourceHref)
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected void deleteAccountOnTeardown(String accountEmail) {
        if (accountEmail) {
            this.methodAccountsToDelete.add(accountEmail)
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected void deleteOnClassTeardown(String resourceHref) {
        if (resourceHref) {
            this.classResourcesToDelete.add(resourceHref)
        }
    }
}
