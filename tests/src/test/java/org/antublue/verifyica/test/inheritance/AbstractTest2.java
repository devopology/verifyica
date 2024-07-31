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

package org.antublue.verifyica.test.inheritance;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.api.Verifyica;

/** Example test */
public abstract class AbstractTest2 {

    @Verifyica.Test
    @Verifyica.Order(order = 1)
    public final void test1(ArgumentContext argumentContext) {
        System.out.println(format("AbstractTest2 test1(%s)", argumentContext.getTestArgument()));

        assertThat(argumentContext).isNotNull();
        assertThat(argumentContext.getStore()).isNotNull();
        assertThat(argumentContext.getTestArgument()).isNotNull();
    }
}