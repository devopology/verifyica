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

package org.antublue.verifyica.engine.interceptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.api.ClassContext;
import org.antublue.verifyica.api.EngineContext;
import org.antublue.verifyica.api.interceptor.ArgumentInterceptorContext;
import org.antublue.verifyica.api.interceptor.ClassInterceptor;
import org.antublue.verifyica.engine.context.DefaultArgumentInterceptorContext;
import org.antublue.verifyica.engine.context.DefaultClassInterceptorContext;
import org.antublue.verifyica.engine.context.DefaultEngineInterceptorContext;
import org.antublue.verifyica.engine.context.ImmutableArgumentContext;
import org.antublue.verifyica.engine.discovery.Predicates;
import org.antublue.verifyica.engine.exception.EngineException;
import org.antublue.verifyica.engine.interceptor.internal.ClearClassContextStoreClassStoreInterceptor;
import org.antublue.verifyica.engine.logger.Logger;
import org.antublue.verifyica.engine.logger.LoggerFactory;
import org.antublue.verifyica.engine.support.ArgumentSupport;
import org.antublue.verifyica.engine.support.ClassPathSupport;
import org.antublue.verifyica.engine.support.ObjectSupport;
import org.antublue.verifyica.engine.support.OrderSupport;

/** Class to implement ClassInterceptorRegistry */
@SuppressWarnings("PMD.EmptyCatchBlock")
public class ClassInterceptorRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassInterceptorRegistry.class);

    private final ReadWriteLock readWriteLock;
    private final List<ClassInterceptor> classInterceptors;
    private final Map<Class<?>, List<ClassInterceptor>> mappedClassInterceptors;
    private boolean initialized;

    /** Constructor */
    private ClassInterceptorRegistry() {
        readWriteLock = new ReentrantReadWriteLock(true);
        classInterceptors = new ArrayList<>();
        mappedClassInterceptors = new LinkedHashMap<>();

        classInterceptors.add(new ClearClassContextStoreClassStoreInterceptor());

        loadClassInterceptors();
    }

    /**
     * Method to register a class interceptor
     *
     * @param testClass testClass
     * @param classInterceptor classInterceptors
     * @return this ClassInterceptorRegistry
     */
    public ClassInterceptorRegistry register(
            Class<?> testClass, ClassInterceptor classInterceptor) {
        ArgumentSupport.notNull(testClass, "testClass is null");
        ArgumentSupport.notNull(classInterceptor, "classInterceptor is null");

        try {
            getReadWriteLock().writeLock().lock();
            mappedClassInterceptors
                    .computeIfAbsent(testClass, c -> new ArrayList<>())
                    .add(classInterceptor);
        } finally {
            getReadWriteLock().writeLock().unlock();
        }

        return this;
    }

    /**
     * Method to remove a class interceptor
     *
     * @param testClass testClass
     * @param classInterceptor classInterceptor
     * @return this ClassInterceptorRegistry
     */
    public ClassInterceptorRegistry remove(Class<?> testClass, ClassInterceptor classInterceptor) {
        ArgumentSupport.notNull(testClass, "testClass is null");
        ArgumentSupport.notNull(classInterceptor, "classInterceptor is null");

        try {
            getReadWriteLock().writeLock().lock();
            mappedClassInterceptors.get(testClass).remove(classInterceptor);
        } finally {
            getReadWriteLock().writeLock().unlock();
        }

        return this;
    }

    /**
     * Method to get the number of class interceptors
     *
     * @param testClass testClass
     * @return the number of class interceptors
     */
    public int size(Class<?> testClass) {
        ArgumentSupport.notNull(testClass, "testClass is null");

        try {
            getReadWriteLock().readLock().lock();
            List<ClassInterceptor> classInterceptors = mappedClassInterceptors.get(testClass);
            return classInterceptors != null ? classInterceptors.size() : 0;
        } finally {
            getReadWriteLock().readLock().unlock();
        }
    }

    /**
     * Method to remove all class interceptors
     *
     * @param testClass testClass
     * @return this ClassInterceptorRegistry
     */
    public ClassInterceptorRegistry clear(Class<?> testClass) {
        ArgumentSupport.notNull(testClass, "testClass is null");

        try {
            getReadWriteLock().writeLock().lock();
            mappedClassInterceptors.remove(testClass);
        } finally {
            getReadWriteLock().writeLock().unlock();
        }

        return this;
    }

    /**
     * Method to execute class interceptors
     *
     * @param engineContext engineContext
     * @param testClass testClass
     * @throws Throwable Throwable
     */
    public void beforeInstantiate(EngineContext engineContext, Class<?> testClass)
            throws Throwable {
        DefaultEngineInterceptorContext defaultEngineInterceptorContext =
                new DefaultEngineInterceptorContext(engineContext);

        for (ClassInterceptor classInterceptor : getClassInterceptors(testClass)) {
            classInterceptor.preInstantiate(defaultEngineInterceptorContext, testClass);
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param engineContext engineContext
     * @param testClass testClass
     * @param testInstance testInstance
     * @param throwable throwable
     * @throws Throwable Throwable
     */
    public void afterInstantiate(
            EngineContext engineContext,
            Class<?> testClass,
            Object testInstance,
            Throwable throwable)
            throws Throwable {
        DefaultEngineInterceptorContext defaultEngineInterceptorContext =
                new DefaultEngineInterceptorContext(engineContext);

        for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
            classInterceptor.postInstantiate(
                    defaultEngineInterceptorContext, testClass, testInstance, throwable);
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param classContext classContext
     * @param prepareMethods prepareMethods
     * @throws Throwable Throwable
     */
    public void prepare(ClassContext classContext, List<Method> prepareMethods) throws Throwable {
        Class<?> testClass = classContext.getTestClass();
        DefaultClassInterceptorContext defaultClassInterceptorContext =
                new DefaultClassInterceptorContext(classContext);

        List<Throwable> throwables = new ArrayList<>();

        try {
            for (ClassInterceptor classInterceptor : getClassInterceptors(testClass)) {
                classInterceptor.prePrepare(defaultClassInterceptorContext);
            }
            try {
                for (Method prepareMethod : prepareMethods) {
                    prepareMethod.invoke(null, classContext);
                }
            } catch (InvocationTargetException e) {
                throwables.add(e.getCause());
            }
        } catch (Throwable t) {
            throwables.add(t);
        } finally {
            for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
                classInterceptor.postPrepare(
                        defaultClassInterceptorContext,
                        !throwables.isEmpty() ? throwables.get(0) : null);
            }
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param argumentContext argumentContext
     * @param beforeAllMethods beforeAllMethods
     * @throws Throwable Throwable
     */
    public void beforeAll(ArgumentContext argumentContext, List<Method> beforeAllMethods)
            throws Throwable {
        ClassContext classContext = argumentContext.getClassContext();
        Class<?> testClass = classContext.getTestClass();
        Object testInstance = classContext.getTestInstance();
        DefaultArgumentInterceptorContext defaultArgumentInterceptorContext =
                new DefaultArgumentInterceptorContext(argumentContext);
        List<Throwable> throwables = new ArrayList<>();

        try {
            for (ClassInterceptor classInterceptor : getClassInterceptors(testClass)) {
                classInterceptor.preBeforeAll(defaultArgumentInterceptorContext);
            }
            try {
                for (Method beforeAllMethod : beforeAllMethods) {
                    beforeAllMethod.invoke(testInstance, argumentContext);
                }
            } catch (InvocationTargetException e) {
                throwables.add(e.getCause());
            }
        } catch (Throwable t) {
            throwables.add(t);
        } finally {
            for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
                classInterceptor.postBeforeAll(
                        defaultArgumentInterceptorContext,
                        !throwables.isEmpty() ? throwables.get(0) : null);
            }
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param argumentContext argumentContext
     * @param beforeEachMethods beforeEachMethods
     * @throws Throwable Throwable
     */
    public void beforeEach(ArgumentContext argumentContext, List<Method> beforeEachMethods)
            throws Throwable {
        ClassContext classContext = argumentContext.getClassContext();
        Class<?> testClass = classContext.getTestClass();
        Object testInstance = classContext.getTestInstance();
        DefaultArgumentInterceptorContext defaultArgumentInterceptorContext =
                new DefaultArgumentInterceptorContext(argumentContext);
        List<Throwable> throwables = new ArrayList<>();

        try {
            for (ClassInterceptor classInterceptor : getClassInterceptors(testClass)) {
                classInterceptor.preBeforeEach(defaultArgumentInterceptorContext);
            }
            try {
                for (Method beforEachMethod : beforeEachMethods) {
                    beforEachMethod.invoke(testInstance, argumentContext);
                }
            } catch (InvocationTargetException e) {
                throwables.add(e.getCause());
            }
        } catch (Throwable t) {
            throwables.add(t);
        } finally {
            for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
                classInterceptor.postBeforeEach(
                        defaultArgumentInterceptorContext,
                        !throwables.isEmpty() ? throwables.get(0) : null);
            }
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param argumentContext argumentContext
     * @param testMethod testMethod
     * @throws Throwable Throwable
     */
    public void test(ArgumentContext argumentContext, Method testMethod) throws Throwable {
        ClassContext classContext = argumentContext.getClassContext();
        Class<?> testClass = classContext.getTestClass();
        Object testInstance = classContext.getTestInstance();
        ArgumentInterceptorContext argumentInterceptorContext =
                new DefaultArgumentInterceptorContext(argumentContext);
        List<Throwable> throwables = new ArrayList<>();

        try {
            for (ClassInterceptor classInterceptor : getClassInterceptors(testClass)) {
                classInterceptor.preTest(argumentInterceptorContext, testMethod);
            }
            try {
                testMethod.invoke(testInstance, ImmutableArgumentContext.wrap(argumentContext));
            } catch (InvocationTargetException e) {
                throwables.add(e.getCause());
            }
        } catch (Throwable t) {
            throwables.add(t);
        } finally {
            for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
                classInterceptor.postTest(
                        argumentInterceptorContext,
                        testMethod,
                        !throwables.isEmpty() ? throwables.get(0) : null);
            }
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param argumentContext argumentContext
     * @param afterEachMethods afterEachMethods
     * @throws Throwable Throwable
     */
    public void afterEach(ArgumentContext argumentContext, List<Method> afterEachMethods)
            throws Throwable {
        ClassContext classContext = argumentContext.getClassContext();
        Class<?> testClass = classContext.getTestClass();
        Object testInstance = classContext.getTestInstance();
        DefaultArgumentInterceptorContext defaultArgumentInterceptorContext =
                new DefaultArgumentInterceptorContext(argumentContext);
        List<Throwable> throwables = new ArrayList<>();

        try {
            for (ClassInterceptor classInterceptor : getClassInterceptors(testClass)) {
                classInterceptor.preAfterEach(defaultArgumentInterceptorContext);
            }
            try {
                for (Method afterEachMethod : afterEachMethods) {
                    afterEachMethod.invoke(testInstance, argumentContext);
                }
            } catch (InvocationTargetException e) {
                throwables.add(e.getCause());
            }
        } catch (Throwable t) {
            throwables.add(t);
        } finally {
            for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
                classInterceptor.postAfterEach(
                        defaultArgumentInterceptorContext,
                        !throwables.isEmpty() ? throwables.get(0) : null);
            }
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param argumentContext argumentContext
     * @param afterAllMethods afterAllMethods
     * @throws Throwable Throwable
     */
    public void afterAll(ArgumentContext argumentContext, List<Method> afterAllMethods)
            throws Throwable {
        ClassContext classContext = argumentContext.getClassContext();
        Class<?> testClass = classContext.getTestClass();
        Object testInstance = classContext.getTestInstance();
        DefaultArgumentInterceptorContext defaultArgumentInterceptorContext =
                new DefaultArgumentInterceptorContext(argumentContext);
        List<Throwable> throwables = new ArrayList<>();

        try {
            for (ClassInterceptor classInterceptor : getClassInterceptors(testClass)) {
                classInterceptor.preAfterAll(defaultArgumentInterceptorContext);
            }
            try {
                for (Method afterAllMethod : afterAllMethods) {
                    afterAllMethod.invoke(testInstance, argumentContext);
                }
            } catch (InvocationTargetException e) {
                throwables.add(e.getCause());
            }
        } catch (Throwable t) {
            throwables.add(t);
        } finally {
            for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
                classInterceptor.postAfterAll(
                        defaultArgumentInterceptorContext,
                        !throwables.isEmpty() ? throwables.get(0) : null);
            }
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param classContext classContext
     * @param concludeMethods concludeMethods
     * @throws Throwable Throwable
     */
    public void conclude(ClassContext classContext, List<Method> concludeMethods) throws Throwable {
        Class<?> testClass = classContext.getTestClass();
        DefaultClassInterceptorContext defaultClassInterceptorContext =
                new DefaultClassInterceptorContext(classContext);

        List<Throwable> throwables = new ArrayList<>();

        try {
            for (ClassInterceptor classInterceptor : getClassInterceptors(testClass)) {
                classInterceptor.preConclude(defaultClassInterceptorContext);
            }
            try {
                for (Method concludeMethod : concludeMethods) {
                    concludeMethod.invoke(null, classContext);
                }
            } catch (InvocationTargetException e) {
                throwables.add(e.getCause());
            }
        } catch (Throwable t) {
            throwables.add(t);
        } finally {
            for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
                classInterceptor.postConclude(
                        defaultClassInterceptorContext,
                        !throwables.isEmpty() ? throwables.get(0) : null);
            }
        }
    }

    /**
     * Method to execute class interceptors
     *
     * @param classContext classContext
     * @throws Throwable Throwable
     */
    public void onDestroy(ClassContext classContext) throws Throwable {
        Class<?> testClass = classContext.getTestClass();
        DefaultClassInterceptorContext defaultClassInterceptorContext =
                new DefaultClassInterceptorContext(classContext);
        for (ClassInterceptor classInterceptor : getClassInterceptorsReversed(testClass)) {
            classInterceptor.onDestroy(defaultClassInterceptorContext);
        }
    }

    /**
     * Method to get a COPY of the List of ClassInterceptors (internal + class specific)
     *
     * @param testClass testClass
     * @return a COPY of the List of ClassInterceptors (internal + class specific)
     */
    private List<ClassInterceptor> getClassInterceptors(Class<?> testClass) {
        try {
            getReadWriteLock().writeLock().lock();
            List<ClassInterceptor> classInterceptors = new ArrayList<>(this.classInterceptors);
            classInterceptors.addAll(
                    mappedClassInterceptors.computeIfAbsent(testClass, o -> new ArrayList<>()));
            return classInterceptors;
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Method to get a COPY of the List of ClassInterceptors in reverse (internal + class specific)
     *
     * @param testClass testClass
     * @return a COPY of the List of ClassInterceptors in reverse (internal + class specific)
     */
    private List<ClassInterceptor> getClassInterceptorsReversed(Class<?> testClass) {
        List<ClassInterceptor> classInterceptors = getClassInterceptors(testClass);
        Collections.reverse(classInterceptors);
        return classInterceptors;
    }

    /**
     * Method to get the ReadWriteLock
     *
     * @return the ReadWriteLock
     */
    private ReadWriteLock getReadWriteLock() {
        return readWriteLock;
    }

    /** Method to load class interceptors */
    private void loadClassInterceptors() {
        try {
            getReadWriteLock().writeLock().lock();

            if (!initialized) {
                LOGGER.trace("loadClassInterceptors()");

                List<Class<?>> autoLoadClassInterceptors =
                        new ArrayList<>(
                                ClassPathSupport.findClasses(
                                        Predicates.AUTO_LOAD_CLASS_INTERCEPTOR_CLASS));

                OrderSupport.orderClasses(autoLoadClassInterceptors);

                LOGGER.trace("class interceptor count [%d]", autoLoadClassInterceptors.size());

                for (Class<?> classInterceptorClass : autoLoadClassInterceptors) {
                    try {
                        LOGGER.trace(
                                "loading class interceptor [%s]", classInterceptorClass.getName());

                        Object object = ObjectSupport.createObject(classInterceptorClass);
                        classInterceptors.add((ClassInterceptor) object);

                        LOGGER.trace(
                                "class interceptor [%s] loaded", classInterceptorClass.getName());
                    } catch (EngineException e) {
                        throw e;
                    } catch (Throwable t) {
                        throw new EngineException(t);
                    }
                }

                initialized = true;
            }
        } finally {
            getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Method to get a singleton instance
     *
     * @return the singleton instance
     */
    public static ClassInterceptorRegistry getInstance() {
        return SingletonHolder.SINGLETON;
    }

    /** Class to hold the singleton instance */
    private static class SingletonHolder {

        /** The singleton instance */
        private static final ClassInterceptorRegistry SINGLETON = new ClassInterceptorRegistry();
    }
}
