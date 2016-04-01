/**
 * Created by edjiang on 3/30/16.
 */

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.builder.ResponseSpecBuilder
import com.jayway.restassured.specification.ResponseSpecification

import static org.hamcrest.Matchers.*

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

}
