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
    final String middleName = null
    final String password = "P@sword123!"
    final String username = email

    def hiddens = [:]

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

    public void setHiddens(hiddens) {
        this.hiddens = hiddens
    }

    def getPropertiesMap() {
        def ret =  [email: email,
                password: password,
                givenName: givenName,
                surname: surname]

        if (!hiddens.isEmpty()) {
            hiddens.each {
                ret.put(it.key, it.value)
            }
        }

        return ret
    }
}