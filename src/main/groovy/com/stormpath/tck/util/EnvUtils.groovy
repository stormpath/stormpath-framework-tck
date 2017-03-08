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

import static org.testng.FileAssert.fail

class EnvUtils {

    public static final String stormpathHtmlEnabled = getVal("STORMPATH_TCK_HTML_ENABLED", "true")

    public static final String jwtSigningKey
    public static final String facebookClientId
    public static final String facebookClientSecret

    static {
        jwtSigningKey = getVal("JWT_SIGNING_KEY")
        facebookClientId = getVal("FACEBOOK_CLIENT_ID")
        facebookClientSecret = getVal("FACEBOOK_CLIENT_SECRET")
        if (jwtSigningKey == null || facebookClientId == null || facebookClientSecret == null) {
            fail("JWT_SIGNING_KEY, FACEBOOK_CLIENT_ID and FACEBOOK_CLIENT_SECRET environment variables are required")
        }
    }

    static String getVal(String name) {
        return getVal(name, null)
    }

    static String getVal(String name, String defaultVal) {

        //convert to system property format and try that first (they have precedence over environment variables):
        String sysPropName = name.toLowerCase().replace('_', '.')
        String val = System.getProperty(sysPropName)
        if (val) {
            return val
        }

        val = System.getenv(name)
        if (val) {
            return val
        }

        return defaultVal
    }
}
