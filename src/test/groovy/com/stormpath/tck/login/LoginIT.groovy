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

import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
import static org.hamcrest.Matchers.*
import static org.testng.Assert.*

@Test
class LoginIT extends AbstractIT {

    private final String randomUUID = UUID.randomUUID().toString()
    private final String accountEmail = "fooemail-" + randomUUID + "@stormpath.com"
    private final String accountGivenName = "GivenName-" + randomUUID
    private final String accountSurname = "Surname-" + randomUUID
    private final String accountPassword = "P@sword123!"
    private final String accountUsername = "foo-" + randomUUID

    private final String loginPath = "/login"

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

    @BeforeClass
    private void createTestAccount() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("givenName", accountGivenName)
        jsonAsMap.put("surname", accountSurname)
        jsonAsMap.put("username", accountUsername)

        String createdHref =
                given()
                    .contentType(ContentType.JSON)
                    .body(jsonAsMap)
                .when()
                    .post("/register")
                .then()
                    .statusCode(200)
                .extract()
                    .path("account.href")

        deleteOnClassTeardown(createdHref)
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
                .get(loginPath)
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
                .post(loginPath)
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
            .post(loginPath)
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
            .post(loginPath)
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
            .post(loginPath)
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
            .post(loginPath)
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
            .post(loginPath)
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
                .get(loginPath)
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
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/102">#103</a>
     * @throws Exception
     */
    @Test
    public void rendersVerifiedMessage() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
                .queryParam("status", "verified")
            .when()
                .get(loginPath)
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
                    .get(loginPath)
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
                    .get(loginPath)
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
                    .get(loginPath)
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
                    .get(loginPath)
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
                .post(loginPath)
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

        Response response =
            given()
                .accept(ContentType.HTML)
            .when()
                .get(loginPath)
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
                    .post(loginPath)
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