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

package com.stormpath.tck.login

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.responseSpecs.AccountResponseSpec
import com.stormpath.tck.responseSpecs.JsonResponseSpec
import com.stormpath.tck.util.FrameworkConstants
import com.stormpath.tck.util.HtmlUtils
import com.stormpath.tck.util.Iso8601Utils
import com.stormpath.tck.util.TestAccount
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.delete
import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.RestAssured.put
import static com.stormpath.tck.util.FrameworkConstants.LoginRoute
import static com.stormpath.tck.util.HtmlUtils.assertAttributesEqual
import static com.stormpath.tck.util.Matchers.urlMatchesPath
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.hasKey
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not
import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertFalse
import static org.testng.Assert.assertTrue

@Test
class LoginIT extends AbstractIT {
    private TestAccount account = new TestAccount()

    private String getNodeText(Node node, boolean addContentsFirst) {
        StringBuilder builder = new StringBuilder()

        if (addContentsFirst){
            if (node.value() != null) {
                builder.append(node.value())
            }
        }

        for (Node child in node.children().list()){
            builder.append(getNodeText(child, addContentsFirst))
        }

        if (!addContentsFirst){
            if (node.value() != null) {
                builder.append(node.value())
            }
        }
        return builder
                .toString()
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+\$", "")
    }

    private Map getJsonCredentials() {
        Map<String, Object>  credentials = new HashMap<>();

        credentials.put("login", account.email)
        credentials.put("password", account.password)

        return credentials
    }

    @BeforeClass
    private void createTestAccount() throws Exception {
        account.registerOnServer()
        deleteOnClassTeardown(account.href)
    }

    /** Only respond to GET and POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/85">#85</a>
     */
    @Test(groups=["v100", "json", "html"])
    public void loginDoesNotHandlePut() throws Exception {
        put(LoginRoute)
            .then()
                .assertThat().statusCode(allOf(not(200), not(500)))
    }

    /** Only respond to GET and POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/85">#85</a>
     */
    @Test(groups=["v100", "json", "html"])
    public void loginDoesNotHandleDelete() throws Exception {
        delete(LoginRoute)
            .then()
                .assertThat().statusCode(allOf(not(200), not(500)))
    }

