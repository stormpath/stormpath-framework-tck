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
import com.jayway.restassured.path.xml.element.NodeChildren
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.Iso8601Utils
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.delete
import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.RestAssured.head
import static com.jayway.restassured.RestAssured.options
import static com.jayway.restassured.RestAssured.patch
import static com.jayway.restassured.RestAssured.put
import static org.hamcrest.Matchers.*
import static org.testng.Assert.*

@Test
class LoginIT extends AbstractIT {

    private final String randomUUID = UUID.randomUUID().toString()
    private final String accountEmail = "fooemail-" + randomUUID + "@stormpath.com"
    private final String accountGivenName = "GivenName-" + randomUUID
    private final String accountSurname = "Surname-" + randomUUID
    private final String accountMiddleName = "Foobar"
    private final String accountPassword = "P@sword123!"
    private final String accountUsername = "foo-" + randomUUID

    private final String loginRoute = "/login"
    private final String registerRoute = "/register"

    private Node findTagWithAttribute(NodeChildren children, String tag, String attributeKey, String attributeValue) {
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

    private List<Node> findTags(NodeChildren children, String tag) {
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

        credentials.put("login", accountEmail)
        credentials.put("password", accountPassword)
        return credentials
    }

    @BeforeClass
    private void createTestAccount() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("givenName", accountGivenName)
        jsonAsMap.put("surname", accountSurname)
        jsonAsMap.put("username", accountUsername)
        jsonAsMap.put("middleName", accountMiddleName)

        String createdHref =
                given()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(jsonAsMap)
                .when()
                    .post(registerRoute)
                .then()
                    .statusCode(200)
                .extract()
                    .path("account.href")

        deleteOnClassTeardown(createdHref)
    }

    /** Anything but GET or POST should return 405
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/85">#85</a>
     */
    @Test
    public void doNotHandleHead() throws Exception {
        head(loginRoute)
            .then()
                .assertThat().statusCode(405)
    }

    /** Anything but GET or POST should return 405
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/85">#85</a>
     */
    @Test
    public void doNotHandlePut() throws Exception {
        put(loginRoute)
            .then()
                .assertThat().statusCode(405)
    }

    /** Anything but GET or POST should return 405
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/85">#85</a>
     */
    @Test
    public void doNotHandleDelete() throws Exception {
        delete(loginRoute)
            .then()
                .assertThat().statusCode(405)
    }

    /** Anything but GET or POST should return 405
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/85">#85</a>
     */
    @Test
    public void doNotHandleOptions() throws Exception {
        options(loginRoute)
            .then()
                .assertThat().statusCode(405)
    }

    /** Anything but GET or POST should return 405
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/85">#85</a>
     */
    @Test
    public void doNotHandlePatch() throws Exception {
        patch(loginRoute)
            .then()
                .assertThat().statusCode(405)
    }

    /**
     * Serve the login view model for request type application/json
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/83">#83</a>
     * @throws Exception
     */
    @Test
    public void servesLoginViewModel() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .get(loginRoute)
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
    @Test
    public void loginViewModelHasFields() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .get(loginRoute)
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
    @Test
    public void loginWithUsername() throws Exception {

        Map<String, Object>  credentials = new HashMap<>();
        credentials.put("login", accountUsername)
        credentials.put("password", accountPassword)

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(credentials)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(".", hasKey("account"))
    }

