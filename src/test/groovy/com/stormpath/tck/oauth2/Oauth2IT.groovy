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
package com.stormpath.tck.oauth2

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.login.LoginJsonIT
import com.stormpath.tck.util.RestUtils
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import static com.jayway.restassured.RestAssured.*
import static com.jayway.restassured.matcher.RestAssuredMatchers.*
import static org.hamcrest.Matchers.*

@Test
class Oauth2IT extends AbstractIT {

    private final String randomUUID = UUID.randomUUID().toString()
    private final String accountEmail = "fooemail-" + randomUUID + "@stormpath.com"
    private final String accountUsername = randomUUID
    private final String accountPassword = "P@ssword123"

    @BeforeClass
    private void createTestAccount() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("givenName", "GivenName-" + randomUUID)
        jsonAsMap.put("surname", "Surname-" + randomUUID)
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

    @Test
    /**
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/6">#6</a>
     */
    public void unsupportedGrantType() throws Exception {

        given()
            .param("grant_type", "foobar_grant")
        .when()
            .post("/oauth/token")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .header("Cache-Control", is("no-store"))
            .header("Pragma", is("no-cache"))
            .body("error", is("unsupported_grant_type"))
    }

    @Test
    /**
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/7">#7</a>
     */
    public void missingGrantType() throws Exception {

        given()
            .param("grant_type", "")
        .when()
            .post("/oauth/token")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .header("Cache-Control", is("no-store"))
            .header("Pragma", is("no-cache"))
            .body("error", is("invalid_request"))
    }

    @Test
    /**
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/15">#15</a>
     */
    public void missingFormParameters() throws Exception {

        given()
            .body("""hello"" : ""world""")
        .when()
            .post("/oauth/token")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .header("Cache-Control", is("no-store"))
            .header("Pragma", is("no-cache"))
            .body("error", is("invalid_request"))
    }


    @Test
    /**
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/16">#16</a>
     */
    public void doNotHandleGet() throws Exception {
        get("/oauth/token")
            .then()
            .assertThat().statusCode(405)
    }

    /**
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/11">#11</a>
     */
    public void passwordGrantWithUsername() throws Exception {

        given()
            .param("grant_type", "password")
            .param("username", accountUsername)
            .param("password", accountPassword)
        .when()
            .post("/oauth/token")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("access_token", not(isEmptyOrNullString()))
            .body("expires_in", is(3600))
            .body("refresh_token", not(isEmptyOrNullString()))
            .body("token_type", is("Bearer"))
    }


    /**
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/18">#18</a>
     */
    public void passwordGrantWithEmail() throws Exception {

        given()
            .param("grant_type", "password")
            .param("username", accountEmail)
            .param("password", accountPassword)
        .when()
            .post("/oauth/token")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("access_token", not(isEmptyOrNullString()))
            .body("expires_in", is(3600))
            .body("refresh_token", not(isEmptyOrNullString()))
            .body("token_type", is("Bearer"))
    }
}