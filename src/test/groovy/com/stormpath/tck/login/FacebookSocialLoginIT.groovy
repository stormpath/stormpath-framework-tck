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
package com.stormpath.tck.login

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.jayway.restassured.path.xml.element.NodeChildren
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.Iso8601Utils
import com.stormpath.tck.util.*
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.delete
import static com.jayway.restassured.RestAssured.given
import static com.jayway.restassured.RestAssured.put
import static org.hamcrest.Matchers.*
import static org.testng.Assert.*

@Test
class SocialLoginIT extends AbstractIT {
    // We are really only going to try testing LoginWithFacebook for now, so might have some things hardcoded.

    private String loginRoute = "/login"

    private String facebookClientID
    private String facebookClientSecret

    private String facebookTestUserAccessToken
    private String facebookTestUserEmail

    @BeforeClass
    private void getSocialProviderTokens() throws Exception {
        getSocialProviderIdsFromStormpath()
        getFacebookAccessToken()
    }

    private void getSocialProviderIdsFromStormpath() {
        assertNotNull(EnvUtils.stormpathApplicationHref, "We need the Application HREF to perform this test.")

        // Pull account stores
        List<String> accountStores = given()
            .header("User-Agent", "stormpath-framework-tck")
            .header("Authorization", RestUtils.getBasicAuthorizationHeaderValue())
            .port(443)
        .when()
            .get(EnvUtils.stormpathApplicationHref + "/accountStoreMappings")
        .then()
            .statusCode(200)
        .extract().path("items.accountStore.href")

        // Find the FB account store, then put its client id in the class
        accountStores.each { accountStore ->
            Map<String> provider = given()
                .header("User-Agent", "stormpath-framework-tck")
                .header("Authorization", RestUtils.getBasicAuthorizationHeaderValue())
                .port(443)
            .when()
                .get(accountStore + "?expand=provider")
            .then()
                .statusCode(200)
            .extract().path("provider")

            if (provider.get("providerId") == "facebook") {
                this.facebookClientID = provider.get("clientId")
                this.facebookClientSecret = provider.get("clientSecret")
                return
            }
        }
    }

    private void getFacebookAccessToken() {
        // We create a FB test user, and get its access token. We don't clean it up but oh well, TODO? 
        def fbTestUser = given()
            .port(443)
            .param("permissions", "email")
        .when()
            .post("https://graph.facebook.com/" + facebookClientID + "/accounts/test-users?access_token=" + facebookClientID + "|" + facebookClientSecret)
        .then()
            .statusCode(200)
        .extract().response()

        this.facebookTestUserAccessToken = fbTestUser.path("access_token")
        this.facebookTestUserEmail = fbTestUser.path("email")
    }

    /**
     * Attempts to login with the Facebook Access Token, and expects an account object back.
     * @throws Exception
     */
    @Test
    public void loginWithValidFacebookAccessTokenSucceeds() throws Exception {
        def loginJSON = ["providerData": [
                "providerId": "facebook",
                "accessToken": facebookTestUserAccessToken
        ]]

        def loginResponse = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(loginJSON)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(200)
            .body("account.email", is(facebookTestUserEmail))
        .extract().response()

        deleteOnClassTeardown(loginResponse.path("account.href"))
    }

    /**
     * Attempts to login with an invalid access token, and should fail. 
     * @throws Exception
     */
    @Test
    public void loginWithInvalidFacebookAccessTokenFails() throws Exception {
        def loginJSON = ["providerData": [
                "providerId": "facebook",
                "accessToken": "garbageToken"
        ]]

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(loginJSON)
        .when()
            .post(loginRoute)
        .then()
            .statusCode(400)
            .body("status", is(400))
    }
}