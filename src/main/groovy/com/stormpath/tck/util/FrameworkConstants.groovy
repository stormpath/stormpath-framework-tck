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

class FrameworkConstants {
    static String LoginRoute = "/login"
    static String LogoutRoute = "/logout"
    static String OauthRoute = "/oauth/token"
    static String RegisterRoute = "/register"
    static String MeRoute = "/me"
    static String VerifyRoute = "/verify"
    static String ForgotRoute = "/forgot"
    static String ChangeRoute = "/change"
    static String MissingRoute = "/missing" // should 404
}
