package com.mastfrog.testmain.suites;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import java.lang.annotation.Target;

/**
 *
 * @author Tim Boudreau
 */
@Retention(SOURCE)
@Target(TYPE)
public @interface Suites {

	public static final String SUITES_FILE = "META-INF/tests/suites.list";

	String[] value();
}
