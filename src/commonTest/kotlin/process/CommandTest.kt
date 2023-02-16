package process

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlin.test.Test
import kotlin.test.assertEquals

expect val subCommand: String

expect fun shellTest()

class CommandTest {

    @Test
    fun test() {
        println("begin")
        Command(subCommand)
            .stdout(Stdio.Pipe)
            .spawn()
        println("end")
    }

    @Test
    fun testOutput() {
        val expectString = "Hello, Kommand!\n"
        val output = Command(subCommand)
            .stdout(Stdio.Pipe)
            .spawn()
            .waitWithOutput()
        assertEquals(expectString, output)
    }

    @Test
    fun testEcho() {
        val expectString = "Hello, Kommand!"
        val child = Command(subCommand)
            .args("echo")
            .stdin(Stdio.Pipe)
            .stdout(Stdio.Pipe)
            .spawn()
        val writer = child.getChildStdin()!!
        writer.appendLine(expectString)
        writer.close()
        val reader = child.getChildStdout()!!
        val output = reader.readLine()
        assertEquals(expectString, output)
    }

    @Test
    fun testEchoMultiLine() {
        val expectString = "Hello, Kommand!"
        val child = Command(subCommand)
            .args("echo")
            .stdin(Stdio.Pipe)
            .stdout(Stdio.Pipe)
            .spawn()
        val writer = child.getChildStdin()!!
        writer.appendLine(expectString)
        writer.appendLine(expectString)
        writer.flush()
        writer.close()
        val reader = child.getChildStdout()!!
        val lines = reader.lines().toList()
        lines.forEach {
            assertEquals(expectString, it)
        }
        assertEquals(2, lines.count())
    }

    @Test
    fun testError() {
        val expectString = "Hello, Kommand!"
        val child = Command(subCommand)
            .args("error")
            .stdin(Stdio.Pipe)
            .stderr(Stdio.Pipe)
            .spawn()
        val writer = child.getChildStdin()!!
        writer.appendLine(expectString)
        writer.flush()
        writer.close()
        val reader = child.getChildStderr()!!
        val output = reader.readLine()
        assertEquals(expectString, output)
        child.wait()
    }

    @Test
    fun testInterval() {
        val expectLineCount = 5
        var lineCount = 0
        Command(subCommand)
            .args("interval")
            .stdout(Stdio.Pipe)
            .spawn()
            .getChildStdout()
            ?.lines()?.forEach {
                println(it)
                lineCount += 1
            }
        assertEquals(expectLineCount, lineCount)
    }

    @Test
    fun testIntervalWithArgs() {
        val expectLineCount = 10
        var lineCount = 0
        Command(subCommand)
            .args("interval", "10")
            .stdout(Stdio.Pipe)
            .spawn()
            .getChildStdout()
            ?.lines()?.forEach {
                println(it)
                lineCount += 1
            }
        assertEquals(expectLineCount, lineCount)
    }

    @Test
    fun shTest() {
        shellTest()
    }
}