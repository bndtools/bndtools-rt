package org.example.tests.examples;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import aQute.bnd.annotation.component.Component;

@Component(provide = Object.class, properties = "osgi.rest.alias=/example1")
@Path("/foo")
public class SingletonServiceResource1 {

    @GET
    @Produces("text/html")
    public String getHtml() {
        return "<html><head></head><body>\n"
                + "This is an easy resource (as html text).\n"
                + "</body></html>";
    }

    @GET
    @Produces("text/plain")
    public String getPlain() {
        return "This is an easy resource (as plain text)";
    }
}