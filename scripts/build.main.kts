@file:Import("common.main.kts")

cli {
    if (args.isEmpty() || args[0] in setOf("-h", "--help")) {
        println(
            "Usage: scripts/run build bootstrap|build|test|task|do|publish [Kotlin CLI arguments]"
        )
    } else {
        val command = args[0]
        require(command in setOf("bootstrap", "build", "test", "task", "do", "publish")) {
            "Unknown build command: $command"
        }
        versions()
        bootstrap()
        if (command == "bootstrap")
            require(args.size == 1) { "bootstrap takes no additional arguments" }
        else runToolchain(args.toList())
    }
}
