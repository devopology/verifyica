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

package org.antublue.verifyica.test.store;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import org.antublue.verifyica.api.Argument;
import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.api.Verifyica;

/** Example test */
public class StoreSemaphoreTest {

    @Verifyica.ArgumentSupplier(parallelism = 10)
    public static Collection<Argument<String>> arguments() {
        Collection<Argument<String>> collection = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            collection.add(Argument.ofString("String " + i));
        }

        return collection;
    }

    @Verifyica.Test
    public void test1(ArgumentContext argumentContext) throws Throwable {
        System.out.println(format("test1(%s)", argumentContext.getTestArgument()));

        assertThat(argumentContext).isNotNull();
        assertThat(argumentContext.getStore()).isNotNull();
        assertThat(argumentContext.getTestArgument()).isNotNull();
    }

    @Verifyica.Test
    public void test2(ArgumentContext argumentContext) throws Throwable {
        Semaphore semaphore = getSemaphore(argumentContext);
        try {
            semaphore.acquire();

            System.out.println(format("test2(%s)", argumentContext.getTestArgument()));

            assertThat(argumentContext).isNotNull();
            assertThat(argumentContext.getStore()).isNotNull();
            assertThat(argumentContext.getTestArgument()).isNotNull();

            Thread.sleep(500);
        } finally {
            semaphore.release();
        }
    }

    @Verifyica.Test
    public void test3(ArgumentContext argumentContext) throws Throwable {
        System.out.println(format("test3(%s)", argumentContext.getTestArgument()));

        assertThat(argumentContext).isNotNull();
        assertThat(argumentContext.getStore()).isNotNull();
        assertThat(argumentContext.getTestArgument()).isNotNull();
    }

    /**
     * Method to get or create a class level Semaphore
     *
     * @param argumentContext argumentContext
     * @return a Semaphore
     * @throws Throwable Throwable
     */
    private Semaphore getSemaphore(ArgumentContext argumentContext) throws Throwable {
        return argumentContext
                .getClassContext()
                .getStore()
                .computeIfAbsent("semaphore", key -> new Semaphore(2), Semaphore.class);
    }
}