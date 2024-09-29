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

package org.verifyica.engine.resolver;

import static java.lang.String.format;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.IterationSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;
import org.verifyica.api.Argument;
import org.verifyica.api.Verifyica;
import org.verifyica.engine.api.ClassDefinition;
import org.verifyica.engine.api.MethodDefinition;
import org.verifyica.engine.common.Precondition;
import org.verifyica.engine.common.Stopwatch;
import org.verifyica.engine.descriptor.ArgumentTestDescriptor;
import org.verifyica.engine.descriptor.ClassTestDescriptor;
import org.verifyica.engine.descriptor.TestMethodTestDescriptor;
import org.verifyica.engine.exception.EngineException;
import org.verifyica.engine.exception.TestClassDefinitionException;
import org.verifyica.engine.filter.ClassDefinitionFilter;
import org.verifyica.engine.logger.Logger;
import org.verifyica.engine.logger.LoggerFactory;
import org.verifyica.engine.support.ClassSupport;
import org.verifyica.engine.support.DisplayNameSupport;
import org.verifyica.engine.support.HierarchyTraversalMode;
import org.verifyica.engine.support.OrderSupport;

/** Class to implement EngineDiscoveryRequestResolver */
public class EngineDiscoveryRequestResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(EngineDiscoveryRequestResolver.class);

    private static final List<Class<? extends DiscoverySelector>> DISCOVERY_SELECTORS_CLASSES;

    private static final Comparator<Object> CLASS_COMPARATOR = Comparator.comparing(
                    clazz -> OrderSupport.getOrder((Class<?>) clazz))
            .thenComparing(clazz -> DisplayNameSupport.getDisplayName((Class<?>) clazz));

    static {
        DISCOVERY_SELECTORS_CLASSES = new ArrayList<>();
        DISCOVERY_SELECTORS_CLASSES.add(FileSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(DirectorySelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(IterationSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(ClasspathResourceSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(ModuleSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(UriSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(ClasspathRootSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(PackageSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(ClassSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(MethodSelector.class);
        DISCOVERY_SELECTORS_CLASSES.add(UniqueIdSelector.class);
    }

    /**
     * Constructor
     */
    public EngineDiscoveryRequestResolver() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to resolve the engine discovery request, building an engine descriptor
     *
     * @param engineDiscoveryRequest engineDiscoveryRequest
     * @param testDescriptor testDescriptor
     */
    public void resolveSelectors(EngineDiscoveryRequest engineDiscoveryRequest, TestDescriptor testDescriptor) {
        LOGGER.trace("resolveSelectors()");

        Stopwatch stopwatch = new Stopwatch();

        Map<Class<?>, List<Method>> testClassMethodMap = new TreeMap<>(CLASS_COMPARATOR);
        Map<Class<?>, List<Argument<?>>> testClassArgumentMap = new TreeMap<>(CLASS_COMPARATOR);
        Map<Class<?>, Set<Integer>> testClassArgumentIndexMap = new TreeMap<>(CLASS_COMPARATOR);

        try {
            List<DiscoverySelector> discoverySelectors = new ArrayList<>();

            if (LOGGER.isTraceEnabled()) {
                for (Class<? extends DiscoverySelector> discoverySelectorClass : DISCOVERY_SELECTORS_CLASSES) {
                    discoverySelectors.addAll(engineDiscoveryRequest.getSelectorsByType(discoverySelectorClass));
                }

                discoverySelectors.forEach(discoverySelector -> LOGGER.trace(
                        "discoverySelector [%s]",
                        discoverySelector.toIdentifier().isPresent()
                                ? discoverySelector.toIdentifier().get()
                                : "null"));
            }

            new ClasspathRootSelectorResolver().resolve(engineDiscoveryRequest, testClassMethodMap);
            new PackageSelectorResolver().resolve(engineDiscoveryRequest, testClassMethodMap);
            new ClassSelectorResolver().resolve(engineDiscoveryRequest, testClassMethodMap);
            new MethodSelectorResolver().resolve(engineDiscoveryRequest, testClassMethodMap);
            new UniqueIdSelectorResolver()
                    .resolve(engineDiscoveryRequest, testClassMethodMap, testClassArgumentIndexMap);

            resolveTestArguments(testClassMethodMap, testClassArgumentMap, testClassArgumentIndexMap);

            List<ClassDefinition> classDefinitions = new ArrayList<>();

            testClassMethodMap.keySet().forEach(testClass -> {
                List<Argument<?>> testArguments = testClassArgumentMap.get(testClass);

                List<Method> testMethods = testClassMethodMap.get(testClass);

                int testArgumentParallelism = getTestArgumentParallelism(testClass);

                OrderSupport.orderMethods(testMethods);

                String testClassDisplayName = DisplayNameSupport.getDisplayName(testClass);

                List<MethodDefinition> testMethodDefinitions = new ArrayList<>();

                testMethods.forEach(testMethod -> {
                    if (testMethod.isAnnotationPresent(Verifyica.Step.class)
                            && testMethod.isAnnotationPresent(Verifyica.Order.class)) {
                        throw new TestClassDefinitionException(format(
                                "Test class [%s] test method [%s] is annotated with both \"@Verify.Step\" and \"@Verifyica.Order\"",
                                testClass.getName(), testMethod.getName()));
                    }

                    String methodDisplayName = DisplayNameSupport.getDisplayName(testMethod);
                    testMethodDefinitions.add(new ConcreteMethodDefinition(testMethod, methodDisplayName));
                });

                classDefinitions.add(new ConcreteClassDefinition(
                        testClass,
                        testClassDisplayName,
                        testMethodDefinitions,
                        testArguments,
                        testArgumentParallelism));
            });

            pruneClassDefinitions(classDefinitions);
            ClassDefinitionFilter.filter(classDefinitions);
            orderStepMethods(classDefinitions);
            buildEngineDescriptor(classDefinitions, testDescriptor);
            prunedDisabledTestMethods(testDescriptor);
        } catch (EngineException e) {
            throw e;
        } catch (Throwable t) {
            throw new EngineException(t);
        } finally {
            stopwatch.stop();
            LOGGER.trace(
                    "resolveSelectors() elapsedTime [%d] ms",
                    stopwatch.elapsedTime().toMillis());
        }
    }

    /**
     * Method to resolve test class test arguments
     *
     * @param testClassMethodMap testClassMethodMap
     * @param testClassArgumentMap testClassArgumentMap
     * @throws Throwable Throwable
     */
    private static void resolveTestArguments(
            Map<Class<?>, List<Method>> testClassMethodMap,
            Map<Class<?>, List<Argument<?>>> testClassArgumentMap,
            Map<Class<?>, Set<Integer>> argumentIndexMap)
            throws Throwable {
        LOGGER.trace("resolveTestArguments()");

        Stopwatch stopwatch = new Stopwatch();

        for (Class<?> testClass : testClassMethodMap.keySet()) {
            List<Argument<?>> testArguments = getTestArguments(testClass);
            Set<Integer> testArgumentIndices = argumentIndexMap.get(testClass);
            if (testArgumentIndices != null) {
                List<Argument<?>> specificTestArguments = new ArrayList<>();
                for (int i = 0; i < testArguments.size(); i++) {
                    if (testArgumentIndices.contains(i)) {
                        specificTestArguments.add(testArguments.get(i));
                    }
                }
                testClassArgumentMap.put(testClass, specificTestArguments);
            } else {
                testClassArgumentMap.put(testClass, testArguments);
            }
        }

        LOGGER.trace(
                "resolveTestArguments() elapsedTime [%d] ms",
                stopwatch.elapsedTime().toMillis());
    }

    /**
     * Method to get test class test arguments
     *
     * @param testClass testClass
     * @return a List of arguments
     * @throws Throwable Throwable
     */
    private static List<Argument<?>> getTestArguments(Class<?> testClass) throws Throwable {
        LOGGER.trace("getTestArguments() testClass [%s]", testClass.getName());

        Stopwatch stopwatch = new Stopwatch();

        List<Argument<?>> testArguments = new ArrayList<>();

        Object object = getArgumentSupplierMethod(testClass).invoke(null, (Object[]) null);
        if (object == null) {
            return testArguments;
        } else if (object.getClass().isArray()) {
            Object[] objects = (Object[]) object;
            if (objects.length > 0) {
                int index = 0;
                for (Object o : objects) {
                    if (o instanceof Argument<?>) {
                        testArguments.add((Argument<?>) o);
                    } else {
                        testArguments.add(Argument.of("argument[" + index + "]", o));
                    }
                    index++;
                }
            } else {
                return testArguments;
            }
        } else if (object instanceof Argument<?>) {
            testArguments.add((Argument<?>) object);
            return testArguments;
        } else if (object instanceof Stream
                || object instanceof Iterable
                || object instanceof Iterator
                || object instanceof Enumeration) {
            Iterator<?> iterator;
            if (object instanceof Enumeration) {
                iterator = Collections.list((Enumeration<?>) object).iterator();
            } else if (object instanceof Iterator) {
                iterator = (Iterator<?>) object;
            } else if (object instanceof Stream) {
                Stream<?> stream = (Stream<?>) object;
                iterator = stream.iterator();
            } else {
                Iterable<?> iterable = (Iterable<?>) object;
                iterator = iterable.iterator();
            }

            long index = 0;
            while (iterator.hasNext()) {
                Object o = iterator.next();
                if (o instanceof Argument<?>) {
                    testArguments.add((Argument<?>) o);
                } else {
                    testArguments.add(Argument.of("argument[" + index + "]", o));
                }
                index++;
            }
        } else {
            testArguments.add(Argument.of("argument[0]", object));
        }

        LOGGER.trace(
                "getTestArguments() elapsedTime [%d] ms",
                stopwatch.elapsedTime().toMillis());

        return testArguments;
    }

    /**
     * Method to get a class argument supplier method
     *
     * @param testClass testClass
     * @return the argument supplier method
     */
    private static Method getArgumentSupplierMethod(Class<?> testClass) {
        LOGGER.trace("getArgumentSupplierMethod() testClass [%s]", testClass.getName());

        List<Method> methods = ClassSupport.findMethods(
                testClass, ResolverPredicates.ARGUMENT_SUPPLIER_METHOD, HierarchyTraversalMode.BOTTOM_UP);

        validateSingleMethodPerClass(Verifyica.ArgumentSupplier.class, methods);

        return methods.get(0);
    }

    /**
     * Method to prune ClassDefinitions for test classes without arguments or test methods
     *
     * @param classDefinitions classDefinitions
     */
    private static void pruneClassDefinitions(List<ClassDefinition> classDefinitions) {
        LOGGER.trace("pruneClassDefinitions()");

        classDefinitions.removeIf(
                classDefinition -> classDefinition.getArguments().isEmpty()
                        || classDefinition.getTestMethodDefinitions().isEmpty());
    }

    /**
     * Method to prune TestMethodTestDescriptors that are disabled
     *
     * @param testDescriptor testDescriptor
     */
    private static void prunedDisabledTestMethods(TestDescriptor testDescriptor) {
        List<TestDescriptor> prunedTestDescriptors = testDescriptor.getDescendants().stream()
                .filter((Predicate<TestDescriptor>)
                        testDescriptor1 -> testDescriptor1 instanceof TestMethodTestDescriptor
                                && ((TestMethodTestDescriptor) testDescriptor1)
                                        .getTestMethod()
                                        .isAnnotationPresent(Verifyica.Disabled.class))
                .collect(Collectors.toList());

        for (TestDescriptor prunedTestDescriptor : prunedTestDescriptors) {
            prunedTestDescriptor.removeFromHierarchy();
        }
    }

    private static void orderStepMethods(List<ClassDefinition> classDefinitions) {
        LOGGER.trace("orderStepMethods()");

        StepMethodOrderer stepMethodOrderer = new StepMethodOrderer();
        classDefinitions.forEach(stepMethodOrderer::orderMethods);
    }

    /**
     * Method to build the EngineDescriptor
     *
     * @param classDefinitions classDefinitions
     * @param testDescriptor testDescriptor
     */
    private static void buildEngineDescriptor(List<ClassDefinition> classDefinitions, TestDescriptor testDescriptor) {
        LOGGER.trace("buildEngineDescriptor()");

        Stopwatch stopwatch = new Stopwatch();

        for (ClassDefinition classDefinition : classDefinitions) {
            Class<?> testClass = classDefinition.getTestClass();

            UniqueId classTestDescriptorUniqueId = testDescriptor.getUniqueId().append("class", testClass.getName());

            List<Method> prepareMethods = ClassSupport.findMethods(
                    testClass, ResolverPredicates.PREPARE_METHOD, HierarchyTraversalMode.TOP_DOWN);

            validateSingleMethodPerClass(Verifyica.Prepare.class, prepareMethods);

            List<Method> concludeMethods = ClassSupport.findMethods(
                    testClass, ResolverPredicates.CONCLUDE_METHOD, HierarchyTraversalMode.BOTTOM_UP);

            validateSingleMethodPerClass(Verifyica.Conclude.class, concludeMethods);

            ClassTestDescriptor classTestDescriptor = new ClassTestDescriptor(
                    classTestDescriptorUniqueId,
                    classDefinition.getDisplayName(),
                    testClass,
                    classDefinition.getArgumentParallelism(),
                    prepareMethods,
                    concludeMethods);

            testDescriptor.addChild(classTestDescriptor);

            int testArgumentIndex = 0;
            for (Argument<?> testArgument : classDefinition.getArguments()) {
                UniqueId argumentTestDescriptorUniqueId =
                        classTestDescriptorUniqueId.append("argument", String.valueOf(testArgumentIndex));

                List<Method> beforeAllMethods = ClassSupport.findMethods(
                        testClass, ResolverPredicates.BEFORE_ALL_METHOD, HierarchyTraversalMode.TOP_DOWN);

                validateSingleMethodPerClass(Verifyica.BeforeAll.class, beforeAllMethods);

                List<Method> afterAllMethods = ClassSupport.findMethods(
                        testClass, ResolverPredicates.AFTER_ALL_METHOD, HierarchyTraversalMode.BOTTOM_UP);

                validateSingleMethodPerClass(Verifyica.AfterAll.class, afterAllMethods);

                ArgumentTestDescriptor argumentTestDescriptor = new ArgumentTestDescriptor(
                        argumentTestDescriptorUniqueId,
                        testArgument.getName(),
                        testArgumentIndex,
                        testArgument,
                        beforeAllMethods,
                        afterAllMethods);

                classTestDescriptor.addChild(argumentTestDescriptor);

                for (MethodDefinition testMethodDefinition : classDefinition.getTestMethodDefinitions()) {
                    Method testMethod = testMethodDefinition.getMethod();

                    UniqueId testMethodDescriptorUniqueId =
                            argumentTestDescriptorUniqueId.append("method", testMethod.getName());

                    List<Method> beforeEachMethods = ClassSupport.findMethods(
                            testClass, ResolverPredicates.BEFORE_EACH_METHOD, HierarchyTraversalMode.TOP_DOWN);

                    validateSingleMethodPerClass(Verifyica.BeforeEach.class, beforeEachMethods);

                    List<Method> afterEachMethods = ClassSupport.findMethods(
                            testClass, ResolverPredicates.AFTER_EACH_METHOD, HierarchyTraversalMode.BOTTOM_UP);

                    validateSingleMethodPerClass(Verifyica.AfterEach.class, beforeEachMethods);

                    TestMethodTestDescriptor testMethodTestDescriptor = new TestMethodTestDescriptor(
                            testMethodDescriptorUniqueId,
                            testMethodDefinition.getDisplayName(),
                            beforeEachMethods,
                            testMethodDefinition.getMethod(),
                            afterEachMethods);

                    argumentTestDescriptor.addChild(testMethodTestDescriptor);
                }

                testArgumentIndex++;
            }
        }

        LOGGER.trace(
                "buildEngineDescriptor() elapsedTime [%d] ms",
                stopwatch.elapsedTime().toMillis());
    }

    /**
     * Method to validate only a single method per declared class is annotation with the given
     * annotation
     *
     * @param annotationClass annotationClass
     * @param methods methods
     */
    private static void validateSingleMethodPerClass(Class<?> annotationClass, List<Method> methods) {
        Precondition.notNull(annotationClass, "annotationClass is null");

        if (methods != null) {
            Set<Class<?>> classes = new HashSet<>();

            methods.forEach(method -> {
                if (classes.contains(method.getDeclaringClass())) {
                    String annotationDisplayName = "@Verifyica." + annotationClass.getSimpleName();
                    throw new TestClassDefinitionException(format(
                            "Test class [%s] contains more than one method" + " annotated with [%s]",
                            method.getDeclaringClass().getName(), annotationDisplayName));
                }

                classes.add(method.getDeclaringClass());
            });
        }
    }

    /**
     * Method to get test class parallelism
     *
     * @param testClass testClass
     * @return test class parallelism
     */
    private static int getTestArgumentParallelism(Class<?> testClass) {
        LOGGER.trace("getTestArgumentParallelism() testClass [%s]", testClass.getName());

        Method argumentSupplierMethod = getArgumentSupplierMethod(testClass);

        Verifyica.ArgumentSupplier annotation = argumentSupplierMethod.getAnnotation(Verifyica.ArgumentSupplier.class);

        int parallelism = Math.max(annotation.parallelism(), 1);

        LOGGER.trace("testClass [%s] parallelism [%d]", testClass.getName(), parallelism);

        return parallelism;
    }
}