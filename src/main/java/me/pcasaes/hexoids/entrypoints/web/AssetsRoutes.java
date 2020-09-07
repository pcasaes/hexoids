package me.pcasaes.hexoids.entrypoints.web;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class AssetsRoutes {

    public enum ClientAssets {
        WEBASM,
        PHASOR3
    }

    private final ClientAssets clientAssets;

    @Inject
    public AssetsRoutes(
            @ConfigProperty(
                    name = "hexoids.config.client.assets",
                    defaultValue = "WEBASM"
            ) ClientAssets clientAssets) {
        this.clientAssets = clientAssets;
    }

    public void start(@Observes Router router) {
        if (this.clientAssets == ClientAssets.WEBASM) {
            router.route("/*").handler(StaticHandler
                    .create("META-INF/resources/hexoids-game-client-html5")
                    .setIndexPage("hexoids-game-client.html"));
        } else {
            router.route("/*").handler(StaticHandler
                    .create("META-INF/resources")
                    .setIndexPage("index.html"));
        }
    }
}
