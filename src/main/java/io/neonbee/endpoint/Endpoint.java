package io.neonbee.endpoint;

import io.neonbee.config.EndpointConfig;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public interface Endpoint {
    /**
     * Get the default configuration for a given endpoint.
     * <p>
     * Note: The {@link Endpoint} class has no control over whether the returned EndpointConfig is tinkered with, thus
     * the returned {@link EndpointConfig} should either be immutable or a new instance to not run into issues when the
     * configuration is changed afterwards.
     *
     * @return the default {@link EndpointConfig} for this endpoint.
     */
    EndpointConfig getDefaultConfig();

    /**
     * Create a router for this endpoint.
     *
     * @param vertx    the Vert.x instance to use to create the router for
     * @param basePath the base path NeonBee will mount the router to, once it was created
     * @param config   any additional configuration provided in the {@link EndpointConfig}
     * @return a fully configured {@link Router} for this endpoint
     */
    Router createEndpointRouter(Vertx vertx, String basePath, JsonObject config);

    /**
     * Convenience method creating a {@link Router} for a single {@link Handler}.
     *
     * @param vertx   the Vert.x instance to create the router for
     * @param handler the handler to use in the router to handle all requests
     * @return a {@link Router} with exactly one handler registered
     */
    static Router createRouter(Vertx vertx, Handler<RoutingContext> handler) {
        Router router = Router.router(vertx);
        router.route().handler(handler);
        return router;
    }
}
