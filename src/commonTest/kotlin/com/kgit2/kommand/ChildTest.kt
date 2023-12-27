package com.kgit2.kommand

import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import kotlin.test.Test
import kotlin.test.assertEquals

class ChildTest {
    @Test
    fun spawnIntervalOutput() {
        val command = Command(platformEchoPath())
            .arg("interval")
            .stdout(Stdio.Pipe)
        val child = command.spawn()
        val output = child.waitWithOutput()
        val expect = listOf("0", "1", "2", "3", "4").joinToString("\n") + "\n"
        assertEquals(expect, output.stdout)
        val expectFailure = runCatching {
            child.waitWithOutput()
        }.onFailure {
            assertEquals("Child has been consumed", it.message)
        }.isFailure
        assertEquals(true, expectFailure)
    }

    @Test
    fun spawnIntervalStdout() {
        val command = Command(platformEchoPath())
            .arg("interval")
            .stdout(Stdio.Pipe)
        val child = command.spawn()
        var line = child.bufferedStdout()?.readLine()
        var expect = 0
        while (!line.isNullOrEmpty()) {
            assertEquals(expect.toString(), line)
            line = child.bufferedStdout()?.readLine()
            expect += 1
        }
        child.wait()
    }

    @Test
    fun spawnPipedEcho() {
        for (i in 0 .. 1000) {
            val command = Command(platformEchoPath())
                .arg("echo")
                .stdin(Stdio.Pipe)
                .stdout(Stdio.Pipe)
            val child = command.spawn()
            val expect = "Hello World!"
            child.bufferedStdin()?.writeLine(expect)
            child.bufferedStdin()?.flush()
            val line = child.bufferedStdout()?.readLine()
            assertEquals(expect, line)
            child.wait()
        }
    }

    @Test
    fun manyStdout() {
        for (i in 0 .. 100) {
            val command = Command(platformEchoPath())
                .arg("stdout")
                .stdin(Stdio.Pipe)
                .stdout(Stdio.Pipe)
            val child = command.spawn()
            val expect = "Hello World!"
            for (j in 0 .. 100) {
                child.bufferedStdin()?.writeLine("[$j]$expect")
                child.bufferedStdin()?.flush()
                val line = child.bufferedStdout()?.readLine()
                assertEquals("[$j]$expect", line)
            }
            child.wait()
        }
    }
}
