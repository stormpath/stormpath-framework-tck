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

package com.stormpath.tck.forgot

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.*
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static org.testng.Assert.*
import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static com.stormpath.tck.util.FrameworkConstants.ForgotRoute

@Test
class ForgotPasswordIT extends AbstractIT {
    private TestAccount account = new TestAccount()

    @BeforeClass
    private void createTestAccount() throws Exception {
        account.registerOnServer()
        deleteOnClassTeardown(account.href)
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/165">#165</a>
     * @throws Exception
     */
    @Test
    public void doNotHandlePut() throws Exception {
        put(ForgotRoute)
            .then()
                .assertThat().statusCode(404)
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/165">#165</a>
     * @throws Exception
     */
    @Test
    public void doNotHandleDelete() throws Exception {
        delete(ForgotRoute)
            .then()
                .assertThat().statusCode(404)
    }

    /** GET should not be handled for JSON requests
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/215">#215</a>
     * @throws Exception
     */
    @Test
    public void doNotHandleJsonGet() throws Exception {
        given()
            .accept(ContentType.JSON)
        .when()
            .get(ForgotRoute)
        .then()
            .statusCode(404)
    }

    /** POST requests preferring application/json should respond with 200 OK
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/142">#142</a>
     * @throws Exception
     */
    @Test
    public void returnsSuccessForValidEmail() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body([ "email": account.email ])
        .when()
            .post(ForgotRoute)
        .then()
            .statusCode(200)
    }

    /** POST requests preferring application/json should respond with 200 OK
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/142">#142</a>
     * @throws Exception
     */
    @Test
    public void returnsSuccessForInvalidEmail() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body([ "email": "foo+notarealemail@bar.baz" ])
        .when()
            .post(ForgotRoute)
        .then()
            .statusCode(200)
    }

    /** Render form if the request type is text/html
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/140">#140</a>
     * @throws Exception
     */
    @Test
    public void rendersForm() throws Exception {

        def response = given()
            .accept(ContentType.HTML)
        .when()
            .get(ForgotRoute)
        .then()
            .contentType(ContentType.HTML)
            .statusCode(200)
        .extract()
            .response()

        XmlPath doc = getHtmlDoc(response)

        Node emailField = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "input", "name", "email")
        assertEquals(emailField.attributes().get("type"), "text")
    }

    /** If query string contains invalid_sptoken, render message above form
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/141">#141</a>
     * @throws Exception
     */
    @Test
    public void rendersFormWithInvalidSptokenWarning() throws Exception {

        def response = given()
            .accept(ContentType.HTML)
            .param("status", "invalid_sptoken")
        .when()
            .get(ForgotRoute)
        .then()
            .contentType(ContentType.HTML)
            .statusCode(200)
        .extract()
            .response()

        XmlPath doc = getHtmlDoc(response)

        Node warningBanner = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "alert-warning")
        assertThat(warningBanner.toString(), not(isEmptyOrNullString()))
    }
}
