package com.ozangunalp.kraft.test.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;

public class ZookeeperNativeContainerIT {

    @Test
    void testSimpleContainer() {
        try (var container = new ZookeeperNativeContainer()) {
            container.start();
        }
    }

}