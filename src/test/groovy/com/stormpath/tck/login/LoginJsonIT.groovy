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
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static com.jayway.restassured.matcher.RestAssuredMatchers.*
import static org.hamcrest.Matchers.*

@Test
class LoginJsonIT extends AbstractIT {

    private final String randomUUID = UUID.randomUUID().toString()
    private final String accountEmail = "fooemail-" + randomUUID + "@stormpath.com"
    private final String accountUsername = randomUUID
    private final String accountGivenName = "GivenName-" + randomUUID
    private final String accountSurname = "Surname-" + randomUUID
    private final String accountPassword = "P@sword123!"

    /** Create account via JSON
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/27">#27</a>
     */
    @Test
    public void accountRegistrationJson() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", accountEmail)
        jsonAsMap.put("password", accountPassword)
        jsonAsMap.put("givenName", accountGivenName)
        jsonAsMap.put("surname", accountSurname)
        jsonAsMap.put("username", accountUsername)

        String createdHref =
            given()
                    .contentType(ContentType.JSON)
                    .body(jsonAsMap)
            .when()
                    .post("/register")
            .then()
                    .statusCode(200)
                    .contentType("application/json;charset=UTF-8")
                    .body("size()", is(1))
                    .body("account.size()", is(10))
                    .body("account.href", not(isEmptyOrNullString()))
                    .body("account.username", is(accountUsername))
                    .body("account.modifiedAt", not(isEmptyOrNullString()))
                    .body("account.status", is("ENABLED"))
                    .body("account.createdAt", not(isEmptyOrNullString()))
                    .body("account.email", is(accountEmail))
                    .body("account.middleName", isEmptyOrNullString())
                    .body("account.surname", is(accountSurname))
                    .body("account.givenName", is(accountGivenName))
                    .body("account.fullName", is(accountGivenName + " " + accountSurname))
            .extract()
                    .path("account.href")

        deleteOnClassTeardown(createdHref)
    }

    /**
     * Errors returned as JSON use API status and response
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/45">#27</a>
     */
    public void invalidLoginError() throws Exception {
        // todo
    }
}