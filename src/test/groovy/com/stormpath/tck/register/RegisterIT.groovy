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
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.testng.Assert.*

@Test
class RegisterIT extends AbstractIT {

    private static final String registerRoute = "/register"
    private static final String loginRoute = "/login"

    private final String randomUUID = UUID.randomUUID().toString()
    private final String accountEmail = "fooemail-$randomUUID@stormpath.com"
    private final String accountGivenName = "GivenName-$randomUUID"
    private final String accountSurname = "Surname-$randomUUID"
    private final String accountMiddleName = "Foobar"
    private final String accountPassword = "P@sword123!"
    private final String accountUsername = "foo-$randomUUID"

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
    @Test
    public void servesViewModel() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .get(registerRoute)
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
    @Test
    public void returnsErrorIfPostIsEmpty() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .post(registerRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/189">#189</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorIfEmailIsMissing() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", "")


        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/189">#189</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorIfRequiredFieldIsMissing() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("surname", accountSurname)
        // givenName is required per the default configuration

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if undefined custom field is present
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/195">#195</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorForUndefinedRootCustomField() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("givenName", accountGivenName)
        jsonAsMap.put("surname", accountSurname)
        jsonAsMap.put("customValue", "foobar")
        // field 'customValue' is not defined in the default configuration

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if undefined custom field is present
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/195">#195</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorForUndefinedCustomField() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("givenName", accountGivenName)
        jsonAsMap.put("surname", accountSurname)

        Map<String, Object> customDataMap = new HashMap<>();
        customDataMap.put("hello", "world")

        jsonAsMap.put("customData", customDataMap)
        // field 'hello' is not defined in the default configuration

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if server error occurs
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/201">#201</a>
     * @throws Exception
     */
    @Test
    public void returnsJsonErrorForServerError() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", "foo@bar")
        jsonAsMap.put("password", "1")
        jsonAsMap.put("givenName", accountGivenName)
        jsonAsMap.put("surname", accountSurname)
        // Email and password will not pass Stormpath API validation and will error

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return sanitized JSON account on success
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/202">#202</a>
     * @throws Exception
     */
    @Test
    public void returnsSanitizedAccountForSuccess() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", "json-$accountEmail")
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("givenName", accountGivenName)
        jsonAsMap.put("middleName", accountMiddleName)
        jsonAsMap.put("surname", accountSurname)
        jsonAsMap.put("username", accountUsername)

        String createdHref =
            given()
                .accept(ContentType.JSON)
                .body(jsonAsMap)
            .when()
                .post(registerRoute)
            .then()
                .contentType(ContentType.JSON)
                .body("size()", is(1))
                .body("account.href", not(isEmptyOrNullString()))
                .body("account.username", is(accountUsername))
                .body("account.modifiedAt", not(isEmptyOrNullString()))
                .body("account.status", equalToIgnoringCase("ENABLED"))
                .body("account.createdAt", not(isEmptyOrNullString()))
                .body("account.email", is("json-$accountEmail"))
                .body("account.middleName", is(accountMiddleName))
                .body("account.surname", is(accountSurname))
                .body("account.givenName", is(accountGivenName))
                .body("account.fullName", is("$accountGivenName $accountMiddleName $accountSurname".toString()))
            .extract()
                .path("account.href")

        deleteOnClassTeardown(createdHref)
    }

    /**
     * Serve a default HTML form for request type text/html
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/180">#180</a>
     * @throws Exception
     */
    @Test
    public void servesRegisterForm() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
            .when()
                .get(registerRoute)
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
    @Test
    public void formShouldContainFieldsOrderedByFieldOrder() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
            .when()
                .get(registerRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
            .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)
        List<Node> fields = findTags(doc.getNodeChildren("html.body"), "input")

        // From default configuration
        assertEquals(fields.get(0).attributes().get("name"), "givenName")
        assertEquals(fields.get(0).attributes().get("placeholder"), "First Name")
        assertEquals(fields.get(0).attributes().get("type"), "text")

        assertEquals(fields.get(1).attributes().get("name"), "surname")
        assertEquals(fields.get(1).attributes().get("placeholder"), "Last Name")
        assertEquals(fields.get(1).attributes().get("type"), "text")

        assertEquals(fields.get(2).attributes().get("name"), "email")
        assertEquals(fields.get(2).attributes().get("placeholder"), "Email")
        assertEquals(fields.get(2).attributes().get("type"), "email")

        assertEquals(fields.get(3).attributes().get("name"), "password")
        assertEquals(fields.get(3).attributes().get("placeholder"), "Password")
        assertEquals(fields.get(3).attributes().get("type"), "password")
    }

    /**
     * Re-render form with error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/188">#188</a>
     * @throws Exception
     */
    @Test
    public void displaysErrorIfPostIsEmpty() throws Exception {

        // todo: work with CSRF

        Response response =
            given()
                .accept(ContentType.HTML)
            .when()
                .post(registerRoute)
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
    @Test
    public void displaysErrorIfEmailIsMissing() throws Exception {

        // todo: work with CSRF

        Response response =
            given()
                .accept(ContentType.HTML)
                .formParam("email", accountEmail)
                .formParam("password", "")
            .when()
                .post(registerRoute)
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
    @Test
    public void displaysErrorIfRequiredFieldIsMissing() throws Exception {

        // todo: work with CSRF

        Response response =
            given()
                .accept(ContentType.HTML)
                .formParam("email", accountEmail)
                .formParam("password", accountPassword)
                .formParam("surname", accountSurname)
                // givenName is required per the default configuration
            .when()
                .post(registerRoute)
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
     * Re-render form with error if undefined custom field is present
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/194">#194</a>
     * @throws Exception
     */
    @Test
    public void displaysErrorForUndefinedCustomField() throws Exception {

        // todo: work with CSRF

        Response response =
            given()
                .accept(ContentType.HTML)
                .formParam("email", accountEmail)
                .formParam("password", accountPassword)
                .formParam("givenName", accountGivenName)
                .formParam("surname", accountSurname)
                .formParam("customValue", "foobar") // not defined in default configuration
            .when()
                .post(registerRoute)
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
    @Test
    public void displaysErrorForServerError() throws Exception {

        Response response =
            given()
                .accept(ContentType.HTML)
                .formParam("email", "foo@bar")
                .formParam("password", "1")
                .formParam("givenName", accountGivenName)
                .formParam("surname", accountSurname)
                // Email and password will not pass Stormpath API validation and will error
            .when()
                .post(registerRoute)
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
    @Test
    public void redirectsToLoginOnSuccess() throws Exception {

        given()
            .accept(ContentType.HTML)
            .formParam("email", accountEmail)
            .formParam("password", accountPassword)
            .formParam("givenName", accountGivenName)
            .formParam("surname", accountSurname)
        .when()
            .post(registerRoute)
        .then()
            .statusCode(302)
            .header("Location", is(loginRoute + "?status=created"))

        deleteAccountOnTeardown(accountEmail)
    }
}
