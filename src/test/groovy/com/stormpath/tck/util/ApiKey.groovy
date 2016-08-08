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
import org.codehaus.groovy.control.ConfigurationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ApiKey {

    private static final Logger log = LoggerFactory.getLogger(ApiKey)

    private static String apiKeyID
    private static String apiKeySecret

    public static final String DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION =
            System.getProperty("user.home") + File.separatorChar + ".stormpath" + File.separatorChar + "apiKey.properties";

    private final static String API_KEY_ID_PROPERTY_NAME = "apiKey.id"
    private final static String API_KEY_SECRET_PROPERTY_NAME = "apiKey.secret"

    private final static String API_KEY_FILE_ENV_NAME = "STORMPATH_API_KEY_FILE";

    private final static String[] API_KEY_ID_ENVIRONMENT_VARIABLE_NAMES = ["STORMPATH_API_KEY_ID", "STORMPATH_CLIENT_APIKEY_ID"]
    private final static String[] API_KEY_SECRET_ENVIRONMENT_VARIABLE_NAMES = ["STORMPATH_API_KEY_SECRET", "STORMPATH_CLIENT_APIKEY_SECRET"]

   static {
       //1. Try to load the default api key properties file.  All other config options have higher priority than this:
       Properties properties = getDefaultApiKeyFileProperties()
       if (properties.size() > 0) {
           apiKeyID = properties.get(API_KEY_ID_PROPERTY_NAME)
           apiKeySecret = properties.get(API_KEY_SECRET_PROPERTY_NAME)
       }

       //2. Try api key properties file environment variable
       properties = getApiKeyFilePropertiesByEnv();
       if (properties.size() > 0) {
           apiKeyID = properties.get(API_KEY_ID_PROPERTY_NAME)
           apiKeySecret = properties.get(API_KEY_SECRET_PROPERTY_NAME)
       }

       //3. Try environment variables:
       properties = getEnvironmentVariableProperties();
       if (properties.size() > 0) {
           apiKeyID = properties.get(API_KEY_ID_PROPERTY_NAME) != null ? properties.get(API_KEY_ID_PROPERTY_NAME) : apiKeyID
           apiKeySecret = properties.get(API_KEY_SECRET_PROPERTY_NAME) != null ? properties.get(API_KEY_SECRET_PROPERTY_NAME) : apiKeySecret
       }

       if (apiKeyID == null || apiKeySecret == null) {
           throw new ConfigurationException(
               "apiKeyID and apiKeySecret must be set by one of the following: \n" +
               "\t~/.stormpath/apiKey.properties file OR \n" +
               "\tSTORMPATH_API_KEY_FILE env variable OR \n" +
               "\tSTORMPATH_API_KEY_ID and STORMPATH_API_KEY_SECRET env variables \n" +
               "\tSTORMPATH_CLIENT_APIKEY_ID and STORMPATH_CLIENT_APIKEY_SECRET env variables."
           )
       }
    }

    static String getId() {
        return apiKeyID
    }

    static String getSecret() {
        return apiKeySecret
    }

    protected static Properties getDefaultApiKeyFileProperties() {
        return getApiKeyFileProperties(DEFAULT_API_KEY_PROPERTIES_FILE_LOCATION);
    }

    protected static Properties getApiKeyFilePropertiesByEnv() {
        return getApiKeyFileProperties(System.getenv(API_KEY_FILE_ENV_NAME));
    }

    protected static Properties getApiKeyFileProperties(String fileName) {
        Properties properties = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream((String)fileName);
            // load a properties file
            properties.load(input);
        } catch (Exception ex) {
            log.debug("Unable to find or load default api key properties file [" +
                    fileName + "].  This can be safely ignored as this is a " +
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

        String value = null
        for (int i = 0; i < API_KEY_ID_ENVIRONMENT_VARIABLE_NAMES.length; i++) {
            value = System.getenv(API_KEY_ID_ENVIRONMENT_VARIABLE_NAMES[i])
            if (Strings.hasText(value)) { break }
        }
        if (Strings.hasText(value)) {
            props.put(API_KEY_ID_PROPERTY_NAME, value)
        }

        value = null
        for (int i = 0; i < API_KEY_SECRET_ENVIRONMENT_VARIABLE_NAMES.length; i++) {
            value = System.getenv(API_KEY_SECRET_ENVIRONMENT_VARIABLE_NAMES[i])
            if (Strings.hasText(value)) { break }
        }
        if (Strings.hasText(value)) {
            props.put(API_KEY_SECRET_PROPERTY_NAME, value);
        }

        return props;
    }

}
