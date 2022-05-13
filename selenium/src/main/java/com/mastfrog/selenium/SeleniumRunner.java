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

import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.assertthat.selenium_shutterbug.core.Snapshot;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.giulius.DependenciesBuilder;
import com.mastfrog.giulius.tests.GuiceRunner;
import com.mastfrog.settings.Settings;
import com.mastfrog.util.preconditions.Exceptions;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.FindBy.FindByBuilder;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * A minor extension to the Mastfrog guice test framework to make it possible to
 * use WebDriver and have objects injected by it.
 * <p/>
 * Usage is very simple: Write a normal JUnit 4 test, but annotate it with
 * &#064;RunWith(SeleniumRunner.class)
 * <p/>
 * Test methods can include test fixtures as test method parameters, e.g.
 * <pre>
 *   &#064;Test
 *   public void testSomething(SearchTab tab) { ... }
 * </pre> Method arguments whose types have fields that use selenium annotations
 * will be instantiated by PageFactory; other arguments are instantiated by
 * Guice.
 * <p/>
 * The base URL can be injected using
 * <pre>&#064;Named("baseUrl") URL url;</pre>
 * <p/>
 * Settings are injected in the standard way if you are used to using Guicy. A
 * few settings are of interest:
 * <ul>
 * <li>credentials - should be : delimited username:password></li>
 * <li>_baseUrl - the base URL to use for tests</li>
 * <li>no.base.url - if the test harness should ignore the base URL (e.g. your
 * test will call WebDriver.get() itself)</li>
 * <li>host, port, path - these are URL components used if _baseUrl is not
 * specified</li>
 * <li>https - if assembling a URL from host/port/path, use HTTPS</li>
 * <li>browser - a name such as "firefox", "chrome" or "ie"</li>
 * <li>webdriver.implicitlyWaitSeconds - setting for how long WebDriver
 * waits</li>
 * <li>webdriver.maximum - boolean for whether to maximize the browser window on
 * test start</li>
 * </ul>
 *
 * Note that the test harness we are extending has the ability to run a single
 * test multiple times with different configurations. This could be used to test
 * in multiple browsers.
 * <p/>
 * Settings can be specified by the usual <code>/etc/defaults.properties</code>,
 * <code>~/defaults.properties</code> and <code>./defaults.properties</code>
 * files,
 * <i>and</i> also can be overridden in a properties file in the same Java
 * package with the same name as the test.
 *
 * @author Tim Boudreau
 */
public final class SeleniumRunner extends GuiceRunner {

    public static final String CREDENTIALS_SETTING = "credentials";
    public static final String BASE_URL_NAME = "baseUrl";
    public static final String NO_BASE_URL_SETTING = "no.base.url";

    public SeleniumRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    private static void log(CharSequence what) {
        if (Boolean.getBoolean("giulius.tests.verbose")) {
            System.err.println(what);
        }
    }

    private File screenshotDestFolder() {
        return screenshotsFolder("screenshots.dir");
    }

