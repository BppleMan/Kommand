package process

import com.kgit2.process.Command
import com.kgit2.process.Stdio
import kotlin.test.assertEquals

actual val eko: String = "eko/target/release/eko"

// actual val subCommand: String = "sub_command/build/install/sub_command/bin/sub_command"

actual fun shellTest() {
    val output = Command("sh")
        .args("-c", "f() { echo username=a; echo password=b; }; f get")
        .stdout(Stdio.Pipe)
        .spawn()
        .waitWithOutput()
    assertEquals("username=a\npassword=b\n", output)
}
