package com.mastfrog.groovytests.tests

import com.mastfrog.giulius.tests.TestWith
import com.mastfrog.video.VideoModule
import com.mastfrog.groovytests.tests.selenium.IndexPageModel
import com.mastfrog.selenium.SeleniumRunner
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.openqa.selenium.support.ui.WebDriverWait
import com.mastfrog.groovytests.tests.selenium.ServerApplication
/**
 *
 * @author Tim Boudreau
 */
@RunWith(SeleniumRunner)
@TestWith([ServerApplication, VideoModule])
class GroovySeleniumTest {

    @Test
    public void seleniumTest(IndexPageModel page, WebDriverWait wait) throws InterruptedException {
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
    }
}
