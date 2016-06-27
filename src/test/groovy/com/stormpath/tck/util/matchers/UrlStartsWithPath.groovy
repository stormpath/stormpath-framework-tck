/**
 * Created by edjiang on 6/8/16.
 */

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class UrlStartsWithPath extends TypeSafeMatcher {
    String path;

    UrlStartsWithPath(String path) {
        this.path = path;
    }

    @Override
    public boolean matchesSafely(Object object) {
        if(object instanceof String) {
            String url = (String) object;
            Pattern regex = Pattern.compile("(http[s]?:\\/\\/?[^\\/\\s]+)?(\\/.*)");
            Matcher matcher = regex.matcher(url);
            matcher.matches();

            if(matcher.groupCount() == 2) {
                return matcher.group(2).startsWith(path);
            } else {
                return false;
            }
        }
        return false
    }

    public void describeTo(Description description) {
        description.appendText("doesn't match path");
    }
}