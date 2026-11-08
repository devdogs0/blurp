package com.lunova.blurp

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameQueueTest {
    private var requests = 0
    private val queue = FrameQueue { requests++ }
    private val ran = ArrayList<String>()

    private fun callback(name: String, body: (Long) -> Unit = {}) = FrameCallback { t ->
        ran += "$name@$t"
        body(t)
    }

    @Test
    fun postingWhileIdleAsksForOneTick() {
        queue.post(callback("a"))
        queue.post(callback("b"))
        assertEquals(1, requests)
    }

    @Test
    fun aTickRunsEachPostedCallbackOnceWithTheFrameTime() {
        queue.post(callback("a"))
        queue.post(callback("b"))
        queue.tick(16)
        queue.tick(32)
        assertEquals(listOf("a@16", "b@16"), ran)
    }

    @Test
    fun nothingPostedMeansNoMoreTicksAreRequested() {
        queue.post(callback("a"))
        queue.tick(16)
        assertEquals(1, requests)
    }

    @Test
    fun aCallbackPostedDuringATickRunsOnTheNextOne() {
        lateinit var again: FrameCallback
        again = callback("loop") { t -> if (t < 48) queue.post(again) }
        queue.post(again)
        queue.tick(16)
        assertEquals(listOf("loop@16"), ran)
        assertEquals(2, requests)
        queue.tick(32)
        queue.tick(48)
        queue.tick(64)
        assertEquals(listOf("loop@16", "loop@32", "loop@48"), ran)
        assertEquals(3, requests)
    }

    @Test
    fun postingTheSameCallbackTwiceRunsItOnce() {
        val a = callback("a")
        queue.post(a)
        queue.post(a)
        queue.tick(16)
        assertEquals(listOf("a@16"), ran)
    }

    @Test
    fun aRemovedCallbackDoesNotRun() {
        val a = callback("a")
        queue.post(a)
        queue.post(callback("b"))
        queue.remove(a)
        queue.tick(16)
        assertEquals(listOf("b@16"), ran)
    }

    @Test
    fun aCallbackRemovedByAnEarlierOneInTheSameTickDoesNotRun() {
        val b = callback("b")
        queue.post(callback("a") { queue.remove(b) })
        queue.post(b)
        queue.tick(16)
        assertEquals(listOf("a@16"), ran)
    }
}
