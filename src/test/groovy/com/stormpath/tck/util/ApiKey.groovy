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

import io.jsonwebtoken.lang.Strings
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ApiKey {

    private static final Logger log = LoggerFactory.getLogger(ApiKey)

    private static String apiKeyID
    private static String apiKeySecret

    public static final String DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION =
            System.getProperty("user.home") + File.separatorChar + ".stormpath" + File.separatorChar + "apiKey.properties";

    private final static String apiKeyIdPropertyName     = "apiKey.id"
    private final static String apiKeySecretPropertyName = "apiKey.secret"
    private final static String apiKeyIdEnvironmentVariableName = "STORMPATH_API_KEY_ID"
    private final static String apiKeySecretEnvironmentVariableName = "STORMPATH_API_KEY_SECRET"

   static {
        //1. Try to load the default api key properties file.  All other config options have higher priority than this:
        Properties properties = getDefaultApiKeyFileProperties()
        apiKeyID = properties.get(apiKeyIdPropertyName)
        apiKeySecret = properties.get(apiKeySecretPropertyName)

        //2. Try environment variables:
        properties = getEnvironmentVariableProperties();
        apiKeyID = properties.get(apiKeyIdPropertyName) != null ? properties.get(apiKeyIdPropertyName) : apiKeyID
        apiKeySecret = properties.get(apiKeySecretPropertyName) != null ? properties.get(apiKeySecretPropertyName) : apiKeySecret
    }

    static String getId() {
        return apiKeyID
    }

    static String getSecret() {
        return apiKeySecret
    }

    protected static Properties getDefaultApiKeyFileProperties() {
        Properties properties = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION);
            // load a properties file
            properties.load(input);
        } catch (IOException ex) {
            log.debug("Unable to find or load default api key properties file [" +
                    DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION + "].  This can be safely ignored as this is a " +
                    "fallback location - other more specific locations will be checked.");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return properties;
    }

    protected static Properties getEnvironmentVariableProperties() {
        Properties props = new Properties();

        String value = System.getenv(apiKeyIdEnvironmentVariableName);
        if (Strings.hasText(value)) {
            props.put(apiKeyIdPropertyName, value);
        }

        value = System.getenv(apiKeySecretEnvironmentVariableName);
        if (Strings.hasText(value)) {
            props.put(apiKeySecretPropertyName, value);
        }

        return props;
    }

}
