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

/** Class to implement ImmutableArgumentContext */
@SuppressWarnings("deprecated")
public class ImmutableArgumentContext implements ArgumentContext {

    private final ClassContext classContext;
    private final ArgumentContext argumentContext;

    /**
     * Constructor
     *
     * @param classContext classContext
     * @param argumentContext argumentContext
     */
    public ImmutableArgumentContext(ClassContext classContext, ArgumentContext argumentContext) {
        this.classContext = classContext;
        this.argumentContext = argumentContext;
    }

    @Override
    public ClassContext getClassContext() {
        return classContext;
    }

    @Override
    public Argument<?> getArgument() {
        return getTestArgument();
    }

    @Override
    public <T> Argument<T> getArgument(Class<T> type) {
        notNull(type, "type is null");
        return getTestArgument(type);
    }

    @Override
    public Argument<?> getTestArgument() {
        return getTestArgument(Object.class);
    }

    @Override
    public <T> Argument<T> getTestArgument(Class<T> type) {
        notNull(type, "type is null");
        return argumentContext.getArgument(type);
    }

    @Override
    public Store getStore() {
        return argumentContext.getStore();
    }

    /**
     * Checks if an Object is not null, throwing an IllegalArgumentException if the Object is null.
     *
     * @param object object
     * @param message message
     */
    private static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }
}
