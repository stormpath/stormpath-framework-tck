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

import io.jsonwebtoken.impl.Base64Codec

class RestUtils {

    public static getBasicAuthorizationHeaderValue() {
        String authorizationHeader = ApiKey.getId() + ":" + ApiKey.getSecret()
        byte[] valueBytes;
        valueBytes = authorizationHeader.getBytes("UTF-8");
        authorizationHeader = new Base64Codec().encode(valueBytes);
        return "Basic " + authorizationHeader
    }

}