    /**
     * Serve the login view model for request type application/json
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/83">#83</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginServesJsonViewModel() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .get(LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body(".", hasKey("form"))
            .body(".", hasKey("accountStores"))
    }

    /**
     * Login view model should have a list of fields ordered by fieldOrder
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/89">#89</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginViewModelHasFields() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .get(LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("form.fields.size()", is(2))
            .body("form.fields[0].label", is("Username or Email"))
            .body("form.fields[0].name", is("login"))
            .body("form.fields[0].placeholder", is("Username or Email"))
            .body("form.fields[0].required", is(true))
            .body("form.fields[0].type", is("text"))
            .body("form.fields[1].label", is("Password"))
            .body("form.fields[1].name", is("password"))
            .body("form.fields[1].placeholder", is("Password"))
            .body("form.fields[1].required", is(true))
            .body("form.fields[1].type", is("password"))
        // Default view model based on configuration
    }

    /** Login value can either be username or email
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/93">#93</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginWithUsernameSucceeds() throws Exception {

        Map<String, Object>  credentials = new HashMap<>();
        credentials.put("login", account.username)
        credentials.put("password", account.password)

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(credentials)
        .when()
            .post(LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(".", hasKey("account"))
    }

    /** Login value can either be username or email
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/93">#93</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginWithEmailSucceeds() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getJsonCredentials())
        .when()
            .post(LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(".", hasKey("account"))
    }

    /** Omitting login or password when posting JSON results in an error
     * Errors rendered as JSON include only message and status properties
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/95">#95</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginWithEmptyStringFails() throws Exception {

        Map<String, Object> badCredentials = new HashMap<>();

        badCredentials.put("login", "")
        badCredentials.put("password", "foo")

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(badCredentials)
        .when()
            .post(LoginRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    /** Omitting login or password when posting JSON results in an error
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/95">#95</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginWithEmptyPasswordFails() throws Exception {

        Map<String, Object> badCredentials = new HashMap<>();

        badCredentials.put("login", "foo@foo.bar")
        badCredentials.put("password", "")

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(badCredentials)
        .when()
            .post(LoginRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    /**
     * Return account JSON on successful authorization for application/json
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/100">#100</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginSucceedsForJson() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getJsonCredentials())
        .when()
            .post(LoginRoute)
        .then()
            .spec(AccountResponseSpec.matchesAccount(account))
    }

    /**
     * Remove all linked resources from JSON account response
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/101">#101</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginJsonDoesNotHaveLinkedResources() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getJsonCredentials())
        .when()
            .post(LoginRoute)
        .then()
            .spec(AccountResponseSpec.withoutLinkedResources())
    }

    /**
     * Datetime fields in JSON account response should be serialized as ISO 8601
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/108">#108</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginAccountDatetimePropertiesAreIso8601() throws Exception {
        saveCSRFAndCookies(LoginRoute)

        def req =
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(getJsonCredentials())

        setCSRFAndCookies(req, ContentType.JSON)

        Response response = req
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
            .extract()
                .body()

        // Clunky way of doing things because this version of hamcrest doesn't have a regex matcher
        String created = response.path("account.createdAt")
        String modified = response.path("account.modifiedAt")
        assertTrue(created.matches(Iso8601Utils.Pattern))
        assertTrue(modified.matches(Iso8601Utils.Pattern))
    }

    /**
     * Errors returned as JSON use API status and response (#45)
     * Return JSON error from API if JSON login is unsuccessful (#110)
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/45">#45</a>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/45">#110</a>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/28">#28</a>
     */
    @Test(groups=["v100", "json"])
    public void loginErrorsWithBadCredentialsJson() throws Exception {

        Map<String, Object> badCredentials = new HashMap<>();

        badCredentials.put("login", "foo@foo.bar")
        badCredentials.put("password", "pwn4g3!!1")

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(badCredentials)
        .when()
            .post(LoginRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    /**
     * JSON response should set OAuth 2.0 cookies on successful login
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/168">#168</a>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/33">#33</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void loginSetsCookiesJson() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getJsonCredentials())
        .when()
            .post(LoginRoute)
        .then()
            .cookie("access_token", not(isEmptyOrNullString()))
            .cookie("refresh_token", not(isEmptyOrNullString()))
    }

    /** Serve a default HTML page with a login form for request type text/html
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/81">#81</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginServesHtmlForm() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
            .when()
                .get(LoginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node loginField = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "login")
        assertEquals(loginField.attributes().get("type"), "text")

        Node passwordField = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "password")
        assertEquals(passwordField.attributes().get("type"), "password")
    }

    /** Default HTML form must require username and password
     *  Omitting login or password will render the form with an error
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/86">#86</a>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/94">#94</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginHtmlRendersErrorWithoutUsernameAndPassword() throws Exception {

        saveCSRFAndCookies(LoginRoute)

        def req =
            given()
                .accept(ContentType.HTML)
                .formParam("foo", "bar")

        setCSRFAndCookies(req, ContentType.HTML)

        Response response = req
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "bad-login")
        assertThat(warning.toString(), not(isEmptyOrNullString()))
    }

    /** Default HTML form should set OAuth 2.0 cookies on successful login
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/88">#88</a>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/33">#33</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginSetsCookiesHtml() throws Exception {

        saveCSRFAndCookies(LoginRoute)

        def req = given()
            .accept(ContentType.HTML)
            .formParam("login", account.email)
            .formParam("password", account.password)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .post(LoginRoute)
            .then()
                .cookie("access_token", not(isEmptyOrNullString()))
                .cookie("refresh_token", not(isEmptyOrNullString()))
    }

    /** Login value can either be username or email
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/92">#92</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginWithEmailSucceedsHtml() throws Exception {
        saveCSRFAndCookies(LoginRoute)

        def req = given()
            .accept(ContentType.HTML)
            .formParam("login", account.email)
            .formParam("password", account.password)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(302)
    }

    /** Login value can either be username or email
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/92">#92</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginWithUsernameSucceedsHtml() throws Exception {
        saveCSRFAndCookies(LoginRoute)

        def req = given()
            .accept(ContentType.HTML)
            .formParam("login", account.username)
            .formParam("password", account.password)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(302)
    }

    /** Redirect to nextUri on successful authorization
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/97">#97</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginRedirectsToNextUriOnSuccess() throws Exception {

        saveCSRFAndCookies(LoginRoute)

        def req = given()
            .accept(ContentType.HTML)
            .formParam("login", account.email)
            .formParam("password", account.password)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(302)
                .header("Location", containsString("/"))
    }

    /** Render unverified status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/102">#102</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginRendersUnverifiedMessage() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
                .queryParam("status", "unverified")
            .when()
                .get(LoginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertThat(getNodeText(header, false), not(isEmptyOrNullString()))
    }

    /** Render verified status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/103">#103</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginRendersVerifiedMessage() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
                .queryParam("status", "verified")
            .when()
                .get(LoginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertThat(getNodeText(header, false), not(isEmptyOrNullString()))
    }

    /** Render created status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/104">#104</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginRendersCreatedMessage() throws Exception {

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .queryParam("status", "created")
                .when()
                    .get(LoginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertThat(getNodeText(header, false), not(isEmptyOrNullString()))
    }

    /** Render forgot status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/105">#105</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginRendersForgotMessage() throws Exception {

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .queryParam("status", "forgot")
                .when()
                    .get(LoginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertThat(getNodeText(header, false), not(isEmptyOrNullString()))
    }

    /** Render reset status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/106">#106</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginRendersResetMessage() throws Exception {

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .queryParam("status", "reset")
                .when()
                    .get(LoginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertThat(getNodeText(header, false), not(isEmptyOrNullString()))
    }

    /** Ignore bogus status query values
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/107">#107</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginDoesNotRenderWrongStatusParameter() throws Exception {

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .queryParam("status", "foobar")
                .when()
                    .get(LoginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")

        // The only header div should be the one that contains the form header
        assertThat(getNodeText(header, true), not(isEmptyOrNullString()))
    }

    /** Rerender form with error UX if login is unsuccessful
     * Errors rendered to an HTML view use the API message property
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/109">#109</a>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/44">#44</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginRendersErrorOnFailure() throws Exception {

        saveCSRFAndCookies(LoginRoute)

        def requestSpecification =
            given()
                .accept(ContentType.HTML)
                .formParam("login", "blah")
                .formParam("password", "foobar!")

        setCSRFAndCookies(requestSpecification, ContentType.HTML)

        def response = requestSpecification
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "bad-login")
        assertThat(warning.toString(), not(isEmptyOrNullString()))
    }

    /** HTML form should contain fields ordered by fieldOrder
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/114">#114</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginFormShouldBeOrderedCorrectly() throws Exception {
        def requiredAttributesList = [
                [name: "login", placeholder: "Username or Email", type: "text"],
                [name: "password", placeholder: "Password", type: "password"]
        ]

        Response response =
            given()
                .accept(ContentType.HTML)
            .when()
                .get(LoginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)
        List<Node> fields = HtmlUtils.findTags(doc.getNodeChildren("html.body"), "input")

        assertAttributesEqual(fields, requiredAttributesList)
    }

    /** Preserve value in login field on unsuccessful attempt
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/177">#177</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void loginFormPreservesValuesOnPostback() throws Exception {

        saveCSRFAndCookies(LoginRoute)

        def req =
            given()
                .accept(ContentType.HTML)
                .formParam("login", "blah")
                .formParam("password", "1")

        setCSRFAndCookies(req, ContentType.HTML)

        Response response = req
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node loginField = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "login")
        assertEquals(loginField.attributes().get("value"), "blah", "The 'login' field should preserve value")

        Node passwordField = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "password")
        assertFalse((passwordField.attributes().get("value")?.trim() as boolean), "The 'password' field should NOT preserve value")
    }

    /**
     * When trying to access an endpoint that requires authentication then the login page should kick in and
     * the user should be redirected back to that original endpoint after a successful login.
     * <p>
     * {@link #loginRedirectsToNextUriOnSuccess()} is already checking that. However, in our Java SDK
     * we had <a href="https://github.com/stormpath/stormpath-sdk-java/issues/718">this issue<a> where Spring Security
     * was always redirecting the user to the very first intercepted URI (clarification: "intercepted" as in user is redirected to
     * login when trying to access an URI that requires authenticated access and the user is not logged in).
     * </p>
     * <p>
     * Although this issues existed only in JSDK the behaviour tested here is standard for every SDK and it will help
     * detect an eventual similar issue in other SDKs.
     * </p>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/274">#274</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void testNoRedirectionStickinessHtml() throws Exception {

        saveCSRFAndCookies("/")

        //Trying to access the "/me" endpoint without authentication; we must be redirected to login
        def req = given()
            .accept(ContentType.HTML)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .get(FrameworkConstants.MeRoute)
            .then()
                .statusCode(302)
                .header("Location", urlMatchesPath("/login?next=%2Fme")) //we must be redirected to login

        //Let's not login for now. The web-app must "forget" about the intercepted URI now.
        req = given()
            .accept(ContentType.HTML)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .get("/")
            .then()
                .statusCode(200)

        //Let's login, we should be redirect to "/" rather than to "/me"

        saveCSRFAndCookies(LoginRoute)

        req = given()
            .cookies(cookies)
            .accept(ContentType.HTML)
            .formParam("login", account.email)
            .formParam("password", account.password)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(302)
                .header("Location", not(urlMatchesPath(FrameworkConstants.MeRoute)))
    }
}
