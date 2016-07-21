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

package com.stormpath.tck.register

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.jayway.restassured.path.xml.element.NodeChildren
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.responseSpecs.AccountResponseSpec
import com.stormpath.tck.responseSpecs.JsonResponseSpec
import com.stormpath.tck.util.FrameworkConstants
import com.stormpath.tck.util.TestAccount
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.RestAssured.requestSpecification
import static com.stormpath.tck.util.FrameworkConstants.RegisterRoute
import static com.stormpath.tck.util.FrameworkConstants.getForgotRoute
import static com.stormpath.tck.util.HtmlUtils.assertAttributesEqual
import static com.stormpath.tck.util.Matchers.urlMatchesPath
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.both
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.hasKey
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not
import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertFalse

@Test
class RegisterIT extends AbstractIT {

    private testAccount

    @BeforeTest
    public void setUp() {
        super.setUp();
    }

    @BeforeMethod
    public void beforeEach() {
        testAccount = new TestAccount()
    }

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

    /**
     * Serve the registration view model for request type application/json
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/179">#179</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void registerServesJsonViewModel() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .get(RegisterRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body(".", hasKey("form"))
            .body(".", hasKey("accountStores"))
    }

    /**
     * Return JSON error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/189">#189</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void registerErrorsOnMissingRequiredFields() throws Exception {
        [
                //No surname
                [params: [email: testAccount.email, password: testAccount.password, givenName: testAccount.givenName], errorMsg: "Last Name is required."],
                //No givenName
                [params: [email: testAccount.email, password: testAccount.password, surname: testAccount.surname], errorMsg: "First Name is required."],
                //No email
                [params: [password: testAccount.password, givenName: testAccount.givenName, surname: testAccount.surname], errorMsg: "Email is required."],
                //No password
                [params: [email: testAccount.email, surname: testAccount.surname, givenName: testAccount.givenName], errorMsg: "Password is required."],
                //Empty body
                [params: [:], errorMsg: "required"/*The message only has the word required cause we don't know the order of the validation in each SDK*/]
        ].each { testCase ->
            // @formatter:off
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(testCase.params)
            .when()
                .post(RegisterRoute)
            .then()
                .body("message", containsString(testCase.errorMsg))
                .spec(JsonResponseSpec.isError(400))
            // @formatter:on
        }
    }

    /**
     * Return JSON error if undefined custom field is present
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/195">#195</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void registerErrorsForUndefinedFields() throws Exception {

        def jsonMap = [email: testAccount.email,
                       password: testAccount.password,
                       givenName: testAccount.givenName,
                       surname: testAccount.surname,
                       customValue: "foobar"]
        // field 'customValue' is not defined in the default configuration

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(jsonMap)
        .when()
            .post(RegisterRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    @Test(groups=["v100", "json"])
    public void registerErrorsForDisableDefaultFields() throws Exception {

        def jsonMap = [email: testAccount.email,
                       password: testAccount.password,
                       givenName: testAccount.givenName,
                       surname: testAccount.surname,
                       middleName: "foobar"]

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(jsonMap)
        .when()
            .post(RegisterRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    /**
     * Return JSON error if undefined custom field is present
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/195">#195</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void registerErrorsForUndefinedCustomFields() throws Exception {

        def jsonMap = [email: testAccount.email,
                       password: testAccount.password,
                       givenName: testAccount.givenName,
                       surname: testAccount.surname,
                       cusotmData: [ hello: "world" ]]

        // field 'hello' is not defined in the default configuration

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(jsonMap)
        .when()
            .post(RegisterRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    /**
     * Return JSON error if server error occurs
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/201">#201</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void registerErrorsOnServerError() throws Exception {

        def jsonMap = [email: "foo@bar",
                       password: "1",
                       givenName: testAccount.givenName,
                       surname: testAccount.surname]
        // Email and password will not pass Stormpath API validation and will error

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(jsonMap)
        .when()
            .post(RegisterRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    /**
     * Return sanitized JSON account on success
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/202">#202</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void registerReturnsSanitizedJsonAccount() throws Exception {

        def testAccount = new TestAccount()

        String createdHref =
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(testAccount.getPropertiesMap())
            .when()
                .post(RegisterRoute)
            .then()
                .spec(AccountResponseSpec.matchesAccount(testAccount))
            .extract()
                .path("account.href")

        deleteOnClassTeardown(createdHref)
    }

    /**
     * Serve a default HTML form for request type text/html
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/180">#180</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void registerServesHtmlForm() throws Exception {

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

        Node submitButton = findTagWithAttribute(doc.getNodeChildren("html.body"), "button", "type", "submit")
        assertEquals(submitButton.value(), "Create Account")
    }

    /** Default form should include fields ordered by fieldOrder
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/181">#181</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void registerFormShouldBeOrdered() throws Exception {
        def requiredAttributesList = [
                [name: "givenName", placeholder: "First Name", type: "text"],
                [name: "surname", placeholder: "Last Name", type: "text"],
                [name: "email", placeholder: "Email", type: "email"],
                [name: "password", placeholder: "Password", type: "password"]
        ]

        // Todo: CSRF support

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
        List<Node> fields = findTags(doc.getNodeChildren("html.body"), "input")

        assertAttributesEqual(fields, requiredAttributesList)
    }

    /**
     * Re-render form with error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/188">#188</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void registerErrorsWithEmptyPostBody() throws Exception {

        saveCSRFAndCookies(RegisterRoute)

        def requestSpecification =
            given()
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)

        setCSRFAndCookies(requestSpecification, ContentType.HTML);

        def response = requestSpecification
            .when()
                .post(RegisterRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "alert-danger")
        assertThat(warning.toString(), not(isEmptyOrNullString()))
    }

    /**
     * Re-render form with error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/188">#188</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void registerErrorsWithMissingRequiredFormFieldsHtml() throws Exception {
        [
                //No surname
                [params: [email: testAccount.email, password: testAccount.password, givenName: testAccount.givenName], errorMsg: "Last Name is required."],
                //No givenName
                [params: [email: testAccount.email, password: testAccount.password, surname: testAccount.surname], errorMsg: "First Name is required."],
                //No email
                [params: [password: testAccount.password, givenName: testAccount.givenName, surname: testAccount.surname], errorMsg: "Email is required."],
                //No password
                [params: [email: testAccount.email, surname: testAccount.surname, givenName: testAccount.givenName], errorMsg: "Password is required."],
                //No body
                [params: [:], errorMsg: "required"/*The message only has the word required cause we don't know the order of the validation in each SDK*/]
        ].each { testCase ->
            saveCSRFAndCookies(RegisterRoute)

            // @formatter:off
            def requestSpecification =
                given()
                    .accept(ContentType.HTML)
                    .contentType(ContentType.URLENC)
                    .formParameters(testCase.params)

            setCSRFAndCookies(requestSpecification, ContentType.HTML)

            def response = requestSpecification
                .when()
                    .post(RegisterRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.HTML)
                .extract()
                    .response()
            // @formatter:on

            XmlPath doc = getHtmlDoc(response)

            Node warning = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "alert-danger")
            assertThat(warning.toString(), containsString(testCase.errorMsg))
        }
    }

    /**
     * Re-render form with error if undefined custom field is present
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/194">#194</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void registerErrorsForUndefinedFieldsHtml() throws Exception {

        saveCSRFAndCookies(RegisterRoute);

        def requestSpecification =
            given()
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("email", testAccount.email)
                .formParam("password", testAccount.password)
                .formParam("givenName", testAccount.givenName)
                .formParam("surname", testAccount.surname)
                .formParam("customValue", "foobar") // not defined in default configuration

        setCSRFAndCookies(requestSpecification, ContentType.JSON);

        def response = requestSpecification
            .when()
                .post(RegisterRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "alert-danger")
        assertThat(warning.toString(), not(isEmptyOrNullString()))
    }

    /**
     * Re-render form and display error if server error occurs
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/200">#200</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void registerErrorsOnServerErrorHtml() throws Exception {

        saveCSRFAndCookies(RegisterRoute);

        def requestSpecification =
            given()
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("email", "foo@bar")
                .formParam("password", "1")
                .formParam("givenName", testAccount.givenName)
                .formParam("surname", testAccount.surname)
                // Email and password will not pass Stormpath API validation and will error

        setCSRFAndCookies(requestSpecification, ContentType.HTML)

        def response = requestSpecification
            .when()
                .post(RegisterRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "alert-danger")
        assertThat(warning.toString(), not(isEmptyOrNullString()))
    }

    /**
     * Redirect to login page with status=created on success
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/203">#203</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void registerRedirectToLoginOnSuccess() throws Exception {

        saveCSRFAndCookies(RegisterRoute)

        def req = given()
            .accept(ContentType.HTML)
            .contentType(ContentType.URLENC)
            .formParam("email", testAccount.email)
            .formParam("password", testAccount.password)
            .formParam("givenName", testAccount.givenName)
            .formParam("surname", testAccount.surname)


        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .post(RegisterRoute)
            .then()
                .statusCode(302)
                .header("Location", urlMatchesPath(FrameworkConstants.LoginRoute + "?status=created"))

        deleteOnTeardown(testAccount.href)
    }

    /** Preserve values in form fields on unsuccessful attempt
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/219">#219</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void registerFormPreservesValuesOnPostback() throws Exception {

        saveCSRFAndCookies(RegisterRoute)

        def requestSpecification =
            given()
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("email", testAccount.email)
                .formParam("password", "1") // Too short, will fail validation
                .formParam("givenName", testAccount.givenName)
                .formParam("surname", testAccount.surname)

        setCSRFAndCookies(requestSpecification, ContentType.HTML)

        def response = requestSpecification
            .when()
                .post(RegisterRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node loginField = findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "email")
        assertEquals(loginField.attributes().get("value"), testAccount.email, "The 'email' field should preserve value")

        Node givenNameField = findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "givenName")
        assertEquals(givenNameField.attributes().get("value"), testAccount.givenName, "The 'givenName' field should preserve value")

        Node surnameField = findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "surname")
        assertEquals(surnameField.attributes().get("value"), testAccount.surname, "The 'surname' field should preserve value")

        Node passwordField = findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "password")
        assertFalse((passwordField.attributes().get("value")?.trim() as boolean), "The 'password' field should NOT preserve value")
    }
}
