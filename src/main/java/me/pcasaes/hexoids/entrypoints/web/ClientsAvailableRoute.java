package me.pcasaes.hexoids.entrypoints.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.ext.web.RoutingContext;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@RouteBase(path = "clients", produces = "application/json")
public class ClientsAvailableRoute {

    private static final String AVAILABLE;

    static {
        Map<String, String> versions = new HashMap<>();

        versions.put("OSX", "0.5.0");
        versions.put("Windows", "0.5.0");
        versions.put("X11", "0.5.0");
        versions.put("HTML5", "0.5.0");

        try {
            AVAILABLE = new ObjectMapper().writeValueAsString(versions);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Route(path = "available", methods = Route.HttpMethod.GET)
    public void available(RoutingContext rc) {
        rc.response()
                .putHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .putHeader("Pragma", "no-cache")
                .putHeader("Expires", "0")
                .end(AVAILABLE);
    }
}
