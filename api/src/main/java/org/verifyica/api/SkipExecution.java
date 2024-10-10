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

package org.verifyica.api;

import java.util.function.BooleanSupplier;

/** Class to implement SkipExecution */
public class SkipExecution extends RuntimeException {

    /**
     * Constructor
     */
    public SkipExecution() {
        super();
    }

    /**
     * Constructor
     *
     * @param message message
     */
    public SkipExecution(String message) {
        super(message);
    }

    /**
     * Throws a SkipException if true
     *
     * @param condition condition
     * @throws SkipExecution SkipExecution
     */
    public static void ifTrue(boolean condition) {
        if (condition) {
            throw new SkipExecution();
        }
    }

    /**
     * Throws a SkipException if true
     *
     * @param condition condition
     * @param message message
     * @throws SkipExecution SkipExecution
     */
    public static void ifTrue(boolean condition, String message) {
        if (condition) {
            if (message == null) {
                throw new SkipExecution();
            } else {
                throw new SkipExecution(message);
            }
        }
    }

    /**
     * Throws a SkipException if true
     *
     * @param booleanSupplier booleanSupplier
     * @throws SkipExecution SkipExecution
     */
    public static void ifTrue(BooleanSupplier booleanSupplier) throws SkipExecution {
        if (booleanSupplier == null || !booleanSupplier.getAsBoolean()) {
            throw new SkipExecution();
        }
    }

    /**
     * Throws a SkipException if true
     *
     * @param booleanSupplier booleanSupplier
     * @param message message
     * @throws SkipExecution SkipExecution
     */
    public static void ifTrue(BooleanSupplier booleanSupplier, String message) throws SkipExecution {
        if (booleanSupplier == null || !booleanSupplier.getAsBoolean()) {
            if (message == null) {
                throw new SkipExecution();
            } else {
                throw new SkipExecution(message);
            }
        }
    }

    /**
     * Throws a SkipException if false
     *
     * @param condition condition
     * @throws SkipExecution SkipExecution
     */
    public static void ifFalse(boolean condition) throws SkipExecution {
        if (!condition) {
            throw new SkipExecution();
        }
    }

    /**
     * Throws a SkipException if false
     *
     * @param condition condition
     * @param message message
     * @throws SkipExecution SkipExecution
     */
    public static void ifFalse(boolean condition, String message) throws SkipExecution {
        if (!condition) {
            if (message == null) {
                throw new SkipExecution();
            } else {
                throw new SkipExecution(message);
            }
        }
    }

    /**
     * Throws a SkipException if false
     *
     * @param booleanSupplier booleanSupplier
     * @throws SkipExecution SkipExecution
     */
    public static void ifFalse(BooleanSupplier booleanSupplier) throws SkipExecution {
        if (booleanSupplier == null || booleanSupplier.getAsBoolean()) {
            throw new SkipExecution();
        }
    }

    /**
     * Throws a SkipException if false
     *
     * @param booleanSupplier booleanSupplier
     * @param message message
     * @throws SkipExecution SkipExecution
     */
    public static void ifFalse(BooleanSupplier booleanSupplier, String message) throws SkipExecution {
        if (booleanSupplier == null || booleanSupplier.getAsBoolean()) {
            if (message == null) {
                throw new SkipExecution();
            } else {
                throw new SkipExecution(message);
            }
        }
    }
}