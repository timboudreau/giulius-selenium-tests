package com.mastfrog.selenium;

import com.google.inject.Inject;
import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.guicy.annotations.Defaults;
import com.mastfrog.selenium.TestSeleniumTest.FixtureOne;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(SeleniumRunner.class)
@Defaults({"browser=htmlunit", "_baseUrl=http://localhost:9223", "webdriver.maximize=false"})
@TestWith(TestServletModule.class)
@Fixtures(FixtureOne.class)
public class TestSeleniumTest {
    private static boolean fixtureOneCreated;
    private static boolean fixtureTwoCreated;

    @Test
    @Fixtures(FixtureTwo.class)
    public void foo(ServletPageModel page, WebDriverWait wait) throws InterruptedException {
        assertNotNull(page.searchButton);
        assertNotNull(page.searchField);
        assertNotNull(page.prev);
        assertEquals("null", page.prev.getText());
        page.searchField.sendKeys("nuclear poodles");
        page.searchButton.click();
        assertNotNull(page.searchButton);
        assertNotNull(page.searchField);
        assertNotNull(page.prev);
        assertEquals("nuclear poodles", page.prev.getText());
        assertTrue(fixtureOneCreated);
        assertTrue(fixtureTwoCreated);
    }

    // Two things which should get created in the right order but not
    // actually passed in to anything - something like a login page

    static class FixtureOne {

        FixtureOne() {
            assertFalse(fixtureOneCreated);
            fixtureOneCreated = true;
            assertFalse(fixtureTwoCreated);
        }
    }

    static class FixtureTwo {

        @Inject
        FixtureTwo(WebDriver driver) {
            assertFalse(fixtureTwoCreated);
            fixtureTwoCreated = true;
            assertNotNull(driver);
        }
    }
}
