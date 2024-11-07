/*
 * Copyright (C) 2024-present Verifyica project authors and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.verifyica.examples.testcontainers.kafka;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.verifyica.examples.support.RandomSupport.randomString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testcontainers.containers.Network;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;
import org.verifyica.examples.support.Logger;

@SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
public class KafkaTest {

    private static final Logger LOGGER = Logger.createLogger(KafkaTest.class);

    private static final String TOPIC = "test";
    private static final String GROUP_ID = "test-group-id";
    private static final String EARLIEST = "earliest";

    private final ThreadLocal<Network> networkThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<String> messageThreadLocal = new ThreadLocal<>();

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<KafkaTestEnvironment> arguments() {
        return KafkaTestEnvironmentFactory.createTestEnvironments();
    }

    @Verifyica.BeforeAll
    public void initializeTestEnvironment(KafkaTestEnvironment kafkaTestEnvironment) {
        LOGGER.info("[%s] initialize test environment ...", kafkaTestEnvironment.name());

        Network network = Network.newNetwork();
        network.getId();

        networkThreadLocal.set(network);
        kafkaTestEnvironment.initialize(network);

        assertThat(kafkaTestEnvironment.isRunning()).isTrue();
    }

    @Verifyica.Test
    @Verifyica.Order(1)
    public void testProduce(KafkaTestEnvironment kafkaTestEnvironment) throws ExecutionException, InterruptedException {
        LOGGER.info("[%s] testing testProduce() ...", kafkaTestEnvironment.name());

        String message = randomString(16);
        LOGGER.info("[%s] producing message [%s] ...", kafkaTestEnvironment.name(), message);

        messageThreadLocal.set(message);

        try (KafkaProducer<String, String> producer = createKafkaProducer(kafkaTestEnvironment)) {
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC, message);
            producer.send(producerRecord).get();
        }

        LOGGER.info("[%s] message [%s] produced", kafkaTestEnvironment.name(), message);
    }

    @Verifyica.Test
    @Verifyica.Order(2)
    public void testConsume1(KafkaTestEnvironment kafkaTestEnvironment) {
        LOGGER.info("[%s] testing testConsume1() ...", kafkaTestEnvironment.name());
        LOGGER.info("[%s] consuming message ...", kafkaTestEnvironment.name());

        String expectedMessage = messageThreadLocal.get();
        boolean messageMatched = false;

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(kafkaTestEnvironment)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(10000));
            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                LOGGER.info("[%s] consumed message [%s]", kafkaTestEnvironment.name(), consumerRecord.value());
                assertThat(consumerRecord.value()).isEqualTo(expectedMessage);
                messageMatched = true;
            }
        }

        assertThat(messageMatched).isTrue();
    }

    @Verifyica.Test
    @Verifyica.Order(3)
    public void testConsume2(KafkaTestEnvironment kafkaTestEnvironment) {
        LOGGER.info("[%s] testing testConsume2() ...", kafkaTestEnvironment.name());
        LOGGER.info("[%s] consuming message ...", kafkaTestEnvironment.name());

        String expectedMessage = messageThreadLocal.get();
        boolean messageMatched = false;

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(kafkaTestEnvironment)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(10000));
            for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                LOGGER.info("[%s] consumed message [%s]", kafkaTestEnvironment.name(), consumerRecord.value());
                assertThat(consumerRecord.value()).isEqualTo(expectedMessage);
                messageMatched = true;
            }
        }

        assertThat(messageMatched).isFalse();
    }

    @Verifyica.AfterAll
    public void destroyTestEnvironment(KafkaTestEnvironment kafkaTestEnvironment) throws Throwable {
        LOGGER.info("[%s] destroy test environment ...", kafkaTestEnvironment.name());

        List<Trap> traps = new ArrayList<>();

        traps.add(new Trap(kafkaTestEnvironment::destroy));
        traps.add(new Trap(() -> ofNullable(networkThreadLocal.get()).ifPresent(Network::close)));
        traps.add(new Trap(messageThreadLocal::remove));
        traps.add(new Trap(networkThreadLocal::remove));

        Trap.assertEmpty(traps);
    }

    /**
     * Method to create a KafkaProducer
     *
     * @param kafkaTestEnvironment kafkaTestEnvironment
     * @return a KafkaProducer
     */
    private static KafkaProducer<String, String> createKafkaProducer(KafkaTestEnvironment kafkaTestEnvironment) {
        Properties properties = new Properties();

        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTestEnvironment.bootstrapServers());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(properties);
    }

    /**
     * Method to create a KafkaConsumer
     *
     * @param kafkaTestEnvironment kafkaTestEnvironment
     * @return a KafkaConsumer
     */
    private static KafkaConsumer<String, String> createKafkaConsumer(KafkaTestEnvironment kafkaTestEnvironment) {
        Properties properties = new Properties();

        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTestEnvironment.bootstrapServers());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, EARLIEST);

        return new KafkaConsumer<>(properties);
    }
}