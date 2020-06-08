package me.pcasaes.hexoids.entrypoints.web;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class AssetsRoutes {

    public void start(@Observes Router router) {
        router.route("/*").handler(StaticHandler
                .create("META-INF/resources")
                .setIndexPage("index.html"));
    }
}
