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
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.responseSpecs.JsonResponseSpec
import com.stormpath.tck.util.EnvUtils
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
import static com.stormpath.tck.util.FrameworkConstants.LoginRoute
import static com.stormpath.tck.util.FrameworkConstants.OauthRoute
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not

class FacebookSocialLoginIT extends AbstractIT {
    // We are really only going to try testing LoginWithFacebook for now, so might have some things hardcoded.

    private String facebookTestUserAccessToken
    private String facebookTestUserEmail

    @BeforeClass(alwaysRun = true)
    private void getSocialProviderTokens() throws Exception {
        getFacebookAccessToken()
    }

    private void getFacebookAccessToken() {
        // We create a FB test user, and get its access token. We don't clean it up but oh well, TODO?
        def fbTestUser = given()
            .port(443)
            .param("permissions", "email")
        .when()
            .post(
                "https://graph.facebook.com/" + EnvUtils.facebookClientId + "/accounts/test-users?access_token=" +
                EnvUtils.facebookClientId + "|" + EnvUtils.facebookClientSecret
            )
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
    @Test(groups = ["v100", "json"])
    void loginWithValidFacebookAccessTokenSucceeds() throws Exception {
        def loginJSON = ["providerData": [
                "providerId": "facebook",
                "accessToken": facebookTestUserAccessToken
        ]]

        def loginResponse = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(loginJSON)
        .when()
            .post(LoginRoute)
        .then()
            .statusCode(200)
            .body("account.email", is(facebookTestUserEmail))
        .extract().response()

        deleteOnClassTeardown(loginResponse.path("account.href").toString())
    }

    /**
     * Attempts to login with an invalid access token, and should fail.
     * @throws Exception
     */
    @Test(groups = ["v100", "json"])
    void loginWithInvalidFacebookAccessTokenFails() throws Exception {
        def loginJSON = ["providerData": [
                "providerId": "facebook",
                "accessToken": "garbageToken"
        ]]

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(loginJSON)
        .when()
            .post(LoginRoute)
        .then()
            .spec(JsonResponseSpec.isError(400))
    }

    /**
     * Attempts to use grant_type=stormpath_social with the Facebook Access Token, and expects an access_token back.
     * @throws Exception
     */
    @Test(groups = ["v100", "json"])
    void loginWithGrantTypeStormpathSocialSucceeds() throws Exception {

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.URLENC)
            .param("grant_type", "stormpath_social")
            .param("providerId", "facebook")
            .param("accessToken", facebookTestUserAccessToken)
        .when()
            .post(OauthRoute)
        .then()
            .statusCode(200)
            .body("access_token", not(isEmptyOrNullString()))
        .extract().response()
    }
}
