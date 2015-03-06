package com.mastfrog.testmain;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 *
 * @author Tim Boudreau
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Ignore {

}
