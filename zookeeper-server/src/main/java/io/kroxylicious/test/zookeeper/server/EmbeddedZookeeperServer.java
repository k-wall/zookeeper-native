package io.kroxylicious.test.zookeeper.server;

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
 * Embedded Zookeeper Server, by default listens on localhost with random port.
 * <p>
 */
public class EmbeddedZookeeperServer implements Closeable {

    static final Logger LOGGER = Logger.getLogger(EmbeddedZookeeperServer.class.getName());

    private int zookeeperPort = 0;
    private Optional<Path> zookeeperReadyFlagFile = Optional.empty();
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

            zooFactory = ServerCnxnFactory.createFactory(new InetSocketAddress(zookeeperPort), 1024);
            zooServer = new ZooKeeperServer(snapshotDir.toFile(), logDir.toFile(), 500);
            zooFactory.startup(zooServer);

            LOGGER.infof("Zookeeper server started in %d ms", System.currentTimeMillis() - start);

            zookeeperReadyFlagFile.ifPresent(path -> {
                CompletionStage<Void> awaitCluster = ZookeeperPoller.awaitZookeeperServerReady("localhost:" + zookeeperPort);
                awaitCluster.thenRunAsync(() -> {
                    try {
                        path.getParent().toFile().mkdirs();
                        path.toFile().createNewFile();
                        LOGGER.infof("Zookeeper server ready for commands", System.currentTimeMillis() - start);
                    } catch (IOException e) {
                        LOGGER.warnf("Failed to create cluster ready flag file %s", path, e);
                    }
                });
            });

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            zooServer.shutdown(true);
            return null;
        }

        return this;
    }

    @Override
    public synchronized void close() {
        try {
            LOGGER.warn("Shutting down zookeeper server");
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

    public EmbeddedZookeeperServer withZookeeperReadyFlagFile(Optional<Path> zookeeperReadyFlagFile) {
        this.zookeeperReadyFlagFile = zookeeperReadyFlagFile;
        return this;
    }

}
