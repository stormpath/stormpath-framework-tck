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

import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.jayway.restassured.path.xml.element.NodeChildren
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.JwtUtils
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.get
import static com.jayway.restassured.RestAssured.given
import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertTrue

@Test
class LoginOldIT extends AbstractIT {

    private final String randomUUID = UUID.randomUUID().toString()
    private final String accountEmail = "fooemail-" + randomUUID + "@stormpath.com"
    private final String accountGivenName = "GivenName-" + randomUUID
    private final String accountSurname = "Surname-" + randomUUID
    private final String accountPassword = "P@sword123!"

    private Map<String, String> cookies
    private String accessToken

    private Node findForm(NodeChildren children) {
        for (Node node : children.list()) {
            return (node.get("form") != null) ? node.get("form") : findForm(node.children())
        }
    }

    private Map<String, String> getHiddenFormFields(Node form) {
        Map<String, String> ret = new HashMap<String, String>()

        for (Node node : form.getNodes("input")) {
            if (node.attributes().get("type").equalsIgnoreCase("hidden")) {
                ret.put(node.attributes().get("name"), node.attributes().get("value"))
            }
        }

        return ret
    }

    @Test
    public void accountRegistration() throws Exception {

        //1.  Get the /register page

        Response response = get("/register")

        // store any cookies - we'll just feed them back later
        this.cookies = response.getCookies()

        XmlPath doc = getHtmlDoc(response)

        Node form = findForm(doc.get("html.body"))

        // grab any hidden form fields - we'll just feed them back later
        Map<String, String> hiddenFields = getHiddenFormFields(form)

        //2. Post the new account

        given()
                .cookies(this.cookies)
                .formParams(hiddenFields)
                .formParam("givenName", accountGivenName)
                .formParam("surname", accountSurname)
                .formParam("email", accountEmail)
                .formParam("password", accountPassword)

        // no confirm for express?

        // form value in java, doesn't interfere with
                .formParam("confirmPassword", accountPassword)
        // form value in laravel
        //.formParam("password_confirmation", accountPassword)

        //assert that the user is redirected
                .expect()
                .statusCode(302)

        //post the account
                .post("/register")
                .andReturn()
    }

    @Test(dependsOnMethods = "accountRegistration")
    public void login() throws Exception {

        //1.  Get the /login page

        Response response =
                given()
                        .header("Accept", "text/html")
                        .when()
                        .get("/login")

        // store any cookies - we'll just feed them back later
        this.cookies = response.getCookies()

        XmlPath doc = getHtmlDoc(response)

        Node form = findForm(doc.get("html.body"))

        // grab any hidden form fields - we'll just feed them back later
        Map<String, String> hiddenFields = getHiddenFormFields(form)

        //2. Post to login

        response =
                given()
                        .cookies(this.cookies)
                        .formParams(hiddenFields)
                        .formParam("login", accountEmail)
                        .formParam("password", accountPassword)

                //assert that the user is redirected to the default login.nextUri (which is '/'):
                        .expect()
                        .statusCode(302)

                //post the account
                        .post("/login")
                        .andReturn()

        //get the resulting cookie:
        this.accessToken = response.getCookie("access_token")

        assertTrue(this.accessToken.length() > 0);

        deleteOnClassTeardown(JwtUtils.extractJwtClaim(this.accessToken, "sub"))
    }

    @Test(dependsOnMethods = "login")
    public void logout() throws Exception {

        Response response =
                given()
                        .log().all()
                        .cookies(this.cookies)
                        .header("Authorization", "Bearer " + this.accessToken)
                        .expect()
                        .statusCode(302)
                        .post("/logout")
                        .andReturn()

        this.accessToken = response.getCookie("access_token");
        assertEquals(this.accessToken, '')
    }
}
