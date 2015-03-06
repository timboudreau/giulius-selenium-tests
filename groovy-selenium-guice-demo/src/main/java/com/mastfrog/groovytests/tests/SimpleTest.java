package com.mastfrog.groovytests.tests;

import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.giulius.tests.TestWith;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(GuiceRunner.class)
@TestWith(TestModule.class)
public class SimpleTest {

    @Test
    public void test(StringBuilder sb) {
        assertEquals("Foo bar", sb.toString());
    }
}
