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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Class to implement a Key */
public class Key {

    private final List<Object> segments;

    /** Constructor */
    private Key(List<Object> objects) {
        this.segments = Collections.unmodifiableList(objects);
    }

    /**
     * Method to get an unmodifiable List of Objects that make up the Key
     *
     * @return a List of Objects
     */
    public List<Object> segments() {
        return segments;
    }

    /**
     * Method to append an Object to the Key, returning a new Key.
     *
     * <p>The Object must not be null
     *
     * @param segment segment
     * @return a new Key with the appended Object
     */
    public Key append(Object segment) {
        notNull(segment, "segment is null");

        List<Object> segments = new ArrayList<>(this.segments);
        segments.add(segment);
        return new Key(segments);
    }

    /**
     * Method to remove the last Object from the Key, returning a new Key
     *
     * @return a new Key with the last appended Object removed
     * @throws IllegalStateException if there is only one Object in the key
     */
    public Key remove() {
        if (this.segments.size() <= 1) {
            throw new IllegalStateException("can't remove root segment");
        }

        List<Object> segments = new ArrayList<>(this.segments);
        segments.remove(segments.size() - 1);
        return new Key(segments);
    }

    /**
     * Method to duplicate a Key
     *
     * @return a duplicate Key
     */
    public Key duplicate() {
        return new Key(new ArrayList<>(this.segments));
    }

    /**
     * Method to create a Key from an array of Objects
     *
     * @param objects objects
     * @return a Key
     */
    public static Key of(Object... objects) {
        notNull(objects, "objects is null");
        isTrue(objects.length > 0, "objects is empty");

        List<Object> segments = new ArrayList<>(objects.length);
        for (int i = 0; i < objects.length; i++) {
            Object object = objects[i];
            notNull(object, format("objects[%d] is null", i));
            segments.add(object);
        }

        return new Key(segments);
    }

    /**
     * Method to create a Key from a List of Objects
     *
     * @param objects objects
     * @return a Key
     */
    public static Object of(List<Object> objects) {
        notNull(objects, "objects is null");
        isTrue(!objects.isEmpty(), "objects is empty");

        List<Object> segments = new ArrayList<>(objects.size());
        for (int i = 0; i < objects.size(); i++) {
            Object object = objects.get(i);
            notNull(object, format("objects[%d] is null", i));
            segments.add(object);
        }

        return new Key(segments);
    }

    @Override
    public String toString() {
        return "Key{" + "segments=" + segments + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key key = (Key) o;
        return Objects.equals(segments, key.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(segments);
    }

    /**
     * Method to validate an Object is not null, throwing an IllegalArgumentException if it is null
     *
     * @param object object
     * @param message message
     */
    private static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Method to validate a condition is true, throwing an IllegalArgumentException if it is false
     *
     * @param condition condition
     * @param message message
     */
    private static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
