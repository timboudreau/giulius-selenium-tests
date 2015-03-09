/*
 * The MIT License
 *
 * Copyright 2015 Tim Boudreau.
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
package com.mastfrog.testmain;

import com.google.common.reflect.ClassPath;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.Timer;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * A simple runner for projects which <i>are</i> a set of JUnit tests,
 * with support for showing a window with the name of the current test,
 * for use when capturing video from Selenium.
 * <p>
 * The following command-line arguments are relevant:
 * <ul>
 * <li>--tests [list of class names, comma sep] - explicitly run certain tests</li>
 * <li>--packages [list of packages, comma sep] - list of packages to scan for classes
 * </li>
 * <li>--exclude [list of packages] - packages to exclude from scanning</li>
 * </ul>
 * The default behavior with no arguments is to scan the entire classpath for
 * classes whose name ends in Test where at least one method has the &#064Test
 * annotation.  This works, but is slower than explicitly specifying classes.
 * <p>
 * A process exit code of 2 means tests failed.
 * <p>
 * All output from the test runner is prefixed by :: to make for easy filtering
 * with grep or similar.
 * <p>
 * If you are using giulius-tests or giulius-selenium-tests, any unknown command
 * line arguments will be set as system properties to ensure that &#064;Named
 * values are bound inside tests.
 *
 * @author Tim Boudreau
 */
public class TestMain {

    // Avoid inadvertently
    static String[] DEFAULT_EXCLUDED_PACKAGES = {
        "junit.framework",
        "com.sun.jna.platform.unix",
        "org.apache.xalan.xsltc.compiler",
        "org.bouncycastle.util.test",
        "org.apache.xerces.impl.xpath", "javafx.scene", "junit.extensions",
        "org.bouncycastle.util.test",
        "org.apache.xpath.axes",
        "com.mastfrog.giulius.tests",
        "org.apache.xpath.patterns",
        "org.junit", "jdk.internal.dynalink.beans",
        "org.apache.regexp", "groovy.transform"
    };
    private static boolean showWindow;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // Ensure that @Named values for within tests are set up including any command
        // line arguments passed here
        Settings settings = new SettingsBuilder().parseCommandLineArguments(args).build();
        for (String key : settings.allKeys()) {
            System.setProperty(key, settings.getString(key));
        }
        String testNamespace = System.getProperty("test.config", "tests");

        Class<?>[] tests = findTests(testNamespace, args);
        JUnitCore core = new JUnitCore();

        core.addListener(new CmdLineOut());

