package com.stormpath.tck.authentication

import com.stormpath.tck.AbstractIT
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
import static com.stormpath.tck.util.FrameworkConstants.MeRoute

class BearerIT extends AbstractIT {

    @Test(groups=["v100", "json", "html"])
    public void refreshTokenAsAccessBearerTokenFails() throws Exception {
        def (String accessToken, String refreshToken) = createTestAccountTokens()

        given()
            .header("Authorization", "Bearer " +refreshToken)
        .when()
            .get(MeRoute)
        .then()
            .statusCode(401)
    }

}
