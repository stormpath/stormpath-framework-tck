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
package com.stormpath.tck.logout

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.jayway.restassured.path.xml.element.NodeChildren
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT

import com.stormpath.tck.util.JwtUtils
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.get
import static com.jayway.restassured.RestAssured.given
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.testng.Assert.*

@Test
class LogoutIT extends AbstractIT {

    private final String randomUUID = UUID.randomUUID().toString()
    private final String accountEmail = "fooemail-" + randomUUID + "@stormpath.com"
    private final String accountGivenName = "GivenName-" + randomUUID
    private final String accountSurname = "Surname-" + randomUUID
    private final String accountPassword = "P@sword123!"

    private final String logoutPath = "/logout"
    private final String registerPath = "/register"
    private final String loginPath = "/login"

    @BeforeClass
    public void createDummyUser() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("givenName", accountGivenName)
        jsonAsMap.put("surname", accountSurname)

        String createdHref =
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(jsonAsMap)
            .when()
                .post(registerPath)
            .then()
                .statusCode(200)
            .extract()
                .path("account.href")

        deleteOnClassTeardown(createdHref)
    }

    public Map<String, String> createSession() throws Exception {

        Map<String, Object>  credentials = new HashMap<>();
        credentials.put("login", accountEmail)
        credentials.put("password", accountPassword)

        Map<String, String> cookies =
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(credentials)
            .when()
                .post(loginPath)
            .then()
                .statusCode(200)
            .extract()
                .cookies()

        return cookies
    }

    /** Return 200 OK for empty JSON request
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/172">#172</a>
     * @throws Exception
     */
    @Test
    public void returnOkForEmptyJsonRequest() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .post(logoutPath)
        .then()
            .statusCode(200)
    }

    /** Return 200 OK for successful JSON logout
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/171">#171</a>
     * @throws Exception
     */
    @Test
    public void returnOkForJsonLogout() throws Exception {
        def sessionCookies = createSession()

        given()
            .accept(ContentType.JSON)
            .cookies(sessionCookies)
        .when()
            .post(logoutPath)
        .then()
            .statusCode(200)
    }
}