        Result result = core.run(tests);
        // Pending - take screen shots on failure, use some reporting engine or other
        System.out.println("::RAN: " + result.getRunCount());
        System.out.println("::FAILURES: " + result.getFailureCount());
        if (result.getFailureCount() > 0) {
            for (Failure failure : result.getFailures()) {
                System.out.println("::-> " + failure.getDescription());
            }
        }
        System.out.println("::IGNORED:" + result.getIgnoreCount());
        System.out.flush();
        if (result.getFailureCount() > 0) {
            System.exit(2);
        }
    }

    private static Class<?>[] findTests(String testNamespace, String... args) throws IOException, ClassNotFoundException {
        // Parse the command-line arguments and any system settings in /etc/tests.properties
        Settings settings = new SettingsBuilder(testNamespace)
                .addDefaultLocations()
                .parseCommandLineArguments(args).build();
        // Determine if we should show a window with the test name, for video recording
        showWindow = settings.getBoolean("test.window", true);
        // User provided individual test classes, e.g. --tests com.foo.Test1,com.foo.Test2
        String individualTests = settings.getString("test");
        if (individualTests != null) {
            // Make sure no contradictory arguments
            if (settings.getString("testPackages") != null) {
                System.err.println("Pass either --tests or --testPackages, not both");
                System.exit(3);
            }
            // Get the list of classes from the command line
            Set<Class<?>> types = new LinkedHashSet<>();
            for (String type : individualTests.split(",")) {
                type = type.trim();
                Class<?> clazz = Class.forName(type);
                if (clazz.isLocalClass()) {
                    System.err.println(clazz.getName() + " cannot be instantiated");
                    System.exit(4);
                }
                if ((Modifier.ABSTRACT & clazz.getModifiers()) != 0) {
                    System.err.println(clazz.getName() + " is abstract");
                    System.exit(5);
                }
                types.add(clazz);
            }
            System.out.println("::TESTS: " + typesToString(types));
            return types.toArray(new Class<?>[types.size()]);
        } else {
            String excludePackageNames = settings.getString("exclude");
            Set<String> excluded = new HashSet<>();
            if (excludePackageNames != null) {
                for (String pkg : excludePackageNames.split(",")) {
                    pkg = pkg.trim();
                    excluded.add(pkg);
                }
            }
            // We will scan packaages for classes whose name ends with "Test"
            String pkgs = settings.getString("packages");
            Set<String> packages = pkgs == null ? Collections.<String>emptySet() : new HashSet<String>(Arrays.asList(pkgs.split(",")));
            ClassPath pth = ClassPath.from(TestMain.class.getClassLoader());
            Set<Class<?>> types = new LinkedHashSet<>();
            NEXT_TYPE:
            for (ClassPath.ClassInfo info : pth.getAllClasses()) {
                String packageName = info.getPackageName();
                // If the user passed e.g. --testPackages com.foo.bar,com.foo.baz
                // then prune out anything that doesn't match
                if (!packages.isEmpty()) {
                    for (String pkg : packages) {
                        if (!packageName.startsWith(pkg)) {
                            continue NEXT_TYPE;
                        }
                    }
                } else {
                    for (String exc : excluded) {
                        if (packageName.startsWith(exc)) {
                            continue NEXT_TYPE;
                        }
                    }
                    // Since we're scanning the classpath, avoid picking up stuff
                    // from the JDK or libraries that happens to end with "Test"
                    for (String pkg : DEFAULT_EXCLUDED_PACKAGES) {
                        if (packageName.startsWith(pkg)) {
                            continue NEXT_TYPE;
                        }
                    }
                    // Ensure on other JDKs that obvious stuff isn't picked up
                    if (packageName.startsWith("java") || packageName.startsWith("javax") || packageName.startsWith("com.sun")) {
                        continue;
                    }
                }
                // Only include classes whose name ends in "Test"
                if (info.getName().endsWith("Test")) {
                    Class<?> type = info.load();
                    // Weed out things that cannot possibly be usable
                    if (type.isLocalClass()) {
                        continue;
                    }
                    if ((type.getModifiers() & Modifier.ABSTRACT) != 0) {
                        continue;
                    }
                    boolean foundTestAnnotation = false;
                    for (Method m : type.getMethods()) {
                        if (m.getAnnotation(Test.class) != null) {
                            foundTestAnnotation = true;
                            break;
                        }
                    }
                    if (foundTestAnnotation && type.getAnnotation(Ignore.class) == null) { // allow base classes to be ignored
                        types.add(info.load());
                    }
                }
            }
            if (types.isEmpty()) {
                System.err.println("No test types found");
                System.exit(1);
            }
            System.out.println("::TESTS: " + typesToString(types));
            return types.toArray(new Class<?>[types.size()]);
        }
    }

    static CharSequence typesToString(Iterable<Class<?>> types) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Class<?>> iter = types.iterator(); iter.hasNext();) {
            sb.append(iter.next().getName());
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }
        return sb;
    }

    static class CmdLineOut extends RunListener {
        // Continuous build simple reporting output like
        // RUN: foo
        // FAIL: foo
        // SUCCESS: bar

        private static final Pattern PAT = Pattern.compile(".*\\.(.*?\\..*?)$");

        private String testName(Description description) {
            String name = description.getClassName() + "." + description.getMethodName();
            Matcher m = PAT.matcher(name);
            if (m.find()) {
                return m.group(1);
            }
            return name;
        }

        @Override
        public void testStarted(Description description) throws Exception {
            String name = testName(description);
            showTest(name);
            System.out.println("::RUN: " + name);
        }

        @Override
        public void testFinished(Description description) throws Exception {
            System.out.println("::SUCCESS: " + testName(description));
        }

        @Override
        public void testFailure(Failure failure) throws Exception {
            System.out.println("::FAIL: " + testName(failure.getDescription()));
            failure.getException().printStackTrace(System.out);
        }
    }

    private static void showTest(String testName) {
        // Shows a window onscreen that names the test - this is needed
        // when capturing video from Selenium tests
        if (!showWindow) {
            return;
        }
        final JWindow dlg = new JWindow();
        final JLabel lbl = new JLabel(testName);
        lbl.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        lbl.setFont(new Font("Dialog", Font.BOLD, 42));
        dlg.setContentPane(lbl);
        class WL extends WindowAdapter implements ActionListener, Runnable {

            private final Timer timer = new Timer(2000, this);

            @Override
            public void actionPerformed(ActionEvent ae) {
                timer.stop();
                EventQueue.invokeLater(this);
            }

            @Override
            public void windowOpened(WindowEvent we) {
                timer.start();
            }

            @Override
            public void run() {
                dlg.dispose();
            }
        }
        dlg.addWindowListener(new WL());
        dlg.pack();
        dlg.setVisible(true);
    }
}
