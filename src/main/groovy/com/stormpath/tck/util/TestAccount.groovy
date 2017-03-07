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
package com.stormpath.tck.util

import static com.jayway.restassured.RestAssured.given
import static com.stormpath.tck.util.FrameworkConstants.RegisterRoute

class TestAccount {
    final String randomUUID = UUID.randomUUID().toString()
    final String email = "fooemail-" + randomUUID + "@testmail.stormpath.com"
    final String givenName = "GivenName-" + randomUUID
    final String surname = "Surname-" + randomUUID
    final String middleName = null
    final String password = "P@sword123!"
    final String username = email

    String csrf
    Map<String, String> cookies

    String href

    void registerOnServer() {
        href =
            given()
                .body(getPropertiesMap())
            .when()
                .post(RegisterRoute)
            .then()
                .statusCode(200)
            .extract()
                .path("account.href")
    }

    void setCSRF(String csrf) {
        this.csrf = csrf
    }

    void setCookies(Map<String, String> cookies) {
        this.cookies = cookies
    }

    def getPropertiesMap() {
        return [email: email,
                password: password,
                givenName: givenName,
                surname: surname]
    }
}