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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import static com.jayway.restassured.RestAssured.get
import static com.jayway.restassured.RestAssured.given
import static com.stormpath.tck.util.FrameworkConstants.RegisterRoute
import static org.testng.FileAssert.fail

class TestAccount {

    static enum Mode {
        WITH_DISPOSABLE_EMAIL,
        WITHOUT_DISPOSABLE_EMAIL
    }

    private static final String GUERILLA_MAIL_BASE = "http://api.guerrillamail.com/ajax.php"
    private static final ObjectMapper mapper = new ObjectMapper()

    final String randomUUID = UUID.randomUUID().toString()
    final String email
    final String givenName = "GivenName-" + randomUUID
    final String surname = "Surname-" + randomUUID
    final String middleName = null
    final String password = "P@sword123!"
    final String username
    final GuerillaEmail guerillaEmail

    String csrf
    Map<String, String> cookies

    // TODO - currently Stormpath specific. Will need to generify when dealing with Okta
    String href

    TestAccount(Mode mode) {
        if (mode == Mode.WITH_DISPOSABLE_EMAIL) {
            guerillaEmail = createGuerrillaEmail()
            username = email = guerillaEmail.email
        } else {
            guerillaEmail = null
            username = email = "fooemail-" + randomUUID + "@testmail.stormpath.com"
        }
    }

    void registerOnServer() {
        href = given()
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
        return [email: email, password: password, givenName: givenName, surname: surname]
    }

    GuerillaEmail createGuerrillaEmail() {
        def jsonResponse = get(GUERILLA_MAIL_BASE + "?f=get_email_address").asString()

        try {
            return mapper.readValue(jsonResponse, GuerillaEmail.class)
        } catch (JsonMappingException e) {
            fail("Couldn't create an email address using GuerillaMail")
        }
    }

    String getEmail(String fromAddressDomain) {
        String emailId = null
        int count = 0

        while (emailId == null && count++ < 30) {
            def jsonResponse =
                get(GUERILLA_MAIL_BASE + "?f=get_email_list&offset=0&sid_token=" + guerillaEmail.getToken()).asString()

            JsonNode emailList = null

            try {
                JsonNode rootNode = mapper.readTree(jsonResponse)
                emailList = rootNode.path("list")
            } catch (JsonMappingException e) {
                // gonna try to hit the api again, so ok to swallow exception
            }

            if (emailList != null) {
                for (JsonNode emailNode : emailList) {
                    String mailFrom = emailNode.get("mail_from").asText()
                    String localEmailId = emailNode.get("mail_id").asText()
                    if (mailFrom.contains(fromAddressDomain)) {
                        emailId = localEmailId
                        break
                    }
                }
            }
            Thread.sleep(1000)
        }

        if (emailId == null) {
            fail("Couldn't retrieve email")
            return null
        }

        // fetch stormpath email content
        def jsonResponse = get(
            GUERILLA_MAIL_BASE + "?f=fetch_email&sid_token=" + guerillaEmail.getToken() + "&email_id=" + emailId
        ).asString()
        def rootNode = mapper.readTree(jsonResponse)
        String emailBody = rootNode.get("mail_body").asText()

        return emailBody
    }
}

class GuerillaEmail {

    private String alias
    private String email
    private long timestamp
    private String token

    String getAlias() {
        return alias
    }

    void setAlias(String alias) {
        this.alias = alias
    }

    String getEmail() {
        return email
    }

    @JsonProperty("email_addr")
    void setEmail(String email) {
        this.email = email
    }

    long getTimestamp() {
        return timestamp
    }

    @JsonProperty("email_timestamp")
    void setTimestamp(long timestamp) {
        this.timestamp = timestamp
    }

    String getToken() {
        return token
    }

    @JsonProperty("sid_token")
    void setToken(String token) {
        this.token = token
    }
}