@file:Import("../../scripts/common.main.kts")

import java.util.ArrayDeque
import kotlin.system.exitProcess

if (args.size == 1 && args.single() in setOf("--help", "-h")) {
    println("Usage: run-kmp-gradle-gate.main.kts COMMAND [ARGUMENT ...]")
    exitProcess(0)
}

// Preserve streaming build output and expose the useful cause in the GitHub check.
cli {
    require(args.isNotEmpty()) { "Usage: run-kmp-gradle-gate.main.kts COMMAND [ARGUMENT ...]" }
    val command = if (windows && args.take(2) == listOf("bash", "./gradlew")) {
        listOf(System.getenv("COMSPEC") ?: "cmd.exe", "/d", "/c", "gradlew.bat") + args.drop(2)
    } else args.toList()
    val tail = ArrayDeque<String>()
    val diagnostics = ArrayDeque<String>()
    val explanation = mutableListOf<String>()
    var inExplanation = false
    fun remember(queue: ArrayDeque<String>, line: String, limit: Int) {
        queue.addLast(line)
        if (queue.size > limit) queue.removeFirst()
    }
    val result = run(command, cwd = java.nio.file.Path.of(System.getProperty("user.dir")), check = false, onLine = { line ->
            println(line)
            remember(tail, line.trimEnd(), 30)
            if (line.startsWith("e: ")) remember(diagnostics, line.trimEnd(), 20)
            if (line.startsWith("* What went wrong:")) inExplanation = true
            else if (line.startsWith("* Try:")) inExplanation = false
            if (inExplanation && explanation.size < 30) explanation += line.trimEnd()
    })
    val code = result.exitCode
    if (code != 0) {
        val summary = (diagnostics.toList() + explanation).ifEmpty { tail.toList() }.joinToString("\n").takeLast(12000)
        println("::error title=KMP Gradle gate failed::" + summary.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A"))
    }
    exitProcess(code)
}
