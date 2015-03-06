package com.mastfrog.groovytests.tests.selenium;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.annotations.GenericApplicationModule;
import com.mastfrog.acteur.annotations.HttpCall;
import com.mastfrog.acteur.headers.Method;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.Path;
import com.mastfrog.acteur.util.Server;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import java.io.IOException;

/**
 * A server that serves an index page
 *
 * @author Tim Boudreau
 */
public class ServerApplication extends AbstractModule {

    private final Settings settings;

    ServerApplication(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        install(new GenericApplicationModule(settings));
        bind(Starter.class).asEagerSingleton();
    }

    static class Starter {
        @Inject
        Starter(Server server) throws IOException {
            server.start();
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        Settings s = new SettingsBuilder("x").add("port", "8123").build();
        Dependencies deps = new Dependencies(new ServerApplication(s));
        deps.getInstance(Server.class);
        Thread.sleep(120000);
    }

    @Path("/")
    @Methods(Method.GET)
    @HttpCall
    static class IndexActeur extends Acteur {
        @Inject
        IndexActeur(HttpEvent evt) {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><title>Unit Test</title></head><body><h1>Search</h1>Search for stuff<p/>");
            String q = evt.getParameter("searchText");
            sb.append("Previous search was: <span id=\"prev\">").append(q).append("</span><p/>\n");
            sb.append("<form name=\"search\" method=\"get\" action=\"/\">\n");
            sb.append("<input id=\"searchField\" type=\"text\" name=\"searchText\"></input>\n");
            sb.append("<input id=\"searchSubmit\" type=\"submit\"></input>\n");
            sb.append("</form></body></html>\n");
            ok(sb.toString());
        }
    }
}
