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

package com.stormpath.tck.me

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.responseSpecs.AccountResponseSpec
import com.stormpath.tck.util.RestUtils
import com.stormpath.tck.util.TestAccount
import io.jsonwebtoken.Jwt
import io.jsonwebtoken.Jwts
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.RestAssured.when
import static com.stormpath.tck.util.FrameworkConstants.MeRoute
import static com.stormpath.tck.util.FrameworkConstants.OauthRoute
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not

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
                .post(OauthRoute)
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
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/37">#37</a>
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void meFailsOnUnauthenticatedRequest() throws Exception {
        when()
            .get(MeRoute)
        .then()
            .statusCode(401)
    }

    /**
     * We should be returning a user, and it should always be JSON.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/61
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/63
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/234
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void meWithCookieAuthReturnsJsonUser() throws Exception {
        ["text/html", "application/json", "*/*", ""].each { contentType ->
            given()
                .header("Accept", contentType)
                .cookie("access_token", accessToken)
            .when()
                .get(MeRoute)
            .then()
                .spec(AccountResponseSpec.matchesAccount(account))
        }
    }

    /**
     * We should be returning a user, and it should always be JSON.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/61
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/63
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/235
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void meWithBearerAuthReturnsJsonUser() throws Exception {
        given()
            .auth().oauth2(accessToken)
        .when()
            .get(MeRoute)
        .then()
            .spec(AccountResponseSpec.matchesAccount(account))
    }

    /**
     * Me should take Basic auth with an account's API keys as well.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/236
     */
    @Test(groups=["v100", "json"])
    public void meWithBasicAuthReturnsJsonUser() throws Exception {
        Response apiKeysResource = given()
            .header("User-Agent", "stormpath-framework-tck")
            .header("Authorization", RestUtils.getBasicAuthorizationHeaderValue())
            .header("Content-Type", "application/json")
            .port(443)
        .when()
            .post(account.href + "/apiKeys")
        .then()
            .statusCode(201)
        .extract()
            .response()

        String apiKeyId = apiKeysResource.body().jsonPath().getString("id")
        String apiKeySecret = apiKeysResource.body().jsonPath().getString("secret")

        given()
            .auth().preemptive().basic(apiKeyId, apiKeySecret)
        .when()
            .get(MeRoute)
        .then()
            .spec(AccountResponseSpec.matchesAccount(account))
    }

    /**
     * We should not have linked resources.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/64
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/234
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void meWithCookieAuthStripsLinkedResources() throws Exception {
        given()
            .cookie("access_token", accessToken)
        .when()
            .get(MeRoute)
        .then()
            .spec(AccountResponseSpec.withoutLinkedResources())
    }

    /**
     * We should not have linked resources.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/64
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/235
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void meWithBearerAuthStripsLinkedResources() throws Exception {
        given()
            .auth().oauth2(accessToken)
        .when()
            .get(MeRoute)
        .then()
            .spec(AccountResponseSpec.withoutLinkedResources())
    }

    /**
     * We should not set cache headers.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/65
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/234
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void meWithCookieAuthHasNoCacheHeaders() throws Exception {
        given()
            .cookie("access_token", accessToken)
        .when()
            .get(MeRoute)
        .then()
            .spec(AccountResponseSpec.matchesAccount(account))
            .header("Cache-Control", containsString("no-cache"))
            .header("Cache-Control", containsString("no-store"))
            .header("Pragma", is("no-cache"))
    }

    /**
     * We should not set cache headers.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/65
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/235
     * @throws Exception
     */
    @Test(groups=["v100", "json"])
    public void meWithBearerAuthHasNoCacheHeaders() throws Exception {
        given()
            .auth().oauth2(accessToken)
        .when()
            .get(MeRoute)
        .then()
            .spec(AccountResponseSpec.matchesAccount(account))
            .header("Cache-Control", containsString("no-cache"))
            .header("Cache-Control", containsString("no-store"))
            .header("Pragma", is("no-cache"))
    }

    /** We shouldn't be able to authenticate with a JWT that uses an algorithm of none.
     * @see https://github.com/stormpath/stormpath-framework-tck/issues/231
     */
    @Test(groups=["v100", "json"])
    public void unsignedAccessTokensShouldFail() throws Exception {
        String unsignedAccessToken = changeJwtAlgorithmToNone(accessToken)

        given()
            .auth().oauth2(unsignedAccessToken)
        .when()
            .get(MeRoute)
        .then()
            .statusCode(401)
    }

    // Helper method that strips the signature & changes the signing algorithm to none for a jwt
    private String changeJwtAlgorithmToNone(String jwt) {
        String jwtWithoutSignature = jwt.substring(0, jwt.lastIndexOf('.')+1)
        Jwt newJwt = Jwts.parser().parseClaimsJwt(jwtWithoutSignature)

        return Jwts.builder().setHeader(newJwt.header).setClaims(newJwt.body).compact()
    }
}