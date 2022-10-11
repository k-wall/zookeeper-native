package com.ozangunalp.kraft.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.jboss.logging.Logger;

/**
 * Embedded KRaft Broker, by default listens on localhost with random broker and controller ports.
 * <p>
 */
public class EmbeddedZookeeperServer implements Closeable {

    static final Logger LOGGER = Logger.getLogger(EmbeddedZookeeperServer.class.getName());

    private int zookeeperPort = 0;
    private Optional<Path> clusterReadyFlagFile = Optional.empty();
    private ServerCnxnFactory zooFactory;
    private ZooKeeperServer zooServer;

    /**
     * Configure the port on which the broker will listen.
     *
     * @param port the port.
     * @return this {@link EmbeddedZookeeperServer}
     */
    public EmbeddedZookeeperServer withZookeeperPort(int port) {
        assertNotRunning();
        this.zookeeperPort = port;
        return this;
    }


    /**
     * Create and start the broker.
     *
     * @return this {@link EmbeddedZookeeperServer}
     */
    public synchronized EmbeddedZookeeperServer start() {
        if (isRunning()) {
            return this;
        }

        long start = System.currentTimeMillis();

        try {
            var zoo = Files.createTempDirectory("zookeeper");
            var snapshotDir = zoo.resolve("snapshot");
            var logDir = zoo.resolve("log");

            zooFactory = ServerCnxnFactory.createFactory(new InetSocketAddress("localhost", zookeeperPort), 1024);
            zooServer = new ZooKeeperServer(snapshotDir.toFile(), logDir.toFile(), 500);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        zooServer.startup();
        LOGGER.infof("Zookeeper server started in %d ms", System.currentTimeMillis() - start);

        clusterReadyFlagFile.ifPresent(path -> {
            CompletionStage<Void> awaitCluster = ZookeeperPoller.awaitZookeeperServerReady("localhost:" + zookeeperPort);
            awaitCluster.thenRunAsync(() -> {
                try {
                    path.getParent().toFile().mkdirs();
                    path.toFile().createNewFile();
                } catch (IOException e) {
                    LOGGER.warnf("Failed to create cluster ready flag file %s", path, e);
                }
            });
        });
        return this;
    }

    @Override
    public synchronized void close() {
        try {
            if (isRunning()) {
                zooServer.shutdown();
                zooFactory.shutdown();;
            }
        } catch (Exception e) {
            LOGGER.error("Error shutting down zookeeper server", e);
        } finally {
            zooServer = null;
            zooFactory = null;
        }
    }

    public boolean isRunning() {
        return zooServer != null;
    }

    private void assertNotRunning() {
        if (isRunning()) {
            throw new IllegalStateException("Configuration of the running zookeeper is not permitted.");
        }
    }

    public EmbeddedZookeeperServer withClusterReadyFlagFile(Optional<Path> clusterReadyFlagFile) {
        this.clusterReadyFlagFile = clusterReadyFlagFile;
        return this;
    }

}
