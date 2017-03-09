package com.stormpath.tck.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class StringUtils {

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
