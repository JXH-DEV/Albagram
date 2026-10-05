package com.albagram.app.domain.quiz

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class AdvanceLockTest {
    @Test
    fun tryLock_onlySucceedsOnceUntilUnlock() {
        val lock = AdvanceLock()
        assertTrue(lock.tryLock())
        assertFalse(lock.tryLock())
        lock.unlock()
        assertTrue(lock.tryLock())
    }

    @Test
    fun answerAndTimeout_cannotBothAdvance() {
        val lock = AdvanceLock()
        val advances = AtomicInteger(0)
        val start = CountDownLatch(1)
        val done = CountDownLatch(2)

        fun attemptAdvance() {
            start.await()
            if (lock.tryLock()) {
                advances.incrementAndGet()
            }
            done.countDown()
        }

        val t1 = thread { attemptAdvance() }
        val t2 = thread { attemptAdvance() }
        start.countDown()
        done.await()
        t1.join()
        t2.join()
        assertEqualsOne(advances.get())
    }

    private fun assertEqualsOne(value: Int) {
        assertTrue("expected exactly one advance, got $value", value == 1)
    }
}
