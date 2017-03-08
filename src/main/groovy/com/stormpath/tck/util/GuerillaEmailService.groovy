package com.stormpath.tck.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import static com.jayway.restassured.RestAssured.get

class GuerillaEmailService {

    private static final String GUERILLA_MAIL_BASE = "http://api.guerrillamail.com/ajax.php"
    private static final ObjectMapper mapper = new ObjectMapper()

    static GuerillaEmail createGuerrillaEmail() {
        def jsonResponse = get(GUERILLA_MAIL_BASE + "?f=get_email_address").asString()

        return mapper.readValue(jsonResponse, GuerillaEmail.class)
    }

    static String getEmail(GuerillaEmail guerillaEmail, String fromAddressDomain) {
        def jsonResponse =
            get(GUERILLA_MAIL_BASE + "?f=get_email_list&offset=0&sid_token=" + guerillaEmail.getToken()).asString()

        JsonNode rootNode = mapper.readTree(jsonResponse)
        JsonNode emailList = rootNode.path("list")

        String emailId = null
        int count = 0

        while (emailId == null && count++ < 15) {
            for (JsonNode emailNode : emailList) {
                String mailFrom = emailNode.get("mail_from").asText()
                String localEmailId = emailNode.get("mail_id").asText()
                if (mailFrom.contains(fromAddressDomain)) {
                    emailId = localEmailId
                    break
                }
            }
            if (emailId == null) { // try retrieving email again
                Thread.sleep(1000)
                jsonResponse = get(GUERILLA_MAIL_BASE + "?f=get_email_list&offset=0&sid_token=" + guerillaEmail.getToken()).asString()
                rootNode = mapper.readTree(jsonResponse)
                emailList = rootNode.path("list")
            }
        }

        if (emailId == null) { return null }

        // fetch stormpath email content
        jsonResponse = get(GUERILLA_MAIL_BASE + "?f=fetch_email&sid_token=" + guerillaEmail.getToken() + "&email_id=" + emailId).asString()
        rootNode = mapper.readTree(jsonResponse)
        String emailBody = rootNode.get("mail_body").asText()

        return emailBody
    }

    static String extractChangePasswordHref(String emailBody, String tokenId) {
        Document doc = Jsoup.parse(emailBody)
        Elements hrefs = doc.getElementsByTag("a")

        // verify email
        String href = null;
        for (Element element : hrefs) {
            String curHref = element.attributes().get("href")
            if (curHref.contains(tokenId)) {
                href = curHref
                break
            }
        }
        return href
    }

    static String extractTokenFromHref(String hrefWithToken, String tokenId) {
        int tokenIndex = hrefWithToken.indexOf(tokenId + "=")
        if (tokenIndex < 0) { return null }
        return hrefWithToken.substring(tokenIndex + (tokenId + "=").length())
    }
}