    /** Login value can either be username or email
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/93">#93</a>
     * @throws Exception
     */
    @Test
    public void loginWithEmail() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getJsonCredentials())
        .when()
            .post(loginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(".", hasKey("account"))
    }

    /** Omitting login or password when posting JSON results in an error
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/95">#95</a>
     * @throws Exception
     */
    @Test
    public void missingLoginThrowsError() throws Exception {

        Map<String, Object> badCredentials = new HashMap<>();

        badCredentials.put("login", "")
        badCredentials.put("password", "foo")

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(badCredentials)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("status", is(400))
            .body("message", is("Missing login or password."))
    }

    /** Omitting login or password when posting JSON results in an error
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/95">#95</a>
     * @throws Exception
     */
    @Test
    public void missingPasswordThrowsError() throws Exception {

        Map<String, Object> badCredentials = new HashMap<>();

        badCredentials.put("login", "foo@foo.bar")
        badCredentials.put("password", "")

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(badCredentials)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("status", is(400))
            .body("message", is("Missing login or password."))
    }

    /**
     * Return account JSON on successful authorization for application/json
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/100">#100</a>
     * @throws Exception
     */
    @Test
    public void successfulAuthorization() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getJsonCredentials())
        .when()
            .post(loginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(1))
            .body("account.href", not(isEmptyOrNullString()))
            .body("account.username", is(accountUsername))
            .body("account.modifiedAt", not(isEmptyOrNullString())) // #108 ensures ISO 8601
            .body("account.status", equalToIgnoringCase("ENABLED"))
            .body("account.createdAt", not(isEmptyOrNullString())) // #108 ensures ISO 8601
            .body("account.email", is(accountEmail))
            .body("account.middleName", is(accountMiddleName))
            .body("account.surname", is(accountSurname))
            .body("account.givenName", is(accountGivenName))
            .body("account.fullName", is("$accountGivenName $accountMiddleName $accountSurname".toString()))
    }

    /**
     * Remove all linked resources from JSON account response
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/101">#101</a>
     * @throws Exception
     */
    @Test
    public void noLinkedResourcesPresent() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getJsonCredentials())
        .when()
            .post(loginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("account.size()", is(10))
            // Todo: there might be an easier way to assert "it has none of these keys", but I'm a restAssured/hamcrest n00b
            .body("account.emailVerificationToken", is(nullValue()))
            .body("account.customData", is(nullValue()))
            .body("account.providerData", is(nullValue()))
            .body("account.directory", is(nullValue()))
            .body("account.tenant", is(nullValue()))
            .body("account.groups", is(nullValue()))
            .body("account.groupMemberships", is(nullValue()))
            .body("account.applications", is(nullValue()))
            .body("account.apiKeys", is(nullValue()))
            .body("account.accessTokens", is(nullValue()))
            .body("account.refreshTokens", is(nullValue()))
    }

    /**
     * Datetime fields in JSON account response should be serialized as ISO 8601
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/108">#108</a>
     * @throws Exception
     */
    @Test
    public void datetimePropertiesAreIso8601() throws Exception {

        Response response =
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(getJsonCredentials())
            .when()
                .post(loginRoute)
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
     */
    @Test
    public void invalidLoginError() throws Exception {

        Map<String, Object> badCredentials = new HashMap<>();

        badCredentials.put("login", "foo@foo.bar")
        badCredentials.put("password", "pwn4g3!!1")

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(badCredentials)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("status", is(400))
            .body("message", is("Invalid username or password."))
    }

    /**
     * JSON response should set OAuth 2.0 cookies on successful login
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/168">#168</a>
     * @throws Exception
     */
    @Test
    public void setsCookiesOnJsonLogin() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getJsonCredentials())
        .when()
            .post(loginRoute)
        .then()
            .cookie("access_token", not(isEmptyOrNullString()))
            .cookie("refresh_token", not(isEmptyOrNullString()))
    }

    /** Serve a default HTML page with a login form for request type text/html
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/81">#81</a>
     * @throws Exception
     */
    @Test
    public void servesLoginForm() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
            .when()
                .get(loginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node loginField = findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "login")
        assertEquals(loginField.attributes().get("type"), "text")

        Node passwordField = findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "password")
        assertEquals(passwordField.attributes().get("type"), "password")
    }

    /** Default HTML form must require username and password
     *  Omitting login or password will render the form with an error
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/86">#86</a>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/94">#94</a>
     * @throws Exception
     */
    @Test
    public void loginAndPasswordAreRequired() throws Exception {

        // todo: work with CSRF

        Response response =
            given()
                .accept(ContentType.HTML)
                .formParam("foo", "bar")
            .when()
                .post(loginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "bad-login")
        assertEquals(warning.toString(), "The login and password fields are required.")
    }

    /** Default HTML form should set OAuth 2.0 cookies on successful login
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/88">#88</a>
     * @throws Exception
     */
    @Test
    public void setsCookiesOnLogin() throws Exception {

        // todo: work with CSRF

        given()
            .accept(ContentType.HTML)
            .formParam("login", accountEmail)
            .formParam("password", accountPassword)
        .when()
            .post(loginRoute)
        .then()
            .cookie("access_token", not(isEmptyOrNullString()))
            .cookie("refresh_token", not(isEmptyOrNullString()))
    }

    /** Login value can either be username or email
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/92">#92</a>
     * @throws Exception
     */
    @Test
    public void loginValueCanBeEmail() throws Exception {

        // todo: work with CSRF

        given()
            .accept(ContentType.HTML)
            .formParam("login", accountEmail)
            .formParam("password", accountPassword)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(302)
    }

    /** Login value can either be username or email
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/92">#92</a>
     * @throws Exception
     */
    @Test
    public void loginValueCanBeUsername() throws Exception {

        // todo: work with CSRF

        given()
            .accept(ContentType.HTML)
            .formParam("login", accountUsername)
            .formParam("password", accountPassword)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(302)
    }

    /** Redirect to nextUri on successful authorization
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/97">#97</a>
     * @throws Exception
     */
    @Test
    public void redirectsToNextUriOnLogin() throws Exception {

        // todo: work with CSRF

        given()
            .accept(ContentType.HTML)
            .formParam("login", accountEmail)
            .formParam("password", accountPassword)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(302)
            .header("Location", is("/"))
    }

    /** Redirect to URI specified by next query parameter on successful authorization
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/99">#99</a>
     * @throws Exception
     */
    @Test
    public void redirectsToNextParameter() throws Exception {

        // todo: work with CSRF

        given()
            .accept(ContentType.HTML)
            .formParam("login", accountEmail)
            .formParam("password", accountPassword)
            .queryParam("next", "/foo")
        .when()
            .post(loginRoute)
        .then()
            .statusCode(302)
            .header("Location", is("/foo"))
    }

    /** Render unverified status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/102">#102</a>
     * @throws Exception
     */
    @Test
    public void rendersUnverifiedMessage() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
                .queryParam("status", "unverified")
            .when()
                .get(loginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertTrue(getNodeText(header, false).startsWith("Your account verification email has been sent! Before you can log into your account, you need to activate your account by clicking the link we sent to your inbox."))
        // todo: groovy's HTML parsing sux. need to figure out how to pull the full text, not just whatever groovy chooses to see. :(
    }

    /** Render verified status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/103">#103</a>
     * @throws Exception
     */
    @Test
    public void rendersVerifiedMessage() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
                .queryParam("status", "verified")
            .when()
                .get(loginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertEquals(getNodeText(header, false), "Your Account Has Been Verified. You may now login.")
    }

    /** Render created status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/104">#104</a>
     * @throws Exception
     */
    @Test
    public void rendersCreatedMessage() throws Exception {

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .queryParam("status", "created")
                .when()
                    .get(loginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertEquals(getNodeText(header, false), "Your Account Has Been Created. You may now login.")
    }

    /** Render forgot status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/105">#105</a>
     * @throws Exception
     */
    @Test
    public void rendersForgotMessage() throws Exception {

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .queryParam("status", "forgot")
                .when()
                    .get(loginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertEquals(getNodeText(header, false), "Password Reset Requested. If an account exists for the email provided, you will receive an email shortly.")
    }

    /** Render reset status message
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/106">#106</a>
     * @throws Exception
     */
    @Test
    public void rendersResetMessage() throws Exception {

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .queryParam("status", "reset")
                .when()
                    .get(loginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")
        assertEquals(getNodeText(header, false), "Password Reset Successfully. You can now login with your new password.")
    }

    /** Ignore bogus status query values
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/107">#107</a>
     * @throws Exception
     */
    @Test
    public void ignoreArbitraryStatus() throws Exception {

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .queryParam("status", "foobar")
                .when()
                    .get(loginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node header = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "header")

        // The only header div should be the one that contains the form header
        assertEquals(getNodeText(header, true), "Log In or Create Account")
    }

    /** Rerender form with error UX if login is unsuccessful
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/109">#109</a>
     * @throws Exception
     */
    @Test
    public void rerenderFormWithErrorIfLoginFails() throws Exception {

        // todo: work with CSRF

        Response response =
            given()
                .accept(ContentType.HTML)
                .formParam("login", "blah")
                .formParam("password", "foobar!")
            .when()
                .post(loginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "bad-login")
        assertEquals(warning.toString(), "Invalid username or password.")
    }

    /** HTML form should contain fields ordered by fieldOrder
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/114">#114</a>
     * @throws Exception
     */
    @Test
    public void formShouldContainFieldsOrderedByFieldOrder() throws Exception {

        // todo: better CSRF handling

        Response response =
            given()
                .accept(ContentType.HTML)
            .when()
                .get(loginRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)
        List<Node> fields = findTags(doc.getNodeChildren("html.body"), "input")

        // From default configuration
        assertEquals(fields.get(0).attributes().get("name"), "login")
        assertEquals(fields.get(0).attributes().get("placeholder"), "Username or Email")
        assertEquals(fields.get(0).attributes().get("type"), "text")

        assertEquals(fields.get(1).attributes().get("name"), "password")
        assertEquals(fields.get(1).attributes().get("placeholder"), "Password")
        assertEquals(fields.get(1).attributes().get("type"), "password")
    }

    /** Preserve value in login field on unsuccessful attempt
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/177">#177</a>
     * @throws Exception
     */
    @Test
    public void preserveValueInLoginFieldOnAttempt() throws Exception {

        // todo: work with CSRF

        Response response =
                given()
                    .accept(ContentType.HTML)
                    .formParam("login", "blah")
                    .formParam("password", "")
                .when()
                    .post(loginRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()

        XmlPath doc = getHtmlDoc(response)

        Node loginField = findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "login")
        assertEquals(loginField.attributes().get("value"), "blah")
    }
}