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
        account.registerOnServer()

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

    /** Respond with 401 if no user is authorized
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/62">#62</a>
     * @throws Exception
     */
    @Test
    public void unauthorizedRequestFails() throws Exception {
        when()
            .get(FrameworkConstants.MeRoute)
        .then()
            .statusCode(401)
    }

    /**
     * We should be returning a user, and it should always be JSON.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/61
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/63
     * @throws Exception
     */
    @Test
    public void testThatMeWithBearerAuthReturnsJsonUser() throws Exception {
        given()
            .auth().oauth2(accessToken)
        .when()
            .get(FrameworkConstants.MeRoute)
        .then()
            .spec(AccountResponseSpec.matchesAccount(account))
    }

    /**
     * We should not have linked resources.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/64
     * @throws Exception
     */
    @Test
    public void testThatMeWithBearerAuthStripsLinkedResources() throws Exception {
        given()
            .auth().oauth2(accessToken)
        .when()
            .get(FrameworkConstants.MeRoute)
        .then()
            .spec(AccountResponseSpec.withoutLinkedResources())
    }

    /**
     * We should not set cache headers.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/65
     * @throws Exception
     */
    @Test
    public void testThatMeWithBearerAuthHasNoCacheHeaders() throws Exception {
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