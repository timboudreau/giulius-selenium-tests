package com.mastfrog.selenium;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.mastfrog.grizzly.WebServer;
import com.mastfrog.grizzly.WebServerBuilder;
import com.mastfrog.shutdown.hooks.ShutdownHookRegistry;
import com.mastfrog.util.preconditions.Exceptions;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Guice module which starts embedded jetty to serve a web page we
 * can test with selenium.
 *
 * @author Tim Boudreau
 */
class TestServletModule extends AbstractModule implements Provider<WebServer>, Runnable {
    private Provider<ShutdownHookRegistry> hooks;
    private WebServer server;

    @Override
    protected void configure() {
        hooks = binder().getProvider(ShutdownHookRegistry.class);
        bind(WebServer.class).toProvider(this).asEagerSingleton();
    }

    @Override
    public WebServer get() {
        if (server == null) {
            server = new WebServerBuilder(9223).add(new FakeSearchServlet(), "/").build();
            hooks.get().add(this);
            try {
                server.start();
                System.out.println("Started embedded web server");
                Thread.sleep(1000);
            } catch (Exception ex) {
                Exceptions.chuck(ex);
            }
        }
        return server;
    }

    @Override
    public void run() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                Logger.getLogger(TestSeleniumTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}
