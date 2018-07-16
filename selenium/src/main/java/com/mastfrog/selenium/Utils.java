/* 
 * The MIT License
 *
 * Copyright 2013 Tim Boudreau.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mastfrog.selenium;

import com.google.inject.Inject;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.util.preconditions.Checks;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Some selenium utilities for waiting for elements and such.
 * 
 * If you subclass SeleniumTest, an instance of Utils will be injected
 * for you and all the methods here are mirrored as protected methods there.
 *
 * @author Tim Boudreau
 */
public final class Utils {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Dependencies deps;

    @Inject
    Utils(WebDriver driver, WebDriverWait wait, Dependencies deps) {
        this.driver = driver;
        this.wait = wait;
        this.deps = deps;
    }

    /**
     * Get the WebDriver in use for this test
     *
     * @return the web driver
     */
    public WebDriver driver() {
        return driver;
    }

    /**
     * Get the thing which can wait for elements to be shown or other changes to
     * occur in the page (e.g. ajax requests completing)
     *
     * @return The waiter
     */
    public WebDriverWait waiter() {
        return wait;
    }

    /**
     * Instantiate an object,using Selenium's PageFactory, and injecting any
     * fields annotated with &#064;Inject by Guice.
     * <p/>
     * This is useful if you have caused the browser page to refresh and you
     * need to rebuild a model of the window's page.
     *
     * @param <T> A type
     * @param type The object to create
     * @return A new instance of this type
     */
    public <T> T instantiate(Class<T> type) {
        Checks.notNull("type", type);
        T result = PageFactory.initElements(driver, type);
        deps.injectMembers(result);
        return result;
    }

    /**
     * Verifies if an element is visible. Continue with tests even if false.
     *
     * @param expression boolean expression to evaluate
     * @param msg message to log on failure
     */
    public void waitForVisible(WebElement elementToBeVisible) {
        Checks.notNull("elementToBeVisible", elementToBeVisible);
        try {
            wait.until(ExpectedConditions.visibilityOf(elementToBeVisible));
        } catch (Exception e) {
            Exceptions.chuck(e);
        }
    }

    /**
     * Wait for some number of seconds.
     *
     * @param seconds A number of seconds
     */
    public static void waitFor(double seconds) {
        Checks.nonNegative("seconds", seconds);
        if (seconds > 1000 * 60 * 60) {
            throw new Error("I don't think you really want to wait for "
                    + (seconds / 60000) + " minutes");
        }
        try {
            Thread.sleep((long) (seconds * 1000));
        } catch (InterruptedException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.INFO, "Exception waiting", ex);
        }
    }

    /**
     * Wait for a web page component to be refreshed
     * @param by The way to look up the element
     */
    public void waitForRefresh(final By by) {
        wait.until(ExpectedConditions.refreshed(new ExpectedCondition<WebElement>() {
            @Override
            public WebElement apply(WebDriver f) {
                return f.findElement(by);
            }
        }));
    }

    /**
     * Wait for a page element to be refreshed.  Attempts to derive a 
     * recipe for looking up the element from its attributes.  May fail.
     * @param el The element
     */
    public void waitForRefresh(final WebElement el) {
        By by = null;
        if (el.getAttribute("id") != null) {
            by = By.id(el.getAttribute("id"));
        } else if (el.getAttribute("name") != null) {
            by = By.name(el.getAttribute("name"));
        } else if (el.getAttribute("class") != null) {
            by = By.className(el.getAttribute("class"));
        } else if (el.getAttribute("style") != null) {
            by = By.cssSelector("style");
        } else {
            throw new Error("No good way to look up " + el);
        }
        waitForRefresh(by);
    }
}
