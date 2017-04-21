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
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.SigningKeyResolver
import io.jsonwebtoken.impl.DefaultJwtParser
import io.jsonwebtoken.impl.crypto.JwtSignatureValidator

import java.security.Key

class JwtUtils {



    static String extractJwtClaim(String jwt, String property) {
        return parseJwt(jwt).getBody().get(property)
    }

    static Jws<Claims> parseJwt(String jwt) {

        if (EnvUtils.jwtValidationEnabled) {
            String secret = EnvUtils.jwtSigningKey
            return Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(jwt)
        }
        else {
            return parseJwtWithoutValidation(jwt)
        }
    }

    private static Jws<Claims> parseJwtWithoutValidation(String jwt) {
        return new DefaultJwtParser() {
            protected JwtSignatureValidator createSignatureValidator(SignatureAlgorithm alg, Key key) {
                return new JwtSignatureValidator() {
                    @Override
                    boolean isValid(String jwtWithoutSignature, String base64UrlEncodedSignature) {
                        return true
                    }
                }
            }
        }.setSigningKeyResolver(new SigningKeyResolver() {
            @Override
            Key resolveSigningKey(JwsHeader header, Claims claims) {
                return new DummyKey()
            }

            @Override
            Key resolveSigningKey(JwsHeader header, String plaintext) {
                return new DummyKey()
            }
        }).parseClaimsJws(jwt)
    }

    static class DummyKey implements Key {
        @Override
        String getAlgorithm() {
            return null
        }

        @Override
        String getFormat() {
            return null
        }

        @Override
        byte[] getEncoded() {
            return new byte[0]
        }
    }
}
