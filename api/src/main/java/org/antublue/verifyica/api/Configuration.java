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

package org.antublue.verifyica.api;

import java.util.Map;
import java.util.Set;

/** Interface to implement Configuration */
public interface Configuration {

    /**
     * Method to get a Configuration value
     *
     * @param key key
     * @return the Configuration property value
     */
    String getProperty(String key);

    /**
     * Method to get a Configuration value
     *
     * @param key key
     * @return the Configuration value
     */
    // String get(String key);

    /**
     * Method to return if a Configuration key
     *
     * @param key key
     * @return true if the key exists, else false
     */
    // boolean containsKey(String key);

    /**
     * Method to get the Configuration key set
     *
     * @return the Configuration key set
     */
    Set<String> keySet();

    /**
     * Method to get the Configuration entry set
     *
     * @return the Configuration entry set
     */
    Set<Map.Entry<String, String>> entrySet();
}
