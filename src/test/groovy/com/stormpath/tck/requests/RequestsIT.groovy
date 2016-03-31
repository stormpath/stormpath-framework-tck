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

import com.jayway.restassured.http.ContentType
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.*
import org.testng.annotations.Test
import static com.jayway.restassured.RestAssured.given
import static org.hamcrest.Matchers.*

@Test
class RequestsIT extends AbstractIT {

    /**
     * Null or empty Accept header is treated as * / *
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/72">#72</a>
     */
    @Test
    public void emptyAccept() {

        given()
            .header("Accept", "")
        .when()
            .get(FrameworkConstants.LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            // application/json is the default Content-Type
    }

    /**
     * Null or empty Accept header is treated as * / *
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/72">#72</a>
     */
    @Test
    public void missingAccept() {

        given()
        .when()
            .get(FrameworkConstants.LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            // application/json is the default Content-Type
    }

    /**
     * Accept: * / * uses first value in web.produces as Content-Type
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/73">#73</a>
     */
    @Test
    public void acceptAny() {

        given()
            .accept(ContentType.ANY)
        .when()
            .get(FrameworkConstants.LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            // application/json is the default Content-Type
    }

    /**
     * Specifying valid Accept Content-Type returns that Content-Type
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/75">#75</a>
     */
    @Test
    public void acceptJson() {

        given()
            .accept(ContentType.JSON)
        .when()
            .get(FrameworkConstants.LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
    }

    /**
     * Specifying valid Accept Content-Type returns that Content-Type
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/75">#75</a>
     */
    @Test
    public void acceptHtml() {

        given()
            .accept(ContentType.HTML)
        .when()
            .get(FrameworkConstants.LoginRoute)
        .then()
            .statusCode(200)
            .contentType(ContentType.HTML)
    }

    /**
     * Unknown Accept header is not handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/76">#76</a>
     */
    @Test
    public void unknownAccept() {

        given()
            .header("Accept", "foo/bar")
        .when()
            .get(FrameworkConstants.LoginRoute)
        .then()
            .statusCode(404)
    }
}
