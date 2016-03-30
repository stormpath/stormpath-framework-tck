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

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.response.Cookie
import com.jayway.restassured.response.Cookies
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.*
import com.stormpath.tck.responseSpecs.*
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static org.testng.Assert.*
import static org.hamcrest.Matchers.*

@Test
class MeIT extends AbstractIT {
    private TestAccount account = new TestAccount()
    private String accessToken

    @BeforeClass
    private void getTestAccountAccessToken() throws Exception {
        accessToken =
            given()
                .param("grant_type", "password")
                .param("username", account.username)
                .param("password", account.password)
            .when()
                .post(FrameworkConstants.OauthRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("access_token", not(isEmptyOrNullString()))
            .extract()
                .path("access_token")
        deleteOnClassTeardown(account.href)
    }

    @Test
    public void testThatMeReturnsJsonUser() throws Exception {
        given()
            .auth().oauth2(accessToken)
        .when()
            .get(FrameworkConstants.MeRoute)
        .then()
            .spec(AccountResponseSpec.matchesAccount(account))
    }

    @Test
    public void testThatMeStripsLinkedResources() throws Exception {
        given()
            .auth().oauth2(accessToken)
        .when()
            .get(FrameworkConstants.MeRoute)
        .then()
            .spec(AccountResponseSpec.withoutLinkedResources())
    }

    @Test
    public void testThatMeHasNoCacheHeaders() throws Exception {
        given()
            .auth().oauth2(accessToken)
        .when()
            .get(FrameworkConstants.MeRoute)
        .then()
            .spec(AccountResponseSpec.matchesAccount(account))
            .header("Cache-Control", containsString("no-cache"))
            .header("Cache-Control", containsString("no-store"))
            .header("Pragma", is("no-cache"))
    }
}