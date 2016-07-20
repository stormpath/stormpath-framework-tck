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

package com.stormpath.tck.errors

import com.jayway.restassured.http.ContentType
import com.stormpath.tck.AbstractIT
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.given
import static com.stormpath.tck.util.FrameworkConstants.MeRoute
import static com.stormpath.tck.util.FrameworkConstants.MissingRoute

@Test
class ErrorsIT extends AbstractIT {

    /**
     * A missing endpoint should return a 404
     * @see <a href="https://github.com/stormpath/stormpath-sdk-java/issues/706">#706</a>
     */
    @Test(groups=["v100", "html"])
    public void missingEndpointShouldReturn404() {
        given()
            .header("Accept", ContentType.HTML)
        .when()
            .get(MissingRoute)
        .then()
            .statusCode(404)
    }

    /**
     * A restricted endpoint should return JSON when Accept header is application/json
     * @see <a href="https://github.com/stormpath/stormpath-sdk-java/issues/706">#706</a>
     */
    @Test(groups=["v100", "json"])
    public void restrictedEndpointShouldReturn401AndWWWAuthenticateHeader() {
        given()
            .header("Accept", ContentType.JSON)
        .when()
            .get(MeRoute)
        .then()
            .statusCode(401)
            // 401 with Accept JSON header does not return JSON
            .header("WWW-Authenticate", "Bearer realm=\"My Application\"")
    }
}
