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

package org.antublue.verifyica.engine.extension.internal.engine.filter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.antublue.verifyica.api.Verifyica;
import org.antublue.verifyica.api.extension.TestClassDefinition;
import org.antublue.verifyica.api.extension.engine.EngineExtensionContext;
import org.antublue.verifyica.engine.configuration.Constants;
import org.antublue.verifyica.engine.context.DefaultEngineContext;
import org.antublue.verifyica.engine.extension.internal.engine.InternalEngineExtension;
import org.antublue.verifyica.engine.logger.Logger;
import org.antublue.verifyica.engine.logger.LoggerFactory;

/** Class to implement FilterEngineExtension */
@Verifyica.Order(order = 0)
@SuppressWarnings("PMD.UnusedPrivateMethod")
public class FilterEngineExtension implements InternalEngineExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEngineExtension.class);

    private final List<Filter> filters;

    /** Constructor */
    public FilterEngineExtension() {
        filters = new ArrayList<>();
    }

    @Override
    public void onTestDiscovery(
            EngineExtensionContext engineExtensionContext,
            List<TestClassDefinition> testClassDefinitions)
            throws Throwable {
        LOGGER.trace("onTestDiscovery()");

        loadFilters();

        List<TestClassDefinition> workingTestClassDefinitions =
                new ArrayList<>(testClassDefinitions);

        testClassDefinitions.forEach(
                testClassDefinition ->
                        filters.forEach(
                                filter -> {
                                    Class<?> testClass = testClassDefinition.getTestClass();
                                    String testClassName = testClass.getName();

                                    switch (filter.getType()) {
                                        case INCLUDE_CLASS_NAME_FILTER:
                                            {
                                                if (filter.getPattern()
                                                        .matcher(testClassName)
                                                        .find()) {
                                                    workingTestClassDefinitions.add(
                                                            testClassDefinition);
                                                }
                                                break;
                                            }
                                        case EXCLUDE_CLASS_NAME_FILTER:
                                            {
                                                if (filter.getPattern()
                                                        .matcher(testClassName)
                                                        .find()) {
                                                    workingTestClassDefinitions.remove(
                                                            testClassDefinition);
                                                }
                                                break;
                                            }
                                    }
                                }));

        testClassDefinitions.clear();
        testClassDefinitions.addAll(workingTestClassDefinitions);
    }

    /**
     * Method to load filters
     *
     * @throws Throwable Throwable
     */
    private void loadFilters() throws Throwable {
        LOGGER.trace("loadFilters()");

        String filtersFilename =
                DefaultEngineContext.getInstance()
                        .getConfiguration()
                        .getOptional(Constants.ENGINE_FILTERS_FILENAME)
                        .orElse(null);

        if (filtersFilename != null && !filtersFilename.trim().isEmpty()) {
            filters.addAll(FilterParser.parse(new File(filtersFilename)));
        }
    }
}
