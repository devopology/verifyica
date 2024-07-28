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

package org.antublue.verifyica.engine.extension.internal.engine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antublue.verifyica.api.Verifyica;
import org.antublue.verifyica.api.extension.engine.EngineExtensionContext;
import org.antublue.verifyica.engine.configuration.Constants;
import org.antublue.verifyica.engine.logger.Logger;
import org.antublue.verifyica.engine.logger.LoggerFactory;

/** Class to implement ShuffleTestClassesEngineExtension */
@Verifyica.Order(order = 1)
public class ShuffleTestClassesEngineExtension implements InternalEngineExtension {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ShuffleTestClassesEngineExtension.class);

    /** Constructor */
    public ShuffleTestClassesEngineExtension() {
        // INTENTIONALLY BLANK
    }

    @Override
    public Map<Class<?>, Set<Method>> onTestDiscovery(
            EngineExtensionContext engineExtensionContext,
            Map<Class<?>, Set<Method>> testClassMethodMap) {
        LOGGER.trace("onTestDiscovery()");

        Map<Class<?>, Set<Method>> workingTestClassMethodMap =
                new LinkedHashMap<>(testClassMethodMap);

        boolean shuffleTestClasses =
                engineExtensionContext
                        .getEngineContext()
                        .getConfiguration()
                        .getOptional(Constants.ENGINE_TEST_CLASS_SHUFFLE)
                        .map(Constants.TRUE::equals)
                        .orElse(false);

        if (shuffleTestClasses) {
            LOGGER.trace("shuffling test class order");

            workingTestClassMethodMap = shuffleMapKeys(workingTestClassMethodMap);
        }

        return workingTestClassMethodMap;
    }

    /**
     * Method to shuffle a Map's keys
     *
     * @param map map
     * @return a Map with shuffled keys
     */
    private static Map<Class<?>, Set<Method>> shuffleMapKeys(Map<Class<?>, Set<Method>> map) {
        List<Class<?>> keys = new ArrayList<>(map.keySet());

        Collections.shuffle(keys);

        Map<Class<?>, Set<Method>> shuffledMap = new LinkedHashMap<>();
        for (Class<?> key : keys) {
            shuffledMap.put(key, map.get(key));
        }

        return shuffledMap;
    }
}
