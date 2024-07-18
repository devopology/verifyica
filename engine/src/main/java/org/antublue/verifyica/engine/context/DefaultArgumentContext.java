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

package org.antublue.verifyica.engine.context;

import org.antublue.verifyica.api.Argument;
import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.api.ClassContext;
import org.antublue.verifyica.api.Store;

/** Class to implement DefaultArgumentContext */
public class DefaultArgumentContext implements ArgumentContext {

    private final DefaultClassContext defaultClassContext;
    private final Store store;
    private final ImmutableArgumentContext immutableArgumentContext;

    private Object testInstance;
    private Argument<?> argument;

    /**
     * Constructor
     *
     * @param defaultClassContext classContextImpl
     */
    public DefaultArgumentContext(DefaultClassContext defaultClassContext) {
        this.defaultClassContext = defaultClassContext;
        this.store = new Store();
        this.immutableArgumentContext =
                new ImmutableArgumentContext(defaultClassContext.asImmutable(), this);
    }

    @Override
    public ClassContext getClassContext() {
        return defaultClassContext;
    }

    @Override
    public Argument<?> getArgument() {
        return argument;
    }

    @Override
    public <T> Argument<T> getArgument(Class<T> type) {
        return Argument.of(argument.getName(), type.cast(argument.getPayload()));
    }

    @Override
    public Store getStore() {
        return store;
    }

    /**
     * Method to set the test instance
     *
     * @param testInstance testInstance
     */
    public void setTestInstance(Object testInstance) {
        this.testInstance = testInstance;
    }

    /**
     * Method to get the test instance
     *
     * @return the test instance
     */
    public Object getTestInstance() {
        return testInstance;
    }

    /**
     * Method to set the argument
     *
     * @param argument argument
     */
    public void setArgument(Argument<?> argument) {
        this.argument = argument;
    }

    /**
     * Method to get an immutable version
     *
     * @return an immutable version
     */
    public ArgumentContext asImmutable() {
        return immutableArgumentContext;
    }
}