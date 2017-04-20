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
import io.jsonwebtoken.Jwts

import java.security.Key
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec

class JwtUtils {
    static Key getPublicKey() {
        def modulus = new BigInteger(1, Base64.getUrlDecoder().decode(EnvUtils.jwtSigningKeyModulus))
        def exponent = new BigInteger(1, Base64.getUrlDecoder().decode(EnvUtils.jwtSigningKeyExponent))
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent))
    }

    static String extractJwtClaim(String jwt, String property) {
        Claims claims = Jwts.parser().setSigningKey(getPublicKey()).parseClaimsJws(jwt).getBody()
        return (String) claims.get(property)
    }

    static Jws<Claims> parseJwt(String jwt) {
        return Jwts.parser().setSigningKey(getPublicKey()).parseClaimsJws(jwt)
    }
}
