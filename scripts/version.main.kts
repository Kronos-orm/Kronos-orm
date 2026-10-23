@file:Import("common.main.kts")

import java.nio.file.Path

fun setVersion(value: String, root: Path = repositoryRoot) {
    require(value.matches(Regex("\\d+\\.\\d+\\.\\d+(?:-SNAPSHOT)?"))) {
        "Expected MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-SNAPSHOT"
    }
    val current = versions(root).getValue("version")
    val replacements =
        listOf(
            Triple(
                root.resolve("libs.versions.toml"),
                Regex(
                    "(?m)^(kronos-compiler-plugin\\s*=\\s*\\{[^\\n]*\\bversion\\s*=\\s*\")[^\"]+(\"[^\\n]*\\})$"
                ),
                { match: MatchResult -> match.groupValues[1] + value + match.groupValues[2] },
            ),
            Triple(
                root.resolve("templates/common.module-template.yaml"),
                Regex("(?m)^(    version: )${Regex.escape(current)}$"),
                { match: MatchResult -> match.groupValues[1] + value },
            ),
            Triple(
                root.resolve(
                    "kronos-gradle-plugin/src/main/resources/META-INF/kronos/compiler-plugin.properties"
                ),
                Regex("(?m)^version=${Regex.escape(current)}$"),
                { _: MatchResult -> "version=$value" },
            ),
        )
    val updates = replacements.associate { (path, pattern, replacement) ->
        val text = path.text()
        val count = pattern.findAll(text).count()
        require(count == 1) { "Expected exactly one publication version in $path, found $count" }
        path to pattern.replace(text, replacement)
    }
    updates.forEach { (path, text) -> path.write(text) }
    versions(root)
}

cli {
    if (args.isEmpty() || args[0] in setOf("-h", "--help"))
        println(
            "Usage: scripts/run version show|json|set VERSION|next-snapshot|next-release|release-from-current"
        )
    else {
        require(args.size <= 2) { "Too many version arguments" }
        val current = versions()
        when (args[0]) {
            "show" -> println(current.getValue("version"))
            "json" -> println(jsonMapper.writeValueAsString(current))
            else -> {
                val value =
                    when (args[0]) {
                        "set" -> args.getOrNull(1) ?: error("set requires a version")
                        "release-from-current" ->
                            current.getValue("version").removeSuffix("-SNAPSHOT")
                        "next-release",
                        "next-snapshot" ->
                            current.getValue("version").removeSuffix("-SNAPSHOT").split('.').let {
                                (major, minor, patch) ->
                                "$major.$minor.${patch.toInt() + 1}"
                            } + if (args[0] == "next-snapshot") "-SNAPSHOT" else ""
                        else -> error("Unknown version command: ${args[0]}")
                    }
                setVersion(value)
                println("NEW_VERSION=$value")
            }
        }
    }
}
