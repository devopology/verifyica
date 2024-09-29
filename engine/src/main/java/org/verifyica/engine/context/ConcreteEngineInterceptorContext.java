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

package org.verifyica.engine.context;

import java.util.Objects;
import org.verifyica.api.EngineContext;
import org.verifyica.api.interceptor.EngineInterceptorContext;
import org.verifyica.engine.common.Precondition;

/** Class to implement ConcreteEngineInterceptorContext */
public class ConcreteEngineInterceptorContext implements EngineInterceptorContext {

    private final EngineContext engineContext;

    /**
     * Constructor
     *
     * @param engineContext engineContext
     */
    public ConcreteEngineInterceptorContext(EngineContext engineContext) {
        Precondition.notNull(engineContext, "engineContext is null");

        this.engineContext = engineContext;
    }

    @Override
    public EngineContext getEngineContext() {
        return engineContext;
    }

    @Override
    public String toString() {
        return "ConcreteEngineInterceptorContext{" + "engineContext=" + engineContext + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcreteEngineInterceptorContext that = (ConcreteEngineInterceptorContext) o;
        return Objects.equals(engineContext, that.engineContext);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(engineContext);
    }
}