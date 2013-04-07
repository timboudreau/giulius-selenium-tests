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
import java.io.File;
import org.hamcrest.Matcher;
import org.junit.runner.RunWith;
import org.junit.internal.ArrayComparisonFailure;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * A base class for Selenium tests, with a few convenience methods and delegates
 * for the entire JUnit assert class' static methods.
 *
 * <p/>
 * If you use the protected assert*() methods on this class, and you annotate
 * your test with
 * <code>&#064;TakeScreenshotOnFailure</code>, a screen shot named with the test
 * name will be written into the process working directory (for example, you
 * might get a file named target/MyTest.doStuff.png).
 *
 * @author Tim Boudreau
 */
@RunWith(SeleniumRunner.class)
public abstract class SeleniumTest {

    @Inject
    private Utils utils;
    @Inject
    private FrameworkMethod method;
    @Inject
    private TestClass testClass;

    private boolean shouldTakeScreenshot() {
        return method != null
                && testClass != null
                && getClass().getAnnotation(TakeScreenshotOnFailure.class) != null
                && !Boolean.getBoolean("dont.take.screenshots");
    }

    private void takeScreenShot() {
        if (shouldTakeScreenshot()) {
            try {
                Screenshot sh = new Screenshot();
                String fileName = testClass.getJavaClass().getSimpleName()
                        + '.' + method.getName() + ".png";
                File f = new File(fileName);
                sh.save(f);
            } catch (Exception ex) {
                // print here - our rethrow of the AssertionError in the finally
                // block will swallow anything thrown otherwise
                ex.printStackTrace(System.err);
            }
        }
    }

    /**
     * Get the WebDriver for this test
     *
     * @return
     */
    protected final WebDriver driver() {
        return utils.driver();
    }

    /**
     * Get an object which can wait for changes in the html
     *
     * @return
     */
    protected final WebDriverWait waiter() {
        return utils.waiter();
    }

    /**
     * Instantiate a test fixture using PageFactory, but also injecting its
     * fields using Guice where need-be.
     *
     * @param <T>
     * @param type The object type
     * @return An instance of that type
     */
    protected final <T> T instantiate(Class<T> type) {
        return utils.instantiate(type);
    }

    /**
     * Wait for something to become visible
     *
     * @param elementToBeVisible
     */
    protected final void waitForVisible(WebElement elementToBeVisible) {
        utils.waitForVisible(elementToBeVisible);
    }

    /**
     * Wait for some number of seconds
     *
     * @param seconds
     */
    protected void waitFor(double seconds) {
        Utils.waitFor(seconds);
    }

    /**
     * Wait for an element to be refreshed
     *
     * @param by A way to find the element
     */
    protected final void waitForRefresh(By by) {
        utils.waitForRefresh(by);
    }

    /**
     * Wait for an element to be refreshed. This call will examine the element's
     * attributes to decide how to look up its replacement. This may fail if it
     * does not have ann id, name, class or css selector that is sufficiently
     * unique.
     *
     * @param el An element
     */
    protected final void waitForRefresh(WebElement el) {
        utils.waitForRefresh(el);
    }

