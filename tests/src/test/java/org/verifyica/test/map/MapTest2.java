/*
 * Copyright (C) Verifyica project authors and contributors
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

package org.verifyica.test.map;

import static org.assertj.core.api.Assertions.assertThat;

import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Verifyica;

public class MapTest2 {

    enum Key {
        MAP_TEST_3_ENGINE_CONTEXT_KEY,
        MAP_TEST_3_CLASS_CONTEXT_KEY,
        MAP_TEST_3_ARGUMENT_CONTEXT_KEY
    }

    @Verifyica.ArgumentSupplier
    public static String arguments() {
        return "test";
    }

    @Verifyica.Test
    @Verifyica.Order(0)
    public void putIntoMaps(ArgumentContext argumentContext) {
        System.out.printf("putIntoMaps(%s)%n", argumentContext.getTestArgument().getPayload());

        argumentContext
                .getClassContext()
                .getEngineContext()
                .getMap()
                .put(scopedName(Key.MAP_TEST_3_ENGINE_CONTEXT_KEY), "engine");
        argumentContext.getClassContext().getMap().put(scopedName(Key.MAP_TEST_3_CLASS_CONTEXT_KEY), "class");
        argumentContext.getMap().put(scopedName(Key.MAP_TEST_3_ARGUMENT_CONTEXT_KEY), "argument");
    }

    @Verifyica.Test
    @Verifyica.Order(1)
    public void getOutOfMaps(ArgumentContext argumentContext) {
        System.out.printf(
                "getOutOfMaps(%s)%n", argumentContext.getTestArgument().getPayload());

        assertThat(argumentContext
                        .getClassContext()
                        .getEngineContext()
                        .getMap()
                        .get(scopedName(Key.MAP_TEST_3_ENGINE_CONTEXT_KEY)))
                .isEqualTo("engine");

        assertThat(argumentContext.getClassContext().getMap().get(scopedName(Key.MAP_TEST_3_CLASS_CONTEXT_KEY)))
                .isEqualTo("class");

        assertThat(argumentContext.getMap().get(scopedName(Key.MAP_TEST_3_ARGUMENT_CONTEXT_KEY)))
                .isEqualTo("argument");
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) {
        System.out.println("conclude()");

        assertThat(classContext.getEngineContext().getMap().remove(scopedName(Key.MAP_TEST_3_ENGINE_CONTEXT_KEY)))
                .isEqualTo("engine");

        assertThat(classContext.getMap().remove(scopedName(Key.MAP_TEST_3_CLASS_CONTEXT_KEY)))
                .isEqualTo("class");
    }

    /**
     * Returns a scoped name for the key, which is the class name + "." + the enum name.
     *
     * @return the scoped name
     */
    private static String scopedName(Enum<?> e) {
        return e.getClass().getName() + "." + e.name();
    }
}
