/**
 * Created by edjiang on 6/8/16.
 */

package com.stormpath.tck.util

import org.hamcrest.Factory
import org.hamcrest.Matcher
import com.stormpath.tck.util.matchers.*

class Matchers {
    @Factory
    public static Matcher urlMatchesPath(String path) {
        return new UrlMatchesPath(path);
    }
}