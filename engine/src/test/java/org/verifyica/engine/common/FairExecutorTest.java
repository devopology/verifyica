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

package org.verifyica.engine.common;

import io.github.thunkware.vt.bridge.ThreadNameRunnable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.verifyica.engine.support.ExecutorServiceSupport;

public class FairExecutorTest {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(-1);
    private static final ExecutorService EXECUTOR_SERVICE = new FairExecutorService(1);
    private static final List<Future<?>> FUTURES = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        Thread[] threads = new Thread[10];

        try {
            for (int i = 0; i < 10; i++) {
                String threadName = "submitThread-" + i;
                Thread thread = new Thread(() -> {
                    for (int j = 0; j < 5; j++) {
                        FUTURES.add(EXECUTOR_SERVICE.submit(new ThreadNameRunnable(threadName, new NamedRunnable())));
                    }
                });
                thread.setName("thread-" + ATOMIC_INTEGER.incrementAndGet());
                thread.setDaemon(true);
                threads[i] = thread;
            }

            List<Thread> threadCollection = new ArrayList<>(Arrays.asList(threads));
            Collections.shuffle(threadCollection);

            for (Thread thread : threadCollection) {
                thread.start();
            }
        } finally {
            for (Thread thread : threads) {
                thread.join();
            }
            ExecutorServiceSupport.waitForAllFutures(FUTURES, EXECUTOR_SERVICE);
        }
    }

    private static class NamedRunnable implements Runnable {

        public NamedRunnable() {
            // INTENTIONALLY BLANK
        }

        public void run() {
            System.out.printf("name [%s]%n", Thread.currentThread().getName());
        }
    }
}
