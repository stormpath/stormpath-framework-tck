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
package com.stormpath.tck.verify

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.jayway.restassured.path.xml.element.NodeChildren
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.responseSpecs.JsonResponseSpec
import org.testng.annotations.Test

import static com.stormpath.tck.util.FrameworkConstants.VerifyRoute
import static com.jayway.restassured.RestAssured.delete
import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.RestAssured.put
import static com.stormpath.tck.util.Matchers.urlMatchesPath
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not
import static org.testng.Assert.assertEquals

class VerifyEmailIT extends AbstractIT {

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

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/166">#166</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    public void verifyEmailDoesNotHandlePut() throws Exception {
        put(VerifyRoute)
                .then()
                .assertThat().statusCode(allOf(not(200), not(500)))
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/166">#166</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    public void verifyEmailDoesNotHandleDelete() throws Exception {
        delete(VerifyRoute)
                .then()
                .assertThat().statusCode(allOf(not(200), not(500)))
    }

    /** Return JSON error if sptoken is invalid
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void verifyEmailErrorsForInvalidSptokenJson() throws Exception {

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .queryParam("sptoken", "NOTEVENCLOSETOVALID")
                .when()
                .get(VerifyRoute)
                .then()
                .spec(JsonResponseSpec.isError(404))
    }

    /**
     * Serve a default HTML form for request type text/html
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void verifyEmailServesHtmlForm() throws Exception {

        Response response =
                given()
                        .accept(ContentType.HTML)
                        .when()
                        .get(VerifyRoute)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.HTML)
                        .extract()
                        .response()

        XmlPath doc = getHtmlDoc(response)

        Node submitButton = findTagWithAttribute(doc.getNodeChildren("html.body"), "button", "type", "submit")
        assertEquals(submitButton.value(), "Send Email")
    }

    /**
     * Serve a default HTML form for request type text/html
     * indicating that the sent token is not valid
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void verifyEmailInvalidToken() throws Exception {

        Response response =
                given()
                        .accept(ContentType.HTML)
                        .queryParam("sptoken", "NOTEVENCLOSETOVALID")
                        .when()
                        .get(VerifyRoute)
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.HTML)
                        .extract()
                        .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "alert-danger")
        assertThat(warning.toString(), not(isEmptyOrNullString()))
    }

    /** Return JSON error if sptoken is missing
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void verifyEmailErrorsForMissingSptokenJson() throws Exception {

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .when()
                .get(VerifyRoute)
                .then()
                .spec(JsonResponseSpec.isError(400))
    }

    /** Respond with 200 OK and empty body for a JSON POST with any valid or invalid email address
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void verifyEmailForAnyEmailWhenPostingJson() throws Exception {

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body([
                "email": "ANYEMAIL@ANYDOMAIN.COM"
        ])
                .when()
                .post(VerifyRoute)
                .then()
                .statusCode(200)
                .body(isEmptyOrNullString())
    }

    /** Respond with error when POSTing JSON without the email parameter
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void verifyEmailEmailRequiredWhenPostingJson() throws Exception {

        given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .when()
                .post(VerifyRoute)
                .then()
                .spec(JsonResponseSpec.isError(400))
    }

    /**
     *  In HTML for a valid account and a valid sptoken
     * the POST request redirects to stormpath.web.verifyEmail.nextUri
     * but replaces the default "status=verified" for "status=unverified"
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void verifyEmailForAnyEmailWhenPosting() throws Exception {

        saveCSRFAndCookies(VerifyRoute)

        def req = given()
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)
                .formParam("email", "ANYEMAIL@ANYDOMAIN.COM")

        setCSRFAndCookies(req, ContentType.HTML)

        req.when()
                .post(VerifyRoute)
                .then()
                .statusCode(302)
                .header("Location", urlMatchesPath("/login?status=unverified"))
    }

    /**
     * Serve a default HTML form for request type text/html
     * indicating that the email is required
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void verifyEmailEmailRequiredWhenPosting() throws Exception {

        saveCSRFAndCookies(VerifyRoute)

        def req = given()
                .accept(ContentType.HTML)
                .contentType(ContentType.URLENC)

        setCSRFAndCookies(req, ContentType.HTML)

        Response response = req.when()
                .post(VerifyRoute)
                .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
                .extract()
                .response()

        XmlPath doc = getHtmlDoc(response)

        Node warning = findTagWithAttribute(doc.getNodeChildren("html.body"), "div", "class", "alert-danger")
        assertThat(warning.toString(), not(isEmptyOrNullString()))
    }
}
