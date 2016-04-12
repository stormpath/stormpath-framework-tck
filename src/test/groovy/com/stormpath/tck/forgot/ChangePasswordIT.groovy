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

package com.stormpath.tck.forgot

import com.jayway.restassured.http.ContentType
import com.jayway.restassured.path.xml.XmlPath
import com.jayway.restassured.path.xml.element.Node
import com.stormpath.tck.AbstractIT
import com.stormpath.tck.util.*
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static com.jayway.restassured.RestAssured.*
import static org.testng.Assert.*
import static org.hamcrest.Matchers.*
import static org.hamcrest.MatcherAssert.assertThat
import static com.stormpath.tck.util.FrameworkConstants.ForgotRoute

class ChangePasswordIT extends AbstractIT {

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/166">#166</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void doNotHandlePut() throws Exception {
        put(ForgotRoute)
                .then()
                .assertThat().statusCode(404)
    }

    /** Only GET and POST should be handled
     * @see <a href="https://github.com/stormpath/stormpath-framework-tck/issues/166">#166</a>
     * @throws Exception
     */
    @Test(groups=["v100"])
    public void doNotHandleDelete() throws Exception {
        delete(ForgotRoute)
                .then()
                .assertThat().statusCode(404)
    }


}
