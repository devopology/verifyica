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

package org.antublue.verifyica.api.concurrency.locks;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

/** Class to implement Locks */
public class Locks {

    private static final LockManager LOCK_MANAGER = new LockManager();

    /** Constructor */
    private Locks() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to get a LockReference
     *
     * @param key key
     * @return a LockReference
     */
    public static LockReference getLock(Object key) {
        notNull(key, "key is null");

        return new DefaultLockReference(LOCK_MANAGER, key.toString());
    }

    /**
     * Execute a Runnable in a lock
     *
     * @param key key
     * @param runnable runnable
     */
    public static void execute(Object key, Runnable runnable) {
        notNull(key, "key is null");
        notNull(runnable, "runnable is null");

        LockReference lockReference = getLock(key);

        try {
            lockReference.lock();
            runnable.run();
        } finally {
            lockReference.unlock();
        }
    }

    /**
     * Execute a Runnable in a lock
     *
     * @param lock lock
     * @param runnable runnable
     */
    public static void execute(Lock lock, Runnable runnable) {
        notNull(lock, "lock is null");
        notNull(runnable, "runnable is null");

        try {
            lock.lock();
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Execute a Runnable in a lock
     *
     * @param readWriteLock readWriteLock
     * @param runnable runnable
     */
    public static void execute(ReadWriteLock readWriteLock, Runnable runnable) {
        notNull(readWriteLock, "readWriteLock is null");
        notNull(runnable, "runnable is null");

        try {
            readWriteLock.writeLock().lock();
            runnable.run();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Execute a Runnable in a lock
     *
     * @param lockProvider lockProvider
     * @param runnable runnable
     */
    public static void execute(LockProvider lockProvider, Runnable runnable) {
        notNull(lockProvider, "lockProvider is null");
        notNull(runnable, "runnable is null");

        try {
            lockProvider.getLock().lock();
            runnable.run();
        } finally {
            lockProvider.getLock().unlock();
        }
    }

    /**
     * Execute a Callable in a lock
     *
     * @param key key
     * @param callable callable
     * @return the callable result
     * @throws Throwable Throwable
     * @param <V> the type
     */
    public static <V> V execute(Object key, Callable<V> callable) throws Throwable {
        notNull(key, "key is null");
        notNull(callable, "callable is null");

        LockReference lockReference = getLock(key);

        try {
            lockReference.lock();
            return callable.call();
        } finally {
            lockReference.unlock();
        }
    }

    /**
     * Execute a Callable in a lock
     *
     * @param lock lock
     * @param callable callable
     * @return the callable result
     * @throws Throwable Throwable
     * @param <V> the type
     */
    public static <V> V execute(Lock lock, Callable<V> callable) throws Throwable {
        notNull(lock, "lock is null");
        notNull(callable, "callable is null");

        try {
            lock.lock();
            return callable.call();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Execute a Callable in a lock
     *
     * @param readWriteLock readWriteLock
     * @param callable callable
     * @return the callable result
     * @throws Throwable Throwable
     * @param <V> the type
     */
    public static <V> V execute(ReadWriteLock readWriteLock, Callable<V> callable)
            throws Throwable {
        notNull(readWriteLock, "readWriteLock is null");
        notNull(callable, "callable is null");

        try {
            readWriteLock.writeLock().lock();
            return callable.call();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Execute a Callable in a lock
     *
     * @param readWriteLockProvider readWriteLockProvider
     * @param callable callable
     * @return the callable result
     * @throws Throwable Throwable
     * @param <V> the type
     */
    public static <V> V execute(ReadWriteLockProvider readWriteLockProvider, Callable<V> callable)
            throws Throwable {
        notNull(readWriteLockProvider, "readWriteLockProvider is null");
        notNull(callable, "callable is null");

        try {
            readWriteLockProvider.getReadWriteLock().writeLock().lock();
            return callable.call();
        } finally {
            readWriteLockProvider.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Checks if an Object is null
     *
     * @param object object
     * @param message message
     */
    private static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Interface to implement LockReference */
    public interface LockReference {

        /** Method to lock the Lock */
        void lock();

        /**
         * Method to try to acquire the Lock
         *
         * @return true if the lock was acquired, else false
         */
        boolean tryLock();

        /**
         * Method to try to acquire the Lock
         *
         * @param time time
         * @param timeUnit timeUnit
         * @return true if the lock was acquired, else false
         * @throws InterruptedException InterruptedException
         */
        boolean tryLock(long time, TimeUnit timeUnit) throws InterruptedException;

        /**
         * Method to unlock the Lock
         *
         * @throws IllegalMonitorStateException if the current thread does not hold this lock
         */
        void unlock();
    }

    /** Class to implement DefaultLockReference */
    private static class DefaultLockReference implements LockReference {

        private final LockManager lockManager;
        private final Object key;

        /**
         * Constructor
         *
         * @param key key
         */
        private DefaultLockReference(LockManager lockManager, Object key) {
            this.lockManager = lockManager;
            this.key = key;
        }

        @Override
        public void lock() {
            lockManager.lock(key);
        }

        @Override
        public boolean tryLock() {
            return lockManager.tryLock(key);
        }

        @Override
        public boolean tryLock(long time, TimeUnit timeUnit) throws InterruptedException {
            notNull(timeUnit, "timeUnit it null");
            return lockManager.tryLock(key, time, timeUnit);
        }

        @Override
        public void unlock() {
            lockManager.unlock(key);
        }

        @Override
        public String toString() {
            return key.toString();
        }
    }

    /** Class to implement LockManager */
    private static class LockManager {

        private final Lock lock = new ReentrantLock(true);
        private final Map<Object, LockHolder> map = new HashMap<>();

        /**
         * Acquires a Lock
         *
         * @param key key
         */
        void lock(Object key) {
            LockHolder lockHolder;

            try {
                lock.lock();

                lockHolder =
                        map.compute(
                                key,
                                (k, lh) -> {
                                    if (lh == null) {
                                        lh = new LockHolder();
                                    }
                                    return lh;
                                });

                lockHolder.increaseLockCount();
            } finally {
                lock.unlock();
            }

            lockHolder.getReentrantLock().lock();
        }

        /**
         * Trys to acquire a Lock
         *
         * @param key key
         * @return rue if the lock was acquired, else false
         */
        boolean tryLock(Object key) {
            LockHolder lockHolder;

            try {
                lock.lock();

                lockHolder =
                        map.compute(
                                key,
                                (k, lh) -> {
                                    if (lh == null) {
                                        lh = new LockHolder();
                                    }
                                    return lh;
                                });

                lockHolder.increaseLockCount();
            } finally {
                lock.unlock();
            }

            return lockHolder.getReentrantLock().tryLock();
        }

        /**
         * Trys to acquire a Lock
         *
         * @param key key
         * @param time time
         * @param timeUnit timeUnit
         * @return true if the lock was acquired, else false
         * @throws InterruptedException InterruptedException
         */
        boolean tryLock(Object key, long time, TimeUnit timeUnit) throws InterruptedException {
            LockHolder lockHolder;

            try {
                lock.lock();

                lockHolder =
                        map.compute(
                                key,
                                (k, lh) -> {
                                    if (lh == null) {
                                        lh = new LockHolder();
                                    }
                                    return lh;
                                });

                lockHolder.increaseLockCount();
            } finally {
                lock.unlock();
            }

            return lockHolder.getReentrantLock().tryLock(time, timeUnit);
        }

        /**
         * Releases a Lock
         *
         * @throws IllegalMonitorStateException if the current thread does not hold the Lock
         */
        void unlock(Object key) {
            try {
                lock.lock();

                LockHolder lockHolder = map.get(key);
                if (lockHolder == null) {
                    throw new IllegalMonitorStateException(
                            format("LockReference [%s] not locked", key));
                }

                if (lockHolder.getLockCount() == 0) {
                    throw new IllegalMonitorStateException(
                            format("LockReference [%s] already unlocked", key));
                }

                lockHolder.getReentrantLock().unlock();
                lockHolder.decreaseLockCount();

                if (lockHolder.getLockCount() == 0) {
                    map.remove(key);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /** Class to implement LockHolder */
    private static class LockHolder {

        private final ReentrantLock reentrantLock;
        private int lockCount;

        /** Constructor */
        LockHolder() {
            reentrantLock = new ReentrantLock(true);
        }

        /**
         * Get the lock
         *
         * @return the Lock
         */
        Lock getReentrantLock() {
            return reentrantLock;
        }

        /** Increase the lock count */
        void increaseLockCount() {
            lockCount++;
        }

        /** Decrease the lock count */
        void decreaseLockCount() {
            lockCount--;
        }

        /**
         * Get the lock count
         *
         * @return the lock count
         */
        int getLockCount() {
            return lockCount;
        }
    }
}