    private File screenshotsFolder(String key) {
        String dirName = System.getProperty(key);
        if (dirName == null) {
            dirName = "target/surefire-reports";
        }
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir = new File("surefire-reports");
            if (!dir.exists()) {
                dir = null;
            }
        }
        if (dir == null) {
            dir = new File(".");
        }
        return dir;
    }

    @Override
    public void run(RunNotifier notifier) {
        if (super.getTestClass().getJavaClass().getAnnotation(TakeScreenshotOnFailure.class) != null) {
            notifier.addFirstListener(new RunListener() {
                @Override
                public void testFailure(Failure failure) throws Exception {
                    Screenshot screenshot = new Screenshot();
                    File dir = screenshotDestFolder();
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    int ix = failure.getDescription().getClassName().lastIndexOf('.');
                    System.out.println("DESC: " + failure.getDescription().getClassName() + " method " + failure.getDescription().getMethodName());

                    String filename = "FAILED-" + failure.getDescription().getClassName().substring(ix + 1)
                            + "-" + failure.getDescription().getMethodName()
                            // + "-" + TimeUtil.toSortableStringFormat(ZonedDateTime.now())
                            + ".png";
                    File f = new File(dir, filename);
                    screenshot.save(f);
                    System.out.println("::FAILURE_SCREENSHOT:" + f.getAbsolutePath());
                    super.testFailure(failure);
                }
            });
        }
        super.run(notifier);
    }

    /**
     * Called just before the test is run, and just before we will create the
     * injector. We override this to introspect and find classes which we should
     * instantiate using PageFactory.
     *
     * @param testClass
     * @param method
     * @param settings
     * @param builder
     */
    @Override
    protected void onBeforeCreateDependencies(final TestClass testClass, final FrameworkMethod method, final Settings settings, DependenciesBuilder builder) {
        builder.add(new WebDriverModule());
        final Set<Class<?>> seen = new HashSet<Class<?>>();
        builder.add(new AbstractModule() {
            @Override
            protected void configure() {
                // Scan the test method's parameters - we inject parameters by
                // using Guice to create them - here we hijack the ones we
                // need to to instead be created by PageFactory
                Class<?>[] params = method.getMethod().getParameterTypes();
                for (Class<?> type : params) {
                    if (hasSeleniumAnnotations(type)) {
                        createWithSelenium(type);
                    }
                    // Allow one level below injected types to be handled by selenium -
                    // if an object is being injected into a field, make sure we
                    // don't need selenium to create the object
                    for (Field field : type.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (field.getAnnotation(Inject.class) != null && hasSeleniumAnnotations(field.getType())) {
                            createWithSelenium(field.getType());
                        }
                    }
                    // Scan the constructors
                    for (Constructor<?> c : type.getDeclaredConstructors()) {
                        c.setAccessible(true);
                        if (c.getAnnotation(Inject.class) != null) {
                            for (Class<?> constructorParamType : c.getParameterTypes()) {
                                if (hasSeleniumAnnotations(constructorParamType)) {
                                    createWithSelenium(constructorParamType);
                                }
                            }
                        }
                    }
                }
                // Also scan fields on the test class
                Class<?> testClassType = testClass.getJavaClass();
                for (Field f : testClassType.getDeclaredFields()) {
                    f.setAccessible(true);
                    if (hasSeleniumAnnotations(f.getType())) {
                        createWithSelenium(f.getType());
                    }
                }
                // Bind the JUnit classes - we will need them to, for instance,
                // create a logger with a name that matches the executing test
                bind(TestClass.class).toInstance(testClass);
                bind(FrameworkMethod.class).toInstance(method);
                // Find the base URL and bind it if present
                bindBaseURL();
            }

            private void bindBaseURL() {
                binder().bind(Key.get(URL.class, Names.named(BASE_URL_NAME))).toProvider(new Provider<URL>() {
                    @Override
                    public URL get() {
                        try {
                            String base = getBaseUrl();
                            log("Base URL for tests: " + base);
                            return base == null ? null : new URL(base);
                        } catch (MalformedURLException ex) {
                            return Exceptions.chuck(ex);
                        }
                    }
                });
            }

            protected final String getBaseUrl() {
                // Allow someone to just inject the webdriver
                if (settings.getBoolean(NO_BASE_URL_SETTING, false)) {
                    return null;
                }

                // Assemble the URL if it is not provided as a single property
                String url = settings.getString("_" + BASE_URL_NAME);
                if (url != null && !url.trim().isEmpty()) {
                    return url.trim();
                }

                url = settings.getBoolean("https", false) ? "https://"
                        : "http://";

                String credentials = settings.getString(CREDENTIALS_SETTING);
                if (credentials != null && !credentials.trim().isEmpty()) {
                    url += credentials.trim() + '@';
                }

                String host = settings.getString("tomcat_hostname", settings.getString("host", "localhost"));
                if (host == null) {
                    host = "localhost";
                }
                url += host;

                int port = settings.getInt("port", -1);
                if (port != -1) {
                    url += ":" + port;
                }
                String path = settings.getString("path", "/");
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                url += path;
                return url;
            }

            private <T> void createWithSelenium(final Class<T> type) {
                // Binds a Guice Provider which will delegate to Selenium
                // for this class
                if (seen.contains(type)) {
                    return;
                }
                seen.add(type);
                final Provider<WebDriver> driverProvider = binder().getProvider(WebDriver.class);
                final Provider<Dependencies> injector = binder().getProvider(Dependencies.class);
                log("Construct using Selenium's PageFactory: " + type.getName());
                class P implements Provider<T> {

                    @Override
                    public T get() {
                        log("Constructing instance of " + type.getName());
                        // Hmm, should we reverse it and let Guice instantiate it?
                        T result = PageFactory.initElements(driverProvider.get(), type);
                        // Allow Guice injection into these as well
                        injector.get().getInjector().getMembersInjector(type).injectMembers(result);
                        return result;
                    }
                }
                bind(type).toProvider(new P());
            }
        });
    }

    private static boolean hasSeleniumAnnotations(Class<?> type) {
        while (type != null && type != Object.class) {
            for (Annotation anno : type.getAnnotations()) {
                if (isSeleniumAnnotation(anno)) {
                    return true;
                }
            }
            for (Field field : type.getDeclaredFields()) {
                field.setAccessible(true);
                for (Annotation anno : field.getAnnotations()) {
                    if (isSeleniumAnnotation(anno)) {
                        return true;
                    }
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean isSeleniumAnnotation(Annotation anno) {
        Class<?> annotationType = anno.annotationType();
        String simpleName = annotationType.getSimpleName();
        String className = annotationType.getName();
        int len = simpleName.length() + 1;
        String packageName = className.substring(0, className.length() - len);
        return "org.openqa.selenium.support".equals(packageName);
    }

    @Override
    protected void onAfterCreateDependencies(TestClass testClass, FrameworkMethod method, Settings settings, Dependencies dependencies) {
        boolean screenshots = settings.getBoolean("selenium.fixture.screenshots", true);
        // Pre-run any constructors that might do things like get through a
        // login procedure
        Fixtures fixtures = testClass.getJavaClass().getAnnotation(Fixtures.class);
        if (fixtures != null) {
            for (Class<?> type : fixtures.value()) {
                createFixture(testClass, method, type, dependencies, screenshots, settings);
            }
        }
        fixtures = method.getAnnotation(Fixtures.class);
        if (fixtures != null) {
            for (Class<?> type : fixtures.value()) {
                createFixture(testClass, method, type, dependencies, screenshots, settings);
            }
        }
    }

    private <T> T createFixture(TestClass tc, FrameworkMethod method, Class<T> type, Dependencies injector, boolean screenshotsEnabled, Settings settings) {
        boolean takeScreenshot = screenshotsEnabled;
        ScreenCapture cap = null;
        if (takeScreenshot) {
            cap = type.getAnnotation(ScreenCapture.class);
        }
        T result = injector.getInstance(type);
        if (cap != null) {
            try {
                takePostFixtureCreationScreenshotAndCompare(injector, result, settings, type, cap, tc, method);
            } catch (Exception e) {
                System.err.println("Exception thrown capturing screen shot - continuing test");
                e.printStackTrace();
            }
        }
        return result;
    }

    protected <T> void takePostFixtureCreationScreenshotAndCompare(Dependencies injector, T result, Settings settings, Class<T> type, ScreenCapture cap, TestClass tc, FrameworkMethod method) throws AssertionError, IOException, InterruptedException {
        WebDriver driver = injector.getInstance(WebDriver.class);
        if (driver instanceof HtmlUnitDriver) {
            return;
        }
        boolean useTimestampedFilenames = settings.getBoolean("selenium.use.timestampd.filenames", false);
        boolean failOnImageDivergence = settings.getBoolean("selenium.fail.on.screenshot.divergence", false);
        String filename = type.getSimpleName();
        if (!cap.value().isEmpty()) {
            filename = cap.value() + "-" + filename;
        }
        filename = tc.getJavaClass().getSimpleName() + "-" + method.getName() + "-" + filename;
        System.err.println("Will take screenshot " + filename);
        String fnbase = filename;
        if (!useTimestampedFilenames) {
            filename = filename + ".png";
        }
        File fld = screenshotDestFolder();
        if (!fld.exists()) {
            System.err.println("Creating screenshots dest " + fld.getAbsolutePath());
            fld.mkdirs();
        }
        if (cap.delayMilliseconds() > 0) {
            Thread.sleep(cap.delayMilliseconds());
        }
        if (!cap.waitForVisible().id().equals("body")) {
            WebElement el = driver.findElement(new FindByBuilder().buildIt(cap.waitForVisible(), null));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5), Duration.ofMillis(10));
            try {
                wait.ignoring(TimeoutException.class).until(ExpectedConditions.visibilityOf(el));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        boolean wholePage = cap.of().id().equals("body");
        Snapshot shot;
        if (wholePage) {
            shot = Shutterbug.shootPage(driver);
        } else {
            WebElement el = driver.findElement(new FindByBuilder().buildIt(cap.of(), null));
            shot = Shutterbug.shootElement(driver, el);
        }
        String path = new File(fld, filename).getPath();
        if (useTimestampedFilenames) {
            shot.save(path);
        } else {
            ImageIO.write(shot.getImage(), "png", new File(path));
        }
        System.err.println("Saved screen shot for " + fnbase + " to " + path);
        String masterFolder = settings.getString("screenshots.master", null);
        if (masterFolder != null) {
            File gdir = new File(masterFolder);
            if (gdir.exists()) {
                File orig = new File(gdir, filename);
                if (orig.exists()) {
                    BufferedImage origImage = ImageIO.read(orig);
                    String diffPath = new File(fld, fnbase + "-diff.png").getPath();
                    boolean equal = shot.equalsWithDiff(origImage, diffPath, cap.maxDeviation());
                    if (!equal) {
                        DecimalFormat df = new DecimalFormat("#000.00");
                        double dev = cap.maxDeviation() * 100;
                        String msg = "Screen shots diverged more than " + df.format(dev) + " after " + fnbase + ". "
                                + "Diff image: " + diffPath;
                        if (failOnImageDivergence) {
                            throw new AssertionError(msg);
                        } else {
                            System.err.println(msg);
                        }
                    }
                }
            }
        }
    }
}
