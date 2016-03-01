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

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.crypto.spec.SecretKeySpec;

public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils)

    static String extractJwtClaim(String jwt, String property) {
        String secret = ApiKey.getSecret()
        byte[] apiKeySecretBytes = Base64.decodeBase64(secret)
        SecretKeySpec keySpec = new SecretKeySpec(apiKeySecretBytes, SignatureAlgorithm.HS256.getJcaName())
        Claims claims = Jwts.parser().setSigningKey(keySpec).parseClaimsJws(jwt).getBody()
        return (String) claims.get(property)
    }
}
