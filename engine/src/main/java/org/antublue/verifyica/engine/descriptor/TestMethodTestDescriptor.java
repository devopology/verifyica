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

package org.antublue.verifyica.engine.descriptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.engine.common.Precondition;
import org.antublue.verifyica.engine.common.StateMachine;
import org.antublue.verifyica.engine.interceptor.ClassInterceptorManager;
import org.antublue.verifyica.engine.logger.Logger;
import org.antublue.verifyica.engine.logger.LoggerFactory;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

/** Class to implement TestMethodTestDescriptor */
public class TestMethodTestDescriptor extends AbstractTestDescriptor
        implements InvocableTestDescriptor {

    private final List<Method> beforeEachMethods;
    private final Method testMethod;
    private final List<Method> afterEachMethods;

    /**
     * Constructor
     *
     * @param uniqueId uniqueId
     * @param displayName displayName
     * @param beforeEachMethods beforeEachMethods
     * @param testMethod testMethod
     * @param afterEachMethods afterEachMethods
     */
    public TestMethodTestDescriptor(
            UniqueId uniqueId,
            String displayName,
            List<Method> beforeEachMethods,
            Method testMethod,
            List<Method> afterEachMethods) {
        super(uniqueId, displayName);

        Precondition.notNull(beforeEachMethods, "beforeEachMethods is null");
        Precondition.notNull(testMethod, "testMethod is null");
        Precondition.notNull(afterEachMethods, "afterEachMethods is null");

        this.beforeEachMethods = beforeEachMethods;
        this.testMethod = testMethod;
        this.afterEachMethods = afterEachMethods;
    }

    @Override
    public Type getType() {
        return Type.TEST;
    }

    @Override
    public Optional<TestSource> getSource() {
        return Optional.of(MethodSource.from(testMethod));
    }

    /**
     * Method to get List of beforeEach Methods
     *
     * @return a List of beforeEach Methods
     */
    public List<Method> getBeforeEachMethods() {
        return beforeEachMethods;
    }

    /**
     * Method to get the test Method
     *
     * @return the test Method
     */
    public Method getTestMethod() {
        return testMethod;
    }

    /**
     * Method to get a List of afterEach Methods
     *
     * @return a List of afterEach Methods
     */
    public List<Method> getAfterEachMethods() {
        return afterEachMethods;
    }

    @Override
    public TestExecutionResult testInvocation(InvocationContext invocationContext) {
        Precondition.notNull(invocationContext, "engineExecutionContext is null");

        return new Executor(invocationContext, this).test();
    }

    @Override
    public void skipInvocation(InvocationContext invocationContext) {
        Precondition.notNull(invocationContext, "engineExecutionContext is null");

        new Executor(invocationContext, this).skip();
    }

    @Override
    public String toString() {
        return "TestMethodTestDescriptor{"
                + "uniqueId="
                + getUniqueId()
                + ", displayName="
                + getDisplayName()
                + ", beforeEachMethods="
                + beforeEachMethods
                + ", testMethod="
                + testMethod
                + ", afterEachMethods="
                + afterEachMethods
                + '}';
    }

    /** Class to implement Executor */
    private static class Executor {

        private static final Logger LOGGER = LoggerFactory.getLogger(Executor.class);

        private final ArgumentContext argumentContext;
        private final TestMethodTestDescriptor testMethodTestDescriptor;
        private final List<Method> beforeEachMethods;
        private final Method testMethod;
        private final List<Method> afterEachMethods;
        private final ClassInterceptorManager classInterceptorManager;
        private final EngineExecutionListener engineExecutionListener;

        private enum State {
            START,
            BEFORE_EACH_SUCCESS,
            BEFORE_EACH_FAILURE,
            TEST_SUCCESS,
            TEST_FAILURE,
            AFTER_EACH_SUCCESS,
            AFTER_EACH_FAILURE,
            END
        }

        /**
         * Constructor
         *
         * @param invocationContext invocationContext
         * @param testMethodTestDescriptor testMethodTestDescriptor
         */
        private Executor(
                InvocationContext invocationContext,
                TestMethodTestDescriptor testMethodTestDescriptor) {
            this.argumentContext = invocationContext.get(ArgumentContext.class);
            this.testMethodTestDescriptor = testMethodTestDescriptor;
            this.beforeEachMethods = testMethodTestDescriptor.getBeforeEachMethods();
            this.testMethod = testMethodTestDescriptor.getTestMethod();
            this.afterEachMethods = testMethodTestDescriptor.getAfterEachMethods();

            this.classInterceptorManager = invocationContext.get(ClassInterceptorManager.class);
            this.engineExecutionListener = invocationContext.get(EngineExecutionListener.class);
        }

        public TestExecutionResult test() {
            LOGGER.trace("test() %s", testMethodTestDescriptor);

            engineExecutionListener.executionStarted(testMethodTestDescriptor);

            AtomicBoolean isSkipped = new AtomicBoolean();
            AtomicReference<String> skippedMessage = new AtomicReference<>();

            StateMachine<State> stateMachine =
                    new StateMachine<State>()
                            .onState(
                                    State.START,
                                    () -> {
                                        try {
                                            classInterceptorManager.beforeEach(
                                                    argumentContext, beforeEachMethods);
                                            return StateMachine.Result.of(
                                                    State.BEFORE_EACH_SUCCESS);
                                        } catch (Throwable t) {
                                            t.printStackTrace(System.err);
                                            return StateMachine.Result.of(
                                                    State.BEFORE_EACH_FAILURE, t);
                                        }
                                    })
                            .onState(
                                    State.BEFORE_EACH_SUCCESS,
                                    () -> {
                                        try {
                                            classInterceptorManager.test(
                                                    argumentContext, testMethod);
                                            return StateMachine.Result.of(State.TEST_SUCCESS);
                                        } catch (Throwable t) {
                                            t.printStackTrace(System.err);
                                            return StateMachine.Result.of(State.TEST_FAILURE, t);
                                        }
                                    })
                            .onStates(
                                    StateMachine.asList(
                                            State.BEFORE_EACH_FAILURE,
                                            State.TEST_SUCCESS,
                                            State.TEST_FAILURE),
                                    () -> {
                                        try {
                                            classInterceptorManager.afterEach(
                                                    argumentContext, afterEachMethods);
                                            return StateMachine.Result.of(State.AFTER_EACH_SUCCESS);
                                        } catch (Throwable t) {
                                            t.printStackTrace(System.err);
                                            return StateMachine.Result.of(
                                                    State.AFTER_EACH_FAILURE, t);
                                        }
                                    })
                            .onStates(
                                    StateMachine.asList(
                                            State.AFTER_EACH_SUCCESS, State.AFTER_EACH_FAILURE),
                                    () -> StateMachine.Result.of(State.END))
                            .run(State.START, State.END);

            LOGGER.trace("state machine [%s]", stateMachine);

            TestExecutionResult testExecutionResult;

            if (!isSkipped.get()) {
                testExecutionResult =
                        stateMachine
                                .getFirstResultWithThrowable()
                                .map(result -> TestExecutionResult.failed(result.getThrowable()))
                                .orElse(TestExecutionResult.successful());

                engineExecutionListener.executionFinished(
                        testMethodTestDescriptor, testExecutionResult);
            } else {
                testExecutionResult = TestExecutionResult.aborted(null);

                engineExecutionListener.executionSkipped(
                        testMethodTestDescriptor, skippedMessage.get());
            }

            return testExecutionResult;
        }

        private void skip() {
            LOGGER.trace("skip() %s", testMethodTestDescriptor);

            engineExecutionListener.executionStarted(testMethodTestDescriptor);

            engineExecutionListener.executionFinished(
                    testMethodTestDescriptor, TestExecutionResult.aborted(null));
        }
    }
}
