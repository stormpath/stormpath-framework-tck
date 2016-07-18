/**
 * Created by edjiang on 3/30/16.
 */

package com.stormpath.tck.responseSpecs

import com.jayway.restassured.builder.ResponseSpecBuilder
import com.jayway.restassured.http.ContentType
import com.jayway.restassured.specification.ResponseSpecification

import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalToIgnoringCase
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.isEmptyOrNullString
import static org.hamcrest.Matchers.not

class JsonResponseSpec {
    static ResponseSpecification isError(int expectedStatusCode) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder()
        builder
            .expectStatusCode(expectedStatusCode)
            .expectContentType(ContentType.JSON)
            .expectBody("size()", is(2))
            .expectBody("status", is(expectedStatusCode))
            .expectBody("message", not(isEmptyOrNullString()))

        return builder.build()
    }

    static ResponseSpecification validAccessAndRefreshTokens() {
        ResponseSpecBuilder builder = new ResponseSpecBuilder()

        builder
            .expectStatusCode(200)
            .expectContentType(ContentType.JSON)
            .expectBody("access_token", not(isEmptyOrNullString()))
            .expectBody("expires_in", is(3600))
            .expectBody("refresh_token", not(isEmptyOrNullString()))
            .expectBody("token_type", equalToIgnoringCase("Bearer"))
            .expectHeader("Cache-Control", containsString("no-store"))
            .expectHeader("Pragma", is("no-cache"))

        return builder.build()
    }

}
