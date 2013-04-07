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

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mastfrog.giulius.ShutdownHookRegistry;
import com.mastfrog.settings.Settings;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Instantiates the correct Selenium WebDriver.   Binds WebDriver
 * and WebDriverWait so they can be injected.
 *
 * @author Tim Boudreau
 */
final class WebDriverModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(WebDriver.class).toProvider(DriverProvider.class);
        bind(WebDriverWait.class).toProvider(WaitProvider.class);
    }

    @Singleton
    private static class WaitProvider implements Provider<WebDriverWait> {

        private final Provider<WebDriver> driver;

        @Inject
        WaitProvider(Provider<WebDriver> driver) {
            this.driver = driver;
        }

        @Override
        public WebDriverWait get() {
            return new WebDriverWait(driver.get(), 15);
        }
    }

    @Singleton
    private static class DriverProvider implements Provider<WebDriver>, Runnable {

        private WebDriver driver;
        private final Settings settings;
        private final Provider<ShutdownHookRegistry> hook;
        private final Provider<URL> baseURL;

        @Inject
        public DriverProvider(Settings settings, Provider<ShutdownHookRegistry> hook, @Named("baseUrl") Provider<URL> baseURL) {
            this.settings = settings;
            this.hook = hook;
            this.baseURL = baseURL;
        }

        private synchronized WebDriver getDriver() {
            if (driver == null) {
                // Make the appropriate web driver
                WebDriver result;
                if (settings.getString("browser").equalsIgnoreCase("iexplore")
                        || settings.getString("browser").equalsIgnoreCase("ie")
                        || settings.getString("browser").equalsIgnoreCase("internet explorer")
                        || settings.getString("browser").equalsIgnoreCase("iexplorer")
                        || settings.getString("browser").equalsIgnoreCase("explorer")) {
                    result = new InternetExplorerDriver();
                } else if (settings.getString("browser").equalsIgnoreCase("firefox")) {
                    result = new FirefoxDriver();
                } else if (settings.getString("browser").equalsIgnoreCase("chrome")) {
                    result = new ChromeDriver();
                } else {
                    result = new HtmlUnitDriver();
                }

                hook.get().add(this);
                driver = result;

                int waitSeconds = settings.getInt("webdriver.implicitlyWaitSeconds", 10);
                driver.manage().timeouts().implicitlyWait(waitSeconds, TimeUnit.SECONDS);
                boolean maximize = settings.getBoolean("webdriver.maximize", false);
                if (maximize) {
                    driver.manage().window().maximize();
                }
                URL url = baseURL.get();
                if (url != null) {
                    driver.navigate().to(url);
                }
            }
            return driver;
        }

        @Override
        public void run() {
            if (driver != null) {
                try {
                    driver.quit();
                    driver.close();
                } catch (Exception e) {
                    // don't care
                }
            }
        }

        @Override
        public WebDriver get() {
            return getDriver();
        }
    }
}
