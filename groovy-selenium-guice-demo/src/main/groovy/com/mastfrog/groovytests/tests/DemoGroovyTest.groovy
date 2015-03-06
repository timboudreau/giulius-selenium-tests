package com.mastfrog.groovytests.tests

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By

import com.google.inject.Inject
import com.mastfrog.giulius.tests.TestWith
import com.mastfrog.giulius.tests.GuiceRunner
import com.mastfrog.groovytests.tests.TestModule

@RunWith(GuiceRunner)
@TestWith(TestModule)
public class DemoGroovyTest {

    @Test
    public void buyAllAvailableLoans(StringBuilder sb) {
        System.out.println("Hey, I'm groovy!");
        Assert.assertEquals("Foo bar", sb.toString());
    }

}
