/**
 * Created by edjiang on 6/8/16.
 */

package com.stormpath.tck.util

import org.hamcrest.Factory
import org.hamcrest.Matcher
import com.stormpath.tck.util.matchers.UrlMatchesPath
import com.stormpath.tck.util.matchers.UrlStartsWithPath

class Matchers {
    @Factory
    public static Matcher urlMatchesPath(String path) {
        return new UrlMatchesPath(path);
    }
    @Factory
    public static Matcher urlStartsWithPath(String path) {
        return new UrlStartsWithPath(path);
    }
}