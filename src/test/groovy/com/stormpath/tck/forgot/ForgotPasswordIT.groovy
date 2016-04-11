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

package com.stormpath.tck.forgot

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.response.Cookie
import com.jayway.restassured.response.Cookies
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.*
import com.stormpath.tck.responseSpecs.*
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static org.testng.Assert.*
import static org.hamcrest.Matchers.*
import static com.stormpath.tck.util.FrameworkConstants.ForgotRoute

@Test
class ForgotPasswordIT extends AbstractIT {
    private TestAccount account = new TestAccount()

    @BeforeClass
    private void createTestAccount() throws Exception {
        account.registerOnServer()
        deleteOnClassTeardown(account.href)
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/165">#165</a>
     * @throws Exception
     */
    @Test
    public void doNotHandlePut() throws Exception {
        put(ForgotRoute)
            .then()
                .assertThat().statusCode(404)
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/165">#165</a>
     * @throws Exception
     */
    @Test
    public void doNotHandleDelete() throws Exception {
        delete(ForgotRoute)
            .then()
                .assertThat().statusCode(404)
    }

    /** GET should not be handled for JSON requests
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/215">#215</a>
     * @throws Exception
     */
    @Test
    public void doNotHandleJsonGet() throws Exception {
        given()
            .accept(ContentType.JSON)
        .when()
            .get(ForgotRoute)
        .then()
            .statusCode(404)
    }
}
