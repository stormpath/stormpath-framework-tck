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
import static com.stormpath.tck.util.FrameworkConstants.LogoutRoute
import static com.stormpath.tck.util.FrameworkConstants.LoginRoute

@Test
class LogoutIT extends AbstractIT {
    private TestAccount account = new TestAccount()

    @BeforeClass
    public void createTestUser() throws Exception {
        account.registerOnServer()
        deleteOnClassTeardown(account.href)
    }

    private Map<String, String> createSession() throws Exception {

        Map<String, Object>  credentials = new HashMap<>();
        credentials.put("login", account.email)
        credentials.put("password", account.password)

        Map<String, String> cookies =
            given()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .body(credentials)
            .when()
                .post(LoginRoute)
            .then()
                .statusCode(200)
            .extract()
                .cookies()

        return cookies
    }

    private void assertCookiesAreDeleted(Cookies cookies) throws Exception {
        def now = new Date()

        Cookie accessTokenCookie = cookies.get("access_token")
        assertTrue(accessTokenCookie.expiryDate < now)

        Cookie refreshTokenCookie = cookies.get("refresh_token")
        assertTrue(refreshTokenCookie.expiryDate < now)
    }

    private void assertTokenIsRevoked(String tokenJwt, String resourceType) throws Exception {
        String tokenId = JwtUtils.extractJwtClaim(tokenJwt, "jti")

        given()
            .header("Authorization", RestUtils.getBasicAuthorizationHeaderValue())
            .header("User-Agent", "stormpath-framework-tck")
        .when()
            .get(EnvUtils.stormpathBaseUrl + '/' + resourceType + '/' + tokenId)
        .then()
            .statusCode(404)
    }

    /** Only handle POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/54">#54</a>
     */
    @Test(groups=["v100"])
    public void doNotHandleGet() throws Exception {
        get(LogoutRoute)
            .then()
                .assertThat().statusCode(404)
    }

    /** Only handle POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/54">#54</a>
     */
    @Test(groups=["v100"])
    public void doNotHandlePut() throws Exception {
        put(LogoutRoute)
            .then()
                .assertThat().statusCode(404)
    }

    /** Only handle POST
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/54">#54</a>
     */
    @Test(groups=["v100"])
    public void doNotHandleDelete() throws Exception {
        delete(LogoutRoute)
            .then()
                .assertThat().statusCode(404)
    }

    /** Return 200 OK for unauthenticated JSON request
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/172">#172</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void returnOkForUnauthenticatedJsonRequest() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .post(LogoutRoute)
        .then()
            .statusCode(200)
    }

    /** Delete cookies on JSON logout
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/174">#174</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void deletesCookiesOnJsonLogout() throws Exception {
        def sessionCookies = createSession()

        Cookies detailedCookies =
            given()
                .accept(ContentType.JSON)
                .cookies(sessionCookies)
            .when()
                .post(LogoutRoute)
            .then()
            .extract()
                .detailedCookies()

        assertCookiesAreDeleted(detailedCookies)
    }

    /** Return 200 OK for successful JSON logout
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/171">#171</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void returnOkOnSuccessfulJsonLogout() throws Exception {
        def sessionCookies = createSession()

        given()
            .accept(ContentType.JSON)
            .cookies(sessionCookies)
        .when()
            .post(LogoutRoute)
        .then()
            .statusCode(200)
    }

    /** Revoke tokens on Stormpath for JSON request
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/175">#175</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void revokesTokensOnJsonLogout() throws Exception {
        def sessionCookies = createSession()

        given()
            .accept(ContentType.JSON)
            .cookies(sessionCookies)
        .post(LogoutRoute)

        assertTokenIsRevoked(sessionCookies.get("access_token"), "accessTokens")
        assertTokenIsRevoked(sessionCookies.get("refresh_token"), "refreshTokens")
    }

    /** Redirect to nextUri for unauthenticated request
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/173">#173</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void redirectForUnauthenticatedRequest() throws Exception {

        given()
            .accept(ContentType.HTML)
        .when()
            .post(LogoutRoute)
        .then()
            .statusCode(302)
            .header("Location", is("/"))
    }

    /** Redirect to nextUri on successful logout
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/170">#170</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void redirectOnSuccessfulLogout() throws Exception {
        def sessionCookies = createSession()

        given()
            .accept(ContentType.HTML)
            .cookies(sessionCookies)
        .when()
            .post(LogoutRoute)
        .then()
            .statusCode(302)
            .header("Location", is("/"))
    }

    /** Delete cookies on logout
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/53">#53</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void deletesCookiesOnLogout() throws Exception {
        def sessionCookies = createSession()

        Cookies detailedCookies =
                given()
                    .accept(ContentType.HTML)
                    .cookies(sessionCookies)
                .when()
                    .post(LogoutRoute)
                .then()
                .extract()
                    .detailedCookies()

        assertCookiesAreDeleted(detailedCookies)
    }

    /** Revoke Stormpath tokens on logout
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/169">#169</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void revokesTokensOnLogout() throws Exception {
        def sessionCookies = createSession()

        given()
            .accept(ContentType.HTML)
            .cookies(sessionCookies)
        .post(LogoutRoute)

        assertTokenIsRevoked(sessionCookies.get("access_token"), "accessTokens")
        assertTokenIsRevoked(sessionCookies.get("refresh_token"), "refreshTokens")
    }
}
