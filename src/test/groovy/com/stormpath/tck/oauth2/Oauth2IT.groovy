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
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.*
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static com.jayway.restassured.matcher.RestAssuredMatchers.*
import static org.hamcrest.Matchers.*
import static org.testng.Assert.*

@Test
class Oauth2IT extends AbstractIT {
    private TestAccount account = new TestAccount()

    private static final String registerRoute = FrameworkConstants.RegisterRoute
    private static final String tokenRoute = FrameworkConstants.OauthRoute

    private String accountHref = ""

    @BeforeClass
    private void createTestAccount() throws Exception {
        deleteOnClassTeardown(account.href)
    }

    /** Unsupported grant type returns error
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/6">#6</a>
     */
    @Test
    public void unsupportedGrantType() throws Exception {

        given()
            .param("grant_type", "foobar_grant")
        .when()
            .post(tokenRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .header("Cache-Control", is("no-store"))
            .header("Pragma", is("no-cache"))
            .body("error", is("unsupported_grant_type"))
    }


    /** Missing grant type returns error
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/7">#7</a>
     */
    @Test
    public void missingGrantType() throws Exception {

        given()
            .param("grant_type", "")
        .when()
            .post(tokenRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .header("Cache-Control", is("no-store"))
            .header("Pragma", is("no-cache"))
            .body("error", is("invalid_request"))
    }


    /** POST must include form parameters
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/15">#15</a>
     */
    @Test
    public void missingFormParameters() throws Exception {

        given()
            .body("""hello"" : ""world""")
        .when()
            .post(tokenRoute)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .header("Cache-Control", is("no-store"))
            .header("Pragma", is("no-cache"))
            .body("error", is("invalid_request"))
    }


    /** Anything but POST should return 405
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/16">#16</a>
     */
    @Test
    public void doNotHandleGet() throws Exception {
        get(tokenRoute)
            .then()
            .assertThat().statusCode(405)
    }

    /** Anything but POST should return 405
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/16">#16</a>
     */
    @Test
    public void doNotHandlePut() throws Exception {
        put(tokenRoute)
            .then()
            .assertThat().statusCode(405)
    }

    /** Anything but POST should return 405
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/16">#16</a>
     */
    @Test
    public void doNotHandleDelete() throws Exception {
        delete(FrameworkConstants.OauthRoute)
            .then()
            .assertThat().statusCode(405)
    }

    /** Password grant flow with username/password
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/11">#11</a>
     */
    @Test
    public void passwordGrantWithUsername() throws Exception {

        String accessToken =
            given()
                .param("grant_type", "password")
                .param("username", account.username)
                .param("password", account.password)
            .when()
                .post(tokenRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("access_token", not(isEmptyOrNullString()))
                .body("expires_in", is(3600))
                .body("refresh_token", not(isEmptyOrNullString()))
                .body("token_type", is("Bearer"))
            .extract()
                .path("access_token")

        assertTrue(JwtUtils.extractJwtClaim(accessToken, "sub") == this.accountHref)
    }

    /** Password grant flow with email/password
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/18">#18</a>
     */
    @Test
    public void passwordGrantWithEmail() throws Exception {

        String accessToken =
            given()
                .param("grant_type", "password")
                .param("username", account.email)
                .param("password", account.password)
            .when()
                .post(tokenRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("access_token", not(isEmptyOrNullString()))
                .body("expires_in", is(3600))
                .body("refresh_token", not(isEmptyOrNullString()))
                .body("token_type", is("Bearer"))
            .extract()
                .path("access_token")

        assertTrue(JwtUtils.extractJwtClaim(accessToken, "sub") == this.accountHref)
    }

    /** Refresh grant flow
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/205">#205</a>
     */
    @Test
    public void refreshGrantTypeWorksWithValidToken() throws Exception {
        Response passwordGrantResponse =
            given()
                .param("grant_type", "password")
                .param("username", account.email)
                .param("password", account.password)
            .when()
                .post(tokenRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("access_token", not(isEmptyOrNullString()))
                .body("expires_in", is(3600))
                .body("refresh_token", not(isEmptyOrNullString()))
                .body("token_type", is("Bearer"))
            .extract()
                .response()

        String accessToken = passwordGrantResponse.path("access_token")
        String refreshToken = passwordGrantResponse.path("refresh_token")

        String newAccessToken =
            given()
                .param("grant_type", "refresh_token")
                .param("refresh_token", refreshToken)
            .when()
                .post(tokenRoute)
            .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("access_token", not(isEmptyOrNullString()))
                .body("expires_in", is(3600))
                .body("refresh_token", not(isEmptyOrNullString()))
                .body("token_type", is("Bearer"))
            .extract()
                .path("access_token")

        assertNotEquals(accessToken, newAccessToken, "The new access token should not equal to the old access token")
        assertTrue(JwtUtils.extractJwtClaim(accessToken, "sub") == this.accountHref, "The access token should be a valid jwt for the test user")
    }

    /** Refresh grant flow should fail without valid refresh token
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/205">#205</a>
     */
    @Test
    public void refreshGrantTypeFailsWithInvalidRefreshToken() throws Exception {
        String refreshToken = "GARBAGE"

        String newAccessToken =
            given()
                .param("grant_type", "refresh_token")
                .param("refresh_token", refreshToken)
            .when()
                .post(tokenRoute)
            .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", is("invalid_grant"))
    }
}