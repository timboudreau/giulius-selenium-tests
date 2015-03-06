/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mastfrog.groovytests.tests.selenium;

import com.mastfrog.giulius.tests.TestWith;
import com.mastfrog.selenium.SeleniumRunner;
import com.mastfrog.video.VideoModule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author Tim Boudreau
 */
@RunWith(SeleniumRunner.class)
@TestWith({ServerApplication.class, VideoModule.class})
public class JavaSeleniumTest {

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
