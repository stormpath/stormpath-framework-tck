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
import com.jayway.restassured.response.Response
import com.stormpath.tck.util.EnvUtils
import com.stormpath.tck.util.HtmlUtils
import com.stormpath.tck.util.RestUtils
import com.stormpath.tck.util.TestAccount
import io.jsonwebtoken.lang.Strings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.AfterSuite
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeSuite
import org.testng.annotations.BeforeTest

import static com.jayway.restassured.RestAssured.config
import static com.jayway.restassured.RestAssured.given
import static com.stormpath.tck.util.EnvUtils.getVal
import static com.stormpath.tck.util.FrameworkConstants.LoginRoute
import static com.stormpath.tck.util.FrameworkConstants.OauthRoute
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not

abstract class AbstractIT {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    static final String webappUrlScheme = getVal("STORMPATH_TCK_WEBAPP_SCHEME", "http")
    static final String webappUrlHost = getVal("STORMPATH_TCK_WEBAPP_HOST", "localhost")
    static final int webappUrlPort = getVal("STORMPATH_TCK_WEBAPP_PORT", "8080") as int
    static final private String webappUrlPortSuffix = toPortSuffix(webappUrlScheme, webappUrlPort)
    static final private String defaultWebappBaseUrl = "$webappUrlScheme://$webappUrlHost$webappUrlPortSuffix"
    static final String webappBaseUrl = getVal("STORMPATH_TCK_WEBAPP_URL", defaultWebappBaseUrl)

    static final private List<String> possibleCSRFKeys = ['_csrf', 'csrfToken', 'authenticity_token']

    static {
        setupRestAssured()
    }

    private final List<String> classResourcesToDelete = []
    private final List<String> methodResourcesToDelete = []
    private final List<String> methodAccountsToDelete = []

    String csrf
    String csrfKey
    Map<String, String> cookies

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
        csrf = null
        cookies = null
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

    protected void saveCSRFAndCookies(String endpoint) {
        if ("false".equals(EnvUtils.stormpathHtmlEnabled)) { return }
        
        def resp =
            given()
                .accept(ContentType.HTML)
            .when()
                .get(endpoint)
            .then()
                .statusCode(200)
            .extract()

        cookies = resp.cookies()

        def xmlPath = getHtmlDoc(resp)

        def form = HtmlUtils.findTagWithAttribute(xmlPath.get("html.body"), "form", "method", "post")

        String ret = null
        if (form != null) {
            def hiddens = HtmlUtils.findTagsWithAttribute(form.children(), "input", "type", "hidden")

            hiddens.each {
                if (possibleCSRFKeys.contains(it.getAttribute("name"))) {
                    csrfKey = it.getAttribute("name")
                    ret = it.getAttribute("value")
                    return true
                }
                return false
            }
        }

        csrf = ret
    }

    protected void setCSRFAndCookies(requestSpecification, contentType) {
        if ("false".equals(EnvUtils.stormpathHtmlEnabled)) { return }

        if (Strings.hasText(csrf) && ContentType.JSON.equals(contentType)) {
            requestSpecification.header("X-CSRF-TOKEN", csrf)
        }

        if (Strings.hasText(csrfKey) && Strings.hasText(csrf) && !ContentType.JSON.equals(contentType)) {
            requestSpecification.formParam(csrfKey, csrf);
        }

        if (cookies != null) {
            requestSpecification.cookies(cookies)
        }
    }

    protected Tuple2 createTestAccountTokens() {
        def account = new TestAccount()
        account.registerOnServer()

        def response =
                given()
                    .param("grant_type", "password")
                    .param("username", account.username)
                    .param("password", account.password)
                .when()
                    .post(OauthRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("access_token", not(isEmptyOrNullString()))
                .extract()
                    .response()
        deleteOnClassTeardown(account.href)

        return new Tuple2(response.path("access_token"), response.path("refresh_token"))
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

    protected Map<String, String> createSession(TestAccount account) throws Exception {

        Map<String, Object>  credentials = new HashMap<>();
        credentials.put("login", account.email)
        credentials.put("password", account.password)

        Map<String, String> cookies =
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(credentials)
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(200)
                .extract()
                .cookies()

        return cookies
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
