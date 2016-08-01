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
import com.stormpath.tck.util.HtmlUtils
import com.stormpath.tck.util.TestAccount
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.delete
import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.RestAssured.put
import static com.stormpath.tck.util.FrameworkConstants.ForgotRoute
import static com.stormpath.tck.util.Matchers.urlMatchesPath
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not
import static org.testng.Assert.assertEquals

@Test
class ForgotPasswordIT extends AbstractIT {
    private TestAccount account = new TestAccount()
    private static final invalidEmail = "foo+notarealemail@bar.baz"

    @BeforeClass
    private void createTestAccount() throws Exception {
        account.registerOnServer()
        deleteOnClassTeardown(account.href)
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/165">#165</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    public void forgotDoesNotHandlePut() throws Exception {
        put(ForgotRoute)
            .then()
                .assertThat().statusCode(allOf(not(200), not(500)))
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/165">#165</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    public void forgotDoesNotHandleDelete() throws Exception {
        delete(ForgotRoute)
            .then()
                .assertThat().statusCode(allOf(not(200), not(500)))
    }

    /** GET should not be handled for JSON requests
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/215">#215</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void forgotDoesNotHandleJsonGet() throws Exception {
        given()
            .accept(ContentType.JSON)
        .when()
            .get(ForgotRoute)
        .then()
            .statusCode(allOf(not(200), not(500)))
    }

    /** POST requests preferring application/json should respond with 200 OK
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/142">#142</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void forgotSucceedsWhenPostingValidEmailJson() throws Exception {
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
    @Test(groups=["v100", "json"])
    public void forgotSucceedsWhenPostingInvalidEmailJson() throws Exception {
        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body([ "email": invalidEmail ])
        .when()
            .post(ForgotRoute)
        .then()
            .statusCode(200)
    }

    /** Render form if the request type is text/html
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/140">#140</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void forgotRendersForm() throws Exception {

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
        assertEquals(emailField.attributes().get("type"), "email")
    }

    /** If query string contains invalid_sptoken, render message above form
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/141">#141</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void forgotRendersFormWithInvalidSptokenBanner() throws Exception {

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

        Node warningBanner = HtmlUtils.findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "alert")
        assertThat(warningBanner.toString(), not(isEmptyOrNullString()))
    }

    /** POST requests preferring text/html should redirect to nextUri
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/143">#143</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void forgotRedirectsToNextUriWhenPostingValidEmail() throws Exception {

        saveCSRFAndCookies(ForgotRoute)

        def req = given()
            .accept(ContentType.HTML)
            .contentType(ContentType.URLENC)
            .param("email", account.email)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .post(ForgotRoute)
            .then()
                .statusCode(302)
                .header("Location", urlMatchesPath("/login?status=forgot"))
    }

    /** POST requests preferring text/html should redirect to nextUri
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/143">#143</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void forgotRedirectsToNextUriWhenPostingInvalidEmail() throws Exception {

        saveCSRFAndCookies(ForgotRoute)

        def req = given()
            .accept(ContentType.HTML)
            .contentType(ContentType.URLENC)
            .param("email", invalidEmail)

        setCSRFAndCookies(req, ContentType.HTML)

        req
            .when()
                .post(ForgotRoute)
            .then()
                .statusCode(302)
                .header("Location", urlMatchesPath("/login?status=forgot"))
    }
}
