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
package com.stormpath.tck.register

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.response.Cookie
import com.jayway.restassured.response.Cookies
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.JwtUtils
import com.stormpath.tck.util.RestUtils
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static org.testng.Assert.*
import static org.hamcrest.Matchers.*

@Test
class RegisterIT extends AbstractIT {

    private static final String registerPath = "/register"

    /**
     * Serve the registration view model for request type application/json
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/179">#179</a>
     * @throws Exception
     */
    @Test
    public void servesViewModel() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .get(registerPath)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body(".", hasKey("form"))
            .body(".", hasKey("accountStores"))
    }

    /**
     * Return JSON error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/189">#189</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorIfPostIsEmpty() throws Exception {

        given()
            .accept(ContentType.JSON)
        .when()
            .post(registerPath)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/189">#189</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorIfEmailIsMissing() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", "foo@bar.baz")
        jsonAsMap.put("password", "")

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerPath)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if required fields are missing
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/189">#189</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorIfRequiredFieldIsMissing() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", "foo@bar.baz")
        jsonAsMap.put("password", "foobar123")
        jsonAsMap.put("surname", "Testerman")
        // givenName is required per the default configuration

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerPath)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if undefined custom field is present
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/195">#195</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorForUndefinedRootCustomField() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", "foo@bar.baz")
        jsonAsMap.put("password", "foobar123")
        jsonAsMap.put("givenName", "Test")
        jsonAsMap.put("surname", "Testerman")
        jsonAsMap.put("customValue", "foobar")
        // field 'customValue' is not defined in the default configuration

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerPath)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if undefined custom field is present
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/195">#195</a>
     * @throws Exception
     */
    @Test
    public void returnsErrorForUndefinedCustomField() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", "foo@bar.baz")
        jsonAsMap.put("password", "foobar123")
        jsonAsMap.put("givenName", "Test")
        jsonAsMap.put("surname", "Testerman")

        Map<String, Object> customDataMap = new HashMap<>();
        customDataMap.put("hello", "world")

        jsonAsMap.put("customData", customDataMap)
        // field 'hello' is not defined in the default configuration

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerPath)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }

    /**
     * Return JSON error if server error occurs
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/201">#201</a>
     * @throws Exception
     */
    @Test
    public void returnsJsonErrorForServerError() throws Exception {

        Map<String, Object>  jsonAsMap = new HashMap<>();
        jsonAsMap.put("email", "foo@bar")
        jsonAsMap.put("password", "1")
        jsonAsMap.put("givenName", "Test")
        jsonAsMap.put("surname", "Testerman")
        // Email and password will not pass Stormpath API validation and will error

        given()
            .accept(ContentType.JSON)
            .body(jsonAsMap)
        .when()
            .post(registerPath)
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("size()", is(2))
            .body("status", is(400))
            .body("message", not(isEmptyOrNullString()))
    }
}
