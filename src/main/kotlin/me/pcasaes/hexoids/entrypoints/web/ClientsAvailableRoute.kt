package me.pcasaes.hexoids.entrypoints.web

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.quarkus.vertx.web.Route
import io.quarkus.vertx.web.RouteBase
import io.vertx.ext.web.RoutingContext
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
@RouteBase(path = "clients", produces = ["application/json"])
class ClientsAvailableRoute {
    @Route(path = "available", methods = [Route.HttpMethod.GET])
    fun available(rc: RoutingContext) {
        rc.response()
            .putHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            .putHeader("Pragma", "no-cache")
            .putHeader("Expires", "0")
            .end(AVAILABLE)
    }

    companion object {
        private val AVAILABLE: String

        init {
            val versions = HashMap<String, String>()

            versions.put("OSX", "0.8.0")
            versions.put("Windows", "0.8.0")
            versions.put("X11", "0.8.0")
            versions.put("HTML5", "0.8.0")

            try {
                AVAILABLE = ObjectMapper().registerKotlinModule().writeValueAsString(versions)
            } catch (ex: JsonProcessingException) {
                throw IllegalStateException(ex)
            }
        }
    }
}
