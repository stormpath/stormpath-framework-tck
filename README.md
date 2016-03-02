# Stormpath Framework TCK

The Stormpath Framework TCK (Test Compatibility Kit) is a collection of HTTP-based integration tests that ensure a
Stormpath web framework integration supports all
[Stormpath Framework Specification](https://github.com/stormpath/stormpath-framework-spec) behavior.  The TCK uses
only HTTP(S) requests to ensure that they may execute against a web application written in any
programming language that uses any of the various Stormpath [integrations](https://docs.stormpath.com/home/)

This project is mostly used by the Stormpath SDK teams to ensure stability and consistent behavior for
our customers, especially those that use Stormpath across multiple programming languages. And for our own
development sanity :)  Comments, suggestions and/or contributions from the Open Source community are most welcome.

## Getting Started

1. If you haven't installed Maven already:

        brew install maven

2. Check out the project:

        git clone git@github.com:stormpath/stormpath-framework-tck.git

## Running tests

Fire up your web application under test.  For example, a Java project might do this:

    mvn jetty:run

And a node.js project might do this:

    node server.js

Once your web app is running, you can run the TCK against this webapp:

    cd stormpath-framework-tck
    mvn clean verify

This will run all default (language-agnostic) tests against the targeted webapp.

## Using Maven Profiles to Customize TCK Behavior

The TCK will attempt to interact with a web application accessible by default via `http://localhost:8080`.  You can
tell the TCK which type of application you are using by using a
[Maven Profile](http://maven.apache.org/guides/introduction/introduction-to-profiles.html) by specifying the `-P` flag:

    mvn clean verify -Pnode

And this will assume a default base URI of `http://localhost:3000` (notice the changed port) since this is common for
node.js environments.

The currently supported profile names are:

* `java`
* `node`

Additional profile names can be added if different language environments require custom settings.

## Test Groups

If `-P` is not specified, all tests in the `default` group will be executed.

_All tests in the TCK **should** be in the `default` group_

However, sometimes a language-specific framework may not support the specification behavior (for legacy reasons), so
language/environment-specific tests can also be executed (in addition to the `default` tests) by specifying the specific
language named profile via `-P`.

For example specifying this:

    mvn clean verify -Pjava

ensures that all tests in the `default` group *and* those in the `java` group will be executed.

Similarly, this:

    mvn clean verify -Pnode

ensures that all tests in the `default` group and those in the `node` group will be executed.

Test authors should try *really* hard to ensure all tests can be `default` tests and avoid writing language/environment-specific
tests whenever possible.

Over time, as all language frameworks converge to specification compliance, there will no longer be a need for
language-specific tests, and they will all be removed such that only the `default` group remains.
