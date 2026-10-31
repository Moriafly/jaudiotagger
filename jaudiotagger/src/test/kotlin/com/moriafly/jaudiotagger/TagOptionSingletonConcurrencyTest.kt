package com.moriafly.jaudiotagger

import org.jaudiotagger.tag.TagOptionSingleton
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class TagOptionSingletonConcurrencyTest {
    @Test
    fun concurrentFirstAccessReturnsOneInstancePerKey() {
        val threadCount = 16
        val executor = Executors.newFixedThreadPool(threadCount)
        val prefix = "concurrent-test-" + UUID.randomUUID()
        try {
            repeat(64) { round ->
                val key = prefix + "-same-" + round
                val results = getConcurrently(executor, List(threadCount) { key })
                val expected = results.first()
                results.forEach { assertSame(expected, it) }
                assertSame(expected, TagOptionSingleton.getInstance(key))
            }

            val differentKeys = List(threadCount) { prefix + "-distinct-" + it }
            val differentResults = getConcurrently(executor, differentKeys)
            differentResults.forEachIndexed { index, result ->
                assertSame(result, TagOptionSingleton.getInstance(differentKeys[index]))
                differentResults.take(index).forEach { assertNotSame(it, result) }
            }

            val nullResults = getConcurrently(executor, List(threadCount) { null })
            val nullInstance = nullResults.first()
            nullResults.forEach { assertSame(nullInstance, it) }
            assertSame(nullInstance, TagOptionSingleton.getInstance(null))
            assertNotSame(differentResults.first(), nullInstance)
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(10, TimeUnit.SECONDS)
        }
    }

    private fun getConcurrently(
        executor: ExecutorService,
        keys: List<String?>
    ): List<TagOptionSingleton> {
        val start = CyclicBarrier(keys.size + 1)
        val results = keys.map { key ->
            executor.submit<TagOptionSingleton> {
                start.await(10, TimeUnit.SECONDS)
                TagOptionSingleton.getInstance(key)
            }
        }
        start.await(10, TimeUnit.SECONDS)
        return results.map { it.get(10, TimeUnit.SECONDS) }
    }
}
