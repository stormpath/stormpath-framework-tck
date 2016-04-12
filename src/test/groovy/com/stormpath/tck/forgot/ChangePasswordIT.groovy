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
import com.stormpath.tck.responseSpecs.*
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static org.testng.Assert.*
import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static com.stormpath.tck.util.FrameworkConstants.ChangeRoute

class ChangePasswordIT extends AbstractIT {

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/166">#166</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void doNotHandlePut() throws Exception {
        put(ChangeRoute)
                .then()
                .assertThat().statusCode(404)
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/166">#166</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void doNotHandleDelete() throws Exception {
        delete(ChangeRoute)
                .then()
                .assertThat().statusCode(404)
    }

    /** Return JSON error if sptoken is invalid
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/239">#239</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void respondWithJsonErrorForInvalidSptoken() throws Exception {

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
    @Test(groups=["v100"])
    public void respondWithJsonErrorForMissingSptoken() throws Exception {

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
    @Test(groups=["v100"])
    public void respondWithJsonErrorForInvalidSptokenWhenPosting() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body([
                "sptoken": "NOTEVENCLOSETOVALID",
                "password": "foobar123!",
                "passwordAgain": "foobar123!"
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
    @Test(groups=["v100"])
    public void redirectToErrorUriForInvalidSptoken() throws Exception {

        given()
            .accept(ContentType.HTML)
            .contentType(ContentType.HTML)
            .queryParam("sptoken", "NOTEVENCLOSETOVALID")
        .when()
            .get(ChangeRoute)
        .then()
            .statusCode(302)
            .header("Location", "/forgot?status=invalid_sptoken")
    }

    /** https://github.com/stormpath/stormpath-framework-tck/issues/151
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/151">#151</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void redirectToForgotUriForMissingSptoken() throws Exception {

        given()
            .accept(ContentType.HTML)
            .contentType(ContentType.HTML)
        .when()
            .get(ChangeRoute)
        .then()
            .statusCode(302)
            .header("Location", "/forgot")
    }

    /** Redirect to errorUri for invalid or expired token on POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/158">#158</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void redirectToErrorUriForInvalidSptokenWhenPosting() throws Exception {

        given()
            .accept(ContentType.HTML)
            .contentType(ContentType.URLENC)
            .queryParam("sptoken", "NOTEVENCLOSETOVALID")
            .param("password", "foobar123!")
            .param("passwordAgain", "foobar123!")
        .when()
            .post(ChangeRoute)
        .then()
            .statusCode(302)
            .header("Location", "/forgot?status=invalid_sptoken")
    }
}
