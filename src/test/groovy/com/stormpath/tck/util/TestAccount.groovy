/**
 * Created by edjiang on 3/30/16.
 */
package com.stormpath.tck.util

import com.jayway.restassured.http.ContentType
import io.jsonwebtoken.lang.Strings

import static com.jayway.restassured.RestAssured.*

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
        def requestSpecification = given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(getPropertiesMap())

        if (Strings.hasText(csrf)) {
            requestSpecification.header("X-CSRF-TOKEN", csrf)
        }

        if (cookies != null) {
            requestSpecification.cookies(cookies)
        }

        href = requestSpecification
            .when()
                .post(FrameworkConstants.RegisterRoute)
            .then()
                .statusCode(200)
            .extract()
                .path("account.href")
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