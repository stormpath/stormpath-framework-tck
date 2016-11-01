package com.stormpath.tck.authentication

import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.RestUtils
import com.stormpath.tck.util.TestAccount
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
import static com.stormpath.tck.util.FrameworkConstants.MeRoute

class BasicIT extends AbstractIT {
    private TestAccount account = new TestAccount()

    @BeforeClass(alwaysRun = true)
    private void getTestAccountAccessToken() throws Exception {
        account.registerOnServer()
        deleteOnClassTeardown(account.href)
    }

    @Test(groups=["v100", "json"])
    public void basicAuthWithInvalidKeyIdFails() throws Exception {
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

        String apiKeyId = "nopenopenope"
        String apiKeySecret = apiKeysResource.body().jsonPath().getString("secret")

        given()
            .auth().preemptive().basic(apiKeyId, apiKeySecret)
        .when()
            .get(MeRoute)
        .then()
            .statusCode(401)
    }

    @Test(groups=["v100", "json"])
    public void basicAuthWithInvalidKeySecretFails() throws Exception {
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
        String apiKeySecret = "definitelyNotTheCorrectSecret!!"

        given()
            .auth().preemptive().basic(apiKeyId, apiKeySecret)
        .when()
            .get(MeRoute)
        .then()
            .statusCode(401)
    }

    @Test(groups=["v100", "json"])
    public void basicAuthWithDisabledApiKeyFails() throws Exception {
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

        String href = apiKeysResource.body().jsonPath().getString("href")

        // Disable the new API key
        given()
            .header("User-Agent", "stormpath-framework-tck")
            .header("Authorization", RestUtils.getBasicAuthorizationHeaderValue())
            .header("Content-Type", "application/json")
            .port(443)
            .body([status: "disabled"])
        .when()
            .post(href)
        .then()
            .statusCode(200)

        String apiKeyId = apiKeysResource.body().jsonPath().getString("id")
        String apiKeySecret = "definitelyNotTheCorrectSecret!!"

        given()
            .auth().preemptive().basic(apiKeyId, apiKeySecret)
        .when()
            .get(MeRoute)
        .then()
            .statusCode(401)
    }
}
