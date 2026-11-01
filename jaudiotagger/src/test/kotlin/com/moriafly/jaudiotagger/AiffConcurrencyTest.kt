package com.moriafly.jaudiotagger

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.aiff.AiffAudioHeader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AiffConcurrencyTest {
    @Test
    fun concurrentReadsKeepCommentTimestamps() {
        val directory = Files.createTempDirectory("aiff-concurrency-")
        val files = listOf(directory.resolve("first.aiff"), directory.resolve("second.aiff"))
        try {
            writeSample(files[0], 3_000_000_000L)
            writeSample(files[1], 3_700_000_000L)

            val expected = files.map(::readComments)
            assertEquals(COMMENT_COUNT, expected[0].size)
            assertEquals(COMMENT_COUNT, expected[1].size)
            assertNotEquals(expected[0], expected[1])

            val threadCount = 8
            val executor = Executors.newFixedThreadPool(threadCount)
            try {
                val start = CyclicBarrier(threadCount + 1)
                val tasks = List(threadCount) { index ->
                    executor.submit {
                        start.await(10, TimeUnit.SECONDS)
                        val fileIndex = index % files.size
                        repeat(100) {
                            assertEquals(expected[fileIndex], readComments(files[fileIndex]))
                        }
                    }
                }
                start.await(10, TimeUnit.SECONDS)
                tasks.forEach { it.get(30, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
                executor.awaitTermination(30, TimeUnit.SECONDS)
            }
        } finally {
            files.forEach { Files.deleteIfExists(it) }
            Files.deleteIfExists(directory)
        }
    }

    private fun readComments(file: Path): List<String> {
        val header = AudioFileIO.read(file.toFile()).audioHeader as AiffAudioHeader
        return header.comments.map { comment ->
            // timestampToDate currently retains the current millisecond
            comment.removeRange(comment.length - 9, comment.length - 5)
        }
    }

    private fun writeSample(file: Path, timestamp: Long) {
        val common = ByteArrayOutputStream()
        DataOutputStream(common).use { output ->
            output.writeShort(1)
            output.writeInt(512)
            output.writeShort(16)
            output.write(byteArrayOf(0x40, 0x0e, 0xac.toByte(), 0x44, 0, 0, 0, 0, 0, 0))
        }

        val comments = ByteArrayOutputStream()
        DataOutputStream(comments).use { output ->
            output.writeShort(COMMENT_COUNT)
            repeat(COMMENT_COUNT) {
                output.writeInt(timestamp.toInt())
                output.writeShort(0)
                output.writeShort(4)
                output.writeBytes("note")
            }
        }

        val body = ByteArrayOutputStream()
        DataOutputStream(body).use { output ->
            output.writeBytes("AIFF")
            writeChunk(output, "COMM", common.toByteArray())
            writeChunk(output, "COMT", comments.toByteArray())
            writeChunk(output, "SSND", ByteArray(8 + 1024))
        }
        DataOutputStream(Files.newOutputStream(file)).use { output ->
            output.writeBytes("FORM")
            output.writeInt(body.size())
            output.write(body.toByteArray())
        }
    }

    private fun writeChunk(output: DataOutputStream, id: String, data: ByteArray) {
        output.writeBytes(id)
        output.writeInt(data.size)
        output.write(data)
        if (data.size % 2 != 0) output.writeByte(0)
    }

    private companion object {
        const val COMMENT_COUNT = 30
    }
}
