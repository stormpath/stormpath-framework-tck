/**
 * Created by edjiang on 3/30/16.
 */
package com.stormpath.tck.util

import com.jayway.restassured.http.ContentType
import static com.jayway.restassured.RestAssured.*

class TestAccount {
    final String randomUUID = UUID.randomUUID().toString()
    final String email = "fooemail-" + randomUUID + "@stormpath.com"
    final String givenName = "GivenName-" + randomUUID
    final String surname = "Surname-" + randomUUID
    final String middleName = "Foobar"
    final String password = "P@sword123!"
    final String username = "foo-" + randomUUID
    String href

    public void registerOnServer() {
        href = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getPropertiesMap())
        .when()
            .post(FrameworkConstants.RegisterRoute)
        .then()
            .statusCode(200)
        .extract()
            .path("account.href")
    }

    def getPropertiesMap() {
        return [email: email,
                password: password,
                givenName: givenName,
                surname: surname,
                middleName: middleName,
                username: username]
    }
}