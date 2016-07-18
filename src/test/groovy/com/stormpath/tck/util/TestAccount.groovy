/**
 * Created by edjiang on 3/30/16.
 */
package com.stormpath.tck.util

import com.jayway.restassured.http.ContentType
import io.jsonwebtoken.lang.Strings

import static com.jayway.restassured.RestAssured.given

class TestAccount {
    final String randomUUID = UUID.randomUUID().toString()
    final String email = "fooemail-" + randomUUID + "@stormpath.com"
    final String givenName = "GivenName-" + randomUUID
    final String surname = "Surname-" + randomUUID
    final String middleName = null
    final String password = "P@sword123!"
    final String username = email

    String csrf
    Map<String, String> cookies

    String href

    public void registerOnServer() {
        href = given()
            .port(443)
            .accept(ContentType.JSON)
            .header("Authorization", RestUtils.getBasicAuthorizationHeaderValue())
            .header("User-Agent", "stormpath-framework-tck")
            .contentType(ContentType.JSON)
            .body(getPropertiesMap())
            .when()
                .post(EnvUtils.stormpathApplicationHref + "/accounts")
            .then()
                .statusCode(201)
            .extract()
                .path("href")
    }

    public void setCSRF(String csrf) {
        this.csrf = csrf
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies
    }

    def getPropertiesMap() {
        return [email: email,
                password: password,
                givenName: givenName,
                surname: surname]
    }
}