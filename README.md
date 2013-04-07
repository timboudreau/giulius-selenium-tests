giulius-selenium-tests
======================

An easy way to write Selenium tests in Java, have objects representing web page contents be created and injected by Guice + WebDriver, and use any JUnit reporting engine with Selenium

Builds and a Maven repository containing this project can be <a href="https://timboudreau.com/builds/">found on timboudreau.com</a>.

Overview
--------

This library makes it easy to
write Selenium tests in Java.  These tests are run using JUnit (which
makes reporting simple). 
It can utilize both [Google Guice](http://code.google.com/p/google-guice/)
and Selenium's own injection to create test fixtures.  It extends 
[Giulius-Tests](https://github.com/timboudreau/giulius-tests), a test-harness
for writing Guice-aware JUnit tests to make the framework Selenium-aware as well.

This means that
test methods can have method parameters, and the framework will pre-create
objects that you want to test.  You write test code to test what
matters, and the framework does the rest.

Here's a test which tests a fake search page.  That page contains
a form with a text field with the ID ``searchField``, a submit button with the ID
``searchSubmit`` and a span with the ID ``prev`` which contains the previously
submitted form.  This test uses a _test fixture_ - a class called 
``MyPageModel`` which contains fields representing each of those HTML
elements.

The very nice thing about this is that we never have to write code to create
any of the objects in question - the framework creates them for us and we
just use them.  

    @Test
    @Fixtures( LoginFixture.class ) // do the login steps ahead of time
    public void foo( MyPageModel page, WebDriverWait wait ) {
        page.searchField.sendKeys ( "giulius" );
        page.searchButton.click ();
        assertEquals ( "giulius", page.prev.getText() );
    }

MyPageModel is a Selenium model for the content of the page, written just
the way you normally do with Selenium tests.

The difference is that the framework sees that this class is an
argument to your test method, and it scans its fields and notices that there
are Selenium annotations on them.  So it uses Selenium's [PageFactory](http://selenium.googlecode.com/svn/trunk/docs/api/java/index.html?org/openqa/selenium/support/PageFactory.html) to instantiate the object, and then uses Guice to inject any other objects
you might have included in it (remember to use ``@Inject`` on such fields).

The result is that you never have to write code to instantiate this class
ourselves - you just mention it as a test method parameter and the rest is
handled for you.

    public class MyPageModel {
        @FindBy(how = How.ID, using = "searchField")
        @CacheLookup
        public WebElement searchField;
        @FindBy(how = How.ID, using = "searchSubmit")
        public WebElement searchButton;
        @FindBy(how = How.ID, using = "prev")
        public WebElement prev;
    }

You may have noticed the annotation:

    @Fixtures (LoginFixture.class)

That annotation is a way of saying "I don't need one of these passed to me, 
but I need you to make one before my test method runs".  The test harness
will construct an instance of ``LoginFixture``.  As a side-effect of constructing
it, it logs into the web page, so that the page is ready to do the things
we want to test.

