/*
 * Copyright (C) 2023 The Verifyica project authors
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

package org.antublue.verifyica.engine.listener;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.antublue.verifyica.api.Configuration;
import org.antublue.verifyica.engine.common.AnsiColor;
import org.antublue.verifyica.engine.common.StopWatch;
import org.antublue.verifyica.engine.configuration.Constants;
import org.antublue.verifyica.engine.context.DefaultEngineContext;
import org.antublue.verifyica.engine.descriptor.ArgumentTestDescriptor;
import org.antublue.verifyica.engine.descriptor.ClassTestDescriptor;
import org.antublue.verifyica.engine.descriptor.TestMethodTestDescriptor;
import org.antublue.verifyica.engine.logger.Logger;
import org.antublue.verifyica.engine.logger.LoggerFactory;
import org.antublue.verifyica.engine.support.HumanReadableTimeSupport;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;

/** Class to implement StatusEngineExecutionListener */
public class StatusEngineExecutionListener implements EngineExecutionListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(StatusEngineExecutionListener.class);

    private static final String INFO =
            new AnsiColor.StringBuilder()
                    .append(AnsiColor.TEXT_WHITE)
                    .append("[")
                    .append(AnsiColor.TEXT_BLUE_BOLD)
                    .append("INFO")
                    .append(AnsiColor.TEXT_WHITE)
                    .append("]")
                    .append(AnsiColor.NONE)
                    .build();

    private final String consoleLogTimingUnits;
    private final boolean consoleLogMessagesStarted;
    private final String consoleTestMessage;
    private final boolean consoleLogMessagesSkipped;
    private final String consoleSkipMessage;
    private final boolean consoleLogMessagesFinished;
    private final String consolePassMessage;
    private final String consoleFailMessage;

    private final Map<TestDescriptor, StopWatch> stopWatchMap;

    /** Constructor */
    public StatusEngineExecutionListener() {
        Configuration configuration = DefaultEngineContext.getInstance().getConfiguration();

        consoleLogTimingUnits =
                Optional.ofNullable(configuration.get(Constants.MAVEN_PLUGIN_TIMING_UNITS))
                        .orElse("milliseconds");

        LOGGER.trace(
                "configuration property [%s] = [%s]",
                Constants.MAVEN_PLUGIN_TIMING_UNITS, consoleLogTimingUnits);

        consoleLogMessagesStarted =
                Optional.ofNullable(configuration.get(Constants.MAVEN_PLUGIN_LOG_MESSAGES_STARTED))
                        .map(Boolean::parseBoolean)
                        .orElse(true);

        LOGGER.trace(
                "configuration property [%s] = [%b]",
                Constants.MAVEN_PLUGIN_LOG_MESSAGES_STARTED, consoleLogMessagesStarted);

        consoleLogMessagesFinished =
                Optional.ofNullable(configuration.get(Constants.MAVEN_PLUGIN_LOG_MESSAGES_FINISHED))
                        .map(Boolean::parseBoolean)
                        .orElse(true);

        LOGGER.trace(
                "configuration property [%s] = [%b]",
                Constants.MAVEN_PLUGIN_LOG_MESSAGES_FINISHED, consoleLogMessagesFinished);

        consoleLogMessagesSkipped =
                Optional.ofNullable(configuration.get(Constants.MAVEN_PLUGIN_LOG_MESSAGES_SKIPPED))
                        .map(Boolean::parseBoolean)
                        .orElse(true);

        LOGGER.trace(
                "configuration property [%s] = [%b]",
                Constants.MAVEN_PLUGIN_LOG_MESSAGES_SKIPPED, consoleLogMessagesSkipped);

        consoleTestMessage =
                new AnsiColor.StringBuilder()
                        .append(AnsiColor.TEXT_WHITE_BRIGHT)
                        .append(
                                Optional.ofNullable(
                                                configuration.get(
                                                        Constants.MAVEN_PLUGIN_LOG_TEST_MESSAGE))
                                        .orElse("TEST"))
                        .append(AnsiColor.NONE)
                        .build();

        consolePassMessage =
                new AnsiColor.StringBuilder()
                        .append(AnsiColor.TEXT_GREEN_BOLD_BRIGHT)
                        .append(
                                Optional.ofNullable(
                                                configuration.get(
                                                        Constants.MAVEN_PLUGIN_LOG_PASS_MESSAGE))
                                        .orElse("PASS"))
                        .append(AnsiColor.NONE)
                        .build();

        consoleSkipMessage =
                new AnsiColor.StringBuilder()
                        .append(AnsiColor.TEXT_YELLOW_BOLD_BRIGHT)
                        .append(
                                Optional.ofNullable(
                                                configuration.get(
                                                        Constants.MAVEN_PLUGIN_LOG_SKIP_MESSAGE))
                                        .orElse("SKIP"))
                        .append(AnsiColor.NONE)
                        .build();

        consoleFailMessage =
                new AnsiColor.StringBuilder()
                        .append(AnsiColor.TEXT_RED_BOLD_BRIGHT)
                        .append(
                                Optional.ofNullable(
                                                configuration.get(
                                                        Constants.MAVEN_PLUGIN_LOG_FAIL_MESSAGE))
                                        .orElse("FAIL"))
                        .append(AnsiColor.NONE)
                        .build();

        stopWatchMap = new ConcurrentHashMap<>();
    }

    @Override
    public void executionStarted(TestDescriptor testDescriptor) {
        if (consoleLogMessagesStarted && shouldProcessDescriptor(testDescriptor)) {
            stopWatchMap.put(testDescriptor, new StopWatch());

            try {
                String testArgumentDisplayName = null;
                String testMethodDisplayName = null;
                String testClassDisplayName =
                        findClassTestDescriptor(testDescriptor).getDisplayName();

                ArgumentTestDescriptor argumentTestDescriptor =
                        findArgumentTestDescriptor(testDescriptor);
                if (argumentTestDescriptor != null) {
                    testArgumentDisplayName = argumentTestDescriptor.getTestArgument().getName();
                }

                TestMethodTestDescriptor testMethodTestDescriptor =
                        findTestMethodTestDescriptor(testDescriptor);
                if (testMethodTestDescriptor != null) {
                    testMethodDisplayName = testMethodTestDescriptor.getDisplayName() + "()";
                }

                AnsiColor.StringBuilder ansiColorStringBuilder =
                        new AnsiColor.StringBuilder()
                                .append(INFO)
                                .append(" ")
                                .append(Thread.currentThread().getName())
                                .append(" | ")
                                .append(consoleTestMessage)
                                .append(AnsiColor.NONE);

                if (testArgumentDisplayName != null) {
                    ansiColorStringBuilder.append(" | ").append(testArgumentDisplayName);
                }

                ansiColorStringBuilder.append(" | ").append(testClassDisplayName);

                if (testMethodDisplayName != null) {
                    ansiColorStringBuilder.append(" | ").append(testMethodDisplayName);
                }

                ansiColorStringBuilder.append(AnsiColor.NONE);

                System.out.println(ansiColorStringBuilder);
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void executionSkipped(TestDescriptor testDescriptor, String reason) {
        if (consoleLogMessagesSkipped && shouldProcessDescriptor(testDescriptor)) {
            Duration elapsedTime = stopWatchMap.remove(testDescriptor).stop().elapsedTime();

            try {
                String testArgumentDisplayName = null;
                String testMethodDisplayName = null;
                String testClassDisplayName =
                        findClassTestDescriptor(testDescriptor).getDisplayName();

                ArgumentTestDescriptor argumentTestDescriptor =
                        findArgumentTestDescriptor(testDescriptor);
                if (argumentTestDescriptor != null) {
                    testArgumentDisplayName = argumentTestDescriptor.getTestArgument().getName();
                }

                TestMethodTestDescriptor testMethodTestDescriptor =
                        findTestMethodTestDescriptor(testDescriptor);
                if (testMethodTestDescriptor != null) {
                    testMethodDisplayName = testMethodTestDescriptor.getDisplayName() + "()";
                }

                AnsiColor.StringBuilder ansiColorStringBuilder =
                        new AnsiColor.StringBuilder()
                                .append(INFO)
                                .append(" ")
                                .append(Thread.currentThread().getName())
                                .append(" | ")
                                .append(AnsiColor.TEXT_WHITE_BRIGHT);

                ansiColorStringBuilder.append(consoleSkipMessage).append(AnsiColor.NONE);

                if (testArgumentDisplayName != null) {
                    ansiColorStringBuilder.append(" | ").append(testArgumentDisplayName);
                }

                ansiColorStringBuilder.append(" | ").append(testClassDisplayName);

                if (testMethodDisplayName != null) {
                    ansiColorStringBuilder.append(" | ").append(testMethodDisplayName);
                }

                ansiColorStringBuilder
                        .append(" ")
                        .append(
                                HumanReadableTimeSupport.toTimingUnit(
                                        elapsedTime.toNanos(), consoleLogTimingUnits));

                ansiColorStringBuilder.append(AnsiColor.NONE);

                System.out.println(ansiColorStringBuilder);
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void executionFinished(
            TestDescriptor testDescriptor, TestExecutionResult testExecutionResult) {
        if (consoleLogMessagesFinished && shouldProcessDescriptor(testDescriptor)) {
            Duration elapsedTime = stopWatchMap.remove(testDescriptor).stop().elapsedTime();

            try {
                String testArgumentDisplayName = null;
                String testMethodDisplayName = null;
                String testClassDisplayName =
                        findClassTestDescriptor(testDescriptor).getDisplayName();

                ArgumentTestDescriptor argumentTestDescriptor =
                        findArgumentTestDescriptor(testDescriptor);
                if (argumentTestDescriptor != null) {
                    testArgumentDisplayName = argumentTestDescriptor.getTestArgument().getName();
                }

                TestMethodTestDescriptor testMethodTestDescriptor =
                        findTestMethodTestDescriptor(testDescriptor);
                if (testMethodTestDescriptor != null) {
                    testMethodDisplayName = testMethodTestDescriptor.getDisplayName() + "()";
                }

                AnsiColor.StringBuilder ansiColorStringBuilder =
                        new AnsiColor.StringBuilder()
                                .append(INFO)
                                .append(" ")
                                .append(Thread.currentThread().getName())
                                .append(" | ")
                                .append(AnsiColor.TEXT_WHITE_BRIGHT);

                TestExecutionResult.Status status = testExecutionResult.getStatus();

                switch (status) {
                    case SUCCESSFUL:
                        {
                            ansiColorStringBuilder.append(consolePassMessage);
                            break;
                        }
                    case FAILED:
                        {
                            ansiColorStringBuilder.append(consoleFailMessage);
                            break;
                        }
                    case ABORTED:
                        {
                            ansiColorStringBuilder.append(consoleSkipMessage);
                            break;
                        }
                    default:
                        {
                            ansiColorStringBuilder.append(AnsiColor.TEXT_CYAN_BOLD.wrap("????"));
                        }
                }

                ansiColorStringBuilder.append(AnsiColor.NONE);

                if (testArgumentDisplayName != null) {
                    ansiColorStringBuilder.append(" | ").append(testArgumentDisplayName);
                }

                ansiColorStringBuilder.append(" | ").append(testClassDisplayName);

                if (testMethodDisplayName != null) {
                    ansiColorStringBuilder.append(" | ").append(testMethodDisplayName);
                }

                ansiColorStringBuilder
                        .append(" ")
                        .append(
                                HumanReadableTimeSupport.toTimingUnit(
                                        elapsedTime.toNanos(), consoleLogTimingUnits))
                        .append(AnsiColor.NONE);

                System.out.println(ansiColorStringBuilder);
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }

    /**
     * Method to find the ClassTestDescriptor
     *
     * @param testDescriptor testDescriptor
     * @return the ClassTestDescriptor or null if not found
     */
    private static ClassTestDescriptor findClassTestDescriptor(TestDescriptor testDescriptor) {
        if (testDescriptor instanceof ClassTestDescriptor) {
            return (ClassTestDescriptor) testDescriptor;
        }

        if (testDescriptor instanceof ArgumentTestDescriptor) {
            return (ClassTestDescriptor) testDescriptor.getParent().orElse(null);
        }

        if (testDescriptor instanceof TestMethodTestDescriptor) {
            return (ClassTestDescriptor)
                    testDescriptor.getParent().flatMap(TestDescriptor::getParent).orElse(null);
        }

        return null;
    }

    /**
     * Method to resolve the ArgumentTestDescriptor
     *
     * @param testDescriptor testDescriptor
     * @return the ArgumentTestDescriptor or null if not found
     */
    private static ArgumentTestDescriptor findArgumentTestDescriptor(
            TestDescriptor testDescriptor) {
        if (testDescriptor instanceof ArgumentTestDescriptor) {
            return (ArgumentTestDescriptor) testDescriptor;
        }

        if (testDescriptor instanceof TestMethodTestDescriptor) {
            return (ArgumentTestDescriptor) testDescriptor.getParent().orElse(null);
        }

        return null;
    }

    /**
     * Method to find the TestMethodTestDescriptor
     *
     * @param testDescriptor testDescriptor
     * @return the TestMethodTestDescriptor or null if not found
     */
    private static TestMethodTestDescriptor findTestMethodTestDescriptor(
            TestDescriptor testDescriptor) {
        return testDescriptor instanceof TestMethodTestDescriptor
                ? (TestMethodTestDescriptor) testDescriptor
                : null;
    }

    /**
     * Method to return whether we should process the TestDescriptor
     *
     * @param testDescriptor testDescriptor
     * @return true if we should process the TestDescriptor, else false
     */
    private static boolean shouldProcessDescriptor(TestDescriptor testDescriptor) {
        return testDescriptor instanceof ClassTestDescriptor
                || testDescriptor instanceof ArgumentTestDescriptor
                || testDescriptor instanceof TestMethodTestDescriptor;
    }
}
