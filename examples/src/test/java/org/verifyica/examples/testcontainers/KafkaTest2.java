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

package org.verifyica.examples.testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.verifyica.examples.support.RandomSupport.randomString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;
import org.verifyica.examples.support.Logger;

@SuppressWarnings("PMD.AvoidBranchingStatementAsLastInLoop")
public class KafkaTest2 {

    private static final Logger LOGGER = Logger.createLogger(KafkaTest2.class);

    private static final String NETWORK = "network";
    private static final String MESSAGE = "message";

    private static final String TOPIC = "test";
    private static final String GROUP_ID = "test-group-id";
    private static final String EARLIEST = "earliest";

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<KafkaTestEnvironment> arguments() {
        return Stream.of(
                new KafkaTestEnvironment("apache/kafka:3.7.0"),
                new KafkaTestEnvironment("apache/kafka:3.7.1"),
                new KafkaTestEnvironment("apache/kafka:3.8.0"),
                new KafkaTestEnvironment("apache/kafka-native:3.8.0"));
    }

    @Verifyica.BeforeAll
    public void initializeTestEnvironment(ArgumentContext argumentContext) {
        LOGGER.info(
                "[%s] initialize test environment ...",
                argumentContext.getTestArgument().getName());

        Network network = Network.newNetwork();
        network.getId();

        argumentContext.getMap().put(NETWORK, network);
        argumentContext.getTestArgument().getPayload(KafkaTestEnvironment.class).initialize(network);

        assertThat(argumentContext
                        .getTestArgument()
                        .getPayload(KafkaTestEnvironment.class)
                        .isRunning())
                .isTrue();
    }

    @Verifyica.Test
    @Verifyica.Order(1)
    public void testProduce(ArgumentContext argumentContext) throws ExecutionException, InterruptedException {
        LOGGER.info(
                "[%s] testing testProduce() ...",
                argumentContext.getTestArgument().getName());

        String message = randomString(16);
        argumentContext.getMap().put(MESSAGE, message);
        LOGGER.info(
                "[%s] producing message [%s] ...",
                argumentContext.getTestArgument().getName(), message);

        try (KafkaProducer<String, String> producer = createKafkaProducer(argumentContext)) {
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(TOPIC, message);
            producer.send(producerRecord).get();
        }

        LOGGER.info(
                "[%s] message [%s] produced", argumentContext.getTestArgument().getName(), message);
    }

    @Verifyica.Test
    @Verifyica.Order(2)
    public void testConsume1(ArgumentContext argumentContext) {
        LOGGER.info(
                "[%s] testing testConsume1() ...",
                argumentContext.getTestArgument().getName());
        LOGGER.info(
                "[%s] consuming message ...", argumentContext.getTestArgument().getName());

        String expectedMessage = argumentContext.getMap().getAs(MESSAGE);
        boolean messageMatched = false;

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(argumentContext)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            for (int i = 0; i < 10; i++) {
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    LOGGER.info(
                            "[%s] consumed message [%s]",
                            argumentContext.getTestArgument().getName(), consumerRecord.value());
                    assertThat(consumerRecord.value()).isEqualTo(expectedMessage);
                    messageMatched = true;
                    break;
                }
            }
        }

        assertThat(messageMatched).isTrue();
    }

    @Verifyica.Test
    @Verifyica.Order(3)
    public void testConsume2(ArgumentContext argumentContext) {
        LOGGER.info(
                "[%s] testing testConsume2() ...",
                argumentContext.getTestArgument().getName());
        LOGGER.info(
                "[%s] consuming message ...", argumentContext.getTestArgument().getName());

        String expectedMessage = argumentContext.getMap().getAs(MESSAGE);
        boolean messageMatched = false;

        try (KafkaConsumer<String, String> consumer = createKafkaConsumer(argumentContext)) {
            consumer.subscribe(Collections.singletonList(TOPIC));

            for (int i = 0; i < 10; i++) {
                ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    LOGGER.info(
                            "[%s] consumed message [%s]",
                            argumentContext.getTestArgument().getName(), consumerRecord.value());
                    assertThat(consumerRecord.value()).isEqualTo(expectedMessage);
                    messageMatched = true;
                    break;
                }
            }
        }

        assertThat(messageMatched).isFalse();
    }

    @Verifyica.AfterAll
    public void destroyTestEnvironment(ArgumentContext argumentContext) throws Throwable {
        LOGGER.info(
                "[%s] destroy test environment ...",
                argumentContext.getTestArgument().getName());

        List<Trap> traps = new ArrayList<>();

        traps.add(new Trap(argumentContext.getTestArgument().getPayload(KafkaTestEnvironment.class)::destroy));
        traps.add(new Trap(() -> Optional.ofNullable(argumentContext.getMap().getAs(NETWORK, Network.class))
                .ifPresent(Network::close)));
        traps.add(new Trap(() -> argumentContext.getMap().clear()));

        Trap.assertEmpty(traps);
    }

    /**
     * Method to create a KafkaProducer
     *
     * @param argumentContext argumentContext
     * @return a KafkaProducer
     */
    private static KafkaProducer<String, String> createKafkaProducer(ArgumentContext argumentContext) {
        Properties properties = new Properties();

        properties.setProperty(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                argumentContext
                        .getTestArgument()
                        .getPayload(KafkaTestEnvironment.class)
                        .bootstrapServers());
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(properties);
    }

    /**
     * Method to create a KafkaConsumer
     *
     * @param argumentContext argumentContext
     * @return a KafkaConsumer
     */
    private static KafkaConsumer<String, String> createKafkaConsumer(ArgumentContext argumentContext) {
        Properties properties = new Properties();

        properties.setProperty(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                argumentContext
                        .getTestArgument()
                        .getPayload(KafkaTestEnvironment.class)
                        .bootstrapServers());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, EARLIEST);

        return new KafkaConsumer<>(properties);
    }
}
