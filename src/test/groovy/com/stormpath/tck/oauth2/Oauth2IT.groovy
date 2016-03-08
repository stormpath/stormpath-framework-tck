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
package com.stormpath.tck.oauth2

import com.stormpath.tck.AbstractIT
import org.testng.annotations.Test
import static com.jayway.restassured.RestAssured.*
import static com.jayway.restassured.matcher.RestAssuredMatchers.*
import static org.hamcrest.Matchers.*

@Test
class Oauth2IT extends AbstractIT {

    @Test
    public void unsupportedGrantType() throws Exception {
        given()
            .param("grant_type", "foobar_grant")
            .param("username", "foo")
            .param("password", "bar")
        .when()
            .post("/oauth/token")
        .then()
            .statusCode(400)
            .header("Content-Type", is("application/json;charset=UTF-8"))
            .header("Cache-Control", is("no-store"))
            .header("Pragma", is("no-cache"))
            .body("error", is("unsupported_grant_type"))
    }
}