    protected void assertTrue(String message, boolean condition) {
        try {
            org.junit.Assert.assertTrue(message, condition);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertTrue(boolean condition) {
        try {
            org.junit.Assert.assertTrue(condition);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertFalse(String message, boolean condition) {
        try {
            org.junit.Assert.assertFalse(message, condition);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertFalse(boolean condition) {
        try {
            org.junit.Assert.assertFalse(condition);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void fail(String message) {
        try {
            org.junit.Assert.fail(message);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void fail() {
        try {
            org.junit.Assert.fail();
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertEquals(String message, Object expected, Object actual) {
        try {
            org.junit.Assert.assertEquals(message, expected, actual);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertEquals(Object expected, Object actual) {
        try {
            org.junit.Assert.assertEquals(expected, actual);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotEquals(String message, Object first, Object second) {
        try {
            org.junit.Assert.assertNotEquals(message, first, second);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotEquals(Object first, Object second) {
        try {
            org.junit.Assert.assertNotEquals(first, second);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotEquals(String message, long first, long second) {
        try {
            org.junit.Assert.assertNotEquals(message, first, second);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotEquals(long first, long second) {
        try {
            org.junit.Assert.assertNotEquals(first, second);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotEquals(String message, double first, double second, double delta) {
        try {
            org.junit.Assert.assertNotEquals(message, first, second, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotEquals(double first, double second, double delta) {
        try {
            org.junit.Assert.assertNotEquals(first, second, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(String message, Object[] expecteds, Object[] actuals) throws ArrayComparisonFailure {
        try {
            org.junit.Assert.assertArrayEquals(message, expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(Object[] expecteds, Object[] actuals) {
        try {
            org.junit.Assert.assertArrayEquals(expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(String message, byte[] expecteds, byte[] actuals) throws ArrayComparisonFailure {
        try {
            org.junit.Assert.assertArrayEquals(message, expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(byte[] expecteds, byte[] actuals) {
        try {
            org.junit.Assert.assertArrayEquals(expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(String message, char[] expecteds, char[] actuals) throws ArrayComparisonFailure {
        try {
            org.junit.Assert.assertArrayEquals(message, expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(char[] expecteds, char[] actuals) {
        try {
            org.junit.Assert.assertArrayEquals(expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(String message, short[] expecteds, short[] actuals) throws ArrayComparisonFailure {
        try {
            org.junit.Assert.assertArrayEquals(message, expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(short[] expecteds, short[] actuals) {
        try {
            org.junit.Assert.assertArrayEquals(expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(String message, int[] expecteds, int[] actuals) throws ArrayComparisonFailure {
        try {
            org.junit.Assert.assertArrayEquals(message, expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(int[] expecteds, int[] actuals) {
        try {
            org.junit.Assert.assertArrayEquals(expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(String message, long[] expecteds, long[] actuals) throws ArrayComparisonFailure {
        try {
            org.junit.Assert.assertArrayEquals(message, expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(long[] expecteds, long[] actuals) {
        try {
            org.junit.Assert.assertArrayEquals(expecteds, actuals);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(String message, double[] expecteds, double[] actuals, double delta) throws ArrayComparisonFailure {
        try {
            org.junit.Assert.assertArrayEquals(message, expecteds, actuals, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(double[] expecteds, double[] actuals, double delta) {
        try {
            org.junit.Assert.assertArrayEquals(expecteds, actuals, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(String message, float[] expecteds, float[] actuals, float delta) throws ArrayComparisonFailure {
        try {
            org.junit.Assert.assertArrayEquals(message, expecteds, actuals, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertArrayEquals(float[] expecteds, float[] actuals, float delta) {
        try {
            org.junit.Assert.assertArrayEquals(expecteds, actuals, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertEquals(String message, double expected, double actual, double delta) {
        try {
            org.junit.Assert.assertEquals(message, expected, actual, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertEquals(String message, float expected, float actual, float delta) {
        try {
            org.junit.Assert.assertEquals(message, expected, actual, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertEquals(long expected, long actual) {
        try {
            org.junit.Assert.assertEquals(expected, actual);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertEquals(String message, long expected, long actual) {
        try {
            org.junit.Assert.assertEquals(message, expected, actual);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertEquals(double expected, double actual, double delta) {
        try {
            org.junit.Assert.assertEquals(expected, actual, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertEquals(float expected, float actual, float delta) {
        try {
            org.junit.Assert.assertEquals(expected, actual, delta);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotNull(String message, Object object) {
        try {
            org.junit.Assert.assertNotNull(message, object);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotNull(Object object) {
        try {
            org.junit.Assert.assertNotNull(object);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNull(String message, Object object) {
        try {
            org.junit.Assert.assertNull(message, object);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNull(Object object) {
        try {
            org.junit.Assert.assertNull(object);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertSame(String message, Object expected, Object actual) {
        try {
            org.junit.Assert.assertSame(message, expected, actual);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertSame(Object expected, Object actual) {
        try {
            org.junit.Assert.assertSame(expected, actual);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotSame(String message, Object unexpected, Object actual) {
        try {
            org.junit.Assert.assertNotSame(message, unexpected, actual);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected void assertNotSame(Object unexpected, Object actual) {
        try {
            org.junit.Assert.assertNotSame(unexpected, actual);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected <T> void assertThat(T actual, Matcher<? super T> matcher) {
        try {
            org.junit.Assert.assertThat(actual, matcher);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }

    protected <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
        try {
            org.junit.Assert.assertThat(reason, actual, matcher);
        } catch (AssertionError err) {
            try {
                takeScreenShot();
            } finally {
                throw err;
            }
        }
    }
}
