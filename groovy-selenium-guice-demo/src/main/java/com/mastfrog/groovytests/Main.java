package com.mastfrog.groovytests;

import com.mastfrog.giulius.annotations.Defaults;
import com.mastfrog.testmain.TestMain;
import java.io.IOException;

/**
 * A simple runner for projects which <i>are</i> a set of JUnit tests, with
 * support for showing a window with the name of the current test, for use when
 * capturing video from Selenium.
 * <p>
 * The following command-line arguments are relevant:
 * <ul>
 * <li>--tests [list of class names, comma sep] - explicitly run certain
 * tests</li>
 * <li>--packages [list of packages, comma sep] - list of packages to scan for
 * classes
 * </li>
 * <li>--exclude [list of packages] - packages to exclude from scanning</li>
 * </ul>
 * The default behavior with no arguments is to scan the entire classpath for
 * classes whose name ends in Test where at least one method has the &#064Test
 * annotation. This works, but is slower than explicitly specifying classes.
 * <p>
 * A process exit code of 2 means tests failed.
 *
 * @author Tim Boudreau
 */
@Defaults({"browser=htmlunit", "_baseUrl=http://localhost:8123", "webdriver.maximize=false"})
public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        // For running the merged jar with java -jar, since the assembly plugin
        // doesn't coalesce defaults.properties files
        System.setProperty("_baseUrl", "http://localhost:8123");
        TestMain.main(args);
    }
}
