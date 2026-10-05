package com.albagram.app.domain.quiz

class AdvanceLock {
    @Volatile
    private var locked: Boolean = false

    fun tryLock(): Boolean {
        synchronized(this) {
            if (locked) return false
            locked = true
            return true
        }
    }

    fun unlock() {
        synchronized(this) {
            locked = false
        }
    }
}
