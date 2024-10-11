/*
 * Copyright (C) 2024-present Verifyica project authors and contributors
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

package org.verifyica.test.inheritance;

import static org.assertj.core.api.Assertions.assertThat;

import org.verifyica.api.Configuration;
import org.verifyica.api.EngineContext;
import org.verifyica.api.Verifyica;

public abstract class AbstractAutowiredTest {

    @Verifyica.Autowired
    private EngineContext engineContext;

    @Verifyica.Autowired
    private Configuration configuration;

    @Verifyica.Test
    public void test1(String argument) {
        assertThat(engineContext).isNotNull();
        assertThat(configuration).isNotNull();
        assertThat(argument).isNotNull();
    }
}
