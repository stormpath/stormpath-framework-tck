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

import groovy.json.JsonSlurper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SigningKeyResolver
import io.jsonwebtoken.lang.Assert
import org.apache.commons.codec.binary.Base64

import java.security.Key
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPublicKeySpec

class JwtUtils {



    static String extractJwtClaim(String jwt, String property) {
        return parseJwt(jwt).getBody().get(property)
    }

    static Jws<Claims> parseJwt(String jwt) {


        if (EnvUtils.jwtSigningKeysUrl) {
            return Jwts.parser().setSigningKeyResolver(new URLSigningKeyResolver(EnvUtils.jwtSigningKeysUrl)).parseClaimsJws(jwt)
        }
        else {
            String secret = EnvUtils.jwtSigningKey
            return Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(jwt)
        }
    }

    private static class URLSigningKeyResolver implements SigningKeyResolver {
        def json

        URLSigningKeyResolver(String keysUrl) {
            def jsonSlurper = new JsonSlurper()
            json = jsonSlurper.parse(new URL(keysUrl))
        }

        @Override
        Key resolveSigningKey(JwsHeader header, Claims claims) {
            return getKey(header)
        }

        @Override
        Key resolveSigningKey(JwsHeader header, String plaintext) {
            return getKey(header)
        }

        private Key getKey(JwsHeader header) {
            String keyId = header.getKeyId()
            String keyAlgorithm = header.getAlgorithm()

            if (!"RS256".equals(keyAlgorithm)) {
                throw new UnsupportedOperationException("Only 'RS256' key algorithm is supported.")
            }

            def key = null
            for (def keyElement : json.keys) {
                if (keyId.equals(keyElement.kid)) {
                    key = keyElement
                    break
                }
            }
            Assert.notNull(key, "Key with 'kid' of "+keyId+" could not be found.")

            try {

                BigInteger modulus = new BigInteger(1, Base64.decodeBase64(key.n))
                BigInteger publicExponent = new BigInteger(1, Base64.decodeBase64(key.e))
                return KeyFactory.getInstance("RSA").generatePublic(
                        new RSAPublicKeySpec(modulus, publicExponent))

            } catch (NoSuchAlgorithmException e) {
                throw new UnsupportedOperationException("Failed to load key Algorithm", e)
            } catch (InvalidKeySpecException e) {
                throw new UnsupportedOperationException("Failed to load key", e)
            }
        }
    }
}
