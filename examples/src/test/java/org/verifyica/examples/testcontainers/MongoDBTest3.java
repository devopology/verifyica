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
import static org.verifyica.examples.support.TestSupport.info;
import static org.verifyica.examples.support.TestSupport.randomString;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.bson.Document;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class MongoDBTest3 {

    private static final String NETWORK = "network";
    private static final String NAME = "name";

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<MongoDBTestEnvironment> arguments() {
        return Stream.of(
                new MongoDBTestEnvironment("mongo:4.4"),
                new MongoDBTestEnvironment("mongo:5.0"),
                new MongoDBTestEnvironment("mongo:6.0"),
                new MongoDBTestEnvironment("mongo:7.0"));
    }

    @Verifyica.BeforeAll
    public void initializeTestEnvironment(ArgumentContext argumentContext) {
        info(
                "[%s] initialize test environment ...",
                argumentContext.testArgument().getName());

        Network network = Network.newNetwork();
        network.getId();

        argumentContext.map().put(NETWORK, network);
        argumentContext.testArgument().payload(MongoDBTestEnvironment.class).initialize(network);
    }

    @Verifyica.Test
    @Verifyica.Order(1)
    public void testInsert(ArgumentContext argumentContext) {
        info("[%s] testing testInsert() ...", argumentContext.testArgument().getName());

        MongoDBTestEnvironment mongoDBTestEnvironment =
                argumentContext.testArgument().payload(MongoDBTestEnvironment.class);

        String name = randomString(16);
        argumentContext.map().put(NAME, name);

        info("[%s] name [%s]", argumentContext.testArgument().getName(), name);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(
                        mongoDBTestEnvironment.getMongoDBContainer().getConnectionString()))
                .build();

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            MongoDatabase database = mongoClient.getDatabase("test-db");
            MongoCollection<Document> collection = database.getCollection("users");
            Document document = new Document("name", name).append("age", 30);
            collection.insertOne(document);
        }

        info("[%s] name [%s] inserted", argumentContext.testArgument().getName(), name);
    }

    @Verifyica.Test
    @Verifyica.Order(2)
    public void testQuery(ArgumentContext argumentContext) {
        info("[%s] testing testQuery() ...", argumentContext.testArgument().getName());

        MongoDBTestEnvironment mongoDBTestEnvironment =
                argumentContext.testArgument().payload(MongoDBTestEnvironment.class);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(
                        mongoDBTestEnvironment.getMongoDBContainer().getConnectionString()))
                .build();

        String name = name(argumentContext);

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            MongoDatabase database = mongoClient.getDatabase("test-db");
            MongoCollection<Document> collection = database.getCollection("users");
            Document query = new Document("name", name);
            Document result = collection.find(query).first();
            assertThat(result).isNotNull();
            assertThat(result.get("name")).isEqualTo(name);
            assertThat(result.get("age")).isEqualTo(30);
        }
    }

    @Verifyica.AfterAll
    public void destroyTestEnvironment(ArgumentContext argumentContext) throws Throwable {
        info("[%s] destroy test environment ...", argumentContext.testArgument().getName());

        List<Trap> traps = new ArrayList<>();

        traps.add(new Trap(() -> argumentContext
                .testArgument()
                .payload(MongoDBTestEnvironment.class)
                .destroy()));
        traps.add(new Trap(() -> Optional.ofNullable(argumentContext.map().removeAs(NETWORK, Network.class))
                .ifPresent(Network::close)));
        traps.add(new Trap(() -> argumentContext.map().clear()));

        Trap.assertEmpty(traps);
    }

    /**
     * Helper method to get the name from the ArgumentContext
     *
     * @param argumentContext argumentContext
     * @return the name
     */
    private static String name(ArgumentContext argumentContext) {
        return (String) argumentContext.map().get(NAME);
    }
}
