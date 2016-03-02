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
import com.jayway.restassured.response.Response
import com.stormpath.tck.AbstractIT

import com.stormpath.tck.util.JwtUtils
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.get
import static com.jayway.restassured.RestAssured.given
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import static org.testng.Assert.*

@Test(groups=['java'])
class JavaLoginIT extends AbstractIT {

    private final String randomUUID = UUID.randomUUID().toString();
    private final String accountEmail = "fooemail-" + randomUUID + "@stormpath.com";
    private final String accountGivenName = "GivenName-" + randomUUID;
    private final String accountSurname = "Surname-" + randomUUID;
    private final String accountPassword = "P@sword123!";

    private String sessionId;
    private String accountCookie;

    @Test
    public void accountRegistration() throws Exception {

        //1.  Get the /register page

        Response response = get("/register");
        XmlPath doc = getHtmlDoc(response)
        assertEquals(doc.get("html.body.div.div.div.div.div.children().children()[1].@type"), "hidden");
        assertEquals(doc.get("html.body.div.div.div.div.div.children().children()[1].@name"), "_csrf");
        this.sessionId = response.getCookie("JSESSIONID");
        String csrfValue = doc.get("html.body.div.div.div.div.div.children().children()[1].@value");


        //2. Post the new account

        response =
            given()
                .cookie("JSESSIONID", this.sessionId)
                .formParam("_csrf", csrfValue)
                .formParam("givenName", accountGivenName)
                .formParam("surname", accountSurname)
                .formParam("email", accountEmail)
                .formParam("password", accountPassword)
                .formParam("confirmPassword", accountPassword)

            //assert that the user is redirected to the default login.nextUri (which is '/'):
            .expect()
                .statusCode(302)
                .header("Location", equalTo(qualify('/')))

            //post the account
            .post("/register")
                .andReturn()

        //get the resulting cookie:
        this.accountCookie = response.getCookie("account");
        assertTrue(accountCookie.length() > 0);

        //3. Get the home page and assert it reflects the new user

        given()
            .cookie("account", accountCookie)
        .expect()
            .body(equalTo("Hello " + accountGivenName + "!"))
        .when()
            .get()

        deleteOnClassTeardown(JwtUtils.extractJwtClaim(accountCookie, "sub"))
    }

    @Test(dependsOnMethods = "accountRegistration")
    public void logout() throws Exception {

        Response response =
            given()
                .cookie("JSESSIONID", this.sessionId)
                .cookie("account", this.accountCookie)
            .expect()
                .statusCode(200)
            .get("/logout")

        this.accountCookie = response.getCookie("account");
        assertEquals(accountCookie, '')
    }

    @Test(dependsOnMethods = "logout")
    public void formAuthenticationInvalidCsrf() throws Exception {

        Response response = get("/login")
        this.sessionId = response.getCookie("JSESSIONID")

        given()
            .cookie("JSESSIONID", this.sessionId)
            .formParam("_csrf", "THIS-IS-NOT-A-VALID-CSRF-VALUE")
            .formParam("login", accountEmail)
            .formParam("password", accountPassword)
        .expect()
            .body(containsString("Invalid CSRF Token"))
            .statusCode(403)
        .when()
            .post("/login")
    }

    @Test(dependsOnMethods = "formAuthenticationInvalidCsrf")
    public void formAuthenticationWithCsrf() throws Exception {
        Response response = get("/login");
        this.sessionId = response.getCookie("JSESSIONID");

        XmlPath doc = new XmlPath(XmlPath.CompatibilityMode.HTML, response.getBody().asString());
        assertEquals(doc.get("html.body.div.div.div.div.div.children().children()[1].@type"), "hidden");
        assertEquals(doc.get("html.body.div.div.div.div.div.children().children()[1].@name"), "_csrf");
        String csrfValue = doc.get("html.body.div.div.div.div.div.children().children()[1].@value");

        response =
            given()
                .cookie("JSESSIONID", this.sessionId)
                .formParam("_csrf", csrfValue)
                .formParam("login", accountEmail)
                .formParam("password", accountPassword)
            .expect()
                .statusCode(302)
            .when()
                .post("/login")
            .andReturn()

        assertNotNull(response.getCookie("account"))
        this.accountCookie = response.getCookie("account")
    }

    @Test(dependsOnMethods = "formAuthenticationWithCsrf")
    public void homeScreenWithAuthenticatedAccount() throws Exception {
        given()
            .cookie("JSESSIONID", this.sessionId)
            .cookie("account", this.accountCookie)
        .expect()
            .statusCode(200)
            .body(containsString("Hello " + accountGivenName))
        .when()
            .get()
    }

}