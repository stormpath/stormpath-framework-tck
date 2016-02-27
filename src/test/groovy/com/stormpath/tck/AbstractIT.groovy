package com.stormpath.tck

import com.jayway.restassured.RestAssured
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeTest

import static com.jayway.restassured.RestAssured.*
import static com.jayway.restassured.matcher.RestAssuredMatchers.*
import static org.hamcrest.Matchers.*

abstract class AbstractIT {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    static final String webappUrlScheme = System.getenv("STORMPATH_TCK_WEBAPP_SCHEME") ?: "http"
    static final String webappUrlHost = System.getenv("STORMPATH_TCK_WEBAPP_HOST") ?: "localhost"
    static final int webappUrlPort = Integer.parseInt(System.getenv("STORMPATH_TCK_WEBAPP_PORT") ?: "8080")
    static final private String webappUrlPortSuffix = toPortSuffix(webappUrlScheme, webappUrlPort)
    static final private String defaultWebappBaseUrl = "$webappUrlScheme://$webappUrlHost$webappUrlPortSuffix"
    static final String webappBaseUrl = System.getenv("STORMPATH_TCK_WEBAPP_URL") ?: defaultWebappBaseUrl

    private final List<String> classResourcesToDelete = []
    private final List<String> methodResourcesToDelete = []

    private static String toPortSuffix(String scheme, int port) {
        if (("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443)) {
            return ""
        }
        return ":$port"
    }

    @BeforeClass
    public void setUpClass() {
        //noinspection UnnecessaryQualifiedReference
        RestAssured.baseURI = webappBaseUrl
    }

    @BeforeTest
    public void setUp() {
        methodResourcesToDelete.clear() //fresh list per test
    }

    @AfterTest
    public void tearDown() {
        deleteResources(methodResourcesToDelete)
    }

    @AfterClass
    def tearDownClass() {
        deleteResources(classResourcesToDelete)
    }

    private void deleteResources(List<String> hrefs) {
        //delete in opposite order - it's cleaner; children are deleted before parents
        hrefs.reverse().each { href ->
            try {
                expect().statusCode(204).when().delete(href)
            } catch (Throwable t) {
                log.error("Unable to delete specified resource: $href", t)
            }
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected void deleteOnTeardown(String resourceHref) {
        if (resourceHref) {
            this.methodResourcesToDelete.add(resourceHref)
        }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected void deleteOnClassTeardown(String resourceHref) {
        if (resourceHref) {
            this.classResourcesToDelete.add(resourceHref)
        }
    }
}
