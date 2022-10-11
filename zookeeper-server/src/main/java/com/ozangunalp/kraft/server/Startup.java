package com.ozangunalp.kraft.server;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class Startup {

    @Inject
    ServerConfig config;

    private EmbeddedZookeeperServer server;

    void startup(@Observes StartupEvent event) {
        server = new EmbeddedZookeeperServer()
                .withZookeeperPort(config.zookeeperPort())
                .withClusterReadyFlagFile(config.clusterReadyFlagFile());
        server.start();
    }

    void shutdown(@Observes ShutdownEvent event) {
        server.close();
    }
}