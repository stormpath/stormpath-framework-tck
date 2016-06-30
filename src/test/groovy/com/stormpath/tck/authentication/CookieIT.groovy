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

package com.stormpath.tck.authentication

import com.jayway.restassured.http.ContentType
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.*
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static com.stormpath.tck.util.FrameworkConstants.LoginRoute
import static org.hamcrest.Matchers.*
import static org.testng.Assert.*
import static com.stormpath.tck.util.FrameworkConstants.MeRoute
import static com.stormpath.tck.util.FrameworkConstants.OauthRoute
import static com.stormpath.tck.util.CookieUtils.isCookieDeleted
import static com.stormpath.tck.util.Matchers.*

@Test
class CookieIT extends AbstractIT {

    def createTestAccountTokens() {
        def account = new TestAccount()
        account.registerOnServer()

        def response =
                given()
                    .param("grant_type", "password")
                    .param("username", account.username)
                    .param("password", account.password)
                .when()
                    .post(OauthRoute)
                .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("access_token", not(isEmptyOrNullString()))
                .extract()
                    .response()
        deleteOnClassTeardown(account.href)

        return new Tuple2(response.path("access_token"), response.path("refresh_token"))
    }

    /** If access token is invalid, use refresh token to get new access token
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/34">#34</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    public void serverRefreshesAccessTokenWhenMissing() throws Exception {
        def (String accessToken, String refreshToken) = createTestAccountTokens()

        given()
            .cookie("refresh_token", refreshToken)
        .when()
            .get(MeRoute)
        .then()
            .statusCode(200)
            .cookie("access_token", not(accessToken))
            .cookie("refresh_token", is(refreshToken))
    }

    /** If refresh token is invalid, reject request and delete cookies
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/35">#35</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    public void serverDeletesCookiesWhenRefreshingWithInvalidToken() throws Exception {
        def response = given()
            .cookie("access_token",  "not_a_valid_access_token_at_all")
            .cookie("refresh_token", "not_a_valid_refresh_token_at_all")
        .when()
            .get(MeRoute)
        .then()
            .statusCode(401)
        .extract()
            .response()

        assertTrue(isCookieDeleted(response.detailedCookies.get("access_token")))
        assertTrue(isCookieDeleted(response.detailedCookies.get("refresh_token")))
    }

    /** Reject unauthorized text/html requests with 302 to login route
     * 302 to login route should preserve original URL including query string
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/36">#36</a>
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/121">#121</a>
     * @throws Exception
     */
    @Test(groups=["v100", "html"])
    public void unauthorizedHtmlRequestIsForwardedToLogin() throws Exception {

        // TODO: This test will probably have to change because we will be updating the spec.

        given()
            .accept(ContentType.HTML)
        .when()
            .get(MeRoute + "?foo=bar")
        .then()
            .statusCode(302)
            .header("Location", urlStartsWithPath("/login"))
    }

    /** Cookie expires flag should be set to token TTL
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/36">#38</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void cookieExpirationMatchesTokenTtl() throws Exception {
        def account = new TestAccount()
        account.registerOnServer()
        deleteOnClassTeardown(account.href)

        def response = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body([ "login": account.email, "password": account.password ])
        .when()
            .post(LoginRoute)
        .then()
            .statusCode(200)
        .extract()
            .response()

        def accessTokenCookie = response.detailedCookies.get("access_token")
        def accessTokenTtl = JwtUtils.extractJwtClaim(accessTokenCookie.value, "exp") as long
        assertEquals(accessTokenCookie.expiryDate, new Date(accessTokenTtl * 1000L))

        def refreshTokenCookie = response.detailedCookies.get("refresh_token")
        def refreshTokenTtl = JwtUtils.extractJwtClaim(refreshTokenCookie.value, "exp") as long
        assertEquals(refreshTokenCookie.expiryDate, new Date(refreshTokenTtl * 1000L))
    }

    /** Passing refresh token as access token should fail
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/232">#232</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json", "html"])
    public void refreshTokenAsAccessTokenFails() throws Exception {
        def (String accessToken, String refreshToken) = createTestAccountTokens()

        given()
            .cookie("access_token", refreshToken)
        .when()
            .get(MeRoute)
        .then()
            .statusCode(401)
    }
}
