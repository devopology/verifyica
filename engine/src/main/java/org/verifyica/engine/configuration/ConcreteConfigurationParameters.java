/*
 * Copyright (C) 2024 The Verifyica project authors
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

package org.verifyica.engine.configuration;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.junit.platform.engine.ConfigurationParameters;
import org.verifyica.api.Configuration;
import org.verifyica.engine.common.Precondition;

/** Class to implement ConcreteConfigurationParameters */
@SuppressWarnings("deprecation")
public class ConcreteConfigurationParameters implements ConfigurationParameters {

    private final Configuration configuration;

    /**
     * Constructor
     *
     * @param configuration configuration
     */
    public ConcreteConfigurationParameters(Configuration configuration) {
        Precondition.notNull(configuration, "configuration is null");

        this.configuration = configuration;
    }

    @Override
    public Optional<String> get(String key) {
        Precondition.notBlank(key, "key is null", "key is blank");

        return Optional.ofNullable(configuration.getProperties().getProperty(key.trim()));
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
        Precondition.notBlank(key, "key is null", "key is blank");

        String value = configuration.getProperties().getProperty(key.trim());
        if ("true".equals(value)) {
            return Optional.of(Boolean.TRUE);
        } else {
            return Optional.of(Boolean.FALSE);
        }
    }

    @Override
    public <T> Optional<T> get(String key, Function<String, T> transformer) {
        Precondition.notBlank(key, "key is null", "key is blank");
        Precondition.notNull(transformer, "transformer is null");

        String value = configuration.getProperties().getProperty(key.trim());
        return value != null ? Optional.ofNullable(transformer.apply(value)) : Optional.empty();
    }

    @Override
    public int size() {
        return configuration.getProperties().size();
    }

    @Override
    public Set<String> keySet() {
        return configuration.getProperties().stringPropertyNames();
    }
}
