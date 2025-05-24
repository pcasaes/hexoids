package me.pcasaes.hexoids.entrypoints.web

import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class AssetsRoutes @Inject constructor(
    @param:ConfigProperty(
        name = "hexoids.client.assets",
        defaultValue = "WEBASM"
    ) private val clientAssets: ClientAssets
) {
    enum class ClientAssets {
        WEBASM,
        PHASOR3
    }

    fun start(@Observes router: Router) {
        when (this.clientAssets) {
            ClientAssets.WEBASM -> {
                router.route("/*").handler(
                    StaticHandler
                        .create("META-INF/resources/hexoids-game-client-html5")
                        .setIndexPage("hexoids-game-client.html")
                )
            }

            ClientAssets.PHASOR3 -> {
                router.route("/*").handler(
                    StaticHandler
                        .create("META-INF/resources")
                        .setIndexPage("index.html")
                )
            }
        }
    }
}
