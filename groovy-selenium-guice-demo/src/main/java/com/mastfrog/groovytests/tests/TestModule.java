package com.mastfrog.groovytests.tests;

import com.google.inject.AbstractModule;

/**
 *
 * @author Tim Boudreau
 */
class TestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(StringBuilder.class).toInstance(new StringBuilder("Foo bar"));
    }
}
