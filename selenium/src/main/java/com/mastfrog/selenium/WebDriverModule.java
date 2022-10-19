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
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.inject.spi.ProvisionListener;
import com.mastfrog.settings.Settings;
import com.mastfrog.shutdown.hooks.ShutdownHookRegistry;
import static com.mastfrog.util.collections.CollectionUtils.setOf;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindAll;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.PageFactoryFinder;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Instantiates the correct Selenium WebDriver. Binds WebDriver and
 * WebDriverWait so they can be injected.
 *
 * @author Tim Boudreau
 */
final class WebDriverModule extends AbstractModule {

    @Override
    protected void configure() {
        final DriverProvider driverProvider = new DriverProvider(binder().getProvider(Settings.class),
                binder().getProvider(ShutdownHookRegistry.class), binder().getProvider(Key.get(URL.class, Names.named("baseUrl"))));

        bind(WebDriver.class).toProvider(driverProvider);
        bind(WebDriverWait.class).toProvider(WaitProvider.class);

        final Set<Class<? extends Annotation>> seleniumAnnotationTypes
                = setOf(FindBy.class, FindBys.class, CacheLookup.class, FindAll.class, PageFactoryFinder.class);

        Matcher<Binding> typeHasSeleniumAnnotatedFields = new AbstractMatcher<Binding>() {
            private final Set<Class<?>> hasSeleniumAnnotations = new HashSet<>();
            private final Set<Class<?>> noSeleniumAnnotations = new HashSet<>();

            @Override
            public boolean matches(Binding t) {
                Class<?> type = t.getKey().getTypeLiteral().getRawType();
                if (hasSeleniumAnnotations.contains(type)) {
                    return true;
                } else if (noSeleniumAnnotations.contains(type)) {
                    return false;
                }
                for (Field f : type.getDeclaredFields()) {
                    for (Class<? extends Annotation> anno : seleniumAnnotationTypes) {
                        if (f.getAnnotation(anno) != null) {
                            hasSeleniumAnnotations.add(type);
                            return true;
                        }
                    }
                }
                noSeleniumAnnotations.add(type);
                return false;
            }
        };

        binder().bindListener(typeHasSeleniumAnnotatedFields, new ProvisionListener() {
            @Override
            public <T> void onProvision(ProvisionListener.ProvisionInvocation<T> provision) {
                Class<? super T> type = provision.getBinding().getKey().getTypeLiteral().getRawType();
                T obj = provision.provision();
                if (driverProvider.driver != null) {
                    PageFactory.initElements(driverProvider.driver, obj);
                }
            }
        });
    }

    @Singleton
    private static class WaitProvider implements Provider<WebDriverWait> {

        private final Provider<WebDriver> driver;
        private final int waitDurationSeconds;
        public static final int MULTIPLIER = 10;

        @Inject
        WaitProvider(Provider<WebDriver> driver, Settings settings) {
            this.driver = driver;
            waitDurationSeconds = (settings.getInt("sleep", 1000) * MULTIPLIER) / 1000;
        }

        @Override
        public WebDriverWait get() {
            return new WebDriverWait(driver.get(), waitDurationSeconds);
        }
    }

    @Singleton
    private static class DriverProvider implements Provider<WebDriver>, Runnable {

        WebDriver driver;
        private final Provider<Settings> settings;
        private final Provider<ShutdownHookRegistry> hook;
        private final Provider<URL> baseURL;

        @Inject
        public DriverProvider(Provider<Settings> settings, Provider<ShutdownHookRegistry> hook, @Named("baseUrl") Provider<URL> baseURL) {
            this.settings = settings;
            this.hook = hook;
            this.baseURL = baseURL;
        }

        private synchronized WebDriver getDriver() {
            if (driver == null) {
                // Make the appropriate web driver
                WebDriver result;
                Settings settings = this.settings.get();
                String browser = settings.getString("browser", "");

                if (browser.equalsIgnoreCase("iexplore")
                        || browser.equalsIgnoreCase("ie")
                        || browser.equalsIgnoreCase("internet explorer")
                        || browser.equalsIgnoreCase("iexplorer")
                        || browser.equalsIgnoreCase("explorer")) {
                    result = new InternetExplorerDriver();
                } else if (browser.equalsIgnoreCase("firefox")) {
                    result = new FirefoxDriver();
                } else if (browser.equalsIgnoreCase("chrome")) {
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
