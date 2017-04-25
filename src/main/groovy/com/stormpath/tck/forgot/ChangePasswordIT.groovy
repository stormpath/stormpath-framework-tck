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
import com.stormpath.tck.responseSpecs.JsonResponseSpec
import com.stormpath.tck.util.HtmlUtils
import com.stormpath.tck.util.StringUtils
import com.stormpath.tck.util.TestAccount
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.delete
import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.RestAssured.put
import static com.stormpath.tck.util.FrameworkConstants.ChangeRoute
import static com.stormpath.tck.util.FrameworkConstants.ForgotRoute
import static com.stormpath.tck.util.FrameworkConstants.OauthRoute
import static com.stormpath.tck.util.HtmlUtils.assertAttributesEqual
import static com.stormpath.tck.util.Matchers.urlMatchesPath
import static com.stormpath.tck.util.TestAccount.Mode.WITH_DISPOSABLE_EMAIL
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not

class ChangePasswordIT extends AbstractIT {

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/166">#166</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    void changeDoesNotHandlePut() throws Exception {
        put(ChangeRoute)
                .then()
                .assertThat().statusCode(allOf(not(200), not(500)))
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/166">#166</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    void changeDoesNotHandleDelete() throws Exception {
        delete(ChangeRoute)
                .then()
                .assertThat().statusCode(allOf(not(200), not(500)))
    }

    /** Return JSON error if sptoken is invalid
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/239">#239</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    void changeErrorsForInvalidSptokenJson() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .queryParam("sptoken", "NOTEVENCLOSETOVALID")
        .when()
            .get(ChangeRoute)
        .then()
            .spec(JsonResponseSpec.isError(404))
    }

    /** Return JSON error if sptoken is missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/216">#216</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    void changeErrorsForMissingSptokenJson() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
        .when()
            .get(ChangeRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    /** Respond with API error for error during JSON POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/159">#159</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    void changeErrorsForInvalidSptokenWhenPostingJson() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body([
                "sptoken": "NOTEVENCLOSETOVALID",
                "password": "foobar123!"
            ])
        .when()
            .post(ChangeRoute)
        .then()
            .spec(JsonResponseSpec.isError(404))
    }

    /** Redirect to errorUri for invalid sptoken
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/147">#147</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    void changeRedirectsToErrorUriForInvalidSptoken() throws Exception {

        given()
            .accept(ContentType.HTML)
            .queryParam("sptoken", "NOTEVENCLOSETOVALID")
        .when()
            .get(ChangeRoute)
        .then()
            .statusCode(302)
            .header("Location", urlMatchesPath("/forgot?status=invalid_sptoken"))
    }

    /** https://github.com/stormpath/stormpath-framework-tck/issues/151
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/151">#151</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    void changeRediredctsToForgotUriForMissingSptoken() throws Exception {

        given()
            .accept(ContentType.HTML)
        .when()
            .get(ChangeRoute)
        .then()
            .statusCode(302)
            .header("Location", urlMatchesPath("/forgot"))
    }

    /** Redirect to errorUri for invalid or expired token on POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/158">#158</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    void changeRedirectsToErrorUriForInvalidSptokenWhenPosting() throws Exception {

        saveCSRFAndCookies(ForgotRoute)

        def req = given()
            .accept(ContentType.HTML)
            .contentType(ContentType.URLENC)
            .queryParam("sptoken", "NOTEVENCLOSETOVALID")
            .param("password", "foobar123!")

        setCSRFAndCookies(req, ContentType.HTML)

        req.when()
            .post(ChangeRoute)
        .then()
            .statusCode(302)
            .header("Location", urlMatchesPath("/forgot?status=invalid_sptoken"))
    }

    /** Render a form to collect new password for valid sptoken
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/149">#149</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    void changeRendersFormForValidSptoken() throws Exception {
        def requiredAttributesList = [
            [name: "password", type: "password"],
            [type: "password"]
        ]

        def account = new TestAccount(WITH_DISPOSABLE_EMAIL)
        account.registerOnServer()
        deleteOnClassTeardown(account.href)

        given()
            .body([email: account.email])
        .when()
            .post(ForgotRoute)
        .then()
            .statusCode(200)

        String rawChangePasswordEmail = account.getEmail(fromEmailDomain)
        String changePasswordHref = StringUtils.extractChangePasswordHref(rawChangePasswordEmail, "sptoken")

        def response = given()
            .accept(ContentType.HTML)
        .when()
            .get(changePasswordHref)
        .then()
            .statusCode(200)
            .contentType(ContentType.HTML)
        .extract()
            .response()

        XmlPath doc = getHtmlDoc(response)
        List<Node> fields = HtmlUtils.findTags(doc.getNodeChildren("html.body"), "input")

        assertAttributesEqual(fields, requiredAttributesList)

        // Todo: asserting the name of the second field is 1.1
        //assertEquals(fields.get(1).attributes().get("name"), "confirmPassword")
    }

    /** Return 200 OK and empty body for application/json POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/155">#155</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    void changeEndpointChangesAccountPasswordWhenPostingJson() throws Exception {

        def account = new TestAccount(WITH_DISPOSABLE_EMAIL)
        account.registerOnServer()
        deleteOnClassTeardown(account.href)

        given()
            .body([email: account.email])
        .when()
            .post(ForgotRoute)
        .then()
            .statusCode(200)

        // sleep between requests okta rate limiting
        Thread.sleep(1000)

        String rawChangePasswordEmail = account.getEmail(fromEmailDomain)
        String changePasswordHref = StringUtils.extractChangePasswordHref(rawChangePasswordEmail, "sptoken")
        String sptoken = StringUtils.extractTokenFromHref(changePasswordHref, "sptoken")

        String newPassword = "N3wP4ssw0rd###"

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body([ "sptoken": sptoken, "password": newPassword ])
        .when()
            .post(ChangeRoute)
        .then()
            .statusCode(200)
            .body(isEmptyOrNullString())

        // sleep between requests okta rate limiting
        Thread.sleep(1000)

        // Verify that the password is now the new password through a login attempt / OAuth token request
        given()
            .param("grant_type", "password")
            .param("username", account.username)
            .param("password", newPassword)
        .when()
            .post(OauthRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("access_token", not(isEmptyOrNullString()))
    }
}
